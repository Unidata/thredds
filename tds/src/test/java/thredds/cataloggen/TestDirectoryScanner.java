// $Id: TestDirectoryScanner.java,v 1.15 2006/03/30 21:50:15 edavis Exp $
package thredds.cataloggen;

import junit.framework.TestCase;
import thredds.catalog.*;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * A description
 * <p/>
 * User: edavis
 * Date: Dec 13, 2004
 * Time: 1:00:42 PM
 */
public class TestDirectoryScanner extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestDirectoryScanner.class);

  private boolean debugShowCatalogs_topLevelDirNoCatRefs = true;
  private boolean debugShowCatalogs_topLevelDirNoName = true;
  private boolean debugShowCatalogs_topLevelDir = true;
  private boolean debugShowCatalogs_withFilter = true;
  private boolean debugShowCatalogs_withDirPattern = true;
  private boolean debugShowCatalogs_catRefRepeatedDirProblem = true;

  private String serviceId = "myServer";
  private String serviceTitle = "My server of data";
  private ServiceType serviceType = ServiceType.DODS;
  private String serviceBase = "http://localhost:8080/thredds/dodsC/";
  private String serviceAccessPoint = "./build/test/classes/thredds/cataloggen/testData/modelNotFlat/";
  private String serviceAccessPointHeader = "./build/test/classes/thredds/cataloggen/";

  private String configResourcePath = "/thredds/cataloggen";
  private String testDirScan_topLevelDirNoCatRefs_ResourceName = "testDirScan.topLevelDirNoCatRefs.result.xml";
  private String testDirScan_topLevelDirNoName_ResourceName = "testDirScan.topLevelDirNoName.result.xml";
  private String testDirScan_topLevelDir_ResourceName = "testDirScan.topLevelDir.result.xml";
  private String testDirScan_withFilter_ResourceName = "testDirScan.withFilter.result.xml";
  private String testDirScan_withDirPattern_ResourceName = "testDirScan.withDirPattern.result.xml";
  private String testDirScan_withAliasDirAndFilePattern_ResourceName = "testDirScan.withAliasDirAndFilePattern.result.xml";
  private String testDirScan_catRefRepeatedDirProblem_ResourceName = "testDirScan.catRefRepeatedDirProblem.result.xml";

  public TestDirectoryScanner( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testTopLevelDirNoCatRefs()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_topLevelDirNoCatRefs_ResourceName;

    // Test on lower directory level than servers root directory (so top-level dataset name is some path)
    // Request is http://localhost:8080/thredds/dodsC/testData/modelNotFlat/catalog.xml
    // where base directory contains testData/
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( this.serviceAccessPointHeader), null, false );
    assertTrue( me != null );

    InvCatalog catalog = me.getDirCatalog( new File( this.serviceAccessPoint), null, false, false);

    if (debugShowCatalogs_topLevelDirNoCatRefs)
    {
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
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testTopLevelDirNoName()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_topLevelDirNoName_ResourceName;

    // Test on the servers root directory (so top-level dataset name is "")
    // Request is http://localhost:8080/thredds/dodsC/testData/modelNotFlat/catalog.xml
    // where base directory is modelNotFlat/
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( this.serviceAccessPoint), null, true );
    assertTrue( me != null );

    InvCatalog catalog = me.getDirCatalog( new File( this.serviceAccessPoint), null, false, false);

    if ( debugShowCatalogs_topLevelDirNoName )
    {
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
    }
    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testTopLevelDir()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_topLevelDir_ResourceName;

    // Test on lower directory level than servers root directory (so top-level dataset name is some path)
    // Request is http://localhost:8080/thredds/dodsC/testData/modelNotFlat/catalog.xml
    // where base directory contains testData/
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( this.serviceAccessPointHeader), null, true );
    assertTrue( me != null );

    InvCatalog catalog = me.getDirCatalog( new File( this.serviceAccessPoint), null, false, false);

    if ( debugShowCatalogs_topLevelDir )
    {
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
    }
    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testWithFilter()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_withFilter_ResourceName;

    File dirToScan = new File( this.serviceAccessPoint + "eta_211/" );
    String filterPattern = ".*12_eta_211\\.nc$";

    // Test on lower directory level than servers root directory (so top-level dataset name is some path)
    // Request is http://localhost:8080/thredds/dodsC/testData/modelNotFlat/eta_211/catalog.xml
    // where base directory contains testData/
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( this.serviceAccessPointHeader), null, true );
    assertTrue( me != null );

    InvCatalog catalog = me.getDirCatalog( dirToScan, filterPattern, false, false);

    if ( debugShowCatalogs_withFilter )
    {
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
    }
    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testWithAliasDirPattern()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_withDirPattern_ResourceName;

    String dataDirHeader = "./test/data/thredds/cataloggen/testData";
    String dataDir = "./test/data/thredds/cataloggen/testData/uahRadarLevelII/2004*/KBMX";
    File dataDirFile = new File( dataDir );
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( dataDirHeader ), null, true );
    assertTrue( me != null );

    CrawlableDataset catalogCrDs;
    try
    {
      catalogCrDs = CrawlableDatasetFactory.createCrawlableDataset( dataDirFile.getAbsolutePath(), null, null );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException creating dataset <" + dataDirFile.getAbsolutePath() + ">: " + e.getMessage(),
                  false);
      return;
    }
    catch ( ClassNotFoundException e )
    {
      throw new IllegalArgumentException( "Did not find class: " + e.getMessage() );
    }
    catch ( NoSuchMethodException e )
    {
      throw new IllegalArgumentException( "Required constructor not found in class: " + e.getMessage() );
    }
    catch ( IllegalAccessException e )
    {
      throw new IllegalArgumentException( "Did not have necessary access to class: " + e.getMessage() );
    }
    catch ( InvocationTargetException e )
    {
      throw new IllegalArgumentException( "Could not invoke required method in class: " + e.getMessage() );
    }
    catch ( InstantiationException e )
    {
      throw new IllegalArgumentException( "Could not instatiate class: " + e.getMessage() );
    }
    if ( ! catalogCrDs.isCollection() )
      throw new IllegalArgumentException( "catalog directory is not a directory <" + dataDir + ">." );

    InvCatalog catalog = me.getDirCatalog( catalogCrDs, ".*", true, "idBase", false, null, null, null);

    if ( debugShowCatalogs_withDirPattern )
    {
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
    }
    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testWithAliasDirAndFilePattern()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_withAliasDirAndFilePattern_ResourceName;

    String dataDirHeader = "./test/data/thredds/cataloggen/testData";
    String dataDir = "./test/data/thredds/cataloggen/testData/uahRadarLevelII/2004*/KBMX/*bz2";
    File dataDirFile = new File( dataDir );
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( dataDirHeader ), null, true );
    assertTrue( me != null );

    CrawlableDataset catalogCrDs;
    try
    {
      catalogCrDs = CrawlableDatasetFactory.createCrawlableDataset( dataDirFile.getAbsolutePath(), null, null );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException creating dataset <" + dataDirFile.getAbsolutePath() + ">: " + e.getMessage(),
                  false );
      return;
    }
    catch ( ClassNotFoundException e )
    {
      throw new IllegalArgumentException( "Did not find class: " + e.getMessage() );
    }
    catch ( NoSuchMethodException e )
    {
      throw new IllegalArgumentException( "Required constructor not found in class: " + e.getMessage() );
    }
    catch ( IllegalAccessException e )
    {
      throw new IllegalArgumentException( "Did not have necessary access to class: " + e.getMessage() );
    }
    catch ( InvocationTargetException e )
    {
      throw new IllegalArgumentException( "Could not invoke required method in class: " + e.getMessage() );
    }
    catch ( InstantiationException e )
    {
      throw new IllegalArgumentException( "Could not instatiate class: " + e.getMessage() );
    }
    if ( ! catalogCrDs.isCollection() )
      throw new IllegalArgumentException( "catalog directory is not a directory <" + dataDir + ">." );

    InvCatalog catalog = me.getDirCatalog( catalogCrDs, ".*", true, "idBase", false, null, null, null);

    if ( debugShowCatalogs_withDirPattern )
    {
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
    }
    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }

  public void testCatRefRepeatedDirProblem()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_catRefRepeatedDirProblem_ResourceName;

    String serviceId = "ncdods";
    String serviceTitle = "nc and dods server";
    ServiceType serviceType = ServiceType.DODS;
    String serviceBase = "http://localhost:8080/thredds/dodsC/";

    String dataDir = "C:\\Ethan\\code\\threddsDev\\netcdf-java-2.2\\test\\data";
    String reqDir = "trajectory/aircraft";

    File dataDirFile = new File( dataDir);
    File dirToScan = new File( dataDir + "/" + reqDir );
    String filterPattern = ".*nc$";

    log.info( "Calling DirectoryScanner( new InvService( \""+serviceId+"\", \""+serviceType.toString()+"\", \""+serviceBase+"\", null, null), \""+serviceTitle+"\", \""+dataDirFile.getPath()+"\", true)");
    InvService service = new InvService( serviceId, serviceType.toString(), serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, serviceTitle, dataDirFile, null, true );
    assertTrue( me != null );

    log.info( "Calling getDirCatalog( \""+dirToScan+"\", \""+filterPattern+"\", false)");
    InvCatalog catalog = me.getDirCatalog( dirToScan, filterPattern, false, false );

    if ( debugShowCatalogs_catRefRepeatedDirProblem )
    {
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
    }
    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName );
  }
}

/*
 * $Log: TestDirectoryScanner.java,v $
 * Revision 1.15  2006/03/30 21:50:15  edavis
 * Minor fixes to get tests running.
 *
 * Revision 1.14  2006/01/20 02:08:26  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.13  2005/12/30 00:18:56  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.12  2005/12/16 23:19:38  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.11  2005/07/22 16:19:51  edavis
 * Allow DatasetSource and InvDatasetScan to add dataset size metadata.
 *
 * Revision 1.10  2005/07/20 22:44:56  edavis
 * Allow InvDatasetScan to work with a service that is not catalog relative.
 * (DatasetSource can now add a prefix path name to resulting urlPaths.)
 *
 * Revision 1.9  2005/07/08 18:35:01  edavis
 * Fix problem dealing with service URLs that are relative
 * to the catalog (base="") and those that are relative to
 * the collection (base URL is not empty).
 *
 * Revision 1.8  2005/06/07 22:50:25  edavis
 * Fixed catalogRef links so relative to catalog instead of to service.
 * Fixed all tests in TestAllCatalogGen (including changing directory
 * filters because catalogRef names no longer contain slashes ("/").
 *
 * Revision 1.7  2005/06/03 19:12:42  edavis
 * Start adding wildcard handling in DirectoryScanner. Change
 * how DatasetSource names datasets and how catalogRefs are
 * constructed in DatasetSource.expand().
 *
 * Revision 1.6  2005/05/04 03:37:06  edavis
 * Remove several unnecessary methods in DirectoryScanner.
 *
 * Revision 1.5  2005/05/03 17:11:31  edavis
 * Fix TestDirectoryScanner.testWithDirPattern().
 *
 * Revision 1.4  2005/05/03 17:04:03  edavis
 * Add sort to datasetScan element and handle wildcard character in directory name.
 *
 * Revision 1.3  2005/04/29 14:55:57  edavis
 * Fixes for change in InvCatalogFactory.writeXML( cat, filename) method
 * signature. And start on allowing wildcard characters in pathname given
 * to DirectoryScanner.
 *
 * Revision 1.2  2005/04/27 23:05:41  edavis
 * Move sorting capabilities into new DatasetSorter class.
 * Fix a bunch of tests and such.
 *
 * Revision 1.1  2005/03/30 05:41:19  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.5  2005/02/01 22:55:16  edavis
 * Add dataset filtering to DirectoryScanner.
 *
 * Revision 1.4  2005/01/20 23:13:30  edavis
 * Extend DirectoryScanner to handle catalog generation for a list of top-level
 * data directories:
 * 1) add getMainCatalog(List):void to DirectoryScanner;
 * 2) add expand(List):void to DatasetSource, and
 * 3) two changes to the abstract methods in DatasetSource:
 *   a) add createDataset(String):InvDataset and
 *   b) rename getTopLevelDataset():InvDataset to
 *      createSkeletonCatalog():InvDataset.
 *
 * Revision 1.3  2005/01/14 18:02:24  edavis
 * Add createCatalogRef to DirectoryScanner constructor. Add testing.
 *
 * Revision 1.2  2004/12/15 17:51:03  edavis
 * Changes to clean up ResultService. Changes to add a server title to DirectoryScanner (becomes the title of the top-level dataset).
 *
 * Revision 1.1  2004/12/14 22:47:22  edavis
 * Add simple interface to thredds.cataloggen and continue adding catalogRef capabilities.
 *
 */