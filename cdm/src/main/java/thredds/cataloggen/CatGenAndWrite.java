package thredds.cataloggen;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.filter.WildcardMatchOnNameFilter;
import thredds.catalog.InvService;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;

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
                                             null, null, true, null, null,null, null, null );

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
  public static void main( String[] args )
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
                                              collectionCrDs, topCatCrDs, filter, catWriteDir );

    try
    {
      cgaw.gen2CatAndSubCats( topCatCrDs );
    }
    catch ( IOException e )
    {
      log.error( "");
      return;
    }

    CatalogBuilder catBuild = new StandardCatalogBuilder( "", "My data",
                                                          collectionCrDs, filter,
                                                          new InvService( "myServer", "File",
                                                                          collectionCrDs.getPath() + "/", //"/thredds/dodsC/",
                                                                          null, null ),
                                                          "DATA", null, null, true, null, null, null, null, null);

    File startFile = new File( collectionFile, startPath );
    CrawlableDataset startCrDs = new CrawlableDatasetFile( startFile );
    CrawlableDataset ncepCrDs;
    try
    {
      ncepCrDs = catBuild.requestCrawlableDataset( startCrDs.getPath());
    }
    catch ( IOException e )
    {
      log.error( "I/O error getting CrDs: " + e.getMessage() );
      return;
    }

    InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory( false);
    genCatAndSubCats( collectionCrDs, ncepCrDs, filter, catBuild, catWriteDirPath, factory );
//      CrawlableDataset ncepGfsCrDs = catBuild.requestCrawlableDataset( dataPath + "/grid/NCEP/GFS");
//      CrawlableDataset ncepGfsAkCrDs = catBuild.requestCrawlableDataset( dataPath + "/grid/NCEP/GFS/Alaska_191km");
//      CrawlableDataset ncepNamCrDs = catBuild.requestCrawlableDataset( dataPath + "/grid/NCEP/NAM");
//      CrawlableDataset ncepNamAkCrDs = catBuild.requestCrawlableDataset( dataPath + "/grid/NCEP/NAM/Alaska_95km");
  }

  static void genCatAndSubCats( CrawlableDataset collectionCrDs,
                                CrawlableDataset catCrDs,
                                CrawlableDatasetFilter filter,
                                CatalogBuilder catBuilder,
                                String writePath,
                                InvCatalogFactory factory )
  {
    File writeFile = new File( writePath );

    File catParentDir = new File( writeFile, catCrDs.getPath().substring( collectionCrDs.getPath().length() ) );
    if ( ! catParentDir.exists() )
    {
      if ( ! catParentDir.mkdirs() )
      {
        log.error( "genCatAndSubCats(): could not create directory(s) for " + catParentDir.getPath() );
        return;
      }
    }
    File catFile = new File( catParentDir, "catalog.xml" );

    InvCatalogImpl cat = null;
    try
    {
      cat = catBuilder.generateCatalog( catCrDs );
    }
    catch ( IOException e )
    {
      log.error( "genCatAndSubCats(): could not generate catalog for " + catCrDs.getPath() + ": " + e.getMessage() );
      return;
    }

    try
    {
      factory.writeXML( cat, catFile.getAbsolutePath());
    }
    catch ( IOException e )
    {
      log.error( "genCatAndSubCats(): could not write catalog <" + catCrDs.getPath() + "> to file <" + catFile.getAbsolutePath()+ ">: " + e.getMessage() );
      return;
    }

    // Find child datasets that are collections and generate catalogs for those as well.
    CrawlableDatasetFilter collectionOnlyFilter = new CollectionOnlyCrDsFilter( filter );
    List collectionChildren = null;
    try
    {
      collectionChildren = catCrDs.listDatasets( collectionOnlyFilter);
    }
    catch ( IOException e )
    {
      log.error( "genCatAndSubCats(): I/O error listing child datasets that are collections: " + e.getMessage());
      return;
    }
    for ( Iterator it = collectionChildren.iterator(); it.hasNext(); )
    {
      CrawlableDataset curCrDs = (CrawlableDataset) it.next();

      genCatAndSubCats( collectionCrDs, curCrDs, filter, catBuilder, writePath, factory );
    }

  }
  public void gen2CatAndSubCats( CrawlableDataset catCrDs )
          throws IOException
  {

    String catWritePath = catCrDs.getPath().substring( topCatCrDs.getPath().length() );
    File catWriteDir = new File( topCatWriteDir, catWritePath );
    if ( ! catWriteDir.exists() )
    {
      if ( ! catWriteDir.mkdirs() )
      {
        log.error( "gen2CatAndSubCats(): could not create directory(s) for " + catWriteDir.getPath() );
        throw new IOException( "Could not create directory(s) for " + catWriteDir.getPath() );
      }
    }

    File catFile = new File( catWritePath, "catalog.xml" );

    InvCatalogImpl cat = catBuilder.generateCatalog( catCrDs );

    factory.writeXML( cat, catFile.getAbsolutePath());

    // Find child datasets that are collections and generate catalogs for those as well.
    List collectionChildren = catCrDs.listDatasets( collectionOnlyFilter);
    for ( Iterator it = collectionChildren.iterator(); it.hasNext(); )
    {
      CrawlableDataset curCrDs = (CrawlableDataset) it.next();

      gen2CatAndSubCats( collectionCrDs );
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
        if ( filter != null )
          return filter.accept( dataset);
        else
          return true;
      }
      return false;
    }

    public Object getConfigObject()
    {
      return null;
    }
  }
}
