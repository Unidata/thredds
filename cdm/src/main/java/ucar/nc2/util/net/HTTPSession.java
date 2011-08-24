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

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.protocol.Protocol;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import sun.net.www.http.*;
import ucar.unidata.util.Urlencoded;


/**
 * wrapper for org.apache.commons.httpclient
 * User: dmh
 * Date:June 15, 2010
 * Time: 4:24 PM
 * container around org.apache.commons.httpclient
 */

public class HTTPSession
{
    static int DFALTTHREADCOUNT = 50;
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


    static MultiThreadedHttpConnectionManager connmgr;
    //fix: protected static SchemeRegistry schemes;
    static String globalAgent = "/NetcdfJava/HttpClient3";
    static int threadcount = DFALTTHREADCOUNT;
    static List<HTTPSession> sessionList; // List of all HTTPSession instances
    static boolean globalauthpreemptive = false;
    static Authenticator globalAuthenticator = null;
    static CredentialsProvider globalProvider = null;
    static String globalPrincipal = null; //==ANY_PRINCIPAL


    static {
        //fix: schemes = new SchemeRegistry();
        //fix: schemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(),80));
        connmgr = new MultiThreadedHttpConnectionManager();
        setGlobalThreadCount(DFALTTHREADCOUNT);
        // Fill in the scheme registry for at least http and https
        // allow self-signed certificates
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
        sessionList = new ArrayList<HTTPSession>(); // see kill function
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
    CredentialsProvider  sessionProvider = null;
    String uriencoded = null;

    /**
     * A session is encapsulated in an instance of the class HTTPSession.
     * The encapsulation is with respect to a specific uri and (optionally) principal.
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
     */

    @Urlencoded  // Note: this a user-defined annotation for tracking which url parameters
                 // are expected to be encoded.
    public HTTPSession(String uri) throws ucar.nc2.util.net.HTTPException
    {
        construct(uri);
    }

    //Shared constructor code
    @Urlencoded
    protected void construct(String uriencoded) throws ucar.nc2.util.net.HTTPException
    {
        this.uriencoded = uriencoded;
        try {
            sessionClient = new HttpClient(connmgr);
            HttpClientParams clientparams = sessionClient.getParams();

            // Allow (circular) redirects
            clientparams.setParameter(ALLOW_CIRCULAR_REDIRECTS, true);
            clientparams.setParameter(MAX_REDIRECTS, 100);

            if (globalProvider != null) {
                clientparams.setParameter(CredentialsProvider.PROVIDER, globalProvider);
            }
            if (globalAgent != null) {
                clientparams.setParameter(USER_AGENT, globalAgent);
            }

            setAuthenticationPreemptive(globalauthpreemptive);

            // H/T: nick.bower@metoceanengineers.com
            setProxy();

            sessionList.add(this);

        } catch (Exception e) {
            throw new HTTPException(e);
        }
    }

    public String getURI()
    {
        return this.uriencoded;
    }

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

    public void setConnectionManagerTimeout(long timeout)
    {
        connmgr.getParams().setConnectionTimeout((int) timeout);

    }

    public void setSoTimeout(int timeout)
    {
        sessionClient.getParams().setSoTimeout(timeout);

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

    // H/T: nick.bower@metoceanengineers.com

    public void setProxy()
    {
        if (sessionClient == null) return;

        String host = System.getProperty("http.proxyHost");
        String port = System.getProperty("http.proxyPort");

        if (host != null) {
            host = host.trim();
            if (host.length() == 0) {
                host = null;
            }
        }
        int portno = 0;
        if (port != null) {
            port = port.trim();
            if (port.length() > 0) {
                portno = Integer.parseInt(port);
            }
        }
        if (host != null && portno > 0) {
            sessionClient.getHostConfiguration().setProxy(host, portno);
        }
    }

    // Define some utility functions

    static public String getCanonicalURI(String urlencoded)
    {
        int index = urlencoded.indexOf('?');
        if(index >= 0) urlencoded = urlencoded.substring(0,index);
        // remove any trailing extension
        //index = urlencoded.lastIndexOf('.');
        //if(index >= 0) urlencoded = urlencoded.substring(0,index);
        return canonicalpath(urlencoded);
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
    // Authorization

    static synchronized public void setGlobalAuthenticationPreemptive(boolean tf)
    {
        globalauthpreemptive = tf;
    }

    public void setCredentialsProvider(CredentialsProvider provider)
    {
        sessionProvider = provider;
        if (sessionClient != null && provider != null)
            sessionClient.getParams().setParameter(CredentialsProvider.PROVIDER, provider);
    }

    static synchronized public void
    setGlobalCredentialsProvider(CredentialsProvider cp)
    {
        globalProvider = cp;
    }

    static synchronized public void setGlobalAuthenticator(String user, String password)
    {
      if (password != null) {
        password = password.trim();
        if (password.length() == 0) {
          password = null;
        }
      }
      if (user != null) {
        user = user.trim();
        if (user.length() == 0) {
          user = null;
        }
      }
      if (user != null && password != null) {
        final PasswordAuthentication pa = new PasswordAuthentication(user, password.toCharArray());
        globalAuthenticator = new Authenticator() {
          public PasswordAuthentication getPasswordAuthentication() {
            return pa;
          }
        };
        Authenticator.setDefault(globalAuthenticator);
      }
    }

    static synchronized public void setGlobalPrincipal(String principal)
    {
        globalPrincipal = principal;
    }

    // Note that session level principal is disabled because it does not appear
    // that it is possible to do per-method execution principals because all
    // method executions share same session state. HttpContext in httpclient-4
    // should fix this.

    ////////////////////////////////////////////////
    // Combine Session creation with method creation
/* IGNORED
    static HTTPMethod sessionPlusMethod(Methods m, String uriencoded)  throws HTTPException
    {
      HTTPSession session = new HTTPSession(uriencoded);
      HTTPMethod method = new HTTPMethod(m,uriencoded,session);
      return method;
    }

    static public HTTPMethod Head(String uriencoded)  throws HTTPException {return sessionPlusMethod(Methods.Head, uriencoded);}
    static public HTTPMethod Get(String uriencoded)  throws HTTPException {return sessionPlusMethod(Methods.Get, uriencoded);}
    static public HTTPMethod Put(String uriencoded)  throws HTTPException {return sessionPlusMethod(Methods.Put, uriencoded);}
    static public HTTPMethod Post(String uriencoded)  throws HTTPException {return sessionPlusMethod(Methods.Post, uriencoded);}
    static public HTTPMethod Options(String uriencoded)  throws HTTPException {return sessionPlusMethod(Methods.Options, uriencoded);}
IGNORED */


}
