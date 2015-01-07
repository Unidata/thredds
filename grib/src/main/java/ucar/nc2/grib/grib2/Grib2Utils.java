/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib2;

import ucar.nc2.grib.GribLevelType;
import ucar.nc2.grib.VertCoord;
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

  // LatLon Orthonogonal
  static public boolean isLatLon2D(int gridTemplate, int center) {
    return ((center == 7) && (gridTemplate == 204));
  }

  public enum LatLonCoordType {U, V, P}

  static public LatLonCoordType getLatLon2DcoordType(String desc) {
    LatLonCoordType type;
    if (desc.contains("u-component")) type = LatLonCoordType.U;
    else if (desc.contains("v-component")) type = LatLonCoordType.V;
    else if (desc.contains("Latitude of") || desc.contains("Longitude of")) type = null;
    else type = LatLonCoordType.P;
    return type;
  }

  static public String cleanupHeader(byte[] raw) {
    String result = StringUtil2.cleanup(raw);
    int pos = result.indexOf("data");
    if (pos > 0) result = result.substring(pos);
    return result;
  }
}
