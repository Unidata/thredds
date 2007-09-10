package thredds.server.wcs;

import thredds.servlet.ServletUtil;
import thredds.servlet.ThreddsConfig;

import javax.servlet.http.HttpServletRequest;

/**
 * Represent the incoming WCS 1.1.0 request.
 *
 * @author edavis
 * @since 4.0
 */
public class WcsRequest
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( WcsRequest.class );

  private String versionString; // 1.1.0
  private String serviceString; // WCS
  private Request request;

  public WcsRequest( HttpServletRequest req )
  {
    String request = ServletUtil.getParameterIgnoreCase( req, "REQUEST" );

    if ( request.equals( Request.GetCapabilities))
    {
      String serviceIdTitle = ThreddsConfig.get( "WCS.serviceIdTitle", null );

    }

  }




  public enum RequestEncoding { GET_KVP, POST_XML, POST_SOAP }
  public enum Request { GetCapabilities, DescribeCoverage, GetCoverage }
  public enum Format { NONE, GeoTIFF, GeoTIFF_Float, NetCDF3 }
}
