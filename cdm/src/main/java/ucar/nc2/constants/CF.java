/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.constants;

import ucar.nc2.NetcdfFile;

/**
 * Constants used in CF Conventions.
 *
 * Updated for CF 1.8
 *
 * @author caron
 * @author wchen@usgs.gov
 */
public class CF {

  // general attribute names
  public static final String ACTUAL_RANGE = "actual_range";
  public static final String ANCILLARY_VARIABLES = "ancillary_variables";
  public static final String ADD_OFFSET = "add_offset";
  public static final String AXIS = "axis";
  public static final String BOUNDS = "bounds";
  public static final String CALENDAR = "calendar";
  public static final String CELL_METHODS = "cell_methods";
  public static final String CELL_MEASURES = "cell_measures";
  public static final String CF_ROLE = "cf_role";
  public static final String CLIMATOLOGY = "climatology";
  public static final String CONVENTIONS = "Conventions";
  public static final String COMPRESS = "compress";
  public static final String COORDINATES = "coordinates";
  public static final String EXTERNAL_VARIABLES = "external_variables";
  public static final String FEATURE_TYPE = "featureType";
  public static final String FORMULA_TERMS = "formula_terms";
  public static final String LONG_NAME = "long_name";
  public static final String POSITIVE = "positive";
  public static final String SCALE_FACTOR = "scale_factor";
  public static final String STANDARD_NAME = "standard_name";
  public static final String UNITS = "units";
  
  // data types
  public static final String CHAR = "char";
  public static final String BYTE = "byte";
  public static final String SHORT = "short";
  public static final String INT = "int";
  public static final String FLOAT = "float";
  public static final String REAL = "real";
  public static final String DOUBLE = "double";

  // data type range attributes
  public static final String VALID_MIN = "valid_min";
  public static final String VALID_MAX = "valid_max";
  public static final String VALID_RANGE = "valid_range";
  public static final String _FILLVALUE = "_FillValue";
  public static final String MISSING_VALUE = "missing_value";

  // flag related attributes
  public static final String FLAG_MASKS = "flag_masks"; 
  public static final String FLAG_VALUES = "flag_values";
  public static final String FLAG_MEANINGS = "flag_meanings";
  
  // general file attributes, though not required for compatibility with COOARDS
  public static final String TITLE = "title";
  public static final String HISTORY = "history";
  public static final String INSTITUTION = "institution";
  public static final String SOURCE = "source";
  public static final String COMMENT = "comment";
  public static final String REFERENCES = "references";

  // positive values
  public static final String POSITIVE_UP = "up";
  public static final String POSITIVE_DOWN = "down";

  // geometry attributes, introduced in CF 1.8
  public static final String GEOMETRY = "geometry";
  public static final String GEOMETRY_TYPE = "geometry_type";
  public static final String PART_NODE_COUNT = "part_node_count";
  public static final String NODES = "nodes";
  public static final String NODE_COUNT = "node_count";
  public static final String NODE_COORDINATES = "node_coordinates";
  public static final String INTERIOR_RING = "interior_ring";
  public static final String POINT = "point";
  public static final String LINE = "line";
  public static final String POLYGON = "polygon";
  
  // calendar leap specification
  public static final String LEAP_MONTH = "leap_month";
  public static final String LEAP_YEAR = "leap_year";
  
  // grid mapping names
  public static final String ALBERS_CONICAL_EQUAL_AREA = "albers_conical_equal_area";
  public static final String AZIMUTHAL_EQUIDISTANT = "azimuthal_equidistant";
  public static final String GEOSTATIONARY = "geostationary";
  public static final String LAMBERT_AZIMUTHAL_EQUAL_AREA = "lambert_azimuthal_equal_area";
  public static final String LAMBERT_CONFORMAL_CONIC = "lambert_conformal_conic";
  public static final String LAMBERT_CYLINDRICAL_EQUAL_AREA = "lambert_cylindrical_equal_area";
  public static final String LATITUDE_LONGITUDE = "latitude_longitude";
  public static final String MERCATOR = "mercator";
  public static final String ORTHOGRAPHIC = "orthographic";
  public static final String POLAR_STEREOGRAPHIC = "polar_stereographic";
  public static final String ROTATED_LATITUDE_LONGITUDE = "rotated_latitude_longitude";
  public static final String STEREOGRAPHIC = "stereographic";
  public static final String SINUSOIDAL = "sinusoidal";       // NOY
  public static final String TRANSVERSE_MERCATOR = "transverse_mercator";
  public static final String VERTICAL_PERSPECTIVE = "vertical_perspective";

  // for grid_mappings
  public static final String EARTH_RADIUS = "earth_radius";
  public static final String FALSE_EASTING = "false_easting";
  public static final String FALSE_NORTHING = "false_northing";
  public static final String GRID_LATITUDE = "grid_latitude";
  public static final String GRID_LONGITUDE = "grid_longitude";
  public static final String GRID_MAPPING = "grid_mapping";
  public static final String FIXED_ANGLE_AXIS = "fixed_angle_axis";  //  geostationary
  public static final String GRID_MAPPING_NAME = "grid_mapping_name";
  public static final String GRID_NORTH_POLE_LATITUDE = "grid_north_pole_latitude";   // rotated grid
  public static final String GRID_NORTH_POLE_LONGITUDE = "grid_north_pole_longitude"; // rotated grid
  public static final String INVERSE_FLATTENING = "inverse_flattening";
  public static final String LATITUDE_OF_PROJECTION_ORIGIN = "latitude_of_projection_origin";
  public static final String LONGITUDE_OF_PROJECTION_ORIGIN = "longitude_of_projection_origin";
  public static final String LATITUDE_OF_PRIME_MERIDIAN = "latitude_of_prime_meridian";
  public static final String LONGITUDE_OF_PRIME_MERIDIAN = "longitude_of_prime_meridian";
  public static final String LONGITUDE_OF_CENTRAL_MERIDIAN = "longitude_of_central_meridian";
  public static final String NORTH_POLE_GRID_LONGITUDE = "north_pole_grid_longitude";  // rotated grid synonym for GRID_NORTH_POLE_LONGITUDE
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
  public static final String atmosphere_sigma_coordinate = "atmosphere_sigma_coordinate";
  public static final String atmosphere_hybrid_sigma_pressure_coordinate = "atmosphere_hybrid_sigma_pressure_coordinate";
  public static final String atmosphere_hybrid_height_coordinate = "atmosphere_hybrid_height_coordinate";
  public static final String atmosphere_sleve_coordinate = "atmosphere_sleve_coordinate";
  public static final String ocean_sigma_coordinate = "ocean_sigma_coordinate";
  public static final String ocean_s_coordinate = "ocean_s_coordinate";
  public static final String ocean_sigma_z_coordinate = "ocean_sigma_z_coordinate";
  public static final String ocean_double_sigma_coordinate = "ocean_double_sigma_coordinate";

  public static final String formula_terms = "formula_terms";

  // standard_names
  public static final String ENSEMBLE = "realization";
  public static final String LATITUDE = "latitude";
  public static final String LONGITUDE = "longitude";
  public static final String TIME = "time";                               // valid; time, obs time
  public static final String TIME_REFERENCE = "forecast_reference_time";  // the "data time", the time of the analysis from which the forecast was made.
  public static final String TIME_OFFSET = "forecast_period";  // Forecast period is the time interval between the forecast reference time and the validity time. A period is an interval of time,

  public static final String PROJECTION_X_COORDINATE = "projection_x_coordinate";
  public static final String PROJECTION_Y_COORDINATE = "projection_y_coordinate";

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

  // not official - used in CFwriter for attribute name
  public static final String DSG_REPRESENTATION = "DSG_representation";

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
    point, timeSeries, profile, trajectory, timeSeriesProfile, trajectoryProfile, line, polygon,;

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
        case TRAJECTORY_PROFILE:
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
          return ucar.nc2.constants.FeatureType.TRAJECTORY_PROFILE;
      }
      return null;
    }

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
  
      // class not interface, per Bloch edition 2 item 19
  private CF() {} // disable instantiation

}
