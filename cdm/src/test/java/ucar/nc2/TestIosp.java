package ucar.nc2;

import junit.framework.*;

/**
 * TestSuite that runs IOSP tests
 *
 */
public class TestIosp {

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();
    suite.addTest( new TestSuite( ucar.nc2.iosp.dmsp.TestDmspIosp.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.gini.TestGini.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.nexrad2.TestNexrad2.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.nids.TestNids.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.dorade.TestDorade.class));
    return suite;
  }

}