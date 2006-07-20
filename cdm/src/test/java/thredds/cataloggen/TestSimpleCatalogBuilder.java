package thredds.cataloggen;

import junit.framework.*;

import java.io.IOException;

import thredds.catalog.InvCatalog;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 30, 2005 4:28:18 PM
 */
public class TestSimpleCatalogBuilder extends TestCase
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( TestSimpleCatalogBuilder.class );

  private boolean debugShowCatalogs = true;

  public TestSimpleCatalogBuilder( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testBasic()
  {
    String collectionPath = "src/test/data/thredds/cataloggen/testData/modelNotFlat";
    String catalogPath = "src/test/data/thredds/cataloggen/testData/modelNotFlat/eta_211";

    CrawlableDataset collectionCrDs = null;
    try
    {
      collectionCrDs = CrawlableDatasetFactory.createCrawlableDataset( collectionPath, null, null );
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to create collection level dataset <" + collectionPath + ">: " + e.getMessage(),
                  false );
      return;
    }

    SimpleCatalogBuilder builder = new SimpleCatalogBuilder( "", collectionCrDs, "server", "OPENDAP", "http://my.server/opendap/" );
    assertTrue( "SimpleCatalogBuilder is null.",
                builder != null );

    CrawlableDataset catalogCrDs = null;
    try
    {
      catalogCrDs = CrawlableDatasetFactory.createCrawlableDataset( catalogPath, null, null );
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to create collection level dataset <" + catalogPath + ">: " + e.getMessage(),
                  false );
      return;
    }

    InvCatalog catalog = null;
    try
    {
      catalog = builder.generateCatalog( catalogCrDs );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException generating catalog for given path <" + catalogPath + ">: " + e.getMessage(),
                  false );
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, "/thredds/cataloggen/testSimpleCatBuilder.basic.result.xml", debugShowCatalogs );
  }
}
