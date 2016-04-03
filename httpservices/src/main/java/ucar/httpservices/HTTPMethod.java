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

import org.apache.http.*;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ucar.httpservices.HTTPSession.Prop;

/**
 * HTTPMethod is the encapsulation of specific
 * kind of server request: GET, HEAD, POST, etc.
 * The general processing sequence is as follows.
 * <ol>
 * <li> Create an HTTPMethod object using one of the
 * methods of HTTPFactory (e.g. HTTPFactory.Get()).
 * <p>
 * <li> Set parameters and headers of the returned HTTPMethod instance.
 * <p>
 * <li> Invoke the execute() method to actually make
 * the request.
 * <p>
 * <li> Extract response headers.
 * <p>
 * <li> Extract any body of the response in one of several forms:
 * an Inputstream, a byte array, or a String.
 * <p>
 * <li> Close the method.
 * </ol>
 * HTTPMethod is designed, at the moment, to be executed only once.
 * Note also that HTTPMethod is not thread safe but since it cannot be
 * executed multiple times, this should be irrelevant.
 * <p>
 * The arguments to the factory method are as follows.
 * <ul>
 * <li> An HTTPSession instance (optional).
 * <li> A URL.
 * </ul>
 * An HTTPMethod instance is assumed to be operating in the context
 * of an HTTPSession instance as specified by the session argument.
 * If not present, the HTTPMethod instance will create a session
 * that will be reclaimed when the method is closed
 * (see the discussion about one-shot operation below).
 * <p>
 * Method URLs may be specified in any of three ways.
 * <ol>
 * <li> It may be inherited from the URL specified when
 * the session was created.
 * <p>
 * <li> It may be specified as part of the HTTPMethod
 * constructor (via the factory). If none is specified,
 * then the session URL is used.
 * </ol>
 * <p>
 * Legal url arguments to HTTPMethod are constrained by the URL
 * specified in creating the HTTPSession instance.  If
 * the session was constructed with a specified URL, then any
 * url specified to HTTMethod (via the factory)
 * must be "compatible" with the session URL.
 * The term "compatible" basically means that the session url's
 * host+port is the same as that of the specified method url.
 * This maintains the semantics of the Session but allows
 * flexibility in accessing data from the server.
 * <p>
 * <u>One-Shot Operation:</u>
 * A reasonably common use case is when a client
 * wants to create a method, execute it, get the response,
 * and close the method. For this use case, creating a session
 * and making sure it gets closed can be a tedious proposition.
 * To support this use case, HTTPMethod supports what amounts
 * to a one-shot use. The steps are as follows:
 * <ol>
 * <li> HTTPMethod method = HTTPFactory.Get(<url string>); note
 * that this implicitly creates a session internal to the
 * method instance.
 * <p>
 * <li> Set any session parameters or headers using method.getSession().setXXX
 * <p>
 * <li> Set any parameters and headers on method
 * <p>
 * <li> method.execute();
 * <p>
 * <li> Get any response method headers
 * <p>
 * <li> InputStream stream = method.getResponseBodyAsStream()
 * <p>
 * <li> process the stream
 * <p>
 * <li> stream.close()
 * </ol>
 * There are several things to note.
 * <ul>
 * <li> Closing the method (directly or through stream.close())
 * will close the one-shot session created by the method.
 * <li> Closing the stream will close the underlying method, so it is not
 * necessary to call method.close().
 * However, if you, for example, get the response body using getResponseBodyAsString(),
 * then you need to explicitly call method.close().
 * The reason is that the stream is likely to be passed out of the scope in which the
 * method was created, hence method.close() is not easily accessible. In the second case,
 * however, this will occur in the same scope as the method and so method.close()
 * is accessible.
 * </ul>
 */

@NotThreadSafe
public class HTTPMethod implements Closeable, Comparable<HTTPMethod>
{

    //////////////////////////////////////////////////

    public static boolean TESTING = false;

    //////////////////////////////////////////////////
    // Instance fields

    protected HTTPSession session = null;
    protected boolean localsession = false;
    protected URI methodurl = null;
    protected String userinfo = null;
    protected HttpEntity content = null;
    protected HTTPSession.Methods methodkind = null;
    protected HTTPMethodStream methodstream = null; // wrapper for strm
    protected HttpRequestBase lastrequest = null;
    protected HttpResponse lastresponse = null;
    protected long[] range = null;
    // State tracking
    protected boolean closed = false;
    protected boolean executed = false;

    protected Map<String, String> headers = new HashMap<String, String>();

    // At the point of execution, the settings of the parent session are
    // captured as immutable

    protected Map<Prop, Object> settings = null;

    // For debugging
    protected RequestConfig debugconfig = null;

    //////////////////////////////////////////////////
    // Constructor(s)
    // These are package scope to prevent public instantiation

    protected HTTPMethod()
            throws HTTPException
    {
    }

    HTTPMethod(HTTPSession.Methods m, String url)
            throws HTTPException
    {
        this(m, null, url);
    }

    HTTPMethod(HTTPSession.Methods m, HTTPSession session)
            throws HTTPException
    {
        this(m, session, null);
    }

    HTTPMethod(HTTPSession.Methods m, HTTPSession session, String url)
            throws HTTPException
    {
        if(HTTPSession.TESTING) HTTPMethod.TESTING = true;
        url = HTTPUtil.nullify(url);
        if(url == null && session != null)
            url = session.getSessionURI();
        if(url == null)
            throw new HTTPException("HTTPMethod: cannot find usable url");
        try {
            this.methodurl = HTTPUtil.parseToURI(url); /// validate
        } catch (URISyntaxException mue) {
            throw new HTTPException("Malformed URL: " + url, mue);
        }

        // Check method and url compatibiltiy

        if(session == null) {
            session = HTTPFactory.newSession(url);
            localsession = true;
        }
        this.session = session;
        this.userinfo = HTTPUtil.nullify(this.methodurl.getUserInfo());
        if(this.userinfo != null) {
            this.methodurl = HTTPUtil.uriExclude(this.methodurl, HTTPUtil.URIPart.USERINFO);
            // convert userinfo to credentials
            this.session.setCredentials(
                    new UsernamePasswordCredentials(this.userinfo));
        }
        this.session.addMethod(this);
        this.methodkind = m;
    }

    public int compareTo(HTTPMethod o)
    {
        if(o == this) return 0;
        if(o == null) return -1;
        return (this.hashCode() - o.hashCode());
    }

    protected RequestBuilder
    buildrequest()
            throws HTTPException
    {
        if(this.methodurl == null)
            throw new HTTPException("Null url");
        RequestBuilder rb = null;
        switch (this.methodkind) {
        case Put:
            rb = RequestBuilder.put();
            break;
        case Post:
            rb = RequestBuilder.post();
            break;
        case Head:
            rb = RequestBuilder.head();
            break;
        case Options:
            rb = RequestBuilder.options();
            break;
        case Get:
        default:
            rb = RequestBuilder.get();
            break;
        }
        rb.setUri(this.methodurl);
        return rb;
    }

    protected void
    setcontent(RequestBuilder rb)
    {
        switch (this.methodkind) {
        case Put:
            if(this.content != null)
                rb.setEntity(this.content);
            break;
        case Post:
            if(this.content != null)
                rb.setEntity(this.content);
            break;
        case Head:
        case Get:
        case Options:
        default:
            break;
        }
        this.content = null; // do not reuse
    }


    //////////////////////////////////////////////////

    /**
     * Calling close will force the method to close, and will
     * force any open stream to terminate. If the session is local,
     * Then that too will be closed.
     */
    public synchronized void
    close()
    {
        if(closed)
            return; // multiple calls ok
        closed = true; // mark as closed to prevent recursive calls
        if(methodstream != null) {
            try {
                methodstream.close();
            } catch (IOException ioe) {/*failure is ok*/}
            ;
            methodstream = null;
        }
        //this.request = null;
        if(session != null) {
            session.removeMethod(this);
            if(localsession) {
                session.close();
                session = null;
            }
        }
        // finally, make this reusable
        if(this.lastrequest != null) this.lastrequest.reset();
    }

    //////////////////////////////////////////////////
    // Execution support

    /**
     * Create a request, add headers, and content,
     * then send to HTTPSession to do the bulk of the work.
     *
     * @return statuscode
     */

    public int execute()
            throws HTTPException
    {
        HttpResponse res = executeRaw();
        if(res != null)
            return res.getStatusLine().getStatusCode();
        else
            throw new HTTPException("HTTPMethod.execute: null response");
    }

    /**
     * Create a request, add headers, and content,
     * then send to HTTPSession to do the bulk of the work.
     *
     * @return statuscode
     */
    // debug only
    public HttpResponse
    executeRaw()
            throws HTTPException
    {
        if(this.closed)
            throw new IllegalStateException("HTTPMethod: attempt to execute closed method");
        if(this.executed)
            throw new IllegalStateException("HTTPMethod: attempt to re-execute method");
        this.executed = true;
        if(this.methodurl == null)
            throw new HTTPException("HTTPMethod: no url specified");
        if(!localsession && !sessionCompatible(this.methodurl))
            throw new HTTPException("HTTPMethod: session incompatible url: " + this.methodurl);

        // Capture the current state of the parent HTTPSession; never to be modified in this class
        this.settings = session.mergedSettings();

        try {
            // add range header
            if(this.range != null) {
                this.headers.put("Range", "bytes=" + range[0] + "-" + range[1]);
                range = null;
            }
            RequestBuilder rb = buildrequest();
            setcontent(rb);
            setheaders(rb, this.headers);
            this.lastrequest = buildRequest(rb, this.settings);
            AuthScope methodscope = HTTPAuthUtil.uriToAuthScope(this.methodurl);
            AuthScope target = HTTPAuthUtil.authscopeUpgrade(session.getSessionScope(), methodscope);
            HttpHost targethost = HTTPAuthUtil.authscopeToHost(target);
            HttpClientBuilder cb = HttpClients.custom();
            configClient(cb, settings);
            session.setAuthenticationAndProxy(cb);
            HttpClient httpclient = cb.build();
            this.lastresponse = httpclient.execute(targethost, this.lastrequest, session.getContext());
            if(this.lastresponse == null)
                throw new HTTPException("HTTPMethod.execute: Response was null");
            return this.lastresponse;
        } catch (IOException ioe) {
            throw new HTTPException(ioe);
        }
    }

    protected RequestConfig
    buildRequestConfig(Map<Prop, Object> settings)
            throws HTTPException
    {
        // Configure the RequestConfig
        HttpRequestBase request;
        RequestConfig.Builder rcb = RequestConfig.custom();
        for(Prop key : settings.keySet()) {
            Object value = settings.get(key);
            boolean tf = (value instanceof Boolean ? (Boolean) value : false);
            if(key == Prop.ALLOW_CIRCULAR_REDIRECTS) {
                rcb.setCircularRedirectsAllowed(tf);
            } else if(key == Prop.HANDLE_REDIRECTS) {
                rcb.setRedirectsEnabled(tf);
                rcb.setRelativeRedirectsAllowed(tf);
            } else if(key == Prop.MAX_REDIRECTS) {
                rcb.setMaxRedirects((Integer) value);
            } else if(key == Prop.SO_TIMEOUT) {
                rcb.setSocketTimeout((Integer) value);
            } else if(key == Prop.CONN_TIMEOUT) {
                rcb.setConnectTimeout((Integer) value);
            } else if(key == Prop.CONN_REQ_TIMEOUT) {
                rcb.setConnectionRequestTimeout((Integer) value);
            } /* else ignore */
        }
        RequestConfig cfg = rcb.build();
        if(TESTING)
            this.debugconfig = cfg;
        return cfg;
    }


    protected void
    configClient(HttpClientBuilder cb, Map<Prop, Object> settings)
            throws HTTPException
    {
        cb.useSystemProperties();
        String agent = (String) settings.get(Prop.USER_AGENT);
        if(agent != null)
            cb.setUserAgent(agent);
        session.setInterceptors(cb);
        session.setContentDecoderRegistry(cb);
        session.setClientManager(cb, this);
        session.setRetryHandler(cb);
    }

    protected void
    setheaders(RequestBuilder rb, Map<String, String> headers)
    {
        // Add any defined headers
        if(headers.size() > 0) {
            for(HashMap.Entry<String, String> entry : headers.entrySet()) {
                rb.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    protected HttpRequestBase
    buildRequest(RequestBuilder rb, Map<Prop, Object> settings)
            throws HTTPException
    {
        HttpRequestBase req;
        RequestConfig config = buildRequestConfig(settings);
        rb.setConfig(config);
        req = (HttpRequestBase) rb.build();
        if(req == null)
            throw new HTTPException("HTTPMethod.buildrequest: requestbuilder failed");
        return req;
    }

    //////////////////////////////////////////////////
    // Accessors

    public String
    getMethodKind()
    {
        return this.methodkind.name();
    }

    public int getStatusCode()
    {
        return (this.lastresponse == null) ? 0 : this.lastresponse.getStatusLine().getStatusCode();
    }

    public String getStatusLine()
    {
        return (this.lastresponse == null) ? null : this.lastresponse.getStatusLine().toString();
    }

    public String getRequestLine()
    {
        //fix: return (method == null ? null : request.getRequestLine().toString());
        throw new UnsupportedOperationException("getrequestline not implemented");
    }

    public String getPath()
    {
        return this.methodurl.toString();
    }

    public boolean canHoldContent()
    {
        return (this.methodkind == HTTPSession.Methods.Head);
    }

    public InputStream getResponseBodyAsStream()
    {
        return getResponseAsStream();
    }

    public InputStream getResponseAsStream()
    {
        if(closed)
            throw new IllegalStateException("HTTPMethod: method is closed");
        if(!executed)
            throw new IllegalStateException("HTTPMethod: method has not been executed");
        if(this.methodstream != null) { // duplicate: caller's problem
            HTTPSession.log.warn("HTTPRequest.getResponseBodyAsStream: Getting method stream multiple times");
        } else { // first time
            HTTPMethodStream stream = null;
            try {
                if(this.lastresponse == null) return null;
                stream = new HTTPMethodStream(this.lastresponse.getEntity().getContent(), this);
            } catch (Exception e) {
                stream = null;
            }
            this.methodstream = stream;
        }
        return this.methodstream;
    }

    public byte[] getResponseAsBytes(int maxbytes)
    {
        byte[] contents = getResponseAsBytes();
        if(contents != null && contents.length > maxbytes) {
            byte[] result = new byte[maxbytes];
            System.arraycopy(contents, 0, result, 0, maxbytes);
            contents = result;
        }
        return contents;
    }

    public byte[] getResponseAsBytes()
    {
        if(closed)
            throw new IllegalStateException("HTTPMethod: method is closed");
        byte[] content = null;
        if(this.lastresponse != null)
            try {
                content = EntityUtils.toByteArray(this.lastresponse.getEntity());
            } catch (Exception e) {/*ignore*/}
        return content;
    }

    public String getResponseAsString(String charset)
    {
        if(closed)
            throw new IllegalStateException("HTTPMethod: method is closed");
        String content = null;
        if(this.lastresponse != null)
            try {
                Charset cset = Charset.forName(charset);
                content = EntityUtils.toString(this.lastresponse.getEntity(), cset);
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        close();//getting the response will disallow later stream
        return content;
    }

    public String getResponseAsString()
    {
        return getResponseAsString("UTF-8");
    }

    public Header getRequestHeader(String name)
    {
        Header[] hdrs = getRequestHeaders();
        for(Header h : hdrs) {
            if(h.getName().equals(name))
                return h;
        }
        return null;
    }

    public Header getResponseHeader(String name)
    {
        try {
            return this.lastresponse == null ? null : this.lastresponse.getFirstHeader(name);
        } catch (Exception e) {
            return null;
        }
    }

    public Header[] getResponseHeaders()
    {
        try {
            if(this.lastresponse == null)
                return null;
            Header[] hs = this.lastresponse.getAllHeaders();
            return hs;
        } catch (Exception e) {
            return null;
        }
    }

    public HTTPMethod setRequestContent(HttpEntity content)
    {
        this.content = content;
        return this;
    }

    public String getCharSet()
    {
        return "UTF-8";
    }

    public URI getURI()
    {
        return this.methodurl;
    }

   /* public String getProtocolVersion()
    {
        String ver = null;
        if(request != null) {
            ver = request.getProtocolVersion().toString();
        }
        return ver;
    }  */

    public String getStatusText()
    {
        return getStatusLine();
    }

    // Convenience methods to minimize changes elsewhere

    public String getResponseCharSet()
    {
        return "UTF-8";
    }

    public HTTPSession
    getSession()
    {
        return this.session;
    }

    public boolean
    isSessionLocal()
    {
        return this.localsession;
    }

    public boolean hasStreamOpen()
    {
        return methodstream != null;
    }

    public boolean isClosed()
    {
        return this.closed;
    }

    public HTTPMethod
    setRange(long lo, long hi)
    {
        range = new long[]{lo, hi};
        return this;
    }

    public Header[] getRequestHeaders()
    {
        return this.lastrequest == null ? null : this.lastrequest.getAllHeaders();
    }

    //////////////////////////////////////////////////
    // Pass thru's to HTTPSession

    public HTTPMethod
    setCompression(String compressors)
    {
        this.session.setGlobalCompression(compressors);
        return this;
    }

    public HTTPMethod setFollowRedirects(boolean tf)
    {
        this.session.setFollowRedirects(tf);
        return this;
    }

    public HTTPMethod setMaxRedirects(int n)
    {
        this.session.setMaxRedirects(n);
        return this;
    }

    public HTTPMethod setSOTimeout(int n)
        {
            this.session.setSoTimeout(n);
            return this;
        }

    public HTTPMethod setUserAgent(String agent)
    {
        this.session.setUserAgent(agent);
        return this;
    }

    public HTTPMethod setUseSessions(boolean tf)
    {
        this.session.setUseSessions(tf);
        return this;
    }

    public HTTPMethod setCredentials(Credentials creds)
            throws HTTPException
    {
        this.session.setCredentials(creds);
        return this;
    }

    public HTTPMethod
    setCredentials(Credentials creds, AuthScope scope)
            throws HTTPException
    {
        this.session.setCredentials(creds, scope);
        return this;
    }


    //////////////////////////////////////////////////
    // Utilities

    /**
     * Test that the given url is "compatible" with the
     * session specified dataset. Wrapper around
     * HTTPAuthUtil.httphostCompatible().
     *
     * @param other to test for compatibility against this method's
     * @return true if compatible, false otherwise.
     */
    protected boolean
    sessionCompatible(AuthScope other)
    {
        return HTTPAuthUtil.authscopeCompatible(session.getAuthScope(), other);
    }

    protected boolean
    sessionCompatible(URI otheruri)
    {
        AuthScope other = HTTPAuthUtil.uriToAuthScope(otheruri);
        return sessionCompatible(other);
    }


    @Deprecated
    protected boolean sessionCompatible(HttpHost otherhost)
    {
        AuthScope other = HTTPAuthUtil.hostToAuthScope(otherhost);
        return sessionCompatible(other);
    }

    //////////////////////////////////////////////////
    // debug interface

    public RequestConfig
    getDebugConfig()
    {
        if(!TESTING) throw new UnsupportedOperationException();
        return this.debugconfig;
    }

    public HttpMessage debugRequest()
    {
        if(!TESTING) throw new UnsupportedOperationException();
        return (this.lastrequest);
    }

    public HttpResponse debugResponse()
    {
        if(!TESTING) throw new UnsupportedOperationException();
        return (this.lastresponse);
    }

    //////////////////////////////////////////////////
    // Deprecated but for back compatibility

    /**
     * Deprecated: use getMethodKind
     *
     * @return Name of the method: e.g. GET, HEAD, ...
     */
    @Deprecated
    public String
    getName()
    {
        return getMethodKind();
    }

    @Deprecated
    public HTTPMethod
    setMethodHeaders(List<Header> headers) throws HTTPException
    {
        try {
            for(Header h : headers) {
                this.headers.put(h.getName(), h.getValue());
            }
        } catch (Exception e) {
            throw new HTTPException(e);
        }
        return this;
    }

    @Deprecated
    public HTTPMethod
    setRequestHeader(String name, String value) throws HTTPException
    {
        return setRequestHeader(new BasicHeader(name, value));
    }

    @Deprecated
    protected HTTPMethod
    setRequestHeader(Header h) throws HTTPException
    {
        try {
            this.headers.put(h.getName(), h.getValue());
        } catch (Exception e) {
            throw new HTTPException("cause", e);
        }
        return this;
    }

}
