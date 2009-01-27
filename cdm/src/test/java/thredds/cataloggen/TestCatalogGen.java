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

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;

import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalog;

/**
 * A description
 * <p/>
 * User: edavis
 * Date: May 22, 2004
 * Time: 10:19:22 PM
 */
public class TestCatalogGen extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestCatalogGen.class);

  private String configResourcePath = "/thredds/cataloggen/config";

  // Test 1: All data files are in a single directory, the CatGenConfig document contains a seperate
  // dataset for each type of data each containing a single CatalogGenConfig metadata element that
  // contains a single DatasetSource element.
  private String test1CatGenConf_0_6_ResourceName = "test1CatGenConfig0.6.xml";
  private String test1CatGenConf_1_0_ResourceName = "test1CatGenConfig1.0.xml";

  private String test1ResultCatalog_0_6_ResourceName = "test1ResultCatalog0.6.xml";
  private String test1ResultCatalog_1_0_ResourceName = "test1ResultCatalog1.0.xml";
  private String test1Catalog_0_6File = "test1Catalog0.6.ToFile.xml";
  private String test1Catalog_1_0File = "test1Catalog1.0.ToFile.xml";

  // Test 2: All data files are in a single directory, the CatGenConfig document contains a
  // single dataset which contains a single CatalogGenConfig metadata element. The metadata contains
  // a single DatasetSource with a DatasetNamer for each type of data.
  private String test2CatGenConf_0_6_ResourceName = "test2CatGenConfig0.6.xml";
  private String test2CatGenConf_1_0_ResourceName = "test2CatGenConfig1.0.xml";
  private String test2Catalog_0_6File = "test2Catalog0.6.ToFile.xml";
  private String test2Catalog_1_0File = "test2Catalog1.0.ToFile.xml";

  // The test2 results should be the same as test1 but the directory level datasets
  // are being sorted in reverse order of that expected.
  // @todo Figure out how to control sorting from config file and how to deal with leaf datasets differently than non-leaf datasets.
  private String test2ResultCatalog_0_6_ResourceName = "test2ResultCatalog0.6.xml";
  private String test2ResultCatalog_1_0_ResourceName = "test2ResultCatalog1.0.xml";

  // Test3: Same as test 1 plus each dataset contains a DublinCore metadata element which has
  // an xlink to an external metadata document.
  private String test3CatGenConf_0_6_ResourceName = "test3CatGenConfig0.6.xml";

  private String test3ResultCatalog_0_6_ResourceName = "test3ResultCatalog0.6.xml";
  private String test3Catalog_0_6File = "test3Catalog0.6.ToFile.xml";

  private CatalogGen me;

  private InvCatalogFactory factory;
  private InvCatalogImpl justWrittenCatalog;

  private boolean displayComparedCatalogs = false;

  public TestCatalogGen( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    this.factory = new InvCatalogFactory( "default", true );
  }

  public void testCreateCatalogRefs()
  {
    String configDocName = "src/test/data/thredds/cataloggen/testCatGen.createCatalogRefs.xml";
    String expectedDocResourceName = "/thredds/cataloggen/testCatGen.createCatalogRefs.result.xml";

    // Expand the CatalogGenConfig document.
    expandConfigDoc( configDocName );

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedDocResourceName, displayComparedCatalogs);
  }

  public void testRejectFilter()
  {
    String configDocName = "src/test/data/thredds/cataloggen/testCatGen.rejectFilter.xml";
    String expectedDocResourceName = "/thredds/cataloggen/testCatGen.rejectFilter.result.xml";

    // Expand the CatalogGenConfig document.
    expandConfigDoc( configDocName );

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedDocResourceName, displayComparedCatalogs);
  }

  public void testDirTreeInvCat1_0()
  {
    String configDocName = "src/test/data/thredds/cataloggen/testCatGen.dirTree.InvCat1.0.xml";
    String expectedDocResourceName = "/thredds/cataloggen/testCatGen.dirTree.InvCat1.0.result.xml";

    // Expand the CatalogGenConfig document.
    expandConfigDoc( configDocName );

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedDocResourceName, displayComparedCatalogs);
  }

  public void testUahRadarLevelII()
  {
    String configDocName = "src/test/data/thredds/cataloggen/testCatGen.uahRadarLevelII.xml";
    String expectedDocResourceName = "/thredds/cataloggen/testCatGen.uahRadarLevelII.result.xml";

    // Expand the CatalogGenConfig document.
    expandConfigDoc( configDocName );

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedDocResourceName, displayComparedCatalogs);
  }

  public void testBadAccessPoint()
  {
    String configDocName = "src/test/data/thredds/cataloggen/testCatGen.badAccessPoint.xml";
    String expectedDocResourceName = "/thredds/cataloggen/testCatGen.badAccessPoint.result.xml";

    // Expand the CatalogGenConfig document.
    expandConfigDoc( configDocName );

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedDocResourceName, displayComparedCatalogs);
  }

  /**
   * Test ...
   */
  public void testExpandTest1InvCat_1_0()
  {
    // Create, validate, and expand a CatalogGen then compare results to expected catalog.
    String catGenConfResourceName = configResourcePath + "/" + test1CatGenConf_1_0_ResourceName;
    String expectedCatalogResourceName = configResourcePath + "/" + test1ResultCatalog_1_0_ResourceName;

    log.debug( "testExpandTest1InvCat_1_0(): expand catalog <" + catGenConfResourceName + "> and compare to expected catalog <" + expectedCatalogResourceName + ">." );
    this.expandCatalogGenTest( catGenConfResourceName, expectedCatalogResourceName );

    log.debug( "testExpandTest1InvCat_1_0(): compare expanded catalog to itself after writing to and reading from a file." );
    String expandedCatalogFileName = test1Catalog_1_0File;
    this.writeCatalogGenTest( expandedCatalogFileName );
  }

  public void testExpandTest2InvCat_1_0()
  {
    // Create, validate, and expand a CatalogGen then compare results to expected catalog.
    String catGenConfResourceName = configResourcePath + "/" + test2CatGenConf_1_0_ResourceName;
    String expectedCatalogResourceName = configResourcePath + "/" + test2ResultCatalog_1_0_ResourceName;

    log.debug( "testExpandTest2InvCat_1_0(): expand catalog <" + catGenConfResourceName + "> and compare to expected catalog <" + expectedCatalogResourceName + ">." );
    this.expandCatalogGenTest( catGenConfResourceName, expectedCatalogResourceName );

    log.debug( "testExpandTest2InvCat_1_0(): compare expanded catalog to itself after writing to and reading from a file." );
    String expandedCatalogFileName = test2Catalog_1_0File;
    this.writeCatalogGenTest( expandedCatalogFileName );
  }

//  public void testExpandTest1InvCat_0_6()
//  {
//    // Create, validate, and expand a CatalogGen then compare results to expected catalog.
//    String catGenConfResourceName = configResourcePath + "/" + test1CatGenConf_0_6_ResourceName;
//    String expectedCatalogResourceName = configResourcePath + "/" + test1ResultCatalog_0_6_ResourceName;
//
//    log.debug( "testExpandTest1InvCat_0_6(): expand catalog <" + catGenConfResourceName + "> and compare to expected catalog <" + expectedCatalogResourceName + ">." );
//    this.expandCatalogGenTest( catGenConfResourceName, expectedCatalogResourceName );
//
//    log.debug( "testExpandTest1InvCat_0_6(): compare expanded catalog to itself after writing to and reading from a file." );
//    String expandedCatalogFileName = test1Catalog_0_6File;
//    this.writeCatalogGenTest( expandedCatalogFileName );
//  }
//
//  public void testExpandTest2InvCat_0_6()
//  {
//    // Create, validate, and expand a CatalogGen then compare results to expected catalog.
//    String catGenConfResourceName = configResourcePath + "/" + test2CatGenConf_0_6_ResourceName;
//    String expectedCatalogResourceName = configResourcePath + "/" + test2ResultCatalog_0_6_ResourceName;
//
//    log.debug( "testExpandTest2InvCat_0_6(): expand catalog <" + catGenConfResourceName + "> and compare to expected catalog <" + expectedCatalogResourceName + ">." );
//    this.expandCatalogGenTest( catGenConfResourceName, expectedCatalogResourceName );
//
//    log.debug( "testExpandTest2InvCat_0_6(): compare expanded catalog to itself after writing to and reading from a file." );
//    String expandedCatalogFileName = test2Catalog_0_6File;
//    this.writeCatalogGenTest( expandedCatalogFileName );
//  }
//
//  public void testExpandWriteTest3InvCat_0_6()
//  {
//    // Create, validate, and expand a CatalogGen then compare results to expected catalog.
//    String catGenConfResourceName = configResourcePath + "/" + test3CatGenConf_0_6_ResourceName;
//    String expectedCatalogResourceName = configResourcePath + "/" + test3ResultCatalog_0_6_ResourceName;
//
//    log.debug( "testExpandTest3InvCat_0_6(): expand catalog <" + catGenConfResourceName + "> and compare to expected catalog <" + expectedCatalogResourceName + ">." );
//    this.expandCatalogGenTest( catGenConfResourceName, expectedCatalogResourceName );
//
//    log.debug( "testExpandTest3InvCat_0_6(): compare expanded catalog to itself after writing to and reading from a file." );
//    String expandedCatalogFileName = test3Catalog_0_6File;
//    this.writeCatalogGenTest( expandedCatalogFileName );
//  }

  private void expandCatalogGenTest( String catGenConfResourceName, String expectedCatalogResourceName )
  {
    String tmpMsg = null;

    // Open a CatalogGenConfig document resource as an InputStream.
    String catGenConfURIName = "http://TestCatalogGen.resource" + catGenConfResourceName;
    URI catGenConfURI = null;
    try
    {
      catGenConfURI = new URI( catGenConfURIName );
    }
    catch ( URISyntaxException e )
    {
      tmpMsg = "URISyntaxException thrown creating URI w/ " + catGenConfURIName + ": " + e.getMessage();
      log.debug( "expandCatalogGenTest(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    InputStream catGenConfInputStream = this.getClass().getResourceAsStream( catGenConfResourceName );

    // Create the CatalogGen with the config doc InputStream
    try
    {
      me = new CatalogGen( catGenConfInputStream, catGenConfURI.toURL());
    }
    catch ( MalformedURLException e )
    {
      tmpMsg = "MalformedURLException thrown creating URL w/ resource URI <" + catGenConfURI.toString() + ">: " + e.getMessage();
      log.debug( "expandCatalogGenTest(): " + tmpMsg, e);
      assertTrue( tmpMsg, false);
    }

    // Close the CatalogGenConf and the expected catalog InputStreams.
    try
    {
      catGenConfInputStream.close();
    }
    catch ( IOException e )
    {
      tmpMsg = "IOException thrown closing catGenConfInputStream: " + e.getMessage();
      log.debug( "expandCatalogGenTest(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }

    // Check that CatalogGen is valid.
    StringBuilder validationMsg = new StringBuilder( );
    assertTrue( "CatalogGen did not validate: " + validationMsg.toString(),
                me.isValid( validationMsg) );

    // Expand the CatalogGen
    me.expand();

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedCatalogResourceName, displayComparedCatalogs);
  }

  private void writeCatalogGenTest( String expandedCatalogFileName )
  {
    String tmpMsg = null;
    URI expandedCatalogFileURI = null;
    File expandedCatalogFile = null;

    // Write the expanded catalog to a file.
    try
    {
      factory.writeXML( (InvCatalogImpl) this.me.getCatalog(), expandedCatalogFileName );
    }
    catch ( IOException e )
    {
      tmpMsg = "IOException writing catalog to file <" + expandedCatalogFileName + ">: " + e.getMessage();
      log.debug( "writeCatalogGenTest(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }

    // Read the expanded catalog back from the file it was just written to.
    expandedCatalogFile = new File( expandedCatalogFileName );
    expandedCatalogFileURI = expandedCatalogFile.toURI();
    BufferedInputStream is = null;
    try
    {
      is = new BufferedInputStream( new FileInputStream( expandedCatalogFile) );
    }
    catch ( FileNotFoundException e )
    {
      tmpMsg = "FileNotFoundException thrown while reading file <" + expandedCatalogFileURI.toString() + ">: " + e.getMessage();
      log.debug( "writeCatalogGenTest(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    this.justWrittenCatalog = factory.readXML( is, expandedCatalogFileURI );

    // Close the InputStream.
    try
    {
      is.close();
    }
    catch ( IOException e )
    {
      tmpMsg = "IOException thrown while closing file input stream <" + expandedCatalogFileURI.toString() + ">: " + e.getMessage();
      log.debug( "writeCatalogGenTest(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }

    // Test that the expanded catalog is the same as the expanded catalog after
    // writing it to and then reading it from a file. (Tests that writing works!)
    assertTrue( "Expanded catalog does not equal the expected catalog after writing to and reading from a file.",
                ( (InvCatalogImpl) this.me.getCatalog()).equals( justWrittenCatalog) );
  }

  private void expandConfigDoc( String configDocFilename )
  {
    File configDocFile = new File( configDocFilename);
    URI configDocUri = configDocFile.toURI();

    InputStream configDocIs = null;
    try
    {
      configDocIs = new FileInputStream( configDocFile);
    }
    catch ( FileNotFoundException e )
    {
      String tmpMsg = "FileNotFoundException when creating InputStream on file <" + configDocFile.getAbsolutePath() + ">: " + e.getMessage();
      log.debug( "expandConfigDoc(): " + tmpMsg, e);
      assertTrue( tmpMsg, false);
    }

    // Create the CatalogGen with the config doc InputStream
    try
    {
      me = new CatalogGen( configDocIs, configDocUri.toURL());
    }
    catch ( MalformedURLException e )
    {
      String tmpMsg = "MalformedURLException thrown creating URL w/ resource URI <" + configDocUri.toString() + ">: " + e.getMessage();
      log.debug( "expandConfigDoc(): " + tmpMsg, e);
      assertTrue( tmpMsg, false);
    }

    // Check that CatalogGen is valid.
    StringBuilder validationMsg = new StringBuilder( );
    assertTrue( "CatalogGen did not validate: " + validationMsg.toString(),
                me.isValid( validationMsg) );

    // Expand the CatalogGen
    me.expand();
  }

  public static void compareCatalogToCatalogResource( InvCatalog expandedCatalog, String expectedCatalogResourceName, boolean display)
  {
    // Open expected catalog document resource as an InputStream.
    String expectedCatalogURIName = "http://TestCatalogGen.resource" + expectedCatalogResourceName;
    URI expectedCatalogURI = null;
    try
    {
      expectedCatalogURI = new URI( expectedCatalogURIName );
    }
    catch ( URISyntaxException e )
    {
      String tmpMsg = "URISyntaxException thrown creating URI w/ " + expectedCatalogURIName + ": " + e.getMessage();
      log.debug( "compareCatalogToCatalogResource(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    InputStream expectedCatalogInputStream = expectedCatalogURIName.getClass().getResourceAsStream( expectedCatalogResourceName );

    // Read in expected result catalog.
    InvCatalogFactory factory = new InvCatalogFactory( "default", true );
    InvCatalogImpl expectedCatalog = factory.readXML( expectedCatalogInputStream, expectedCatalogURI);

    // Close the CatalogGenConf and the expected catalog InputStreams.
    try
    {
      expectedCatalogInputStream.close();
    }
    catch ( IOException e )
    {
      String tmpMsg = "IOException thrown closing InputStream: " + e.getMessage();
      log.debug( "compareCatalogToCatalogResource(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }

    if ( display )
    {
      // Print expected and resulting catalogs to std out.
      String expectedCatalogAsString;
      String catalogAsString;
      try
      {
        expectedCatalogAsString = factory.writeXML( (InvCatalogImpl) expectedCatalog );
        catalogAsString = factory.writeXML( (InvCatalogImpl) expandedCatalog );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
        return;
      }
      System.out.println( "Expected catalog (" + expectedCatalogResourceName + "):" );
      System.out.println( "--------------------" );
      System.out.println( expectedCatalogAsString );
      System.out.println( "--------------------" );
      System.out.println( "Resulting catalog (" + expandedCatalog.getUriString() + "):" );
      System.out.println( "--------------------" );
      System.out.println( catalogAsString );
      System.out.println( "--------------------\n" );
    }

    // Compare the two catalogs.
    assertTrue( "Expanded catalog does not equal expected catalog.",
                ( (InvCatalogImpl) expandedCatalog ).equals( expectedCatalog ) );

  }
}

