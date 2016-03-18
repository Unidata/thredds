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

package ucar.nc2.constants;

/**
 * Type-safe enumeration of netCDF Coordinate Axis types. These are used for tagging georeferencing axes.
 * Do not change the ordering of these enums, as they are used in protobuf messages, only add new ones onto the end.
 *
 * @author john caron
 */

public enum AxisType {
  RunTime(0, "R"),   // runtime / reference time
  Ensemble(2, "E"),
  Time(1, "T"),
  GeoX(5, "X"),
  GeoY(4, "Y"),
  GeoZ(3, "Z"),     // typically "dimensionless" vertical coordinate
  Lat(4, "Y"),
  Lon(5, "X"),
  Height(3, "Z"),   // vertical height coordinate
  Pressure(3, "Z"), // vertical pressure coordinate
  RadialAzimuth(7),
  RadialDistance(8),
  RadialElevation(6),
  Spectral(1),
  TimeOffset(1,"TO"),  // time offset from runtime / reference time
  Dimension(99,"Dim");      // used for dimension axis (experimental);

  private final int order; // canonical ordering runTime - ensemble - time - z - y - x  or elev - azimuth - distance
  private final String cfAxisName; // X, Y, Z, T from http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#coordinate-types

  AxisType(int order) {
    this.order = order;
    this.cfAxisName = null;
  }

  AxisType(int order, String cfAxisName) {
    this.order = order;
    this.cfAxisName = cfAxisName;
  }

  /**
   * Find the AxisType that matches this name.
   *
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
   *
   * @return order
   */
  public int axisOrder() {
    return order;
  }

  public String getCFAxisName() {
    return cfAxisName;
  }

  public boolean isHoriz() {
    return this == GeoX || this == GeoY || this == Lat || this == Lon;
  }

  public boolean isTime() {
    return this == Time || this == RunTime || this == TimeOffset;
  }

  public boolean isVert() {
    return this == Height || this == Pressure || this == GeoZ;
  }

}