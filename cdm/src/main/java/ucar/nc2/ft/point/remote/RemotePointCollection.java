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

package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.point.PointCollectionImpl;
import ucar.nc2.ft.point.PointIteratorAbstract;
import ucar.nc2.ft.point.PointIteratorEmpty;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpMethod;

/**
 * PointCollection over cdmRemote protocol
 *
 * @author caron
 * @since Jun 15, 2009
 */
class RemotePointCollection extends PointCollectionImpl implements QueryMaker {
  private String uri;
  private QueryMaker queryMaker;

  RemotePointCollection(String uri, QueryMaker queryMaker) {
    super(uri);
    this.uri = uri;
    this.queryMaker = (queryMaker == null) ? this : queryMaker;
  }

  public String makeQuery() {
    return PointDatasetRemote.makeQuery(null, boundingBox, dateRange); // default query
  }

  public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
    HttpMethod method = null;
    String errMessage = null;

    try {
      method = CdmRemote.sendQuery(uri, queryMaker.makeQuery());
      InputStream in = method.getResponseBodyAsStream();

      PointStream.MessageType mtype = PointStream.readMagic(in);
      if (mtype == PointStream.MessageType.PointFeatureCollection) {
        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);
        PointFeatureIterator iter = new RemotePointFeatureIterator(method, in, new PointStream.ProtobufPointFeatureMaker(pfc));
        iter.setCalculateBounds(this);
        return iter;

      } else if (mtype == PointStream.MessageType.End) {
        return new PointIteratorEmpty(); // nothing in the iteration

      } else if (mtype == PointStream.MessageType.Error) {
        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        NcStreamProto.Error proto = NcStreamProto.Error.parseFrom(b);
        errMessage = NcStream.decodeErrorMessage(proto);

      } else {
        errMessage = "Illegal pointstream message type= " + mtype; // maybe kill the socket ?
      }

    } catch (Throwable t) {
      if (method != null) method.releaseConnection();
      throw new RuntimeException(t);
    }

    if (errMessage != null)
      throw new IOException(errMessage);
    return null;
  }


  // Must override default subsetting implementation for efficiency

  @Override
  public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new PointFeatureCollectionSubset(this, boundingBox, dateRange);
  }


  private class PointFeatureCollectionSubset extends RemotePointCollection {
    PointCollectionImpl from;

    PointFeatureCollectionSubset(RemotePointCollection from, LatLonRect filter_bb, DateRange filter_date) throws IOException {
      super(from.uri, null);
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

}


/* private class RemotePointFeatureIterator extends PointIteratorAbstract {
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
     e.printStackTrace();
   }

   int offset = 0;
   sm = new StructureMembers(pfc.getName());
   for (PointStreamProto.Member m : pfc.getMembersList()) {
     StructureMembers.Member member = sm.addMember(m.getName(), m.getDesc(), m.getUnits(), NcStream.decodeDataType(m.getDataType()),
             NcStream.decodeSection(m.getSection()).getShape());
     member.setDataParam( offset);
     offset += member.getSizeBytes();
   }
   sm.setStructureSize( offset);
 }

 public boolean hasNext() throws IOException {
   int len = NcStream.readVInt(in);
   if (len <= 0) {
     pf = null;
     return false;
   }

   byte[] b = new byte[len];
   NcStream.readFully(in, b);
   PointStreamProto.PointFeature pfp = PointStreamProto.PointFeature.parseFrom(b);
   PointStreamProto.Location locp = pfp.getLoc();
   EarthLocationImpl location = new EarthLocationImpl(locp.getLat(), locp.getLon(), locp.getAlt());

   pf = new MyPointFeature(location, locp.getTime(), locp.getNomTime(), timeUnit, pfp);
   count++;
   return true;
 }

 public PointFeature next() throws IOException {
   if (pf != null)
     calcBounds(pf);
   return pf;
 }

 public void finish() {
   if (method != null)
     method.releaseConnection();
   method = null;

   finishCalcBounds();
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
} */

