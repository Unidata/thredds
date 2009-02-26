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

import thredds.util.StringValidateEncodeUtils;
import thredds.servlet.ServletUtil;
import thredds.servlet.UsageLog;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * Reject any request with an invalid query string.
 *
 * <p>
 * The query string is considered valid if, after decoding, it is
 * a single-line string. For more details, see
 * {@link thredds.util.StringValidateEncodeUtils#validSingleLineString(String)}  validSingleLineString()}.
 *
 * <p>
 * <strong>Note:</strong>
 * Currently also rejecting strings that contain any less than ("<"),
 * greater than (">"), or backslash ("\") characters. [May loosen this
 * restriction later.]
 *
 * <p>
 * <strong>Note:</strong>
 * HttpServletRequest.getQueryString()) is not decoded by default so we run it
 * through URLDecoder.decode().
 *
 * @author edavis
 * @since 3.16.47
 * @see thredds.util.StringValidateEncodeUtils#validSingleLineString(String)
 * @see URLDecoder
 */
public class RequestQueryFilter
        implements Filter
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  private boolean allowAngleBrackets = false;

  public void init( FilterConfig filterConfig ) throws ServletException
  {
    String s = filterConfig.getInitParameter( "allowAngleBrackets" );
    if ( s != null && s.equalsIgnoreCase( "true" ))
      allowAngleBrackets = true;
  }

  public void destroy()
  {
  }

  public void doFilter( ServletRequest servletRequest,
                        ServletResponse servletResponse,
                        FilterChain filterChain )
          throws IOException, ServletException
  {
    if ( ! ( servletRequest instanceof HttpServletRequest ) )
    {
      log.error( "doFilter(): Not an HTTP request! How did this filter get here?" );
      filterChain.doFilter( servletRequest, servletResponse );
      return;
    }
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    String query = request.getQueryString();
    if ( query != null )
    {
      String decodedQuery = URLDecoder.decode( query, StringValidateEncodeUtils.CHARACTER_ENCODING_UTF_8 );
      boolean badQuery = false;
      if ( ! allowAngleBrackets
             && StringValidateEncodeUtils.containsAngleBracketCharacters( decodedQuery ) )
        badQuery = true;
      else if ( StringValidateEncodeUtils.containsBackslashCharacters( decodedQuery )
                || !StringValidateEncodeUtils.validSingleLineString( decodedQuery ))
        badQuery = true;

      if ( badQuery )
      {
        String msg = "Invalid query string [" + query + "].";
        log.error( "doFilter(): " + msg );
        log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, msg.length() ));
        response.sendError( HttpServletResponse.SC_NOT_FOUND, msg );
        return;
      }
    }

    filterChain.doFilter( servletRequest, servletResponse );
    return;
  }
}