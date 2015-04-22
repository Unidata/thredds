/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.wcs.v1_0_0_Plus;

import thredds.core.TdsRequestedDataset;
import thredds.servlet.ServletUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.wcs.v1_0_0_1.WcsException;
import ucar.nc2.dt.GridDataset;

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

  public static thredds.wcs.v1_0_0_Plus.WcsRequest parseRequest(String version, URI serverURI, HttpServletRequest req, HttpServletResponse res)
          throws IOException, WcsException {
    // These are handled in WcsServlet. Don't need to validate here.
//    String serviceParam = ServletUtil.getParameterIgnoreCase( req, "Service" );
//    String versionParam = ServletUtil.getParameterIgnoreCase( req, "Version" );
//    String acceptVersionsParam = ServletUtil.getParameterIgnoreCase( req, "AcceptVersions" );

    // General request info
    thredds.wcs.v1_0_0_Plus.WcsRequest.Operation operation;
    GridDataset gridDataset = null;
    TdsRequestedDataset trd = new TdsRequestedDataset(req, "/wcs");
    try {
      gridDataset = trd.openAsGridDataset(req, res);
      if (gridDataset == null)
        return null;

      thredds.wcs.v1_0_0_Plus.WcsDataset wcsDataset = new thredds.wcs.v1_0_0_Plus.WcsDataset(gridDataset, trd.getPath());

      // Determine the request operation.
      String requestParam = ServletUtil.getParameterIgnoreCase(req, "Request");
      try {
        operation = thredds.wcs.v1_0_0_Plus.WcsRequest.Operation.valueOf(requestParam);
      } catch (IllegalArgumentException e) {
        throw new thredds.wcs.v1_0_0_Plus.WcsException(thredds.wcs.v1_0_0_Plus.WcsException.Code.InvalidParameterValue, "Request", "Unsupported operation request <" + requestParam + ">.");
      }

      // Handle "GetCapabilities" request.
      if (operation.equals(thredds.wcs.v1_0_0_Plus.WcsRequest.Operation.GetCapabilities)) {
        String sectionParam = ServletUtil.getParameterIgnoreCase(req, "Section");
        String updateSequenceParam = ServletUtil.getParameterIgnoreCase(req, "UpdateSequence");

        if (sectionParam == null)
          sectionParam = "";
        thredds.wcs.v1_0_0_Plus.GetCapabilities.Section section = null;
        try {
          section = thredds.wcs.v1_0_0_Plus.GetCapabilities.Section.getSection(sectionParam);
        } catch (IllegalArgumentException e) {
          throw new thredds.wcs.v1_0_0_Plus.WcsException(thredds.wcs.v1_0_0_Plus.WcsException.Code.InvalidParameterValue, "Section", "Unsupported GetCapabilities section requested <" + sectionParam + ">.");
        }

        return new thredds.wcs.v1_0_0_Plus.GetCapabilities(operation, version, wcsDataset, serverURI, section, updateSequenceParam, null);
      }
      // Handle "DescribeCoverage" request.
      else if (operation.equals(thredds.wcs.v1_0_0_Plus.WcsRequest.Operation.DescribeCoverage)) {
        String coverageIdListParam = ServletUtil.getParameterIgnoreCase(req, "Coverage");
        List<String> coverageIdList = splitCommaSeperatedList(coverageIdListParam);

        return new thredds.wcs.v1_0_0_Plus.DescribeCoverage(operation, version, wcsDataset, coverageIdList);
      }
      // Handle "GetCoverage" request.
      else if (operation.equals(thredds.wcs.v1_0_0_Plus.WcsRequest.Operation.GetCoverage)) {
        String coverageId = ServletUtil.getParameterIgnoreCase(req, "Coverage");
        String crs = ServletUtil.getParameterIgnoreCase(req, "CRS");
        String responseCRS = ServletUtil.getParameterIgnoreCase(req, "RESPONSE_CRS");
        String bbox = ServletUtil.getParameterIgnoreCase(req, "BBOX");
        String time = ServletUtil.getParameterIgnoreCase(req, "TIME");
        // ToDo The name of this parameter is dependent on the coverage (see WcsCoverage.getRangeSetAxisName()).
        String rangeSubset = ServletUtil.getParameterIgnoreCase(req, "RangeSubset");
        String format = ServletUtil.getParameterIgnoreCase(req, "FORMAT");

        return new thredds.wcs.v1_0_0_Plus.GetCoverage(operation, version, wcsDataset, coverageId,
                crs, responseCRS, bbox, time, rangeSubset, format);
      } else
        throw new thredds.wcs.v1_0_0_Plus.WcsException(thredds.wcs.v1_0_0_Plus.WcsException.Code.InvalidParameterValue, "Request", "Invalid requested operation <" + requestParam + ">.");

    } catch (Throwable e) {
      if (gridDataset != null)
        gridDataset.close();
      throw new RuntimeException(e);
    }
  }

  private static List<String> splitCommaSeperatedList(String identifiers) {
    List<String> idList = new ArrayList<>();
    String[] idArray = identifiers.split(",");
    for (String anIdArray : idArray) {
      idList.add(anIdArray.trim());
    }
    return idList;
  }

  /* private static GridDataset openDataset( HttpServletRequest req, HttpServletResponse res )
          throws thredds.wcs.v1_0_0_Plus.WcsException
  {
    String datasetURL = ServletUtil.getParameterIgnoreCase( req, "dataset" );
    boolean isRemote = ( datasetURL != null );
    String datasetPath = isRemote ? datasetURL : TdsPathUtils.extractPath(req, "wcs/");

    GridDataset dataset;
    try
    {
      dataset = isRemote ? ucar.nc2.dt.grid.GridDataset.open( datasetPath ) : TdsRequestedDataset.openGridDataset(req, res, datasetPath);
    }
    catch ( IOException e )
    {
      log.warn( "WcsRequestParser(): Failed to open dataset <" + datasetPath + ">: " + e.getMessage() );
      throw new thredds.wcs.v1_0_0_Plus.WcsException( "Failed to open dataset, \"" + datasetPath + "\"." );
    }
    if ( dataset == null )
    {
      log.debug( "WcsRequestParser(): Unknown dataset <" + datasetPath + ">." );
      throw new thredds.wcs.v1_0_0_Plus.WcsException( "Unknown dataset, \"" + datasetPath + "\"." );
    }
    return dataset;
  }  */
}
