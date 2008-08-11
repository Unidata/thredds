package thredds.util;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class HttpUriResolverFactory
{
  private static long defaultConnectionTimeout = 30 * 1000; // Default: 30 seconds
  private static int defaultSocketTimeout = 1 * 30 * 1000; // Default: 30 seconds
  private static boolean defaultAllowContentEncoding = true;
  private static boolean defaultFollowRedirects = true;

  public static HttpUriResolver getDefaultHttpUriResolver( URI uri )
  {
    return new HttpUriResolver( uri, defaultConnectionTimeout, defaultSocketTimeout,
                                defaultAllowContentEncoding, defaultFollowRedirects );
  }

  private long connectionTimeout;
  private int socketTimeout;
  private boolean allowContentEncoding;
  private boolean followRedirects;

  public HttpUriResolverFactory()
  {
    this.connectionTimeout = defaultConnectionTimeout;
    this.socketTimeout = defaultSocketTimeout;
    this.allowContentEncoding = defaultAllowContentEncoding;
    this.followRedirects = defaultFollowRedirects;
  }

  public long getConnectionTimeout()
  {
    return connectionTimeout;
  }

  public void setConnectionTimeout( long connectionTimeout )
  {
    this.connectionTimeout = connectionTimeout;
  }

  public int getSocketTimeout()
  {
    return socketTimeout;
  }

  public void setSocketTimeout( int socketTimeout )
  {
    this.socketTimeout = socketTimeout;
  }

  public boolean getAllowContentEncoding()
  {
    return allowContentEncoding;
  }

  public void setAllowContentEncoding( boolean allowContentEncoding )
  {
    this.allowContentEncoding = allowContentEncoding;
  }

  public boolean getFollowRedirects()
  {
    return followRedirects;
  }

  public void setFollowRedirects( boolean followRedirects )
  {
    this.followRedirects = followRedirects;
  }

  public HttpUriResolver newHttpUriResolver( URI uri )
  {
    return new HttpUriResolver( uri, this.connectionTimeout, this.socketTimeout, this.allowContentEncoding, this.followRedirects );
  }
}
