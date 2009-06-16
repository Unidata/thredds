package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.point.*;
import ucar.nc2.ft.*;
import ucar.nc2.stream.NcStreamRemote;
import ucar.nc2.stream.NcStream;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.HttpMethod;

/**
 * Connect to remote Station Collection
 *
 * @author caron
 */
public class RemoteStationCollection extends StationTimeSeriesCollectionImpl {
  private NcStreamRemote ncremote;
  protected LatLonRect boundingBox;
  protected DateRange dateRange;
  private boolean restrictedList = false;

  public RemoteStationCollection(String name, NcStreamRemote ncremote) throws IOException {
    super(name);
    this.ncremote = ncremote;

    // read in all the stations with the "stations" query
    stationHelper = new StationHelper();
    HttpMethod method = null;
    try {
      String query = "stations";
      method = ncremote.sendQuery(query);
      InputStream in = method.getResponseBodyAsStream();

      int len = NcStream.readVInt(in);
      byte[] b = new byte[len];
      NcStream.readFully(in, b);
      PointStreamProto.StationList stationsp = PointStreamProto.StationList.parseFrom(b);
      for (ucar.nc2.ft.point.remote.PointStreamProto.Station sp : stationsp.getStationsList()) {
        Station s = new StationImpl(sp.getId(), sp.getDesc(), sp.getWmoId(), sp.getLat(), sp.getLon(), sp.getAlt());
        stationHelper.addStation(s);
      }

    } finally {
      if (method != null) method.releaseConnection();
    }
  }

  protected RemoteStationCollection(String name, NcStreamRemote ncremote, StationHelper sh) throws IOException {
    super(name);
    this.ncremote = ncremote;
    this.stationHelper = sh;
    this.restrictedList = true;
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {

    // an anonymous class iterating over the stations
    return new PointFeatureCollectionIterator() {
      Iterator<Station> stationIter = stationHelper.getStations().iterator();

      public boolean hasNext() throws IOException {
        return stationIter.hasNext();
      }

      public PointFeatureCollection next() throws IOException {
        return new RemoteStationFeatureImpl(stationIter.next(), null);
      }

      public void setBufferSize(int bytes) {
      }

      public void finish() {
      }
    };
  }

  // Must override default subsetting implementation for efficiency: eg to make a single call to server

  // StationTimeSeriesFeatureCollection

  @Override
  public StationTimeSeriesFeatureCollection subset(List<Station> stations) throws IOException {
    if (stations == null) return this;
    StationHelper sh = new StationHelper();
    sh.setStations(stations);
    return new RemoteStationCollectionSubset(this, sh, null, null);
  }

  @Override
  public StationTimeSeriesFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    if (boundingBox == null) return this;
    StationHelper sh = new StationHelper();
    sh.setStations(this.stationHelper.getStations(boundingBox));
    return new RemoteStationCollectionSubset(this, sh, boundingBox, null);
  }

  @Override
  public StationTimeSeriesFeature getStationFeature(Station s) throws IOException {
    return new RemoteStationFeatureImpl(s, null);
  }

  // NestedPointFeatureCollection
  @Override
  public PointFeatureCollection flatten(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    QueryMaker queryMaker = restrictedList ? new QueryByStationList() : null;
    RemotePointCollection pfc = new RemotePointCollection(getName() + "-flatten", ncremote, queryMaker);
    return pfc.subset(boundingBox, dateRange);
  }

  private class QueryByStationList implements QueryMaker {

    public String makeQuery() {
      StringBuilder query = new StringBuilder("stns=");
      for (Station s : stationHelper.getStations()) {
        query.append(s.getName());
        query.append(",");
      }
      return PointDatasetRemote.makeQuery(query.toString(), boundingBox, dateRange);
    }
  }

  //////////////////////////////////////////////////////////////////////////////


  private class RemoteStationCollectionSubset extends RemoteStationCollection {

    RemoteStationCollectionSubset(RemoteStationCollection from, StationHelper sh, LatLonRect filter_bb, DateRange filter_date) throws IOException {
      super(from.getName(), from.ncremote, sh);

      if (filter_bb == null)
        this.boundingBox = from.getBoundingBox();
      else
        this.boundingBox = (from.getBoundingBox() == null) ? filter_bb : from.getBoundingBox().intersect(filter_bb);

      if (filter_date == null) {
        this.dateRange = from.dateRange;
      } else {
        this.dateRange = (from.dateRange == null) ? filter_date : from.dateRange.intersect(filter_date);
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  private class RemoteStationFeatureImpl extends StationFeatureImpl {
    RemotePointFeatureIterator riter;
    DateRange dateRange;

    RemoteStationFeatureImpl(Station s, DateRange dateRange) {
      super(s, null, -1);
      this.dateRange = dateRange;
    }

    // Must override default subsetting implementation to make a single call to server

    // StationTimeSeriesFeature

    @Override
    public StationTimeSeriesFeature subset(DateRange dateRange) throws IOException {
      if (dateRange == null) return this;
      return new RemoteStationFeatureImpl(s, dateRange);
    }

    // PointCollection
    @Override
    public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
      if (boundingBox != null) {
        if (!boundingBox.contains(s.getLatLon())) return null;
        if (dateRange == null) return this;
      }
      return subset(dateRange);
    }

    // an iterator over the observations for this station
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      String query = PointDatasetRemote.makeQuery("stn=" + s.getName(), null, dateRange);

      HttpMethod method = null;
      try {
        method = ncremote.sendQuery(query);
        InputStream in = method.getResponseBodyAsStream();

        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);

        riter = new RemotePointFeatureIterator(method, in, new PointDatasetRemote.ProtobufPointFeatureMaker(pfc));
        riter.setCalculateBounds(this);
        return riter;

      } catch (Throwable t) {
        if (method != null) method.releaseConnection();
        // log.error( "", t);
        throw new IOException(t.getMessage());
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////

  /* makes a PointFeature from the raw bytes of the protobuf message
  private class ProtobufPointFeatureMaker implements FeatureMaker {
    private DateUnit dateUnit;
    private StructureMembers sm;

    ProtobufPointFeatureMaker(PointStreamProto.PointFeatureCollection pfc, DateUnit dateUnit) throws IOException {
      this.dateUnit = dateUnit;

      int offset = 0;
      sm = new StructureMembers(pfc.getName());
      for (PointStreamProto.Member m : pfc.getMembersList()) {
        StructureMembers.Member member = sm.addMember(m.getName(), m.getDesc(), m.getUnits(),
                NcStream.decodeDataType(m.getDataType()),
                NcStream.decodeSection(m.getSection()).getShape());
        member.setDataParam(offset);
        //System.out.printf("%s offset=%d%n", member.getName(), offset);
        offset += member.getSizeBytes();
      }
      sm.setStructureSize(offset);
    }

    public PointFeature make(byte[] rawBytes) throws InvalidProtocolBufferException {
      PointStreamProto.PointFeature pfp = PointStreamProto.PointFeature.parseFrom(rawBytes);
      PointStreamProto.Location locp = pfp.getLoc();
      EarthLocationImpl location = new EarthLocationImpl(locp.getLat(), locp.getLon(), locp.getAlt());
      return new MyPointFeature(location, locp.getTime(), locp.getNomTime(), dateUnit, pfp);
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
          asbb.addObjectToHeap(s);
        return asbb.getStructureData(0);
      }

      public String toString() {
        return location + " obs=" + obsTime + " nom=" + nomTime;
      }
    }
  }

      // an iterator over the observations for this station
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      String query = makeQuery(s.getName(), dateRange);

      HttpMethod method = null;
      try {
        method = ncremote.sendQuery(query);
        InputStream in = method.getResponseBodyAsStream();

        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);

        // dont know timeUnit unitil now
        try {
          timeUnit = new DateUnit(pfc.getTimeUnit());
        } catch (Exception e) {
          e.printStackTrace();
        }

        riter = new RemotePointFeatureIterator(method, in, new ProtobufPointFeatureMaker(pfc, timeUnit));
        riter.setCalculateBounds(this);
        return riter;

      } catch (Throwable t) {
        if (method != null) method.releaseConnection();
        // log.error( "", t);
        throw new IOException(t.getMessage());
      }
    }*/


}
