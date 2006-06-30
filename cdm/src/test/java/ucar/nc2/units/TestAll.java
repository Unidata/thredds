package ucar.nc2.units;

import junit.framework.*;

import java.io.IOException;

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.TestIndexer;

/**
 * TestSuite that runs all nj22 unit tests.
 *
 */
public class TestAll {

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();
    suite.addTest(new TestSuite(TestBasic.class));
    suite.addTest(new TestSuite(TestDate.class));

    suite.addTest(new TestSuite(TestSimpleUnits.class));
    suite.addTest(new TestSuite(TestTimeUnits.class));
    suite.addTest(new TestSuite(TestDateUnits.class));

    return suite;
  }
}