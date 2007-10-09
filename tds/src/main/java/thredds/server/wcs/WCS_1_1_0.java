package thredds.server.wcs;

import thredds.servlet.ServletUtil;
import thredds.wcs.v1_1_0.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

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
          throws IOException //, ServletException
  {
    try
    {
      URI serverURI = new URI( req.getRequestURL().toString());
      Request request = WcsRequestParser.parseRequest( this.getVersion().getVersionString(), req, res);
      if ( request.getOperation().equals( Request.Operation.GetCapabilities))
      {
        GetCapabilities getCapabilities =
                new GetCapabilities( serverURI, request.getSections(),
                                     getServiceId(), getServiceProvider(),
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
        // ToDo Handle multi-part MIME response
        GetCoverage getCoverage =
                new GetCoverage( serverURI, request.getIdentifier(),
                                 request.getDatasetPath(),
                                 request.getDataset() );
        File covFile = getCoverage.writeCoverageDataToFile();
        if ( covFile != null && covFile.exists())
        {
          res.setContentType( "application/netcdf" );
          res.setStatus( HttpServletResponse.SC_OK );

          //ServletUtil.returnFile( servlet, req, res, covFile, "application/netcdf");
          ServletOutputStream out = res.getOutputStream();
          thredds.util.IO.copyFileB( covFile, out, 60000 );
          res.flushBuffer();
          out.close();
          ServletUtil.logServerAccess( HttpServletResponse.SC_OK, covFile.length() );
        }
        else
        {
          log.error( "handleKVP(): Failed to create coverage file" + (covFile == null ? "" : (": " )) );
          throw new WcsException( "Problem creating requested coverage.");
        }
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
    GetCapabilities.ServiceProvider.OnlineResource resource =
            new GetCapabilities.ServiceProvider.OnlineResource( null, "a link");
    GetCapabilities.ServiceProvider.Address address = null; //new GetCapabilities.ServiceProvider.Address(...);
    List<String> phone = Collections.emptyList();
    List<String> fax = Collections.emptyList();
    GetCapabilities.ServiceProvider.ContactInfo contactInfo =
            new GetCapabilities.ServiceProvider.ContactInfo( phone, fax, address, null, "hours", "contact instructions");
    GetCapabilities.ServiceProvider.ServiceContact contact =
            new GetCapabilities.ServiceProvider.ServiceContact( "individual name", "position name", contactInfo, "role");

    return new GetCapabilities.ServiceProvider( "sp name", resource, contact);
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
