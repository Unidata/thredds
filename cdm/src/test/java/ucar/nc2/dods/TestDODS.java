package ucar.nc2.dods;

import junit.framework.*;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestDODS {
  //public static String server = "http://dods.coas.oregonstate.edu:8080/dods/dts/";
  public static String server = "http://test.opendap.org:8080/dods/dts/";

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();

    // just read em and see if they weep
    suite.addTest(new TestSuite(TestDODSRead.class)); //

    // scalars and arrays
    suite.addTest(new TestSuite(TestDODSScalars.class));  // test.01 //
    suite.addTest(new TestSuite(TestDODSArrayPrimitiveTypes.class)); // test.02
    suite.addTest(new TestSuite(TestDODSMultiArrayPrimitiveTypes.class)); // test.03

    // structures
    suite.addTest(new TestSuite(TestDODSStructureScalars.class)); // test.04
    suite.addTest(new TestSuite(TestDODSStructureScalarsNested.class)); // test.05
    //suite.addTest(new TestSuite(TestDODSSubset.class)); // test.05, 02 with CE : DOESNT WORK
    suite.addTest(new TestSuite(TestDODSStructureArray.class)); // test.21

    // arrays of structure
    suite.addTest(new TestSuite(TestDODSArrayOfStructure.class)); // test.50
    suite.addTest(new TestSuite(TestDODSArrayOfStructureNested.class)); // test.53

    // grids
    suite.addTest(new TestSuite(TestDODSGrid.class));           // test.06a
    suite.addTest(new TestSuite(TestDODSGrids.class));           // test.06, 23 //
    suite.addTest(new TestSuite(TestBennoGrid.class));           // Benno grid

    // sequences
    suite.addTest(new TestSuite(TestDODSSequence.class)); // test.07, test.23
    suite.addTest(new TestSuite(TestDODSnestedSequence.class)); // nestedSeq   */
    // suite.addTest(new TestSuite(TestDODSStructureForSequence.class)); // read sequence using DODSStructure
    // 
    return suite;
  }
}