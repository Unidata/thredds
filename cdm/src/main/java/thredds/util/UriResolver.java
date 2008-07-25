package thredds.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.*;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class UriResolver
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( UriResolver.class );

  private long connectionTimeout = 30000; // in milliseconds
  private int socketTimeout = 30000; // in milliseconds, time to wait for data
  private String contentCharset = "UTF-8";

  private UriResolver() {}
  public static UriResolver newDefaultUriResolver()
  {
    return new UriResolver();
  }

  public static UriResolver newUriResolverSettingTimeouts( long connectionTimeout,
                                                           int socketTimeout )
  {
    UriResolver u = new UriResolver();
    u.connectionTimeout = connectionTimeout;
    u.socketTimeout = socketTimeout;
    return u;
  }

  public Reader getReader( URI uri )
  { return null; }

  public InputStream getInputStream( URI uri )
          throws IOException
  {
    if ( uri.getScheme().equalsIgnoreCase( "file") )
        return new FileInputStream( getFile( uri) );

    if ( uri.getScheme().equalsIgnoreCase( "http" ))
    {
      return getHttpResponseBodyAsStream( uri );
    }

    return null;
  }

  private InputStream getHttpResponseBodyAsStream( URI uri )
          throws IOException
  {
    HttpMethod method = getHttpResponse( uri );

    return method.getResponseBodyAsStream();
    //method.getResponseHeader( "Character-encoding" );
  }

  private Reader getHttpResponseBodyAsReader( URI uri )
          throws IOException
  {
    HttpMethod method = getHttpResponse( uri );
    InputStream bodyAsStream = method.getResponseBodyAsStream();
    Reader reader = new InputStreamReader( bodyAsStream);
    Header charEncodingHeader = method.getResponseHeader( "Character-encoding" );
    if ( charEncodingHeader != null )
    {
      charEncodingHeader.getValue();
    }

    //return reader;
    return null;
  }

  private HttpMethod getHttpResponse( URI uri )
          throws IOException
  {
    HttpClient client = new HttpClient();
    HttpClientParams params = client.getParams();
    params.setConnectionManagerTimeout( this.connectionTimeout );
    params.setSoTimeout( this.socketTimeout );
    params.setContentCharset( this.contentCharset );
    HttpMethod method = new GetMethod( uri.toString() );

    client.executeMethod( method );
    int statusCode = method.getStatusCode();
    if ( statusCode == 200 || statusCode == 201 )
    {
      return method;
    }

    return null; // ToDo throw exception with some informative inforamtion.
  }

  private File getFile( URI uri)
  {
    File file = null;
    if ( uri.getScheme().equals( "file" ) )
      file = new File( uri);
    if ( file.exists() && file.isFile() && file.canRead())
      return file;
    return null;
  }
}
