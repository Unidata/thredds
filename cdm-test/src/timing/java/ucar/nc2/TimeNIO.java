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
package ucar.nc2;

import ucar.nc2.util.Stat;
import ucar.nc2.iosp.netcdf3.SPFactory;

import java.io.*;
import java.util.*;

public class TimeNIO {

/**
 * results (5/1/03 on IBM thinkpad / XP
 *
 *   C optimized :
 *    first: 29.8, 20.9
 *    avg5: 2.18, 1.85
 *
 *   Java 1.4.2 NIO
 *     first: 3.8, 2.7
 *     avg5: 2.0, 1.9
 *
 *   Java 1.4.2 NC2
 *     first: 49.4
 *     avg5: 10.2
 *
 * first time much faster (memory mapping?)
 * subsequent times the same.
 */

  static void testSP(String spName, String fname, Stat stat) throws IOException {
    String name = spName;
    long startTime, endTime, size = 0;

    // read
      startTime = System.currentTimeMillis();
      try {
        SPFactory.setServiceProvider(spName);
        NetcdfFile ncFile = NetcdfFile.open( fname);

        Iterator iter = ncFile.getVariables().iterator();
        while (iter.hasNext()) {
          Variable v = (Variable) iter.next();
          v.read();
          size += v.getSize();
        }

        ncFile.close();
      } catch (Exception ioe) {
        ioe.printStackTrace();
      }

      endTime = System.currentTimeMillis();
      long diff = endTime - startTime;
      if (stat != null)
        stat.avg(name+fname, diff);
      System.out.println("read "+size+" from "+fname+ " took "+diff+ " msecs");
  }

  public static void doit(Stat s)  throws IOException {
    testSP( "ucar.nc2.SPVer1", "C:/data/conventions/mm5/n040.nc", s);
    testSP( "ucar.nc2.SPNioMMap", "C:/data/conventions/mm5/copy_n040.nc", s);
    testSP( "ucar.nc2.SPNio", "C:/data/conventions/mm5/copy_n040.nc", s);
    testSP( "ucar.nc2.SPVer1", "C:/data/conventions/cf/cf1.nc", s);
    testSP( "ucar.nc2.SPNio", "C:/data/conventions/cf/copy_cf1.nc", s);
    testSP( "ucar.nc2.SPNioMMap", "C:/data/conventions/cf/copy_cf1.nc", s);
 }

  public static void testSP(String filename, Stat s) throws IOException {
    testSP( "ucar.nc2.SPVer1", filename, s);
    testSP( "ucar.nc2.SPNioMMap", filename, s);
    testSP( "ucar.nc2.SPNio", filename, s);
 }

   public static void testReadAll(Stat s, int n) throws IOException {
    long totalRead = 0;
    for (int i=0; i<n; i++) {
      try {
        File dir = new File("timing/data/");
        String[] flist = dir.list();
        for (int j=0; j<flist.length; j++)
          testSP( "ucar.nc2.SPNioCD", "timing/data/"+flist[j], s);

      } catch (IOException ioe) {
        ioe.printStackTrace();
        System.out.println("CD total before error= " + totalRead);
        break;
      }
      if (s != null) s.print();
      System.out.println("CD total done = " + totalRead);
    }
  }

  // this reads a small amount of data that is spread out through a large file
  static long readSpreadData(String fname, Stat stat, int n) throws IOException {
    String name = "readSpreadData";
    long startTime, endTime, nbytes = 0;

    // read
      //System.out.println("Open "+fname);
      startTime = System.currentTimeMillis();
      try {
        NetcdfFile ncFile = NetcdfFile.open( fname);

        Variable v = ncFile.findVariable("CH4");
        for (int i=0;i<n;i++) {
          //System.out.print(" read "+v.getName());
          v.read();
          //System.out.println(" ok");
          nbytes += v.getSize() * v.getDataType().getSize();
        }

        ncFile.close();

      endTime = System.currentTimeMillis();
      long diff = endTime - startTime;
      if (stat != null)
        stat.avg(name, diff);
      System.out.println(" read "+nbytes+" bytes from "+fname+ " took "+diff+ " msecs");
      return nbytes;

    } catch (Exception ioe) {
      System.out.println(" read from "+fname+ " failed ");
      ioe.printStackTrace();
      return 0;
    }
  }

  //////////////////////////////////////////////////////////
  static long readAllData(String fname, Stat stat) throws IOException {
    String name = "readAll";
    long startTime, endTime, nbytes = 0;
    if (!fname.endsWith(".nc")) return 0;

    // read
      //System.out.println("Open "+fname);
      startTime = System.currentTimeMillis();
      try {
        NetcdfFile ncFile = NetcdfFile.open( fname);

        Iterator iter = ncFile.getVariables().iterator();
        while (iter.hasNext()) {
          Variable v = (Variable) iter.next();
          //System.out.print(" read "+v.getName());
          v.read();
          //System.out.println(" ok");
          nbytes += v.getSize() * v.getDataType().getSize();
        }

        ncFile.close();

      endTime = System.currentTimeMillis();
      long diff = endTime - startTime;
      if (stat != null)
        stat.avg(name, diff);
      System.out.println(" read "+nbytes+" bytes from "+fname+ " took "+diff+ " msecs");
      return nbytes;

    } catch (Exception ioe) {
      System.out.println(" read from "+fname+ " failed ");
      ioe.printStackTrace();
      return 0;
    }
  }

  static long readAllDataRecord(String fname, Stat stat) throws IOException {
    String name = "readAll";
    long startTime, endTime, nbytes = 0;

    /* read
      //System.out.println("Open "+fname);
      startTime = System.currentTimeMillis();
      try {
        NetcdfFile ncFile = new NetcdfFile( fname);

        Iterator iter = ncFile.getVariableIterator();
        while (iter.hasNext()) {
          Variable v = (Variable) iter.next();
          if (!v.isUnlimited()) {
            v.read();
            nbytes += v.getSize() * v.getDataType().getSize();
          }
        }

        StructureVariable record = (StructureVariable) ncFile.findVariable("record");
        if (record == null) {
          System.out.println("no record variable in "+ncFile.getPathName());
          return 0;
        }
        StructureVariable.Iterator riter = record.getStructureIterator();
        while (riter.hasNext()) {
          StructureVariable s = riter.next();

          // loop over structure variables
          Iterator viter = s.getMembers().iterator();
          while (viter.hasNext()) {
            Variable v2 = (Variable) viter.next();
            v2.read();
            nbytes += v2.getSize() * v2.getDataType().getSize();
          }
        }

        ncFile.close();

        endTime = System.currentTimeMillis();
        long diff = endTime - startTime;
        if (stat != null)
          stat.avg(name, diff);
        System.out.println(" read "+nbytes+" bytes from "+fname+ " took "+diff+ " msecs");
        return nbytes;

    } catch (Exception ioe) {
      System.out.println(" read from "+fname+ " failed ");
      ioe.printStackTrace();
      return 0;
    } */

    return 0;
  }

  public static long doOneDirRecord(File dir, Stat s) throws IOException {
    long total = 0;
    File[] flist = dir.listFiles();
    for (int j=0; j<flist.length; j++) {
      File file = flist[j];
      if (file.isDirectory())
        total += doOneDir( file, s);
      else
        total += readAllDataRecord( file.getAbsolutePath(), s);
    }
    return total;
  }

  public static long doOneDir(File dir, Stat s) throws IOException {
    long total = 0;
    File[] flist = dir.listFiles();
    for (int j=0; j<flist.length; j++) {
      File file = flist[j];
      if (file.isDirectory())
        total += doOneDir( file, s);
      else
        total += readAllData( file.getAbsolutePath(), s);
    }
    return total;
  }


  public static void main(String[] args) {


    Stat v1 = new Stat();
    Stat raf = new Stat();

    long total;
    File topDir = new File("C:/data/conventions/");

    try {
      //SPFactory.setServiceProvider("ucar.nc2.SPNioMMap");
      //readAllData("C:/data/conventions/csm/B06.62.atm.0703.nc", null);

      //SPFactory.setServiceProvider("ucar.nc2.SPNioMMap");
      //total = doOneDirRecord(topDir, mm);
      //System.out.println("total bytes= "+total);

      ///SPFactory.setServiceProvider("ucar.nc2.SPNioCD");
      //total = doOneDirRecord(topDir, cd);
      //System.out.println("total bytes= "+total);

      int n = 10;

      SPFactory.setServiceProvider("ucar.nc2.N3ver1");
      total = readSpreadData("C:/data/conventions/mm5/copy_n040.nc", v1, n);
      System.out.println("total bytes= "+total);

      SPFactory.setServiceProvider("ucar.nc2.iosp.netcdf3.N3raf");
      total = readSpreadData("C:/data/conventions/mm5/n040.nc", raf, n);
      System.out.println("total bytes= "+total);

      SPFactory.setServiceProvider("ucar.nc2.N3ver1");
      total = doOneDir(topDir, v1);
      System.out.println("total bytes= "+total); // */
      
      SPFactory.setServiceProvider("ucar.nc2.iosp.netcdf3.N3raf");
      total = doOneDir(topDir, raf);
      System.out.println("total bytes= "+total);

      /* SPFactory.setServiceProvider("ucar.nc2.SPnioMMap");
      total = doOneDir(topDir, mm);
      System.out.println("total bytes= "+total);

      SPFactory.setServiceProvider("ucar.nc2.SPnioCD");
      total = doOneDir(topDir, cd);
      System.out.println("total bytes= "+total); */


      /* SPFactory.setServiceProvider("ucar.nc2.SPnioMMap");
      total = readSpreadData("C:/data/conventions/mm5/copy2_n040.nc", mm, n);
      System.out.println("total bytes= "+total);

      SPFactory.setServiceProvider("ucar.nc2.SPnioCD");
      total = readSpreadData("C:/data/conventions/mm5/n040.nc", cd, n);
      System.out.println("total bytes= "+total); */


    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.print("Ver1=");
    v1.print();
    System.out.print("Raf=");
    raf.print();


    /*
    doit(null);

   for (int i=0; i<3; i++)
     doit(stat);

    stat.print(); */

  }

}


