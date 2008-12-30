package thredds.servlet.filter;

import thredds.util.StringValidateEncodeUtils;
import thredds.servlet.ServletUtil;

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
        ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, msg.length() );
        response.sendError( HttpServletResponse.SC_NOT_FOUND, msg );
        return;
      }
    }

    filterChain.doFilter( servletRequest, servletResponse );
    return;
  }
}