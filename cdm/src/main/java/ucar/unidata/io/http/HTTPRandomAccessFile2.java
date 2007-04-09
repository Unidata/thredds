// $Id: HTTPRandomAccessFile2.java 64 2006-07-12 22:30:50Z edavis $
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

import java.net.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileDescriptor;

/**
 * Gives access to files over HTTP, using java.net.HttpURLConnection.
 * @deprecated use HTTPRandomAccessFile
 * @author John Caron, based on work by Donald Denbo
 */

public class HTTPRandomAccessFile2 extends ucar.unidata.io.RandomAccessFile {
  static public int defaultHTTPBufferSize = 20000;

  private long total_length = 0;
  private URL url;
  private HttpURLConnection conn;

  private boolean debug = false, debugHeaders = false;

  public HTTPRandomAccessFile2(URL url) throws IOException {
    this( url, defaultHTTPBufferSize);
    location = url.toString();
  }

  public HTTPRandomAccessFile2(URL url, int bufferSize) throws IOException {
    super( bufferSize);
    file = null;
    this.url = url;
    location = url.toString();

    if (debug) System.out.println("HTTPRandomAccessFile2 open "+url);
    conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    checkResponse( conn);

    total_length = conn.getContentLength();
    if (debug) System.out.println("HTTPRandomAccessFile2 open "+url+" len = "+total_length);
    if (debugLeaks) openFiles.add( location);
  }

  protected int read_(long pos, byte[] buff, int off, int len) throws IOException {
    long end = pos + len - 1;
    if (end >= total_length) {
      end = total_length - 1;
      len = (int) (end - pos + 1);
    }
    if (debug) System.out.println("-HTTPRandomAccessFile2 wants "+len+" = ("+pos+"-"+end+")");

    conn = (HttpURLConnection) url.openConnection();
    conn.addRequestProperty("Range", "bytes="+pos+"-"+end);
    checkResponse( conn);
    int length = conn.getContentLength();

    // copy to buffer
    int readLen = Math.min( len, length);

    InputStream is = conn.getInputStream();
    readLen = copy( is, buff, readLen);
    //if (debug) System.out.println(" HTTPRandomAccessFile2 readLen="+readLen);

    return readLen;
  }

  private int copy(InputStream in, byte[] buff, int want) throws IOException {
    int done = 0;
    while (true) {
      int bytesRead = in.read( buff, done, want);
      if (bytesRead == -1) break;
      done += bytesRead;
      want -= bytesRead;
    }
    return done;
  }

  private void checkResponse(HttpURLConnection currentConnection) throws IOException {
    int status = currentConnection.getResponseCode();
    String response = currentConnection.getResponseMessage();

    if (status == 404)
      throw new FileNotFoundException( url+" "+response);
    if (status >= 300)
      throw new IOException( url+" "+response);

    if (debugHeaders) {
      System.out.println("HTTP/1.x " + status + " " + response);
      for (int j = 1; ; j++) {
        String header = currentConnection.getHeaderField(j);
        String key = currentConnection.getHeaderFieldKey(j);
        if (header == null || key == null) break;
        System.out.println(" "+key + ": " + header);
      }
    }
  }


  // override RandomAccessFile public methods

  public long length( ) throws IOException {
    long fileLength = total_length;
    if( fileLength < dataEnd )
      return dataEnd;
    else
      return fileLength;
  }

  public void close() {
    if (conn != null)
      conn.disconnect();
    conn = null;
    if (debugLeaks) openFiles.remove( location);
  }

  public FileDescriptor getFD() {
    return null;
  }

}

