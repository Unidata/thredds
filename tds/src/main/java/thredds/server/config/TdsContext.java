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

  public TdsContext() { }

  public void init( ServletContext servletContext )
  {
    if ( servletContext == null )
      throw new IllegalArgumentException( "ServletContext must not be null.");

    this.contextPath = servletContext.getContextPath();
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

}
