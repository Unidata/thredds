package ucar.nc2.grib;

import org.junit.Test;
import thredds.catalog.parser.jdom.FeatureCollectionReader;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 11/14/2014
 */
public class TestGribIndexCreation {

  @Test
  public void testGdsHashChange() throws IOException {
    String dataDir2 = TestDir.cdmUnitTestDir + "gribCollections/gdsHashChange/";
    FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection(TestDir.cdmUnitTestDir + "gribCollections/cfsr/config.xml#cfsr-pwat");
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);

  }
}
