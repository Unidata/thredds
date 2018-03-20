/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.servlet.filter;

import thredds.util.StringValidateEncodeUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * Reject any request with an invalid query string.
 * <p/>
 * <p/>
 * The query string is considered valid if, after decoding, it is
 * a single-line string. For more details, see
 * {@link thredds.util.StringValidateEncodeUtils#validSingleLineString(String)}  validSingleLineString()}.
 * <p/>
 * <p/>
 * <strong>Note:</strong>
 * Currently also rejecting strings that contain any less than ("<"),
 * greater than (">"), or backslash ("\") characters. [May loosen this
 * restriction later.]
 * <p/>
 * <p/>
 * <strong>Note:</strong>
 * HttpServletRequest.getQueryString()) is not decoded by default so we run it
 * through URLDecoder.decode().
 *
 * @author edavis
 * @see thredds.util.StringValidateEncodeUtils#validSingleLineString(String)
 * @see URLDecoder
 * @since 3.16.47
 */
public class RequestQueryFilter implements Filter {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  private boolean allowAngleBrackets = false;

  public void setAllowAngleBrackets(boolean allowAngleBrackets) throws ServletException {
      this.allowAngleBrackets = allowAngleBrackets;
  }

  public void destroy() {
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // do nothing
  }

  public void doFilter(ServletRequest servletRequest,
                       ServletResponse servletResponse,
                       FilterChain filterChain)
          throws IOException, ServletException {
    if (!(servletRequest instanceof HttpServletRequest)) {
      log.error("doFilter(): Not an HTTP request! How did this filter get here?");
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    String query = request.getQueryString();
    if (query != null) {
      //String decodedQuery = EscapeStrings.unescapeURLQuery(query);
      while (true) {
        String decodedQuery = URLDecoder.decode(query, StringValidateEncodeUtils.CHARACTER_ENCODING_UTF_8);
        boolean badQuery = false;
        if (!allowAngleBrackets && StringValidateEncodeUtils.containsAngleBracketCharacters(decodedQuery))
          badQuery = true;

        // else if (StringValidateEncodeUtils.containsBackslashCharacters(decodedQuery) || !StringValidateEncodeUtils.validSingleLineString(decodedQuery))
        else if (!StringValidateEncodeUtils.validSingleLineString(decodedQuery))
          badQuery = true;

        if (badQuery) {
          log.debug("doFilter(): Invalid query string [" + query + "].");
          String msg = "Invalid query string ";
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
          return;
        }

        if (query.equals(decodedQuery)) break;
        query = decodedQuery;
      } // while
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }
}