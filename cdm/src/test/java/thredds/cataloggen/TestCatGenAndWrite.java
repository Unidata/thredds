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

/**
 * _more_
 *
 * @author edavis
 * @since Feb 1, 2007 10:20:23 PM
 */
public class TestCatGenAndWrite extends TestCase
{

  public TestCatGenAndWrite( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testLocalDataFiles()
  {
    String collectionPath = "src/test/data/thredds/cataloggen/testData"; // "C:/Ethan/data/mlode";
    String startPath = "modelNotFlat";
    String catWriteDirPath = "target/tmpTest/TestCatGenAndWrite/testLocalDataFiles"; // "C:/Ethan/data/tmpTest";

    File catWriteDir = new File( catWriteDirPath );

    File collectionFile = new File( collectionPath );
    CrawlableDataset collectionCrDs = new CrawlableDatasetFile( collectionFile );
    InvService service = new InvService( "myServer", "File", collectionCrDs.getPath() + "/", null, null );
    CrawlableDatasetFilter filter = null;
    CrawlableDataset topCatCrDs = collectionCrDs.getDescendant( startPath );

    CatGenAndWrite cgaw = null;
    try
    {
      cgaw = new CatGenAndWrite( "DATA", "My data", "", service,
                                 collectionCrDs, topCatCrDs, filter, catWriteDir );
    }
    catch ( IllegalArgumentException e )
    {
      assertTrue( "Bad argument: " + e.getMessage(),
                  false );
      return;
    }

    try
    {
      cgaw.genCatAndSubCats( topCatCrDs );
    }
    catch ( IOException e )
    {
      assertTrue( "I/O error generating and writing catalogs at and under \"" + topCatCrDs.getPath() + "\": " + e.getMessage(),
                  false );
      return;
    }

    crawlCatalogs( new File( new File( catWriteDir, startPath), "catalog.xml") );
  }

  public void testLocalDataFilesOnTds()
  {
    String collectionPath = "src/test/data/thredds/cataloggen/testData"; // "C:/Ethan/data/mlode";
    String startPath = "modelNotFlat";
    String catWriteDirPath = "target/tmpTest/TestCatGenAndWrite/testLocalDataFilesOnTds"; // "C:/Ethan/data/tmpTest";

    File catWriteDir = new File( catWriteDirPath );

    File collectionFile = new File( collectionPath );
    CrawlableDataset collectionCrDs = new CrawlableDatasetFile( collectionFile );
    InvService service = new InvService( "myServer", "OPeNDAP", "/thredds/dodsC/", null, null );
    CrawlableDatasetFilter filter = null;
    CrawlableDataset topCatCrDs = collectionCrDs.getDescendant( startPath );

    CatGenAndWrite cgaw = null;
    try
    {
      cgaw = new CatGenAndWrite( "DATA", "My data", "tdr", service,
                                 collectionCrDs, topCatCrDs, filter, catWriteDir );
    }
    catch ( IllegalArgumentException e )
    {
      assertTrue( "Bad argument: " + e.getMessage(),
                  false );
      return;
    }

    try
    {
      cgaw.genCatAndSubCats( topCatCrDs );
    }
    catch ( IOException e )
    {
      assertTrue( "I/O error generating and writing catalogs at and under \"" + topCatCrDs.getPath() + "\": " + e.getMessage(),
                  false );
      return;
    }

    crawlCatalogs( new File( new File( catWriteDir, startPath), "catalog.xml") );
  }

  private void crawlCatalogs( File topCatalogFile)
  {
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false);
    InvCatalogImpl topCatalog = fac.readXML( topCatalogFile.toURI() );

    
  }
}
