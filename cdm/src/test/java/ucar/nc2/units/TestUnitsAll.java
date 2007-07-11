package ucar.nc2.units;

import junit.framework.*;

/**
 * TestSuite that runs all nj22 unit tests.
 *
 */
public class TestUnitsAll {

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();
    suite.addTest(new TestSuite(TestBasic.class));
    suite.addTest(new TestSuite(TestDate.class));

    suite.addTest(new TestSuite(TestSimpleUnits.class));
    //suite.addTest(new TestSuite(TestTimeUnits.class));
    suite.addTest(new TestSuite(TestDateUnits.class));

    return suite;
  }
}