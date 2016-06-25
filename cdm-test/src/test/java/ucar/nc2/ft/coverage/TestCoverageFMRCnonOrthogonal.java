/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
import java.util.Formatter;

/**
 * Grib 2D time that is regular, not orthogonal.
 *
 * @author caron
 * @since 10/12/2015.
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageFMRCnonOrthogonal {

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
