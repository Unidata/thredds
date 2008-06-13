/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * HTTPRandomAccessFile.java.
 * @author John Caron, based on work by Donald Denbo
 */

package ucar.unidata.io.http;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

import java.io.FileNotFoundException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;

/**
 * Gives access to files over HTTP, using jakarta commons HttpClient library.
 * This version uses a single instance of HttpClient, following performance guidelines at
 * http://jakarta.apache.org/commons/httpclient/performance.html
 * Plus other improvements.
 *
 * @author John Caron
 */

public class HTTPRandomAccessFile extends ucar.unidata.io.RandomAccessFile {
  static public int defaultHTTPBufferSize = 20000;

  static private HttpClient _client;

  /**
   * Set the HttpClient object - a single instance is used.
   * @param client the HttpClient object
   */
  static public void setHttpClient(HttpClient client) {
    _client = client;
  }

  /**
   * Get the HttpClient object - a single instance is used.
   * @return client the HttpClient object
   */
  static public HttpClient getHttpClient() {
    return _client;
  }

  // default HttpClient
  private synchronized void initHttpClient() {
    if (_client != null) return;
    MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    _client = new HttpClient(connectionManager);
  }

  private String url;
  private long total_length = 0;

  private boolean debug = false, debugDetails = false;

  public HTTPRandomAccessFile(String url) throws IOException {
    this(url, defaultHTTPBufferSize);
    location = url;
  }

  public HTTPRandomAccessFile(String url, int bufferSize) throws IOException {
    super(bufferSize);
    file = null;
    this.url = url;
    location = url;

    initHttpClient();

    HttpMethod method = null;
    try {
      method = new HeadMethod(url);
      method.setFollowRedirects(true);

      doConnect(method);

      Header head = method.getResponseHeader("Accept-Ranges");
      if (head == null || !head.getValue().equalsIgnoreCase("bytes")) {
        throw new IOException("Server does not support byte Ranges");
      }

      head = method.getResponseHeader("Content-Length");
      if (head == null) {
        throw new IOException("Server does not support Content-Length");
      }

      try {
        total_length = Long.parseLong(head.getValue());
      } catch (NumberFormatException e) {
        throw new IOException("Server has malformed Content-Length header");
      }

    } finally {
      if (method != null) method.releaseConnection();
    }
    // header[0] = new NVPair("User-Agent", "HTTPnetCDF;"); ??

    if (debugLeaks) openFiles.add(location);
  }

  private void doConnect(HttpMethod method) throws IOException {

    // Execute the method.
    int statusCode = _client.executeMethod(method);

    if (statusCode == 404)
      throw new FileNotFoundException(url + " " + method.getStatusLine());
    
    if (statusCode >= 300)
      throw new IOException(url + " " + method.getStatusLine());

    if (debugDetails) {
      printHeaders("Request: " + method.getName() + " " + method.getPath(), method.getRequestHeaders());
      printHeaders("Response: " + method.getStatusCode(), method.getResponseHeaders());
    }
  }

  private void printHeaders(String title, Header[] heads) {
    System.out.println(title);
    for (Header head : heads) {
      System.out.print("  " + head.toString());
    }
    System.out.println();
  }

  /**
   * Read directly from file, without going through the buffer.
   * All reading goes through here or readToByteChannel;
   *
   * @param pos    start here in the file
   * @param buff      put data into this buffer
   * @param offset buffer offset
   * @param len    this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  protected int read_(long pos, byte[] buff, int offset, int len) throws IOException {
    long end = pos + len - 1;
    if (end >= total_length)
      end = total_length - 1;

    if (debug) System.out.println(" HTTPRandomAccessFile bytes=" + pos + "-" + end + ": ");

    HttpMethod method = null;
    try {
      method = new GetMethod(url);
      method.setFollowRedirects(true);
      method.setRequestHeader("Range", "bytes=" + pos + "-" + end);
      doConnect(method);

      int code = method.getStatusCode();
      if (code != 206)
        throw new IOException("Server does not support Range requests, code= " + code);

      String s = method.getResponseHeader("Content-Length").getValue();
      int readLen = Integer.parseInt(s);
      readLen = Math.min(len, readLen);

      InputStream is = method.getResponseBodyAsStream();
      readLen = copy(is, buff, offset, readLen);
      return readLen;

    } finally {
      if (method != null) method.releaseConnection();
    }
  }

  private int copy(InputStream in, byte[] buff, int offset, int want) throws IOException {
    int done = 0;
    while (want > 0) {
      int bytesRead = in.read(buff, offset + done, want);
      if (bytesRead == -1) break;
      done += bytesRead;
      want -= bytesRead;
    }
    return done;
  }

  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
    int n = (int) nbytes;
    byte[] buff = new byte[n];
    int done = read_(offset, buff, 0, n);
    dest.write(ByteBuffer.wrap(buff));
    return done;
  }

  // override the rest of the RandomAccessFile public methods
  public long length() throws IOException {
    long fileLength = total_length;
    if (fileLength < dataEnd)
      return dataEnd;
    else
      return fileLength;
  }

  public void close() {
    if (debugLeaks) openFiles.remove(location);
  }

  public FileDescriptor getFD() {
    return null;
  }

}

