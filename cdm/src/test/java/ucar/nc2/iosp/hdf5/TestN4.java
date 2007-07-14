package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestNC2;
import ucar.nc2.TestAll;

import java.io.IOException;

/** Test nc2 read JUnit framework. */

public class TestN4 extends TestCase {

  public TestN4( String name) {
    super(name);
  }

  public void testReadHdf5() throws IOException {
    NetcdfFile ncfile = TestNC2.open( TestAll.upcShareTestDataDir + "hdf5/support/bool.h5");
    System.out.println( "**** testReadNetcdf4 done "+ncfile);
    ncfile.close();
  }

  public void testReadNetcdf4() throws IOException {
    NetcdfFile ncfile = TestNC2.open( TestAll.upcShareTestDataDir + "netcdf4/nc4_pres_temp_4D.nc");
    System.out.println( "**** testReadNetcdf4 done "+ncfile);
    ncfile.close();
  }

}
