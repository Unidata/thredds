// $Id: TimeDuration.java,v 1.9 2006/06/06 16:17:08 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package thredds.datatype;

import ucar.nc2.units.*;
import ucar.units.ConversionException;

/**
 * Implements the thredds "duration" XML element type: specifies a length of time.
 * This is really the same as a ucar.nc2.units.TimeUnit, but it allows xsd:duration syntax as well as udunits syntax. It
 * also keeps track if the text is empty.
 *
 * A duration can be one of the following:
 <ol>
   <li> a valid udunits string compatible with "secs"
   <li> NOT DONE YET an xsd:duration type specified in the following form "PnYnMnDTnHnMnS" where:
    <ul>
    <li>P indicates the period (required)
    <li>nY indicates the number of years
    <li>nM indicates the number of months
    <li>nD indicates the number of days
    <li>T indicates the start of a time section (required if you are going to specify hours, minutes, or seconds)
    <li>nH indicates the number of hours
    <li>nM indicates the number of minutes
    <li>nS indicates the number of seconds
  </ul>
 </ol>
 *
 * @see "http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/InvCatalogSpec.html#durationType"
 * @author john caron
 * @version $Revision: 1.9 $ $Date: 2006/06/06 16:17:08 $
 */


public class TimeDuration {
  private static TimeUnit secUnits;
  static {
    try {
      secUnits = new TimeUnit("1 sec");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String text;
  private TimeUnit timeUnit;
  private boolean isBlank;

  private boolean debug = false;

  /** copy constructor */
  public TimeDuration( TimeDuration src) {
    text = src.getText();
    timeUnit = new TimeUnit( src.getTimeUnit());
    isBlank = src.isBlank();
  }

  public TimeDuration( TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
    this.text = timeUnit.toString();
  }

  public TimeDuration(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    this.text = text;

    // see if its blank
    if (text.length() == 0) {
      isBlank = true;
      try { timeUnit = new TimeUnit("1 sec"); } catch (Exception e) {}
      return;
    }

    // LOOK need xsd:duration parsing

    // see if its a udunits string
    try {
      timeUnit = new TimeUnit(text);
      if (debug) System.out.println(" set time unit= "+timeUnit);
    }
    catch (Exception e) {
      throw new java.text.ParseException(e.getMessage(), 0);
    }

  }

  /** get the duration in natural units */
  public double getValue() {
    return timeUnit.getValue();
  }

  /** get the duration in want units */
  public double getValue(TimeUnit want) throws ConversionException {
    return timeUnit.convertTo(timeUnit.getValue(), want);
  }

  /** get the duration in seconds */
  public double getValueInSeconds() {
    return timeUnit.getValueInSeconds();
  }

  /** set the duration in seconds */
  public void setValueInSeconds( double secs) {
    timeUnit.setValueInSeconds( secs);
    text = null;
  }


  public boolean isBlank() {
    return isBlank;
  }

  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  public String getText() {
    return text == null ? timeUnit.toString() : text;
  }

  public String toString() {
    return getText();
  }

  public int hashCode() {
    return isBlank() ? 0 : timeUnit.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (! (o instanceof TimeDuration))
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

  /** debug */
  public static void main(String[] args) {
    doDuration("3 days");
  }
}