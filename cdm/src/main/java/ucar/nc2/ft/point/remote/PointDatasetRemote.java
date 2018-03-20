/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.ft.remote.CdmrFeatureDataset;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Client view of a CdmRemote Point Dataset.
 *
 * @author caron
 * @since Feb 16, 2009
 */
public class PointDatasetRemote extends PointDatasetImpl {

  public PointDatasetRemote(FeatureType wantFeatureType, String uri, CalendarDateUnit timeUnit, String altUnits, List<VariableSimpleIF> vars, LatLonRect bb, CalendarDateRange dr) throws IOException {

    super(wantFeatureType);
    setBoundingBox(bb);
    setDateRange(dr);
    setLocationURI(CdmrFeatureDataset.SCHEME + uri);

    dataVariables = new ArrayList<>( vars);

    collectionList = new ArrayList<>(1);
    switch (wantFeatureType) {
      case POINT:
        collectionList.add(new PointCollectionStreamRemote(uri, timeUnit, altUnits, null));
        break;
      case STATION:
        collectionList.add(new StationCollectionStream(uri, timeUnit, altUnits));
        break;
      default:
        throw new UnsupportedOperationException("No implementation for " + wantFeatureType);
    }
  }

  static String makeQuery(String station, LatLonRect boundingBox, CalendarDateRange dateRange) {
    StringBuilder query = new StringBuilder();
    boolean needamp = false;

    if (station != null) {
      query.append(station);
      needamp = true;
    }

    if (boundingBox != null) {
      if (needamp) query.append("&");
      query.append("west=");
      query.append(boundingBox.getLonMin());
      query.append("&east=");
      query.append(boundingBox.getLonMax());
      query.append("&south=");
      query.append(boundingBox.getLatMin());
      query.append("&north=");
      query.append(boundingBox.getLatMax());
      needamp = true;
    }

    if (dateRange != null) {
      if (needamp) query.append("&");
      query.append("time_start=");
      query.append(dateRange.getStart());
      query.append("&time_end=");
      query.append(dateRange.getEnd());
    }

    if (!needamp) query.append("all");
    return query.toString();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /* private class RemotePointCollection extends PointCollectionImpl {

RemotePointCollection() {
  super(getLocation());
}

public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
  HTTPMethod method = null;

  try {
    method = ncremote.sendQuery(makeRequest());
    InputStream in = method.getResponseBodyAsStream();

    int len = NcStream.readVInt(in);
    byte[] b = new byte[len];
    NcStream.readFully(in, b);
    PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);
    PointFeatureIterator iter = new RemotePointFeatureIterator(pfc, method, in);
    iter.setCalculateBounds(this);
    return iter;

  } catch (Throwable t) {
    // log.error(t);
    if (method != null) method.releaseConnection();
    throw new RuntimeException(t);
  }
}

private class RemotePointFeatureIterator extends PointIteratorAbstract {
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
      System.out.println(" total read= " + count);
      finish();
      pf = null;
      return false;
    }

    byte[] b = new byte[len];
    NcStream.readFully(in, b);
    PointStreamProto.PointFeature pfp = PointStreamProto.PointFeature.parseFrom(b);
    PointStreamProto.Location locp = pfp.getLoc();
    EarthLocationImpl location = new EarthLocationImpl(locp.getLat(), locp.getLon(), locp.getAlt());

    pf = new MyPointFeature(location, locp.getTime(), locp.getNomTime(), timeUnit, pfp);
    //System.out.println(" count= " + count + " pf=" + pf);
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
  boolean needamp = false;
  StringBuilder sb = new StringBuilder();
  if (boundingBox != null) {
    sb.append("east=");
    sb.append(boundingBox.getLonMin());
    sb.append("&west=");
    sb.append(boundingBox.getLonMax());
    sb.append("&south=");
    sb.append(boundingBox.getLatMin());
    sb.append("&north=");
    sb.append(boundingBox.getLatMax());
    needamp = true;
  }

  if (dateRange != null) {
    if (needamp) sb.append("&");
    sb.append("time_min=");
    sb.append(dateRange.getStart().toDateTimeStringISO());
    sb.append("&time_max=");
    sb.append(dateRange.getStart().toDateTimeStringISO());
    needamp = true;
  }

  if (!needamp) sb.append("all");
  return sb.toString();
}
}    */

}
