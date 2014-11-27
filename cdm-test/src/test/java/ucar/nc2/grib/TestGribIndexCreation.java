package ucar.nc2.grib;

import org.junit.Test;
import thredds.catalog.parser.jdom.FeatureCollectionReader;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Test that the CDM Index Creation works
 *
 * @author caron
 * @since 11/14/2014
 */
public class TestGribIndexCreation {

  @Test
  public void testGdsHashChange() throws IOException {
    // this dataset 0-6 hour forecasts  x 124 runtimes (4x31)
    // there are  2 groups, likely miscoded, the smaller group are 0 hour,  duplicates, possibly miscoded
    FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection(TestDir.cdmUnitTestDir + "gribCollections/cfsr/config.xml#cfsr-pwat");
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    // LOOK add check that records were combined
  }

  @Test
  public void testRdvamds083p2() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds083.2_46", "test/ds083.2", FeatureCollectionType.GRIB1, "B:/rdavm/ds083.2/grib1/**/.*gbx9", null, null, "directory", null, null);
    config.gribConfig.useTableVersion = false;
    // config.gribConfig.unionRuntimeCoord = true;
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Test
  public void testRdvamds627p0() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds627.0_46", "test/ds627.0", FeatureCollectionType.GRIB1,
            "B:/rdavm/ds627.0/ei.oper.an.pv/**/.*gbx9", "#ei.oper.an.pv/#yyyyMM", null, "directory", null, null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }


  @Test   // has one file for for each month, all in same directory
  public void testRdvamds627p1() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("GCpass1-union", "test/GCpass1", FeatureCollectionType.GRIB1, "B:/rdavm/ds627.1/.*gbx9", null, null, "directory", null, null);
    config.gribConfig.useTableVersion = false;
    config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.test, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Test
  public void testRdvamds083p2_SampleMonth() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds083.2-union", "test/GCpass1", FeatureCollectionType.GRIB1, "B:/rdavm/ds083.2/sampleMonth/.*gbx9", null, null, "directory", null, null);
    config.gribConfig.useTableVersion = false;
    config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.test, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Test
  public void testDgex() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("dgex_46", "test/dgex", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/dgex/**/.*grib2", null, null, "file", null, null);
    //config.gribConfig.useTableVersion = false;
    //config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
  public void testGFSconus80() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("gfsConus80_46", "test/gfsConus80", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/**/.*grib1", null, null, "file", null, null);
    //config.gribConfig.useTableVersion = false;
    //config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
  }


  @Test
  public void testCfrsAnalysis() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("cfrsAnalysis_46", "test/cfrsAnalysis", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/cfsr/.*grb2", null, null, "directory", null, null);
    // <gdsHash from="1450192070" to="1450218978"/>
    config.gribConfig.addGdsHash("1450192070", "1450218978");
    //config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
  }

}
