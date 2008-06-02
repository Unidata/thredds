package ucar.nc2;

import junit.framework.*;
//import ucar.nc2.TestHTTP;

/**
 * ucar.nc2 core testing
 *
 */
public class TestNC2 {
  public static boolean dumpFile = false;

  public static NetcdfFile open( String filename) {
    try {
      System.out.println("**** Open "+filename);
      NetcdfFile ncfile = NetcdfFile.open(filename, null);
      if (TestNC2.dumpFile) System.out.println("open "+ncfile);
      return ncfile;

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert false;
      return null;
    }
  }

  public static NetcdfFile openFile( String filename) {
    return open( TestAll.cdmTestDataDir +filename);
  }

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();
    suite.addTest(new TestSuite(TestHTTP.class)); // */
    suite.addTest(new TestSuite(TestStructure.class)); //
    suite.addTest(new TestSuite(TestStructureArray.class)); //
    suite.addTest(new TestSuite(TestStructureArray2.class)); //
    suite.addTest(new TestSuite(TestCompareFileWriter.class));// */
    
    return suite;
  }

}