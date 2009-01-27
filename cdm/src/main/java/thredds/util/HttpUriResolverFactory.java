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
