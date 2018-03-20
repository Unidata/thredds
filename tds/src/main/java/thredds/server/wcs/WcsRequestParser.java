/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.wcs;

import thredds.core.TdsRequestedDataset;
import thredds.servlet.ServletUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.net.URI;

/**
 * Parse an incoming WCS 1.0.0+ request.
 *
 * @author edavis
 * @since 4.0
 */
public class WcsRequestParser {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WcsRequestParser.class);

  public static thredds.server.wcs.v1_0_0_1.WcsRequest parseRequest(String version, URI serverURI, HttpServletRequest req, HttpServletResponse res)
          throws thredds.server.wcs.v1_0_0_1.WcsException, IOException {

// These are handled in WcsServlet. Don't need to validate here.
//    String serviceParam = ServletUtil.getParameterIgnoreCase( req, "Service" );
//    String versionParam = ServletUtil.getParameterIgnoreCase( req, "Version" );
//    String acceptVersionsParam = ServletUtil.getParameterIgnoreCase( req, "AcceptVersions" );

      /* General request info
      Request.Operation operation;
      String datasetPath = TdsPathUtils.extractPath(req, "wcs/");
      boolean isRemote = false;
      if (datasetPath == null) {
        datasetPath = ServletUtil.getParameterIgnoreCase(req, "dataset");
        isRemote = (datasetPath != null);
      }
      if (datasetPath == null) {
        log.debug("parseRequest(): Request did not specify dataset.");
        throw new thredds.wcs.v1_0_0_1.WcsException(thredds.wcs.v1_0_0_1.WcsException.Code.CoverageNotDefined, "",
                "Request did not specify dataset. See \"" + req.getContextPath()
                        + "/catalog.xml\" for available datasets.");
      } */


    TdsRequestedDataset trd = new TdsRequestedDataset(req, "/wcs");
    CoverageCollection gridDataset = null;
    try {
      gridDataset = trd.openAsCoverageDataset(req, res);
      if (gridDataset == null)
        return null;

      thredds.server.wcs.v1_0_0_1.WcsDataset wcsDataset = new thredds.server.wcs.v1_0_0_1.WcsDataset(gridDataset, trd.getPath());

      // Determine the request operation.
      String requestParam = ServletUtil.getParameterIgnoreCase(req, "Request");
      Request.Operation operation = parseOperation(requestParam);
      if (operation == null) {
        log.debug("parseRequest(): Unsupported operation request [" + requestParam + "].");
        throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "Request",
                "Unsupported operation request [" + requestParam + "].");
      }

      // Handle "GetCapabilities" request.
      if (operation.equals(Request.Operation.GetCapabilities)) {
        String sectionParam = ServletUtil.getParameterIgnoreCase(req, "Section");
        String updateSequenceParam = ServletUtil.getParameterIgnoreCase(req, "UpdateSequence");

        if (sectionParam == null)
          sectionParam = "";
        thredds.server.wcs.v1_0_0_1.GetCapabilities.Section section = parseGetCapabilitiesSection(sectionParam);
        if (section == null) {
          log.debug("parseRequest(): Unsupported GetCapabilities section requested [" + sectionParam + "].");
          throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "Section",
                  "Unsupported GetCapabilities section requested [" + sectionParam + "].");
        }

        return new thredds.server.wcs.v1_0_0_1.GetCapabilities(operation, version, wcsDataset, serverURI, section, updateSequenceParam, null);
      }
      // Handle "DescribeCoverage" request.
      else if (operation.equals(Request.Operation.DescribeCoverage)) {

        // Parse the "coverage" parameter (null, or csv String).
        String coverageIdListParam = ServletUtil.getParameterIgnoreCase(req, "Coverage");
        List<String> coverageIdList;
        if (coverageIdListParam != null)
          coverageIdList = splitCommaSeperatedList(coverageIdListParam);
        else {
          coverageIdList = new ArrayList<>();
          for (thredds.server.wcs.v1_0_0_1.WcsCoverage curCov : wcsDataset.getAvailableCoverageCollection())
            coverageIdList.add(curCov.getName());
        }

        return new thredds.server.wcs.v1_0_0_1.DescribeCoverage(operation, version, wcsDataset, coverageIdList);
      }
      // Handle "GetCoverage" request.
      else if (operation.equals(Request.Operation.GetCoverage)) {
        String coverageId = ServletUtil.getParameterIgnoreCase(req, "Coverage");
        String crs = ServletUtil.getParameterIgnoreCase(req, "CRS");
        String responseCRS = ServletUtil.getParameterIgnoreCase(req, "RESPONSE_CRS");
        String bbox = ServletUtil.getParameterIgnoreCase(req, "BBOX");
        String time = ServletUtil.getParameterIgnoreCase(req, "TIME");
        // ToDo The name of this parameter is dependent on the coverage (see WcsCoverage.getRangeSetAxisName()).
        String parameter = ServletUtil.getParameterIgnoreCase(req, "Vertical");
        String formatString = ServletUtil.getParameterIgnoreCase(req, "FORMAT");

        // Assign and validate PARAMETER ("Vertical") parameter.
        thredds.server.wcs.v1_0_0_1.WcsCoverage.VerticalRange verticalRange = parseRangeSetAxisValues(parameter);

        // Assign and validate FORMAT parameter.
        if (formatString == null) {
          log.debug("parseRequest(): FORMAT parameter required.");
          throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "FORMAT", "FORMAT parameter required.");
        }
        Request.Format format = parseFormat(formatString);
        if (format == null) {
          String msg = "Unrecognized FORMAT parameter value [" + formatString + "].";
          log.debug("parseRequest(): " + msg);
          throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "FORMAT", msg);
        }

        // Return GetCoverage request.
        return new thredds.server.wcs.v1_0_0_1.GetCoverage(operation, version, wcsDataset, coverageId,
                crs, responseCRS, parseBoundingBox(bbox),
                parseTime(time),
                verticalRange,
                format);
      } else {
        log.debug("parseRequest(): Invalid request operation [" + requestParam + "].");
      }
      throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "Request",
              "Invalid requested operation [" + requestParam + "].");

    } catch (Throwable t) {
      if (gridDataset != null)
        gridDataset.close();
      throw t;
    }
  }


  private static Request.Operation parseOperation(String operationString) {
    Request.Operation[] ops = Request.Operation.values();
    for (Request.Operation curOp : ops)
      if (curOp.toString().equalsIgnoreCase(operationString))
        return curOp;

    return null;
  }

  private static thredds.server.wcs.v1_0_0_1.GetCapabilities.Section parseGetCapabilitiesSection(String sectionString) {
    thredds.server.wcs.v1_0_0_1.GetCapabilities.Section[] sections = thredds.server.wcs.v1_0_0_1.GetCapabilities.Section.values();
    for (thredds.server.wcs.v1_0_0_1.GetCapabilities.Section curSection : sections)
      if (curSection.toString().equalsIgnoreCase(sectionString))
        return curSection;

    return null;
  }

  private static Request.Format parseFormat(String formatString)
          throws thredds.server.wcs.v1_0_0_1.WcsException {
    Request.Format[] formats = Request.Format.values();
    for (Request.Format curFormat : formats)
      if (curFormat.toString().equalsIgnoreCase(formatString))
        return curFormat;

    return null;
  }

  private static Request.BoundingBox parseBoundingBox(String bboxString) throws thredds.server.wcs.v1_0_0_1.WcsException {
    if (bboxString == null || bboxString.equals(""))
      return null;

    String[] bboxSplit = bboxString.split(",");
    if (bboxSplit.length != 4) {
      String msg = "BBOX [" + bboxString + "] has more values [" + bboxSplit.length
              + "] than expected [4] (not limited to X and Y).";
      log.debug("parseBoundingBox(): " + msg);
      throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "BBOX", msg);
    }
    double[] minP = new double[2];
    double[] maxP = new double[2];
    try {
      minP[0] = Double.parseDouble(bboxSplit[0]);
      minP[1] = Double.parseDouble(bboxSplit[1]);
      maxP[0] = Double.parseDouble(bboxSplit[2]);
      maxP[1] = Double.parseDouble(bboxSplit[3]);
    } catch (NumberFormatException e) {
      String msg = "BBOX [" + bboxString + "] contains an invalid number(s).";
      log.debug("parseBoundingBox(): " + msg);
      throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "BBOX", msg);
    }

    if (minP[0] > maxP[0] || minP[1] > maxP[1]) {
      String msg = "BBOX [" + bboxString + "] minimum point larger than maximum point.";
      log.debug("parseBoundingBox(): " + msg);
      throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "BBOX", msg);
    }

    return new Request.BoundingBox(minP, maxP);
  }

  private static CalendarDateRange parseTime(String time) throws thredds.server.wcs.v1_0_0_1.WcsException {
    if (time == null || time.equals(""))
      return null;

    CalendarDateRange dateRange;

    // try
    {
      if (time.contains(",")) {
        log.debug("parseTime(): Unsupported time parameter (list) [" + time + "].");
        throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "TIME",
                "Not currently supporting time list.");
        //String[] timeList = time.split( "," );
        //dateRange = new DateRange( date, date, null, null );
      } else if (time.contains("/")) {
        String[] timeRange = time.split("/");
        if (timeRange.length != 2) {
          log.debug("parseTime(): Unsupported time parameter (time range with resolution) [" + time + "].");
          throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "TIME",
                  "Not currently supporting time range with resolution.");
        }
        dateRange = CalendarDateRange.of(CalendarDate.parseISOformat(null, timeRange[0]), CalendarDate.parseISOformat(null, timeRange[1]));

      } else {
        CalendarDate date = CalendarDate.parseISOformat(null, time);
        dateRange = CalendarDateRange.of(date, date);
      }
    }
    /* catch ( ParseException e )
    {
      log.debug( "parseTime(): Failed to parse time parameter [" + time + "]: " + e.getMessage() );
      throw new WcsException( WcsException.Code.InvalidParameterValue, "TIME",
                              "Invalid time format [" + time + "]." );
    }  */

    return dateRange;
  }

  private static thredds.server.wcs.v1_0_0_1.WcsCoverage.VerticalRange parseRangeSetAxisValues(String rangeSetAxisSelectionString)
          throws thredds.server.wcs.v1_0_0_1.WcsException {
    if (rangeSetAxisSelectionString == null || rangeSetAxisSelectionString.equals(""))
      return null;

    thredds.server.wcs.v1_0_0_1.WcsCoverage.VerticalRange range;

    if (rangeSetAxisSelectionString.contains(",")) {
      log.debug("parseRangeSetAxisValues(): Vertical value list not supported [" + rangeSetAxisSelectionString + "].");
      throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "Vertical",
              "Not currently supporting list of Vertical values (just range, i.e., \"min/max\").");
    } else if (rangeSetAxisSelectionString.contains("/")) {
      String[] rangeSplit = rangeSetAxisSelectionString.split("/");
      if (rangeSplit.length != 2) {
        log.debug("parseRangeSetAxisValues(): Unsupported Vertical value (range with resolution) ["
                + rangeSetAxisSelectionString + "].");
        throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "Vertical",
                "Not currently supporting vertical range with resolution.");
      }
      double minValue = 0;
      double maxValue = 0;
      try {
        minValue = Double.parseDouble(rangeSplit[0]);
        maxValue = Double.parseDouble(rangeSplit[1]);
      } catch (NumberFormatException e) {
        log.debug("parseRangeSetAxisValues(): Failed to parse Vertical range min or max [" + rangeSetAxisSelectionString + "]: " + e.getMessage());
        throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "Vertical", "Failed to parse Vertical range min or max.");
      }
      if (minValue > maxValue) {
        log.debug("parseRangeSetAxisValues(): Vertical range must be \"min/max\" [" + rangeSetAxisSelectionString + "].");
        throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "Vertical", "Vertical range must be \"min/max\".");
      }
      range = new thredds.server.wcs.v1_0_0_1.WcsCoverage.VerticalRange(minValue, maxValue, 1);
    } else {
      double value = 0;
      try {
        value = Double.parseDouble(rangeSetAxisSelectionString);
      } catch (NumberFormatException e) {
        log.debug("parseRangeSetAxisValues(): Failed to parse Vertical value [" + rangeSetAxisSelectionString + "]: " + e.getMessage());
        throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "Vertical", "Failed to parse Vertical value.");
      }
      range = new thredds.server.wcs.v1_0_0_1.WcsCoverage.VerticalRange(value, 1);
    }

/* cannot be null
   if (range == null) {
      log.debug("parseRangeSetAxisValues(): Invalid Vertical range requested [" + rangeSetAxisSelectionString + "].");
      throw new thredds.server.wcs.v1_0_0_1.WcsException(thredds.server.wcs.v1_0_0_1.WcsException.Code.InvalidParameterValue, "Vertical", "Invalid Vertical range requested.");
    } */

    return range;
  }

  private static List<String> splitCommaSeperatedList(String identifiers) {
    List<String> idList = new ArrayList<>();
    String[] idArray = identifiers.split(",");
    for (String anIdArray : idArray) {
      idList.add(anIdArray.trim());
    }
    return idList;
  }
}
