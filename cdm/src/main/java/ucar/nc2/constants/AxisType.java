/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
  //Ensemble(1),
  Ensemble(2),
  /**
   * represents the time coordinate
   */
  //Time(2),
  Time(1),
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
   * @param name match this name
   * @return AxisType or null if no match.
   */
  public static AxisType getType(String name) {
   if (name == null) return null;
    try {
      return valueOf(name);
    } catch (IllegalArgumentException e) { // lame!
      return null;
    }
  }

  /**
   * axis ordering: runTime - ensemble - time - z - y - x  or elev - azimuth - distance
   * @return order
   */
  public int axisOrder() { return order; }

}