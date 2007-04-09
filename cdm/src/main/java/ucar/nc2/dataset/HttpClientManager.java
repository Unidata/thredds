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
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import opendap.dap.DConnect2;
import ucar.unidata.io.http.HTTPRandomAccessFile;
import thredds.util.net.EasySSLProtocolSocketFactory;

/**
 * Manage Http Client protocol settings.
 * <pre>
 * Example: 
 *   org.apache.commons.httpclient.auth.CredentialsProvider provider = new thredds.ui.UrlAuthenticatorDialog(frame);
     ucar.nc2.dataset.HttpClientManager.init(provider, "ToolsUI");
  </pre>
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class HttpClientManager {

  static private boolean protocolRegistered = false;
  static private HttpClient _client;

  /** initialize the HttpClient layer.
   *
   * @param provider CredentialsProvider.
   * @param userAgent Content of User-Agent header, may be null
   */
  static public void init(CredentialsProvider provider, String userAgent) {

    if (!protocolRegistered) {
      // this allows self-signed certificates.
      Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 8443));
      protocolRegistered = true;
    }

    initHttpClient();
    if (provider != null)
      _client.getParams().setParameter(CredentialsProvider.PROVIDER, provider);
    if (userAgent != null)
      _client.getParams().setParameter(HttpMethodParams.USER_AGENT, userAgent+"/NetcdfJava/HttpClient");
    else
      _client.getParams().setParameter(HttpMethodParams.USER_AGENT, "NetcdfJava/HttpClient");

    setHttpClient( _client);
  }

  /**
   * Set the HttpClient object - a single instance is used.
   * Propaget to entire NetcdfJava library.
   */
  static public void setHttpClient(HttpClient client) {
    _client = client;
    DConnect2.setHttpClient( _client);
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

    params.setParameter(HttpMethodParams.SO_TIMEOUT, new Integer(15000));  // 15 sec timeout
    params.setParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, Boolean.TRUE);
    params.setAuthenticationPreemptive(false);


    // look need default CredentialsProvider ??
    // _client.getParams().setParameter(CredentialsProvider.PROVIDER, provider);

  }

}
