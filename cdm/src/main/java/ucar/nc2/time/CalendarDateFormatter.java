/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.time;

import net.jcip.annotations.ThreadSafe;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ucar.nc2.units.DateFormatter;

import java.text.ParseException;
import java.util.Date;

/**
 * Threadsafe static routines for date formatting.
 * Replacement for ucar.nc2.units.DateFormatter
 *
 * @author John
 * @since 7/9/11
 */
@ThreadSafe
public class CalendarDateFormatter {
  // these are thread-safe (yeah!)
  private static DateTimeFormatter isof = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZoneUTC();
  private static DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss'Z'").withZoneUTC();
  private static DateTimeFormatter df = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC();

  // DatUtil.ISO_DATE_TIME = new DateFormatHandler("yyyy-MM-dd\'T\'HH:mm:ssz");

  static public String toDateTimeStringISO(CalendarDate cd) {
    return isof.print(cd.getDateTime());
  }

  static public String toDateTimeStringISO(Date d) {
    return isof.print( new DateTime(d, DateTimeZone.UTC));
  }

  static public String toDateTimeString(CalendarDate cd) {
    return dtf.print(cd.getDateTime());
  }

  static public String toDateTimeStringPresent() {
    return dtf.print(new DateTime());
  }

  static public String toDateString(CalendarDate cd) {
    return df.print(cd.getDateTime());
  }

  static public String toDateStringPresent() {
    return df.print(new DateTime());
  }

  //////////////////////////

  static public String toDateTimeString(Date date) {
    return toDateTimeString(CalendarDate.of(date));
  }

  static public Date parseISODate(String iso) {
    DateFormatter df = new DateFormatter();
    return df.getISODate(iso);
  }
  
  /**
   *  
   * @param iso ISO 8601 date String
   * @return 
   * @throws IllegalArgumentException if the String is not a valid ISO 8601 date
   */
  static public Date isoStringToDate(String iso) throws IllegalArgumentException{
	  DateTime dt = new DateTime(iso);
	  
	  return new Date(dt.getMillis());
  }

  /*
   * Parse text in the format "yyyy-MM-dd'T'HH:mm:ss"
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   *
  public Date isoDateTimeFormat(String text) throws java.text.ParseException { */

   public static void main(String arg[]) {
     CalendarDate cd = CalendarDate.present();
     /* {"S", "M", "L", "F", "-"}
     System.out.printf("%s%n", DateTimeFormat.forStyle("SS").print(cd.getDateTime()));
     System.out.printf("%s%n", DateTimeFormat.forStyle("MM").print(cd.getDateTime()));
     System.out.printf("%s%n", DateTimeFormat.forStyle("LL").print(cd.getDateTime()));
     System.out.printf("%s%n", DateTimeFormat.forStyle("FF").print(cd.getDateTime())); */

     System.out.printf("%s%n", cd);
     System.out.printf("%s%n", toDateTimeStringISO(cd));
     System.out.printf("%s%n", toDateTimeString(cd));
     System.out.printf("%s%n", toDateString(cd));

     Date d = new Date();
     System.out.printf("%s%n", toDateTimeString(d));

     DateFormatter df = new DateFormatter();
     System.out.printf("%s%n", df.toDateTimeString(d));
     System.out.printf("%s%n", df.toDateOnlyString(d));
   }

}
