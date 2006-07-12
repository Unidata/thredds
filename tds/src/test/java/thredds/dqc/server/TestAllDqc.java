// $Id$

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
