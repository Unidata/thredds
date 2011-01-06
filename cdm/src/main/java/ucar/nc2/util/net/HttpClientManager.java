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

import opendap.dap.http.HTTPException;
import opendap.dap.http.HTTPMethod;
import opendap.dap.http.HTTPSession;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.commons.httpclient.params.HttpParams;
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
 * @deprecated use HTTPSession
 */
public class HttpClientManager {
  static private boolean debug = false;
  static private HTTPSession session;
  static private int timeout = 0;
  static HttpParams globalparams = new DefaultHttpParams();

  /**
   * initialize the HttpClient layer.
   *
   * @param provider  CredentialsProvider.
   * @param userAgent Content of User-Agent header, may be null
   */
  static public HTTPSession init(CredentialsProvider provider, String userAgent) {
    initSession();
    
    if (provider != null)
      globalparams.setParameter(CredentialsProvider.PROVIDER, provider);

    if (userAgent != null)
      globalparams.setParameter(HttpMethodParams.USER_AGENT, userAgent + "/NetcdfJava/HttpClient");
    else
      globalparams.setParameter(HttpMethodParams.USER_AGENT, "NetcdfJava/HttpClient");

    // nick.bower@metoceanengineers.com
    String proxyHost = System.getProperty("http.proxyHost");
    String proxyPort = System.getProperty("http.proxyPort");
    if ((proxyHost != null) && (proxyPort != null) && !proxyPort.trim().equals("")) {
        session.setProxy(proxyHost, Integer.parseInt(proxyPort));
    }

    return session;
  }

  /**
   * Get the HttpClient object - a single instance is used.
   * @return the  HttpClient object
   */
  static public HTTPSession getHttpSession() {
    return session;
  }

  private static synchronized void initSession() {
    if (session != null) return;
    try { session = new HTTPSession();  } catch (HTTPException he) {session = null; return;}

    session.setMethodParameter(HttpMethodParams.SO_TIMEOUT, timeout);
    session.setMethodParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, Boolean.TRUE);
    session.setMethodParameter(HttpClientParams.COOKIE_POLICY, CookiePolicy.RFC_2109);

    // LOOK need default CredentialsProvider ??
    // _client.getParams().setParameter(CredentialsProvider.PROVIDER, provider);

  }

  public static void clearState() {
    if (session != null)
      session.clearState();
  }

  /**
   * Get the content from a url. For large returns, its better to use getResponseAsStream.
   * @param urlString url as a String
   * @return contents of url as a String
   * @throws java.io.IOException on error
   */
  public static String getContent(String urlString) throws IOException {
    HTTPSession session = new HTTPSession();
    HTTPMethod m = session.newMethodGet(urlString);
    m.setFollowRedirects(true);

    try {
      m.execute();
      return m.getResponseAsString();
    } finally {
      session.close();
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
    initSession();
    HTTPMethod m = session.newMethodPut(urlString);
    //fix m.setDoAuthentication( true );

    try {
      m.setRequestContentAsString(content);

      m.execute();

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
      if(m != null) m.close();
    }
  }

  //////////////////////

   static public String getUrlContents(String urlString, int maxKbytes) {
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
     initSession();
     HTTPMethod m = session.newMethodGet(urlString);
     m.setRequestHeader("Accept-Encoding", "gzip,deflate");

     try {
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
       if(m != null) m.close();
     }
   }

   static public long appendUrlContentsToFile(String urlString, File file, long start, long end) throws HTTPException {
     long nbytes = 0;

     initSession();
     HTTPMethod m = session.newMethodGet(urlString);
     m.setRequestHeader("Accept-Encoding", "gzip,deflate");
     m.setRequestHeader("Range", "bytes=" + start + "-" + end);

     try {
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
       if (m !=null) m.close();
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
