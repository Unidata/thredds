package thredds.server;

import thredds.server.TdsServletResponseWrapper;
import thredds.servlet.UsageLog;
import thredds.util.StringValidateEncodeUtils;
import thredds.util.TdsPathUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class RequestBracketingLogMessageFilter
        implements javax.servlet.Filter
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  public void init( FilterConfig filterConfig ) throws ServletException {
  }

  public void destroy() {
  }

  public void doFilter( ServletRequest servletRequest,
                        ServletResponse servletResponse,
                        FilterChain filterChain )
          throws IOException, ServletException
  {
    if ( !( servletRequest instanceof HttpServletRequest ) )
    {
      log.error( "doFilter(): Not an HTTP request! How did this filter get here?" );
      filterChain.doFilter( servletRequest, servletResponse );
      return;
    }
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    TdsServletResponseWrapper response =
            new TdsServletResponseWrapper( (HttpServletResponse) servletResponse);

    request.getServletPath();

    // Just checking
    if ( response.isCommitted())
      log.error( "doFilter(): Yikes! Response is already committed (Heiko's bug?)." );

    // Initial setup
    log.info( UsageLog.setupRequestContext( request ) );
    filterChain.doFilter( request, response );
    log.info( UsageLog.closingMessageForRequestContext( response.getHttpStatusCode(), response.getHttpResponseBodyLength() ) );

    return;
  }

}
