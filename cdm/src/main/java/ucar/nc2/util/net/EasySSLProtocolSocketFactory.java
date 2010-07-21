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


import opendap.dap.DAPException;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

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
 *     AbstractHttpClient client = new DefaultHttpClient();
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
 *     AbstractHttpClient client = new DefaultHttpClient();
 *     GetMethod httpget = new GetMethod("https://localhost/");
 *     client.executeMethod(httpget);
 *     </pre>
 * </p>
 *
 * @author <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 *         <p/>
 *         <p/>
 *         DISCLAIMER: AbstractHttpClient developers DO NOT actively support this component.
 *         The component is provided as a reference material, which may be inappropriate
 *         for use without additional customization.
 *         </p>
 */

public class EasySSLProtocolSocketFactory extends SSLProtocolSocketFactory
{

  /**
   * Log object for this class.
   */
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EasySSLProtocolSocketFactory.class);
  //private static final Log LOG = LogFactory.getLog(EasySSLProtocolSocketFactory.class);

  private SSLContext sslcontext = null;

  /**
   * Constructor for EasySSLProtocolSocketFactory.
   */
  public EasySSLProtocolSocketFactory() throws IOException {
          super();

  }

  private static SSLContext createEasySSLContext() throws IOException {
    try {
      SSLContext context = SSLContext.getInstance("SSL");
      context.init(
              null,
              new TrustManager[]{new EasyX509TrustManager(null)},
              null);
      return context;
    } catch (Exception e) {
      logger.error("createEasySSLContext", e);
      throw new IOException(e.toString());
    }
  }

  public boolean isSecure(Socket s) {return true;};

  private SSLContext getSSLContext() throws IOException {
    if (this.sslcontext == null) {
      this.sslcontext = createEasySSLContext();
    }
    return this.sslcontext;
  }

  /**
   */
  public Socket createSocket(
          String host,
          int port,
          InetAddress clientHost,
          int clientPort)
          throws IOException, HttpException
  {

    return getSSLContext().getSocketFactory().createSocket(
            host,
            port,
            clientHost,
            clientPort
    );
  }

  /**
   * Attempts to get a new socket connection to the given host within the given time limit.
   * <p/>
   * To circumvent the limitations of older JREs that do not support connect timeout a
   * controller thread is executed. The controller thread attempts to create a new socket
   * within the given limit of time. If socket constructor does not return until the
   * timeout expires, the controller terminates and throws an { ConnectTimeoutException}
   * </p>
   *
   * @param host         the host name/IP
   * @param port         the port on the host
   * @param localAddress the local host name/IP to bind the socket to
   * @param localPort    the port on the local machine
   * @param params       {@link HttpParams HttpSession connection parameters}
   * @return Socket a new socket
   * @throws IOException          if an I/O error occurs while creating the socket
   * @throws UnknownHostException if the IP address of the host cannot be
   *                              determined
   */
  private Socket createSocket(
          final String host,
          final int port,
          final InetAddress localAddress,
          final int localPort,
          final HttpParams params
  ) throws IOException, HttpException
  {
    if (params == null) {
      throw new IllegalArgumentException("Parameters may not be null");
    }
    int timeout = params.getIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT,25);
    return createSocket(host, port, localAddress, localPort);
  }

  /**
   */
  public Socket createSocket(String host, int port)
          throws IOException {
    return getSSLContext().getSocketFactory().createSocket(
            host,
            port
    );
  }

  /**
   */
  public Socket getSocket(
          Socket socket,
          String host,
          int port,
          boolean autoClose)
          throws IOException {
    return getSSLContext().getSocketFactory().createSocket(
            socket,
            host,
            port,
            autoClose
    );
  }
    public Socket getSocket()
          throws IOException {
        return getSSLContext().getSocketFactory().createSocket();
    }

}
