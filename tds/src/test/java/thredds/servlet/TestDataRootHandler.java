package thredds.servlet;

import junit.framework.*;
import thredds.TestAll;
import thredds.catalog.*;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileOutputStream;

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

  protected void setUp()
  {
  }

  /**
   * Test behavior when a datasetScan@location results in
   * a CrDs whose exists() method returns false.
   */
  public void testNonexistentScanLocation()
  {
    // Create a temporary contentPath directory for this test.
    String contentPath = TestAll.temporaryDataDir + "TestDataRootHandler/testNonexistentScanLocation/contentPath/";
    File contentPathFile = new File( contentPath);
    if ( contentPathFile.exists() )
    {
      System.out.println( "**Deleting temporary content path <" + contentPath + "> from previous run." );
      if ( ! deleteDirectoryAndContent( contentPathFile ))
      {
        assertTrue( "Unable to delete already existing temporary content path directory <" + contentPathFile.getAbsolutePath() + ">.",
                    false );
        return;
      }
    }

    if ( ! contentPathFile.mkdirs() )
    {
      assertTrue( "Failed to make content path directory <" + contentPathFile.getAbsolutePath() + ">.",
                  false );
      return;
    }

    // Create a catalog with a datasetScan that points to a non-existent location.
    InvCatalogImpl configCat = null;
    configCat = new InvCatalogImpl( "Test TDS Config Catalog with nonexistent scan location", "1.0.1", null );

    InvService myService = new InvService( "ncdods", ServiceType.DODS.toString(),
                                           "/thredds/dodsC/", null, null );
    configCat.addService( myService );

    // Create the test datasetScan that points to nonexistent location.
    String dsScanName = "Test Nonexist Location";
    String dsScanPath = "testNonExistLoc";
    String dsScanLocation = "content/nonExistDir";
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
    String configCatName = "catalog.xml";
    String configCatPath = contentPath + configCatName;

    try
    {
      FileOutputStream fos = new FileOutputStream( configCatPath);
      InvCatalogFactory.getDefaultFactory( false).writeXML( configCat, fos, true);
      fos.close();
    }
    catch ( IOException e )
    {
      assertTrue( "I/O error writing config catalog <" + configCatPath + ">: " + e.getMessage(),
                  false );
      return;
    }

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( contentPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
      drh.initCatalog( configCatName );
    }
    catch ( FileNotFoundException e )
    {
      assertTrue( e.getMessage(),
                  false );
      return;
    }
    catch ( IOException e )
    {
      assertTrue( "I/O error while initializing catalog <" + configCatName + ">: " + e.getMessage(),
                  false );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      assertTrue( "IllegalArgumentException while initializing catalog <" + configCatName + ">: " + e.getMessage(),
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
