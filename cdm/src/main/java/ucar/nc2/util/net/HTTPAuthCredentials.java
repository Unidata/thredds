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
 * HTTPAuthCredentials is an implementation of CredentialsProvider.
 * Its purpose is to access the HTTPAuthStore when it is invoked,
 * and it is presumably invoked when the connection needs authorization.
 */

public class HTTPAuthCredentials implements CredentialsProvider
{
    HTTPSession session = null;
    HTTPMethod method = null;
    String principal = null;
    HTTPAuthScheme authscheme = null;
    HTTPAuthScheme proxyscheme = null;

public HTTPAuthCredentials(HTTPMethod method,
		           String principal,
		           HTTPAuthStore.Entry authentry,
		           HTTPAuthStore.Entry proxyentry)
{
    this.method = method;
    this.session = method.getSession();
    this.principal = principal;
    if(authentry != null) this.authscheme = authentry.scheme;
    if(proxyentry != null) this.proxyscheme = proxyentry.scheme;
}


// Entry points for e.g. EasySSLProtocolSocketFactory

public HTTPAuthScheme
getAuthScheme() {return this.authscheme;}

public HTTPAuthScheme
getProxyScheme() {return this.proxyscheme;}

public String
getPrincipal() {return this.principal;}

public HTTPSession
getSession() {return this.session;}

//////////////////////////////////////////////////
// CredentialsProvider Interface

public Credentials getCredentials(AuthScheme scheme,
                                  String host,
                                  int port,
                                  boolean proxy)
    throws CredentialsNotAvailableException
{
    Credentials creds = null;
    CredentialsProvider cp = null;

    if(!proxy && authscheme == null)
        throw new CredentialsNotAvailableException("HTTPAuthCredentials.getCredentials: No credentials available");
    else if(proxy && proxyscheme == null)
        throw new CredentialsNotAvailableException("HTTPAuthCredentials.getCredentials: No proxy credentials available");

    if(!proxy) { // => authscheme
	boolean isbasic = authscheme.getScheme() == HTTPAuthScheme.BASIC;
        creds = (Credentials)authscheme.get(HTTPAuthScheme.CREDENTIALS);
        if(creds == null) {
            // invoke the (real) credentials provider to get some credentials
            cp = (CredentialsProvider) authscheme.get(HTTPAuthScheme.CREDENTIALSPROVIDER);
            if(cp == null)
                throw new CredentialsNotAvailableException("HTTPAuthCredentials.getCredentials: AuthStore did not specify credentials or credentials provider");
            // Use the incoming parameters
            creds = cp.getCredentials(scheme,host,port,proxy);
	    // cache for next time
            authscheme.insert(HTTPAuthScheme.CREDENTIALS,creds);
	}
    } else { // => proxyscheme; Establish proxy credentials
        // See if we already have proper credentials
        creds = (Credentials)proxyscheme.get(HTTPAuthScheme.CREDENTIALS);
        if(creds == null) {
            // invoke the (real) credentials provider
           cp = (CredentialsProvider)proxyscheme.get(HTTPAuthScheme.CREDENTIALSPROVIDER);
           if(cp == null)
               throw new CredentialsNotAvailableException("HTTPAuthCredentials.getCredentials: AuthStore did not specify proxy credentials or credentials provider");
           creds = cp.getCredentials(scheme,host,port,proxy);
	   // cache for later
           proxyscheme.insert(HTTPAuthScheme.CREDENTIALS,creds);
        }
    }
    if(creds == null)
        throw new CredentialsNotAvailableException("HTTPAuthCredentials.getCredentials: cannot obtain credentials");
    return creds;
}

}

