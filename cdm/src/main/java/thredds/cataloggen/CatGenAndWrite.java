package thredds.cataloggen;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.catalog.InvService;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDatasetImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

/**
 * _more_
 *
 * @author edavis
 * @since Jan 29, 2007 4:48:08 PM
 */
public class CatGenAndWrite
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenAndWrite.class );

  private CatalogBuilder catBuilder;
  private CrawlableDataset collectionCrDs;
  private CrawlableDataset topCatCrDs;

  private File topCatWriteDir;

  private CrawlableDatasetFilter collectionOnlyFilter;
  private InvCatalogFactory factory;

  public CatGenAndWrite( String collectionId, String collectionTitle,
                         String collectionUrlId, InvService service,
                         CrawlableDataset collectionCrDs, CrawlableDataset topCatCrDs,
                         CrawlableDatasetFilter filter,
                         InvDatasetImpl topLevelMetadataContainer,
                         File catWriteDir )
  {
    String topCatWritePath = topCatCrDs.getPath().substring( collectionCrDs.getPath().length() );
    File topCatWriteDir = new File( catWriteDir, topCatWritePath );
    if ( ! topCatWriteDir.exists() )
    {
      if ( ! topCatWriteDir.mkdirs() )
      {
        log.error( "CatGenAndWrite(): could not create directory(s) for " + topCatWriteDir.getPath() );
        throw new IllegalArgumentException( "Could not create directory(s) for " + topCatWriteDir.getPath() );
      }
    }
    this.topCatWriteDir = topCatWriteDir;

    catBuilder = new StandardCatalogBuilder( collectionUrlId, collectionTitle,
                                             collectionCrDs, filter, service, collectionId,
                                             null, null, true, null, null,null, topLevelMetadataContainer, null );

    this.collectionCrDs = collectionCrDs;
    this.topCatCrDs = topCatCrDs;

    collectionOnlyFilter = new CollectionOnlyCrDsFilter( filter );
    factory = InvCatalogFactory.getDefaultFactory( false );

  }

  /**
   * Need a few test cases:
   * 1) Anne's TDR case - catalog a collection of TDS served data.
   * 2) Catalog a collection of local data files.
   * 3)
   *
   * @param args
   */
  public static void main1( String[] args )
  {
    // Test case 2: local data files.
    String collectionPath = "C:/Ethan/data/mlode";
    String startPath = "grid/NCEP";
    String catWriteDirPath = "C:/Ethan/data/tmpTest";

    if ( args.length == 3 )
    {
      collectionPath = args[0];
      startPath = args[1];
      catWriteDirPath = args[2];
    }

    File catWriteDir = new File( catWriteDirPath );

    File collectionFile = new File( collectionPath );
    CrawlableDataset collectionCrDs = new CrawlableDatasetFile( collectionFile );
    InvService service = new InvService( "myServer", "File", collectionCrDs.getPath() + "/", null, null);
    CrawlableDatasetFilter filter = null;
    CrawlableDataset topCatCrDs = collectionCrDs.getDescendant( startPath );

    CatGenAndWrite cgaw = new CatGenAndWrite( "DATA", "My data", "", service,
                                              collectionCrDs, topCatCrDs, filter, null, catWriteDir );

    try
    {
      cgaw.genCatAndSubCats( topCatCrDs );
    }
    catch ( IOException e )
    {
      log.error( "I/O error generating and writing catalogs at and under \"" + topCatCrDs.getPath() + "\": " + e.getMessage());
      return;
    }

  }
  public static void main( String[] args )
  {
    // Test case 1: local data files served by TDS.
    String collectionPath = "C:/Ethan/data/mlode";
    String startPath = "grid/NCEP";
    String catWriteDirPath = "C:/Ethan/code/svnThredds/tds/content/thredds/catGenAndWrite";
    //String catWriteDirPath = "C:/Ethan/data/tmpTest2";

    if ( args.length == 3 )
    {
      collectionPath = args[0];
      startPath = args[1];
      catWriteDirPath = args[2];
    }

    File catWriteDir = new File( catWriteDirPath );

    File collectionFile = new File( collectionPath );
    CrawlableDataset collectionCrDs = new CrawlableDatasetFile( collectionFile );
    InvService service = new InvService( "myServer", "OPENDAP", "/thredds/dodsC/", null, null );
    CrawlableDatasetFilter filter = null;
    CrawlableDataset topCatCrDs = collectionCrDs.getDescendant( startPath );

    CatGenAndWrite cgaw = new CatGenAndWrite( "DATA", "My data", "mlode", service,
                                              collectionCrDs, topCatCrDs, filter, null, catWriteDir );

    try
    {
      cgaw.genCatAndSubCats( topCatCrDs );
    }
    catch ( IOException e )
    {
      log.error( "I/O error generating and writing catalogs at and under \"" + topCatCrDs.getPath() + "\": " + e.getMessage() );
      return;
    }
  }

  public void genAndWriteCatalogTree()
          throws IOException
  {
    this.genCatAndSubCats( topCatCrDs);
  }

  private void genCatAndSubCats( CrawlableDataset catCrDs )
          throws IOException
  {

    String catWritePath = catCrDs.getPath().substring( topCatCrDs.getPath().length() );
    File catWriteDir = new File( topCatWriteDir, catWritePath );
    if ( ! catWriteDir.exists() )
    {
      if ( ! catWriteDir.mkdirs() )
      {
        log.error( "genCatAndSubCats(): could not create directory(s) for " + catWriteDir.getPath() );
        throw new IOException( "Could not create directory(s) for " + catWriteDir.getPath() );
      }
    }

    File catFile = new File( catWriteDir, "catalog.xml" );

    InvCatalogImpl cat = catBuilder.generateCatalog( catCrDs );

    factory.writeXML( cat, catFile.getAbsolutePath());

    // Find child datasets that are collections and generate catalogs for those as well.
    List collectionChildren = catCrDs.listDatasets( collectionOnlyFilter);
    for ( Iterator it = collectionChildren.iterator(); it.hasNext(); )
    {
      CrawlableDataset curCrDs = (CrawlableDataset) it.next();

      genCatAndSubCats( curCrDs );
    }

  }

  static class CollectionOnlyCrDsFilter implements CrawlableDatasetFilter
  {
    private CrawlableDatasetFilter filter;


    CollectionOnlyCrDsFilter( CrawlableDatasetFilter filter )
    {
      this.filter = filter;
    }

    public boolean accept( CrawlableDataset dataset )
    {
      if ( dataset.isCollection() )
      {
        if ( filter == null )
          return true;
        else
          return filter.accept( dataset );
      }
      return false;
    }

    public Object getConfigObject()
    {
      return null;
    }
  }
}
