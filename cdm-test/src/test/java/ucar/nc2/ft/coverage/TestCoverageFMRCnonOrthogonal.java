/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

/**
 * Grib 2D time that is regular, not orthogonal.
 *
 * @author caron
 * @since 10/12/2015.
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageFMRCnonOrthogonal {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testRegularTime2D() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "datasets/NDFD-CONUS-5km/NDFD-CONUS-5km.ncx4";
    String gridName = "Maximum_temperature_height_above_ground_12_Hour_Maximum";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    // FeatureType wantFeatureType, String location, ucar.nc2.util.CancelTask task, Formatter errlog
    Formatter errlog = new Formatter();
    try (FeatureDatasetCoverage fdc = (FeatureDatasetCoverage) FeatureDatasetFactoryManager.open(null, filename, null, errlog)) {
      Assert.assertNotNull(errlog.toString(), fdc);

      VariableSimpleIF vs = fdc.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      CoverageCollection cc = fdc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull(FeatureType.FMRC.toString(), cc);

      Coverage cov = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, cov);

      int[] expectShape = new int[] {4,4,1,689,1073};
      Assert.assertArrayEquals(expectShape, cov.getShape());

      CoverageCoordSys gcs = cov.getCoordSys();
      Assert.assertNotNull(gcs);

      CoverageCoordAxis reftime = gcs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(reftime);
      Assert.assertEquals(4, reftime.getNcoords());
      double[] want = new double[]{0., 12., 24., 36.};
      CompareNetcdf2 cn = new CompareNetcdf2();
      assert cn.compareData("time", reftime.getCoordsAsArray(), Array.makeFromJavaArray(want), false);

      CoverageCoordAxis time = gcs.getTimeAxis();
      Assert.assertNotNull(time);
      Assert.assertTrue(time instanceof TimeAxis2DFmrc);
      Assert.assertEquals(16, time.getNcoords());

      //double[] want = new double[]{108.000000, 132.000000, 156.000000, 180.000000};
      //assert cn.compareData("time", time.getCoordsAsArray(), Array.makeFromJavaArray(want), false);
    }
  }
}
