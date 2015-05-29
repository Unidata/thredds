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
  String AXIS = "axis";
  String BOUNDS = "bounds";
  String CALENDAR = "calendar";
  String CELL_METHODS = "cell_methods";
  String CF_ROLE = "cf_role";
  String COORDINATES = "coordinates";
  String FEATURE_TYPE = "featureType";
  String POSITIVE = "positive";
  String STANDARD_NAME = "standard_name";
  
  String FORMULA_TERMS = "formula_terms";

  // positive values
  String POSITIVE_UP = "up";
  String POSITIVE_DOWN = "down";

  // grid mapping names
  String ALBERS_CONICAL_EQUAL_AREA = "albers_conical_equal_area";
  String AZIMUTHAL_EQUIDISTANT = "azimuthal_equidistant";
  String GEOSTATIONARY = "geostationary";
  String LAMBERT_AZIMUTHAL_EQUAL_AREA = "lambert_azimuthal_equal_area";
  String LAMBERT_CONFORMAL_CONIC = "lambert_conformal_conic";
  String LAMBERT_CYLINDRICAL_EQUAL_AREA = "lambert_cylindrical_equal_area";
  String LATITUDE_LONGITUDE = "latitude_longitude";
  String MERCATOR = "mercator";
  String ORTHOGRAPHIC = "orthographic";
  String POLAR_STEREOGRAPHIC = "polar_stereographic";
  String ROTATED_LATITUDE_LONGITUDE = "rotated_latitude_longitude";
  String STEREOGRAPHIC = "stereographic";
  String SINUSOIDAL = "sinusoidal";       // NOY
  String TRANSVERSE_MERCATOR = "transverse_mercator";
  String VERTICAL_PERSPECTIVE = "vertical_perspective";

  // for grid_mappings
  String EARTH_RADIUS = "earth_radius";
  String FALSE_EASTING = "false_easting";
  String FALSE_NORTHING = "false_northing";
  String GRID_LATITUDE = "grid_latitude";
  String GRID_LONGITUDE = "grid_longitude";
  String GRID_MAPPING = "grid_mapping";
  String FIXED_ANGLE_AXIS = "fixed_angle_axis";  //  geostationary
  String GRID_MAPPING_NAME = "grid_mapping_name";
  String GRID_NORTH_POLE_LATITUDE = "grid_north_pole_latitude";   // rotated grid
  String GRID_NORTH_POLE_LONGITUDE = "grid_north_pole_longitude"; // rotated grid
  String INVERSE_FLATTENING = "inverse_flattening";
  String LATITUDE_OF_PROJECTION_ORIGIN = "latitude_of_projection_origin";
  String LONGITUDE_OF_PROJECTION_ORIGIN = "longitude_of_projection_origin";
  String LATITUDE_OF_PRIME_MERIDIAN = "latitude_of_prime_meridian";
  String LONGITUDE_OF_PRIME_MERIDIAN = "longitude_of_prime_meridian";
  String LONGITUDE_OF_CENTRAL_MERIDIAN = "longitude_of_central_meridian";
  String NORTH_POLE_GRID_LONGITUDE = "north_pole_grid_longitude";  // rotated grid synonym for GRID_NORTH_POLE_LONGITUDE
  String PERSPECTIVE_POINT_HEIGHT = "perspective_point_height";   // geostationary
  String SCALE_FACTOR_AT_CENTRAL_MERIDIAN = "scale_factor_at_central_meridian";
  String SCALE_FACTOR_AT_PROJECTION_ORIGIN = "scale_factor_at_projection_origin";
  String SEMI_MAJOR_AXIS = "semi_major_axis";
  String SEMI_MINOR_AXIS = "semi_minor_axis";
  String SWEEP_ANGLE_AXIS = "sweep_angle_axis"; // geostationary
  String STANDARD_PARALLEL = "standard_parallel";
  String STRAIGHT_VERTICAL_LONGITUDE_FROM_POLE = "straight_vertical_longitude_from_pole";

  // vertical coordinate
  String atmosphere_ln_pressure_coordinate = "atmosphere_ln_pressure_coordinate";
  String atmosphere_sigma_coordinate = "atmosphere_sigma_coordinate";
  String atmosphere_hybrid_sigma_pressure_coordinate = "atmosphere_hybrid_sigma_pressure_coordinate";
  String atmosphere_hybrid_height_coordinate = "atmosphere_hybrid_height_coordinate";
  String atmosphere_sleve_coordinate = "atmosphere_sleve_coordinate";
  String ocean_sigma_coordinate = "ocean_sigma_coordinate";
  String ocean_s_coordinate = "ocean_s_coordinate";
  String ocean_sigma_z_coordinate = "ocean_sigma_z_coordinate";
  String ocean_double_sigma_coordinate = "ocean_double_sigma_coordinate";

  String formula_terms = "formula_terms";

  // standard_names
  String TIME = "time";                               // valid; time, obs time
  String TIME_REFERENCE = "forecast_reference_time";  // the "data time", the time of the analysis from which the forecast was made.
  String TIME_OFFSET = "forecast_period";  // Forecast period is the time interval between the forecast reference time and the validity time. A period is an interval of time,

  String PROJECTION_X_COORDINATE = "projection_x_coordinate";
  String PROJECTION_Y_COORDINATE = "projection_y_coordinate";

  // cf_role
  String PROFILE_ID = "profile_id";
  String TIMESERIES_ID = "timeseries_id"; // alias STATION_ID
  String TRAJECTORY_ID = "trajectory_id";

  // DSG
  String SAMPLE_DIMENSION = "sample_dimension";
  String INSTANCE_DIMENSION = "instance_dimension";

  String PLATFORM_NAME = "platform_name"; // instead of STATION_DESC
  String SURFACE_ALTITUDE = "surface_altitude"; // alias STATION_ALTITUDE
  String PLATFORM_ID = "platform_id";  // alias STATION_WMOID

  ///////////////////////////////////////////////////////////////////
  // DSG proposed - not adopted; here for backwards compatibility
  String RAGGED_ROWSIZE = "CF:ragged_row_count";
  String RAGGED_PARENTINDEX = "CF:ragged_parent_index";

  // proposed standard_names
  String STATION_ID = "station_id";
  String STATION_DESC = "station_description";
  String STATION_ALTITUDE = "station_altitude";
  String STATION_WMOID = "station_WMO_id";

  String featureTypeAtt2 = "CF:featureType";
  String featureTypeAtt3 = "CF:feature_type"; // GRIB was using this form (!)
  ///////////////////////////////////////////////////////////////////////

  /**
   * Map from CF feature type names to our FeatureType enums.
   */
  enum FeatureType {
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
