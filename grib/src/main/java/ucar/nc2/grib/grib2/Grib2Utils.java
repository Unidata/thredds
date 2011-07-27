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

import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.table.WmoCodeTable;
import ucar.nc2.time.CalendarDuration;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.util.StringUtil;

import java.util.Formatter;

/**
 * static utilities
 *
 * @author caron
 * @since 3/29/11
 */
public class Grib2Utils {

  static public String clean(String s) {
    StringBuilder sb = new StringBuilder(s);
    StringUtil.replace(sb, "/. ", "-p_");
    StringUtil.remove(sb, "(),;");
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

  static public CalendarDuration getCalendarDuration(int timeUnit) {

    switch (timeUnit) { // code table 4.4
      case 0:
        return new CalendarDuration(1, CalendarPeriod.Minute);
      case 1:
        return new CalendarDuration(1, CalendarPeriod.Hour);
      case 2:
        return new CalendarDuration(1, CalendarPeriod.Day);
      case 3:
        return new CalendarDuration(1, CalendarPeriod.Month);
      case 4:
        return new CalendarDuration(1, CalendarPeriod.Year);
      case 5:
        return new CalendarDuration(10, CalendarPeriod.Year);
      case 6:
        return new CalendarDuration(30, CalendarPeriod.Year);
      case 7:
        return new CalendarDuration(100, CalendarPeriod.Year);
      case 10:
        return new CalendarDuration(3, CalendarPeriod.Hour);
      case 11:
        return new CalendarDuration(6, CalendarPeriod.Hour);
      case 12:
        return new CalendarDuration(12, CalendarPeriod.Hour);
      case 13:
        return new CalendarDuration(1, CalendarPeriod.Second);
      default:
        throw new UnsupportedOperationException("Unknown time unit = "+timeUnit);
    }
  }

  /**
   * Unit of vertical coordinate.
   * from Grib2 code table 4.5.
   * LOOK need scientific vetting, need center specific override- move to GribTables
   *
   * @param code code from table 4.5
   * @return level unit, default is empty unit string
   */
  static public VertCoord.VertUnit getLevelUnit(int code) {
    //     public VertUnit(int code, String units, String datum, boolean isPositiveUp)

    switch (code) {

      case 11:
      case 12:
        return new VertCoord.VertUnit(code, "m", null, true);

      case 20:
        return new VertCoord.VertUnit(code, "K", null, false);

      case 100:
        return new VertCoord.VertUnit(code, "Pa", null, false);

      case 102:
        return new VertCoord.VertUnit(code, "m", "mean sea level", true);

      case 103:
        return new VertCoord.VertUnit(code, "m", "ground", true);

      case 104:
      case 105:
        return new VertCoord.VertUnit(code, "sigma", null, false); // positive?

      case 106:
        return new VertCoord.VertUnit(code, "m", "land surface", false);

      case 107:
        return new VertCoord.VertUnit(code, "K", null, true); // positive?

      case 108:
        return new VertCoord.VertUnit(code, "Pa", "ground", true);

      case 109:
        return new VertCoord.VertUnit(code, "K m2 kg-1 s-1", null, true);// positive?

      case 117:
        return new VertCoord.VertUnit(code, "m", null, true);

      case 119:
        return new VertCoord.VertUnit(code, "Pa", null, false); // ??

      case 160:
        return new VertCoord.VertUnit(code, "m", "sea level", false);

      // LOOK NCEP specific
      case 235:
        return new VertCoord.VertUnit(code, "0.1 C", null, true);

      case 237:
        return new VertCoord.VertUnit(code, "m", null, true);

      case 238:
        return new VertCoord.VertUnit(code, "m", null, true);

      default:
        return new VertCoord.VertUnit(code, "", null, true);
    }
  }

  static public boolean isLatLon(int gridTemplate, int center) {
    return ((gridTemplate < 4) || ((gridTemplate >= 40) && (gridTemplate < 44)));
  }

  static public boolean isLatLon2D(int gridTemplate, int center) {
    return ((center == 7) && (gridTemplate == 204));
  }

  public enum LatLonCoordType {U, V, P}
  static public LatLonCoordType getLatLon2DcoordType(String desc) {
    LatLonCoordType type = null;
    if (desc.contains("u-component")) type = LatLonCoordType.U;
    else if (desc.contains("v-component")) type = LatLonCoordType.V;
    else if (desc.contains("Latitude of") || desc.contains("Longitude of")) type = null;
    else type = LatLonCoordType.P;
    return type;
  }

  /**
   * Check to see if this grid is a layer variable
   *
   * @param gr record to check
   * @return true if a layer
   */
  static public boolean isLayer(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    if (pds.getLevelType2() == 255 || pds.getLevelType2() == 0)
      return false;
    return true;
  }

}
