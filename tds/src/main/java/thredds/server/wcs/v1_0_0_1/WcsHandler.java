package thredds.server.wcs.v1_0_0_1;

import thredds.servlet.ServletUtil;
import thredds.server.wcs.VersionHandler;
import thredds.server.wcs.Version;
import thredds.wcs.v1_0_0_1.*;
import thredds.wcs.Request;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import ucar.nc2.util.DiskCache2;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class WcsHandler implements VersionHandler
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( WcsHandler.class );

  private Version version;

  /**
   * Declare the default constructor to be package private.
   * @param verString the version string.
   */
  public WcsHandler( String verString )
  {
    this.version = new Version( verString );
  }

  public Version getVersion() { return this.version; }

  public VersionHandler setDiskCache( DiskCache2 diskCache )
  {
    WcsCoverage.setDiskCache( diskCache);
    return this;
  }

  private boolean deleteImmediately = true;
  public VersionHandler setDeleteImmediately( boolean deleteImmediately )
  {
    this.deleteImmediately = deleteImmediately;
    return this;
  }

  public void handleKVP( HttpServlet servlet, HttpServletRequest req, HttpServletResponse res )
          throws IOException //, ServletException
  {
    WcsRequest request = null;
    try
    {
      URI serverURI = new URI( req.getRequestURL().toString());
      request = WcsRequestParser.parseRequest( this.getVersion().getVersionString(),
                                                          serverURI, req, res);
      if ( request.getOperation().equals( Request.Operation.GetCapabilities))
      {
        res.setContentType( "text/xml" );
        res.setStatus( HttpServletResponse.SC_OK );
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1 );

        PrintWriter pw = res.getWriter();
        ((GetCapabilities) request).writeCapabilitiesReport( pw );
        pw.flush();
      }
      else if ( request.getOperation().equals( Request.Operation.DescribeCoverage ) )
      {
        res.setContentType( "text/xml" );
        res.setStatus( HttpServletResponse.SC_OK );
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1 );

        PrintWriter pw = res.getWriter();
        ((DescribeCoverage) request).writeDescribeCoverageDoc( pw );
        pw.flush();
      }
      else if ( request.getOperation().equals( Request.Operation.GetCoverage ) )
      {
        File covFile = ((GetCoverage) request).writeCoverageDataToFile();
        if ( covFile != null && covFile.exists())
        {
          int pos = covFile.getPath().lastIndexOf( "." );
          String suffix = covFile.getPath().substring( pos );
          String resultFilename = request.getDataset().getDatasetName(); // this is name browser will show
          if ( !resultFilename.endsWith( suffix ) )
            resultFilename = resultFilename + suffix;
          res.setHeader( "Content-Disposition", "attachment; filename=" + resultFilename );

          ServletUtil.returnFile( servlet, "", covFile.getPath(), req, res, ((GetCoverage) request).getFormat().getMimeType() );
          if ( deleteImmediately ) covFile.delete();
        }
        else
        {
          log.error( "handleKVP(): Failed to create coverage file" + (covFile == null ? "" : (": " + covFile.getAbsolutePath() )) );
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
    catch ( Throwable t)
    {
      log.error( "Unknown problem.", t);
      handleExceptionReport( res, new WcsException( "Unknown problem", t));
    }
    finally
    {
      if ( request != null && request.getDataset() != null )
      {
        request.getDataset().close();
      }
    }
  }

  public GetCapabilities.ServiceInfo getServiceInfo()
  {
    // Todo Figure out how to configure serviceId info.
    GetCapabilities.ServiceInfo sid;
    GetCapabilities.ResponsibleParty respParty;
    GetCapabilities.ResponsibleParty.ContactInfo contactInfo;
    contactInfo = new GetCapabilities.ResponsibleParty.ContactInfo(
            Collections.singletonList( "voice phone"),
            Collections.singletonList( "voice phone"),
            new GetCapabilities.ResponsibleParty.Address(
                    Collections.singletonList( "address"), "city", "admin area", "postal code", "country",
                    Collections.singletonList( "email")
            ),
            new GetCapabilities.ResponsibleParty.OnlineResource(null, "title")
    );
    respParty= new GetCapabilities.ResponsibleParty( "indiv name", "org name", "position",
                                                     contactInfo );
    sid = new GetCapabilities.ServiceInfo( "name", "label", "description",
                                          Collections.singletonList( "keyword" ),
                                          respParty, "no fees",
                                          Collections.singletonList( "no access constraints" ) );

    return sid;
  }

  public void handleExceptionReport( HttpServletResponse res, WcsException exception )
          throws IOException
  {
    res.setContentType( "application/vnd.ogc.se_xml" );
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
