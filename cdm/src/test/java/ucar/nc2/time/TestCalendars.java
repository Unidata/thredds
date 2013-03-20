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

import org.junit.Test;

/**
 * Test on non-standard Calendars
 *
 * @author caron
 * @since 11/8/11
 */
public class TestCalendars {

  @Test
  public void testEach() {
    for (Calendar cal : Calendar.values())
      testCalendar(cal, "calendar months since 1953-01-01");
    for (Calendar cal : Calendar.values())
      testCalendar(cal, "calendar years since 1953-01-01");
  }

  private void testCalendar(Calendar cal, String s) {

    CalendarDateUnit cdu;
    try {
      cdu = CalendarDateUnit.withCalendar(cal, s);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    System.out.printf("%s == %s unit (cal=%s)%n", s, cdu, cdu.getCalendar());

    CalendarDate base = null;
    for (int i = 0; i < 13; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i);
      if (base == null) base = cd;
      double diff = cd.getDifferenceInMsecs(base) * 1.0e-6;
      System.out.printf(" %d %s == %s diff = %f%n", i, cdu, CalendarDateFormatter.toDateTimeStringISO(cd), diff);
    }
    System.out.printf("%n");
  }


  /*
  double time(time) ;
      time:units = "days since 2289-12-1" ;
      time:calendar = "360_day" ;
      time:axis = "T" ;
      time:standard_name = "time" ;

      {25200.0, 46800.0}

      days since 2289-12-1
      time =
        {25200.0, 46800.0}

       2358-11-30T00:00:00.000Z
       2418-01-19T00:00:00.000Z

   */

  @Test
  public void test360bug() {
    CalendarDateUnit unit = CalendarDateUnit.withCalendar(Calendar.uniform30day, "days since 2289-12-1");
    CalendarDate cd1 = unit.makeCalendarDate(25200.0);  // 70 years =  2359-12-1
    CalendarDate cd2 = unit.makeCalendarDate(46800.0);  // 130 =       2419-12-1
    System.out.printf("%s%n", unit);
    System.out.printf("%s%n", cd1);
    System.out.printf("%s%n", cd2);

    assert cd1.toString().equals("2359-12-01T00:00:00Z");
    assert cd2.toString().equals("2419-12-01T00:00:00Z");
  }

  @Test
  public void testCalendarToDate() {
    CalendarDate cdate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.uniform30day, "1968-01-30T15:00:00.000Z");
    System.out.printf("%s%n", cdate);
    System.out.printf("%s%n", cdate.getDateTime());
    System.out.printf("%s%n", cdate.toDate());
    System.out.printf("%s%n", CalendarDateFormatter.toDateTimeStringISO(cdate.toDate()));
    System.out.printf("%s%n", CalendarDateFormatter.toDateString(cdate));

    CalendarDateFormatter cdf = new CalendarDateFormatter("yyyyMMdd");
    System.out.printf("%s%n", cdf.toString(cdate));
  }

  @Test
  public void testNemoDate() {
    for (Calendar cal : Calendar.values()) {
      CalendarDateUnit unit = CalendarDateUnit.withCalendar(cal, "days since -4713-01-01T00:00:00Z");
      CalendarDate cd1 = unit.makeCalendarDate(2434567);
      System.out.printf("%s with Calendar %s%n", cd1, cal);
    }
  }

}

