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

package ucar.nc2.iosp.hdf4;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Test reading HDF4 EOS files.
 *
 * @author caron
 * @since Oct 15, 2008
 */
@Category(NeedsCdmUnitTest.class)
public class TestH4eos {

  static public String testDir = TestDir.cdmUnitTestDir + "formats/hdf4/eos/";

  // test the coordSysBuilder - check if grid exists
  @Test
  public void testModis() throws IOException, InvalidRangeException {
    // GEO (lat//lon)
    testGridExists(testDir + "modis/MOD17A3.C5.1.GEO.2000.hdf", "MOD_Grid_MOD17A3/Data_Fields/Npp_0\\.05deg");

    // SINUSOIDAL
    testGridExists(testDir + "modis/MOD13Q1.A2012321.h00v08.005.2012339011757.hdf", "MODIS_Grid_16DAY_250m_500m_VI/Data_Fields/250m_16_days_NIR_reflectance");

  }

  private void testGridExists(String filename, String vname) throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      Variable v = ncfile.findVariable(vname);
      assert v != null : filename+" "+vname;
    }

    try (GridDataset gds = GridDataset.open(filename)) {
      GeoGrid v = gds.findGridByName(vname);
      assert v != null : filename+" "+vname;
    }

  }


  @Test
  public void testSpecificVariableSection() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(TestH4readAndCount.testDir + "96108_08.hdf")) {

      Variable v = ncfile.findVariable("CalibratedData");
      assert (null != v);
      assert v.getRank() == 3;
      int[] shape = v.getShape();
      assert shape[0] == 810;
      assert shape[1] == 50;
      assert shape[2] == 716;

      Array data = v.read("0:809:10,0:49:5,0:715:2");
      assert data.getRank() == 3;
      int[] dshape = data.getShape();
      assert dshape[0] == 810 / 10;
      assert dshape[1] == 50 / 5;
      assert dshape[2] == 716 / 2;

      // read entire array
      Array A;
      try {
        A = v.read();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }

      // compare
      Array Asection = A.section(new Section("0:809:10,0:49:5,0:715:2").getRanges());
      assert (Asection.getRank() == 3);
      for (int i = 0; i < 3; i++)
        assert Asection.getShape()[i] == dshape[i];

      ucar.unidata.util.test.CompareNetcdf.compareData(data, Asection);
    }
  }


}
