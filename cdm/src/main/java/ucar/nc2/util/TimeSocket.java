/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.util;

import ucar.unidata.util.Format;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NCdumpW;
import ucar.nc2.iosp.netcdf3.N3streamWriter;
import ucar.nc2.iosp.netcdf3.N3channelWriter;
import ucar.nc2.iosp.netcdf3.N3outputStreamWriter;
import ucar.ma2.Section;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.channels.*;
import java.util.*;

/**
 * @author caron
 * @since Feb 1, 2008
 */
public class TimeSocket {

  static double sendFake(int len) throws IOException {
    Socket s = new Socket(host, port);
    OutputStream out = new BufferedOutputStream( s.getOutputStream(), 8000);
    byte[] b = new byte[1000];
    for (int i=0; i < len * 1000; i++)
      out.write(b);
    out.close();
    return (double) len;
  }

  static double copyFile(File f) throws IOException {
    Socket s = new Socket(host, port);
    OutputStream out = new BufferedOutputStream( s.getOutputStream(), 8000);
    ucar.nc2.util.IO.copyFileB(f, out, 8000);
    out.close();
    return f.length() / (1000 * 1000);
  }

  static double copyChannel(File f) throws IOException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);
    FileChannel fc = new FileInputStream(f).getChannel();
    long len = f.length();
    long done = 0;
    while (done < len) {
      done += fc.transferTo(done, len-done, sc);
      //System.out.println(" done= "+done);
    }
    sc.close();
    return ((double) done) / (1000 * 1000);
  }

  static double copyChannelFromRaf(File f) throws IOException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);
    FileChannel fc = new RandomAccessFile(f,"r").getChannel();
    long len = f.length();
    long done = 0;
    while (done < len) {
      done += fc.transferTo(done, len-done, sc);
      System.out.println(" done= "+done);
    }
    sc.close();
    return ((double) done) / (1000 * 1000);
  }

  static double copyChannelFromNetcdf(File f) throws IOException, InvalidRangeException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);

    int done = 0;
    NetcdfFile ncfile = NetcdfFile.open(f.getPath());
    String want = "T,u,v,RH,Z,omega,absvor,T_fhg,u_fhg,v_fhg,RH_fhg";
    StringTokenizer stoke = new StringTokenizer(want, ",");
    while (stoke.hasMoreTokens()) {
      NCdumpW.CEresult cer = NCdumpW.parseVariableSection(ncfile, stoke.nextToken());
      done += ncfile.readData(cer.v, new Section(cer.ranges), sc);
    }
    sc.close();
    return ((double) done) / (1000 * 1000);
  }

  static double streamOutputWriter(File f) throws IOException, InvalidRangeException {
    Socket s = new Socket(host, port);
    DataOutputStream stream = new DataOutputStream(new BufferedOutputStream( s.getOutputStream(), 8000));

    NetcdfFile ncfile = NetcdfFile.open(f.getPath());
    N3outputStreamWriter writer = new N3outputStreamWriter(ncfile);

    writer.writeHeader(stream);
    writer.writeDataAll(stream);
    stream.close();

    stream.close();

    return ((double) f.length()) / (1000 * 1000);
  }

  static double streamChannelWriter(File f) throws IOException, InvalidRangeException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);

    NetcdfFile ncfile = NetcdfFile.open(f.getPath());
    
    N3channelWriter.writeToChannel(ncfile, sc);
    sc.close();

    return ((double) f.length()) / (1000 * 1000);
  }

  static private String dir = "/data/ldm/pub/decoded/netcdf/grid/NCEP/GFS/Global_5x2p5deg/";
  static private Iterator<File> files;
  static private File getFile() {
    if (null == files) {
      File[] fila = new File(dir).listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".nc");
        }
      });
      files = Arrays.asList(fila).iterator();
    }
    return files.next();
  }

  static int port = 8080;
  static InetAddress host;
  static String hostname = "BERT.unidata.ucar.edu";
  public static void main(String[] args) throws IOException, InvalidRangeException {
    host = InetAddress.getByName(hostname);
    System.out.println("host="+host);

    long start = System.currentTimeMillis();
    double len = sendFake( 100);
    double took = .001 * (System.currentTimeMillis() - start);
    double rate = len / took ;
    System.out.println("sendFake took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");

    start = System.currentTimeMillis();
    len = copyFile( getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = len / took ;
    System.out.println("copyFile took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");

    start = System.currentTimeMillis();
    len = copyChannel( getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = len / took ;
    System.out.println("copyChannel took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");

    start = System.currentTimeMillis();
    len = copyChannelFromRaf( getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = len / took ;
    System.out.println("copyChannelFromRaf took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");

    start = System.currentTimeMillis();
    len = streamOutputWriter( getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = len / took ;
    System.out.println("streamOutputWriter took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");

    start = System.currentTimeMillis();
    len = streamChannelWriter( getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = len / took ;
    System.out.println("streamChannelWriter took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");

    start = System.currentTimeMillis();
    len = copyChannelFromNetcdf( getFile());
    took = .001 * (System.currentTimeMillis() - start);
    rate = len / took ;
    System.out.println("copyChannelFromNetcdf took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");

    //double len = copyFile();
    //double len = copyChannel();
    //double len = copyChannelFromRaf();
    //double len = copyChannelFromNetcdf();
    // double len = streamOutputWriter();
    //double len = streamChannelWriter();
    //double took = .001 * (System.currentTimeMillis() - start);
    //double rate = len / took ;
    //System.out.println(" took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");
  }

}
