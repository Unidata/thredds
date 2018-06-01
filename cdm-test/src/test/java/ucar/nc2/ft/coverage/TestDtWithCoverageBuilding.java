package ucar.nc2.ft.coverage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.adapter.DtCoverage;
import ucar.nc2.ft2.coverage.adapter.DtCoverageAdapter;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCS;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.ft2.coverage.adapter.GridCS;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/**
 * Coverages built with DtCoverageDataset
 *
 * @author caron
 * @since 7/14/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestDtWithCoverageBuilding {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testScalarRuntimeCoordinate() throws IOException {

    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1.ncx4";
    try (DtCoverageDataset gds = DtCoverageDataset.open(filename)) {
      Assert.assertNotNull(filename, gds);

      String gridName =  "Pressure_surface";
      DtCoverage grid = gds.findGridByShortName(gridName);
      Assert.assertNotNull(gridName, grid);

      DtCoverageCS gridCS = grid.getCoordinateSystem();
      Assert.assertNotNull("Pressure_surface cs", gridCS);
      Assert.assertEquals("ucar.nc2.ft2.coverage.adapter.GridCS", gridCS.getClass().getName());

      CoordinateAxis1DTime runAxis = gridCS.getRunTimeAxis();
      Assert.assertNotNull("runtime axis", runAxis);
      assert runAxis.isScalar();

      Formatter errlog = new Formatter();
      try (FeatureDatasetCoverage cc = DtCoverageAdapter.factory(gds, errlog)) {
        Assert.assertNotNull(filename, cc);
        Assert.assertEquals(1, cc.getCoverageCollections().size());
        CoverageCollection cd = cc.getCoverageCollections().get(0);
        Coverage cov = cd.findCoverage(gridName);
        Assert.assertNotNull(gridName, cov);

        CoverageCoordAxis cca = cd.findCoordAxis(runAxis.getShortName());
        Assert.assertNotNull(runAxis.getShortName(), cca);
        Assert.assertEquals(CoverageCoordAxis.Spacing.regularPoint, cca.getSpacing());
      }
    }
  }

  @Test
  public void test2DRuntimeCoordinate() throws IOException {

    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km.ncx4";
    String gridName =  "TwoD/Pressure_surface";
    String covName =  "Pressure_surface";

    try (DtCoverageDataset gds = DtCoverageDataset.open(filename)) {
      Assert.assertNotNull(filename, gds);

      DtCoverage grid = gds.findGridByFullName(gridName);
      Assert.assertNotNull(gridName, grid);

      DtCoverageCS gcs = grid.getCoordinateSystem();
      Assert.assertNotNull("Pressure_surface cs", gcs);
      Assert.assertEquals("ucar.nc2.ft2.coverage.adapter.FmrcCS", gcs.getClass().getName());

      CoordinateAxis1DTime runAxis = gcs.getRunTimeAxis();
      Assert.assertNotNull("runtime axis", runAxis);
      assert !runAxis.isScalar();
      Assert.assertEquals(10, runAxis.getSize());

      CoordinateAxis timeAxis = gcs.getTimeAxis();
      Assert.assertNotNull("time axis", timeAxis);
      assert !timeAxis.isScalar();
      Assert.assertEquals(360, timeAxis.getSize());

      Formatter errlog = new Formatter();
      try (FeatureDatasetCoverage cc = DtCoverageAdapter.factory(gds, errlog)) {
        Assert.assertNotNull(filename, cc);
        Assert.assertEquals(1, cc.getCoverageCollections().size());   // LOOK only get 2D
        CoverageCollection cd = cc.findCoverageDataset(FeatureType.FMRC);
        Assert.assertNotNull(FeatureType.FMRC.toString(), cd);

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

    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km.ncx4";
    String gridName = "Best/Geopotential_height_surface";
    String covName = "Geopotential_height_surface";

    try (DtCoverageDataset gds = DtCoverageDataset.open(filename)) {
      Assert.assertNotNull(filename, gds);

      DtCoverage grid = gds.findGridByFullName(gridName);
      Assert.assertNotNull(gridName, grid);

      DtCoverageCS gcs = grid.getCoordinateSystem();
      Assert.assertNotNull("getCoordinateSystem", gcs);
      Assert.assertEquals("ucar.nc2.ft2.coverage.adapter.GridCS", gcs.getClass().getName());

      CoordinateAxis1DTime runAxis = gcs.getRunTimeAxis();
      Assert.assertNotNull("runtime axis", runAxis);
      assert !runAxis.isScalar();
      Assert.assertEquals(10, runAxis.getSize());
      double[] runValuesDt = runAxis.getCoordValues();

      CoordinateAxis1DTime timeAxis = (CoordinateAxis1DTime) gcs.getTimeAxis();
      Assert.assertNotNull("time axis", timeAxis);
      assert !timeAxis.isScalar();
      Assert.assertEquals(10, timeAxis.getSize());
      double[] timeValuesDt = timeAxis.getCoordValues();

      try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
        Assert.assertNotNull(filename, cc);
        Assert.assertEquals(2, cc.getCoverageCollections().size());
        CoverageCollection cd = cc.findCoverageDataset(FeatureType.GRID);
        Assert.assertNotNull(FeatureType.GRID.toString(), cd);

        Coverage cov = cd.findCoverage(covName);
        Assert.assertNotNull(gridName, cov);
        CoverageCoordSys csys = cov.getCoordSys();
        Assert.assertNotNull("CoverageCoordSys", csys);

        CoverageCoordAxis time = csys.getAxis(AxisType.Time);
        Assert.assertNotNull(AxisType.Time.toString(), time);
        assert !time.isScalar();
        Assert.assertEquals(10, time.getNcoords());
        double[] timeValuesGrib = time.getValues();
        for (int i=0; i<time.getNcoords(); i++)
          Assert2.assertNearlyEquals(timeValuesDt[i], timeValuesGrib[i]);

        CoverageCoordAxis runtime = csys.getAxis(AxisType.RunTime);
        Assert.assertNotNull(AxisType.RunTime.toString(), runtime);
        assert !runtime.isScalar();
        Assert.assertEquals(10, runtime.getNcoords());
        double[] runValuesGrib = runtime.getValues();
        for (int i=0; i<runtime.getNcoords(); i++)
          Assert2.assertNearlyEquals(runValuesDt[i], runValuesGrib[i]);
      }
    }
  }

  @Test
  public void testGaussianLats() throws IOException {

    String filename = TestDir.cdmUnitTestDir + "formats/grib1/cfs.wmo";

    try (DtCoverageDataset gds = DtCoverageDataset.open(filename)) {
      Assert.assertNotNull(filename, gds);
      String gridName =  "Albedo_surface_1_Month_Average";

      DtCoverage grid = gds.findGridByShortName(gridName);
      Assert.assertNotNull(gridName, grid);

      DtCoverageCS gcs = grid.getCoordinateSystem();
      Assert.assertNotNull(gridName+" cs", gcs);
      Assert.assertEquals("ucar.nc2.ft2.coverage.adapter.GridCS", gcs.getClass().getName());
      GridCS gridCS = (GridCS) gcs;

      CoordinateAxis1D latAxis = gridCS.getYHorizAxis();
      Assert.assertNotNull("latAxis axis", latAxis);
      Assert.assertTrue(!latAxis.isRegular());
      Attribute att = latAxis.findAttribute(CDM.GAUSSIAN);
      Assert.assertNotNull(att);
      Assert.assertEquals("true", att.getStringValue());

      Formatter errlog = new Formatter();
      try (FeatureDatasetCoverage cc = DtCoverageAdapter.factory(gds, errlog)) {
        Assert.assertNotNull(filename, cc);
        Assert.assertEquals(1, cc.getCoverageCollections().size());
        CoverageCollection cd = cc.getCoverageCollections().get(0);
        Coverage cov = cd.findCoverage(gridName);
        Assert.assertNotNull(gridName, cov);

        CoverageCoordAxis cca = cd.findCoordAxis(latAxis.getShortName());
        Assert.assertNotNull(latAxis.getShortName(), cca);
        Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, cca.getSpacing());
      }
    }
  }

}
