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
package timing.IO;

import ucar.nc2.util.IO;
import ucar.unidata.util.Format;

import java.io.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.util.Date;

/*
  Sequential read - 60-80 Mb/sec.
  Sequential read and write - 23 Mb/sec
  Sequential read send across localhost socket - 25 Mb/sec   (retest on diff machines)
  Sequential read send across localhost socket and write - 20 Mb/sec

  netcdf service: read into array, send across:

  with MD5:
   copyURL (10000) took = 1.688 sec; len= 10.107288 Mbytes; rate = 5.987729857819906Mb/sec ok=ok
   copyURL (10000) took = 2.391 sec; len= 15.160932 Mbytes; rate = 6.340833124215809Mb/sec ok=ok

 with MD2:
   copyURL (10000) took = 6.328 sec; len= 15.160932 Mbytes; rate = 2.3958489254108724Mb/sec ok=ok
   copyURL (10000) took = 6.234 sec; len= 15.160932 Mbytes; rate = 2.4319749759384024Mb/sec ok=ok

  without checksum:
   copyURL (10000) took = 1.4060000000000001 sec; len= 10.107256 Mbytes; rate = 7.188660028449501Mb/sec ok=ok
   copyURL (10000) took = 1.687 sec; len= 15.160884 Mbytes; rate = 8.98689033787789Mb/sec ok=ok

 */

public class TimeCopy {
  static boolean debug = true;

  static public void main(String args[]) throws IOException {
    // took = 316.531 sec; len= 20.0 Mbytes; rate = .0631Mb/sec ok=ok
    copyURL("http://motherlode.ucar.edu:9080/thredds/netcdf/null/dd/RUC2_CONUS_40km_20070709_1800.nc?v,u,Z,T", "C:/temp/null.nc", 10000);
    
    //copyURL("http://localhost:8080/thredds/netcdf/dd/RUC2_CONUS_40km_20070709_1800.nc?v,u,Z,T", "C:/temp/ruc.nc", 10000);
    //copyURL("http://localhost:8080/thredds/netcdf/stream/dd/RUC2_CONUS_40km_20070709_1800.nc?v,u,Z,T", "C:/temp/rucStream.nc", 10000);
    //copyURL("http://localhost:8080/thredds/netcdf/null/dd/RUC2_CONUS_40km_20070709_1800.nc?v,u,Z,T", "C:/temp/null.nc", 10000);

    //copyURL2null("http://localhost:8080/thredds/netcdf/null/dd/RUC2_CONUS_40km_20070709_1800.nc?v,u,Z,T", "C:/temp/null.nc", 10000);
    //copyURL2null("http://localhost:8080/thredds/netcdf/dd/RUC2_CONUS_40km_20070709_1800.nc?v,u,Z,T", "C:/temp/ruc.nc", 10000);
    //copyURL2null("http://localhost:8080/thredds/netcdf/stream/dd/RUC2_CONUS_40km_20070709_1800.nc?v,u,Z,T", "C:/temp/rucStream.nc", 10000);
    //copyURL2null("http://localhost:8080/thredds/netcdf/null/dd/RUC2_CONUS_40km_20070709_1800.nc?v,u,Z,T", "C:/temp/null.nc", 10000);
    //copyURL("http://localhost:8080/thredds/netcdf/dd/nssl/mosaic3d_nc/tile1/20070803-2300.netcdf?mrefl_mosaic", "C:/temp/netcdf2.nc", 10000);
    //copyURL("http://localhost:8080/thredds/netcdf/stream/dd/nssl/mosaic3d_nc/tile1/20070803-2300.netcdf?mrefl_mosaic", "C:/temp/nsslStream.nc", 10000);
    //copyURL("http://localhost:8080/thredds/netcdf/dd/nssl/mosaic3d_nc/tile1/20070803-2300.netcdf?mrefl_mosaic", "C:/temp/nssl.nc", 10000); // */
  }

    /* readURL( "http://localhost:8080/thredds/fileServer/bigOle/copyOut", 10000);
    readURL( "http://localhost:8080/thredds/fileServer/bigOle/copyOut2", 20000);
    readURL( "http://localhost:8080/thredds/fileServer/bigOle/copyOut3", 40000);
    readURL( "http://localhost:8080/thredds/fileServer/bigOle/copyOut4", 60000);

    /*
  readURL (8000) took = 74.907 sec; rate = 26.288926562270547Mb/sec avg read= 4548.0
 readURL (16000) took = 108.406 sec; rate = 18.165273342803903Mb/sec avg read= 6923.0
 readURL (32000) took = 70.265 sec; rate = 28.02568308546218Mb/sec avg read= 8459.0
 readURL (64000) took = 88.84400000000001 sec; rate = 22.164970307505286Mb/sec avg read= 8054.0
  readURL (10000) took = 91.593 sec; rate = 21.49972838535696Mb/sec avg read= 7046.0
 readURL (20000) took = 86.766 sec; rate = 22.695809671991334Mb/sec avg read= 7338.0
 readURL (40000) took = 92.516 sec; rate = 21.285233062389207Mb/sec avg read= 7091.0
 readURL (60000) took = 91.85900000000001 sec; rate = 21.437470710545508Mb/sec avg read= 6792.0

     */

    // copyURL("http://localhost:8080/thredds/fileServer/bigOle/hdf5/IASI.h5", "C:/temp/copyURL", 40000);

    /*
 copyURL (20000) took = 5.656 sec; rate = 9.224809052333805Mb/sec
 copyURL (40000) took = 123.093 sec; rate = 15.99786033324397Mb/sec
 copyURL (100000) took = 96.516 sec; rate = 20.40308987110945Mb/sec
 copyURL (40000) took = 111.672 sec; rate = 17.634005140053013Mb/sec



     */

    /* readFile("C:/temp/copyOut", 9100);
     readFile("C:/temp/copyOut2", 9200);
     readFile("C:/temp/copyOut3", 9300);
     readFile("C:/temp/copyOut4", 9600);

     /*
  copy (4000) took = 32.968 sec; rate = 59.73139474642076Mb/sec total=5625549
  copy (8000) took = 30.813000000000002 sec; rate = 63.90888981923214Mb/sec total=2815958
  copy (14000) took = 39.812 sec; rate = 49.46309208278911Mb/sec total=1603234
  copy (20000) took = 46.313 sec; rate = 42.51991065143696Mb/sec total=1121352
   copy (7000) took = 32.344 sec; rate = 60.883768921592875Mb/sec total=3208423
  copy (8000) took = 25.89 sec; rate = 76.06120594824256Mb/sec total=2815958
  copy (9000) took = 24.406 sec; rate = 80.6860862902565Mb/sec total=2491645
  copy (10000) took = 24.907 sec; rate = 79.06309961055125Mb/sec total=2243816
  copy (8096) took = 28.063 sec; rate = 70.17156476499305Mb/sec total=2780070
  copy (8000) took = 26.562 sec; rate = 74.13691069949552Mb/sec total=2815958
  copy (9100) took = 24.235 sec; rate = 81.25540012378791Mb/sec total=2462211
  copy (10000) took = 28.109 sec; rate = 70.056729944146Mb/sec total=2243816
  copy (8900) took = 40.938 sec; rate = 48.10260936049636Mb/sec total=2512936
  copy (9000) took = 26.375 sec; rate = 74.66254490995262Mb/sec total=2491645
  copy (9100) took = 24.859 sec; rate = 79.215761776419Mb/sec total=2462211
  copy (9200) took = 24.641000000000002 sec; rate = 79.91658707032994Mb/sec total=2442723
   copy (9100) took = 29.406000000000002 sec; rate = 66.96676263347615Mb/sec total=2462211
  copy (9200) took = 42.141 sec; rate = 46.729423174580575Mb/sec total=2442723
  copy (9300) took = 27.563 sec; rate = 71.4444952291115Mb/sec total=2407049
  copy (9600) took = 28.672 sec; rate = 68.6811042829241Mb/sec total=2339086

    */

    //copyFile("C:/data/hdf5/IASI.h5", "C:/temp/copyOut", true);
    /* copyFile("C:/data/hdf5/IASI.h5", "C:/temp/copyOut2", true);
    copyFile("C:/data/hdf5/IASI.h5", "C:/temp/copyOut3", false);
    copyFile("C:/data/hdf5/IASI.h5", "C:/temp/copyOut4", true); */

    /*
     copy (no buffer) took = 90.516 sec; rate = 21.755541804763798Mb/sec
     copy (buffer) took = 84.703 sec; rate = 23.248581773963142Mb/sec
     copy (no buffer) took = 87.98400000000001 sec; rate = 22.38162190852882Mb/sec
     copy (buffer) took = 86.5 sec; rate = 22.765602566473987Mb/sec */

    /*
     *  copyFileNIO(1000 kb chunk) took = 177.282 sec; rate = 11.107865558827179Mb/sec
     *  copyFileNIO(1000 kb chunk) took = 218.234 sec; rate = 9.023454741241054Mb/sec
     *  copyFileNIO(1000 kb chunk) took = 150.468 sec; rate = 13.087331671850492Mb/sec
     *  prealloc
     *  copyFileNIO(1000 kb chunk) took = 218.26500000000001 sec; rate = 9.022173147320917Mb/sec
     *  copyFileNIO(1000 kb chunk) took = 218.26500000000001 sec; rate = 9.022173147320917Mb/sec
     *  copyFileNIO(10 kb chunk) took = 117.422 sec; rate = 16.770491236735875Mb/sec
     *  copyFileNIO(20 kb chunk) took = 123.85900000000001 sec; rate = 15.898922339111408Mb/sec
     *  copyFileNIO(9 kb chunk) took = 128.187 sec; rate = 15.362124255969793Mb/sec
     *  copy (buffer) took = 106.125 sec; rate = 18.555709041224972Mb/sec
     *  copyFileNIO(100 kb chunk) took = 203.687 sec; rate = 9.667895457245676Mb/sec
     *  copy (buffer) took = 146.766 sec; rate = 13.417444244579809Mb/sec
     *  copyFileNIO(1 kb chunk) took = 167.937 sec; rate = 11.725972370591352Mb/sec
     *
     * bert
     *  copyFileNIO(10 kb chunk) took = 185.266 sec; rate = 10.629174387097471Mb/sec
        copy (buffer) took = 130.828 sec; rate = 15.052011969914696Mb/sec
        copyFileNIO(10 kb chunk) took = 296.363 sec; rate = 6.644637225294655Mb/sec
        copy (buffer) took = 84.73400000000001 sec; rate = 23.24007626218519Mb/sec
        copy (buffer) took = 107.459 sec; rate = 18.325357782968386Mb/sec
        copyFileNIO(10 kb chunk) took = 115.798 sec; rate = 17.005687680270817Mb/sec
     *
    copyFileNIO("C:/data/hdf5/IASI.h5", "C:/temp/nio", 10);
    copyFile("C:/data/hdf5/IASI.h5", "C:/temp/copyOut4", true);
    copyFileNIO("C:/data/hdf5/IASI.h5", "C:/temp/nio", 100);  */

  //}

  static public void copyFile(String filenameIn, String filenameOut, boolean buffer) throws IOException {
    long lenIn = new File(filenameIn).length();
    if (debug) System.out.println("read " + filenameIn + " len = " + lenIn);

    InputStream in = new FileInputStream(filenameIn);
    if (buffer) new BufferedInputStream(in, 10000);

    OutputStream out = new FileOutputStream(filenameOut);
    if (buffer) out = new BufferedOutputStream(out, 1000);

    long start = System.currentTimeMillis();
    IO.copyB(in, out, 10000);
    out.flush();
    double took = .001 * (System.currentTimeMillis() - start);

    out.close();
    in.close();

    long lenOut = new File(filenameOut).length();
    if (debug) System.out.println(" write file= " + filenameOut + " len = " + lenOut);

    double rate = lenIn / took / (1000 * 1000);
    String name = buffer ? "buffer" : "no buffer";
    System.out.println(" copy (" + name + ") took = " + took + " sec; rate = " + rate + "Mb/sec");
  }

  static public void copyFileNIO(String filenameIn, String filenameOut, long kbchunks) throws IOException {

    FileInputStream in = new FileInputStream(filenameIn);
    FileChannel inChannel = in.getChannel();

    FileOutputStream out = new FileOutputStream(filenameOut);
    FileChannel outChannel = out.getChannel();

    long size = inChannel.size();
    //outChannel.position(size-2);
    //outChannel.write(ByteBuffer.allocate(1));
    //outChannel.position(0);

    if (debug) System.out.println("read " + filenameIn + " len = " + size+" out starts at="+outChannel.position());

    long start = System.currentTimeMillis();
    long done = 0;
    while (done < size) {
      long need = Math.min(kbchunks * 1000, size-done);
      done += inChannel.transferTo(done, need, outChannel);
    }

    outChannel.close();
    inChannel.close();

    double took = .001 * (System.currentTimeMillis() - start);
    if (debug) System.out.println(" write file= " + filenameOut + " len = " + size);

    double rate = size / took / (1000 * 1000);
    System.out.println(" copyFileNIO("+kbchunks+" kb chunk) took = " + took + " sec; rate = " + rate + "Mb/sec");
  }

  static public void testNIO(String filenameIn, String filenameOut, long kbchunks) throws IOException {

    ByteBuffer bb = ByteBuffer.allocate(8*1000);
    FloatBuffer fb = bb.asFloatBuffer();
  }

  static public void readFile(String filenameIn, int bufferSize) throws IOException {
    long lenIn = new File(filenameIn).length();
    if (debug) System.out.println("read " + filenameIn + " len = " + lenIn);

    InputStream in = new FileInputStream(filenameIn);

    long total = 0;
    long start = System.currentTimeMillis();
    byte[] buffer = new byte[bufferSize];
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1) break;
      total += buffer[0];
    }
    double took = .001 * (System.currentTimeMillis() - start);

    in.close();

    double rate = lenIn / took / (1000 * 1000);
    System.out.println(" copy (" + bufferSize + ") took = " + took + " sec; rate = " + rate + "Mb/sec total=" + total);
  }


  static public void copyURL(String url, String filenameOut, int bufferSize) throws IOException {

    File outFile = new File(filenameOut);

    long start = System.currentTimeMillis();
    String ok = IO.readURLtoFileWithExceptions(url, outFile, bufferSize);
    double took = .001 * (System.currentTimeMillis() - start);

    double len = (double) outFile.length() / (1000 * 1000);

    double rate = len / took ;
    System.out.println(" copyURL (" + url + ") took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + "Mb/sec ok="+ok);
  }

  static public void copyURL2null(String url, String filenameOut, int bufferSize) throws IOException {

    long start = System.currentTimeMillis();
    long count = IO.copyUrlB(url, null, bufferSize);
    double len = (double) count / (1000 * 1000);
    double took = .001 * (System.currentTimeMillis() - start);

    double rate = len / took ;
    System.out.println(" copyURL2null (" + url + ") took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");
  }


  static public void readURL(String urlString, int bufferSize) throws IOException {

    System.out.println("start="+new Date());
    long start = System.currentTimeMillis();


    URL url;
    java.io.InputStream is = null;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      throw new IOException("** MalformedURLException on URL <" + urlString + ">\n" + e.getMessage() + "\n");
    }

    try {
      java.net.HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        // check response code is good
        int responseCode = httpConnection.getResponseCode();
        System.out.println(" response code= "+responseCode);
        if (responseCode / 100 != 2)
          throw new IOException("** Cant open URL <" + urlString + ">\n Response code = " + responseCode
              + "\n" + httpConnection.getResponseMessage() + "\n");

      // read it
      is = httpConnection.getInputStream();

      int nreads=0;
      long totalB = 0;
      long total = 0;
      byte[] buffer = new byte[bufferSize];
      while (true) {
        int bytesRead = is.read(buffer);
        if (bytesRead == -1) break;
        totalB += bytesRead;
        total += buffer[0]; // prevent compiler optimization
        nreads++;
      }
      double took = .001 * (System.currentTimeMillis() - start);
      double rate = totalB / took / (1000 * 1000);
      double avg = totalB / nreads;
      System.out.println(" readURL (" + bufferSize + ") took = " + took + " sec; rate = " + rate + "Mb/sec avg read= "+avg);
      System.out.println("   dummy="+total);
      System.out.println(" end="+new Date());

    } catch (java.net.ConnectException e) {
      throw new IOException("** ConnectException on URL: <" + urlString + ">\n" +
          e.getMessage() + "\nServer probably not running");

    } finally {
      if (is != null) is.close();
    }


  }


}
