package IO;

import java.io.*;
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
  static boolean debug = false;

  static public void main(String args[]) throws IOException {
    copyURL("http://localhost:8080/thredds/netcdf/dd/NAM_CONUS_80km_20070501_1200.nc?v,u,RH", "C:/temp/netcdf.nc", 10000);

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

    /* copyFile("C:/data/hdf5/IASI.h5", "C:/temp/copyOut", false);
    copyFile("C:/data/hdf5/IASI.h5", "C:/temp/copyOut2", true);
    copyFile("C:/data/hdf5/IASI.h5", "C:/temp/copyOut3", false);
    copyFile("C:/data/hdf5/IASI.h5", "C:/temp/copyOut4", true);

    /*
     copy (no buffer) took = 90.516 sec; rate = 21.755541804763798Mb/sec
     copy (buffer) took = 84.703 sec; rate = 23.248581773963142Mb/sec
     copy (no buffer) took = 87.98400000000001 sec; rate = 22.38162190852882Mb/sec
     copy (buffer) took = 86.5 sec; rate = 22.765602566473987Mb/sec */
  }

  static public void copyFile(String filenameIn, String filenameOut, boolean buffer) throws IOException {
    long lenIn = new File(filenameIn).length();
    if (debug) System.out.println("read " + filenameIn + " len = " + lenIn);

    InputStream in = new FileInputStream(filenameIn);
    if (buffer) new BufferedInputStream(in, 10000);

    OutputStream out = new FileOutputStream(filenameOut);
    if (buffer) out = new BufferedOutputStream(out, 1000);

    long start = System.currentTimeMillis();
    thredds.util.IO.copyB(in, out, 10000);
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
    String ok = thredds.util.IO.readURLtoFileWithExceptions(url, outFile, bufferSize);
    double took = .001 * (System.currentTimeMillis() - start);

    double len = (double) outFile.length() / (1000 * 1000);

    double rate = len / took ;
    System.out.println(" copyURL (" + bufferSize + ") took = " + took + " sec; len= "+len+" Mbytes; rate = " + rate + "Mb/sec ok="+ok);
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
