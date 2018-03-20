/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.httpservices;

import org.apache.http.*;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract public class HTTPUtil
{

    //////////////////////////////////////////////////
    // Constants

    static final public Charset UTF8 = Charset.forName("UTF-8");
    static final public Charset ASCII = Charset.forName("US-ASCII");
    static final public String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    static final public String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final public String DRIVELETTERS = LOWERCASE + UPPERCASE;

    //////////////////////////////////////////////////

    enum URIPart
    {
        SCHEME, USERINFO, HOST, PORT, PATH, QUERY, FRAGMENT
    }

    //////////////////////////////////////////////////
    // Interceptors

    static abstract class InterceptCommon
    {
        protected HttpContext context = null;
        protected List<Header> headers = new ArrayList<Header>();
        protected HttpRequest request = null;
        protected HttpResponse response = null;
        protected boolean printheaders = false;

        public InterceptCommon setPrint(boolean tf)
        {
            this.printheaders = tf;
            return this;
        }

        public void
        clear()
        {
            context = null;
            headers.clear();
            request = null;
            response = null;
        }

        synchronized public HttpRequest getRequest()
        {
            return this.request;
        }

        synchronized public HttpResponse getResponse()
        {
            return this.response;
        }

        synchronized public HttpContext getContext()
        {
            return this.context;
        }

        synchronized public HttpEntity getRequestEntity()
        {
            if(this.request != null
                    && this.request instanceof HttpEntityEnclosingRequest) {
                return ((HttpEntityEnclosingRequest) this.request).getEntity();
            } else
                return null;
        }

        synchronized HttpEntity getResponseEntity()
        {
            if(this.response != null) {
                return this.response.getEntity();
            } else
                return null;
        }

        synchronized public List<Header> getHeaders(String key)
        {
            List<Header> keyh = new ArrayList<Header>();
            for(Header h : this.headers) {
                if(h.getName().equalsIgnoreCase(key.trim()))
                    keyh.add(h);
            }
            return keyh;
        }

        synchronized List<Header> getHeaders()
        {
            return this.headers;
        }

        void
        printHeaders()
        {
            if(this.request != null) {
                Header[] hdrs = this.request.getAllHeaders();
                if(hdrs == null) hdrs = new Header[0];
                System.err.println("Request Headers:");
                for(Header h : hdrs) {
                    System.err.println(h.toString());
                }
            }
            if(this.response != null) {
                Header[] hdrs = this.response.getAllHeaders();
                if(hdrs == null) hdrs = new Header[0];
                System.err.println("Response Headers:");
                for(Header h : hdrs) {
                    System.err.println(h.toString());
                }
            }
            System.err.flush();
        }
    }

    static public class InterceptResponse extends InterceptCommon
            implements HttpResponseInterceptor
    {
        synchronized public void
        process(HttpResponse response, HttpContext context)
                throws HttpException, IOException
        {
            this.response = response;
            this.context = context;
            if(this.printheaders)
                printHeaders();
            else if(this.response != null) {
                Header[] hdrs = this.response.getAllHeaders();
                for(int i = 0; i < hdrs.length; i++) {
                    headers.add(hdrs[i]);
                }
            }
        }
    }

    static public class InterceptRequest extends InterceptCommon
            implements HttpRequestInterceptor
    {
        synchronized public void
        process(HttpRequest request, HttpContext context)
                throws HttpException, IOException
        {
            this.request = request;
            this.context = context;
            if(this.printheaders)
                printHeaders();
            else if(this.request != null) {
                Header[] hdrs = this.request.getAllHeaders();
                for(int i = 0; i < hdrs.length; i++) {
                    headers.add(hdrs[i]);
                }
            }
        }
    }

    //////////////////////////////////////////////////
    // Misc.

    static public byte[]
    readbinaryfile(File f)
            throws IOException
    {
        try (FileInputStream fis = new FileInputStream(f)) {
            return readbinaryfile(fis);
        }
    }

    static public byte[]
    readbinaryfile(InputStream stream)
            throws IOException
    {
        // Extract the stream into a bytebuffer
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] tmp = new byte[1 << 16];
        for(; ; ) {
            int cnt;
            cnt = stream.read(tmp);
            if(cnt <= 0) break;
            bytes.write(tmp, 0, cnt);
        }
        return bytes.toByteArray();
    }

    static public File
    fillTempFile(String base, String content)
            throws IOException
    {
        // Locate a temp directory
        String tmppath = System.getenv("TEMP");
        if(tmppath == null || tmppath.length() == 0)
            tmppath = "/tmp";
        File tmpdir = new File(tmppath);
        if(!tmpdir.exists() || !tmpdir.canWrite())
            throw new IOException("Cannot create temp file: no tmp dir");
        try {
            String suffix;
            String prefix;
            int index = base.lastIndexOf('.');
            if(index < 0) index = base.length();
            suffix = base.substring(index, base.length());
            prefix = base.substring(0, index);
            if(prefix.length() == 0)
                throw new IOException("Malformed base: " + base);
            File f = File.createTempFile(prefix, suffix, tmpdir);
            // Fill with the content
            FileOutputStream fw = new FileOutputStream(f);
            fw.write(content.getBytes(UTF8));
            fw.close();
            return f;
        } catch (IOException e) {
            throw new IOException("Cannot create temp file", e);
        }
    }


    /**
     * @return {@code true} if the objects are equal or both null
     */
    static boolean equals(final Object obj1, final Object obj2)
    {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }

    /**
     * @return {@code true} if the objects are equal or both null
     */
    static boolean schemeEquals(String s1, String s2)
    {
        if(s1 == s2) return true;
        if((s1 == null) ^ (s2 == null)) return false;
        if((s1.length() == 0) ^ (s2.length() == 0)) return true;
        return s1.equals(s2);
    }

    /**
     * Convert a uri string to an instance of java.net.URI.
     * The critical thing is that this procedure can handle backslash
     * escaped uris as well as %xx escaped uris.
     *
     * @param u the uri to convert
     * @return The URI corresponding to u.
     * @throws URISyntaxException
     */
    static public URI
    parseToURI(final String u)
            throws URISyntaxException
    {
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while(i < u.length()) {
            char c = u.charAt(i);
            if(c == '\\') {
                if(i + 1 == u.length())
                    throw new URISyntaxException(u, "Trailing '\' at end of url");
                buf.append("%5c");
                i++;
                c = u.charAt(i);
                buf.append(String.format("%%%02x", (int) c));
            } else
                buf.append(c);
            i++;
        }
        return new URI(buf.toString());
    }

    /**
     * Remove selected fields from a  URI producing a new URI
     *
     * @param uri      the uri to convert
     * @param excludes the parts to exclude
     * @return The new URI instance
     */
    static URI
    uriExclude(final URI uri, URIPart... excludes)
    {
        URIBuilder urib = new URIBuilder();
        EnumSet<URIPart> set = EnumSet.of(excludes[0], excludes);
        for(URIPart part : URIPart.values()) {
            if(set.contains(part)) continue;
            switch (part) {
            case SCHEME:
                urib.setScheme(uri.getScheme());
                break;
            case USERINFO:
                urib.setUserInfo(uri.getUserInfo());
                break;
            case HOST:
                urib.setHost(uri.getHost());
                break;
            case PORT:
                urib.setPort(uri.getPort());
                break;
            case PATH:
                urib.setPath(uri.getPath());
                break;
            case QUERY:
                urib.setCustomQuery(uri.getQuery());
                break;
            case FRAGMENT:
                urib.setFragment(uri.getFragment());
                break;
            }
        }
        try {
            return urib.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Temporary hack to remove Content-Encoding: XXX-Endian headers
     */
    static class ContentEncodingInterceptor extends InterceptCommon
            implements HttpResponseInterceptor
    {
        synchronized public void
        process(HttpResponse response, HttpContext context)
                throws HttpException, IOException
        {
            if(response == null) return;
            Header[] hdrs = response.getAllHeaders();
            if(hdrs == null) return;
            boolean modified = false;
            for(int i = 0; i < hdrs.length; i++) {
                Header h = hdrs[i];
                if(!h.getName().equalsIgnoreCase("content-encoding")) continue;
                String value = h.getValue();
                if(value.trim().toLowerCase().endsWith("-endian")) {
                    hdrs[i] = new BasicHeader("X-Content-Encoding", value);
                    modified = true;
                }
            }
            if(modified)
                response.setHeaders(hdrs);
            // Similarly, suppress encoding for Entity
            HttpEntity entity = response.getEntity();
            if(entity != null) {
                Header ceheader = entity.getContentEncoding();
                if(ceheader != null) {
                    String value = ceheader.getValue();
                }
            }
        }
    }

    static protected Map<HTTPSession.Prop, Object>
    merge(Map<HTTPSession.Prop, Object> globalsettings, Map<HTTPSession.Prop, Object> localsettings)
    {
        // merge global and local settings; local overrides global.
        Map<HTTPSession.Prop, Object> merge = new ConcurrentHashMap<HTTPSession.Prop, Object>();
        for(HTTPSession.Prop key : globalsettings.keySet()) {
            merge.put(key, globalsettings.get(key));
        }
        for(HTTPSession.Prop key : localsettings.keySet()) {
            merge.put(key, localsettings.get(key));
        }
        return merge;
    }


    /**
     * Convert a zero-length string to null
     *
     * @param s the string to check for length
     * @return null if s.length() == 0, s otherwise
     */
    static public String nullify(String s)
    {
        if(s != null && s.length() == 0) s = null;
        return s;
    }

    String joinList(List<String> list, String delim)
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < list.size(); i++) {
            if(i > 0) buf.append(delim);
            buf.append(list.get(i));
        }
        return buf.toString();
    }

    /**
     * Join two string together to form proper path
     * WITHOUT trailing slash
     *
     * @param prefix
     * @param suffix
     * @return
     */
    static public String
    canonjoin(String prefix, String suffix)
    {
        if(prefix == null) prefix = "";
        if(suffix == null) suffix = "";
        prefix = HTTPUtil.canonicalpath(prefix);
        suffix = HTTPUtil.canonicalpath(suffix);
        StringBuilder result = new StringBuilder();
        result.append(prefix);
        int prelen = prefix.length();
        if(prelen > 0 && result.charAt(prelen - 1) != '/') {
            result.append('/');
            prelen++;
        }
        if(suffix.length() > 0 && suffix.charAt(0) == '/')
            result.append(suffix.substring(1));
        else
            result.append(suffix);
        int len = result.length();
        if(len > 0 && result.charAt(len - 1) == '/') {
            result.deleteCharAt(len - 1);
            len--;
        }
        return result.toString();
    }

    /**
     * Convert path to use '/' consistently and
     * to remove any trailing '/'
     *
     * @param path convert this path
     * @return canonicalized version
     */

    static public String
    canonicalpath(String path)
    {
        if(path == null) return null;
        StringBuilder b = new StringBuilder(path);
        canonicalpath(b);
        return b.toString();
    }

    static public void
    canonicalpath(StringBuilder s)
    {
        if(s == null || s.length() == 0)
            return;
        int index = 0;
        // "\\" -> "/"
        for(; ; ) {
            index = s.indexOf("\\", index);
            if(index < 0) break;
            s.replace(index, index + 1, "/");
        }
        boolean isabs = (s.charAt(0) == '/'); // remember
        for(; ; ) { // kill any leading '/'s
            if(s.length() == 0 || s.charAt(0) != '/') break;
            s.deleteCharAt(0);
        }
        // Do we have drive letter?
        boolean hasdrive = hasDriveLetter(s);

        if(hasdrive)
            s.setCharAt(0, Character.toLowerCase(s.charAt(0)));

        while(s.length() > 0 && s.charAt(s.length() - 1) == '/') {
            s.deleteCharAt(s.length() - 1); // kill any trailing '/'s
        }

        // Add back leading '/', if any
        if(!hasdrive && isabs)
            s.insert(0, '/');
    }

    /**
     * Convert path to remove any leading '/' or drive letter assumes canonical.
     *
     * @param path convert this path
     * @return relatived version
     */
    static public String relpath(String path)
    {
        if(path == null) return null;
        StringBuilder b = new StringBuilder(path);
        canonicalpath(b);
        if(b.length() > 0) {
            if(b.charAt(0) == '/')
                b.deleteCharAt(0);
            if(hasDriveLetter(b))
                b.delete(0, 2);
        }
        return b.toString();
    }

    /**
     * @param path to test
     * @return true if path appears to start with Windows drive letter
     */
    static public boolean
    hasDriveLetter(String path)
    {
        return (path != null && path.length() >= 2
                && path.charAt(1) == ':'
                && DRIVELETTERS.indexOf(path.charAt(0)) >= 0);
    }

    // Support function
    static protected boolean
    hasDriveLetter(StringBuilder path)
    {
        return (path.length() >= 2
                && path.charAt(1) == ':'
                && DRIVELETTERS.indexOf(path.charAt(0)) >= 0);
    }

    /**
     * @param path to test
     * @return true if path is absolute
     */
    static public boolean
    isAbsolutePath(String path)
    {
        return (path != null && path.length() > 0
                && (path.charAt(0) == '/' || hasDriveLetter(path)));
    }

    /**
     * Convert path to add a  leading '/'; assumes canonical.
     *
     * @param path convert this path
     * @return absolute version
     */
    static public String abspath(String path)
    {
        if(path == null) return "/";
        StringBuilder b = new StringBuilder(path);
        canonicalpath(b);
        if(b.charAt(0) == '/')
            b.deleteCharAt(0);
        if(b.charAt(0) != '/' || !hasDriveLetter(b))
            b.insert(0, '/');
        return b.toString();
    }

    static public String
    readtextfile(InputStream stream)
               throws IOException
    {
        InputStreamReader rdr = new InputStreamReader(stream, UTF8);
        return readtextfile(rdr);
    }

    static public String
    readtextfile(Reader rdr)
            throws IOException
    {
        StringBuilder buf = new StringBuilder();
        for(; ; ) {
            int c = rdr.read();
            if(c < 0) break;
            buf.append((char) c);
        }
        return buf.toString();
    }

    static public void
    writebinaryfile(byte[] content, File dst)
            throws IOException
    {
        FileOutputStream fos = new FileOutputStream(dst);
        fos.write(content);
        fos.close();
    }

}
