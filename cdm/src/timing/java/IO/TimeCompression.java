package IO;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;
import java.util.Random;

/*  Conclusions.

    Deflate vs GZIP, Random floats
    Buffering gives factor of 7-8, no diff between buffer size 1000 and 10000
        No performance diff between deflate and gzip on random floats
        only 12% compression

        deflateRandom took = 5.25 sec; compress = 1.120589430040201
        deflateRandom took = 0.75 sec; compress = 1.120589430040201
        gzipRandom took = 6.406 sec; compress = 1.1205793843648921
        gzipRandom took = 0.766 sec; compress = 1.1206478240941524

    Buffering gives factor of 15, no diff bewteen buffer size 1000 and 10000
       No performance diff between inflate and gunzip on random floats

       inflate took = 4.828 sec
       inflate took = 0.328 sec
       unzip took = 4.781 sec
       unzip took = 0.329 sec

    Deflate vs GZIP, Real data files

    No performance diff between deflate and gzip on real files;
       4.4 X compression on real data file, which are 99% floats

     deflate took = 6.532 sec; rate = 7.987679118187385Mb/sec; compress = 4.4094069227210015
     gzip took = 6.656000000000001 sec; rate = 7.838870192307692Mb/sec; compress = 4.409402451003301

    inflate took = 1.203 sec; rate = 9.836055694098087Mb/sec; compress = 4.4094069227210015
    gunzip took = 1.578 sec; rate = 7.4985975918884655Mb/sec; compress = 4.409402451003301

    inflate took = 1.875 sec; rate = 6.310813333333333Mb/sec; compress = 4.4094069227210015
    gunzip took = 1.171 sec; rate = 10.10485653287788Mb/sec; compress = 4.409402451003301

    inflate took = 1.3900000000000001 sec; rate = 8.512787769784172Mb/sec; compress = 4.4094069227210015
    gunzip took = 1.453 sec; rate = 8.143693737095663Mb/sec; compress = 4.409402451003301
    // */

public class TimeCompression {
  static boolean debug = false;

  static public void main( String[] args) throws IOException {
    testCompressRandom();
    testCompressFile("C:/data/NAM_CONUS_80km_20070501_1200.nc");
    testCompressFile("C:/data/NAM_CONUS_80km_20070501_1200.nc");
  }

  static public void testCompressFile(String filename) throws IOException {
    compressFile(filename, "C:/temp/tempFile.compress1", true);
    compressFile(filename, "C:/temp/tempFile.gzip", false);

    uncompressFile("C:/temp/tempFile.compress1", "C:/temp/tempFile.uncompress", true);
    uncompressFile("C:/temp/tempFile.gzip", "C:/temp/tempFile.unzip", false);
  }

  // test Deflater on real files
  static public void compressFile( String filenameIn, String filenameOut, boolean inflate) throws IOException {
    long lenIn = new File(filenameIn).length();
    if (debug) System.out.println("read "+filenameIn+ " len = "+lenIn);

    FileInputStream fin = new FileInputStream(filenameIn);
    InputStream in = new BufferedInputStream(fin, 1000);

    FileOutputStream fout = new FileOutputStream(filenameOut);
    OutputStream out = (inflate) ? new DeflaterOutputStream(fout) : new GZIPOutputStream(fout);
    out = new BufferedOutputStream(out, 1000);

    long start = System.currentTimeMillis();
    thredds.util.IO.copyB( in, out, 10000);
    out.flush();
    double took = .001 * (System.currentTimeMillis() - start);

    out.close();
    fin.close();

    long lenOut = new File(filenameOut).length();
    if (debug) System.out.println(" write compressed file= "+filenameOut+ " len = "+lenOut);

    double rate = lenIn/took/(1000*1000);
    double ratio = (double) lenIn/lenOut;
    String name = inflate ? "deflate" : "gzip";
    System.out.println(" "+name + " took = "+took+" sec; rate = "+ rate+"Mb/sec; compress = "+ratio);
  }

  static public void uncompressFile( String filenameIn, String filenameOut, boolean inflate) throws IOException {
    long lenIn = new File(filenameIn).length();
    if (debug) System.out.println("read compressed file= "+filenameIn+ " len = "+lenIn);

    FileInputStream fin = new FileInputStream(filenameIn);
    InputStream in = (inflate) ? new InflaterInputStream(fin) : new GZIPInputStream(fin);
    in = new BufferedInputStream(in, 1000);

    FileOutputStream fout = new FileOutputStream(filenameOut);
    OutputStream out = new BufferedOutputStream(fout, 1000);

    long start = System.currentTimeMillis();
    thredds.util.IO.copyB( in, out, 10000);
    out.flush();
    double took = .001 * (System.currentTimeMillis() - start);

    out.close();
    fin.close();

    long lenOut = new File(filenameOut).length();
    if (debug) System.out.println(" write uncompressed file= "+filenameOut+ " len = "+lenOut);
    double rate = lenIn/took/(1000*1000);
    double ratio = (double) lenOut/lenIn;

    String name = inflate ? "inflate" : "gunzip";
    System.out.println(name+" took = "+took+" sec; rate = "+ rate+"Mb/sec; compress = "+ratio);
  }

  /////////////////////////////////////////////////////////////
  // random floats
  static public void testCompressRandom() throws IOException {
    deflateRandom("C:/temp/tempFile.compress1", false);
    deflateRandom("C:/temp/tempFile.compress2", true);

    gzipRandom("C:/temp/tempFile.compress3", false);
    gzipRandom("C:/temp/tempFile.compress4", true);

    inflateRandom("C:/temp/tempFile.compress1", false);
    inflateRandom("C:/temp/tempFile.compress2", true);

    unzipRandom("C:/temp/tempFile.compress3", false);
    unzipRandom("C:/temp/tempFile.compress4", true);
  }

  // test DeflaterOutputStream - write 1M random floats
  static public void deflateRandom( String filenameOut, boolean buffer) throws IOException {
    FileOutputStream fout = new FileOutputStream(filenameOut);
    OutputStream out = new DeflaterOutputStream(fout);
    if (buffer)
      out = new BufferedOutputStream(out, 1000); // 3X performance by having this !!
    DataOutputStream dout = new DataOutputStream(out);

    Random r = new Random();
    long start = System.currentTimeMillis();
    for (int i=0; i<(1000*1000);i++) {
      dout.writeFloat(r.nextFloat());
    }
    dout.flush();
    fout.close();
    double took = .001 * (System.currentTimeMillis() - start);
    File f = new File(filenameOut);
    long len = f.length();
    double ratio = 4 * 1000.0 * 1000.0 / len;
    System.out.println(" deflateRandom took = "+took+" sec; compress = "+ratio);
  }

  // test GZIPOutputStream - write 1M random floats
  static public void gzipRandom(String filenameOut, boolean buffer) throws IOException {
    FileOutputStream fout = new FileOutputStream(filenameOut);
    OutputStream out = new GZIPOutputStream(fout);
    if (buffer)
      out = new BufferedOutputStream(out, 1000); // 3X performance by having this !!
    DataOutputStream dout = new DataOutputStream(out);

    Random r = new Random();
    long start = System.currentTimeMillis();
    for (int i=0; i<(1000*1000);i++) {
      dout.writeFloat(r.nextFloat());
    }
    fout.flush();
    fout.close();
    double took = .001 * (System.currentTimeMillis() - start);
    File f = new File(filenameOut);
    long len = f.length();
    double ratio = 4 * 1000.0 * 1000.0 / len;
    System.out.println(" gzipRandom took = "+took+" sec; compress = "+ratio);
  }

  // test InflaterInputStream
  static public void inflateRandom( String filename, boolean buffer) throws IOException {
    FileInputStream fin = new FileInputStream(filename);
    InputStream in = new InflaterInputStream(fin);
    if (buffer)
      in = new BufferedInputStream(in, 1000);
    DataInputStream din = new DataInputStream(in);

    long start = System.currentTimeMillis();
    float total = 0.0f;
    try {
      while (true) {
        total += din.readFloat();
      }
    } catch (EOFException e) {
      System.out.println("total="+total);
    }
    fin.close();
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(" inflate took = "+took+" sec");
  }

  // test InflaterInputStream
  static public void unzipRandom( String filename, boolean buffer) throws IOException {
    FileInputStream fin = new FileInputStream(filename);
    InputStream in = new GZIPInputStream(fin);
    if (buffer)
      in = new BufferedInputStream(in, 1000);
    DataInputStream din = new DataInputStream(in);

    long start = System.currentTimeMillis();
    float total = 0.0f;
    try {
      while (true) {
        total += din.readFloat();
      }
    } catch (EOFException e) {
      System.out.println("total="+total);
    }
    fin.close();
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(" unzip took = "+took+" sec");
  }

   // test DeflaterOutputStream
  static public void main2( String[] args) throws IOException {
    String filenameIn = "C:/temp/tempFile2";
    String filenameOut = "C:/temp/tempFile2.compress2";

    FileInputStream fin = new FileInputStream(filenameIn);
    FileOutputStream fout = new FileOutputStream(filenameOut);
    BufferedOutputStream bout = new BufferedOutputStream(fout, 10 * 1000);
    DeflaterOutputStream out = new DeflaterOutputStream(fout);

    long start = System.currentTimeMillis();
    thredds.util.IO.copyB(fin, out, 10 * 1000);
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(" that took = "+took+"sec");

    fin.close();
    fout.close();
  }

}
