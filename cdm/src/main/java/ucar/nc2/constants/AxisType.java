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

public class AxisType {

  private static java.util.Map<String,AxisType> hash = new java.util.HashMap<String,AxisType>(10);

  /**
   * represents the runTime coordinate
   */
  public final static AxisType RunTime = new AxisType("RunTime", 0);
  /**
   * represents the ensemble coordinate
   */
  public final static AxisType Ensemble = new AxisType("Ensemble", 1);
  /**
   * represents the time coordinate
   */
  public final static AxisType Time = new AxisType("Time", 2);
  /**
   * represents a x coordinate
   */
  public final static AxisType GeoX = new AxisType("GeoX", 5);
  /**
   * represents a y coordinate
   */
  public final static AxisType GeoY = new AxisType("GeoY", 4);
  /**
   * represents a z coordinate
   */
  public final static AxisType GeoZ = new AxisType("GeoZ", 3);
  /**
   * represents a latitude coordinate
   */
  public final static AxisType Lat = new AxisType("Lat", 4);
  /**
   * represents a longitude coordinate
   */
  public final static AxisType Lon = new AxisType("Lon", 5);
  /**
   * represents a vertical height coordinate
   */
  public final static AxisType Height = new AxisType("Height", 3);
  /**
   * represents a vertical pressure coordinate
   */
  public final static AxisType Pressure = new AxisType("Pressure", 3);
  /**
   * represents a radial azimuth coordinate
   */
  public final static AxisType RadialAzimuth = new AxisType("RadialAzimuth", 2);
  /**
   * represents a radial distance coordinate
   */
  public final static AxisType RadialDistance = new AxisType("RadialDistance", 1);
  /**
   * represents a radial elevation coordinate
   */
  public final static AxisType RadialElevation = new AxisType("RadialElevation", 3);

  private int order; // canonical ordering runTime - ensemble - time - z - y - x  or elev - azimuth - distance
  private String _AxisType;

  private AxisType(String s, int order) {
    this._AxisType = s;
    this.order = order;
    hash.put(s, this);
  }

  /**
   * Find the AxisType that matches this name.
   *
   * @param name match this name
   * @return AxisType or null if no match.
   */
  public static AxisType getType(String name) {
    if (name == null) return null;
    return hash.get(name);
  }

  /**
   * Axis name.
   *
   * @return the string name.
   */
  public String toString() {
    return _AxisType;
  }

  /**
   * canonical ordering: (time, z, x, y) or (time, elevation, azimuth, distance)
   * @param o compare to this  AxisType
   * @return +1, 0, -1 if greater tham, equal to, or less than the given AxisType
   */
  public int compareTo(AxisType o) {
    return order - o.order;
  }

}