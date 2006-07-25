// $Id:DateType.java 63 2006-07-12 21:50:51Z edavis $
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

import ucar.nc2.units.TimeUnit;
import ucar.nc2.units.DateFormatter;

import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Implements the thredds "dateType" and "dateTypeFormatted" XML element types.
 * This is mostly a general way to specify dates in a string.
 * It also allows a date to mean "present".
 * It also allows an optional attribute called "type" which is an enumeration like "created", "modified", etc
 *  taken from Dublin Core vocabulary.
 *
 * A DateType can be specified in the following ways:
 * <ol>
   <li> an xsd:date, with form "CCYY-MM-DD"
   <li> an xsd:dateTime with form "CCYY-MM-DDThh:mm:ss"
   <li> a valid udunits date string
   <li> the string "present"
   </ol>
 *
 * @see "http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/InvCatalogSpec.html#dateType"
 * @see "http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/InvCatalogSpec.html#dateTypeFormatted"
 * @author john caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */

public class DateType {
  // for bean editing
  static public String hiddenProperties() { return "text blank present"; }
  // static public String editableProperties() { return "date format type"; }

  private DateFormatter formatter = null;

  private String text, format, type;
  private boolean isPresent, isBlank;
  private Date date;

  /**
   * Constructor using a java.util.Date
   * @param isPresent
   * @param date
   */
  public DateType(boolean isPresent, java.util.Date date) {
    this.isPresent = isPresent;
    this.date = date;
    if (isPresent) text = "present";
  }

  /** no argument constructor for beans */
  public DateType() {
    isBlank = true;
  }

  /** copy constructor */
  public DateType( DateType src) {
    text = src.getText();
    format = src.getFormat();
    type = src.getType();
    isPresent = src.isPresent();
    isBlank = src.isBlank();
    date = src.getDate();
    if (isPresent) text = "present";
  }

  /**
   * Constructor.
   * @param text string representation
   * @param format using java.text.SimpleDateFormat, or null
   * @param type type of date, or null
   * @throws java.text.ParseException
   */
  public DateType(String text, String format, String type) throws java.text.ParseException {

    text = (text == null) ? "" : text.trim();
    this.text = text;
    this.format = format;
    this.type = type;

    // see if its blank
    if (text.length() == 0) {
      isBlank = true;
      return;
    }

    // see if its got a format
    if (format != null) {
      SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(format);
      date = dateFormat.parse(text);
      return;
    }

    // see if its the string "present"
    isPresent = text.equalsIgnoreCase("present");
    if (isPresent) {
      date = new Date();
      return;
    }

    // see if its a udunits string
    if (text.indexOf("since") > 0) {
      date = ucar.nc2.units.DateUnit.getStandardDate(text);
      if (date == null)
        throw new java.text.ParseException("invalid udunit date unit", 0);
      return;
    }

    if (null == formatter)
      formatter = new DateFormatter();

    date = formatter.getISODate( text);
  }

  public Date getDate() { return date; }
  public void setDate( Date date) {
    this.date = date;
    this.text = toDateTimeString();
  }

  public boolean isPresent() { return isPresent; }
  public boolean isBlank() { return isBlank; }
  public String getText() {
    if (text == null) text = toDateTimeString();
    return text;
  }
  public String getFormat() { return format; }
  public String getType() { return type; }
  public void setType( String type ) { this.type = type; }

  public boolean before( Date d) {
    if (isPresent()) return false;
    return date.before( d);
  }

  public boolean after( Date d) {
    if (isPresent()) return true;
    return date.after( d);
  }

  public String toDateString() {
    if (null == formatter) formatter = new DateFormatter();
    return formatter.toDateOnlyString( date);
  }

  public String toDateTimeString() {
    if (null == formatter) formatter = new DateFormatter();
    return formatter.toDateTimeString( date);
  }

  public String toDateTimeStringISO() {
    if (null == formatter) formatter = new DateFormatter();
    return formatter.toDateTimeStringISO( date);
  }

  public String toString() { return getText(); }

  public int hashCode() {
    if (isBlank()) return 0;
    if (isPresent()) return 1;
    else if (text != null) return text.hashCode();
    else if (date != null) return date.hashCode();
    else return 0;
  }
   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof DateType)) return false;
     return o.hashCode() == this.hashCode();
  }

  private Calendar cal = null;

  public DateType add( TimeDuration d) {
    return add( d.getTimeUnit());
  }

  public DateType add( TimeUnit d) {
    Date useDate = (isPresent) ? new Date() : date;
    if (cal == null) cal = Calendar.getInstance();
    cal.setTime( useDate);
    cal.add( Calendar.SECOND, (int) d.getValueInSeconds());
    //System.out.println(" add start "+date+" add "+d.getSeconds()+" = "+cal.getTime());
    return new DateType(false, (Date) cal.getTime().clone()); // prob dont need clone LOOK
  }

  public DateType subtract( TimeDuration d) {
    return subtract( d.getTimeUnit());
  }

  public DateType subtract( TimeUnit d) {
    Date useDate = (isPresent) ? new Date() : date;
    if (cal == null) cal = Calendar.getInstance();
    cal.setTime( useDate);
    cal.add( Calendar.SECOND, (int) -d.getValueInSeconds());
    //System.out.println(" subtract start "+date+" add "+d.getSeconds()+" = "+cal.getTime());
    return new DateType(false, (Date) cal.getTime().clone());
  }

  ////////////////////////////////////////////
  // test
  private static void doOne( String s) {
    try {
      System.out.println("\nStart = (" + s + ")");
      DateType d = new DateType(s, null, null);
      System.out.println("Date = (" + d.toString() + ")");
    } catch (java.text.ParseException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    doOne("1991-11-16");
    doOne("1991-11-16T12:00:00");
    doOne("1991-11-16T16:03:09");
    doOne("5 hours since 1991-11-16");
    doOne("3600 secs since 1991-11-16");
    doOne("36000 secs since 1991-11-16 01:00:00");
    doOne("5 days since 1991-11-16");
    doOne("5 days since 1991-11-16T12:00:00");
    doOne("5 days since 1991-BADDOG!:00");
  }

}

