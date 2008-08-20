package thredds.wcs.v1_0_0_1;

import thredds.wcs.WcsRequest;
import ucar.nc2.dt.GridDataset;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class WcsRequestBuilder
{
  private WcsRequest.Operation operation;
  private GridDataset dataset;

  public static WcsRequestBuilder newWcsRequestFactory( WcsRequest.Operation operation,
                                                        GridDataset dataset )
  {
    return new WcsRequestBuilder( operation, dataset );
  }

  private WcsRequestBuilder( WcsRequest.Operation operation,
                             GridDataset dataset )
  {
    if ( operation == null ) throw new IllegalArgumentException( "Operation may not be null." );
    if ( dataset == null ) throw new IllegalArgumentException( "Dataset may not be null." );

    this.operation = operation;
    this.dataset = dataset;
  }

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
