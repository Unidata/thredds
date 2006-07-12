// $Id: TestCollectionLevelScanner.java,v 1.3 2006/05/19 19:23:06 edavis Exp $
package thredds.cataloggen;

import junit.framework.*;

import java.io.IOException;

import thredds.catalog.*;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;

/**
 * A description
 *
 * @author edavis
 * @since Aug 5, 2005T4:13:08 PM
 */
public class TestCollectionLevelScanner extends TestCase
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( TestCollectionLevelScanner.class );

  private boolean debugShowCatalogs = true;

  private String resourcePath = "/thredds/cataloggen";
  private String resource_simpleWithEmptyServiceBase_result = "testCollectionScanner.simpleWithEmptyServiceBase.result.xml";
  private String resource_simpleWithNotEmptyServiceBase_result = "testCollectionScanner.simpleWithNotEmptyServiceBase.result.xml";
  private String resource_namedForDirWithNotEmptyServiceBase_result = "testCollectionScanner.namedForDirWithNotEmptyServiceBase.result.xml";

  public TestCollectionLevelScanner( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testSimpleWithEmptyServiceBase()
  {
    String resultResourceName = resourcePath + "/" + resource_simpleWithEmptyServiceBase_result;

    String collectionPath = "src/test/data/thredds/cataloggen/testData/modelNotFlat";
    String catalogPath = "src/test/data/thredds/cataloggen/testData/modelNotFlat/eta_211";
    CrawlableDataset collCrDs;
    CrawlableDataset catCrDs;
    try
    {
      collCrDs = CrawlableDatasetFactory.createCrawlableDataset( collectionPath, null, null );
      catCrDs = CrawlableDatasetFactory.createCrawlableDataset( catalogPath, null, null );
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to create CrawlableDataset for given collectionPath <" + collectionPath + "> or catalogPath <" + catalogPath + ">: " + e.getMessage(),
                  false );
      return;
    }

    CollectionLevelScanner me =
            new CollectionLevelScanner( "myModelData", collCrDs, catCrDs, null, null,
                                        new InvService( "service", ServiceType.DODS.toString(),
                                                        "", null, null));
    assertTrue( me != null );

    InvCatalog cat;
    try
    {
      me.scan();
      cat = me.generateCatalog();
    }
    catch ( IOException e )
    {
      assertTrue( "Failed to generate catalog: " + e.getMessage(),
                  false );
      return;
    }

    if ( debugShowCatalogs )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) cat ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( cat, resultResourceName );

  }

  public void testSimpleWithNotEmptyServiceBase()
  {
    String resultResourceName = resourcePath + "/" + resource_simpleWithNotEmptyServiceBase_result;

    String collectionPath = "src/test/data/thredds/cataloggen/testData/modelNotFlat";
    String catalogPath = "src/test/data/thredds/cataloggen/testData/modelNotFlat/eta_211";
    CrawlableDataset collCrDs;
    CrawlableDataset catCrDs;
    try
    {
      collCrDs = CrawlableDatasetFactory.createCrawlableDataset( collectionPath, null, null );
      catCrDs = CrawlableDatasetFactory.createCrawlableDataset( catalogPath, null, null );
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to create CrawlableDataset for given collectionPath <" + collectionPath + "> or catalogPath <" + catalogPath + ">: " + e.getMessage(),
                  false );
      return;
    }

    CollectionLevelScanner me =
            new CollectionLevelScanner( "myModelData", collCrDs, catCrDs, null, null,
                                        new InvService( "service", ServiceType.DODS.toString(),
                                                        "/thredds/dodsC", null, null ) );
    assertTrue( me != null );

    InvCatalog cat;
    try
    {
      me.scan();
      cat = me.generateCatalog();
    }
    catch ( IOException e )
    {
      assertTrue( "Failed to generate catalog: " + e.getMessage(),
                  false );
      return;
    }

    if ( debugShowCatalogs )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) cat ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( cat, resultResourceName );

  }

  public void testNamedForDirWithNotEmptyServiceBase()
  {
    String resultResourceName = resourcePath + "/" + resource_namedForDirWithNotEmptyServiceBase_result;

    String collectionPath = "src/test/data/thredds/cataloggen/testData/modelNotFlat";
    String catalogPath = "src/test/data/thredds/cataloggen/testData/modelNotFlat/eta_211";
    CrawlableDataset collCrDs;
    CrawlableDataset catCrDs;
    try
    {
      collCrDs = CrawlableDatasetFactory.createCrawlableDataset( collectionPath, null, null );
      catCrDs = CrawlableDatasetFactory.createCrawlableDataset( catalogPath, null, null );
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to create CrawlableDataset for given collectionPath <" + collectionPath + "> or catalogPath <" + catalogPath + ">: " + e.getMessage(),
                  false );
      return;
    }

    CollectionLevelScanner me =
            new CollectionLevelScanner( "", collCrDs, catCrDs, null, null,
                                        new InvService( "service", ServiceType.DODS.toString(),
                                                        "/thredds/dodsC", null, null ) );
    assertTrue( me != null );

    InvCatalog cat;
    try
    {
      me.scan();
      cat = me.generateCatalog();
    }
    catch ( IOException e )
    {
      assertTrue( "Failed to generate catalog: " + e.getMessage(),
                  false );
      return;
    }

    if ( debugShowCatalogs )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) cat ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( cat, resultResourceName );

  }
}