/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.units;

import ucar.units.*;

import java.util.Date;
import java.util.StringTokenizer;

/**
 * Handles udunits dates, represented as "n units of time since reference date" eg
 *  "1203 days since 1970-01-01 00:00:00".
 * <p>
 * This is a wrapper around ucar.units package.
 * It tracks the value, the base time unit, and the date origin seperately.
 *
 * @author caron
 */

public class DateUnit { // extends SimpleUnit {

  /**
   * Create a java.util.Date from this udunits String.
   * @param text a udunit string.
   *   <pre>[number] (units) since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]</pre>
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
      udunitString = text.substring( firstToke.length());
    } catch (NumberFormatException e) { // stupid way to test if it starts with a number
      value = 0.0;
      udunitString = text;
    }

    DateUnit du;
    try {
      du = new DateUnit( udunitString);
    } catch (Exception e) {
      return null;
    }

    return du.makeDate( value);
  }

  /** Create a java.util.Date from a udunit or ISO String.
   * @param text a udunit or ISO string.
   * @return Date or null if not date unit.
   * @see #getStandardDate
   * @see DateFormatter#getISODate
   */
  static public Date getStandardOrISO( String text) {
    Date result = getStandardDate( text);
    if (result == null) {
      DateFormatter formatter = new DateFormatter();
      result = formatter.getISODate( text);
    }
    return result;
  }

  ////////////////////////////////////////////////////////////////////////
  private double value;
  private String timeUnitString, udunitString;
  private TimeUnit timeUnit = null;
  private DateFormatter formatter;
  private Unit uu;

  /**
   * Constructor.
   * @param text udunits String, eg 3 secs since 1991-01-01T03:12
   * @throws Exception if malformed String.
   */
  public DateUnit(String text) throws Exception {
    super();

    text = text.trim();
    StringTokenizer stoker = new StringTokenizer(text);
    String firstToke = stoker.nextToken();
    try {
      this.value = Double.parseDouble(firstToke);
      //if (this.value == 0.0)
      //  this.value = 1.0;
      this.udunitString = text.substring( firstToke.length()); // eliminate the value if there is one
      this.timeUnitString = stoker.nextToken();

    } catch (NumberFormatException e) { // stupid way to test if it starts with a number
      this.value = 0.0;
      this.udunitString = text;
      this.timeUnitString = firstToke;
    }

    uu = SimpleUnit.makeUnit( udunitString); // always a base unit
    timeUnit = new TimeUnit( timeUnitString);
  }

  /**
   * Constructor that takes a value and a "base" udunit string.
   * @param value
   * @param unitString eg "secs since 1970-01-01T00:00:00Z"
   * @throws java.lang.Exception if not valid time unit.
   *
  public DateUnit(double value, String unitString) throws Exception {
    this.value = value;
    this.unitString = unitString;
    uu = SimpleUnit.makeUnit( unitString);
  } */

  /** Get the origin Date.
   * @return Date or null if not a time unit.
   */
  public Date getDateOrigin() {
    if (!(uu instanceof TimeScaleUnit)) return null;
    TimeScaleUnit tu = (TimeScaleUnit) uu;
    return tu.getOrigin();
  }

  /**
   * For udunit dates, get the time unit only, as a String, eg "secs" or "days"
   * @return  time unit as a string
   */
  public String getTimeUnitString() { return timeUnitString; }
   /**
   * For udunit dates, get the time unit.
    * @return time unit
    */
   public TimeUnit getTimeUnit() { return timeUnit; }

  /** Get the equivilent java.util.Date.
   * @return Date or null if failure
   */
  public Date getDate() {
    double secs = timeUnit.getValueInSeconds(value);
    return new Date( getDateOrigin().getTime() + (long)(1000*secs));
  }

  /** Create a Date from this base unit and the given value.
   * @param val value in the units of this base unit, eg sec since base date
   * @return Date .
   */
  public Date makeDate(double val) {
    double secs = timeUnit.getValueInSeconds(val); //
    return new Date( getDateOrigin().getTime() + (long)(1000*secs));
  }

 /** Create the equivilent value from this base unit and the given Date.
  * Inverse of makeDate.
   * @param date to convert.
   * @return value in units of this base unit.
   */
  public double makeValue(Date date) {
    double secs = date.getTime() / 1000;
    double origin_secs =  getDateOrigin().getTime() / 1000;
    double diff = secs - origin_secs;

   try {
     timeUnit.setValueInSeconds( diff);
   } catch (Exception e) {
     throw new RuntimeException( e.getMessage());
   }
   return timeUnit.getValue();
  }

  /** Make a standard GMT string representation from this unit and given value.
   * @param value of time in these units.
   * @return String or null if not time unit.
   */
  public String makeStandardDateString(double value) {
    Date date = makeDate( value);
    if (date == null) return null;
    if (formatter == null) formatter = new DateFormatter();
    return formatter.toDateTimeStringISO(date);
  }

  public String toString() {
    return value + " "+udunitString;
  }

  /**
   * The udunits string, but no value, ie its a base unit.
   * @return the udunits base string
   */
  public String getUnitsString() {
    return udunitString;
  }

}
