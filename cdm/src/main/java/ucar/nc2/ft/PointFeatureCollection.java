/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import java.io.IOException;
import javax.annotation.Nullable;

import ucar.nc2.time.CalendarDateRange;

/**
 * A collection of PointFeatures.
 *
 * @author caron
 * @since Mar 1, 2008
 */
public interface PointFeatureCollection extends DsgFeatureCollection, Iterable<PointFeature> {

  /**
   * Subset this collection by boundingBox and/or dateRange
   * @param boundingBox only points in this lat/lon bounding box. may be null.
   * @param dateRange only points in this date range. may be null.
   * @return subsetted collection, may be null if empty
   * @throws IOException on read error
   */
  @Nullable
  PointFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException;

  //////////////////////////////////////////////////////
  // deprecated, use foreach

  /**
   * Use the internal iterator to check if there is another PointFeature in the iteration.
   * Note that this is not thread-safe; use getPointFeatureIterator() for a threadsafe iterator.
   * @return true is there is another PointFeature in the iteration.
   * @throws java.io.IOException on read error
   * @see PointFeatureIterator#hasNext
   * @deprecated use foreach
   */
  boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next PointFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next PointFeature in the iteration
   * @throws java.io.IOException on read error
   * @see PointFeatureIterator#next
   * @deprecated use foreach
   */
  PointFeature next() throws java.io.IOException;

  /**
   * Reset the internal iterator for another iteration over the PointFeatures in this Collection.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  void resetIteration() throws IOException;

  /**
   * Make sure that the internal iterator is complete, and recover resources.
   * You must complete the iteration (until hasNext() returns false) or call finish().
   * @deprecated use foreach
   */
  void finish();

  /**
    * Get an iterator over the PointFeatures of this collection. call PointFeatureIterator.finish() when done
    * @return iterator over the PointFeatures of this collection
    * @throws IOException on read error
    * @deprecated use foreach
    */
   PointFeatureIterator getPointFeatureIterator() throws java.io.IOException;

}
