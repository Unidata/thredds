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
package ucar.nc2;

import junit.framework.*;
import junit.extensions.TestSetup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileFilter;

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.TestDiskCacheUtils;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * TestSuite that runs all nj22 unit tests.
 *
 */
public class TestAll {

  public static long startTime;

  /**
   * Old test data directory. may have cruft in it
   * Unidata "//fileserver/data/testdata2" directory.
   */
  public static String testdataDir = null;

  /**
   * New test data directory. do not put temprory files in here. migrate all test data here eventually
   * Unidata "//fileserver/data/testdata2/cdmUnitTest" directory.
   */
  public static String cdmUnitTestDir = null;

  /**
   * Level 1 test data directory (distributed with code and MAY be used in Unidata nightly testing).
   */
  public static String cdmLocalTestDataDir = "src/test/data/";

  /**
   * Temporary data directory (for writing temporary data).
   */
  public static String temporaryLocalDataDir = "target/test/tmp/";

  //////////////////////////////////////////////////////////////////////
  /** Property name for the path to the Unidata test data directory,
   * e.g unidata.testdata2.path=//shemp/data/testdata2/
   * the real directory is at shemp:/data/testdata2
   */
  private static String testdataDirPropName ="unidata.testdata.path";


  /** Filename of the user property file read from the "user.home" directory
   * if the "unidata.testdata2.path" and "unidata.upc.share.path" are not
   * available as system properties. */
  private static String threddsPropFileName = "thredds.properties";

  // TODO all the static stuff below is also in thredds.unidata.testUtil.TestAll, can we unify?

  // Determine how Unidata "/upc/share" directory is mounted
  // on local machine by reading system or THREDDS property.
  static {
    // Check for system property
    String testdataDirPath = System.getProperty( testdataDirPropName );

    if (testdataDirPath == null )
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
          if ( testdataDirPath == null )
            testdataDirPath = userThreddsProps.getProperty( testdataDirPropName );
        }
      }
    }

    // Use default paths if needed.
    if ( testdataDirPath == null )
    {
      System.out.println( "**No \"unidata.testdata.path\"property, defaulting to \"/share/testdata/\"." );
      testdataDirPath = "/share/testdata/";
    }
    // Make sure paths ends with a slash.
    if ((!testdataDirPath.endsWith( "/")) && !testdataDirPath.endsWith( "\\"))
      testdataDirPath += "/";

    testdataDir = testdataDirPath;
    cdmUnitTestDir = testdataDirPath + "cdmUnitTest/";

    File file = new File( cdmUnitTestDir );
    if ( ! file.exists() || !file.isDirectory() )
    {
      System.out.println( "**WARN: Non-existence of Level 3 test data directory [" + file.getAbsolutePath() + "]." );
    }

    File tmpDataDir = new File(temporaryLocalDataDir);
    if ( ! tmpDataDir.exists() )
    {
      if ( ! tmpDataDir.mkdirs() )
      {
        System.out.println( "**ERROR: Could not create temporary data dir <" + tmpDataDir.getAbsolutePath() + ">." );
      }
    }

    // Setup for DiskCache
    TestDiskCacheUtils.setupDiskCacheInTmpDir( true );
    TestDiskCacheUtils.emptyDiskCache( null );

    // Setup for DiskCache2.
    TestDiskCacheUtils.setupDiskCache2WithTmpRootDir();
  }

  public static junit.framework.Test suite ( ) {
    RandomAccessFile.setDebugLeaks( true);

    TestSuite suite= new TestSuite();

    suite.addTest( ucar.nc2.TestLocal.suite()); // data in the release

    suite.addTest( ucar.nc2.TestNC2.suite());
    suite.addTest( ucar.nc2.dataset.TestDataset.suite());  //

    // aggregation, no cache
    suite.addTest( ucar.nc2.ncml.TestNcML.suite());
    suite.addTest( ucar.nc2.ncml.TestNcMLoffsite.suite()); //

    suite.addTest( ucar.nc2.dt.grid.TestGrid.suite()); //
    suite.addTest( ucar.nc2.ft.TestFeatureDatasets.suite());
    suite.addTest( ucar.nc2.dt.TestTypedDatasets.suite()); // deprecated, but backwards compatible

    suite.addTest( ucar.unidata.geoloc.TestGeoloc.suite());  //
    suite.addTest( ucar.nc2.dods.TestDODS.suite()); //

    suite.addTest( thredds.catalog.TestCatalogAll.suite()); //

    suite.addTest( ucar.nc2.TestIosp.suite());   //  */

   return new TestSetup(suite) {

      protected void setUp() {
        //NetcdfDataset.initNetcdfFileCache(10, 20, 60*60);
        RandomAccessFile.setDebugLeaks(true);
        startTime = System.currentTimeMillis();
      }

      protected void tearDown() {
        checkLeaks();
        ucar.nc2.util.cache.FileCache fc = NetcdfDataset.getNetcdfFileCache(); 
        if (fc != null) fc.clearCache(true); // give messages on files not closed
        checkLeaks();
        NetcdfDataset.shutdown();

        double took = (System.currentTimeMillis() - startTime) * .001;
        System.out.println(" that took= "+took+" secs");

        showFilesUsed();
      }
    };
  }

  static public void checkLeaks() {
    if (RandomAccessFile.getOpenFiles().size() > 0) {
      System.out.println("RandomAccessFile still open:");
      for (String filename : RandomAccessFile.getOpenFiles()) {
        System.out.println(" open= " + filename);
      }
    } else {
      System.out.println(" no leaks");
    }
  }

  static private void showFilesUsed() {
    System.out.println("All Files Used:");
    for (String s : RandomAccessFile.getAllFiles()) {
      System.out.printf(" %s%n", s);
    }
  }

  static public boolean closeEnough( double d1, double d2) {
    if (Math.abs(d1) < 1.0e-5) return Math.abs(d1-d2) < 1.0e-5;
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }

  static public boolean closeEnough( double d1, double d2, double tol) {
    if (Math.abs(d1) < tol) return Math.abs(d1-d2) < tol;
    double pd = (d1-d2)/d1;
    return Math.abs(pd) < tol;
  }

  static public double howClose( double d1, double d2) {
    double pd = (d1-d2)/d1;
    return Math.abs(pd);
  }

  static public boolean closeEnough( float d1, float d2) {
    if (Math.abs(d1) < 1.0e-5) return Math.abs(d1-d2) < 1.0e-5;
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }

  static public void showMem(String where) {
    Runtime runtime = Runtime.getRuntime();
    System.out.println(where+ " memory free = " + runtime.freeMemory() * .001 * .001 +
        " total= " + runtime.totalMemory() * .001 * .001 +
        " max= " + runtime.maxMemory() * .001 * .001 +
        " MB");
  }

  public static void openAllInDir(String dirName, FileFilter ff) throws IOException {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID "+dirName);
      return;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if ((ff == null) || ff.accept(f)) {
        System.out.println("  try to open "+name);
        NetcdfFile ncfile = NetcdfFile.open(name);
        ncfile.close();
      }
    }

    for (File f : allFiles) {
      if (f.isDirectory())
        openAllInDir(f.getAbsolutePath(), ff);
    }

  }

  /* usage:
    TestAll.readAllDir(dirName, new FileFilter() {
      public boolean accept(File file) {
        String name = file.getPath();
        return (name.endsWith(".h5") || name.endsWith(".H5") || name.endsWith(".he5") || name.endsWith(".nc"));
      }
    });
   */
  public static int readAllDir(String dirName, FileFilter ff) {
    int count = 0;

    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID "+dirName);
      return count;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude"))
        count += readAll(name);
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude"))
        count += readAllDir(f.getAbsolutePath(), ff);
    }

    return count;
  }

  static public int readAll( String filename) {
    System.out.println("\n------Reading filename "+filename);
    try {
      NetcdfFile ncfile = NetcdfFile.open(filename);

      for (Variable v : ncfile.getVariables()) {
        if (v.getSize() > max_size) {
          Section s = makeSubset(v);
          System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize() + " section= " + s);
          v.read(s);
        } else {
          System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize());
          v.read();
        }
      }
      ncfile.close();
    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

    return 1;
  }

  static public int readAll( NetcdfFile ncfile) {
    System.out.println("\n------Reading ncfile "+ncfile.location);
    try {

      for (Variable v : ncfile.getVariables()) {
        if (v.getSize() > max_size) {
          Section s = makeSubset(v);
          System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize() + " section= " + s);
          v.read(s);
        } else {
          System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize());
          v.read();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

    return 1;
  }


  ////////////////////////////////////////////////

  public interface Act {
    /**
     * @param filename file to act on
     * @return count
     * @throws IOException  on IO error
     */
    int doAct( String filename) throws IOException;
  }

  public static class FileFilterImpl implements FileFilter {
    String[] suffixes;
    public FileFilterImpl(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s: suffixes)
        if (file.getPath().endsWith(s)) return true;
      return false;
    }
  }

  /**
   * @param dirName recurse into this directory
   * @param ff for files that pass this filter, may be null
   * @param act perform this acction
   * @return count
   * @throws IOException on IO error
   */
  public static int actOnAll(String dirName, FileFilter ff, Act act) throws IOException {
    int count = 0;

    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID "+dirName);
      return count;
    }
    List<File> flist = Arrays.asList(allFiles);
    Collections.sort(flist);

    for (File f : flist) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude"))
        count += act.doAct(name);
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude"))
        count += actOnAll(f.getAbsolutePath(), ff, act);
    }

    return count;
  }

  ////////////////////////////////////////////////

  static int max_size = 1000 * 1000 * 10;
  static Section makeSubset(Variable v) throws InvalidRangeException {
    int[] shape = v.getShape();
    shape[0] = 1;
    Section s = new Section(shape);
    long size = s.computeSize();
    shape[0] = (int) Math.max(1, max_size / size);
    return new Section(shape);
  }


}