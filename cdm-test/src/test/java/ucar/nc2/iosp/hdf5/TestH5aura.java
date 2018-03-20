/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf5;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;
import java.lang.invoke.MethodHandles;

/** Test nc2 read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5aura {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  String testDir = TestH5.testDir +"auraData/";

  @org.junit.Test
  public void test1() throws IOException {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    try (NetcdfFile ncfile = TestH5.open(testDir +"HIRDLS1_v4.0.2a-aIrix-c2_2003d106.he5")) {
      Variable dset = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS_L1_Swath/Data_Fields/Scaled_Ch01_Radiance");
      assert dset != null;
      dset.read();
    }
  }

  @org.junit.Test
  public void test2() throws IOException {
    try (NetcdfFile ncfile = TestH5.open(testDir +"HIRDLS2-AFGL_b027_na.he5")) {
      Variable dset = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS/Data_Fields/Altitude");

      //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/dataBtree"));
      Array data = dset.read();
      assert data.getElementType() == float.class;
    }
  }

  @org.junit.Test
  public void testEosMetadata() throws IOException {
    //NetcdfFile ncfile = TestH5.open("c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");
    try (NetcdfFile ncfile = TestH5.open(testDir + "HIRDLS2-Aura73p_b029_2000d275.he5")) {

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
        assert (false);
        return;
      }
      assert (A.getRank() == 1);
      assert (A instanceof ArrayChar);

      ArrayChar ca = (ArrayChar) A;
      String sval = ca.getString();
      System.out.println(dset.getFullName());
      System.out.println(" Length = " + sval.length());
      System.out.println(" Value = " + sval);

      ////////////////
      dset = g.findVariable("coremetadata.0");
      assert (null != dset);
      assert (dset.getDataType() == DataType.CHAR);

      // read entire array
      try {
        A = dset.read();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 1);
      assert (A instanceof ArrayChar);

      ca = (ArrayChar) A;
      sval = ca.getString();
      System.out.println(dset.getFullName());
      System.out.println(" Length = " + sval.length());
      System.out.println(" Value = " + sval);
    }
  }

}
