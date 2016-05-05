/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp.hdf4;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 10/27/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestH4misc {
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
