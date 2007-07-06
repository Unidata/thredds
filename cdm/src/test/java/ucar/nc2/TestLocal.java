package ucar.nc2;

import junit.framework.*;
import junit.extensions.TestSetup;

import java.util.List;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ucar.unidata.io.RandomAccessFile;

/**
 * TestSuite that runs all nj22 unit tests.
 *
 */
public class TestLocal {

  public static long startTime;

  /**
   * Level 1 test data directory (distributed with code and MAY be used in Unidata nightly testing).
   */
  public static String cdmTestDataDir = "src/test/data/";

  /**
   * Temporary data directory (for writing temporary data).
   */
  public static String temporaryDataDir = "target/test/tmp/";

  // Make sure the temp data dir is created.
  static {
    File tmpDataDir = new File( temporaryDataDir);
    if ( ! tmpDataDir.exists() )
    {
      if ( ! tmpDataDir.mkdirs() )
      {
        System.out.println( "**ERROR: Could not create temporary data dir <" + tmpDataDir.getAbsolutePath() + ">." );
      }
    }
  }

  public static junit.framework.Test suite ( ) {

    RandomAccessFile.setDebugLeaks( true);

    TestSuite suite= new TestSuite();
    suite.addTest( ucar.ma2.TestMA2.suite());
    suite.addTest( ucar.nc2.TestLocalNC2.suite());
    suite.addTest( ucar.nc2.units.TestUnitsAll.suite());

    // suite.addTest( ucar.nc2.TestH5.suite()); //
    // suite.addTest( ucar.nc2.TestIosp.suite());   //

    /* suite.addTest( ucar.nc2.dataset.TestDataset.suite());  //
    suite.addTest( ucar.nc2.ncml.TestNcML.suite());  //

    suite.addTest( ucar.nc2.dt.grid.TestGrid.suite()); //
    suite.addTest( ucar.nc2.dt.TestTypedDatasets.suite());

    suite.addTest( ucar.unidata.geoloc.TestGeoloc.suite());  //
    suite.addTest( ucar.nc2.dods.TestDODS.suite()); //

    suite.addTest( thredds.catalog.TestCatalogAll.suite()); // */

    TestSetup wrapper = new TestSetup(suite) {

      protected void setUp() {
        //NetcdfFileCache.init();
        //NetcdfDatasetCache.init();
        RandomAccessFile.setDebugLeaks(true);
        startTime = System.currentTimeMillis();
      }

      protected void tearDown() {
        checkLeaks();
        //NetcdfFileCache.clearCache( true);
        //NetcdfDatasetCache.clearCache( true);
        checkLeaks();

        double took = (System.currentTimeMillis() - startTime) * .001;
        System.out.println(" that took= "+took+" secs");
      }
    };

    return wrapper;
  }

  static private void checkLeaks() {
    List openFiles = RandomAccessFile.openFiles;
    for (int i = 0; i < openFiles.size(); i++) {
      String o = (String) openFiles.get(i);
      System.out.println(" RandomAccessFile still open= " + o);
    }
  }

  static public boolean closeEnough( double d1, double d2) {
    if (d1 < 1.0e-5) return Math.abs(d1-d2) < 1.0e-5;
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }

  static public boolean closeEnough( double d1, double d2, double tol) {
    if (d1 < tol) return Math.abs(d1-d2) < tol;
    return Math.abs((d1-d2)/d1) < tol;
  }

  static public boolean closeEnough( float d1, float d2) {
    if (d1 < 1.0e-5) return Math.abs(d1-d2) < 1.0e-5;
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }

  static public void showMem(String where) {
    Runtime runtime = Runtime.getRuntime();
    System.out.println(where+ " memory free = " + runtime.freeMemory() * .001 * .001 +
        " total= " + runtime.totalMemory() * .001 * .001 +
        " max= " + runtime.maxMemory() * .001 * .001 +
        " MB");
  }

}