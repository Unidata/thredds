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
 * Date: May 5, 2009
 * Time: 4:40:05 PM
 */

package ucar.nc2;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;

import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.CompareNetcdf;

public class TestTextIndexes extends TestCase {

    public TestTextIndexes(String name) {
      super(name);
    }

    public void testCompare( String[] newargs ) throws IOException {
      File where = new File("C:/data/grib/idd");
      if( where.exists() ) {
        String[] args = new String[ 1 ];
        args[ 0 ] = "C:/data/grib/idd";
        doAll( args );
      } else {
        doAll( newargs );
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
    NetcdfFile ncfileBinary = new NetcdfFile(spiB, rafB, fileBinary, null);
    //System.out.println( "Time to create Netcdf object using GridGrib Iosp "+
    //  (System.currentTimeMillis() - start) );
    System.out.println( "Text3_17 Netcdf object created" );

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
    NetcdfFile ncfileText = new NetcdfFile(spiB, rafT, fileText, null);

    System.out.println( "Text Index Netcdf object created" );

    //System.out.println( "Time to create Netcdf object using Grid1 Grib2 Iosp "+
    //  (System.currentTimeMillis() - start) );
    // org,  copy,  _compareData,  _showCompare,  _showEach
    //ucar.nc2.TestCompare.compareFiles(ncfileBinary, ncfileText, true, true, true);
     CompareNetcdf.compareFiles(ncfileBinary, ncfileText, false, true, false);
     ncfileBinary.close();
     ncfileText.close();
  }

  void doAll(String args[]) throws IOException {

    String dirB, dirT;
    if ( args == null || args.length < 1 ) {
      dirB = TestAll.testdataDir +"test/motherlode/grid/grib/binary";
      dirT = TestAll.testdataDir +"test/motherlode/grid/grib/text";
    } else {
      dirB = args[ 0 ] +"/text3_17"; // "/local/robb/data/grib/idd/binary";
      dirT = args[ 0 ] +"/text"; // "/local/robb/data/grib/idd/text";
    }
    File dir = new File( dirB );
    if (dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String child : children) {
        //System.out.println( "children i ="+ children[ i ]);
        File aChild = new File(dir, child);
        //System.out.println( "child ="+ child.getName() );
        if (aChild.isDirectory()) {
          continue;
          // skip index *gbx and inventory *xml files
        } else if (
            // can't be displayed by Grib(1|2) iosp
            child.contains( "Ensemble") ||
            child.contains( "SREF") ||
            child.contains( "GFS_Spectral") || //uses >1 parameter tables
            child.contains( "SPECTRAL") || //uses >1 parameter tables
            child.contains( "OCEAN") || //uses >1 horizontal coord system
            child.contains( "ECMWF") || //uses >1 horizontal coord system
            child.contains( "SST") || //uses >1 horizontal coord system
            child.contains( "UKMET") || //uses >1 horizontal coord system
            child.contains( "GFS_Global_1p25deg") || //uses >1 horizontal coord system
            child.endsWith("gbx") ||
            child.endsWith("xml") ||
            child.endsWith("tmp") || //index in creation process
            child.length() == 0) { // zero length file, ugh...
        } else {
          System.out.println( "\n\nComparing File "+ child );
          compareNC( dirB +"/"+ child, dirT +"/"+ child);
        }
      }
    } else {
    }
  }

  static public void main(String args[]) throws IOException {
    TestTextIndexes ti = new TestTextIndexes( "" );
    ti.testCompare( args );
  }
  
}
