/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
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
package thredds.server.ncss.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.core.StandardService;
import thredds.server.config.TdsContext;
import thredds.server.ncss.exception.NcssException;
import thredds.util.TdsPathUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

/**
 * Superclass for ncss controllers
 *
 * @author jcaron
 * @author mhermida
 */
@Controller
@RequestMapping("/ncss")
public abstract class AbstractNcssController {
  static private final Logger logger = LoggerFactory.getLogger(AbstractNcssController.class);

  @Autowired
  TdsContext tdsContext;

  @Autowired
  NcssShowFeatureDatasetInfo ncssShowDatasetInfo;

  @Autowired
  NcssDiskCache ncssDiskCache;

    /* @RequestMapping("/ncss/grid/**")
  public String forwardGrid(HttpServletRequest req) {
    String reqString = req.getServletPath();
    assert reqString.startsWith("/ncss/grid");
    reqString = reqString.substring(10);
    String forwardString = "forward:/ncss" + reqString;  // strip off '?/grid
    if (null != req.getQueryString())
      forwardString += "?"+req.getQueryString();

     return forwardString;
  }  */

  /**
   * figure out how to deal with requests that dont have grid or point in them
   *
   * @ RequestMapping("**")
   * public void handleRequest(HttpServletRequest req, HttpServletResponse res,
   * @ Valid NcssParamsBean params,
   * BindingResult validationResult) throws Exception {
   * <p/>
   * // System.out.printf("%s%n", ServletUtil.showRequestDetail(null, req));
   * <p/>
   * if (validationResult.hasErrors()) {
   * handleValidationErrorsResponse(res, HttpServletResponse.SC_BAD_REQUEST, validationResult);
   * return;
   * }
   * <p/>
   * String datasetPath = getDatasetPath(req);
   * try (FeatureDataset fd = TdsRequestedDataset.getFeatureDataset(req, res, datasetPath)) {
   * if (fd == null) return;
   * <p/>
   * Formatter errs = new Formatter();
   * if (!params.intersectsTime(fd, errs)) {
   * handleValidationErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, errs.toString());
   * return;
   * }
   * <p/>
   * FeatureType ft = fd.getFeatureType();
   * if (ft == FeatureType.GRID) {
   * if (!params.hasLatLonPoint()) {
   * handleRequestGrid(res, params, datasetPath, (GridDataset) fd);
   * } else {
   * handleRequestGridAsPoint(res, params, datasetPath, fd);
   * }
   * } else if (ft == FeatureType.POINT) {
   * handleRequestDsg(res, params, datasetPath, fd);
   * } else if (ft == FeatureType.STATION) {
   * handleRequestDsg(res, params, datasetPath, fd);
   * } else {
   * throw new UnsupportedOperationException("Feature Type " + ft.toString() + " not supported");
   * }
   * <p/>
   * }
   * }
   */

  //////////////////////////////////////////////////////////////////////////
  // common methods

  private static final String[] endings = new String[]{"/dataset.xml", "/dataset.html", "/pointDataset.html",
          "/pointDataset.xml", "/datasetBoundaries.xml", "/datasetBoundaries.wkt", "/datasetBoundaries.json",
          "/station.xml"
  };

  public String getDatasetPath(HttpServletRequest req) {
    return TdsPathUtils.extractPath(req, getBase(), endings);
  }


  protected void setResponseHeaders(HttpServletResponse response, HttpHeaders httpHeaders) {
    Set<String> keySet = httpHeaders.keySet();
    for (String key : keySet) {
      if (httpHeaders.containsKey(key)) { // LOOK why test again?
        response.setHeader(key, httpHeaders.get(key).get(0));  // LOOK why only first one ?
      }
    }
  }

  abstract String getBase();

  protected String buildDatasetUrl(String path) {
    if (path.startsWith("/")) path = path.substring(1);
    return tdsContext.getContextPath() + getBase() + path;
  }

  protected void handleValidationErrorsResponse(HttpServletResponse response, int status,
                                                BindingResult validationResult) {

    List<ObjectError> errors = validationResult.getAllErrors();
    response.setStatus(status);
    // String responseStr="Validation errors: ";
    StringBuilder responseStr = new StringBuilder();
    responseStr.append("Validation errors: ");
    for (ObjectError err : errors) {
      responseStr.append(err.getDefaultMessage());
      responseStr.append("  -- ");
    }

    try {
      PrintWriter pw = response.getWriter();
      pw.write(responseStr.toString());
      pw.flush();

    } catch (IOException ioe) {
      logger.error(ioe.getMessage());
    }

  }

  protected void handleValidationErrorMessage(HttpServletResponse response, int status, String errorMessage) {
    response.setStatus(status);

    try {
      PrintWriter pw = response.getWriter();
      pw.write(errorMessage);
      pw.flush();

    } catch (IOException ioe) {
      logger.error(ioe.getMessage());
    }
  }

  ////////////////////////////////////////////////////////
  // Exception handlers
  @ExceptionHandler(NcssException.class)
  public ResponseEntity<String> handle(NcssException e) {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);

    return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST);
  }

  ///////////////////////////////////////////
  // unit testing

  public static String getDatasetPath(String path) {
    if (path.startsWith(StandardService.netcdfSubsetGrid.getBase())) {               // strip off /ncss/grid/
      path = path.substring(StandardService.netcdfSubsetGrid.getBase().length());

    } else if (path.startsWith(StandardService.netcdfSubsetPoint.getBase())) {               // strip off /ncss/point/
      path = path.substring(StandardService.netcdfSubsetPoint.getBase().length());
    }

    // strip off endings
    for (String ending : endings) {
      if (path.endsWith(ending)) {
        int len = path.length() - ending.length();
        path = path.substring(0, len);
        break;
      }
    }

    return path;
  }


}
