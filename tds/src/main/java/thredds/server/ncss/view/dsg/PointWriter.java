package thredds.server.ncss.view.dsg;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.controller.NcssController;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.server.ncss.view.gridaspoint.NetCDFPointDataWriter;
import thredds.util.ContentType;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.remote.PointStream;
import ucar.nc2.ft.point.remote.PointStreamProto;
import ucar.nc2.ft.point.writer.WriterCFPointCollection;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.Format;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * NCSS subsetting for point data.
 *
 * @author caron
 * @since 10/3/13
 */
public class PointWriter extends AbstractWriter {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(thredds.server.cdmremote.StationWriter.class);

  private static final boolean debug = false, debugDetail = false;

  private PointFeatureCollection pfc;
  private Writer writer;

  private LatLonRect wantBB;

    /// ------- new stuff for decoupling things ----///

  public static PointWriter factory(FeatureDatasetPoint fd, PointFeatureCollection sfc, NcssParamsBean qb,
                                    ucar.nc2.util.DiskCache2 diskCache, OutputStream out, SupportedFormat format) throws IOException, ParseException, NcssException {
    PointWriter sw = new PointWriter(fd, sfc, qb, diskCache);
    sw.writer = sw.getWriterForFormat(out, format);
    return sw;
  }

  public HttpHeaders getHttpHeaders(FeatureDataset fd, SupportedFormat format, String datasetPath) {
    return writer.getHttpHeaders(datasetPath);
  }

  private PointWriter(FeatureDatasetPoint fd, PointFeatureCollection pfc, NcssParamsBean qb, ucar.nc2.util.DiskCache2 diskCache) throws IOException, NcssException {
    super(fd, qb, diskCache);
    this.pfc = pfc;
  }

  /* private boolean validate() throws IOException {

    // verify TemporalSelection intersects
    if (qb.getTemporalSelection() == CdmrfQueryBean.TemporalSelection.range) {
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
    List<String> varNames = qb.getVar();
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
    if (qb.getSpatialSelection() == CdmrfQueryBean.SpatialSelection.bb) {
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
  }   */


  private Writer getWriterForFormat(OutputStream out, SupportedFormat format) throws IOException, ParseException, NcssException {

     Writer w;

     switch (format) {
       case XML_STREAM:
       case XML_FILE:
         w = new WriterXML(new PrintWriter(out), format);
         break;

       case CSV_STREAM:
       case CSV_FILE:
         w = new WriterCSV(new PrintWriter(out), format);
         break;

       case NETCDF3:
         w = new WriterNetcdf(out, format);
         break;

       case NETCDF4:
         w = new WriterNetcdf(out, format);
         break;

       default:
         log.error("Unknown result type = " + format.getFormatName());
         return null;
     }

     return w;
   }


  ////////////////////////////////////////////////////////////////
  // writing

  public File writeNetcdf() throws IOException {
    WriterNetcdf w = (WriterNetcdf) write();
    return w.netcdfResult;
  }

  public Writer write() throws IOException {
    long start = System.currentTimeMillis();
    Limit counter = new Limit();
    //counter.limit = 150;

    pfc.resetIteration();
    Action act = writer.getAction();
    writer.header();
    scan(pfc, wantRange, null, act, counter);

    writer.trailer();

    if (debug) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\nread " + counter.count + " records; match and write " + counter.matches + " raw records");
      System.out.println("that took = " + took + " msecs");
    }

    return writer;
  }

  /* public boolean intersect(DateRange dr) throws IOException {
    return dr.intersects(start, end);
  } */

  ////////////////////////////////////////////////////////
  // scanning

  // scan collection, records that pass the predicate match are acted on, within limits

  private void scan(PointFeatureCollection collection, CalendarDateRange range, Predicate p, Action a, Limit limit) throws IOException {

    collection.resetIteration();
    while (collection.hasNext()) {
      PointFeature pf = collection.next();

      if (range != null) {
        CalendarDate obsDate = pf.getObservationTimeAsCalendarDate(); // LOOK: needed?
        if (!range.includes(obsDate)) continue;
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

  public abstract class Writer {
    abstract void header();

    abstract Action getAction();

    abstract void trailer();

    abstract HttpHeaders getHttpHeaders(String pathInfo);

    SupportedFormat wantFormat;
    boolean isStream;
    java.io.PrintWriter writer;
    int count = 0;

    Writer(java.io.PrintWriter writer, SupportedFormat wantFormat) {
      this.writer = writer;
      this.wantFormat = wantFormat;
      this.isStream = wantFormat.isStream();
    }
  }

  class WriterNetcdf extends Writer {
    File netcdfResult;
    WriterCFPointCollection cfWriter;
    boolean headerWritten = false;
    final NetcdfFileWriter.Version version;
    final OutputStream out;

    WriterNetcdf(OutputStream out, SupportedFormat wantFormat) throws IOException {
      super(null, wantFormat);
      this.version = (wantFormat == SupportedFormat.NETCDF3) ? NetcdfFileWriter.Version.netcdf3 : NetcdfFileWriter.Version.netcdf4;
      this.out = out;

      netcdfResult = diskCache.createUniqueFile("ncssTemp", ".nc");
      List<Attribute> atts = new ArrayList<Attribute>();
      atts.add( new Attribute( CDM.TITLE, "Extracted data from TDS Feature Collection " + fd.getLocation() ));
      cfWriter = new WriterCFPointCollection(null, netcdfResult.getAbsolutePath(), atts );
    }

    HttpHeaders getHttpHeaders(String pathInfo) {

      HttpHeaders httpHeaders = new HttpHeaders();
      //String pathInfo = fd.getTitle();
      String fileName = NetCDFPointDataWriter.getFileNameForResponse(version, pathInfo);
      String url = NcssRequestUtils.getTdsContext().getContextPath() + NcssController.getServletCachePath() + "/" + fileName;
      httpHeaders.set(ContentType.HEADER, wantFormat.getResponseContentType());
      httpHeaders.set("Content-Location", url);
      httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

      return httpHeaders;
    }

    public void header() {
    }

    public void trailer() {
      if (!headerWritten)
        throw new IllegalStateException("no data was written");

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

    WriterNcstream(OutputStream os, SupportedFormat wantFormat) throws IOException {
      super(null, wantFormat);
      out = os;
    }

    public HttpHeaders getHttpHeaders(String pathInfo) {
      return new HttpHeaders();
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

    WriterRaw(final java.io.PrintWriter writer, SupportedFormat wantFormat) {
      super(writer, wantFormat);
    }

    public HttpHeaders getHttpHeaders(String pathInfo) {
      return new HttpHeaders();
    }

    public void header() {
    }

    public void trailer() {
      writer.flush();
    }

    Action getAction() {
      return new Action() {
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          writer.print(CalendarDateFormatter.toDateTimeString(pf.getObservationTimeAsCalendarDate()));
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

    WriterXML(final java.io.PrintWriter writer, SupportedFormat wantFormat) {
      super(writer, wantFormat);
      XMLOutputFactory f = XMLOutputFactory.newInstance();
      try {
        staxWriter = f.createXMLStreamWriter(writer);
      } catch (XMLStreamException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    HttpHeaders getHttpHeaders(String pathInfo) {
      HttpHeaders httpHeaders = new HttpHeaders();

      if (!isStream) {
        httpHeaders.set("Content-Location", pathInfo);
        httpHeaders.set("Content-Disposition", "attachment; filename=\"" + NcssRequestUtils.nameFromPathInfo(pathInfo) + ".xml\"");
      }

      httpHeaders.set(ContentType.HEADER, wantFormat.getResponseContentType());
      return httpHeaders;
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
            staxWriter.writeAttribute("date", CalendarDateFormatter.toDateTimeString(pf.getObservationTimeAsCalendarDate()));
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
                staxWriter.writeAttribute(CDM.UNITS, var.getUnitsString());

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

    WriterCSV(java.io.PrintWriter writer, SupportedFormat wantFormat) {
      super(writer, wantFormat);
    }

    HttpHeaders getHttpHeaders(String pathInfo) {
      HttpHeaders httpHeaders = new HttpHeaders();

      if (!isStream) {
        httpHeaders.set("Content-Location", pathInfo);
        httpHeaders.set("Content-Disposition", "attachment; filename=\"" + NcssRequestUtils.nameFromPathInfo(pathInfo) + ".csv\"");
        httpHeaders.add(ContentType.HEADER, ContentType.csv.getContentHeader());
      } else {
        httpHeaders.add(ContentType.HEADER, ContentType.text.getContentHeader());
      }

      return httpHeaders;
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

          writer.print( CalendarDateFormatter.toDateTimeString(pf.getObservationTimeAsCalendarDate()));
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
}
