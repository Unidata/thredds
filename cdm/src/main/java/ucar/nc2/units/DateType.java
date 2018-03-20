/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.units;

import java.text.SimpleDateFormat;
import java.util.Date;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;

/**
 * Implements the thredds "dateType" and "dateTypeFormatted" XML element types.
 * This is mostly a general way to specify dates in a string.
 * It allows a date to mean "present". <strong>"Present" always sorts after any date, including dates in the future.</strong>
 * It allows an optional attribute called "type" which is an enumeration like "created", "modified", etc
 * taken from Dublin Core vocabulary.
 * <p/>
 * A DateType can be specified in the following ways:
 * <ol>
 * <li> an xsd:date, with form "CCYY-MM-DD"
 * <li> an xsd:dateTime with form "CCYY-MM-DDThh:mm:ss"
 * <li> a valid udunits date string
 * <li> the string "present"
 * </ol>
 *
 * @author john caron
 * @see <a href=""http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/InvCatalogSpec.html#dateType"">THREDDS dateType</a>
 */

public class DateType {
  private String text, format, type;
  private boolean isPresent, isBlank;
  private final CalendarDate date;

  /**
   * Constructor using a java.util.Date
   *
   * @param isPresent if true, this represents the "present time"
   * @param date      the given Date
   */
  public DateType(boolean isPresent, java.util.Date date) {
    this.isPresent = isPresent;
    this.date = isPresent ? null : CalendarDate.of(date);
  }

  /**
   * Constructor using a java.util.CalendarDate
   *
   * @param date the given CalendarDate
   */
  public DateType(CalendarDate date) {
    this.isPresent = false;
    this.date = date;
  }

  /**
   * no argument constructor for beans
   */
  public DateType() {
    isBlank = true;
    date = null;
  }

  /**
   * copy constructor
   *
   * @param src copy from here
   */
  public DateType(DateType src) {
    text = src.getText();
    format = src.getFormat();
    type = src.getType();
    isPresent = src.isPresent();
    isBlank = src.isBlank();
    date = src.getCalendarDate();
  }

  /**
   * Constructor.
   *
   * @param text   string representation
   * @param format using java.text.SimpleDateFormat, or null
   * @param type   type of date, or null
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
      date = null;
      return;
    }

    // see if its the string "present"
    isPresent = text.equalsIgnoreCase("present");
    if (isPresent) {
      this.date = null;
      return;
    }

    // see if its got a format
    if (format != null) {
      SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(format);
      Date d = dateFormat.parse(text);
      date = CalendarDate.of(d);
      return;
    }

    // see if its a udunits string
    if (text.indexOf("since") > 0) {
      date = CalendarDate.parseUdunits(null, text);
      if (date == null)
        throw new java.text.ParseException("invalid udunit date unit ="+text, 0);
      return;
    }

    date = CalendarDate.parseISOformat(null, text);
    if (date == null)
      throw new java.text.ParseException("invalid ISO date unit ="+text, 0);
  }
  
  
  /**
   * Constructor.
   *
   * @param text   string representation
   * @param format using java.text.SimpleDateFormat, or null
   * @param type   type of date, or null
   * @param cal2   ucar.nc2.time.Calendar of date, or null
   * @throws java.text.ParseException if error parsing text
   */
  public DateType(String text, String format, String type, ucar.nc2.time.Calendar cal2) throws java.text.ParseException {

	  if( cal2 == null) cal2 = ucar.nc2.time.Calendar.getDefault();
	  
    text = (text == null) ? "" : text.trim();
    this.text = text;
    this.format = format;
    this.type = type;

    // see if its blank
    if (text.length() == 0) {
      isBlank = true;
      date = null;
      return;
    }

    // see if its the string "present"
    isPresent = text.equalsIgnoreCase("present");
    if (isPresent) {
      date = null;
      return;
    }

    // see if its got a format
    if (format != null) {
      SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(format);    
      java.util.Calendar  c =  java.util.Calendar.getInstance();
      c.setTime(dateFormat.parse(text));      
      date = CalendarDate.of(cal2, c.getTimeInMillis());
      return;
    }

    // see if its a udunits string
    String calName = (cal2 == null) ? null : cal2.name();
    if (text.indexOf("since") > 0) {
      date = CalendarDate.parseUdunits(calName, text);
      if (date == null)
        throw new java.text.ParseException("invalid udunit date unit ="+text, 0);
      return;
    }

    date = CalendarDate.parseISOformat(calName, text);
    if (date == null)
      throw new java.text.ParseException("invalid ISO date unit ="+text, 0);
  }
  

  /**
   * Get this as a Date.
   * Does not handle non-standard Calendars.
   * @deprecated use getCalendarDate()
   * @return Date
   */
  public Date getDate() {
    return isPresent() ? new Date() : date.toDate();
  }

  /**
   * Get this as a CalendarDate
   *
   * @return CalendarDate
   */
  public CalendarDate getCalendarDate() {
    return isPresent() ? CalendarDate.present() : date;
  }

  /**
   * Does this represent the present time.
   *
   * @return true if present time.
   */
  public boolean isPresent() {
    return isPresent;
  }

  /**
   * Was blank text passed to the constructor.
   *
   * @return true if blank text passed to the constructor.
   */
  public boolean isBlank() {
    return isBlank;
  }

  /**
   * Get a text representation.
   *
   * @return text representation
   */
  public String getText() {
    if (isPresent) text = "present";
    if (text == null) text = toDateTimeString();
    return text;
  }

  /**
   * Get the SimpleDateFormat format for parsing the text.
   *
   * @return SimpleDateFormat format, or null
   */
  public String getFormat() {
    return format;
  }

  /**
   * Get the type of Date.
   *
   * @return type of Date, or null
   */
  public String getType() {
    return type;
  }

  /**
   * Set the type of Date.
   *
   * @param type type of Date
   */
  public DateType setType(String type) {
    this.type = type;
    return this;
  }

  /**
   * Is this date before the given date. if isPresent, always false.
   *
   * @param d test against this date
   * @return true if this date before the given date
   */
  public boolean before(Date d) {
    if (isPresent()) return false;
    return date.isBefore(CalendarDate.of(d));
  }

  /**
   * Is this date before the given date. if d.isPresent, always true, else if this.isPresent, false.
   *
   * @param d test against this date
   * @return true if this date before the given date
   */
  public boolean before(DateType d) {
    if (d.isPresent()) return true;
    if (isPresent()) return false;
    return date.isBefore(d.getCalendarDate());
  }

  /**
   * Is this date after the given date. if isPresent, always true.
   *
   * @param d test against this date
   * @return true if this date after the given date
   */
  public boolean after(Date d) {
    if (isPresent()) return true;
    return date.isAfter(CalendarDate.of(d));
  }

  /**
   * Same as DateFormatter.toDateOnlyString()
   *
   * @return formatted date
   */
  public String toDateString() {
    if (isPresent())
      return CalendarDateFormatter.toDateStringPresent();
    else
      return CalendarDateFormatter.toDateString(date);
  }

  /**
   * Same as CalendarDateFormatter.toDateTimeStringISO
   *
   * @return formatted date
   */
  public String toDateTimeString() {
   if (isPresent())
      return CalendarDateFormatter.toDateTimeStringISO(new Date());
    else
      return CalendarDateFormatter.toDateTimeStringISO(date);
  }

  /**
   * Get ISO formatted string
   *
   * @return ISO formatted date
   */
  public String toDateTimeStringISO() {
    return toDateTimeString();
  }

  /**
   * String representation
   *
   * @return getText()
   */
  public String toString() {
    return getText();
  }

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

  //private java.util.Calendar cal = null;

  public DateType add(TimeDuration d) {
    return add(d.getTimeUnit());
  }

  public DateType add(TimeUnit d) {
    CalendarDate useDate = getCalendarDate();
    CalendarDate result = useDate.add((int) d.getValueInSeconds(), CalendarPeriod.Field.Second);
    return new DateType(result);
  }

  public DateType subtract(TimeDuration d) {
    return subtract(d.getTimeUnit());
  }

  public DateType subtract(TimeUnit d) {
    CalendarDate useDate = getCalendarDate();
    CalendarDate result = useDate.add((int) -d.getValueInSeconds(), CalendarPeriod.Field.Second);
    return new DateType(result);
  }

  ////////////////////////////////////////////
  // test
  private static void doOne(String s) {
    try {
      System.out.println("\nString = (" + s + ")");
      DateType d = new DateType(s, null, null);
      System.out.println("DateType = (" + d.toString() + ")");
      System.out.println("Date = (" + d.getDate() + ")");
    } catch (java.text.ParseException e) {
      e.printStackTrace();
    }
  }

  /**
   * test
   */
  public static void main(String[] args) {
    doOne("T00:00:00Z");
  }

}

