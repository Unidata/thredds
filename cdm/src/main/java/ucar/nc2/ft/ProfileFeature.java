/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

import ucar.unidata.geoloc.LatLonPoint;

/**
 * A Profile of observations. A set of observations along the vertical (z) axis.
 * All obs have the same lat/lon. Time is either constant, or it may vary with z.
 * The z coordinates are monotonc, but may be increasing or decreasing.
 *
 * @author caron
 * @since Feb 8, 2008
 */
public interface ProfileFeature extends PointFeatureCollection {

  /**
   * The number of points along the z axis.
   * @return number of points along the z axis.
   */
  public int getNumberPoints();

  /**
   * Location of this profile
   * @return the location of this observation as a lat/lon point
   */
  public LatLonPoint getLatLon();

}
