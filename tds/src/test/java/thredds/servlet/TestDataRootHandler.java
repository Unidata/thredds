package thredds.servlet;

import junit.framework.*;
import thredds.TestAll;
import thredds.catalog.*;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since Mar 21, 2007 1:07:18 PM
 */
public class TestDataRootHandler extends TestCase
{
//  static private org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( TestDataRootHandler.class );

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
    String contentPath = TestAll.temporaryDataDir + "TestDataRootHandler/testNonexistentScanLocation/contentPath/";
    File contentPathFile = createContentPathFile( contentPath );

    // Create a catalog with a datasetScan that points to a non-existent location
    // and write it to contentPath/catFilename.
    String catFilename = "catalog.xml";
    String catalogName = "Test TDS Config Catalog with nonexistent scan location";
    String dsScanName = "Test Nonexist Location";
    String dsScanPath = "testNonExistLoc";
    String dsScanLocation = "content/nonExistDir";

    createAndWriteConfigCatalog( catalogName, dsScanName, dsScanPath, dsScanLocation, contentPath, catFilename );

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( contentPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
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
      assertTrue( "I/O error while initializing catalog <" + catFilename + ">: " + e.getMessage(),
                  false );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      assertTrue( "IllegalArgumentException while initializing catalog <" + catFilename + ">: " + e.getMessage(),
                  false );
      return;
    }

    // Check that bad dsScan wasn't added to DataRootHandler.
    if ( drh.hasDataRootMatch( dsScanPath) )
    {
      assertTrue( "DataRootHandler has path match for DatasetScan <" + dsScanPath + ">.",
                  false );
      return;
    }

    // Remove temporary contentPath dir and contents
    deleteDirectoryAndContent( contentPathFile );
  }

  /**
   * Test behavior when a datasetScan@location results in
   * a CrDs whose isCollection() method returns false.
   */
  public void testNondirectoryScanLocation()
  {
    // Create a temporary contentPath directory for this test.
    String contentPath = TestAll.temporaryDataDir + "TestDataRootHandler/testNondirectoryScanLocation/contentPath/";
    File contentPathFile = createContentPathFile( contentPath );

    // Create public directory in content path.
    File publicDirectoryFile = new File( contentPathFile, "public");
    if ( !publicDirectoryFile.mkdirs() )
    {
      assertTrue( "Failed to make content path \"public\" directory <" + publicDirectoryFile.getAbsolutePath() + ">.",
                  false );
      return;
    }

    // Create a non-directory file which will be the datasetScan@location value.
    File nondirectoryFile = new File( publicDirectoryFile, "nonDirFile");
    try
    {
      if ( ! nondirectoryFile.createNewFile())
      {
        assertTrue( "Could not create nondirectory file <" + nondirectoryFile.getAbsolutePath() + ">.",
                    false);
        return;
      }
    }
    catch ( IOException e )
    {
      assertTrue( "I/O error creating nondirectory file <" + nondirectoryFile.getAbsolutePath() + ">: " + e.getMessage(),
                  false );
      return;
    }

    // Create a catalog with a datasetScan that points to a non-directory location
    // and write it to contentPath/catFilename.
    String catFilename = "catalog.xml";
    String catalogName = "Test TDS Config Catalog with nondirectory scan location";
    String dsScanName = "Test Nondirectory Location";
    String dsScanPath = "testNonDirLoc";
    String dsScanLocation = "content/nonDirFile";

    createAndWriteConfigCatalog( catalogName, dsScanName, dsScanPath, dsScanLocation, contentPath, catFilename );

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( contentPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
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
      assertTrue( "I/O error while initializing catalog <" + catFilename + ">: " + e.getMessage(),
                  false );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      assertTrue( "IllegalArgumentException while initializing catalog <" + catFilename + ">: " + e.getMessage(),
                  false );
      return;
    }

    // Check that bad dsScan wasn't added to DataRootHandler.
    if ( drh.hasDataRootMatch( dsScanPath) )
    {
      assertTrue( "DataRootHandler has path match for DatasetScan <" + dsScanPath + ">.",
                  false );
      return;
    }

    // Remove temporary contentPath dir and contents
    deleteDirectoryAndContent( contentPathFile );
  }

  /**
   * Test behavior when a datasetScan@location results in a CrDs whose
   * listDatasets() returns a set of CrDs all of whose isCollection()
   * method returns false.
   */
  public void testScanLocationContainOnlyAtomicDatasets()
  {
    // Create a temporary contentPath directory for this test.
    String contentPath = TestAll.temporaryDataDir + "TestDataRootHandler/testScanLocationContainOnlyAtomicDatasets/contentPath/";
    File contentPathFile = createContentPathFile( contentPath );

    // Create public data directory in content path.
    File publicDataDir = new File( contentPathFile, "public/dataDir");
    if ( ! publicDataDir.mkdirs() )
    {
      assertTrue( "Failed to make content path public data directory <" + publicDataDir.getAbsolutePath() + ">.",
                  false );
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
          assertTrue( "Could not create data file <" + curFile.getAbsolutePath() + ">.",
                      false );
          return;
        }
      }
      catch ( IOException e )
      {
        assertTrue( "I/O error creating data file <" + curFile.getAbsolutePath() + ">: " + e.getMessage(),
                    false );
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

    createAndWriteConfigCatalog( catalogName, dsScanName, dsScanPath, dsScanLocation, contentPath, catFilename );

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( contentPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
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
      assertTrue( "I/O error while initializing catalog <" + catFilename + ">: " + e.getMessage(),
                  false );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      assertTrue( "IllegalArgumentException while initializing catalog <" + catFilename + ">: " + e.getMessage(),
                  false );
      return;
    }

    // Check that dsScan was added to DataRootHandler.
    if ( ! drh.hasDataRootMatch( dsScanPath) )
    {
      assertTrue( "DataRootHandler has no path match for DatasetScan <" + dsScanPath + ">.",
                  false );
      return;
    }
    
    // Read in catalog and check that it contains expected datasets.
    String path = "testScanLocationContainOnlyAtomicDatasets/catalog.xml";
    URI uri = null;
    try
    {
      uri = new URI( path );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "DataRootHandler has no path match for DatasetScan <" + dsScanPath + ">: " + e.getMessage(),
                  false );
      return;
    }
    InvCatalogImpl cat = (InvCatalogImpl) drh.getCatalog( path, uri );

    InvDatasetImpl topDs = (InvDatasetImpl) cat.getDatasets().get(0);
    if ( topDs.getDatasets().size() != dataFileNames.size())
    {
      assertTrue( "Number of datasets in generated catalog <" + topDs.getDatasets().size() + "> not as expected <" + dataFileNames.size() + ">.",
                  false );
      return;
    }
    boolean ok = true;
    StringBuffer buf = new StringBuffer( "Generated catalog does not contain all the expected datasets (");
    for ( String curName : dataFileNames )
    {
      if ( topDs.findDatasetByName( curName) != null )
      {
        ok = false;
        buf.append( curName).append( ", ");
      }
    }
    if ( ! ok )
    {
      buf.setLength( buf.lastIndexOf( ","));
      buf.append( ").");
      assertTrue( buf.toString(), false );
      return;
    }

    // Remove temporary contentPath dir and contents
    deleteDirectoryAndContent( contentPathFile );
  }

  private File createContentPathFile( String contentPath )
  {
    File contentPathFile = new File( contentPath );
    if ( contentPathFile.exists() )
    {
      System.out.println( "**Deleting temporary content path <" + contentPath + "> from previous run." );
      if ( !deleteDirectoryAndContent( contentPathFile ) )
      {
        assertTrue( "Unable to delete already existing temporary content path directory <" + contentPathFile.getAbsolutePath() + ">.",
                    false );
        return null;
      }
    }

    if ( !contentPathFile.mkdirs() )
    {
      assertTrue( "Failed to make content path directory <" + contentPathFile.getAbsolutePath() + ">.",
                  false );
      return null;
    }
    return contentPathFile;
  }

  private void createAndWriteConfigCatalog( String catalogName, String dsScanName, String dsScanPath, String dsScanLocation, String contentPath, String catFilename )
  {
    InvCatalogImpl configCat = new InvCatalogImpl( catalogName, "1.0.1", null );

    InvService myService = new InvService( "ncdods", ServiceType.DODS.toString(),
                                           "/thredds/dodsC/", null, null );
    configCat.addService( myService );

    // Create the test datasetScan that points to nonexistent location.
    InvDatasetScan dsScan = new InvDatasetScan( null, dsScanName, dsScanPath,
                                                dsScanLocation, null, null, null, null, null,
                                                true, null, null, null, null );
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( myService.getName() );
    InvMetadata md = new InvMetadata( dsScan, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    dsScan.setLocalMetadata( tm2 );

    configCat.addDataset( dsScan );

    configCat.finish();

    // Write the config catalog into the contentPath directory
    String configCatPath = contentPath + catFilename;

    try
    {
      FileOutputStream fos = new FileOutputStream( configCatPath );
      InvCatalogFactory.getDefaultFactory( false ).writeXML( configCat, fos, true );
      fos.close();
    }
    catch ( IOException e )
    {
      assertTrue( "I/O error writing config catalog <" + configCatPath + ">: " + e.getMessage(),
                  false );
    }
  }

  /**
   * Delete the given directory including any files or directories contained in the directory.
   *
   * @param directory the directory to remove
   * @return true if and only if the file or directory is successfully deleted; false otherwise.
   */
  private boolean deleteDirectoryAndContent( File directory )
  {
    if ( ! directory.exists() ) return false;
    if ( ! directory.isDirectory() ) return false;

    boolean removeAll = true;

    File[] files = directory.listFiles();
    for ( int i = 0; i < files.length; i++ )
    {
      File curFile = files[i];
      if ( curFile.isDirectory() )
      {
        removeAll &= deleteDirectoryAndContent( curFile);
      }
      else
      {
        if ( ! curFile.delete())
        {
          System.out.println( "**Failed to delete file <" + curFile.getAbsolutePath() + ">" );
          removeAll = false;
        }
      }
    }

    if ( ! directory.delete() )
    {
      System.out.println( "**Failed to delete directory <" + directory.getAbsolutePath() + ">" );
      removeAll = false;
    }

    return removeAll;
  }
}
