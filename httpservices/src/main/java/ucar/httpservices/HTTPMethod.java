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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
 * In practice, one has an HTTPMethod instance, one can
 * repeat the steps 2-5. Of course this assumes that one is
 * doing the same kind of action (e.g. GET).
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
 * <p>
 * <li> It may be specified as an argument to the
 * execute() method. If none is specified, then
 * the factory constructor URL is used (which might,
 * in turn have come from the session).
 * </ol>
 * <p>
 * Legal url arguments to HTTPMethod are constrained by the URL
 * specified in creating the HTTPSession instance, if any.  If
 * the session was constructed with a specified URL, then any
 * url specified to HTTMethod (via the factory or via
 * execute()) must be "compatible" with the session URL). The
 * term "compatible" basically means that the session url's host+port
 * is the same as that of the specified method url.  This
 * maintains the semantics of the Session but allows
 * flexibility in accessing data from the server.
 * <p>
 * <u>One-Shot Operation:</u>
 * A reasonably common use case is when a client
 * wants to create a method, execute it, get the response,
 * and close the method. For this use case, creating a session
 * and making sure it gets closed can be a tricky proposition.
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
 * <li> Closing the stream will close the underlying method, so it is not
 * necessary to call method.close().
 * <li> However, if you, for example, get the response body using getResponseBodyAsString(),
 * then you need to explicitly call method.close().
 * <li> Closing the method (directly or through stream.close())
 * will close the one-shot session created by the method.
 * </ul>
 */

public class HTTPMethod implements Closeable
{
    //////////////////////////////////////////////////
    // Static Methods

    static public Set<String> getAllowedMethods()
    {
        HttpResponse rs = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 0, "");
        Set<String> set = new HttpOptions().getAllowedMethods(rs);
        return set;
    }

    //////////////////////////////////////////////////
    // Instance fields

    protected HTTPSession session = null;
    protected boolean localsession = false;
    protected URI methodurl = null;
    protected String userinfo = null;
    protected HttpEntity content = null;
    protected HTTPSession.Methods methodkind = null;
    protected HTTPMethodStream methodstream = null; // wrapper for strm
    protected boolean closed = false;
    protected HttpRequestBase request = null;
    protected CloseableHttpResponse response = null;
    protected long[] range = null;

    protected List<Header> headers = new ArrayList<Header>();

    //////////////////////////////////////////////////
    // Constructor(s)
    // These are package scope to prevent public instantiation

    HTTPMethod()
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
    setheaders(RequestBuilder rb)
    {
        if(range != null) {
            rb.addHeader("Range", "bytes=" + range[0] + "-" + range[1]);
            range = null;
        }
        // Add any defined headers
        if(this.headers.size() > 0) {
            for(Header h : this.headers) {
                rb.addHeader(h);
            }
        }
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
    // Execution support

    /**
     * Create a request, add headers, and content,
     * then send to HTTPSession to do the bulk of the work.
     */

    public int execute()
            throws HTTPException
    {
        if(closed)
            throw new HTTPException("HTTPMethod: attempt to execute closed method");
        if(this.methodurl == null)
            throw new HTTPException("HTTPMethod: no url specified");
        if(!localsession && !sessionCompatible(this.methodurl))
            throw new HTTPException("HTTPMethod: session incompatible url: " + this.methodurl);

        RequestBuilder rb = buildrequest();
        try {
            // Apply settings
            setcontent(rb);
            // Add any user defined headers
            setheaders(rb);

            // use the session to do the heavy lifting.
            HTTPSession.ExecState estate = session.execute(this, methodurl, rb);
            this.request = estate.request;
            this.response = estate.response;
            if(this.request == null || this.response == null)
                throw new IllegalStateException("HTTPMethod.execute: request or response was null");
            HttpClientContext execcontext = session.getExecutionContext();
            int code = this.response.getStatusLine().getStatusCode();
            return code;
        } catch (Exception ie) {
            throw new HTTPException(ie);
        }
    }

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
        if(this.request != null) this.request.reset();
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
        return (this.response == null) ? 0 : this.response.getStatusLine().getStatusCode();
    }

    public String getStatusLine()
    {
        return (this.response == null) ? null : this.response.getStatusLine().toString();
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
        if(this.methodstream != null) { // duplicate: caller's problem
            HTTPSession.log.warn("HTTPRequest.getResponseBodyAsStream: Getting method stream multiple times");
        } else { // first time
            HTTPMethodStream stream = null;
            try {
                if(this.response == null) return null;
                stream = new HTTPMethodStream(this.response.getEntity().getContent(), this);
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
        if(this.response != null)
            try {
                content = EntityUtils.toByteArray(this.response.getEntity());
            } catch (Exception e) {/*ignore*/}
        return content;
    }

    public String getResponseAsString(String charset)
    {
        if(closed)
            throw new IllegalStateException("HTTPMethod: method is closed");
        String content = null;
        if(this.response != null)
            try {
                Charset cset = Charset.forName(charset);
                content = EntityUtils.toString(this.response.getEntity(), cset);
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
            return this.response == null ? null : this.response.getFirstHeader(name);
        } catch (Exception e) {
            return null;
        }
    }

    public Header[] getResponseHeaders()
    {
        try {
            if(this.response == null)
                return null;
            Header[] hs = this.response.getAllHeaders();
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

    public String getURL()
    {
        return this.methodurl.toString();
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

    //////////////////////////////////////////////////
    // Pass thru's to HTTPSession

    public Header[] getRequestHeaders()
    {
        return this.session.getRequestHeaders();
    }

    public RequestConfig
    getDebugConfig()
    {
        return this.session.getDebugConfig();
    }

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

    public HttpMessage debugRequest()
    {
        return (this.request);
    }

    public HttpResponse debugResponse()
    {
        return (this.response);
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
                this.headers.add(h);
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
            headers.add(h);
        } catch (Exception e) {
            throw new HTTPException("cause", e);
        }
        return this;
    }

}
