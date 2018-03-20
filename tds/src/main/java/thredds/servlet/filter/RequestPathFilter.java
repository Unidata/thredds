/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.servlet.filter;

import com.coverity.security.Escape;
import thredds.servlet.ServletUtil;
import thredds.util.StringValidateEncodeUtils;
import thredds.util.TdsPathUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Reject any request with an invalid path (i.e., HttpServletRequest.getPathInfo()).
 * <p/>
 * <p/>
 * The decoded request path must be a single-line string without any
 * parent path segments ("../"). For more details, see
 * {@link thredds.util.StringValidateEncodeUtils#validPath(String) validPath()}.
 * <p/>
 * <p/>
 * <strong>Note:</strong>
 * Currently also rejecting strings that contain any less than ("<"),
 * greater than (">"), or backslash ("\") characters. [May loosen this
 * restriction later.]
 *
 * @author edavis
 * @see thredds.util.StringValidateEncodeUtils#validPath(String)
 * @since 3.16.47
 */
public class RequestPathFilter implements javax.servlet.Filter {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

  public void init(FilterConfig filterConfig) throws ServletException {
  }

  public void destroy() {
  }

  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
          throws IOException, ServletException {

    if (!(servletRequest instanceof HttpServletRequest)) {
      log.error("doFilter(): Not an HTTP request! How did this filter get here?");
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    String path = TdsPathUtils.extractPath(request, null);
    if (path != null) {
      if (StringValidateEncodeUtils.containsAngleBracketCharacters(path)
//              || StringValidateEncodeUtils.containsBackslashCharacters(path)
              || !StringValidateEncodeUtils.validPath(path)) {

        String msg = "Invalid request path [" + Escape.html(ServletUtil.getRequestPath(request)) + "].";
        log.error("doFilter(): " + msg);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        return;
      }
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }
}
