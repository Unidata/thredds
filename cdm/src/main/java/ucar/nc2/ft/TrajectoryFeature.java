/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import ucar.ma2.StructureData;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * A set of observations along a 1 dimensional path, connected in space and time.
 * The observations are ordered in time (in other words, the time dimension must
 * increase monotonically along the trajectory).
 *
 * @author edavis
 * @author caron
 */
public interface TrajectoryFeature extends PointFeatureCollection {

  /**
    * The number of points along the trajectory. May not be known until after iterating through the collection.
    * @return number of points along the trajectory, or -1 if not known.
    */
  int size();

  /**
   * DateRange for the points along the trajectory. May not be known until after iterating through the collection.
   * @return stating date for the trajectory, or null if not known
   */
  CalendarDateRange getCalendarDateRange();

  /**
   * BoundingBox for the trajectory. May not be known until after iterating through the collection.
   * @return BoundingBox for the trajectory, or null if not known.
   */
  ucar.unidata.geoloc.LatLonRect getBoundingBox();

  /**
   * The actual data of just this Trajectory feature.
   * @return the actual data of this Trajectory, may not be null but may be empty.
   * @throws java.io.IOException on i/o error
   */
  @Nonnull
  StructureData getFeatureData() throws IOException;

}
