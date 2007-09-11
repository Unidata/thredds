package thredds.server.wcs;

import thredds.servlet.ServletUtil;
import thredds.servlet.Debug;
import thredds.wcs.v1_1_0.XMLwriter;
import thredds.wcs.v1_1_0.ExceptionReport;
import thredds.wcs.v1_1_0.WcsException;
import thredds.wcs.v1_1_0.GetCapabilities;

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
    try
    {
      WcsRequest wcsRequest = new WcsRequest( req);
      if ( wcsRequest.getOperation().equals( WcsRequest.Operation.GetCapabilities))
      {
        new GetCapabilities( wcsRequest.getServiceId(), wcsRequest.getDataset() );
      }
      else if ( wcsRequest.getOperation().equals( WcsRequest.Operation.DescribeCoverage ) )
      {

      }
      else if ( wcsRequest.getOperation().equals( WcsRequest.Operation.GetCoverage ) )
      {

      }

      // Do everything in here.
      throw new WcsException();
    }
    catch ( WcsException e)
    {
      handleExceptionReport( res, e);
    }
  }

  public void handleExceptionReport( HttpServletResponse res, WcsException exception )
          throws IOException
  {
    res.setContentType( "text/xml" ); // 1.0.0 was ("application/vnd.ogc.se_xml" );
    res.setStatus( HttpServletResponse.SC_BAD_REQUEST );
    ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );

    ExceptionReport exceptionReport = new ExceptionReport( exception );

    PrintWriter pw = res.getWriter();
    exceptionReport.writeExceptionReport( pw );
    pw.flush();
  }

  public void handleExceptionReport( HttpServletResponse res, String code, String locator, String message )
          throws IOException
  {
    WcsException.Code c;
    WcsException exception;
    try
    {
      c = WcsException.Code.valueOf( code);
      exception = new WcsException( c, locator, message );
    }
    catch ( IllegalArgumentException e )
    {
      exception = new WcsException( message );
      log.debug( "handleExceptionReport(): bad code given <" + code + ">.");
    }

    handleExceptionReport( res, exception);
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
