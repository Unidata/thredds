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
package thredds.wcs.v1_0_0_Plus;

import java.io.File;
import java.text.ParseException;
import java.util.List;
import java.util.ArrayList;

import ucar.unidata.geoloc.ogc.EPSG_OGC_CF_Helper;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;

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

  private Format format;
  // Requests for GeoTIFF encoding must be for a single time, single vertical level, and single range field.
  private boolean isSingleTimeRequest = false;
  private boolean isSingleVerticalRequest = false;
  private boolean isSingleRangeFieldRequest = false;

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
// -----
//    WCS Spec says at least one of BBOX and TIME are required in a request.
//    We will not require, default is everything.
//    
//    if ( bbox == null && time == null )
//      throw new WcsException( WcsException.Code.MissingParameterValue, "BBOX", "BBOX and/or TIME required.");
// -----
    if ( bbox != null && ( ! bbox.equals( "" ) ) )
    {
      String[] bboxSplit = splitBoundingBox( bbox);
      requestLatLonBBox = genRequestLatLonBoundingBox( bboxSplit, coverage.getCoordinateSystem());

      CoordinateAxis1D vertAxis = this.coverage.getCoordinateSystem().getVerticalAxis();
      if ( vertAxis != null )
        requestVertSubset = genRequestVertSubset( bboxSplit, vertAxis );
    }
    if ( time != null && ( ! time.equals( "" )) )
      timeRange = parseTime( time);

    // WIDTH, HEIGHT, DEPTH parameters not needed since the only interpolation method is "NONE".
    // RESX, RESY, RESZ parameters not needed since the only interpolation method is "NONE".

    // Assign and validate RangeSubset parameter.
    this.rangeSubset = parseRangeSubset( rangeSubset);//, coverage.getRange());

    // Assign and validate FORMAT parameter.
    if ( format == null || format.equals( "" ))
    {
      log.error( "GetCoverage(): FORMAT parameter required.");
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", "FORMAT parameter required.");
    }
    try
    {
      this.format = Format.valueOf( format.trim());
    }
    catch ( IllegalArgumentException e )
    {
      String msg = "Unknown format value [" + format + "].";
      log.error( "GetCoverage(): " + msg );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", msg );
    }

    if ( ! this.coverage.isSupportedCoverageFormat( this.format ))
    {
      String msg = "Unsupported format value [" + format + "].";
      log.error( "GetCoverage(): " + msg );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", msg );
    }

    if ( this.format == WcsRequest.Format.GeoTIFF || this.format == WcsRequest.Format.GeoTIFF_Float)
    {
      // Check that request is for one time and one vertical level and one range field
      // since that is all we support for GeoTIFF[-Float].
      if ( ! this.isSingleTimeRequest &&
           ! this.isSingleVerticalRequest &&
           ! this.isSingleRangeFieldRequest )
      {
        StringBuffer msgB = new StringBuffer( "GeoTIFF supported only for requests at a single time [");
        if ( time != null )
          msgB.append( time);
        msgB.append( "] and a single vertical level [");
        if ( bbox != null )
          msgB.append( bbox);
        msgB.append( "] and a single range field [");
        if ( rangeSubset != null )
          msgB.append( rangeSubset );
        msgB.append( "].");

        log.error( "GetCoverage(): " + msgB );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", msgB.toString() );
      }
    }

  }

  public Format getFormat() { return format; }
  
  public File writeCoverageDataToFile()
          throws WcsException
  {
    return this.coverage.writeCoverageDataToFile( this.format,
                                                  this.requestLatLonBBox,
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
    // Check if no vertical range request.
    if ( bboxSplit == null || bboxSplit.length == 4 )
    {
      // If there is no vertical axis (or only one level), still a single level request.
      if ( vertAxis == null || vertAxis.getShape(0) == 1 )
        this.isSingleVerticalRequest = true;

      return null;
    }
    if ( bboxSplit.length != 6 )
    {
      String message = "BBOX must have 4 or 6 items [" + bboxSplit.toString() + "].";
      log.error( "genRequestVertSubset(): " + message );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", message );
    }

    // If there is no vertical axis (or only one level), still a single level request.
    if ( vertAxis == null || vertAxis.getShape(0) == 1 )
    {
      this.isSingleVerticalRequest = true;
      return null;
    }

    double minz = 0;
    double maxz = 0;
    try
    {
      minz = Double.parseDouble( bboxSplit[4] );
      maxz = Double.parseDouble( bboxSplit[5] );
    }
    catch ( NumberFormatException e )
    {
      String message = "BBOX item(s) have incorrect number format (not double) [" + bboxSplit.toString() + "].";
      log.error( "genRequestVertSubset(): " + message + " - " + e.getMessage() );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", message );
    }

    AxisSubset axisSubset = new AxisSubset( vertAxis, minz, maxz, 1 );
    Range range = null;
    try
    {
      range = axisSubset.getRange();
    }
    catch ( InvalidRangeException e )
    {
      String message = "BBOX results in invalid array index range [" + bboxSplit.toString() + "].";
      log.error( "genRequestVertSubset(): " + message + " - " + e.getMessage() );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", message );
    }
    if ( range.length() == 1 )
    {
      // Check whether vertical range results in a single level.
      this.isSingleVerticalRequest = true;
      return null;
    }

    return axisSubset;
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

  private List<String> parseRangeSubset( String rangeSubset)
          throws WcsException
  {
    List<String> response = new ArrayList<String>();

    // Default is to return all fields.
    if ( rangeSubset == null || rangeSubset.equals( "" ) )
    {
      response.addAll( this.coverage.getRangeFieldNames() );
      if ( response.size() == 1 )
        this.isSingleRangeFieldRequest = true;
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

    if ( response.size() == 1 )
      this.isSingleRangeFieldRequest = true;

    return response;
  }

}
