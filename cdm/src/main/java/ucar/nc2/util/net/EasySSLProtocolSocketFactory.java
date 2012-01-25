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

/*
 * ====================================================================
 *
 *  Copyright 2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package ucar.nc2.util.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.auth.CredentialsProvider;

import javax.net.ssl.*;

/**
 * <p/>
 * EasySSLProtocolSocketFactory can be used to creats SSL {@link Socket}s
 * that accept self-signed certificates.
 * </p>
 * <p/>
 * This socket factory SHOULD NOT be used for productive systems
 * due to security reasons, unless it is a concious decision and
 * you are perfectly aware of security implications of accepting
 * self-signed certificates
 * </p>
 * <p/>
 * <p/>
 * Example of using custom protocol socket factory for a specific host:
 * <pre>
 *     Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
 * <p/>
 *     HttpClient client = new HttpClient();
 *     client.getHostConfiguration().setHost("localhost", 443, easyhttps);
 *     // use relative url only
 *     GetMethod httpget = new GetMethod("/");
 *     client.executeMethod(httpget);
 *     </pre>
 * </p>
 * <p/>
 * Example of using custom protocol socket factory per default instead of the standard one:
 * <pre>
 *     Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
 *     Protocol.registerProtocol("https", easyhttps);
 * <p/>
 *     HttpClient client = new HttpClient();
 *     GetMethod httpget = new GetMethod("https://localhost/");
 *     client.executeMethod(httpget);
 *     </pre>
 * </p>
 *
 * @author <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 *         <p/>
 *         <p/>
 *         DISCLAIMER: HttpClient developers DO NOT actively support this component.
 *         The component is provided as a reference material, which may be inappropriate
 *         for use without additional customization.
 *         </p>
 */

public class EasySSLProtocolSocketFactory implements ProtocolSocketFactory {

//////////////////////////////////////////////////

  private SSLContext sslcontext = null;

  /**
   * Constructor for EasySSLProtocolSocketFactory.
   */
  public EasySSLProtocolSocketFactory() {
    super();
  }


  /**
   * @see SecureProtocolSocketFactory#createSocket(java.net.Socket, java.lang.String, int, boolean)
   */
  public Socket createSocket(
          Socket socket,
          String host,
          int port,
          boolean autoClose)
          throws IOException, UnknownHostException {

    return getSSLContext(null, host, port).getSocketFactory().createSocket(
            socket,
            host,
            port,
            autoClose);
  }

  /**
   * @see SecureProtocolSocketFactory#createSocket(java.lang.String, int)
   */
  public Socket createSocket(String host, int port)
          throws IOException, UnknownHostException {
    return getSSLContext(null, host, port).getSocketFactory().createSocket(host, port);
  }

  /**
   * @see SecureProtocolSocketFactory#createSocket(java.lang.String, int, java.net.InetAddress, int)
   */
  public Socket createSocket(
          String host,
          int port,
          InetAddress clientHost,
          int clientPort)
          throws IOException, UnknownHostException {
    return createSocket(
            host,
            port,
            clientHost,
            clientPort,
            new HttpConnectionParams());
  }

  /**
   * Attempts to get a new socket connection to the given host within the given time limit.
   * <p/>
   * To circumvent the limitations of older JREs that do not support connect timeout a
   * controller thread is executed. The controller thread attempts to create a new socket
   * within the given limit of time. If socket constructor does not return until the
   * timeout expires, the controller terminates and throws an {@link ConnectTimeoutException}
   * </p>
   *
   * @param host         the host name/IP
   * @param port         the port on the host
   * @param localAddress the local host name/IP to bind the socket to
   * @param localPort    the port on the local machine
   * @param params       {@link HttpConnectionParams Http connection parameters}
   * @return Socket a new socket
   * @throws IOException          if an I/O error occurs while creating the socket
   * @throws UnknownHostException if the IP address of the host cannot be
   *                              determined
   */
  public Socket createSocket(
          final String host,
          final int port,
          final InetAddress localAddress,
          final int localPort,
          final HttpConnectionParams params)
          throws IOException, UnknownHostException, ConnectTimeoutException {
    if (params == null) {
      throw new IllegalArgumentException("Parameters may not be null");
    }
    int timeout = params.getConnectionTimeout();
    if (true) {
      return getSSLContext(params, host, port).getSocketFactory().createSocket(host, port);
    } else {
      if (timeout == 0) {
        return getSSLContext(params, host, port).getSocketFactory().createSocket(host, port);
      } else {
        // To be eventually deprecated when migrated to Java 1.4 or above
        return ControllerThreadSocketFactory.createSocket(this, host, port, localAddress, localPort, timeout);
      }
    }

  }

  private SSLContext getSSLContext(HttpConnectionParams params, String host, int port) throws HTTPException {
    if (this.sslcontext == null) {
      this.sslcontext = createSSLContext(params, host, port);
    }
    return this.sslcontext;
  }

  private SSLContext createSSLContext(HttpConnectionParams params, String host, int port) throws HTTPException {
    SSLContext sslcontext = null;
    KeyManager[] keymanagers = null;
    KeyStore keystore = null;
    KeyStore truststore = null;
    TrustManager[] trustmanagers = null;

    String keypassword = null;
    String keypath = null;
    String trustpassword = null;
    String trustpath = null;

    try {

      // Get the HTTPAuthProvider
      HTTPAuthProvider provider;
      provider = (HTTPAuthProvider) params.getParameter(CredentialsProvider.PROVIDER);
      if (provider == null) return stdauthenticate();

      // Abuse the getCredentials() api
      Credentials creds = null;
      try {
          creds = provider.getCredentials(HTTPSSLScheme.Default, null, 0, false);
          if (creds == null) return stdauthenticate();
      } catch (CredentialsNotAvailableException e) {
          return stdauthenticate();
      }

      HTTPSSLProvider sslprovider = (creds == null ? null : (HTTPSSLProvider) creds);
      if (sslprovider == null)
          return stdauthenticate();

      keypath = (String) sslprovider.getKeystore();
      keypassword = (String) sslprovider.getKeypassword();
      trustpath = (String) sslprovider.getTruststore();
      trustpassword = (String) sslprovider.getTrustpassword();

      keystore = buildstore(keypath, keypassword, "key");
      if (keystore != null) {
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance("SunX509");
        kmfactory.init(keystore, keypassword.toCharArray());
        keymanagers = kmfactory.getKeyManagers();
      }

      truststore = buildstore(trustpath, trustpassword, "trust");
      if (truststore != null) {
        //TrustManagerFactory trfactory = TrustManagerFactory.getInstance("SunX509");
        //trfactory.init(truststore, trustpassword.toCharArray());
        //trustmanagers = trfactory.getTrustManagers();
        trustmanagers = new TrustManager[]{new EasyX509TrustManager(truststore)};
      } else {
        trustmanagers = new TrustManager[]{new EasyX509TrustManager(null)};
      }

      sslcontext = SSLContext.getInstance("SSL");
      sslcontext.init(keymanagers, trustmanagers, null);

      return sslcontext;

    } catch (KeyManagementException e) {
        throw new HTTPException("Key Management exception: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
        throw new HTTPException("Unsupported algorithm exception: " + e.getMessage());
    } catch (KeyStoreException e) {
      throw new HTTPException("Keystore exception: " + e.getMessage());
    } catch (GeneralSecurityException e) {
      throw new HTTPException("Key management exception: " + e.getMessage());
    } catch (IOException e) {
      throw new HTTPException("I/O error reading keystore/truststore file: " + e.getMessage());
    }
  }

  // Do no authentication
  static private SSLContext
  stdauthenticate()
      throws KeyManagementException,NoSuchAlgorithmException, KeyStoreException
  {
    TrustManager[] trustmanagers = new TrustManager[]{new EasyX509TrustManager(null)};
    SSLContext sslcontext = SSLContext.getInstance("SSL");
    sslcontext.init(null, trustmanagers, null);
    return sslcontext;
  }

  static KeyStore
  buildstore(String path, String password, String prefix) throws HTTPException {
    KeyStore store = null;
    try {
      if (path != null && password != null) {
        File storefile = new File(path);
        if (!storefile.canRead())
          throw new HTTPException("Cannot read specified " + prefix + "store:" + storefile.getAbsolutePath());
        store = KeyStore.getInstance("JKS");
        InputStream is = null;
        try {
          is = new FileInputStream(storefile);
          store.load(is, password.toCharArray());
        } finally {
          if (is != null) is.close();
        }
      }
    } catch (Exception e) {
      throw new HTTPException(e);
    }
    return store;
  }


}
