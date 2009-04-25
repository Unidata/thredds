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
  static HttpClient httpclient;
  static {
    CredentialsProvider provider = new thredds.ui.UrlAuthenticatorDialog(null);
    httpclient = HttpClientManager.init(provider, "TdsMonitor");
  }

  File dir = new File("D:/logs/motherlode/test/access/");
  String server;
  List<RemoteLog> logs;

  LogManager( String server) {
    this.server = server;
    try {
      logs = getRemoteFiles();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void getLocalFiles() {
    File[] files = dir.listFiles();
  }

  List<RemoteLog> getRemoteFiles() throws IOException {
    List<RemoteLog> result = new ArrayList<RemoteLog>(50);

    String urls = "http://" + server + "/thredds/admin/log/access/";
    String contents = getUrlContents(urls);
    if (contents == null) return null;

    String[] lines = contents.split("\n");
    for (String line : lines) {
      RemoteLog remoteLog = new RemoteLog(line);
      System.out.printf(" %s == %s %n", line, remoteLog);
      result.add( new RemoteLog(line));
    }

    return result;
  }


  private class RemoteLog {
    String name;
    long size;

    RemoteLog( String line) {
      String[] tokes = line.split(" ");
      name = tokes[0];
      size = Long.parseLong( tokes[1]);
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
    m.setRequestHeader("Accept-Encoding","gzip,deflate");

    try {
      httpclient.executeMethod(m);

        String charset = m.getResponseCharSet();
        if (charset == null) charset = "UTF-8";

        // check for deflate and gzip compression
        Header h = m.getResponseHeader("content-encoding");
        String encoding = (h == null) ? null : h.getValue();

        if (encoding != null && encoding.equals("deflate")) {
          byte[] body = m.getResponseBody();
          InputStream is = new BufferedInputStream(new InflaterInputStream( new ByteArrayInputStream(body)), 10000);
          return IO.readContents(is, charset);

        } else if (encoding != null && encoding.equals("gzip")) {
          byte[] body = m.getResponseBody();
          InputStream is = new BufferedInputStream(new GZIPInputStream( new ByteArrayInputStream(body)),  10000);
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


}
