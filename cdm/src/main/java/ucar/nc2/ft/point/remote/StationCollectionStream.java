/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point.remote;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ucar.ma2.StructureData;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.point.PointIteratorEmpty;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.StationHelper;
import ucar.nc2.ft.point.StationTimeSeriesCollectionImpl;
import ucar.nc2.ft.point.StationTimeSeriesFeatureImpl;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Connect to remote Station Collection using cdmremote
 *
 * @author caron
 */
public class StationCollectionStream extends StationTimeSeriesCollectionImpl {
  private String uri;
  protected LatLonRect boundingBoxSubset;
  protected CalendarDateRange dateRangeSubset;

  /**
   * Constructor. defer metadata
   *
   * @param uri cdmremote endpoint
   */
  public StationCollectionStream(String uri, CalendarDateUnit timeUnit, String altUnits) {
    super(uri, timeUnit, altUnits);
    this.uri = uri;
  }

  /**
   * initialize the stationHelper.
   */
  @Override
  protected StationHelper createStationHelper() throws IOException {
    // read in all the stations with the "stations" query
    StationHelper stationHelper = new StationHelper();

    try (InputStream in = CdmRemote.sendQuery(null, uri, "req=stations")) {
      PointStream.MessageType mtype = PointStream.readMagic(in);
      if (mtype != PointStream.MessageType.StationList) {
        throw new RuntimeException("Station Request: bad response");
      }

      int len = NcStream.readVInt(in);
      byte[] b = new byte[len];
      NcStream.readFully(in, b);
      PointStreamProto.StationList stationsp = PointStreamProto.StationList.parseFrom(b);
      for (ucar.nc2.ft.point.remote.PointStreamProto.Station sp : stationsp.getStationsList()) {
//        Station s = new StationImpl(sp.getId(), sp.getDesc(), sp.getWmoId(), sp.getLat(), sp.getLon(), sp.getAlt());
        stationHelper.addStation(new StationFeatureStream(null, null));    // LOOK WRONG
      }
      return stationHelper;
    }
  }

  // note this assumes that a PointFeature is-a StationPointFeature
  /* @Override
  public StationFeature getStation(PointFeature feature) throws IOException {
    return (StationFeature) feature; // LOOK probably will fail here
  } */


  // Must override default subsetting implementation for efficiency: eg to make a single call to server

  // StationTimeSeriesFeatureCollection

  @Override
  public StationTimeSeriesFeatureCollection subset(List<StationFeature> stations) throws IOException {
    if (stations == null) return this;
//    List<StationFeature> subset = getStationHelper().getStationFeatures(stations);
    return new Subset(this, null, null); // LOOK WRONG
  }

  @Override
  public StationTimeSeriesFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    if (boundingBox == null) return this;
    return new Subset(this, boundingBox, null);
  }

  // NestedPointFeatureCollection
  @Override
  public PointFeatureCollection flatten(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    //boolean restrictedList = false;
    //QueryMaker queryMaker = restrictedList ? new QueryByStationList() : null;
    PointFeatureCollection pfc = new PointCollectionStreamRemote(uri, getTimeUnit(), getAltUnits(), null);
    return pfc.subset(boundingBox, dateRange);
  }

  private class QueryByStationList implements QueryMaker {
    @Override
    public String makeQuery() {
      StringBuilder query = new StringBuilder("stns=");
      for (StationFeature s : getStationHelper().getStationFeatures()) {
        query.append(s.getName());
        query.append(",");
      }
      return PointDatasetRemote.makeQuery(query.toString(), boundingBoxSubset, dateRangeSubset);
    }
  }

  //////////////////////////////////////////////////////////////////////////////


  private static class Subset extends StationCollectionStream {
    StationCollectionStream from;

    Subset(StationCollectionStream from, LatLonRect filter_bb, CalendarDateRange filter_date) throws IOException {
      super(from.uri, from.getTimeUnit(), from.getAltUnits());
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
    protected StationHelper createStationHelper() throws IOException {
      List<ucar.nc2.ft.point.StationFeature> stations = from.getStationHelper().getStationFeatures(boundingBoxSubset);
      StationHelper stationHelper = new StationHelper();
      stationHelper.setStations(stations);
      return stationHelper;
    }

  }

  ///////////////////////////////////////////////////////////////////////////////

  private class StationFeatureStream extends StationTimeSeriesFeatureImpl {
    StationTimeSeriesFeature stnFeature;
    PointIteratorStream riter;

    StationFeatureStream(StationTimeSeriesFeature s, CalendarDateRange dateRange) {
      super(s, StationCollectionStream.this.getTimeUnit(), StationCollectionStream.this.getAltUnits(), -1);
      this.stnFeature = s;
      if (dateRange != null) {
        getInfo();
        info.setCalendarDateRange(dateRange);
      }
    }

    // Must override default subsetting implementation to make a single call to server

    // StationTimeSeriesFeature

    @Override
    public StationTimeSeriesFeature subset(CalendarDateRange dateRange) throws IOException {
      if (dateRange == null) return this;
      return new StationFeatureStream(stnFeature, dateRange);
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return stnFeature.getFeatureData();
    }

    // PointCollection
    @Override
    @Nullable
    public PointFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
      if (boundingBox != null) {
        if (!boundingBox.contains(s.getLatLon())) return null;
        if (dateRange == null) return this;
      }
      return subset(dateRange);
    }

    // an iterator over the observations for this station
    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
      String query = PointDatasetRemote.makeQuery("stn=" + s.getName(), null, getInfo().getCalendarDateRange(this.getTimeUnit()));

      InputStream in = null;
      try {
        in = CdmRemote.sendQuery(null, uri, query);

        PointStream.MessageType mtype = PointStream.readMagic(in);
        if (mtype == PointStream.MessageType.End) {  // no obs were found
          in.close();
          return new PointIteratorEmpty(); // return empty iterator
        }

        if (mtype != PointStream.MessageType.PointFeatureCollection) {
          throw new RuntimeException("Station Request: bad response = " + mtype);
        }

        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);

        riter = new PointIteratorStream(StationFeatureStream.this, in, new PointStream.ProtobufPointFeatureMaker(pfc));
        return riter;

      } catch (Throwable t) {
        if (in != null) in.close();
        throw new IOException(t.getMessage(), t);
      }
    }
  }
}
