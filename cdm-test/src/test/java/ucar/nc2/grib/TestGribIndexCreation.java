package ucar.nc2.grib;

import org.junit.Test;
import thredds.catalog.parser.jdom.FeatureCollectionReader;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Test that the
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
  public void testRdvam() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection("B:/rdavm/ds083.2/catalog_ds083.2.xml#ds083.2_Aggregation_MRC");
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(null);
  }

}
