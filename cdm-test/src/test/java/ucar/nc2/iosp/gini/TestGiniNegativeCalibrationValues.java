package ucar.nc2.iosp.gini;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.MAMath;
import ucar.ma2.MAMath.MinMax;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class TestGiniNegativeCalibrationValues {

  private static final double comparisonTolerance = 1e-6;

  @Test
  public void testMinMaxValues() throws IOException {
    MinMax expectedMinMax = new MinMax(-30.0, 60.0);
    try (NetcdfFile ncfile =
        NetcdfFile.open(TestDir.cdmUnitTestDir + "formats/gini/images_sat_NEXRCOMP_1km_n0r_n0r_20200907_0740")) {
      Variable variable = ncfile.findVariable("Reflectivity");
      Array array = variable.read();
      MinMax minMax = MAMath.getMinMax(array);
      // If the bug reported in https://github.com/Unidata/netcdf-java/issues/480 shows up, and we are
      // incorrectly reading the calibration coefficients from the GINI file headers "Unidata cal block"
      // (in the case of the original bug, we were decoding negative numbers as intended), the data values
      // will be on the order of -3.9 x 10^5 for the minimum, and -2.1 x 10^5 for the maximum. Those
      // values (radar reflectivity) should be -30 dBZ to 60 dBZ.
      Assert.assertTrue(Math.abs(minMax.min - expectedMinMax.min) < comparisonTolerance);
      Assert.assertTrue(Math.abs(minMax.max - expectedMinMax.max) < comparisonTolerance);
    }
  }
}
