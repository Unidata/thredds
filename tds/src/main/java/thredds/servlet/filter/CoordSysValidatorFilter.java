package thredds.servlet.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Reject any CoordSysValidator request with invalid parameters.
 *
 * <p>
 * The following request parameters are validated as described:
 * <ul>
 *   <li>GET
 *     <ul>
 *       <li>"URL" - single-value, URI string</li>
 *       <li>"xml" - single-value, boolean string</li>
 *     </ul>
 *   </li>
 *   <li>POST
 *     <ul>
 *       <li>??? lots of file upload stuff ???
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author edavis
 * @since 3.16.47
 * @see thredds.util.StringValidateEncodeUtils
 */
public class CoordSysValidatorFilter
        implements Filter
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  public void init( FilterConfig filterConfig ) throws ServletException
  {
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

    if ( request.getMethod().equals( "GET" ))
    {
      if ( ParameterValidationUtils.validateParameterAsSingleValueUriString( request, response, "URL" )
           && ParameterValidationUtils.validateParameterAsSingleValueBooleanString( request, response, "xml" ) )
      {
        filterChain.doFilter( servletRequest, servletResponse );
      }
      return;
    }
    else if ( request.getMethod().equals( "POST" ))
    {
      // LOOK ToDo whatever validation is needed for POST. 
      filterChain.doFilter( servletRequest, servletResponse );
      return;
    }

    filterChain.doFilter( servletRequest, servletResponse );
    return;
  }
}