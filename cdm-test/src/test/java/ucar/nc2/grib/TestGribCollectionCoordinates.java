/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.ma2.ArrayDouble;
import ucar.nc2.Variable;
import ucar.nc2.dataset.*;
import ucar.nc2.grib.collection.*;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Describe
 *
 * @author caron
 * @since 3/2/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionCoordinates {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static CollectionUpdateType updateMode = CollectionUpdateType.always;

  @BeforeClass
  static public void before() throws IOException {
    GribIosp.debugIndexOnlyCount = 0;
    GribCollectionImmutable.countGC = 0;
    PartitionCollectionImmutable.countPC = 0;
    RandomAccessFile.enableDefaultGlobalFileCache();
    RandomAccessFile.setDebugLeaks(true);
    // GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    GribCdmIndex.setGribCollectionCache(new ucar.nc2.util.cache.FileCacheGuava("GribCollectionCacheGuava", 100));
    GribCdmIndex.gribCollectionCache.resetTracking();
  }

  @AfterClass
  static public void after() {
    Grib.setDebugFlags(new DebugFlagsImpl());
    /* Formatter out = new Formatter(System.out);

    FileCacheIF cache = GribCdmIndex.gribCollectionCache;
    if (cache != null) {
      cache.showTracking(out);
      cache.showCache(out);
      cache.clearCache(false);
    }

    FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
    if (rafCache != null) {
      rafCache.showCache(out);
    }

    System.out.printf("            countGC=%7d%n", GribCollectionImmutable.countGC);
    System.out.printf("            countPC=%7d%n", PartitionCollectionImmutable.countPC);
    System.out.printf("    countDataAccess=%7d%n", GribIosp.debugIndexOnlyCount);
    System.out.printf(" total files needed=%7d%n", GribCollectionImmutable.countGC + PartitionCollectionImmutable.countPC + GribIosp.debugIndexOnlyCount);

    FileCache.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
    TestDir.checkLeaks();
    RandomAccessFile.setDebugLeaks(false); */
  }

  /////////////////////////////////////////////////////////

  // check that all time variables are coordinates (TwoD PofP was not eliminating unused coordinates after merging)
  @Test
  public void testExtraCoordinates() throws IOException {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("namAlaska22", "test/namAlaska22", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/namAlaska22/.*gbx9", null, null, null, "file", null);
    // config.gribConfig.setOption("timeUnit", "1 minute");

    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);

    boolean ok = true;

    try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + "gribCollections/namAlaska22/namAlaska22.ncx4")) {
      for (Variable vds : ds.getVariables()) {
        String stdname = ds.findAttValueIgnoreCase(vds, "standard_name", "no");
        if (!stdname.equalsIgnoreCase("time")) continue;

        System.out.printf(" %s == %s%n", vds.getFullName(), vds.getClass().getName());
        assert vds instanceof CoordinateAxis : vds.getFullName();

        // test that zero Intervals are removed
        if (vds instanceof CoordinateAxis1D) {
          CoordinateAxis1D axis = (CoordinateAxis1D) vds;
          if (axis.isInterval()) {
            for (int i = 0; i < axis.getSize(); i++) {
              double[] bound = axis.getCoordBounds(i);
              if (bound[0] == bound[1]) {
                System.out.printf("%s(%d) = [%f,%f]%n", vds.getFullName(), i, bound[0], bound[1]);
                ok = false;
              }
            }
          }

        } else if (vds instanceof CoordinateAxis2D) {
          CoordinateAxis2D axis2 = (CoordinateAxis2D) vds;
          if (axis2.isInterval()) {
            ArrayDouble.D3 bounds = axis2.getCoordBoundsArray();
            for (int i = 0; i < axis2.getShape(0); i++)
              for (int j = 0; j < axis2.getShape(1); j++) {
                double start = bounds.get(i, j, 0);
                double end = bounds.get(i, j, 1);
                if (start == end) {
                  System.out.printf("%s(%d,%d) = [%f,%f]%n", vds.getFullName(), i, j, start, end);
                  ok = false;
                }
              }
          }
        }
      }
    }

    assert ok;
  }

  // make sure Best reftimes always increase
  @Test
  public void testBestReftimeMonotonic() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("gfs_2p5deg", "test/gfs_2p5deg", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/.*grib2", null, null,  null, "file", null);

    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);

    boolean ok = true;

    try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4")) {
      for (Variable vds : ds.getVariables()) {
        String stdname = ds.findAttValueIgnoreCase(vds, "standard_name", "no");
        if (!stdname.equalsIgnoreCase("forecast_reference_time")) continue;

        System.out.printf(" %s == %s%n", vds.getFullName(), vds.getClass().getName());
        assert vds instanceof CoordinateAxis1D : vds.getFullName();
        CoordinateAxis1D axis = (CoordinateAxis1D) vds;

        // test that values are monotonic
        double last = Double.NaN;
        for (int i = 0; i < axis.getSize(); i++) {
          double val = axis.getCoordValue(i);
          if (i > 0 && (val < last)) {
            System.out.printf("  %s(%d) == %f < %f%n", vds.getFullName(), i, val, last);
            ok = false;
          }
          last = val;
        }
      }
    }

    assert ok;
  }



}
