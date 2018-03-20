/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.filter.StreamFilter;
import thredds.inventory.partition.DirectoryCollection;
import thredds.inventory.partition.TimePartition;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;

@Category(NeedsCdmUnitTest.class)
public class TestMCollection {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testStreamFilterInDirPartition() throws IOException {
    // this dataset 0-6 hour forecasts  x 124 runtimes (4x31)
    // there are  2 groups, likely miscoded, the smaller group are 0 hour,  duplicates, possibly miscoded
    FeatureCollectionConfig config = new FeatureCollectionConfig("cfrsAnalysis_46", "test/testCfrsAnalysisOnly", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/cfsr/.*grb2", null, null, null, "directory", null);

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);
    Path rootPath = Paths.get(specp.getRootDir());

    try (DirectoryCollection dcm = new DirectoryCollection(config.collectionName, rootPath, true, config.olderThan, logger)) {
      dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
      if (specp.getFilter() != null)
        dcm.setStreamFilter(new StreamFilter(specp.getFilter(), specp.getFilterOnName()));

      int count = 0;
      for (MFile mfile : dcm.getFilesSorted()) {
        System.out.printf("%s%n", mfile);
        assert mfile.getName().equals("pwat.gdas.199612.grb2");
        count++;
      }
      assert count == 1;
    }
  }

  @Test
  public void testTimePartition() throws IOException {

    FeatureCollectionConfig config = new FeatureCollectionConfig("ds627.1", "test/ds627.1", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds627.1/.*gbx9", null, "#ei.mdfa.fc12hr.sfc.regn128sc.#yyyyMMddhh", null, "year", null);

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);

    try (TimePartition tp = new TimePartition(config, specp, logger)) {
      tp.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);

      int countP = 0;
      for (MCollection mc : tp.makePartitions(CollectionUpdateType.always)) {
        System.out.printf("%s%n", mc);
        countP++;

        int count = 0;
        for (MFile mfile : mc.getFilesSorted()) {
          System.out.printf("  %s%n", mfile);
          count++;
        }
        assert count == 12 : count;
      }
      assert countP == 34 : countP;
    }

  }

}
