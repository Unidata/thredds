/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.iosp.hdf5;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
import java.io.PrintWriter;

/**
 * Miscellaneous test on HDF5
 *
 * @author caron
 * @since 6/9/14
 */
@Category(NeedsCdmUnitTest.class)
public class TestN4problems {

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
    NCdumpW.printArray(data, "primary_cloud", new PrintWriter(System.out), null);
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
