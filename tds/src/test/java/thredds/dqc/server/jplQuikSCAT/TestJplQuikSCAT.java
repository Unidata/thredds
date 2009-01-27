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
// $Id: TestJplQuikSCAT.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server.jplQuikSCAT;

import junit.framework.TestCase;
import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.query.DqcFactory;
import thredds.catalog.query.QueryCapability;
import thredds.dqc.server.DqcHandler;
import thredds.dqc.server.DqcServletConfigItem;

import java.io.IOException;

/**
 * A description
 *
 * User: edavis
 * Date: Jan 2, 2004
 * Time: 11:26:49 PM
 */
public class TestJplQuikSCAT extends TestCase
{
  static private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( TestJplQuikSCAT.class );

  private JplQuikSCAT me;

  private String catTitle = "NASA Jet Propulsion Lab, Global Level-2B QuikSCAT Archive";
  private String catStartDateString = "1999/7/19";
  private String catEndDateString = "2003/12/31";

  private String handlerName = "jplQuikSCAT";
  private String handlerDescription = "Handles requests for JPL QuikSCAT data.";
  private String handlerClassName = "thredds.dqc.server.jplQuikSCAT.JplQuikSCAT";
  private String handlerConfigFileName = null;
  private String handlerConfigFilePath = null;

  private DqcServletConfigItem handlerInfo = null;


  public TestJplQuikSCAT( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    String tmpMsg = null;

    logger.debug( "setUp(): starting." );

    handlerInfo = new DqcServletConfigItem( handlerName, handlerDescription,
                                            handlerClassName, handlerConfigFileName );

    try
    {
      this.me = (JplQuikSCAT) DqcHandler.factory( handlerInfo, handlerConfigFilePath );
    }
    catch ( thredds.dqc.server.DqcHandlerInstantiationException e)
    {
      tmpMsg = "Unexpected DqcHandlerInstantiationException from DqcHandler.factory("
               + handlerInfo.toString() + "," + handlerConfigFilePath + "): " + e.getMessage();
      logger.debug( "setup(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    catch ( IOException e )
    {
      tmpMsg = "Unexpected IOException from DqcHandler.factory("
               + handlerInfo.toString() + "," + handlerConfigFilePath + "): " + e.getMessage();
      logger.debug( "setup(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
  }

  /** Test ... 
   */
  public void testAll()
  {
    String tmpMsg = null;
    // Create the DQC document for this handler.
    QueryCapability dqc = this.me.createDqcDocument( "http://dods.jpl.nasa.gov/dqcServlet/quikscat" );
    DqcFactory dqcFactory = new DqcFactory( false );
    String dqcAsString = null;
    try
    {
      dqcAsString = dqcFactory.writeXML( dqc );
    }
    catch ( IOException e )
    {
      tmpMsg = "Unexpected IOException from DqcFactory.writeXML(): " + e.getMessage();
      logger.debug( "testAll(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    System.out.println( dqcAsString );

    InvCatalog resultCat = null;
    try
    {
      resultCat = this.me.buildCatalogFromRequest( "147680000.0", "157680000.0", "45.0", "75.0");
    }
    catch (Exception e)
    {
      tmpMsg = "Exception thrown on call to" +
              " buildCatalogFromRequest( \"147680000.0\", \"157680000.0\", \"45.0\", \"75.0\"): " + e.getMessage();
      logger.debug( "testAll(): " + tmpMsg);
      assertTrue( tmpMsg, false);
    }
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( true );
    String catalogAsString = null;
    try
    {
      catalogAsString = fac.writeXML_1_0( (InvCatalogImpl) resultCat );
    }
    catch ( IOException e )
    {
      tmpMsg = "Exception thrown while writing catalog to string: " + e.getMessage();
      logger.debug( "testAll(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    System.out.println( catalogAsString );
  }


//  public void testGetAttInfo()
//  {
//    logger.debug( "testGetAttInfo(): starting." );
//
//    logger.debug( "testGetAttInfo(): catalog title <" + this.me.getCatalogTitle() + ">.");
//    assertTrue( "testGetAttInfo(): catalog title <" + this.me.getCatalogTitle() + ">.",
//                this.me.getCatalogTitle().equals( this.catTitle)); // ???
//    logger.debug( "testGetAttInfo(): catalog start date <" + this.me.getAllowedDateRangeStartString() + ">.");
//    assertTrue( "testGetAttInfo(): catalog start date <" + this.me.getAllowedDateRangeStartString() + ">.",
//                this.me.getAllowedDateRangeStartString().equals( this.catStartDateString)); // ???
//    logger.debug( "testGetAttInfo(): catalog end date <" + this.me.getAllowedDateRangeEndString() + ">.");
//    assertTrue( "testGetAttInfo(): catalog end date <" + this.me.getAllowedDateRangeEndString() + ">.",
//                this.me.getAllowedDateRangeEndString().equals( this.catEndDateString)); // ???
//  }
//
//  public void testGetConstraintExpression()
//  {
//    logger.debug( "testGetConstraintExpression(): starting." );
//
//    String startDate = "1999/08/01";
//    String endDate = "1999/08/02";
//    double startDateSecSinceEpoch = this.me.getSecondsSinceEpochFromDfsYearMonthDayString( startDate);
//    double endDateSecSinceEpoch = this.me.getSecondsSinceEpochFromDfsYearMonthDayString( endDate);
//    String startDateSecSinceEpochString = Double.toString( startDateSecSinceEpoch);
//    String endDateSecSinceEpochString = Double.toString( endDateSecSinceEpoch);
//
//    logger.debug( "testGetConstraintExpression(): start date < " + startDate + " - " + startDateSecSinceEpoch +
//                  " - " + startDateSecSinceEpochString + " > and end date < " + endDate + " - " + endDateSecSinceEpoch +
//                  " - " + endDateSecSinceEpochString + " >.");
//    String startLong = "0.0";
//    String endLong = "2.0";
//
//    String ce = ",DODS_Date_Time(QuikSCAT_L2B)&date_time(\"" + startDateSecSinceEpochString +
//            "\",\"" + endDateSecSinceEpochString + "\")&QuikSCAT_L2B.longitude>" + startLong +
//            "&QuikSCAT_L2B.longitude<" + endLong;
//    logger.debug( "testGetConstraintExpression(): expected CE <" + ce + ">.");
//    String resultCE = null;
//    try
//    {
//      resultCE = this.me.getConstraintExpression( startDateSecSinceEpochString, endDateSecSinceEpochString,
//                                                  startLong, endLong);
//    }
//    catch (Exception e)
//    {
//      logger.debug( "testGetConstraintExpression(): exception thrown while getting CE:" + e.getMessage());
//      assertTrue( "testGetConstraintExpression(): exception thrown while getting CE:" + e.getMessage(),
//                  false);
//    }
//
//    String tmp = "testGetConstraintExpression(): " +
//            "getConstraintExpression( \"" + startDateSecSinceEpochString + "\", " +
//            "\"" + endDateSecSinceEpochString + "\", \"" + startLong + "\", \"" + endLong + "\")" +
//            " <" + resultCE + "> does not equal the desired CE <" + ce + ">.";
//    logger.debug( tmp);
//    assertTrue( tmp, resultCE.equals( ce));
//  }















//  public void testGetCatalogEntries()
//  {
//    logger.debug( "testGetCatalogEntries(): starting." );
//
//    Calendar cal = Calendar.getInstance( TimeZone.getTimeZone("GMT"), Locale.US);
//    cal.setTime( this.epochStartDate);
//    cal.add( Calendar.YEAR, 1);
//    Date start = cal.getTime();
//    cal.add( Calendar.DAY_OF_YEAR, 1);
//    Date end = cal.getTime();
//
//    logger.debug( "testGetCatalogEntries(): start date <" + start.toString() +
//                "> should be before end date <" + end.toString() + ">.");
//    assertTrue( "testGetCatalogEntries(): start date <" + start.toString() +
//                "> should be before end date <" + end.toString() + ">.",
//                start.before( end));
//
//    Iterator it = null;
//    try
//    {
//      it = this.me.getCatEntriesByTimeRange( start, end);
//    }
//    catch (Exception e)
//    {
//      logger.debug( "testGetCatalogEntries(): exception thrown while getting catalog entries by time range: " +
//                    e.getMessage());
//      assertTrue( "testGetCatalogEntries(): exception thrown while getting catalog entries by time range: " +
//                    e.getMessage(), false);
//    }
//
//    DODSStructure entry = null;
//    while ( it.hasNext())
//    {
//      entry = (DODSStructure) it.next();
//      logger.debug( "testGetCatalogEntries(): " + entry.getName());
//      Iterator it2 = entry.getVariables().iterator();
//      DODSVariable curVar = null;
//      while ( it2.hasNext())
//      {
//        curVar = (DODSVariable) it2.next();
//        logger.debug( "testGetCatalogEntries():      " + curVar.getName());
//      }
//    }
//  }
}

/*
 * $Log: TestJplQuikSCAT.java,v $
 * Revision 1.2  2006/01/23 22:11:14  edavis
 * Switch from log4j to SLF4J logging.
 *
 * Revision 1.1  2005/03/30 05:41:20  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.3  2004/08/24 23:46:08  edavis
 * Update for DqcServlet version 0.3.
 *
 * Revision 1.2  2004/08/23 16:45:18  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.1  2004/04/05 18:37:33  edavis
 * Added to and updated existing DqcServlet test suite.
 *
 * Revision 1.1  2004/01/15 19:43:07  edavis
 * Some additions to the tests.
 *
 */