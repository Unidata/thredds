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
package ucar.nc2.dt2;

import java.io.IOException;
import java.util.Date;

/**
 * A sequence (one-dimensional, variable-length list) of PointObsDatatype, connected in space and time.
 * The observations are ordered in time (in other words, the time dimension must
 * increase monotonically along the trajectory).
 *
 * @author caron
 */
public interface TrajectoryObsFeature extends ObsFeature {

  /**
   * The number of points along the trajectory.
   * @return number of points along the trajectory.
   */
  public int getNumberPoints();


  /**
   * Get a PointObsDatatype for the requested trajectory point.
   *
   * @param point the point along the trajectory
   * @return corresponding PointObsDatatype
   * @throws IOException on read error
   */
  public PointObsFeature getPointObsData(int point) throws IOException;

  /////////////////////////////////////////////////////////////
  // all below are convenience routines

  /**
   * Start date for the trajectory.
   * @return stating date for the trajectory.
   */
  public Date getStartDate();

  /**
   * End date for the trajectory.
   * @return End date for the trajectory.
   */
  public Date getEndDate();

  /**
   * BoundingBox for the trajectory. May not be available.
   * @return BoundingBox for the trajectory. May not be available.
   */
  public ucar.unidata.geoloc.LatLonRect getBoundingBox();

  /**
   * Get the elevation at the requested trajectory point in units of meters, missing values = NaN.
   * @param pt index of the observation.
   * @return the elevation at the requested trajectory point in units of meters, missing values = NaN.
   * @throws IOException on read error
   */
  public double getElevation(int pt) throws IOException;

}
