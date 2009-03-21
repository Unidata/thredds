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

import ucar.unidata.util.Format;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.iosp.netcdf3.N3channelWriter;
import ucar.nc2.iosp.netcdf3.N3outputStreamWriter;
import ucar.ma2.InvalidRangeException;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;

/**
 * @author caron
 * @since Feb 1, 2008
 */
public class TimeSocket {
  int bufferSize = 8000;

  Average sendFake, copyFake, copyFile, copyZip, copyChannel, copyChannelFromRaf, streamOutputWriter, streamChannelWriter;

  TimeSocket() {
    sendFake = new Average();
    copyFake = new Average();
    copyFile = new Average();
    copyZip = new Average();
    copyChannel = new Average();
    copyChannelFromRaf = new Average();
    streamOutputWriter = new Average();
    streamChannelWriter = new Average();
  }

  // len in Mbytes
  double sendFake(int len) throws IOException {
    Socket s = new Socket(host, port);
    OutputStream out = new BufferedOutputStream(s.getOutputStream(), bufferSize);
    byte[] b = new byte[bufferSize];
    for (int i = 0; i < len * 50; i++)
      out.write(b);
    out.close();
    return (double) len;
  }

  double copyFake(File f) throws IOException {
    InputStream in = new FileInputStream(f);
    ucar.nc2.util.IO.copy2null( in, -1);
    in.close();
    return f.length() / (1000 * 1000);
  }

  double copyFile(File f) throws IOException {
    Socket s = new Socket(host, port);
    OutputStream out = new BufferedOutputStream(s.getOutputStream(), bufferSize);
    ucar.nc2.util.IO.copyFileB(f, out, bufferSize);
    out.close();
    return f.length() / (1000 * 1000);
  }

  double copyFileZipped(File f) throws IOException {
    Socket s = new Socket(host, port);
    OutputStream out = new BufferedOutputStream( new DeflaterOutputStream( s.getOutputStream()), bufferSize);
    ucar.nc2.util.IO.copyFileB(f, out, bufferSize);
    out.close();
    return f.length() / (1000 * 1000);
  }

  double copyChannel(File f) throws IOException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);
    FileChannel fc = new FileInputStream(f).getChannel();
    long len = f.length();
    long done = 0;
    while (done < len) {
      done += fc.transferTo(done, len - done, sc);
    }
    sc.close();
    return ((double) done) / (1000 * 1000);
  }

  double copyChannelFromRaf(File f) throws IOException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);
    FileChannel fc = new RandomAccessFile(f, "r").getChannel();
    long len = f.length();
    long done = 0;
    while (done < len) {
      done += fc.transferTo(done, len - done, sc);
    }
    sc.close();
    return ((double) done) / (1000 * 1000);
  }

  double streamOutputWriter(File f) throws IOException, InvalidRangeException {
    Socket s = new Socket(host, port);
    DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(s.getOutputStream(), bufferSize));

    NetcdfFile ncfile = NetcdfFile.open(f.getPath());
    N3outputStreamWriter writer = new N3outputStreamWriter(ncfile);

    writer.writeHeader(stream, -1);
    writer.writeDataAll(stream);
    stream.close();

    stream.close();

    return ((double) f.length()) / (1000 * 1000);
  }

  double streamChannelWriter(File f) throws IOException, InvalidRangeException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);

    NetcdfFile ncfile = NetcdfFile.open(f.getPath());

    N3channelWriter.writeToChannel(ncfile, sc);
    sc.close();

    return ((double) f.length()) / (1000 * 1000);
  }

  double copyChannelFromNetcdf(File f) throws IOException, InvalidRangeException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);

    int done = 0;
    NetcdfFile ncfile = NetcdfFile.open(f.getPath());
    String want = "T,u,v,RH,Z,omega,absvor,T_fhg,u_fhg,v_fhg,RH_fhg";
    StringTokenizer stoke = new StringTokenizer(want, ",");
    while (stoke.hasMoreTokens()) {
      ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ncfile, stoke.nextToken());
      done += cer.v.readToByteChannel(cer.section, sc);
    }
    sc.close();
    return ((double) done) / (1000 * 1000);
  }


  static private String dir = "/data/ldm/pub/decoded/netcdf/grid/NCEP/";
  private FileTranverser ft;

  private File getFile() {
    if (null == ft)
      ft = new FileTranverser(new File(dir));

    File result =  ft.next();
    if (result == null) {
      System.out.println("Done");
      System.exit(0);
    }
    if (!result.exists())
      return getFile();

    System.out.println("Using file="+result.getPath()+" size= "+result.length());
    return result;
  }

  private class FileTranverser {
    private List<File> files;
    private int next = 0;
    private boolean useFiles = true;
    private FileTranverser child;

    FileTranverser(File dir) {
      File[] fila = dir.listFiles();
      files = Arrays.asList(fila);
      next = 0;
    }

    File next() {
      if (child != null) {
        File result = child.next();
        if (result != null) return result;
        child = null;
      }

      if (useFiles) {
        File result = nextFile();
        if (result != null) return result;
        useFiles = false;
        next = 0;
      }

      while (next < files.size()) {
        File f = files.get(next++);
        if (f.isDirectory()) {
          child = new FileTranverser(f);
          File result = child.next();
          if (result != null) return result;
        }
      }
      return null;
    }

    File nextFile() {
      while (next < files.size()) {
        File f = files.get(next++);
        if (f.getName().endsWith(".nc")) return f;
      }
      return null;
    }

  }

  public void run() throws IOException, InvalidRangeException {

    long start = System.currentTimeMillis();
    double len = sendFake(100);
    double took = .001 * (System.currentTimeMillis() - start);
    double rate = took / len;
    sendFake.add(rate);
    System.out.println("sendFake took = " + took + " sec; len= " + len + " Mbytes; rate = " + Format.d(rate, 3) + " sec/Mb");

    start = System.currentTimeMillis();
    len = copyFake(getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = took / len;
    copyFake.add(rate);
    System.out.println("copyFake took = " + took + " sec; len= " + len + " Mbytes; rate = " + Format.d(rate, 3) + " sec/Mb");

    start = System.currentTimeMillis();
    len = copyFile(getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = took / len;
    copyFile.add(rate);
    System.out.println("BufferedInputStream->BufferedOutputStream took = " + took + " sec; len= " + len + " Mbytes; rate = " + Format.d(rate, 3) + " sec/Mb");

    start = System.currentTimeMillis();
    len = copyFileZipped(getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = took / len;
    copyZip.add(rate);
    System.out.println("copyZip took = " + took + " sec; len= " + len + " Mbytes; rate = " + Format.d(rate, 3) + " sec/Mb");

    start = System.currentTimeMillis();
    len = copyChannel(getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = took / len;
    copyChannel.add(rate);
    System.out.println("FileChannel.transferTo(socketChannel) took = " + took + " sec; len= " + len + " Mbytes; rate = " + Format.d(rate, 3) + " sec/Mb");

    /* start = System.currentTimeMillis();
    len = copyChannelFromRaf(getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = took / len;
    copyChannelFromRaf.add(rate);
    System.out.println("RAF.fileChannel.transferTo(socketChannel) took = " + took + " sec; len= " + len + " Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");
    */

    start = System.currentTimeMillis();
    len = streamOutputWriter(getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = took / len;
    streamOutputWriter.add(rate);
    System.out.println("N3outputStreamWriter(socket.outputStream) took = " + took + " sec; len= " + len + " Mbytes; rate = " + Format.d(rate, 3) + " sec/Mb");
    
    start = System.currentTimeMillis();
    len = streamChannelWriter(getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = took / len;
    streamChannelWriter.add(rate);
    System.out.println("N3channelWriter(socketChannel) took = " + took + " sec; len= " + len + " Mbytes; rate = " + Format.d(rate, 3) + " sec/Mb");

    System.out.println("Run "+count);
    System.out.println("  sendFake=           " + sendFake);
    System.out.println("  copyFake=           " + copyFake);
    System.out.println("  copyFile=           " + copyFile);
    System.out.println("  copyZip=            " + copyZip);
    System.out.println("  copyChannel=        " + copyChannel);
    //System.out.println("  copyChannelFromRaf= " + copyChannelFromRaf);
    System.out.println("  streamOutputWriter= " + streamOutputWriter);
    System.out.println("  streamChannelWriter=" + streamChannelWriter);
    count++;

    System.out.println();
  }
  int count = 0;
  
  static int port = 8080;
  static InetAddress host;
  static String hostname = "BERT.unidata.ucar.edu";

  public static void main(String[] args) throws IOException, InvalidRangeException, InterruptedException {

    if ((args.length == 1) && args[0].equals("help")) {
      System.out.println("ucar.nc2.TimeSocket [skip]");
      System.out.println("ucar.nc2.TimeSocket <directory> <skip>");
      System.out.println("ucar.nc2.TimeSocket <directory> <skip> <wait secs>");
      System.exit(0);
    }

    TimeSocket test = new TimeSocket();
    host = InetAddress.getByName(hostname);
    System.out.println("host=" + host);

    if ((args.length == 2) && args[1].equals("test")) {
      dir = args[0];
      File f;
      while (null != (f = test.getFile()))
        System.out.println("  " + f.getPath());

      System.exit(0);
    }

    int wait_secs = 10;
    if (args.length > 2) {
      dir = args[0];
      int skipFiles = Integer.parseInt(args[1]);
      for (int i = 0; i < skipFiles; i++) test.getFile();
      wait_secs = Integer.parseInt(args[2]);

    } else if (args.length > 1) {
        dir = args[0];
        int skipFiles = Integer.parseInt(args[1]);
        for (int i = 0; i < skipFiles; i++) test.getFile();

    } else if (args.length > 0) {
      int skipFiles = Integer.parseInt(args[0]);
      for (int i = 0; i < skipFiles; i++) test.getFile();
    }

    while (true) {
      test.run();
      Thread.currentThread().sleep(1000 * wait_secs);
    }
  }

  private class Average {
    int count = -3; // throw away the first 3;

    /**
     * The set of values stored as doubles.  Autoboxed.
     */

    private ArrayList values = new ArrayList();

    /**
     * Add a new value to the series.  Changes the values returned by
     * mean() and stddev().
     *
     * @param value the new value to add to the series.
     */
    public void add(double value) {
      count++;
      if (count < 1) return;

      if (!Double.isInfinite(value) && !Double.isNaN(value) && (value > 0))
        values.add(new Double(value));
    }

    /**
     * Calculate and return the mean of the series of numbers.
     * Throws an exception if this is called before the add() method.
     *
     * @return the mean of all the numbers added to the series.
     * @throws IllegalStateException if no values have been added yet.
     *                               Otherwise we could cause a NullPointerException.
     */

    public double mean() {
      int elements = values.size();
      if (elements == 0) return 0.0;
      double sum = 0;
      for (int i = 0; i < values.size(); i++) {
        Double valo = (Double) values.get(i);
        sum += valo.doubleValue();
      }
      return sum / elements;
    }

    /**
     * Calculate and return the standard deviation of the series of
     * numbers.  See Stats 101 for more information...
     * Throws an exception if this is called before the add() method.
     *
     * @return the standard deviation of numbers added to the series.
     * @throws IllegalStateException if no values have been added yet.
     *                               Otherwise we could cause a NullPointerException.
     */

    public double stddev() {
      double mean = mean();
      double stddevtotal = 0;
      for (int i = 0; i < values.size(); i++) {
        Double valo = (Double) values.get(i);
        double dev = valo.doubleValue() - mean;
        stddevtotal += dev * dev;
      }

      return Math.sqrt(stddevtotal / values.size());
    }

    public String toString() {
      return "mean="+mean()+" stdev="+stddev();
    }
  }

}
