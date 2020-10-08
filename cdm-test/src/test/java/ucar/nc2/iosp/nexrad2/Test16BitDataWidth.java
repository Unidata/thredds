package ucar.nc2.iosp.nexrad2;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.ma2.MAMath.MinMax;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class Test16BitDataWidth {

  private static final String filename =
      TestDir.cdmUnitTestDir + "formats/nexrad/newLevel2/testfiles/Level2_KDDC_20201007_1914.ar2v";
  private static final double comparisonTolerance = 1e-6;


  @Test
  public void testNonPhiVar() throws IOException, InvalidRangeException {
    try (NetcdfFile ncf = NetcdfFile.open(filename)) {
      // verified against metpy
      MinMax expectedMinMax = new MinMax(0, 1058);
      Variable var = ncf.findVariable("DifferentialReflectivity_HI");
      Array data = var.read("0,:,:");
      MinMax minMax = MAMath.getMinMax(data);

      Assert.assertTrue(Math.abs(minMax.min - expectedMinMax.min) < comparisonTolerance);
      Assert.assertTrue(Math.abs(minMax.max - expectedMinMax.max) < comparisonTolerance);
    }
  }

  @Test
  public void testNonPhiVarEnhanced() throws IOException, InvalidRangeException {
    try (NetcdfDataset ncf = NetcdfDataset.openDataset(filename)) {
      // verified against metpy
      MinMax expectedMinMax = new MinMax(-13, 20);
      Variable var = ncf.findVariable("DifferentialReflectivity_HI");
      Array data = var.read("0,:,:");
      MinMax minMax = MAMath.getMinMax(data);

      Assert.assertTrue(Math.abs(minMax.min - expectedMinMax.min) < comparisonTolerance);
      Assert.assertTrue(Math.abs(minMax.max - expectedMinMax.max) < comparisonTolerance);
    }
  }
}
