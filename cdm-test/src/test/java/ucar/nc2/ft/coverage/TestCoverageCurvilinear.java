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
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.GeoReferencedArray;
import ucar.nc2.ft2.coverage.HorizCoordSys;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/**
 * Description
 *
 * @author John
 * @since 8/24/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageCurvilinear {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void TestGribCurvilinear() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";  // GRIB Curvilinear
    logger.debug("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());
      Assert.assertEquals(7, gds.getCoverageCount());

      HorizCoordSys hcs = gds.getHorizCoordSys();
      Assert.assertNotNull(endpoint, hcs);
      Assert.assertTrue(endpoint, !hcs.isProjection());
      Assert.assertNull(endpoint, hcs.getTransform());

      String covName = "Mixed_layer_depth_surface";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      GeoReferencedArray geo = cover.readData(new SubsetParams());
      TestCoverageSubsetTime.testGeoArray(geo, null, null, null);
    }
  }

  @Test
  public void TestGribCurvilinearSubset() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";  // GRIB Curvilinear
    logger.debug("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());
      Assert.assertEquals(7, gds.getCoverageCount());

      HorizCoordSys hcs = gds.getHorizCoordSys();
      Assert.assertNotNull(endpoint, hcs);
      Assert.assertTrue(endpoint, !hcs.isProjection());
      Assert.assertNull(endpoint, hcs.getTransform());

      String covName = "Mixed_layer_depth_surface";
      Coverage coverage = gds.findCoverage(covName);
      Assert.assertNotNull(covName, coverage);

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(64.0, -61.), new LatLonPointImpl(59.0, -52.));

      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true).set(SubsetParams.latlonBB, bbox);
      GeoReferencedArray geo = coverage.readData(params);
      logger.debug("csys shape={}", Misc.showInts(geo.getCoordSysForData().getShape()));

      Array data = geo.getData();
      logger.debug("data shape={}", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(geo.getCoordSysForData().getShape(), data.getShape());

      int[] expectedShape = new int[] {1, 165, 161};
      Assert.assertArrayEquals(expectedShape, data.getShape());
    }
  }

  @Test
  public void TestNetcdfCurvilinear() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc";  // NetCDF has 2D and 1D
    logger.debug("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());
      Assert.assertEquals(20, gds.getCoverageCount());

      String covName = "u";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams().setVertCoord(-.05).set(SubsetParams.timePresent, true);
      GeoReferencedArray geo = cover.readData(params);

      Array data = geo.getData();
      Index ima = data.getIndex();
      int[] expectedShape = new int[] {1,1,22,12};
      Assert.assertArrayEquals(expectedShape, data.getShape());
      Assert2.assertNearlyEquals(0.0036624447, data.getDouble(ima.set(0, 0, 0, 0)), 1e-6);
      Assert2.assertNearlyEquals(0.20564626, data.getDouble(ima.set(0, 0, 21, 11)), 1e-6);
    }
  }

  @Test
  public void TestNetcdfCurvilinear2D() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";  // NetCDF Curvilinear 2D only
    logger.debug("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());
      Assert.assertEquals(10, gds.getCoverageCount());

      String covName = "hs";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true);
      GeoReferencedArray geo = cover.readData(params);

      Array data = geo.getData();
      Index ima = data.getIndex();
      int[] expectedShape = new int[] {1,151,171};
      Assert.assertArrayEquals(expectedShape, data.getShape());
      Assert2.assertNearlyEquals(1.782, data.getDouble(ima.set(0, 0, 0)), 1e-6);
      Assert2.assertNearlyEquals(1.769, data.getDouble(ima.set(0, 11, 0)), 1e-6);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void TestNetcdfCurvilinear2Dsubset() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";  // NetCDF Curvilinear 2D only
    logger.debug("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());
      Assert.assertEquals(10, gds.getCoverageCount());

      String covName = "hs";
      Coverage coverage = gds.findCoverage(covName);
      Assert.assertNotNull(covName, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("coordSys", 3, cs.getShape().length);
      logger.debug("org shape={}", Misc.showInts(cs.getShape()));
      int[] expectedOrgShape = new int[] {85,151,171};
      Assert.assertArrayEquals(expectedOrgShape, cs.getShape());

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(43.489, -8.5353), new LatLonPointImpl(43.371, -8.2420));

      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true).setLatLonBoundingBox(bbox);
      GeoReferencedArray geo = coverage.readData(params);
      logger.debug("geoCs shape={}", Misc.showInts(geo.getCoordSysForData().getShape()));

      Array data = geo.getData();
      logger.debug("data shape={}", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(geo.getCoordSysForData().getShape(), data.getShape());

      int[] expectedShape = new int[] {1,99,105};
      Assert.assertArrayEquals(expectedShape, data.getShape());
      Index ima = data.getIndex();
      Assert2.assertNearlyEquals(1.781999945640564, data.getDouble(ima.set(0,0,0)));
      Assert2.assertNearlyEquals(1.7690000534057617, data.getDouble(ima.set(0,11,0)));
    }
  }


  @Test
  public void testNetcdf2D() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc";
    logger.debug("open {}", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.CURVILINEAR);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "salt";
      Coverage coverage = gcs.findCoverage(gribId);
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);

      int[] expectedOrgShape = new int[] {1,20,64,128};
      Assert.assertArrayEquals(expectedOrgShape, cs.getShape());
      logger.debug("org shape={}", Misc.showInts(cs.getShape()));

      // just try to bisect ot along the width
      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(90, -180), new LatLonPointImpl(-90, -90));

      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true).set(SubsetParams.latlonBB, bbox);
      GeoReferencedArray geo = coverage.readData(params);
      logger.debug("geoCs shape={}", Misc.showInts(geo.getCoordSysForData().getShape()));

      Array data = geo.getData();
      logger.debug("data shape={}", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(geo.getCoordSysForData().getShape(), data.getShape());

      int[] expectedShape = new int[] {1,20, 64, 75};
      Assert.assertArrayEquals(expectedShape, data.getShape());
    }
  }
}
