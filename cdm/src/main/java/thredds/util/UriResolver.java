package thredds.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
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

  public Reader getReader( URI uri )
  { return null; }

  public InputStream getInputStream( URI uri )
          throws IOException
  {
    if ( uri.getScheme().equalsIgnoreCase( "file") )
        return new FileInputStream( getFile( uri) );

    if ( uri.getScheme().equalsIgnoreCase( "http" ))
    {
      HttpClient client = new HttpClient();
      HttpMethod method = new GetMethod( uri.toString() );
      HttpConnectionParams connectParams = new HttpConnectionParams();
      connectParams.setConnectionTimeout( 45000 );
      connectParams.setSoTimeout( 45000 );

      HttpConnectionManager connectManager = new MultiThreadedHttpConnectionManager();
      //connectManager.setParams( connectParams );

      client.executeMethod( method );

      method.getResponseBodyAsStream();
      method.getResponseHeader( "Character-encoding" );

    }

    return null;
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
