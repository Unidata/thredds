package thredds.wcs.v1_0_0_1;

import thredds.wcs.WcsRequest;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.units.DateRange;

import java.net.URI;
import java.util.List;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class WcsRequestBuilder
{
  private String versionString;
  private WcsRequest.Operation operation;
  private GridDataset dataset;
  private String datasetPath;

  public static WcsRequestBuilder newWcsRequestFactory( String versionString,
                                                        WcsRequest.Operation operation,
                                                        GridDataset dataset,
                                                        String datasetPath )
  {
    return new WcsRequestBuilder( versionString, operation, dataset, datasetPath );
  }

  private WcsRequestBuilder( String versionString,
                             WcsRequest.Operation operation,
                             GridDataset dataset,
                             String datasetPath )
  {
    if ( versionString == null || versionString.equals( ""))
      throw new IllegalArgumentException( "Versions string may not be null or empty string.");
    if ( operation == null )
      throw new IllegalArgumentException( "Operation may not be null." );
    if ( dataset == null )
      throw new IllegalArgumentException( "Dataset may not be null." );
    if ( datasetPath == null )
      throw new IllegalArgumentException( "Dataset path may not be null." );

    this.versionString = versionString;
    this.operation = operation;
    this.dataset = dataset;
    this.datasetPath = datasetPath;
  }

  public WcsRequest.Operation getOperation() { return this.operation; }
  public boolean isGetCapabilitiesOperation() { return operation.equals( WcsRequest.Operation.GetCapabilities ); }
  public boolean isDescribeCoverageOperation() { return operation.equals( WcsRequest.Operation.DescribeCoverage ); }
  public boolean isGetCoverageOperation() { return operation.equals( WcsRequest.Operation.GetCoverage ); }
  public String getVersionString() { return versionString; }
  public GridDataset getDataset() { return dataset; } 
  public String getDatasetPath() { return datasetPath; }

  // ----- GetCapabilities -----

  private URI serverUri;
  private GetCapabilities.Section section;
  private String updateSequence;
  private GetCapabilities.ServiceInfo serviceInfo;

  public URI getServerUri() { return this.serverUri; }
  public void setServerUri( URI serverUri )
  {
    if ( ! this.isGetCapabilitiesOperation() )
      throw new IllegalStateException( "Server URI only setable for GetCapabilities request.");
    this.serverUri = serverUri;
  }

  public GetCapabilities.Section getSection() { return this.section; }
  public void setSection( GetCapabilities.Section section )
  {
    if ( !this.isGetCapabilitiesOperation() )
      throw new IllegalStateException( "Section only setable for GetCapabilities request." );
    this.section = section;
  }

  public String getUpdateSequence() { return updateSequence; }
  public void setUpdateSequence( String updateSequence )
  {
    if ( !this.isGetCapabilitiesOperation() )
      throw new IllegalStateException( "Update sequence only setable for GetCapabilities request." );
    this.updateSequence = updateSequence;
  }

  public GetCapabilities.ServiceInfo getServiceInfo() { return serviceInfo; }
  public void setServiceInfo( GetCapabilities.ServiceInfo serviceInfo )
  {
    if ( !this.isGetCapabilitiesOperation() )
      throw new IllegalStateException( "Service info only settable for GetCapabilities request." );
    this.serviceInfo = serviceInfo;
  }

  public GetCapabilities buildGetCapabilities()
  {
    if ( !operation.equals( WcsRequest.Operation.GetCapabilities ) )
      throw new IllegalStateException( "Can't build GetCapabilities request, " + operation.name() + " builder was specified." );

    // Check GetCapabilities requirements.
    if (this.serverUri == null)
      throw new IllegalStateException( "Server URI may not be null for GetCapabilities request.");

    return null;
    //return new GetCapabilities( operation, versionString, new WcsDataset( dataset, datasetPath), null, null, null, null  );
  }

  // ----- DescribeCoverage -----

  private List<String> coverageIdList;

  public List<String> getCoverageIdList() { return coverageIdList; }
  public void setCoverageIdList( List<String> coverageIdList )
  {
    if ( !this.isDescribeCoverageOperation() )
      throw new IllegalStateException( "List of coverage IDs only settable for DescribeCoverage request." );
    this.coverageIdList = coverageIdList;
  }

  // ----- GetCoverage -----

  private String coverageId, crs, responseCRS;
  private WcsRequest.BoundingBox bbox;
  private DateRange timeRange;
  private WcsCoverage.VerticalRange verticalRange;  // parameter
  private WcsRequest.Format format;

  public String getCoverageId() { return coverageId; }
  public void setCoverageId( String coverageId )
  {
    if ( !this.isGetCoverageOperation() )
      throw new IllegalStateException( "Coverage ID only settable for GetCoverage request." );
    this.coverageId = coverageId;
  }

  public String getCrs() { return crs; }
  public void setCrs( String crs )
  {
    if ( !this.isGetCoverageOperation() )
      throw new IllegalStateException( "CRS only settable for GetCoverage request." );
    this.crs = crs;
  }

  public String getResponseCRS() { return responseCRS; }
  public void setResponseCRS( String responseCRS )
  {
    if ( !this.isGetCoverageOperation() )
      throw new IllegalStateException( "Response CRS only settable for GetCoverage request." );
    this.responseCRS = responseCRS;
  }

  public WcsRequest.BoundingBox getBbox() { return bbox; }
  public void setBbox( WcsRequest.BoundingBox bbox )
  {
    if ( !this.isGetCoverageOperation() )
      throw new IllegalStateException( "Bounding box only settable for GetCoverage request." );
    this.bbox = bbox;
  }

  public DateRange getTimeRange() { return timeRange; }
  public void setTimeRange( DateRange timeRange )
  {
    if ( !this.isGetCoverageOperation() )
      throw new IllegalStateException( "Time range only settable for GetCoverage request." );
    this.timeRange = timeRange;
  }

  public WcsCoverage.VerticalRange getVerticalRange() { return verticalRange; }
  public void setVerticalRange( WcsCoverage.VerticalRange verticalRange )
  {
    if ( !this.isGetCoverageOperation() )
      throw new IllegalStateException( "Vertical range only settable for GetCoverage request." );
    this.verticalRange = verticalRange;
  }

  public WcsRequest.Format getFormat() { return format; }
  public void setFormat( WcsRequest.Format format )
  {
    if ( !this.isGetCoverageOperation() )
      throw new IllegalStateException( "Format only settable for GetCoverage request." );
    this.format = format;
  }

  public DescribeCoverage buildDescribeCoverage()
  {
    if ( ! operation.equals( WcsRequest.Operation.DescribeCoverage ) )
      throw new IllegalStateException( "Can't build DescribeCoverage request, " + operation.name() + " builder was specified." );

    return null;
  }

  public GetCoverage buildGetCoverage()
  {
    if ( ! operation.equals( WcsRequest.Operation.GetCoverage ) )
      throw new IllegalStateException( "Can't build GetCoverage request, " + operation.name() + " builder was specified." );

    return null;
  }

  public String getImplementationId()
  {
    return "WCS_1_0_TAKE_TWO";
  }


}
