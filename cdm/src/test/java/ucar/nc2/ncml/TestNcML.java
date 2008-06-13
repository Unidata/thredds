package ucar.nc2.ncml;

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

    //test reading XML
    suite.addTest(new TestSuite(TestNcMLequals.class)); // ncml == referenced dataset
    suite.addTest(new TestSuite(TestNcMLRead.class)); // explicit;  metadata in xml
    suite.addTest(new TestSuite(TestNcMLRead.TestRead2.class)); // readMetadata //

    suite.addTest(new TestSuite(TestNcMLReadOverride.class)); // read and override
    suite.addTest(new TestSuite(TestNcMLModifyAtts.class)); // modify atts
    suite.addTest(new TestSuite(TestNcMLModifyVars.class)); // modify vars
    suite.addTest(new TestSuite(TestNcMLRenameVar.class)); // all metadata in xml, rename vars  */

    // test aggregations
    suite.addTest(new TestSuite(TestAggUnionSimple.class));
    suite.addTest(new TestSuite(TestAggUnion.class));

    suite.addTest(new TestSuite(TestAggExistingCoordVars.class));
    suite.addTest(new TestSuite(TestAggExisting.class));

    suite.addTest(new TestSuite(TestAggSynthetic.class)); //

    suite.addTest(new TestSuite(TestAggExistingPromote.class));

    // requires remote (network) access
    // suite.addTest(new TestSuite(TestRemoteCrawlableDataset.class));

    // LOOK wait until grids are done
    //suite.addTest(new TestSuite(TestAggSynGrid.class));

    return suite;
  }

}