/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.hdf5;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Class Description.
 *
 * @author caron
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5filter {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @org.junit.Test
  public void testFilterNoneApplied() throws IOException {
    // H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));

    // actually bogus - apparently all filters arre turned off
    // but its a test of filtered data with no filter actually applied
   try ( NetcdfFile ncfile = TestH5.openH5("support/zip.h5")) {
     Variable v = ncfile.findVariable("Data/Compressed_Data");
     assert v != null;
     Array data = v.read();
     int[] shape = data.getShape();
     assert shape[0] == 1000;
     assert shape[1] == 20;

     Index ima = data.getIndex();
     for (int i = 0; i < 1000; i++)
       for (int j = 0; j < 20; j++) {
         int val = data.getInt(ima.set(i, j));
         assert val == i + j : val + " != " + (i + j);
       }
   }
  }

  @org.junit.Test
  public void test2() throws IOException {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));

    // probably bogus also, cant find any non-zero filtered variables
    try (NetcdfFile ncfile = TestH5.openH5("wrf/wrf_input_seq.h5")) {
      Variable v = ncfile.findVariable("DATASET=INPUT/GSW");
      assert v != null;
      Array data = v.read();
      int[] shape = data.getShape();
      assert shape[0] == 1;
      assert shape[1] == 20;
      assert shape[2] == 10;
    }
  }

  @org.junit.Test
  public void testDeflate() throws IOException {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("msg/MSG1_8bit_HRV.H5")) {

      // picture looks ok in ToolsUI
      Variable v = ncfile.findVariable("image1/image_data");
      assert v != null;
      Array data = v.read();
      int[] shape = data.getShape();
      assert shape[0] == 1000;
      assert shape[1] == 1500;
    }
  }

  @org.junit.Test
  public void testMissing() throws IOException {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("HIRDLS/HIRDLS2-AFGL_b027_na.he5")) {

      // picture looks ok in ToolsUI
      Variable v = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS/Data_Fields/Altitude");
      assert v != null;
      Array data = v.read();
      int[] shape = data.getShape();
      assert shape[0] == 6;
      assert shape[1] == 145;
    }
  }


}
