/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.servlet.filter;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * @author Jayson Falkner
 * @see <a href="http://www.onjava.com/pub/a/onjava/2004/03/03/filters.html">http://www.onjava.com/pub/a/onjava/2004/03/03/filters.html</a>
 */

/*  cache for 10 days:

  <filter>
    <filter-name>Cache10dayFilter</filter-name>
    <filter-class>thredds.servlet.ResponseHeaderFilter</filter-class>
    <init-param>
      <param-name>Cache-Control</param-name>
      <param-value>max-age=864000</param-value>
    </init-param>
  </filter>

  <filter-mapping>
    <filter-name>Cache10dayFilter</filter-name>
    <url-pattern>/folder.gif</url-pattern>
  </filter-mapping>

  <filter-mapping>
    <filter-name>Cache10dayFilter</filter-name>
    <url-pattern>/thredds.jpg</url-pattern>
  </filter-mapping>

 do not cache:
 
<filter>
  <filter-name>NoCacheFilter</filter-name>
   <filter-class>thredds.servlet.ResponseHeaderFilter</filter-class>
  <init-param>
    <param-name>Cache-Control</param-name>
    <param-value>private,no-cache,no-store</param-value>
   </init-param>
</filter>

 */
  
public class ResponseHeaderFilter implements Filter {
  private FilterConfig fc;

  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                       throws IOException, ServletException {
    HttpServletResponse response = (HttpServletResponse) res;

    // set the provided HTTP response parameters
    for (Enumeration e=fc.getInitParameterNames();e.hasMoreElements();) {
      String headerName = (String)e.nextElement();
      response.addHeader(headerName, fc.getInitParameter(headerName));
    }

    // pass the request/response on
    chain.doFilter(req, response);
  }

  public void init(FilterConfig filterConfig) {
    this.fc = filterConfig;
  }

  public void destroy() {
    this.fc = null;
  }
}
