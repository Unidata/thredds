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
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ucar.nc2.units.DateFormatter;

import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private static DateTimeFormatter isof = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC();
  private static DateTimeFormatter isof_with_millis_of_second = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZoneUTC();
  private static DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss'Z'").withZoneUTC();
  
  private static DateTimeFormatter dtf_with_millis_of_second = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'").withZoneUTC();
  private static DateTimeFormatter df = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC();

  static public String toDateTimeStringISO(CalendarDate cd) {
	
	 if(cd.getDateTime().getMillisOfSecond() == 0)
		 return isof.print( cd.getDateTime() );
	 else
		 return isof_with_millis_of_second.print(cd.getDateTime());
	 
  }

  static public String toDateTimeStringISO(Date d) {
	  return toDateTimeStringISO( CalendarDate.of(d) );
//	DateTime dt = new DateTime(d, DateTimeZone.UTC);
//	if( dt.getMillisOfSecond() == 0 )
//		return isof.print( dt);
//	else
//		return isof_with_millis_of_second.print(dt);
  }

  static public String toDateTimeString(CalendarDate cd) {

	  if(cd.getDateTime().getMillisOfSecond()==0)	  
		  return dtf.print(cd.getDateTime());
	  else
		  return dtf_with_millis_of_second.print(cd.getDateTime());
  }
  
  static public String toDateTimeString(Date date) {
	    return toDateTimeString(CalendarDate.of(date));
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


    
  /////////////////////////////////////////////////////////////////////////////
  // reading an ISO formatted date

  /**
   * Old version using DateFormatter
   * @param iso ISO 8601 date String
   * @return equivilent Date
   * 
   * @deprecated As of 4.3.10 use {@link #isoStringToDate(String)} instead
   *     
   */
  @Deprecated
  static public Date parseISODate(String iso) {
    DateFormatter df = new DateFormatter();
    return df.getISODate(iso);
  }
  
  /**
   * Convert an ISO formatted String to a CalendarDate.
   * @param calt calendar, may be null for default calendar (Calendar.getDefault())
   * @param iso ISO 8601 date String
     <pre>possible forms for W3C profile of ISO 8601
        Year:
         YYYY (eg 1997)
      Year and month:
         YYYY-MM (eg 1997-07)
      Complete date:
         YYYY-MM-DD (eg 1997-07-16)
      Complete date plus hours and minutes:
         YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
      Complete date plus hours, minutes and seconds:
         YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
      Complete date plus hours, minutes, seconds and a decimal fraction of a second
         YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)

   where:
        YYYY = four-digit year
        MM   = two-digit month (01=January, etc.)
        DD   = two-digit day of month (01 through 31)
        hh   = two digits of hour (00 through 23) (am/pm NOT allowed)
        mm   = two digits of minute (00 through 59)
        ss   = two digits of second (00 through 59)
        s    = one or more digits representing a decimal fraction of a second
        TZD  = time zone designator (Z or +hh:mm or -hh:mm)
   except:
       You may use a space instead of the 'T'
       The year may be preceeded by a '+' (ignored) or a '-' (makes the date BCE)
       The date part uses a '-' delimiter instead of a fixed number of digits for each field
       The time part uses a ':' delimiter instead of a fixed number of digits for each field
   </pre>
   * @return CalendarDate using given calendar
   * @throws IllegalArgumentException if the String is not a valid ISO 8601 date
   * @see "http://www.w3.org/TR/NOTE-datetime"
   */
  static public CalendarDate isoStringToCalendarDate(Calendar calt, String iso) throws IllegalArgumentException{
	  DateTime dt = parseIsoTimeString(calt, iso);
	  return new CalendarDate(calt, dt);
  }

  /**
   * Does not handle non-standard Calendars
   * @param iso iso formatted string
   * @return Date
   * @throws IllegalArgumentException
   * @deprecated use isoStringToCalendarDate
   */
  static public Date isoStringToDate(String iso) throws IllegalArgumentException {
    CalendarDate dt = isoStringToCalendarDate(null, iso);
	  return dt.toDate();
  }

  //                                                   1                  2            3
  static public final String isodatePatternString = "([\\+\\-\\d]+)([ t]([\\.\\:\\d]*)([ \\+\\-]\\S*)?z?)?$"; // public for testing
  // private static final String isodatePatternString = "([\\+\\-\\d]+)[ Tt]([\\.\\:\\d]*)([ \\+\\-]\\S*)?z?)?$";
  private static final Pattern isodatePattern = Pattern.compile(isodatePatternString);

  private static DateTime parseIsoTimeString(Calendar calt, String iso) {
    iso = iso.trim();
    iso = iso.toLowerCase();

    Matcher m = isodatePattern.matcher(iso);
    if (!m.matches()) {
      //System.out.printf("'%s' does not match regexp '%s'%n", dateUnitString, udunitPatternString);
      throw new IllegalArgumentException(iso + " does not match " + isodatePatternString);
    }

    String dateString = m.group(1);
    String timeString = m.group(3);
    String zoneString = m.group(4);

    // Set the defaults for any values that are not specified
    int year = 0;
    int month = 1;
    int day = 1;
    int hour = 0;
    int minute = 0;
    double second = 0.0;

    try {
      boolean isMinus = false;
      if (dateString.startsWith("-")) {
         isMinus = true;
         dateString = dateString.substring(1);
       } else if (dateString.startsWith("+")) {
         dateString = dateString.substring(1);
       }

      StringTokenizer dateTokenizer = new StringTokenizer(dateString, "-");
      if (dateTokenizer.hasMoreTokens()) year = Integer.parseInt(dateTokenizer.nextToken());
      if (dateTokenizer.hasMoreTokens()) month = Integer.parseInt(dateTokenizer.nextToken());
      if (dateTokenizer.hasMoreTokens()) day = Integer.parseInt(dateTokenizer.nextToken());

      // Parse the time if present
      if (timeString != null && timeString.length() > 0) {
        StringTokenizer timeTokenizer = new StringTokenizer(timeString, ":");
        if (timeTokenizer.hasMoreTokens()) hour = Integer.parseInt(timeTokenizer.nextToken());
        if (timeTokenizer.hasMoreTokens()) minute = Integer.parseInt(timeTokenizer.nextToken());
        if (timeTokenizer.hasMoreTokens()) second = Double.parseDouble(timeTokenizer.nextToken());
      }

      if (isMinus) year = -year;

      // Get a DateTime object in this Chronology
      Chronology cron = Calendar.getChronology(calt);
      cron = cron.withUTC(); // default is UTC
      DateTime dt = new DateTime(year, month, day, hour, minute, 0, 0, cron);

      // Add the seconds
      dt = dt.plus((long) (1000 * second));

      // Parse the time zone if present
      if (zoneString != null) {
        zoneString = zoneString.trim();
        if (zoneString.length() > 0 && !zoneString.equalsIgnoreCase("Z") && !zoneString.equalsIgnoreCase("UTC") && !zoneString.equalsIgnoreCase("GMT")) {
          isMinus = false;
          if (zoneString.startsWith("-")) {
             isMinus = true;
             zoneString = zoneString.substring(1);
           } else if (zoneString.startsWith("+")) {
             zoneString = zoneString.substring(1);
           }

          // allow 01:00, 1:00, 01 or 0100
          int hourOffset = 0;
          int minuteOffset = 0;
          int posColon = zoneString.indexOf(':');
          if (posColon > 0) {
            String hourS = zoneString.substring(0,posColon);
            String minS = zoneString.substring(posColon+1);
            hourOffset = Integer.parseInt(hourS);
            minuteOffset = Integer.parseInt(minS);

          } else {   // no colon - assume 2 digit hour, optional minutes
            if (zoneString.length() > 2) {
              String hourS = zoneString.substring(0,2);
              String minS = zoneString.substring(2);
              hourOffset = Integer.parseInt(hourS);
              minuteOffset = Integer.parseInt(minS);
            } else {
              hourOffset = Integer.parseInt(zoneString);
            }
          }
          if (isMinus) {
            // DateTimeZone.forOffsetHoursMinutes: "If constructed with the values (-2, 30), the resulting zone is '-02:30'.
            // so i guess dont make minuteOffset negetive
            hourOffset = -hourOffset;
          }
          DateTimeZone dtz = DateTimeZone.forOffsetHoursMinutes(hourOffset, minuteOffset);

          // Apply the time zone offset, retaining the field values.  This
          // manipulates the millisecond instance.
          dt = dt.withZoneRetainFields(dtz);
          // Now convert to the UTC time zone, retaining the millisecond instant
          dt = dt.withZone(DateTimeZone.UTC);
        } //else {
          //dt = dt.withZone(DateTimeZone.UTC);   // default UTC
        //}

      //} else {
      //  dt = dt.withZone(DateTimeZone.UTC);   // default UTC
      }

      return dt;
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Illegal base time specification: '" + dateString+"' "+e.getMessage());
    }
  }

  /////////////////////////////////////////////
  private final DateTimeFormatter dflocal;

  /**
   * Date formatter with specified pattern.
   * NOTE: we are using jodatime patterns right now, but may switch to jsr-310 when thats available in java 8.
   *  Not sure whether these patterns will still work then, so use this formatter at the risk of having to
   *  change it eventually. OTOH, its likely that the same functionality will be present in jsr-310.
   * <p>
   * The pattern syntax is mostly compatible with java.text.SimpleDateFormat -
   * time zone names cannot be parsed and a few more symbols are supported.
   * All ASCII letters are reserved as pattern letters, which are defined as follows:
   * </p>
   * <pre>
   * Symbol  Meaning                      Presentation  Examples
   * ------  -------                      ------------  -------
   * G       era                          text          AD
   * C       century of era (&gt;=0)         number        20
   * Y       year of era (&gt;=0)            year          1996
   *
   * x       weekyear                     year          1996
   * w       week of weekyear             number        27
   * e       day of week                  number        2
   * E       day of week                  text          Tuesday; Tue
   *
   * y       year                         year          1996
   * D       day of year                  number        189
   * M       month of year                month         July; Jul; 07
   * d       day of month                 number        10
   *
   * a       halfday of day               text          PM
   * K       hour of halfday (0~11)       number        0
   * h       clockhour of halfday (1~12)  number        12
   *
   * H       hour of day (0~23)           number        0
   * k       clockhour of day (1~24)      number        24
   * m       minute of hour               number        30
   * s       second of minute             number        55
   * S       fraction of second           number        978
   *
   * z       time zone                    text          Pacific Standard Time; PST
   * Z       time zone offset/id          zone          -0800; -08:00; America/Los_Angeles
   *
   * '       escape for text              delimiter
   * ''      single quote                 literal       '
   * </pre>
   */
  public CalendarDateFormatter(String pattern) {
    dflocal = DateTimeFormat.forPattern(pattern).withZoneUTC();
  }

  public CalendarDateFormatter(String pattern, CalendarTimeZone tz, Calendar cal) {
    Chronology chron = Calendar.getChronology(cal);
    dflocal = DateTimeFormat.forPattern(pattern).withChronology(chron).withZone(tz.getJodaTimeZone());
  }

  public CalendarDateFormatter(String pattern, CalendarTimeZone tz) {
    dflocal = DateTimeFormat.forPattern(pattern).withZone( tz.getJodaTimeZone());
  }

  public String toString(CalendarDate cd) {
    return dflocal.print(cd.getDateTime());
  }

  public CalendarDate parse(String timeString) {
    DateTime dt = dflocal.parseDateTime(timeString);
    Calendar cal = Calendar.get(dt.getChronology().toString());
    return new CalendarDate(cal, dt);
  }

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
