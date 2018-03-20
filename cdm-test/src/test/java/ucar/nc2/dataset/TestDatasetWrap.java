/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 * Test things are ok when wrapping by a Dataset
 *
 * @author caron
 * @since 11/6/13
 */
@Category(NeedsCdmUnitTest.class)
public class TestDatasetWrap {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testDatasetWrap() throws Exception {
    doOne(TestDir.cdmUnitTestDir + "conventions/nuwg/eta.nc");
  }

  private void doOne(String filename) throws Exception {
    try (NetcdfFile ncfile = NetcdfDataset.acquireFile(new DatasetUrl(null, filename), null);
         NetcdfDataset ncWrap = new NetcdfDataset(ncfile, true)) {

      NetcdfDataset ncd = NetcdfDataset.acquireDataset(new DatasetUrl(null, filename), true , null);
      System.out.println(" dataset wraps= " + filename);

      ucar.unidata.util.test.CompareNetcdf.compareFiles(ncd, ncWrap);
      ncd.close();
      ncWrap.close();
    }
  }

  @Test
  public void testMissingDataReplaced() throws Exception {
    // this one has misssing longitude data, but not getting set to NaN
    String filename = TestDir.cdmUnitTestDir + "/ft/point/netcdf/Surface_Synoptic_20090921_0000.nc";
    NetcdfFile ncfile = null;
    NetcdfDataset ds = null;

    try {
      ncfile = NetcdfFile.open(filename);
      ds = NetcdfDataset.openDataset(filename);

      String varName = "Lon";
      Variable wrap = ds.findVariable(varName);
      Array data_wrap = wrap.read();

      CompareNetcdf2 compare = new CompareNetcdf2();

      assert wrap instanceof CoordinateAxis1D;
      CoordinateAxis1D axis = (CoordinateAxis1D) wrap;

      assert compare.compareData(varName, data_wrap, axis.getCoordValues());
    } finally {

      if (ncfile != null) ncfile.close();
      if (ds != null) ds.close();
    }
  }

  @Test
  public void testLongitudeWrap() throws Exception {
    // this one was getting clobbered by longitude wrapping
    String filename = TestDir.cdmUnitTestDir + "/ft/profile/sonde/sgpsondewnpnC1.a1.20020507.112400.cdf";
    NetcdfFile ncfile = null;
    NetcdfDataset ds = null;

    try {
      ncfile = NetcdfFile.open(filename);
      ds = NetcdfDataset.openDataset(filename);

      String varName = "lon";
      Variable org = ncfile.findVariable(varName);
      Variable wrap = ds.findVariable(varName);

      Array data_org = org.read();
      Array data_wrap = wrap.read();

      boolean ok;
      CompareNetcdf2 compare = new CompareNetcdf2();
      ok = compare.compareData(varName, data_org, data_wrap);

      assert wrap instanceof CoordinateAxis1D;
      CoordinateAxis1D axis = (CoordinateAxis1D) wrap;

      ok &= compare.compareData(varName, data_org, axis.getCoordValues());

      assert ok;
    } finally {

      if (ncfile != null) ncfile.close();
      if (ds != null) ds.close();
    }
  }
}
