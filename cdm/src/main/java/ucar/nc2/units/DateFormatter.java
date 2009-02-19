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

import java.util.Date;

/**
 * Date parsing and formatting. Always uses GMT.
 * These are not thread-safe.
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


  /**
   * Parse the text in W3C profile of ISO 8601 format.
   * @param text parse this text
   * @return equivalent Date or null if failure
   * @see <a href="http://www.w3.org/TR/NOTE-datetime">W3C profile of ISO 8601</a>
   */
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

  /**
   * Parse text in the format "yyyy-MM-dd HH:mm:ss"
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   */
  public Date stdDateTimeFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    stdDateTimeFormat();
    return stdDateTimeFormat.parse(text);
  }

  /**
   * Parse text in the format "yyyy-MM-dd HH:mm"
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   */
  public Date stdDateNoSecsFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    stdDateNoSecsFormat();
    return stdDateNoSecsFormat.parse(text);
  }

  /**
   * Parse text in the format "yyyy-MM-dd'T'HH:mm:ss"
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   */
  public Date isoDateTimeFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    isoDateTimeFormat();
    return isoDateTimeFormat.parse(text);
  }

  /**
   * Parse text in the format "yyyy-MM-dd'T'HH:mm"
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   */
  public Date isoDateNoSecsFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    isoDateNoSecsFormat();
    return isoDateNoSecsFormat.parse(text);
  }

  /**
   * Parse text in the format "yyyy-MM-dd"
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   */
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

  /** test */
  public static void main(String args[]) {
    test("2001-09-11T12:09:20");
    test("2001-09-11 12:10:12");
    test("2001-09-11T12:10");
    test("2001-09-11 12:01");
    test("2001-09-11");
  }


}
