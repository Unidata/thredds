// $Id$
package thredds.cataloggen;

import junit.framework.*;

import java.io.IOException;

import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
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
    String collectionPath = "test/data/thredds/cataloggen/testData/modelNotFlat";
    String catalogPath = "test/data/thredds/cataloggen/testData/modelNotFlat/eta_211";

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

    // Print catalog to std out.
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    try
    {
      System.out.println( fac.writeXML( (InvCatalogImpl) catalog ) );
    }
    catch ( IOException e )
    {
      System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, "/thredds/cataloggen/testSimpleCatBuilder.basic.result.xml" );
  }
}
/*
 * $Log: TestSimpleCatalogBuilder.java,v $
 * Revision 1.4  2005/12/30 00:18:56  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.3  2005/12/16 23:19:38  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.2  2005/12/01 20:42:46  edavis
 * Some clean up before getting CrawlableDataset and SimpleCatalogBuilder stuff to Nathan.
 *
 * Revision 1.1  2005/12/01 00:15:03  edavis
 * More work on move to using CrawlableDataset.
 *
 */