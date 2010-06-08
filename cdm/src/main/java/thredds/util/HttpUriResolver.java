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
package thredds.util;

import org.apache.http.HttpResponse;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

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

  //private HttpRequestBase method = null;
  private HttpResponse response = null;
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
    if ( response != null )
      throw new IllegalStateException( "Request already made.");

    this.response = getHttpResponse( uri );
  }

  public int getResponseStatusCode()
  {
    if ( response == null )
      throw new IllegalStateException( "Request has not been made." );
    return this.response.getStatusLine().getStatusCode();
  }

  public Map<String,String> getResponseHeaders()
  {
    if ( response == null )
      throw new IllegalStateException( "Request has not been made." );

    if ( this.respHeaders == null )
    {
      this.respHeaders = new HashMap<String,String>();
      Header[] headers = this.response.getAllHeaders();
      for ( Header h : headers )
        this.respHeaders.put( h.getName(), h.getValue() );
    }

    return respHeaders;
  }

  public String getResponseHeaderValue( String name )
  {
    if ( response == null )
      throw new IllegalStateException( "Request has not been made." );

    Header responseHeader = this.response.getFirstHeader( name );
    return responseHeader == null ? null : responseHeader.getValue();
  }

  public InputStream getResponseBodyAsInputStream()
          throws IOException
  {
    if ( response == null )
      throw new IllegalStateException( "Request has not been made." );

    InputStream is = response.getEntity().getContent();
    Header contentEncodingHeader = response.getFirstHeader( "Content-Encoding" );
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

  private HttpResponse getHttpResponse( URI uri )
          throws IOException
  {
    AbstractHttpClient client = new DefaultHttpClient();
    HttpParams params = client.getParams();
    params.setParameter(AllClientPNames.CONNECTION_TIMEOUT, this.connectionTimeout );
    params.setParameter(AllClientPNames.SO_TIMEOUT, this.socketTimeout );
    HttpGet method = new HttpGet( uri.toString() );
    //method.setFollowRedirects( this.followRedirects );
    method.addHeader( "Accept-Encoding", this.contentEncoding );

    client.execute( method );
    HttpResponse response = client.execute(method);
    int statusCode = response.getStatusLine().getStatusCode();
    if ( statusCode == 200 || statusCode == 201 )
    {
      return response;
    }

    return null; // ToDo throw exception with some informative inforamtion.
  }
}
