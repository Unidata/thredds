package ucar.nc2.ncml;

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
    
    suite.addTest(new TestSuite(TestOffAggExistingTimeUnitsChange.class));

    suite.addTest(new TestSuite(TestOffNcMLWriteRead.class)); // write and read back NcML //
    suite.addTest(new TestSuite(TestOffNcMLWriteReadwithCoords.class)); // write and read back NcML //

    suite.addTest(new TestSuite(TestOffAggExistingSSTA.class));
    suite.addTest(new TestSuite(TestOffAggDirectory.class));  //
    suite.addTest(new TestSuite(TestOffAggDirDateFormat.class));  // */
    suite.addTest(new TestSuite(TestOffAggReadGridDataset.class)); // */

    suite.addTest(new TestSuite(TestOffAggNewSync.class));  // */

    suite.addTest(new TestSuite(TestOffAggFmrcNetcdf.class));  //
    suite.addTest(new TestSuite(TestOffAggFmrcGrib.class));  //
    suite.addTest(new TestSuite(TestOffAggFmrcNonuniform.class));  //

    suite.addTest(new TestSuite(TestOffAggForecastModel.class));
    suite.addTest(new TestSuite(TestOffAggFmrcScan2.class));

    return suite;
  }

}