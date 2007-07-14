package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;

/**
 * TestSuite that runs all the sample testsNew
 *
 */
public class TestH5 {
  public static boolean dumpFile = false;

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
      System.out.println("**** Open "+ TestAll.upcShareTestDataDir + "hdf5/"+filename);
      NetcdfFile ncfile = NetcdfFile.open( TestAll.upcShareTestDataDir + "hdf5/"+filename);
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
    //suite.addTest(new TestSuite(TestN4.class)); //
    suite.addTest(new TestSuite(TestH5read.class)); //
    suite.addTest(new TestSuite(TestH5ReadBasic.class)); //
    suite.addTest(new TestSuite(TestH5ReadStructure.class)); //
    suite.addTest(new TestSuite(TestH5ReadStructure2.class)); //
    suite.addTest(new TestSuite(TestH5Vlength.class)); //
    suite.addTest(new TestSuite(TestH5misc.class)); //
    suite.addTest(new TestSuite(TestH5compressed.class)); //
    // suite.addTest(new TestSuite(TestH5eos.class)); //

    return suite;
  }
}