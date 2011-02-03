package opendap.dap.http;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import ucar.nc2.util.IO;

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


    //////////////////////////////////////////////////////
    /**
     * The following procedures were moved here (with some renaming)
     * from HttpClientManager -- which is no longer used.
     * These procedures are static and create their own session and method instances
     * and then kill them.
     */

    /**
        * Define a method to combine making the method, executing it, and returning
        * a result as a string
        */

       public static String getContentAsString(String url) throws HTTPException
       {
           HTTPSession session = new HTTPSession();
           HTTPMethod method = session.newMethodGet(url);
           method.execute();
           String result = method.getResponseAsString();
           method.close();
           return result;
       }

        /**
     * Define a method to combine making the method, executing it, and returning
     * a result as a stream
     */

    public static InputStream getContentAsStream(String url) throws HTTPException
    {
        HTTPSession session = new HTTPSession();
        HTTPMethod method = session.newMethodGet(url);
        method.execute();
        InputStream in = method.getResponseAsStream();
        HTTPMethodStream result = new HTTPMethodStream(null,method,in);
        return result;
    }

    /**
       * Put content to a url, using HTTP PUT. Handles one level of 302 redirection.
       * @param urlString url as a String
       * @param content PUT this content at the given url.
       * @return the HTTP status return code
       * @throws HTTPException on error
       */
    public static int putContent(String urlString, String content) throws HTTPException
    {
        HTTPSession session = null;

        try {

        session = new HTTPSession();
        HTTPMethod m = session.newMethodPut(urlString);
        //fix m.setDoAuthentication( true );

          m.setRequestContentAsString(content);

          m.execute();

          int resultCode = m.getStatusCode();

           // followRedirect wont work for PUT
           if (resultCode == 302) {
             String redirectLocation;
             Header locationHeader = m.getResponseHeader("location");
             if (locationHeader != null) {
               redirectLocation = locationHeader.getValue();
               resultCode = putContent(redirectLocation, content);
             }
           }

          return resultCode;

        } finally {
          if(session != null) session.close();
        }
      }

      //////////////////////

       static public String getUrlContentsAsString(String urlString, int maxKbytes) {
         HTTPSession session = null;
         try {
           session = new HTTPSession();
           HTTPMethod m = session.newMethodGet(urlString);
           m.setFollowRedirects(true);
           m.setRequestHeader("Accept-Encoding", "gzip,deflate");

           int status = m.execute();
           if (status != 200) {
             throw new RuntimeException("failed status = "+status);
           }

           String charset = m.getResponseCharSet();
           if (charset == null) charset = "UTF-8";

           // check for deflate and gzip compression
           Header h = m.getResponseHeader("content-encoding");
           String encoding = (h == null) ? null : h.getValue();

           if (encoding != null && encoding.equals("deflate")) {
             byte[] body = m.getResponseAsBytes();
             InputStream is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(body)), 10000);
             return readContents(is, charset, maxKbytes);

           } else if (encoding != null && encoding.equals("gzip")) {
             byte[] body = m.getResponseAsBytes();
             InputStream is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(body)), 10000);
             return readContents(is, charset, maxKbytes);

           } else {
             byte[] body = m.getResponseAsBytes(maxKbytes * 1000);
             return new String(body, charset);
           }

         } catch (Exception e) {
           e.printStackTrace();
           return null;

         } finally {
           if(session != null) session.close();
         }
       }

       static private String readContents(InputStream is, String charset, int maxKbytes) throws IOException {
         ByteArrayOutputStream bout = new ByteArrayOutputStream(1000 * maxKbytes);
         IO.copy(is, bout, 1000 * maxKbytes);
         return bout.toString(charset);
       }

       static public  void copyUrlContentsToFile(String urlString, File file) throws HTTPException {
         HTTPSession session = null;

         try {
         session = new HTTPSession();
         HTTPMethod m = session.newMethodGet(urlString);
         m.setRequestHeader("Accept-Encoding", "gzip,deflate");

           int status = m.execute();

           if (status != 200) {
             throw new RuntimeException("failed status = "+status);
           }

           String charset = m.getResponseCharSet();
           if (charset == null) charset = "UTF-8";

           // check for deflate and gzip compression
           Header h = m.getResponseHeader("content-encoding");
           String encoding = (h == null) ? null : h.getValue();

           if (encoding != null && encoding.equals("deflate")) {
             InputStream is = new BufferedInputStream(new InflaterInputStream(m.getResponseAsStream()), 10000);
             IO.writeToFile(is, file.getPath());

           } else if (encoding != null && encoding.equals("gzip")) {
             InputStream is = new BufferedInputStream(new GZIPInputStream(m.getResponseAsStream()), 10000);
             IO.writeToFile(is, file.getPath());

           } else {
             IO.writeToFile(m.getResponseAsStream(), file.getPath());
           }

         } catch (Exception e) {
           e.printStackTrace();

         } finally {
           if(session != null) session.close();
         }
       }

       static public long appendUrlContentsToFile(String urlString, File file, long start, long end) throws HTTPException {
         long nbytes = 0;

         HTTPSession session = null;
         try {
         session = new HTTPSession();
         HTTPMethod m = session.newMethodGet(urlString);
         m.setRequestHeader("Accept-Encoding", "gzip,deflate");
         m.setRequestHeader("Range", "bytes=" + start + "-" + end);


           int status = m.execute();
           if ((status != 200) && (status != 206)) {
             throw new RuntimeException("failed status = "+status);
           }

           String charset = m.getResponseCharSet();
           if (charset == null) charset = "UTF-8";

           // check for deflate and gzip compression
           Header h = m.getResponseHeader("content-encoding");
           String encoding = (h == null) ? null : h.getValue();

           if (encoding != null && encoding.equals("deflate")) {
             InputStream is = new BufferedInputStream(new InflaterInputStream(m.getResponseAsStream()), 10000);
             nbytes = IO.appendToFile(is, file.getPath());

           } else if (encoding != null && encoding.equals("gzip")) {
             InputStream is = new BufferedInputStream(new GZIPInputStream(m.getResponseAsStream()), 10000);
             nbytes = IO.appendToFile(is, file.getPath());

           } else {
             nbytes = IO.appendToFile(m.getResponseAsStream(), file.getPath());
           }

         } catch (Exception e) {
           e.printStackTrace();

         } finally {
           if (session!=null) session.close();
         }

         return nbytes;
         }

      static public void showHttpRequestInfo(Formatter f, HttpMethodBase m) {
        f.format("HttpClient request %s %s %n", m.getName(), m.getPath());
        f.format("   do Authentication=%s%n", m.getDoAuthentication());
        f.format("   follow Redirects =%s%n", m.getFollowRedirects());
        f.format("   effectiveVersion =%s%n", m.getEffectiveVersion());
        f.format("   hostAuthState    =%s%n", m.getHostAuthState());

        HttpMethodParams p = m.getParams();
        f.format("   cookie policy    =%s%n", p.getCookiePolicy());
        f.format("   http version     =%s%n", p.getVersion());
        f.format("   timeout (msecs)  =%d%n", p.getSoTimeout());
        f.format("   virtual host     =%s%n", p.getVirtualHost());

        f.format("Request Headers = %n");
        Header[] heads = m.getRequestHeaders();
        for (int i = 0; i < heads.length; i++)
          f.format("  %s", heads[i]);

        f.format("%n");
      }

      static public void showHttpResponseInfo(Formatter f, HttpMethodBase m) {
        f.format("HttpClient response status = %s%n", m.getStatusLine());
        f.format("Reponse Headers = %n");
        Header[] heads = m.getResponseHeaders();
        for (int i = 0; i < heads.length; i++)
          f.format("  %s", heads[i]);
        f.format("%n");
      }
}
