/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.servlet.filter;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * This class will modify the session cookie path, if a session attribute named SESSION_PATH exists.
 * Appears to be part of an early (2009) attempt to use sessions in opendap to ensure that datasets dont change during a session.
 * That functionality is turned off, so assume we dont need this.
 *
 * @author caron
 * @since Jun 23, 2009
 */
public class CookieFilter implements Filter {

  public static final String JSESSIONID = "JSESSIONID";
  public static final String SESSION_PATH = "SESSION_PATH";

  public void init(FilterConfig config) throws ServletException {
  }

  public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain chain) throws IOException, ServletException {
    chain.doFilter(_request, _response);

    // examine response after the request is processed
    if (_response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) _request;
      HttpServletResponse httpResponse = (HttpServletResponse) _response;

      HttpSession session = httpRequest.getSession(false);
      if ((session != null) && (session.getId() != null) && (session.getAttribute(SESSION_PATH) != null)) {
        Cookie sessionCookie = new Cookie(JSESSIONID, session.getId());
        sessionCookie.setPath((String) session.getAttribute(SESSION_PATH));
        httpResponse.addCookie(sessionCookie);
      }
    }
  }

  public void destroy() {
  }
}
