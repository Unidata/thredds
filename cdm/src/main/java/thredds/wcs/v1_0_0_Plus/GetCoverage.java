package thredds.wcs.v1_0_0_Plus;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.text.ParseException;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.nc2.util.DiskCache2;
import ucar.ma2.InvalidRangeException;
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
  private String crs, responseCRS;
  private String bbox, time, parameter, format;
  // private String width, height, depth; // Don't need since only interpolation method is "NONE".
  // private String resx, resy, resz; // Don't need since only interpolation method is "NONE".

  private LatLonRect bboxLatLonRect;
  private DateRange timeRange;
  private List<String> rangeSetAxisValueList;


  public GetCoverage( Operation operation, String version, String datasetPath, GridDataset dataset,
                      String coverageId, String crs, String responseCRS,
                      String bbox, String time, String parameter, String format )
          throws WcsException
  {
    super( operation, version, datasetPath, dataset);

    // Assign and validate coverage ID parameter.
    this.coverageId = coverageId;
    if ( this.coverageId == null )
      throw new WcsException( WcsException.Code.MissingParameterValue, "coverage", "Coverage identifier required." );
    if ( !this.isAvailableCoverageName( this.coverageId ) )
      throw new WcsException( WcsException.Code.InvalidParameterValue, "coverage", "Unknown coverage identifier <" + this.coverageId + ">." );
    GridDatatype coverage = this.getAvailableCoverage( this.coverageId );
    if ( coverage == null ) // Double check just in case.
      throw new WcsException( WcsException.Code.InvalidParameterValue, "coverage", "Unknown coverage identifier <" + this.coverageId + ">." );

    // Assign and validate request and response CRS parameters.
    this.crs = crs;
    this.responseCRS = responseCRS;

    if ( this.crs == null )
      throw new WcsException( WcsException.Code.MissingParameterValue, "CRS", "Request CRS required.");
    if ( ! this.crs.equalsIgnoreCase( getDefaultRequestCrs() ) )
      throw new WcsException( WcsException.Code.InvalidParameterValue, "CRS", "Request CRS <" + this.crs + "> not allowed <" + getDefaultRequestCrs() + ">." );

    String nativeCRS = EPSG_OGC_CF_Helper.getWcs1_0CrsId( coverage.getCoordinateSystem().getProjection() );
    if ( nativeCRS == null )
      throw new WcsException( WcsException.Code.CoverageNotDefined, "", "Coverage not in recognized CRS. (???)");

    // Response CRS not required if data is in latLon ("OGC:CRS84"). Default is request CRS.
    if ( this.responseCRS == null )
    {
      if ( ! nativeCRS.equalsIgnoreCase( getDefaultRequestCrs()))
        throw new WcsException( WcsException.Code.MissingParameterValue, "Response_CRS", "Response CRS required." );
    }
    else if ( ! this.responseCRS.equalsIgnoreCase( nativeCRS))
        throw new WcsException( WcsException.Code.InvalidParameterValue, "response_CRS", "Respnse CRS <" + this.responseCRS + "> not allowed <" + nativeCRS + ">." );

    // Assign and validate BBOX and TIME parameters.
    this.bbox = bbox;
    this.time = time;
    if ( this.bbox == null && this.time == null )
      throw new WcsException( WcsException.Code.MissingParameterValue, "BBOX", "BBOX and/or TIME required.");
    if ( this.bbox != null )
      bboxLatLonRect = parseBoundingBox( this.bbox, coverage.getCoordinateSystem().getLatLonBoundingBox());
    if ( this.time != null )
      timeRange = parseTime( this.time);

    // Assign and validate PARAMETER ("Vertical") parameter.
    this.parameter = parameter;
    if ( this.parameter != null )
            rangeSetAxisValueList = parseRangeSetAxisValues( this.parameter);

    // Assign and validate FORMAT parameter.
    this.format = format;
    if ( this.format == null )
    {
      log.error( "GetCoverage(): FORMAT parameter required.");
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", "FORMAT parameter required.");
    }
    if ( ! this.format.equalsIgnoreCase( getAllowedCoverageFormat() ))
    {
      throw new WcsException( WcsException.Code.InvalidFormat, "", "Request format <" + this.format + "> now allowed <" + getAllowedCoverageFormat() + ">");
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
    File ncFile = getDiskCache().getCacheFile( this.getDatasetPath() + "-" + coverageId + ".nc" );

    NetcdfCFWriter writer = new NetcdfCFWriter();
    try
    {
      rangeSetAxisValueList.size();  // ToDo figure out how to deal with vertical selection.
      writer.makeFile( ncFile.getPath(), this.getDataset(),
                       Collections.singletonList( coverageId ),
                       this.bboxLatLonRect, this.timeRange,
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

  private List<String> parseRangeSetAxisValues( String rangeSetAxisSelectionString)
          throws WcsException
  {
    if ( rangeSetAxisSelectionString == null || rangeSetAxisSelectionString.equals( "" ) )
      return null;

    List<String> rangeSetAxisValueList;

    if ( rangeSetAxisSelectionString.indexOf( "," ) != -1 )
    {
      String[] listSplit = rangeSetAxisSelectionString.split( ",");
      rangeSetAxisValueList = new ArrayList<String>();
      StringBuffer badValueList = new StringBuffer();
      for ( String curItem : listSplit )
      {
        //if ( ! coverage.isRangeSetAxisValue( curItem) )
        if ( false)
        {
          badValueList.append( badValueList.length() > 0 ? "," : "").append( curItem);
        }
        else
          rangeSetAxisValueList.add( curItem);
      }
      if ( badValueList.length() > 0 )
      {
        log.error( "parseRangeSetAxisValues(): Unrecognized RangeSet Axis values <" + badValueList + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "Vertical",
                                "Unrecognized RangeSet Axis values <" + badValueList + ">." );
      }
    }
    else if ( rangeSetAxisSelectionString.indexOf( "/" ) != -1 )
    {
      String[] timeRange = rangeSetAxisSelectionString.split( "/" );
      rangeSetAxisValueList = new ArrayList<String>();
      if ( timeRange.length != 2 )
      {
        log.error( "parseRangeSetAxisValues(): Unsupported Vertical value (range with resolution) <" + rangeSetAxisSelectionString + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "Vertical", "Not currently supporting vertical range with resolution." );
      }
      rangeSetAxisValueList.add( timeRange[0]);
      rangeSetAxisValueList.add( timeRange[1]);
    }
    else
    {
      //if ( ! coverage.isRangeSetAxisValue( rangeSetAxisSelectionString ) )
      if ( false )
      {
        log.error( "parseRangeSetAxisValues(): Unrecognized RangeSet Axis value <" + rangeSetAxisSelectionString + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "Vertical",
                                "Unrecognized RangeSet Axis value <" + rangeSetAxisSelectionString + ">." );
      }
      else
        rangeSetAxisValueList = Collections.singletonList( rangeSetAxisSelectionString);
    }

    return rangeSetAxisValueList;
  }
}
