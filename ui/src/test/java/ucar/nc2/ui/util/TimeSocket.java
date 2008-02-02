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
package ucar.nc2.ui.util;

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
import java.util.StringTokenizer;

/**
 * @author caron
 * @since Feb 1, 2008
 */
public class TimeSocket {
  static int port = 4444;
  static InetAddress host;

  static double sendFake(int len) throws IOException {
    Socket s = new Socket(host, port);
    OutputStream out = new BufferedOutputStream( s.getOutputStream(), 8000);
    byte[] b = new byte[1000];
    for (int i=0; i < len * 1000; i++)
      out.write(b);
    out.close();
    return (double) len;
  }

  static double copyFile() throws IOException {
    Socket s = new Socket(host, port);
    OutputStream out = new BufferedOutputStream( s.getOutputStream(), 8000);
    File f = new File("C:/data/hdf5/ssec-h5/I3A_CCD_13FEB2007_0501_L1B_STD.h5");
    ucar.nc2.util.IO.copyFileB(f, out, 8000);
    out.close();
    return f.length() / (1000 * 1000);
  }

  static double copyChannel() throws IOException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);
    File f = new File("C:/install/oxygen.exe");
    FileChannel fc = new FileInputStream(f).getChannel();
    long len = f.length();
    long done = 0;
    while (done < len) {
      done += fc.transferTo(done, len-done, sc);
      System.out.println(" done= "+done);
    }
    sc.close();
    return ((double) done) / (1000 * 1000);
  }

  static double copyChannelFromRaf() throws IOException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);
    File f = new File("C:/install/jdk-6-windows-i586.exe");
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

  static double copyChannelFromNetcdf() throws IOException, InvalidRangeException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);
    File f = new File("C:/install/jdk-6-windows-i586.exe");

    int done = 0;
    NetcdfFile ncfile = NetcdfFile.open("C:/data/nssl/mosaic2d_nc/tile1/20070803-2300.netcdf");
    String want = "cref,shi,posh,hsr,hsrh,pcp_type";
    StringTokenizer stoke = new StringTokenizer(want, ",");
    while (stoke.hasMoreTokens()) {
      NCdumpW.CEresult cer = NCdumpW.parseVariableSection(ncfile, stoke.nextToken());
      done += ncfile.readData(cer.v, new Section(cer.ranges), sc);
    }
    sc.close();
    return ((double) done) / (1000 * 1000);
  }

  static double streamOutputWriter() throws IOException, InvalidRangeException {
    Socket s = new Socket(host, port);
    DataOutputStream stream = new DataOutputStream(new BufferedOutputStream( s.getOutputStream(), 8000));

    String filename = "C:/data/metars/Surface_METAR_20070326_0000.nc";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    N3outputStreamWriter writer = new N3outputStreamWriter(ncfile);

    writer.writeHeader(stream);
    writer.writeDataAll(stream);
    stream.close();

    stream.close();

    File f = new File(filename);

    return ((double) f.length()) / (1000 * 1000);
  }

  static double streamChannelWriter() throws IOException, InvalidRangeException {
    InetSocketAddress sadd = new InetSocketAddress(host, port);
    SocketChannel sc = java.nio.channels.SocketChannel.open(sadd);

    String filename = "C:/data/metars/Surface_METAR_20070326_0000.nc";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    
    N3channelWriter.writeToChannel(ncfile, sc);
    sc.close();

    File f = new File(filename);
    return ((double) f.length()) / (1000 * 1000);
  }

  public static void main(String[] args) throws IOException, InvalidRangeException {
    host = InetAddress.getByName(null);

    long start = System.currentTimeMillis();
    //double len = sendFake( 100);
    //double len = copyFile();
    //double len = copyChannel();
    //double len = copyChannelFromRaf();
    //double len = copyChannelFromNetcdf();
    // double len = streamOutputWriter();
    double len = streamChannelWriter();
    double took = .001 * (System.currentTimeMillis() - start);
    double rate = len / took ;
    System.out.println(" took = " + took + " sec; len= "+len+" Mbytes; rate = " + Format.d(rate, 3) + " Mb/sec");
  }

}
