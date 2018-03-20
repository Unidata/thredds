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
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * @author caron
 * @since Jul 18, 2007
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5npoess {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
      logger.debug(NCdumpW.toString(data, "data", null));
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
      logger.debug(NCdumpW.toString(data, dset.getFullName(), null));
    }
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl());
  }

}
