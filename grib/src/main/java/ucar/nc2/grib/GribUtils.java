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

package ucar.nc2.grib;

import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.util.StringUtil2;

/**
 * General Utilities used by GRIB code.
 *
 * @author caron
 * @since 11/16/11
 */
public class GribUtils {
  public static final String CENTER = "Originating_or_generating_Center";
  public static final String SUBCENTER = "Originating_or_generating_Subcenter";
  public static final String GEN_PROCESS = "Generating_process_or_model";
  public static final String TABLE_VERSION = "GRIB_table_version";

  /**
   * Convert a time unit to a CalendarPeriod
   * GRIB1 and GRIB2 are the same (!)
   *
   * @param timeUnit (GRIB1 table 4) (GRIB2   Code table 4.4 : Indicator of unit of time range)
   * @return equivalent CalendarPeriod
   */
  static public CalendarPeriod getCalendarPeriod(int timeUnit) {
    // LOOK - some way to intern these ? put is hash table
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
        return CalendarPeriod.of(15, CalendarPeriod.Field.Minute);
      case 14:
        return CalendarPeriod.of(30, CalendarPeriod.Field.Minute);
      default:
        throw new UnsupportedOperationException("Unknown time unit = " + timeUnit);
    }
  }

  static public String cleanupUnits(String unit) {
    if (unit == null) return null;
    if (unit.equalsIgnoreCase("-")) unit = "";
    else {
      if (unit.startsWith("/")) unit = "1" + unit;
      unit = unit.trim();
      unit = StringUtil2.remove(unit, "**");
      StringBuilder sb = new StringBuilder(unit);
      StringUtil2.remove(sb, "^[]");
      StringUtil2.substitute(sb, " / ", "/");
      StringUtil2.replace(sb, ' ', ".");
      StringUtil2.replace(sb, '*', ".");
      unit = sb.toString();
    }
    return unit;
  }

  static public String cleanupDescription(String desc) {
    if (desc == null) return null;
    int pos = desc.indexOf("(see");
    if (pos < 0) pos = desc.indexOf("(See");
    if (pos > 0) desc = desc.substring(0, pos);

    StringBuilder sb = new StringBuilder(desc.trim());
    StringUtil2.remove(sb, ".;,=[]()/");
    return sb.toString().trim();
  }

  static public String makeNameFromDescription(String desc) {
    if (desc == null) return null;
    int pos = desc.indexOf("(see");
    if (pos < 0) pos = desc.indexOf("(See");
    if (pos > 0) desc = desc.substring(0, pos);

    StringBuilder sb = new StringBuilder(desc.trim());
    StringUtil2.replace(sb, "/. ", "-p_");
    StringUtil2.remove(sb, ";,=[]()");
    return sb.toString();
  }
}
