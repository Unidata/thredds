package thredds.servlet.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Reject any CatalogService request with invalid parameters.
 *
 * <p>
 * The following CatalogService request parameters are validated as described:
 * <ul>
 *   <li>"catalog" - single-value, URI string</li>
 *   <li>"dataset" - single-value, path string</li>
 *   <li>"cmd" - single-value, one of the following string values: "show", "subset", "validate", "convert"</li>
 *   <li>"debug" - single-value, boolean string</li>
 * </ul>
 *
 * <p>The validation types use the following methods:
 * <ul>
 *   <li>single-value - HttpServletRequest.getParameterValues() must return an array of length one.</li>
 *   <li>URI string - {@link thredds.util.StringValidateEncodeUtils#validUriString(String)}  validUriString()} returns true
 *   <li>path string - {@link thredds.util.StringValidateEncodeUtils#validPath(String)}  validPath()} returns true
 *   <li>boolean string - {@link thredds.util.StringValidateEncodeUtils#validBooleanString(String)}  validBooleanString()} returns true
 * </ul>
 *
 * @author edavis
 * @since 3.16.47
 */
public class CatalogServiceUserInputFilter
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

    String[] cmdValidSet = {"show", "subset", "validate", "convert"};

    if ( ParameterValidationUtils.validateParameterAsSingleValueUriString( request, response, "catalog" )
         && ParameterValidationUtils.validateParameterAsSingleValuePathString( request, response, "dataset" )
         && ParameterValidationUtils.validateParameterAsSingleValueAlphanumericStringConstrained( request, response, "cmd", cmdValidSet, true )
         && ParameterValidationUtils.validateParameterAsSingleValueBooleanString( request, response, "debug" ) )
    {
      filterChain.doFilter( servletRequest, servletResponse );
    }

    return;
  }
}