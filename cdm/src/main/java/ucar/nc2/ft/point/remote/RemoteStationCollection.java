package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.point.*;
import ucar.nc2.ft.*;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.stream.NcStream;
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
  private String uri;
  protected LatLonRect boundingBox;
  protected DateRange dateRange;
  private boolean restrictedList = false;

  public RemoteStationCollection(String uri) throws IOException {
    super(uri);
    this.uri = uri;
  }

  @Override
  protected void initStationHelper() {
    // read in all the stations with the "stations" query
    stationHelper = new StationHelper();
    HttpMethod method = null;
    try {
      String query = "req=stations";
      method = CdmRemote.sendQuery(uri, query);
      InputStream in = method.getResponseBodyAsStream();

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

  protected RemoteStationCollection(String uri, StationHelper sh) throws IOException {
    super(uri);
    this.uri = uri;
    this.stationHelper = sh;
    this.restrictedList = true;
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
    StationHelper sh = new StationHelper();
    sh.setStations(this.stationHelper.getStations(boundingBox));
    return new RemoteStationCollectionSubset(this, sh, boundingBox, null);
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
      return PointDatasetRemote.makeQuery(query.toString(), boundingBox, dateRange);
    }
  }

  //////////////////////////////////////////////////////////////////////////////


  private class RemoteStationCollectionSubset extends RemoteStationCollection {
    RemoteStationCollection from;

    RemoteStationCollectionSubset(RemoteStationCollection from, StationHelper sh, LatLonRect filter_bb, DateRange filter_date) throws IOException {
      super(from.uri, sh);
      this.from = from;

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

        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);

        riter = new RemotePointFeatureIterator(method, in, new PointDatasetRemote.ProtobufPointFeatureMaker(pfc));
        riter.setCalculateBounds(this);
        return riter;

      } catch (Throwable t) {
        if (method != null) method.releaseConnection();
        throw new IOException(t.getMessage());
      } 
    }
  }

}
