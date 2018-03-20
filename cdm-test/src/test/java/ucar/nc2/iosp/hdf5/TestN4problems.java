/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.hdf5;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Miscellaneous test on HDF5
 *
 * @author caron
 * @since 6/9/14
 */
@Category(NeedsCdmUnitTest.class)
public class TestN4problems {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @AfterClass
  static public void after() {
    H5header.setDebugFlags(new DebugFlagsImpl(""));  // make sure debug flags are off
  }

  @Test
  public void testTiling2() throws IOException, InvalidRangeException {
    // java.lang.AssertionError: shape[2] (385) >= pt[2] (390)
    String filename = TestN4reading.testDir+"UpperDeschutes_t4p10_swemelt.nc";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      Variable v = ncfile.findVariable(null, "UpperDeschutes_t4p10_swemelt");
      Array data = v.read("8087, 150:155, 150:155");
      assert data != null;
    }
  }

  // margolis@ucar.edu
  // I really don't think this is a problem with your code
  // may be bug in HDF5 1.8.4-patch1
  @Test
  public void testTiling() throws IOException {
    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = TestN4reading.testDir+"tiling.nc4";
    try( GridDataset gridDataset = GridDataset.open(filename)) {
      GridDatatype grid = gridDataset.findGridByName("Turbulence_SIGMET_AIRMET");
      System.out.printf("grid=%s%n", grid);
      Array data = grid.readDataSlice(4, 13, 176, 216); // FAILS
      assert data != null;
    }
  }


  //@Test
  public void utestEnum() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = TestN4reading.testDir+"nc4/tst_enum_data.nc";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    Variable v = ncfile.findVariable("primary_cloud");
    Array data = v.read();
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    logger.debug(NCdumpW.toString(data, "primary_cloud", null));
    ncfile.close();
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl());
  }

  //  @Test
  public void utestEnum2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = NetcdfDataset.openFile("D:/netcdf4/tst_enum_data.nc", null);
    Variable v2 = ncfile.findVariable("primary_cloud");
    assert v2 != null;

    Array data = v2.read();
    assert data.getElementType() == byte.class;

    NetcdfDataset ncd = NetcdfDataset.openDataset("D:/netcdf4/tst_enum_data.nc");
    v2 = ncd.findVariable("primary_cloud");
    assert v2 != null;

    data = v2.read();
    assert data.getElementType() == String.class;
    ncfile.close();
  }

}
