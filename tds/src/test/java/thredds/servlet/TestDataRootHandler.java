package thredds.servlet;

import junit.framework.*;
import thredds.TestAll;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.filter.WildcardMatchOnNameFilter;
import thredds.crawlabledataset.filter.MultiSelectorFilter;
import thredds.catalog.*;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;

import ucar.unidata.util.TestUtil;

/**
 * _more_
 *
 * @author edavis
 * @since Mar 21, 2007 1:07:18 PM
 */
public class TestDataRootHandler extends TestCase
{

  private String tmpDirPath = TestAll.temporaryDataDir + "TestDataRootHandler/";
  private String contentPath = tmpDirPath + "contentPath/";

  public TestDataRootHandler( String name )
  {
    super( name );
  }

  /**
   * Test behavior when a datasetScan@location results in
   * a CrDs whose exists() method returns false.
   */
  public void testNonexistentScanLocation()
  {
    // Create a temporary contentPath directory for this test.
    File contentPathFile = TestUtil.createDirectory( contentPath );
    try
    {
      contentPathFile = contentPathFile.getCanonicalFile();
    }
    catch ( IOException e )
    {
      fail( "I/O error getting canonical file for content path <" + contentPath + ">: " + e.getMessage());
      return;
    }
    String fullCanonicalContentPath = contentPathFile.getAbsolutePath() + "/";
    fullCanonicalContentPath = fullCanonicalContentPath.replace( '\\', '/' );

    // Create a catalog with a datasetScan that points to a non-existent location
    // and write it to contentPath/catFilename.
    String catFilename = "catalog.xml";
    String catalogName = "Test TDS Config Catalog with nonexistent scan location";
    String dsScanName = "Test Nonexist Location";
    String dsScanPath = "testNonExistLoc";
    String dsScanLocation = "content/nonExistDir";

    InvCatalogImpl catalog = createConfigCatalog( catalogName, dsScanName, dsScanPath,
                                                  dsScanLocation, null, null, null );
    writeConfigCatalog( catalog, new File( contentPathFile, catFilename) );

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( fullCanonicalContentPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
      drh.reinit();
      drh.initCatalog( catFilename );
    }
    catch ( FileNotFoundException e )
    {
      fail( e.getMessage() );
      return;
    }
    catch ( IOException e )
    {
      fail( "I/O error while initializing catalog <" + catFilename + ">: " + e.getMessage() );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      fail( "IllegalArgumentException while initializing catalog <" + catFilename + ">: " + e.getMessage() );
      return;
    }

    // Check that bad dsScan wasn't added to DataRootHandler.
    if ( drh.hasDataRootMatch( dsScanPath) )
    {
      fail( "DataRootHandler has path match for DatasetScan <" + dsScanPath + ">." );
      return;
    }

    // Remove temporary contentPath dir and contents
    TestUtil.deleteDirectoryAndContent( contentPathFile );
  }

  /**
   * Test behavior when a datasetScan@location results in
   * a CrDs whose isCollection() method returns false.
   */
  public void testNondirectoryScanLocation()
  {
    // Create a temporary contentPath directory for this test.
    File contentPathFile = TestUtil.createDirectory( contentPath );
    try
    {
      contentPathFile = contentPathFile.getCanonicalFile();
    }
    catch ( IOException e )
    {
      fail( "I/O error getting canonical file for content path <" + contentPath + ">: " + e.getMessage() );
      return;
    }
    String fullCanonicalContentPath = contentPathFile.getAbsolutePath() + "/";
    fullCanonicalContentPath = fullCanonicalContentPath.replace( '\\', '/' );

    // Create public directory in content path.
    File publicDirectoryFile = new File( contentPathFile, "public");
    if ( !publicDirectoryFile.mkdirs() )
    {
      fail( "Failed to make content path \"public\" directory <" + publicDirectoryFile.getAbsolutePath() + ">." );
      return;
    }

    // Create a non-directory file which will be the datasetScan@location value.
    File nondirectoryFile = new File( publicDirectoryFile, "nonDirFile");
    try
    {
      if ( ! nondirectoryFile.createNewFile())
      {
        fail( "Could not create nondirectory file <" + nondirectoryFile.getAbsolutePath() + ">." );
        return;
      }
    }
    catch ( IOException e )
    {
      fail( "I/O error creating nondirectory file <" + nondirectoryFile.getAbsolutePath() + ">: " + e.getMessage() );
      return;
    }

    // Create a catalog with a datasetScan that points to a non-directory location
    // and write it to contentPath/catFilename.
    String catFilename = "catalog.xml";
    String catalogName = "Test TDS Config Catalog with nondirectory scan location";
    String dsScanName = "Test Nondirectory Location";
    String dsScanPath = "testNonDirLoc";
    String dsScanLocation = "content/nonDirFile";

    InvCatalogImpl catalog = createConfigCatalog( catalogName, dsScanName, dsScanPath,
                                                  dsScanLocation, null, null, null );
    writeConfigCatalog( catalog, new File( contentPathFile, catFilename) );

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( fullCanonicalContentPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
      drh.reinit();
      drh.initCatalog( catFilename );
    }
    catch ( FileNotFoundException e )
    {
      assertTrue( e.getMessage(),
                  false );
      return;
    }
    catch ( IOException e )
    {
      fail( "I/O error while initializing catalog <" + catFilename + ">: " + e.getMessage() );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      fail( "IllegalArgumentException while initializing catalog <" + catFilename + ">: " + e.getMessage() );
      return;
    }

    // Check that bad dsScan wasn't added to DataRootHandler.
    if ( drh.hasDataRootMatch( dsScanPath) )
    {
      fail( "DataRootHandler has path match for DatasetScan <" + dsScanPath + ">." );
      return;
    }

    // Remove temporary contentPath dir and contents
    TestUtil.deleteDirectoryAndContent( contentPathFile );
  }

  /**
   * Test behavior when a datasetScan@location results in a CrDs whose
   * listDatasets() returns a set of CrDs all of whose isCollection()
   * method returns false.
   */
  public void testScanLocationContainOnlyAtomicDatasets()
  {
    // Create a temporary contentPath directory for this test.
    File contentPathFile = TestUtil.createDirectory( contentPath );
    try
    {
      contentPathFile = contentPathFile.getCanonicalFile();
    }
    catch ( IOException e )
    {
      fail( "I/O error getting canonical file for content path <" + contentPath + ">: " + e.getMessage() );
      return;
    }
    String fullCanonicalContentPath = contentPathFile.getAbsolutePath() + "/";
    fullCanonicalContentPath = fullCanonicalContentPath.replace( '\\', '/' );

    // Create public data directory in content path.
    File publicDataDir = new File( contentPathFile, "public/dataDir");
    if ( ! publicDataDir.mkdirs() )
    {
      fail( "Failed to make content path public data directory <" + publicDataDir.getAbsolutePath() + ">." );
      return;
    }

    // Create some data files in data directory
    List<String> dataFileNames = new ArrayList<String>();
    dataFileNames.add( "file1.nc");
    dataFileNames.add( "file2.nc");
    dataFileNames.add( "file3.nc");
    dataFileNames.add( "file4.nc");
    for ( String curName : dataFileNames )
    {
      File curFile = new File( publicDataDir, curName );
      try
      {
        if ( ! curFile.createNewFile() )
        {
          fail( "Could not create data file <" + curFile.getAbsolutePath() + ">." );
          return;
        }
      }
      catch ( IOException e )
      {
        fail( "I/O error creating data file <" + curFile.getAbsolutePath() + ">: " + e.getMessage() );
        return;
      }
    }

    // Create a catalog with a datasetScan whose location contains some atomic datasets
    // and write the catalog to contentPath/catFilename.
    String catFilename = "catalog.xml";
    String catalogName = "Test TDS Config Catalog with scan location containing atomic datasets";
    String dsScanName = "Test scan location containing only atomicadDatasets";
    String dsScanPath = "testScanLocationContainOnlyAtomicDatasets";
    String dsScanLocation = "content/dataDir";
    String catReqPath = dsScanPath + "/" + catFilename;

    InvCatalogImpl catalog = createConfigCatalog( catalogName, dsScanName, dsScanPath,
                                                  dsScanLocation, null, null, null );
    writeConfigCatalog( catalog, new File( contentPathFile, catFilename) );

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( fullCanonicalContentPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
      drh.reinit();
      drh.initCatalog( catFilename );
    }
    catch ( FileNotFoundException e )
    {
      fail( e.getMessage() );
      return;
    }
    catch ( IOException e )
    {
      fail( "I/O error while initializing catalog <" + catFilename + ">: " + e.getMessage() );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      fail( "IllegalArgumentException while initializing catalog <" + catFilename + ">: " + e.getMessage() );
      return;
    }

    // Check that dsScan was added to DataRootHandler.
    if ( ! drh.hasDataRootMatch( dsScanPath) )
    {
      fail( "DataRootHandler has no path match for DatasetScan <" + dsScanPath + ">." );
      return;
    }
    
    // Read in catalog and check that it contains expected datasets.
    URI uri = null;
    try
    {
      uri = new URI( catReqPath );
    }
    catch ( URISyntaxException e )
    {
      fail( "DataRootHandler has no path match for DatasetScan <" + dsScanPath + ">: " + e.getMessage() );
      return;
    }
    InvCatalogImpl cat = (InvCatalogImpl) drh.getCatalog( catReqPath, uri );

    InvDatasetImpl topDs = (InvDatasetImpl) cat.getDatasets().get(0);
    if ( topDs.getDatasets().size() != dataFileNames.size())
    {
      fail( "Number of datasets in generated catalog <" + topDs.getDatasets().size() + "> not as expected <" + dataFileNames.size() + ">." );
      return;
    }
    boolean ok = true;
    StringBuffer buf = new StringBuffer( "Generated catalog does not contain all the expected datasets (");
    for ( String curName : dataFileNames )
    {
      if ( topDs.findDatasetByName( curName) == null )
      {
        ok = false;
        buf.append( curName).append( ", ");
      }
    }
    if ( ! ok )
    {
      buf.setLength( buf.lastIndexOf( ","));
      buf.append( ").");
      fail( buf.toString() );
      return;
    }

    // Remove temporary contentPath dir and contents
    TestUtil.deleteDirectoryAndContent( contentPathFile );
  }
  /**
   * Test behavior when dataset with name containing a plus sign ("+") is
   * found under a datasetScan@location.
   */
  public void testScanLocationContainsDatasetWithPlusSignInName()
  {
    // Create a temporary contentPath directory for this test.
    File contentPathFile = TestUtil.createDirectory( contentPath );
    try
    {
      contentPathFile = contentPathFile.getCanonicalFile();
    }
    catch ( IOException e )
    {
      fail( "I/O error getting canonical file for content path <" + contentPath + ">: " + e.getMessage() );
      return;
    }
    String fullCanonicalContentPath = contentPathFile.getAbsolutePath() + "/";
    fullCanonicalContentPath = fullCanonicalContentPath.replace( '\\', '/' );

    // Create public data directory in content path.
    File publicDataDir = new File( contentPathFile, "public/dataDir");
    if ( ! publicDataDir.mkdirs() )
    {
      fail( "Failed to make content path public data directory <" + publicDataDir.getAbsolutePath() + ">." );
      return;
    }

    // Create some data files in data directory
    List<String> dataFileNames = new ArrayList<String>();
    dataFileNames.add( "file1.nc");
    dataFileNames.add( "file2.nc");
    dataFileNames.add( "file3.nc");
    dataFileNames.add( "file3+.nc");
    dataFileNames.add( "file4.nc");
    for ( String curName : dataFileNames )
    {
      File curFile = new File( publicDataDir, curName );
      try
      {
        if ( ! curFile.createNewFile() )
        {
          fail( "Could not create data file <" + curFile.getAbsolutePath() + ">." );
          return;
        }
      }
      catch ( IOException e )
      {
        fail( "I/O error creating data file <" + curFile.getAbsolutePath() + ">: " + e.getMessage() );
        return;
      }
    }

    // Create a catalog with a datasetScan whose location contains an atomic dataset
    // with a plus sign ("+") in its name. Write the catalog to contentPath/catFilename.
    String catFilename = "catalog.xml";
    String catalogName = "Test TDS Config Catalog where scan location contains an atomic dataset with a plus sign in its name.";
    String dsScanName = "Test scan location containing an atomic dataset with plus sign in name";
    String dsScanPath = "testScanLocationContainsDatasetWithPlusSignInName";
    String dsScanLocation = "content/dataDir";
    String catReqPath = dsScanPath + "/" + catFilename;

    InvCatalogImpl catalog = createConfigCatalog( catalogName, dsScanName, dsScanPath,
                                                  dsScanLocation, null, null, null );
    writeConfigCatalog( catalog, new File( contentPathFile, catFilename) );

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( fullCanonicalContentPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
      drh.reinit();
      drh.initCatalog( catFilename );
    }
    catch ( FileNotFoundException e )
    {
      fail( e.getMessage() );
      return;
    }
    catch ( IOException e )
    {
      fail( "I/O error while initializing catalog <" + catFilename + ">: " + e.getMessage() );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      fail( "IllegalArgumentException while initializing catalog <" + catFilename + ">: " + e.getMessage() );
      return;
    }

    // Check that dsScan was added to DataRootHandler.
    if ( ! drh.hasDataRootMatch( dsScanPath) )
    {
      fail( "DataRootHandler has no path match for DatasetScan <" + dsScanPath + ">." );
      return;
    }

    // Read in catalog and check that it contains expected datasets.
    URI uri = null;
    try
    {
      uri = new URI( catReqPath );
    }
    catch ( URISyntaxException e )
    {
      fail( "DataRootHandler has no path match for DatasetScan <" + dsScanPath + ">: " + e.getMessage() );
      return;
    }
    InvCatalogImpl cat = (InvCatalogImpl) drh.getCatalog( catReqPath, uri );

    InvDatasetImpl topDs = (InvDatasetImpl) cat.getDatasets().get(0);
    if ( topDs.getDatasets().size() != dataFileNames.size())
    {
      fail( "Number of datasets in generated catalog <" + topDs.getDatasets().size() + "> not as expected <" + dataFileNames.size() + ">." );
      return;
    }
    boolean ok = true;
    StringBuffer buf = new StringBuffer( "Generated catalog does not contain all the expected datasets (");
    for ( String curName : dataFileNames )
    {
      if ( topDs.findDatasetByName( curName) == null )
      {
        ok = false;
        buf.append( curName).append( ", ");
      }
    }
    if ( ! ok )
    {
      buf.setLength( buf.lastIndexOf( ","));
      buf.append( ").");
      fail( buf.toString() );
      return;
    }

    // Now test with HTML view
    HtmlWriter.init( "/thredds", "TDS", "ver", "docs/", "tds.css", "tdsCat.css", "thredds.jpg", "thredds", "unidataLogo.jpg", "Unidata", "folder.gif", "folder");
    String catAsHtmlString = HtmlWriter.getInstance().convertCatalogToHtml( cat, true);

    // Remove temporary contentPath dir and contents
    TestUtil.deleteDirectoryAndContent( contentPathFile );
  }

  /**
   * Test behavior when a catalogRef@xlink:href, using ".." directory path
   * segments, points to a catalog outside of the content directory.
   */
  public void testCatRefOutOfContentDirUsingDotDotDirs()
  {
    // Create a temporary directory and a child content directory for this test.
    File tmpDir = TestUtil.createDirectory( tmpDirPath );
    File contentPathFile = TestUtil.createDirectory( contentPath ); // child of tmpDir
    try
    {
      contentPathFile = contentPathFile.getCanonicalFile();
    }
    catch ( IOException e )
    {
      fail( "I/O error getting canonical file for content path <" + contentPath + ">: " + e.getMessage() );
      return;
    }
    String fullCanonicalContentPath = contentPathFile.getAbsolutePath() + "/";
    fullCanonicalContentPath = fullCanonicalContentPath.replace( '\\', '/');


    File publicDataDir = TestUtil.addDirectory( contentPathFile, "public/dataDir" );
    File dataFileNc = TestUtil.addFile( publicDataDir, "data.nc");
    File dataFileGrib1 = TestUtil.addFile( publicDataDir, "data.grib1");
    File dataFileGrib2 = TestUtil.addFile( publicDataDir, "data.grib2");

    String mainCatFilename = "catalog.xml";

    // Write <tmp>/content/catalog.xml containing a datasetScan to the nc data
    // and a catalogRef to "mine.xml" (i.e., <tmp>/content/mine.xml).
    InvCatalogImpl mainCat = createConfigCatalog( "Main catalog", "netCDF Data", "ncData",
                                                   publicDataDir.getAbsolutePath(),
                                                   "*.nc", "mine", "mine.xml" );
    writeConfigCatalog( mainCat, new File( contentPathFile, mainCatFilename) );

    // Write <tmp>/content/mine.xml which contains a datasetScan to the grib1 data
    // and a catalogRef to "../catalog.xml (i.e., <tmp>/catalog.xml).
    InvCatalogImpl notAllowedRefCat = createConfigCatalog( "Cat that contains reference to a catalog outside the content directory",
                                                           "GRIB1 Data", "grib1Data",
                                                           publicDataDir.getAbsolutePath(),
                                                           "*.grib1", "noGoodCat", "../catalog.xml" );
    writeConfigCatalog( notAllowedRefCat, new File( contentPathFile, "mine.xml") );

    // Write <tmp>/catalog.xml which contains a datasetScan to the grib2 data.
    InvCatalogImpl outsideContentDirCat = createConfigCatalog( "Catalog outside of content directory",
                                                          "GRIB2 Data", "grib2Data",
                                                          publicDataDir.getAbsolutePath(),
                                                          "*.grib2", null, null );
    writeConfigCatalog( outsideContentDirCat, new File( tmpDir, "catalog.xml") );

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( fullCanonicalContentPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
      drh.reinit();
      drh.initCatalog( mainCatFilename );
    }
    catch ( FileNotFoundException e )
    {
      fail( e.getMessage() );
      return;
    }
    catch ( IOException e )
    {
      fail( "I/O error while initializing catalog <" + mainCatFilename + ">: " + e.getMessage() );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      fail( "IllegalArgumentException while initializing catalog <" + mainCatFilename + ">: " + e.getMessage() );
      return;
    }
    catch ( StringIndexOutOfBoundsException e )
    {
      fail( "Failed to initialized catalog <" + mainCatFilename + ">: " + e.getMessage());
      return;
    }

    // Make sure DRH does not have "../catalog.xml".
    String dotDotPath = "../catalog.xml";
    InvCatalogImpl dotDotCatalog = (InvCatalogImpl) drh.getCatalog( dotDotPath, new File( "" ).toURI() );
    assertTrue( "DRH has catalog for non-canonical path <" + dotDotPath + "> which is outside of content directory.",
                dotDotCatalog == null );

//    if ( drh.hasDataRootMatch( "../catalog.xml") )
//    {
//      fail( "DataRootHandler has match for \"../catalog.xml\" which is outside content directory.");
//      return;
//    }

    // Remove temporary contentPath dir and contents
    TestUtil.deleteDirectoryAndContent( contentPathFile );
  }

  /**
   * Test canonicalization of paths to remove "./" and "../" directories.
   */
  public void testInitCatalogWithDotDotInPath()
  {
    // Create a temporary directory and in that a content directory and in that a subdirectory.
    File tmpDir = TestUtil.createDirectory( tmpDirPath );
    File contentPathFile = TestUtil.createDirectory( contentPath ); // child of tmpDir
    String subDirName = "aSubDir";
    File subDir = TestUtil.addDirectory( contentPathFile, subDirName );
    try
    {
      contentPathFile = contentPathFile.getCanonicalFile();
    }
    catch ( IOException e )
    {
      fail( "I/O error getting canonical file for content path <" + contentPath + ">: " + e.getMessage() );
      return;
    }
    String fullCanonicalContentPath = contentPathFile.getAbsolutePath() + "/";
    fullCanonicalContentPath = fullCanonicalContentPath.replace( '\\', '/');


    String cat1Filename = "catalog1.xml";
    String path1 = cat1Filename;
    String cat2Filename = "catalog2.xml";
    String path2 = subDirName + "/../" + cat2Filename;

    // Write <tmp>/content/catalog1.xml, just an empty dataset.
    InvCatalogImpl catalog1 = createConfigCatalog( "catalog 1", null, null, null,
                                                   null, null, null );
    writeConfigCatalog( catalog1, new File( contentPathFile, cat1Filename) );

    // Write <tmp>/content/catalog1.xml, just an empty dataset.
    InvCatalogImpl catalog2 = createConfigCatalog( "catalog 2", null, null, null,
                                                   null, null, null );
    writeConfigCatalog( catalog2, new File( contentPathFile, cat2Filename) );

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( fullCanonicalContentPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
      drh.reinit();
      drh.initCatalog( path1 );
      drh.initCatalog( path2 );
    }
    catch ( FileNotFoundException e )
    {
      fail( e.getMessage() );
      return;
    }
    catch ( IOException e )
    {
      fail( "I/O error while initializing catalog <" + cat1Filename + ">: " + e.getMessage() );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      fail( "IllegalArgumentException while initializing catalog <" + cat1Filename + ">: " + e.getMessage() );
      return;
    }
    catch ( StringIndexOutOfBoundsException e )
    {
      fail( "Failed to initialized catalog <" + cat1Filename + ">: " + e.getMessage());
      return;
    }


    StringBuffer checkMsg = new StringBuffer();

    // Make sure DRH has "catalog1.xml".
    InvCatalogImpl cat1 = (InvCatalogImpl) drh.getCatalog( path1, new File( "" ).toURI() );
    if ( cat1 == null )
    {
      fail( "Catalog1 <" + path1 + "> not found by DataRootHandler." );
      return;
    }
    assertTrue( "Catalog1 <" + path1 + "> not valid: " + checkMsg.toString(),
                cat1.check( checkMsg ) );
    if ( checkMsg.length() > 0 )
    {
      System.out.println( "Catalog1 <" + path1 + "> valid but had message: " + checkMsg.toString() );
      checkMsg = new StringBuffer();
    }

    // Make sure DRH does not have "aSubDir/../catalog2.xml".
    InvCatalogImpl cat2WithDotDot = (InvCatalogImpl) drh.getCatalog( path2, new File( "" ).toURI() );
    if ( cat2WithDotDot != null )
    {
      fail( "Catalog2 with bad-path (contains \"../\" directory) <" + path2 + "> found by DataRootHandler." );
      return;
    }

    // Make sure DRH has "catalog2.xml".
    InvCatalogImpl cat2 = (InvCatalogImpl) drh.getCatalog( cat2Filename, new File( "" ).toURI() );
    if ( cat2 == null )
    {
      fail( "Catalog2 with good-path <" + cat2Filename + "> not found by DataRootHandler." );
      return;
    }
    assertTrue( "Catalog2 with good-path <" + cat2Filename + "> not valid: " + checkMsg.toString(),
                cat2.check( checkMsg ) );
    if ( checkMsg.length() > 0 )
    {
      System.out.println( "Catalog2 with good-path <" + cat2Filename + "> valid but had message: " + checkMsg.toString() );
      checkMsg = new StringBuffer();
    }

    // Remove temporary contentPath dir and contents
    TestUtil.deleteDirectoryAndContent( contentPathFile );
  }

  private InvCatalogImpl createConfigCatalog( String catalogName,
                                              String dsScanName,
                                              String dsScanPath,
                                              String dsScanLocation,
                                              String filterWildcardString,
                                              String catRefTitle, String catRefHref )
  {
    // Create empty catalog.
    InvCatalogImpl configCat = new InvCatalogImpl( null, "1.0.1", null );

    // Add OPeNDAP service to catalog.
    InvService myService = new InvService( "ncdods", ServiceType.OPENDAP.toString(),
                                           "/thredds/dodsC/", null, null );
    configCat.addService( myService );

    // Create top level dataset and add to catalog.
    InvDatasetImpl topDs = new InvDatasetImpl( null, catalogName );
    configCat.addDataset( topDs );

    // Add service as inherited metadata to top level dataset.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( topDs, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    topDs.setLocalMetadata( tm2 );

    // Create the test datasetScan.
    if ( dsScanName != null && dsScanPath != null && dsScanLocation != null )
    {
      CrawlableDatasetFilter filter = null;
      if ( filterWildcardString != null && ! filterWildcardString.equals( "") )
        filter = new MultiSelectorFilter( new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( filterWildcardString ), true, true, false ) );

      InvDatasetScan dsScan = new InvDatasetScan( topDs, dsScanName, dsScanPath,
                                                  dsScanLocation, null, null, filter, null, null,
                                                  true, null, null, null, null );

      topDs.addDataset( dsScan );
    }

    // Create the test catalogRef.
    if ( catRefTitle != null && catRefHref != null )
    {
      InvCatalogRef catRef = new InvCatalogRef( topDs, catRefTitle, catRefHref );
      topDs.addDataset( catRef );
    }

    configCat.finish();
    return configCat;
  }

  private void writeConfigCatalog( InvCatalogImpl catalog, File configCatFile )
  {
    // Write the config catalog
    try
    {
      FileOutputStream fos = new FileOutputStream( configCatFile );
      InvCatalogFactory.getDefaultFactory( false ).writeXML( catalog, fos, true );
      fos.close();
    }
    catch ( IOException e )
    {
      fail( "I/O error writing config catalog <" + configCatFile.getPath() + ">: " + e.getMessage() );
    }
  }

}
