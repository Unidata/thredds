package thredds.servlet.filter;

import thredds.server.catalogservice.Command;

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
 *   <li>"command" - single-value, one of the following string values
 *     (case-insensitive): "SHOW", "SUBSET", "VALIDATE"
 *     (see {@link thredds.server.catalogservice.Command})</li>
 *   <li>"dataset" - single-value, path string</li>
 *   <li>"htmlView" - single-value, boolean string</li>
 *   <li>"verbose" - single-value, boolean string</li>
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
 * @see thredds.util.StringValidateEncodeUtils
 * @see thredds.server.catalogservice.Command
 * @see thredds.server.catalogservice.LocalCatalogServiceController
 * @see thredds.server.catalogservice.RemoteCatalogServiceController
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

    String[] cmdValidSet = { Command.SHOW.toString(), Command.SUBSET.toString(), Command.VALIDATE.toString()};

    if ( ParameterValidationUtils.validateParameterAsSingleValueUriString( request, response, "catalog" )
         && ParameterValidationUtils.validateParameterAsSingleValueAlphanumericStringConstrained( request, response, "command", cmdValidSet, true )
         && ParameterValidationUtils.validateParameterAsSingleValuePathString( request, response, "dataset" )
         && ParameterValidationUtils.validateParameterAsSingleValueBooleanString( request, response, "htmlView" )
         && ParameterValidationUtils.validateParameterAsSingleValueBooleanString( request, response, "verbose" ) )
    {
      filterChain.doFilter( servletRequest, servletResponse );
    }

    return;
  }
}