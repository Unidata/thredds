/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.constants;

import ucar.nc2.NetcdfFile;

/**
 * Constants used in CF Conventions.
 *
 * @author caron
 * @since Jan 21, 2009
 */
public interface CF {

  // attribute names
  public static final String AXIS = "axis";
  public static final String BOUNDS = "bounds";
  public static final String CALENDAR = "calendar";
  public static final String CELL_METHODS = "cell_methods";
  public static final String CF_ROLE = "cf_role";
  public final static String COORDINATES = "coordinates";
  public static final String FEATURE_TYPE = "featureType";
  public static final String POSITIVE = "positive";
  public static final String STANDARD_NAME = "standard_name";
  
  public static final String FORMULA_TERMS = "formula_terms";

  // positive values
  public final static String POSITIVE_UP = "up";
  public final static String POSITIVE_DOWN = "down";

  // grid mapping names
  public final static String ALBERS_CONICAL_EQUAL_AREA = "albers_conical_equal_area";
  public final static String AZIMUTHAL_EQUIDISTANT = "azimuthal_equidistant";
  public final static String GEOSTATIONARY = "geostationary";
  public final static String LAMBERT_AZIMUTHAL_EQUAL_AREA = "lambert_azimuthal_equal_area";
  public final static String LAMBERT_CONFORMAL_CONIC = "lambert_conformal_conic";
  public final static String LAMBERT_CYLINDRICAL_EQUAL_AREA = "lambert_cylindrical_equal_area";
  public final static String LATITUDE_LONGITUDE = "latitude_longitude";
  public final static String MERCATOR = "mercator";
  public final static String ORTHOGRAPHIC = "orthographic";
  public final static String POLAR_STEREOGRAPHIC = "polar_stereographic";
  public final static String ROTATED_LATITUDE_LONGITUDE = "rotated_latitude_longitude";
  public final static String STEREOGRAPHIC = "stereographic";
  public final static String SINUSOIDAL = "sinusoidal";       // NOY
  public final static String TRANSVERSE_MERCATOR = "transverse_mercator";
  public final static String VERTICAL_PERSPECTIVE = "vertical_perspective";

  // for grid_mappings
  public final static String EARTH_RADIUS = "earth_radius";
  public static final String FALSE_EASTING = "false_easting";
  public static final String FALSE_NORTHING = "false_northing";
  public static final String GRID_MAPPING = "grid_mapping";
  public static final String FIXED_ANGLE_AXIS = "fixed_angle_axis";  //  geostationary
  public static final String GRID_MAPPING_NAME = "grid_mapping_name";
  public static final String GRID_NORTH_POLE_LATITUDE = "grid_north_pole_latitude";
  public static final String GRID_NORTH_POLE_LONGITUDE = "grid_north_pole_longitude";
  public static final String INVERSE_FLATTENING = "inverse_flattening";
  public static final String LATITUDE_OF_PROJECTION_ORIGIN = "latitude_of_projection_origin";
  public static final String LONGITUDE_OF_PROJECTION_ORIGIN = "longitude_of_projection_origin";
  public static final String LATITUDE_OF_PRIME_MERIDIAN = "latitude_of_prime_meridian";
  public static final String LONGITUDE_OF_PRIME_MERIDIAN = "longitude_of_prime_meridian";
  public static final String LONGITUDE_OF_CENTRAL_MERIDIAN = "longitude_of_central_meridian";
  public static final String NORTH_POLE_GRID_LONGITUDE = "north_pole_grid_longitude";
  public static final String PERSPECTIVE_POINT_HEIGHT = "perspective_point_height";   // geostationary
  public static final String SCALE_FACTOR_AT_CENTRAL_MERIDIAN = "scale_factor_at_central_meridian";
  public static final String SCALE_FACTOR_AT_PROJECTION_ORIGIN = "scale_factor_at_projection_origin";
  public static final String SEMI_MAJOR_AXIS = "semi_major_axis";
  public static final String SEMI_MINOR_AXIS = "semi_minor_axis";
  public static final String SWEEP_ANGLE_AXIS = "sweep_angle_axis"; // geostationary
  public static final String STANDARD_PARALLEL = "standard_parallel";
  public static final String STRAIGHT_VERTICAL_LONGITUDE_FROM_POLE = "straight_vertical_longitude_from_pole";

  // vertical coordinate
  public static final String atmosphere_ln_pressure_coordinate = "atmosphere_ln_pressure_coordinate";

  public static final String formula_terms = "formula_terms";

  // standard_names
  public static final String TIME = "time";                               // valid; time, obs time
  public static final String TIME_REFERENCE = "forecast_reference_time";  // the "data time", the time of the analysis from which the forecast was made.

  public static final String PROJECTION_X_COORDINATE = "projection_x_coordinate";
  public static final String PROJECTION_Y_COORDINATE = "projection_y_coordinate";

  public static final String GRID_LONGITUDE = "grid_longitude";
  public static final String GRID_LATITUDE = "grid_latitude";

  // cf_role
  public static final String PROFILE_ID = "profile_id";
  public static final String TIMESERIES_ID = "timeseries_id"; // alias STATION_ID
  public static final String TRAJECTORY_ID = "trajectory_id";

  // DSG
  public static final String SAMPLE_DIMENSION = "sample_dimension";
  public static final String INSTANCE_DIMENSION = "instance_dimension";

  public static final String PLATFORM_NAME = "platform_name"; // instead of STATION_DESC
  public static final String SURFACE_ALTITUDE = "surface_altitude"; // alias STATION_ALTITUDE
  public static final String PLATFORM_ID = "platform_id";  // alias STATION_WMOID

  ///////////////////////////////////////////////////////////////////
  // DSG proposed - not adopted; here for backwards compatibility
  public static final String RAGGED_ROWSIZE = "CF:ragged_row_count";
  public static final String RAGGED_PARENTINDEX = "CF:ragged_parent_index";

  // proposed standard_names
  public static final String STATION_ID = "station_id";
  public static final String STATION_DESC = "station_description";
  public static final String STATION_ALTITUDE = "station_altitude";
  public static final String STATION_WMOID = "station_WMO_id";

  public static final String featureTypeAtt2 = "CF:featureType";
  public static final String featureTypeAtt3 = "CF:feature_type"; // GRIB was using this form (!)
  ///////////////////////////////////////////////////////////////////////

  /**
   * Map from CF feature type names to our FeatureType enums.
   */
  public enum FeatureType {
    point, timeSeries, profile, trajectory, timeSeriesProfile, trajectoryProfile;

    public static FeatureType convert(ucar.nc2.constants.FeatureType ft) {
      switch (ft) {
        case POINT:
          return CF.FeatureType.point;
        case STATION:
          return CF.FeatureType.timeSeries;
        case PROFILE:
          return CF.FeatureType.profile;
        case TRAJECTORY:
          return CF.FeatureType.trajectory;
        case STATION_PROFILE:
          return CF.FeatureType.timeSeriesProfile;
        case SECTION:
          return CF.FeatureType.trajectoryProfile;
      }
      return null;
    }

    public static ucar.nc2.constants.FeatureType convert(FeatureType cff) {
      switch (cff) {
        case point:
          return ucar.nc2.constants.FeatureType.POINT;
        case timeSeries:
          return ucar.nc2.constants.FeatureType.STATION;
        case profile:
          return ucar.nc2.constants.FeatureType.PROFILE;
        case trajectory:
          return ucar.nc2.constants.FeatureType.TRAJECTORY;
        case timeSeriesProfile:
          return ucar.nc2.constants.FeatureType.STATION_PROFILE;
        case trajectoryProfile:
          return ucar.nc2.constants.FeatureType.SECTION;
      }
      return null;
    }

    /*
1) The CF discrete sampling proposal will be the recommended one for point data when thats finalized. Unfortunately, it will be
somewhat different from whats gone before. The CF: prefix is dropped until the namespace proposal can be completed.
So those feature types are now proposed to be:

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
    public static FeatureType getFeatureType(String s) {
      if (s.equalsIgnoreCase("point")) return FeatureType.point;
      if (s.equalsIgnoreCase("timeSeries")) return FeatureType.timeSeries;
      if (s.equalsIgnoreCase("stationTimeSeries")) return FeatureType.timeSeries;
      if (s.equalsIgnoreCase("station")) return FeatureType.timeSeries;
      if (s.equalsIgnoreCase("profile")) return FeatureType.profile;
      if (s.equalsIgnoreCase("trajectory")) return FeatureType.trajectory;
      if (s.equalsIgnoreCase("timeSeriesProfile")) return FeatureType.timeSeriesProfile;
      if (s.equalsIgnoreCase("stationProfile")) return FeatureType.timeSeriesProfile;
      if (s.equalsIgnoreCase("stationProfileTimeSeries")) return FeatureType.timeSeriesProfile;
      if (s.equalsIgnoreCase("trajectoryProfile")) return FeatureType.trajectoryProfile;
      if (s.equalsIgnoreCase("section")) return FeatureType.trajectoryProfile;
      return null;
    }

    public static FeatureType getFeatureTypeFromGlobalAttribute(NetcdfFile ds) {
      String ftypeS = ds.findAttValueIgnoreCase(null, CF.FEATURE_TYPE, null);
      if (ftypeS == null)
        ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt2, null);
      if (ftypeS == null)
        ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt3, null);

      if (ftypeS == null)
        return null;

      return CF.FeatureType.getFeatureType(ftypeS);
    }

  }

  // http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#appendix-cell-methods
  public enum CellMethods {
    point, sum, maximum, median, mid_range, minimum, mean, mode, standard_deviation, variance;

    // deprecated
    public static CellMethods convertGribCodeTable4_10(int code) {
      switch (code) {
        case 0:
          return CellMethods.mean; // "Average";
        case 1:
          return CellMethods.sum; // "Accumulation";
        case 2:
          return CellMethods.maximum; // "Maximum";
        case 3:
          return CellMethods.minimum; // "Minimum";
        //case 4: return	"Difference"; // (Value at the end of time range minus value at the beginning)";
        //case 5: return	"RootMeanSquare";
        case 6:
          return CellMethods.standard_deviation; // "StandardDeviation";
        case 7:
          return CellMethods.variance; // "Covariance"; // (Temporal variance)";
        //case 8: return	"Difference"; // (Value at the start of time range minus value at the end)";
        //case 9: return	"Ratio";
        default:
          return null;
      }
    }
  }

}
