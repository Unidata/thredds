/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
