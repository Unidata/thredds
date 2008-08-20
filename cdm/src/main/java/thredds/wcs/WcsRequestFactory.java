package thredds.wcs;

import ucar.nc2.dt.GridDataset;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class WcsRequestFactory
{
  private WcsRequest request;

  private Version version;
  private WcsRequest.Operation operation;
  private GridDataset dataset;
  
  public static WcsRequestFactory newWcsRequestFactory( Version version,
                                                        WcsRequest.Operation operation,
                                                        GridDataset dataset )
  {
    return new WcsRequestFactory( version, operation, dataset);
  }

  private WcsRequestFactory( Version version,
                             WcsRequest.Operation operation,
                             GridDataset dataset )
  {
    if ( version == null ) throw new IllegalArgumentException( "Version may not be null.");
    if ( operation == null ) throw new IllegalArgumentException( "Operation may not be null.");
    if ( dataset == null ) throw new IllegalArgumentException( "Dataset may not be null.");
    
    this.version = version;
    this.operation = operation;
    this.dataset = dataset;
  }

  public WcsRequest getRequest()
  {
    return request;
  }
}
