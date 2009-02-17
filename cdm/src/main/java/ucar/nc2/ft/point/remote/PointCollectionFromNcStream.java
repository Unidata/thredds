/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 * 
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
package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.point.PointCollectionImpl;
import ucar.nc2.ft.point.PointFeatureImpl;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;
import ucar.nc2.stream.NcStreamRemote;
import ucar.nc2.stream.NcStream;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.ma2.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author caron
 * @since Feb 16, 2009
 */
public class PointCollectionFromNcStream extends PointCollectionImpl {
  private NcStreamRemote ncremote;

  protected PointCollectionFromNcStream(String endpoint) throws IOException {
    super(endpoint);
    ncremote = new NcStreamRemote(endpoint, null);
    System.out.println(" ncremote= " + ncremote);
  }

  public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
    try {
      InputStream in = ncremote.readSequence(makeRequest());

      int len = NcStream.readVInt(in);
      byte[] b = new byte[len];
      NcStream.readFully(in, b);
      PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);
      return new MyPointFeatureIterator(pfc, in);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      return null;
    }
  }


  private class MyPointFeatureIterator implements PointFeatureIterator {
    PointStreamProto.PointFeatureCollection pfc;
    InputStream in;
    int count = 0;
    PointFeature pf;
    DateUnit timeUnit;
    StructureMembers sm;

    MyPointFeatureIterator(PointStreamProto.PointFeatureCollection pfc, InputStream in) throws IOException {
      this.pfc = pfc;
      this.in = in;

      try {
        timeUnit = new DateUnit(pfc.getTimeUnit());
      } catch (Exception e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

      sm = new StructureMembers(pfc.getName());
      for (PointStreamProto.Member m : pfc.getMembersList()) {
        sm.addMember(m.getName(), m.getDesc(), m.getUnits(), NcStream.decodeDataType(m.getDataType()),
                NcStream.decodeSection(m.getSection()).getShape());
      }
    }

    public boolean hasNext() throws IOException {
      int len = NcStream.readVInt(in);
      if (len <= 0) return false;

      byte[] b = new byte[len];
      NcStream.readFully(in, b);
      PointStreamProto.PointFeature pfp = PointStreamProto.PointFeature.parseFrom(b);
      PointStreamProto.Location locp = pfp.getLoc();
      EarthLocationImpl location = new EarthLocationImpl(locp.getLat(), locp.getLon(), locp.getAlt());

      pf = new MyPointFeature(location, locp.getTime(), locp.getNomTime(), timeUnit, pfp);
      System.out.println(" count= " + count + " pf=" + pf);
      count++;
      return true;
    }

    public PointFeature next() throws IOException {
      return pf;
    }

    public void setBufferSize(int bytes) {
    }

    private class MyPointFeature extends PointFeatureImpl {
      PointStreamProto.PointFeature pfp;

      MyPointFeature(EarthLocation location, double obsTime, double nomTime, DateUnit timeUnit, PointStreamProto.PointFeature pfp) {
        super(location, obsTime, nomTime, timeUnit);
        this.pfp = pfp;
      }

      public StructureData getData() throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(pfp.getData().toByteArray());
        ArrayStructureBB asbb = new ArrayStructureBB(sm, new int[] {1}, bb, 0);
        for (String s : pfp.getSdataList())
          asbb.addObjectToHeap(s); // not quite right
        return asbb.getStructureData(0);
      }

      public String toString() {
        return location + " obs=" + obsTime + " nom=" + nomTime;
      }
    }
  }


  @Override
  public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new PointFeatureCollectionSubset(this, boundingBox, dateRange);
  }

  private class PointFeatureCollectionSubset extends PointCollectionFromNcStream {
    PointCollectionImpl from;

    PointFeatureCollectionSubset(PointCollectionImpl from, LatLonRect filter_bb, DateRange filter_date) throws IOException {
      super(from.getName());
      this.from = from;

      if (filter_bb == null)
        this.boundingBox = from.getBoundingBox();
      else
        this.boundingBox = (from.getBoundingBox() == null) ? filter_bb : from.getBoundingBox().intersect(filter_bb);

      if (filter_date == null) {
        this.dateRange = from.getDateRange();
      } else {
        this.dateRange = (from.getDateRange() == null) ? filter_date : from.getDateRange().intersect(filter_date);
      }
    }
  }

  private String makeRequest() {
    return "request";
  }

  public static void main(String args[]) throws IOException {
    String endpoint = "http://localhost:8080/thredds/ncstream/point/data";
    PointCollectionFromNcStream pc = new PointCollectionFromNcStream(endpoint);

    PointFeatureIterator pfIter = pc.getPointFeatureIterator(-1);
    while (pfIter.hasNext()) {
      PointFeature pf = pfIter.next();
      System.out.println("pf= "+pf);

    }
  }


}
