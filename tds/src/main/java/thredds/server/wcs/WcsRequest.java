package thredds.server.wcs;

import thredds.servlet.ServletUtil;
import thredds.servlet.ThreddsConfig;
import thredds.wcs.v1_1_0.ExceptionReport;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

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

  private HttpServletRequest req;

  // General parameters
  private String serviceParam;
  private String requestParam;
  private String versionParam;
  private String acceptVersionsParam;

  // GetCapabilities parameters
  private String sectionsParam;
  private String updateSequenceParam;
  private String acceptFormatsParam;

  private String negotiatedVersion;
  private Operation operation;

  public enum Operation
  { GetCapabilities, DescribeCoverage, GetCoverage }

  public enum RequestEncoding { GET_KVP, POST_XML, POST_SOAP }

  public enum Format { NONE, GeoTIFF, GeoTIFF_Float, NetCDF3 }

  public WcsRequest( HttpServletRequest req )
          throws WcsException
  {
    this.req = req;

    serviceParam = ServletUtil.getParameterIgnoreCase( req, "Service" );
    requestParam = ServletUtil.getParameterIgnoreCase( req, "Request" );
    versionParam = ServletUtil.getParameterIgnoreCase( req, "Version" );
    acceptVersionsParam = ServletUtil.getParameterIgnoreCase( req, "AcceptVersions" );

    try
    {
      this.operation = Operation.valueOf( requestParam);
    }
    catch ( IllegalArgumentException e )
    {
      ExceptionReport.Exception excep = new ExceptionReport.Exception( ExceptionReport.Code.OperationNotSupported, requestParam, Collections.singletonList( "") );
      throw new WcsException( "", excep);
    }

    if ( operation.equals( Operation.GetCapabilities))
    {
      sectionsParam = ServletUtil.getParameterIgnoreCase( req, "Sections");
      updateSequenceParam = ServletUtil.getParameterIgnoreCase( req, "UpdateSequence" );
      acceptFormatsParam = ServletUtil.getParameterIgnoreCase( req, "AccpetFormats" );

      String serviceIdTitle = ThreddsConfig.get( "WCS.serviceIdTitle", null );
//      serviceIdTitle = "need a title";
//      serviceIdAbstract = "need an abstract";
//      serviceIdKeywords = new ArrayList<String>();
//      serviceIdKeywords.add( "need keywords");
//      serviceType = "OGC WCS";
//      serviceTypeVersion = new ArrayList<String>();
//      serviceTypeVersion.add("1.1.0");
//      fees = "NONE";
//      accessConstraints = new ArrayList<String>();
//      accessConstraints.add( "NONE");

    }
    else if ( operation.equals( Operation.DescribeCoverage ) )
    {

    }
    else if ( operation.equals( Operation.GetCoverage ) )
    {

    }
    else
    {
    }

  }


  public String getServiceParam() { return serviceParam; }
  public String getRequestParam() { return requestParam; }
  public String getVersionParam() { return versionParam; }
  public String getAcceptVersionsParam() { return acceptVersionsParam; }

  public Operation getOperation() { return operation; }

}
