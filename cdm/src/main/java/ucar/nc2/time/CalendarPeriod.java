/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.time;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.util.StringUtil2;

import javax.annotation.concurrent.Immutable;

/**
 * A CalendarPeriod is a logical duration of time, it requires a Calendar to convert to an actual duration of time.
 * A CalendarField is expressed as {integer x Field}.
 *
 * Design follows joda Period class.
 * @author caron
 * @since 3/30/11
 */
@Immutable
public class CalendarPeriod {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CalendarPeriod.class);

  private static final Cache<CalendarPeriod, CalendarPeriod> cache = CacheBuilder.newBuilder()
                .maximumSize(100)  // limit cache size....
                .build();

  public static final CalendarPeriod Hour = CalendarPeriod.of(1, Field.Hour);

  public enum Field {
    Millisec(PeriodType.millis()), Second(PeriodType.seconds()), Minute(PeriodType.minutes()), Hour(PeriodType.hours()),
    Day(PeriodType.days()), Month(PeriodType.months()), Year(PeriodType.years());

    PeriodType p;
    Field(PeriodType p) {
      this.p = p;
    }
  }

  /**
   * Convert a period string into a CalendarPeriod.Field.
   * @param udunit period string
   * @return CalendarPeriod.Field enum
   * @throws IllegalArgumentException if not valid format
   */
  public static CalendarPeriod.Field fromUnitString(String udunit) {
    udunit = udunit.trim();
    udunit = udunit.toLowerCase();

    if (udunit.equals("s")) return Field.Second;
    if (udunit.equals("ms")) return Field.Millisec;

      // eliminate plurals
    if (udunit.endsWith("s")) udunit = udunit.substring(0, udunit.length()-1);

    switch (udunit) {
      case "second":
      case "sec":
        return Field.Second;
      case "millisecond":
      case "millisec":
      case "msec":
        return Field.Millisec;
      case "minute":
      case "min":
        return Field.Minute;
      case "hour":
      case "hr":
      case "h":
        return Field.Hour;
      case "day":
      case "d":
        return Field.Day;
      case "month":
      case "mon":
        return Field.Month;
      case "year":
      case "yr":
        return Field.Year;
      default:
        throw new IllegalArgumentException("cant convert " + udunit + " to CalendarPeriod");
    }
  }

  // minimize memory use by interning. wacko shit in GribPartitionBuilder TimeCoordinate, whoduhthunk?
  public static CalendarPeriod of(int value, Field field) {
    CalendarPeriod want = new CalendarPeriod(value, field);
    if (cache == null) return want;
    CalendarPeriod got = cache.getIfPresent(want);
    if (got != null) return got;
    cache.put(want, want);
    return want;
  }

  /**
   * Convert a udunit period string into a CalendarPeriod
   * @param udunit period string : "[val] unit"
   * @return CalendarPeriod or null if illegal
   */
  public static CalendarPeriod of(String udunit) {
    int value;
    String units;

    String[] split = StringUtil2.splitString(udunit);
    if (split.length == 1) {
      value = 1;
      units =  split[0];

    } else if (split.length == 2) {
      try {
        value = Integer.parseInt(split[0]);
      } catch (Throwable t) {
        return null;
      }
      units =  split[1];
    } else
      return null;


    CalendarPeriod.Field unit = CalendarPeriod.fromUnitString(units);
    return CalendarPeriod.of(value, unit);
  }

  public static CalendarPeriod of(TimeDuration td) {
    CalendarPeriod.Field unit = CalendarPeriod.fromUnitString(td.getTimeUnit().getUnitString());
    return CalendarPeriod.of( (int) td.getValue(), unit);
  }

  ////////////////////////
  // the common case is a single field
  private final int value;
  private final Field field;

  private CalendarPeriod (int value, Field field) {
    this.value = value;
    this.field = field;
  }

  /**
   * Multiply the period by an integer
   * @param value multiply by this
   * @return new period
   */
  public CalendarPeriod multiply(int value) {
    return CalendarPeriod.of(this.value * value, this.field);
  }

  public int getValue() {
    return value;
  }

  public Field getField() {
    return field;
  }

  /**
   * Subtract two dates, return difference in units of this period.
   * If not even, will round down and log a warning
   * @param start start date
   * @param end   end date
   * @return  difference in units of this period
   */
  public int subtract(CalendarDate start, CalendarDate end) {
    long diff = end.getDifferenceInMsecs(start);
    int thislen = millisecs();
    if ((diff % thislen != 0))
      log.warn("roundoff error");
    return (int) (diff / thislen);
  }

  /**
   * Get the conversion factor of the other CalendarPeriod to this one
   * @param from convert from this
   * @return conversion factor, so that getConvertFactor(from) * from = this
   */
  public double getConvertFactor(CalendarPeriod from) {
    if (field == CalendarPeriod.Field.Month || field == CalendarPeriod.Field.Year) {
      log.warn(" CalendarDate.convert on Month or Year");
    }

    return (double) from.millisecs() / millisecs();
  }

  /**
   * Get the duration in milliseconds                                               -+
   * @return the duration in seconds
   * @deprecated dont use because these are fixed length and thus approximate.
   */
  public double getValueInMillisecs() {
     if (field == CalendarPeriod.Field.Month)
       return 30.0 * 24.0 * 60.0 * 60.0 * 1000.0 * value;
     else if (field == CalendarPeriod.Field.Year)
       return 365.0 * 24.0 * 60.0 * 60.0 * 1000.0 * value;
     else return millisecs();
   }

  private int millisecs() {
     if (field == CalendarPeriod.Field.Millisec)
       return value;
     else if (field == CalendarPeriod.Field.Second)
       return 1000 * value;
     else if (field == CalendarPeriod.Field.Minute)
       return 60 * 1000 * value;
     else if (field == CalendarPeriod.Field.Hour)
       return 60 * 60 * 1000 * value;
     else if (field == CalendarPeriod.Field.Day)
       return 24 * 60 * 60 * 1000 * value;

     else throw new IllegalStateException("Illegal Field = "+field);
   }

   /* LOOK from old TimeCoord code, which is better ??
  public static int getOffset(CalendarDate refDate, CalendarDate cd, CalendarPeriod timeUnit) {
    long msecs = cd.getDifferenceInMsecs(refDate);
    return (int) Math.round(msecs / timeUnit.getValueInMillisecs());
  }
  */

  // offset from start to end, in these units
  // start + offset = end
  public int getOffset(CalendarDate start, CalendarDate end) {
    if (start.equals(end)) return 0;
    long start_millis = start.getDateTime().getMillis();
    long end_millis = end.getDateTime().getMillis();

    // 5 second slop
    Period p;
    if (start_millis < end_millis)
      p = new Period(start_millis, end_millis + 5000, getPeriodType());
    else
      p = new Period(start_millis+5000, end_millis, getPeriodType());

    return p.get(getDurationFieldType());
  }

  PeriodType getPeriodType() {
    return getField().p;
  }

  DurationFieldType getDurationFieldType() {
    return getField().p.getFieldType(0);
  }

  @Override
  public String toString() {
    return value + " " + field;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CalendarPeriod that = (CalendarPeriod) o;

    if (value != that.value) return false;
    if (field != that.field) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = value;
    result = 31 * result + (field != null ? field.hashCode() : 0);
    return result;
  }

  public static void main(String[] args) {
    CalendarPeriod cp = CalendarPeriod.of(1, Field.Day);
    CalendarDate start =  CalendarDate.parseUdunits(null, "3 days since 1970-01-01 12:00");
    CalendarDate end =  CalendarDate.parseUdunits(null, "6 days since 1970-01-01 12:00");
    int offset = cp.getOffset(start, end);
    System.out.printf("offset=%d%n", offset);
  }
}
