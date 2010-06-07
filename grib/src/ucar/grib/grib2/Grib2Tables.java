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
/**
 * User: rkambic
 * Date: Jun 29, 2009
 * Time: 3:22:58 PM
 */

package ucar.grib.grib2;

import ucar.grid.GridTableLookup;

/**
 * Class contains most of the hard coded tables for Grib2. Tables extracted from PDS
 * and GDS sections.
 */
public class Grib2Tables {

  /**
   * Grid Definition Template
   *
   * @param gdtn Grid definition template number same as type of grid
   * @return GridName as a String
   */
  public static String codeTable3_1(int gdtn) {
    switch (gdtn) {  // code table 3.1

      case 0:
        return "Latitude_Longitude";

      case 1:
        return "Rotated_Latitude_Longitude";

      case 2:
        return "Stretched_Latitude_Longitude";

      case 3:
        return "Rotated_and_Stretched_Latitude_Longitude";

      case 10:
        return "Mercator";

      case 20:
        return "Polar_Stereographic";

      case 30:
        return "Lambert_Conformal";

      case 31:
        return "Albers_Equal_Area";

      case 40:
        return "Gaussian_Latitude_Longitude";

      case 41:
        return "Rotated_Gaussian_Latitude_Longitude";

      case 42:
        return "Stretched_Gaussian Latitude_Longitude";

      case 43:
        return "Rotated_and_Stretched_Gaussian_Latitude_Longitude";

      case 50:
        return "Spherical_Harmonic_Coefficients";

      case 51:
        return "Rotated_Spherical_Harmonic_Coefficients";

      case 52:
        return "Stretched_Spherical_Harmonic_Coefficients";

      case 53:
        return "Rotated_and_Stretched_Spherical_Harmonic_Coefficients";

      case 90:
        return "Space_View_Perspective_or_Orthographic";

      case 100:
        return "Triangular_Grid_Based_on_an_Icosahedron";

      case 110:
        return "Equatorial_Azimuthal_Equidistant";

      case 120:
        return "Azimuth_Range";

      case 204:
        return "Curvilinear_Orthogonal";

      case 1000:
        return "Cross_Section_Grid_with_Points_Equally_Spaced_on_the_Horizontal";

      case 1100:
        return "Hovmoller_Diagram_with_Points_Equally_Spaced_on_the_Horizontal";

      case 1200:
        return "Time_Section_Grid";

      case 32768:
        return "Rotated_Latitude_Longitude_Arakawa_Staggered_E_Grid";
      default:
        return "Unknown projection" + gdtn;
    }
  }                    // end getGridName

  /**
   * Gets the ProjectionType based on the Grid definition template number.
   * From code table 3.1
   * @param gridType Grid definition template number
   * @return ProjectionType
   */
  public static final int getProjectionType(int gridType) {
    switch (gridType) {
      case 1:
        return GridTableLookup.RotatedLatLon;

      case 10:
        return GridTableLookup.Mercator;

      case 20:
        return GridTableLookup.PolarStereographic;

      case 30:
        return GridTableLookup.LambertConformal;

      case 31:
        return GridTableLookup.AlbersEqualArea;

      case 40:
        return GridTableLookup.GaussianLatLon;

      case 90:
        return GridTableLookup.Orthographic;

      case 204:
        return GridTableLookup.Curvilinear;

      default:
        return -1;
    }
  }

  /**
   * Shape of the Earth
   *
   * @param shape as an int
   * @return shapeName as a String
   */
  static public String codeTable3_2(int shape) {
    switch (shape) {

      case 0:
        return "Earth spherical with radius = 6,367,470 m";

      case 1:
        return "Earth spherical with radius specified by producer in m";

      case 2:
        return "Earth oblate spheroid with major axis = 6,378,160 m and minor axis = 6,356,775 m";

      case 3:
        return "Earth oblate spheroid with axes specified by producer in m";

      case 4:
        return "Earth oblate spheroid with major axis = 6,378,137.0 m and minor axis = 6,356,752.314 m";

      case 5:
        return "Earth represent by WGS84";

      case 6:
        return "Earth spherical with radius of 6,371,229.0 m";

      case 7:
        return "Earth oblate spheroid with axes specified by producer in m";

      case 8:
        return "Earth spherical with radius of 6,371,200.0 m, represent by WGS84";

      default:
        return "Unknown Earth Shape";
    }
  }

  /*
   * Center name  Uses same table as Grib1
   *
   * @param center int
   * @return center name
   *
  public static String getCenter_idName(int center) {
    return Grib1Tables.getCenter_idName( center );
  }

  /*
   * SubCenter as String. NCEP has there own sub-center table, otherwise it's the
   * same as center. Uses same table as Grib1
   *
   * @param center_id center
   * @param subCenter subCenter
   * @return subCenter
   *
  public static final String getSubCenter_idName(int center_id, int subCenter) {
    return Grib1Tables.getSubCenter_idName( center_id, subCenter);
  } */

  /**
   * Product Definition Template
   * Code table 4.0.
   *
   * @param productDefinition productDefinition
   * @return ProductDefinitionName
   */
  static public String codeTable4_0(int productDefinition) {
    switch (productDefinition) {

      case 0:
        return "Analysis/forecast at horizontal level/layer at a point in time";

      case 1:
        return "Individual ensemble forecast at a point in time";

      case 2:
        return "Derived forecast on all ensemble members at a point in time";

      case 3:
        return "Derived forecasts on cluster of ensemble members over rectangular area at a point in time";

      case 4:
        return "Derived forecasts on cluster of ensemble members over circular area at a point in time";

      case 5:
        return "Probability forecasts at a horizontal level at a point in time";

      case 6:
        return "Percentile forecasts at a horizontal level at a point in time";

      case 7:
        return "Analysis or forecast error at a horizontal level at a point in time";

      case 8:
        return "Average, accumulation, extreme values or other statistically processed value at a horizontal level in a time interval";

      case 9:
        return "Probability forecasts at a horizontal level or in a horizontal layer in a time interval";

      case 10:
        return "Percentile forecasts at a horizontal level or in a horizontal layer in a time interval";

      case 11:
        return "Individual ensemble forecast in a time interval";

      case 12:
        return "Derived forecast on all ensemble members in a time interval";

      case 13:
        return "Derived forecasts on cluster of ensemble members over rectangular area  in a time interval";

      case 14:
        return "Derived forecasts on cluster of ensemble members over circular area  in a time interval";

      case 15:
        return "Average, accumulation, extreme values or other statistically-processed values over a spatial area at a "
        +"horizontal level or in a horizontal layer at a point in time.";

      case 20:
        return "Radar product";

      case 30:
        return "Satellite product";

      case 31:
        return "Satellite product";

      case 40:
        return "Analysis or forecast at a horizontal level or in a horizontal layer at a point in time for atmospheric "
        +"chemical constituents.";

      case 41:
        return "Individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer at "
        +"a point in time for atmospheric chemical constituents.";

      case 42:
        return "Average, accumulation, and/or extreme values or other statistically processed values at a horizontal "
        +"level or in a horizontal layer in a continuous or non-continuous time interval for atmospheric chemical constituents";

      case 43:
        return "Individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer, in a "
        +"continuous or non-continuous time interval for atmospheric chemical constituents.";

      case 254:
        return "CCITT IA5 character string";

      default:
        return "Unknown";
    }
  }

  /**
   * typeGenProcess name.
   * GRIB2 - TABLE 4.3
   * TYPE OF GENERATING PROCESS
   * Section 4, Octet 12
   * Created 05/11/05
   *
   * @param typeGenProcess _more_
   * @return GenProcessName
   */
  public static final String getTypeGenProcessName(String typeGenProcess) {
    int tgp;
    if (typeGenProcess.startsWith("4")) {
      tgp = 4;
    } else {
      tgp = Integer.parseInt(typeGenProcess);
    }
    return codeTable4_3(tgp);
  }

  public static final String codeTable4_3(int typeGenProcess) {

    switch (typeGenProcess) {

      case 0:
        return "Analysis";

      case 1:
        return "Initialization";

      case 2:
        return "Forecast";

      case 3:
        return "Bias Corrected Forecast";

      case 4:
        return "Ensemble Forecast";

      case 5:
        return "Probability Forecast";

      case 6:
        return "Forecast Error";

      case 7:
        return "Analysis Error";

      case 8:
        return "Observation";

      case 9:
        return "Climatological";

      case 10:
        return "Probability-Weighted Forecast";

      case 192:
        return "Forecast Confidence Indicator";

      case 193:
        return "Bias Corrected Ensemble Forecast";

      case 255:
        return "Missing";

      default:
        // ensemble will go from 4000 to 4399
        if (typeGenProcess > 3999 && typeGenProcess < 4400)
          return "Ensemble Forecast";

        return "Unknown";
    }
  }

  /**
   * return Time Range Unit Name from code table 4.4.
   *
   * @param code44 code for table 4.4
   * @return Time Range Unit Name from table 4.4
   */
  static public String codeTable4_4(int code44) {
    switch (code44) {

      case 0:
        return "Minute";

      case 1:
        return "Hour";

      case 2:
        return "Day";

      case 3:
        return "Month";

      case 4:
        return "Year";

      case 5:
        return "Decade";

      case 6:
        return "Normal";

      case 7:
        return "Century";

      case 10:
        return "3hours";
        //return "3hours";

      case 11:
        return "6hours";
        //return "hours";

      case 12:
        return "12hours";
        //return "hours";

      case 13:
        return "Second";

      default:
        //return "unknown";
        return "minutes"; // some grids don't set, so default is minutes, same default as old code
    }
  }


  /**
   * return a udunits time unit
   *
   * @param code44 code for table 4.4
   * @return udunits time unit
   */
  static public String getTimeUnitFromTable4_4(int code44) {
    switch (code44) {

      case 0:
        return "minute";

      case 1:
        return "hour";

      case 2:
        return "day";

      case 3:
        return "month";

      case 4:
        return "year";

      case 5:
        return "decade";

      case 6:
        return "normal";

      case 7:
        return "century";

      case 10:
        return "hour";

      case 11:
        return "hour";

      case 12:
        return "hour";

      case 13:
        return "second";

      default:
        return "minutes"; // some grids don't set, so default is minutes, same default as old code
    }
  }


  /**
   * type of vertical coordinate: Name
   * code table 4.5.
   *
   * @param id surface type
   * @return SurfaceName
   */
  static public String codeTable4_5(int id) {

    switch (id) {

      case 0:
        return "";

      case 1:
        return "Ground or water surface";

      case 2:
        return "Cloud base level";

      case 3:
        return "Level of cloud tops";

      case 4:
        return "Level of 0o C isotherm";

      case 5:
        return "Level of adiabatic condensation lifted from the surface";

      case 6:
        return "Maximum wind level";

      case 7:
        return "Tropopause";

      case 8:
        return "Nominal top of the atmosphere";

      case 9:
        return "Sea bottom";

      case 10:
        return "Entire Atmosphere";

      case 11:
        return "Cumulonimbus Base";

      case 12:
        return "Cumulonimbus Top";

      case 20:
        return "Isothermal level";

      case 100:
        return "Isobaric surface";

      case 101:
        return "Mean sea level";

      case 102:
        return "Specific altitude above mean sea level";

      case 103:
        return "Specified height level above ground";

      case 104:
        return "Sigma level";

      case 105:
        return "Hybrid level";

      case 106:
        return "Depth below land surface";

      case 107:
        return "Isentropic 'theta' level";

      case 108:
        return "Level at specified pressure difference from ground to level";

      case 109:
        return "Potential vorticity surface";

      case 111:
        return "Eta level";

      case 117:
        return "Mixed layer depth";

      case 160:
        return "Depth below sea level";

      case 200:
        return "Entire atmosphere layer";

      case 201:
        return "Entire ocean layer";

      case 204:
        return "Highest tropospheric freezing level";

      case 206:
        return "Grid scale cloud bottom level";

      case 207:
        return "Grid scale cloud top level";

      case 209:
        return "Boundary layer cloud bottom level";

      case 210:
        return "Boundary layer cloud top level";

      case 211:
        return "Boundary layer cloud layer";

      case 212:
        return "Low cloud bottom level";

      case 213:
        return "Low cloud top level";

      case 214:
        return "Low cloud layer";

      case 215:
        return "Cloud ceiling";

      case 220:
        return "Planetary Boundary Layer";

      case 221:
        return "Layer Between Two Hybrid Levels";

      case 222:
        return "Middle cloud bottom level";

      case 223:
        return "Middle cloud top level";

      case 224:
        return "Middle cloud layer";

      case 232:
        return "High cloud bottom level";

      case 233:
        return "High cloud top level";

      case 234:
        return "High cloud layer";

      case 235:
        return "Ocean isotherm level";

      case 236:
        return "Layer between two depths below ocean surface";

      case 237:
        return "Bottom of ocean mixed layer";

      case 238:
        return "Bottom of ocean isothermal layer";

      case 239:
        return "Layer Ocean Surface and 26C Ocean Isothermal Level";

      case 240:
        return "Ocean Mixed Layer";

      case 241:
        return "Ordered Sequence of Data";

      case 242:
        return "Convective cloud bottom level";

      case 243:
        return "Convective cloud top level";

      case 244:
        return "Convective cloud layer";

      case 245:
        return "Lowest level of the wet bulb zero";

      case 246:
        return "Maximum equivalent potential temperature level";

      case 247:
        return "Equilibrium level";

      case 248:
        return "Shallow convective cloud bottom level";

      case 249:
        return "Shallow convective cloud top level";

      case 251:
        return "Deep convective cloud bottom level";

      case 252:
        return "Deep convective cloud top level";

      case 253:
        return "Lowest bottom level of supercooled liquid water layer";

      case 254:
        return "Highest top level of supercooled liquid water layer";

      case 255:
        return "Missing";

      default:
        return "Unknown=" + id;
    }

  }  // end getTypeSurfaceName

  /**
   * type of vertical coordinate: short Name
   * derived from code table 4.5.
   *
   * @param id surfaceType
   * @return SurfaceNameShort
   */
  static public String getTypeSurfaceNameShort(int id) {

    switch (id) {

      case 0:
        return "";

      case 1:
        return "surface";

      case 2:
        return "cloud_base";

      case 3:
        return "cloud_tops";

      case 4:
        return "zeroDegC_isotherm";

      case 5:
        return "adiabatic_condensation_lifted";

      case 6:
        return "maximum_wind";

      case 7:
        return "tropopause";

      case 8:
        return "atmosphere_top";

      case 9:
        return "sea_bottom";

      case 10:
        return "entire_atmosphere";

      case 11:
        return "cumulonimbus_base";

      case 12:
        return "cumulonimbus_top";

      case 20:
        return "isotherm";

      case 100:
        return "pressure";

      case 101:
        return "msl";

      case 102:
        return "altitude_above_msl";

      case 103:
        return "height_above_ground";

      case 104:
        return "sigma";

      case 105:
        return "hybrid";

      case 106:
        return "depth_below_surface";

      case 107:
        return "isentrope";

      case 108:
        return "pressure_difference";

      case 109:
        return "potential_vorticity_surface";

      case 111:
        return "eta";

      case 117:
        return "mixed_layer_depth";

      case 160:
        return "depth_below_sea";

      case 200:
        return "entire_atmosphere";

      case 201:
        return "entire_ocean";

      case 204:
        return "highest_tropospheric_freezing";

      case 206:
        return "grid_scale_cloud_bottom";

      case 207:
        return "grid_scale_cloud_top";

      case 209:
        return "boundary_layer_cloud_bottom";

      case 210:
        return "boundary_layer_cloud_top";

      case 211:
        return "boundary_layer_cloud";

      case 212:
        return "low_cloud_bottom";

      case 213:
        return "low_cloud_top";

      case 214:
        return "low_cloud";

      case 215:
        return "cloud_ceiling";

      case 220:
        return "planetary_boundary";

      case 221:
        return "between_two_hybrids";

      case 222:
        return "middle_cloud_bottom";

      case 223:
        return "middle_cloud_top";

      case 224:
        return "middle_cloud";

      case 232:
        return "high_cloud_bottom";

      case 233:
        return "high_cloud_top";

      case 234:
        return "high_cloud";

      case 235:
        return "ocean_isotherm";

      case 236:
        return "layer_between_two_depths_below_ocean";

      case 237:
        return "bottom_of_ocean_mixed";

      case 238:
        return "bottom_of_ocean_isothermal";

      case 239:
        return "ocean_surface_and_26C_isothermal";

      case 240:
        return "ocean_mixed";

      case 241:
        return "ordered_sequence_of_data";

      case 242:
        return "convective_cloud_bottom";

      case 243:
        return "convective_cloud_top";

      case 244:
        return "convective_cloud";

      case 245:
        return "lowest_level_of_the_wet_bulb_zero";

      case 246:
        return "maximum_equivalent_potential_temperature";

      case 247:
        return "equilibrium";

      case 248:
        return "shallow_convective_cloud_bottom";

      case 249:
        return "shallow_convective_cloud_top";

      case 251:
        return "deep_convective_cloud_bottom";

      case 252:
        return "deep_convective_cloud_top";

      case 253:
        return "lowest_level_water_layer";

      case 254:
        return "highest_level_water_layer";

      case 255:
        return "missing";

      default:
        return "Unknown" + id;
    }

  }  // end getTypeSurfaceNameShort

  /**
   * type of vertical coordinate: Units.
   * code table 4.5.
   *
   * @param id units id as int
   * @return surfaceUnit
   */
  static public String getTypeSurfaceUnit(int id) {
    switch (id) {

      case 11:
      case 12:
        return "m";
      
      case 20:
        return "K";

      case 100:
        return "Pa";

      case 102:
        return "m";

      case 103:
        return "m";

      case 106:
        return "m";

      case 107:
        return "K";

      case 108:
        return "Pa";

      case 109:
        return "K m2 kg-1 s-1";

      case 117:
        return "m";

      case 160:
        return "m";

      case 235:
        return "C 0.1";

      case 237:
        return "m";

      case 238:
        return "m";

      default:
        return "";
    }
  }  // end getTypeSurfaceUnit

  /**
   * Gets a Ensemble type, Derived or Perturbed
   *
   * @param productType,    productType
   * @param type            of ensemble, derived or perturbed
   * @return Ensemble type as String
   */
  public static String getEnsembleType( int productType, int type) {
    switch (productType) {

      case 1:
      case 11:
      case 41:
      case 43: {
        // ensemble data

        //if (typeGenProcess == 4) {
          if (type == 0) {
            return "Cntrl_high";
          } else if (type == 1) {
            return "Cntrl_low";
          } else if (type == 2) {
            return "Perturb_neg";
          } else if (type == 3) {
            return "Perturb_pos";
          } else {
            return "unknownEnsemble";
          }

        //}

        //break;
      }
      case 2:
      case 3:
      case 4:
      case 12:
      case 13:
      case 14: {
        // Derived data
        //if (typeGenProcess == 4) {
          if (type == 0) {
            return "unweightedMean";
          } else if (type == 1) {
            return "weightedMean";
          } else if (type == 2) {
            return "stdDev";
          } else if (type == 3) {
            return "stdDevNor";
          } else if (type == 4) {
            return "spread";
          } else if (type == 5) {
            return "anomaly";
          } else if (type == 6) {
            return "unweightedMeanCluster";
          } else {
            return "unknownEnsemble";
          }
        //}
        //break;
      }

      default:
        return "";
    }
    //return "";
  }

  /*
   * typeEnsemble number.
   *  @deprecated
   * @param tgp typeGenProcess
   * @return typeEnsemble
   *
  public static final int getTypeEnsemble(String tgp) {
    if (tgp.contains("C_high")) {
      return 0;
    } else if (tgp.contains("C_low")) {
      return 1;
    } else if (tgp.contains("P_neg")) {
      return 2;
    } else if (tgp.contains("P_pos")) {
      return 3;
    }
    return -9999; //didn't know what to put as illegal
  } */

  // GDS static Tables

  /**
   * enum for componet_flag  for both Grib2 and Grib1
   *
   */
   public static enum VectorComponentFlag
     {  easterlyNortherlyRelative, gridRelative   };

  /**
   * Code Table 4.9:	Probability Type
   * @param code number
   * @return String name
   */
  static public String codeTable4_9(int code) {
    switch (code) {
     case 0: return	"Probability of event below lower limit";
     case 1: return	"Probability of event above upper limit";
     case 2: return	"Probability of event between lower and upper limits.  The range includes the lower limit but not the upper limit";
     case 3: return	"Probability of event above lower limit";
     case 4: return	"Probability of event below upper limit";
     default: return "Missing";
    }
  }

  /**
   *  Code Table 4.10: Type of statistical processing
   * @param code number
   * @return String name
   */
  static public String codeTable4_10(int code) {
    switch (code) {
      case 0: return	"Average";
      case 1: return	"Accumulation";
      case 2: return	"Maximum";
      case 3: return	"Minimum";
      case 4: return	"Difference (Value at the end of time range minus value at the beginning)";
      case 5: return	"Root mean square";
      case 6: return	"Standard deviation";
      case 7: return	"Covariance (Temporal variance)";
      case 8: return	"Difference (Value at the start of time range minus value at the end)";
      case 9: return	"Ratio";
     default: return "Missing";
    }
  }

  /**
   * Code Table 4.10: Type of statistical processing, short form
   * For embedding in a variable name
   * @param code number
   * @return String name, short form
   */
  static public String codeTable4_10short(int code) {
    switch (code) {
      case 0: return	"Average";
      case 1: return	"Accumulation";
      case 2: return	"Maximum";
      case 3: return	"Minimum";
      case 4: return	"Difference"; // (Value at the end of time range minus value at the beginning)";
      case 5: return	"RootMeanSquare";
      case 6: return	"StandardDeviation";
      case 7: return	"Covariance"; // (Temporal variance)";
      case 8: return	"Difference"; // (Value at the start of time range minus value at the end)";
      case 9: return	"Ratio";
     default: return null;
    }
  }

  /**
   *  Code Table 4.11: Type of time intervals
   * @param code number
   * @return String name
   */
  static public String codeTable4_11(int code) {
    switch (code) {
      case 0: return	"Reserved";
      case 1: return	"Successive times processed have same forecast time, start time of forecast is incremented";
      case 2: return	"Successive times processed have same start time of forecast, forecast time is incremented";
      case 3: return	"Successive times processed have start time of forecast incremented and forecast time decremented so that valid time remains constant";
      case 4: return	"Successive times processed have start time of forecast decremented and forecast time incremented so that valid time remains constant";
      case 5: return	"Floating subinterval of time between forecast time and end of overall time interval";
      default: return "Missing";
     }
   }


}