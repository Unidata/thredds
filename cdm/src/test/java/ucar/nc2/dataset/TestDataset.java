package ucar.nc2.dataset;

import junit.framework.*;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestDataset {
  public static String topDir = "test/data/";
  public static String xmlDir = topDir+"dataset/xml/";
  public static String writeDir = topDir+"tmp/";

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();

    suite.addTest(new TestSuite(TestJustRead.class));
    suite.addTest(new TestSuite(TestStandardVar.class));
    suite.addTest(new TestSuite(TestDatasetWrap.class));

    suite.addTest(new TestSuite(TestCoordinates.class));

    return suite;
  }
}