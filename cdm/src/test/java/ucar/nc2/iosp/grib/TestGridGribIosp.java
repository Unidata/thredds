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
 *
 * By:   Robb Kambic
 * Date: Mar 10, 2009
 * Time: 10:12:28 AM
 *
 */

package ucar.nc2.iosp.grib;

import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.CompareNetcdf;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.File;

import junit.framework.TestCase;

public class TestGridGribIosp extends TestCase {

    public TestGridGribIosp(String name) {
      super(name);
    }

    public void testCompare() throws IOException {
      File where = new File("C:/data/grib/idd");
      if( where.exists() ) {
        String[] args = new String[ 1 ];
        args[ 0 ] = "C:/data/grib/idd";
        doAll( args );
      } else {
        doAll( null );
      }
    }

    void compareNC(String fileBinary, String fileText) throws IOException {

    long start = System.currentTimeMillis() ;
 
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
    NetcdfFile ncfileBinary = new NetcdfFileSPI(spiB, rafB, fileBinary, null);
    //System.out.println( "Time to create Netcdf object using GridGrib Iosp "+
    //  (System.currentTimeMillis() - start) );
    System.out.println( "Binary Netcdf created" );

    start = System.currentTimeMillis();

    IOServiceProvider spiT = null;
    try {
      spiT = (IOServiceProvider) c.newInstance();
    } catch (InstantiationException e) {
      throw new IOException("IOServiceProvider " + c.getName() + "must have no-arg constructor.");
    } catch (IllegalAccessException e) {
      throw new IOException("IOServiceProvider " + c.getName() + " IllegalAccessException: " + e.getMessage());
    }
    ucar.unidata.io.RandomAccessFile rafT = new ucar.unidata.io.RandomAccessFile(fileText, "r");
    rafT.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
    NetcdfFile ncfileText = new NetcdfFileSPI(spiT, rafT, fileText, null);

    System.out.println( "Text Netcdf created" );

      //System.out.println( "Time to create Netcdf object using Grid1 Grib2 Iosp "+
    //  (System.currentTimeMillis() - start) );
    // org,  copy,  _compareData,  _showCompare,  _showEach
    //ucar.nc2.TestCompare.compareFiles(ncfileBinary, ncfileText, true, true, true);
     CompareNetcdf.compareFiles(ncfileBinary, ncfileText, false, true, false);
     ncfileBinary.close();
     ncfileText.close();
  }

  void doAll(String args[]) throws IOException {

    String dirB1, dirB2;
    if ( args == null || args.length < 1 ) {
      dirB1 = TestAll.testdataDir +"test/motherlode/grid/grib/binary";
      dirB2 = TestAll.testdataDir +"test/motherlode/grid/grib/text";
    } else {
      dirB1 = args[ 0 ] +"/binary";
      dirB2 = args[ 0 ] +"/text";
    }
    File dir = new File( dirB1 );
    if (dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String child : children) {
        //System.out.println( "children i ="+ children[ i ]);
        File aChild = new File(dir, child);
        //System.out.println( "child ="+ child.getName() );
        if (aChild.isDirectory()) {
          // skip index *gbx and inventory *xml files
        } else if (
            child.contains( "ECMWF") ||
            child.contains( "1p25") ||
            child.contains( "OCEAN") ||  
            child.contains( "SPECTRAL") ||  
            child.contains( "SST") ||  
            child.contains( "ukm") ||  
            child.contains( "UKM") ||  
            child.contains( "Ensemble") || // Generating Process ID are Strings
            child.endsWith("gbx") ||
            child.endsWith("gbx8") ||
            child.endsWith("xml") ||
            child.endsWith("tmp") || //index in creation process
            child.length() == 0) { // zero length file, ugh...
        } else {
          System.out.println( "\n\nComparing File "+ child );
          compareNC( dirB1 +"/"+ child, dirB2 +"/"+ child);
        }
      }
    } else {
    }
  }

  private static class NetcdfFileSPI extends NetcdfFile {
    NetcdfFileSPI(IOServiceProvider spi, RandomAccessFile raf, String location, CancelTask cancelTask) throws IOException {
      super(spi, raf, location, cancelTask);
    }
  }

  static public void main(String args[]) throws IOException {
    TestGridGribIosp ggi = new TestGridGribIosp( "" );
    ggi.testCompare();
  }
}