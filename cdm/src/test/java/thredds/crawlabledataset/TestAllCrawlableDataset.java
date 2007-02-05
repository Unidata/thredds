// $Id: TestAllCrawlableDataset.java 61 2006-07-12 21:36:00Z edavis $
package thredds.crawlabledataset;

import junit.framework.*;

/**
 * A description
 *
 * @author edavis
 * @since Mar 30, 2005T4:26:37 PM
 */
public class TestAllCrawlableDataset extends TestCase
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( TestAllCrawlableDataset.class );

  public TestAllCrawlableDataset( String name )
  {
    super( name );
  }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTestSuite( thredds.crawlabledataset.TestCrawlableDatasetAlias.class );
    suite.addTestSuite( thredds.crawlabledataset.TestCrawlableDatasetFilter.class );
    suite.addTestSuite( thredds.crawlabledataset.filter.TestRegExpMatchOnNameFilter.class );
    suite.addTestSuite( thredds.crawlabledataset.filter.TestWildcardMatchOnNameFilter.class );
    suite.addTestSuite( thredds.crawlabledataset.filter.TestLogicalFilterComposer.class );

    return suite;
  }
}

/*
 * $Log: TestAllCrawlableDataset.java,v $
 * Revision 1.1  2005/11/15 18:40:51  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.2  2005/08/16 21:47:51  edavis
 * Switch from Log4j to commons logging.
 *
 * Revision 1.1  2005/03/30 23:56:07  edavis
 * Fix tests.
 *
 */