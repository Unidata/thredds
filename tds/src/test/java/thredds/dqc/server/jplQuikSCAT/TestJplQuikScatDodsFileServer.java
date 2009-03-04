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
package thredds.dqc.server.jplQuikSCAT;

import junit.framework.*;

import java.util.*;

/**
 * A description
 *
 * User: edavis
 * Date: Jan 29, 2004
 * Time: 11:51:39 AM
 */
public class TestJplQuikScatDodsFileServer extends TestCase
{
  static private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( TestJplQuikScatDodsFileServer.class );

  private String name1;

  private String name2;

  private JplQuikScatDodsFileServer me;

  private JplQuikScatCalendar calendar = null;
  private String epochStartDateTimeString = "1999-01-01T00:00:00.000GMT";

  public TestJplQuikScatDodsFileServer( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    logger.debug( "setUp(): starting." );

    calendar = new JplQuikScatCalendar( this.epochStartDateTimeString );

    name1 = "name 1";

    name2 = "name 2";
  }

  /** Test ... 
   */
  public void testAll()
  {
    this.setupMe();
    assertTrue( me != null );

//    assertTrue( me.getName().equals( name1 ) );
//    me.setName( name2 );
//    assertTrue( me.getName().equals( name2 ) );

    Calendar cal = Calendar.getInstance( TimeZone.getTimeZone("GMT"), Locale.US);
    String testDateString = null;
    Date testDate = null;
    String expectDateString = null;
    Date expectDate = null;
    String tmpMsg = null;

    JplQuikScatUserQuery request = new JplQuikScatUserQuery( this.calendar );

    // Test getDateFromDfsDateString()
    cal.set( 2002, 9, 14);
    cal.set( Calendar.HOUR_OF_DAY, 0);
    cal.set( Calendar.MINUTE, 0);
    cal.set( Calendar.SECOND, 0);
    cal.set( Calendar.MILLISECOND, 0);
    expectDate = cal.getTime();

    testDateString = "2002/10/14";
    testDate = this.me.getDateFromDfsDateString( testDateString);
    tmpMsg = "testAll(): testing equality of date <" + testDate + "> calculated from" +
            " getDateFromDfsDateString(\"" + testDateString + "\") and expected date <" + expectDate + ">.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, expectDate.equals( testDate));

    // Test getDateFromDfsDateTimeString()
    cal.clear();
    cal.set( 2002, 9, 14, 2, 32, 20);
    cal.set( Calendar.MILLISECOND, 0);
    expectDate = cal.getTime();

    testDateString = "2002/10/14:02:32:20 GMT";
    testDate = this.me.getDateFromDfsDateTimeString( testDateString);

    tmpMsg = "testAll(): testing equality of date <" + testDate + "> calculated from" +
            " getDateFromDfsDateTimeString(\"" + testDateString + "\") and expected date <" + expectDate + ">.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, expectDate.equals( testDate));

    // Test getDfsDateTimeFunctionParameterStringFromDate()
    cal.clear();
    cal.set( 2002, 9, 14, 2, 32, 20);
    cal.set( Calendar.MILLISECOND, 0);
    testDate = cal.getTime();

    expectDateString = "2002-10-14:02:32";
    testDateString = this.me.getDfsDateTimeFunctionParameterStringFromDate( testDate);

    tmpMsg = "testAll(): testing equality of date string <" + testDateString + "> calculated from" +
            " getDfsDateTimeFunctionParameterStringFromDate(\"" + testDate + "\") and" +
            " expected date string <" + expectDateString + ">.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, expectDateString.equals( testDateString));

    String reqDateRangeMinString = "2002/02/14:00:00:00 GMT";
    String reqDateRangeMaxString = "2002/02/14:15:00:00 GMT";

    Date reqDateRangeMin = this.me.getDateFromDfsDateTimeString( reqDateRangeMinString);
    Date reqDateRangeMax = this.me.getDateFromDfsDateTimeString( reqDateRangeMaxString);

    double reqLongRangeMin = 300.0;
    double reqLongRangeMax = 350.0;

    String ce = null;     // The anticipated CE (built here for each test).
    String testCE = null; // The CE to be tested (build by JplQuikScatDodsFileServer.buildConstraintExpression()).

    String ceSelDate = "date_time(\"" + this.me.getDfsDateTimeFunctionParameterStringFromDate( reqDateRangeMin) + "\"," +
            "\"" +  this.me.getDfsDateTimeFunctionParameterStringFromDate( reqDateRangeMax) + "\")";
    String ceSelLong = this.me.catSeqName + ".longitude>" + reqLongRangeMin + "&" +
            this.me.catSeqName + ".longitude<" + reqLongRangeMax;

    // Test with all parameters (date and longitude range) valid.
    ce = "&" + ceSelDate + "&" + ceSelLong;
    request.setDateRange( this.calendar.getSecondsSinceEpochFromDate( reqDateRangeMin),
                          this.calendar.getSecondsSinceEpochFromDate( reqDateRangeMax) );
    request.setLongitudeRange( reqLongRangeMin, reqLongRangeMax);
    testCE = this.me.buildConstraintExpression( request );
    tmpMsg = "testAll(): testing match of constructed CE <" + testCE + "> " +
                "and the expected CE <" + ce + ">.";
    logger.debug( tmpMsg );
    assertTrue( tmpMsg, ce.equals( testCE));

    // Test with valid longitude range but null date range.
    ce = "&" + ceSelLong;
    request.setDateRange( null, null );
    request.setLongitudeRange( reqLongRangeMin, reqLongRangeMax);
    testCE = this.me.buildConstraintExpression( request );
    tmpMsg = "testAll(): testing match of constructed CE with null date range <" + testCE + "> " +
            "and the expected CE <" + ce + ">.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, ce.equals( testCE));

    // Test with valid date range but longitude range with no extent.
    ce = "&" + ceSelDate;
    request.setDateRange( this.calendar.getSecondsSinceEpochFromDate( reqDateRangeMin),
                          this.calendar.getSecondsSinceEpochFromDate( reqDateRangeMax) );
    request.setLongitudeRange( 0.0, 0.0 );
    testCE = this.me.buildConstraintExpression( request );
    tmpMsg = "testAll(): testing match of constructed CE with 0 degree longitude range <" + testCE + "> " +
            "and the expected CE <" + ce + ">.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, ce.equals( testCE));

    // Test with valid date range but longitude range with full extent.
    ce = "&" + ceSelDate;
    request.setDateRange( this.calendar.getSecondsSinceEpochFromDate( reqDateRangeMin),
                          this.calendar.getSecondsSinceEpochFromDate( reqDateRangeMax) );
    request.setLongitudeRange( 0.0, 360.0 );
    testCE = this.me.buildConstraintExpression( request );
    tmpMsg = "testAll(): testing match of constructed CE with 360 degree longitude range <" + testCE + "> " +
            "and the expected CE <" + ce + ">.";
    logger.debug( tmpMsg);

    // Test findMatchingCatalogEntries()
    cal.clear();
    cal.set( 2002, 9, 14);
    cal.set( Calendar.HOUR_OF_DAY, 0);
    cal.set( Calendar.MINUTE, 0);
    cal.set( Calendar.SECOND, 0);
    cal.set( Calendar.MILLISECOND, 0);
    reqDateRangeMin = cal.getTime(); // 14 Oct 2002 00:00 GMT
    cal.set( Calendar.HOUR_OF_DAY, 12);
    reqDateRangeMax = cal.getTime(); // 14 Oct 2002 12:00 GMT

    reqLongRangeMin = 300.0;
    reqLongRangeMax = 355.0;

    request.setDateRange( this.calendar.getSecondsSinceEpochFromDate( reqDateRangeMin),
                          this.calendar.getSecondsSinceEpochFromDate( reqDateRangeMax) );
    request.setLongitudeRange( reqLongRangeMin, reqLongRangeMax );
    logger.debug( "testAll(): ce <" + ce + ">");
    Iterator it = null;
    try
    {
      it = this.me.findMatchingCatalogEntries( request);
    }
    catch (Exception e)
    {
      tmpMsg = "testAll(): exception thrown on call to" +
              " findMatchingCatalogEntries( \"" + ce + "\"): " + e.getMessage();
      logger.debug( tmpMsg);
      assertTrue( tmpMsg, false);
    }

    tmpMsg = "testAll(): Iterator is empty.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, it.hasNext());

    // Test the get methods for the catalog entry values.
    Date curEntryDate = null;
    String curEntryDodsUrl = null;
    float curEntryLong = 0.0F;
    int curEntryRevOrRow = 0;

    JplQuikScatEntry entry = null;
    entry = (JplQuikScatEntry) it.next();

    // Test the Date for this catalog entry.
    curEntryDate = entry.getDate();
    cal.clear();
    cal.set( 2002, 9, 14);
    cal.set( Calendar.HOUR_OF_DAY, 6);
    cal.set( Calendar.MINUTE, 21);
    cal.set( Calendar.SECOND, 26);
    cal.set( Calendar.MILLISECOND, 110);
    expectDate = cal.getTime();
    tmpMsg = "testAll(): testing equality of catalog entry date <" + curEntryDate.toString() + "-" + curEntryDate.getTime() +
            "> and the expected date <" + expectDate.toString() + "-" + expectDate.getTime() + ">.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, curEntryDate.equals( expectDate));

    // Test the OPeNDAP URL for this catalog entry.
    curEntryDodsUrl = entry.getDodsUrl();
    String expectDodsUrl = "http://dods.jpl.nasa.gov/cgi-bin/nph-dods/pub/ocean_wind/quikscat/L2B/data/2002/287/QS_S2B17284.20022871309.Z";
    tmpMsg = "testAll(): testing equality of catalog entry OPeNDAP URL <" + curEntryDodsUrl +
            "> and the expected OPeNDAP URL <" + expectDodsUrl + ">.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, curEntryDodsUrl.equals( expectDodsUrl));

    // Test the longitude for this catalog entry.
    curEntryLong = entry.getLongitude();
    float expectLong = 353.0928F;
    tmpMsg = "testAll(): testing equality of catalog entry longitude <" + curEntryLong +
            "> and the expected longitude <" + expectLong + ">.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, curEntryLong == expectLong);

    // Test the rev number for this catalog entry.
    curEntryRevOrRow = entry.getRevNum();
    int expectRevOrRow = 17284;
    tmpMsg = "testAll(): testing equality of catalog entry rev number <" + curEntryRevOrRow +
            "> and the expected rev number <" + expectRevOrRow + ">.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, curEntryRevOrRow == expectRevOrRow);

    // Test the WVC rows for this catalog entry.
    curEntryRevOrRow = entry.getWvcRows();
    expectRevOrRow = 1624;
    tmpMsg = "testAll(): testing equality of catalog entry WVC rows <" + curEntryRevOrRow +
            "> and the expected WVC rows <" + expectRevOrRow + ">.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, curEntryRevOrRow == expectRevOrRow);

//    System.out.println( "Dataset 1: date <" + entry.getDate() + ">, DODS Url <" + entry.getDodsUrl() + ">, " +
//                        "long <" + entry.getLongitude() + ">, rev# <" + entry.getRevNum() + ">, and " +
//                        "wvcRows <" + entry.getWvcRows() + ">.");

    // Test the iterator for the second entry for this CE.
    tmpMsg = "testAll(): testing that iterator contains second element.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, it.hasNext());
    entry = (JplQuikScatEntry) it.next();

    // Test the iterator for the third (and final) entry for this CE.
    tmpMsg = "testAll(): testing that iterator contains third (and final) element.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, it.hasNext());
    entry = (JplQuikScatEntry) it.next();

    // Test that iterator does not contain a fourth entry for this CE.
    tmpMsg = "testAll(): testing that iterator does not contain a fourth element.";
    logger.debug( tmpMsg);
    assertTrue( tmpMsg, ! it.hasNext());

  }

  private void setupMe()
  {
    try
    {
      logger.debug( "setupMe(): calling JplQuikScatDodsFileServer()." );
      me = new JplQuikScatDodsFileServer();
    }
    catch (Exception e)
    {
      String tmpMsg = "setupMe(): JplQuikScatDodsFileServer() threw exception: " + e.getMessage();
      logger.debug( tmpMsg);
      assertTrue( tmpMsg, false);
    }
  }
}