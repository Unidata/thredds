/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;

import ucar.ma2.StructureData;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

/**
 * Time series of ProfileFeature at named locations.
 * @author caron
 * @since Feb 29, 2008
 */
public interface StationProfileFeature extends StationFeature, PointFeatureCC, Iterable<ProfileFeature> {

  /**
   * The number of profiles in the time series. May not be known until after iterating through the collection.
   * @return number of profiles in the time series, or -1 if not known.
   */
  int size();

  /**
   * Subset this collection by dateRange
   * @param dateRange only points in this date range. may be null.
   * @return subsetted collection, may be null if empty
   * @throws java.io.IOException on read error
   */
  StationProfileFeature subset(CalendarDateRange dateRange) throws IOException;


  /**
   * Get the list of times in the time series of profiles. Note that this may be as costly as iterating over the collection.
   * @return list of times in the time series of profiles.
   * @throws java.io.IOException on read error
   */
  List<CalendarDate> getTimes() throws IOException;

  /**
   * Get a particular profile by date. Note that this may be as costly as iterating over the collection.
   * @param date get profile matching this date.
   * @return profile whose date matches the given date
   * @throws java.io.IOException on read error
   */
  ProfileFeature getProfileByDate(CalendarDate date) throws IOException;

  /**
   * The data associated with the StationProfile feature.
   * @return the actual data of this section. may be empty, not null.
   * @throws java.io.IOException on i/o error
   */
  @Nonnull
  StructureData getFeatureData() throws IOException;

  ////////////////////////////////////////////////////////////////////////////

  /**
   * Use the internal iterator to check if there is another ProfileFeature in the iteration.
   * @return true is there is another ProfileFeature in the iteration.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next ProfileFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next ProfileFeature in the iteration
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  ProfileFeature next() throws java.io.IOException;

  /**
   * Reset the internal iterator for another iteration over the ProfileFeature in this Collection.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  void resetIteration() throws IOException;

  /**
   * @deprecated use foreach
   */
  PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws java.io.IOException;


}
