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

import junit.framework.*;
import ucar.units.*;

import java.util.Date;

public class TestDate extends TestCase {
  private static final boolean debug = true, debug2 = true;
  private DateFormatter formatter = new DateFormatter();

  public TestDate( String name) {
    super(name);
  }

  void doTime2(double value, String name, boolean ok) {
    ucar.units.UnitFormat format = UnitFormatManager.instance();

    ucar.units.Unit timeUnit;
    try {
      timeUnit = format.parse("secs since 1970-01-01 00:00:00");
    } catch (Exception e) {
      System.out.println("SimpleUnit initialization failed " +e);
      return;
    }

    ucar.units.Unit uu;
    try {
      uu = format.parse(name);
    } catch (Exception e) {
      System.out.println("Parse " +name +" got Exception " +e);
      return;
    }

    System.out.println("isCompatible="+uu.isCompatible( timeUnit));

    try {
      System.out.println("convert "+uu.convertTo( value, timeUnit));
    } catch (Exception e) {
      System.out.println("convert " +name +" got Exception " +e);
      return;
    }
  }

  public void testStandardDate() {
    Date d = DateUnit.getStandardDate("25 days since 1985-02-02 00:00:00");
    System.out.println(" d="+formatter.toDateTimeStringISO(d));

    d = DateUnit.getStandardDate("1000.0 secs since 1985-02-02 12:00:00");
    System.out.println(" d="+formatter.toDateTimeStringISO(d));

    d = DateUnit.getStandardDate("1.0 secs since 1985-02-02 12:00:00");
    System.out.println(" d="+formatter.toDateTimeStringISO(d));
  }

  /**
   * Test the precision of udunits date string conversion. Example from email
   * dated 2008-05-13 from Rich Signell to the netcdf-java email list:
   *
   * <p>Subject: Re: [netcdf-java] Data precision while aggregating data</p>
   *
   * <p>http://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/2008-May/000631.html
   *
   * <p>[snip]
   *
   * <pre>
   * &lt;variable name="time" shape="time" type="double"&gt;
   *   &lt;attribute name="units" value="days since 1858-11-17 00:00:00 UTC"/&gt;
   *   &lt;attribute name="long_name" value="Modified Julian Day"/&gt;
   *   &lt;values start="47865.7916666665110000" increment="0.0416666666666667"/&gt;
   * &lt;/variable&gt;
   * </pre>
   *
   * <p>As Sachin mentioned, the start time for this file is  "05-Dec-1989
   * 19:00:00", and as proof that we have sufficient precision, when we
   * simply load the time vector in NetCDF-java and do the double precision
   * math in Matlab, we get the right start time:
   *
   * <p>datestr(datenum([1858 11 17 0 0 0]) + 47865.791666666511)
   *
   * <p>ans =  05-Dec-1989 19:00:00
   *
   * <p>but when we use the NetCDF-Java time routines to convert to Gregorian, we get
   *
   * <p>05-Dec-1989 18:59:59 GMT
   *
   * <p>[snip]
   *
   */
  public void utestStandardDatePrecision() {
    Date d = DateUnit.getStandardDate("47865.7916666665110000 days since 1858-11-17 00:00:00 UTC");
    String isoDateTimeString = formatter.toDateTimeStringISO( d );

    String expectedValue = "1989-12-05T19:00:00Z";
    assertEquals( "Calculated date string [" + isoDateTimeString + "] not as expected [" + expectedValue + "].",
                  expectedValue, isoDateTimeString );
  }

  public void testTime() {
    doTime2(1.0, "years since 1985", true);
    doTime2(1.0, "year since 1985", true);
  }

  public void testDoublePrecision() {
    double dval = 47865.7916666665110000;
    double eval = 47865.791666666664;
    System.out.println(" dval= "+dval);
    System.out.println(" eval= "+eval);
    System.out.println(" diff= "+(eval-dval));
    System.out.println(" rdiff= "+(eval-dval)/eval);
    System.out.println(" rdiff= "+(1.0-dval/eval));
    double rdiff= (eval-dval);
    System.out.println(" add= "+(rdiff+dval));
    rdiff= 1.0-dval/eval;
    System.out.println(" add= "+(rdiff+eval));
  }

  public void testStandardDatePrecision2() throws Exception {
    DateUnit du = new DateUnit("days since 1858-11-17 00:00:00 UTC");
    System.out.println(" date= "+formatter.toDateTimeStringISO(du.makeDate(0)));
    System.out.println(" dateOrigin= "+formatter.toDateTimeStringISO(du.getDateOrigin()));

    Date isoDate = formatter.getISODate( "1989-12-05T19:00:00Z" );
    System.out.println(" dateWant= "+formatter.toDateTimeStringISO(isoDate));

    double val = du.makeValue(isoDate);
    System.out.println(" days since= "+val);

    Date roundTrip = du.makeDate(val);
    System.out.println(" roundTrip= "+formatter.toDateTimeStringISO(roundTrip));
    assert roundTrip.equals(isoDate);

  }


}