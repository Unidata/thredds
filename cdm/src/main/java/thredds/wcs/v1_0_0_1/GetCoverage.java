package thredds.wcs.v1_0_0_1;

import java.io.File;

import ucar.unidata.geoloc.ogc.EPSG_OGC_CF_Helper;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.units.DateRange;
import thredds.wcs.Request;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class GetCoverage extends WcsRequest
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( GetCoverage.class );


  private WcsCoverage coverage;

  private LatLonRect bboxLatLonRect = null;
  private DateRange timeRange;
  private WcsCoverage.VerticalRange rangeSetAxisValueRange;

  private Request.Format format;
  // GeoTIFF only supported for requests for a single time and a single vertical level.
  private boolean isSingleTimeRequest = false;
  private boolean isSingleVerticalRequest = false;


  public GetCoverage( Request.Operation operation, String version, WcsDataset dataset,
                      String coverageId, String crs, String responseCRS,
                      Request.BoundingBox bbox, DateRange timeRange,
                      WcsCoverage.VerticalRange verticalRange, Request.Format format )
          throws WcsException
  {
    super( operation, version, dataset);

    // Validate coverage ID parameter.
    if ( coverageId == null )
      throw new WcsException( WcsException.Code.MissingParameterValue, "coverage", "Coverage identifier required." );
    if ( !this.getDataset().isAvailableCoverageName( coverageId ) )
      throw new WcsException( WcsException.Code.InvalidParameterValue, "coverage", "Unknown coverage identifier <" + coverageId + ">." );
    this.coverage = this.getDataset().getAvailableCoverage( coverageId );
    if ( this.coverage == null ) // Double check just in case.
      throw new WcsException( WcsException.Code.InvalidParameterValue, "coverage", "Unknown coverage identifier <" + coverageId + ">." );

    // Assign and validate request and response CRS parameters.

    if ( crs == null )
      throw new WcsException( WcsException.Code.MissingParameterValue, "CRS", "Request CRS required.");
    if ( ! crs.equalsIgnoreCase( this.coverage.getDefaultRequestCrs() ) )
      throw new WcsException( WcsException.Code.InvalidParameterValue, "CRS", "Request CRS <" + crs + "> not allowed <" + this.coverage.getDefaultRequestCrs() + ">." );

    String nativeCRS = EPSG_OGC_CF_Helper.getWcs1_0CrsId( coverage.getCoordinateSystem().getProjection() );
    if ( nativeCRS == null )
      throw new WcsException( WcsException.Code.CoverageNotDefined, "", "Coverage not in recognized CRS. (???)");

    // Response CRS not required if data is in latLon ("OGC:CRS84"). Default is request CRS.
    if ( responseCRS == null )
    {
      if ( ! nativeCRS.equalsIgnoreCase( this.coverage.getDefaultRequestCrs()))
        throw new WcsException( WcsException.Code.MissingParameterValue, "Response_CRS", "Response CRS required." );
    }
    else if ( ! responseCRS.equalsIgnoreCase( nativeCRS))
        throw new WcsException( WcsException.Code.InvalidParameterValue, "response_CRS", "Respnse CRS <" + responseCRS + "> not allowed <" + nativeCRS + ">." );

    // Assign and validate BBOX and TIME parameters.
// -----
//    WCS Spec says at least one of BBOX and TIME are required in a request.
//    We will not require, default is everything.
//
//    if ( bbox == null && time == null )
//      throw new WcsException( WcsException.Code.MissingParameterValue, "BBOX", "BBOX and/or TIME required.");
// -----

    if ( bbox != null )
      bboxLatLonRect = convertBoundingBox( bbox, coverage.getCoordinateSystem());

    this.timeRange = timeRange;
    this.isSingleTimeRequest = timeRange.isPoint();

    // WIDTH, HEIGHT, DEPTH parameters not needed since the only interpolation method is "NONE".
    // RESX, RESY, RESZ parameters not needed since the only interpolation method is "NONE".

    // Assign and validate PARAMETER ("Vertical") parameter.
    this.rangeSetAxisValueRange = verticalRange;
    this.isSingleVerticalRequest = verticalRange.isSinglePoint();

    // Assign and validate FORMAT parameter.
    if ( format == null )
    {
      log.error( "GetCoverage(): FORMAT parameter required.");
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", "FORMAT parameter required.");
    }

    if ( ! this.coverage.isSupportedCoverageFormat( this.format ))
    {
      String msg = "Unsupported format value [" + format + "].";
      log.error( "GetCoverage(): " + msg );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", msg );
    }
    this.format = format;

    if ( this.format == Request.Format.GeoTIFF || this.format == Request.Format.GeoTIFF_Float)
    {
      // Check that request is for one time and one vertical level
      // since that is all we support for GeoTIFF[-Float].
      if ( ! this.isSingleTimeRequest && ! this.isSingleVerticalRequest )
      {
        StringBuffer msgB = new StringBuffer( "GeoTIFF supported only for requests at a single time [");
        if ( this.timeRange != null )
          msgB.append( this.timeRange);
        msgB.append( "] and a single vertical level [");
        if ( verticalRange != null )
          msgB.append( verticalRange );
        msgB.append( "].");

        log.error( "GetCoverage(): " + msgB );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", msgB.toString() );
      }
    }
  }

  public Request.Format getFormat() { return format; }

  public File writeCoverageDataToFile()
          throws WcsException
  {
    return this.coverage.writeCoverageDataToFile( this.format,
                                                  this.bboxLatLonRect,
                                                  this.rangeSetAxisValueRange,
                                                  this.timeRange);
  }

  private LatLonRect convertBoundingBox( Request.BoundingBox bbox, GridCoordSystem gcs)
          throws WcsException
  {
    if ( bbox == null )
      return null;

    LatLonPointImpl minll = new LatLonPointImpl( bbox.getMinPointValue( 1 ), bbox.getMinPointValue( 0 ));
    LatLonPointImpl maxll = new LatLonPointImpl( bbox.getMaxPointValue( 1 ), bbox.getMaxPointValue( 0 ) );

    LatLonRect bboxLatLonRect = new LatLonRect( minll, maxll );

//    if ( ! bboxLatLonRect.containedIn( covLatLonRect))
//    {
//      log.error( "convertBoundingBox(): BBOX <" + bbox + "> not contained in coverage BBOX <"+ covLatLonRect.toString2()+">.");
//      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", "BBOX <" + bbox + "> not contained in coverage.");
//    }

    return bboxLatLonRect;
  }
}
