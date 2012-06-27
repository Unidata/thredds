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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import com.sun.org.apache.xerces.internal.util.*;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.auth.*;

import org.apache.commons.httpclient.protocol.Protocol;
import ucar.nc2.util.log.LogStream;

///////////////////////////////////////////////////////////////////////////////////////////////

@NotThreadSafe
public class HTTPMethod
{
// Define static factory methods

static public HTTPMethod Get(HTTPSession session) throws HTTPException
    {return new HTTPMethod(HTTPSession.Methods.Get,session,null);}

static public HTTPMethod Head(HTTPSession session) throws HTTPException
    {return new HTTPMethod(HTTPSession.Methods.Head,session,null);}

static public HTTPMethod Put(HTTPSession session) throws HTTPException
    {return new HTTPMethod(HTTPSession.Methods.Put,session,null);}

static public HTTPMethod Post(HTTPSession session) throws HTTPException
    {return new HTTPMethod(HTTPSession.Methods.Post,session,null);}

static public HTTPMethod Options(HTTPSession session) throws HTTPException
    {return new HTTPMethod(HTTPSession.Methods.Options,session,null);}

static public HTTPMethod Get(HTTPSession session, String legalurl) throws HTTPException
    {return new HTTPMethod(HTTPSession.Methods.Get,session,legalurl);}
static public HTTPMethod Head(HTTPSession session,String legalurl) throws HTTPException
    {return new HTTPMethod(HTTPSession.Methods.Head,session,legalurl);}
static public HTTPMethod Put(HTTPSession session, String legalurl) throws HTTPException
    {return new HTTPMethod(HTTPSession.Methods.Put,session,legalurl);}
static public HTTPMethod Post(HTTPSession session, String legalurl) throws HTTPException
    {return new HTTPMethod(HTTPSession.Methods.Post,session,legalurl);}
static public HTTPMethod Options(HTTPSession session, String legalurl) throws HTTPException
    {return new HTTPMethod(HTTPSession.Methods.Options,session,legalurl);}

static private org.slf4j.Logger LOG = null;

//////////////////////////////////////////////////
// Define a Retry Handler that supports more retries and is verbose.


    static public class RetryHandler extends  org.apache.commons.httpclient.DefaultHttpMethodRetryHandler
    {
        static final int MAXRETRIES = 5;
	    static final boolean verbose = false;

        public RetryHandler() {super(MAXRETRIES,false);}
        public boolean retryMethod(final org.apache.commons.httpclient.HttpMethod method,
                                   final IOException exception,
                                   int executionCount)
        {
	    if(verbose) {
            if(LOG == null)  LOG = org.slf4j.LoggerFactory.getLogger(HTTPMethod.class);
		    LOG.debug(String.format("Retry: count=%d exception=%s\n",executionCount, exception.toString()));
        }
        return super.retryMethod(method,exception,executionCount);
        }
    }



//////////////////////////////////////////////////
// Static fields

static HashMap<String, Object> globalparams = new HashMap<String, Object>();

// Static methods

static public synchronized void setGlobalParameter(String name, Object value)
{
    globalparams.put(name, value);
}

//////////////////////////////////////////////////
// Instance fields

HTTPSession session = null;
HttpMethodBase method = null; // Current method
String legalurl = null;
List<Header> headers = new ArrayList<Header>();
HashMap<String, Object> params = new HashMap<String, Object>();
HttpState context = null;
boolean executed = false;
protected boolean closed = false;
InputStream strm = null;
RequestEntity content = null;
HTTPSession.Methods methodclass = null;
Part[] multiparts = null;

/**
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

public HTTPMethod(HTTPSession.Methods m, HTTPSession session, String url)
        throws HTTPException
{
    if (session == null)
        throw new HTTPException("HTTPMethod: no session object specified");
    this.session = session;

    if(url == null)
        url = session.getURL();
    if(url == null)
        throw new HTTPException("HTTPMethod: no url specified");
    url = HTTPSession.removeprincipal(url);
    this.legalurl = url;
    if(!sessionCompatible(url))
        throw new HTTPException("HTTPMethod: session incompatible url");
    this.session.addMethod(this);

    // Unfortunately, the apache httpclient 3 code has a restrictive
    // notion of a legal url, so we need to encode it before use
    String urlencoded = EscapeStrings.escapeURL(this.legalurl);

    this.methodclass = m;
    switch (this.methodclass) {
    case Put:
        this.method = new PutMethod(urlencoded);
        break;
    case Post:
        this.method = new PostMethod(urlencoded);
        break;
    case Get:
        this.method = new GetMethod(urlencoded);
        break;
    case Head:
        this.method = new HeadMethod(urlencoded);
        break;
    case Options:
        this.method = new OptionsMethod(urlencoded);
        break;
    default:
        this.method = null;
    }
    // Force some actions
    if (method != null) {
        method.setFollowRedirects(true);
        method.setDoAuthentication(true);
    }
}

void setcontent()
{
    switch (this.methodclass) {
    case Put:
        if (this.content != null)
            ((PutMethod) method).setRequestEntity(this.content);
        break;
    case Post:
        if (multiparts != null && multiparts.length > 0) {
            MultipartRequestEntity mre = new MultipartRequestEntity(multiparts, method.getParams());
            ((PostMethod) method).setRequestEntity(mre);
        } else if (this.content != null)
            ((PostMethod) method).setRequestEntity(this.content);
        break;
    case Head:
    case Get:
    case Options:
    default:
        break;
    }
    this.content = null; // do not reuse
    this.multiparts = null;
}

public int execute() throws HTTPException
{
    if (executed)
        throw new HTTPException("Method instance already executed");
    if (legalurl == null)
        throw new HTTPException("No url specified");

    try {
        if (headers.size() > 0) {
            for (Header h : headers) {
                method.addRequestHeader(h);
            }
        }
        if (globalparams != null) {
            HttpMethodParams hmp = method.getParams();
            for (String key : globalparams.keySet()) {
                hmp.setParameter(key, globalparams.get(key));
            }
        }
        if (params != null) {
            HttpMethodParams hmp = method.getParams();
            for (String key : params.keySet()) {
                hmp.setParameter(key, params.get(key));
            }
        }

        // Change the retry handler
	    method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,new RetryHandler());

        setcontent();

        setAuthentication(session,this);
        // WARNING; DANGER WILL ROBINSION
        // httpclient3 only allows one registered https protocol, so
        // we built our own protocol registry to also take port into account
        // (see HTTPSession.registerProtocol())

        // get the protocol and port
        URL hack = new URL(this.legalurl);
        Protocol handler = session.getProtocol(hack.getProtocol(),
                                               hack.getPort());

        HostConfiguration hc = session.sessionClient.getHostConfiguration();
        hc  = new HostConfiguration(hc);
        hc.setHost(hack.getHost(),hack.getPort(),handler);
        session.sessionClient.executeMethod(hc,method);
        int code = getStatusCode();
        return code;
    } catch (Exception ie) {
        ie.printStackTrace();
        throw new HTTPException(ie);
    } finally {
        executed = true;
    }
}

public void close()
{
    // try to release underlying resources
    if (closed) {
        LogStream.err.println("HTTPMethod: attempt to close already closed method.");
        return;
    }
    if (executed) {
        consumeContent();
    } else if(method != null)
        method.abort();
    if(method != null) method.releaseConnection();
    closed = true;
    session.removeMethod(this);
}

public void consumeContent()
{
    //try {
    //InputStream st = method.getResponseBodyAsStream();
    //while((st.skip(10000) >= 0));
    method.abort();
    //} catch (IOException e) {}
}

public void setContext(HttpState cxt)
{
    session.setContext(cxt);
}

public HttpState getContext()
{
    return session.getContext();
}

public int getStatusCode()
{
    return method == null ? 0 : method.getStatusCode();
}

public String getStatusLine()
{
    return method == null ? null : method.getStatusLine().toString();
}

public String getRequestLine()
{
    //fix: return (method == null ? null : method.getRequestLine().toString());
    return "getrequestline not implemented";
}

public String getPath()
{
    try {
        return (method == null ? null : method.getURI().toString());
    } catch (URIException e) {
        return null;
    }
}

public boolean canHoldContent()
{
    if (method == null)
        return false;
    return !(method instanceof HeadMethod);
}


public InputStream getResponseBodyAsStream()
{
    return getResponseAsStream();
}

public InputStream getResponseAsStream()
{
    if (closed)
        return null;
    if (strm != null) {
        try {
            new Exception("Getting MethodStream").printStackTrace();
        } catch (Exception e) {
        }
        ;
        assert strm != null : "attempt to get method stream twice";
    }
    try {
        if (method == null) return null;
        strm = method.getResponseBodyAsStream();
        return strm;
    } catch (Exception e) {
        return null;
    }
}

public byte[] getResponseAsBytes(int maxsize)
{
    if (closed)
        return null;
    byte[] content = getResponseAsBytes();
    if (content.length > maxsize) {
        byte[] limited = new byte[maxsize];
        System.arraycopy(content, 0, limited, 0, maxsize);
        content = limited;
    }
    return content;
}

public byte[] getResponseAsBytes()
{
    if (closed || method == null)
        return null;
    try {
        return method.getResponseBody();
    } catch (Exception e) {
        return null;
    }
}

public String getResponseAsString(String charset)
{
    if (closed || method == null)
        return null;
    try {
        return method.getResponseBodyAsString();
    } catch (Exception e) {
        return null;
    }
}

public String getResponseAsString()
{
    return getResponseAsString("UTF-8");
}


public void setMethodHeaders(List<Header> headers) throws HTTPException
{
    try {
        for (Header h : headers) {
            this.headers.add(h);
        }
    } catch (Exception e) {
        throw new HTTPException(e);
    }
}

public void setRequestHeader(String name, String value) throws HTTPException
{
    setRequestHeader(new Header(name, value));
}

public void setRequestHeader(Header h) throws HTTPException
{
    try {
        headers.add(h);
    } catch (Exception e) {
        throw new HTTPException("cause", e);
    }
}

public Header getRequestHeader(String name)
{
    if (method == null)
        return null;
    try {
        return (method.getRequestHeader(name));
    } catch (Exception e) {
        return null;
    }
}

public Header[] getRequestHeaders()
{
    if (method == null)
        return null;
    try {
        Header[] hs = method.getRequestHeaders();
        return hs;
    } catch (Exception e) {
        return null;
    }
}

public Header getResponseHeader(String name)
{
    try {
        return method.getResponseHeader(name);
    } catch (Exception e) {
        return null;
    }
}

public Header getResponseHeaderdmh(String name)
{
    try {

        Header[] headers = getResponseHeaders();
        for (Header h : headers) {
            if (h.getName().equals(name))
                return h;
        }
        return null;
    } catch (Exception e) {
        return null;
    }
}

public Header[] getResponseHeaders()
{
    try {
        Header[] hs = method.getResponseHeaders();
        return hs;
    } catch (Exception e) {
        return null;
    }
}

public Header[] getResponseFooters()
{
    try {
        Header[] hs = method.getResponseFooters();
        return hs;
    } catch (Exception e) {
        return null;
    }
}

public void setRequestParameter(String name, Object value)
{
    params.put(name, value);
}

public Object getMethodParameter(String key)
{
    if (method == null)
        return null;
    return method.getParams().getParameter(key);
}

public HttpMethodParams getMethodParameters()
{
    if (method == null)
        return null;
    return method.getParams();
}

public Object getResponseParameter(String name)
{
    if (method == null)
        return null;
    return method.getParams().getParameter(name);
}


public void setRequestContentAsString(String content) throws HTTPException
{
    try {
        this.content = new StringRequestEntity(content, "application/text", "UTF-8");
    } catch (UnsupportedEncodingException ue) {
    }
}

public void setMultipartRequest(Part[] parts) throws HTTPException
{
    multiparts = new Part[parts.length];
    for (int i = 0; i < parts.length; i++) {
        multiparts[i] = parts[i];
    }
}

public String getCharSet()
{
    return "UTF-8";
}

public String getName()
{
    return method == null ? null : method.getName();
}

public String getURL()
{
    return method == null ? null : method.getPath().toString();
}

public String getEffectiveVersion()
{
    String ver = null;
    if (method != null) {
        ver = method.getEffectiveVersion().toString();
    }
    return ver;
}


public String getProtocolVersion()
{
    return getEffectiveVersion();
}

public String getSoTimeout()
{
    return method == null ? null : "" + method.getParams().getSoTimeout();
}

public String getVirtualHost()
{
    return method == null ? null : method.getParams().getVirtualHost();
}

/*public HeaderIterator headerIterator() {
    return new BasicHeaderIterator(getResponseHeaders(), null);
}*/

public String getStatusText()
{
    return getStatusLine();
}

public static Enumeration getAllowedMethods()
{
    Enumeration e = new OptionsMethod().getAllowedMethods();
    return e;
}

// Convenience methods to minimize changes elsewhere

public void setFollowRedirects(boolean tf)
{
    return; //ignore ; always done
}

public String getResponseCharSet()
{
    return "UTF-8";
}


public HTTPSession
getSession()
{
    return this.session;
}

public HttpMethodBase
getMethod()
{
    return this.method;
}

/**
 * Test that the given url is "compatible" with the
 * session specified dataset. Compatible means:
 * 1. remove any query
 * 2. HTTPAuthStore.compatibleURL must return true;
 *
 *  * @param url  to test for compatibility
 * @return
 */
boolean sessionCompatible(String other)
{
    // Remove any trailing constraint
    String sessionurl = HTTPSession.getCanonicalURL(this.session.getURL());
    if(sessionurl == null) return true; // always compatible
    other = HTTPSession.getCanonicalURL(other);
    return HTTPAuthStore.compatibleURL(sessionurl,other);
}

/**
 * Handle authentication.
 * We do not know, necessarily,
 * which scheme(s) will be
 * encountered, so most testing
 * occurs in HTTPAuthCreds.
*/

static synchronized private void
setAuthentication(HTTPSession session, HTTPMethod method)
{
    String url = session.getURL();
    if(url == null) url = HTTPAuthStore.ANY_URL;

    // Provide a credentials (provider) to enact the process
    CredentialsProvider cp  = new HTTPAuthProvider(url,method);

    // Since we not know where this will get called, do everywhere
    session.sessionClient.getParams().setParameter(CredentialsProvider.PROVIDER,cp);

    // Pass down info to the socket factory
    HttpConnectionManagerParams hcp = session.sessionClient.getHttpConnectionManager().getParams();
    hcp.setParameter(CredentialsProvider.PROVIDER,cp);

}

}
