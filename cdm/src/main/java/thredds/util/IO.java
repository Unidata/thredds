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

package thredds.util;

import ucar.unidata.util.StringUtil;

import java.io.*;
import java.net.*;
import java.util.zip.DeflaterOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Input/Output utilities.
 *
 * @author John Caron
 * @version $Id$
 */
public class IO {

  static boolean showStackTrace = true;
  static boolean debug = false, showResponse = false;

  /**
   * copy all bytes from in to out.
   * @param in: InputStream
   * @param out: OutputStream
   * @throws IOException
   */
  static public void copy(InputStream in, OutputStream out) throws IOException {
      byte[] buffer = new byte[9000];
      while (true) {
        int bytesRead = in.read(buffer);
        if (bytesRead == -1) break;
        out.write(buffer, 0, bytesRead);
      }
  }

  /**
   * copy all bytes from in to out.
   * @param in: InputStream
   * @param out: OutputStream
   * @param bufferSize : internal buffer size.
   * @throws IOException
   */
  static public void copyB(InputStream in, OutputStream out, int bufferSize) throws IOException {
    byte[] buffer = new byte[bufferSize];
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1) break;
      out.write(buffer, 0, bytesRead);
    }
  }

  /**
   * copy n bytes from in to out.
   * @param in: InputStream
   * @param out: OutputStream
   * @param n: number of bytes to copy
   * @throws IOException
   */
  static public void copy(InputStream in, OutputStream out, int n) throws IOException {
      byte[] buffer = new byte[9000];
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
   * with any error messages  put in the return String.
   * @param is : the inputStream to read from.
   * @return String holding the contents, or an error message.
   */
  static public String readContents( InputStream is) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(1000);
    thredds.util.IO.copy(is, bout);
    return bout.toString();
  }

  /**
   * Read the contents from the inputStream and place into a byte array,
   * with any error messages  put in the return String.
   * @param is : the inputStream to read from.
   * @return  byte[] holding the contents, or an error message.
   */
  static public byte[] readContentsToByteArray( InputStream is) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(1000);
    thredds.util.IO.copy(is, bout);
    return bout.toByteArray();
  }

   /**
   * Wite the contents from the String to a Stream,
   * @param contents String holding the contents.
   * @param os write to this OutputStream
   */
  static public void writeContents( String contents, OutputStream os) throws IOException {
    ByteArrayInputStream bin = new ByteArrayInputStream( contents.getBytes());
    thredds.util.IO.copy(bin, os);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Files

   /**
    * copy one file to another.
    * @param fileInName: copy from this file, which must exist.
    * @param fileOutName: copy to this file, which is overrwritten if already exists.
    * @throws IOException
    */
  static public void copyFile(String fileInName, String fileOutName) throws IOException {
    InputStream in = null;
    OutputStream out = null;
    try {
      in = new BufferedInputStream( new FileInputStream( fileInName));
      out = new BufferedOutputStream( new FileOutputStream( fileOutName));
      thredds.util.IO.copy( in, out);
    } finally {
      if (null != in) in.close();
      if (null != out) out.close();
    }
  }

   /**
    * copy one file to another.
    * @param fileIn: copy from this file, which must exist.
    * @param fileOut: copy to this file, which is overrwritten if already exists.
    * @throws IOException
    */
  static public void copyFile(File fileIn, File fileOut) throws IOException {
    InputStream in = null;
    OutputStream out = null;
    try {
      in = new BufferedInputStream( new FileInputStream( fileIn));
      out = new BufferedOutputStream( new FileOutputStream( fileOut));
      thredds.util.IO.copy( in, out);
    } finally {
      if (null != in) in.close();
      if (null != out) out.close();
    }
  }

  /**
   * copy file to output stream
   * @param fileInName open this file
   * @param out : copy here
   * @throws IOException
   */
  static public void copyFile(String fileInName, OutputStream out) throws IOException {
    copyFileB( new File( fileInName), out, 20000);
  }

  /**
   * copy file to output stream, specify internal buffer size
   * @param fileIn copy this file
   * @param out : copy to this stream
   * @param bufferSize : internal buffer size.
   * @throws IOException
   */
  static public void copyFileB(File fileIn, OutputStream out, int bufferSize) throws IOException {
    InputStream in = null;
    try {
      in = new BufferedInputStream( new FileInputStream( fileIn));
      thredds.util.IO.copyB( in, out, bufferSize);
    } finally {
      if (null != in) in.close();
    }
  }

  /**
   * Copy part of a RandomAccessFile to output stream, specify internal buffer size
   * @param raf copy this file
   * @param offset start here (byte offset)
   * @param length number of bytes to copy
   * @param out : copy to this stream
   * @param buffer : use this buffer.
   * @throws IOException
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
   * @param fromDirName from this directory (do nothing if not exist)
   * @param toDirName to this directory (will create if not exist)
   * @throws IOException
   */
  static public void copyDirTree(String fromDirName, String toDirName) throws IOException {
    File fromDir = new File(fromDirName);
    File toDir = new File(toDirName);
    if (!fromDir.exists())
      return;
    if (!toDir.exists())
      toDir.mkdirs();

    File[] files = fromDir.listFiles();
    for (int i=0; i<files.length; i++) {
      File f = files[i];
      if (f.isDirectory())
        copyDirTree(f.getAbsolutePath(), toDir.getAbsolutePath() + "/" + f.getName());
      else
        copyFile( f.getAbsolutePath(), toDir.getAbsolutePath() + "/" + f.getName());
    }
  }

  /**
   * Read the file and place contents into a byte array,
   * with any error messages  put in the return String.
   * @param filename : the file to read from.
   * @return  byte[] holding the contents, or an error message.
   */
  static public byte[] readFileToByteArray( String filename) throws IOException {
    InputStream in = null;
    try {
      in = new BufferedInputStream( new FileInputStream( filename));
      return readContentsToByteArray( in);

    } finally {
      if (in != null) in.close();
    }
  }

  /**
   * Read the contents from the named file and place into a String,
   * with any error messages  put in the return String.
   * @param filename: the URL to read from.
   * @return String holding the contents, or an error message.
   */
  static public String readFile( String filename) throws IOException {
    InputStream in = null;
    try {
       in = new BufferedInputStream( new FileInputStream( filename));
       return readContents (in);

    } finally {
      if (in != null) in.close();
    }
  }

  /**
   * Write contents to a file
   * @param contents String holding the contents
   * @param file  write to this file (overwrite if exists)
   * @param contents String holding the contents
   */
  static public void writeToFile( String contents, File file) throws IOException {
    OutputStream out = null;
    try {
      out = new BufferedOutputStream( new FileOutputStream( file));
      thredds.util.IO.copy( new ByteArrayInputStream(contents.getBytes()), out);
      out.flush();

    } finally {
      if (null != out)
        out.close();
    }
  }

  /**
   * Write contents to a file
   * @param contents String holding the contents
   * @param fileOutName  write to this file (overwrite if exists)
   */
  static public void writeToFile(String contents, String fileOutName) throws IOException {
    writeToFile( contents, new File(fileOutName ));
  }

  /**
   * copy input stream to file. close input stream when done.
   *
   * @param in          copy from here
   * @param fileOutName open this file (overwrite) and copy to it.
   * @throws IOException
   */
  static public void writeToFile(InputStream in, String fileOutName) throws IOException {
    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(fileOutName));
      thredds.util.IO.copy(in, out);

    } finally {
      if (null != in) in.close();
      if (null != out) out.close();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // URLs

  /**
   * copy contents of URL to output stream, specify internal buffer size
   * @param urlString copy the contents of this URL
   * @param out : copy to this stream
   * @param bufferSize : internal buffer size.
   * @throws IOException
   */
  static public void copyUrlB(String urlString, OutputStream out, int bufferSize) throws IOException {
    URL url = null;
    java.io.InputStream is = null;
    try {
      url =  new URL( urlString);
    } catch (MalformedURLException e) {
      throw new IOException( "** MalformedURLException on URL <"+urlString+">\n"+e.getMessage()+"\n");
    }

    try {
      java.net.URLConnection connection = url.openConnection();

      if (connection instanceof HttpURLConnection) {
        java.net.HttpURLConnection httpConnection = (HttpURLConnection) connection;
        // check response code is good
        int responseCode = httpConnection.getResponseCode();
        if (responseCode/100 != 2)
          throw new IOException( "** Cant open URL <"+urlString+">\n Response code = " +responseCode
              +"\n"+httpConnection.getResponseMessage()+"\n");
      }

      // read it
      is = connection.getInputStream();
      thredds.util.IO.copyB(is, out, bufferSize);

    } catch (java.net.ConnectException e) {
        if (showStackTrace) e.printStackTrace();
        throw new IOException( "** ConnectException on URL: <"+urlString+">\n"+
          e.getMessage()+"\nServer probably not running");

    } finally {
      if (is != null) is.close();
    }
  }

  /**
   * read the contents from the named URL, write to a file.
   * @param urlString: the URL to read from.
   * @return status or error message.
   */
  static public String  readURLtoFile( String urlString, File file) {
    OutputStream out = null;
    try {
      out = new BufferedOutputStream( new FileOutputStream( file));
    } catch (IOException e) {
        if (showStackTrace) e.printStackTrace();
        return "** IOException opening file: <"+file+">\n"+e.getMessage()+"\n";
    }

    try {
      copyUrlB(urlString, out, 20000);
      return "ok";

    } catch (IOException e) {
        if (showStackTrace) e.printStackTrace();
        return "** IOException reading URL: <"+urlString+">\n"+e.getMessage()+"\n";

    } finally {

      try {
        out.close();
      } catch (IOException e) {
        return "** IOException closing file : <"+file+">\n"+e.getMessage()+"\n";
      }
    }

  }

  /**
   * Read the contents from the named URL and place into a String.
   * @param urlString: the URL to read from.
   * @return String holding the contents.
   * @throws IOException if fails
   */
  static public String readURLcontentsWithException( String urlString) throws IOException {
      ByteArrayOutputStream bout = new ByteArrayOutputStream(20000);
      copyUrlB( urlString, bout, 20000);
      return bout.toString();
  }

  /**
   * Read the contents from the named URL and place into a String,
   * with any error messages  put in the return String.
   * @param urlString: the URL to read from.
   * @return String holding the contents, or an error message.
   */
  static public String readURLcontents( String urlString) {
    try {
      return readURLcontentsWithException( urlString);
    } catch (IOException e) {
      return e.getMessage();
    }
  }

  /**
   * use HTTP PUT to send the contents to the named URL.
   * @param urlString : the URL to read from. must be http:
   * @param contents String holding the contents
   * @return a Result object; generally 0 <= code <=400 is ok
   */
  static public Result putToURL( String urlString, String contents) {

    URL url;
    try {
      url =  new URL( urlString);
    } catch (MalformedURLException e) {
      return new Result( -1, "** MalformedURLException on URL ("+urlString+")\n"+e.getMessage());
    }

    try {
      java.net.HttpURLConnection c = (HttpURLConnection) url.openConnection();
      c.setDoOutput(true);
      c.setRequestMethod("PUT");

      // read it
      OutputStream out = new BufferedOutputStream( c.getOutputStream());
      thredds.util.IO.copy( new ByteArrayInputStream(contents.getBytes()), out);
      out.flush();
      out.close();

      int code = c.getResponseCode();
      String mess = c.getResponseMessage();
      return new Result(code, mess);

    } catch (java.net.ConnectException e) {
        if (showStackTrace) e.printStackTrace();
        return new Result(-2, "** ConnectException on URL: <"+urlString+">\n"+e.getMessage()+"\nServer probably not running");

    } catch (IOException e) {
        if (showStackTrace) e.printStackTrace();
        return new Result(-3, "** IOException on URL: ("+urlString+")\n"+e.getMessage());
    }

  }

  static public class Result {
    public int code;
    public String message;
     Result( int code, String message) {
      this.code = code;
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

    for (int i=1; i<159; i++) {
      String n  = StringUtil.padZero(i, 3);
      String filename = n+".jpg";
      System.out.println("Open "+baseDir + filename);
      File file = new File( baseDir + filename);

      readURLtoFile( baseUrl+filename, file);
    }
  }

  static public void main3( String[] args) throws IOException {
    String filenameOut = "C:/temp/tempFile3.compress";

    FileOutputStream fout = new FileOutputStream(filenameOut);
    DeflaterOutputStream dout = new DeflaterOutputStream(fout);
    BufferedOutputStream bout = new BufferedOutputStream(dout, 100 * 1000);
    DataOutputStream out = new DataOutputStream(dout);

    long start = System.currentTimeMillis();
    for (int i=0; i<(100*1000);i++) {
      out.writeFloat((float) i);
    }
    fout.close();
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(" that took = "+took+"sec");

  }

  static public void main2( String[] args) throws IOException {
    String filenameIn = "C:/temp/tempFile2";
    String filenameOut = "C:/temp/tempFile2.compress2";

    FileInputStream fin = new FileInputStream(filenameIn);
    FileOutputStream fout = new FileOutputStream(filenameOut);
    BufferedOutputStream bout = new BufferedOutputStream(fout, 10 * 1000);
    DeflaterOutputStream out = new DeflaterOutputStream(fout);

    long start = System.currentTimeMillis();
    copyB(fin, out, 10 * 1000);
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(" that took = "+took+"sec");

    fin.close();
    fout.close();
  }

  static public void main4( String[] args) {
    String url = "http://whoopee:8080/thredds/dodsC/test/2005052412_NAM.wmo.dods?Best_4-layer_lifted_index";
    String filenameOut = "C:/temp/tempFile4.compress";
    File f = new File(filenameOut);

    long start = System.currentTimeMillis();
    String result = readURLtoFile( url, f);
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(result);
    System.out.println(" that took = "+took+"sec");
    System.out.println(" file size = "+f.length());
  }

  // how many files can be opened ??
  static public void main( String[] args) {
    long start = System.currentTimeMillis();
    ArrayList fileList = new ArrayList();
    ArrayList rafList = new ArrayList();
    int count = 0;
    while (true) {
      try {
        File temp = File.createTempFile("test", "tmp");
        fileList.add( temp);
        RandomAccessFile raf = new RandomAccessFile( temp, "r");
        rafList.add( raf);

      } catch (IOException e) {
        e.printStackTrace();
        break;
      }
      count++;
      if (count % 50 == 0) System.out.println(" "+count);
    }
    long took = System.currentTimeMillis() - start;
    System.out.println(" Created and opened "+count+" files in "+ took+" msecs");

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
    System.out.println(" Closed "+count+" files in "+ took+" msecs");

    iter = fileList.iterator();
    while (iter.hasNext()) {
      File file = (File) iter.next();
      file.delete();
    }
    took = System.currentTimeMillis() - start;
    System.out.println(" Deleted "+count+" files in "+ took+" msecs");

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
    System.out.println(" Deleted "+count+" files in "+ took+" msecs");

  }
}