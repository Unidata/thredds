/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft;

import java.io.IOException;

import ucar.unidata.geoloc.LatLonRect;

/**
 * A collection of TrajectoryFeatures
 *
 * @author caron
 * @since Mar 19, 2008
 */
public interface TrajectoryFeatureCollection extends PointFeatureCC, Iterable<TrajectoryFeature> {

  TrajectoryFeatureCollection subset(LatLonRect boundingBox) throws IOException;

  ////////////////////////////////////////////////////////////////

    /**
     * Use the internal iterator to check if there is another TrajectoryFeature in the iteration.
     * @return true is there is another TrajectoryFeature in the iteration.
     * @throws java.io.IOException on read error
     * @deprecated use foreach
     */
  boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next TrajectoryFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next TrajectoryFeature in the iteration
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  TrajectoryFeature next() throws java.io.IOException;

  /**
   * Reset the internal iterator for another iteration over the TrajectoryFeatures in this Collection.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  void resetIteration() throws IOException;

  /**
   * @deprecated use foreach
   */
  PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws java.io.IOException;


}

