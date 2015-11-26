/* Copyright Unidata */
package ucar.nc2.grib.grib1;

import org.junit.Assert;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 11/23/2015.
 */
public class TestGrib1Unpack {
  // Tests reading data with complex packing
  @Test
  public void testUnpackComplexData() throws IOException {
    final String testfile = "../grib/src/test/data/complex_packing.grib1";
    NetcdfFile nc = NetcdfFile.open(testfile);

    Variable var = nc.findVariable("Minimum_temperature_at_2_metres_in_the_last_6_hours_surface_6_Hour_2");
    float[] data = (float[]) var.read().get1DJavaArray(float.class);

    Assert.assertEquals(-99., data[15], 1e-6);
    Assert.assertEquals(18.5, data[5602228], 1e-6);
  }
}
