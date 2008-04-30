/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ft;

import ucar.nc2.units.DateRange;

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
   public int size();

  /**
   * DateRange for the points along the trajectory. May not be known until after iterating through the collection.
   * @return stating date for the trajectory, or null if not known
   */
  public DateRange getDateRange();

  /**
   * BoundingBox for the trajectory. May not be known until after iterating through the collection.
   * @return BoundingBox for the trajectory, or null if not known.
   */
  public ucar.unidata.geoloc.LatLonRect getBoundingBox();

}
