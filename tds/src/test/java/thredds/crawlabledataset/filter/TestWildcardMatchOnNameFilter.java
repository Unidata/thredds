// $Id: TestWildcardMatchOnNameFilter.java,v 1.4 2005/12/30 00:18:56 edavis Exp $
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
public class TestWildcardMatchOnNameFilter extends TestCase
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( TestWildcardMatchOnNameFilter.class );

  public TestWildcardMatchOnNameFilter( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testWildcardStringConversionToRegExp()
  {
    checkWildcardStringAsRegExp( "aFile.nc", "aFile\\.nc" );
    checkWildcardStringAsRegExp( "a?ile.nc", "a.?ile\\.nc");
    checkWildcardStringAsRegExp( "a*e.nc", "a.*e\\.nc");
    checkWildcardStringAsRegExp( "a*e.n?", "a.*e\\.n.?");
  }

  public void testWildcardMatch()
  {
    checkWildcardMatch( "build.xml", "build.xml" );
    checkWildcardMatch( "build.xm?", "build.xml" );
    checkWildcardMatch( "build.*", "build.xml" );

    checkWildcardMatch( "visad.jar", "lib/visad.jar" );
    checkWildcardMatch( "visad.j?r", "lib/visad.jar" );
    checkWildcardMatch( "*ad.jar", "lib/visad.jar" );
  }

  private void checkWildcardStringAsRegExp( String wildcardString, String wildcardStringAsRegExp )
  {
    WildcardMatchOnNameFilter me = new WildcardMatchOnNameFilter( wildcardString );
    assertTrue( "Failed to construct WildcardMatchOnNameFilter <" + wildcardString + ">.",
                me != null );

    assertTrue( "Wildcard string <" + wildcardString + "> as regExp <" +
                me.wildcardString + "> not as expected <" + wildcardStringAsRegExp + ">.",
                me.wildcardString.equals( wildcardStringAsRegExp));
  }

  private void checkWildcardMatch( String wildcardString, String dsPath )
  {
    WildcardMatchOnNameFilter me = new WildcardMatchOnNameFilter( wildcardString );

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
    assertTrue( "Failed to construct WildcardMatchOnNameFilter <" + wildcardString + ">.",
                me != null );

    assertTrue( "Wildcard string <" + wildcardString + "> did not match dataset <" + ds.getName() + ">.",
                me.accept( ds) );
  }
}
/*
 * $Log: TestWildcardMatchOnNameFilter.java,v $
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