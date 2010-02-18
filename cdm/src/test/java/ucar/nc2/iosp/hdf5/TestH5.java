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
package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * TestSuite that runs all the sample testsNew
 *
 */
public class TestH5 {
  public static boolean dumpFile = false;
  public static String testDir = TestAll.testdataDir + "hdf5/";

 public static NetcdfFile open( String filename) {
    try {
      System.out.println("**** Open "+filename);
      NetcdfFile ncfile = NetcdfFile.open(filename);
      if (TestH5.dumpFile) System.out.println("open "+ncfile);
      return ncfile;

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
      return null;
    }
  }

  public static NetcdfFile openH5( String filename) {
    try {
      System.out.println("**** Open "+ TestAll.testdataDir + "hdf5/"+filename);
      NetcdfFile ncfile = NetcdfFile.open( TestAll.testdataDir + "hdf5/"+filename);
      if (TestH5.dumpFile) System.out.println("open H5 "+ncfile);
      return ncfile;

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
      return null;
    }
  }

  public static NetcdfDataset openH5dataset( String filename) {
    try {
      System.out.println("**** Open "+ TestAll.testdataDir + "hdf5/"+filename);
      NetcdfDataset ncfile = NetcdfDataset.openDataset( TestAll.testdataDir + "hdf5/"+filename);
      if (TestH5.dumpFile) System.out.println("open H5 "+ncfile);
      return ncfile;

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
      return null;
    }
  }

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();

    // hdf5 reading
    suite.addTest(new TestSuite(TestH5ReadBasic.class)); //
    suite.addTest(new TestSuite(TestH5ReadAndCount.class)); //
    suite.addTest(new TestSuite(TestH5ReadStructure.class)); //
    suite.addTest(new TestSuite(TestH5ReadStructure2.class)); //
    suite.addTest(new TestSuite(TestH5Vlength.class)); //
    suite.addTest(new TestSuite(TestH5ReadArray.class)); //
    suite.addTest(new TestSuite(TestOddTypes.class)); //
    suite.addTest(new TestSuite(TestH5compressed.class)); //
    suite.addTest(new TestSuite(TestChunkIndexer.class)); //
    suite.addTest(new TestSuite(TestH5filter.class)); //
    suite.addTest(new TestSuite(TestH5eos.class)); //
    suite.addTest(new TestSuite(TestH5aura.class)); //
    suite.addTest(new TestSuite(TestH5npoess.class)); //

    suite.addTest(new TestSuite(TestN4.class)); //
    suite.addTest(new TestSuite(TestH5read.class)); //
    return suite;
  }
}