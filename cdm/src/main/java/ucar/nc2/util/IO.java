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

package ucar.nc2.util;

import ucar.unidata.util.StringUtil;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Formatter;
import java.util.zip.GZIPInputStream;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

/**
 * Input/Output utilities.
 *
 * @author John Caron
 */
public class IO {

  static private int default_file_buffersize = 9200;
  static private int default_socket_buffersize = 64000;
  static private boolean showStackTrace = false;
  static private boolean debug = false, showResponse = false;
  static private boolean showHeaders = false;

  static private Class cl;

  /** Open a resource as a Stream. First try ClassLoader.getResourceAsStream().
   *  If that fails, try a plain old FileInputStream().
   * @param resourcePath name of file path (use forward slashes!)
   * @return InputStream or null on failure
  */
  public static InputStream getFileResource( String resourcePath) {
    if (cl == null) cl = (new IO()).getClass();

    InputStream is = cl.getResourceAsStream(resourcePath);
    if (is != null) {
      if (debug) System.out.println("Resource.getResourceAsStream ok on "+resourcePath);
      return is;
    } else if (debug)
      System.out.println("Resource.getResourceAsStream failed on ("+resourcePath+")");

    try {
      is =  new FileInputStream(resourcePath);
      if (debug) System.out.println("Resource.FileInputStream ok on "+resourcePath);
    } catch (FileNotFoundException e) {
      if (debug)  System.out.println("  FileNotFoundException: Resource.getFile failed on "+resourcePath);
    } catch (java.security.AccessControlException e) {
      if (debug)  System.out.println("  AccessControlException: Resource.getFile failed on "+resourcePath);
    }

    return is;
  }

  /**
   * copy all bytes from in to out.
   *
   * @param in  InputStream
   * @param out OutputStream
   * @return number of bytes copied
   * @throws java.io.IOException on io error
   */
  static public long copy(InputStream in, OutputStream out) throws IOException {
    long totalBytesRead = 0;
    byte[] buffer = new byte[default_file_buffersize];
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1) break;
      out.write(buffer, 0, bytesRead);
      totalBytesRead += bytesRead;
    }
    return totalBytesRead;
  }

  /**
   * copy all bytes from in and throw them away.
   *
   * @param in  InputStream
   * @param buffersize size of buffer to use, if -1 uses default value (9200)
   * @return number of bytes copied
   * @throws java.io.IOException on io error
   */
  static public long copy2null(InputStream in, int buffersize) throws IOException {
    long totalBytesRead = 0;
    if (buffersize <= 0) buffersize = default_file_buffersize;
    byte[] buffer = new byte[buffersize];
    while (true) {
      int n = in.read(buffer);
      if (n == -1) break;
      totalBytesRead += n;
    }
    // if (fout != null) fout.format("done=%d %n",totalBytesRead);
    return totalBytesRead;
  }

  static public long touch(InputStream in, int buffersize) throws IOException {
    long touch = 0;
    if (buffersize <= 0) buffersize = default_file_buffersize;
    byte[] buffer = new byte[buffersize];
    while (true) {
      int n = in.read(buffer);
      if (n == -1) break;
      for (int i=0; i<buffersize; i++)
        touch += buffer[i];
    }
    // if (fout != null) fout.format("done=%d %n",totalBytesRead);
    return touch;
  }

  /**
   * copy all bytes from in and throw them away.
   *
   * @param in  FileChannel
   * @param buffersize size of buffer to use, if -1 uses default value (9200)
   * @return number of bytes copied
   * @throws java.io.IOException on io error
   */
  static public long copy2null(FileChannel in, int buffersize) throws IOException {
    long totalBytesRead = 0;
    if (buffersize <= 0) buffersize = default_file_buffersize;
    ByteBuffer buffer = ByteBuffer.allocate( buffersize);
    while (true) {
      int n = in.read(buffer);
      if (n == -1) break;
      totalBytesRead += n;
      buffer.flip();
    }
    return totalBytesRead;
  }

  static public long touch(FileChannel in, int buffersize) throws IOException {
    long touch = 0;
    if (buffersize <= 0) buffersize = default_file_buffersize;
    ByteBuffer buffer = ByteBuffer.allocate( buffersize);
    while (true) {
      int n = in.read(buffer);
      if (n == -1) break;

      // touch all the bytes
      //buffer.rewind();
      //for (int i=0; i<buffersize; i++)
      //  touch += buffer.get();

      byte[] result = buffer.array();
      for (int i=0; i<buffersize; i++)
        touch += result[i];

      buffer.flip();
    }
    return touch;
  }

  /**
   * copy all bytes from in to out, specify buffer size
   *
   * @param in         InputStream
   * @param out        OutputStream
   * @param bufferSize : internal buffer size.
   * @return number of bytes copied
   * @throws java.io.IOException on io error
   */
  static public long copyB(InputStream in, OutputStream out, int bufferSize) throws IOException {
    long totalBytesRead = 0;
    int done = 0, next = 1;
    boolean show = false;

    byte[] buffer = new byte[bufferSize];
    while (true) {
      int n = in.read(buffer);
      if (n == -1) break;
      out.write(buffer, 0, n);
      totalBytesRead += n;

      if (show) {
        done += n;
        if (done > 1000 * 1000 * next) {
          System.out.println(next + " Mb");
          next++;
        }
      }
    }
    return totalBytesRead;
  }

  /**
   * copy n bytes from in to out.
   *
   * @param in  InputStream
   * @param out OutputStream
   * @param n   number of bytes to copy
   * @throws java.io.IOException on io error
   */
  static public void copy(InputStream in, OutputStream out, int n) throws IOException {
    byte[] buffer = new byte[default_file_buffersize];
    int count = 0;
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1) break;
      out.write(buffer, 0, bytesRead);
      count += bytesRead;
      if (count > n) return;
    }
  }

  /**
   * Read the contents from the inputStream and place into a String,
   * with any error messages put in the return String.
   * Assume UTF-8 encoding.
   *
   * @param is the inputStream to read from.
   * @return String holding the contents, or an error message.
   * @throws java.io.IOException on io error
   */
  static public String readContents(InputStream is) throws IOException {
    return readContents(is, "UTF-8");
  }

   /**
   * Read the contents from the inputStream and place into a String,
   * with any error messages  put in the return String.
   *
   * @param is the inputStream to read from.
   * @return String holding the contents, or an error message.
   * @throws java.io.IOException on io error
   */
  static public String readContents(InputStream is, String charset) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(10 * default_file_buffersize);
    IO.copy(is, bout);
    return bout.toString(charset);
  }

  /**
   * Read the contents from the inputStream and place into a byte array,
   * with any error messages  put in the return String.
   *
   * @param is the inputStream to read from.
   * @return byte[] holding the contents, or an error message.
   * @throws java.io.IOException on io error
   */
  static public byte[] readContentsToByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(10 * default_file_buffersize);
    IO.copy(is, bout);
    return bout.toByteArray();
  }

  /**
   * Wite the contents from the String to a Stream,
   *
   * @param contents String holding the contents.
   * @param os       write to this OutputStream
   * @throws java.io.IOException on io error
   */
  static public void writeContents(String contents, OutputStream os) throws IOException {
    ByteArrayInputStream bin = new ByteArrayInputStream(contents.getBytes());
    IO.copy(bin, os);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Files

  /**
   * copy one file to another.
   *
   * @param fileInName  copy from this file, which must exist.
   * @param fileOutName copy to this file, which is overrwritten if already exists.
   * @throws java.io.IOException on io error
   */
  static public void copyFile(String fileInName, String fileOutName) throws IOException {
    InputStream in = null;
    OutputStream out = null;
    try {
      in = new BufferedInputStream(new FileInputStream(fileInName));
      out = new BufferedOutputStream(new FileOutputStream(fileOutName));
      IO.copy(in, out);
    } finally {
      if (null != in) in.close();
      if (null != out) out.close();
    }
  }

  /**
   * copy one file to another.
   *
   * @param fileIn  copy from this file, which must exist.
   * @param fileOut copy to this file, which is overrwritten if already exists.
   * @throws java.io.IOException on io error
   */
  static public void copyFile(File fileIn, File fileOut) throws IOException {
    InputStream in = null;
    OutputStream out = null;
    try {
      in = new BufferedInputStream(new FileInputStream(fileIn));
      out = new BufferedOutputStream(new FileOutputStream(fileOut));
      IO.copy(in, out);
    } finally {
      if (null != in) in.close();
      if (null != out) out.close();
    }
  }

  /**
   * copy file to output stream
   *
   * @param fileInName open this file
   * @param out        copy here
   * @throws java.io.IOException on io error
   */
  static public void copyFile(String fileInName, OutputStream out) throws IOException {
    copyFileB(new File(fileInName), out, default_file_buffersize);
  }

  /**
   * copy file to output stream, specify internal buffer size
   *
   * @param fileIn     copy this file
   * @param out        copy to this stream
   * @param bufferSize internal buffer size.
   * @throws java.io.IOException on io error
   */
  static public void copyFileB(File fileIn, OutputStream out, int bufferSize) throws IOException {
    InputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(fileIn));
      IO.copyB(in, out, bufferSize);
    } finally {
      if (null != in) in.close();
    }
  }

  /**
   * Copy part of a RandomAccessFile to output stream, specify internal buffer size
   *
   * @param raf    copy this file
   * @param offset start here (byte offset)
   * @param length number of bytes to copy
   * @param out    copy to this stream
   * @param buffer use this buffer.
   * @return number of bytes copied
   * @throws java.io.IOException on io error
   */
  static public long copyRafB(ucar.unidata.io.RandomAccessFile raf, long offset, long length, OutputStream out, byte[] buffer) throws IOException {
    int bufferSize = buffer.length;
    long want = length;
    raf.seek(offset);
    while (want > 0) {
      int len = (int) Math.min(want, bufferSize);
      int bytesRead = raf.read(buffer, 0, len);
      if (bytesRead <= 0) break;
      out.write(buffer, 0, bytesRead);
      want -= bytesRead;
    }
    return length - want;
  }

  /**
   * Copy an entire directory tree.
   *
   * @param fromDirName from this directory (do nothing if not exist)
   * @param toDirName   to this directory (will create if not exist)
   * @throws java.io.IOException on io error
   */
  static public void copyDirTree(String fromDirName, String toDirName) throws IOException {
    File fromDir = new File(fromDirName);
    File toDir = new File(toDirName);
    if (!fromDir.exists())
      return;
    if (!toDir.exists())
      toDir.mkdirs();

    for (File f : fromDir.listFiles()) {
      if (f.isDirectory())
        copyDirTree(f.getAbsolutePath(), toDir.getAbsolutePath() + "/" + f.getName());
      else
        copyFile(f.getAbsolutePath(), toDir.getAbsolutePath() + "/" + f.getName());
    }
  }

  /**
   * Read the file and place contents into a byte array,
   * with any error messages  put in the return String.
   *
   * @param filename the file to read from.
   * @return byte[] holding the contents, or an error message.
   * @throws java.io.IOException on io error
   */
  static public byte[] readFileToByteArray(String filename) throws IOException {
    InputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(filename));
      return readContentsToByteArray(in);

    } finally {
      if (in != null) in.close();
    }
  }

  /**
   * Read the contents from the named file and place into a String, assuming UTF-8 encoding.
   *
   * @param filename the URL to read from.
   * @return String holding the file contents
   * @throws java.io.IOException on io error
   */
  static public String readFile(String filename) throws IOException {
    InputStreamReader reader = new InputStreamReader(new FileInputStream(filename), "UTF-8");

    try {
      StringWriter swriter = new StringWriter(50000);
      UnsynchronizedBufferedWriter writer = new UnsynchronizedBufferedWriter(swriter);
      writer.write(reader);
      return swriter.toString();

    } finally {
      if (reader != null) reader.close();
    }
  }

  /**
   * Write String contents to a file, using UTF-8 encoding.
   *
   * @param contents String holding the contents
   * @param file     write to this file (overwrite if exists)
   * @throws java.io.IOException on io error
   */
  static public void writeToFile(String contents, File file) throws IOException {
    OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
    UnsynchronizedBufferedWriter writer = new UnsynchronizedBufferedWriter(fw);

    try {
      writer.write(contents);
      writer.flush();

    } finally {
      if (null != writer)
        writer.close();
    }
  }

  /**
   * Write byte[] contents to a file.
   *
   * @param contents String holding the contents
   * @param file     write to this file (overwrite if exists)
   * @throws java.io.IOException on io error
   */
  static public void writeToFile(byte[] contents, File file) throws IOException {
    FileOutputStream fw = new FileOutputStream( file);

    try {
      fw.write(contents);
      fw.flush();

    } finally {
      if (null != fw)
        fw.close();
    }
  }

  /**
   * Write contents to a file, using UTF-8 encoding.
   *
   * @param contents    String holding the contents
   * @param fileOutName write to this file (overwrite if exists)
   * @throws java.io.IOException on io error
   */
  static public void writeToFile(String contents, String fileOutName) throws IOException {
    writeToFile(contents, new File(fileOutName));
  }

  /**
   * copy input stream to file. close input stream when done.
   *
   * @param in          copy from here
   * @param fileOutName open this file (overwrite) and copy to it.
   * @return number of bytes copied
   * @throws java.io.IOException on io error
   */
  static public long writeToFile(InputStream in, String fileOutName) throws IOException {
    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(fileOutName));
      return IO.copy(in, out);

    } finally {
      if (null != in) in.close();
      if (null != out) out.close();
    }
  }

  static public long appendToFile(InputStream in, String fileOutName) throws IOException {
    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(fileOutName, true));
      return IO.copy(in, out);

    } finally {
      if (null != in) in.close();
      if (null != out) out.close();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // URLs

  /**
   * copy contents of URL to output stream, specify internal buffer size. request gzip encoding
   *
   * @param urlString  copy the contents of this URL
   * @param out        copy to this stream. If null, throw bytes away
   * @param bufferSize internal buffer size.
   * @return number of bytes copied
   * @throws java.io.IOException on io error
   */
  static public long copyUrlB(String urlString, OutputStream out, int bufferSize) throws IOException {
    long count;
    URL url;
    java.io.InputStream is = null;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      throw new IOException("** MalformedURLException on URL <" + urlString + ">\n" + e.getMessage() + "\n");
    }

    try {
      java.net.URLConnection connection = url.openConnection();
      java.net.HttpURLConnection httpConnection = null;
      if (connection instanceof java.net.HttpURLConnection) {
        httpConnection = (java.net.HttpURLConnection) connection;
        httpConnection.addRequestProperty("Accept-Encoding", "gzip");
      }

      if (showHeaders) {
        System.out.println("\nREQUEST Properties for " + urlString + ": ");
        Map reqs = connection.getRequestProperties();
        Iterator reqIter = reqs.keySet().iterator();
        while (reqIter.hasNext()) {
          String key = (String) reqIter.next();
          java.util.List values = (java.util.List) reqs.get(key);
          System.out.print(" " + key + ": ");
          for (int i = 0; i < values.size(); i++) {
            String v = (String) values.get(i);
            System.out.print(v + " ");
          }
          System.out.println("");
        }
      }

      // get response
      if (httpConnection != null) {
        int responseCode = httpConnection.getResponseCode();
        if (responseCode / 100 != 2)
          throw new IOException("** Cant open URL <" + urlString + ">\n Response code = " + responseCode
              + "\n" + httpConnection.getResponseMessage() + "\n");
      }

      if (showHeaders && (httpConnection != null)) {
        int code = httpConnection.getResponseCode();
        String response = httpConnection.getResponseMessage();

        // response headers
        System.out.println("\nRESPONSE for " + urlString + ": ");
        System.out.println(" HTTP/1.x " + code + " " + response);
        System.out.println("Headers: ");

        for (int j = 1; ; j++) {
          String header = connection.getHeaderField(j);
          String key = connection.getHeaderFieldKey(j);
          if (header == null || key == null) break;
          System.out.println(" " + key + ": " + header);
        }
      }

      // read it
      is = connection.getInputStream();

      // check if its gzipped
      if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
        //is = new GZIPInputStream( new BufferedInputStream(is, 1024));
        //is = new GZIPInputStream(is);
        is = new BufferedInputStream(new GZIPInputStream(is), 1000);
      }
      if (out == null)
        count = IO.copy2null(is, bufferSize);
      else
        count = IO.copyB(is, out, bufferSize);

    } catch (java.net.ConnectException e) {
      if (showStackTrace) e.printStackTrace();
      throw new IOException("** ConnectException on URL: <" + urlString + ">\n" +
          e.getMessage() + "\nServer probably not running");

    } catch (java.net.UnknownHostException e) {
      if (showStackTrace) e.printStackTrace();
      throw new IOException("** UnknownHostException on URL: <" + urlString + ">\n");

    } catch (Exception e) {
      if (showStackTrace) e.printStackTrace();
      throw new IOException("** Exception on URL: <" + urlString + ">\n" + e);

    } finally {
      if (is != null) is.close();
    }

    return count;
  }


  /**
   * read the contents from the named URL, write to a file.
   *
   * @param urlString the URL to read from.
   * @param file      write to this file
   * @return status or error message.
   */
  static public String readURLtoFile(String urlString, File file) {
    OutputStream out;
    try {
      out = new BufferedOutputStream(new FileOutputStream(file));
    } catch (IOException e) {
      if (showStackTrace) e.printStackTrace();
      return "** IOException opening file: <" + file + ">\n" + e.getMessage() + "\n";
    }

    try {
      copyUrlB(urlString, out, 20000);
      return "ok";

    } catch (IOException e) {
      if (showStackTrace) e.printStackTrace();
      return "** IOException reading URL: <" + urlString + ">\n" + e.getMessage() + "\n";

    } finally {

      try {
        out.close();
      } catch (IOException e) {
        return "** IOException closing file : <" + file + ">\n" + e.getMessage() + "\n";
      }
    }

  }

  /**
   * Read the contents from the given URL and place into a byte array,
   * with any error messages  put in the return String.
   *
   * @param urlString read from this URL.
   * @return byte[] holding the contents, or an error message.
   * @throws java.io.IOException on io error
   */
  static public byte[] readURLContentsToByteArray(String urlString) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(200000);
    copyUrlB(urlString, bout, 200000);
    return bout.toByteArray();
  }


  /**
   * read the contents from the named URL, write to a file.
   *
   * @param urlString the URL to read from.
   * @param file      write to this file
   * @return status or error message.
   * @throws IOException if failure
   */
  static public String readURLtoFileWithExceptions(String urlString, File file) throws IOException {
    return readURLtoFileWithExceptions(urlString, file, default_socket_buffersize);

  }

  /**
   * read the contents from the named URL, write to a file.
   *
   * @param urlString   the URL to read from.
   * @param file        write to this file
   * @param buffer_size read/write in this size chunks
   * @return status or error message.
   * @throws IOException if failure
   */
  static public String readURLtoFileWithExceptions(String urlString, File file, int buffer_size) throws IOException {
    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));

    try {
      copyUrlB(urlString, out, buffer_size);
      return "ok";

    } finally {

      try {
        out.close();
      } catch (IOException e) {
        return "** IOException closing file : <" + file + ">\n" + e.getMessage() + "\n";
      }
    }

  }


  /**
   * Read the contents from the named URL and place into a String.
   *
   * @param urlString the URL to read from.
   * @return String holding the contents.
   * @throws IOException if fails
   */
  static public String readURLcontentsWithException(String urlString) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(20000);
    copyUrlB(urlString, bout, 20000);
    return bout.toString();
  }

  /**
   * Read the contents from the named URL and place into a String,
   * with any error messages  put in the return String.
   *
   * @param urlString the URL to read from.
   * @return String holding the contents, or an error message.
   */
  static public String readURLcontents(String urlString) {
    try {
      return readURLcontentsWithException(urlString);
    } catch (IOException e) {
      return e.getMessage();
    }
  }

  /**
   * use HTTP PUT to send the contents to the named URL.
   *
   * @param urlString the URL to read from. must be http:
   * @param contents  String holding the contents
   * @return a Result object; generally 0 <= code <=400 is ok
   */
  static public HttpResult putToURL(String urlString, String contents) {

    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      return new HttpResult(-1, "** MalformedURLException on URL (" + urlString + ")\n" + e.getMessage());
    }

    try {
      java.net.HttpURLConnection c = (HttpURLConnection) url.openConnection();
      c.setDoOutput(true);
      c.setRequestMethod("PUT");

      // read it
      OutputStream out = new BufferedOutputStream(c.getOutputStream());
      IO.copy(new ByteArrayInputStream(contents.getBytes()), out);
      out.flush();
      out.close();

      int code = c.getResponseCode();
      String mess = c.getResponseMessage();
      return new HttpResult(code, mess);

    } catch (java.net.ConnectException e) {
      if (showStackTrace) e.printStackTrace();
      return new HttpResult(-2, "** ConnectException on URL: <" + urlString + ">\n" + e.getMessage() + "\nServer probably not running");

    } catch (IOException e) {
      if (showStackTrace) e.printStackTrace();
      return new HttpResult(-3, "** IOException on URL: (" + urlString + ")\n" + e.getMessage());
    }

  }

  /**
   * Holds the result of an HTTP action.
   */
  static public class HttpResult {
    public int statusCode;
    public String message;

    HttpResult(int code, String message) {
      this.statusCode = code;
      this.message = message;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // test

  static public void testRead() {
    String baseUrl = "http://moca.virtual.museum/legac/legac01.htm";
    String baseDir = "";
    File dir = new File(baseDir);
    dir.mkdirs();

    for (int i = 1; i < 159; i++) {
      String n = StringUtil.padZero(i, 3);
      String filename = n + ".jpg";
      System.out.println("Open " + baseDir + filename);
      File file = new File(baseDir + filename);

      readURLtoFile(baseUrl + filename, file);
    }
  }

  // read URL to File
  static public void main4(String[] args) {
    String url = "http://whoopee:8080/thredds/dodsC/test/2005052412_NAM.wmo.dods?Best_4-layer_lifted_index";
    String filenameOut = "C:/temp/tempFile4.compress";
    File f = new File(filenameOut);

    long start = System.currentTimeMillis();
    String result = readURLtoFile(url, f);
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(result);
    System.out.println(" that took = " + took + "sec");
    System.out.println(" file size = " + f.length());
  }

  // how many files can be opened ??
  static public void mainn(String[] args) {
    long start = System.currentTimeMillis();
    ArrayList fileList = new ArrayList();
    ArrayList rafList = new ArrayList();
    int count = 0;
    while (true) {
      try {
        File temp = File.createTempFile("test", "tmp");
        fileList.add(temp);
        RandomAccessFile raf = new RandomAccessFile(temp, "r");
        rafList.add(raf);

      } catch (IOException e) {
        e.printStackTrace();
        break;
      }
      count++;
      if (count % 50 == 0) System.out.println(" " + count);
    }
    long took = System.currentTimeMillis() - start;
    System.out.println(" Created and opened " + count + " files in " + took + " msecs");

    start = System.currentTimeMillis();
    Iterator iter = rafList.iterator();
    while (iter.hasNext()) {
      RandomAccessFile raf = (RandomAccessFile) iter.next();
      try {
        raf.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    took = System.currentTimeMillis() - start;
    System.out.println(" Closed " + count + " files in " + took + " msecs");

    iter = fileList.iterator();
    while (iter.hasNext()) {
      File file = (File) iter.next();
      file.delete();
    }
    took = System.currentTimeMillis() - start;
    System.out.println(" Deleted " + count + " files in " + took + " msecs");

    count = 0;
    took = System.currentTimeMillis() - start;
    File dir = new File("/var/tmp/");
    File[] list = dir.listFiles();
    for (int i = 0; i < list.length; i++) {
      File file = list[i];
      if (file.getName().endsWith("tmp")) {
        file.delete();
        count++;
      }
    }
    took = System.currentTimeMillis() - start;
    System.out.println(" Deleted " + count + " files in " + took + " msecs");

  }

  // read URL to File
  static Formatter fout;
  static public void main(String[] args) throws IOException {
    FileOutputStream f = new FileOutputStream("C:/TEMP/read.txt");
    fout = new Formatter(f);
    //String url = "http://newmotherlode.ucar.edu:8081/thredds/fileServer/nexrad/level2/KDVN/20080921/Level2_KDVN_20080921_1024.ar2v";
    String url = "http://newmotherlode.ucar.edu:8081/thredds/fileServer/fmrc/NCEP/GFS/Global_onedeg/files/GFS_Global_onedeg_20080922_0600.grib2";
    long start = System.currentTimeMillis();
    IO.copyUrlB(url, null, 1000 * 1000); // read data and throw away
    double took = .001 * (System.currentTimeMillis() - start);

    //String url = "http://motherlode.ucar.edu:9080/thredds/ncss/metars?variables=all&north=82.5199&west=88.4499&east=90.4000&south=-90.0000&latitude=&longitude=&spatial=stns&stn=LOWW&time_start=2007-12-02T23%3A45%3A04Z&time_end=present&temporal=point&time=2007-12-02T23%3A45%3A04Z&accept=raw";
    //String url2 = "http://motherlode.ucar.edu:8080/thredds/dodsC/fmrc/NCEP/NAM/CONUS_80km/files/NAM_CONUS_80km_20071217_0000.grib1.dods?Total_precipitation";
    /* long start = System.currentTimeMillis();
    File fileResult = new File("R:/testdata2/bufr/vosclim_2008032301");
    String result = readURLtoFile(url, fileResult);
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(result);
    System.out.println(" result size = " + result.length());
    System.out.println(" file size = " + fileResult.length());
    */
    fout.flush();
    f.close();
    System.out.println(" that took = " + took + " sec");
  }

}