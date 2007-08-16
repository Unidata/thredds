package ucar.nc2.ncml4;

import junit.framework.*;
import ucar.nc2.TestAll;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestNcML {
  public static String topDir = TestAll.cdmTestDataDir + "ncml/";

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();

    /* test reading XML
    suite.addTest(new TestSuite(TestNcMLequals.class)); // ncml == referenced dataset
    suite.addTest(new TestSuite(TestNcMLRead.class)); // explicit;  metadata in xml
    suite.addTest(new TestSuite(TestNcMLRead.TestRead2.class)); // readMetadata //
    suite.addTest(new TestSuite(TestNcMLWriteRead.class)); // write and read back NcML //

    suite.addTest(new TestSuite(TestNcMLReadOverride.class)); // read and override
    suite.addTest(new TestSuite(TestNcMLModifyAtts.class)); // modify atts
    suite.addTest(new TestSuite(TestNcMLModifyVars.class)); // modify vars
    suite.addTest(new TestSuite(TestNcMLRenameVar.class)); // all metadata in xml, rename vars  */

    // test aggregations
    suite.addTest(new TestSuite(TestNcmlUnionSimple.class));
    suite.addTest(new TestSuite(TestNcmlUnion.class));

    suite.addTest(new TestSuite(TestAggExistingCoordVars.class));
    suite.addTest(new TestSuite(TestNcmlAggExisting.class));
    suite.addTest(new TestSuite(TestExistingSSTA.class));

    suite.addTest(new TestSuite(TestNcmlAggSynthetic.class)); //
    suite.addTest(new TestSuite(TestNcmlAggSynGrid.class));

    return suite;
  }

}