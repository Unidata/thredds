package ucar.nc2.ft.coverage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.collection.GribDataReader;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Mostly checks GeoReferencedArray matches request.
 * Requires that the data exist, not just the gbx9.
 *
 * @author John
 * @since 8/15/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGribCoverageSubsetP {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @BeforeClass
  public static void before() {
    GribDataReader.validator = new GribCoverageValidator();
  }

  @AfterClass
  public static void after() {
    GribDataReader.validator = null;
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    ////////////// dt
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", "RH",
            null, "2003-06-12T22:00:00Z", null, 400.0});   // projection, no reftime, no timeOffset

    ////////////// grib
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1", "Pressure_surface",
            "2012-02-27T00:00:00Z", null, 42.0, null});   // projection, scalar reftime

    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", "Momentum_flux_u-component_surface_Mixed_intervals_Average",
            "2015-03-01T06:00:00Z", "2015-03-01T12:00:00Z", null, null});   // time

    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", "Momentum_flux_u-component_surface_Mixed_intervals_Average",
            "2015-03-01T06:00:00Z", null, 213.0, null});                 // time offset

    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", "Ozone_Mixing_Ratio_isobaric",
            "2015-03-01T06:00:00Z", null, 213.0, null});   // all levels

    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", "Ozone_Mixing_Ratio_isobaric",
            "2015-03-01T06:00:00Z", null, 213.0, 10000.});   // specific level
// */
    return result;
  }

  String endpoint, covName;
  CalendarDate rt_val, time_val;
  Double time_offset, vert_level;

  public TestGribCoverageSubsetP(String endpoint, String covName, String rt_val, String time_val, Double time_offset, Double vert_level) {
    this.endpoint = endpoint;
    this.covName = covName;
    this.rt_val = (rt_val == null) ? null : CalendarDate.parseISOformat(null, rt_val);
    this.time_val = (time_val == null) ? null : CalendarDate.parseISOformat(null, time_val);
    this.time_offset = time_offset;
    this.vert_level = vert_level;
  }

  @Test
  public void testGridCoverageDatasetFmrc() throws IOException, InvalidRangeException {

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.FMRC);
      if (gcs == null) return;
      logger.debug("testGridCoverageDatasetFmrc {}", endpoint);

      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      readOne(cover, rt_val, time_val, time_offset, vert_level);
    }
  }

  @Test
  public void testGridCoverageDatasetBest() throws IOException, InvalidRangeException {
    logger.debug("testGridCoverageDatasetBest {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      // Cant subset on runtime for Best, so we set to null
      readOne(cover, null, time_val, time_offset, vert_level);
    }
  }

  @Test
  public void testFmrcStride() throws IOException, InvalidRangeException {

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.FMRC);
      if (gcs == null) return; // not all datasets have an Fmrc
      logger.debug("testFmrcStride {}", endpoint);

      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      CoverageCoordSys csys = cover.getCoordSys();
      int[] csysShape =  csys.getShape();
      logger.debug("csys shape = {}", Misc.showInts(csysShape));

      SubsetParams params = new SubsetParams().setHorizStride(2).set(SubsetParams.runtimeAll, true);
      Optional<CoverageCoordSys> opt = csys.subset(params);
      if (!opt.isPresent()) {
        logger.debug("err={}", opt.getErrorMessage());
        assert false;
      }

      CoverageCoordSys subsetCoordSys = opt.get();
      int[] subsetShape =  subsetCoordSys.getShape();
      logger.debug("csysSubset shape = {}", Misc.showInts(subsetShape));

      int n = csysShape.length;
      csysShape[n-1] = (csysShape[n-1]+1)/2;
      csysShape[n-2] = (csysShape[n-2]+1)/2;

      Assert.assertArrayEquals(csysShape, subsetShape);
    }
  }

  @Test
  public void testBestStride() throws IOException, InvalidRangeException {
    logger.debug("testBestStride {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      CoverageCoordSys csys = cover.getCoordSys();
      int[] csysShape =  csys.getShape();
      logger.debug("csys shape = {}", Misc.showInts(csysShape));

      SubsetParams params = new SubsetParams().setHorizStride(2);
      Optional<CoverageCoordSys> opt = csys.subset(params);
      if (!opt.isPresent()) {
        logger.debug("err={}", opt.getErrorMessage());
        assert false;
      }

      CoverageCoordSys subsetCoordSys = opt.get();
      int[] subsetShape =  subsetCoordSys.getShape();
      logger.debug("csysSubset shape = {}", Misc.showInts(subsetShape));

      int n = csysShape.length;
      csysShape[n-1] = (csysShape[n-1]+1)/2;
      csysShape[n-2] = (csysShape[n-2]+1)/2;

      Assert.assertArrayEquals(csysShape, subsetShape);
    }
  }

  static void readOne(Coverage cover, CalendarDate rt_val, CalendarDate time_val, Double time_offset, Double vert_level) throws IOException, InvalidRangeException {
    logger.debug("===Request Subset {} runtime={} time={} timeOffset={} vert={}", cover.getName(), rt_val, time_val, time_offset, vert_level);

    SubsetParams subset = new SubsetParams();
    if (rt_val != null)
      subset.setRunTime(rt_val);
    if (time_val != null)
      subset.setTime(time_val);
    if (time_offset != null)
      subset.setTimeOffset(time_offset);
    if (vert_level != null)
      subset.setVertCoord(vert_level);

    GeoReferencedArray geoArray = cover.readData(subset);
    CoverageCoordSys geoCs = geoArray.getCoordSysForData();
    logger.debug("{}\n", geoArray);
    logger.debug("geoArray shape={}", Misc.showInts(geoArray.getData().getShape()));

    if (rt_val != null) {
      CoverageCoordAxis1D runAxis = (CoverageCoordAxis1D) geoCs.getAxis(AxisType.RunTime);
      if (runAxis != null) {
        Assert.assertEquals(1, runAxis.getNcoords());
        double val = runAxis.getCoordMidpoint(0);
        CalendarDate runDate = runAxis.makeDate(val);
        Assert.assertEquals(rt_val, runDate);
      }
    }

    if (time_val != null || time_offset != null) {
      CoverageCoordAxis timeAxis = geoCs.getAxis(AxisType.TimeOffset);
      if (timeAxis != null) {
        TimeOffsetAxis timeOffsetAxis = (TimeOffsetAxis) timeAxis;
        CoverageCoordAxis1D runAxis = (CoverageCoordAxis1D) geoCs.getAxis(AxisType.RunTime);
        Assert.assertNotNull(AxisType.RunTime.toString(), runAxis);
        Assert.assertEquals(1, runAxis.getNcoords());
        double val = runAxis.getCoordMidpoint(0);
        CalendarDate runDate = runAxis.makeDate(val);
        if (rt_val != null)
          Assert.assertEquals(rt_val, runDate);
        Assert.assertEquals(1, timeOffsetAxis.getNcoords());

        if (time_val != null) {
          if (timeOffsetAxis.isInterval()) {
            CalendarDate edge1 = timeOffsetAxis.makeDate(timeOffsetAxis.getCoordEdge1(0));
            CalendarDate edge2 = timeOffsetAxis.makeDate(timeOffsetAxis.getCoordEdge2(0));

            Assert.assertTrue(edge1+">"+time_val, !edge1.isAfter(time_val));
            Assert.assertTrue(edge2+"<"+time_val, !edge2.isBefore(time_val));

          } else {
            double val2 = timeOffsetAxis.getCoordMidpoint(0);
            CalendarDate forecastDate = timeOffsetAxis.makeDate(runDate, val2);
            Assert.assertEquals(time_val, forecastDate);
          }

        } else {
          if (timeOffsetAxis.isInterval()) {
            Assert.assertTrue(timeOffsetAxis.getCoordEdge1(0) <= time_offset);
            Assert.assertTrue(timeOffsetAxis.getCoordEdge2(0) >= time_offset);

          } else {
            double val2 = timeOffsetAxis.getCoordMidpoint(0);
            Assert2.assertNearlyEquals(val2, time_offset);
          }
        }
      }

      if (vert_level != null) {
        CoverageCoordAxis zAxis = geoCs.getZAxis();
        Assert.assertNotNull(AxisType.Pressure.toString(), zAxis);
        Assert.assertEquals(1, zAxis.getNcoords());
        double val = ((CoverageCoordAxis1D) zAxis).getCoordMidpoint(0);
        Assert2.assertNearlyEquals(vert_level.doubleValue(), val);
      }
    }
  }
}
