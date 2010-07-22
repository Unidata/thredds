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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.auth.CredentialsProvider;

//import opendap.dap.DConnect2;
//import ucar.unidata.io.http.HTTPRandomAccessFile;

import java.io.*;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;
import java.util.Formatter;

import ucar.nc2.util.IO;

/**
 * Manage Http Client protocol settings.
 * <pre>
 * Example:
 *   org.apache.commons.httpclient.auth.CredentialsProvider provider = new thredds.ui.UrlAuthenticatorDialog(frame);
 * ucar.nc2.util.net.HttpClientManager.init(provider, "ToolsUI");
 * </pre>
 *
 * @author caron
 */
public class HttpClientManager {
  static private boolean debug = false;
  static private HttpClient _client;
  static private int timeout = 0;

  /**
   * initialize the HttpClient layer.
   *
   * @param provider  CredentialsProvider.
   * @param userAgent Content of User-Agent header, may be null
   */
  static public org.apache.commons.httpclient.HttpClient init(CredentialsProvider provider, String userAgent) {
    initHttpClient();
    
    if (provider != null)
      _client.getParams().setParameter(CredentialsProvider.PROVIDER, provider);

    if (userAgent != null)
      _client.getParams().setParameter(HttpMethodParams.USER_AGENT, userAgent + "/NetcdfJava/HttpClient");
    else
      _client.getParams().setParameter(HttpMethodParams.USER_AGENT, "NetcdfJava/HttpClient");

    // nick.bower@metoceanengineers.com
    String proxyHost = System.getProperty("http.proxyHost");
    String proxyPort = System.getProperty("http.proxyPort");
    if ((proxyHost != null) && (proxyPort != null) && !proxyPort.trim().equals("")) {
        _client.getHostConfiguration().setProxy(proxyHost, Integer.parseInt(proxyPort));
    }

    return _client;
  }

  /**
   * Get the HttpClient object - a single instance is used.
   * @return the  HttpClient object
   */
  static public HttpClient getHttpClient() {
    return _client;
  }

  private static synchronized void initHttpClient() {
    if (_client != null) return;
    MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    _client = new HttpClient(connectionManager);

    HttpClientParams params = _client.getParams();
    params.setParameter(HttpMethodParams.SO_TIMEOUT, timeout);
    params.setParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, Boolean.TRUE);
    params.setParameter(HttpClientParams.COOKIE_POLICY, CookiePolicy.RFC_2109);

    // allow self-signed certificates
    Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 8443));

    // LOOK need default CredentialsProvider ??
    // _client.getParams().setParameter(CredentialsProvider.PROVIDER, provider);

  }

  public static void clearState() {
    _client.getState().clearCookies();
    _client.getState().clearCredentials();
  }

  /**
   * Get the content from a url. For large returns, its better to use getResponseAsStream.
   * @param urlString url as a String
   * @return contents of url as a String
   * @throws java.io.IOException on error
   */
  public static String getContent(String urlString) throws IOException {
    GetMethod m = new GetMethod(urlString);
    m.setFollowRedirects(true);

    try {
      _client.executeMethod(m);
      return m.getResponseBodyAsString();

    } finally {
      m.releaseConnection();
    }
  }

  /**
   * Put content to a url, using HTTP PUT. Handles one level of 302 redirection.
   * @param urlString url as a String
   * @param content PUT this content at the given url.
   * @return the HTTP status return code
   * @throws java.io.IOException on error
   */
  public static int putContent(String urlString, String content) throws IOException {
    PutMethod m = new PutMethod(urlString);
    m.setDoAuthentication( true );

    try {
      m.setRequestEntity(new StringRequestEntity(content));

      _client.executeMethod(m);

      int resultCode = m.getStatusCode();

       // followRedirect wont work for PUT
       if (resultCode == 302) {
         String redirectLocation;
         Header locationHeader = m.getResponseHeader("location");
         if (locationHeader != null) {
           redirectLocation = locationHeader.getValue();
           if (debug) System.out.println("***Follow Redirection = "+redirectLocation);
           resultCode = putContent(redirectLocation, content);
         }
       }

      return resultCode;

    } finally {
      m.releaseConnection();
    }
  }

  //////////////////////

   static public String getUrlContents(String urlString, int maxKbytes) {
     HttpMethodBase m = new GetMethod(urlString);
     m.setFollowRedirects(true);
     m.setRequestHeader("Accept-Encoding", "gzip,deflate");

     try {
       int status = _client.executeMethod(m);
       if (status != 200) {
         throw new RuntimeException("failed status = "+status);
       }

       String charset = m.getResponseCharSet();
       if (charset == null) charset = "UTF-8";

       // check for deflate and gzip compression
       Header h = m.getResponseHeader("content-encoding");
       String encoding = (h == null) ? null : h.getValue();

       if (encoding != null && encoding.equals("deflate")) {
         byte[] body = m.getResponseBody();
         InputStream is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(body)), 10000);
         return readContents(is, charset, maxKbytes);

       } else if (encoding != null && encoding.equals("gzip")) {
         byte[] body = m.getResponseBody();
         InputStream is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(body)), 10000);
         return readContents(is, charset, maxKbytes);

       } else {
         byte[] body = m.getResponseBody(maxKbytes * 1000);
         return new String(body, charset);
       }

     } catch (Exception e) {
       e.printStackTrace();
       return null;

     } finally {
       m.releaseConnection();
     }
   }

   static private String readContents(InputStream is, String charset, int maxKbytes) throws IOException {
     ByteArrayOutputStream bout = new ByteArrayOutputStream(1000 * maxKbytes);
     IO.copy(is, bout, 1000 * maxKbytes);
     return bout.toString(charset);
   }

   static public  void copyUrlContentsToFile(String urlString, File file) {
     HttpMethodBase m = new GetMethod(urlString);
     m.setFollowRedirects(true);
     m.setRequestHeader("Accept-Encoding", "gzip,deflate");

     try {
       int status = _client.executeMethod(m);

       if (status != 200) {
         throw new RuntimeException("failed status = "+status);
       }

       String charset = m.getResponseCharSet();
       if (charset == null) charset = "UTF-8";

       // check for deflate and gzip compression
       Header h = m.getResponseHeader("content-encoding");
       String encoding = (h == null) ? null : h.getValue();

       if (encoding != null && encoding.equals("deflate")) {
         InputStream is = new BufferedInputStream(new InflaterInputStream(m.getResponseBodyAsStream()), 10000);
         IO.writeToFile(is, file.getPath());

       } else if (encoding != null && encoding.equals("gzip")) {
         InputStream is = new BufferedInputStream(new GZIPInputStream(m.getResponseBodyAsStream()), 10000);
         IO.writeToFile(is, file.getPath());

       } else {
         IO.writeToFile(m.getResponseBodyAsStream(), file.getPath());
       }

     } catch (Exception e) {
       e.printStackTrace();

     } finally {
       m.releaseConnection();
     }
   }

   static public long appendUrlContentsToFile(String urlString, File file, long start, long end) {
     long nbytes = 0;

     HttpMethodBase m = new GetMethod(urlString);
     m.setRequestHeader("Accept-Encoding", "gzip,deflate");
     m.setFollowRedirects(true);
     m.setRequestHeader("Range", "bytes=" + start + "-" + end);

     try {
       int status = _client.executeMethod(m);
       if ((status != 200) && (status != 206)) {
         throw new RuntimeException("failed status = "+status);
       }

       String charset = m.getResponseCharSet();
       if (charset == null) charset = "UTF-8";

       // check for deflate and gzip compression
       Header h = m.getResponseHeader("content-encoding");
       String encoding = (h == null) ? null : h.getValue();

       if (encoding != null && encoding.equals("deflate")) {
         InputStream is = new BufferedInputStream(new InflaterInputStream(m.getResponseBodyAsStream()), 10000);
         nbytes = IO.appendToFile(is, file.getPath());

       } else if (encoding != null && encoding.equals("gzip")) {
         InputStream is = new BufferedInputStream(new GZIPInputStream(m.getResponseBodyAsStream()), 10000);
         nbytes = IO.appendToFile(is, file.getPath());

       } else {
         nbytes = IO.appendToFile(m.getResponseBodyAsStream(), file.getPath());
       }

     } catch (Exception e) {
       e.printStackTrace();

     } finally {
       m.releaseConnection();
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
