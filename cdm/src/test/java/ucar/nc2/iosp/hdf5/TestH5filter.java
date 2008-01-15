/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.iosp.hdf5;

import junit.framework.TestCase;
import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.Array;
import ucar.ma2.Index;

import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestH5filter extends TestCase {

  public TestH5filter(String name) {
    super(name);
  }

  public void testFilterNoneApplied() throws IOException {
    // H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));

    // actually bogus - apparently all filters arre turned off
    // but its a test of filtered data with no filter actually applied
    NetcdfFile ncfile = TestH5.openH5("support/zip.h5");
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

  public void test2() throws IOException {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));

    // probably bogus also, cant find any non-zero filtered variables
    NetcdfFile ncfile = TestH5.openH5("wrf/wrf_input_seq.h5");
    Variable v = ncfile.findVariable("DATASET=INPUT/GSW");
    assert v != null;
    Array data = v.read();
    int[] shape = data.getShape();
    assert shape[0] == 1;
    assert shape[1] == 20;
    assert shape[2] == 10;
  }

  public void testDeflate() throws IOException {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    NetcdfFile ncfile = TestH5.openH5("msg/MSG1_8bit_HRV.H5");

    // picture looks ok in ToolsUI
    Variable v = ncfile.findVariable("image1/image_data");
    assert v != null;
    Array data = v.read();
    int[] shape = data.getShape();
    assert shape[0] == 1000;
    assert shape[1] == 1500;
  }

  public void testMissing() throws IOException {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    NetcdfFile ncfile = TestH5.openH5("eos/HIRDLS/HIRDLS2-AFGL_b027_na.he5");

    // picture looks ok in ToolsUI
    Variable v = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS/Data Fields/Altitude");
    assert v != null;
    Array data = v.read();
    int[] shape = data.getShape();
    assert shape[0] == 6;
    assert shape[1] == 145;
  }


}
