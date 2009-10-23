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
package thredds.servlet;

import junit.framework.*;
import thredds.TestAll;
import thredds.util.TdsConfiguredPathAliasReplacement;
import thredds.util.PathAliasReplacement;
import thredds.server.config.TdsContext;
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
import java.util.Collections;
import java.net.URI;
import java.net.URISyntaxException;

import ucar.unidata.util.TestFileDirUtils;

import org.springframework.mock.web.MockServletContext;
import org.springframework.core.io.FileSystemResourceLoader;

/**
 * _more_
 *
 * @author edavis
 * @since Mar 21, 2007 1:07:18 PM
 */
public class TestDataRootHandler extends TestCase
{

//  private String tmpDirPath = TestAll.temporaryDataDir + "TestDataRootHandler/";
//  private String contentPath = tmpDirPath + "contentPath/";

  private File tmpDir;
  private File warRootDir;
  private File contentDir;
  private File publicContentDir;

  private DataRootHandler drh;

  public TestDataRootHandler( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    // Create a data directory and some data files.
    tmpDir = TestFileDirUtils.createTempDirectory( "TestDataRootHandler", new File( TestAll.temporaryDataDir ) );
    contentDir = TestFileDirUtils.addDirectory( TestFileDirUtils.addDirectory( tmpDir, "content" ), "thredds");
    publicContentDir = TestFileDirUtils.addDirectory( contentDir, "public");
    
    warRootDir = TestFileDirUtils.addDirectory( tmpDir, "dir/war" );
    TestFileDirUtils.addDirectory( warRootDir, "startup");
    TestFileDirUtils.addDirectory( warRootDir, "idd");
    TestFileDirUtils.addDirectory( warRootDir, "motherlode");
  }

  protected void tearDown()
  {
    // Delete temp directory.
    TestFileDirUtils.deleteDirectoryAndContent( tmpDir );
  }

  private void buildTdsContextAndDataRootHandler()
  {
    // Create, configure, and initialize a DataRootHandler.
    TdsContext tdsContext = new TdsContext();
    tdsContext.setWebappVersion( "0.0.0.0" );
    tdsContext.setWebappVersionBrief( "0.0" );
    tdsContext.setWebappVersionBuildDate( "20080904.2244" );
    tdsContext.setContentPath( "thredds" );
    tdsContext.setContentRootPath( "../../content" );
    tdsContext.setStartupContentPath( "startup" );
    tdsContext.setIddContentPath( "idd" );
    tdsContext.setMotherlodeContentPath( "motherlode" );
    tdsContext.setTdsConfigFileName( "threddsConfig.xml" );
    //MockServletContext sc = new MockServletContext( "target/war", new FileSystemResourceLoader() );
    MockServletContext sc = new MockServletContext( warRootDir.getPath(), new FileSystemResourceLoader() );
    sc.setContextPath( "/thredds" );
    sc.setServletContextName( "THREDDS Data Server" );
    tdsContext.init( sc );
    drh = new DataRootHandler( tdsContext ); // DataRootHandler.getInstance();
    PathAliasReplacement par = new TdsConfiguredPathAliasReplacement( "content" );
    drh.setDataRootLocationAliasExpanders( Collections.singletonList( par ) );
    drh.init();
    DataRootHandler.setInstance( drh );
  }

  /**
   * Test behavior when a datasetScan@location results in
   * a CrDs whose exists() method returns false.
   */
  public void testNonexistentScanLocation()
  {
    // Create a catalog with a datasetScan that points to a non-existent location
    // and write it to contentPath/catFilename.
    String catFilename = "catalog.xml";
    String catalogName = "Test TDS Config Catalog with nonexistent scan location";
    String dsScanName = "Test Nonexist Location";
    String dsScanPath = "testNonExistLoc";
    String dsScanLocation = "content/nonExistDir";

    InvCatalogImpl catalog = createConfigCatalog( catalogName, dsScanName, dsScanPath,
                                                  dsScanLocation, null, null, null );
    writeConfigCatalog( catalog, new File( contentDir, catFilename) );

    buildTdsContextAndDataRootHandler();

    // Check that bad dsScan wasn't added to DataRootHandler.
    if ( drh.hasDataRootMatch( dsScanPath) )
    {
      fail( "DataRootHandler has path match for DatasetScan <" + dsScanPath + ">." );
      return;
    }
  }

  /**
   * Test behavior when a datasetScan@location results in
   * a CrDs whose isCollection() method returns false.
   */
  public void testNondirectoryScanLocation()
  {
    // Create a non-directory file which will be the datasetScan@location value.
    File nondirectoryFile = new File( publicContentDir, "nonDirFile");
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
    writeConfigCatalog( catalog, new File( contentDir, catFilename) );

    buildTdsContextAndDataRootHandler();

    // Check that bad dsScan wasn't added to DataRootHandler.
    if ( drh.hasDataRootMatch( dsScanPath) )
    {
      fail( "DataRootHandler has path match for DatasetScan <" + dsScanPath + ">." );
      return;
    }
  }

  /**
   * Test behavior when a datasetScan@location results in a CrDs whose
   * listDatasets() returns a set of CrDs all of whose isCollection()
   * method returns false.
   */
  public void testScanLocationContainOnlyAtomicDatasets()
  {
    // Create public data directory in content path.
    File publicDataDir = new File( publicContentDir, "dataDir");
    if ( ! publicDataDir.mkdir() )
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
    writeConfigCatalog( catalog, new File( contentDir, catFilename) );

    buildTdsContextAndDataRootHandler();

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
    StringBuilder buf = new StringBuilder( "Generated catalog does not contain all the expected datasets (");
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
  }
  /**
   * Test behavior when dataset with name containing a plus sign ("+") is
   * found under a datasetScan@location.
   */
  public void testScanLocationContainsDatasetWithPlusSignInName()
  {
    // Create public data directory in content path.
    File publicDataDir = new File( publicContentDir, "dataDir");
    if ( ! publicDataDir.mkdir() )
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
    writeConfigCatalog( catalog, new File( contentDir, catFilename) );

    buildTdsContextAndDataRootHandler();

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
    StringBuilder buf = new StringBuilder( "Generated catalog does not contain all the expected datasets (");
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

//    // Now test with HTML view
//    HtmlWriter.init( "/thredds", "TDS", "ver", "docs/", "tds.css", "tdsCat.css", "thredds.jpg", "thredds", "unidataLogo.jpg", "Unidata", "folder.gif", "folder");
//    String catAsHtmlString = HtmlWriter.getInstance().convertCatalogToHtml( cat, true);
  }

  /**
   * Test behavior when a catalogRef@xlink:href, using ".." directory path
   * segments, points to a catalog outside of the content directory.
   */
  public void testCatRefOutOfContentDirUsingDotDotDirs()
  {
    File publicDataDir = TestFileDirUtils.addDirectory( publicContentDir, "dataDir" );
    File dataFileNc = TestFileDirUtils.addFile( publicDataDir, "data.nc");
    File dataFileGrib1 = TestFileDirUtils.addFile( publicDataDir, "data.grib1");
    File dataFileGrib2 = TestFileDirUtils.addFile( publicDataDir, "data.grib2");

    String mainCatFilename = "catalog.xml";

    // Write <tmp>/content/catalog.xml containing a datasetScan to the nc data
    // and a catalogRef to "mine.xml" (i.e., <tmp>/content/mine.xml).
    InvCatalogImpl mainCat = createConfigCatalog( "Main catalog", "netCDF Data", "ncData",
                                                   publicDataDir.getAbsolutePath(),
                                                   "*.nc", "mine", "mine.xml" );
    writeConfigCatalog( mainCat, new File( contentDir, mainCatFilename) );

    // Write <tmp>/content/mine.xml which contains a datasetScan to the grib1 data
    // and a catalogRef to "../catalog.xml (i.e., <tmp>/catalog.xml).
    InvCatalogImpl notAllowedRefCat = createConfigCatalog( "Cat that contains reference to a catalog outside the content directory",
                                                           "GRIB1 Data", "grib1Data",
                                                           publicDataDir.getAbsolutePath(),
                                                           "*.grib1", "noGoodCat", "../catalog.xml" );
    writeConfigCatalog( notAllowedRefCat, new File( contentDir, "mine.xml") );

    // Write <tmp>/catalog.xml which contains a datasetScan to the grib2 data.
    InvCatalogImpl outsideContentDirCat = createConfigCatalog( "Catalog outside of content directory",
                                                          "GRIB2 Data", "grib2Data",
                                                          publicDataDir.getAbsolutePath(),
                                                          "*.grib2", null, null );
    writeConfigCatalog( outsideContentDirCat, new File( contentDir, "../catalog.xml") );

    buildTdsContextAndDataRootHandler();

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
  }

  /**
   * Test canonicalization of paths to remove "./" and "../" directories.
   */
  public void testInitCatalogWithDotDotInPath()
  {
    String subDirName = "aSubDir";
    File subDir = TestFileDirUtils.addDirectory( contentDir, subDirName );

    String cat1Filename = "catalog1.xml";
    String path1 = cat1Filename;
    String cat2Filename = "catalog2.xml";
    String path2 = subDirName + "/../" + cat2Filename;

    // Write <tmp>/content/catalog1.xml, just an empty dataset.
    InvCatalogImpl catalog1 = createConfigCatalog( "catalog 1", null, null, null,
                                                   null, null, null );
    File cat1File = new File( contentDir, cat1Filename );
    writeConfigCatalog( catalog1, cat1File );

    // Write <tmp>/content/catalog2.xml, just an empty dataset.
    InvCatalogImpl catalog2 = createConfigCatalog( "catalog 2", null, null, null,
                                                   null, null, null );
    File cat2File = new File( contentDir, cat2Filename );
    writeConfigCatalog( catalog2, cat2File );

    buildTdsContextAndDataRootHandler();

    // Call DataRootHandler.initCatalogs() on the config catalogs
    List<String> catPaths = new ArrayList<String>();
    catPaths.add( path1 );
    catPaths.add( path2 );

    drh.reinit();
    drh.initCatalogs( catPaths );

    // Make sure DRH has "catalog1.xml".
    StringBuilder checkMsg = new StringBuilder();
    InvCatalogImpl cat1 = (InvCatalogImpl) drh.getCatalog( path1, cat1File.toURI() );
    assertNotNull( "Catalog1 <" + path1 + "> not found by DataRootHandler.", cat1 );

    assertTrue( "Catalog1 <" + path1 + "> not valid: " + checkMsg.toString(),
                cat1.check( checkMsg ) );
    if ( checkMsg.length() > 0 )
    {
      System.out.println( "Catalog1 <" + path1 + "> valid but had message: " + checkMsg.toString() );
      checkMsg = new StringBuilder();
    }

    // Make sure DRH does not have "aSubDir/../catalog2.xml".
    InvCatalogImpl cat2WithDotDot = (InvCatalogImpl) drh.getCatalog( path2, cat2File.toURI() );
    assertNull( "Catalog2 with bad-path (contains \"../\" directory) <" + path2 + "> found by DataRootHandler.",
                cat2WithDotDot);

    // Make sure DRH has "catalog2.xml".
    InvCatalogImpl cat2 = (InvCatalogImpl) drh.getCatalog( cat2Filename, cat2File.toURI() );
    assertNotNull( "Catalog2 with good-path <" + cat2Filename + "> not found by DataRootHandler.",
                   cat2);
    assertTrue( "Catalog2 with good-path <" + cat2Filename + "> not valid: " + checkMsg.toString(),
                cat2.check( checkMsg ) );
    if ( checkMsg.length() > 0 )
    {
      System.out.println( "Catalog2 with good-path <" + cat2Filename + "> valid but had message: " + checkMsg.toString() );
      checkMsg = new StringBuilder();
    }
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
