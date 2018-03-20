/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.crawlabledataset;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * An implementation of CrawlableDataset where the dataset being represented
 * is a local file (java.io.File).
 *
 * <p>The constructor extends the allowed form of a CrawlableDataset path to
 * allow file paths to be given in their native formats including Unix
 * (/my/file), Windows (c:\my\file), and UNC file paths (\\myhost\my\file).
 * However, the resulting CrawlableDataset path is normalized to conform to the
 * allowed form of the CrawlableDataset path.
 *
 * <p>This is the default implementation of CrawlableDataset used by
 * CrawlableDatasetFactory if the class name handed to the
 * createCrawlableDataset() method is null.</p>
 *
 * @author edavis
 * @since Jun 8, 2005 15:34:04 -0600
 */
public class CrawlableDatasetFile implements CrawlableDataset
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CrawlableDatasetFile.class);
  //private static Log log = LogFactory.getLog( CrawlableDatasetFile.class );

  private final File file;
  //private final String path;
  //private final String name;

  private final Object configObj;

  //protected CrawlableDatasetFile() {}

  /**
   * Constructor required by CrawlableDatasetFactory.
   *
   * @param path the path of the CrawlableDataset being constructed.
   * @param configObj the configuration object required by CrawlableDatasetFactory; it is ignored.
   */
  public CrawlableDatasetFile( String path, Object configObj )
  {
    if ( path.startsWith( "file:" ) ) {
      path = path.substring(5);
    }
    /*   URI url;
      try {
        url = new URI( path );
      } catch ( URISyntaxException e ) {
        String tmpMsg = "Bad URI syntax for path <" + path + ">: " + e.getMessage();
        log.debug( "CrawlableDatasetFile(): " + tmpMsg );
        throw new IllegalArgumentException( tmpMsg );
      }
      try {
        this.file = new File( url );
      } catch ( IllegalArgumentException e ) {
        String tmpMsg = "path= <" + path + ">: " + e.getMessage();
        log.debug( "CrawlableDatasetFile(): " + tmpMsg );
        throw new IllegalArgumentException( tmpMsg );
      }
    }
    else
    {  */
      file = new File( path );
    // }

    //this.path = this.normalizePath( this.file.getPath() );
    //this.name = this.file.getName();

    if ( configObj != null )
    {
      log.warn( "CrawlableDatasetFile(): config object not null, it will be ignored <" + configObj.toString() + ">.");
      this.configObj = configObj;
    }
    else
      this.configObj = null;
  }

  private CrawlableDatasetFile( CrawlableDatasetFile parent, String childPath )
  {
    this.file = new File( parent.getFile(), childPath);
    //this.path = this.normalizePath( this.file.getPath());
    // this.name = this.file.getName();

    this.configObj = null;
  }

  public CrawlableDatasetFile( File file )
  {
    this.file = file;
    // this.path = this.normalizePath( this.file.getPath() );
    // this.name = this.file.getName();
    this.configObj = null;
  }

  /**
   * Return the given path with backslashes ("\") converted to slashes ("/).
   * Slashes are the normalized CrawlableDatset path seperator.
   * This method can be used on absolute or relative paths.
   * <p/>
   *
   * @param path the path to be normalized.
   * @return the normalized path.
   * @throws NullPointerException if path is null.
   */
  private String normalizePath( String path )
  {
    // Replace any occurance of a backslash ("\") with a slash ("/").
    // NOTE: Both String and Pattern escape backslash, so need four backslashes to find one.
    // NOTE: No longer replace multiple backslashes with one slash, which allows for UNC pathnames (Windows LAN addresses).
    //       Was path.replaceAll( "\\\\+", "/");
    return path.replaceAll( "\\\\", "/" );

//    String newPath = path.replaceAll( "\\\\", "/" );
    // Note: No longer remove trailing slashes as new File() removes slashes for us.
//    // Remove trailing slashes.
//    while ( newPath.endsWith( "/" ) && ! newPath.equals( "/" ) )
//      newPath = newPath.substring( 0, newPath.length() - 1 );
//
//    return newPath;
  }

  /**
   * Provide access to the file that this CrawlableDataset represents.
   *
   * @return the file that this CrawlableDataset represents or {@code null} if it could not be obtained.
   */
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
    return normalizePath( this.file.getPath());
  }

  public String getName()
  {
    return this.file.getName();
  }

  public boolean exists()
  {
    return file.exists() && file.canRead();
  }

  public boolean isCollection()
  {
    return( file.isDirectory());
  }

  public CrawlableDataset getDescendant( String relativePath)
  {
    if ( relativePath.startsWith( "/" ) )
      throw new IllegalArgumentException( "Path must be relative <" + relativePath + ">." );
    return new CrawlableDatasetFile( this, relativePath );
  }

  public List<CrawlableDataset> listDatasets() throws IOException
  {
   if ( ! this.exists() )
    {
      String tmpMsg = "This dataset <" + this.getPath() + "> does not exist.";
      log.error( "listDatasets(): " + tmpMsg);
      throw new IllegalStateException( tmpMsg );
    }

  if ( ! this.isCollection() )
    {
      String tmpMsg = "This dataset <" + this.getPath() + "> is not a collection dataset.";
      log.error( "listDatasets(): " + tmpMsg);
      throw new IllegalStateException( tmpMsg );
    }

    List<CrawlableDataset> list = new ArrayList<CrawlableDataset>();
    File[] files = this.file.listFiles();
    if ( files == null )
    {
      log.error( "listDatasets(): the underlying file [" + this.file.getPath() + "] exists, is a directory, and canRead()==true but listFiles() returns null. This may be a problem Java has on Windows XP (Java 7 should fix).");
      return Collections.emptyList();
    }
    for (File allFile : files ) {
      CrawlableDatasetFile crDs = new CrawlableDatasetFile( this, allFile.getName() );
      if ( crDs.exists())
        list.add( crDs );
    }

    return list;
  }

  public List<CrawlableDataset> listDatasets( CrawlableDatasetFilter filter ) throws IOException
  {
    List<CrawlableDataset> list = this.listDatasets();
    if ( filter == null ) return list;
    List<CrawlableDataset> retList = new ArrayList<CrawlableDataset>();
    for ( CrawlableDataset curDs: list )
    {
      if ( filter.accept( curDs ) )
      {
        retList.add( curDs );
      }
    }
    return ( retList );
  }

  public CrawlableDataset getParentDataset()
  {
    File parentFile = this.file.getParentFile();
    if ( parentFile == null ) return null;
    return new CrawlableDatasetFile( parentFile );
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

  public String toString()
  {
    return getPath();
  }
}
