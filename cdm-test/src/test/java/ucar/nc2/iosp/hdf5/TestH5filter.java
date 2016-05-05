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
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5filter {

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
