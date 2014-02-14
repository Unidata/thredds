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

import ucar.nc2.util.net.HTTPSession;
import ucar.nc2.util.rc.RC;

import javax.net.ssl.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author olamy
 * @version $Id: EasyX509TrustManager.java 765355 2009-04-15 20:59:07Z evenisse $
 * @since 1.2.3
 */
public class EasyX509TrustManager implements X509TrustManager
{

    static public org.slf4j.Logger logger = HTTPSession.log;
 
    private X509TrustManager standardTrustManager = null;
 
    /**
     * Constructor for EasyX509TrustManager.
     */
    public EasyX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException
    {
        super();
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keystore);
        TrustManager[] trustmanagers = factory.getTrustManagers();
        if (trustmanagers.length == 0) {
            throw new NoSuchAlgorithmException("no trust manager found");
        }
        this.standardTrustManager = (X509TrustManager) trustmanagers[0];
    }
 
    /**
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[],String authType)
     */
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException
    {
        standardTrustManager.checkClientTrusted(certificates, authType);
    }
 
    /**
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[],String authType)
     */
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException
    {
	if ((certificates != null) && logger.isDebugEnabled()) {
	    logger.debug("Server certificate chain:");
	    for (int i = 0; i < certificates.length; i++) {
		logger.debug("X509Certificate[" + i + "]=" + certificates[i]);
	    }
        }

	// The certificate checking rules are as follows:
	// 1. If !RC.getVerifyServer()
	//    then just return (indicating success)
	// 2. If certificates.length > 1 || !RC.getAllowSelfSigned() then 
	//    call standardTrustManager.checkServerTrusted() to
	//    see if this is a valid certificate chain.
	// 3. Otherwise, see if this looks like a self signed certificate.

	if(RC.getVerifyServer()) {
            if(RC.getAllowSelfSigned() && certificates != null && certificates.length == 1) {
		        X509Certificate certificate = certificates[0];
                certificate.checkValidity(); // check that current date is within the certficates valid dates
                // See if this looks like a self-signed cert
                if(!certificate.getIssuerDN().equals(certificate.getSubjectDN())) {
                    // apparently not self-signed so check certificate chain
                    standardTrustManager.checkServerTrusted(certificates,authType);
		}
	    } else {
		standardTrustManager.checkServerTrusted(certificates, authType);
	    }
	}
    }
 
    /**
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    public X509Certificate[] getAcceptedIssuers() {
        return this.standardTrustManager.getAcceptedIssuers();
    }
 
}
