// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.dataset;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import opendap.dap.DConnect2;
import ucar.unidata.io.http.HTTPRandomAccessFile;
import ucar.nc2.util.net.EasySSLProtocolSocketFactory;

import java.io.IOException;

/**
 * DO NOT USE, TEMPORARY!
 */
public class HttpClientManager {
  static private boolean debug = false;
  static private HttpClient _client;
  static private int timeout = 0;

  /**
   * initialize the HttpClient layer.
   *
   * @param provider  CredentialsProvider.
   * @param userAgent Content of User-Agent header, may be null
   */
  static public void init(CredentialsProvider provider, String userAgent) {
    initHttpClient();

    if (provider != null)
      _client.getParams().setParameter(CredentialsProvider.PROVIDER, provider);

    if (userAgent != null)
      _client.getParams().setParameter(HttpMethodParams.USER_AGENT, userAgent + "/NetcdfJava/HttpClient");
    else
      _client.getParams().setParameter(HttpMethodParams.USER_AGENT, "NetcdfJava/HttpClient");

    // nick.bower@metoceanengineers.com
    String proxyHost = System.getProperty("http.proxyHost");
    String proxyPort = System.getProperty("http.proxyPort");
    if ((proxyHost != null) && (proxyPort != null) && !proxyPort.trim().equals("")) {
        _client.getHostConfiguration().setProxy(proxyHost, Integer.parseInt(proxyPort));
    }

    setHttpClient(_client);
  }

  /**
   * Set the HttpClient object - a single instance is used.
   * Propaget to entire NetcdfJava library.
   */
  static public void setHttpClient(HttpClient client) {
    _client = client;
    DConnect2.setHttpClient(_client);
    HTTPRandomAccessFile.setHttpClient(_client);
  }

  /**
   * Get the HttpClient object - a single instance is used.
   */
  static public HttpClient getHttpClient() {
    return _client;
  }

  private static synchronized void initHttpClient() {
    if (_client != null) return;
    MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    _client = new HttpClient(connectionManager);

    HttpClientParams params = _client.getParams();
    params.setParameter(HttpMethodParams.SO_TIMEOUT, new Integer(timeout));
    params.setParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, Boolean.TRUE);
    params.setParameter(HttpClientParams.COOKIE_POLICY, CookiePolicy.RFC_2109);

    // allow self-signed certificates
    Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 8443));

    // LOOK need default CredentialsProvider ??
    // _client.getParams().setParameter(CredentialsProvider.PROVIDER, provider);

  }

  public static void clearState() {
    _client.getState().clearCookies();
    _client.getState().clearCredentials();
  }

  /**
   * COnvenience better to use getResponseAsStream
   */
  public static String getContent(String urlString) throws IOException {
    GetMethod m = new GetMethod(urlString);
    m.setFollowRedirects(true);

    try {
      _client.executeMethod(m);
      return m.getResponseBodyAsString();

    } finally {
      m.releaseConnection();
    }
  }

  public static int putContent(String urlString, String content) throws IOException {
    PutMethod m = new PutMethod(urlString);
    m.setDoAuthentication( true );

    try {
      m.setRequestEntity(new StringRequestEntity(content));

      _client.executeMethod(m);

      int resultCode = m.getStatusCode();

       // followRedirect wont work for PUT
       if (resultCode == 302) {
         String redirectLocation;
         Header locationHeader = m.getResponseHeader("location");
         if (locationHeader != null) {
           redirectLocation = locationHeader.getValue();
           if (debug) System.out.println("***Follow Redirection = "+redirectLocation);
           resultCode = putContent(redirectLocation, content);
         }
       }

      return resultCode;

    } finally {
      m.releaseConnection();
    }
  }

}
