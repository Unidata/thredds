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

package ucar.nc2.grib.grib2;

import ucar.nc2.constants.AxisType;
import ucar.nc2.grib.grib2.table.WmoCodeTable;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.util.StringUtil2;

/**
 * static utilities for Grib-2
 *
 * @author caron
 * @since 3/29/11
 */
public class Grib2Utils {

  static public String clean(String s) {
    StringBuilder sb = new StringBuilder(s);
    StringUtil2.replace(sb, "/. ", "-p_");
    StringUtil2.remove(sb, "(),;");
    char c = sb.charAt(0);
    if (Character.isLetter(c)) {
      if (Character.isLowerCase(c))
        sb.setCharAt(0, Character.toUpperCase(c));
    } else {
      sb.insert(0, 'N');
    }

    return sb.toString().trim();
  }

  static public String cleanupHeader(byte[] raw) {
    String result = StringUtil2.cleanup(raw);
    int pos = result.indexOf("data");
    if (pos > 0) result = result.substring(pos);
    return result;
  }

  static public String getVariableName(Grib2Record gr) {
    String s =  WmoCodeTable.getParameterName(gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
    if (s == null)
      s = "U"+ gr.getDiscipline()+"-"+gr.getPDS().getParameterCategory()+"-" + gr.getPDS().getParameterNumber();
    return s;
  }

  static public CalendarPeriod getCalendarPeriod(int timeUnit) {

    switch (timeUnit) { // code table 4.4
      case 0:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Minute);
      case 1:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Hour);
      case 2:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Day);
      case 3:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Month);
      case 4:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Year);
      case 5:
        return CalendarPeriod.of(10, CalendarPeriod.Field.Year);
      case 6:
        return CalendarPeriod.of(30, CalendarPeriod.Field.Year);
      case 7:
        return CalendarPeriod.of(100, CalendarPeriod.Field.Year);
      case 10:
        return CalendarPeriod.of(3, CalendarPeriod.Field.Hour);
      case 11:
        return CalendarPeriod.of(6, CalendarPeriod.Field.Hour);
      case 12:
        return CalendarPeriod.of(12, CalendarPeriod.Field.Hour);
      case 13:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Second);
      default:
        return null;
        // throw new UnsupportedOperationException("Unknown time unit = "+timeUnit); // LOOK cant look at record with exception here
    }
  }

  /**
   * Check to see if this pds is a layer variable
   * @param pds record to check
   * @return true if a layer
   */
  static public boolean isLayer(Grib2Pds pds) {
    if (pds.getLevelType2() == 255 || pds.getLevelType2() == 0)
      return false;
    return true;
  }

  static public boolean isLatLon(int gridTemplate, int center) {
    return ((gridTemplate < 4) || ((gridTemplate >= 40) && (gridTemplate < 44)));
  }

  //////////////////////////////////////////////////////////////////////////////////
  // pretty much lame stuff
  // possibly move to Customizer

  // check if grid template is "Curvilinear Orthogonal", (NCEP 204) methods below only used when thats true
  static public boolean isCurvilinearOrthogonal(int gridTemplate, int center) {
    return ((center == 7) && (gridTemplate == 204));
  }

  // isLatLon2D is true, check parameter to see if its a 2D lat/lon coordinate
  static public LatLon2DCoord getLatLon2DcoordType(int discipline, int category, int parameter) {
    if ((discipline != 0) || (category != 2) || (parameter < 198 || parameter > 203)) return null;
    switch (parameter) {
      case 198:
        return LatLon2DCoord.U_Latitude;
      case 199:
         return LatLon2DCoord.U_Longitude;
      case 200:
        return LatLon2DCoord.V_Latitude;
      case 201:
        return LatLon2DCoord.V_Longitude;
      case 202:
       return LatLon2DCoord.P_Latitude;
      case 203:
       return LatLon2DCoord.P_Longitude;
    }
    return null;
  }

  public enum LatLonCoordType {U, V, P }
  public enum LatLon2DCoord {
    U_Latitude, U_Longitude, V_Latitude, V_Longitude, P_Latitude, P_Longitude;

    public AxisType getAxisType() {
      return this.name().contains("Latitude") ? AxisType.Lat : AxisType.Lon;
    }
  }


  /**
   * This looks for snippets in the variable name/desc as to whether it wants U, V, or P 2D coordinates
   * @param desc variable name/desc
   * @return  U, V, or P for normal variables, null for the coordinates themselves
   */
  static public LatLonCoordType getLatLon2DcoordType(String desc) {
    LatLonCoordType type;
    if (desc.contains("u-component")) type = LatLonCoordType.U;
    else if (desc.contains("v-component")) type = LatLonCoordType.V;
    else if (desc.contains("Latitude of") || desc.contains("Longitude of")) type = null;
    else type = LatLonCoordType.P;
    return type;
  }

}
