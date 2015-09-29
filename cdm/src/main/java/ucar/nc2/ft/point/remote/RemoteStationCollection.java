/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point.remote;

import ucar.ma2.StructureData;
import ucar.nc2.ft.point.*;
import ucar.nc2.ft.*;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Connect to remote Station Collection using cdmremote
 *
 * @author caron
 */
public class RemoteStationCollection extends StationTimeSeriesCollectionImpl {
  private String uri;
  protected LatLonRect boundingBoxSubset;
  protected CalendarDateRange dateRangeSubset;

  /**
   * Constructor. defer metadata
   *
   * @param uri cdmremote endpoint
   */
  public RemoteStationCollection(String uri, CalendarDateUnit timeUnit, String altUnits) {
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
        stationHelper.addStation(new RemoteStationFeatureImpl(null, null));    // LOOK WRONG
      }
      return stationHelper;
    }
  }

  // note this assumes that a PointFeature is-a StationPointFeature
  @Override
  public Station getStation(PointFeature feature) throws IOException {
    StationPointFeature stationFeature = (StationPointFeature) feature; // LOOK probably will fail here
    return stationFeature.getStation();
  }


  // Must override default subsetting implementation for efficiency: eg to make a single call to server

  // StationTimeSeriesFeatureCollection

  @Override
  public StationTimeSeriesFeatureCollection subset(List<Station> stations) throws IOException {
    if (stations == null) return this;
//    List<StationFeature> subset = getStationHelper().getStationFeatures(stations);
    return new RemoteStationCollectionSubset(this, null, null); // LOOK WRONG
  }

  @Override
  public StationTimeSeriesFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    if (boundingBox == null) return this;
    return new RemoteStationCollectionSubset(this, boundingBox, null);
  }

  // NestedPointFeatureCollection
  @Override
  public PointFeatureCollection flatten(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    //boolean restrictedList = false;
    // QueryMaker queryMaker = restrictedList ? new QueryByStationList() : null;
    RemotePointCollection pfc = new RemotePointCollection(uri, getTimeUnit(), getAltUnits(), null); // queryMaker);
    return pfc.subset(boundingBox, dateRange);
  }

  private class QueryByStationList implements QueryMaker {
    @Override
    public String makeQuery() {
      StringBuilder query = new StringBuilder("stns=");
      for (Station s : getStationHelper().getStations()) {
        query.append(s.getName());
        query.append(",");
      }
      return PointDatasetRemote.makeQuery(query.toString(), boundingBoxSubset, dateRangeSubset);
    }
  }

  //////////////////////////////////////////////////////////////////////////////


  private static class RemoteStationCollectionSubset extends RemoteStationCollection {
    RemoteStationCollection from;

    RemoteStationCollectionSubset(RemoteStationCollection from, LatLonRect filter_bb,
            CalendarDateRange filter_date) throws IOException {
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
      List<StationFeature> stations = from.getStationHelper().getStationFeatures(boundingBoxSubset);
      StationHelper stationHelper = new StationHelper();
      stationHelper.setStations(stations);
      return stationHelper;
    }

    @Override
    public Station getStation(PointFeature feature) throws IOException {
      return from.getStation(feature);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  private class RemoteStationFeatureImpl extends StationTimeSeriesFeatureImpl {
    StationTimeSeriesFeature stnFeature;
    RemotePointFeatureIterator riter;

    RemoteStationFeatureImpl(StationTimeSeriesFeature s, CalendarDateRange dateRange) {
      super(s, RemoteStationCollection.this.getTimeUnit(), RemoteStationCollection.this.getAltUnits(), -1);
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
      return new RemoteStationFeatureImpl(stnFeature, dateRange);
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
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
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

        riter = new RemotePointFeatureIterator(RemoteStationFeatureImpl.this, in, new PointStream.ProtobufPointFeatureMaker(pfc));
        return riter;

      } catch (Throwable t) {
        if (in != null) in.close();
        throw new IOException(t.getMessage(), t);
      }
    }
  }
}
