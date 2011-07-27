package ucar.nc2.time;

import net.jcip.annotations.Immutable;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.chrono.ZonedChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

/**
 * A Calendar Date.
 * Cover for joda time, may use javax.time (threeten) in the future.
 *
 * @author caron
 * @since 3/21/11
 */
@Immutable
public class CalendarDate implements Comparable<CalendarDate> {
  // these are thread-safe (yeah!)
  //private static DateTimeFormatter isof = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZoneUTC();
  //private static DateTimeFormatter isof2 = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'").withZoneUTC();

  /**
   * Get a CalendarDate representing the present moment
   * @return CalendarDate representing the present moment
   */
  static public CalendarDate present() {
     return new CalendarDate(null, new DateTime());
  }

  static CalendarDate of(Calendar cal, DateTime dateTime) {
     return new CalendarDate(cal, dateTime);
  }

  public static CalendarDate of(Calendar cal, int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute) {
    Chronology base = Calendar.getChronology(cal);
    if (base == null)
      base = ISOChronology.getInstanceUTC(); // already in UTC
    else
      base = ZonedChronology.getInstance( base, DateTimeZone.UTC); // otherwise wrap it to be in UTC

    DateTime dt = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, base);
    dt = dt.withZone(DateTimeZone.UTC);
    return new CalendarDate(cal, dt);
  }

  public static CalendarDate of(Date date) {
    DateTime dt = new DateTime(date, DateTimeZone.UTC) ;
    return new CalendarDate(Calendar.gregorian, dt);
  }

  /**
   * Create CalendarDate from msecs since epoch
   * @param msecs milliseconds from 1970-01-01T00:00:00Z
   * @return CalendarDate, UTC, ISOChronology
   */
  public static CalendarDate of(long msecs) {
    // Constructs an instance set to the milliseconds from 1970-01-01T00:00:00Z using ISOChronology in the specified time zone.
    DateTime dt = new DateTime(msecs, DateTimeZone.UTC) ;
    return new CalendarDate(Calendar.gregorian, dt);
  }

  /**
   * Get CalendarDate from ISO date string
   * @param calendarName get Calendar from Calendar.get(calendarName). may be null
   * @param isoDateString ISO date string
   * @return  CalendarDate
   */
  public static CalendarDate parseISOformat(String calendarName, String isoDateString) {

    Date date = CalendarDateFormatter.parseISODate(isoDateString);

    //DateTime dt = (isoDateString.indexOf('T') > 0) ? isof.parseDateTime(isoDateString) : isof2.parseDateTime(isoDateString);

    Calendar cal = Calendar.get(calendarName);
    Chronology chronology = Calendar.getChronology(cal);
    DateTime dt = new DateTime(date, chronology);
    //if (chronology != null)
    //  dt = dt.toDateTime(chronology);

    return new CalendarDate(cal, dt);
  }

  /**
   * Get CalendarDate from ISO date string
   * @param calendarName get Calendar from Calendar.get(calendarName). may be null
   * @param udunits must be value (space) udunits string
   * @return  CalendarDate
   */
  public static CalendarDate parseUdunits(String calendarName, String udunits) {
    int pos = udunits.indexOf(' ');
    if (pos < 0) return null;
    String valString = udunits.substring(0, pos).trim();
    String unitString = udunits.substring(pos+1).trim();

    CalendarDateUnit cdu = CalendarDateUnit.of(calendarName, unitString);
    double val = Double.parseDouble(valString);
    return cdu.makeCalendarDate(val);
  }

  ////////////////////////
  private final DateTime dateTime;
  private final Calendar cal;

  private CalendarDate(Calendar cal, DateTime dateTime) {
    this.cal = cal == null ? Calendar.gregorian : cal;
    this.dateTime = dateTime;
  }

  public Calendar getCalendar() {
    return cal;
  }

  // package private
  DateTime getDateTime() {
    return dateTime;
  }

  @Override
  public int compareTo(CalendarDate o) {
    return dateTime.compareTo(o.dateTime);
  }

  public boolean isAfter( CalendarDate o) {
    return dateTime.isAfter(o.dateTime);
  }

  public boolean isBefore( CalendarDate o) {
    return dateTime.isBefore(o.dateTime);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CalendarDate)) return false;
    CalendarDate other = (CalendarDate) o;
    return other.cal == cal && other.dateTime.equals(dateTime);
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (cal != null)
        result += 37 * result + cal.hashCode();
      result += 37 * result + dateTime.hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private int hashCode = 0;

  /**
   * ISO formatted string
   * @return ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZ)
   */
  @Override
  public String toString() {
    return CalendarDateFormatter.toDateTimeStringISO(this);
  }

  /**
   * Get the hour of day (0-23) field for this chronology.
   * @return hour of day (0-23)
   */
  public int getHourOfDay() {
    return dateTime.getHourOfDay();
  }

  public CalendarDate add(double value, CalendarPeriod unit) {
    switch (unit) {
      case Second:
        return new CalendarDate(cal, dateTime.plusSeconds( (int) value ));
      case Minute:
        return new CalendarDate(cal, dateTime.plusSeconds( (int) (value * 60) ));
      case Hour:
        return new CalendarDate(cal, dateTime.plusSeconds( (int) (value * 60 * 60) ));
      case Day:
        return new CalendarDate(cal, dateTime.plusDays( (int) value));
      case Month:
        return new CalendarDate(cal, dateTime.plusMonths( (int) value ));
      case Year:
        return new CalendarDate(cal, dateTime.plusYears( (int) value ));
    }
    throw new UnsupportedOperationException("period units = "+unit);
  }

  public CalendarDate add(double value, CalendarDuration duration) {
    value *=  duration.getValue();
    return add(value, duration.getTimeUnit());
  }

  public Date toDate() {
    return dateTime.toDate();
  }

  public long getMillis() {
    return dateTime.getMillis();
  }

  /**
   * Get difference between two calendar dates in millisecs
   * @param o  other calendar date
   * @return  (this minus o) difference in millisecs
   */
  public long getDifferenceInMsecs(CalendarDate o) {
    return dateTime.getMillis() - o.dateTime.getMillis();
  }

  public static void main(String[] args) {
    CalendarDate dt = CalendarDate.parseISOformat("", "2008-08-01 01:00:00Z");
    System.out.printf("%s%n", dt);
    CalendarDate dt2 = CalendarDate.parseISOformat("", "2005-05-12T00:52:56");
    System.out.printf("%s%n", dt2);
  }

}
