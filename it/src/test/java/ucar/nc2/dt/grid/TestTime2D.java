/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.grid;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;

/**
 * Describe
 *
 * @author caron
 * @since 3/11/2015
 */
public class TestTime2D {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTime2D() throws Exception {
    try (NetcdfFile dataset = NetcdfDataset.openDataset(TestOnLocalServer.withDodsPath(
            "dodsC/gribCollection.v5/GFS_GLOBAL_2p5/TwoD"))) {
      Variable v = dataset.findVariable(null, "Pressure_surface");
      assert null != v;
      assert v.getRank() == 4;

      // bug is that
      //    Float32 Pressure_surface[reftime = 4][time = 4][lat = 73][lon = 144];
      // should be
      //   Float32 Pressure_surface[reftime = 4][time = 93][lat = 73][lon = 144];

      // dont rely on exact lengths - assert times are not equal.
      Dimension reftime = v.getDimension(0);
      Dimension time = v.getDimension(1);
      Assert.assertNotEquals(reftime.getLength(), time.getLength());
    }
  }
}
