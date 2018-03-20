/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.hdf5;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.*;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Class Description.
 *
 * @author caron
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5eos {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
