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

package ucar.nc2.util.net;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.auth.*;

import static ucar.nc2.util.net.HTTPAuthStore.*;

/**
 * HTTPAuthManager is a subclass of CredentialsProvider.
 * Its purpose is to access the HTTPAuthStore when it is invoked,
 * and it is presumably invoked when the connection needs authorization.
 */


public class HTTPAuthManager implements CredentialsProvider
{
    HTTPSession session = null;
    HTTPMethod method = null;
    HTTPAuthScheme scheme = null;
    String principal = null;
    HTTPAuthStore.Entry authentry = null;
    HTTPAuthStore.Entry proxyentry = null;

public HTTPAuthManager(HTTPMethod method,HTTPAuthScheme scheme, String principal,
                       HTTPAuthStore.Entry authentry, HTTPAuthStore.Entry proxyentry)
{
    this.method = method;
    this.scheme = scheme;
    this.principal = principal;
    this.authentry = authentry;
    this.proxyentry = proxyentry;
    this.session = method.getSession();
}


// Entry points for e.g. EasySSLProtocolSocketFactory

public HTTPAuthStore.Entry
getAuthEntry() {return this.authentry;}

public HTTPAuthStore.Entry
getProxyEntry() {return this.proxyentry;}

public String
getPrincipal() {return this.principal;}

public HTTPSession
getSession() {return this.session;}

public HTTPAuthScheme
getScheme() {return this.scheme;}

//////////////////////////////////////////////////
// CredentialsProvider Interface

public Credentials getCredentials(AuthScheme authscheme,
                              String host,
                              int port,
                              boolean proxy)
    throws CredentialsNotAvailableException
{
    Credentials creds = null;
    CredentialsProvider cp = null;

    // Set up an entry and search for authstore matches
    HTTPAuthStore.Entry pattern = new HTTPAuthStore.Entry(
                                      session,
                                      this.principal,
                                      host,
                                      port,
                                      ANY_PATH,
                                      null) ;
    
    if(!proxy && authentry == null)
        throw new CredentialsNotAvailableException("GetCredentials: No credentials available");
    else if(proxy && proxyentry == null)
        throw new CredentialsNotAvailableException("GetCredentials: No proxy credentials available");

    if(!proxy) { // => authentry
	boolean isbasic = scheme.getScheme() == HTTPAuthScheme.BASIC;
        creds = (Credentials)authentry.scheme.get(HTTPAuthScheme.CREDENTIALS);
        if(creds == null) {
            // invoke the (real) credentials provider
            cp = (CredentialsProvider) authentry.scheme.get(HTTPAuthScheme.CREDENTIALSPROVIDER);
            if(cp == null)
                throw new CredentialsNotAvailableException("GetCredentials: AuthStore does not specify credentials or credentials provider");
            creds = cp.getCredentials(authentry.scheme,host,port,false);
            authentry.scheme.insert(HTTPAuthScheme.CREDENTIALS,creds);
	}
    } else { // Establish proxy credentials
        // See if we already have proper credentials
        creds = (Credentials)proxyentry.scheme.get(HTTPAuthScheme.CREDENTIALS);
        if(creds == null) {
            // invoke the (real) credentials provider
           cp = (CredentialsProvider)proxyentry.scheme.get(HTTPAuthScheme.CREDENTIALSPROVIDER);
           if(cp == null)
               throw new CredentialsNotAvailableException("GetCredentials: AuthStore does not specify proxy credentials or credentials provider");
           creds = cp.getCredentials(proxyentry.scheme,host,port,false);
           proxyentry.scheme.insert(HTTPAuthScheme.CREDENTIALS,creds);
        }
    }
    if(creds == null)
        throw new CredentialsNotAvailableException("GetCredentials: cannot obtain credentials");
    return creds;
}

}

