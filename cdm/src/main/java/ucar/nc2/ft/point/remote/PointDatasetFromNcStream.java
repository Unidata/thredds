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

import ucar.nc2.ft.*;
import ucar.nc2.ft.point.PointCollectionImpl;
import ucar.nc2.ft.point.PointFeatureImpl;
import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;
import ucar.nc2.stream.NcStreamRemote;
import ucar.nc2.stream.NcStream;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.ma2.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.commons.httpclient.HttpMethod;

/**
 * Connect to remote PointFeatureCollection
 *
 * @author caron
 * @since Feb 16, 2009
 */
public class PointDatasetFromNcStream extends PointDatasetImpl {

  static public FeatureDatasetPoint factory(String endpoint) throws IOException {
    NcStreamRemote ncremote = new NcStreamRemote(endpoint, null);
    NetcdfDataset ncd = new NetcdfDataset(ncremote);
    return new PointDatasetFromNcStream(ncd, ncremote);
  }

  private NcStreamRemote ncremote;

  private PointDatasetFromNcStream(NetcdfDataset ncd, NcStreamRemote ncremote) {
    super(ncd, FeatureType.POINT);
    this.ncremote = ncremote;
    collectionList = new ArrayList<FeatureCollection>(1);
    collectionList.add( new RemotePointCollection());
  }

  @Override
  public FeatureType getFeatureType() {
    return FeatureType.POINT;
  }

  private class RemotePointCollection extends PointCollectionImpl {

    RemotePointCollection() {
      super(getLocationURI());
    }

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      try {

        HttpMethod method = ncremote.readSequence(makeRequest());
        InputStream in = method.getResponseBodyAsStream();

        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);
        return new RemotePointFeatureIterator(pfc, method, in);

      } catch (InvalidRangeException e) {
        e.printStackTrace();
        return null;
      }
    }


    private class RemotePointFeatureIterator implements PointFeatureIterator {
      PointStreamProto.PointFeatureCollection pfc;
      HttpMethod method;
      InputStream in;

      int count = 0;
      PointFeature pf;
      DateUnit timeUnit;
      StructureMembers sm;

      RemotePointFeatureIterator(PointStreamProto.PointFeatureCollection pfc, HttpMethod method, InputStream in) throws IOException {
        this.pfc = pfc;
        this.method = method;
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
        if (len <= 0) {
          cancel();
          return false;
        }

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

      public void cancel() {
        if (method != null)
          method.releaseConnection();
        method = null;
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
          ArrayStructureBB asbb = new ArrayStructureBB(sm, new int[]{1}, bb, 0);
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

    private class PointFeatureCollectionSubset extends RemotePointCollection {
      PointCollectionImpl from;

      PointFeatureCollectionSubset(PointCollectionImpl from, LatLonRect filter_bb, DateRange filter_date) throws IOException {
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
  }

  public static void main(String args[]) throws IOException {
    String endpoint = "http://localhost:8080/thredds/ncstream/point/data";
    FeatureDatasetPoint fd = PointDatasetFromNcStream.factory(endpoint);
    PointFeatureCollection pc = (PointFeatureCollection) fd.getPointFeatureCollectionList().get(0);

    PointFeatureIterator pfIter = pc.getPointFeatureIterator(-1);
    while (pfIter.hasNext()) {
      PointFeature pf = pfIter.next();
      System.out.println("pf= " + pf);

    }
  }


}
