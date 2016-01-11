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

import org.apache.http.Header;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.StringEntity;
import ucar.httpservices.*;
import ucar.nc2.util.IO;
import ucar.unidata.util.Urlencoded;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Convenience routines that wrap HTTPSession.
 *
 * @author caron
 */
public class HttpClientManager
{
    static private boolean debug = false;
    static private int timeout = 0;

    /**
     * initialize the HttpClient layer.
     *
     * @param provider  CredentialsProvider.
     * @param userAgent Content of User-Agent header, may be null
     */
    static public void init(CredentialsProvider provider, String userAgent)
    {
        if(provider != null)
            try {
                HTTPSession.setGlobalCredentialsProvider(provider);
            } catch(HTTPException e) {
                throw new IllegalArgumentException(e);
            }
        if(userAgent != null)
            HTTPSession.setGlobalUserAgent(userAgent + "/NetcdfJava/HttpClient");
        else
            HTTPSession.setGlobalUserAgent("NetcdfJava/HttpClient");

    }

    /**
     * Get the content from a url. For large returns, its better to use getResponseAsStream.
     *
     * @param urlencoded url as a String
     * @return contents of url as a String
     * @throws java.io.IOException on error
     */
    @Urlencoded
    public static String getContentAsString(String urlencoded)
            throws IOException
    {
        return getContentAsString(null, urlencoded);
    }

    /**
     * Get the content from a url. For large returns, its better to use getResponseAsStream.
     *
     * @param session    use this session, if null, create a new one
     * @param urlencoded url as a String
     * @return contents of url as a String
     * @throws java.io.IOException on error
     */
    @Urlencoded
    @Deprecated
    public static String getContentAsString(HTTPSession session, String urlencoded) throws IOException
    {
        HTTPSession useSession = session;
	try {
        if(useSession == null)
                useSession = HTTPFactory.newSession(urlencoded);
        try(HTTPMethod m = HTTPFactory.Get(useSession, urlencoded)) {
            m.execute();
            return m.getResponseAsString();
        }
	} finally {
            if((session == null) && (useSession != null))
                useSession.close();
        }
    }

    /**
     * Put content to a url, using HTTP PUT. Handles one level of 302 redirection.
     *
     * @param urlencoded url as a String
     * @param content    PUT this content at the given url.
     * @return the HTTP status return code
     * @throws java.io.IOException on error
     */
    public static int putContent(String urlencoded, String content) throws IOException
    {
        try (HTTPMethod m = HTTPFactory.Put(urlencoded)) {

            m.setRequestContent(new StringEntity(content, "application/text", "UTF-8"));
            m.execute();

            int resultCode = m.getStatusCode();

            // followRedirect wont work for PUT
            if(resultCode == 302) {
                String redirectLocation;
                Header locationHeader = m.getResponseHeader("location");
                if(locationHeader != null) {
                    redirectLocation = locationHeader.getValue();
                    resultCode = putContent(redirectLocation, content);
                }
            }
            return resultCode;
        }
    }

    //////////////////////

    static public String getUrlContentsAsString(String urlencoded, int maxKbytes) throws IOException
    {
        return getUrlContentsAsString(null, urlencoded, maxKbytes);
    }

    @Deprecated
    static public String getUrlContentsAsString(HTTPSession session, String urlencoded, int maxKbytes) throws IOException
    {
        HTTPSession useSession = session;
        try {
            if(useSession == null) {
                useSession = HTTPFactory.newSession(urlencoded);
            }
            try (HTTPMethod m = HTTPFactory.Get(useSession, urlencoded)) {
                m.setFollowRedirects(true);
                m.setCompression("gzip,deflate");

                int status = m.execute();
                if(status != 200) {
                    throw new RuntimeException("failed status = " + status);
                }

                String charset = m.getResponseCharSet();
                if(charset == null) charset = "UTF-8";

                // check for deflate and gzip compression
                Header h = m.getResponseHeader("content-encoding");
                String encoding = (h == null) ? null : h.getValue();

                if(encoding != null && encoding.equals("deflate")) {
                    byte[] body = m.getResponseAsBytes();
                    if (body == null) throw new IOException("empty body");
                    InputStream is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(body)), 10000);
                    if(useSession != null) {
                        useSession.close();
                    }
                    return readContents(is, charset, maxKbytes);

                } else if(encoding != null && encoding.equals("gzip")) {
                    byte[] body = m.getResponseAsBytes();
                    if (body == null) throw new IOException("empty body");
                    InputStream is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(body)), 10000);
                    if(useSession != null) {
                        useSession.close();
                    }
                    return readContents(is, charset, maxKbytes);

                } else {
                    byte[] body = m.getResponseAsBytes(maxKbytes * 1000);
                    if (body == null) throw new IOException("empty body");
                    if(useSession != null) {
                        useSession.close();
                    }
                    return new String(body, charset);
                }
            }
        } finally {
            if((session == null) && (useSession != null))
                useSession.close();
        }
    }

    static private String readContents(InputStream is, String charset, int maxKbytes) throws IOException
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(1000 * maxKbytes);
        IO.copy(is, bout, 1000 * maxKbytes);
        return bout.toString(charset);
    }

    static public void copyUrlContentsToFile(String urlencoded, File file)
            throws IOException
    {
        copyUrlContentsToFile(null, urlencoded, file);
    }

    @Deprecated
    static public void copyUrlContentsToFile(HTTPSession session, String urlencoded, File file)
            throws IOException
    {
        HTTPSession useSession = session;
        try {
            if(useSession == null)
                useSession = HTTPFactory.newSession(urlencoded);

            HTTPMethod m = HTTPFactory.Get(useSession, urlencoded);
            m.setCompression("gzip,deflate");

            int status = m.execute();

            if(status != 200) {
                throw new IOException(urlencoded+": failed status = " + status);
            }

            String charset = m.getResponseCharSet();
            if(charset == null) charset = "UTF-8";

            // check for deflate and gzip compression
            Header h = m.getResponseHeader("content-encoding");
            String encoding = (h == null) ? null : h.getValue();

            if(encoding != null && encoding.equals("deflate")) {
                InputStream is = new BufferedInputStream(new InflaterInputStream(m.getResponseAsStream()), 10000);
                IO.writeToFile(is, file.getPath());

            } else if(encoding != null && encoding.equals("gzip")) {
                InputStream is = new BufferedInputStream(new GZIPInputStream(m.getResponseAsStream()), 10000);
                IO.writeToFile(is, file.getPath());

            } else {
                IO.writeToFile(m.getResponseAsStream(), file.getPath());
            }

        } finally {
            if((session == null) && (useSession != null))
                useSession.close();
        }
    }

    static public long appendUrlContentsToFile(String urlencoded, File file, long start, long end)
            throws IOException
    {
        return appendUrlContentsToFile(null, urlencoded, file, start, end);
    }

    @Deprecated
    static public long appendUrlContentsToFile(HTTPSession session, String urlencoded, File file, long start, long end)
            throws IOException
    {
        HTTPSession useSession = session;
        long nbytes = 0;

        try {
            if(useSession == null) {
                useSession = HTTPFactory.newSession(urlencoded);
            }

            try (HTTPMethod m = HTTPFactory.Get(useSession, urlencoded)) {
                m.setCompression("gzip,deflate");
                m.setRange(start,end);

                int status = m.execute();
                if((status != 200) && (status != 206)) {
                    throw new RuntimeException("failed status = " + status);
                }

                String charset = m.getResponseCharSet();
                if(charset == null) charset = "UTF-8";

                // check for deflate and gzip compression
                Header h = m.getResponseHeader("content-encoding");
                String encoding = (h == null) ? null : h.getValue();

                if(encoding != null && encoding.equals("deflate")) {
                    InputStream is = new BufferedInputStream(new InflaterInputStream(m.getResponseAsStream()), 10000);
                    nbytes = IO.appendToFile(is, file.getPath());
                } else if(encoding != null && encoding.equals("gzip")) {
                    InputStream is = new BufferedInputStream(new GZIPInputStream(m.getResponseAsStream()), 10000);
                    nbytes = IO.appendToFile(is, file.getPath());
                } else {
                    nbytes = IO.appendToFile(m.getResponseAsStream(), file.getPath());
                }
            }
        } finally {
            if(session == null && useSession != null) {
                // close use session if we created it inside this method
                useSession.close();
            }
        }

        return nbytes;
    }

    /* todo:
    static public void showHttpRequestInfo(Formatter f, HttpRequestBase m)
    {
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
        for(int i = 0;i < heads.length;i++)
            f.format("  %s", heads[i]);

        f.format("%n");
    }

    static public void showHttpResponseInfo(Formatter f, HttpRequest m)
    {
        f.format("HttpClient response status = %s%n", m.getStatusLine());
        f.format("Reponse Headers = %n");
        Header[] heads = m.getResponseHeaders();
        for(int i = 0;i < heads.length;i++)
            f.format("  %s", heads[i]);
        f.format("%n");
    }  */

    /*
    public static void main(String[] args) throws IOException
    {
        HTTPSession.setGlobalUserAgent("TestUserAgent123global");
        HttpClientManager.getContentAsString(null, "http://motherlode.ucar.edu:9080/thredds/catalog.html");

        HTTPSession sess = HTTPFactory.newSession("http://motherlode.ucar.edu:9080/thredds/catalog.html");
        sess.setUserAgent("TestUserAgent123session");
        HttpClientManager.getContentAsString(sess, "http://motherlode.ucar.edu:9080/thredds/catalog.html");

    } */

}
