/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import java.io.IOException;
import java.util.List;

import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

/**
 * A collection of StationProfileFeatures
 * @author caron
 * @since Feb 29, 2008
 */
public interface StationProfileFeatureCollection extends PointFeatureCCC, Iterable<StationProfileFeature> {

  List<StationFeature> getStationFeatures() throws IOException;
  List<StationFeature> getStationFeatures( List<String> stnNames)  throws IOException;
  List<StationFeature> getStationFeatures( ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  StationFeature findStationFeature(String name);
  StationProfileFeature getStationProfileFeature(StationFeature s) throws IOException;

  // subsetting
  StationProfileFeatureCollection subset(List<StationFeature> stations) throws IOException;
  StationProfileFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;
  StationProfileFeatureCollection subset(List<StationFeature> stns, CalendarDateRange dateRange) throws IOException;
  StationProfileFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException;


  ///////////////////////////////

  /**
   * Use the internal iterator to check if there is another StationProfileFeature in the iteration.
   * @return true is there is another StationProfileFeature in the iteration.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next StationProfileFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next StationProfileFeature in the iteration
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  StationProfileFeature next() throws java.io.IOException;

  /**
   * Reset the internal iterator for another iteration over the StationProfileFeature in this Collection.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  void resetIteration() throws IOException;

  /**
   * @deprecated use foreach
   */
  PointFeatureCCIterator getNestedPointFeatureCollectionIterator() throws java.io.IOException;


}
