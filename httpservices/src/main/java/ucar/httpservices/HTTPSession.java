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
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.print.attribute.UnmodifiableSetException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * A session is encapsulated in an instance of the class HTTPSession.
 * The encapsulation is with respect to a specific HttpHost "realm",
 * where the important part is is host+port.  This means that once a
 * session is specified, it is tied permanently to that realm.
 * <p>
 * A Session encapsulate a number of other objects:
 * <ul>
 * <li> An instance of an Apache HttpClient.
 * <li> A http session id
 * <li> A RequestContext object; this also includes authentication:
 * specifically a credential and a credentials provider.
 * </ul>
 * <p>
 * Currently, it is assumed that only one set of credentials is needed,
 * whether directly for server X or for server Y. This may change in the
 * future.
 * <p>
 * As a rule, if the client gives an HTTPSession object to the "create method"
 * procedures of  HTTPFactory (e.g. HTTPFactory.Get or HTTPFactory.Post)
 * then that creation call must specify a url that is "compatible" with the
 * scope of the session.  The method url is <it>compatible</i> if its
 * host+port is the same as the session's host+port (=scope) and its scheme is
 * compatible, where e.g. http is compatible with https
 * (see HTTPAuthUtil.httphostCompatible)
 * <p>
 * If the HTTPFactory method creation call does not specify a session
 * object, then one is created (and destroyed) behind the scenes
 * along with the method.
 * <p>
 * Note that the term legalurl in the following code means that the url has
 * reserved characters within identifieers in escaped form. This is
 * particularly and issue for queries. Especially: ?x[0:5] is legal and the
 * square brackets need not be encoded.
 * <p>
 * As of the move to Apache Httpclient 4.4 and later, the underlying
 * HttpClient objects are generally immutable. This means that at least
 * this class (HTTPSession) and the HTTPMethod class must store the
 * relevant info and create the HttpClient and HttpMethod objects
 * dynamically. This also means that when a parameter is changed (Agent,
 * for example), any existing cached HttpClient must be thrown away and
 * reconstructed using the change. As a rule, the HttpClient object will be
 * created at the last minute so that multiple parameter changes can be
 * effected without have to re-create the HttpClient for each parameter
 * change. Also note that the immutable objects will be cached and reused
 * if no parameters are changed.
 * <p>
 * <em>Authorization</em>
 * We assume that the session supports two CredentialsProvider instances:
 * one global to all HTTPSession objects and one specific to each
 * HTTPSession object.
 * <p>
 * As an aside, authentication is a bit tricky because some
 * authorization schemes use redirection. That is, the initial request
 * is made to server X, but X says: goto to server Y" to get, say, and
 * authorization token.  Then Y says: return to X with this token and
 * proceed.
 * <p>
 * <em>SSL</em>
 * TBD.
 */

@ThreadSafe
public class HTTPSession implements Closeable
{
    //////////////////////////////////////////////////
    // Constants

    // only used when testing flag is set
    static public boolean TESTING = false; // set to true during testing, should be false otherwise

    /**
     * Determine wether to use a Pooling connection manager
     * or to manage a bunch of individual connections.
     */
    static protected final boolean USEPOOL = true;

    // Define all the legal properties
    // Previously taken from class AllClientPNames, but that is now
    // deprecated, so just use an enum

    static /*package*/ enum Prop
    {
        ALLOW_CIRCULAR_REDIRECTS,
        HANDLE_REDIRECTS,
        HANDLE_AUTHENTICATION,
        MAX_REDIRECTS,
        MAX_CONNECTIONS,
        SO_TIMEOUT,
        CONN_TIMEOUT,
        CONN_REQ_TIMEOUT,
        USER_AGENT,
        COOKIE_STORE,
        RETRIES,
        UNAVAILRETRIES,
        COMPRESSION,
        CREDENTIALS,
        USESESSIONS,
    }

    // Header names
    // from: http://en.wikipedia.org/wiki/List_of_HTTP_header_fields
    static final public String HEADER_USERAGENT = "User-Agent";
    static final public String ACCEPT_ENCODING = "Accept-Encoding";

    static final int DFALTREDIRECTS = 25;
    static final int DFALTCONNTIMEOUT = 1 * 60 * 1000; // 1 minutes (60000 milliseconds)
    static final int DFALTCONNREQTIMEOUT = DFALTCONNTIMEOUT;
    static final int DFALTSOTIMEOUT = 5 * 60 * 1000; // 5 minutes (300000 milliseconds)

    static final int DFALTMAXCONNS = 20;
    static final int DFALTRETRIES = 1;
    static final int DFALTUNAVAILRETRIES = 1;
    static final int DFALTUNAVAILINTERVAL = 3000; // 3 seconds
    static final String DFALTUSERAGENT = "/NetcdfJava/HttpClient4.4";

    static final String[] KNOWNCOMPRESSORS = {"gzip", "deflate"};

    //////////////////////////////////////////////////////////////////////////

    static final boolean IGNORECERTS = false;

    //////////////////////////////////////////////////////////////////////////
    // Type Declaration(s)

    // Define property keys for selected authorization related properties
    static /*package*/ enum AuthProp
    {
        KEYSTORE,
        KEYPASSWORD,
        TRUSTSTORE,
        TRUSTPASSWORD,
        SSLFACTORY,
        HTTPPROXY,
        HTTPSPROXY,
        PROXYUSER,
        PROXYPWD,
    }

    // Capture the value of various properties
    // Control read-only state; primarily for debugging

    static class AuthControls extends ConcurrentHashMap<AuthProp, Object>
    {
        protected boolean readonly = false;

        public AuthControls()
        {
            super();
        }

        public AuthControls setReadOnly(boolean tf)
        {
            this.readonly = tf;
            return this;
        }

        // Override to make map (but not contents) read only
        public Set<Map.Entry<AuthProp, Object>> entrySet()
        {
            throw new UnsupportedOperationException();
        }

        public Object put(AuthProp key, Object val)
        {
            if(readonly) throw new UnmodifiableSetException();
            if(val != null)
                super.put(key, val);
            return val;
        }

        public Object replace(AuthProp key, Object val)
        {
            if(readonly) throw new UnmodifiableSetException();
            return super.replace(key, val);
        }

        public void clear()
        {
            if(readonly) throw new UnmodifiableSetException();
            super.clear();
        }

    }


    // Support loose certificate acceptance 
    static class LooseTrustStrategy extends TrustSelfSignedStrategy
    {
        @Override
        public boolean
        isTrusted(final X509Certificate[] chain, String authType)
                throws CertificateException
        {
            try {
                if(super.isTrusted(chain, authType)) return true;
                // check expiration dates
                for(X509Certificate x5 : chain) {
                    try {
                        x5.checkValidity();
                    } catch (CertificateExpiredException
                            | CertificateNotYetValidException ce) {
                        return true;
                    }
                }
            } catch (CertificateException e) {
                return true; // temporary
            }
            return false;
        }
    }

    // For communication between HTTPSession.execute and HTTPMethod.execute.
   /* static package class ExecState
    {
        public HttpRequestBase request = null;
        public HttpResponse response = null;
    }
        */
    static /*package*/ enum Methods
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

    static class GZIPResponseInterceptor implements HttpResponseInterceptor
    {
        public void process(final HttpResponse response, final HttpContext context)
                throws HttpException, IOException
        {
            HttpEntity entity = response.getEntity();
            if(entity != null) {
                Header ceheader = entity.getContentEncoding();
                if(ceheader != null) {
                    HeaderElement[] codecs = ceheader.getElements();
                    for(HeaderElement h : codecs) {
                        if(h.getName().equalsIgnoreCase("gzip")) {
                            response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                            return;
                        }
                    }
                }
            }
        }
    }

    static class DeflateResponseInterceptor implements HttpResponseInterceptor
    {
        public void process(final HttpResponse response, final HttpContext context)
                throws HttpException, IOException
        {
            HttpEntity entity = response.getEntity();
            if(entity != null) {
                Header ceheader = entity.getContentEncoding();
                if(ceheader != null) {
                    HeaderElement[] codecs = ceheader.getElements();
                    for(HeaderElement h : codecs) {
                        if(h.getName().equalsIgnoreCase("deflate")) {
                            response.setEntity(new DeflateDecompressingEntity(response.getEntity()));
                            return;
                        }
                    }
                }
            }
        }
    }

    static class ZipStreamFactory implements InputStreamFactory
    {
        // InputStreamFactory methods
        @Override
        public InputStream create(InputStream instream)
                throws IOException
        {
            return new ZipInputStream(instream, HTTPUtil.UTF8);
        }
    }

    static class GZIPStreamFactory implements InputStreamFactory
    {
        // InputStreamFactory methods
        @Override
        public InputStream create(InputStream instream)
                throws IOException
        {
            return new GZIPInputStream(instream);
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // Static variables

    static final public org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(HTTPSession.class);

    // Define a settings object to hold all the
    // settable values; there will be one
    // instance for global and one for local.

    static Map<Prop, Object> globalsettings;

    static HttpRequestRetryHandler globalretryhandler = null;

    // Define interceptor instances; use copy on write for thread safety
    static List<HttpRequestInterceptor> reqintercepts = new CopyOnWriteArrayList<HttpRequestInterceptor>();
    static List<HttpResponseInterceptor> rspintercepts = new CopyOnWriteArrayList<HttpResponseInterceptor>();

    // This is a hack to suppress content-encoding headers from request
    // Effectively final because its set in the static initializer and otherwise
    // read only.
    static protected HttpResponseInterceptor CEKILL;

    // Debug Header interceptors
    static protected List<HttpRequestInterceptor> dbgreq = new CopyOnWriteArrayList<>();
    static protected List<HttpResponseInterceptor> dbgrsp = new CopyOnWriteArrayList<>();

    static protected HTTPConnections connmgr;

    static protected Map<String, InputStreamFactory> contentDecoderMap;

    //public final HttpClientBuilder setContentDecoderRegistry(Map<String,InputStreamFactory> contentDecoderMap)

    static protected Map<AuthScope, HTTPProviderFactory> globalcredfactories = new HashMap<>();

    // As taken from the command line
    static protected AuthControls authcontrols;

    static { // watch out: order is important for these initializers
        if(USEPOOL)
            connmgr = new HTTPConnectionPool();
        else
            connmgr = new HTTPConnectionSimple();
        CEKILL = new HTTPUtil.ContentEncodingInterceptor();
        contentDecoderMap = new HashMap<String, InputStreamFactory>();
        contentDecoderMap.put("zip", new ZipStreamFactory());
        contentDecoderMap.put("gzip", new GZIPStreamFactory());
        globalsettings = new ConcurrentHashMap<Prop, Object>();
        setDefaults(globalsettings);
        authcontrols = new AuthControls();
        authcontrols.setReadOnly(false);
        buildproxy(authcontrols);
        buildkeystores(authcontrols);
        buildsslfactory(authcontrols);
        authcontrols.setReadOnly(true);
        connmgr.addProtocol("https", (ConnectionSocketFactory) authcontrols.get(AuthProp.SSLFACTORY));
        setGlobalUserAgent(DFALTUSERAGENT);
        setGlobalMaxConnections(DFALTMAXCONNS);
        setGlobalConnectionTimeout(DFALTCONNTIMEOUT);
        setGlobalSoTimeout(DFALTSOTIMEOUT);
    }

    //////////////////////////////////////////////////////////////////////////
    // Static Initialization

    // Provide defaults for a settings map
    static synchronized protected void setDefaults(Map<Prop, Object> props)
    {
        if(false) {// turn off for now
            props.put(Prop.HANDLE_AUTHENTICATION, Boolean.TRUE);
        }
        props.put(Prop.HANDLE_REDIRECTS, Boolean.TRUE);
        props.put(Prop.ALLOW_CIRCULAR_REDIRECTS, Boolean.TRUE);
        props.put(Prop.MAX_REDIRECTS, (Integer) DFALTREDIRECTS);
        props.put(Prop.SO_TIMEOUT, (Integer) DFALTSOTIMEOUT);
        props.put(Prop.CONN_TIMEOUT, (Integer) DFALTCONNTIMEOUT);
        props.put(Prop.CONN_REQ_TIMEOUT, (Integer) DFALTCONNREQTIMEOUT);
        props.put(Prop.USER_AGENT, DFALTUSERAGENT);
    }

    static final void
    buildsslfactory(AuthControls authcontrols)
    {
        KeyStore keystore = (KeyStore) authcontrols.get(AuthProp.KEYSTORE);
        String keypass = (String) authcontrols.get(AuthProp.KEYPASSWORD);
        KeyStore truststore = (KeyStore) authcontrols.get(AuthProp.TRUSTSTORE);
        buildsslfactory(authcontrols, truststore, keystore, keypass);
    }

    static final void
    buildkeystores(AuthControls authcontrols)
    {
        // SSL flags
        String keypath = cleanproperty("keystore");
        String keypassword = cleanproperty("keystorepassword");
        String trustpath = cleanproperty("truststore");
        String trustpassword = cleanproperty("truststorepassword");
        KeyStore truststore = buildkeystore(trustpath, trustpassword);
        KeyStore keystore = buildkeystore(keypath, keypassword);

        authcontrols.put(AuthProp.KEYSTORE, keystore);
        authcontrols.put(AuthProp.KEYPASSWORD, keypassword);
        authcontrols.put(AuthProp.TRUSTSTORE, truststore);
        authcontrols.put(AuthProp.TRUSTPASSWORD, trustpassword);
    }

    static protected final KeyStore
    buildkeystore(String keypath, String keypassword)
    {
        KeyStore keystore;
        try {
            if(keypath != null && keypassword != null) {
                keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                try (FileInputStream instream = new FileInputStream(new File(keypath))) {
                    keystore.load(instream, keypassword.toCharArray());
                }
            } else
                keystore = null;
        } catch (IOException
                | CertificateException
                | NoSuchAlgorithmException
                | KeyStoreException e) {
            keystore = null;
        }
        return keystore;
    }

    static final void
    buildproxy(AuthControls ac)
    {
        // Proxy flags
        String proxyurl = getproxyurl();
        if(proxyurl == null)
            return;
        URI uri;
        try {
            uri = HTTPUtil.parseToURI(proxyurl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Bad proxy URL: " + proxyurl);
        }
        HttpHost httpproxy = null;
        HttpHost httpsproxy = null;
        String user = null;
        String pwd = null;
        if(uri.getScheme().equals("http")) {
            httpproxy = new HttpHost(uri.getHost(), uri.getPort(), "http");
        } else if(uri.getScheme().equals("https")) {
            httpsproxy = new HttpHost(uri.getHost(), uri.getPort(), "https");
        }
        String upw = uri.getUserInfo();
        if(upw != null) {
            String[] pieces = upw.split("[:]");
            if(pieces.length == 2
                    && HTTPUtil.nullify(pieces[0]) != null
                    && HTTPUtil.nullify(pieces[1]) != null) {
                user = pieces[0];
                pwd = pieces[1];
            } else {
                throw new IllegalArgumentException("Bad userinfo: " + proxyurl);
            }
        }
        ac.put(AuthProp.HTTPPROXY, httpproxy);
        ac.put(AuthProp.HTTPSPROXY, httpsproxy);
        ac.put(AuthProp.PROXYUSER, user);
        ac.put(AuthProp.PROXYPWD, pwd);
    }

    static final String
    getproxyurl()
    {
        String proxyurl = cleanproperty("proxyurl");
        if(proxyurl == null) {
            // Check the java.net flags
            String proxyhost = cleanproperty("https.proxyHost");
            if(proxyhost != null) {
                StringBuilder buf = new StringBuilder();
                buf.append("https://");
                buf.append(proxyhost);
                String proxyport = cleanproperty("https.proxyPort");
                if(proxyport != null) {
                    buf.append(":");
                    buf.append(proxyport);
                }
                proxyurl = buf.toString();
            }
        }
        return proxyurl;

    }

    static void
    buildsslfactory(AuthControls authcontrols, KeyStore truststore, KeyStore keystore, String keypassword)
    {
        SSLConnectionSocketFactory globalsslfactory;
        try {
            // set up the context
            SSLContext scxt = null;
            if(IGNORECERTS) {
                scxt = SSLContext.getInstance("TLS");
                TrustManager[] trust_mgr = new TrustManager[]{
                        new X509TrustManager()
                        {
                            public X509Certificate[] getAcceptedIssuers()
                            {
                                return null;
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String t)
                            {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String t)
                            {
                            }
                        }};
                scxt.init(null,               // key manager
                        trust_mgr,          // trust manager
                        new SecureRandom()); // random number generator
            } else {
                SSLContextBuilder sslbuilder = SSLContexts.custom();
                TrustStrategy strat = new LooseTrustStrategy();
                if(truststore != null)
                    sslbuilder.loadTrustMaterial(truststore, strat);
                else
                    sslbuilder.loadTrustMaterial(strat);
                sslbuilder.loadTrustMaterial(truststore, new LooseTrustStrategy());
                if(keystore != null)
                    sslbuilder.loadKeyMaterial(keystore, keypassword.toCharArray());
                scxt = sslbuilder.build();
            }
            globalsslfactory = new SSLConnectionSocketFactory(scxt, new NoopHostnameVerifier());
        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | KeyManagementException
                | UnrecoverableEntryException e) {
            log.error("Failed to set key/trust store(s): " + e.getMessage());
            globalsslfactory = null;
        }
        if(globalsslfactory != null)
            authcontrols.put(AuthProp.SSLFACTORY, globalsslfactory);
    }

    //////////////////////////////////////////////////////////////////////////
    // Static Methods (Mostly global accessors)
    // Synchronized as a rule.

    static synchronized public void setGlobalUserAgent(String userAgent)
    {
        globalsettings.put(Prop.USER_AGENT, userAgent);
    }

    static synchronized public String getGlobalUserAgent()
    {
        return (String) globalsettings.get(Prop.USER_AGENT);
    }

    static synchronized public void setGlobalMaxConnections(int n)
    {
        globalsettings.put(Prop.MAX_CONNECTIONS, n);
        connmgr.setMaxConnections(n);
    }

    static synchronized public int getGlobalMaxConnection()
    {
        return (Integer) globalsettings.get(Prop.MAX_CONNECTIONS);
    }

    // Timeouts

    static synchronized public void setGlobalConnectionTimeout(int timeout)
    {
        if(timeout >= 0) {
            globalsettings.put(Prop.CONN_TIMEOUT, (Integer) timeout);
            globalsettings.put(Prop.CONN_REQ_TIMEOUT, (Integer) timeout);
        }
    }

    static synchronized public void setGlobalSoTimeout(int timeout)
    {
        if(timeout >= 0) globalsettings.put(Prop.SO_TIMEOUT, (Integer) timeout);
    }

    /**
     * Enable/disable redirection following
     * Default is yes.
     */
    static synchronized public void setGlobalFollowRedirects(boolean tf)
    {
        globalsettings.put(Prop.HANDLE_REDIRECTS, (Boolean) tf);
    }


    /**
     * Set the max number of redirects to follow
     *
     * @param n
     */
    static synchronized public void setGlobalMaxRedirects(int n)
    {
        if(n < 0) //validate
            throw new IllegalArgumentException("setMaxRedirects");
        globalsettings.put(Prop.MAX_REDIRECTS, n);
    }

    static synchronized public Object getGlobalSetting(String key)
    {
        return globalsettings.get(key);
    }

    //////////////////////////////////////////////////
    // Compression

    static synchronized public void
    setGlobalCompression(String compressors)
    {
        if(globalsettings.get(Prop.COMPRESSION) != null)
            removeGlobalCompression();
        String compresslist = checkCompressors(compressors);
        if(HTTPUtil.nullify(compresslist) == null)
            throw new IllegalArgumentException("Bad compressors: " + compressors);
        globalsettings.put(Prop.COMPRESSION, compresslist);
        HttpResponseInterceptor hrsi;
        if(compresslist.contains("gzip")) {
            hrsi = new GZIPResponseInterceptor();
            rspintercepts.add(hrsi);
        }
        if(compresslist.contains("deflate")) {
            hrsi = new DeflateResponseInterceptor();
            rspintercepts.add(hrsi);
        }
    }

    static synchronized public void
    removeGlobalCompression()
    {
        if(globalsettings.remove(Prop.COMPRESSION) != null) {
            for(int i = rspintercepts.size() - 1; i >= 0; i--) { // walk backwards
                HttpResponseInterceptor hrsi = rspintercepts.get(i);
                if(hrsi instanceof GZIPResponseInterceptor
                        || hrsi instanceof DeflateResponseInterceptor)
                    rspintercepts.remove(i);
            }
        }
    }

    static synchronized protected String
    checkCompressors(String compressors)
    {
        // Syntactic check of compressors
        Set<String> cset = new HashSet<>();
        compressors = compressors.replace(',', ' ');
        compressors = compressors.replace('\t', ' ');
        String[] pieces = compressors.split("[ ]+");
        for(String p : pieces) {
            for(String c : KNOWNCOMPRESSORS) {
                if(p.equalsIgnoreCase(c)) {
                    cset.add(c);
                    break;
                }
            }
        }
        StringBuilder buf = new StringBuilder();
        for(String s : cset) {
            if(buf.length() > 0) buf.append(",");
            buf.append(s);
        }
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // Authorization

    /**
     * @param factory the credentials provider factory
     * @param scope   where to use it (i.e. on what host)
     * @throws HTTPException
     */
    static public void
    setCredentialsProviderFactory(HTTPProviderFactory factory, AuthScope scope)
            throws HTTPException
    {
        if(factory == null)
            throw new NullPointerException(HTTPSession.class.getName());
        if(scope == null)
            scope = AuthScope.ANY;
        globalcredfactories.put(scope, factory);
    }

    /**
     * It is convenient to be able to directly set the Credentials
     * (not the provider) when those credentials are fixed.
     * Scope defaults to ANY
     *
     * @param creds
     * @throws HTTPException
     */
    static public void
    setGlobalCredentials(Credentials creds)
            throws HTTPException
    {
        setGlobalCredentials(creds, null);
    }

    /**
     * It is convenient to be able to directly set the Credentials
     * (not the provider) when those credentials are fixed.
     *
     * @param creds
     * @param scope where to use it (i.e. on what host)
     * @throws HTTPException
     */
    static public void
    setGlobalCredentials(Credentials creds, AuthScope scope)
            throws HTTPException
    {
        assert (creds != null);
        if(scope == null) scope = AuthScope.ANY;
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(scope, creds);
        setGlobalCredentialsProvider(provider, scope);
    }

    static synchronized public void setGlobalRetryCount(int n)
    {
        if(n < 0) //validate
            throw new IllegalArgumentException("setGlobalRetryCount");
        globalsettings.put(Prop.RETRIES, n);
        globalretryhandler = new DefaultHttpRequestRetryHandler(n, false);
    }

    //////////////////////////////////////////////////
    // Instance variables

    // Currently, the granularity of authorization is host+port.
    protected String sessionURI = null; // This is either a real url
    // or one constructed from an AuthScope
    protected URI scopeURI = null; // constructed
    protected AuthScope scope = null;
    protected boolean closed = false;

    // Since can't access CredentialsProvider map, mimic
    protected Map<AuthScope, CredentialsProvider> localcreds = new HashMap<>();

    protected ConcurrentSkipListSet<HTTPMethod> methods = new ConcurrentSkipListSet<>();
    protected String identifier = "Session";
    protected Map<Prop, Object> localsettings = new ConcurrentHashMap<Prop, Object>();

    // We currently only allow the use of global interceptors
//    protected List<Object> intercepts = new ArrayList<Object>(); // current set of interceptors;

    // This context is re-used over all method executions so that we maintain
    // cookies, credentials, etc.
    // But we do need a way to clear so that e.g. we can clear credentials cache
    protected HttpClientContext sessioncontext = HttpClientContext.create();
    protected BasicAuthCache sessioncache = new BasicAuthCache();

    protected URI requestURI = null;  // full uri from the HTTPMethod call

    // cached and recreated as needed
    protected boolean cachevalid = false; // Are cached items up-to-date?
    protected RequestConfig cachedconfig = null;

    //////////////////////////////////////////////////
    // Constructor(s)
    // All are package level so that only HTTPFactory can be used externally

    protected HTTPSession()
            throws HTTPException
    {
    }

    HTTPSession(String host, int port)
            throws HTTPException
    {
        init(new AuthScope(host, port, null, null), null);
    }

    HTTPSession(String uri)
            throws HTTPException
    {
        init(HTTPAuthUtil.uriToAuthScope(uri), uri);
    }

    HTTPSession(HttpHost httphost)
            throws HTTPException
    {
        init(HTTPAuthUtil.hostToAuthScope(httphost), null);
    }

    protected void init(AuthScope scope, String actualurl)
            throws HTTPException
    {
        assert (scope != null);
        if(actualurl != null)
            this.sessionURI = actualurl;
        else
            this.sessionURI = HTTPAuthUtil.authscopeToURI(scope).toString();
        this.scope = scope;
        this.scopeURI = HTTPAuthUtil.authscopeToURI(scope);
        this.cachevalid = false; // Force build on first use
        this.sessioncontext.setCookieStore(new BasicCookieStore());
        this.sessioncache = new BasicAuthCache();
        this.sessioncontext.setAuthCache(sessioncache);
    }

    //////////////////////////////////////////////////
    // Interceptors: Only supported at global level

    static protected void
    setInterceptors(HttpClientBuilder cb)
    {
        for(HttpRequestInterceptor hrq : reqintercepts) {
            cb.addInterceptorLast(hrq);
        }
        for(HttpResponseInterceptor hrs : rspintercepts) {
            cb.addInterceptorLast(hrs);
        }
        // Add debug interceptors
        for(HttpRequestInterceptor hrq : dbgreq) {
            cb.addInterceptorFirst(hrq);
        }
        for(HttpResponseInterceptor hrs : dbgrsp) {
            cb.addInterceptorFirst(hrs);
        }
        // Hack: add Content-Encoding suppressor
        cb.addInterceptorFirst(CEKILL);
    }

    //////////////////////////////////////////////////
    // Accessor(s)

    public AuthScope getAuthScope()
    {
        return this.scope;
    }

    public String getSessionURI()
    {
        return this.sessionURI;

    }

    /**
     * Extract the sessionid cookie value
     *
     * @return sessionid string
     */
    public String getSessionID()
    {
        String sid = null;
        String jsid = null;
        List<Cookie> cookies = this.sessioncontext.getCookieStore().getCookies();
        for(Cookie cookie : cookies) {
            if(cookie.getName().equalsIgnoreCase("sessionid"))
                sid = cookie.getValue();
            if(cookie.getName().equalsIgnoreCase("jsessionid"))
                jsid = cookie.getValue();
        }
        return (sid == null ? jsid : sid);
    }

    public HTTPSession setUserAgent(String agent)
    {
        if(agent == null || agent.length() == 0) throw new IllegalArgumentException("null argument");
        localsettings.put(Prop.USER_AGENT, agent);
        this.cachevalid = false;
        return this;
    }

    public HTTPSession setSoTimeout(int timeout)
    {
        if(timeout <= 0)
            throw new IllegalArgumentException("setSoTimeout");
        localsettings.put(Prop.SO_TIMEOUT, timeout);
        this.cachevalid = false;
        return this;
    }

    public HTTPSession setConnectionTimeout(int timeout)
    {
        if(timeout <= 0)
            throw new IllegalArgumentException("setConnectionTImeout");
        localsettings.put(Prop.CONN_TIMEOUT, timeout);
        localsettings.put(Prop.CONN_REQ_TIMEOUT, timeout);
        this.cachevalid = false;
        return this;
    }

    /**
     * Set the max number of redirects to follow
     *
     * @param n
     */
    public HTTPSession setMaxRedirects(int n)
    {
        if(n < 0) //validate
            throw new IllegalArgumentException("setMaxRedirects");
        localsettings.put(Prop.MAX_REDIRECTS, n);
        this.cachevalid = false;
        return this;
    }

    /**
     * Enable/disable redirection following
     * Default is yes.
     */
    public HTTPSession setFollowRedirects(boolean tf)
    {
        localsettings.put(Prop.HANDLE_REDIRECTS, (Boolean) tf);
        this.cachevalid = false;
        return this;
    }

    /**
     * Should we use sessionid's?
     *
     * @param tf
     */
    public HTTPSession setUseSessions(boolean tf)
    {
        localsettings.put(Prop.USESESSIONS, (Boolean) tf);
        this.cachevalid = false;
        return this;
    }

    public List<Cookie> getCookies()
    {
        if(this.sessioncontext == null)
            return null;
        List<Cookie> cookies = this.sessioncontext.getCookieStore().getCookies();
        return cookies;
    }

    public HTTPSession clearCookies()
    {
        BasicCookieStore cookies = (BasicCookieStore) this.sessioncontext.getCookieStore();
        if(cookies != null) cookies.clear();
        return this;
    }

    public HTTPSession clearCredentialsCache()
    {
        if(this.sessioncache != null)
            this.sessioncache.clear();
        return this;
    }

    public HTTPSession clearCredentialsCache(AuthScope template)
    {
        if(this.sessioncache != null) {
            HttpHost h = HTTPAuthUtil.authscopeToHost(template);
            this.sessioncache.remove(h);
        }
        return this;
    }

    public BasicAuthCache getCredentialsCache()
    {
        return this.sessioncache;
    }


    // make package specific

    HttpClientContext
    getExecutionContext()
    {
        return this.sessioncontext;
    }

    public Object getSetting(String key)
    {
        return localsettings.get(key);
    }

    //////////////////////////////////////////////////

    /**
     * Close the session. This implies closing
     * any open methods.
     */

    synchronized public void close()
    {
        if(this.closed)
            return; // multiple calls ok
        closed = true;
        for(HTTPMethod m : this.methods) {
            m.close(); // forcibly close; will invoke removemethod().
        }
        methods.clear();
    }

    synchronized HTTPSession addMethod(HTTPMethod m)
    {
        if(!this.methods.contains(m))
            this.methods.add(m);
        return this;
    }

    synchronized HTTPSession removeMethod(HTTPMethod m)
    {
        this.methods.remove(m);
        connmgr.freeManager(m);
        return this;
    }

    //////////////////////////////////////////////////
    // Authorization
    // per-session versions of the global accessors

    /**
     * @param provider
     * @throws HTTPException
     */
    public HTTPSession setCredentialsProvider(CredentialsProvider provider)
            throws HTTPException
    {
        setCredentialsProvider(provider, null);
        return this;
    }

    /**
     * This is the most general case
     *
     * @param provider the credentials provider
     * @param scope    where to use it (i.e. on what host+port)
     * @throws HTTPException
     */
    public HTTPSession setCredentialsProvider(CredentialsProvider provider, AuthScope scope)
            throws HTTPException
    {
        if(provider == null)
            throw new NullPointerException(this.getClass().getName());
        if(scope == null)
            scope = AuthScope.ANY;
        localcreds.put(scope, provider);
        return this;
    }

    /**
     * It is convenient to be able to directly set the Credentials
     * (not the provider) when those credentials are fixed.
     * Scope defaults to ANY
     *
     * @param creds
     * @throws HTTPException
     */
    public HTTPSession setCredentials(Credentials creds)
            throws HTTPException
    {
        setCredentials(creds, null);
        return this;
    }

    /**
     * It is convenient to be able to directly set the Credentials
     * (not the provider) when those credentials are fixed.
     *
     * @param creds
     * @param scope where to use it (i.e. on what host)
     * @throws HTTPException
     */
    public HTTPSession
    setCredentials(Credentials creds, AuthScope scope)
            throws HTTPException
    {
        assert (creds != null);
        if(scope == null) scope = AuthScope.ANY;
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(scope, creds);
        setCredentialsProvider(provider, scope);
        return this;
    }


    //////////////////////////////////////////////////
    // Called only by HTTPMethod.execute to get relevant session state.

    /**
     * Handle authentication and Proxy'ing
     *
     * @param cb
     * @throws HTTPException
     */

    synchronized
    protected void
    setAuthenticationAndProxy(HttpClientBuilder cb)
            throws HTTPException
    {
        // First, setup the ssl factory
        cb.setSSLSocketFactory((SSLConnectionSocketFactory) authcontrols.get(AuthProp.SSLFACTORY));

        // Second, Construct a CredentialsProvider that is
        // the union of the Proxy credentials plus
        // either the global local credentials; local overrides global
        // Unfortunately, we cannot either clone or extract the contents
        // of the client supplied provider, so we are forced (for now)
        // to modify the client supplied provider.

        // Look in the local credentials first for for best scope match
        AuthScope bestMatch = HTTPAuthUtil.bestmatch(scope, localcreds.keySet());
        CredentialsProvider cp = null;
        if(bestMatch != null) {
            cp = localcreds.get(bestMatch);
        } else {
            bestMatch = HTTPAuthUtil.bestmatch(scope, globalcredfactories.keySet());
            if(bestMatch != null) {
                HTTPProviderFactory factory = globalcredfactories.get(bestMatch);
                cp = factory.getProvider(bestMatch);
            }
        }
        // Build the proxy credentials and AuthScope
        Credentials proxycreds = null;
        AuthScope proxyscope = null;
        String user = (String) authcontrols.get(AuthProp.PROXYUSER);
        String pwd = (String) authcontrols.get(AuthProp.PROXYPWD);
        HttpHost httpproxy = (HttpHost) authcontrols.get(AuthProp.HTTPPROXY);
        HttpHost httpsproxy = (HttpHost) authcontrols.get(AuthProp.HTTPSPROXY);
        if(user != null && (httpproxy != null || httpsproxy != null)) {
            if(httpproxy != null)
                proxyscope = HTTPAuthUtil.hostToAuthScope(httpproxy);
            else //httpsproxy != null
                proxyscope = HTTPAuthUtil.hostToAuthScope(httpsproxy);
            proxycreds = new UsernamePasswordCredentials(user, pwd);
        }
        if(cp == null && proxycreds != null && proxyscope != null) {
            // If client provider is null and proxycreds are not,
            // then use proxycreds alone
            cp = new BasicCredentialsProvider();
            cp.setCredentials(proxyscope, proxycreds);
        } else if(cp != null && proxycreds != null && proxyscope != null) {
            // If client provider is not null and proxycreds are not,
            // then add proxycreds to the client provider
            cp.setCredentials(proxyscope, proxycreds);
        }
        if(cp != null)
            this.sessioncontext.setCredentialsProvider(cp);
    }

    /*package*/
    void
    setRetryHandler(HttpClientBuilder cb)
            throws HTTPException
    {
        if(globalretryhandler != null) {
            cb.setRetryHandler(globalretryhandler);
        }
    }

    /*package*/
    void
    setContentDecoderRegistry(HttpClientBuilder cb)
    {
        cb.setContentDecoderRegistry(contentDecoderMap);
    }

    /*package*/
    void setClientManager(HttpClientBuilder cb, HTTPMethod m)
    {
        connmgr.setClientManager(cb, m);
    }

    /*package*/
    HttpClientContext
    getContext()
    {
        return this.sessioncontext;
    }

    /*package*/
    AuthScope
    getSessionScope()
    {
        return this.scope;
    }

    /*package scope*/
    Map<Prop, Object> mergedSettings()
    {
        Map<Prop, Object> merged;
        synchronized (this) {// keep coverity happy
            //Merge Settings;
            merged = HTTPUtil.merge(globalsettings, localsettings);
        }
        return Collections.unmodifiableMap(merged);
    }

    //////////////////////////////////////////////////
    // Utilities

    static String getCanonicalURL(String legalurl)
    {
        if(legalurl == null) return null;
        int index = legalurl.indexOf('?');
        if(index >= 0) legalurl = legalurl.substring(0, index);
        // remove any trailing extension
        //index = legalurl.lastIndexOf('.');
        //if(index >= 0) legalurl = legalurl.substring(0,index);
        return HTTPUtil.canonicalpath(legalurl);
    }


    static String getUrlAsString(String url)
            throws HTTPException
    {
        try (
                HTTPMethod m = HTTPFactory.Get(url);) {
            int status = m.execute();
            String content = null;
            if(status == 200) {
                content = m.getResponseAsString();
            }
            return content;
        }
    }

    static int putUrlAsString(String content, String url)
            throws HTTPException
    {
        int status = 0;
        try {
            try (HTTPMethod m = HTTPFactory.Put(url)) {
                m.setRequestContent(new StringEntity(content,
                        ContentType.create("application/text", "UTF-8")));
                status = m.execute();
            }
        } catch (UnsupportedCharsetException uce) {
            throw new HTTPException(uce);
        }
        return status;
    }

    static String getstorepath(String prefix)
    {
        String path = System.getProperty(prefix + "store");
        if(path != null) {
            path = path.trim();
            if(path.length() == 0) path = null;
        }
        return path;
    }

    static String getpassword(String prefix)
    {
        String password = System.getProperty(prefix + "storepassword");
        if(password != null) {
            password = password.trim();
            if(password.length() == 0) password = null;
        }
        return password;
    }

    static String cleanproperty(String property)
    {
        String value = System.getProperty(property);
        if(value != null) {
            value = value.trim();
            if(value.length() == 0) value = null;
        }
        return value;
    }

    //////////////////////////////////////////////////
    // Testing support

    // Expose the state for testing purposes
    synchronized public boolean isClosed()
    {
        return this.closed;
    }

    synchronized public int getMethodcount()
    {
        return methods.size();
    }

    //////////////////////////////////////////////////
    // Debug interface

    // Provide a way to kill everything at the end of a Test

    // When testing, we need to be able to clean up
    // all existing sessions because JUnit can run all
    // test within a single jvm.
    static Set<HTTPSession> sessionList = null; // List of all HTTPSession instances

    // If we are testing, then track the sessions for kill
    static protected synchronized void track(HTTPSession session)
    {
        if(!TESTING) throw new UnsupportedOperationException();
        if(sessionList == null)
            sessionList = new ConcurrentSkipListSet<HTTPSession>();
        sessionList.add(session);
    }

    static synchronized public void debugHeaders(boolean print)
    {
        if(!TESTING) throw new UnsupportedOperationException();
        HTTPUtil.InterceptRequest rq = new HTTPUtil.InterceptRequest();
        HTTPUtil.InterceptResponse rs = new HTTPUtil.InterceptResponse();
        rq.setPrint(print);
        rs.setPrint(print);
        /* remove any previous */
        for(int i = reqintercepts.size() - 1; i >= 0; i--) {
            HttpRequestInterceptor hr = reqintercepts.get(i);
            if(hr instanceof HTTPUtil.InterceptCommon)
                reqintercepts.remove(i);
        }
        for(int i = rspintercepts.size() - 1; i >= 0; i--) {
            HttpResponseInterceptor hr = rspintercepts.get(i);
            if(hr instanceof HTTPUtil.InterceptCommon)
                rspintercepts.remove(i);
        }
        reqintercepts.add(rq);
        rspintercepts.add(rs);
    }

    public static void
    debugReset()
    {
        if(!TESTING) throw new UnsupportedOperationException();
        for(HttpRequestInterceptor hri : reqintercepts) {
            if(hri instanceof HTTPUtil.InterceptCommon)
                ((HTTPUtil.InterceptCommon) hri).clear();
        }
    }

    public static HTTPUtil.InterceptRequest
    debugRequestInterceptor()
    {
        if(!TESTING) throw new UnsupportedOperationException();
        for(HttpRequestInterceptor hri : reqintercepts) {
            if(hri instanceof HTTPUtil.InterceptRequest)
                return ((HTTPUtil.InterceptRequest) hri);
        }
        return null;
    }

    public static HTTPUtil.InterceptResponse
    debugResponseInterceptor()
    {
        if(!TESTING) throw new UnsupportedOperationException();
        for(HttpResponseInterceptor hri : rspintercepts) {
            if(hri instanceof HTTPUtil.InterceptResponse)
                return ((HTTPUtil.InterceptResponse) hri);
        }
        return null;
    }


    /* Only allow if debugging */
    static public void
    clearkeystore()
    {
        if(!TESTING) throw new UnsupportedOperationException();
        authcontrols.setReadOnly(false);
        authcontrols.remove(AuthProp.KEYSTORE);
        authcontrols.remove(AuthProp.KEYPASSWORD);
        authcontrols.remove(AuthProp.TRUSTSTORE);
        authcontrols.remove(AuthProp.TRUSTPASSWORD);
        authcontrols.setReadOnly(true);
    }

    /* Only allow if debugging */
    static public void
    rebuildkeystore(String path, String pwd)
    {
        if(!TESTING) throw new UnsupportedOperationException();
        KeyStore newks = buildkeystore(path, pwd);
        authcontrols.setReadOnly(false);
        authcontrols.put(AuthProp.KEYSTORE, newks);
        authcontrols.setReadOnly(true);
    }

    //////////////////////////////////////////////////
    // Deprecated, but here for back compatibility

    @Deprecated
    static public void
    setGlobalCredentialsProvider(AuthScope scope, CredentialsProvider provider)
            throws HTTPException
    {
        setGlobalCredentialsProvider(provider, scope);
    }

    @Deprecated
    static public void
    setGlobalCredentialsProvider(String url, CredentialsProvider provider)
            throws HTTPException
    {
        assert (url != null && provider != null);
        AuthScope scope = HTTPAuthUtil.uriToAuthScope(url);
        setGlobalCredentialsProvider(provider, scope);
    }

    @Deprecated
    static public void
    setGlobalCredentials(String url, Credentials creds)
            throws HTTPException
    {
        assert (url != null && creds != null);
        AuthScope scope = HTTPAuthUtil.uriToAuthScope(url);
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(scope, creds);
        setGlobalCredentialsProvider(provider, scope);
    }

    @Deprecated
    public HTTPSession
    setCredentials(String url, Credentials creds)
            throws HTTPException
    {
        assert (creds != null);
        AuthScope scope = HTTPAuthUtil.uriToAuthScope(url);
        setCredentials(creds, scope);
        return this;
    }

    @Deprecated
    public HTTPSession
    setCredentialsProvider(String url, CredentialsProvider provider)
            throws HTTPException
    {
        assert (url != null && provider != null);
        AuthScope scope = HTTPAuthUtil.uriToAuthScope(url);
        setCredentialsProvider(provider, scope);
        return this;
    }

    @Deprecated
    public HTTPSession
    setCredentialsProvider(AuthScope scope, CredentialsProvider provider)
            throws HTTPException
    {
        return setCredentialsProvider(provider, scope);
    }

    @Deprecated
    static public int getRetryCount()
    {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    static public void setGlobalCompression()
    {
        setGlobalCompression("gzip,deflate");
    }

    @Deprecated
    static public void setGlobalProxy(String host, int port)
    {
        throw new UnsupportedOperationException("setGlobalProxy: use -D flags");
    }

    @Deprecated
    public void setProxy(String host, int port)
    {
        setGlobalProxy(host, port);
    }


    @Deprecated
    static public void setGlobalCredentialsProvider(CredentialsProvider provider, String scheme) throws HTTPException
    {
        setGlobalCredentialsProvider(provider);
    }

    @Deprecated
    public void clearState()
    {
        // no-op
    }

    @Deprecated
    public String getSessionURL()
    {
        return getSessionURI();
    }


    //////////////////////////////////////////////////
    // obsolete

    static protected synchronized void kill()
    {
        if(sessionList != null) {
            for(HTTPSession session : sessionList) {
                session.close();
            }
            sessionList.clear();
            connmgr.close();
        }
    }


    static public void validatestate()
    {
        connmgr.validate();
    }

    /**
     * @param provider
     * @throws HTTPException
     */
    @Deprecated
    static public void
    setGlobalCredentialsProvider(CredentialsProvider provider)
            throws HTTPException
    {
        setGlobalCredentialsProvider(provider, (AuthScope) null);
    }

    /**
     * This is the most general case
     *
     * @param provider the credentials provider
     * @param scope    where to use it (i.e. on what host)
     * @throws HTTPException
     */
    @Deprecated
    static public void
    setGlobalCredentialsProvider(CredentialsProvider provider, AuthScope scope)
            throws HTTPException
    {
        HTTPProviderFactory factory = new SingleProviderFactory(provider);
        setCredentialsProviderFactory(factory, scope);
    }

}
