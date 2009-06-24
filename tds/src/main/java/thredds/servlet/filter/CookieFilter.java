/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * This class will modify the session cookie path, if a session attribute named SESSION_PATH exists
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
