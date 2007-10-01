package thredds.server.wcs;

import thredds.servlet.ServletUtil;
import thredds.wcs.v1_1_0.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

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
      Request request = WcsRequestParser.parseRequest( this.getVersion().getVersionString(), req, res);
      if ( request.getOperation().equals( Request.Operation.GetCapabilities))
      {
        GetCapabilities.ServiceId sid =
                new GetCapabilities.ServiceId("title", "abstract",
                                              Collections.singletonList( "keyword" ),
                                              "WCS", Collections.singletonList("1.1.0"),
                                              "", Collections.singletonList( "") );
        GetCapabilities.ServiceProvider sp =
                new GetCapabilities.ServiceProvider();
        GetCapabilities getCapabilities =
                new GetCapabilities( serverURI, request.getSections(),
                                     getServiceId(), getServiceProvider(),
                                     // ToDo Remove serviceId and serviceProvider from Requst
                                     //request.getServiceId(), request.getServiceProvider(),
                                     request.getDataset() );
        res.setContentType( "text/xml" );
        res.setStatus( HttpServletResponse.SC_OK );
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1 );

        PrintWriter pw = res.getWriter();
        getCapabilities.writeCapabilitiesReport( pw );
        pw.flush();
      }
      else if ( request.getOperation().equals( Request.Operation.DescribeCoverage ) )
      {
        DescribeCoverage descCoverage =
                new DescribeCoverage( serverURI, request.getIdentifierList(),
                                      request.getDataset() );
        res.setContentType( "text/xml" );
        res.setStatus( HttpServletResponse.SC_OK );
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1 );

        PrintWriter pw = res.getWriter();
        descCoverage.writeDescribeCoverageDoc( pw );
        pw.flush();
      }
      else if ( request.getOperation().equals( Request.Operation.GetCoverage ) )
      {
        GetCoverage getCoverage =
                new GetCoverage( serverURI, request.getIdentifier(),
                                 request.getDatasetPath(),
                                 request.getDataset() );
        res.setContentType( "text/xml" );
        res.setStatus( HttpServletResponse.SC_OK );
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1 );

        PrintWriter pw = res.getWriter();
        getCoverage.writeGetCoverageDoc( pw );
        pw.flush();
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

  private GetCapabilities.ServiceId getServiceId()
  {
    // Todo Figure out how to configure serviceId info.
    GetCapabilities.ServiceId sid;
    sid = new GetCapabilities.ServiceId( "title", "abstract",
                                          Collections.singletonList( "keyword" ),
                                          "WCS", Collections.singletonList( "1.1.0" ),
                                          "", Collections.singletonList( "" ) );

    return sid;
  }
  private GetCapabilities.ServiceProvider getServiceProvider()
  {
    // Todo Figure out how to configure serviceProvider info.
    GetCapabilities.ServiceProvider sp =
            new GetCapabilities.ServiceProvider();
    sp.name = "sp name";
    sp.site.link = null; //new URI( "");
    sp.site.title = "a link";
    sp.contact.individualName = "";
    sp.contact.positionName = "";
    sp.contact.contactInfo = null;
    sp.contact.role = "";

    return sp;
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
