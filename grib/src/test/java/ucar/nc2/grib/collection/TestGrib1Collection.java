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
public class TestGrib1Collection {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String topDir = "../grib/src/test/data/collection/grib1/";


  @Test
  public void testEcmwfMwp() throws IOException {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ECMWFmwp", "path/ECMWFmwp", FeatureCollectionType.GRIB1,
            topDir + "ecmwf/.*gbx9",
            null, null, null, "directory", null);

    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    Grib.setDebugFlags(new DebugFlagsImpl());

    String filename = topDir + "ecmwf/ECMWFmwp.ncx4";
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, fdc);
      CoverageCollection cc = fdc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.GRID.toString(), cc);

      System.out.printf(" %s type=%s%n", cc.getName(), cc.getCoverageType());
      for (CoverageCoordSys coordSys : cc.getCoordSys()) {
        Assert.assertFalse( coordSys.isTime2D(coordSys.getAxis(AxisType.RunTime)));
        Assert.assertFalse( coordSys.isTime2D(coordSys.getTimeAxis()));
      }

      Coverage cov = cc.findCoverage("Significant_height_of_wind_waves_msl");
      Assert.assertNotNull(cov);
      CoverageCoordSys varcc = cov.getCoordSys();
      Assert.assertArrayEquals(new int[] {48, 721, 1440}, varcc.getShape());
    }
  }

  @Test
  public void testRdvamds083pofpWithIndexPresent() throws IOException {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds083.2_pofp", "test/ds083.2_pofp",
            FeatureCollectionType.GRIB1,
            topDir + "pofpFromIndex/**/.*gbx9",
            null, null, null, "directory", null);

    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    Grib.setDebugFlags(new DebugFlagsImpl());

    String filename = topDir + "pofp/ds083.2_pofp.ncx4";
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, fdc);

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
      Assert.assertArrayEquals(new int[] {144, 26, 181, 360}, varccb.getShape());
    }

  }

}
