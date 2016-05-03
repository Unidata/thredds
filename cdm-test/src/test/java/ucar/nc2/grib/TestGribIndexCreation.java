package ucar.nc2.grib;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.collection.PartitionCollectionImmutable;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Formatter;

/**
 * Test that the CDM Index Creation works
 *
 * @author caron
 * @since 11/14/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribIndexCreation {
  private static CollectionUpdateType updateMode = CollectionUpdateType.always;

  @BeforeClass
  static public void before() {
    GribIosp.debugIndexOnlyCount = 0;
    GribCollectionImmutable.countGC = 0;
    PartitionCollectionImmutable.countPC = 0;
    RandomAccessFile.enableDefaultGlobalFileCache();
    RandomAccessFile.setDebugLeaks(true);
    // GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    GribCdmIndex.setGribCollectionCache(new ucar.nc2.util.cache.FileCacheGuava("GribCollectionCacheGuava", 100));
    GribCdmIndex.gribCollectionCache.resetTracking();

    // make sure that the indexes are created with the data files
    DiskCache2 diskCache = GribIndexCache.getDiskCache2();
    diskCache.setNeverUseCache(true);
    diskCache.setAlwaysUseCache(false);
  }

  @AfterClass
  static public void after() {
    GribIosp.setDebugFlags(new DebugFlagsImpl());
    Formatter out = new Formatter(System.out);

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
    RandomAccessFile.setDebugLeaks(false);
  }

  /////////////////////////////////////////////////////////

  @Test
  public void testGdsHashChange() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("NDFD-CONUS_noaaport", "test/NDFD-CONUS_noaaport", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/gdsHashChange/noaaport/.*gbx9", null, null, null, "file", null);
    // 	  <gdsHash from='-1506003048' to='-1505079527'/>
    config.gribConfig.addGdsHash("-1506003048", "-1505079527");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    // LOOK add check that records were combined
    try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "gribCollections/gdsHashChange/noaaport/NDFD-CONUS_noaaport.ncx3", null)) {
      Group root = ncfile.getRootGroup();
      Assert.assertEquals(2, root.getGroups().size());
      Group twoD = root.findGroup("TwoD");
      assert twoD != null;
      Assert.assertEquals(0, twoD.getGroups().size());
    }


    GribIosp.setDebugFlags(new DebugFlagsImpl(""));
  }

  @Test
  public void testCfrsAnalysisOnly() throws IOException {
     // this dataset 0-6 hour forecasts  x 124 runtimes (4x31)
    // there are  2 groups, likely miscoded, the smaller group are 0 hour,  duplicates, possibly miscoded
    FeatureCollectionConfig config = new FeatureCollectionConfig("cfrsAnalysis_46", "test/testCfrsAnalysisOnly", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/cfsr/.*grb2", null, null, null, "directory", null);
    // <gdsHash from="1450192070" to="1450218978"/>
    config.gribConfig.addGdsHash("1450192070", "1450218978");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
  public void testDgex() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("dgex_46", "test/dgex", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/dgex/**/.*grib2", null, null, null, "file", null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
  public void testGFSconus80() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("gfsConus80_46", "test/gfsConus80", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/**/.*grib1", null, null,  null, "file", null);

    System.out.printf("===testGFSconus80 %n");
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
  public void testGFS_2p5deg() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("gfs_2p5deg", "test/gfs_2p5deg", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/.*grib2", null, null,  null, "file", null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
  public void testSeanProblem() throws IOException {


    FeatureCollectionConfig config = new FeatureCollectionConfig("gfs_2p5deg", "test/gfs_2p5deg", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/.*grib2", null, null,  null, "file", null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
  public void testEnsembles() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("gefs_ens", "test/gefs_ens", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/ens/.*grib2", null, null,  null, "file", null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  ////////////////

  @Test
  public void testRdvamds083p2_PofP() throws IOException {
    //DiskCache2 gribCache = DiskCache2.getDefault();
    //gribCache.setRootDirectory("C:/dev/github/thredds46/tds/content/thredds/cache/grib/");
    //gribCache.setAlwaysUseCache(true);
    //GribIndexCache.setDiskCache2(gribCache);

    FeatureCollectionConfig config = new FeatureCollectionConfig("ds083.2-pofp", "test/ds083.2-pofp", FeatureCollectionType.GRIB1,
           TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/PofP/**/.*grib1",
            null, null,  null, "directory", null);
    config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Test
  public void testRdvamds083p2() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds083.2_Aggregation", "test/ds083.2", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/grib1/**/.*gbx9",
            null, null, null, "directory", null);
    config.gribConfig.unionRuntimeCoord = true;
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("always");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Test
  public void testRdvamds083p2_1999() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds083.2_Aggregation", "test/ds083.2", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/grib1/2008/**/.*gbx9",
            null, null, null,  "directory", null);
    config.gribConfig.unionRuntimeCoord = true;
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("always");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Test
  public void testRdvamds627p0() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds627.0_46", "test/ds627.0", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds627.0/ei.oper.an.pv/**/.*gbx9", null , "#ei.oper.an.pv/#yyyyMM", null, "directory", null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }


  @Test   // has one file for for each month, all in same directory
  public void testRdvamds627p1() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("GCpass1-union", "test/GCpass1", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds627.1/.*gbx9", null, null, null, "directory", null);
    config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

    ////////////////

  @Test   // has one file for for each month, all in same directory
  public void testTimePartition() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("yearPartition", "test/yearPartition", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds627.1/.*gbx9", null, "#ei.mdfa.fc12hr.sfc.regn128sc.#yyyyMMddhh", null, "year", null);
    config.gribConfig.unionRuntimeCoord = true;
    System.out.printf("config = %s%n", config);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Test
  public void testWW3() throws IOException {
    // String name, String path, FeatureCollectionType fcType,
    // String spec, String collectionName,
    // String dateFormatMark, String olderThan, String timePartition, Element innerNcml)
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds093.1", "test/ds093.1", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "tds/ncep/WW3_Coastal_Alaska_20140804_0000.grib2", null,
            null, null, "file", null);
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
   public void testTP() throws IOException {   // should be a TP (multiple runtime, single offset
     // String name, String path, FeatureCollectionType fcType,
     // String spec, String collectionName,
     // String dateFormatMark, String olderThan, String timePartition, Element innerNcml)
     FeatureCollectionConfig config = new FeatureCollectionConfig("GFSonedega", "test/GFSonedega", FeatureCollectionType.GRIB2,
             TestDir.cdmUnitTestDir + "gribCollections/tp/.*grib2", null,
             null, null, "file", null);
     org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
     boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
     System.out.printf("changed = %s%n", changed);
   }


}
