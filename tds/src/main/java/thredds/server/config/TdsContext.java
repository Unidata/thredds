package thredds.server.config;

import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import thredds.util.filesource.*;

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

  private String contextPath;
  private int webappMajorVersion = -1;
  private int webappMinorVersion = -1;
  private int webappBugfixVersion = -1;
  private int webappBuildVersion = -1;
  private String webappVersion;
  private String webappVersionFull;

  private String contentPath;
  private String startupContentPath;
  private String iddContentPath;
  private String motherlodeContentPath;

  private File rootDirectory;
  private File contentDirectory;
  private File publicContentDirectory;

  private File initialContentDirectory;
  private File iddContentDirectory;
  private File motherlodeContentDirectory;

  private DescendantFileSource rootDirSource;
  private DescendantFileSource contentDirSource;
  private DescendantFileSource publicContentDirSource;
  private DescendantFileSource initialContentDirSource;
  private DescendantFileSource iddContentPublicDirSource;
  private DescendantFileSource motherlodeContentPublicDirSource;

  private FileSource configSource;
  private FileSource publicDocSource;

  private RequestDispatcher defaultRequestDispatcher;
  private RequestDispatcher jspRequestDispatcher;

  public TdsContext() {}
  public void setMajorVersion( int majorVer) { this.webappMajorVersion = majorVer; }
  public void setMinorVersion( int minorVer) { this.webappMinorVersion = minorVer; }
  public void setBugfixVersion( int bugfixVer) { this.webappBugfixVersion = bugfixVer; }
  public void setBuildVersion( int buildVer) { this.webappBuildVersion = buildVer; }

  public void setContentPath( String contentPath) {this.contentPath = contentPath; }
  public void setStartupContentPath( String startupContentPath ) { this.startupContentPath = startupContentPath; }
  public void setIddContentPath( String iddContentPath ) { this.iddContentPath = iddContentPath; }
  public void setMotherlodeContentPath( String motherlodeContentPath ) { this.motherlodeContentPath = motherlodeContentPath; }
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

    // Set the context path.
    // Servlet 2.5 allows the following.
    contextPath = servletContext.getContextPath();
    //String tmpContextPath = servletContext.getInitParameter( "ContextPath" );  // cannot be overridden in the ThreddsConfig file
    //if ( tmpContextPath == null ) tmpContextPath = "thredds";
    //contextPath = "/" + tmpContextPath;

    // Set the version.


    // Determine content path.
    File tmpFile = new File( this.contentPath);
    if ( tmpFile.isAbsolute())
    {
      if ( tmpFile.isDirectory()) ;
    }
    this.contentPath = "../../content" + this.contextPath; // if not absolute, relative to root directory.

    // Set the root directory
    this.rootDirectory = new File( servletContext.getRealPath( "/" ) );
    this.rootDirSource = new BasicDescendantFileSource( this.rootDirectory);

    this.contentDirectory = new File( this.rootDirectory, this.contentPath );
    if ( ! this.contentDirectory.exists() )
    {
      if ( ! this.contentDirectory.mkdirs() )
      {
        String tmpMsg = "Creation of content directory failed";
        log.error( "init(): " + tmpMsg + " <" + this.contentDirectory.getAbsolutePath() + ">" );
//        throw new IOException( tmpMsg );
      }
    }
    this.contentDirSource = new BasicDescendantFileSource( StringUtils.cleanPath( this.contentDirectory.getAbsolutePath()) );
    this.contentDirectory = this.contentDirSource.getRootDirectory();

    this.publicContentDirectory = new File( this.contentDirectory, "public");
    this.publicContentDirSource = new BasicDescendantFileSource( this.publicContentDirectory);

    this.initialContentDirectory = new File( this.rootDirectory, this.startupContentPath );
    this.initialContentDirSource = new BasicDescendantFileSource( this.initialContentDirectory);

    this.iddContentDirectory = new File( this.rootDirectory, this.iddContentPath);
    this.iddContentPublicDirSource = new BasicDescendantFileSource( this.iddContentDirectory );

    this.motherlodeContentDirectory = new File( this.rootDirectory, this.motherlodeContentPath);
    this.motherlodeContentPublicDirSource = new BasicDescendantFileSource( this.motherlodeContentDirectory );

    List<DescendantFileSource> chain = new ArrayList<DescendantFileSource>();
    DescendantFileSource contentMinusPublicSource =
            new BasicWithExclusionsDescendantFileSource( this.contentDirectory,
                                                         Collections.singletonList( "public" ) );
    chain.add( contentMinusPublicSource );
    if ( false )
    {
      chain.add( this.iddContentPublicDirSource );
      chain.add( this.motherlodeContentPublicDirSource );
    }
    this.configSource = new ChainedFileSource( chain );
    this.publicDocSource = this.publicContentDirSource; // allow for chain?

    jspRequestDispatcher = servletContext.getNamedDispatcher( "jsp" );
    defaultRequestDispatcher = servletContext.getNamedDispatcher( "default" );

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
   * Return the version string for this web application.
   *
   * @return the context path.
   */
  public String getWebappVersion()
  {
    return this.webappVersion;
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

  /**
   * Return File for the initial content directory. I.e., the directory
   * that contains default content for the content directory, copied
   * there when TDS is first installed.
   *
   * @return a File to the initial content directory.
   */
  public File getInitialContentDirectory()
  {
    return initialContentDirectory;
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
