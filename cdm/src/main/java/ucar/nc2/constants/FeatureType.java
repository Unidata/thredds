/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.constants;

/**
 * Enumeration of CDM Feature types, aka "Scientific Data Types".
 *
 * @author john caron
 * @author wchen@usgs.gov
 * @see <a href="http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/reference/FeatureDatasets/Overview.html">CDM Feature Types</a>
 */
/*
1) The CF discrete sampling proposal will be the recommended one for point data when thats finalized. Unfortunately, it will be somewhat different from whats gone before. The CF: prefix is dropped until the namespace proposal can be completed. So those feature types are now proposed to be:

    * point: one or more parameters measured at a set of points in time and space
    * timeSeries: a time-series of data points at the same location, with varying time
    * trajectory: a connected set of data points along a 1D curve in time and space
    * profile: a set of data points along a vertical line
    * timeSeriesProfile: a time-series of profiles at a named location
    * trajectoryProfile: a collection of profiles which originate along a trajectory

The CDM will be backwards compatible, including:

    *   accepting the CF: prefix
    *   being case insensitive
    *   "station" and "stationTimeSeries"as aliases for "timeSeries"
    *   "stationProfile" as alias for "timeSeriesProfile"
    *   "section" as alias for "trajectoryProfile"

I know that CF wants to standardize on other feature types also. Its hard to anticipate what they will come with, but its likely:

    * grid
    * swath

maybe:

    * image
    * radial
    * unstructuredGrid
 */

public enum FeatureType {
  ANY,        // No specific type

  COVERAGE,   // any of the coverage types: GRID, FMRC, SWATH, CURVILINEAR
  GRID,       // seperable coordinates
  FMRC,       // two time dimensions, runtime and forecast time
  SWATH,      // 2D latlon, dependent time, polar orbiting satellites
  CURVILINEAR,// 2D latlon, independent time

  ANY_POINT,  // Any of the point types
  POINT,      // unconnected points
  PROFILE,    // fixed x,y with data along z
  STATION,    // timeseries at named location
  STATION_PROFILE, // timeseries of profiles
  TRAJECTORY, // connected points in space and time
  TRAJECTORY_PROFILE, //  trajectory of profiles

  RADIAL,     // polar coordinates
  STATION_RADIAL, // time series of radial data

  SIMPLE_GEOMETRY, // geospatial associations with data
  
  // experimental
  IMAGE,    // pixels, may not be geolocatable
  UGRID;    // unstructured grids

  /**
   * Find the FeatureType that matches this name.
   *
   * @param name find FeatureType with this name, case insensitive.
   * @return FeatureType or null if no match.
   */
  public static FeatureType getType(String name) {
    if (name == null) return null;
    try {
      return valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) { // lame!
      return null;
    }
  }

  public boolean isPointFeatureType() {
    return (this == FeatureType.POINT) || (this == FeatureType.STATION) || (this == FeatureType.TRAJECTORY) ||
            (this == FeatureType.PROFILE) || (this == FeatureType.STATION_PROFILE) || (this == FeatureType.TRAJECTORY_PROFILE);
  }

  public boolean isCoverageFeatureType() {
    return (this == FeatureType.COVERAGE) ||
            (this == FeatureType.GRID) || (this == FeatureType.FMRC) || (this == FeatureType.SWATH)|| (this == FeatureType.CURVILINEAR);
  }

  public boolean isUnstructuredGridFeatureType() {
    return this == FeatureType.UGRID;
  }

  public boolean isSimpleGeometry() {
	return this == FeatureType.SIMPLE_GEOMETRY;
  }
}
