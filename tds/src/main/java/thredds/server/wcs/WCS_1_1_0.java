package thredds.server.wcs;

import thredds.servlet.ServletUtil;
import thredds.servlet.Debug;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileNotFoundException;

/**
 * _more_
 *
 * @author edavis
 * @since Aug 14, 2007 1:20:49 PM
 */
public class WCS_1_1_0 implements VersionHandler
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( WCS_1_1_0.class );

  private Version version;

  /**
   * Declare the default constructor to be package private.
   */
  WCS_1_1_0()
  {
    this.version = new Version( "1.1.0" );
  }

  public Version getVersion()
  {
    return this.version;  
  }

  public void handleKVP( HttpServlet servlet, HttpServletRequest req, HttpServletResponse res )
          throws ServletException, IOException
  {

  }

  public void handleExceptionReport( HttpServletResponse res, String code, String locator, String message ) throws IOException
  {
    res.setContentType( "application/vnd.ogc.se_xml" );
    res.setStatus( HttpServletResponse.SC_BAD_REQUEST );

    PrintStream ps = new PrintStream( res.getOutputStream() );

    ps.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
    ps.println( "<ExceptionReport version='1.0.0'>" );
    ps.println( "  <Exception code='" + code + ( ( locator != null ) ? "locator='" + locator + "'" : "" ) + "'>" );
    ps.println( "   <ExceptionText>" );
    ps.println( "     " + message );
    ps.println( "   </ExceptionText>" );
    ps.println( "  </Exception>" );
    ps.println( "</ExceptionReport>" );

    ps.flush();
    ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 ); // LOOK, actual return is 200 = OK !
  }

  public void handleExceptionReport( HttpServletResponse res, String code, String locator, Throwable t ) throws IOException
  {
    res.setContentType( "application/vnd.ogc.se_xml" );
    res.setStatus( HttpServletResponse.SC_BAD_REQUEST );

    PrintStream ps = new PrintStream( res.getOutputStream() );

    ps.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
    ps.println( "<ExceptionReport xmlns='http://www.opengis.net/ows' version='1.0.0'>" );
    ps.println( "  <Exception code='" + code + ((locator != null) ? "locator='" + locator + "'" : "") + "'>" );
    ps.println( "   <ExceptionText>" );

    if ( Debug.isSet( "trustedMode" ) ) // security issue: only show stack if trusted
      t.printStackTrace( ps );
    else
      ps.println( t.getMessage() );

    ps.println( "   </ExceptionText>" );
    ps.println( "  </Exception>" );
    ps.println( "</ExceptionReport>" );

    ps.flush();
    if ( t instanceof FileNotFoundException )
      log.info( "handleExceptionReport", t.getMessage() ); // dont clutter up log files
    else
      log.info( "handleExceptionReport", t );
    ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 ); // LOOK, actual return is 200 = OK !
  }

}
