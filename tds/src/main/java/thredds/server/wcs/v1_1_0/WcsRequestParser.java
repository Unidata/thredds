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
package thredds.server.wcs.v1_1_0;

import thredds.core.TdsRequestedDataset;
import thredds.servlet.ServletUtil;
import thredds.util.TdsPathUtils;
import thredds.wcs.v1_1_0.WcsException;
import thredds.wcs.v1_1_0.GetCapabilities;
import thredds.wcs.v1_1_0.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

import ucar.nc2.dt.GridDataset;

/**
 * Represent the incoming WCS 1.1.0 request.
 *
 * @author edavis
 * @since 4.0
 */
public class WcsRequestParser {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WcsRequestParser.class);

  public static Request parseRequest(String version, HttpServletRequest req, HttpServletResponse res)
          throws WcsException, IOException {
    // These are handled in WcsServlet. Don't need to validate here.
//    String serviceParam = ServletUtil.getParameterIgnoreCase( req, "Service" );
//    String versionParam = ServletUtil.getParameterIgnoreCase( req, "Version" );
//    String acceptVersionsParam = ServletUtil.getParameterIgnoreCase( req, "AcceptVersions" );

    // General request info
    Request request; // The Request object to be built and returned.
    Request.Operation operation;
    String datasetPath = TdsPathUtils.extractPath(req, "wcs/");
    GridDataset dataset = null;

    try {
      dataset = TdsRequestedDataset.getGridDataset(req, res, null);
      if (dataset == null) return null;

      // GetCapabilities request info
      List<GetCapabilities.Section> sections;

      // DescribeCoverage request info

      // GetCoverage request info

      // Determine the request operation.
      String requestParam = ServletUtil.getParameterIgnoreCase(req, "Request");
      try {
        operation = Request.Operation.valueOf(requestParam);
      } catch (IllegalArgumentException e) {
        throw new WcsException(WcsException.Code.OperationNotSupported, requestParam, "");
      }

      // Handle "GetCapabilities" request.
      if (operation.equals(Request.Operation.GetCapabilities)) {
        String sectionsParam = ServletUtil.getParameterIgnoreCase(req, "Sections");
//      String updateSequenceParam = ServletUtil.getParameterIgnoreCase( req, "UpdateSequence" );
//      String acceptFormatsParam = ServletUtil.getParameterIgnoreCase( req, "AcceptFormats" );

        if (sectionsParam != null) {
          String[] sectionArray = sectionsParam.split(",");
          sections = new ArrayList<>(sectionArray.length);
          for (String curSection : sectionArray) {
            sections.add(GetCapabilities.Section.valueOf(curSection));
          }
        } else
          sections = Collections.emptyList();

        request = Request.getGetCapabilitiesRequest(operation, version, sections, datasetPath, dataset);
      }
      // Handle "DescribeCoverage" request.
      else if (operation.equals(Request.Operation.DescribeCoverage)) {
        // The "Identifier" parameter is KVP encoded as "Identifiers", handle both.
        String identifiers = ServletUtil.getParameterIgnoreCase(req, "Identifiers");
        if (identifiers == null)
          identifiers = ServletUtil.getParameterIgnoreCase(req, "Identifier");
        List<String> idList = splitCommaSeperatedList(identifiers);

        request = Request.getDescribeCoverageRequest(operation, version, idList, datasetPath, dataset);
      }
      // Handle "GetCoverage" request.
      else if (operation.equals(Request.Operation.GetCoverage)) {
        String identifier = ServletUtil.getParameterIgnoreCase(req, "Identifier");

        request = Request.getGetCoverageRequest(operation, version, identifier, datasetPath, dataset);
      } else
        throw new WcsException(WcsException.Code.OperationNotSupported, requestParam, "");

      return request;
    } catch (Throwable e) {
      if (dataset != null)
        dataset
                .close();
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

}
