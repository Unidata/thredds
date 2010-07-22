package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.point.*;
import ucar.nc2.ft.*;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.stream.NcStream;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.httpclient.HttpMethod;

/**
 * Connect to remote Station Collection using cdmremote
 *
 * @author caron
 */
public class RemoteStationCollection extends StationTimeSeriesCollectionImpl {
  private String uri;
  protected LatLonRect boundingBoxSubset;
  protected DateRange dateRangeSubset;
  private boolean restrictedList = false;

  /**
   * Constructor. defererred station list.
   * @param uri cdmremote endpoint
   */
  public RemoteStationCollection(String uri) {
    super(uri);
    this.uri = uri;
  }

  /**
   * initialize the stationHelper.
   */
  @Override
  protected void initStationHelper() {
    // read in all the stations with the "stations" query
    stationHelper = new StationHelper();
    HttpMethod method = null;
    try {
      String query = "req=stations";
      method = CdmRemote.sendQuery(uri, query);
      InputStream in = method.getResponseBodyAsStream();

      PointStream.MessageType mtype = PointStream.readMagic(in);
      if (mtype != PointStream.MessageType.StationList) {
        throw new RuntimeException("Station Request: bad response");
      }

      int len = NcStream.readVInt(in);
      byte[] b = new byte[len];
      NcStream.readFully(in, b);
      PointStreamProto.StationList stationsp = PointStreamProto.StationList.parseFrom(b);
      for (ucar.nc2.ft.point.remote.PointStreamProto.Station sp : stationsp.getStationsList()) {
        Station s = new StationImpl(sp.getId(), sp.getDesc(), sp.getWmoId(), sp.getLat(), sp.getLon(), sp.getAlt());
        stationHelper.addStation( new RemoteStationFeatureImpl(s, null));
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);

    } finally {
      if (method != null) method.releaseConnection();
    }
  }

  /**
   * Constructor for a subset. defererred station list.
   * @param uri cdmremote endpoint
   * @param sh station Helper subset or null.
   */
  protected RemoteStationCollection(String uri, StationHelper sh) {
    super(uri);
    this.uri = uri;
    this.stationHelper = sh;
    this.restrictedList = (sh != null);
  }

  // note this assumes that a PointFeature is-a StationPointFeature
  public Station getStation(PointFeature feature) throws IOException {
    StationPointFeature stationFeature = (StationPointFeature) feature; // LOOK probably will fail here
    return stationFeature.getStation();
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
    return new RemoteStationCollectionSubset(this, null, boundingBox, null);
  }

  // NestedPointFeatureCollection
  @Override
  public PointFeatureCollection flatten(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    QueryMaker queryMaker = restrictedList ? new QueryByStationList() : null;
    RemotePointCollection pfc = new RemotePointCollection(uri, queryMaker);
    return pfc.subset(boundingBox, dateRange);
  }

  private class QueryByStationList implements QueryMaker {

    public String makeQuery() {
      StringBuilder query = new StringBuilder("stns=");
      for (Station s : stationHelper.getStations()) {
        query.append(s.getName());
        query.append(",");
      }
      return PointDatasetRemote.makeQuery(query.toString(), boundingBoxSubset, dateRangeSubset);
    }
  }

  //////////////////////////////////////////////////////////////////////////////


  private class RemoteStationCollectionSubset extends RemoteStationCollection {
    RemoteStationCollection from;

    RemoteStationCollectionSubset(RemoteStationCollection from, StationHelper sh, LatLonRect filter_bb, DateRange filter_date) throws IOException {
      super(from.uri, sh);
      this.from = from;

      if (filter_bb == null)
        this.boundingBoxSubset = from.getBoundingBox();
      else
        this.boundingBoxSubset = (from.getBoundingBox() == null) ? filter_bb : from.getBoundingBox().intersect(filter_bb);

      if (filter_date == null) {
        this.dateRangeSubset = from.dateRangeSubset;
      } else {
        this.dateRangeSubset = (from.dateRangeSubset == null) ? filter_date : from.dateRangeSubset.intersect(filter_date);
      }
    }

    @Override
    protected void initStationHelper() {
      from.initStationHelper();
      this.stationHelper = new StationHelper();
      try {
        this.stationHelper.setStations(this.stationHelper.getStations(boundingBoxSubset));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }


    @Override
    public Station getStation(PointFeature feature) throws IOException {
      return from.getStation(feature);
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
        method = CdmRemote.sendQuery(uri, query);
        InputStream in = method.getResponseBodyAsStream();

        PointStream.MessageType mtype = PointStream.readMagic(in);
        if (mtype != PointStream.MessageType.PointFeatureCollection) {
          throw new RuntimeException("Station Request: bad response");
        }

        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);

        riter = new RemotePointFeatureIterator(method, in, new PointStream.ProtobufPointFeatureMaker(pfc));
        riter.setCalculateBounds(this);
        return riter;

      } catch (Throwable t) {
        if (method != null) method.releaseConnection();
        throw new IOException(t.getMessage());
      } 
    }
  }

}
