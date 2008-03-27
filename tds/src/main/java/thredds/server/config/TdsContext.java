package thredds.server.config;

import thredds.servlet.ServletUtil;

import javax.servlet.ServletContext;
import java.io.File;

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

  private ServletContext servletContext;

  private String contextPath;
  private File rootDirectory;
  private File contentDirectory;

  public TdsContext() { }

  public void init( ServletContext servletContext )
  {
    if ( servletContext == null )
      throw new IllegalArgumentException( "ServletContext must not be null.");
    this.servletContext = servletContext;

    this.contextPath = servletContext.getContextPath();
    this.rootDirectory = new File( servletContext.getRealPath( "/" ) );
    this.contentDirectory = new File( this.rootDirectory, "../../content" + this.contextPath );
  }

  public String getContextPath()
  {
    return ServletUtil.getContextPath();
  }

  public File getRootDirectory()
  {
    return rootDirectory;
  }

  public File getContentDirectory()
  {
    return contentDirectory;
  }

}
