package ucar.nc2.time;

import net.jcip.annotations.Immutable;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.ISOChronology;

import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Calendar Date Unit: "unit since date"
 * Cover for joda time, may use javax.time (threeten) in the future.
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
 * @author caron
 * @since 3/18/11
 */
@Immutable
public class CalendarDateUnit {
  private static final String isodatePatternString = "([\\+\\-\\d]*)[ T]([\\.\\:\\d]*)([ \\+\\-]\\S*)?$";
  private static final Pattern isodatePattern = Pattern.compile(isodatePatternString);
  //private static final String udunitPatternString = "(\\w*)\\s*since\\s*([\\+\\-\\d]*)[ T]?([\\.\\:\\d]*)([ \\+\\-]\\S*)?$";
  //                                                  1                     2             3    4             5
  private static final String udunitPatternString = "(\\w*)\\s*since\\s*"+"([\\+\\-\\d]+)([ T]([\\.\\:\\d]*)([ \\+\\-]\\S*)?Z?)?$";
  private static final Pattern udunitPattern = Pattern.compile(udunitPatternString);

  /**
   * Create a CalendarDateUnit from a calendar name and a udunit string = "unit since calendarDate"
   * @param calendarName must match a calendar enum, or one of its aliases, see ucar.nc2.time.Calendar.get()
   * @param udunitString "unit since calendarDate"
   * @return CalendarDateUnit
   */
  static public CalendarDateUnit of(String calendarName, String udunitString) {
    return new CalendarDateUnit(calendarName, udunitString);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  private final Calendar cal;
  private final String unitString;
  private final long unitLengthMsecs;
  private final DateTime baseDateTime;

  private CalendarDateUnit(String calendarName, String dateUnitString) {
    cal = Calendar.get(calendarName);
    Chronology chronology = Calendar.getChronology(cal);
    if (chronology == null) chronology = ISOChronology.getInstanceUTC();

    if (!chronology.getZone().equals(DateTimeZone.UTC)) {
      throw new IllegalArgumentException("The time zone of the Chronology must be UTC");
    }

    Matcher m = udunitPattern.matcher(dateUnitString);
    if (!m.matches()) {
      System.out.printf("%s does not match %s%n", dateUnitString, udunitPatternString);
      throw new IllegalArgumentException(dateUnitString + " does not match " + udunitPatternString);
    }

    unitString = m.group(1);
    unitLengthMsecs = getUnitLengthMillis(unitString);

    baseDateTime = parseUdunitsTimeString(chronology, dateUnitString, m.group(2), m.group(4), m.group(5));
  }

  private long getUnitLengthMillis(String unit) {
    unit = unit.trim();
    unit = unit.toLowerCase();
    if (unit.equals("seconds") || unit.equals("second") || unit.equals("secs") || unit.equals("sec") || unit.equals("s")) {
      return 1000;
    } else if (unit.equals("msecs") || unit.equals("msec")) {
        return 1;
    } else if (unit.equals("minutes") || unit.equals("minute") || unit.equals("mins") || unit.equals("min")) {
      return 1000 * 60;
    } else if (unit.equals("hours") || unit.equals("hour") || unit.equals("hrs") || unit.equals("hr") || unit.equals("h")) {
      return 1000 * 60 * 60;
    } else if (unit.equals("days") || unit.equals("day") || unit.equals("d")) {
      return 1000 * 60 * 60 * 24;
    } else {
      throw new IllegalArgumentException("Unrecognized unit for time axis: " + unit);
    }
  }

  private DateTime parseUdunitsTimeString(Chronology chronology, String dateUnitString, String dateString, String timeString, String zoneString) {
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
      DateTime dt = new DateTime(year, month, day, hour, minute, 0, 0, chronology);
      // Add the seconds
      dt = dt.plus((long) (1000 * second));

      // Parse the time zone if present
      if (zoneString != null) {
        zoneString = zoneString.trim();
        if (zoneString.length() > 0 && !zoneString.equals("Z") && !zoneString.equals("UTC") && !zoneString.equals("GMT")) {
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
      throw new IllegalArgumentException("Illegal base time specification: '" + dateUnitString+"' "+e.getMessage());
    }
  }

  private DateTime parseIsoTimeString(Chronology chronology, String dateString, String timeString, String zoneString) {
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
      DateTime dt = new DateTime(year, month, day, hour, minute, 0, 0, chronology);
      // Add the seconds
      dt = dt.plus((long) (1000 * second));

      // Parse the time zone if present
      if (zoneString != null) {
        zoneString = zoneString.trim();
        if (zoneString.length() > 0 && !zoneString.equals("Z") && !zoneString.equals("UTC") && !zoneString.equals("GMT")) {
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
      throw new IllegalArgumentException("Illegal base time specification: '" + dateString+"' "+e.getMessage());
    }
  }

  public CalendarDate makeCalendarDate(double value) {
    return CalendarDate.of(cal, baseDateTime.plus((long) (unitLengthMsecs * value)));
  }

  @Override
  public String toString() {
    return unitString+" since "+baseDateTime;
  }

  public CalendarDate getBaseCalendarDate() {
    return CalendarDate.of(cal, baseDateTime);
  }

  public CalendarDuration getTimeUnit() {
    return CalendarDuration.fromUnitString(unitString);
  }

  public Calendar getCalendar() {
    return cal;
  }

  // testing
  Date getBaseDate() {
    return baseDateTime.toDate();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) {
    CalendarDateUnit cdu;

    String s = "secs since ";
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