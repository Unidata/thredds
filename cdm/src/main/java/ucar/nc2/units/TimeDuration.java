/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.units;

import ucar.units.ConversionException;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.Date;

/**
 * Implements the thredds "duration" XML element type: specifies a length of time.
 * This is really the same as a ucar.nc2.units.TimeUnit, but it allows xsd:duration syntax as well as udunits syntax.
 * It also keeps track if the text is empty.
 * <p/>
 * A duration can be one of the following:
 * <ol>
 * <li> a valid udunits string compatible with "secs"
 * <li> an xsd:duration type specified in the following form "PnYnMnDTnHnMnS" where:
 * <ul>
 * <li>P indicates the period (required)
 * <li>nY indicates the number of years
 * <li>nM indicates the number of months
 * <li>nD indicates the number of days
 * <li>T indicates the start of a time section (required if you are going to specify hours, minutes, or seconds)
 * <li>nH indicates the number of hours
 * <li>nM indicates the number of minutes
 * <li>nS indicates the number of seconds
 * </ul>
 * </ol>
 *
 * @author john caron
 * @see "http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/InvCatalogSpec.html#durationType"
 */


public class TimeDuration {
  /* private static TimeUnit secUnits;

  static {
    try {
      secUnits = new TimeUnit("1 sec");
    } catch (Exception e) {
      e.printStackTrace();
    }
  } */

  private String text;
  private TimeUnit timeUnit;
  private boolean isBlank;

  static private boolean debug = false;

  private TimeDuration() {
  }

  /**
   * Copy constructor.
   *
   * @param src copy this
   */
  public TimeDuration(TimeDuration src) {
    text = src.getText();
    timeUnit = new TimeUnit(src.getTimeUnit());
    isBlank = src.isBlank();
  }

  /**
   * Construct from a TimeUnit.
   *
   * @param timeUnit copy this
   */
  public TimeDuration(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
    this.text = timeUnit.toString();
  }

  /**
   * Construct from 1) udunit time unit string, 2) xsd:duration syntax, 3) blank string.
   *
   * @param text parse this text.
   * @throws java.text.ParseException if invalid text.
   */
  public TimeDuration(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    this.text = text;

    // see if its blank
    if (text.length() == 0) {
      isBlank = true;
      try {
        timeUnit = new TimeUnit("1 sec");
      } catch (Exception e) { // cant happen
      }
      return;
    }

    // see if its a udunits string
    try {
      timeUnit = new TimeUnit(text);
      if (debug) System.out.println(" set time unit= " + timeUnit);
    }

    catch (Exception e) {
      // see if its a xsd:duration
      try {
        DatatypeFactory factory = DatatypeFactory.newInstance();
        Duration d = factory.newDuration(text);
        long secs = d.getTimeInMillis(new Date()) / 1000;
        timeUnit = new TimeUnit(secs + " secs");
      } catch (Exception e1) {
        throw new java.text.ParseException(e.getMessage(), 0);
      }

    }

  }

  /**
   * A time span as defined in the W3C XML Schema 1.0 specification:
   * "PnYnMnDTnHnMnS, where nY represents the number of years, nM the number of months, nD the number of days,
   * 'T' is the date/time separator, nH the number of hours, nM the number of minutes and nS the number of seconds.
   * The number of seconds can include decimal digits to arbitrary precision."
   *
   * @param text parse this text, format PnYnMnDTnHnMnS
   * @return TimeDuration
   * @throws java.text.ParseException when text is misformed
   */
  static public TimeDuration parseW3CDuration(String text) throws java.text.ParseException {
    TimeDuration td = new TimeDuration();

    text = (text == null) ? "" : text.trim();
    td.text = text;

    try {
      DatatypeFactory factory = DatatypeFactory.newInstance();
      Duration d = factory.newDuration(text);
      long secs = d.getTimeInMillis(new Date()) / 1000;
      td.timeUnit = new TimeUnit(secs + " secs");
    } catch (Exception e) {
      throw new java.text.ParseException(e.getMessage(), 0);
    }
    return td;
  }

  /**
   * Get the duration in natural units, ie units of getTimeUnit()
   * @return the duration in natural units
   */
  public double getValue() {
    return timeUnit.getValue();
  }

  /**
   * Get the time duration in a specified unit of time.
   *
   * @param want in these units
   * @return the duration in units
   * @throws ucar.units.ConversionException is specified unit is not compatible with time
   */
  public double getValue(TimeUnit want) throws ConversionException {
    return timeUnit.convertTo(timeUnit.getValue(), want);
  }

  /**
   * Get the duration in seconds
   * @return the duration in seconds
   */
  public double getValueInSeconds() {
    return timeUnit.getValueInSeconds();
  }

  /**
   * Set the duration in seconds
   * @param secs the duration in seconds
   */
  public void setValueInSeconds(double secs) {
    timeUnit.setValueInSeconds(secs);
    text = null;
  }

  /**
   * If this is a blank string
   * @return true if this is a blank string
   */
  public boolean isBlank() {
    return isBlank;
  }

  /**
   * Get the corresponding time unit
   * @return the corresponding time unit
   */
  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  /**
   * Get the String text
   * @return the text
   */
  public String getText() {
    return text == null ? timeUnit.toString() : text;
  }

  /**
   * String representation
   * @return getText()
   */
  public String toString() {
    return getText();
  }

  /**
   * Override to be consistent with equals
   */
  public int hashCode() {
    return isBlank() ? 0 : (int) getValueInSeconds();
  }

  /**
   * TimeDurations with same value in seconds are equals
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof TimeDuration))
      return false;

    TimeDuration to = (TimeDuration) o;
    return to.getValueInSeconds() == getValueInSeconds();
  }

  ////////////////////////////////////////////
  // test

  private static void doDuration(String s) {
    try {
      System.out.println("start = (" + s + ")");
      TimeDuration d = new TimeDuration(s);
      System.out.println("duration = (" + d.toString() + ")");
    }
    catch (java.text.ParseException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    doDuration("3 days");
  }
}