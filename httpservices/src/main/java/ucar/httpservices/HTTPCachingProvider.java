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
    protected AuthScope authscope = null; // for verification
    protected String principal = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public HTTPCachingProvider(HTTPAuthStore store, AuthScope authscope, String principal)
    {
        this.store = store;
        this.authscope = authscope;
        this.principal = principal;
    }

    //////////////////////////////////////////////////
    // Credentials Provider Interface

    public Credentials
    getCredentials(AuthScope scope)
    {
        // Is this still true in httpclient 4.2.x?
        // There appears to be a bug in HttpMethodDirector such that
        // as long as bad credentials are provided, it will keep on
        // calling the credentials provider.  We fix by checking for
        // retry in same way as HttpMethodDirector.processWWWAuthChallenge.
        // After MAX_RETRIES, we force retries to stop.
        //todo: AuthState authstate = method.getMethod().getHostAuthState();
        //if(retryCount == 0 && authstate.isAuthAttempted() && authscheme.isComplete()) {
        //    return null; // Stop the retry.
        //}
        //retryCount--;

        if(TESTING) {
            System.err.println("HTTPCachingProvider.getCredentials,arg " + scope.toString());
            System.err.println("HTTPCachingProvider.getCredentials,intern " + this.authscope.toString());
            System.err.flush();
        }

/* Remove because of redirects
        // Verify that the scope argument "subsumes"
        // this.authscope
        if(!HTTPAuthScope.subsumes(scope, this.authscope))
            throw new IllegalStateException("HTTPCachingProvider: scope :: authscope mismatch");
*/
        // See if the credentials have been cached.
        Credentials credentials = checkCache(principal, authscope);
        if(credentials != null) {
            if(TESTING)
                System.err.println("Using cached credentials: " + credentials);
            return credentials;
        }

        // Not cached.
        if(TESTING)
            System.err.println("Credentials not cached");

        String scheme = scope.getScheme();

        if(scheme == null)
            throw new IllegalStateException("HTTPCachingProvider: unsupported scheme: " + scope.getScheme());

        // search for matching authstore entries
        List<HTTPAuthStore.Entry> matches = this.store.search(this.principal, this.authscope);
        if(matches.size() == 0)
            throw new IllegalStateException("HTTPCachingProvider: no match for:" + this.authscope);

        // Choose the most restrictive
        HTTPAuthStore.Entry entry = matches.get(0);
        CredentialsProvider provider = entry.provider;

        if(provider == null)
            throw new IllegalStateException("HTTPCachingProvider: no credentials provider provided");

        // Invoke the (real) credentials provider
        // using the incoming parameters
        credentials = provider.getCredentials(scope);
        if(credentials == null)
            throw new IllegalStateException("HTTPCachingProvider: cannot obtain credentials");

        // Insert into the credentials cache
        cacheCredentials(entry.principal, this.authscope, credentials);

        if(TESTING)
            System.err.println("Caching credentials: " + credentials);

        return credentials;
    }

    public void setCredentials(AuthScope scope, Credentials creds)
    {
        cacheCredentials(HTTPAuthStore.ANY_PRINCIPAL, this.authscope, creds);
    }

    public void clear()
    {
    }

    ///////////////////////////////////////////////////
    // toString

    public String
    toString()
    {
        return "HTTPCachingProvider(" + this.authscope + ")";
    }


    //////////////////////////////////////////////////
    // Credentials cache

    static public class Triple
    {
        public String principal;
        public AuthScope scope;
        public Credentials creds;

        public Triple(String principal, AuthScope scope, Credentials creds)
        {
            this.principal = principal;
            this.scope = scope;
            this.creds = creds;
        }

        public String toString()
        {
            return "(" + this.principal + "," + this.scope.toString() + "," + creds.toString() + ")";
        }
    }

    static protected List<Triple> cache = new ArrayList<Triple>();
    static protected List<Triple> testlist = null; // for testing

    /**
     * Insert a credentials into the cache; will return
     * any previous value.
     *
     * @param scope the key for retrieving a credentials object.
     * @param creds the credentials object associated with this key
     * @return the old credentials object if overwriting, else null
     */
    static protected synchronized Credentials
    cacheCredentials(String principal, AuthScope scope, Credentials creds)
    {
        Triple p = null;
        Credentials old = null;

        for(Triple t: HTTPCachingProvider.cache) {
            if(t.scope.equals(scope))  {
                p = t;
                break;
            }
        }
        if(p != null) {
            old = p.creds;
            p.creds = creds;
        } else {
            p = new Triple(principal, scope, creds);
            HTTPCachingProvider.cache.add(p);
        }
        return old;
    }

    /**
     * Retrieve a credentials from the cache.
     *
     * @param principal a key for retrieving a credentials object.
     * @param scope     a key for retrieving a credentials object.
     * @return the matching credentials object, else null
     */
    static synchronized protected Credentials
    checkCache(String principal, AuthScope scope)
    {
        Credentials creds = null;
        for(Triple p : HTTPCachingProvider.cache) {
            if(HTTPAuthScope.identical(p.scope, scope)) {
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
            if(testlist == null) testlist = new ArrayList<Triple>();
        }
        // walk backward because we are removing entries
        for(int i = HTTPCachingProvider.cache.size() - 1;i >= 0;i--) {
            Triple p = HTTPCachingProvider.cache.get(i);
            if(HTTPAuthScope.equivalent(scope, p.scope)) {
                if(TESTING) {
                    System.err.println("invalidating: " + p);
                    if(testlist == null)
                        testlist = new ArrayList<Triple>();
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

    static synchronized public List<Triple>// for testing
    getCache()
    {
        List<Triple> localcache = new ArrayList<Triple>();
        for(Triple p : HTTPCachingProvider.cache) {
            Triple newp = new Triple(p.principal, p.scope, p.creds);
            localcache.add(newp);
        }
        return localcache;
    }

    static synchronized public List<Triple> getTestList()
    {
        List<Triple> list = new ArrayList<Triple>();
        list.addAll(testlist);
        if(testlist != null)
            testlist.clear();
        return list;
    }
}

