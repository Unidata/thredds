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

import net.jcip.annotations.NotThreadSafe;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.client.protocol.*;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.*;
import org.apache.http.entity.StringEntity;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;

import static org.apache.http.auth.AuthScope.*;
import static ucar.nc2.util.net.HTTPAuthScope.*;

/**
 * A session is encapsulated in an instance of the class
 * HTTPSession.  The encapsulation is with respect to a specific url
 * This means that once a session is
 * specified, it is tied permanently to that url.
 * <p/>
 * <p/>
 * It is important to note that Session objects do NOT correspond
 * with the HttpClient objects of the Apache httpclient library.
 * A Session does, however, encapsulate an instance of an Apache HttpClient.
 * <p/>
 * It is possible to specify a url when invoking, for example,
 * HTTPFactory.Get.  This is because the url argument to the
 * HTTPSession constructor actually serves two purposes.  First, if
 * the method is created without specifying a url, then the session
 * url is used to specify the data to be retrieved by the method
 * invocation.  Second, if the method is created and specifies a
 * url, for example, HTTPMethod m = HTTPFactory.Get(session,url2);
 * this second url is used to specify the data to be retrieved by
 * the method invocation.  This might (and does) occur if, for
 * example, the url given to HTTPSession represented some general
 * url such as http://motherlode.ucar.edu/path/file.nc and the url
 * given to HTTPFactory.Get was for something more specific such as
 * http://motherlode.ucar.edu/path/file.nc.dds.
 * <p/>
 * The important point is that in this second method, the url must
 * be "compatible" with the session url.  The term "compatible"
 * basically means that the HTTPSession url, as a string, must be a
 * prefix of the url given to HTTPFactory.Get. This maintains the
 * semantics of the Session but allows flexibility in accessing data
 * from the server.
 * <p/>
 * Note that the term legalurl means that the url has reserved
 * characters within identifieers in escaped form. This is
 * particularly and issue for queries. Especially: ?x[0:5] is legal
 * and the square brackets need not be encoded.
 * <p/>
 * Finally, note that if the session was created with no url then all method
 * constructions must specify a url.
 */

@NotThreadSafe
public class HTTPSession
{
    //////////////////////////////////////////////////
    // Constants

    // Define all the legal properties
    // From class AllClientPNames
    // Use aliases because in httpclietn 4.3, AllClientPNames is deprecated

    static final String ALLOW_CIRCULAR_REDIRECTS = AllClientPNames.ALLOW_CIRCULAR_REDIRECTS;
    static final String HANDLE_REDIRECTS = AllClientPNames.HANDLE_REDIRECTS;
    static final String HANDLE_AUTHENTICATION = AllClientPNames.HANDLE_AUTHENTICATION;
    static final String MAX_REDIRECTS = AllClientPNames.MAX_REDIRECTS;
    static final String SO_TIMEOUT = AllClientPNames.SO_TIMEOUT;
    static final String CONN_TIMEOUT = AllClientPNames.CONNECTION_TIMEOUT;
    static final String USER_AGENT = AllClientPNames.USER_AGENT;
    static final String PROXY = AllClientPNames.DEFAULT_PROXY;
    static final String COMPRESSION = "COMPRESSION";

    // from: http://en.wikipedia.org/wiki/List_of_HTTP_header_fields
    static final public String HEADER_USERAGENT = "User-Agent";
    static final public String ACCEPT_ENCODING = "Accept-Encoding";

    static final public String BASIC = HTTPAuthPolicy.BASIC;
    static final public String DIGEST = HTTPAuthPolicy.DIGEST;
    static final public String NTLM = HTTPAuthPolicy.NTLM;
    static final public String SSL = HTTPAuthPolicy.SSL;

    static final int DFALTTHREADCOUNT = 50;
    static final int DFALTREDIRECTS = 25;
    static final int DFALTCONNTIMEOUT = 1 * 60 * 1000; // 1 minutes (60000 milliseconds)
    static final int DFALTSOTIMEOUT = 5 * 60 * 1000; // 5 minutes (300000 milliseconds)
    static final String DFALTUSERAGENT = "/NetcdfJava/HttpClient4.3";

    //////////////////////////////////////////////////////////////////////////
    // Type Declarations

    // Provide an alias for HttpParams
    static class Settings extends SyncBasicHttpParams
    {
    }

    static class Proxy
    {
        public String host = null;
        public int port = -1;
    }

    static enum Methods
    {
        Get("get"), Head("head"), Put("put"), Post("post"), Options("options");
        private final String name;

        Methods(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }
    }

    // Define a Retry Handler that supports specifiable retries
    // and is optionally verbose.
    static public class RetryHandler
        implements org.apache.http.client.HttpRequestRetryHandler
    {
        static final int DFALTRETRIES = 5;
        static int retries = DFALTRETRIES;
        static boolean verbose = false;

        public RetryHandler()
        {
        }

        public boolean
        retryRequest(IOException exception,
                     int executionCount,
                     HttpContext context)
        {
            if(verbose) {
                HTTPSession.log.debug(String.format("Retry: count=%d exception=%s\n", executionCount, exception.toString()));
            }
            if(executionCount >= retries)
                return false;
            if((exception instanceof InterruptedIOException) // Timeout
                || (exception instanceof UnknownHostException)
                || (exception instanceof ConnectException) // connection refused
                || (exception instanceof SSLException)) // ssl handshake problem
                return false;
            HttpRequest request
                = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if(idempotent) // Retry if the request is considered idempotent
                return true;

            return false;
        }

        static public synchronized int getRetries()
        {
            return RetryHandler.retries;
        }

        static public synchronized void setRetries(int retries)
        {
            if(retries > 0)
                RetryHandler.retries = retries;
        }

        static public synchronized boolean getVerbose()
        {
            return RetryHandler.verbose;
        }

        static public synchronized void setVerbose(boolean tf)
        {
            RetryHandler.verbose = tf;
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // Static variables

    static public org.slf4j.Logger log
        = org.slf4j.LoggerFactory.getLogger(HTTPSession.class);

    static PoolingClientConnectionManager connmgr;

    // Define a set of settings to hold all the
    // settable values; there will be one
    // instance for global and one for local.

    static Settings globalsettings;

    static {
        connmgr = new PoolingClientConnectionManager();
        connmgr.getSchemeRegistry().register(
            new Scheme("https", 8443,
                new CustomSSLProtocolSocketFactory()));
        connmgr.getSchemeRegistry().register(
            new Scheme("https", 443,
                new CustomSSLProtocolSocketFactory()));
        globalsettings = new Settings();
        setDefaults(globalsettings);
        setGlobalUserAgent(DFALTUSERAGENT);
        setGlobalThreadCount(DFALTTHREADCOUNT);
        setGlobalConnectionTimeout(DFALTCONNTIMEOUT);
        setGlobalSoTimeout(DFALTSOTIMEOUT);
        getGlobalProxyD(); // get info from -D if possible
        setGlobalKeyStore();
        setGlobalCompression();
    }

    //////////////////////////////////////////////////////////////////////////
    // Static Methods (Mostly global accessors)

    /// Provide defaults for a settings map
    static void setDefaults(Settings props)
    {
        props.setParameter(ALLOW_CIRCULAR_REDIRECTS, Boolean.TRUE);
        props.setParameter(HANDLE_REDIRECTS, Boolean.TRUE);
        props.setParameter(HANDLE_AUTHENTICATION, Boolean.TRUE);
        props.setParameter(MAX_REDIRECTS, (Integer) DFALTREDIRECTS);
        props.setParameter(SO_TIMEOUT, (Integer) DFALTSOTIMEOUT);
        props.setParameter(CONN_TIMEOUT, (Integer) DFALTCONNTIMEOUT);
        props.setParameter(USER_AGENT, DFALTUSERAGENT);
    }

    public Settings getGlobalSettings()
    {
        return globalsettings;
    }

    static synchronized public void setGlobalUserAgent(String userAgent)
    {
        globalsettings.setParameter(USER_AGENT, userAgent);
    }

    static public String getGlobalUserAgent()
    {
        return (String) globalsettings.getParameter(USER_AGENT);
    }

    static synchronized public void setGlobalThreadCount(int nthreads)
    {
        connmgr.setMaxTotal(nthreads);
        connmgr.setDefaultMaxPerRoute(nthreads);
    }

    // Alias
    static public void setGlobalMaxConnections(int nthreads)
    {
        setGlobalThreadCount(nthreads);
    }

    static public int getGlobalThreadCount()
    {
        return connmgr.getMaxTotal();
    }

    static public List<Cookie> getGlobalCookies()
    {
        AbstractHttpClient client = new DefaultHttpClient(connmgr);
        List<Cookie> cookies = client.getCookieStore().getCookies();
        return cookies;
    }

    // Timeouts

    static synchronized public void setGlobalConnectionTimeout(int timeout)
    {
        if(timeout >= 0) globalsettings.setParameter(CONN_TIMEOUT, (Integer) timeout);
    }

    static synchronized public void setGlobalSoTimeout(int timeout)
    {
        if(timeout >= 0) globalsettings.setParameter(SO_TIMEOUT, (Integer) timeout);
    }

    // Proxy

    static synchronized public void
    setGlobalProxy(String host, int port)
    {
        Proxy proxy = new Proxy();
        proxy.host = host;
        proxy.port = port;
        globalsettings.setParameter(PROXY, proxy);
    }

    // Misc.

    static synchronized public void
    setGlobalCompression()
    {
        globalsettings.setParameter(COMPRESSION, "gzip,deflate");
    }

    // Authorization

    static synchronized protected void
    defineCredentialsProvider(String principal, AuthScope scope, CredentialsProvider provider, HTTPAuthStore store)
    {
        // Add/remove entry to AuthStore
        try {
            if(provider == null) {//remove
                store.remove(new HTTPAuthStore.Entry(principal, scope, provider));
            } else { // add
                store.insert(new HTTPAuthStore.Entry(principal, scope, provider));
            }
        } catch (HTTPException he) {
            log.error("HTTPSession.setCredentialsProvider failed");
        }
    }

    static public void
    setGlobalCredentialsProvider(AuthScope scope, CredentialsProvider provider)
    {
        defineCredentialsProvider(ANY_PRINCIPAL, scope, provider, HTTPAuthStore.DEFAULTS);
    }

    static public void
    setGlobalCredentialsProvider(CredentialsProvider provider)
    {
        defineCredentialsProvider(ANY_PRINCIPAL, HTTPAuthScope.ANY, provider, HTTPAuthStore.DEFAULTS);
    }

    // It is convenient to be able to directly set the Credentials
    // (not the provider) when those credentials are fixed.
    static public void
    setGlobalCredentials(AuthScope scope, Credentials creds)
    {
        CredentialsProvider provider = new HTTPCredsProvider(creds);
        setGlobalCredentialsProvider(scope, provider);
    }

    static public int
    getRetryCount()
    {
        return RetryHandler.getRetries();
    }

    static public void
    setRetryCount(int count)
    {
        RetryHandler.setRetries(count);
    }


    //////////////////////////////////////////////////
    // Static Utility functions

    static public String getCanonicalURL(String legalurl)
    {
        if(legalurl == null) return null;
        int index = legalurl.indexOf('?');
        if(index >= 0) legalurl = legalurl.substring(0, index);
        // remove any trailing extension
        //index = legalurl.lastIndexOf('.');
        //if(index >= 0) legalurl = legalurl.substring(0,index);
        return canonicalpath(legalurl);
    }

    /**
     * Convert path to use '/' consistently and
     * to remove any trailing '/'
     *
     * @param path convert this path
     * @return canonicalized version
     */
    static public String canonicalpath(String path)
    {
        if(path == null) return null;
        path = path.replace('\\', '/');
        if(path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        return path;
    }

    static public String
    removeprincipal(String u)
    {
        // Must be a simpler way
        String newurl = null;
        try {
            int index;
            URL url = new URL(u);
            String protocol = url.getProtocol() + "://";
            String host = url.getHost();
            int port = url.getPort();
            String path = url.getPath();
            String query = url.getQuery();
            String ref = url.getRef();

            String sport = (port <= 0 ? "" : (":" + port));
            path = (path == null ? "" : path);
            query = (query == null ? "" : "?" + query);
            ref = (ref == null ? "" : "#" + ref);

            // rebuild the url
            // (and leaving encoding in place)
            newurl = protocol + host + sport + path + query + ref;

        } catch (MalformedURLException use) {
            newurl = u;
        }
        return newurl;
    }

    static public String
    getUrlAsString(String url) throws HTTPException
    {
        HTTPSession session = HTTPFactory.newSession(url);
        HTTPMethod m = HTTPFactory.Get(session);
        int status = m.execute();
        String content = null;
        if(status == 200) {
            content = m.getResponseAsString();
        }
        m.close();
        return content;
    }

    static public int
    putUrlAsString(String content, String url) throws HTTPException
    {
        HTTPMethod m = HTTPFactory.Put(url);
        try {
            m.setRequestContent(new StringEntity(content, "application/text", "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            throw new HTTPException(uee);
        }
        int status = m.execute();
        m.close();
        return status;
    }

    static String
    getstorepath(String prefix)
    {
        String path = System.getProperty(prefix + "store");
        if(path != null) {
            path = path.trim();
            if(path.length() == 0) path = null;
        }
        return path;
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
    cleanproperty(String property)
    {
        String value = System.getProperty(property);
        if(value != null) {
            value = value.trim();
            if(value.length() == 0) value = null;
        }
        return value;
    }

    // Provide for backward compatibility
    // through the -D properties

    static synchronized void
    setGlobalKeyStore()
    {
        String keypassword = cleanproperty("keystorepassword");
        String keypath = cleanproperty("keystore");
        String trustpassword = cleanproperty("truststorepassword");
        String trustpath = cleanproperty("truststore");

        if(keypath != null || trustpath != null) { // define conditionally
            HTTPSSLProvider sslprovider = new HTTPSSLProvider(keypath, keypassword,
                trustpath, trustpassword);
            setGlobalCredentialsProvider(
                new AuthScope(ANY_HOST, ANY_PORT, ANY_REALM, HTTPAuthPolicy.SSL),
	        sslprovider);
        }
    }

    // For backward compatibility, provide
    // programmatic access for setting proxy info
    // Extract proxy info from command line -D parameters
    // extended 5/7/2012 to get NTLM domain
    // H/T: nick.bower@metoceanengineers.com
    static void
    getGlobalProxyD()
    {
        Proxy proxy = new Proxy();
        String host = System.getProperty("http.proxyHost");
        String port = System.getProperty("http.proxyPort");
        int portno = -1;

        if(host != null) {
            host = host.trim();
            if(host.length() == 0) host = null;
        }
        if(port != null) {
            port = port.trim();
            if(port.length() > 0) {
                try {
                    portno = Integer.parseInt(port);
                } catch (NumberFormatException nfe) {
                    portno = -1;
                }
            }
        }

        if(host != null)
            setGlobalProxy(host, portno);
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected AbstractHttpClient sessionClient = null;
    protected List<ucar.nc2.util.net.HTTPMethod> methodList = new Vector<HTTPMethod>();
    protected String identifier = "Session";
    protected String legalurl = null;
    protected boolean closed = false;
    protected HttpContext execcontext = null; // same instance must be used for all methods
    protected Settings localsettings = new Settings();
    protected HTTPAuthStore authlocal = new HTTPAuthStore();

    //////////////////////////////////////////////////
    // Constructor(s)

    public HTTPSession()
        throws HTTPException
    {
        this(null);
    }

    public HTTPSession(String url)
        throws HTTPException
    {
        try {
            new URL(url);
        } catch (MalformedURLException mue) {
            throw new HTTPException("Malformed URL: " + url, mue);
        }
        this.legalurl = url;
        try {
            sessionClient = new DefaultHttpClient(connmgr);
            if(TESTING) HTTPSession.track(this);
            setInterceptors(this);
        } catch (Exception e) {
            throw new HTTPException("url=" + url, e);
        }
        this.execcontext = new BasicHttpContext();// do we need to modify?
    }

    //////////////////////////////////////////////////
    // Accessor(s)

    public Settings getSettings()
    {
        return localsettings;
    }

    public String getURL()
    {
        return this.legalurl;
    }

    public void setUserAgent(String agent)
    {
        if(agent != null)
            localsettings.setParameter(USER_AGENT, agent);
    }

    public void setSoTimeout(int timeout)
    {
        if(timeout >= 0) localsettings.setParameter(SO_TIMEOUT, timeout);
    }

    public void setConnectionTimeout(int timeout)
    {
        if(timeout >= 0) localsettings.setParameter(CONN_TIMEOUT, timeout);
    }

    public void setMaxRedirects(int n)
    {
        localsettings.setParameter(MAX_REDIRECTS, n);
    }

    public void setCompression()
    {
        localsettings.setParameter(COMPRESSION, "gzip,deflate");
    }


    // make package specific

    HttpContext
    getContext()
    {
        return this.execcontext;
    }


    HttpClient
    getClient()
    {
        return this.sessionClient;
    }

    HTTPAuthStore
    getAuthStore()
    {
        return this.authlocal;
    }

    HttpContext
    getExecutionContext()
    {
        return this.execcontext;
    }

    //////////////////////////////////////////////////

    /**
     * Close the session. This implies closing
     * any open methods.
     */

    synchronized public void close()
    {
        if(closed)
            return; // multiple calls ok
        while(methodList.size() > 0) {
            HTTPMethod m = methodList.get(0);
            m.close(); // forcibly close; will invoke removemethod().
        }
        closed = true;
    }

    public List<Cookie> getCookies()
    {
        if(sessionClient == null)
            return null;
        List<Cookie> cookies = sessionClient.getCookieStore().getCookies();
        return cookies;
    }

    synchronized void addMethod(HTTPMethod m)
    {
        if(!methodList.contains(m))
            methodList.add(m);
    }

    synchronized void removeMethod(HTTPMethod m)
    {
        methodList.remove(m);
    }

    public void clearState()
    {
        sessionClient.getCredentialsProvider().clear();
        sessionClient.getCookieStore().clear();
        execcontext = new BasicHttpContext();
        localsettings.clear();
        authlocal.clear();
    }

    //////////////////////////////////////////////////
    // Possibly authenticating proxy

    // All proxy activity goes thru here
    void
    setProxy(Proxy proxy)
    {
        if(sessionClient == null) return;
        if(proxy != null && proxy.host != null)
            localsettings.setParameter(PROXY, proxy);
    }

    //////////////////////////////////////////////////
    // External API

    public void
    setProxy(String host, int port)
    {
        Proxy proxy = new Proxy();
        proxy.host = host;
        proxy.port = port;
        setProxy(proxy);
    }

    //////////////////////////////////////////////////
    // Authorization
    // per-session versions of the global accessors

    public void
    setCredentialsProvider(AuthScope scope, CredentialsProvider provider)
    {
        defineCredentialsProvider(ANY_PRINCIPAL, scope, provider, this.authlocal);
    }

    public void
    setCredentialsProvider(CredentialsProvider provider)
    {
        setCredentialsProvider(HTTPAuthScope.ANY, provider);
    }

    public void
    setCredentialsProvider(String scheme, CredentialsProvider provider)
    {
        AuthScope scope = new AuthScope(ANY_HOST,ANY_PORT,ANY_REALM,scheme);
        setCredentialsProvider(scope, provider);
    }

    public void
    setCredentials(String scheme, Credentials creds)
    {
        CredentialsProvider provider = new HTTPCredsProvider(creds);
        setCredentialsProvider(scheme, provider);
    }

    // Assumes that user info exists in the url and we can
    // use it to build a simple UsernamePasswordCredentials as our provider.
    // Also assume this is a compatible url to the Session url
    public void
    setCredentialsProvider(String surl)
        throws HTTPException
    {
        // Try to extract user info
        URI uri = HTTPAuthScope.decompose(surl);
        String userinfo = uri.getUserInfo();
        if(userinfo != null) {
            int index = userinfo.indexOf(':');
            String user = userinfo.substring(index);
            String pwd = userinfo.substring(index + 1, userinfo.length());
            if(user != null && pwd != null) {
                // Create a non-interactive user+pwd handler
                CredentialsProvider bp = new HTTPBasicProvider(user, pwd);
                setCredentialsProvider(HTTPAuthPolicy.BASIC, bp);
            }
        }
    }

    //////////////////////////////////////////////////
    // Testing support

    // Expose the state for testing purposes
    public boolean isClosed()
    {
        return this.closed;
    }

    public int getMethodcount()
    {
        return methodList.size();
    }

    // This provides support for HTTPMethod.setAuthentication method
    synchronized protected void
    setAuthentication(HTTPCredentialsCache hap)
    {
        this.sessionClient.setCredentialsProvider(hap);
        if(false)
            this.execcontext.setAttribute(ClientContext.CREDS_PROVIDER, hap);
    }

    // do an actual execution
    protected HttpResponse
    execute(HttpRequestBase request)
        throws IOException
    {
        if(false)
            return sessionClient.execute(request, this.execcontext);
        else
            return sessionClient.execute(request);
    }

    /*package*/ void invalidate(AuthScope scope)
    {
        HTTPCredentialsCache.invalidate(scope);
    }

    //////////////////////////////////////////////////
    // Debug interface

    // Provide a way to kill everything at the end of a Test

    // When testing, we need to be able to clean up
    // all existing sessions because JUnit can run all
    // test within a single jvm.
    static List<HTTPSession> sessionList = null; // List of all HTTPSession instances
    // only used when testing flag is set
    static public boolean TESTING = true; // set to true during testing, should be false otherwise


    static protected synchronized void kill()
    {
        if(sessionList != null) {
            for(HTTPSession session : sessionList) {
                session.close();
            }
            sessionList.clear();
            // Rebuild the connection manager
            connmgr.shutdown();
            connmgr = new PoolingClientConnectionManager();
            setGlobalThreadCount(DFALTTHREADCOUNT);
        }
    }

    // If we are testing, then track the sessions for kill
    static protected synchronized void track(HTTPSession session)
    {
        if(sessionList == null)
            sessionList = new ArrayList<HTTPSession>();
        sessionList.add(session);
    }


    static protected HttpRequestInterceptor globaldebugrequest = null;
    static protected HttpResponseInterceptor globaldebugresponse = null;

    protected HttpRequestInterceptor localdebugrequest = null;
    protected HttpResponseInterceptor localdebugresponse = null;

    synchronized static public void
    setInterceptors(HTTPSession session)
    {
        HttpRequestInterceptor ireq = session.localdebugrequest;
        if(ireq == null)
            ireq = globaldebugrequest;
        if(ireq != null) {
            if(session != null) {
                AbstractHttpClient client = (AbstractHttpClient) session.getClient();
                client.addRequestInterceptor(ireq);
            }
        }
        HttpResponseInterceptor hi = session.localdebugresponse;
        if(hi == null)
            hi = globaldebugresponse;
        if(hi != null) {
            if(session != null) {
                AbstractHttpClient client = (AbstractHttpClient) session.getClient();
                client.addResponseInterceptor(hi);
            }
        }

    }

    static public void debugGlobal(HttpRequestInterceptor ireq,
                                   HttpResponseInterceptor iresp)
    {
        globaldebugrequest = ireq;
        globaldebugresponse = iresp;
    }

    public void debugSession(HttpRequestInterceptor ireq,
                             HttpResponseInterceptor iresp)
    {
        localdebugrequest = ireq;
        localdebugresponse = iresp;
    }

    static public void debugHeaders()
    {
        debugGlobal(new HTTPUtil.RequestHeaderDump(), new HTTPUtil.ResponseHeaderDump());
    }

}
