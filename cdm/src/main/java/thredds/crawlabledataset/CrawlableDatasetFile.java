// $Id$
package thredds.crawlabledataset;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.lang.reflect.InvocationTargetException;

/**
 * A description
 *
 * @author edavis
 * @since Jun 8, 2005 15:34:04 -0600
 */
public class CrawlableDatasetFile implements CrawlableDataset
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CrawlableDatasetFile.class);
  //private static Log log = LogFactory.getLog( CrawlableDatasetFile.class );

  private File file;
  private String path;
  private String name;

  private Object configObj = null;

  protected CrawlableDatasetFile() {}
  protected CrawlableDatasetFile( String path, Object configObj ) throws IOException
  {
    if ( configObj != null )
    {
      log.debug( "CrawlableDatasetFile(): config object not null, it will be ignored <" + configObj.toString() + ">.");
      this.configObj = configObj;
    }

    if ( path.startsWith( "file:" ) )
    {
      try
      {
        this.path = path;
        this.file = new File( new URI( path ) );
        this.name = this.file.getName();
      }
      catch ( URISyntaxException e )
      {
        String tmpMsg = "Bad URI syntax for path <" + path + ">: " + e.getMessage();
        log.debug( "CrawlableDatasetFile(): " + tmpMsg);
        throw new IllegalArgumentException( tmpMsg);
      }
    }
    else
    {
      // Determine name (i.e., last name in the path name sequence).
      String tmpName = "";
      if ( ! path.equals( "/"))
      {
        tmpName = path.endsWith( "/" ) ? path.substring( 0, path.length() - 1 ) : path;
        int index = tmpName.lastIndexOf( "/" );
        if ( index != -1 ) tmpName = tmpName.substring( index + 1 );
      }

      // Make sure file exists and file name is same as name determined above.
      File tmpFile = new File( path );
      if ( ! tmpFile.exists() )
      {
        String tmpMsg = "File <" + path + "> does not exist.";
        log.debug( "CrawlableDatasetFile(): " + tmpMsg);
        throw new IOException( tmpMsg);
      }
      if ( ! tmpFile.getName().equals( tmpName ) )
      {
        String tmpMsg = "File name <" + tmpFile.getName() + "> not as calculated <" + tmpName + "> from path <" + path + ">.";
        log.debug( "CrawlableDatasetFile(): " + tmpMsg);
        throw new IOException( tmpMsg );
      }
      this.path = path;
      this.file = tmpFile;
      this.name = this.file.getName();
    }
  }

  public File getFile()
  {
    return file;
  }

  public Object getConfigObject()
  {
    return configObj;
  }

  public String getPath()
  {
    return( this.path);
  }

  public String getName()
  {
    return( this.name);
  }

  public boolean isCollection()
  {
    return( file.isDirectory());
  }

  public List listDatasets() throws IOException
  {
    if ( ! this.isCollection() )
    {
      String tmpMsg = "This dataset <" + this.getPath() + "> is not a collection dataset.";
      log.error( "listDatasets(): " + tmpMsg);
      throw new IllegalStateException( tmpMsg );
    }

    File[] allFiles = this.file.listFiles();
    List list = new ArrayList();
    for ( int i = 0; i < allFiles.length; i++ )
    {
      try
      {
        list.add( CrawlableDatasetFactory.createCrawlableDataset( allFiles[i].getPath(), this.getClass().getName(), null ) );
      }
      catch ( ClassNotFoundException e )
      {
        log.warn( "listDatasets(): Can't make CrawlableDataset for child file <" + allFiles[i].getPath() + ">: " + e.getMessage() );
      }
      catch ( NoSuchMethodException e )
      {
        log.warn( "listDatasets(): Can't make CrawlableDataset for child file <" + allFiles[i].getPath() + ">: " + e.getMessage() );
      }
      catch ( IllegalAccessException e )
      {
        log.warn( "listDatasets(): Can't make CrawlableDataset for child file <" + allFiles[i].getPath() + ">: " + e.getMessage() );
      }
      catch ( InvocationTargetException e )
      {
        log.warn( "listDatasets(): Can't make CrawlableDataset for child file <" + allFiles[i].getPath() + ">: " + e.getMessage() );
      }
      catch ( InstantiationException e )
      {
        log.warn( "listDatasets(): Can't make CrawlableDataset for child file <" + allFiles[i].getPath() + ">: " + e.getMessage() );
      }
    }

    return ( list );
  }

  public List listDatasets( CrawlableDatasetFilter filter ) throws IOException
  {
    List list = this.listDatasets();
    if ( filter == null ) return list;
    List retList = new ArrayList();
    for ( Iterator it = list.iterator(); it.hasNext(); )
    {
      CrawlableDataset curDs = (CrawlableDataset) it.next();
      if ( filter.accept( curDs ) )
      {
        retList.add( curDs );
      }
    }
    return ( retList );
  }

  public CrawlableDataset getParentDataset() throws IOException
  {
    String parentPath = this.file.getParent();
    if ( parentPath == null ) return null;
    String normalizedPath = CrawlableDatasetFactory.normalizePath( parentPath );
    return new CrawlableDatasetFile( normalizedPath, null );
  }

  public long length()
  {
    if ( this.isCollection()) return( 0);
    return( this.file.length());
  }

  public Date lastModified()
  {
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.setTimeInMillis( this.file.lastModified());
    return( cal.getTime() );
  }

//  public InvDataset correspondingInvDataset( ResultService service )
//  {
//    // Check that the accessPointHeader local file exists.
//    File aphFile = new File( service.getAccessPointHeader() );
//    if ( !aphFile.exists() ) throw new IllegalArgumentException( "The accessPointHeader file does not exist: service=<" + service.getName() + ">; path=<" + aphFile.getPath() + ">." );
//
//    // Check that this file starts with accessPointHeader.
//    String filePath;
//    String aphFilePath;
//    try
//    {
//      filePath = file.getCanonicalFile().toURI().toString();
//      aphFilePath = aphFile.getCanonicalFile().toURI().toString();
//    }
//    catch ( IOException e )
//    {
//      throw new IllegalStateException( "Problem" + e.getMessage());
//    }
//    if ( !filePath.startsWith( aphFilePath ) )
//    {
//      throw new IllegalStateException( "This <" + this.getPath() + "> must start with the accessPointHeader <" + aphFile.getPath() + ">." );
//    }
//
//    String urlPath = this.isCollection() ? null : filePath.substring( aphFilePath.length() );
//    return( new InvDatasetImpl( null, this.getName(),  null, service.getLabel(), urlPath) );
//  }
}

/*
 * $Log: CrawlableDatasetFile.java,v $
 * Revision 1.7  2006/05/19 19:23:05  edavis
 * Convert DatasetInserter to ProxyDatasetHandler and allow for a list of them (rather than one) in
 * CatalogBuilders and CollectionLevelScanner. Clean up division between use of url paths (req.getPathInfo())
 * and translated (CrawlableDataset) paths.
 *
 * Revision 1.6  2006/01/26 18:20:45  edavis
 * Add CatalogRootHandler.findRequestedDataset() method (and supporting methods)
 * to check that the requested dataset is allowed, i.e., not filtered out.
 *
 * Revision 1.5  2006/01/23 18:51:06  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.4  2006/01/20 02:08:24  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.3  2005/12/30 00:18:54  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.2  2005/11/18 23:51:04  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.1  2005/11/15 18:40:49  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.2  2005/08/22 17:40:23  edavis
 * Another round on CrawlableDataset: make CrawlableDatasetAlias a subclass
 * of CrawlableDataset; start generating catalogs (still not using in
 * InvDatasetScan or CatalogGen, yet).
 *
 * Revision 1.1  2005/07/13 22:54:22  edavis
 * Fix CrawlableDatasetAlias.
 *
 * Revision 1.2  2005/06/29 22:04:05  edavis
 * Remove the correspondingInvDataset() method. It should be in a
 * factory method perhaps in CollectionLevelScanner class.
 *
 * Revision 1.1  2005/06/24 22:08:32  edavis
 * Second stab at the CrawlableDataset interface.
 *
 */