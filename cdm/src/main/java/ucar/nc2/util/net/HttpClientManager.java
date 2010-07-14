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


//import opendap.dap.DConnect2;
//import ucar.unidata.io.http.HTTPRandomAccessFile;

import java.io.*;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;
import java.util.Formatter;

import opendap.dap.HttpWrap;
import opendap.dap.HttpWrapException;
import org.apache.http.HttpHost;
import org.apache.http.Header;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import ucar.nc2.util.IO;


/**
 * Manage HttpWrap Client protocol settings.
 * <pre>
 * Example:
 *   CredentialsProvider provider = new thredds.ui.UrlAuthenticatorDialog(frame);
 * opendap.dap.HttpWrapClientManager.init(provider, "ToolsUI");
 * <p/>
 * </pre>
 *
 * @author caron
 */
public class HttpClientManager {
    static private boolean debug = false;
    static private int timeout = 0;

     private HttpWrap _client;

    /**
     * initialize the AbstractHttpClient layer.
     *
     * @param provider  CredentialsProvider.
     * @param userAgent Content of User-Agent header, may be null
     */
     public HttpWrap  init(CredentialsProvider provider, String userAgent) throws IOException  {
         initHttpClient();

        if (provider != null)
            _client.setCredentialsProvider(provider);

        if (userAgent != null)
            _client.setUserAgent(userAgent + "/NetcdfJava/HttpClient");
        else
            _client.setParameter(CoreProtocolPNames.USER_AGENT, "NetcdfJava/HttpClient");

        // nick.bower@metoceanengineers.com
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        if ((proxyHost != null) && (proxyPort != null) && !proxyPort.trim().equals("")) {
            HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
            _client.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        return _client;
    }

    /**
     * Get the HttpClient object - a single instance is used.
     *
     * @return the  HttpClient object
     */
     /*
     public HttpWrap getHttpClient() {
        return _client;
    }
    */

    private  void initHttpClient()  throws IOException {

       if (_client != null) return;
        _client = new HttpWrap();
        _client.setParameter(AllClientPNames.SO_TIMEOUT, timeout);
        _client.setParameter(AllClientPNames.ALLOW_CIRCULAR_REDIRECTS, Boolean.TRUE);
        _client.setParameter(AllClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);


        // allow self-signed certificates

        // LOOK need default CredentialsProvider ??
        // _client.getParams().setParameter(CredentialsProvider.PROVIDER, provider);

    }

    public static void clearState() {
        //_client.close();
    }

    /**
     * Get the content from a url. For large returns, its better to use getResponseAsStream.
     *
     * @param urlString url as a String
     * @return contents of url as a String
     * @throws java.io.IOException on error
     */
    public  String getContent(String urlString) throws IOException {

       try {
            _client.setMethodGet(urlString);
            return _client.getContentString();
       } finally {
          // _client.close();
       }
    }

    /**
     * Put content to a url, using HTTP PUT. Handles one level of 302 redirection.
     *
     * @param urlString url as a String
     * @param content   PUT this content at the given url.
     * @return the HTTP status return code
     * @throws java.io.IOException on error
     */
    public  int putContent(String urlString, String content) throws IOException {
        try {
            _client.setHeader("Accept-Encoding", "gzip,deflate");

             _client.setContent(new StringEntity(content));
            _client.setMethodPut(urlString);
            int resultCode = _client.execute();

            if (resultCode == 302) {
                String redirectLocation;
                Header locationHeader = _client.getHeader("location");
                if (locationHeader != null) {
                    redirectLocation = locationHeader.getValue();
                    if (debug) System.out.println("***Follow Redirection = " + redirectLocation);
                    resultCode = putContent(redirectLocation, content);
                }
            }
            return resultCode;

        } finally {
            //_client.close();
        }
    }

    //////////////////////

     public String getUrlContents(String urlString, int maxKbytes)  {

        try {
            _client.setHeader("Accept-Encoding", "gzip,deflate");
             _client.setMethodGet(urlString);
            int status = _client.execute();
            if (status != 200) {
                throw new HttpWrapException("failed status = " + status);
            }

            String charset = _client.getCharSet();
            if (charset == null) charset = "UTF-8";

            // check for deflate and gzip compression
            Header h = _client.getHeader("content-encoding");
            String encoding = (h == null) ? null : h.getValue();

            if (encoding != null && encoding.equals("deflate")) {
                InputStream body = _client.getContentStream();
                InputStream is = new BufferedInputStream(new InflaterInputStream(body), 10000);
                return IO.readContents(is, charset); //readContents(is, charset, maxKbytes);

            } else if (encoding != null && encoding.equals("gzip")) {
                InputStream body = _client.getContentStream();
                InputStream is = new BufferedInputStream(new GZIPInputStream(body), 10000);
                return IO.readContents(is, charset); //readContents(is, charset, maxKbytes);

            } else {
                return _client.getContentString(charset);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

     public void copyUrlContentsToFile(String urlString, File file)  {

        try {
            _client.setHeader("Accept-Encoding", "gzip,deflate");

             _client.setMethodGet(urlString);
            int status = _client.execute();

            if (status != 200) {
                throw new RuntimeException("failed status = " + status);
            }

            String charset = _client.getCharSet();
            if (charset == null) charset = "UTF-8";

            // check for deflate and gzip compression
            Header h = _client.getHeader("content-encoding");
            String encoding = (h == null) ? null : h.getValue();

            if (encoding != null && encoding.equals("deflate")) {
                InputStream is = new BufferedInputStream(new InflaterInputStream(_client.getContentStream()), 10000);
                IO.writeToFile(is, file.getPath());

            } else if (encoding != null && encoding.equals("gzip")) {
                InputStream is = new BufferedInputStream(new GZIPInputStream(_client.getContentStream()), 10000);
                IO.writeToFile(is, file.getPath());

            } else {
                IO.writeToFile(_client.getContentStream(), file.getPath());
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            //_client.close();
        }
    }

     public long appendUrlContentsToFile(String urlString, File file, long start, long end)  {
        long nbytes = 0;


        try {
            _client.setHeader("Accept-Encoding", "gzip,deflate");
            _client.setHeader("Range", "bytes=" + start + "-" + end);
             _client.setMethodGet(urlString);
            int status = _client.execute();
            if ((status != 200) && (status != 206)) {
                throw new HttpWrapException("failed status = " + status);
            }

            String charset = _client.getCharSet();
            if (charset == null) charset = "UTF-8";

            // check for deflate and gzip compression
            Header h = _client.getHeader("content-encoding");
            String encoding = (h == null) ? null : h.getValue();

            if (encoding != null && encoding.equals("deflate")) {
                InputStream is = new BufferedInputStream(new InflaterInputStream(_client.getContentStream()), 10000);
                nbytes = IO.appendToFile(is, file.getPath());

            } else if (encoding != null && encoding.equals("gzip")) {
                InputStream is = new BufferedInputStream(new GZIPInputStream(_client.getContentStream()), 10000);
                nbytes = IO.appendToFile(is, file.getPath());

            } else {
                nbytes = IO.appendToFile(_client.getContentStream(), file.getPath());
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            //_client.close();
        }

        return nbytes;
    }

     public void showHttpRequestInfo(Formatter f, AbstractHttpMessage m)  {
        f.format("HttpClient request %s %s %n", _client.getName(), _client.getURI());
        // fix f.format("   do Authentication=%s%n", _client.getDoAuthentication());
        f.format("   follow Redirects =%s%n", _client.getFollowRedirects());
        // fix f.format("   effectiveVersion =%s%n", m.getEffectiveVersion());
        // fix f.format("   hostAuthState    =%s%n", m.getHostAuthState());

        HttpParams p = m.getParams();
        f.format("   cookie policy    =%s%n", _client.getCookiePolicy());
        f.format("   http version     =%s%n", _client.getProtocolVersion());
        f.format("   timeout (msecs)  =%d%n", _client.getSoTimeout());
        f.format("   virtual host     =%s%n", _client.getVirtualHost());

        f.format("Request Headers = %n");
        Header[] heads = _client.getHeaders();
        for (int i = 0; i < heads.length; i++)
            f.format("  %s", heads[i]);

        f.format("%n");
    }

    static public void showHttpResponseInfo(Formatter f, HttpWrap h)  {
        f.format("HttpClient response status = %s%n", h.getStatusLine());
        f.format("Reponse Headers = %n");
        Header[] heads = h.getHeaders();
        for (int i = 0; i < heads.length; i++)
            f.format("  %s", heads[i]);
        f.format("%n");
    }


}
