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

package ucar.nc2.constants;

/**
 * Type-safe enumeration of netCDF Coordinate Axis types. These are used for tagging
 * georeferencing axes.
 *
 * @author john caron
 */

public enum AxisType {

  //private static java.util.Map<String,AxisType> hash = new java.util.HashMap<String,AxisType>(10);

  /**
   * represents the runTime coordinate
   */
  RunTime(0),
  /**
   * represents the ensemble coordinate
   */
  Ensemble(1),
  /**
   * represents the time coordinate
   */
  Time(2),
  /**
   * represents a x coordinate
   */
  GeoX(5),
  /**
   * represents a y coordinate
   */
  GeoY(4),
  /**
   * represents a z coordinate
   */
  GeoZ(3),
  /**
   * represents a latitude coordinate
   */
  Lat(4),
  /**
   * represents a longitude coordinate
   */
  Lon(5),
  /**
   * represents a vertical height coordinate
   */
  Height(3),
  /**
   * represents a vertical pressure coordinate
   */
  Pressure(3),
  /**
   * represents a radial azimuth coordinate
   */
  RadialAzimuth(7),
  /**
   * represents a radial distance coordinate
   */
  RadialDistance(8),
  /**
   * represents a radial elevation coordinate
   */
  RadialElevation(6);

  private int order; // canonical ordering runTime - ensemble - time - z - y - x  or elev - azimuth - distance
  //private String _AxisType;

  private AxisType(int order) {
    //this._AxisType = s;
    this.order = order;
    //hash.put(s, this);
  }

  /**
   * Find the AxisType that matches this name.
   *
   * @param name match this name
   * @return AxisType or null if no match.
   * @deprecated use valueOf() directly
   */
  public static AxisType getType(String name) {
    return valueOf( name);
  }

  /**
   * axis ordering: runTime - ensemble - time - z - y - x  or elev - azimuth - distance
   * @return order
   */
  public int axisOrder() { return order; }

}