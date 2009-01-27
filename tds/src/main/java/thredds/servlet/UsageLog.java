/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.servlet;

import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for gathering context information for use in log messages.
 * Includes methods appropriate when the context thread is an individual
 * HTTP request and when the context thread is an initialization thread.
 * The context information is contained in a key/value map.
 *
 * <p>Uses the SLF4J MDC framework (see @link{org.slf4j.MDC} for more details).
 *
 * <p>If properly configured, each log entry within the context thread
 * will include the gathered context information. For instance, in log4j
 * and slf4j, the appender pattern would contain strings with the form
 * "%X{&lt;contextKey&gt;}", where "&lt;contextKey&gt;" is a context key
 * value. The context key strings are given in each setup method below.
 *
 * @author caron
 * @since Jan 9, 2009
 * @see org.slf4j.MDC
 */
public class UsageLog {
  public static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( UsageLog.class);
  private static AtomicLong logServerAccessId = new AtomicLong(0);

  /**
   * Gather context information for the given HTTP request and return
   * a log message appropriate for logging at the start of the request.
   *
   * <p>The following context information is gathered:
   * <ul>
   * <li>"ID" - an identifier for the current thread;</li>
   * <li>"host" - the remote host (IP address or host name);</li>
   * <li>"userid" - the id of the remote user;</li>
   * <li>"startTime" - the system time in millis when this request is started (i.e., when this method is called); and</li>
   * <li>"request" - The HTTP request, e.g., "GET /index.html HTTP/1.1".</li>
   * </ul>
   *
   * <p>Call this method at the start of each HttpServlet doXXX() method
   * (e.g., doGet(), doPut()) or Spring MVC Controller handle() method.
   *
   * @param req the current request
   * @return a log message appropriate for the start of the request.
   */
  public static String setupRequestContext(HttpServletRequest req) {
     HttpSession session = req.getSession(false);

     // Setup context.
     MDC.put("ID", Long.toString( logServerAccessId.incrementAndGet() ));
     MDC.put("host", req.getRemoteHost());
     MDC.put("ident", (session == null) ? "-" : session.getId());
     MDC.put("userid", req.getRemoteUser() != null ? req.getRemoteUser() : "-");
     MDC.put("startTime", Long.toString(System.currentTimeMillis()));
     String query = req.getQueryString();
     query = (query != null) ? "?" + query : "";
     StringBuffer request = new StringBuffer();
     request.append("\"").append(req.getMethod()).append(" ")
         .append(req.getRequestURI()).append(query)
         .append(" ").append(req.getProtocol()).append("\"");

     MDC.put("request", request.toString());
     return "Remote host: " + req.getRemoteHost() + " - Request: " + request.toString();
   }

   /**
    * Return a log message appropriate for logging at the completion of
    * the contexts HTTP request.
    *
    * <p>Call this method at every exit point in each HttpServlet doXXX() method
    * (e.g., doGet(), doPut()) or Spring MVC Controller handle() method.
    *
    * @param resCode        - the result code for this request.
    * @param resSizeInBytes - the number of bytes returned in this result, -1 if unknown.
    */
   public static String closingMessageForRequestContext(int resCode, long resSizeInBytes) {
     long duration = calculateElapsedTime();

     return "Request Completed - " + resCode + " - " + resSizeInBytes + " - " + duration;
   }

  /**
   * Gather context information for the current non-request thread and
   * return a log message appropriate for logging.
   *
   * <p>The following context information is gathered:
   * <ul>
   * <li>"ID" - an identifier for the current thread; and</li>
   * <li>"startTime" - the system time in millis when this method is called.</li>
   * </ul>
   *
   * <p>Call this method only for non-request servlet activities, e.g.,
   * during init() or destroy().
   */
   public static String setupNonRequestContext() {
     // Setup context.
     MDC.put("ID", Long.toString( logServerAccessId.incrementAndGet() ));
     MDC.put("startTime", Long.toString(System.currentTimeMillis()));

    return "Non-request thread opening.";
   }

  /**
   * Return a log message appropriate for logging at the close of
   * the non-request context.
   *
   * @return a log message appropriate for logging at the close of the non-request context.
   */
  public static String closingMessageNonRequestContext()
  {
    long duration = calculateElapsedTime();

    return "Non-request thread closing - " + duration;
  }

  private static long calculateElapsedTime()
  {
    long endTime = System.currentTimeMillis();
    long startTime = Long.parseLong( MDC.get( "startTime" ) );
    long duration = endTime - startTime;
    return duration;
  }
}
