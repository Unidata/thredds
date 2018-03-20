/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateUnit;

/**
 * Helper class for time coordinates
 *
 * @author caron
 * @since 11/6/2014
 */
public class CoordinateAxisTimeHelper {
  private final ucar.nc2.time.Calendar calendar;
  private final CalendarDateUnit dateUnit;

  public CoordinateAxisTimeHelper(Calendar calendar, String unitString) {
    this.calendar = calendar;
    if (unitString == null) {
      this.dateUnit = null;
      return;
    }
    this.dateUnit = CalendarDateUnit.withCalendar(calendar, unitString); // this will throw exception on failure
  }

  public CalendarDate makeCalendarDateFromOffset(double offset) {
    return dateUnit.makeCalendarDate(offset);
}

  public CalendarDate makeCalendarDateFromOffset(String offset) {
    return CalendarDateFormatter.isoStringToCalendarDate(calendar, offset);
  }

  public double offsetFromRefDate(CalendarDate date) {
    return dateUnit.makeOffsetFromRefDate(date);
  }

}
