// $Id$
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

/**
 * Gives access to files over HTTP, using jakarta commons HttpClient.
 * @author John Caron
 */

public class HTTPRandomAccessFile3 extends ucar.unidata.io.RandomAccessFile {
  static public int defaultHTTPBufferSize = 20000;
  static public int timeoutMsecs = 5000;

  private String url;
  private HttpClient client;
  private long total_length = 0;

  private boolean debug = false, debugDetails = false;

  public HTTPRandomAccessFile3(String url) throws IOException {
    this( url, defaultHTTPBufferSize);
    location = url.toString();
  }

  public HTTPRandomAccessFile3(String url, int bufferSize) throws IOException {
    super( bufferSize);
    file = null;
    this.url = url;
    location = url.toString();

    client = new HttpClient();
    client.setConnectionTimeout(timeoutMsecs); //establish a connection within this time

    HttpMethod method = new HeadMethod(url);
    method.setFollowRedirects(true);
    method.setStrictMode(false);

    doConnect( method);

    /* Header head = method.getResponseHeader("Accept-Ranges");
    if (head == null || !head.getValue().equalsIgnoreCase("bytes")) {
      throw new IOException("Server does not support byte Ranges");
    } */

    Header head = method.getResponseHeader("Content-Length");
    if (head == null) {
      throw new IOException("Server does not support Content-Length");
    }

    try {
      total_length = Integer.parseInt( head.getValue());
    } catch ( NumberFormatException e) {
      throw new IOException("Server has malformed Content-Length header");
    }

    method.releaseConnection();
    // header[0] = new NVPair("User-Agent", "HTTPnetCDF;"); ??

    if (debugLeaks) openFiles.add( location);
  }

  private void doConnect(HttpMethod method) throws IOException {

    // Execute the method.
    int statusCode = -1;

    // We will retry up to 3 times.
    for (int attempt = 0; statusCode == -1 && attempt < 3; attempt++) {
      try {
        statusCode = client.executeMethod(method);

      } catch (HttpRecoverableException e) {
         if (attempt == 2) throw new IOException( e.getMessage());
         System.err.println("A recoverable exception occurred, retrying." + e.getMessage());
      }

    }

    if (statusCode == 404)
      throw new FileNotFoundException( url+" "+method.getStatusLine());
    if (statusCode >= 300)
      throw new IOException( url+" "+method.getStatusLine());

    if (debugDetails) {
      printHeaders("Request: " +method.getName() + " "+ method.getPath(), method.getRequestHeaders());
      printHeaders("Response: " + method.getStatusCode(), method.getResponseHeaders());
    }

  }

  private void printHeaders(String title, Header[] heads) {
    System.out.println(title);
    for (int i = 0; i < heads.length; i++) {
      Header head = heads[i];
      System.out.print("  "+head.toString());
    }
    System.out.println();
  }

  protected int read_(long pos, byte[] buff, int offset, int len) throws IOException {
    long end = pos + len - 1;
    if (end >= total_length)
      end = total_length - 1;

    if (debug) System.out.println(" HTTPRandomAccessFile bytes="+pos+"-"+end+": ");

    HttpMethod method = null;
    try {
      method = new GetMethod(url);  // LOOK: can we resuse?
      method.setFollowRedirects(true);
      method.setStrictMode(false);

      method.setRequestHeader("Range", "bytes="+pos+"-"+end);
      doConnect( method);

      int code = method.getStatusCode();
      if (code != 206)
        throw new IOException("Server does not support Range requests, code= "+code);

      String s = method.getResponseHeader("Content-Length").getValue();
      int readLen = Integer.parseInt( s);
      readLen = Math.min( len, readLen);

      InputStream is = method.getResponseBodyAsStream();
      readLen = copy( is, buff, offset, readLen);
      return readLen;

    } finally {
      if (method != null) method.releaseConnection();
    }
  }

  private int copy(InputStream in, byte[] buff, int offset, int want) throws IOException {
    int done = 0;
    while (want > 0) {
      int bytesRead = in.read( buff, offset + done, want);
      if (bytesRead == -1) break;
      done += bytesRead;
      want -= bytesRead;
    }
    return done;
  }

  public long length( ) throws IOException {
    long fileLength = total_length;
    if( fileLength < dataEnd )
      return dataEnd;
    else
      return fileLength;
  }


  /**
   * override the rest of the RandomAccessFile public methods
   */
  public void close() {
    client = null;
    if (debugLeaks) openFiles.remove( location);
  }

  public FileDescriptor getFD() {
    return null;
  }

}

