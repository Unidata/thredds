/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.writer.CFGridCoverageWriter2;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test coverage.getSizeInBytes()
 *
 * @author caron
 * @since 10/6/2015.
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageMisc {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void TestCoverageSize() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    logger.info("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      String covName = "Temperature_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(25243920, size);

      covName = "Relative_humidity_layer_between_two_sigmas_layer";
      cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      size = cover.getSizeInBytes();
      Assert.assertEquals(870480, size);
    }
  }

  @Test
  public void TestCFWriterCoverageSize() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    logger.info("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      // CFGridCoverageWriter2 adds another (dependent) time coordinate, so we need to test this case
      ucar.nc2.util.Optional<Long> opt = CFGridCoverageWriter2.writeOrTestSize(gds, Lists.newArrayList("Temperature_isobaric"),
              new SubsetParams(), false, true, null);
      Assert.assertTrue(opt.isPresent());

      long size = opt.get();
      Assert.assertEquals(25245084, size);  // Includes sizes of non-coverage variables.
    }
  }

  @Test
  public void TestCoverageSubsetWithFullLatlonBounds() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    logger.info("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      String covName = "Temperature_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(25243920, size);

      CoverageCoordSys csys = cover.getCoordSys();
      LatLonRect llbb = gds.getLatlonBoundingBox();
      ProjectionRect projBB = gds.getProjBoundingBox();
      ProjectionImpl proj = csys.getProjection();
      ProjectionRect projBB2 = proj.latLonToProjBB(llbb);
      logger.info("ProjRect = {}", projBB);
      logger.info("LatLonBB = {}", llbb);
      logger.info("ProjRect2 = {}", projBB2);

      SubsetParams subset = new SubsetParams().setLatLonBoundingBox(gds.getLatlonBoundingBox()); // should be the same!
      Optional<CoverageCoordSys> opt = csys.subset(subset);
      Assert.assertTrue(opt.isPresent());

      CoverageCoordSys csyss = opt.get();
      Assert.assertEquals(csys.getXAxis().getNcoords(), csyss.getXAxis().getNcoords());
      Assert.assertEquals(csys.getYAxis().getNcoords(), csyss.getYAxis().getNcoords());
    }
  }

  @Test
  public void TestCoverageSubsetWithFullLatlonBoundsPS() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds/ncep/DGEX_Alaska_12km_20100524_0000.grib2"; // Polar stereographic
    logger.info("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      String covName = "Pressure_surface";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(6433128, size);

      CoverageCoordSys csys = cover.getCoordSys();
      LatLonRect llbb = gds.getLatlonBoundingBox();
      ProjectionRect projBB = gds.getProjBoundingBox();
      ProjectionImpl proj = csys.getProjection();
      ProjectionRect projBB2 = proj.latLonToProjBB(llbb);
      logger.info("ProjRect = {}", projBB);
      logger.info("LatLonBB = {}", llbb);
      logger.info("ProjRect2 = {}", projBB2);

      SubsetParams subset = new SubsetParams().setLatLonBoundingBox(gds.getLatlonBoundingBox()); // should be the same!
      Optional<CoverageCoordSys> opt = csys.subset(subset);
      Assert.assertTrue(opt.isPresent());

      CoverageCoordSys csyss = opt.get();
      Assert.assertEquals(csys.getXAxis().getNcoords(), csyss.getXAxis().getNcoords());
      Assert.assertEquals(csys.getYAxis().getNcoords(), csyss.getYAxis().getNcoords());
    }
  }

  // CFGridCoverageWriter2 adds another (dependent) time coordinate, so test that file

  @Test
  public void TestCFWriterCoverageRead() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ncss/test/GFS_CONUS_80km_20120227_0000.grib1.nc4";
    logger.info("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.FMRC, gds.getCoverageType());

      String covName = "Temperature_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(25243920, size);

      SubsetParams subset = new SubsetParams().setVertCoord(300.0).setTimeOffset(42.0);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      logger.info("{}", Misc.showInts(data.getShape()));
    }
  }
}
