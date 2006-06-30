// $Id: TestCatalogGen.java,v 1.6 2006/01/20 02:08:25 caron Exp $
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
    String configDocName = "./test/data/thredds/cataloggen/testCatGen.createCatalogRefs.xml";
    String expectedDocResourceName = "/thredds/cataloggen/testCatGen.createCatalogRefs.result.xml";

    // Expand the CatalogGenConfig document.
    expandConfigDoc( configDocName );

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedDocResourceName);
//    String catAsString = null;
//    try
//    {
//      catAsString = factory.writeXML( (InvCatalogImpl) me.getCatalog());
//    }
//    catch ( IOException e )
//    {
//      String tmpMsg = "IOException thrown trying to write catalog as String: " + e.getMessage();
//      log.debug( "testDirTreeInvCat1_0(): " + tmpMsg, e );
//      assertTrue( tmpMsg, false );
//    }
//    System.out.print( catAsString);
  }

  public void testRejectFilter()
  {
    String configDocName = "./test/data/thredds/cataloggen/testCatGen.rejectFilter.xml";
    String expectedDocResourceName = "/thredds/cataloggen/testCatGen.rejectFilter.result.xml";

    // Expand the CatalogGenConfig document.
    expandConfigDoc( configDocName );

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedDocResourceName);
//    String catAsString = null;
//    try
//    {
//      catAsString = factory.writeXML( (InvCatalogImpl) me.getCatalog());
//    }
//    catch ( IOException e )
//    {
//      String tmpMsg = "IOException thrown trying to write catalog as String: " + e.getMessage();
//      log.debug( "testDirTreeInvCat1_0(): " + tmpMsg, e );
//      assertTrue( tmpMsg, false );
//    }
//    System.out.print( catAsString);
  }

  public void testDirTreeInvCat1_0()
  {
    String configDocName = "./test/data/thredds/cataloggen/testCatGen.dirTree.InvCat1.0.xml";
    String expectedDocResourceName = "/thredds/cataloggen/testCatGen.dirTree.InvCat1.0.result.xml";

    // Expand the CatalogGenConfig document.
    expandConfigDoc( configDocName );

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedDocResourceName);
//    String catAsString = null;
//    try
//    {
//      catAsString = factory.writeXML( (InvCatalogImpl) me.getCatalog());
//    }
//    catch ( IOException e )
//    {
//      String tmpMsg = "IOException thrown trying to write catalog as String: " + e.getMessage();
//      log.debug( "testDirTreeInvCat1_0(): " + tmpMsg, e );
//      assertTrue( tmpMsg, false );
//    }
//    System.out.print( catAsString);
  }

  public void testUahRadarLevelII()
  {
    String configDocName = "./test/data/thredds/cataloggen/testCatGen.uahRadarLevelII.xml";
    String expectedDocResourceName = "/thredds/cataloggen/testCatGen.uahRadarLevelII.result.xml";

    // Expand the CatalogGenConfig document.
    expandConfigDoc( configDocName );

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedDocResourceName);
  }

  public void testBadAccessPoint()
  {
    String configDocName = "./test/data/thredds/cataloggen/testCatGen.badAccessPoint.xml";
    String expectedDocResourceName = "/thredds/cataloggen/testCatGen.badAccessPoint.result.xml";

    // Expand the CatalogGenConfig document.
    expandConfigDoc( configDocName );

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedDocResourceName);
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
    StringBuffer validationMsg = new StringBuffer( );
    assertTrue( "CatalogGen did not validate: " + validationMsg.toString(),
                me.isValid( validationMsg) );

    // Expand the CatalogGen
    me.expand();

    // Compare the expanded catalog to the expected catalog.
    compareCatalogToCatalogResource( me.getCatalog(), expectedCatalogResourceName);
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
    StringBuffer validationMsg = new StringBuffer( );
    assertTrue( "CatalogGen did not validate: " + validationMsg.toString(),
                me.isValid( validationMsg) );

    // Expand the CatalogGen
    me.expand();
  }

  public static void compareCatalogToCatalogResource( InvCatalog expandedCatalog, String expectedCatalogResourceName)
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

    // Compare the two catalogs.
    assertTrue( "Expanded catalog does not equal expected catalog.",
                ( (InvCatalogImpl) expandedCatalog ).equals( expectedCatalog ) );

  }
}

/*
 * $Log: TestCatalogGen.java,v $
 * Revision 1.6  2006/01/20 02:08:25  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.5  2005/06/08 21:20:16  edavis
 * Fixed naming of top dataset in InvDatasetScan produced catalogs
 * (removed "/" from end of name). Added to TestInvDatasetScan.
 *
 * Revision 1.4  2005/06/07 22:50:25  edavis
 * Fixed catalogRef links so relative to catalog instead of to service.
 * Fixed all tests in TestAllCatalogGen (including changing directory
 * filters because catalogRef names no longer contain slashes ("/").
 *
 * Revision 1.3  2005/04/29 14:55:57  edavis
 * Fixes for change in InvCatalogFactory.writeXML( cat, filename) method
 * signature. And start on allowing wildcard characters in pathname given
 * to DirectoryScanner.
 *
 * Revision 1.2  2005/03/30 19:55:09  edavis
 * Continue simplifying build process (build.xml fixes and update tests.
 *
 * Revision 1.1  2005/03/30 05:41:19  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.7  2005/01/14 21:24:33  edavis
 * Add handling of datasetSource@createCatalogRefs to DTD/XSD and
 * CatGenConfigMetadataFactory and testing.
 *
 * Revision 1.6  2005/01/14 18:02:23  edavis
 * Add createCatalogRef to DirectoryScanner constructor. Add testing.
 *
 * Revision 1.5  2005/01/12 22:51:40  edavis
 * 1) Remove all empty collection datasets before returning the catalog from
 * DatasetSource.expand(). 2)Improve how a CatGen config document and the
 * generated datasets are merged, mainly involved the dropping of the top-level
 * generated dataset. 3) Provide for filtering by a group of DatasetFilters: add
 * reject capability, and add the acceptDatasetByFilterGroup() static method.
 *
 * Revision 1.4  2004/12/29 21:53:21  edavis
 * Added catalogRef generation capability to DatasetSource: 1) a catalogRef
 * is generated for all accepted collection datasets; 2) once a DatasetSource
 * is expanded, information about each catalogRef is available. Added tests
 * for new catalogRef generation capability.
 *
 * Revision 1.3  2004/12/22 22:29:00  edavis
 * 1) Fix collection vs atomic dataset filtering includes fix so that default values are handled properly for the DatasetFilter attributes applyToCollectionDataset, applyToAtomicDataset, and invertMatchMeaning.
 * 2) Convert DatasetSource subclasses to use isCollection(), getTopLevelDataset(), and expandThisLevel() instead of expandThisType().
 *
 * Revision 1.2  2004/11/30 22:19:26  edavis
 * Clean up some CatalogGen tests and add testing for DatasetSource (without and with filtering on collection datasets).
 *
 * Revision 1.1  2004/06/03 20:41:36  edavis
 * Added tests and test data to check that CatGen config files are parsed
 * correctly and expanded catalogs are written correctly.
 *
 */