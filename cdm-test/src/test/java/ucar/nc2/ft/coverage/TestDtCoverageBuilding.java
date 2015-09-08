/* Copyright */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.adapter.*;
import ucar.nc2.util.Misc;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Coverages built with DtCoverageDataset
 *
 * @author caron
 * @since 7/14/2015
 */
public class TestDtCoverageBuilding {

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

      try (CoverageDatasetCollection cc = DtCoverageAdapter.factory(gds)) {
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

      CoordinateAxis timeAxis = fmrcCS.getTimeAxis();
      Assert.assertNotNull("time axis", timeAxis);
      assert !timeAxis.isScalar();
      Assert.assertEquals(360, timeAxis.getSize());

      try (CoverageDatasetCollection cc = DtCoverageAdapter.factory(gds)) {
        Assert.assertNotNull(filename, cc);
        Assert.assertEquals(1, cc.getCoverageDatasets().size());   // LOOK only get 2D
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

  @Test
  public void testBestRuntimeCoordinateDtvsGrib() throws IOException {

    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km.ncx3";
    try (DtCoverageDataset gds = DtCoverageDataset.open(filename)) {
      Assert.assertNotNull(filename, gds);

      String gridName =  "Best/Temperature_isobaric";
      DtCoverage grid = gds.findGridByFullName(gridName);
      Assert.assertNotNull(gridName, grid);

      DtCoverageCS gcs = grid.getCoordinateSystem();
      Assert.assertNotNull("getCoordinateSystem", gcs);
      Assert.assertEquals("ucar.nc2.ft2.coverage.adapter.GridCS", gcs.getClass().getName());
      GridCS gridCS = (GridCS) gcs;

      CoordinateAxis1DTime runAxis = gridCS.getRunTimeAxis();
      Assert.assertNotNull("runtime axis", runAxis);
      assert !runAxis.isScalar();
      Assert.assertEquals(46, runAxis.getSize());
      double[] runValuesDt = runAxis.getCoordValues();

      CoordinateAxis1DTime timeAxis = gridCS.getTimeAxis();
      Assert.assertNotNull("time axis", timeAxis);
      assert !timeAxis.isScalar();
      Assert.assertEquals(46, timeAxis.getSize());
      double[] timeValuesDt = timeAxis.getCoordValues();

      try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(filename)) {
        Assert.assertNotNull(filename, cc);
        Assert.assertEquals(2, cc.getCoverageDatasets().size());
        CoverageDataset cd = cc.findCoverageDataset(CoverageCoordSys.Type.Grid);
        Assert.assertNotNull(CoverageCoordSys.Type.Grid.toString(), cd);

        String gridNameCov =  "Temperature_isobaric";
        Coverage cov = cd.findCoverage(gridNameCov);
        Assert.assertNotNull(gridNameCov, cov);
        CoverageCoordSys csys = cov.getCoordSys();
        Assert.assertNotNull("CoverageCoordSys", csys);

        CoverageCoordAxis time = csys.getAxis(AxisType.Time);
        Assert.assertNotNull(AxisType.Time.toString(), time);
        assert !time.isScalar();
        Assert.assertEquals(46, time.getNcoords());
        double[] timeValuesGrib = time.getValues();
        for (int i=0; i<time.getNcoords(); i++)
          Assert.assertEquals(timeValuesDt[i], timeValuesGrib[i], Misc.maxReletiveError);

        CoverageCoordAxis runtime = csys.getAxis(AxisType.RunTime);
        Assert.assertNotNull(AxisType.RunTime.toString(), runtime);
        assert !runtime.isScalar();
        Assert.assertEquals(46, runtime.getNcoords());
        double[] runValuesGrib = runtime.getValues();
        for (int i=0; i<runtime.getNcoords(); i++)
          Assert.assertEquals(runValuesDt[i], runValuesGrib[i], Misc.maxReletiveError);

      }
    }
  }

}
