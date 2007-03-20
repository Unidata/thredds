package ucar.nc2;

import junit.framework.*;
import junit.extensions.TestSetup;

import java.util.List;
import java.util.Properties;

import ucar.unidata.io.RandomAccessFile;

/**
 * TestSuite that runs all nj22 unit tests.
 *
 */
public class TestAll {

  public static long startTime;

  static {
    Properties env = System.getProperties();
    String path = env.getProperty( "unidata.upc.share.path" );
    if ( path == null )     
    {
      System.out.println( "**No \"unidata.upc.share.path\"property, using \"/upc/share/\"." );
      path = "/upc/share/";
    }
    if ((! path.endsWith( "/")) && ! path.endsWith( "\\"))
    {
      path = path + "/";
    }
    upcShareDir = path;
  }

  public static String upcShareDir = "R:/";
  public static String upcShareTestDataDir = upcShareDir + "testdata/";

  public static String cdmTestDataDir = "./src/test/data/";
  // public static String testdataDir = "/upc/share/testdata/";

  public static String temporaryDataDir = "./target/test/tmp/";

  public static junit.framework.Test suite ( ) {
    RandomAccessFile.setDebugLeaks( true);

    TestSuite suite= new TestSuite();
    suite.addTest( ucar.nc2.units.TestUnitsAll.suite());
    suite.addTest( ucar.nc2.TestNC2.suite());

    suite.addTest( ucar.ma2.TestMA2.suite());
    suite.addTest( ucar.nc2.TestH5.suite()); //
    suite.addTest( ucar.nc2.TestIosp.suite());   //

    suite.addTest( ucar.nc2.dataset.TestDataset.suite());  //
    suite.addTest( ucar.nc2.ncml.TestNcML.suite());  // */

    suite.addTest( ucar.nc2.dt.grid.TestGrid.suite()); //
    suite.addTest( ucar.nc2.dt.TestTypedDatasets.suite());

    suite.addTest( ucar.unidata.geoloc.TestGeoloc.suite());  //
    suite.addTest( ucar.nc2.dods.TestDODS.suite()); // */

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
    System.out.println("RandomAccessFile still open");
    List openFiles = RandomAccessFile.openFiles;
    for (int i = 0; i < openFiles.size(); i++) {
      String o = (String) openFiles.get(i);
      System.out.println(" open= " + o);
    }
  }

  static public boolean closeEnough( double d1, double d2) {
    if (d1 < 1.0e-5) return Math.abs(d1-d2) < 1.0e-5;
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }

  static public boolean closeEnough( float d1, float d2) {
    if (d1 < 1.0e-5) return Math.abs(d1-d2) < 1.0e-5;
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }

}