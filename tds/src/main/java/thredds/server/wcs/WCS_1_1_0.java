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
import java.net.URI;
import java.net.URISyntaxException;

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
      URI serverURI = new URI( req.getRequestURL().toString());
      WcsRequest wcsRequest = new WcsRequest( req, res);
      if ( wcsRequest.getOperation().equals( WcsRequest.Operation.GetCapabilities))
      {
        GetCapabilities getCapabilities =
                new GetCapabilities( serverURI, wcsRequest.getSections(),
                                     wcsRequest.getServiceId(), wcsRequest.getServiceProvider(),
                                     wcsRequest.getDataset() );
        res.setContentType( "text/xml" );
        res.setStatus( HttpServletResponse.SC_OK );
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1 );

        PrintWriter pw = res.getWriter();
        getCapabilities.writeCapabilitiesReport( pw );
        pw.flush();
      }
      else if ( wcsRequest.getOperation().equals( WcsRequest.Operation.DescribeCoverage ) )
      {

      }
      else if ( wcsRequest.getOperation().equals( WcsRequest.Operation.GetCoverage ) )
      {

      }
    }
    catch ( WcsException e)
    {
      handleExceptionReport( res, e);
    }
    catch ( URISyntaxException e )
    {
      handleExceptionReport( res, new WcsException( "Bad URI: " + e.getMessage()));
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
