package ucar.nc2.ncml4;

import junit.framework.*;
import ucar.nc2.TestAll;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestNcMLoffsite {
  public static String topDir = TestAll.cdmTestDataDir + "ncml/";

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();

    suite.addTest(new TestSuite(TestExistingSSTA.class));

    suite.addTest(new TestSuite(TestNcmlAggDirectory.class));  //
    suite.addTest(new TestSuite(TestNcmlAggDirDateFormat.class));  // */

    suite.addTest(new TestSuite(TestNcmlAggNewSync.class));  // */

    suite.addTest(new TestSuite(TestAggFmrcNetcdf.class));  //
    suite.addTest(new TestSuite(TestAggFmrcGrib.class));  //
    suite.addTest(new TestSuite(TestAggFmrcNonuniform.class));  //

    suite.addTest(new TestSuite(TestNcmlReadGridDataset.class)); // */

    suite.addTest(new TestSuite(TestAggForecastModel.class));
    suite.addTest(new TestSuite(TestAggFmrcScan2.class));

    return suite;
  }

}