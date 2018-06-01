package ucar.nc2.ft.coverage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCoordAxis;
import ucar.nc2.ft2.coverage.CoverageCoordAxis1D;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.GeoReferencedArray;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.ft2.coverage.TimeOffsetAxis;
import ucar.nc2.grib.collection.GribDataReader;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/**
 * Test CoverageSubsetTime, esp 2DTime
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageSubsetTime {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @BeforeClass
  public static void before() {
    GribDataReader.validator = new GribCoverageValidator();
  }

  @AfterClass
  public static void after() {
    GribDataReader.validator = null;
  }

  @Test  // there is no interval with offset value = 51
  public void testNoIntervalFound() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    logger.debug("test1Runtime1TimeOffset Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate runtime = CalendarDate.parseISOformat(null, "2015-03-01T12:00:00Z");
      params.set(SubsetParams.runtime, runtime);
      double offsetVal = 51.0;  // should fail
      params.set(SubsetParams.timeOffset, offsetVal);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);
      testGeoArray(geo, runtime, null, offsetVal);

      // should be empty, but instead its a bunch of NaNs
      assert Float.isNaN(geo.getData().getFloat(0));
    }
  }

  @Test  // 1 runtime, 1 timeOffset (Time2DCoordSys case 1a)
  public void test1Runtime1TimeOffset() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    logger.debug("test1Runtime1TimeOffset Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate runtime = CalendarDate.parseISOformat(null, "2015-03-01T06:00:00Z");
      params.set(SubsetParams.runtime, runtime);
      double offsetVal = 205.0;
      params.set(SubsetParams.timeOffset, offsetVal);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);
      testGeoArray(geo, runtime, null, offsetVal);

      // LOOK need to test data
    }
  }


  // Momentum_flux_u-component_surface_Mixed_intervals_Average runtime=2015-03-01T00:00:00Z (0) ens=0.000000 (-1) time=2015-03-06T19:30:00Z (46) vert=0.000000 (-1)
  @Test  // 1 runtime, 1 time (Time2DCoordSys case 1b)
  public void test1Runtime1TimeIntervalEdge() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    logger.debug("test1Runtime1TimeInterval Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate runtime = CalendarDate.parseISOformat(null, "2015-03-01T00:00:00Z");
      params.set(SubsetParams.runtime, runtime);
      CalendarDate time = CalendarDate.parseISOformat(null, "2015-03-06T19:30:00Z"); // (6,12), (12,18)
      params.set(SubsetParams.time, time);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);
      testGeoArray(geo, runtime, time, null);

      Array data = geo.getData();
      Index ai = data.getIndex();
      float testValue = data.getFloat(ai.set(0, 0, 3, 0));
      Assert2.assertNearlyEquals(0.244f, testValue);
    }
  }


  // Momentum_flux_u-component_surface_Mixed_intervals_Average runtime=2015-03-01T06:00:00Z (1) ens=0.000000 (-1) time=2015-03-01T12:00:00Z (1) vert=0.000000 (-1)
  @Test  // 1 runtime, 1 time (Time2DCoordSys case 1b)
  public void test1Runtime1TimeInterval() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    logger.debug("test1Runtime1TimeInterval Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate runtime = CalendarDate.parseISOformat(null, "2015-03-01T06:00:00Z");
      params.set(SubsetParams.runtime, runtime);
      CalendarDate time = CalendarDate.parseISOformat(null, "2015-03-01T11:00:00Z"); // (6,12), (12,18)
      params.set(SubsetParams.time, time);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);
      testGeoArray(geo, runtime, time, null);

      Array data = geo.getData();
      Index ai = data.getIndex();
      float testValue = data.getFloat(ai.set(0,0,2,2));
      Assert2.assertNearlyEquals(0.073f, testValue);
    }
  }

  // Slice Total_ozone_entire_atmosphere_single_layer runtime=2015-03-01T06:00:00Z (1) ens=0.000000 (-1) time=2015-03-01T12:00:00Z (2) vert=0.000000 (-1)
  @Test  // 1 runtime, 1 time (Time2DCoordSys case 1b)
  public void test1Runtime1Time() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Total_ozone_entire_atmosphere_single_layer";

    logger.debug("test1Runtime1Time Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate runtime = CalendarDate.parseISOformat(null, "2015-03-01T06:00:00Z");
      params.set(SubsetParams.runtime, runtime);
      CalendarDate time = CalendarDate.parseISOformat(null, "2015-03-01T12:00:00Z");
      params.set(SubsetParams.time, time);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);
      testGeoArray(geo, runtime, time, null);

      Array data = geo.getData();
      Index ai = data.getIndex();
      float testValue = data.getFloat(ai.set(0,0,1,0));
      Assert2.assertNearlyEquals(371.5, testValue);
    }
  }

  @Test  // 1 runtime, all times (Time2DCoordSys case 1c)
  public void testConstantRuntime() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    logger.debug("testConstantRuntime Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate runtime = CalendarDate.parseISOformat(null, "2015-03-01T12:00:00Z");
      params.set(SubsetParams.runtime, runtime);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);
      CoverageCoordSys geoCs = geo.getCoordSysForData();

      CoverageCoordAxis runtimeAxis = geoCs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(runtimeAxis);
      Assert.assertTrue(runtimeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(1, runtimeAxis.getNcoords());
      CoverageCoordAxis1D runtimeAxis1D = (CoverageCoordAxis1D) runtimeAxis;
      Assert.assertEquals("runtime coord", runtime, runtimeAxis.makeDate(runtimeAxis1D.getCoordMidpoint(0)));

      CoverageCoordAxis1D timeAxis = (CoverageCoordAxis1D) geoCs.getAxis(AxisType.TimeOffset);
      Assert.assertNotNull(timeAxis);
      Assert.assertEquals(92, timeAxis.getNcoords());
      Assert.assertEquals(CoverageCoordAxis.Spacing.discontiguousInterval, timeAxis.getSpacing());
      Assert2.assertNearlyEquals(0.0, timeAxis.getStartValue());
      Assert2.assertNearlyEquals(384.0, timeAxis.getEndValue());

      // LOOK need to test data
    }
  }

  @Test  // all runtimes, 1 timeOffset (Time2DCoordSys case 2a)
  public void testConstantOffset() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    logger.debug("testConstantOffset Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      double offsetVal = 205.0;
      params.set(SubsetParams.timeOffset, offsetVal);
      params.set(SubsetParams.runtimeAll, true);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);
      CoverageCoordSys geoCs = geo.getCoordSysForData();

      CoverageCoordAxis1D runtimeAxis = (CoverageCoordAxis1D) geoCs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(runtimeAxis);
      Assert.assertEquals(4, runtimeAxis.getNcoords());
      Assert.assertEquals(CoverageCoordAxis.Spacing.regularPoint, runtimeAxis.getSpacing());
      Assert2.assertNearlyEquals(0.0, runtimeAxis.getCoordMidpoint(0));
      Assert2.assertNearlyEquals(6.0, runtimeAxis.getResolution());

      CoverageCoordAxis timeAxis = geoCs.getAxis(AxisType.TimeOffset);
      Assert.assertNotNull(timeAxis);
      Assert.assertTrue(timeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(1, timeAxis.getNcoords());
      CoverageCoordAxis1D timeAxis1D = (CoverageCoordAxis1D) timeAxis;
      if (timeAxis.isInterval()) {
        Assert.assertTrue("time coord lower", timeAxis1D.getCoordEdge1(0) <= offsetVal);          // lower <= time
        Assert.assertTrue("time coord lower", timeAxis1D.getCoordEdge2(0) >= offsetVal);          // upper >= time

      } else {
        Assert2.assertNearlyEquals(offsetVal, timeAxis1D.getCoordMidpoint(0));
      }

      // LOOK need to test data
    }
  }

  @Test  // all runtimes, 1 time (Time2DCoordSys case 2a) not time interval
  public void testConstantForecast() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Pressure_convective_cloud_bottom";

    logger.debug("testConstantForecast Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate time = CalendarDate.parseISOformat(null, "2015-03-01T15:00:00Z");
      params.set(SubsetParams.time, time);
      params.set(SubsetParams.runtimeAll, true);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);
      CoverageCoordSys geoCs = geo.getCoordSysForData();

      CoverageCoordAxis1D runtimeAxis = (CoverageCoordAxis1D) geoCs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(runtimeAxis);
      Assert.assertEquals(3, runtimeAxis.getNcoords());
      Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, runtimeAxis.getSpacing());
      Assert2.assertNearlyEquals(0.0, runtimeAxis.getCoordMidpoint(0));
      Assert2.assertNearlyEquals(6.0, runtimeAxis.getResolution());

      CoverageCoordAxis timeAxis = geoCs.getAxis(AxisType.Time);
      if (timeAxis != null) {
        Assert.assertTrue(timeAxis instanceof CoverageCoordAxis1D);
        Assert.assertEquals(1, timeAxis.getNcoords());
        CoverageCoordAxis1D timeAxis1D = (CoverageCoordAxis1D) timeAxis;
        if (timeAxis.isInterval()) {
          CalendarDate lower = timeAxis1D.makeDate(timeAxis1D.getCoordEdge1(0));
          Assert.assertTrue("time coord lower", !lower.isAfter(time));          // lower <= time
          CalendarDate upper = timeAxis1D.makeDate(timeAxis1D.getCoordEdge2(0));
          Assert.assertTrue("time coord lower", !upper.isBefore(time));         // upper >= time

        } else {
          Assert.assertEquals("time coord", time, timeAxis1D.makeDate(timeAxis1D.getCoordMidpoint(0)));
        }
      }

      CoverageCoordAxis timeOffsetAxis = geoCs.getAxis(AxisType.TimeOffset);
      if (timeOffsetAxis != null) {
        Assert.assertTrue(timeOffsetAxis instanceof TimeOffsetAxis);
        Assert.assertEquals(3, timeOffsetAxis.getNcoords());
        Assert.assertEquals(CoverageCoordAxis.DependenceType.dependent, timeOffsetAxis.getDependenceType());
        Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, timeOffsetAxis.getSpacing());  // LOOK wrong
      }
    }
  }

  public static void testGeoArray(GeoReferencedArray geo, CalendarDate runtime, CalendarDate time, Double offsetVal) {
    CoverageCoordSys geoCs = geo.getCoordSysForData();

    CoverageCoordAxis runtimeAxis = geoCs.getAxis(AxisType.RunTime);
    Assert.assertNotNull(runtimeAxis);
    Assert.assertTrue(runtimeAxis instanceof CoverageCoordAxis1D);
    Assert.assertEquals(1, runtimeAxis.getNcoords());
    CoverageCoordAxis1D runtimeAxis1D = (CoverageCoordAxis1D) runtimeAxis;
    if (runtime != null)
      Assert.assertEquals("runtime coord", runtime, runtimeAxis.makeDate(runtimeAxis1D.getCoordMidpoint(0)));

    CoverageCoordAxis timeAxis = geoCs.getAxis(AxisType.TimeOffset);
    if (timeAxis == null) timeAxis = geoCs.getAxis(AxisType.Time);

    Assert.assertNotNull(timeAxis);
    Assert.assertTrue(timeAxis instanceof CoverageCoordAxis1D);
    Assert.assertEquals(1, timeAxis.getNcoords());
    CoverageCoordAxis1D timeAxis1D = (CoverageCoordAxis1D) timeAxis;
    if (offsetVal != null)
       time = timeAxis1D.makeDate(offsetVal);

    if (time != null) {
      if (timeAxis.isInterval()) {
        CalendarDate lower = timeAxis1D.makeDate(timeAxis1D.getCoordEdge1(0));
        Assert.assertTrue("time coord lower", !lower.isAfter(time));          // lower <= time
        CalendarDate upper = timeAxis1D.makeDate(timeAxis1D.getCoordEdge2(0));
        Assert.assertTrue("time coord lower", !upper.isBefore(time));         // upper >= time
      } else {
        Assert.assertEquals("time coord", time, timeAxis1D.makeDate(timeAxis1D.getCoordMidpoint(0)));
      }
    }

    int[] shapeCs = geoCs.getShape();
    int [] dataShape = geo.getData().getShape();

    Assert.assertArrayEquals("geo shape", shapeCs, dataShape);
  }


  ////////////////////////////////////////////////////////////////////////////////////////////
  // Best

  @Test
  public void testBestPresent() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Temperature_altitude_above_msl";

    logger.debug("testBestPresent Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      params.set(SubsetParams.timePresent, true);
      params.setVertCoord(3658.0);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);

      // should not be missing !
      assert !Float.isNaN(geo.getData().getFloat(0));

      Array data = geo.getData();
      Index ai = data.getIndex();
      float testValue = data.getFloat(ai.set(0, 0, 3, 0));
      Assert2.assertNearlyEquals(244.8f, testValue);
    }
  }

  @Test
  public void testBestTimeCoord() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Temperature_altitude_above_msl";

    logger.debug("testBestPresent Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      params.setTime(CalendarDate.parseISOformat(null, "2015-03-03T00:00:00Z"));
      params.setVertCoord(3658.0);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);

      // should not be missing !
      assert !Float.isNaN(geo.getData().getFloat(0));

      Array data = geo.getData();
      Index ai = data.getIndex();
      float testValue = data.getFloat(ai.set(0, 0, 0, 0));
      Assert2.assertNearlyEquals(244.3f, testValue);
    }
  }

  @Test
  public void testBestTimeOffsetCoord() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Temperature_altitude_above_msl";

    logger.debug("testBestPresent Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      params.setTimeOffset(48.0);
      params.setVertCoord(3658.0);
      logger.debug("  subset {}", params);

      GeoReferencedArray geo = cover.readData(params);

      // should not be missing !
      assert !Float.isNaN(geo.getData().getFloat(0));

      Array data = geo.getData();
      Index ai = data.getIndex();
      float testValue = data.getFloat(ai.set(0, 0, 3, 0));
      Assert2.assertNearlyEquals(250.5, testValue);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////
  // SRC

  @Test
  public void testSrcNoParams() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    String covName = "Temperature_isobaric";

    logger.debug("testSrcNoParams Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      logger.debug("  subset {}", params);
      GeoReferencedArray geo = cover.readData(params);

      int[] resultShape = geo.getData().getShape();
      int[] expectShape = new int[] {36, 29, 65, 93};
      Assert.assertArrayEquals("shape", expectShape, resultShape);
    }
  }

  @Test
  public void testSrcTimePresent() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    String covName = "Temperature_isobaric";

    logger.debug("testSrcTimePresent Dataset {} coverage {}", endpoint, covName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      CoverageCoordSys cs = cover.getCoordSys();
      CoverageCoordAxis timeAxis = cs.getAxis(AxisType.Time);
      Assert.assertNotNull("timeoffset axis", timeAxis);
      Assert.assertEquals(36, timeAxis.getNcoords());

      SubsetParams params = new SubsetParams();
      params.set(SubsetParams.timePresent, true);
      logger.debug("  subset {}", params);
      GeoReferencedArray geo = cover.readData(params);

      int[] resultShape = geo.getData().getShape();
      int[] expectShape = new int[] {1, 29, 65, 93};
      Assert.assertArrayEquals("shape", expectShape, resultShape);

      CoverageCoordSys geocs = geo.getCoordSysForData();
      CoverageCoordAxis toAxis2 = geocs.getAxis(AxisType.Time);
      Assert.assertNotNull("timeoffset axis", toAxis2);
      Assert.assertEquals(1, toAxis2.getNcoords());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////
  // ENsemble

  //     result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/MM_cnrm_129_red.ncml", FeatureType.FMRC, "geopotential"});


}
