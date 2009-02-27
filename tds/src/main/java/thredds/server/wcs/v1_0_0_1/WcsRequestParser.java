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
package thredds.server.wcs.v1_0_0_1;

import thredds.wcs.v1_0_0_1.*;
import thredds.wcs.Request;
import thredds.servlet.ServletUtil;
import thredds.servlet.DatasetHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;

/**
 * Parse an incoming WCS 1.0.0+ request.
 *
 * @author edavis
 * @since 4.0
 */
public class WcsRequestParser
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( WcsRequestParser.class );

  public static WcsRequest parseRequest( String version, URI serverURI, HttpServletRequest req, HttpServletResponse res )
          throws WcsException, IOException
  {
    GridDataset gridDataset = null;
    try
    {
// These are handled in WcsServlet. Don't need to validate here.
//    String serviceParam = ServletUtil.getParameterIgnoreCase( req, "Service" );
//    String versionParam = ServletUtil.getParameterIgnoreCase( req, "Version" );
//    String acceptVersionsParam = ServletUtil.getParameterIgnoreCase( req, "AcceptVersions" );

      // General request info
      Request.Operation operation;
      String datasetPath = req.getPathInfo();
      gridDataset = openDataset( req, res );
      if ( gridDataset == null )
      {
        log.debug( "parseRequest(): Failed to open dataset (???).");
        throw new WcsException( WcsException.Code.CoverageNotDefined, "", "Failed to open dataset.");
      }
      WcsDataset wcsDataset = new WcsDataset( gridDataset, datasetPath);

      // Determine the request operation.
      String requestParam = ServletUtil.getParameterIgnoreCase( req, "Request" );
      try
    {
      operation = Request.Operation.valueOf( requestParam );
      }
      catch ( IllegalArgumentException e )
      {
        log.debug( "parseRequest(): Unsupported operation request [" + requestParam + "].");
        throw new WcsException( WcsException.Code.InvalidParameterValue, "Request", "Unsupported operation request [" + requestParam + "]." );
      }

      // Handle "GetCapabilities" request.
      if ( operation.equals( Request.Operation.GetCapabilities ) )
      {
        String sectionParam = ServletUtil.getParameterIgnoreCase( req, "Section" );
        String updateSequenceParam = ServletUtil.getParameterIgnoreCase( req, "UpdateSequence" );

        if ( sectionParam == null)
          sectionParam = "";
        GetCapabilities.Section section = null;
        try
        {
          section = GetCapabilities.Section.getSection( sectionParam);
        }
        catch ( IllegalArgumentException e )
        {
          log.debug( "parseRequest(): Unsupported GetCapabilities section requested [" + sectionParam + "]." );
          throw new WcsException( WcsException.Code.InvalidParameterValue, "Section", "Unsupported GetCapabilities section requested [" + sectionParam + "]." );
        }

        return new GetCapabilities( operation, version, wcsDataset, serverURI, section, updateSequenceParam, null);
      }
      // Handle "DescribeCoverage" request.
      else if ( operation.equals( Request.Operation.DescribeCoverage ) )
      {
        String coverageIdListParam = ServletUtil.getParameterIgnoreCase( req, "Coverage" );
        if ( coverageIdListParam == null )
        {
          log.debug( "parseRequest(): GetCapabilities request requires \"Coverage\" parameter." );
          throw new WcsException( WcsException.Code.MissingParameterValue, "Coverage", "GetCapabilities request requires \"Coverage\" parameter." );
        }

        return new DescribeCoverage( operation, version, wcsDataset, splitCommaSeperatedList( coverageIdListParam ));
      }
      // Handle "GetCoverage" request.
      else if ( operation.equals( Request.Operation.GetCoverage ) )
      {
        String coverageId = ServletUtil.getParameterIgnoreCase( req, "Coverage" );
        String crs = ServletUtil.getParameterIgnoreCase( req, "CRS" );
        String responseCRS = ServletUtil.getParameterIgnoreCase( req, "RESPONSE_CRS" );
        String bbox = ServletUtil.getParameterIgnoreCase( req, "BBOX" );
        String time = ServletUtil.getParameterIgnoreCase( req, "TIME" );
        // ToDo The name of this parameter is dependent on the coverage (see WcsCoverage.getRangeSetAxisName()).
        String parameter = ServletUtil.getParameterIgnoreCase( req, "Vertical" );
        String formatString = ServletUtil.getParameterIgnoreCase( req, "FORMAT" );

        // Assign and validate PARAMETER ("Vertical") parameter.
        WcsCoverage.VerticalRange verticalRange = parseRangeSetAxisValues( parameter );
        Request.Format format = parseFormat( formatString );

        return new GetCoverage( operation, version, wcsDataset, coverageId,
                                crs, responseCRS, parseBoundingBox( bbox),
                                parseTime( time ),
                                verticalRange,
                                format);
      }
      else
      {
        log.debug( "parseRequest(): Invalid request operation [" + requestParam + "].");
      }
        throw new WcsException( WcsException.Code.InvalidParameterValue, "Request", "Invalid requested operation [" + requestParam + "]." );
      }
    catch ( WcsException e )
    {
      gridDataset.close();
      throw e;
    }
  }

  private static Request.BoundingBox parseBoundingBox( String bboxString )
          throws WcsException
  {
    if ( bboxString == null || bboxString.equals( "" ) )
      return null;

    String[] bboxSplit = bboxString.split( "," );
    if ( bboxSplit.length != 4 )
    {
      log.debug( "parseBoundingBox(): BBOX <" + bboxString + "> not limited to X and Y." );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", "BBOX <" + bboxString + "> has more values <" + bboxSplit.length + "> than expected <4>." );
    }
    double minx = Double.parseDouble( bboxSplit[0] );
    double miny = Double.parseDouble( bboxSplit[1] );
    double maxx = Double.parseDouble( bboxSplit[2] );
    double maxy = Double.parseDouble( bboxSplit[3] );

    double[] minP = new double[2];
    minP[0] = Double.parseDouble( bboxSplit[0] );
    minP[1] = Double.parseDouble( bboxSplit[1] );
    double[] maxP = new double[2];
    maxP[0] = Double.parseDouble( bboxSplit[2] );
    maxP[1] = Double.parseDouble( bboxSplit[3] );

    if ( minP[0] > maxP[0] || minP[1] > maxP[1])
      throw new WcsException( WcsException.Code.InvalidParameterValue, "BBOX", "BBOX [" + bboxString + "] minimum point larger than maximum point.");

    return new Request.BoundingBox( minP, maxP);
  }

  private static DateRange parseTime( String time )
          throws WcsException
  {
    if ( time == null || time.equals( "" ) )
      return null;

    DateRange dateRange;

    try
    {
      if ( time.indexOf( "," ) != -1 )
      {
        log.debug( "parseTime(): Unsupported time parameter (list) <" + time + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "TIME",
                                "Not currently supporting time list." );
        //String[] timeList = time.split( "," );
        //dateRange = new DateRange( date, date, null, null );
      }
      else if ( time.indexOf( "/" ) != -1 )
      {
        String[] timeRange = time.split( "/" );
        if ( timeRange.length != 2 )
        {
          log.debug( "parseTime(): Unsupported time parameter (time range with resolution) <" + time + ">." );
          throw new WcsException( WcsException.Code.InvalidParameterValue, "TIME", "Not currently supporting time range with resolution." );
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
      log.debug( "parseTime(): Failed to parse time parameter <" + time + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "TIME",
                              "Invalid time format <" + time + ">." );
    }

    return dateRange;
  }

  private static WcsCoverage.VerticalRange parseRangeSetAxisValues( String rangeSetAxisSelectionString )
          throws WcsException
  {
    if ( rangeSetAxisSelectionString == null || rangeSetAxisSelectionString.equals( "" ) )
      return null;

    WcsCoverage.VerticalRange range;

    if ( rangeSetAxisSelectionString.indexOf( "," ) != -1 )
    {
      log.debug( "parseRangeSetAxisValues(): Vertical value list not supported <" + rangeSetAxisSelectionString + ">." );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "Vertical", "Not currently supporting list of Vertical values (just range, i.e., \"min/max\")." );
    }
    else if ( rangeSetAxisSelectionString.indexOf( "/" ) != -1 )
    {
      String[] rangeSplit = rangeSetAxisSelectionString.split( "/" );
      if ( rangeSplit.length != 2 )
      {
        log.debug( "parseRangeSetAxisValues(): Unsupported Vertical value (range with resolution) <" + rangeSetAxisSelectionString + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "Vertical", "Not currently supporting vertical range with resolution." );
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
        log.debug( "parseRangeSetAxisValues(): Failed to parse Vertical range min or max <" + rangeSetAxisSelectionString + ">: " + e.getMessage() );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "Vertical", "Failed to parse Vertical range min or max." );
      }
      if ( minValue > maxValue )
      {
        log.debug( "parseRangeSetAxisValues(): Vertical range must be \"min/max\" <" + rangeSetAxisSelectionString + ">." );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "Vertical", "Vertical range must be \"min/max\"." );
      }
      range = new WcsCoverage.VerticalRange( minValue, maxValue, 1 );
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
        log.debug( "parseRangeSetAxisValues(): Failed to parse Vertical value <" + rangeSetAxisSelectionString + ">: " + e.getMessage() );
        throw new WcsException( WcsException.Code.InvalidParameterValue, "Vertical", "Failed to parse Vertical value." );
      }
      range = new WcsCoverage.VerticalRange( value, 1 );
    }

    if ( range == null )
    {
      log.debug( "parseRangeSetAxisValues(): Invalid Vertical range requested <" + rangeSetAxisSelectionString + ">." );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "Vertical", "Invalid Vertical range requested." );
    }

    return range;
  }

  private static Request.Format parseFormat( String formatString )
          throws WcsException
  {
    // Assign and validate FORMAT parameter.
    if ( formatString == null )
    {
      log.debug( "parseFormat(): FORMAT parameter required." );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", "FORMAT parameter required." );
    }
    Request.Format format;
    try
    {
      format = Request.Format.valueOf( formatString.trim() );
    }
    catch ( IllegalArgumentException e )
    {
      String msg = "Unknown format value [" + formatString + "].";
      log.debug( "parseFormat(): " + msg );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "FORMAT", msg );
    }
    return format;
  }

  private static List<String> splitCommaSeperatedList( String identifiers )
  {
    List<String> idList = new ArrayList<String>();
    String[] idArray = identifiers.split( ",");
    for ( int i = 0; i < idArray.length; i++ )
    {
      idList.add( idArray[i].trim());
    }
    return idList;
  }

  private static GridDataset openDataset( HttpServletRequest req, HttpServletResponse res )
          throws WcsException
  {
    String datasetURL = ServletUtil.getParameterIgnoreCase( req, "dataset" );
    boolean isRemote = ( datasetURL != null );
    String datasetPath = isRemote ? datasetURL : req.getPathInfo();

    GridDataset dataset;
    try
    {
      dataset = isRemote ? ucar.nc2.dt.grid.GridDataset.open( datasetPath ) : DatasetHandler.openGridDataset( req, res, datasetPath );
    }
    catch ( IOException e )
    {
      log.debug( "WcsRequestParser(): Failed to open dataset <" + datasetPath + ">: " + e.getMessage() );
      throw new WcsException( "Failed to open dataset, \"" + datasetPath + "\"." );
    }
    if ( dataset == null )
    {
      log.debug( "WcsRequestParser(): Unknown dataset <" + datasetPath + ">." );
      throw new WcsException( "Unknown dataset, \"" + datasetPath + "\"." );
    }
    return dataset;
  }
}
