package ucar.nc2.grib;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.collection.PartitionCollectionImmutable;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Formatter;

/**
 * Test that the CDM Index Creation works
 *
 * @author caron
 * @since 11/14/2014
 */
public class TestGribIndexCreationOther {
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

  @Ignore("B: not visible on spock")
  @Test
  public void testFireWx() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("namFirewx", "test/namFirewx", FeatureCollectionType.GRIB2,
 //           TestDir.cdmUnitTestDir + "gribCollections/www/.*grib2",
            "B:/lead/namFirewx/.*gbx9",  null,
            null, null, "file", null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Ignore("B: not visible on spock")
  @Test
  public void testRadarNWS() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("radarNWS", "test/radarNWS", FeatureCollectionType.GRIB1,
 //           TestDir.cdmUnitTestDir + "gribCollections/www/.*grib2",
            "B:/lead/radar/**/.*gbx9",  null,
            null, null, "directory", null);
    config.gribConfig.setOption("timeUnit", "1 minute");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl(""));
  }

  @Ignore("B: not visible on spock")
  @Test
  public void testWwwCoastalAlaska() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("Coastal_Alaska", "test/Coastal_Alaska", FeatureCollectionType.GRIB2,
 //           TestDir.cdmUnitTestDir + "gribCollections/www/.*grib2",
            "B:/idd/WWW/Coastal_Alaska/.*gbx9",
            null, null, null, "file", null);
    // config.gribConfig.addGdsHash("-804803647", "-804803709");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl(""));
  }


  //          <collection name = "ds626.0.pl.3hr" spec="/glade/p/rda/data/ds626.0/e20c.oper.an.pl.3hr/**/.*grb$"
  //                    dateFormatMark="#regn80#...yyyyMMddHH"
  //                    timePartition="year" />

  @Ignore("B: not visible on spock")
  @Test
  public void testTimePartitionWithSubdirs() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds626.0", "test/ds626.0", FeatureCollectionType.GRIB1,
            "B:/rdavm/ds626.0/**/.*gbx9", null, "#regn80#...yyyyMMddHH", null, "year", null);
    // config.gribConfig.unionRuntimeCoord = true;
    System.out.printf("config = %s%n", config);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Ignore("B: not visible on spock")
  @Test
  public void make626inv() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds626.0.invariants", "test/ds626.0inv", FeatureCollectionType.GRIB1,
            "B:/rdavm/ds6260inv/.*grb$", null, null, null, null, null);
    System.out.printf("config = %s%n", config);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Ignore("B: not visible on spock")
  @Test
  public void makeCfsr() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds093.1", "test/ds093.1", FeatureCollectionType.GRIB2,
            "B:/rdavm/ds093.1/data/.*gbx9", null, null, null, null, null);
    // config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Ignore("B: not visible on spock")
  @Test
  public void makeCfsr2() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds094.1", "test/ds094.1", FeatureCollectionType.GRIB2,
            "B:/rdavm/ds094.1/2011/.*gbx9", null, null, null, null, null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }


  @Ignore("B: not visible on spock")
  @Test
  public void makeDs0832() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds083.2", "test/ds083.2", FeatureCollectionType.GRIB1,
            "B:/rdavm/ds083.2/grib1/**/.*gbx9", null, null, null, null, null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Ignore("B: not visible on spock")
  @Test
  public void testGsdHrrSurface() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("GSD_HRRR_CONUS_3km_surface", "test/GSD_HRRR_CONUS_3km_surface", FeatureCollectionType.GRIB2,
            "B:/idd/GSD_HRRR_CONUS_3km_surface/.*gbx9", null, null, null, "file", null);
    config.gribConfig.setOption("timeUnit", "1 minute");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Ignore("B: not visible on spock")
  @Test
  public void testRtma() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("RTMA-CONUS_2p5km", "test/RTMA-CONUS_2p5km", FeatureCollectionType.GRIB2,
            "B:/idd/RTMA-CONUS_2p5km/.*gbx9", null, null, null, "file", null);
    // config.gribConfig.setOption("timeUnit", "1 minute");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Ignore("B: not visible on spock")
  @Test
  public void testJma() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds628-redo", "test/ds628.0", FeatureCollectionType.GRIB1,
            "B:/rdavm/ds628.0/anl_land/**/.*gbx9", null, null, null, null, null);
    // config.gribConfig.setOption("timeUnit", "1 minute");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Ignore("B: not visible on spock")
  @Test
  public void testRDAds628p1_fcst_surf125_var() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds628p1_fcst_surf125_var", "rdavm/ds628p1_fcst_surf125_var", FeatureCollectionType.GRIB1,
            "B:/rdavm/ds628.1_fcst_surf125_var/fcst_surf125_var\\..*195812$", null, null, null, null, null);
    // config.gribConfig.setOption("timeUnit", "1 minute");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Ignore("B: not visible on spock")
  @Test
  public void testRDAds131() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("testRDAds131", "rdavm/testRDAds131", FeatureCollectionType.GRIB1,
            "B:/rdavm/ds131.1/sflxfg_mean/.*grib$", null, null, null, null, null);
    config.gribConfig.setUseTableVersion(true);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
  }

   @Ignore("B: not visible on spock")
   @Test
   public void testCarlosMoraga() throws IOException {
     FeatureCollectionConfig config = new FeatureCollectionConfig("CarlosMoragaEcmwf", "CarlosMoragaEcmwf", FeatureCollectionType.GRIB1,
             "B:/testdata/support/CarlosMoraga/ECMWF_GNERA.*", null, null, null, null, null);

     org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
     boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
     System.out.printf("changed = %s%n", changed);
   }

  @Ignore("B: not visible on spock")
  @Test
  public void testNcdcNarr() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("NcdcNarr", "NcdcNarr", FeatureCollectionType.GRIB1,
            "B:/ncdc/0402/home/tomcat/dans-tdm-content/content/tdm/cache/GribIndex/global/nomads/nexus/narr/**/.*gbx9", null, null, null, null, null);
    config.setFilter("B:/ncdc/0402/home/tomcat/dans-tdm-content/content/tdm/cache/GribIndex/global/nomads/nexus/narr/", "\\d{6}/\\d{8}/.*gbx9$");
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  // String name, String path, FeatureCollectionType fcType, String spec, String collectionName, String dateFormatMark, String olderThan, String timePartition, Element innerNcml
  @Ignore("B: not visible on spock")
  @Test
  public void testNam20() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("Nam10", "Nam10", FeatureCollectionType.GRIB1,
            "B:/atm/nam20/.*gbx9", null, null, null, "file", null);
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.testIndexOnly, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }


  // String name, String path, FeatureCollectionType fcType, String spec, String collectionName, String dateFormatMark, String olderThan, String timePartition, Element innerNcml
  @Ignore("B: not visible on spock")
  @Test
  public void testNdfdNoaaport() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ndfdNoaaport", "ndfdNoaaport", FeatureCollectionType.GRIB2,
            "B:/atm/ndfd/.*gbx9", null, null, null, "file", null);
    config.gribConfig.addGdsHash("-1506003048","-1505079527");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  // String name, String path, FeatureCollectionType fcType, String spec, String collectionName, String dateFormatMark, String olderThan, String timePartition, Element innerNcml
  @Ignore("B: not visible on spock")
  @Test
  public void testNcdcGfsanl3() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("Gfs003anl", "Gfs003anl", FeatureCollectionType.GRIB1,
            "B:/ncdc/0416/indexes/gfsanl3/**/gfsanl_3_.*gbx9", null, null, null, null, null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Ignore("B: not visible on spock")
  @Test
  public void testNcdcGfsanl4() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("Gfs004anl", "Gfs004anl", FeatureCollectionType.GRIB2,
            "B:/ncdc/0416/indexes/gfsanl3/**/gfsanl_4_.*gbx9", null, null, null, null, null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

}
