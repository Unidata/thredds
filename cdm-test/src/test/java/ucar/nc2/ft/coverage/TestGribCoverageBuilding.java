/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.coverage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/**
 * Description
 *
 * @author John
 * @since 8/17/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCoverageBuilding {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testScalarRuntimeCoordinate() throws IOException {

    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1.ncx4";
    String gridName = "Pressure_surface";

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection cd = cc.getCoverageCollections().get(0);
      Coverage cov = cd.findCoverage(gridName);
      Assert.assertNotNull(gridName, cov);
      CoverageCoordSys csys = cov.getCoordSys();
      Assert.assertNotNull("CoverageCoordSys", csys);

      CoverageCoordAxis1D runtime = (CoverageCoordAxis1D) csys.getAxis(AxisType.RunTime);
      Assert.assertNotNull(AxisType.RunTime.toString(), runtime);
      Assert.assertTrue(runtime.getClass().getName(), runtime instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.regularPoint, runtime.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.scalar, runtime.getDependenceType());
      CalendarDate startDate = runtime.makeDate(runtime.getCoordMidpoint(0));
      Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), startDate);
    }
  }

  @Test
    public void test2DTimeCoordinates() throws IOException {
      String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km.ncx4";
      String gridName = "Pressure_surface";

      try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
        Assert.assertNotNull(filename, cc);
        Assert.assertEquals(2, cc.getCoverageCollections().size());
        CoverageCollection cd = cc.findCoverageDataset(FeatureType.FMRC);
        Assert.assertNotNull(FeatureType.FMRC.toString(), cd);

        Coverage cov = cd.findCoverage(gridName);
        Assert.assertNotNull(gridName, cov);
        CoverageCoordSys csys = cov.getCoordSys();
        Assert.assertNotNull("CoverageCoordSys", csys);

        CoverageCoordAxis runtime = csys.getAxis(AxisType.RunTime);
        Assert.assertNotNull(AxisType.RunTime.toString(), runtime);
        Assert.assertTrue(runtime.getClass().getName(), runtime instanceof CoverageCoordAxis1D);
        Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, runtime.getSpacing());
        Assert.assertEquals(CoverageCoordAxis.DependenceType.independent, runtime.getDependenceType());
        Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), runtime.makeDate(0));
        Assert.assertTrue(Misc.nearlyEquals(6.0, runtime.getResolution()));

        CoverageCoordAxis time = csys.getAxis(AxisType.TimeOffset);
        Assert.assertNotNull(AxisType.TimeOffset.toString(), time);
        Assert.assertTrue(time.getClass().getName(), time instanceof TimeOffsetAxis);
        Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, time.getSpacing());
        Assert.assertEquals(CoverageCoordAxis.DependenceType.independent, time.getDependenceType());
        Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), time.makeDate(0));
        Assert.assertTrue(Misc.nearlyEquals(6.0, time.getResolution()));
        Assert.assertEquals(true, csys.isTime2D(time));
      }
    }

  @Test public void testBestTimeCoordinates() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km.ncx4";
    String gridName = "Pressure_surface";

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      Assert.assertEquals(2, cc.getCoverageCollections().size());
      CoverageCollection cd = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.GRID.toString(), cd);

      Coverage cov = cd.findCoverage(gridName);
      Assert.assertNotNull(gridName, cov);
      CoverageCoordSys csys = cov.getCoordSys();
      Assert.assertNotNull("CoverageCoordSys", csys);

      CoverageCoordAxis time = csys.getAxis(AxisType.Time);
      Assert.assertNotNull(AxisType.Time.toString(), time);
      Assert.assertTrue(time.getClass().getName(), time instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, time.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.independent, time.getDependenceType());
      Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), time.makeDate(0));
      Assert.assertTrue(Misc.nearlyEquals(6.0, time.getResolution()));
      Assert.assertEquals(false, csys.isTime2D(time));

      CoverageCoordAxis runtime = csys.getAxis(AxisType.RunTime);
      Assert.assertNotNull(AxisType.RunTime.toString(), runtime);
      Assert.assertTrue(runtime.getClass().getName(), runtime instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, runtime.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.dependent, runtime.getDependenceType());
      Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), runtime.makeDate(0));
    }
  }

  @Test
  public void testTimeOffsetSubsetWhenTimePresent() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    String gridName = "Temperature_isobaric";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection cd = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.GRID.toString(), cd);

      Coverage cov = cd.findCoverage(gridName);
      Assert.assertNotNull(gridName, cov);

      CoverageCoordSys csys = cov.getCoordSys();
      Assert.assertNotNull("CoverageCoordSys", csys);

      CoverageCoordAxis time = csys.getAxis(AxisType.Time);
      Assert.assertNotNull(AxisType.Time.toString(), time);
      Assert.assertTrue(time.getClass().getName(), time instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, time.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.independent, time.getDependenceType());
      Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), time.makeDate(0));
      Assert.assertTrue(Misc.nearlyEquals(6.0, time.getResolution()));

      CoverageCoordAxis runtime = csys.getAxis(AxisType.RunTime);
      Assert.assertNotNull(AxisType.RunTime.toString(), runtime);
      Assert.assertTrue(runtime.getClass().getName(), runtime instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.regularPoint, runtime.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.scalar, runtime.getDependenceType());
      Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), runtime.makeDate(0));
    }
  }

  @Test
  public void testGaussianLats() throws IOException {

    String filename = TestDir.cdmUnitTestDir + "formats/grib1/cfs.wmo";
    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      String gridName =  "Albedo_surface_Average";

      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection cd = cc.getCoverageCollections().get(0);
      Coverage cov = cd.findCoverage(gridName);
      Assert.assertNotNull(gridName, cov);
      CoverageCoordSys csys = cov.getCoordSys();
      Assert.assertNotNull("CoverageCoordSys", csys);

      CoverageCoordAxis latAxis = csys.getAxis(AxisType.Lat);
      Assert.assertNotNull(AxisType.RunTime.toString(), latAxis);
      Assert.assertTrue(latAxis.getClass().getName(), latAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, latAxis.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.independent, latAxis.getDependenceType());

      Attribute att = latAxis.findAttribute(CDM.GAUSSIAN);
      Assert.assertNotNull(att);
      Assert.assertEquals("true", att.getStringValue());
    }
  }

}
