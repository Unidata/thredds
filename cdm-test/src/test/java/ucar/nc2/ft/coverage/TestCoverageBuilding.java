/* Copyright */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.adapter.*;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 7/14/2015
 */
public class TestCoverageBuilding {

  @Test
  public void testScalarRuntimeCoordinate() throws IOException {

    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1.ncx3";
    try (DtCoverageDataset gds = DtCoverageDataset.open(filename)) {
      Assert.assertNotNull(filename, gds);

      String gridName =  "Pressure_surface";
      DtCoverage grid = gds.findGridByShortName(gridName);
      Assert.assertNotNull(gridName, grid);

      DtCoverageCS gcs = grid.getCoordinateSystem();
      Assert.assertNotNull("Pressure_surface cs", gcs);
      Assert.assertEquals("ucar.nc2.ft2.coverage.adapter.GridCS", gcs.getClass().getName());
      GridCS gridCS = (GridCS) gcs;

      CoordinateAxis1DTime runAxis = gridCS.getRunTimeAxis();
      Assert.assertNotNull("runtime axis", runAxis);
      assert runAxis.isScalar();

      try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(filename)) {
        Assert.assertNotNull(filename, cc);
        Assert.assertEquals(1, cc.getCoverageDatasets().size());
        CoverageDataset cd = cc.getCoverageDatasets().get(0);
        Coverage cov = cd.findCoverage(gridName);
        Assert.assertNotNull(gridName, cov);

        CoverageCoordAxis cca = cd.findCoordAxis(runAxis.getShortName());
        Assert.assertNotNull(runAxis.getShortName(), cca);
        Assert.assertEquals(CoverageCoordAxis.Spacing.regular, cca.getSpacing());
      }
    }
  }

  @Test
  public void test2DRuntimeCoordinate() throws IOException {

    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km.ncx3";
    try (DtCoverageDataset gds = DtCoverageDataset.open(filename)) {
      Assert.assertNotNull(filename, gds);

      String gridName =  "TwoD/Pressure_surface";
      DtCoverage grid = gds.findGridByFullName(gridName);
      Assert.assertNotNull(gridName, grid);

      DtCoverageCS gcs = grid.getCoordinateSystem();
      Assert.assertNotNull("Pressure_surface cs", gcs);
      Assert.assertEquals("ucar.nc2.ft2.coverage.adapter.FmrcCS", gcs.getClass().getName());
      FmrcCS fmrcCS = (FmrcCS) gcs;

      CoordinateAxis1DTime runAxis = fmrcCS.getRunTimeAxis();
      Assert.assertNotNull("runtime axis", runAxis);
      assert !runAxis.isScalar();
      Assert.assertEquals(10, runAxis.getSize());

      CoordinateAxis2D timeAxis = fmrcCS.getTimeAxis();
      Assert.assertNotNull("time axis", timeAxis);
      assert !runAxis.isScalar();
      Assert.assertEquals(360, timeAxis.getSize());

      try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(filename)) {
        Assert.assertNotNull(filename, cc);
        Assert.assertEquals(1, cc.getCoverageDatasets().size());
        CoverageDataset cd = cc.getCoverageDatasets().get(0);
        Coverage cov = cd.findCoverage(gridName);
        Assert.assertNotNull(gridName, cov);

        CoverageCoordAxis ccaRuntime = cd.findCoordAxis(runAxis.getFullName());
        Assert.assertNotNull(runAxis.getFullName(), ccaRuntime);
        Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, ccaRuntime.getSpacing());

        CoverageCoordAxis ccaTime = cd.findCoordAxis(timeAxis.getFullName());
        Assert.assertNotNull(timeAxis.getFullName(), ccaTime);
        Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, ccaTime.getSpacing());
      }
    }
  }
}
