/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.wcs.v1_0_0_1;

import thredds.server.wcs.Request;
import ucar.nc2.ft2.coverage.CoverageCollection;

public abstract class WcsRequestBuilder
{
  public static WcsRequestBuilder newWcsRequestBuilder( String versionString,
                                                        Request.Operation operation,
                                                        CoverageCollection dataset,
                                                        String datasetPath )
  {
    if ( operation == null )
      throw new IllegalArgumentException( "Null operation not allowed.");

    if ( operation.equals( Request.Operation.GetCapabilities ) )
      return new GetCapabilitiesBuilder( versionString, operation, dataset, datasetPath );
    else if ( operation.equals( Request.Operation.DescribeCoverage ) )
      return new DescribeCoverageBuilder( versionString, operation, dataset, datasetPath );
    else if ( operation.equals( Request.Operation.GetCoverage ) )
      return new GetCoverageBuilder( versionString, operation, dataset, datasetPath );
    else
      throw new IllegalArgumentException( "Unknown operation [" + operation.name() + "].");
  }

  private String versionString;
  private Request.Operation operation;
  private CoverageCollection dataset;
  private String datasetPath;
  private WcsDataset wcsDataset;

  WcsRequestBuilder( String versionString,
                     Request.Operation operation,
                     CoverageCollection dataset,
                     String datasetPath )
  {
    if ( versionString == null || versionString.length() == 0 )
      throw new IllegalArgumentException( "Versions string may not be null or empty string." );
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
    this.wcsDataset = new WcsDataset( this.dataset, this.datasetPath );
  }

  public Request.Operation getOperation() { return this.operation; }
  public boolean isGetCapabilitiesOperation() { return this.operation.equals( Request.Operation.GetCapabilities ); }
  public boolean isDescribeCoverageOperation() { return this.operation.equals( Request.Operation.DescribeCoverage ); }
  public boolean isGetCoverageOperation() { return this.operation.equals( Request.Operation.GetCoverage ); }

  public String getVersionString() { return versionString; }
  public CoverageCollection getDataset() { return dataset; }
  public String getDatasetPath() { return datasetPath; }
  public WcsDataset getWcsDataset() { return wcsDataset; }
}
