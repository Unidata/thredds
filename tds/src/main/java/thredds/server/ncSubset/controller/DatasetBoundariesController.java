/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncSubset.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import thredds.server.ncSubset.dataservice.FeatureDatasetService;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.params.NcssParamsBean;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.gis.GridBoundariesExtractor;
import ucar.nc2.ft.FeatureDataset;


/**
 * @author mhermida
 */
@Controller
@Scope("request")
@RequestMapping(value = "/ncss/**")
public class DatasetBoundariesController extends AbstractFeatureDatasetController {

  static private final Logger log = LoggerFactory.getLogger(DatasetBoundariesController.class);

  @Autowired
  FeatureDatasetService datasetService;

  @RequestMapping(value = {"datasetBoundaries"})
  void getDatasetBoundaries(NcssParamsBean params, HttpServletRequest req, HttpServletResponse res) throws IOException, UnsupportedResponseFormatException {

    //Checking request format...
    SupportedFormat sf = getSupportedFormat(params, SupportedOperation.DATASET_BOUNDARIES_REQUEST);

    String pathInfo = extractRequestPathInfo(req.getServletPath());
    FeatureDataset fd = null;
    try {
      fd = datasetService.findDatasetByPath(req, res, pathInfo);

      if (fd == null)
        throw new UnsupportedOperationException("Feature Type not supported");
      if (fd.getFeatureType() != FeatureType.GRID)
        throw new UnsupportedOperationException("Dataset Boundaries request is only supported on Grid features");

      String boundaries = getBoundaries(sf, (GridDataset) fd);

      res.setContentType(sf.getResponseContentType());
      res.getWriter().write(boundaries);
      res.getWriter().flush();
    } finally {
      if (fd != null) fd.close();
    }
  }

  private String getBoundaries(SupportedFormat format, GridDataset gridDataset) {

    String boundaries = "";
    GridBoundariesExtractor gbe = GridBoundariesExtractor.valueOf(gridDataset);

    if (format == SupportedFormat.WKT)
      boundaries = gbe.getDatasetBoundariesWKT();
    if (format == SupportedFormat.JSON)
      boundaries = gbe.getDatasetBoundariesGeoJSON();

    return boundaries;
  }

  String extractRequestPathInfo(String requestPathInfo) {

    requestPathInfo = requestPathInfo.substring(servletPath.length(), requestPathInfo.length());
    if (requestPathInfo.endsWith("datasetBoundaries")) {
      requestPathInfo = requestPathInfo.trim();
      String[] pathInfoArr = requestPathInfo.split("/");
      StringBuilder sb = new StringBuilder();
      int len = pathInfoArr.length;
      sb.append(pathInfoArr[1]);
      for (int i = 2; i < len - 1; i++) {
        sb.append("/" + pathInfoArr[i]);
      }
      requestPathInfo = sb.toString();
    }

    return requestPathInfo;
  }

  protected SupportedFormat getSupportedFormat(NcssParamsBean params, SupportedOperation operation) throws UnsupportedResponseFormatException {

    //Checking request format...
    SupportedFormat sf;
    if (params.getAccept() == null) {
      //setting the default format
      sf = operation.getDefaultFormat();
      params.setAccept(sf.getFormatName());
    } else {
      sf = SupportedOperation.isSupportedFormat(params.getAccept(), operation);
      if (sf == null) {
        throw new UnsupportedResponseFormatException("Requested format: " + params.getAccept() + " is not supported for " + operation.getOperation().toLowerCase());
      }
    }

    return sf;
  }

}
