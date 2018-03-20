/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft;

import java.io.IOException;

/**
 * A collection of SectionFeatures.
 *
 * @author caron
 * @since Mar 19, 2008
 */
public interface TrajectoryProfileFeatureCollection extends PointFeatureCCC, Iterable<TrajectoryProfileFeature> {

  /////////////////////////////////////////////////////////////////////////////

  /**
   * Use the internal iterator to check if there is another SectionFeature in the iteration.
   * @return true is there is another SectionFeature in the iteration.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next SectionFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next SectionFeature in the iteration
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  TrajectoryProfileFeature next() throws java.io.IOException;

  /**
   * Reset the internal iterator for another iteration over the SectionFeatures in this Collection.
   * @throws java.io.IOException on read error
   * @deprecated use foreach
   */
  void resetIteration() throws IOException;

  /**
   * @deprecated use foreach
   */
  PointFeatureCCIterator getNestedPointFeatureCollectionIterator() throws java.io.IOException;

}
