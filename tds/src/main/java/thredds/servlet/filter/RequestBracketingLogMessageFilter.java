/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.servlet.filter;

import org.slf4j.MDC;
import thredds.servlet.UsageLog;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Wraps a request with the UsageLog.setup and UsageLog.closing log messages
 *
 * @author edavis
 * @since 4.1
 */
public class RequestBracketingLogMessageFilter implements javax.servlet.Filter {

  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("threddsServlet");

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
    TdsServletResponseWrapper response = new TdsServletResponseWrapper((HttpServletResponse) servletResponse);

    //request.getServletPath();

    // Just checking
    if (response.isCommitted())
      log.error("doFilter(): Yikes! Response is already committed (Heiko's bug?).");

    // Initial setup
    log.info(UsageLog.setupRequestContext(request));

    filterChain.doFilter(request, response);

    log.info(UsageLog.closingMessageForRequestContext(response.getHttpStatusCode(), response.getHttpResponseBodyLength()));
    MDC.clear();
  }

}
