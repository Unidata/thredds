package thredds.server.wcs;

import thredds.servlet.ServletUtil;
import thredds.servlet.Debug;
import thredds.wcs.v1_1_0.XMLwriter;
import thredds.wcs.v1_1_0.ExceptionReport;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class WCS_1_1_0 implements VersionHandler
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( WCS_1_1_0.class );

  private Version version;
  private XMLwriter xmlWriter;

  /**
   * Declare the default constructor to be package private.
   */
  WCS_1_1_0()
  {
    this.version = new Version( "1.1.0" );
    this.xmlWriter = new XMLwriter();

  }

  public Version getVersion()
  {
    return this.version;  
  }

  public void handleKVP( HttpServlet servlet, HttpServletRequest req, HttpServletResponse res )
          throws ServletException, IOException
  {
  }

  public void handleExceptionReport( HttpServletResponse res, String code, String locator, String message )
          throws IOException
  {
    res.setContentType( "text/xml" ); // 1.0.0 was ("application/vnd.ogc.se_xml" );
    res.setStatus( HttpServletResponse.SC_BAD_REQUEST );

    PrintWriter pw = res.getWriter();
    PrintStream ps = new PrintStream( res.getOutputStream() );
    ExceptionReport exceptionReport = new ExceptionReport( ExceptionReport.Code.valueOf( code ), locator, message);
    exceptionReport.writeExceptionReport( pw );
    pw.flush();

    ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );
  }

  public void handleExceptionReport( HttpServletResponse res, String code, String locator, Throwable t )
          throws IOException
  {
    handleExceptionReport( res, code, locator, t.getMessage());

    if ( t instanceof FileNotFoundException )
      log.info( "handleExceptionReport", t.getMessage() ); // dont clutter up log files
    else
      log.info( "handleExceptionReport", t );
  }

}
