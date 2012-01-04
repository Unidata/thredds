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
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.protocol.Protocol;
import ucar.nc2.util.log.LogStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


@NotThreadSafe
public class HTTPSession
{
// Convenience
static final public HTTPAuthScheme BASIC = HTTPAuthScheme.BASIC;
static final public HTTPAuthScheme DIGEST = HTTPAuthScheme.DIGEST;
static final public HTTPAuthScheme SSL = HTTPAuthScheme.SSL;
static final public HTTPAuthScheme PROXY = HTTPAuthScheme.PROXY;

static public int SC_NOT_FOUND = HttpStatus.SC_NOT_FOUND;
static public int SC_UNAUTHORIZED = HttpStatus.SC_UNAUTHORIZED;
static public int SC_OK = HttpStatus.SC_OK;
static public String CONNECTION_TIMEOUT = HttpConnectionParams.CONNECTION_TIMEOUT;
static public String SO_TIMEOUT = HttpMethodParams.SO_TIMEOUT;

static public String ALLOW_CIRCULAR_REDIRECTS = HttpClientParams.ALLOW_CIRCULAR_REDIRECTS;
static public String MAX_REDIRECTS = HttpClientParams.MAX_REDIRECTS;
static public String USER_AGENT = HttpMethodParams.USER_AGENT;
static public String PROTOCOL_VERSION = HttpMethodParams.PROTOCOL_VERSION;
static public String VIRTUAL_HOST = HttpMethodParams.VIRTUAL_HOST;
static public String USE_EXPECT_CONTINUE = HttpMethodParams.USE_EXPECT_CONTINUE;
static public String STRICT_TRANSFER_ENCODING = HttpMethodParams.STRICT_TRANSFER_ENCODING;
static public String HTTP_ELEMENT_CHARSET = HttpMethodParams.HTTP_ELEMENT_CHARSET;
static public String HTTP_CONTENT_CHARSET = HttpMethodParams.HTTP_CONTENT_CHARSET;

/*fix:*/
static public String HTTP_CONNECTION = "<undefined>";
static public String HTTP_PROXY_HOST = "<undefined>";
static public String HTTP_REQ_SENT = "<undefined>";
static public String HTTP_REQUEST = "<undefined>";
static public String HTTP_RESPONSE = "<undefined>";
static public String HTTP_TARGET_HOST = "<undefined>";
static public String ORIGIN_SERVER = "<undefined>";
static public String WAIT_FOR_CONTINUE = "<undefined>";

static int DFALTTHREADCOUNT = 50;
static int DFALTTIMEOUT = 5*60*1000; // 5 minutes (300000 milliseconds)

static MultiThreadedHttpConnectionManager connmgr;
//fix: protected static SchemeRegistry schemes;
static String globalAgent = "/NetcdfJava/HttpClient3";
static int threadcount = DFALTTHREADCOUNT;
static List<HTTPSession> sessionList; // List of all HTTPSession instances
static boolean globalauthpreemptive = false;
static CredentialsProvider globalProvider = null;
static int globalSoTimeout = 0;
static int globalConnectionTimeout = 0;
static String globalsimpleproxyhost = null;
static int globalsimpleproxyport = 0;

static {
    //fix: schemes = new SchemeRegistry();
    //fix: schemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(),80));
    connmgr = new MultiThreadedHttpConnectionManager();
    setGlobalThreadCount(DFALTTHREADCOUNT);
    // Fill in the scheme registry for at least http and https
    // allow self-signed certificates
    Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
    sessionList = new ArrayList<HTTPSession>(); // see kill function
    setGlobalConnectionTimeout(DFALTTIMEOUT);
    setGlobalSoTimeout(DFALTTIMEOUT);
    setGlobalSimpleProxy();
    setGlobalKeyStore();
}

// ////////////////////////////////////////////////////////////////////////

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

static synchronized public void setGlobalUserAgent(String _userAgent)
{
    globalAgent = _userAgent;
}

static public String getGlobalUserAgent()
{
    return globalAgent;
}

static public void setGlobalThreadCount(int nthreads)
{
    connmgr.getParams().setMaxTotalConnections(nthreads);
    connmgr.getParams().setDefaultMaxConnectionsPerHost(nthreads);
}

// Alias
static public void setGlobalMaxConnections(int nthreads)
{
    setGlobalThreadCount(nthreads);
}

static public int getGlobalThreadCount()
{
    return connmgr.getParams().getMaxTotalConnections();
}


static public Cookie[] getGlobalCookies()
{
    HttpClient client = new HttpClient(connmgr);
    Cookie[] cookies = client.getState().getCookies();
    return cookies;
}

// Provide a way to kill everything at the end of a Test

static private synchronized void kill()
{
    for (HTTPSession session : sessionList) {
        session.close();
    }
    sessionList.clear();
    // Rebuild the connection manager
    connmgr.shutdown();
    connmgr = new MultiThreadedHttpConnectionManager();
    setGlobalThreadCount(DFALTTHREADCOUNT);

}

////////////////////////////////////////////////////////////////////////

HttpClient sessionClient = null;
List<ucar.nc2.util.net.HTTPMethod> methodList = new Vector<HTTPMethod>();
HttpState context = null;
boolean closed = false;
String identifier = "Session";
String useragent = null;
CredentialsProvider sessionProvider = null;
String legalurl = null;

/**
 * A session is encapsulated in an instance of the class HTTPSession.
 * The encapsulation is with respect to a specific url and (optionally) principal.
 * This means that once a session is specified, it is tied permanently to that url and principal.
 * Note that the term "principal" is another name for user id.
 * If no principal is ever set, the the session assumes a special principal called ANY_PRINCIPAL.
 * Currently principals are ignored, but will become important when new authorization mechanisms are put in place.
 * Also, per-instance setting of principals is disabled because the Apache httpclient-3 library cannot
 * utilize them. They will be activated when the httpclient-4 library replaces the httpclient-3 library.
 * It is important to note that Session objects do NOT correspond with the HttpClient objects
 * of the Apache httpclient library.
 * A Session encapulates an instance of an Apache HttpClient,
 * but Sessions also wrap and control httpclient library methods such as GetMethod via the class HTTPMethod.
 * This is so it can ensure that the Session - url correspondence is not violated.
 *
 * It is possible to specify a url when invoking, for example, HTTPMethod.Get.
 * This is because the url argument to the HTTPSession constructor actually serves two purposes.
 * First, if the method is created without specifying a url, then the session url is used to specify
 * the data to be retrieved by the method invocation.
 * Second, if the method is created and specifies a url, for example,
 *        HTTPMethod m = HTTPMethod.Get(session,url2);
 * this second url is used to specify the data to be retrieved by the method invocation.
 * This might (and does) occur if, for example, the url given to HTTPSession represented
 * some general url such as http://motherlode.ucar.edu/path/file.nc and the url given to
 * HTTPMethod.Get was for something more specific such as http://motherlode.ucar.edu/path/file.nc.dds.
 *
 * The important point is that this second, method, url must be "compatible" with the session url.
 * The term "compatible" basically means that the HTTPSession url, as a string, must be a prefix
 * of the url given to HTTPMethod.Get. This maintains the semantics of the Session but allows flexibility
 * in accessing data from the server.
 *
 * Note that if the session was created with no url
 * then all method constructions must specify a url.
 *
 * Note: the term legalurl means that the url has reserved characters
 * within identifieers in escaped form. This is particularly and issue
 * for queries. Especially: ?x[0:5] is legal and the square brackets
 * need not be encoded.
 */

public HTTPSession(String legalurl) throws ucar.nc2.util.net.HTTPException
{
    construct(legalurl);
}

public HTTPSession()
        throws HTTPException
{
    construct(null);
}

protected void construct(String legalurl)
    throws HTTPException
{
    this.legalurl = legalurl;
    try {
        sessionClient = new HttpClient(connmgr);
        HttpClientParams clientparams = sessionClient.getParams();

        // Allow (circular) redirects
        clientparams.setParameter(ALLOW_CIRCULAR_REDIRECTS, true);
        clientparams.setParameter(MAX_REDIRECTS, 25);

        if(globalSoTimeout > 0)
            setSoTimeout(globalSoTimeout);
        if(globalConnectionTimeout > 0)
            setConnectionTimeout(globalConnectionTimeout);

        setAuthenticationPreemptive(globalauthpreemptive);

        setSimpleProxy();

        sessionList.add(this);

    } catch (Exception e) {
        throw new HTTPException(e);
    }
}

public String getURL() { return this.legalurl;} // alias function

public void setUserAgent(String agent)
{
    useragent = agent;
    if (useragent != null && sessionClient != null)
        sessionClient.getParams().setParameter(USER_AGENT, useragent);
}

public void setAuthenticationPreemptive(boolean tf)
{
    if (sessionClient != null)
        sessionClient.getParams().setAuthenticationPreemptive(tf);
}


public void setSoTimeout(int timeout)
{
    sessionClient.getParams().setSoTimeout(timeout);
}

public void setConnectionTimeout(int timeout)
{
    sessionClient.setConnectionTimeout(timeout);
}


//fix: public void setStateX(HttpState cxt) {sessionState = cxt;}

synchronized public void close()
{
    closed = true;
    if (methodList != null)
        for (HTTPMethod m : methodList) {
            m.close();
            removeMethod(m);
        }
}

public String getCookiePolicy()
{
    return sessionClient == null ? null : sessionClient.getParams().getCookiePolicy();
}

public Cookie[] getCookies()
{
    if (sessionClient == null)
        return null;
    Cookie[] cookies = sessionClient.getState().getCookies();
    return cookies;
}

protected synchronized void addMethod(HTTPMethod m)
{
    if (!methodList.contains(m))
        methodList.add(m);
}

protected synchronized void removeMethod(HTTPMethod m)
{
    if (!closed)
        methodList.remove(m);
}

public void setMaxRedirects(int n)
{
    HttpClientParams clientparams = sessionClient.getParams();
    clientparams.setParameter(MAX_REDIRECTS, n);
}

public void setContext(HttpState cxt)
{
    context = cxt;
}

public HttpState getContext()
{
    return context;
}


public void clearState()
{
    sessionClient.getState().clearCookies();
    sessionClient.getState().clearCredentials();
}


// Define some utility functions

static public String getCanonicalURL(String legalurl)
{
    if(legalurl == null) return null;
    int index = legalurl.indexOf('?');
    if(index >= 0) legalurl = legalurl.substring(0,index);
    // remove any trailing extension
    //index = legalurl.lastIndexOf('.');
    //if(index >= 0) legalurl = legalurl.substring(0,index);
    return canonicalpath(legalurl);
}
/**
 * Convert path to use '/' consistently and
 * to remove any trailing '/'
 *
 * @param path
 * @return
 */
static public String canonicalpath(String path)
{
    if(path == null) return null;
    path = path.replace('\\','/');
    if(path.endsWith("/"))
        path = path.substring(0,path.length()-1);
    return path;
}

static public String
removeprincipal(String u)
{
    return setPrincipal(u,"");
}

static public String
setPrincipal(String u, String p)
{

    String newurl = null;
    try {
        URL url = new URL(u);
        String protocol = url.getProtocol();
        String principal = (p == null ? url.getUserInfo() : p);
        String host = url.getHost();
        int port = url.getPort();
        String path = url.getPath();
        String query = url.getQuery();
        String ref = url.getRef();
        if(principal != null && principal.length() == 0) principal = null;
        if(path != null && path.length() == 0) path = null;
        if(query != null && query.length() == 0) query = null;
        if(ref != null && ref.length() == 0) ref = null;

        protocol = protocol + "://";
        principal = (principal == null ? "" : (principal + "@"));
        String sport = (port <= 0 ? "" : (":"+port));
        path = (path ==  null ? "" : path);
        query = (query == null ? "" : "?" + query);
        ref = (ref == null ? "" : "#" + ref);

        // rebuild the url
        // (and leaving encoding in place)
        newurl =   protocol + principal + host + sport + path + query + ref;
    } catch (MalformedURLException use) {newurl=u;}
    return newurl;
}

static public String
getUrlAsString(String url) throws HTTPException
{
    HTTPSession session = new HTTPSession(url);
    HTTPMethod m = HTTPMethod.Get(session);
    int status = m.execute();
    String content = null;
    if (status == 200) {
        content = m.getResponseAsString();
    }
    m.close();
    return content;
}

static public int
putUrlAsString(String content, String url) throws HTTPException
{
    HTTPSession session = new HTTPSession(url);
    HTTPMethod m = HTTPMethod.Put(session);
    m.setRequestContentAsString(content);
    int status = m.execute();
    m.close();
    return status;
}

/////////////////////////////////////////////
// Timeouts

static public void setConnectionManagerTimeout(int timeout)
{
    setGlobalConnectionTimeout(timeout);
}

static public void setGlobalConnectionTimeout(int timeout)
{
    connmgr.getParams().setConnectionTimeout(timeout);

}

static public void setGlobalSoTimeout(int timeout)
{
    globalSoTimeout = timeout;
}


//////////////////////////////////////////////////
// Simple (not authenticating proxy support)

// For backward compatibility, provide
// programmatic access for setting proxy info

// Extract proxy info from command line -D parameters
// H/T: nick.bower@metoceanengineers.com
static void
setGlobalSimpleProxy()
{
    String host = System.getProperty("http.proxyHost");
    String port = System.getProperty("http.proxyPort");
    if(host != null && port != null) {
        host = host.trim();
        if(host.length() == 0) host = null;
        int portno = 0;
        if(port != null) {
            port = port.trim();
            if(port.length() > 0) {
                try {
                    portno = Integer.parseInt(port);
                } catch (NumberFormatException nfe) {portno = 0;}
            }
        }
        setGlobalSimpleProxy(host,portno);
    }
}

void
setSimpleProxy()
{
    if(globalsimpleproxyhost == null) return;
    setSimpleProxy(globalsimpleproxyhost,globalsimpleproxyport);
}

// These are externally visible

static synchronized public void
setGlobalSimpleProxy(String proxyhost, int proxyport)
{
    globalsimpleproxyhost = proxyhost;
    globalsimpleproxyport = proxyport;
}

public void
setSimpleProxy(String host, int port)
{
    if(sessionClient == null) return;
    sessionClient.getHostConfiguration().setProxy(host, port);
}

//////////////////////////////////////////////////
// Authorization

static synchronized
public void setGlobalAuthenticationPreemptive(boolean tf)
{
    globalauthpreemptive = tf;
}

static public void
setCredentialsProvider(HTTPAuthScheme scheme, String url, CredentialsProvider provider)
{
    // Add/remove entry to AuthStore
    try {
        if(provider == null) {//remove
            HTTPAuthStore.remove(new HTTPAuthStore.Entry(scheme,url,provider));
        } else { // add
            HTTPAuthStore.insert(new HTTPAuthStore.Entry(scheme,url,provider));
        }
    } catch (HTTPException he) {
        LogStream.err.println("HTTPSession.setCredentialsProvider failed");
    }
}

public void
setCredentialsProvider(CredentialsProvider provider)
{
    sessionProvider = provider;
    setCredentialsProvider(HTTPAuthStore.DEFAULT_SCHEME,legalurl,provider);
}

static synchronized public void
setGlobalCredentialsProvider(CredentialsProvider provider)
{
    globalProvider = provider;
    setCredentialsProvider(HTTPAuthStore.DEFAULT_SCHEME,HTTPAuthStore.ANY_URL,provider);
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
        HTTPSSLProvider sslprovider = new HTTPSSLProvider(keypath,keypassword,
						      trustpath,trustpassword);
        setCredentialsProvider(HTTPAuthScheme.SSL,HTTPAuthStore.ANY_URL,sslprovider);
    }
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
    if(path != null) {
        path = path.trim();
        if(path.length() == 0) path = null;
    }
    return path;
}

}
