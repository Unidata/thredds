// $Id: CrawlableDatasetFile.java 63 2006-07-12 21:50:51Z edavis $
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
    long lastModDate = this.file.lastModified();
    if ( lastModDate == 0 ) return null;
    
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.setTimeInMillis( lastModDate );
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
