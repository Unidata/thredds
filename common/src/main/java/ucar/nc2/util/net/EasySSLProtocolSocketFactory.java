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
import java.security.cert.CertificateException;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

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

public class EasySSLProtocolSocketFactory implements ProtocolSocketFactory
{

/**
 * Log object for this class.
 */
/* FIX
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EasySSLProtocolSocketFactory.class);
//  private static final Log LOG = LogFactory.getLog(EasySSLProtocolSocketFactory.class);
*/

private SSLContext sslcontext = null;

/**
 * Constructor for EasySSLProtocolSocketFactory.
 */
public EasySSLProtocolSocketFactory()
{
    super();
}


/**
 * @see SecureProtocolSocketFactory#createSocket(java.lang.String, int, java.net.InetAddress, int)
 */
public Socket createSocket(
        String host,
        int port,
        InetAddress clientHost,
        int clientPort)
        throws IOException, UnknownHostException
{
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
        final HttpConnectionParams params
                          ) throws IOException, UnknownHostException, ConnectTimeoutException
{

    if (params == null) {
        throw new IllegalArgumentException("Parameters may not be null");
    }
    int timeout = params.getConnectionTimeout();
    if (timeout == 0) {
        //return createSocket(host, port, localAddress, localPort);
        return createSocket(host, port);
    } else {
        // To be eventually deprecated when migrated to Java 1.4 or above
        return ControllerThreadSocketFactory.createSocket(this, host, port, localAddress, localPort, timeout);
    }
}

/**
 * @see SecureProtocolSocketFactory#createSocket(java.lang.String, int)
 */
public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
{
    return getSSLContext().getSocketFactory().createSocket(
            host,
            port);
}

/**
 * @see SecureProtocolSocketFactory#createSocket(java.net.Socket, java.lang.String, int, boolean)
 */
public Socket createSocket(
        Socket socket,
        String host,
        int port,
        boolean autoClose)
        throws IOException, UnknownHostException
{

       return getSSLContext().getSocketFactory().createSocket(
            socket,
            host,
            port,
            autoClose);
}

/**
 * Add code to handle ESG authorization using a keystore
 * H/T to Apache and Philip Kershaw and Jon Blower for this code.
 */
private static KeyStore createKeyStore(final File keystorefile, final String password)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
{
    KeyStore keystore = KeyStore.getInstance("jks");
    InputStream is = null;
    try {
        is = new FileInputStream(keystorefile);
        keystore.load(is, password.toCharArray());
    } finally {
        if (is != null) is.close();
    }
    return keystore;
}

private SSLContext createSSLContext()  throws HTTPException
{
    try {
        KeyManager[] keymanagers = null;
        KeyStore keystore = null;
        KeyStore truststore = null;
        TrustManager[] trustmanagers = null;

        String keypassword = getpassword("key");
        String keypath = getstorepath("key");
        String trustpassword = getpassword("trust");
        String trustpath = getstorepath("trust");

        keystore = buildstore(keypath, keypassword, "key");
        if(keystore != null) {
            KeyManagerFactory kmfactory = KeyManagerFactory.getInstance("SunX509");
            kmfactory.init(keystore, keypassword.toCharArray());
            keymanagers = kmfactory.getKeyManagers();
        }

        truststore = buildstore(trustpath, trustpassword,"trust");
        if(truststore != null) {
            //TrustManagerFactory trfactory = TrustManagerFactory.getInstance("SunX509");
            //trfactory.init(truststore, trustpassword.toCharArray());
            //trustmanagers = trfactory.getTrustManagers();
            trustmanagers = new TrustManager[]{new EasyX509TrustManager(truststore)};
        }  else  {
            trustmanagers = new TrustManager[] {new EasyX509TrustManager(null)};
        }
        SSLContext sslcontext = SSLContext.getInstance("SSL");
        sslcontext.init(keymanagers, trustmanagers, null);

        return sslcontext;

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
    static String
        getpassword(String prefix)
        {
            String password = System.getProperty(prefix + "storepassword");
            if(password != null) {
                password = password.trim();
                if(password.length() == 0) password = null;
            }
                 return password;
        }

    static String
        getstorepath(String prefix)
        {
                String path = System.getProperty(prefix + "store");
           return path;
        }

    static KeyStore
    buildstore(String path, String password, String prefix) throws HTTPException
    {
        KeyStore store = null;
        try {
            if(path != null && password != null) {
            File storefile = new File(path);
            if(!storefile.canRead())
                throw new HTTPException("Cannot read specified "+prefix+"store:"+storefile.getAbsolutePath());
            store = KeyStore.getInstance("JKS");
            InputStream is = null;
            try {
                is = new FileInputStream(storefile);
                store.load(is, password.toCharArray());
            } finally {
                if (is != null) is.close();
            }
            }
        } catch (Exception e) {throw new HTTPException(e);}
        return store;
    }

    private SSLContext getSSLContext() throws HTTPException
{
    if (this.sslcontext == null) {
        this.sslcontext = createSSLContext();
    }
    return this.sslcontext;
}

}
