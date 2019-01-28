/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
  Dimension(99,"Dim"),      // used for dimension axis (experimental);
  SimpleGeometryX(100, "SgX"),	// Simple Geometry X
  SimpleGeometryY(101, "SgY"),	// Simple Geometry Y
  SimpleGeometryZ(102, "SgZ"),	// Simple Geometry Z
  SimpleGeometryID(103, "SgID"); // Simple Geometry ID Axis, used for indexing simple geometry variables
  
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