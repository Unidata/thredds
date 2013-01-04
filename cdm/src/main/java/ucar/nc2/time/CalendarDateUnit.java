package ucar.nc2.time;

import net.jcip.annotations.Immutable;
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
    if (calt == null)
      calt = Calendar.getDefault();
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
    if (calt == null)
      calt = Calendar.getDefault();
    return new CalendarDateUnit(calt, udunitString);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  private final Calendar cal;
  private final String unitString;
  private final CalendarPeriod.Field periodField;
  private final CalendarDate baseDate;
  private final boolean isCalendarField;

  private CalendarDateUnit(Calendar calt, String dateUnitString) {
    this.cal = calt;

    dateUnitString = dateUnitString.trim();
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

    unitString = m.group(1);
    periodField = CalendarPeriod.fromUnitString(unitString);

    int pos = dateUnitString.indexOf("since");
    String iso = dateUnitString.substring(pos + 5);
    baseDate = CalendarDateFormatter.isoStringToCalendarDate(cal, iso);
    //DateTime dt = parseUdunitsTimeString(dateUnitString, m.group(2), m.group(4), m.group(5));
    //baseDate = CalendarDate.of(cal, dt);
  }

  /* private DateTime parseUdunitsTimeString(String dateUnitString, String dateString, String timeString, String zoneString) {
    // Set the defaults for any values that are not specified
    int year = 0;
    int month = 1;
    int day = 1;
    int hour = 0;
    int minute = 0;
    double second = 0.0;

    try {
      boolean isMinus = false;
      if (dateString.startsWith("-")) {
         isMinus = true;
         dateString = dateString.substring(1);
       } else if (dateString.startsWith("+")) {
         dateString = dateString.substring(1);
       }

      StringTokenizer dateTokenizer = new StringTokenizer(dateString, "-");
      if (dateTokenizer.hasMoreTokens()) year = Integer.parseInt(dateTokenizer.nextToken());
      if (dateTokenizer.hasMoreTokens()) month = Integer.parseInt(dateTokenizer.nextToken());
      if (dateTokenizer.hasMoreTokens()) day = Integer.parseInt(dateTokenizer.nextToken());

      // Parse the time if present
      if (timeString != null && timeString.length() > 0) {
        StringTokenizer timeTokenizer = new StringTokenizer(timeString, ":");
        if (timeTokenizer.hasMoreTokens()) hour = Integer.parseInt(timeTokenizer.nextToken());
        if (timeTokenizer.hasMoreTokens()) minute = Integer.parseInt(timeTokenizer.nextToken());
        if (timeTokenizer.hasMoreTokens()) second = Double.parseDouble(timeTokenizer.nextToken());
      }

      if (isMinus) year = -year;

      // Get a DateTime object in this Chronology
      DateTime dt = new DateTime(year, month, day, hour, minute, 0, 0, Calendar.getChronology(cal));
      // Add the seconds
      dt = dt.plus((long) (1000 * second));

      // Parse the time zone if present
      if (zoneString != null) {
        zoneString = zoneString.trim();
        if (zoneString.length() > 0 && !zoneString.equals("z") && !zoneString.equals("utc") && !zoneString.equals("gmt")) {
          isMinus = false;
          if (zoneString.startsWith("-")) {
             isMinus = true;
             zoneString = zoneString.substring(1);
           } else if (zoneString.startsWith("+")) {
             zoneString = zoneString.substring(1);
           }

          StringTokenizer zoneTokenizer = new StringTokenizer(zoneString, ":");
          int hourOffset = zoneTokenizer.hasMoreTokens() ? Integer.parseInt(zoneTokenizer.nextToken()) : 0;
          int minuteOffset = zoneTokenizer.hasMoreTokens() ? Integer.parseInt(zoneTokenizer.nextToken()) : 0;
          if (isMinus) hourOffset = -hourOffset;
          DateTimeZone dtz = DateTimeZone.forOffsetHoursMinutes(hourOffset, minuteOffset);

          // Apply the time zone offset, retaining the field values.  This
          // manipulates the millisecond instance.
          dt = dt.withZoneRetainFields(dtz);
          // Now convert to the UTC time zone, retaining the millisecond instant
          dt = dt.withZone(DateTimeZone.UTC);
        }
      }

      return dt;
    } catch (Exception e) {
      throw new IllegalArgumentException("Illegal base time specification: '" + dateUnitString+"'", e);
    }
  } */

  public CalendarDate makeCalendarDate(double value) {
    if (isCalendarField)
      return baseDate.add(CalendarPeriod.of( (int) value, periodField));
    else
      return baseDate.add( value, periodField);
  }

  public CalendarDate makeCalendarDate(int value) {
    if (isCalendarField)
      return baseDate.add(CalendarPeriod.of( value, periodField));
    else
      return baseDate.add( value, periodField);
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    if (isCalendarField) f.format("%s", byCalendarString);
    f.format("%s since %s", unitString, CalendarDateFormatter.toDateTimeString(baseDate));
    return f.toString();
  }

  public CalendarDate getBaseCalendarDate() {
    return baseDate;
  }

  public CalendarPeriod getTimeUnit() {
    return CalendarPeriod.of(1, CalendarPeriod.fromUnitString(unitString));
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