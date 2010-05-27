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
package ucar.grib.grib1;

import junit.framework.*;
import ucar.grib.*;
import ucar.grid.GridIndex;

import java.io.IOException;
import java.io.File;

/**
 * Some grib tests.
 */
public final class TestGrib1Indexer extends TestCase {
  private final boolean reset = false;
  private String dataPath;
  private String testPath;
  private File f;

  protected final void setUp() {
    dataPath = TestAll.testdataDir +"grid/grib/grib1/data/";
    testPath = TestAll.testdataDir +"grid/grib/grib1/test/";
  }

  /**
   * gives status of test.
   *
   * @return status
   */
  public static Test suite() {
    return new TestSuite(TestGrib1Indexer.class);
  }

  /**
   * test the indexing of Grib files against test files.
   *
   * @throws IOException
   */
  public final void testIndexer() throws IOException {
    System.out.println("\nTesting indexing of RUC.wmo");
    String args[] = new String[2];
    args[0] = dataPath + "RUC.wmo";
    File f;
    if (reset) {
      args[1] = testPath + "RUC.wmo"+ GribIndexName.currentSuffix;
      f = new File( args[1] );
      f.delete();
      System.out.println("Creating new test index " + args[1]);
      Grib1WriteIndex.main(args);
    } else {
      args[1] = dataPath + "RUC.wmo"+ GribIndexName.currentSuffix;
      Grib1WriteIndex.main(args);
      GridIndex index1 = new GribReadIndex().open(args[1]);

      GridIndex index2 = new GribReadIndex().open(testPath + "RUC.wmo"+ GribIndexName.currentSuffix);
      // Compare Indexes
      testEquals( index1, index2 );
      f = new File(args[1]);
      f.delete();
    }

    System.out.println("\nTesting indexing of RUC2_CONUS_20km_surface_20051011_2300.grib1");
    args[0] = dataPath + "RUC2_CONUS_20km_surface_20051011_2300.grib1";
    if (reset) {
      args[1] = testPath + "RUC2_CONUS_20km_surface_20051011_2300.grib1"+ GribIndexName.currentSuffix;
      f = new File( args[1] );
      f.delete();
      System.out.println("Creating new test index " + args[1]);
      Grib1WriteIndex.main(args);
    } else {
      args[1] = dataPath + "RUC2_CONUS_20km_surface_20051011_2300.grib1"+ GribIndexName.currentSuffix;
      Grib1WriteIndex.main(args);
      GridIndex index1 = new GribReadIndex().open(args[1]);

      GridIndex index2 = new GribReadIndex().open(testPath + "RUC2_CONUS_20km_surface_20051011_2300.grib1"+ GribIndexName.currentSuffix);
      // Compare Indexes
      testEquals( index1, index2 );
      f = new File(args[1]);
      f.delete();
    }
  }

  public void testEquals( GridIndex index1, GridIndex index2 ) {

    assert( index1.getGridRecords().size() == index1.getGridRecords().size() );
    assert( index1.getHorizCoordSys().size() == index1.getHorizCoordSys().size() );
    assert( index1.getGlobalAttributes().size()  == index1.getGlobalAttributes().size() );

  }

  /**
   * main.
   *
   * @param args
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
