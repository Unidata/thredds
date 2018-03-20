/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.ncss.controller;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.core.StandardService;
import thredds.server.config.TdsContext;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import thredds.util.TdsPathUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
  NcssDiskCache ncssDiskCache;

  //////////////////////////////////////////////////////////////////////////
  // common methods

  private static final String[] endings = new String[] {
          "/dataset.xml", "/dataset.html",
          "/pointDataset.html", "/pointDataset.xml",
          "/datasetBoundaries.xml", "/datasetBoundaries.wkt", "/datasetBoundaries.json",
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

  public static Element makeAcceptXML(SupportedOperation ops) {
    Element acceptList = new Element("AcceptList");
    for (SupportedFormat sf : ops.getSupportedFormats()) {
      Element accept =
              new Element("accept").addContent(sf.getFormatName()).setAttribute("displayName", sf.getFormatName());
      acceptList.addContent(accept);
    }

    return acceptList;
  }

  public static List<String> makeAcceptList(SupportedOperation ops) {
    List<String> result = new ArrayList<>();
    for (SupportedFormat sf : ops.getSupportedFormats()) {
      result.add(sf.getFormatName());
    }

    return result;
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

    } else if (path.startsWith(StandardService.netcdfSubsetPoint.getBase())) {       // strip off /ncss/point/
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
