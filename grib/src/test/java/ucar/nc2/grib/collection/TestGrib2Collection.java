/* Copyright Unidata */
package ucar.nc2.grib.collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.DebugFlagsImpl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

@RunWith(JUnit4.class)
public class TestGrib2Collection {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String topDir = "../grib/src/test/data/collection/grib2/";

  @Test
  public void testGFS_2p5deg() throws IOException {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("gfs_2p5deg", "test/gfs_2p5deg", FeatureCollectionType.GRIB2,
            topDir + "gfs_2p5deg/.*gbx9", null, null,  null, "file", null);

    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    Grib.setDebugFlags(new DebugFlagsImpl());

    String filename = topDir + "gfs_2p5deg/gfs_2p5deg.ncx4";
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, fdc);

      // FMRC
      CoverageCollection fmrc = fdc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull(FeatureType.GRID.toString(), fmrc);

      System.out.printf(" %s type=%s%n", fmrc.getName(), fmrc.getCoverageType());
      for (CoverageCoordSys coordSys : fmrc.getCoordSys()) {
        Assert.assertTrue( coordSys.isTime2D(coordSys.getAxis(AxisType.RunTime)));
        Assert.assertTrue( coordSys.isTime2D(coordSys.getTimeAxis()));
      }

      Coverage cov = fmrc.findCoverage("Absolute_vorticity_isobaric");
      Assert.assertNotNull(cov);
      CoverageCoordSys varcc = cov.getCoordSys();
      Assert.assertArrayEquals(new int[] {4, 93, 26, 73, 144}, varcc.getShape());

      // BEST
      CoverageCollection best = fdc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.GRID.toString(), best);

      System.out.printf(" %s type=%s%n", best.getName(), best.getCoverageType());
      for (CoverageCoordSys coordSys : best.getCoordSys()) {
        Assert.assertFalse( coordSys.isTime2D(coordSys.getAxis(AxisType.RunTime)));
        Assert.assertFalse( coordSys.isTime2D(coordSys.getTimeAxis()));
      }

      Coverage covb = best.findCoverage("Absolute_vorticity_isobaric");
      Assert.assertNotNull(covb);
      CoverageCoordSys varccb = covb.getCoordSys();
      Assert.assertArrayEquals(new int[] {99, 26, 73, 144}, varccb.getShape());
    }
  }
  @Test
  public void testGefsEns() throws IOException {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("gefs_ens", "test/gefs_ens", FeatureCollectionType.GRIB2,
            topDir + "ens/.*gbx9", null, null,  null, "file", null);

    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    Grib.setDebugFlags(new DebugFlagsImpl());

    String filename = topDir + "ens/gefs_ens.ncx4";
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, fdc);

      // FMRC
      CoverageCollection fmrc = fdc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull(FeatureType.GRID.toString(), fmrc);

      System.out.printf(" %s type=%s%n", fmrc.getName(), fmrc.getCoverageType());
      for (CoverageCoordSys coordSys : fmrc.getCoordSys()) {
        Assert.assertTrue( coordSys.isTime2D(coordSys.getAxis(AxisType.RunTime)));
        Assert.assertTrue( coordSys.isTime2D(coordSys.getTimeAxis()));
      }

      Coverage cov = fmrc.findCoverage("Categorical_Freezing_Rain_surface_6_Hour_Average_ens");
      Assert.assertNotNull(cov);
      CoverageCoordSys varcc = cov.getCoordSys();
      Assert.assertArrayEquals(new int[] {4, 64, 21, 181, 360}, varcc.getShape());

      // BEST
      CoverageCollection best = fdc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.GRID.toString(), best);

      System.out.printf(" %s type=%s%n", best.getName(), best.getCoverageType());
      for (CoverageCoordSys coordSys : best.getCoordSys()) {
        Assert.assertFalse( coordSys.isTime2D(coordSys.getAxis(AxisType.RunTime)));
        Assert.assertFalse( coordSys.isTime2D(coordSys.getTimeAxis()));
      }

      Coverage covb = best.findCoverage("Categorical_Freezing_Rain_surface_6_Hour_Average_ens");
      Assert.assertNotNull(covb);
      CoverageCoordSys varccb = covb.getCoordSys();
      Assert.assertArrayEquals(new int[] {67, 21, 181, 360}, varccb.getShape());
    }
  }

}
