package ucar.nc2.dataset;

import junit.framework.*;
import ucar.nc2.TestAll;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestDataset {
  public static String writeDir = TestAll.temporaryDataDir;

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();

    suite.addTest(new TestSuite(TestJustRead.class));
    suite.addTest(new TestSuite(TestStandardVar.class));
    suite.addTest(new TestSuite(TestSectionFillValue.class));
    suite.addTest(new TestSuite(TestDatasetWrap.class));

    suite.addTest(new TestSuite(TestCoordinates.class));
    suite.addTest(new TestSuite(TestTransforms.class));
    suite.addTest(new TestSuite(TestNestedConvert.class));

    return suite;
  }
}