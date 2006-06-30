package ucar.nc2;

import junit.framework.*;

/** Test nc2 read JUnit framework. */

public class TestN4 extends TestCase {

  public TestN4( String name) {
    super(name);
  }

  public void testReadNetcdf4() {

    NetcdfFile ncfile = TestNC2.open( "C:/data/hdf5/netcdf4/test4.nc");

    System.out.println( "**** testReadNetcdf4 done");
  }

}
