/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1.tables;

/**
 * Standard WMO level types
 *
 * @author deprecated - moved to resources/grib1/wmoTable3.xml
 * @author caron
 * @since 1/13/12
 */
public class Grib1LevelTypeTable {

  /**
   * type of vertical coordinate: Description or short Name
   * taken from WMO Manual on codes -  GRIB1 Code table 3
   *
   * @param levelType table 3 code
   * @return level description
   */
  static public String getLevelDescription(int levelType) {

    switch (levelType) {

      case 0:
        return "Reserved";

      case 1:
        return "Ground or water surface";

      case 2:
        return "Cloud base level";

      case 3:
        return "Level of cloud tops";

      case 4:
        return "Level of 0deg C isotherm";

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

      case 20:
        return "Isothermal level";

      case 100:
        return "Isobaric surface";

      case 101:
        return "Layer between 2 isobaric levels";

      case 102:
        return "Mean sea level";

      case 103:
        return "Altitude above mean sea level";

      case 104:
        return "Layer between 2 altitudes above msl";

      case 105:
        return "Specified height level above ground";

      case 106:
        return "Layer between 2 specified height level above ground";

      case 107:
        return "Sigma level";

      case 108:
        return "Layer between 2 sigma levels";

      case 109:
        return "Hybrid level";

      case 110:
        return "Layer between 2 hybrid levels";

      case 111:
        return "Depth below land surface";

      case 112:
        return "Layer between 2 depths below land surface";

      case 113:
        return "Isentropic theta level";

      case 114:
        return "Layer between 2 isentropic levels";

      case 115:
        return "Level at specified pressure difference from ground to level";

      case 116:
        return "Layer between 2 level at pressure difference from ground to level";

      case 117:
        return "Potential vorticity surface";

      case 119:
        return "Eta level";

      case 120:
        return "Layer between 2 Eta levels";

      case 121:
        return "Layer between 2 isobaric levels";

      case 125:
        return "Specified height level above ground";

      case 128:
        return "Layer between 2 sigma levels (hi precision)";

      case 141:
        return "Layer between 2 isobaric surfaces (mixed precision)";

      case 160:
        return "Depth below sea level";

      case 200:
        return "Entire atmosphere";

      case 201:
        return "Entire ocean";

      case 210:
        return "Isobaric surface (high precision)";

      case 255:
        return "Missing";

      default:
        return "Unknown Level code=" + levelType;
    }

  }

  /**
   * short name of level.
   *
   * @param levelType table 3 code
   * @return short name of level, for use in variable names and dimensions
   */
  static public String getNameShort(int levelType) {

    switch (levelType) {

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

      case 20:
        return "isotherm";

      case 100:
        return "isobaric";

      case 101:
        return "layer_between_two_isobaric";

      case 102:
        return "msl";

      case 103:
        return "altitude_above_msl";

      case 104:
        return "layer_between_two_altitudes_above_msl";

      case 105:
        return "height_above_ground";

      case 106:
        return "layer_between_two_heights_above_ground";

      case 107:
        return "sigma";

      case 108:
        return "layer_between_two_sigmas";

      case 109:
        return "hybrid";

      case 110:
        return "layer_between_two_hybrids";

      case 111:
        return "depth_below_surface";

      case 112:
        return "layer_between_two_depths_below_surface";

      case 113:
        return "isentrope";

      case 114:
        return "layer_between_two_isentrope";

      case 115:
        return "pressure_difference";

      case 116:
        return "layer_between_two_pressure_difference_from_ground";

      case 117:
        return "potential_vorticity_surface";

      case 119:
        return "eta";

      case 120:
        return "layer_between_two_eta";

      case 121:
        return "layer_between_two_isobaric_surfaces";

      case 125:
        return "height_above_ground";

      case 128:
        return "layer_between_two_sigmas_hi";

      case 141:
        return "layer_between_two_isobaric_surfaces";

      case 160:
        return "depth_below_sea";

      case 200:
        return "entire_atmosphere";

      case 201:
        return "entire_ocean";

      case 210:
        return "isobaric_surface";

      default:
        return "unknown_" + levelType;
    }
  }

  /**
   * units of vertical coordinate.
   * taken from WMO Manual on codes -  GRIB1 Code table 3
   * note that some units get converted in Grib1ParamLevel, so these dont always match the manual.
   *
   * @param levelType table 3 code
   * @return unit as String, or empty string for no units, or null for 2D surface
   */
  static public String getUnits(int levelType) {

    switch (levelType) {
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 102:
      case 200:
      case 201:
        return null;

      case 20:
      case 113:
      case 114:
        return "K";

      case 100:
      case 101:
      case 115:
      case 116:
      case 121:
      case 141:
        return "hPa";

      case 103:
      case 104:
      case 105:
      case 106:
      case 160:
        return "m";

      case 107:
      case 108:
      case 128:
        return "sigma";

      case 111:
      case 112:
      case 125:
        return "cm";

      case 117:
        return "1.0E-9 K.kg-1.m2.s-1";    // 10–9 K m2 kg–1 s–1

      case 210:
        return "Pa";

      default:
        return "";  // LOOK assume to be UNITLESS  (?)
    }

  }

  // datums are only used for 3D variables
  static public String getDatum(int levelType) {

    switch (levelType) {

      case 103:
        return "mean sea level";

      case 105:
      case 106:
      case 111:
      case 112:
      case 115:
      case 116:
      case 125:
        return "ground";

      case 160:
        return "sea level";

      default:
        return null;
    }
  }

  /**
   * positive up is a CF thing
   * it means "increasing values of this coordinate are up from the ground".
   *
   * @param levelType table 3 code
   * @return true if increasing values go up
   */
  public static boolean isPositiveUp(int levelType) {

    if (levelType == 103) return true;
    if (levelType == 104) return true;
    if (levelType == 105) return true;
    if (levelType == 106) return true;
    if (levelType == 125) return true;
    return false;
  }

  /**
   * Check to see if this grid is a layer variable
   *
   * @param levelType table 3 code
   * @return true if a layer
   */
  public static boolean isLayer(int levelType) {
    if (levelType == 101) return true;
    if (levelType == 104) return true;
    if (levelType == 106) return true;
    if (levelType == 108) return true;
    if (levelType == 110) return true;
    if (levelType == 112) return true;
    if (levelType == 114) return true;
    if (levelType == 116) return true;
    if (levelType == 120) return true;
    if (levelType == 121) return true;
    if (levelType == 128) return true;
    if (levelType == 141) return true;
    return false;
  }

}
