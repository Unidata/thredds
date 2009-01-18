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

import org.apache.log4j.MDC;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utilities for creating log info in the threddsServlet log, aka server usage log.
 *
 * @author caron
 * @since Jan 9, 2009
 */
public class UsageLog {
  public static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( UsageLog.class);
  private static AtomicLong logServerAccessId = new AtomicLong(0);

  public static String setupInfo(HttpServletRequest req) {
     HttpSession session = req.getSession(false);

     // Setup context.
     MDC.put("ID", Long.toString( logServerAccessId.incrementAndGet() ));
     MDC.put("host", req.getRemoteHost());
     MDC.put("ident", (session == null) ? "-" : session.getId());
     MDC.put("userid", req.getRemoteUser() != null ? req.getRemoteUser() : "-");
     MDC.put("startTime", System.currentTimeMillis());
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
    * Write log entry to THREDDS access log.
    *
    * @param resCode        - the result code for this request.
    * @param resSizeInBytes - the number of bytes returned in this result, -1 if unknown.
    */
   public static String accessInfo(int resCode, long resSizeInBytes) {
     long endTime = System.currentTimeMillis();
     long startTime = (Long) MDC.get("startTime");
     long duration = endTime - startTime;

     return "Request Completed - " + resCode + " - " + resSizeInBytes + " - " + duration;
   }

  /**
    * Gather current thread information for inclusion in regular logging
    * messages. Call this method only for non-request servlet activities, e.g.,
    * during the init() or destroy().
    * <p/>
    * Use the SLF4J API to log a regular logging messages.
    * <p/>
    * This method gathers the following information:
    * 1) "ID" - an identifier for the current thread; and
    * 2) "startTime" - the system time in millis when this method is called.
    * <p/>
    * The appearance of the regular log messages are controlled in the
    * log4j.xml configuration file.
    */
   public static void logServerSetup() {
     // Setup context.
     MDC.put("ID", Long.toString( logServerAccessId.incrementAndGet() ));
     MDC.put("startTime", System.currentTimeMillis());
   }
}
