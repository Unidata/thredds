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

import ucar.units.*;

import java.util.Date;
import java.util.StringTokenizer;

/**
 * Handles udunits dates, represented as "n units of time since reference date" eg
 *  "1203 days since 1970-01-01 00:00:00".
 * <p>
 * This is a wrapper around ucar.units package.
 *
 * @author caron
 */

public class DateUnit {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DateUnit.class);

  /**
   * Create a java.util.Date from this udunits String.
   * @param text a udunit string.
   *   <pre>[number] (units) since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]</pre>
   * @return Date or null if not date unit.
   */
  static public Date getStandardDate(String text) {

    DateUnit du;
    try {
      du = new DateUnit( text);
    } catch (Exception e) {
      return null; // bad input
    }

    return du.getDate();
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


  static public DateUnit getUnixDateUnit() {
    try {
      return new DateUnit("secs since 1970-00-00:00.00");
    } catch (Exception e) {
      log.error("Error parsing UnixDateUnit", e);
      return null;
    }
  }

  ////////////////////////////////////////////////////////////////////////
  //private double value;
  private String orgText, udunitString;

  // private TimeUnit timeUnit = null;
  private Unit uu;
  private Converter converter = null;
  private Converter converterInverse = null;


  /**
   * Constructor.
   * @param text udunits String, eg 3 secs since 1991-01-01T03:12
   * @throws Exception if malformed String.
   */
  public DateUnit(String text) throws Exception {
    this.orgText = text;
    UnitFormat format = UnitFormatManager.instance();
    uu = format.parse( text);
    udunitString = uu.getCanonicalString();

    /* String timeUnitString;

    text = text.trim();
    StringTokenizer stoker = new StringTokenizer(text);
    String firstToke = stoker.nextToken();
    try {
      this.value = Double.parseDouble(firstToke);
      //if (this.value == 0.0)
      //  this.value = 1.0;
      this.udunitString = text.substring( firstToke.length()); // eliminate the value if there is one
      timeUnitString = stoker.nextToken();

    } catch (NumberFormatException e) { // stupid way to test if it starts with a number
      this.value = 0.0;
      this.udunitString = text;
      timeUnitString = firstToke;
    }

    uu = SimpleUnit.makeUnit( udunitString); // always a base unit */
  }

  private Converter getConverter() {
   if (converter == null)  {
     try {
       Unit ref = SimpleUnit.dateReferenceUnit;
       converter = uu.getConverterTo(ref);
     } catch (Exception e) {
       e.printStackTrace();
     }
   }
   return converter;
 }

  private Converter getConverterInverse() {
   if (converterInverse == null)  {
     try {
       Unit ref = SimpleUnit.dateReferenceUnit;
       converterInverse = ref.getConverterTo(uu);
     } catch (Exception e) {
       e.printStackTrace();
     }
   }
   return converterInverse;
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

  /** Get the equivilent java.util.Date.
   * @return Date or null if failure
   */
  public Date getDate() {
    return new Date( (long) getConverter().convert(1.0));
  }

  /** Create a Date from this base unit and the given value.
   * @param val value in the units of this base unit, eg sec since base date
   * @return Date .
   */
  public Date makeDate(double val) {
    return new Date( (long) getConverter().convert(val));

    //double secs = timeUnit.getValueInSeconds(val); //
    //return new Date( getDateOrigin().getTime() + (long)(1000*secs));
  }

 /** Create the equivilent value from this base unit and the given Date.
  * Inverse of makeDate.
   * @param date to convert.
   * @return value in units of this base unit.
   */
  public double makeValue(Date date) {
    return (double) getConverterInverse().convert(date.getTime());
  }

  /** Make a standard GMT string representation from this unit and given value.
   * @param value of time in these units.
   * @return String or null if not time unit.
   */
  public String makeStandardDateString(double value) {
    Date date = makeDate( value);
    if (date == null) return null;
    DateFormatter formatter = new DateFormatter();
    return formatter.toDateTimeStringISO(date);
  }

  public String toString() {
    return orgText;
  }

  /**
   * The udunits string, but no value, ie its a base unit.
   * @return the udunits base string
   */
  public String getUnitsString() {
    return udunitString;
  }

  // testing
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
    Date d = new Date( (long) val);
    System.out.printf(" val=%f date=%s (%s)%n", 1.0, d, formatter.toDateTimeStringISO(d));

    double val2 = converter.convert(2.0);
    Date d2 = new Date( (long) val2);
    System.out.printf(" val=%f date=%s (%s)%n", 2.0, d, formatter.toDateTimeStringISO(d2));

  }

}
