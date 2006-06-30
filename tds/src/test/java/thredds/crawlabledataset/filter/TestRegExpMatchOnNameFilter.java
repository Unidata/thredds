// $Id: TestRegExpMatchOnNameFilter.java,v 1.4 2005/12/30 00:18:56 edavis Exp $
package thredds.crawlabledataset.filter;

import junit.framework.*;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 5, 2005 1:26:20 PM
 */
public class TestRegExpMatchOnNameFilter extends TestCase
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( TestWildcardMatchOnNameFilter.class );

  public TestRegExpMatchOnNameFilter( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testRegExpMatch()
  {
    checkRegExpMatch( "build\\.xml", "build.xml" );
    checkRegExpMatch( "build\\.xm.?", "build.xml" );
    checkRegExpMatch( "build\\..*", "build.xml" );

    checkRegExpMatch( "visad\\.jar", "lib/visad.jar" );
    checkRegExpMatch( "visad\\.j.?r", "lib/visad.jar" );
    checkRegExpMatch( ".*ad\\.jar", "lib/visad.jar" );
  }

  private void checkRegExpMatch( String regExpString, String dsPath )
  {
    RegExpMatchOnNameFilter me = new RegExpMatchOnNameFilter( regExpString );

    CrawlableDataset ds = null;
    try
    {
      ds = CrawlableDatasetFactory.createCrawlableDataset( dsPath, null, null );
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to create CrawlableDataset <" + dsPath + ">: " + e.getMessage(),
                  false );
    }
    assertTrue( "Failed to construct RegExpMatchOnNameFilter <" + regExpString + ">.",
                me != null );

    assertTrue( "RegExp string <" + regExpString + "> did not match dataset <" + ds.getName() + ">.",
                me.accept( ds) );
  }
}
/*
 * $Log: TestRegExpMatchOnNameFilter.java,v $
 * Revision 1.4  2005/12/30 00:18:56  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.3  2005/12/16 23:19:39  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.2  2005/11/18 23:51:06  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.1  2005/11/15 18:40:51  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 */