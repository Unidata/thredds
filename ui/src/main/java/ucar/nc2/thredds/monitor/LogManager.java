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

package ucar.nc2.thredds.monitor;

import ucar.nc2.util.IO;
import ucar.nc2.util.net.HttpClientManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Class Description.
 *
 * @author caron
 * @since Apr 24, 2009
 */
public class LogManager {
  static private HttpClient httpclient;
  static private File topDir;

  static {
    CredentialsProvider provider = new thredds.ui.UrlAuthenticatorDialog(null);
    httpclient = HttpClientManager.init(provider, "TdsMonitor");

    // decide where to put the logs locally
    String dataDir = System.getProperty( "tdsMonitor.dataDir" );
    if (dataDir != null) {
      topDir = new File(dataDir);
    } else {
      String homeDir = System.getProperty( "user.home" );
      topDir = new File(homeDir, "tdsMonitor");
    }

  }

  String server, type;
  List<RemoteLog> logs;
  private File localDir;

  LogManager(String server, boolean isAccess) throws IOException {
    this.server = server;
    this.type = isAccess ? "access" : "thredds";

    String cleanServer = java.net.URLEncoder.encode(server, "UTF8");            
    localDir = new File(topDir, cleanServer+"/"+type);
    localDir.mkdirs();

    logs = getRemoteFiles();
  }

  List<RemoteLog> getRemoteFiles() throws IOException {
    List<RemoteLog> result = new ArrayList<RemoteLog>(50);

    String urls = "http://" + server + "/thredds/admin/log/"+type+"/";
    String contents = getUrlContents(urls);
    if (contents == null) return null;

    String[] lines = contents.split("\n");
    for (String line : lines) {
      RemoteLog remoteLog = new RemoteLog(line.trim());
      //System.out.printf(" %s == %s %n", line, remoteLog);
      result.add(remoteLog);
    }

    return result;
  }

  List<File> getLocalFiles() throws IOException {
    List<File> result = new ArrayList<File>(logs.size());
    for (RemoteLog rlog : logs)
      result.add(rlog.localFile);
    return result;
  }

  private class RemoteLog {
    String name;
    long size;
    File localFile;

    RemoteLog(String line) {
      String[] tokes = line.split(" ");
      name = tokes[0];
      size = Long.parseLong(tokes[1]);

      localFile = new File(localDir, name);
      if (!localFile.exists() || (localFile.length() > size)) {
        System.out.printf("Read RemoteLog length=%d local=%d for %s%n", size, localFile.length(), name);
        read();
      } else if (localFile.length() < size) {
        System.out.printf("Append RemoteLog length=%d local=%d for %s%n", size, localFile.length(), name);
        append();
      } else {
        System.out.printf("Ok RemoteLog length=%d local=%d for %s%n", size, localFile.length(), name);
      }
    }

    void read() {
      String urls = "http://" + server + "/thredds/admin/log/"+type+"/" + name;
      copyUrlContentsToFile(urls, localFile);
      System.out.printf(" read %s to %s %n", urls, localFile.getPath());
    }

    void append() {
      String urls = "http://" + server + "/thredds/admin/log/"+type+"/" + name;
      long start = localFile.length();
      long want = size - start;
      long got = appendUrlContentsToFile(urls, localFile, start, size);
      if (want == got)
        System.out.printf(" append %d bytes to %s %n", got, localFile.getPath());
      else
        System.out.printf(" append got=%d want=%d bytes to %s %n", got, want, localFile.getPath());
    }

    @Override
    public String toString() {
      return "RemoteLog{" +
              "name='" + name + '\'' +
              ", size=" + size +
              '}';
    }
  }

  //////////////////////

  private String getUrlContents(String urlString) {
    HttpMethodBase m = new GetMethod(urlString);
    m.setFollowRedirects(true);
    m.setRequestHeader("Accept-Encoding", "gzip,deflate");

    try {
      httpclient.executeMethod(m);

      String charset = m.getResponseCharSet();
      if (charset == null) charset = "UTF-8";

      // check for deflate and gzip compression
      Header h = m.getResponseHeader("content-encoding");
      String encoding = (h == null) ? null : h.getValue();

      if (encoding != null && encoding.equals("deflate")) {
        byte[] body = m.getResponseBody();
        InputStream is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(body)), 10000);
        return IO.readContents(is, charset);

      } else if (encoding != null && encoding.equals("gzip")) {
        byte[] body = m.getResponseBody();
        InputStream is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(body)), 10000);
        return IO.readContents(is, charset);

      } else {
        byte[] body = m.getResponseBody(50 * 1000);
        return new String(body, charset);
      }

    } catch (Exception e) {
      e.printStackTrace();
      return null;

    } finally {
      m.releaseConnection();
    }
  }

  private void copyUrlContentsToFile(String urlString, File file) {
    HttpMethodBase m = new GetMethod(urlString);
    m.setFollowRedirects(true);
    m.setRequestHeader("Accept-Encoding", "gzip,deflate");

    try {
      httpclient.executeMethod(m);

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

  private long appendUrlContentsToFile(String urlString, File file, long start, long end) {
    long nbytes = 0;

    HttpMethodBase m = new GetMethod(urlString);
    m.setRequestHeader("Accept-Encoding", "gzip,deflate");
    m.setFollowRedirects(true);
    m.setRequestHeader("Range", "bytes=" + start + "-" + end);

    try {
      httpclient.executeMethod(m);

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


}
