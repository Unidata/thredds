/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.servlet;

import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import java.util.Formatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for gathering context information for use in log messages.
 * Includes methods appropriate when the context thread is an individual
 * HTTP request and when the context thread is an initialization thread.
 * The context information is contained in a key/value map.
 * <p/>
 * <p>Uses the SLF4J MDC framework (see @link{org.slf4j.MDC} for more details).
 * <p/>
 * <p>If properly configured, each log entry within the context thread
 * will include the gathered context information. For instance, in log4j
 * and slf4j, the appender pattern would contain strings with the form
 * "%X{&lt;contextKey&gt;}", where "&lt;contextKey&gt;" is a context key
 * value. The context key strings are given in each setup method below.
 *
 * @author caron
 * @see org.slf4j.MDC
 * @since Jan 9, 2009
 */
public class UsageLog {
  private static final AtomicLong logServerAccessId = new AtomicLong(0);

  /**
   * Gather context information for the given HTTP request and return
   * a log message appropriate for logging at the start of the request.
   * <p/>
   * <p>The following context information is gathered:
   * <ul>
   * <li>"ID" - an identifier for the current thread;</li>
   * <li>"host" - the remote host (IP address or host name);</li>
   * <li>"userid" - the id of the remote user;</li>
   * <li>"startTime" - the system time in millis when this request is started (i.e., when this method is called); and</li>
   * <li>"request" - The HTTP request, e.g., "GET /index.html HTTP/1.1".</li>
   * </ul>
   * <p/>
   * <p>Call this method at the start of each HttpServlet doXXX() method
   * (e.g., doGet(), doPut()) or Spring MVC Controller handle() method.
   *
   * @param req the current request
   * @return a log message appropriate for the start of the request.
   */
  public static String setupRequestContext(HttpServletRequest req) {

    // Setup context.
    //HttpSession session = req.getSession(false);
    /* MDC.put("host", req.getRemoteHost());
    MDC.put("ident", (session == null) ? "-" : session.getId());
    MDC.put("userid", req.getRemoteUser() != null ? req.getRemoteUser() : "-"); */

    MDC.put("ID", Long.toString(logServerAccessId.incrementAndGet()));
    MDC.put("startTime", Long.toString(System.currentTimeMillis()));

    String query = req.getQueryString();
    query = (query != null) ? "?" + query : "";
    Formatter request = new Formatter();
    request.format("\"%s %s%s %s\"", req.getMethod(), req.getRequestURI(), query, req.getProtocol());

    MDC.put("request", request.toString());
    return "Remote host: " + req.getRemoteHost() + " - Request: " + request.toString();
  }

  /**
   * Return a log message appropriate for logging at the completion of
   * the contexts HTTP request.
   * <p/>
   * <p>Call this method at every exit point in each HttpServlet doXXX() method
   * (e.g., doGet(), doPut()) or Spring MVC Controller handle() method.
   *
   * @param resCode        - the result code for this request.
   * @param resSizeInBytes - the number of bytes returned in this result, -1 if unknown.
   * @return closing log message
   */
  public static String closingMessageForRequestContext(int resCode, long resSizeInBytes) {
    long duration = calculateElapsedTime();

    return "Request Completed - " + resCode + " - " + resSizeInBytes + " - " + duration;
  }

  /**
   * Gather context information for the current non-request thread and
   * return a log message appropriate for logging.
   * <p/>
   * <p>The following context information is gathered:
   * <ul>
   * <li>"ID" - an identifier for the current thread; and</li>
   * <li>"startTime" - the system time in millis when this method is called.</li>
   * </ul>
   * <p/>
   * <p>Call this method only for non-request servlet activities, e.g.,
   * during init() or destroy().
   * @return starting log message
   */
  public static String setupNonRequestContext() {
    // Setup context.
    MDC.put("ID", Long.toString(logServerAccessId.incrementAndGet()));
    MDC.put("startTime", Long.toString(System.currentTimeMillis()));

    return "Non-request thread opening.";
  }

  /**
   * Return a log message appropriate for logging at the close of
   * the non-request context.
   *
   * @return a log message appropriate for logging at the close of the non-request context.
   */
  public static String closingMessageNonRequestContext() {
    long duration = calculateElapsedTime();

    return "Non-request thread closing - " + duration;
  }

  private static long calculateElapsedTime() {
    long endTime = System.currentTimeMillis();
    String startTimeS = MDC.get("startTime");
    if (startTimeS == null) return -1;
    long startTime = Long.parseLong(startTimeS);
    return endTime - startTime;
  }

  public void add2map(String key, String value) {
    MDC.put(key, value);
  }
}
