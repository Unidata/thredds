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
// $Id: TestAllDqc.java 51 2006-07-12 17:13:13Z caron $

package thredds.dqc.server;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestAllDqc extends TestCase
{

  public TestAllDqc( String name )
  {
    super( name );

    // Setup logging.
    PatternLayout layout = new PatternLayout( "%d{yyyy-MM-dd HH:mm:ss.SSS} [%10r] %-5p - %c - %m%n" );
    Logger offLogger = Logger.getRootLogger();
    Logger testLogger = Logger.getLogger( "thredds.dqc" );
    try
    {
      File tmpFile = new File( "thredds.dqc.server.TestAllDqc.log" );
      FileAppender fa = new FileAppender( layout, tmpFile.toString(), false );
      testLogger.addAppender( fa );
      offLogger.setLevel( Level.toLevel( "OFF" ) );
      testLogger.setLevel( Level.toLevel( "DEUG" ) );
    }
    catch ( IOException e )
    {
      throw new RuntimeException( "Log creation got IOException", e );
    }
  }

  public static Test suite ( )
  {
    TestSuite suite= new TestSuite();

    suite.addTestSuite( thredds.dqc.server.TestDqcServletConfigItem.class);
    suite.addTestSuite( thredds.dqc.server.TestDqcServletConfig.class);
    suite.addTestSuite( thredds.dqc.server.TestDqcHandler.class);
    suite.addTestSuite( thredds.dqc.server.TestLatestModel.class);

    suite.addTestSuite( thredds.dqc.server.jplQuikSCAT.TestJplQuikScatCalendar.class);
    suite.addTestSuite( thredds.dqc.server.jplQuikSCAT.TestJplQuikScatDodsFileServer.class);
    suite.addTestSuite( thredds.dqc.server.jplQuikSCAT.TestJplQuikSCAT.class);
    return suite;
  }
}
/*
 * $Log: TestAllDqc.java,v $
 * Revision 1.1  2005/03/30 23:56:07  edavis
 * Fix tests.
 *
 * Revision 1.1  2005/03/30 05:41:20  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.6  2004/01/15 19:43:07  edavis
 * Some additions to the tests.
 *
 * Revision 1.5  2003/12/11 01:07:08  edavis
 * Turn on logging in test suite.
 *
 * Revision 1.4  2003/12/04 00:40:18  edavis
 * Added logging and replaced TestConfig.java with TestDqcServletConfig.java
 * and TestDqcServletConfigItem.java
 *
 * Revision 1.3  2003/08/29 22:23:05  edavis
 * Optimize includes.
 *
 * Revision 1.2  2003/05/06 21:43:06  edavis
 * Expand the configuration tests.
 *
 * Revision 1.1  2003/04/28 18:01:11  edavis
 * Initial checkin of THREDDS DqcServlet.
 *
 *
 */
