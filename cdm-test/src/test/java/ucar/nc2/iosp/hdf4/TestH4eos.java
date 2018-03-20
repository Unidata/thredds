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
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test reading HDF4 EOS files.
 *
 * @author caron
 * @since Oct 15, 2008
 */
@Category(NeedsCdmUnitTest.class)
public class TestH4eos {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "formats/hdf4/96108_08.hdf")) {

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
