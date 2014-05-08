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
public class CatalogGenFilter
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