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

import java.util.Date;

/**
 * Date parsing and formatting. Always uses GMT.
 * Use one of these in each thread for thread safety.
 *
 * @author caron
 */
public class DateFormatter {

  private java.text.SimpleDateFormat isoDateTimeFormat, isoDateNoSecsFormat, stdDateTimeFormat, stdDateNoSecsFormat, dateOnlyFormat;

  private void isoDateTimeFormat() {
    if (isoDateTimeFormat != null) return;
    isoDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  private void isoDateNoSecsFormat() {
    if (isoDateNoSecsFormat != null) return;
    isoDateNoSecsFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    isoDateNoSecsFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  private void stdDateTimeFormat() {
    if (stdDateTimeFormat != null) return;
    stdDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    stdDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  private void stdDateNoSecsFormat() {
    if (stdDateNoSecsFormat != null) return;
    stdDateNoSecsFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
    stdDateNoSecsFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT")); // same as UTC
  }

  private void dateOnlyFormat() {
    if (dateOnlyFormat != null) return;
    dateOnlyFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
    dateOnlyFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  ////////////////////////////////////////////////////////////////////////////////
 

  public Date getISODate(String text) {
    Date result;

    // try "yyyy-MM-dd HH:mm:ss"
    try {
      result = stdDateTimeFormat(text);
      return result;
    } catch (java.text.ParseException e) {
    }

    // now try  "yyyy-MM-dd'T'HH:mm:ss"
    try {
      result = isoDateTimeFormat(text);
      return result;
    } catch (java.text.ParseException e) {
    }

    // now try "yyyy-MM-dd'T'HH:mm"
    try {
      result = isoDateNoSecsFormat(text);
      return result;
    } catch (java.text.ParseException e) {
    }

    // now try "yyyy-MM-dd HH:mm"
    try {
      result = stdDateNoSecsFormat(text);
      return result;
    } catch (java.text.ParseException e) {
    }

    // now try "yyyy-MM-dd"
    try {
      result = dateOnlyFormat(text);
      return result;
    } catch (java.text.ParseException e) {
    }

    return null;
  }


  public Date stdDateTimeFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    stdDateTimeFormat();
    return stdDateTimeFormat.parse(text);
  }

  public Date stdDateNoSecsFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    stdDateNoSecsFormat();
    return stdDateNoSecsFormat.parse(text);
  }

  public Date isoDateTimeFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    isoDateTimeFormat();
    return isoDateTimeFormat.parse(text);
  }

  public Date isoDateNoSecsFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    isoDateNoSecsFormat();
    return isoDateNoSecsFormat.parse(text);
  }

  public Date dateOnlyFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    dateOnlyFormat();
    return dateOnlyFormat.parse(text);
  }



  ////////////


  /** Return standard GMT date format; show date only, not time. Format = "yyyy-MM-dd"
   * @deprecated use toDateOnlyString */
  public String getStandardDateOnlyString(Date date) {
     return toDateOnlyString(date);
   }

  /** date only format= yyyy-MM-dd
   * @deprecated use toDateOnlyString */
  public String toDateString( Date date) {
    return toDateOnlyString( date);
  }

  /** date only format= yyyy-MM-dd
   * @param date format this date
   * @return date formatted as date only
   */
  public String toDateOnlyString( Date date) {
    dateOnlyFormat();
    return dateOnlyFormat.format( date);
  }

  /** Return standard formatted GMT date and time String. Format = "yyyy-MM-dd HH:mm:ss'Z'"
   *  @deprecated use toDateTimeString
   */
   public String getStandardDateString2(Date date) {
     return toDateTimeString(date);
   }

  /** "standard date format" = yyyy-MM-dd HH:mm:ssZ
   * @param date format this date
   * @return date formatted as date/time
   */
  public String toDateTimeString( Date date) {
    if (date == null) return "Unknown";
    stdDateTimeFormat();
    return stdDateTimeFormat.format( date) +"Z";
  }

  /** Return standard formatted GMT date and time String. Format = "yyyy-MM-dd'T'HH:mm:ss'Z'"
   *  @deprecated use toDateTimeStringISO
   */
   public String getStandardDateString(Date date) {
     return toDateTimeStringISO(date);
   }

  /** "ISO date format" = yyyy-MM-dd'T'HH:mm:ssZ
   *  @param date format this date
   * @return date formatted as ISO date string
   */
  public String toDateTimeStringISO( Date date) {
    isoDateTimeFormat();
    return isoDateTimeFormat.format( date) +"Z";
  }

  private static void test(String text) {
    DateFormatter formatter = new DateFormatter();
    Date date = formatter.getISODate(text);
    String text2 = formatter.toDateTimeStringISO( date);
    Date date2 = formatter.getISODate(text2);
    assert date.equals(date2);
    System.out.println(text+" == "+text2);
  }


  public static void main(String args[]) {
    test("2001-09-11T12:09:20");
    test("2001-09-11 12:10:12");
    test("2001-09-11T12:10");
    test("2001-09-11 12:01");
    test("2001-09-11");
  }


}
