package thredds.server.config;

import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import thredds.util.filesource.*;
import thredds.servlet.ThreddsConfig;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TdsContext
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TdsContext.class );

  private String webappName;
  private String webappVersion;
  private String webappVersionFull;
  private String webappBuildDate;

  private String contextPath;
  private int webappMajorVersion = -1;
  private int webappMinorVersion = -1;
  private int webappBugfixVersion = -1;
  private int webappBuildVersion = -1;

  private String contentPath;
  private String startupContentPath;
  private String iddContentPath;
  private String motherlodeContentPath;

  private File rootDirectory;
  private File contentDirectory;
  private boolean contentDirectoryWritable = true;
  private File publicContentDirectory;

  private File startupContentDirectory;
  private File iddContentDirectory;
  private File motherlodeContentDirectory;

  private DescendantFileSource rootDirSource;
  private DescendantFileSource contentDirSource;
  private DescendantFileSource publicContentDirSource;
  private DescendantFileSource startupContentDirSource;
  private DescendantFileSource iddContentPublicDirSource;
  private DescendantFileSource motherlodeContentPublicDirSource;

  private FileSource configSource;
  private FileSource publicDocSource;

  private RequestDispatcher defaultRequestDispatcher;
  private RequestDispatcher jspRequestDispatcher;

  private String tdsConfigFilename;
  private TdsConfigHtml tdsConfigHtml;

  public TdsContext() {}
  public void setMajorVersion( int majorVer) { this.webappMajorVersion = majorVer; }
  public void setMinorVersion( int minorVer) { this.webappMinorVersion = minorVer; }
  public void setBugfixVersion( int bugfixVer) { this.webappBugfixVersion = bugfixVer; }
  public void setBuildVersion( int buildVer) { this.webappBuildVersion = buildVer; }
  public void setWebappBuildDate( String buildDateString) { this.webappBuildDate = buildDateString; }

  public void setContentPath( String contentPath) {this.contentPath = contentPath; }
  public void setStartupContentPath( String startupContentPath ) { this.startupContentPath = startupContentPath; }
  public void setIddContentPath( String iddContentPath ) { this.iddContentPath = iddContentPath; }
  public void setMotherlodeContentPath( String motherlodeContentPath ) { this.motherlodeContentPath = motherlodeContentPath; }

  public void setTdsConfigFileName( String filename ) { this.tdsConfigFilename = filename; }
  public String getTdsConfigFileName() { return this.tdsConfigFilename; }

  public void setTdsConfigHtml( TdsConfigHtml tdsConfigHtml ) { this.tdsConfigHtml = tdsConfigHtml; }
  public TdsConfigHtml getTdsConfigHtml() { return this.tdsConfigHtml; }

//  /**
//   * Constructor.
//   *
//   * @param startupContentPath initial content path (relative to the web app root directory).
//   * @param iddContentPath
//   * @param motherlodeContentPath
//   */
//  public TdsContext( String startupContentPath,
//                     String iddContentPath, String motherlodeContentPath )
//  {
//    if ( startupContentPath == null
//         || iddContentPath == null
//         || motherlodeContentPath == null )
//      throw new IllegalArgumentException( "Null values not allowed.");
//
//    this.startupContentPath = startupContentPath;
//    this.iddContentPath = iddContentPath;
//    this.motherlodeContentPath = motherlodeContentPath;
//  }

  public void destroy() {}

  public void init( ServletContext servletContext )
  {
    if ( servletContext == null )
      throw new IllegalArgumentException( "ServletContext must not be null.");

    // Set the webapp name.
    this.webappName = servletContext.getServletContextName();

    // Set the context path.
    // Servlet 2.5 allows the following.
    //contextPath = servletContext.getContextPath();
    String tmpContextPath = servletContext.getInitParameter( "ContextPath" );  // cannot be overridden in the ThreddsConfig file
    if ( tmpContextPath == null ) tmpContextPath = "thredds";
    contextPath = "/" + tmpContextPath;

    // Set the version.
    if ( this.webappMajorVersion < 0 || this.webappMinorVersion < 0 )
    {
      this.webappVersion = "unknown";
      this.webappVersionFull = "unknown";
    }
    else
    {
      StringBuilder ver = new StringBuilder();
      ver.append( this.webappMajorVersion)
              .append( ".").append( this.webappMinorVersion);
      this.webappVersion = ver.toString();

      if ( this.webappBugfixVersion > -1 )
      {
        ver.append( "." ).append( this.webappBugfixVersion );
        if ( this.webappBuildVersion > -1 )
          ver.append( "." ).append( this.webappBuildVersion );
      }
      this.webappVersionFull = ver.toString();
    }

    // Set the root directory and source.
    this.rootDirectory = new File( servletContext.getRealPath( "/" ) );
    this.rootDirSource = new BasicDescendantFileSource( this.rootDirectory );
    this.rootDirectory = this.rootDirSource.getRootDirectory();

    // Set the startup (initial install) content directory and source.
    this.startupContentDirectory = new File( this.rootDirectory, this.startupContentPath );
    this.startupContentDirSource = new BasicDescendantFileSource( this.startupContentDirectory );
    this.startupContentDirectory = this.startupContentDirSource.getRootDirectory();

    // Set the content directory and source.
    this.contentDirectory = new File( new File( this.rootDirectory, "../../content"), this.contentPath);
    if ( ! this.contentDirectory.exists() || ! this.contentDirectory.isDirectory() )
    {
      String tmpMsg = "Creation of content directory failed";
      log.error( "init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]" );
      System.out.println( "ERROR - TdsContext.init(): " + tmpMsg + " [" + this.contentDirectory.getAbsolutePath() + "]." );
      this.contentDirectory = this.startupContentDirectory;
      this.contentDirSource = this.startupContentDirSource;
      this.contentDirectoryWritable = false;
    }
    else
    {
      this.contentDirSource = new BasicDescendantFileSource( StringUtils.cleanPath( this.contentDirectory.getAbsolutePath()) );
      this.contentDirectory = this.contentDirSource.getRootDirectory();
      this.contentDirectoryWritable = this.contentDirectory.canWrite();
    }

    // read in persistent user-defined params from threddsConfig.xml
    File tdsConfigFile = this.contentDirSource.getFile( this.getTdsConfigFileName() );
    String tdsConfigFilename = tdsConfigFile != null ? tdsConfigFile.getPath() : "";
    ThreddsConfig.init( servletContext, tdsConfigFilename, log );

    File logDir = new File( this.contentDirectory, "logs");
    if ( ! logDir.exists())
    {
      if ( ! logDir.mkdirs())
      {
        System.out.println( "ERROR - TdsContext.init(): Couldn't create TDS log directory [" + logDir.getPath() + "]." );
      }
    }
    System.setProperty( "tds.log.dir", logDir.getPath() ); // variable substitution

    this.publicContentDirectory = new File( this.contentDirectory, "public");
    this.publicContentDirSource = new BasicDescendantFileSource( this.publicContentDirectory);

    this.iddContentDirectory = new File( this.rootDirectory, this.iddContentPath);
    this.iddContentPublicDirSource = new BasicDescendantFileSource( this.iddContentDirectory );

    this.motherlodeContentDirectory = new File( this.rootDirectory, this.motherlodeContentPath);
    this.motherlodeContentPublicDirSource = new BasicDescendantFileSource( this.motherlodeContentDirectory );

    List<DescendantFileSource> chain = new ArrayList<DescendantFileSource>();
    DescendantFileSource contentMinusPublicSource =
            new BasicWithExclusionsDescendantFileSource( this.contentDirectory,
                                                         Collections.singletonList( "public" ) );
    chain.add( contentMinusPublicSource );
    for ( String curContentRoot : ThreddsConfig.getContentRootList() )
    {
      if ( curContentRoot.equalsIgnoreCase( "idd" ))
        chain.add( this.iddContentPublicDirSource );
      else if ( curContentRoot.equalsIgnoreCase( "motherlode" ))
        chain.add( this.motherlodeContentPublicDirSource );
      else
      {
        try
        {
          chain.add( new BasicDescendantFileSource( StringUtils.cleanPath( curContentRoot ) ) );
        }
        catch ( IllegalArgumentException e )
        {
          System.out.println( "WARN - TdsContext.init(): Couldn't add content root [" + curContentRoot + "]: " + e.getMessage() );
        }
      }
    }
    this.configSource = new ChainedFileSource( chain );
    this.publicDocSource = this.publicContentDirSource;

    jspRequestDispatcher = servletContext.getNamedDispatcher( "jsp" );
    defaultRequestDispatcher = servletContext.getNamedDispatcher( "default" );

    if ( this.tdsConfigHtml != null )
      this.tdsConfigHtml.init( this);
  }

  public String getWebappName()
  {
    return this.webappName;
  }
  
  /**
   * Return the context path under which this web app is running (e.g., "/thredds").
   *
   * @return the context path.
   */
  public String getContextPath()
  {
    return contextPath;
  }

  /**
   * Return the version string (<major>.<minor>) for this web application.
   *
   * @return the version string.
   */
  public String getWebappVersion()
  {
    return this.webappVersion;
  }

  /**
   * Return the full version string (<major>.<minor>.<bug>.<build>)
   * for this web application.
   *
   * @return the full version string.
   */
  public String getWebappVersionFull()
  {
    return this.webappVersionFull;
  }

  public String getWebappBuildDate()
  {
    return this.webappBuildDate;
  }
  
  /**
   * Return the web apps root directory (i.e., getRealPath( "/")).
   *
   * @return the root directory for the web app.
   */
  public File getRootDirectory()
  {
    return rootDirectory;
  }

  /**
   * Return File for content directory (exists() may be false).
   *  
   * @return a File to the content directory.
   */
  public File getContentDirectory()
  {
    return contentDirectory;
  }

  public boolean isContentDirectoryWritable()
  {
    return contentDirectoryWritable;
  }

  /**
   * Return File for the initial content directory. I.e., the directory
   * that contains default content for the content directory, copied
   * there when TDS is first installed.
   *
   * @return a File to the initial content directory.
   */
  public File getStartupContentDirectory()
  {
    return startupContentDirectory;
  }

  public File getIddContentDirectory()
  {
    return iddContentDirectory;
  }

  public File getMotherlodeContentDirectory()
  {
    return motherlodeContentDirectory;
  }

  public FileSource getConfigFileSource()
  {
    return this.configSource;
  }

  public FileSource getPublicDocFileSource()
  {
    return this.publicDocSource;
  }

  public RequestDispatcher getDefaultRequestDispatcher()
  {
    return this.defaultRequestDispatcher;
  }
  public RequestDispatcher getJspRequestDispatcher()
  {
    return this.jspRequestDispatcher;
  }
}
