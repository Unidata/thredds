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
package ucar.grib.grib2;

import junit.framework.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.grib.grib1.*;
import ucar.grib.*;

import java.io.IOException;
import java.io.File;

/**
 * Some grib tests.
 */
public final class TestGrib2Data extends TestCase {
  private final boolean reset = false;
  private String dataPath;
  private String testPath;
  private Diff d;

  protected final void setUp() {
    dataPath = TestAll.testdataDir + "grid/grib/grib2/data/";
    testPath = TestAll.testdataDir + "grid/grib/grib2/test/";
    d = new Diff();
  }

  /**
   * .
   *
   * @return results of tests
   */
  public static Test suite() {

    /*
       * the dynamic way
       */
    return new TestSuite(TestGrib2Data.class);
  }

  /**
   * .
   */
  public final void testData() {

    String arg[] = new String[4];
    File f;

/*
        // output file too large for doDiff
        System.out.println( "\nTesting data extraction from Level3_N0R.grib2" );
        arg[ 0 ] = dataPath + "Level3_N0R.grib2";
        arg[ 1 ] = "37";
        arg[ 2 ] = "118";
        arg[ 3 ] = "Level3_N0R";
        Grib2GetData.main( arg );
        d.doDiff( testPath + "Level3_N0R.test", "Level3_N0R" );
        f = new File( "Level3_N0R" );
        f.delete();
        if( reset ) {
            arg[ 3 ] = testPath + "Level3_N0R.test";
            Grib2GetData.main( arg );
        }
*/
    System.out.println("\nTesting data extraction from ndfd.wmo");
    arg[0] = dataPath + "ndfd.wmo";
    arg[1] = "498193";
    arg[2] = "498274";
    arg[3] = "NdfdData";
    Grib2GetData.main(arg);
    d.doDiff(testPath + "NdfdData.test", "NdfdData");
    f = new File("NdfdData");
    f.delete();
    if (reset) {
      arg[3] = testPath + "NdfdData.test";
      Grib2GetData.main(arg);
    }


    System.out.println("\nTesting data extraction from ds.mint.bin");
    arg[0] = dataPath + "ds.mint.bin";
    arg[1] = "117";
    arg[2] = "198";
    arg[3] = "ds.mint.binData";
    Grib2GetData.main(arg);
    d.doDiff(testPath + "ds.mint.binData.test", "ds.mint.binData");
    f = new File("ds.mint.binData");
    f.delete();
    if (reset) {
      arg[3] = testPath + "ds.mint.binData.test";
      Grib2GetData.main(arg);
    }


    System.out.println("\nTesting data extraction from gfs.000192 ");
    arg[0] = dataPath + "gfs.000192";
    arg[1] = "37";
    arg[2] = "130428";
    arg[3] = "gfs.000192Data";
    if (reset) {
      arg[3] = testPath + "gfs.000192Data.test";
      Grib2GetData.main(arg);
    } else {
      Grib2GetData.main(arg);
      d.doDiff(testPath + "gfs.000192Data.test", "gfs.000192Data");
      f = new File("gfs.000192Data");
      f.delete();
    }


    System.out.println("\nTesting data extraction from AVN.5deg.wmo");
    arg[0] = dataPath + "AVN.5deg.wmo";
    arg[1] = "22575268";
    arg[2] = "22575340";
    arg[3] = "AVN.5deg.data";
    Grib2GetData.main(arg);
    d = new Diff();
    d.doDiff(testPath + "AVN.5degData.test", "AVN.5deg.data");
    f = new File("AVN.5deg.data");
    f.delete();
    if (reset) {
      arg[3] = testPath + "AVN.5degData.test";
      Grib2GetData.main(arg);
    }
    //

    System.out.println("\nTesting data extraction from CLDGRIB2.2005040905");
    arg[0] = dataPath + "CLDGRIB2.2005040905";
    arg[1] = "193485";
    arg[2] = "193566";
    arg[3] = "CLDGRIB2";
    Grib2GetData.main(arg);
    d = new Diff();
    d.doDiff(testPath + "CLDGRIB2Data.test", "CLDGRIB2");
    f = new File("CLDGRIB2");
    f.delete();
    if (reset) {
      arg[3] = testPath + "CLDGRIB2Data.test";
      Grib2GetData.main(arg);
    }

    System.out.println("\nTesting data extraction from z_tigge_c_ecmf_20060827000000_0078_pv_glob_test.grib2");
    arg[0] = dataPath + "z_tigge_c_ecmf_20060827000000_0078_pv_glob_test.grib2";
    arg[1] = "42";
    arg[2] = "914";
    arg[3] = "ztigge.data";
    Grib2GetData.main(arg);
    d = new Diff();
    d.doDiff(testPath + "ztigge.data.test", "ztigge.data");
    f = new File("ztigge.data");
    f.delete();
    if (reset) {
      arg[3] = testPath + "ztigge.data.test";
      Grib2GetData.main(arg);
    }

    //if( true ) return;

    System.out.println("\nTesting Jpeg2000 data extraction from 0905712000000");
    arg[0] = dataPath + "0905712000000";
    arg[1] = "882470";
    arg[2] = "882551";
    arg[3] = "0905712000000.data";
    Grib2GetData.main(arg);
    d = new Diff();
    d.doDiff(testPath + "0905712000000.data.test", "0905712000000.data");
    f = new File("0905712000000.data");
    f.delete();
    if (reset) {
      arg[3] = testPath + "0905712000000.data.test";
      Grib2GetData.main(arg);
    }

  }
  //public void testEquals() {
  //assertEquals(12, 12);
  //assertEquals(12L, 12L);
  //assertEquals(new Long(12), new Long(12));

  //assertEquals("Size", 12, 13);
  //assertEquals("Capacity", 12.0, 11.99, 0.0);
  //}

  /**
   * main.
   *
   * @param args tests to run
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
