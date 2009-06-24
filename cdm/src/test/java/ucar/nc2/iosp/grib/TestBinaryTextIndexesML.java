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
 * Date: May 28, 2009
 * Time: 12:43:00 PM
 */

package ucar.nc2.iosp.grib;

import ucar.grib.*;
import ucar.grib.grib2.Grib2Record;
import ucar.grib.grib2.Grib2Input;
import ucar.grib.grib2.Grib2GridTableLookup;
import ucar.grib.grib1.Grib1Record;
import ucar.grib.grib1.Grib1Input;
import ucar.grib.grib1.Grib1GridTableLookup;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.grid.GridIndexToNC;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.grid.GridIndex;
import ucar.grid.GridDefRecord;
import ucar.grid.GridRecord;
import ucar.grid.GridTableLookup;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.io.*;

// Purpose: 	walks directory structure testing Grib Binary and Text Indexes .
//            Uses a configuration file to designate which dirs to test on ML
//

public class TestBinaryTextIndexesML {

  /**
   * delete all indexes, it makes a complete rebuild
   */
  private static boolean removeGBX = false;

  /*
  * dirs to inspect
  */
  private List<String> dirs = new ArrayList<String>();

  /*
  * reads in the configuration file
  *
  */
  private boolean readConf(String conf) throws IOException {

    InputStream ios = new FileInputStream(conf);
    BufferedReader dataIS =
        new BufferedReader(new InputStreamReader(ios));

    while (true) {
      String line = dataIS.readLine();
      if (line == null) {
        break;
      }
      if (line.startsWith("#")) {
        continue;
      }
      dirs.add(line);
      //System.out.println( line );
    }
    ios.close();
    return true;
  }

  /*
  * walks the directory trees checking directories
  *
  */
  private void checker() throws IOException {

    System.out.println("Start " + Calendar.getInstance().getTime().toString());
    long start = System.currentTimeMillis();
    for (String dir : dirs) {
      File d = new File(dir);
      if (!d.exists()) {
        System.out.println("Dir " + dir + " doesn't exists");
        continue;
      }
      checkDirs(d);
    }
    System.out.println("End " + Calendar.getInstance().getTime().toString());
    System.out.println("Total time in ms " + (System.currentTimeMillis() - start));
  }

  /*
  * checkDirs is a recursive routine used to walk the directory tree in a
  * depth first search checking the index of GRIB files .
  */
  private void checkDirs(File dir) throws IOException {
    int count = 0;
    if (dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String aChildren : children) {
        if (count == 50 )
          break;
        //System.out.println( "children i ="+ children[ i ]);
        File child = new File(dir, aChildren);
        //System.out.println( "child ="+ child.getName() );
        if (child.isDirectory()) {
          checkDirs(child);
          // skip index *gbx and inventory *xml files
        } else if (aChildren.endsWith("gbx") ||
            aChildren.endsWith("gbx2") ||
            aChildren.endsWith("xml") ||
            aChildren.endsWith("tmp") || //index in creation process
            //aChildren.startsWith("GFS_Global_1p25deg") ||
            aChildren.length() == 0) { // zero length file, ugh...

        } else {
          checkGrib( child);
          count++;
        }
      }
    } else {
    }

  }

  /*
  * Compares index files and netcdf objects
  *
  */
  private void checkGrib(  File grib)
      throws IOException {


    System.out.println("\n\nComparing File:  " + grib.getPath());
    System.out.println("  Index comparisons Binary and Text");
    if ( compareIndexes(grib.getPath(), grib.getPath()) ) {
      System.out.println("  \n  Netcdf Object comparisons Binary and Text");
      compareNC(grib.getPath(), grib.getPath());
    }
    System.out.println();
  }

  void compareNC(String fileBinary, String fileText) throws IOException {

    long start = System.currentTimeMillis();

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
    // this is really a ncfile built with a text index, so now use a binary index
    // reconstruct the ncfile objects
    ncfileBinary.empty();
    GridIndex index = new GribReadIndex().open(fileBinary + ".gbx2");
    Map<String, String> attr = index.getGlobalAttributes();
    int saveEdition = attr.get("grid_edition").equals("2") ? 2 : 1;

    GridTableLookup lookup;
    if (saveEdition == 2) {
      Grib2Record firstRecord = null;
      try {
        Grib2Input g2i = new Grib2Input(rafB);

        long start2 = System.currentTimeMillis();
        // params getProducts (implies  unique GDSs too), oneRecord
        // open it up and get the first product
        rafB.seek(0);
        g2i.scan(false, true);

        List records = g2i.getRecords();
        firstRecord = (Grib2Record) records.get(0);

      } catch (NotSupportedException noSupport) {
        System.err.println("NotSupportedException : " + noSupport);
      }

      lookup = new Grib2GridTableLookup(firstRecord);
    } else {
      Grib1Record firstRecord = null;
      try {
        Grib1Input g1i = new Grib1Input(rafB);

        long start2 = System.currentTimeMillis();
        // params getProducts (implies  unique GDSs too), oneRecord
        // open it up and get the first product
        rafB.seek(0);
        g1i.scan(false, true);

        List records = g1i.getRecords();
        firstRecord = (Grib1Record) records.get(0);

      } catch (NotSupportedException noSupport) {
        System.err.println("NotSupportedException : " + noSupport);
      } catch (NoValidGribException noValid) {
        System.err.println("NoValidGribException : " + noValid);
      }

      lookup = new Grib1GridTableLookup(firstRecord);
    }

    // make it into netcdf objects
    new GridIndexToNC().open(index, lookup, saveEdition, ncfileBinary, null, null);
    ncfileBinary.finish();

    //System.out.println( "Time to create Netcdf object using GridGrib Iosp "+
    //  (System.currentTimeMillis() - start) );
    System.out.println("Binary Netcdf object created");

    start = System.currentTimeMillis();

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

    //System.out.println( "Time to create Netcdf object using Grid1 Grib2 Iosp "+
    //  (System.currentTimeMillis() - start) );
    // org,  copy,  _compareData,  _showCompare,  _showEach
    //ucar.nc2.TestCompare.compareFiles(ncfileBinary, ncfileText, true, true, true);
    TestCompareGrib.compareFiles(ncfileBinary, ncfileText, false, false, false);
    ncfileBinary.close();
    ncfileText.close();
  }

  boolean compareIndexes(String fileBinary, String fileText) throws IOException {
    boolean lengthOK = true;

    long start = System.currentTimeMillis();
    GridIndex giB = new GribReadIndex().open(fileBinary + ".gbx2");
    GridIndex giT = new GribReadIndex().open(fileText + ".gbx");

    // Coordinate systems
    List<GridDefRecord> hcsB = giB.getHorizCoordSys();
    List<GridDefRecord> hcsT = giT.getHorizCoordSys();

    for (int i = 0; i < hcsB.size(); i++) {
      GridDefRecord gdrB = hcsB.get(i);
      GridDefRecord gdrT = hcsT.get(i);

      java.util.Set<String> keysB = gdrB.getKeys();
      for (String key : keysB) {
        if (key.equals("grid_units") || key.equals("created") || key.equals("location") || key.equals("grid_units"))
          continue;
        String valueB = gdrB.getParam(key);
        String valueT = gdrT.getParam(key);
        if (!valueB.equals(valueT))
          System.out.println("hcs " + key + " differ for Binary and Text  " + valueB + " " + valueT);

      }
      java.util.Set<String> keysT = gdrT.getKeys();
      for (String key : keysT) {
        if (key.equals("ScanningMode") || key.equals("created") || key.equals("location") || key.equals("grid_units"))
          continue;
        String valueB = gdrB.getParam(key);
        String valueT = gdrT.getParam(key);
        if (!valueT.equals(valueB))
          System.out.println("hcs " + key + " differ for Binary and Text " + valueB + " " + valueT);

      }
    }

    // Attribubutes
    Map<String, String> attB = giB.getGlobalAttributes();
    Map<String, String> attT = giT.getGlobalAttributes();
    java.util.Set<String> keysB = attB.keySet();
    for (String key : keysB) {
      if (key.equals("basetime") || key.equals("created") || key.equals("location") || key.equals("grid_units"))
        continue;
      String valueB = attB.get(key);
      String valueT = attT.get(key);
      if (!valueB.equals(valueT)) {
        System.out.println("attribute " + key + " differ for Binary and Text  " + valueB + " " + valueT);
        if( key.equals( "length"))
           lengthOK = false;
      }

    }
    java.util.Set<String> keysT = attT.keySet();
    for (String key : keysT) {
      if (key.equals("ensemble") || key.equals("tiles") || key.equals("thin") ||
          key.equals("created") || key.equals("location") || key.equals("grid_units"))
        continue;
      String valueB = attB.get(key);
      String valueT = attT.get(key);
      if (!valueT.equals(valueB)) {
        System.out.println("attribute " + key + " differ for Binary and Text " + valueB + " " + valueT);
        if( key.equals( "length"))
           lengthOK = false;
      }
    }

    // records
    List<GridRecord> grsB = giB.getGridRecords();
    List<GridRecord> grsT = giT.getGridRecords();
    //for(int i = 0; i < grsB.size(); i++ ) {
    int stop = (grsB.size() < 10) ? grsB.size() : 10;
    for (int i = 0; i < stop; i++) {
      GribGridRecord grB = (GribGridRecord) grsB.get(i);
      GribGridRecord grT = (GribGridRecord) grsT.get(i);
      int valueB = grB.gdsKey;
      //String valueB = grB.toString();
      int valueT = grT.gdsKey;
      //String valueT = grT.toString();
      if (valueB != valueT)
        System.out.println("record gdsKey  differ for Binary and Text  " + valueB + "  " + valueT);
    }
    return lengthOK;
  }

  static class MyNetcdfFile extends NetcdfFile {
    MyNetcdfFile(IOServiceProvider spi, ucar.unidata.io.RandomAccessFile raf, String location, CancelTask cancelTask) throws IOException {
      super(spi, raf, location, cancelTask);
    }
  }

  /**
   * main.
   *
   * @param args can be clear and the GribIndexer.conf file
   * @throws IOException on io error
   */
  // process command line switches
  static public void main(String[] args) throws IOException {


    TestBinaryTextIndexesML gbi = new TestBinaryTextIndexesML();

    boolean clear = false;
    for (String arg : args) {
      if (arg.equals("remove")) {
        removeGBX = true;
        System.out.println("Removing all indexes");
        continue;
      }
      // else conf file
      File f = new File(arg);
      if (!f.exists()) {
        System.out.println("Conf file " + arg + " doesn't exist: ");
        return;
      }
      // read in conf file
      gbi.readConf(arg);
    }

    // Grib Index files in dirs
    gbi.checker();

  }

}
