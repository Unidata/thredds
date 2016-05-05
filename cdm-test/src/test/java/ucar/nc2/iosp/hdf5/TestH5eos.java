/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp.hdf5;

import org.junit.experimental.categories.Category;
import ucar.nc2.*;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5eos {

  @org.junit.Test
  public void testStructMetadata() throws IOException {
    //NetcdfFile ncfile = TestH5.open("c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");
    try (NetcdfFile ncfile = TestH5.openH5("HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5")) {

      Group root = ncfile.getRootGroup();
      Group g = root.findGroup("HDFEOS_INFORMATION");
      Variable dset = g.findVariable("StructMetadata.0");
      assert (null != dset);
      assert (dset.getDataType() == DataType.CHAR);

      // read entire array
      Array A;
      try {
        A = dset.read();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert false;
        return;
      }
      assert (A.getRank() == 1);
      assert (A instanceof ArrayChar);

      ArrayChar ca = (ArrayChar) A;
      String sval = ca.getString();
      System.out.println(dset.getFullName());
      System.out.println(" Length = " + sval.length());
      System.out.println(" Value = " + sval);
    }
  }

  @org.junit.Test
  public void test1() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("HIRDLS/HIR2ARSP_c3_na.he5")) {
      Variable v = ncfile.findVariable("HDFEOS/SWATHS/H2SO4_H2O_Tisdale/Data_Fields/Wavenumber");
      assert v != null;
      Dimension dim = v.getDimension(0);
      assert dim != null;
      assert dim.getShortName() != null;

      assert dim.getShortName().equals("nChans");
    }
  }

  @org.junit.Test
  public void test2() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("HIRDLS/HIRDLS1_v4.0.2a-aIrix-c2_2003d106.he5")) {

      Variable v = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS_L1_Swath/Data_Fields/Elevation_Angle");
      assert v != null;
      assert v.getRank() == 4;
      assert v.getDimension(0).getShortName().equals("MaF");
      assert v.getDimension(1).getShortName().equals("MiF");
      assert v.getDimension(2).getShortName().equals("CR");
      assert v.getDimension(3).getShortName().equals("CC");

    }
  }


}
