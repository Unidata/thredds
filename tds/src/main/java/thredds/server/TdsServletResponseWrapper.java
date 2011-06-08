package thredds.server;

import thredds.server.config.TdsConfig;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class TdsServletResponseWrapper extends HttpServletResponseWrapper
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

//  private TdsContext tdsContext;
  private int httpStatusCode = 200;
  private long httpResponseBodyLength = -1;

  public TdsServletResponseWrapper( HttpServletResponse response ) {
    super( response);
  }

//  public TdsContext getTdsContext() {
//    return this.tdsContext;
//  }
//
//  public void setTdsContext( TdsContext tdsContext ) {
//    this.tdsContext = tdsContext;
//  }

  public int getHttpStatusCode() {
    return this.httpStatusCode;
  }

  public void setHttpStatusCode( int statusCode ) {
    this.httpStatusCode = statusCode;
  }

  public long getHttpResponseBodyLength() {
    return this.httpResponseBodyLength;
  }

  public void setHttpResponseBodyLength( long responseBodyLength ) {
    this.httpResponseBodyLength = responseBodyLength;
  }

  @Override
  public void sendError( int sc, String msg ) throws IOException
  {
    this.setHttpStatusCode( sc );
    super.sendError( sc, msg );
  }

  @Override
  public void sendError( int sc ) throws IOException
  {
    this.setHttpStatusCode( sc );
    super.sendError( sc );
  }

  @Override
  public void sendRedirect( String location ) throws IOException
  {
    this.setHttpStatusCode( HttpServletResponse.SC_FOUND );
    super.sendRedirect( location );
  }

  @Override
  public void setStatus( int sc )
  {
    this.setHttpStatusCode( sc );
    super.setStatus( sc );
  }

  @Override
  public void setStatus( int sc, String sm )
  {
    this.setHttpStatusCode( sc );
    super.setStatus( sc, sm );
  }

  @Override
  public void setContentLength( int len )
  {
    this.setHttpResponseBodyLength( len );
    super.setContentLength( len );
  }
}
