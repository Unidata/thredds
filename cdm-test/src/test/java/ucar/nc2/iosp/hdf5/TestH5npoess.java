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

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * @author caron
 * @since Jul 18, 2007
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5npoess {

  @AfterClass
  static public void after() {
    H5header.setDebugFlags(new DebugFlagsImpl(""));  // make sure debug flags are off
  }

  // FIXME: This is a crappy test; it doesn't fail when the file can't be read.
  @Test
  public void test1() throws InvalidRangeException, IOException {
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    TestDir.readAll(TestDir.cdmUnitTestDir +"formats/hdf5/npoess/ExampleFiles/AVAFO_NPP_d2003125_t10109_e101038_b9_c2005829155458_devl_Tst.h5");
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl());
  }

  @Test
  public void test2() throws InvalidRangeException, IOException {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("npoess/ExampleFiles/AVAFO_NPP_d2003125_t10109_e101038_b9_c2005829155458_devl_Tst.h5")) {
      Variable dset = ncfile.findVariable("Data_Products/VIIRS-AF-EDR/VIIRS-AF-EDR_Gran_0");
      Array data = dset.read();
      NCdumpW.printArray(data, "data", System.out, null);
    }
  }

  @Test
  public void test3() throws InvalidRangeException, IOException {
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/reference"));
    try (NetcdfFile ncfile = TestH5.openH5("npoess/ExampleFiles/GDNBF-VNCCO_NPP_d2003125_t101038_e10116_b9_c2005829162517_dev.h5")) {
      Variable dset = ncfile.findVariable("Data_Products/VIIRS-DNB-FGEO/VIIRS-DNB-FGEO_Aggr");
      assert (null != dset);
    }
  }

  public void problem() throws InvalidRangeException, IOException {
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    try (NetcdfFile ncfile = TestH5.open("C:/data/HDF5Files/CrIMSS - CrIS - ATMS/ATMS/ATMS_SCIENCE_RDR/RASCI_npp_d20030125_t104457_e104505_b00016_c20061210190242_den_SWC.h5")) {
      Variable dset = ncfile.findVariable("Data_Products/ATMS-SCIENCE-RDR/ATMS-SCIENCE-RDR_Aggr");
      assert (null != dset);
      Array data = dset.read();
      NCdumpW.printArray(data, dset.getFullName(), System.out, null);
    }
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl());
  }

}
