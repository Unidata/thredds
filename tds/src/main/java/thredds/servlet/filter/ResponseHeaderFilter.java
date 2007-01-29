// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package thredds.servlet.filter;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * @author Jayson Falkner
 * @see "http://www.onjava.com/pub/a/onjava/2004/03/03/filters.html"
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
