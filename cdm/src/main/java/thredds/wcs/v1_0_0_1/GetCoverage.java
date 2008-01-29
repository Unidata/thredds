package thredds.wcs.v1_0_0_1;

import java.io.File;
import java.text.ParseException;

import ucar.unidata.geoloc.ogc.EPSG_OGC_CF_Helper;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;

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

  private LatLonRect bboxLatLonRect;
  private DateRange timeRange;
  private WcsCoverage.VerticalRange rangeSetAxisValueRange;

  private Format format;
  // GeoTIFF only supported for requests for a single time and a single vertical level.
  private boolean isSingleTimeRequest = false;
  private boolean isSingleVerticalRequest = false;


  public GetCoverage( Operation operation, String version, WcsDataset dataset,
                      String coverageId, String crs, String responseCRS,
                      String bbox, String time, String parameter, String format )
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
      bboxLatLonRect = parseBoundingBox( bbox, coverage.getCoordinateSystem());
    if ( time != null )
      timeRange = parseTime( time);

    // WIDTH, HEIGHT, DEPTH parameters not needed since the only interpolation method is "NONE".
    // RESX, RESY, RESZ parameters not needed since the only interpolation method is "NONE".

    // Assign and validate PARAMETER ("Vertical") parameter.
    if ( parameter != null )
            rangeSetAxisValueRange = parseRangeSetAxisValues( parameter);

    // Assign and validate FORMAT parameter.
    if ( format == null )
    {
      log.error( "GetCoverage(): FORMAT parameter required.");
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", "FORMAT parameter required.");
    }
    try
    {
      this.format = Format.valueOf( format.trim() );
    }
    catch( IllegalArgumentException e)
    {
      String msg = "Unknown format value [" + format + "].";
      log.error( "GetCoverage(): " + msg);
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", msg);
    }

    if ( ! this.coverage.isSupportedCoverageFormat( this.format ))
    {
      String msg = "Unsupported format value [" + format + "].";
      log.error( "GetCoverage(): " + msg );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", msg );
    }

    if ( this.format == WcsRequest.Format.GeoTIFF || this.format == WcsRequest.Format.GeoTIFF_Float)
    {
      // Check that request is for one time and one vertical level
      // since that is all we support for GeoTIFF[-Float].
      if ( ! this.isSingleTimeRequest && ! this.isSingleVerticalRequest )
      {
        StringBuffer msgB = new StringBuffer( "GeoTIFF supported only for requests at a single time [");
        if ( time != null )
          msgB.append( time);
        msgB.append( "] and a single vertical level [");
        if ( parameter != null )
          msgB.append( parameter);
        msgB.append( "].");

        log.error( "GetCoverage(): " + msgB );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", msgB.toString() );
      }
    }
  }

  public File writeCoverageDataToFile()
          throws WcsException
  {
    return this.coverage.writeCoverageDataToFile( this.format,
                                                  this.bboxLatLonRect,
                                                  this.rangeSetAxisValueRange,
                                                  this.timeRange);
  }

  private LatLonRect parseBoundingBox( String bbox, GridCoordSystem gcs)
          throws WcsException
  {
    if ( bbox == null || bbox.equals( "") )
      return null;

    String[] bboxSplit = bbox.split( ",");
    if ( bboxSplit.length != 4)
    {
      log.error( "parseBoundingBox(): BBOX <" + bbox + "> not limited to X and Y." );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", "BBOX <"+bbox+"> has more values <" + bboxSplit.length + "> than expected <4>.");
    }
    double minx = Double.parseDouble( bboxSplit[0] );
    double miny = Double.parseDouble( bboxSplit[1] );
    double maxx = Double.parseDouble( bboxSplit[2] );
    double maxy = Double.parseDouble( bboxSplit[3] );

    LatLonPointImpl minll = new LatLonPointImpl( miny, minx );
    LatLonPointImpl maxll = new LatLonPointImpl( maxy, maxx );

    LatLonRect bboxLatLonRect = new LatLonRect( minll, maxll );

//    if ( ! bboxLatLonRect.containedIn( covLatLonRect))
//    {
//      log.error( "parseBoundingBox(): BBOX <" + bbox + "> not contained in coverage BBOX <"+ covLatLonRect.toString2()+">.");
//      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", "BBOX <" + bbox + "> not contained in coverage.");
//    }

    return bboxLatLonRect;
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
        this.isSingleTimeRequest = true;
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

  private WcsCoverage.VerticalRange parseRangeSetAxisValues( String rangeSetAxisSelectionString)
          throws WcsException
  {
    if ( rangeSetAxisSelectionString == null || rangeSetAxisSelectionString.equals( "" ) )
      return null;

    WcsCoverage.VerticalRange range;

    if ( rangeSetAxisSelectionString.indexOf( "," ) != -1 )
    {
      log.error( "parseRangeSetAxisValues(): Vertical value list not supported <" + rangeSetAxisSelectionString + ">." );
      throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeField().getAxis().getName(), "Not currently supporting list of Vertical values (just range, i.e., \"min/max\")." );
    }
    else if ( rangeSetAxisSelectionString.indexOf( "/" ) != -1 )
    {
      String[] rangeSplit = rangeSetAxisSelectionString.split( "/" );
      if ( rangeSplit.length != 2 )
      {
        log.error( "parseRangeSetAxisValues(): Unsupported Vertical value (range with resolution) <" + rangeSetAxisSelectionString + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeField().getAxis().getName(), "Not currently supporting vertical range with resolution." );
      }
      double minValue = 0;
      double maxValue = 0;
      try
      {
        minValue = Double.parseDouble( rangeSplit[0] );
        maxValue = Double.parseDouble( rangeSplit[1] );
      }
      catch ( NumberFormatException e )
      {
        log.error( "parseRangeSetAxisValues(): Failed to parse Vertical range min or max <" + rangeSetAxisSelectionString + ">: " + e.getMessage() );
        throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeField().getAxis().getName(), "Failed to parse Vertical range min or max." );
      }
      if ( minValue > maxValue)
      {
        log.error( "parseRangeSetAxisValues(): Vertical range must be \"min/max\" <" + rangeSetAxisSelectionString + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeField().getAxis().getName(), "Vertical range must be \"min/max\"." );
      }
      range = new WcsCoverage.VerticalRange( minValue, maxValue, 1);
    }
    else
    {
      if ( ! coverage.getRangeField().getAxis().getValues().contains( rangeSetAxisSelectionString))
      {
        log.error( "parseRangeSetAxisValues(): Unrecognized RangeSet Axis value <" + rangeSetAxisSelectionString + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeField().getAxis().getName(),
                                "Unrecognized RangeSet Axis value <" + rangeSetAxisSelectionString + ">." );
      }
      else
      {
        double value = 0;
        try
        {
          value = Double.parseDouble( rangeSetAxisSelectionString );
        }
        catch ( NumberFormatException e )
        {
          log.error( "parseRangeSetAxisValues(): Failed to parse Vertical value <" + rangeSetAxisSelectionString + ">: " + e.getMessage() );
          throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeField().getAxis().getName(), "Failed to parse Vertical value." );
        }
        range = new WcsCoverage.VerticalRange( value, value, 1 );
        this.isSingleVerticalRequest = true;
      }
    }

    if ( range == null)
    {
      log.error( "parseRangeSetAxisValues(): Invalid Vertical range requested <" + rangeSetAxisSelectionString + ">." );
      throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeField().getAxis().getName(), "Invalid Vertical range requested." );
    }

    return range;
  }

}
