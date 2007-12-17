package thredds.wcs.v1_0_0_Plus;

import java.io.File;
import java.text.ParseException;
import java.util.List;
import java.util.ArrayList;

import ucar.unidata.geoloc.EPSG_OGC_CF_Helper;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.CoordinateAxis1D;
import thredds.datatype.DateRange;
import thredds.datatype.DateType;

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

  private LatLonRect requestLatLonBBox;
  private AxisSubset requestVertSubset;
  private DateRange timeRange;
  private List<String> rangeSubset;

  public GetCoverage( Operation operation, String version, WcsDataset dataset,
                      String coverageId, String crs, String responseCRS,
                      String bbox, String time, String rangeSubset, String format )
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
    if ( bbox == null && time == null )
      throw new WcsException( WcsException.Code.MissingParameterValue, "BBOX", "BBOX and/or TIME required.");
    if ( bbox != null )
    {
      String[] bboxSplit = splitBoundingBox( bbox);
      requestLatLonBBox = genRequestLatLonBoundingBox( bboxSplit, coverage.getCoordinateSystem());
      requestVertSubset = genRequestVertSubset( bboxSplit, this.coverage.getCoordinateSystem().getVerticalAxis());
    }
    if ( time != null )
      timeRange = parseTime( time);

    // WIDTH, HEIGHT, DEPTH parameters not needed since the only interpolation method is "NONE".
    // RESX, RESY, RESZ parameters not needed since the only interpolation method is "NONE".

    // Assign and validate RangeSubset parameter.
    if ( rangeSubset != null )
            this.rangeSubset = parseRangeSubset( rangeSubset);//, coverage.getRange());

    // Assign and validate FORMAT parameter.
    if ( format == null )
    {
      log.error( "GetCoverage(): FORMAT parameter required.");
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", "FORMAT parameter required.");
    }
    if ( ! format.equalsIgnoreCase( this.coverage.getAllowedCoverageFormat() ))
    {
      throw new WcsException( WcsException.Code.InvalidFormat, "", "Request format <" + format + "> now allowed <" + this.coverage.getAllowedCoverageFormat() + ">");
    }
  }

  public File writeCoverageDataToFile()
          throws WcsException
  {
    return this.coverage.writeCoverageDataToFile( this.requestLatLonBBox,
                                                  this.requestVertSubset,
                                                  this.rangeSubset,
                                                  this.timeRange);
  }

  private String[] splitBoundingBox( String bbox)
          throws WcsException
  {
    if ( bbox == null || bbox.equals( "" ) )
      return null;

    String[] bboxSplit = bbox.split( "," );
    if ( bboxSplit.length != 4 && bboxSplit.length != 6 )
    {
      log.error( "splitBoundingBox(): BBOX <" + bbox + "> must be \"minx,miny,maxx,maxy[,minz,maxz]\"." );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", "BBOX <" + bbox + "> not in expected format \"minx,miny,maxx,maxy[,minz,maxz]\"." );
    }
    return bboxSplit;
  }

  private LatLonRect genRequestLatLonBoundingBox( String[] bboxSplit, GridCoordSystem gcs)
          throws WcsException
  {
    if ( bboxSplit == null || gcs == null )
      return null;
    if ( bboxSplit.length < 4 )
      throw new IllegalArgumentException( "BBOX contains fewer than four items \"" + bboxSplit.toString() + "\".");

    double minx = 0;
    double miny = 0;
    double maxx = 0;
    double maxy = 0;
    try
    {
      minx = Double.parseDouble( bboxSplit[0] );
      miny = Double.parseDouble( bboxSplit[1] );
      maxx = Double.parseDouble( bboxSplit[2] );
      maxy = Double.parseDouble( bboxSplit[3] );
    }
    catch ( NumberFormatException e )
    {
      String message = "BBOX item(s) have incorrect number format [not double] <" + bboxSplit.toString() + ">.";
      log.error( "genRequestLatLonBoundingBox(): " + message + " - " + e.getMessage());
      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", message );
    }

    LatLonPointImpl minll = new LatLonPointImpl( miny, minx );
    LatLonPointImpl maxll = new LatLonPointImpl( maxy, maxx );

    LatLonRect requestLatLonRect = new LatLonRect( minll, maxll );

    LatLonRect covLatLonRect = gcs.getLatLonBoundingBox();
//    if ( ! requestLatLonRect.containedIn( covLatLonRect))
//    {
//      log.error( "genRequestLatLonBoundingBox(): BBOX <" + bbox + "> not contained in coverage BBOX <"+ covLatLonRect.toString2()+">.");
//      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", "BBOX <" + bbox + "> not contained in coverage.");
//    }

    return requestLatLonRect;
  }

  private AxisSubset genRequestVertSubset( String[] bboxSplit, CoordinateAxis1D vertAxis )
          throws WcsException
  {
    if ( bboxSplit == null || bboxSplit.length == 4 )
      return null;
    if ( bboxSplit.length != 6 )
      throw new IllegalArgumentException( "BBOX does not contain six items \"" + bboxSplit.toString() + "\"." );

    double minz = 0;
    double maxz = 0;
    try
    {
      minz = Double.parseDouble( bboxSplit[4] );
      maxz = Double.parseDouble( bboxSplit[5] );
    }
    catch ( NumberFormatException e )
    {
      String message = "BBOX item(s) have incorrect number format [not double] <" + bboxSplit.toString() + ">.";
      log.error( "genRequestVertSubset(): " + message + " - " + e.getMessage() );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", message );
    }

    return new AxisSubset( vertAxis, minz, maxz, 1 );
  }

  private DateRange parseTime( String time )
          throws WcsException
  {
    if ( time == null || time.equals( ""))
      return null;

    DateRange dateRange;

    try
    {
      if ( time.indexOf( ",") != -1 )
      {
        log.error( "parseTime(): Unsupported time parameter (list) <" + time + ">.");
        throw new WcsException( WcsException.Code.InvalidParameterValue, "TIME",
                                "Not currently supporting time list." );
        //String[] timeList = time.split( "," );
        //dateRange = new DateRange( date, date, null, null );
      }
      else if ( time.indexOf( "/") != -1 )
      {
        String[] timeRange = time.split( "/" );
        if ( timeRange.length != 2)
        {
          log.error( "parseTime(): Unsupported time parameter (time range with resolution) <" + time + ">.");
          throw new WcsException( WcsException.Code.InvalidParameterValue, "TIME", "Not currently supporting time range with resolution.");
        }
        dateRange = new DateRange( new DateType( timeRange[0], null, null ),
                                   new DateType( timeRange[1], null, null ), null, null );
      }
      else
      {
        DateType date = new DateType( time, null, null );
        dateRange = new DateRange( date, date, null, null );
      }
    }
    catch ( ParseException e )
    {
      log.error( "parseTime(): Failed to parse time parameter <" + time + ">: " + e.getMessage() );

      throw new WcsException( WcsException.Code.InvalidParameterValue, "TIME",
                              "Invalid time format <" + time + ">." );
    }

    return dateRange;
  }

  private List<String> parseRangeSubset( String rangeSubset)
          throws WcsException
  {
    List<String> response = new ArrayList<String>();

    // Default is to return all fields.
    if ( rangeSubset == null || rangeSubset.equals( "" ) )
    {
      response.addAll( this.coverage.getRangeFieldNames() );
      return response;
    }

    // Split the rangeSubset request into fieldSubset requests.
    String[] fieldSubsetArray;
    if ( rangeSubset.indexOf( ";") == -1 )
    {
      fieldSubsetArray = new String[1];
      fieldSubsetArray[0] = rangeSubset;
    }
    else
    {
      fieldSubsetArray = rangeSubset.split( ";" );
    }

    for ( String curFieldSubset : fieldSubsetArray )
    {
      if ( this.coverage.isRangeFieldName( curFieldSubset) )
        response.add( curFieldSubset );
      else
      {
        String message = "Requested range field <" + curFieldSubset + "> not available.";
        log.warn( "parseRangeSubset(): " + message );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "RangeSubset", message );
      }
    }

    return response;
  }

}
