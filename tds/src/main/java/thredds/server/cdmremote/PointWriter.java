/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.cdmremote;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.point.writer.WriterCFPointCollection;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.remote.PointStreamProto;
import ucar.nc2.ft.point.remote.PointStream;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.EarthLocation;
import ucar.ma2.StructureData;
import ucar.ma2.Array;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.Format;

import java.util.*;
import java.io.*;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

/**
 * CdmrFeature subsetting for point data.
 * thread safety: new object for each request
 *
 * @author caron
 * @since Nov 2010
 */
public class PointWriter {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StationWriter.class);

  private static final boolean debug = false, debugDetail = false;

  private final FeatureDatasetPoint fd;
  private final CdmRemoteQueryBean qb;
  private PointFeatureCollection pfc;

  private LatLonRect wantBB;
  private DateRange wantRange;
  private List<VariableSimpleIF> wantVars;
  private ucar.nc2.util.DiskCache2 diskCache;

  public PointWriter(FeatureDatasetPoint fd, PointFeatureCollection pfc, CdmRemoteQueryBean qb, ucar.nc2.util.DiskCache2 diskCache) throws IOException {
    this.fd = fd;
    this.pfc = pfc;
    this.qb = qb;
    this.diskCache = diskCache;
  }

  boolean validate(HttpServletResponse res) throws IOException {

    // verify TemporalSelection intersects
    if (qb.getTemporalSelection() == CdmRemoteQueryBean.TemporalSelection.range) {
      wantRange = qb.getDateRange();
      DateRange haveRange = fd.getDateRange();
      if (!haveRange.intersects(wantRange)) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "ERROR: This dataset does not include the requested time range= " + wantRange +
                "\ndataset time range = " + haveRange);
        return false;
      }
    }

    // restrict to these variables
    List<? extends VariableSimpleIF> dataVars = fd.getDataVariables();
    String[] vars = qb.getVarNames();
    List<String> varNames = (vars == null) ? null : Arrays.asList(vars);
    if ((varNames == null) || (varNames.size() == 0)) {
      wantVars = new ArrayList<VariableSimpleIF>(dataVars);
    } else {
      wantVars = new ArrayList<VariableSimpleIF>();
      for (VariableSimpleIF v : dataVars) {
        if ((varNames == null) || varNames.contains(v.getShortName())) // LOOK N**2
          wantVars.add(v);
      }
       if (wantVars.size() == 0) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "ERROR: This dataset does not include the requested variables= " + qb.getVar());
        return false;
      }
    }

    // verify SpatialSelection has some stations
    if (qb.getSpatialSelection() == CdmRemoteQueryBean.SpatialSelection.bb) {
      wantBB = qb.getLatLonRect();
      LatLonRect haveBB = pfc.getBoundingBox();
      if ((wantBB != null) && (haveBB != null) && (wantBB.intersect(haveBB) == null)) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "ERROR: This dataset does not include the requested bb= " + wantBB);
        return false;
      }

    }

    // let the PointFeatureCollection do the subsetting, then we only have to scan
    this.pfc = ((wantBB != null) || (wantRange != null)) ?
      pfc.subset(wantBB, wantRange) : pfc;

    return true;
  }

  ////////////////////////////////////////////////////////////////
  // writing

  public File writeNetcdf() throws IOException {
    WriterNetcdf w = (WriterNetcdf) write(null);
    return w.netcdfResult;
  }

  public Writer write(HttpServletResponse res) throws IOException {
    long start = System.currentTimeMillis();
    Limit counter = new Limit();
    //counter.limit = 150;

    // which writer, based on desired response
    CdmRemoteQueryBean.ResponseType resType = qb.getResponseType();
    Writer w;
    if (resType == CdmRemoteQueryBean.ResponseType.xml) {
      w = new WriterXML(res.getWriter());
    } else if (resType == CdmRemoteQueryBean.ResponseType.csv) {
      w = new WriterCSV(res.getWriter());
    } else if (resType == CdmRemoteQueryBean.ResponseType.netcdf) {
      w = new WriterNetcdf();
    } else if (resType == CdmRemoteQueryBean.ResponseType.ncstream) {
      w = new WriterNcstream(res.getOutputStream());
    } else {
      log.error("Unknown result type = " + resType);
      return null;
    }

    Action act = w.getAction();
    w.header();
    scan(pfc, wantRange, null, act, counter);

    w.trailer();

    if (debug) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\nread " + counter.count + " records; match and write " + counter.matches + " raw records");
      System.out.println("that took = " + took + " msecs");
    }

    return w;
  }

  /* public boolean intersect(DateRange dr) throws IOException {
    return dr.intersects(start, end);
  } */

  ////////////////////////////////////////////////////////
  // scanning

  // scan collection, records that pass the predicate match are acted on, within limits

  private void scan(PointFeatureCollection collection, DateRange range, Predicate p, Action a, Limit limit) throws IOException {

    while (collection.hasNext()) {
      PointFeature pf = collection.next();

      if (range != null) {
        Date obsDate = pf.getObservationTimeAsDate(); // LOOK: needed?
        if (!range.contains(obsDate)) continue;
      }
      limit.count++;

      StructureData sdata = pf.getData();
      if ((p == null) || p.match(sdata)) {
        a.act(pf, sdata);
        limit.matches++;
      }

      if (limit.matches > limit.limit) {
        collection.finish();
        break;
      }
      if (debugDetail && (limit.matches % 50 == 0)) System.out.println(" matches " + limit.matches);
    }
    collection.finish();

  }

  private void scan(StationTimeSeriesFeatureCollection collection, DateRange range, Predicate p, Action a, Limit limit) throws IOException {

    while (collection.hasNext()) {
      StationTimeSeriesFeature sf = collection.next();

      while (sf.hasNext()) {
        PointFeature pf = sf.next();

        if (range != null) {
          Date obsDate = pf.getObservationTimeAsDate();
          if (!range.contains(obsDate)) continue;
        }
        limit.count++;

        StructureData sdata = pf.getData();
        if ((p == null) || p.match(sdata)) {
          a.act(pf, sdata);
          limit.matches++;
        }

        if (limit.matches > limit.limit) {
          sf.finish();
          break;
        }
        if (debugDetail && (limit.matches % 50 == 0)) System.out.println(" matches " + limit.matches);
      }

      if (limit.matches > limit.limit) {
        collection.finish();
        break;
      }
    }

  }

  private interface Predicate {
    boolean match(StructureData sdata);
  }

  private interface Action {
    void act(PointFeature pf, StructureData sdata) throws IOException;
  }

  private class Limit {
    int count;   // how many scanned
    int limit = Integer.MAX_VALUE; // max matches
    int matches; // how want matched
  }

  abstract class Writer {
    abstract void header();

    abstract Action getAction();

    abstract void trailer();

    java.io.PrintWriter writer;
    DateFormatter format = new DateFormatter();
    int count = 0;

    Writer(final java.io.PrintWriter writer) {
      this.writer = writer; // LOOK what about buffering?
    }
  }

  class WriterNetcdf extends Writer {
    File netcdfResult;
    WriterCFPointCollection cfWriter;
    boolean headerWritten = false;

    WriterNetcdf() throws IOException {
      super(null);
      netcdfResult = diskCache.createUniqueFile("CdmrFeature", ".nc");
      cfWriter = new WriterCFPointCollection(netcdfResult.getAbsolutePath(), "Extracted data from TDS Feature Collection " + fd.getLocation());
    }

    public void header() {
    }

    public void trailer() {
      try {
        cfWriter.finish();
      } catch (IOException e) {
        log.error("WriterNetcdf.trailer", e);
      }
    }

    Action getAction() {
      return new Action() {
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          if (!headerWritten) {
            try {
              cfWriter.writeHeader(wantVars, pf.getTimeUnit(), "m"); // look - fake units
              headerWritten = true;
            } catch (IOException e) {
              log.error("WriterNetcdf.header", e);
            }
          }

          cfWriter.writeRecord(pf, sdata);
          count++;
        }
      };
    }
  }

  class WriterNcstream extends Writer {
    OutputStream out;

    WriterNcstream(OutputStream os) throws IOException {
      super(null);
      out = os;
    }

    public void header() {
    }

    public void trailer() {
      try {
        PointStream.writeMagic(out, PointStream.MessageType.End);
        out.flush();
      } catch (IOException e) {
        log.error("WriterNcstream.trailer", e);
      }
    }

    Action getAction() {
      return new Action() {
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          try {
            if (count == 0) {  // first time : need a point feature so cant do it in header
              PointStreamProto.PointFeatureCollection proto = PointStream.encodePointFeatureCollection(fd.getLocation(), pf);
              byte[] b = proto.toByteArray();
              PointStream.writeMagic(out, PointStream.MessageType.PointFeatureCollection);
              NcStream.writeVInt(out, b.length);
              out.write(b);
            }

            PointStreamProto.PointFeature pfp = PointStream.encodePointFeature(pf);
            byte[] b = pfp.toByteArray();
            PointStream.writeMagic(out, PointStream.MessageType.PointFeature);
            NcStream.writeVInt(out, b.length);
            out.write(b);
            count++;

          } catch (Throwable t) {
            String mess = t.getMessage();
            if (mess == null) mess = t.getClass().getName();
            NcStreamProto.Error err = NcStream.encodeErrorMessage( t.getMessage());
            byte[] b = err.toByteArray();
            PointStream.writeMagic(out, PointStream.MessageType.Error);
            NcStream.writeVInt(out, b.length);
            out.write(b);

            throw new IOException(t);
          }
        }
      };
    }
  }


  class WriterRaw extends Writer {

    WriterRaw(final java.io.PrintWriter writer) {
      super(writer);
    }

    public void header() {
    }

    public void trailer() {
      writer.flush();
    }

    Action getAction() {
      return new Action() {
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          writer.print(format.toDateTimeStringISO(pf.getObservationTimeAsDate()));
          writer.print("= ");
          String report = sdata.getScalarString("report");
          writer.println(report);
          count++;
        }
      };
    }
  }

  class WriterXML extends Writer {
    XMLStreamWriter staxWriter;

    WriterXML(final java.io.PrintWriter writer) {
      super(writer);
      XMLOutputFactory f = XMLOutputFactory.newInstance();
      try {
        staxWriter = f.createXMLStreamWriter(writer);
      } catch (XMLStreamException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    public void header() {
      try {
        staxWriter.writeStartDocument("UTF-8", "1.0");
        staxWriter.writeCharacters("\n");
        staxWriter.writeStartElement("stationFeatureCollection");
        //staxWriter.writeAttribute("dataset", datasetName);
        staxWriter.writeCharacters("\n ");
      } catch (XMLStreamException e) {
        throw new RuntimeException(e.getMessage());
      }

      //writer.println("<?xml version='1.0' encoding='UTF-8'?>");
      //writer.println("<metarCollection dataset='"+datasetName+"'>\n");
    }

    public void trailer() {
      try {
        staxWriter.writeEndElement();
        staxWriter.writeCharacters("\n");
        staxWriter.writeEndDocument();
        staxWriter.close();
      } catch (XMLStreamException e) {
        throw new RuntimeException(e.getMessage());
      }
      writer.flush();
    }

    Action getAction() {
      return new Action() {
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          EarthLocation loc = pf.getLocation();

          try {
            staxWriter.writeStartElement("pointFeature");
            staxWriter.writeAttribute("date", format.toDateTimeStringISO(pf.getObservationTimeAsDate()));
            staxWriter.writeCharacters("\n  ");

            staxWriter.writeStartElement("location");
            staxWriter.writeAttribute("latitude", Format.dfrac(loc.getLatitude(), 3));
            staxWriter.writeAttribute("longitude", Format.dfrac(loc.getLongitude(), 3));
            if (!Double.isNaN(loc.getAltitude()))
              staxWriter.writeAttribute("altitude", Format.dfrac(loc.getAltitude(), 0));
            staxWriter.writeEndElement();
            staxWriter.writeCharacters("\n ");

            for (VariableSimpleIF var : wantVars) {
              staxWriter.writeCharacters(" ");
              staxWriter.writeStartElement("data");
              staxWriter.writeAttribute("name", var.getShortName());
              if (var.getUnitsString() != null)
                staxWriter.writeAttribute("units", var.getUnitsString());

              Array sdataArray = sdata.getArray(var.getShortName());
              String ss = sdataArray.toString();
              Class elemType = sdataArray.getElementType();
              if ((elemType == String.class) || (elemType == char.class) || (elemType == StructureData.class))
                ss = ucar.nc2.util.xml.Parse.cleanCharacterData(ss); // make sure no bad chars
              staxWriter.writeCharacters(ss);
              staxWriter.writeEndElement();
              staxWriter.writeCharacters("\n ");
            }
            staxWriter.writeEndElement();
            staxWriter.writeCharacters("\n");
            count++;
          } catch (XMLStreamException e) {
            throw new RuntimeException(e.getMessage());
          }
        }
      };
    }
  }

  class WriterCSV extends Writer {

    WriterCSV(final java.io.PrintWriter writer) {
      super(writer);
    }

    public void header() {
      writer.print("time,station,latitude[unit=\"degrees_north\"],longitude[unit=\"degrees_east\"]");
      for (VariableSimpleIF var : wantVars) {
        writer.print(",");
        writer.print(var.getShortName());
        if (var.getUnitsString() != null)
          writer.print("[unit=\"" + var.getUnitsString() + "\"]");
      }
      writer.println();
    }

    public void trailer() {
      writer.flush();
    }

    Action getAction() {
      return new Action() {
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          EarthLocation loc = pf.getLocation();

          writer.print(format.toDateTimeStringISO(pf.getObservationTimeAsDate()));
          writer.print(',');
          writer.print(Format.dfrac(loc.getLatitude(), 3));
          writer.print(',');
          writer.print(Format.dfrac(loc.getLongitude(), 3));

          for (VariableSimpleIF var : wantVars) {
            writer.print(',');
            Array sdataArray = sdata.getArray(var.getShortName());
            writer.print(sdataArray.toString());
          }
          writer.println();
          count++;
        }
      };
    }
  }

  static public void main(String args[]) throws IOException {
    //getFiles("R:/testdata2/station/ldm/metar/");
    // StationObsCollection soc = new StationObsCollection("C:/data/metars/", false);
  }

}

