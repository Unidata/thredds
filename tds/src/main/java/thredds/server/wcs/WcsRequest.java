package thredds.server.wcs;

import thredds.servlet.ServletUtil;
import thredds.servlet.ThreddsConfig;
import thredds.servlet.DatasetHandler;
import thredds.wcs.v1_1_0.WcsException;
import thredds.wcs.v1_1_0.GetCapabilities;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

import ucar.nc2.dt.GridDataset;

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

  private Operation operation;
  private String negotiatedVersion;

  // GetCapabilities parameters
  private String sectionsParam;
  private String updateSequenceParam;
  private String acceptFormatsParam;

  private List<GetCapabilities.Section> sections;
  private GetCapabilities.ServiceId serviceId;
  private GetCapabilities.ServiceProvider serviceProvider;

  // DescribeCoverage parameters

  // GetCoverage parameters

  // Dataset
  private GridDataset dataset;


  public enum Operation
  { GetCapabilities, DescribeCoverage, GetCoverage }

  public enum RequestEncoding { GET_KVP, POST_XML, POST_SOAP }

  public enum Format { NONE, GeoTIFF, GeoTIFF_Float, NetCDF3 }

  public WcsRequest( HttpServletRequest req, HttpServletResponse res )
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
      throw new WcsException( WcsException.Code.OperationNotSupported, requestParam, "");
    }

    if ( operation.equals( Operation.GetCapabilities))
    {
      sectionsParam = ServletUtil.getParameterIgnoreCase( req, "Sections");
      updateSequenceParam = ServletUtil.getParameterIgnoreCase( req, "UpdateSequence" );
      acceptFormatsParam = ServletUtil.getParameterIgnoreCase( req, "AccpetFormats" );

      if ( sectionsParam != null )
      {
        String[] sectionArray = sectionsParam.split( ",");
        sections = new ArrayList<GetCapabilities.Section>( sectionArray.length);
        for ( String curSection : sectionArray )
        {
          sections.add( GetCapabilities.Section.valueOf( curSection ));
        }
      }
      else
        sections = Collections.emptyList();

      serviceId = null;
      serviceProvider = null;
//      serviceId = new GetCapabilities.ServiceId( ThreddsConfig.get( "WCS.serviceId.title", null ),
//                                                 ThreddsConfig.get( "WCS.serviceId.abstract", null),
//                                                 )

    }
    else if ( operation.equals( Operation.DescribeCoverage ) )
    {

    }
    else if ( operation.equals( Operation.GetCoverage ) )
    {

    }

    // Find the dataset.
    String datasetPath = req.getPathInfo();
    try
    {
      dataset = DatasetHandler.openGridDataset( req, res, datasetPath );
    }
    catch ( IOException e )
    {
      log.warn( "WcsRequest(): Failed to open dataset <" + datasetPath + ">: " + e.getMessage());
      throw new WcsException( WcsException.Code.NoApplicableCode, null, "Failed to open dataset, \"" + datasetPath + "\"." );
    }
    if ( dataset == null )
    {
      log.debug( "WcsRequest(): Unknown dataset <" + datasetPath + ">.");
      throw new WcsException( WcsException.Code.NoApplicableCode, null, "Unknown dataset, \"" + datasetPath + "\"." );
    }

//    String datasetURL = ServletUtil.getParameterIgnoreCase( req, "dataset" );
//    boolean isRemote = ( datasetURL != null );
//    String datasetPath = isRemote ? datasetURL : req.getPathInfo();
//
//    // convert to a GridDataset
//    GridDataset gd = isRemote ? ucar.nc2.dt.grid.GridDataset.open( datasetPath ) : DatasetHandler.openGridDataset( req, res, datasetPath );
//    if ( gd == null ) return null;
  }


  public String getServiceParam() { return serviceParam; }
  public String getRequestParam() { return requestParam; }
  public String getVersionParam() { return versionParam; }
  public String getAcceptVersionsParam() { return acceptVersionsParam; }

  public Operation getOperation() { return operation; }
  public List<GetCapabilities.Section> getSections() { return Collections.unmodifiableList( sections); }
  public GetCapabilities.ServiceId getServiceId() { return this.serviceId; }
  public GetCapabilities.ServiceProvider getServiceProvider() { return this.serviceProvider; }

  public GridDataset getDataset() { return dataset; }
}
