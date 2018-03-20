/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import ucar.nc2.time.CalendarDate;
import ucar.units.*;

import java.util.Date;
import java.util.StringTokenizer;

/**
 * Handles udunits dates, represented as "n units of time since reference date" eg
 * "1203 days since 1970-01-01 00:00:00".
 * <p>
 * This is a wrapper around ucar.units package.
 * It tracks the value, the base time unit, and the date origin seperately.
 *
 * @author caron
 */

public class DateUnit { // extends SimpleUnit {

  /**
   * Create a java.util.Date from this udunits String.
   *
   * @param text a udunit string.
   *             <pre>[number] (units) since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]</pre>
   * @return Date or null if not date unit.
   */
  static public Date getStandardDate(String text) {
    double value;
    String udunitString;

    text = text.trim();
    StringTokenizer stoker = new StringTokenizer(text);
    String firstToke = stoker.nextToken();
    try {
      value = Double.parseDouble(firstToke);
      udunitString = text.substring(firstToke.length());
    } catch (NumberFormatException e) { // stupid way to test if it starts with a number
      value = 0.0;
      udunitString = text;
    }

    DateUnit du;
    try {
      du = new DateUnit(udunitString);
    } catch (Exception e) {
      return null;
    }

    return du.makeDate(value);
  }

  /**
   * Create a java.util.Date from a udunit or ISO String.
   *
   * @param text a udunit or ISO string.
   * @return Date or null if not date unit.
   * @see #getStandardDate
   * @see DateFormatter#getISODate
   */
  static public Date getStandardOrISO(String text) {
    Date result = getStandardDate(text);
    if (result == null) {
      DateFormatter formatter = new DateFormatter();
      result = formatter.getISODate(text);
    }
    return result;
  }

  static public CalendarDate parseCalendarDate(String text) {
    Date result = getStandardDate(text);
    if (result == null) {
      DateFormatter formatter = new DateFormatter();
      result = formatter.getISODate(text);
    }
    return CalendarDate.of(result);
  }

  ////////////////////////////////////////////////////////////////////////
  private double value;
  private String udunitString;
  private TimeUnit timeUnit = null;
  private Unit uu;

  static public DateUnit getUnixDateUnit() {
    try {
      return new DateUnit("s since 1970-01-01 00:00:00");
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static DateUnit factory(String text) {
    try {
      return new DateUnit(text);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Constructor.
   *
   * @param text udunits String, eg 3 secs since 1991-01-01T03:12
   * @throws UnitException if malformed String.
   */
  public DateUnit(String text) throws UnitException {
    String timeUnitString;

    text = text.trim();
    StringTokenizer stoker = new StringTokenizer(text);
    String firstToke = stoker.nextToken();
    try {
      this.value = Double.parseDouble(firstToke);
      //if (this.value == 0.0)
      //  this.value = 1.0;
      this.udunitString = text.substring(firstToke.length()); // eliminate the value if there is one
      timeUnitString = stoker.nextToken();

    } catch (NumberFormatException e) { // stupid way to test if it starts with a number
      this.value = 0.0;
      this.udunitString = text;
      timeUnitString = firstToke;
    }

    uu = SimpleUnit.makeUnit(udunitString); // always a base unit
    timeUnit = new TimeUnit(timeUnitString);
  }

  /**
   * Constructor that takes a value, timeUnitString, and a Date since
   *
   * @param value          number of time units
   * @param timeUnitString eg "secs"
   * @param since date since, eg "secs since 1970-01-01T00:00:00Z"
   * @throws UnitException if not valid time unit.
   */
  public DateUnit(double value, String timeUnitString, Date since) throws UnitException {
    this.value = value;
    DateFormatter df = new DateFormatter();
    this.udunitString = timeUnitString + " since " + df.toDateTimeStringISO(since);
    uu = SimpleUnit.makeUnit(this.udunitString);
    timeUnit = new TimeUnit(timeUnitString);
  }

  /**
   * Get the origin Date.
   *
   * @return Date or null if not a time unit.
   */
  public Date getDateOrigin() {
    if (!(uu instanceof TimeScaleUnit)) return null;
    TimeScaleUnit tu = (TimeScaleUnit) uu;
    return tu.getOrigin();
  }

  /**
   * For udunit dates, get the time unit only, as a String, eg "secs" or "days"
   *
   * @return time unit as a string
   */
  public String getTimeUnitString() {
    return timeUnit.getUnitString();
  }

  /**
   * For udunit dates, get the time unit.
   *
   * @return time unit
   */
  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  /**
   * Get the equivalent java.util.Date.
   *
   * @return Date or null if failure
   */
  public Date getDate() {
    double secs = timeUnit.getValueInSeconds(value);
    return new Date(getDateOrigin().getTime() + (long) (1000 * secs));
  }

  /**
   * Get the equivalent CalendarDate.
   *
   * @return CalendarDate or null if failure
   */
  public CalendarDate makeCalendarDate(double val) {
    return CalendarDate.of(makeDate(val));
  }

  /**
   * Create a Date from this base unit and the given value.
   *
   * @param val value in the units of this base unit, eg sec since base date
   * @return Date .
   */
  public Date makeDate(double val) {
    if (Double.isNaN(val)) return null;
    double secs = timeUnit.getValueInSeconds(val); //
    return new Date(getDateOrigin().getTime() + (long) (1000 * secs));
  }

  /**
   * Create the equivalent value from this base unit and the given Date.
   * Inverse of makeDate.
   *
   * @param date to convert.
   * @return value in units of this base unit.
   */
  public double makeValue(Date date) {
    double secs = date.getTime() / 1000.0;
    double origin_secs = getDateOrigin().getTime() / 1000.0;
    double diff = secs - origin_secs;

    try {
      timeUnit.setValueInSeconds(diff);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
    return timeUnit.getValue();
  }

  /**
   * Make a standard GMT string representation from this unit and given value.
   *
   * @param value of time in these units.
   * @return String or null if not time unit.
   */
  public String makeStandardDateString(double value) {
    Date date = makeDate(value);
    if (date == null) return null;
    DateFormatter formatter = new DateFormatter();
    return formatter.toDateTimeStringISO(date);
  }

  public String toString() {
    return value + " " + udunitString;
  }

  /**
   * The udunits string, but no value, ie its a base unit.
   *
   * @return the udunits base string
   */
  public String getUnitsString() {
    return udunitString;
  }

  public static void main(String[] args) throws Exception {
    UnitFormat udunit = UnitFormatManager.instance();
    //String text = "days since 2009-06-14 04:00:00";
    String text2 = "days since 2009-06-14 04:00:00 +00:00";
    Unit uu2 = udunit.parse(text2);
    System.out.printf("%s == %s %n", text2, uu2);

    String text = "days since 2009-06-14 04:00:00";
    Unit uu = udunit.parse(text);
    System.out.printf("%s == %s %n", text, uu);

    Unit ref = udunit.parse("ms since 1970-01-01");
    Converter converter = uu.getConverterTo(ref);
    DateFormatter formatter = new DateFormatter();

    double val = converter.convert(1.0);
    Date d = new Date((long) val);
    System.out.printf(" val=%f date=%s (%s)%n", 1.0, d, formatter.toDateTimeStringISO(d));

    double val2 = converter.convert(2.0);
    Date d2 = new Date((long) val2);
    System.out.printf(" val=%f date=%s (%s)%n", 2.0, d, formatter.toDateTimeStringISO(d2));

  }


}
