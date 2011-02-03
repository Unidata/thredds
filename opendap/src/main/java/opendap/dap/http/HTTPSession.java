package opendap.dap.http;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;

import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: dmh
 * Date:June 15, 2010
 * Time: 4:24 PM
 * container around org.apache.commons.
 */

public class HTTPSession
{
    static int DFALTTHREADCOUNT = 50;
    static public int SC_NOT_FOUND = HttpStatus.SC_NOT_FOUND;
    static public int SC_UNAUTHORIZED = HttpStatus.SC_UNAUTHORIZED;
    static public int SC_OK = HttpStatus.SC_OK;

    static public String CONNECTION_TIMEOUT = HttpConnectionParams.CONNECTION_TIMEOUT;
    static public String SO_TIMEOUT = HttpMethodParams.SO_TIMEOUT;

    static public String USER_AGENT = HttpMethodParams.USER_AGENT;
    static public String PROTOCOL_VERSION = HttpMethodParams.PROTOCOL_VERSION;
    static public String VIRTUAL_HOST = HttpMethodParams.VIRTUAL_HOST;
    static public String USE_EXPECT_CONTINUE = HttpMethodParams.USE_EXPECT_CONTINUE;
    static public String STRICT_TRANSFER_ENCODING = HttpMethodParams.STRICT_TRANSFER_ENCODING;
    static public String HTTP_ELEMENT_CHARSET = HttpMethodParams.HTTP_ELEMENT_CHARSET;
    static public String HTTP_CONTENT_CHARSET = HttpMethodParams.HTTP_CONTENT_CHARSET;

    /*fix:*/
    static public String HTTP_CONNECTION  = "<undefined>";
    static public String HTTP_PROXY_HOST = "<undefined>";
    static public String HTTP_REQ_SENT  = "<undefined>";
    static public String HTTP_REQUEST   = "<undefined>";
    static public String HTTP_RESPONSE  = "<undefined>";
    static public String HTTP_TARGET_HOST  = "<undefined>";
    static public String ORIGIN_SERVER  = "<undefined>";
    static public String WAIT_FOR_CONTINUE = "<undefined>";


    protected static MultiThreadedHttpConnectionManager connmgr;
    //fix: protected static SchemeRegistry schemes;
    protected static CredentialsProvider globalProvider;
    protected static String globalAgent = "/NetcdfJava/HttpClient3";
    protected static int threadcount = DFALTTHREADCOUNT;
    protected static List<HTTPSession> sessionList; // List of all HTTPSession instances

    static {
        //fix: schemes = new SchemeRegistry();
        // Fill in the scheme registry for at least http and https
        globalProvider = null;
        //fix: schemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(),80));
        connmgr = new MultiThreadedHttpConnectionManager();
        setThreadCount(DFALTTHREADCOUNT);
        // allow self-signed certificates
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 8443));
        sessionList = new ArrayList<HTTPSession>(); // see kill function
    }

    static enum Methods
    {
        Get("get"), Head("head"), Put("put"), Post("post"), Options("options");
        private final String name;
        Methods(String name) {this.name = name;}
        public String getName() {return name;}
    }

    public static synchronized CredentialsProvider getGlobalCredentialsProvider()
    {
        return globalProvider;
    }

    public static synchronized void setGlobalCredentialsProvider(CredentialsProvider p)
    {
        globalProvider = p;
        if(globalProvider != null) {
	    for(HTTPSession session: sessionList) {
		session.setCredentialsProvider(globalProvider);
	    }
	}
    }

    public static synchronized void setGlobalUserAgent(String _userAgent)
    {
        globalAgent = _userAgent;
        if(globalAgent != null) {
	    for(HTTPSession session: sessionList) {
		session.setUserAgent(globalAgent);
	    }
	}
    }

    public static String getGlobalUserAgent()
    {
        return globalAgent;
    }

    public static void setThreadCount(int nthreads)
    {
        connmgr.getParams().setMaxTotalConnections(nthreads);
        connmgr.getParams().setDefaultMaxConnectionsPerHost(nthreads);
    }

    public static int getThreadCount()
    {
        return connmgr.getParams().getMaxTotalConnections();
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
        setThreadCount(DFALTTHREADCOUNT);

    } ////////////////////////////////////////////////////////////////////////

    protected HttpClient sessionClient = null;
    protected HttpState sessionState = null;
    protected CredentialsProvider sessionProvider;
    protected String sessionAgent = "/NetcdfJava/HttpClient4";
    protected List<opendap.dap.http.HTTPMethod> methodList = new Vector<HTTPMethod>();
    HttpState context = null;
    boolean closed = false;
    // Track Method sets
    String useragent = null;
    HttpMethodParams globalmethodparams = new HttpMethodParams();
    String identifier = "Session";

    public HTTPSession() throws opendap.dap.http.HTTPException
    {
        this("Session");
    }

    public HTTPSession(String id) throws opendap.dap.http.HTTPException
    {
        this.identifier = id;
        try {
            sessionClient = new HttpClient(new HttpClientParams(), connmgr);

          // H/T: nick.bower@metoceanengineers.com
          String proxyHost = System.getProperty("http.proxyHost");
          String proxyPort = System.getProperty("http.proxyPort").trim();
	  if(proxyPort.length() == 0) proxyPort = null; // canonical form
          if(proxyHost != null && proxyPort != null) {
              this.setProxy(proxyHost, Integer.parseInt(proxyPort));
          }

          sessionList.add(this);

        } catch (Exception e) {
            throw new opendap.dap.http.HTTPException(e);
        }
    }

    public void setCredentialsProvider(CredentialsProvider provider)
    {
        sessionProvider = provider;
        if (sessionClient != null && provider != null)
            sessionClient.getParams().setParameter(CredentialsProvider.PROVIDER, provider);
    }

    public void setUserAgent(String agent)
    {
        useragent = agent;
        if (useragent != null)
            sessionClient.getParams().setParameter(USER_AGENT, useragent);
    }

    public void setConnectionManagerTimeout(long timeout)
    {
          connmgr.getParams().setConnectionTimeout((int)timeout);

    }
     public void setSoTimeout(int timeout)
    {
         sessionClient.getParams().setSoTimeout(timeout);

    }

    public void setGlobalMethodParameter(String name, Object value)
        {
            if (globalmethodparams == null)
                globalmethodparams = new HttpMethodParams();
            globalmethodparams.setParameter(name, value);
        }


    //fix: public void setStateX(HttpState cxt) {sessionState = cxt;}

    public synchronized void close()
    {
        closed = true;
        if(methodList != null)
            for (HTTPMethod m : methodList) {
            m.close();
            removeMethod(m);
        }
    }

 // Method factory methods

    public HTTPMethod newMethodGet(String uri) throws HTTPException
	{return newMethod(Methods.Get,uri);}
    public HTTPMethod newMethodHead(String uri)  throws HTTPException
	{return newMethod(Methods.Head,uri);}
    public HTTPMethod newMethodPut(String uri)   throws HTTPException
	{return newMethod(Methods.Put,uri);}
    public HTTPMethod newMethodPost(String uri)  throws HTTPException
	{return newMethod(Methods.Post,uri);}
    public HTTPMethod newMethodOptions(String uri)  throws HTTPException
	{return newMethod(Methods.Options,uri);}


    public HTTPMethod newMethod(Methods m, String uri)
            throws HTTPException
    {
        assert !closed : "Attempt to use a closed session";
        HTTPMethod method = new HTTPMethod(m, uri, this);
        addMethod(method);

        //method.setState(sessionState);
        return method;
    }

    public void setProxy(String proxyHost, int port)
    {
        sessionClient.getHostConfiguration().setProxy(proxyHost,port);
    }

    public String getCookiePolicy() {
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

    public void setContext(HttpState cxt)
    {
        context = cxt;
    }

    public HttpState getContext()
    {
        return context;
    }

    public void clearState() {
        sessionClient.getState().clearCookies();
        sessionClient.getState().clearCredentials();
      }

}
