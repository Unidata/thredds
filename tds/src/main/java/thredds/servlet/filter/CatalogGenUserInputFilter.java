package thredds.servlet.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Reject any CatalogGen request with invalid parameters.
 *
 * <p>
 * The following CatalogGen request parameters are validated as described:
 * <ul>
 *   <li>"taskName" - single-value, ID string</li>
 *   <li>"fileName" - single-value, file path string</li>
 *   <li>"resultFileName" - single-value, file path string</li>
 *   <li>"period" - single-value, decimal number</li>
 *   <li>"delay" - single-value, decimal number</li>
 * </ul>
 *
 * <p>The validation types use the following methods:
 * <ul>
 *   <li>single-value - HttpServletRequest.getParameterValues() must return an array of length one.</li>
 *   <li>ID string - {@link thredds.util.StringValidateEncodeUtils#validIdString(String)}  validIdString()} returns true
 *   <li>file path string - {@link thredds.util.StringValidateEncodeUtils#validFilePath(String)}  validFilePath()} returns true
 *   <li>decimal number - {@link thredds.util.StringValidateEncodeUtils#validDecimalNumber(String)}  validDecimalNumber()} returns true
 * </ul>
 *
 * @author edavis
 * @since 3.16.47
 * @see thredds.util.StringValidateEncodeUtils#validIdString(String)
 * @see thredds.util.StringValidateEncodeUtils#validDecimalNumber(String)
 */
public class CatalogGenUserInputFilter
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

    if ( ParameterValidationUtils.validateParameterAsSingleValueIdString( request, response, "taskName" )
         && ParameterValidationUtils.validateParameterAsSingleValueFilePathString( request, response, "fileName" )
         && ParameterValidationUtils.validateParameterAsSingleValueFilePathString( request, response, "resultFileName" )
         && ParameterValidationUtils.validateParameterAsSingleValueDecimalNumber( request, response, "period" )
         && ParameterValidationUtils.validateParameterAsSingleValueDecimalNumber( request, response, "delay" ) )
    {
      filterChain.doFilter( servletRequest, servletResponse );
    }

    return;
  }
}