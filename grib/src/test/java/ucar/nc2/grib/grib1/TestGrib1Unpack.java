/* Copyright Unidata */
package ucar.nc2.grib.grib1;

import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * Test misc GRIB1 unpacking
 *
 * @author caron
 * @since 11/23/2015.
 */
public class TestGrib1Unpack {

  // Tests reading data with Ecmwf extended complex packing
  @Test
  public void testEcmwfExtendedComplexData() throws IOException {
    final String testfile = "../grib/src/test/data/complex_packing.grib1";
    try (NetcdfFile nc = NetcdfFile.open(testfile)) {
      Variable var = nc.findVariable("Minimum_temperature_at_2_metres_in_the_last_6_hours_surface_6_Hour_2");
      Array data = var.read();
      float first = data.getFloat(0);

      Assert.assertEquals(264.135559, first, 1e-6);
    }
  }

  // Tests reading data with Ecmwf extended complex packing
  @Test
  public void testEcmwfExtendedComplexData2() throws IOException {
    final String testfile = "../grib/src/test/data/complex_packing2.grib1";
    try (NetcdfFile nc = NetcdfFile.open(testfile)) {

      Variable var = nc.findVariable("Snowfall_surface");
      Array data = var.read();
      float first = data.getFloat(0);

      Assert.assertEquals(.326607, first, 1e-6);
    }
  }

}
