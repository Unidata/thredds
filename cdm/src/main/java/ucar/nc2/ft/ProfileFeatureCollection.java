/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft;

import java.io.IOException;

import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

/**
 * A collection of ProfileFeature.
 *
 * @author caron
 * @since Mar 19, 2008
 */
public interface ProfileFeatureCollection extends PointFeatureCC, Iterable<ProfileFeature> {

  /**
   * Subset this collection by boundingBox
   * @param boundingBox want only profiles in this lat/lon bounding box.
   * @return subsetted collection, may be null if empty
   * @throws IOException on read error
   */
  ProfileFeatureCollection subset(LatLonRect boundingBox) throws IOException;
  ProfileFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException;

  //////////////////////////////////////////////////////
  // deprecated use foreach

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
   * Reset the internal iterator for another iteration over the ProfileFeatures in this Collection.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  void resetIteration() throws IOException;

  /**
   * @deprecated use foreach
   */
  PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws java.io.IOException;

}
