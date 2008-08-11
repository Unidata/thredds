package thredds.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.*;
import java.net.URI;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.Map;
import java.util.HashMap;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class HttpUriResolver
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( HttpUriResolver.class );

  private URI uri;
  private long connectionTimeout;
  private int socketTimeout;
  private String contentEncoding = "gzip,deflate";
  private boolean allowContentEncoding;
  private boolean followRedirects;

  private HttpMethod method = null;
  private Map<String,String> respHeaders;

  HttpUriResolver( URI uri, long connectionTimeout, int socketTimeout,
                          boolean allowContentEncoding,
                          boolean followRedirects )
  {
    if ( ! uri.getScheme().equalsIgnoreCase( "http" ) )
      throw new IllegalArgumentException( "Given a Non-HTTP URI [" + uri.toString() + "].");

    this.uri = uri;
    this.connectionTimeout = connectionTimeout;
    this.socketTimeout = socketTimeout;
    this.allowContentEncoding = allowContentEncoding;
    this.followRedirects = followRedirects;
  }

  public URI getUri() { return this.uri; }
  public long getConnectionTimeout() { return this.connectionTimeout; }
  public int getSocketTimeout() { return this.socketTimeout; }
  public String getContentEncoding() { return this.contentEncoding; }
  public boolean getAllowContentEncoding() { return this.allowContentEncoding; }
  public boolean getFollowRedirects() { return this.followRedirects; }

  public void makeRequest()
          throws IOException
  {
    if ( method != null )
      throw new IllegalStateException( "Request already made.");

    this.method = getHttpResponse( uri );
  }

  public int getResponseStatusCode()
  {
    if ( method == null )
      throw new IllegalStateException( "Request has not been made." );
    return this.method.getStatusCode();
  }

  public String getResponseStatusText()
  {
    if ( method == null )
      throw new IllegalStateException( "Request has not been made." );
    return this.method.getStatusText();
  }

  public Map<String,String> getResponseHeaders()
  {
    if ( method == null )
      throw new IllegalStateException( "Request has not been made." );

    if ( this.respHeaders == null )
    {
      this.respHeaders = new HashMap<String,String>();
      Header[] headers = this.method.getResponseHeaders();
      for ( Header h : headers )
        this.respHeaders.put( h.getName(), h.getValue() );
    }

    return respHeaders;
  }

  public String getResponseHeaderValue( String name )
  {
    if ( method == null )
      throw new IllegalStateException( "Request has not been made." );

    Header responseHeader = this.method.getResponseHeader( name );
    return responseHeader == null ? null : responseHeader.getValue();
  }

  public InputStream getResponseBodyAsInputStream()
          throws IOException
  {
    if ( method == null )
      throw new IllegalStateException( "Request has not been made." );

    InputStream is = method.getResponseBodyAsStream();
    Header contentEncodingHeader = method.getResponseHeader( "Content-Encoding" );
    if ( contentEncodingHeader != null )
    {
      String contentEncoding = contentEncodingHeader.getValue();
      if ( contentEncoding != null )
      {
        if ( contentEncoding.equalsIgnoreCase( "gzip" ) )
          return new GZIPInputStream( is );
        else if ( contentEncoding.equalsIgnoreCase( "deflate" ) )
          return new InflaterInputStream( is );
      }
    }
    return is;
  }

  private HttpMethod getHttpResponse( URI uri )
          throws IOException
  {
    HttpClient client = new HttpClient();
    HttpClientParams params = client.getParams();
    params.setConnectionManagerTimeout( this.connectionTimeout );
    params.setSoTimeout( this.socketTimeout );
    HttpMethod method = new GetMethod( uri.toString() );
    method.setFollowRedirects( this.followRedirects );
    method.addRequestHeader( "Accept-Encoding", this.contentEncoding );

    client.executeMethod( method );
    int statusCode = method.getStatusCode();
    if ( statusCode == 200 || statusCode == 201 )
    {
      return method;
    }

    return null; // ToDo throw exception with some informative inforamtion.
  }
}
