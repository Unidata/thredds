/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.wcs.v1_0_0_1;

import thredds.wcs.Request;
import ucar.nc2.dt.GridDataset;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public abstract class WcsRequestBuilder
{
  public static WcsRequestBuilder newWcsRequestBuilder( String versionString,
                                                        Request.Operation operation,
                                                        GridDataset dataset,
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
  private GridDataset dataset;
  private String datasetPath;
  private WcsDataset wcsDataset;

  WcsRequestBuilder( String versionString,
                     Request.Operation operation,
                     GridDataset dataset,
                     String datasetPath )
  {
    if ( versionString == null || versionString.equals( "" ) )
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
  public GridDataset getDataset() { return dataset; }
  public String getDatasetPath() { return datasetPath; }
  public WcsDataset getWcsDataset() { return wcsDataset; }
}
