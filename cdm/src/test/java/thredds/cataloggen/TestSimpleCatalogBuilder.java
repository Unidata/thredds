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
