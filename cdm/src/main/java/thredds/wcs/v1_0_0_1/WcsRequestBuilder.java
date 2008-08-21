package thredds.wcs.v1_0_0_1;

import thredds.wcs.WcsRequest;
import ucar.nc2.dt.GridDataset;

import java.net.URI;
import java.util.List;

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

  public static WcsRequestBuilder newWcsRequestFactory( String versionString,
                                                        WcsRequest.Operation operation,
                                                        GridDataset dataset )
  {
    return new WcsRequestBuilder( versionString, operation, dataset );
  }

  private WcsRequestBuilder( String versionString,
                             WcsRequest.Operation operation,
                             GridDataset dataset )
  {
    if ( versionString == null || versionString.equals( ""))
      throw new IllegalArgumentException( "Versions string may not be null or empty string.");
    if ( operation == null )
      throw new IllegalArgumentException( "Operation may not be null." );
    if ( dataset == null )
      throw new IllegalArgumentException( "Dataset may not be null." );

    this.operation = operation;
    this.dataset = dataset;
  }


  private URI serverURI;
  private GetCapabilities.Section section;
  private String updateSequence;
  private GetCapabilities.ServiceInfo serviceInfo;

  private List<String> coverageIds;

  private String coverageId, crs, responseCRS, bbox, time, parameter, format;

  public GetCapabilities buildGetCapabilities()
  {
    if ( ! operation.equals(  WcsRequest.Operation.GetCapabilities ))
      throw new IllegalStateException( "Can't build GetCapabilities request, " + operation.name() + " builder was specified.");

    return null;
    //return new GetCapabilities( operation, "", new WcsDataset( dataset, ""), null, null, null, null  );
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
