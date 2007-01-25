// $Id: TestCrawlableDatasetFilter.java 61 2006-07-12 21:36:00Z edavis $
package thredds.crawlabledataset;

import junit.framework.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

import thredds.crawlabledataset.filter.*;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 4, 2005 10:09:07 PM
 */
public class TestCrawlableDatasetFilter extends TestCase
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( TestCrawlableDatasetFilter.class );

  private List resultsNcNoCVS;
  private List resultsAll;

  public TestCrawlableDatasetFilter( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    resultsNcNoCVS = new ArrayList();
    resultsNcNoCVS.add( "test/data/thredds/cataloggen/testData/modelNotFlat/gfs_211/2004050300_gfs_211.nc" );
    resultsNcNoCVS.add( "test/data/thredds/cataloggen/testData/modelNotFlat/gfs_211/2004050306_gfs_211.nc" );
    resultsNcNoCVS.add( "test/data/thredds/cataloggen/testData/modelNotFlat/gfs_211/2004050312_gfs_211.nc" );
    resultsNcNoCVS.add( "test/data/thredds/cataloggen/testData/modelNotFlat/gfs_211/2004050318_gfs_211.nc" );

    resultsAll = new ArrayList( resultsNcNoCVS );
    resultsAll.add( "test/data/thredds/cataloggen/testData/modelNotFlat/gfs_211/CVS" );
  }

  /**
   * Test ...
   */
  public void testRegExpIncludeAll()
  {
    String path = "test/data/thredds/cataloggen/testData/modelNotFlat/gfs_211";
    String name = "gfs_211";

    CrawlableDataset cd = createCrawlableDataset( path, name );

    // Construct filter
    List selectors = new ArrayList();
    selectors.add( new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( ".*"), true, true, false ) );
    CrawlableDatasetFilter me = new MultiSelectorFilter( selectors );
    assertTrue( me != null );

    // Get filtered list of datasets.
    List list = null;
    try
    {
      list = cd.listDatasets( me);
    }
    catch ( IOException e )
    {
      assertTrue( "IOException getting children datasets <" + cd.getName() + ">: " + e.getMessage(),
                  false );
      return;
    }

    assertTrue( "Number of datasets <" + list.size() + "> not as expected <2>.",
                list.size() == 5 );
    for ( Iterator it = list.iterator(); it.hasNext(); )
    {
      CrawlableDataset curCd = (CrawlableDataset) it.next();
      assertTrue( "Result path <" + curCd.getPath() + "> not as expected <" + resultsAll + ">.",
                  resultsAll.contains( curCd.getPath() ) );
    }

  }

  public void testRegExpIncludeNcExcludeCVS()
  {
    String path = "test/data/thredds/cataloggen/testData/modelNotFlat/gfs_211";
    String name = "gfs_211";

    CrawlableDataset cd = createCrawlableDataset( path, name );

    // Construct filter
    List selectors = new ArrayList();
    selectors.add( new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( ".*nc$"), true, true, false ) );
    selectors.add( new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( "CVS"), false, false, true ) );
    CrawlableDatasetFilter me = new MultiSelectorFilter( selectors );
    assertTrue( me != null );

    // Get filtered list of datasets.
    List list = null;
    try
    {
      list = cd.listDatasets( me );
    }
    catch ( IOException e )
    {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    assertTrue( "Number of datasets <" + list.size() + "> not as expected <2>.",
                list.size() == 4 );
    for ( Iterator it = list.iterator(); it.hasNext(); )
    {
      CrawlableDataset curCd = (CrawlableDataset) it.next();
      assertTrue( "Result path <" + curCd.getPath() + "> not as expected <" + resultsNcNoCVS + ">.",
                  resultsNcNoCVS.contains( curCd.getPath() ) );
    }
  }

  public void testWildcardIncludeNcExcludeCVS()
  {
    String path = "test/data/thredds/cataloggen/testData/modelNotFlat/gfs_211";
    String name = "gfs_211";

    CrawlableDataset cd = createCrawlableDataset( path, name );

    // Construct filter
    List selectors = new ArrayList();
    selectors.add( new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( "*.nc$"), true, true, false ) );
    selectors.add( new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( "CVS"), false, false, true ) );
    CrawlableDatasetFilter me = new MultiSelectorFilter( selectors );
    assertTrue( me != null );

    // Get filtered list of datasets.
    List list = null;
    try
    {
      list = cd.listDatasets( me );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException getting children datasets <" + cd.getName() + ">: " + e.getMessage(),
                  false );
      return;
    }

    assertTrue( "Number of datasets <" + list.size() + "> not as expected <2>.",
                list.size() == 4 );
    for ( Iterator it = list.iterator(); it.hasNext(); )
    {
      CrawlableDataset curCd = (CrawlableDataset) it.next();
      assertTrue( "Result path <" + curCd.getPath() + "> not as expected <" + resultsNcNoCVS + ">.",
                  resultsNcNoCVS.contains( curCd.getPath() ) );
    }
  }

  private static CrawlableDataset createCrawlableDataset( String path, String name )
  {
    // Create CrawlableDataset.
    CrawlableDataset cd = null;
    try
    {
      cd = CrawlableDatasetFactory.createCrawlableDataset( path, null, null );
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to create CrawlableDataset <" + path + ">: " + e.getMessage(),
                  false );
      return null;
    }

    assertTrue( "CD path <" + cd.getPath() + "> not as expected <" + path + ">.",
                cd.getPath().equals( path ) );
    assertTrue( "CD name <" + cd.getName() + "> not as expected <" + name + ">.",
                cd.getName().equals( name ) );
    return cd;
  }

}
/*
 * $Log: TestCrawlableDatasetFilter.java,v $
 * Revision 1.3  2005/12/30 00:18:56  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.2  2005/11/18 23:51:06  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.1  2005/11/15 18:40:52  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 */