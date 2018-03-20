/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import java.io.IOException;
import java.util.List;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

/**
 * A collection of StationTimeSeriesFeature.
 *
 * @author caron
 */
public interface StationTimeSeriesFeatureCollection extends PointFeatureCC, Iterable<StationTimeSeriesFeature> {

  List<StationFeature> getStationFeatures() throws IOException;
  List<StationFeature> getStationFeatures( List<String> stnNames)  throws IOException;
  List<StationFeature> getStationFeatures( ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  StationFeature findStationFeature(String name);
  StationTimeSeriesFeature getStationTimeSeriesFeature(StationFeature s) throws IOException;

  // subsetting
  StationTimeSeriesFeatureCollection subset(List<StationFeature> stations) throws IOException;
  StationTimeSeriesFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;
  StationTimeSeriesFeatureCollection subset(List<StationFeature> stns, CalendarDateRange dateRange) throws IOException;
  StationTimeSeriesFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException;

  /**
   * Flatten into a PointFeatureCollection, discarding connectedness information.
   *
   * @param stations only contain these stations; if null or empty use all
   * @param dateRange only points in this date range. may be null.
   * @param varList only these member variables. may be null. currently ignored
   * @return a PointFeatureCollection, may be null if its empty.
   * @throws IOException on read error
   */
  PointFeatureCollection flatten(List<String> stations, CalendarDateRange dateRange, List<VariableSimpleIF> varList) throws IOException;
  PointFeatureCollection flatten(LatLonRect llbbox, CalendarDateRange dateRange) throws IOException;
  StationFeature getStationFeature(PointFeature flatPointFeature) throws IOException; // for flattened point only

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // deprecated

  // use subset()
  StationTimeSeriesFeatureCollection subsetFeatures(List<StationFeature> stations) throws IOException;

  /**
   * Use the internal iterator to check if there is another StationTimeSeriesFeature in the iteration.
   * @return true is there is another StationTimeSeriesFeature in the iteration.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next StationTimeSeriesFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next StationTimeSeriesFeature in the iteration
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  StationTimeSeriesFeature next() throws java.io.IOException;

  /**
   * Make sure that the internal iterator is complete, and recover resources.
   * You must complete the iteration (until hasNext() returns false)
   *  or call finish().
   * @see PointFeatureIterator#close
   * @deprecated use foreach
   */
  void finish();

  /**
   * Reset the internal iterator for another iteration over the StationTimeSeriesFeatures in this Collection.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  void resetIteration() throws IOException;

  /**
   * @deprecated use foreach
   */
  PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws java.io.IOException;

}
