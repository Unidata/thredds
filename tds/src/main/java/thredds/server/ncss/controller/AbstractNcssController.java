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
package thredds.server.ncss.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import thredds.server.ncss.exception.NcssException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author mhermida
 */
public class AbstractNcssController {
    protected static final String servletPath = "/ncss/";
    protected static final String servletPathGrid = "/ncss/grid/";

    protected static final String servletCachePath = "/cache/ncss";

    static private final Logger logger = LoggerFactory.getLogger(AbstractNcssController.class);

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

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<String> handle(FileNotFoundException e) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_PLAIN);

        return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handle(UnsupportedOperationException e) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_PLAIN);

        return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<String> handle(Throwable t) {
        logger.error("Uncaught exception", t);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_PLAIN);

        return new ResponseEntity<>(t.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
    }

  /*

    @ResponseStatus(value=HttpStatus.BAD_REQUEST)
    @ExceptionHandler(NcssException.class)
    public void handle(NcssException e) {
     logger.debug("NcssException", e);
    }

    @ResponseStatus(value=HttpStatus.NOT_FOUND, reason="Unknown Dataset")
    @ExceptionHandler(FileNotFoundException.class)
    public void handle(FileNotFoundException e) {
      logger.debug("Not Found", e);
    }

    @ResponseStatus(value=HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UnsupportedOperationException.class)
    public void handle(UnsupportedOperationException e) {
      logger.debug("UnsupportedOperationException", e);
    }

    @ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    public void handle(Throwable t) {
      logger.error("Uncaught exception", t);
    }   */

    public static String getNCSSServletPath() {
        return servletPath;
    }

    public static String getServletCachePath() {
        return servletCachePath;
    }

    private static final String[] endings = new String[]{"/dataset.xml", "/dataset.html", "/pointDataset.html",
            "/pointDataset.xml", "/datasetBoundaries.xml", "/station.xml"
    };

    public static String getDatasetPath(HttpServletRequest req) {
        return getDatasetPath(req.getServletPath());
    }

    public static String  getDatasetPath(String path) {
      if (path.startsWith(NcssController.servletPathGrid)) {               // strip off /ncss/grid/
          path = path.substring(NcssController.servletPathGrid.length());

      }  else if (path.startsWith(NcssController.servletPath)) {               // strip off /ncss/
          path = path.substring(NcssController.servletPath.length());
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

  /* String extractRequestPathInfo(String requestPathInfo) {
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
  } */
    /* String[] servletPathTokens = servletPath.split("/");
    String lastToken = servletPathTokens[servletPathTokens.length - 1];
    if (lastToken.endsWith(".html") || lastToken.endsWith(".xml")) {
      servletPath = servletPath.substring(0, servletPath.length() - lastToken.length() - 1);
    }

    return servletPath.substring(
            FeatureDatasetController.servletPath.length(),
            servletPath.length());  */

    /*
      private String getDatasetPath(HttpServletRequest req) {

    String servletPath = req.getServletPath();

    return servletPath.substring(
            FeatureDatasetController.servletPath.length(),
            servletPath.length());
  }
     */
}
