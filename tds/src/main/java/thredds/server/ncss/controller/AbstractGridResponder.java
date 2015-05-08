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
package thredds.server.ncss.controller;

import java.text.ParseException;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

/**
 * superclass for Grid responders
 */
public abstract class AbstractGridResponder {

  public static CalendarDateRange getRequestedDateRange(NcssParamsBean params, Calendar cal) throws ParseException {
    if (params.getTime() != null) {
      CalendarDate date;
      if (params.getTime().equalsIgnoreCase("present")) {
        date = CalendarDate.present();
      } else {
        date = CalendarDateFormatter.isoStringToCalendarDate(cal, params.getTime());
      }
      return CalendarDateRange.of(date, date);
    }

    //We should have valid params here...
    DateRange dr = new DateRange(new DateType(params.getTime_start(), null, null, cal), new DateType(params.getTime_end(), null, null, cal), new TimeDuration(params.getTime_duration()), null);
    return CalendarDateRange.of(dr.getStart().getCalendarDate(), dr.getEnd().getCalendarDate());
  }

  /* protected List<CalendarDate> getRequestedDates(GridCoverageDataset gcd, NcssParamsBean params) throws OutOfBoundariesException, ParseException, TimeOutOfWindowException {

    //GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gcd, params.getVar());
    List<CalendarDate> dates = gap.getDates();

    if (dates.isEmpty()) return dates;

    //Get the calendar
    Calendar cal = dates.get(0).getCalendar();

    long time_window = 0;
    if (params.getTime_window() != null) {
      TimeDuration dTW = new TimeDuration(params.getTime_window());
      time_window = (long) dTW.getValueInSeconds() * 1000;
    }

    //Check param temporal=all (ignore any other value) --> returns all dates
    if (params.isAllTimes()) {
      return dates;
    } else { //Check if some time param was provided, if not closest time to current
      if (params.getTime() == null && params.getTime_start() == null && params.getTime_end() == null && params.getTime_duration() == null) {
        //Closest to present
        List<CalendarDate> closestToPresent = new ArrayList<>();
        DateTime dt = new DateTime(new Date(), DateTimeZone.UTC);
        CalendarDate now = CalendarDate.of(cal, dt.getMillis());

        CalendarDate start = dates.get(0);
        CalendarDate end = dates.get(dates.size() - 1);
        if (now.isBefore(start)) {
          //now = start;
          if (time_window <= 0 || Math.abs(now.getDifferenceInMsecs(start)) < time_window) {
            closestToPresent.add(start);
            return closestToPresent;
          } else {
            throw new TimeOutOfWindowException("There is no time within the provided time window");
          }
        }
        if (now.isAfter(end)) {
          //now = end;
          if (time_window <= 0 || Math.abs(now.getDifferenceInMsecs(end)) < time_window) {
            closestToPresent.add(end);
            return closestToPresent;
          } else {
            throw new TimeOutOfWindowException("There is no time within the provided time window");
          }
        }

        return NcssRequestUtils.wantedDates(gap, CalendarDateRange.of(now, now), time_window);
      }
    }

    //We should have a time or a timeRange...
    if (params.getTime_window() != null && params.getTime() != null) {
      DateRange dr = new DateRange(new DateType(params.getTime(), null, null, cal), null, new TimeDuration(params.getTime_window()), null);
      time_window = CalendarDateRange.of(dr.getStart().getCalendarDate(), dr.getEnd().getCalendarDate()).getDurationInSecs() * 1000;
    }

    CalendarDateRange dateRange = getRequestedDateRange(params, cal);
    return NcssRequestUtils.wantedDates(gap, dateRange, time_window);
  }   */

}
