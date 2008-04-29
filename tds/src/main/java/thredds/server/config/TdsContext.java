package thredds.server.config;

import thredds.servlet.ServletUtil;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;

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
  private File rootDirectory;
  private File contentDirectory;

  private File initialContentDirectory;

  public TdsContext() { }

  public void init( ServletContext servletContext )
  {
    if ( servletContext == null )
      throw new IllegalArgumentException( "ServletContext must not be null.");

    // Set the context path.
    // Servlet 2.5 allows the following.
    //contextPath = servletContext.getContextPath();
    String tmpContextPath = servletContext.getInitParameter( "ContextPath" );  // cannot be overridden in the ThreddsConfig file
    if ( tmpContextPath == null ) tmpContextPath = "thredds";
    contextPath = "/" + tmpContextPath;
    
    this.rootDirectory = new File( servletContext.getRealPath( "/" ) );
    this.contentDirectory = new File( this.rootDirectory, "../../content" + this.contextPath );
    if ( ! this.contentDirectory.exists() )
    {
      if ( ! this.contentDirectory.mkdirs() )
      {
        String tmpMsg = "Creation of content directory failed";
        log.error( "init(): " + tmpMsg + " <" + this.contentDirectory.getAbsolutePath() + ">" );
//        throw new IOException( tmpMsg );
      }
    }

    this.initialContentDirectory = new File( this.rootDirectory, "WEB-INF/altContent/startup");
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
}
