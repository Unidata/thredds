/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.util;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Wrapper around RequestDispatcher
 *
 * @author edavis
 * @since 4.0
 */
public class RequestForwardUtils {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RequestForwardUtils.class);

  private RequestForwardUtils() {
  }

  public static void forwardRequestRelativeToCurrentContext(String fwdPath, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException {

    if (fwdPath == null || request == null || response == null) {
      String msg = "Path, request, and response may not be null";
      log.error("forwardRequestRelativeToCurrentContext() ERROR: " + msg + (fwdPath == null ? ": " : "[" + fwdPath + "]: "));
      throw new IllegalArgumentException(msg + ".");
    }

    Escaper urlPathEscaper = UrlEscapers.urlPathSegmentEscaper();

    String encodedPath = urlPathEscaper.escape(fwdPath); // LOOK path vs query
    RequestDispatcher dispatcher = request.getRequestDispatcher(encodedPath);

    if (dispatcherWasFound(encodedPath, dispatcher, response))
      dispatcher.forward(request, response);
  }

  public static void forwardRequestRelativeToGivenContext(String fwdPath,
                                                          ServletContext targetContext,
                                                          HttpServletRequest request,
                                                          HttpServletResponse response)
          throws IOException, ServletException {
    if (fwdPath == null || targetContext == null || request == null || response == null) {
      String msg = "Path, context, request, and response may not be null";
      log.error("forwardRequestRelativeToGivenContext() ERROR: " + msg + (fwdPath == null ? ": " : "[" + fwdPath + "]: "));
      throw new IllegalArgumentException(msg + ".");
    }

    Escaper urlPathEscaper = UrlEscapers.urlPathSegmentEscaper();
    String encodedPath = urlPathEscaper.escape(fwdPath); // LOOK path vs query
    RequestDispatcher dispatcher = targetContext.getRequestDispatcher(encodedPath);

    if (dispatcherWasFound(encodedPath, dispatcher, response))
      dispatcher.forward(request, response);
  }

  public static void forwardRequest(String fwdPath, RequestDispatcher dispatcher, HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {

    if (fwdPath == null || dispatcher == null || request == null || response == null) {
      String msg = "Path, dispatcher, request, and response may not be null";
      log.error("forwardRequestRelativeToGivenContext() ERROR: " + msg + (fwdPath == null ? ": " : "[" + fwdPath + "]: "));
      throw new IllegalArgumentException(msg + ".");
    }

    dispatcher.forward(request, response);
  }

  private static boolean dispatcherWasFound(String fwdPath, RequestDispatcher dispatcher, HttpServletResponse response)
          throws IOException {

    if (dispatcher == null) {
      log.error("dispatcherWasFound() ERROR : Dispatcher for forwarding [" + fwdPath + "] not found:");
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return false;
    }
    return true;
  }
}
