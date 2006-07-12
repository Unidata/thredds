// $Id$
package thredds.cataloggen;

import junit.framework.*;

/**
 * A description
 *
 * @author edavis
 * @since Mar 30, 2005T4:26:37 PM
 */
public class TestAllCatGen extends TestCase
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TestAllCatGen.class );

  public TestAllCatGen( String name )
  {
    super( name );
  }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();

    suite.addTestSuite( thredds.cataloggen.servlet.TestCatGenTimerTask.class );
    suite.addTestSuite( thredds.cataloggen.servlet.TestCatGenServletConfig.class );

    return suite;
  }
}

/*
 * $Log: TestAllCatGen.java,v $
 * Revision 1.4  2006/01/20 20:42:06  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.3  2005/12/16 23:19:38  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.2  2005/08/16 21:47:51  edavis
 * Switch from Log4j to commons logging.
 *
 * Revision 1.1  2005/03/30 23:56:07  edavis
 * Fix tests.
 *
 */