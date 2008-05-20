package thredds.cataloggen;

import junit.framework.*;

import java.io.File;
import java.io.IOException;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.catalog.InvService;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import ucar.nc2.TestAll;
import ucar.unidata.util.TestUtils;

/**
 * _more_
 *
 * @author edavis
 * @since Feb 1, 2007 10:20:23 PM
 */
public class TestCatGenAndWrite extends TestCase
{
  private File tmpDir;

  public TestCatGenAndWrite( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    // Create a data directory and some data files.
    tmpDir = TestUtils.addDirectory( new File( TestAll.temporaryDataDir ), "TestCatGenAndWrite" );
  }

  protected void tearDown()
  {
    // Delete temp directory.
    TestUtils.deleteDirectoryAndContent( tmpDir );
  }

  /**
   * Test ...
   */
  public void testLocalDataFiles()
  {
    String startPath = "dataDir";
    File dataDir = TestUtils.addDirectory( tmpDir, startPath );

    File eta211Dir = TestUtils.addDirectory( dataDir, "eta_211" );
    TestUtils.addFile( eta211Dir, "2004050300_eta_211.nc" );
    TestUtils.addFile( eta211Dir, "2004050312_eta_211.nc" );
    TestUtils.addFile( eta211Dir, "2004050400_eta_211.nc" );
    TestUtils.addFile( eta211Dir, "2004050412_eta_211.nc" );

    File gfs211Dir = TestUtils.addDirectory( dataDir, "gfs_211" );
    TestUtils.addFile( gfs211Dir, "2004050300_gfs_211.nc" );
    TestUtils.addFile( gfs211Dir, "2004050306_gfs_211.nc" );
    TestUtils.addFile( gfs211Dir, "2004050312_gfs_211.nc" );
    TestUtils.addFile( gfs211Dir, "2004050318_gfs_211.nc" );


    File catWriteDir = new File( tmpDir, "catWriteDir");
    CrawlableDataset collectionCrDs = new CrawlableDatasetFile( tmpDir );
    InvService service = new InvService( "myServer", "File", collectionCrDs.getPath() + "/", null, null );
    CrawlableDatasetFilter filter = null;
    CrawlableDataset topCatCrDs = collectionCrDs.getDescendant( startPath );

    CatGenAndWrite cgaw = null;
    try
    {
      cgaw = new CatGenAndWrite( "DATA", "My data", "", service,
                                 collectionCrDs, topCatCrDs, filter, null, catWriteDir );
    }
    catch ( IllegalArgumentException e )
    {
      fail( "Bad argument: " + e.getMessage() );
      return;
    }

    try
    {
      cgaw.genAndWriteCatalogTree();
    }
    catch ( IOException e )
    {
      fail( "I/O error generating and writing catalogs at and under \"" + topCatCrDs.getPath() + "\": " + e.getMessage() );
      return;
    }

    crawlCatalogs( new File( new File( catWriteDir, startPath), "catalog.xml") );
  }

  public void testLocalDataFilesOnTds()
  {
    String startPath = "dataDir";
    File dataDir = TestUtils.addDirectory( tmpDir, startPath );

    File eta211Dir = TestUtils.addDirectory( dataDir, "eta_211" );
    TestUtils.addFile( eta211Dir, "2004050300_eta_211.nc" );
    TestUtils.addFile( eta211Dir, "2004050312_eta_211.nc" );
    TestUtils.addFile( eta211Dir, "2004050400_eta_211.nc" );
    TestUtils.addFile( eta211Dir, "2004050412_eta_211.nc" );

    File gfs211Dir = TestUtils.addDirectory( dataDir, "gfs_211" );
    TestUtils.addFile( gfs211Dir, "2004050300_gfs_211.nc" );
    TestUtils.addFile( gfs211Dir, "2004050306_gfs_211.nc" );
    TestUtils.addFile( gfs211Dir, "2004050312_gfs_211.nc" );
    TestUtils.addFile( gfs211Dir, "2004050318_gfs_211.nc" );


    File catWriteDir = new File( tmpDir, "catWriteDir" );
    CrawlableDataset collectionCrDs = new CrawlableDatasetFile( tmpDir );

    InvService service = new InvService( "myServer", "OPeNDAP", "/thredds/dodsC/", null, null );
    CrawlableDatasetFilter filter = null;
    CrawlableDataset topCatCrDs = collectionCrDs.getDescendant( startPath );

    CatGenAndWrite cgaw = null;
    try
    {
      cgaw = new CatGenAndWrite( "DATA", "My data", "tdr", service,
                                 collectionCrDs, topCatCrDs, filter, null, catWriteDir );
    }
    catch ( IllegalArgumentException e )
    {
      fail( "Bad argument: " + e.getMessage() );
      return;
    }

    try
    {
      cgaw.genAndWriteCatalogTree();
    }
    catch ( IOException e )
    {
      fail( "I/O error generating and writing catalogs at and under \"" + topCatCrDs.getPath() + "\": " + e.getMessage() );
      return;
    }

    crawlCatalogs( new File( new File( catWriteDir, startPath), "catalog.xml") );
  }

  private void crawlCatalogs( File topCatalogFile)
  {
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false);
    InvCatalogImpl topCatalog = fac.readXML( topCatalogFile.toURI() );

    //topCatalog.g
    // TODO actually test something
  }
}
