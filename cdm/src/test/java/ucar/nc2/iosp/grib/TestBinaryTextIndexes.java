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
/**
 * User: rkambic
 * Date: May 6, 2009
 * Time: 2:04:15 PM
 */

package ucar.nc2.iosp.grib;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Map;

import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;
import ucar.grid.GridIndex;
import ucar.grid.GridDefRecord;
import ucar.grid.GridRecord;
import ucar.grib.GribReadIndex;
import ucar.grib.GribGridRecord;
import ucar.unidata.io.RandomAccessFile;

public class TestBinaryTextIndexes extends TestCase {

  public TestBinaryTextIndexes(String name) {
    super(name);
  }

  public void testCompare() throws IOException {
    File where = new File("C:/data/grib/idd");
    if (where.exists()) {
      String[] args = new String[1];
      args[0] = "C:/data/grib/idd";
      doAll(args);
    } else {
      doAll(null);
    }
  }

  void compareNC(String fileBinary, String fileText) throws IOException {

    Class c = ucar.nc2.iosp.grib.GribGridServiceProvider.class;
    IOServiceProvider spiB = null;
    try {
      spiB = (IOServiceProvider) c.newInstance();
    } catch (InstantiationException e) {
      throw new IOException("IOServiceProvider " + c.getName() + "must have no-arg constructor.");
    } catch (IllegalAccessException e) {
      throw new IOException("IOServiceProvider " + c.getName() + " IllegalAccessException: " + e.getMessage());
    }
    ucar.unidata.io.RandomAccessFile rafB = new ucar.unidata.io.RandomAccessFile(fileBinary, "r");
    rafB.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
    NetcdfFile ncfileBinary = new MyNetcdfFile(spiB, rafB, fileBinary, null);
    System.out.println("Binary Netcdf object created");

    try {
      spiB = (IOServiceProvider) c.newInstance();
    } catch (InstantiationException e) {
      throw new IOException("IOServiceProvider " + c.getName() + "must have no-arg constructor.");
    } catch (IllegalAccessException e) {
      throw new IOException("IOServiceProvider " + c.getName() + " IllegalAccessException: " + e.getMessage());
    }
    ucar.unidata.io.RandomAccessFile rafT = new ucar.unidata.io.RandomAccessFile(fileText, "r");
    rafT.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
    NetcdfFile ncfileText = new MyNetcdfFile(spiB, rafT, fileText, null);

    System.out.println("Text Index Netcdf object created");

    // org,  copy,  _compareData,  _showCompare,  _showEach
    //ucar.nc2.TestCompare.compareFiles(ncfileBinary, ncfileText, true, true, true);
    TestCompareGrib.compareFiles(ncfileBinary, ncfileText, false, true, false);
    ncfileBinary.close();
    ncfileText.close();
  }

  void compareIndexes(String fileBinary, String fileText) throws IOException {
    GridIndex giB = new GribReadIndex().open(fileBinary + ".gbx8");
    GridIndex giT = new GribReadIndex().open(fileText + ".gbx");

    // Coordinate systems
    List<GridDefRecord> hcsB = giB.getHorizCoordSys();
    List<GridDefRecord> hcsT = giT.getHorizCoordSys();

    for (int i = 0; i < hcsB.size(); i++) {
      GridDefRecord gdrB = hcsB.get(i);
      GridDefRecord gdrT = hcsT.get(i);

      java.util.Set<String> keysB = gdrB.getKeys();
      for (String key : keysB) {
        if (key.equals("grid_units") || key.equals("created") || key.equals("location")
            || key.equals("VectorComponentFlag") || key.equals("GDSkey")
            || key.equals("grid_name") || key.equals("scanning_mode")
            || key.equals("grid_shape") )
          continue;
        String valueB = gdrB.getParam(key);
        String valueT = gdrT.getParam(key);
        if (!valueB.equals(valueT))
          System.out.println("hcs " + key + " differ for Binary and Text  " + valueB + " " + valueT);
        assert( valueB.equals(valueT) );
      }
      java.util.Set<String> keysT = gdrT.getKeys();
      for (String key : keysT) {
        if (key.equals("grid_units") || key.equals("created") || key.equals("location")
            || key.equals("VectorComponentFlag") || key.equals("GDSkey")
            || key.equals("grid_name") || key.equals("ScanningMode")
            || key.equals("grid_shape") )
          continue;
        String valueB = gdrB.getParam(key);
        String valueT = gdrT.getParam(key);
        if (!valueT.equals(valueB))
          System.out.println("hcs " + key + " differ for Text and Binary " + valueT + " " + valueB);
        assert( valueB.equals(valueT) );
      }
    }

    // Attribubutes
    Map<String, String> attB = giB.getGlobalAttributes();
    Map<String, String> attT = giT.getGlobalAttributes();
    java.util.Set<String> keysB = attB.keySet();
    for (String key : keysB) {
      if (key.equals("basetime") || key.equals("created") || key.equals("location")
          || key.equals("grid_units") || key.equals("ensemble"))
        continue;
      String valueB = attB.get(key);
      String valueT = attT.get(key);
      if (!valueB.equals(valueT) || key.equals( "index_version"))
        System.out.println("attribute " + key + " differ for Binary and Text  " + valueB + " " + valueT);
      if( key.equals( "index_version") )
        continue;
      assert( valueB.equals(valueT) );
    }
    java.util.Set<String> keysT = attT.keySet();
    for (String key : keysT) {
      if (key.equals("ensemble") || key.equals("tiles") || key.equals("thin") ||
              key.equals("created") || key.equals("location") || key.equals("grid_units"))
        continue;
      String valueB = attB.get(key);
      String valueT = attT.get(key);
      if (!valueT.equals(valueB) || key.equals( "index_version"))
        System.out.println("attribute " + key + " differ for Text and Binary " + valueT + " " + valueB);
      if( key.equals( "index_version") )
        continue;
      assert( valueB.equals(valueT) );
    }

  }


  void doAll(String args[]) throws IOException {

    String dirB, dirT;
    if (args == null || args.length < 1) {
      dirB = TestAll.testdataDir + "test/motherlode/grid/grib/binary";
      dirT = TestAll.testdataDir + "test/motherlode/grid/grib/text";
    } else {
      dirB = args[0] + "/binary"; // "/local/robb/data/grib/idd/binary";
      dirT = args[0] + "/text"; // "/local/robb/data/grib/idd/text";
    }
    //File dir = new File(dirB);
    File dir = new File(dirT);
    if (dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String child : children) {
        //System.out.println( "children i ="+ children[ i ]);
        File aChild = new File(dir, child);
        //System.out.println( "child ="+ child.getName() );
        if (aChild.isDirectory()) {
          continue;
          // skip old grib1 files, new grib2 Ensemble files, index and inventory files
        } else if (
            child.contains( "Ensemble") ||
            child.contains( "SREF") ||
            child.contains( "GFS_Spectral") || //uses >1 parameter tables
            child.contains( "SPECTRAL") || //uses >1 parameter tables
            child.contains( "OCEAN") || //uses >1 horizontal coord system
            child.contains( "ECMWF") || //uses >1 horizontal coord system
            child.contains( "GFS_Extended") || //uses >1 horizontal coord system
            child.contains( "SST") || //uses >1 horizontal coord system
            child.contains( "UKMET") || //uses >1 horizontal coord system
            child.contains( "ukm") || //uses >1 horizontal coord system
            child.contains( "GFS_Global_1p25deg") || //uses >1 horizontal coord system
            child.endsWith("gbx") ||
            child.endsWith("gbx8") ||
            child.endsWith("xml") ||
            child.endsWith("tmp") || //index in creation process
            child.length() == 0) { // zero length file, ugh...
        } else {
          //child = "GFS_CONUS_191km_20090313_1200.grib1";
          System.out.println("\n\nComparing File:  " + child);
          System.out.println("  Index comparisons Binary and Text");
          compareIndexes(dirB + "/" + child, dirT + "/" + child);
          // This test doesn't consider the additions/deletions to the index structure
          //System.out.println("  \n  Netcdf Object comparisons Binary and Text");
          //compareNC(dirB + "/" + child, dirT + "/" + child);
          System.out.println();
        }
      }
    } else {
    }
  }

  static class MyNetcdfFile extends NetcdfFile {
    MyNetcdfFile(IOServiceProvider spi, RandomAccessFile raf, String location, CancelTask cancelTask) throws IOException {
      super(spi, raf, location, cancelTask);
    }
  }

  static public void main(String args[]) throws IOException {
    TestBinaryTextIndexes bti = new TestBinaryTextIndexes("");
    bti.testCompare();
  }

}
