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
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package ucar.nc2.util.net;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.*;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.io.IOException;

public class EasySSLProtocolSocketFactory implements SchemeLayeredSocketFactory
{

    private SSLContext sslcontext = null;

    private SSLContext createSSLContext(HttpParams params)
        throws IOException
    {
        if(this.sslcontext == null)
            try {
                // Get the Desired kind of authentication
                this.sslcontext = trustedauthentication(params);
                if(this.sslcontext == null)
                    this.sslcontext = stdauthentication();
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
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        return this.sslcontext;
    }

    // Default is to try self-signed certificates
    private SSLContext
    stdauthentication()
        throws Exception
    {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{new EasyX509TrustManager(null)}, null);
        return context;
    }

    private SSLContext
    trustedauthentication(HttpParams params)
        throws Exception
    {
        String keypath = null;
        String keypassword = null;
        String trustpath = null;
        String trustpassword = null;
        HTTPSSLProvider provider = null;
        if(params == null) return null;
        Object o = params.getParameter(HTTPAuthScheme.PROVIDER);
        if(o == null) return null;
        if(!(o instanceof HTTPSSLProvider))
            throw new HTTPException("EasySSLProtocolSocketFactory: provide is not SSL provider");
        provider = (HTTPSSLProvider)o;
        keypath = provider.getKeystore();
        keypassword = provider.getKeypassword();
        trustpath = provider.getTruststore();
        trustpassword = provider.getTrustpassword();

        TrustManager[] trustmanagers = null;
        KeyManager[] keymanagers = null;

        KeyStore keystore = buildstore(keypath, keypassword, "key");
        if(keystore != null) {
            KeyManagerFactory kmfactory = KeyManagerFactory.getInstance("SunX509");
            kmfactory.init(keystore, keypassword.toCharArray());
            keymanagers = kmfactory.getKeyManagers();
        }
        KeyStore truststore = buildstore(trustpath, trustpassword, "trust");
        if(truststore != null) {
            //todo: TrustManagerFactory trfactory = TrustManagerFactory.getInstance("SunX509");
            //trfactory.init(truststore, trustpassword.toCharArray());
            //trustmanagers = trfactory.getTrustManagers();
            trustmanagers = new TrustManager[]{new EasyX509TrustManager(truststore)};
        }
        if(trustmanagers == null)
            trustmanagers = new TrustManager[]{new EasyX509TrustManager(null)};

        SSLContext sslcontext = SSLContext.getInstance("TSL");
        sslcontext.init(keymanagers, trustmanagers, null);
        return sslcontext;
    }

    static KeyStore
    buildstore(String path, String password, String prefix)
        throws HTTPException
    {
        KeyStore store = null;
        try {
            if(path != null && password != null) {
                File storefile = new File(path);
                if(!storefile.canRead())
                    throw new HTTPException("Cannot read specified " + prefix + "store:" + storefile.getAbsolutePath());
                store = KeyStore.getInstance("JKS");
                InputStream is = null;
                try {
                    is = new FileInputStream(storefile);
                    store.load(is, password.toCharArray());
                } finally {
                    if(is != null) is.close();
                }
            }
        } catch (Exception e) {
            throw new HTTPException(e);
        }
        return store;
    }

    // -------------------------------------------------------------------
    // javadoc in org.apache.http.conn.scheme.SocketFactory says :
    // Both Object.equals() and Object.hashCode() must be overridden
    // for the correct operation of some connection managers
    // -------------------------------------------------------------------

    public boolean equals(Object obj)
    {
        return ((obj != null) && obj.getClass().equals(
            EasySSLProtocolSocketFactory.class));
    }

    public int hashCode()
    {
        return EasySSLProtocolSocketFactory.class.hashCode();
    }

    //SchemeLayeredSocketFactory API

    @Override
    public boolean isSecure(Socket socket)
        throws IllegalArgumentException
    {
        return true;
    }

    @Override
    public Socket createLayeredSocket(Socket socket, String s, int i, HttpParams httpParams)
        throws IOException
    {
        return createSSLContext(httpParams).getSocketFactory().createSocket();
    }

    public Socket createSocket(HttpParams httpParams)
        throws IOException
    {
        return createSSLContext(httpParams).getSocketFactory().createSocket();
    }

    public Socket connectSocket(Socket sock,
                                InetSocketAddress remoteAddress,
                                InetSocketAddress localAddress,
                                HttpParams params)
        throws IOException
    {
        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);
        SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket(params));
        if(localAddress != null) {
            // we need to bind explicitly
            sslsock.bind(localAddress);
        }
        sslsock.connect(remoteAddress, connTimeout);
        sslsock.setSoTimeout(soTimeout);
        return sslsock;
    }

}
