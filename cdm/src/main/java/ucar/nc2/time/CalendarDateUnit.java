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
package ucar.nc2.time;

import javax.annotation.concurrent.Immutable;

import java.util.Date;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Calendar Date Unit: "unit since date"
 *
 * <pre>
 * UNIT since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
 *
 * UNIT = "msecs" | "msec" |
          "seconds" | "second" | "secs" | "sec" | "s" |
          "minutes" | "minute" | "mins" | "min" |
          "hours" | "hour" | "hrs" | "hr" | "h" |
          "days" | "day" | "d"
 * </pre>
  *
 *  "months since base_date by calendar field"
 *  "years since base_date by calendar field"
 *
 *  bob simon's proposal:
 *
 *  "calendar_month since base_date"
 *  "calendar_year since base_date"
 *  * only integer values are allowed (?)
 *
 *  jon blowers comment:
In your view would the solution to add "by calendar field" to the existing udunits string be acceptable?  It’s
 backward-compatible with the current interpretation and adds clarification for the cases in which we *do* want to
 do calendar-field arithmetic (instead of adding fixed intervals).
There’s an alternative proposition, in which the new units of calendar_month and calendar_year are added, with the same semantic effect.
 (However, personally I like the "by calendar field" solution since it allows other fields to vary between calendars, e.g.
 because of leap-seconds.)
 *
 * @author caron
 * @since 3/18/11
 */
@Immutable
public class CalendarDateUnit {
  public static final CalendarDateUnit unixDateUnit = CalendarDateUnit.of(null, CalendarPeriod.Field.Second, CalendarDate.parseISOformat(null, "1970-01-01T00:00:00"));

  private static final String byCalendarString = "calendar ";
  //                                                  1                     2             3    4             5
  public static final String udunitPatternString = "(\\w*)\\s*since\\s*"+CalendarDateFormatter.isodatePatternString;
  //                                                                     "([\\+\\-\\d]+)([ Tt]([\\.\\:\\d]*)([ \\+\\-]\\S*)?z?)?$"; // public for testing
  private static final Pattern udunitPattern = Pattern.compile(udunitPatternString);

  /**
   * Create a CalendarDateUnit from a calendar name and a udunit string = "unit since calendarDate"
   * @param calendarName must match a calendar enum, or one of its aliases, see ucar.nc2.time.Calendar.get()
   * @param udunitString "unit since calendarDate"
   * @return CalendarDateUnit
   * @throws IllegalArgumentException if udunitString is not paresable
   */
  static public CalendarDateUnit of(String calendarName, String udunitString) {
    Calendar calt = Calendar.get(calendarName);
    if (calt == null) calt = Calendar.getDefault();
    return new CalendarDateUnit(calt, udunitString);
  }

  /**
   * Create a CalendarDateUnit from a calendar and a udunit string = "unit since calendarDate"
   * @param calt use this Calendar, or null for default calendar
   * @param udunitString "unit since calendarDate"
   * @return CalendarDateUnit
   * @throws IllegalArgumentException if udunitString is not paresable
   */
  static public CalendarDateUnit withCalendar(Calendar calt, String udunitString) {
    if (calt == null) calt = Calendar.getDefault();
    return new CalendarDateUnit(calt, udunitString);
  }

  /**
   * Create a CalendarDateUnit from a calendar, a CalendarPeriod.Field, and a base date
   * @param calt use this Calendar, or null for default calendar
   * @param periodField a CalendarPeriod.Field like Hour or second
   * @param baseDate "since baseDate"
   * @return CalendarDateUnit
   */
  static public CalendarDateUnit of(Calendar calt, CalendarPeriod.Field periodField, CalendarDate baseDate) {
    if (calt == null) calt = Calendar.getDefault();
    return new CalendarDateUnit(calt, periodField, baseDate);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  private final Calendar cal;
  // private final String unitString;
  private final CalendarPeriod period;
  private final CalendarPeriod.Field periodField;
  private final CalendarDate baseDate;
  private final boolean isCalendarField;

  private CalendarDateUnit(Calendar calt, String dateUnitString) {

    dateUnitString = dateUnitString.trim();
    // dateUnitString = dateUnitString.replaceAll("\\s+", " ");  LOOK think about should we allow this ??
    dateUnitString = dateUnitString.toLowerCase();

    isCalendarField =  dateUnitString.startsWith(byCalendarString);
    if (isCalendarField) {
      dateUnitString = dateUnitString.substring(byCalendarString.length()).trim();
    }

    Matcher m = udunitPattern.matcher(dateUnitString);
    if (!m.matches()) {
      //System.out.printf("'%s' does not match regexp '%s'%n", dateUnitString, udunitPatternString);
      throw new IllegalArgumentException(dateUnitString + " does not match " + udunitPatternString);
    }

    String unitString = m.group(1);
    period = CalendarPeriod.of(unitString);
    periodField = CalendarPeriod.fromUnitString(unitString);

    int pos = dateUnitString.indexOf("since");
    String iso = dateUnitString.substring(pos + 5);
    baseDate = CalendarDateFormatter.isoStringToCalendarDate(calt, iso);

    // calendar might change !!
    calt = baseDate.getCalendar();
    this.cal = calt;
  }

  private CalendarDateUnit(Calendar calt, CalendarPeriod.Field periodField, CalendarDate baseDate) {
    this.cal = calt;
    this.periodField = periodField;
    this.period = CalendarPeriod.of(1, periodField);
    this.baseDate = baseDate;

    if (periodField == CalendarPeriod.Field.Month || periodField == CalendarPeriod.Field.Year) {
      isCalendarField = true;
    } else {
      isCalendarField = false;
    }
  }

  // given a CalendarDate, find the values in this unit (secs, days, etc) from the baseDate
  // inverse of makeCalendarDate
  public double makeOffsetFromRefDate( CalendarDate date) {
    if (isCalendarField) {
      if (date.equals(baseDate)) return 0.0;
      return date.getDifference(baseDate, periodField);
    } else {
      long msecs = date.getDifferenceInMsecs(baseDate);
      return msecs / period.getValueInMillisecs();
    }
  }

  // given a value in this unit (secs, days, etc), create the CalendarDate from the RefDate
  // inverse of makeOffsetFromRefDate
  public CalendarDate makeCalendarDate(double value) {
    if (isCalendarField)
      return baseDate.add(CalendarPeriod.of( (int) value, periodField));  // LOOK int vs double
    else
      return baseDate.add( value, periodField);
  }

  public CalendarDate makeCalendarDate(int value) {
    if (isCalendarField)
      return baseDate.add(CalendarPeriod.of( value, periodField));
    else
      return baseDate.add( value, periodField);
  }

  public String getUdUnit() {
      return toString();
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    if (isCalendarField) f.format("%s", byCalendarString);
    f.format("%s since %s", periodField, CalendarDateFormatter.toDateTimeStringISO(baseDate));
    return f.toString();
  }

  public CalendarDate getBaseCalendarDate() {
    return baseDate;
  }

  public CalendarPeriod getCalendarPeriod() {
    return period;
  }

  public CalendarPeriod.Field getCalendarField() {
    return periodField;
  }

  public Calendar getCalendar() {
    return cal;
  }

  public boolean isCalendarField() {
    return isCalendarField;
  }

  // testing
  Date getBaseDate() {
    return baseDate.toDate();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) {
    CalendarDateUnit cdu;

    String s = "calendar Month since 2012-01-19T18:00:00.000Z";
    cdu = CalendarDateUnit.of(null, s);
    System.out.printf("%s == %s%n", s, cdu);
  }

}

/*

The acceptable units for time are listed in the udunits.dat file. The most commonly used of these strings (and their abbreviations)
includes day (d), hour (hr, h), minute (min) and second (sec, s). Plural forms are also acceptable. The reference time string
(appearing after the identifier since) may include date alone; date and time; or date, time, and time zone.
The reference time is required. A reference time in year 0 has a special meaning (see Section 7.4, Climatological Statistics).

Note: if the time zone is omitted the default is UTC, and if both time and time zone are omitted the default is 00:00:00 UTC.

We recommend that the unit year be used with caution. The Udunits package defines a year to be exactly 365.242198781 days
(the interval between 2 successive passages of the sun through vernal equinox). It is not a calendar year.
Udunits includes the following definitions for years: a common_year is 365 days, a leap_year is 366 days, a Julian_year is 365.25 days,
and a Gregorian_year is 365.2425 days.

For similar reasons the unit month, which is defined in udunits.dat to be exactly year/12, should also be used with caution.

Example 4.4. Time axis

double time(time) ;
  time:long_name = "time" ;
  time:units = "days since 1990-1-1 0:0:0" ;


A time coordinate is identifiable from its units string alone. The Udunits routines utScan() and utIsTime() can be used to
make this determination.

Optionally, the time coordinate may be indicated additionally by providing the standard_name attribute with an appropriate
value, and/or the axis attribute with the value
*/
