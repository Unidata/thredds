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
package thredds.wcs.v1_1_0;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import ucar.nc2.dt.GridDataset;

/**
 * Represent the incoming WCS 1.1.0 request.
 *
 * @author edavis
 * @since 4.0
 * ToDo Make this an AbstractFactory class for GetCapabilities, DescribeCoverge, and GetCoverage classes.
 */
public class Request
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( Request.class );

  // General request info
  private Operation operation;
  private String negotiatedVersion;

  private String expectedVersion = "1.1.0"; 

  // GetCapabilities request info
  private List<GetCapabilities.Section> sections;

  // DescribeCoverage request info
  private List<String> identifierList;

  // GetCoverage request info
  private String identifier;

  // Dataset
  private String datasetPath;
  private GridDataset dataset;
  private List<String> availableCoverageNames;


  public enum Operation
  {
    GetCapabilities, DescribeCoverage, GetCoverage
  }

  public enum RequestEncoding
  {
    GET_KVP, POST_XML, POST_SOAP
  }

  public enum Format
  {
    NONE( "" ),
    GeoTIFF( "image/tiff" ),
    GeoTIFF_Float( "image/tiff" ),
    NetCDF3( "application/x-netcdf" );

    private String mimeType;

    Format( String mimeType ) { this.mimeType = mimeType; }

    public String getMimeType() { return mimeType; }

    public static Format getFormat( String mimeType )
    {
      for ( Format curSection : Format.values() )
      {
        if ( curSection.mimeType.equals( mimeType ) )
          return curSection;
      }
      throw new IllegalArgumentException( "No such instance <" + mimeType + ">." );
    }
  }

  static public Request getGetCapabilitiesRequest( Operation operation,
                                                   String negotiatedVersion,
                                                   List<GetCapabilities.Section> sections,
                                                   String datasetPath,
                                                   GridDataset dataset)
  {
    Request req = new Request( operation, negotiatedVersion, datasetPath, dataset );
    if ( ! operation.equals( Operation.GetCapabilities ) )
      throw new IllegalArgumentException( "The \"" + operation.toString() + "\" operation not supported by this method.");
    req.sections = sections;

    if ( req.sections == null )
      throw new IllegalArgumentException( "Non-null section list required.");

    return req;
  }

  static public Request getDescribeCoverageRequest( Operation operation,
                                                    String negotiatedVersion,
                                                    List<String> identifiers,
                                                    String datasetPath,
                                                    GridDataset dataset)
          throws WcsException
  {
    Request req = new Request( operation, negotiatedVersion, datasetPath, dataset );
    if ( ! operation.equals( Operation.DescribeCoverage ) )
      throw new IllegalArgumentException( "The \"" + operation.toString() + "\" operation not supported by this method." );
    if ( ! req.availableCoverageNames.containsAll( identifiers))
      throw new WcsException( WcsException.Code.InvalidParameterValue, "identifiers", "The \"identifiers\" parameter contains unrecognized values: " + identifiers);
    req.identifierList = identifiers;

    return req;
  }

  static public Request getGetCoverageRequest( Operation operation,
                                               String negotiatedVersion,
                                               String identifier,
                                               String datasetPath,
                                               GridDataset dataset)
          throws WcsException
  {
    Request req = new Request( operation, negotiatedVersion, datasetPath, dataset );
    if ( !operation.equals( Operation.GetCoverage ) )
      throw new IllegalArgumentException( "The \"" + operation.toString() + "\" operation not supported by this method." );
    if ( !req.availableCoverageNames.contains( identifier ) )
      throw new WcsException( WcsException.Code.InvalidParameterValue, "identifier", "Unrecognized value in \"identifier\" parameter: " + identifier );
    req.identifier = identifier;

    return req;
  }

  Request( Operation operation, String negotiatedVersion, String datasetPath, GridDataset dataset)
  {
    this.operation = operation;
    this.negotiatedVersion = negotiatedVersion;
    this.datasetPath = datasetPath;
    this.dataset = dataset;
    this.availableCoverageNames = new ArrayList<String>();
    for ( GridDataset.Gridset gs : this.dataset.getGridsets() )
    {
      this.availableCoverageNames.add( gs.getGeoCoordSystem().getName() );
    }


    if ( operation == null )
      throw new IllegalArgumentException( "Non-null operation required." );
    if ( this.negotiatedVersion == null )
      throw new IllegalArgumentException( "Non-null negotiated version required." );
    if ( ! this.negotiatedVersion.equals( expectedVersion) )
      throw new IllegalArgumentException( "Version <" + negotiatedVersion + "> not as expected <" + expectedVersion + ">." );
    if ( this.datasetPath == null )
      throw new IllegalArgumentException( "Non-null dataset path required." );
    if ( this.dataset == null )
      throw new IllegalArgumentException( "Non-null dataset required." );
  }

  // ---------- General getters
  public Operation getOperation() { return operation; }

  public String getDatasetName()
  {
    int pos = datasetPath.lastIndexOf( "/");
    return pos == -1 ? datasetPath : datasetPath.substring( pos + 1 );
  }

  public String getDatasetPath() { return datasetPath; }
  public GridDataset getDataset() { return dataset; }
  public List<String> getAvailableCoverageNames() { return availableCoverageNames; }


  // ---------- GetCapabilities getters
  public List<GetCapabilities.Section> getSections()
  {
    return Collections.unmodifiableList( sections );
  }

  // ---------- DescribeCoverage getters
  public List<String> getIdentifierList() { return identifierList; }

  // ---------- GetCoverage getters
  public String getIdentifier() { return identifier; }

}
