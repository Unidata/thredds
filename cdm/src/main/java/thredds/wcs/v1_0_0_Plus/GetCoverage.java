package thredds.wcs.v1_0_0_Plus;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.text.ParseException;

import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.nc2.util.DiskCache2;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.unidata.geoloc.EPSG_OGC_CF_Helper;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
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

  private String coverageId;

  private WcsCoverage coverage;

  private LatLonRect bboxLatLonRect;
  private DateRange timeRange;
  private Range rangeSetAxisValueRange;


  public GetCoverage( Operation operation, String version, WcsDataset dataset,
                      String coverageId, String crs, String responseCRS,
                      String bbox, String time, String parameter, String format )
          throws WcsException
  {
    super( operation, version, dataset);

    // Assign and validate coverage ID parameter.
    this.coverageId = coverageId;
    if ( this.coverageId == null )
      throw new WcsException( WcsException.Code.MissingParameterValue, "coverage", "Coverage identifier required." );
    if ( !this.getDataset().isAvailableCoverageName( this.coverageId ) )
      throw new WcsException( WcsException.Code.InvalidParameterValue, "coverage", "Unknown coverage identifier <" + this.coverageId + ">." );
    this.coverage = this.getDataset().getAvailableCoverage( this.coverageId );
    if ( this.coverage == null ) // Double check just in case.
      throw new WcsException( WcsException.Code.InvalidParameterValue, "coverage", "Unknown coverage identifier <" + this.coverageId + ">." );

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
      bboxLatLonRect = parseBoundingBox( bbox, coverage.getCoordinateSystem().getLatLonBoundingBox());
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
    if ( ! format.equalsIgnoreCase( this.coverage.getAllowedCoverageFormat() ))
    {
      throw new WcsException( WcsException.Code.InvalidFormat, "", "Request format <" + format + "> now allowed <" + this.coverage.getAllowedCoverageFormat() + ">");
    }
  }

  //public NetcdfFile getCoverageData() {}

  static private DiskCache2 diskCache = null;

  static public void setDiskCache( DiskCache2 _diskCache )
  {
    diskCache = _diskCache;
  }

  static private DiskCache2 getDiskCache()
  {
    if ( diskCache == null )
    {
      log.error( "getDiskCache(): Disk cache has not been set." );
      throw new IllegalStateException( "Disk cache must be set before calling GetCoverage.getDiskCache()." );
      //diskCache = new DiskCache2( "/wcsCache/", true, -1, -1 );
    }
    return diskCache;
  }


  public File writeCoverageDataToFile()
          throws WcsException
  {
    File ncFile = getDiskCache().getCacheFile( this.getDataset().getDatasetPath() + "-" + coverageId + ".nc" );

    //GridDatatype gridDatatype = this.coverage.getGridDatatype().makeSubset( );

    NetcdfCFWriter writer = new NetcdfCFWriter();
    try
    {
      this.coverage.getCoordinateSystem().getVerticalAxis().isNumeric();
      writer.makeFile( ncFile.getPath(), this.getDataset().getDataset(),
                       Collections.singletonList( coverageId ),
                       this.bboxLatLonRect,
                       this.rangeSetAxisValueRange,
                       this.timeRange,
                       true, 1, 1, 1 );
    }
    catch ( InvalidRangeException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to subset coverage <" + coverageId + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.CoverageNotDefined, "", "Failed to subset coverage <" + coverageId + ">." );
    }
    catch ( IOException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to write file for requested coverage <" + coverageId + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.UNKNOWN, "", "Problem creating coverage <" + coverageId + ">." );
    }
    return ncFile;

  }

  private LatLonRect parseBoundingBox( String bbox, LatLonRect covLatLonRect)
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

    if ( ! bboxLatLonRect.containedIn( covLatLonRect))
    {
      log.error( "parseBoundingBox(): BBOX <" + bbox + "> not contained in coverage BBOX <"+ covLatLonRect.toString2()+">.");
      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", "BBOX <" + bbox + "> not contained in coverage.");
    }

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

  private Range parseRangeSetAxisValues( String rangeSetAxisSelectionString)
          throws WcsException
  {
    if ( rangeSetAxisSelectionString == null || rangeSetAxisSelectionString.equals( "" ) )
      return null;

    Range range;

    if ( rangeSetAxisSelectionString.indexOf( "," ) != -1 )
    {
      log.error( "parseRangeSetAxisValues(): Vertical value list not supported <" + rangeSetAxisSelectionString + ">." );
      throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeSetAxisName(), "Not currently supporting list of Vertical values (just range, i.e., \"min/max\")." );
    }
    else if ( rangeSetAxisSelectionString.indexOf( "/" ) != -1 )
    {
      String[] rangeSplit = rangeSetAxisSelectionString.split( "/" );
      if ( rangeSplit.length != 2 )
      {
        log.error( "parseRangeSetAxisValues(): Unsupported Vertical value (range with resolution) <" + rangeSetAxisSelectionString + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeSetAxisName(), "Not currently supporting vertical range with resolution." );
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
        throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeSetAxisName(), "Failed to parse Vertical range min or max." );
      }
      if ( minValue > maxValue)
      {
        log.error( "parseRangeSetAxisValues(): Vertical range must be \"min/max\" <" + rangeSetAxisSelectionString + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeSetAxisName(), "Vertical range must be \"min/max\"." );
      }
      range = coverage.getRangeSetAxisRange( minValue, maxValue );
    }
    else
    {
      if ( ! coverage.isRangeSetAxisValue( rangeSetAxisSelectionString ) )
      {
        log.error( "parseRangeSetAxisValues(): Unrecognized RangeSet Axis value <" + rangeSetAxisSelectionString + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeSetAxisName(),
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
          throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeSetAxisName(), "Failed to parse Vertical value." );
        }
        range = coverage.getRangeSetAxisRange( value, value );
      }
    }

    if ( range == null)
    {
      log.error( "parseRangeSetAxisValues(): Invalid Vertical range requested <" + rangeSetAxisSelectionString + ">." );
      throw new WcsException( WcsException.Code.InvalidParameterValue, coverage.getRangeSetAxisName(), "Invalid Vertical range requested." );
    }

    return range;
  }
}
