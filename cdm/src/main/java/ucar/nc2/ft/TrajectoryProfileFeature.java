/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft;

import java.io.IOException;
import javax.annotation.Nonnull;

import ucar.ma2.StructureData;

/**
 * A collection of profiles which originate along a trajectory.
 *
 * @author caron
 * @since Mar 18, 2008
 */
public interface TrajectoryProfileFeature extends PointFeatureCC, Iterable<ProfileFeature> {

  /**
   * The number of profiles along the trajectory.
   * @return number of profiles along the trajectory, or -1 if not known.
   */
  int size();

  /**
   * The data associated with the Section feature.
   * @return the actual data of this section. may be empty, not null.
   * @throws java.io.IOException on i/o error
   */
  @Nonnull
  StructureData getFeatureData() throws IOException;

  //////////////////////////////////////////////////////////
  // deprecated use foreach

  /**
   * Use the internal iterator to check if there is another ProfileFeature in the iteration.
   * @return true is there is another Section in the iteration.
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
