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

import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Implements the thredds "dateType" and "dateTypeFormatted" XML element types.
 * This is mostly a general way to specify dates in a string.
 * It allows a date to mean "present". <strong>"Present" always sorts after any date, including dates in the future.</strong>
 * It allows an optional attribute called "type" which is an enumeration like "created", "modified", etc
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
   * @param isPresent represents the "present time"
   * @param date the given date
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

  /** copy constructor
   * @param src copy from here
   */
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
   * @throws java.text.ParseException if error parsing text
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

    // see if its the string "present"
    isPresent = text.equalsIgnoreCase("present");
    if (isPresent) {
      return;
    }

    // see if its got a format
    if (format != null) {
      SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(format);
      date = dateFormat.parse(text);
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

  public Date getDate() {
    return isPresent() ? new Date() : date;
  }
  public void setDate( Date date) {
    this.date = date;
    this.text = toDateTimeString();
    this.isPresent = false;
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

  public boolean before( DateType d) {
    if (d.isPresent()) return true;
    if (isPresent()) return false;
    return date.before( d.getDate());
  }

  public boolean after( Date d) {
    if (isPresent()) return true;
    return date.after( d);
  }

  public String toDateString() {
    if (null == formatter) formatter = new DateFormatter();
    return formatter.toDateOnlyString( getDate());
  }

  public String toDateTimeString() {
    if (null == formatter) formatter = new DateFormatter();
    return formatter.toDateTimeString( getDate());
  }

  public String toDateTimeStringISO() {
    if (null == formatter) formatter = new DateFormatter();
    return formatter.toDateTimeStringISO( getDate());
  }

  public String toString() { return getText(); }

  public int hashCode() {
    if (isBlank()) return 0;
    if (isPresent()) return 1;
    return getDate().hashCode();
  }
  
  public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof DateType)) return false;
     DateType oo = (DateType) o;
     if (isPresent() && oo.isPresent()) return true;
     if (isBlank() && oo.isBlank()) return true;
     return oo.getDate().equals(getDate());
  }

  private Calendar cal = null;

  public DateType add( TimeDuration d) {
    return add( d.getTimeUnit());
  }

  public DateType add( TimeUnit d) {
    if (cal == null) cal = Calendar.getInstance();
    cal.setTime( getDate());
    cal.add( Calendar.SECOND, (int) d.getValueInSeconds());
    return new DateType(false, (Date) cal.getTime().clone()); // prob dont need clone LOOK
  }

  public DateType subtract( TimeDuration d) {
    return subtract( d.getTimeUnit());
  }

  public DateType subtract( TimeUnit d) {
    if (cal == null) cal = Calendar.getInstance();
    cal.setTime( getDate());
    cal.add( Calendar.SECOND, (int) -d.getValueInSeconds());
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

