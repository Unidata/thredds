package ucar.nc2.dt;

import junit.framework.*;

/**
 * TestSuite that runs all ucar.nc2.dt unit tests.
 *
 */
public class TestTypedDatasets {

  public static Test suite ( ) {
    TestSuite suite= new TestSuite();

    suite.addTestSuite( ucar.nc2.dt.point.TestPointDataset.class);
    suite.addTestSuite( ucar.nc2.dt.point.TestStationDataset.class);
    suite.addTestSuite( ucar.nc2.dt.point.TestScaleOffsetMissing.class);

    suite.addTest( ucar.nc2.dt.trajectory.TestTrajectoryObsDataset.suite());

    // suite.addTestSuite( ucar.nc2.dt.radial.TestRadialDataset.class);

    return suite;
  }
}