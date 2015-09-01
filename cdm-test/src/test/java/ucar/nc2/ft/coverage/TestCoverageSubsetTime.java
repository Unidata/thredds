package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.AxisType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Misc;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Test CoverageSubsetTime, esp 2DTime
 */
public class TestCoverageSubsetTime {

  @Test  // 1 runtime, 1 timeOffset (Time2DCoordSys case 1a)
  public void test1Runtime1TimeOffset() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    System.out.printf("Test Dataset %s coverage %s%n", endpoint, covName);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Fmrc);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate runtime = CalendarDate.parseISOformat(null, "2015-03-01T06:00:00Z");
      params.set(SubsetParams.runtime, runtime);
      double offsetVal = 205.0;
      params.set(SubsetParams.timeOffset, offsetVal);
      GeoReferencedArray geo = cover.readData(params);
      CoverageCoordSys geoCs = geo.getCoordSysForData();

      CoverageCoordAxis runtimeAxis = geoCs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(runtimeAxis);
      Assert.assertTrue(runtimeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(1, runtimeAxis.getNcoords());
      CoverageCoordAxis1D runtimeAxis1D = (CoverageCoordAxis1D) runtimeAxis;
      Assert.assertEquals("runtime coord", runtime, runtimeAxis.makeDate(runtimeAxis1D.getCoord(0)));

      CoverageCoordAxis timeAxis = geoCs.getAxis(AxisType.TimeOffset);
      Assert.assertNotNull(timeAxis);
      Assert.assertTrue(timeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(1, timeAxis.getNcoords());
      CoverageCoordAxis1D timeAxis1D = (CoverageCoordAxis1D) timeAxis;
      CalendarDate time = timeAxis1D.makeDate(offsetVal);
      if (timeAxis.isInterval()) {
        CalendarDate lower = timeAxis1D.makeDate(timeAxis1D.getCoordEdge1(0));
        Assert.assertTrue("time coord lower", !lower.isAfter(time));          // lower <= time
        CalendarDate upper = timeAxis1D.makeDate(timeAxis1D.getCoordEdge2(0));
        Assert.assertTrue("time coord lower", !upper.isBefore(time));         // upper >= time

      }else {
        Assert.assertEquals("time coord", time, timeAxis1D.makeDate(timeAxis1D.getCoord(0)));
      }

      // LOOK need to test data
    }
  }

  @Test  // 1 runtime, 1 time (Time2DCoordSys case 1b)
  public void test1Runtime1Time() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    System.out.printf("Test Dataset %s coverage %s%n", endpoint, covName);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Fmrc);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate runtime = CalendarDate.parseISOformat(null, "2015-03-01T06:00:00Z");
      params.set(SubsetParams.runtime, runtime);
      CalendarDate time = CalendarDate.parseISOformat(null, "2015-03-06T06:00:00Z");
      params.set(SubsetParams.time, time);
      GeoReferencedArray geo = cover.readData(params);
      CoverageCoordSys geoCs = geo.getCoordSysForData();

      CoverageCoordAxis runtimeAxis = geoCs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(runtimeAxis);
      Assert.assertTrue(runtimeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(1, runtimeAxis.getNcoords());
      CoverageCoordAxis1D runtimeAxis1D = (CoverageCoordAxis1D) runtimeAxis;
      Assert.assertEquals("runtime coord", runtime, runtimeAxis.makeDate(runtimeAxis1D.getCoord(0)));

      CoverageCoordAxis timeAxis = geoCs.getAxis(AxisType.TimeOffset);
      Assert.assertNotNull(timeAxis);
      Assert.assertTrue(timeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(1, timeAxis.getNcoords());
      CoverageCoordAxis1D timeAxis1D = (CoverageCoordAxis1D) timeAxis;
      if (timeAxis.isInterval()) {
        CalendarDate lower = timeAxis1D.makeDate(timeAxis1D.getCoordEdge1(0));
        Assert.assertTrue("time coord lower", !lower.isAfter(time));          // lower <= time
        CalendarDate upper = timeAxis1D.makeDate(timeAxis1D.getCoordEdge2(0));
        Assert.assertTrue("time coord lower", !upper.isBefore(time));         // upper >= time

      }else {
        Assert.assertEquals("time coord", time, timeAxis1D.makeDate(timeAxis1D.getCoord(0)));
      }

      // LOOK need to test data
    }
  }

  @Test  // 1 runtime, all times (Time2DCoordSys case 1c)
  public void testConstantRuntime() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    System.out.printf("Test Dataset %s coverage %s%n", endpoint, covName);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Fmrc);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate runtime = CalendarDate.parseISOformat(null, "2015-03-01T12:00:00Z");
      params.set(SubsetParams.runtime, runtime);
      GeoReferencedArray geo = cover.readData(params);
      CoverageCoordSys geoCs = geo.getCoordSysForData();

      CoverageCoordAxis runtimeAxis = geoCs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(runtimeAxis);
      Assert.assertTrue(runtimeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(1, runtimeAxis.getNcoords());
      CoverageCoordAxis1D runtimeAxis1D = (CoverageCoordAxis1D) runtimeAxis;
      Assert.assertEquals("runtime coord", runtime, runtimeAxis.makeDate(runtimeAxis1D.getCoord(0)));

      CoverageCoordAxis timeAxis = geoCs.getAxis(AxisType.TimeOffset);
      Assert.assertNotNull(timeAxis);
      Assert.assertTrue(timeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(92, timeAxis.getNcoords());
      Assert.assertEquals(CoverageCoordAxis.Spacing.discontiguousInterval, timeAxis.getSpacing());
      Assert.assertEquals(0.0, timeAxis.getStartValue(), Misc.maxReletiveError);
      Assert.assertEquals(384.0, timeAxis.getEndValue(), Misc.maxReletiveError);

      // LOOK need to test data
    }
  }

  @Test  // all runtimes, 1 timeOffset (Time2DCoordSys case 2a)
  public void testConstantOffset() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    System.out.printf("Test Dataset %s coverage %s%n", endpoint, covName);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Fmrc);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      double offsetVal = 205.0;
      params.set(SubsetParams.timeOffset, offsetVal);
      params.set(SubsetParams.runtimeAll, true);
      GeoReferencedArray geo = cover.readData(params);
      CoverageCoordSys geoCs = geo.getCoordSysForData();

      CoverageCoordAxis runtimeAxis = geoCs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(runtimeAxis);
      Assert.assertTrue(runtimeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(4, runtimeAxis.getNcoords());
      Assert.assertEquals(CoverageCoordAxis.Spacing.regular, runtimeAxis.getSpacing());
      Assert.assertEquals(0.0, runtimeAxis.getStartValue(), Misc.maxReletiveError);
      Assert.assertEquals(6.0, runtimeAxis.getResolution(), Misc.maxReletiveError);

      CoverageCoordAxis timeAxis = geoCs.getAxis(AxisType.TimeOffset);
      Assert.assertNotNull(timeAxis);
      Assert.assertTrue(timeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(1, timeAxis.getNcoords());
      CoverageCoordAxis1D timeAxis1D = (CoverageCoordAxis1D) timeAxis;
      if (timeAxis.isInterval()) {
        Assert.assertTrue("time coord lower", timeAxis1D.getCoordEdge1(0) <= offsetVal);          // lower <= time
        Assert.assertTrue("time coord lower", timeAxis1D.getCoordEdge2(0) >= offsetVal);          // upper >= time

      }else {
        Assert.assertEquals("offset coord", offsetVal, timeAxis1D.getCoord(0), Misc.maxReletiveError);
      }

      // LOOK need to test data
    }
  }

  @Test  // all runtimes, 1 time (Time2DCoordSys case 2a) not time interval
  public void testConstantForecast() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3";
    String covName = "Pressure_convective_cloud_bottom";

    System.out.printf("Test Dataset %s coverage %s%n", endpoint, covName);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Fmrc);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams();
      CalendarDate time = CalendarDate.parseISOformat(null, "2015-03-01T15:00:00Z");
      params.set(SubsetParams.time, time);
      params.set(SubsetParams.runtimeAll, true);
      GeoReferencedArray geo = cover.readData(params);
      CoverageCoordSys geoCs = geo.getCoordSysForData();

      CoverageCoordAxis runtimeAxis = geoCs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(runtimeAxis);
      Assert.assertTrue(runtimeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(3, runtimeAxis.getNcoords());
      Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, runtimeAxis.getSpacing());
      Assert.assertEquals(0.0, runtimeAxis.getStartValue(), Misc.maxReletiveError);
      Assert.assertEquals(6.0, runtimeAxis.getResolution(), Misc.maxReletiveError);

      CoverageCoordAxis timeAxis = geoCs.getAxis(AxisType.Time);
      Assert.assertNotNull(timeAxis);
      Assert.assertTrue(timeAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(1, timeAxis.getNcoords());
      CoverageCoordAxis1D timeAxis1D = (CoverageCoordAxis1D) timeAxis;
      if (timeAxis.isInterval()) {
        CalendarDate lower = timeAxis1D.makeDate(timeAxis1D.getCoordEdge1(0));
        Assert.assertTrue("time coord lower", !lower.isAfter(time));          // lower <= time
        CalendarDate upper = timeAxis1D.makeDate(timeAxis1D.getCoordEdge2(0));
        Assert.assertTrue("time coord lower", !upper.isBefore(time));         // upper >= time

      }else {
        Assert.assertEquals("time coord", time, timeAxis1D.makeDate(timeAxis1D.getCoord(0)));
      }

      CoverageCoordAxis timeOffsetAxis = geoCs.getAxis(AxisType.TimeOffset);
      Assert.assertNotNull(timeOffsetAxis);
      Assert.assertTrue(timeOffsetAxis instanceof TimeOffsetAxis);
      Assert.assertEquals(3, timeAxis.getNcoords());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.dependent, timeAxis.getDependenceType());
      Assert.assertEquals(CoverageCoordAxis.Spacing.discontiguousInterval, timeAxis.getSpacing());
     // Assert.assertEquals(0.0, timeAxis.getStartValue(), Misc.maxReletiveError);
     // Assert.assertEquals(384.0, timeAxis.getEndValue(), Misc.maxReletiveError);

      // LOOK need to test data
    }
  }
}
