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

package ucar.httpservices;

import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * With httpclient 4.2.x, credentials caching is
 * pushed to the CredentialsProvider implementation.
 * See org.apache.http.impl.client.BasicCredentialsProvider,
 * for an example. However, most existing CredentialsProviders
 * do not do caching.
 * <p/>
 * So this class is used as a wrapper around
 * CredentialsProviders to supply caching support.
 * The cache itself is a singleton kept as static data
 * in this class.
 * <p/>
 * Previously, the cache was per-session, but this
 * turn out to be a mistake because the opendap code
 * repeatedly calls the server in checkifdods() with
 * a new HTTPSession, so we get repeated requests to
 * the credentials provider.
 */

public class HTTPCachingProvider implements CredentialsProvider
{
    static final int MAX_RETRIES = 3;

    //////////////////////////////////////////////////
    // Predefined keys (Used local to the package)

    static final String PRINCIPAL = "ucar.nc2.principal";
    static final String URI = "ucar.nc2.url";
    static final String CREDENTIALSPROVIDER = "ucar.nc2.credentialsprovider";
    static final String KEYSTOREPATH = "ucar.nc2.keystore";
    static final String KEYSTOREPASSWORD = "ucar.nc2.keystorepassword";
    static final String TRUSTSTOREPATH = "ucar.nc2.truststore";
    static final String TRUSTSTOREPASSWORD = "ucar.nc2.truststorepassword";
    static final String CREDENTIALS = "ucar.nc2.credentials";
    static final String AUTHSTRING = "ucar.nc2.authstring";
    static String SCHEME = "ucar.nc2.scheme";
    static String PASSWORD = "ucar.nc2.password";
    static String USER = "ucar.nc2.user";
    static public final String WWW_AUTH_RESP = "Authorization";   // from HttpMethodDirector
    static public final String PROXY_AUTH_RESP = "Proxy-Authorization"; // from HttpMethodDirector

    //////////////////////////////////////////////////

    static public boolean TESTING = false;

    //////////////////////////////////////////////////
    // Instance variables

    protected HTTPAuthStore store = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public HTTPCachingProvider(HTTPAuthStore store)
    {
        this.store = store;
    }

    //////////////////////////////////////////////////
    // Credentials Provider Interface

    synchronized public Credentials
    getCredentials(AuthScope scope)
    {
        if(TESTING) {
            System.err.println("HTTPCachingProvider.getCredentials,arg " + scope.toString());
            System.err.flush();
        }

        // See if the credentials have been cached.
        Credentials credentials = checkCache(scope);
        if(credentials != null) {
            if(TESTING)
                System.err.println("Using cached credentials: " + credentials);
            return credentials;
        }

        // Not cached.
        if(TESTING)
            System.err.println("Credentials not cached");

        String scheme = scope.getScheme();
        try {
            if(scheme == null)
                throw new InvalidCredentialsException("HTTPCachingProvider: null scope scheme: " + scope);

            // search for matching authstore entries
            CredentialsProvider cp = this.store.lookup(scope);
            if(cp == null)
                throw new InvalidCredentialsException("HTTPCachingProvider: no credentialsprovider that match Authorization scope:" + scope);

            // Invoke the (real) credentials provider
            // using the incoming parameters
            credentials = cp.getCredentials(scope);
            if(credentials == null)
                throw new InvalidCredentialsException("HTTPCachingProvider: cannot obtain credentials");

            // Insert into the credentials cache
            cacheCredentials(scope, credentials);

            if(TESTING)
                System.err.println("Caching credentials: " + credentials);

            return credentials;
        } catch (InvalidCredentialsException ice) {
            HTTPSession.log.debug(ice.getMessage());
            return null;
        }
    }

    public void setCredentials(AuthScope scope, Credentials creds)
    {
        cacheCredentials(scope, creds);
    }

    public void clear()
    {
          cache.clear();
    }

    ///////////////////////////////////////////////////
    // toString

    public String
    toString()
    {
        return "HTTPCachingProvider";
    }


    //////////////////////////////////////////////////
    // Credentials cache

    static public class Auth
    {
        public AuthScope scope;
        public Credentials creds;

        public Auth(AuthScope scope, Credentials creds)
        {
            this.scope = scope;
            this.creds = creds;
        }

        public String toString()
        {
            return "(" + this.scope.toString() + "," + creds.toString() + ")";
        }
    }

    static protected List<Auth> cache = new ArrayList<Auth>();
    static protected List<Auth> testlist = null; // for testing

    /**
     * Insert a credentials into the cache; will return
     * any previous value.
     *
     * @param scope the key for retrieving a credentials object.
     * @param creds the credentials object associated with this key
     * @return the old credentials object if overwriting, else null
     */
    static protected synchronized Credentials
    cacheCredentials(AuthScope scope, Credentials creds)
    {
        Auth p = null;
        Credentials old = null;

        for(Auth t : HTTPCachingProvider.cache) {
            if(t.scope.equals(scope)) {
                p = t;
                break;
            }
        }
        if(p != null) {
            old = p.creds;
            p.creds = creds;
        } else {
            p = new Auth(scope, creds);
            HTTPCachingProvider.cache.add(p);
        }
        return old;
    }

    /**
     * Retrieve a credentials from the cache.
     *
     * @param scope     a key for retrieving a credentials object.
     * @return the matching credentials object, else null
     */
    static synchronized protected Credentials
    checkCache(AuthScope scope)
    {
        Credentials creds = null;
        for(Auth p : HTTPCachingProvider.cache) {
            if(HTTPAuthUtil.equals(p.scope, scope)) {
                creds = p.creds;
                break;
            }
        }
        return creds;
    }

    /**
     * Clear some entries matching the argument
     */
    static synchronized public void // public only to allow testing
    invalidate(AuthScope scope)
    {
        if(TESTING) {
            if(testlist == null) testlist = new ArrayList<Auth>();
        }
        // walk backward because we are removing entries
        for(int i = HTTPCachingProvider.cache.size() - 1; i >= 0; i--) {
            Auth p = HTTPCachingProvider.cache.get(i);
            if(HTTPAuthUtil.equals(scope, p.scope)) {
                if(TESTING) {
                    System.err.println("invalidating: " + p);
                    if(testlist == null)
                        testlist = new ArrayList<Auth>();
                    testlist.add(p);
                }
                HTTPCachingProvider.cache.remove(i);
            }
        }
    }

    static synchronized public void
    clearCache()
    {
        HTTPCachingProvider.cache.clear();
    }

    static synchronized public List<Auth>// for testing
    getCache()
    {
        List<Auth> localcache = new ArrayList<Auth>();
        for(Auth p : HTTPCachingProvider.cache) {
            Auth newp = new Auth(p.scope, p.creds);
            localcache.add(newp);
        }
        return localcache;
    }

    static synchronized public List<Auth> getTestList()
    {
        List<Auth> list = new ArrayList<Auth>();
        list.addAll(testlist);
        if(testlist != null)
            testlist.clear();
        return list;
    }
}

