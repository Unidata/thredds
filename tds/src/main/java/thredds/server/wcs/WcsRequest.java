package thredds.server.wcs;

import thredds.servlet.ServletUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * _more_
 *
 * @author edavis
 * @since Sep 4, 2007 4:00:10 PM
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

  }




  public enum RequestEncoding { GET_KVP, POST_XML, POST_SOAP }
  public enum Request { GetCapabilities, DescribeCoverage, GetCoverage }
  public enum Format { NONE, GeoTIFF, GeoTIFF_Float, NetCDF3 }
}
