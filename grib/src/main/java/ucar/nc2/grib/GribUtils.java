/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import ucar.nc2.time.CalendarDate;
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
  public static CalendarPeriod getCalendarPeriod(int timeUnit) {
    // LOOK - some way to intern these ? put in hash table  ?
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

  public static CalendarDate getValidTime(CalendarDate refDate, int timeUnit, int offset) {
    CalendarPeriod period = GribUtils.getCalendarPeriod(timeUnit);
    return refDate.add(period.multiply(offset));
  }

  public static String cleanupUnits(String unit) {
    if (unit == null) return "";
    if (unit.equalsIgnoreCase("-")) unit = "";
    else {
      if (unit.startsWith("/")) unit = "1" + unit;
      unit = unit.trim();
      unit = StringUtil2.remove(unit, "**");
      StringBuilder sb = new StringBuilder(unit);
      StringUtil2.removeAll(sb, "^[]");
      StringUtil2.substitute(sb, " / ", "/");
      StringUtil2.replace(sb, ' ', ".");
      StringUtil2.replace(sb, '*', ".");
      unit = sb.toString();
    }
    return unit;
  }

  public static String cleanupDescription(String desc) {
    if (desc == null) return "";
    int pos = desc.indexOf("(see");
    if (pos < 0) pos = desc.indexOf("(See");
    if (pos > 0) desc = desc.substring(0, pos);

    StringBuilder sb = new StringBuilder(desc.trim());
    StringUtil2.replace(sb, '+', "and");
    StringUtil2.removeAll(sb, ".;,=[]()/");
    return sb.toString().trim();
  }

  public static String makeNameFromDescription(String desc) {
    if (desc == null) return "";
    int pos = desc.indexOf("(see");
    if (pos < 0) pos = desc.indexOf("(See");
    if (pos > 0) desc = desc.substring(0, pos);

    StringBuilder sb = new StringBuilder(desc.trim());
    StringUtil2.replace(sb, '+', "plus");
    StringUtil2.replace(sb, "/. ", "-p_");
    StringUtil2.removeAll(sb, ";,=[]()");
    return sb.toString();
  }

  /*
  scanMode
  Grib1
  Flag/Code table 8 – Scanning mode
  Bit No. Value Meaning
  1 0 Points scan in +i direction
    1 Points scan in –i direction
  2 0 Points scan in –j direction
    1 Points scan in +j direction
  3 0 Adjacent points in i direction are consecutive
    1 Adjacent points in j direction are consecutive
  Notes:
  (1) i direction: west to east along a parallel, or left to right along an X-axis.
  (2) j direction: south to north along a meridian, or bottom to top along a Y-axis.

  Grin2
    Flag table 3.4 – Scanning mode
       Bit No. Value Meaning
 128    1 0 Points of first row or column scan in the +i (+x) direction
          1 Points of first row or column scan in the –i (–x) direction
 64     2 0 Points of first row or column scan in the –j (–y) direction
          1 Points of first row or column scan in the +j (+y) direction
 32     3 0 Adjacent points in i (x) direction are consecutive
          1 Adjacent points in j (y) direction is consecutive
 16     4 0 All rows scan in the same direction
          1 Adjacent rows scans in the opposite direction
        5–8 Reserved
        Notes:
        (1) i direction: west to east along a parallel or left to right along an x-axis.
        (2) j direction: south to north along a meridian, or bottom to top along a y-axis.
        (3) If bit number 4 is set, the first row scan is as defined by previous flags.
   */

  /**
    * X Points scan in +/- direction. Grib 1 or 2.
    * Positive means west to east along a parallel, or left to right along an X-axis..
    * @param scanMode scanMode byte
    * @return true: x points scan in positive direction, false: x points scan in negetive direction
    */
  public static boolean scanModeXisPositive(int scanMode) {
    return !GribNumbers.testGribBitIsSet(scanMode, 1);
  }

  /**
    * Y Points scan in +/- direction. Grib 1 or 2.
   * Positive means south to north along a meridian, or bottom to top along a Y-axis.
    * @param scanMode scanMode byte
    * @return true: y points scan in positive direction, false: y points scan in negetive direction
    */
  public static boolean scanModeYisPositive(int scanMode) {
    return GribNumbers.testGribBitIsSet(scanMode, 2);
  }

  /**
   * Adjacent points in x/y direction are consecutive. Grib 1 or 2.
   * @param scanMode scanMode byte
   * @return true: x points are consecutive (row) false: y points are consecutive (col)
   */
  public static boolean scanModeXisConsecutive(int scanMode) {
    return !GribNumbers.testGribBitIsSet(scanMode, 3);
  }

  /**
   * All rows scan in the same/opposite direction. Grib 2 only.
   * @param scanMode scanMode byte
   * @return  true: All rows scan in the same direction, false: Adjacent rows scan in the opposite direction, the first row scan is as defined by previous flags
   */
  public static boolean scanModeSameDirection(int scanMode) {
    return !GribNumbers.testGribBitIsSet(scanMode, 4);
  }

}
