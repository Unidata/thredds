/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.ncss.view.dsg.station;

import thredds.server.ncss.exception.FeaturesNotFoundException;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.view.dsg.DsgSubsetWriter;
import ucar.ma2.StructureData;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.PointIteratorFiltered;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ft.point.StationTimeSeriesFeatureImpl;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cwardgar on 2014/05/20.
 */
public abstract class AbstractStationSubsetWriter extends DsgSubsetWriter {
  protected final StationTimeSeriesFeatureCollection stationFeatureCollection;
  protected final List<StationFeature> wantedStations;
  protected boolean headerDone = false;

  public AbstractStationSubsetWriter(FeatureDatasetPoint fdPoint, SubsetParams ncssParams)
          throws NcssException, IOException {
    super(fdPoint, ncssParams);

    List<DsgFeatureCollection> featColList = fdPoint.getPointFeatureCollectionList();
    assert featColList.size() == 1 : "Is there ever a case when this is NOT 1?";
    assert featColList.get(0) instanceof StationTimeSeriesFeatureCollection :
            "This class only deals with StationTimeSeriesFeatureCollections.";

    this.stationFeatureCollection = (StationTimeSeriesFeatureCollection) featColList.get(0);
    this.wantedStations = getStationsInSubset(stationFeatureCollection, ncssParams);

    if (this.wantedStations.isEmpty()) {
      throw new FeaturesNotFoundException("No stations found in subset.");
    }
  }

  protected abstract void writeHeader(StationPointFeature stationPointFeat) throws Exception;

  protected abstract void writeStationPointFeature(StationPointFeature stationPointFeat) throws Exception;

  protected abstract void writeFooter() throws Exception;

  @Override
  public void write() throws Exception {

    // Perform spatial subset.
    StationTimeSeriesFeatureCollection subsettedStationFeatCol = stationFeatureCollection.subsetFeatures(wantedStations);
    int count = 0;

    for (StationTimeSeriesFeature stationFeat : subsettedStationFeatCol) {

      // Perform temporal subset. We do this even when a time instant is specified, in which case wantedRange
      // represents a sanity check (i.e. "give me the feature closest to the specified time, but it must at
      // least be within an hour").
      StationTimeSeriesFeature subsettedStationFeat = stationFeat.subset(wantedRange);

      if (ncssParams.getTime() != null) {
        CalendarDate wantedTime = ncssParams.getTime();
        subsettedStationFeat = new ClosestTimeStationFeatureSubset(
                (StationTimeSeriesFeatureImpl) subsettedStationFeat, wantedTime);
      }

      count += writeStationTimeSeriesFeature(subsettedStationFeat);
    }

    if (count == 0)
      throw new NcssException("No features are in the requested subset");

    writeFooter();
  }

  protected int writeStationTimeSeriesFeature(StationTimeSeriesFeature stationFeat)
          throws Exception {
    int count = 0;
    for (PointFeature pointFeat : stationFeat) {
      assert pointFeat instanceof StationPointFeature :
              "Expected pointFeat to be a StationPointFeature, not a " + pointFeat.getClass().getSimpleName();

      if (!headerDone) {
        writeHeader((StationPointFeature) pointFeat);
        headerDone = true;
      }
      writeStationPointFeature((StationPointFeature) pointFeat);
      count++;
    }
    return count;
  }

  protected static class ClosestTimeStationFeatureSubset extends StationTimeSeriesFeatureImpl {
    private final StationTimeSeriesFeature stationFeat;
    private CalendarDate closestTime;

    protected ClosestTimeStationFeatureSubset(
            StationTimeSeriesFeatureImpl stationFeat, CalendarDate wantedTime) throws IOException {
      super(stationFeat, stationFeat.getTimeUnit(), stationFeat.getAltUnits(), -1);
      this.stationFeat = stationFeat;
      CalendarDateRange cdr = stationFeat.getCalendarDateRange();
      if (cdr != null) {
        getInfo();
        info.setCalendarDateRange(cdr);
      }

      long smallestDiff = Long.MAX_VALUE;

      stationFeat.resetIteration();
      try {
        while (stationFeat.hasNext()) {
          PointFeature pointFeat = stationFeat.next();
          CalendarDate obsTime = pointFeat.getObservationTimeAsCalendarDate();
          long diff = Math.abs(obsTime.getMillis() - wantedTime.getMillis());

          if (diff < smallestDiff) {
            closestTime = obsTime;
          }
        }
      } finally {
        stationFeat.finish();
      }
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return stationFeat.getFeatureData();
    }

    // Filter out PointFeatures that don't have the wantedTime.
    protected static class TimeFilter implements PointFeatureIterator.Filter {
      private final CalendarDate wantedTime;

      protected TimeFilter(CalendarDate wantedTime) {
        this.wantedTime = wantedTime;
      }

      @Override
      public boolean filter(PointFeature pointFeature) {
        return pointFeature.getObservationTimeAsCalendarDate().equals(wantedTime);
      }
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
      if (closestTime == null) {
        return stationFeat.getPointFeatureIterator();
      } else {
        return new PointIteratorFiltered(
                stationFeat.getPointFeatureIterator(), new TimeFilter(closestTime));
      }
    }
  }


  // LOOK could do better : "all", and maybe HashSet<Name>
  public static List<StationFeature> getStationsInSubset(
          StationTimeSeriesFeatureCollection stationFeatCol, SubsetParams ncssParams) throws IOException {

    List<StationFeature> wantedStations;

    // verify SpatialSelection has some stations
    if (ncssParams.getStations() != null) {
      List<String> stnNames = ncssParams.getStations();

      if (stnNames.get(0).equals("all")) {
        wantedStations = stationFeatCol.getStationFeatures();
      } else {
        wantedStations = stationFeatCol.getStationFeatures(stnNames);
      }
    } else if (ncssParams.getLatLonBoundingBox() != null) {
      LatLonRect llrect = ncssParams.getLatLonBoundingBox();
      wantedStations = stationFeatCol.getStationFeatures(llrect);

    } else if (ncssParams.getLatLonPoint() != null) {
      Station closestStation = findClosestStation(stationFeatCol, ncssParams.getLatLonPoint());
      List<String> stnList = new ArrayList<>();
      stnList.add(closestStation.getName());
      wantedStations = stationFeatCol.getStationFeatures(stnList);

    } else { // Want all.
      wantedStations = stationFeatCol.getStationFeatures();
    }

    return wantedStations;
  }

  /*
   * Find the station closest to the specified point.
   * The metric is (lat-lat0)**2 + (cos(lat0)*(lon-lon0))**2
   *
   * @param lat latitude value
   * @param lon longitude value
   * @return name of station closest to the specified point
   * @throws IOException if read error
   */
  public static Station findClosestStation(StationTimeSeriesFeatureCollection stationFeatCol, LatLonPoint pt)
          throws IOException {
    double lat = pt.getLatitude();
    double lon = pt.getLongitude();
    double cos = Math.cos(Math.toRadians(lat));
    List<StationFeature> stations = stationFeatCol.getStationFeatures();
    Station min_station = stations.get(0);
    double min_dist = Double.MAX_VALUE;

    for (Station s : stations) {
      double lat1 = s.getLatitude();
      double lon1 = LatLonPointImpl.lonNormal(s.getLongitude(), lon);
      double dy = Math.toRadians(lat - lat1);
      double dx = cos * Math.toRadians(lon - lon1);
      double dist = dy * dy + dx * dx;
      if (dist < min_dist) {
        min_dist = dist;
        min_station = s;
      }
    }
    return min_station;
  }
}
