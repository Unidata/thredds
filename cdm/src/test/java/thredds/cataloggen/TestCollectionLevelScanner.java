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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( cat, resultResourceName, debugShowCatalogs );

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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( cat, resultResourceName, debugShowCatalogs );

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

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( cat, resultResourceName, debugShowCatalogs );

  }
}