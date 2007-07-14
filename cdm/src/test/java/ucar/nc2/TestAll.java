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
public class TestAll {

  public static long startTime;

  // TODO all the static stuff below is also in thredds.unidata.testUtil.TestAll, can we unify?

  // Determine how Unidata "/upc/share" directory is mounted
  // on local machine by reading system or THREDDS property.
  static {
    String upcSharePropName = "unidata.upc.share.path";
    String threddsPropFileName = "thredds.properties";

    // Get system property
    String path = System.getProperty( upcSharePropName );
    if ( path == null )
    {
      // Get user property.
      File userHomeDirFile = new File( System.getProperty( "user.home" ) );
      File userThreddsPropsFile = new File( userHomeDirFile, threddsPropFileName );
      if ( userThreddsPropsFile.exists() && userThreddsPropsFile.canRead() )
      {
        Properties userThreddsProps = new Properties();
        try
        {
          userThreddsProps.load( new FileInputStream( userThreddsPropsFile ) );
        }
        catch ( IOException e )
        {
          System.out.println( "**Failed loading user THREDDS property file: " + e.getMessage() );
        }
        if ( userThreddsProps != null && ! userThreddsProps.isEmpty() )
        {
          path = userThreddsProps.getProperty( upcSharePropName );
        }
      }
    }

    if ( path == null )
    {
      // Get default path.
      System.out.println( "**No \"unidata.upc.share.path\"property, defaulting to \"/upc/share/\"." );
      path = "/upc/share/";
    }
    // Make sure path ends with a slash.
    if ((! path.endsWith( "/")) && ! path.endsWith( "\\"))
    {
      path = path + "/";
    }
    upcShareDir = path;
  }

  /**
   *  Unidata "/upc/share" directory (MAY NOT be used in Unidata nightly testing).
   */
  public static String upcShareDir;

  /**
   * Level 3 test data directory (MAY NOT be used in Unidata nightly testing).
   * Unidata "/upc/share/testdata" directory. For once off testing and debuging.
   */
  public static String upcShareTestDataDir = upcShareDir + "testdata/";

  /**
   * Level 2 test data directory (MAY be used in Unidata nightly testing).
   * Unidata "/upc/share/thredds/data" directory
   */
  public static String upcShareThreddsDataDir = upcShareDir + "thredds/data/";

  // Make sure the temp data dir is created.
  static
  {
    File file = new File( upcShareDir );
    if ( ! file.exists() )
    {
      System.out.println( "**WARN: Non-existence of \"/upc/share\" directory <" + file.getAbsolutePath() + ">." );
    }

    file = new File( upcShareTestDataDir);
    if ( ! file.exists() )
    {
      System.out.println( "**WARN: Non-existence of Level 3 test data directory <" + file.getAbsolutePath() + ">." );
    }
    file = new File( upcShareThreddsDataDir );
    if ( ! file.exists() )
    {
      System.out.println( "**WARN: Non-existence if Level 2 test data directory <" + file.getAbsolutePath() + ">." );
    }
  }

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
    suite.addTest( ucar.nc2.TestLocal.suite());
    suite.addTest( ucar.nc2.TestNC2.suite());
    // suite.addTest( ucar.nc2.iosp.hdf5.TestH5.suite()); //
    suite.addTest( ucar.nc2.TestIosp.suite());   //

    suite.addTest( ucar.nc2.dataset.TestDataset.suite());  //
    //suite.addTest( ucar.nc2.ncml.TestNcML.suite());  //

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