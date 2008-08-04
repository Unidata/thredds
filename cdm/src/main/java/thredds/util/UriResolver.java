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
  private int socketTimeout = 1 * 60 * 1000; // in milliseconds, time to wait for data
  private String contentCharset = "UTF-8";
  private String contentEncoding = "gzip,deflate";
  private boolean wantContentEncoding = true;

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

  public Reader getResponseBodyAsReader( URI uri )
  { return null; }

  public String getResponseBodyAsString( URI uri )
          throws IOException
  {
//    if ( uri.getScheme().equalsIgnoreCase( "file") )
//        return new FileInputStream( getFile( uri) );
//
    if ( uri.getScheme().equalsIgnoreCase( "http" ))
    {
      return getHttpResponseBodyAsString( uri );
    }

    return null;
  }

  public InputStream getResponseBodyAsInputStream( URI uri )
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

  private String getHttpResponseBodyAsString( URI uri )
          throws IOException
  {
    HttpMethod method = getHttpResponse( uri );

    return method.getResponseBodyAsString();
    //method.getResponseHeader( "Character-encoding" );
  }

  private InputStream getHttpResponseBodyAsStream( URI uri )
          throws IOException
  {
    HttpMethod method = getHttpResponse( uri );

    InputStream is = new BufferedInputStream( method.getResponseBodyAsStream(), 1000 * 1000);
    Header contentEncodingHeader = method.getResponseHeader( "Content-Encoding" );
    if ( contentEncodingHeader != null )
    {
      String contentEncoding = contentEncodingHeader.getValue();
      if (contentEncoding != null && contentEncoding.equalsIgnoreCase( "gzip" ))
      {
        System.out.println( "GZIP" );
        is = new GZIPInputStream( is );
      }
      else if ( contentEncoding != null && contentEncoding.equalsIgnoreCase( "deflate" ) )
      {
        System.out.println( "GZIP" );
        is = new InflaterInputStream( is );
      }
    }
    return is;
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
    HttpMethod method = new GetMethod( uri.toString() );
    method.addRequestHeader( "Accept-Encoding", "gzip" );

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
