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
import ucar.grid.GridIndex;
import ucar.grib.GribReadIndex;
import ucar.grib.TestAll;
import ucar.grib.GribIndexName;

import java.io.IOException;
import java.io.File;

/**
 * Some grib Indexing tests.
 */
public final class TestGrib2Indexer extends TestCase {
  private final boolean reset = false;
  private String dataPath;
  private String testPath;

  protected final void setUp() {
    dataPath = TestAll.testdataDir + "grid/grib/grib2/data/";
    testPath = TestAll.testdataDir + "grid/grib/grib2/test/";
  }

  /**
   * .
   *
   * @return results of test
   */
  public static Test suite() {

    /*
       * the dynamic way
       */
    return new TestSuite(TestGrib2Indexer.class);
  }

  /**
   * .
   *
   * @throws IOException
   */
  public final void testIndex() throws IOException {

    System.out.println("\nTesting indexing of ndfd.wmo");
    String args[] = new String[2];
    args[0] = dataPath + "ndfd.wmo";
    File f;
    if (reset) {
      args[1] = testPath + "ndfd.wmo"+ GribIndexName.currentSuffix;
      f = new File(args[1]);
      f.delete();
      System.out.println("Creating new test index " + args[1]);
      Grib2WriteIndex.main(args);
    } else {
      args[1] = dataPath + "ndfd.wmo"+ GribIndexName.currentSuffix;
      Grib2WriteIndex.main(args);
      GridIndex index1 = new GribReadIndex().open(args[1]);

      GridIndex index2 = new GribReadIndex().open(testPath + "ndfd.wmo"+ GribIndexName.currentSuffix);
      // Compare Indexes
      testEquals(index1, index2);
      f = new File(args[1]);
      f.delete();
    }
    //if (true )
    //  return;

    System.out.println("\nTesting indexing of AVN.5deg.wmo");
    args[0] = dataPath + "AVN.5deg.wmo";
    if (reset) {
      args[1] = testPath + "AVN.5deg.wmo"+ GribIndexName.currentSuffix;
      f = new File(args[1]);
      f.delete();
      System.out.println("Creating new test index " + args[1]);
      Grib2WriteIndex.main(args);
    } else {
      args[1] = dataPath + "AVN.5deg.wmo"+ GribIndexName.currentSuffix;
      Grib2WriteIndex.main(args);
      GridIndex index1 = new GribReadIndex().open(args[1]);

      GridIndex index2 = new GribReadIndex().open(testPath + "AVN.5deg.wmo"+ GribIndexName.currentSuffix);
      // Compare Indexes
      testEquals(index1, index2);
      f = new File(args[1]);
      f.delete();
    }


    System.out.println("\nTesting indexing of CLDGRIB2.2005040905");
    args[0] = dataPath + "CLDGRIB2.2005040905";
    if (reset) {
      args[1] = testPath + "CLDGRIB2.2005040905"+ GribIndexName.currentSuffix;
      f = new File(args[1]);
      f.delete();
      System.out.println("Creating new test index " + args[1]);
      Grib2WriteIndex.main(args);
    } else {
      args[1] = dataPath + "CLDGRIB2.2005040905"+ GribIndexName.currentSuffix;
      Grib2WriteIndex.main(args);
      GridIndex index1 = new GribReadIndex().open(args[1]);

      GridIndex index2 = new GribReadIndex().open(testPath + "CLDGRIB2.2005040905"+ GribIndexName.currentSuffix);
      // Compare Indexes
      testEquals(index1, index2);
      f = new File(args[1]);
      f.delete();
    }

    System.out.println("\nTesting indexing of Global_1p0deg_Ensemble.grib2");
    args[0] = dataPath + "Global_1p0deg_Ensemble.grib2";
    if (reset) {
      args[1] = testPath + "Global_1p0deg_Ensemble.grib2"+ GribIndexName.currentSuffix;
      f = new File(args[1]);
      f.delete();
      System.out.println("Creating new test index " + args[1]);
      Grib2WriteIndex.main(args);
    } else {
      args[1] = dataPath + "Global_1p0deg_Ensemble.grib2"+ GribIndexName.currentSuffix;
      Grib2WriteIndex.main(args);
      GridIndex index1 = new GribReadIndex().open(args[1]);

      GridIndex index2 = new GribReadIndex().open(testPath + "Global_1p0deg_Ensemble.grib2"+ GribIndexName.currentSuffix);
      // Compare Indexes
      testEquals(index1, index2);
      f = new File(args[1]);
      f.delete();
    }

  }

  public void testEquals(GridIndex index1, GridIndex index2) {

    assert (index1.getGridRecords().size() == index1.getGridRecords().size());
    assert (index1.getHorizCoordSys().size() == index1.getHorizCoordSys().size());
    assert (index1.getGlobalAttributes().size() == index1.getGlobalAttributes().size());

  }

  /**
   * main.
   *
   * @param args indexing tests to run
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
