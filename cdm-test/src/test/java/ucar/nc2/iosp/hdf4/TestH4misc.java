/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.hdf4;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Describe
 *
 * @author caron
 * @since 10/27/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestH4misc {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  static public String testDir = TestDir.cdmUnitTestDir + "formats/hdf4/";

  @Test
  public void testUnsigned() throws IOException, InvalidRangeException {
    String filename = testDir + "MOD021KM.A2004328.1735.004.2004329164007.hdf";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      String vname = "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr1km_RefSB";
      Variable v = ncfile.findVariable(vname);
      assert v != null : filename + " " + vname;

      Array data = v.read();
      System.out.printf(" sum =          %f%n", MAMath.sumDouble(data));

      double sum2 = 0;
      double sum3 = 0;
      int[] varShape = v.getShape();
      int[] origin = new int[3];
      int[] size = new int[]{1, varShape[1], varShape[2]};
      for (int i = 0; i < varShape[0]; i++) {
        origin[0] = i;
        Array data2D = v.read(origin, size);

        double sum = MAMath.sumDouble(data2D);
        System.out.printf("  %d sum3D =        %f%n", i, sum);
        sum2 += sum;

//      assert data2D.getRank() == 2;
        sum = MAMath.sumDouble(data2D.reduce(0));
        System.out.printf("  %d sum2D =        %f%n", i, sum);
        sum3 += sum;

        ucar.unidata.util.test.CompareNetcdf.compareData(data2D, data2D.reduce(0));
      }
      System.out.printf(" sum2D =        %f%n", sum2);
      System.out.printf(" sum2D.reduce = %f%n", sum3);
      assert sum2 == sum3;
    }
  }


}
