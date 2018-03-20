/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MFile;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.util.Indent;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 3/7/2016.
 */
@Ignore("until we add MRC Best")
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGribPartitionTypesP {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String topdir = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80";
  private static final String spec = topdir + "/**/.*grib1";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{ "gfsConus80_dir", "directory", true});
    result.add(new Object[]{ "gfsConus80_file", "file", true});
    result.add(new Object[]{ "gfsConus80_none", "none", false});

    return result;
  }

  String collectionName;
  String partitionType;
  boolean isTwoD;

  public TestGribPartitionTypesP(String collectionName, String partitionType, Boolean isTwoD) {
    this.collectionName = collectionName;
    this.partitionType = partitionType;
    this.isTwoD = isTwoD;
  }

  @Test
  public void testGFSconus80_dir() throws IOException {
    Indent indent = new Indent(2);

    // create it
    FeatureCollectionConfig config = new FeatureCollectionConfig(collectionName, "test/" + collectionName, FeatureCollectionType.GRIB1,
            spec, null, null, null, partitionType, null);

    System.out.printf("============== create %s %n", collectionName);
    try (GribCollectionImmutable gc = GribCdmIndex.openGribCollection(config, CollectionUpdateType.always, logger)) {  // recreate the index each time
      Assert.assertNotNull(collectionName, gc);
      indent.incr();
      openGC(gc.getLocation(), config, indent);
      indent.decr();

      /* List<GribCollectionImmutable.Dataset> datasets = gc.getDatasets();
      Assert.assertEquals(isTwoD ? 2 : 1, datasets.size());
      if (isTwoD) {
        Assert.assertEquals(datasets.get(0).getType(), datasets.get(0).getType().isTwoD());
        Assert.assertEquals(GribCollectionImmutable.Type.Best, datasets.get(1).getType());
      }

      indent.incr();
      for (GribCollectionImmutable.Dataset dataset : gc.getDatasets()) {
        System.out.printf("%sdataset = %s %n", indent, dataset.getType());
      }

      for (MFile mfile : gc.getFiles()) {
        openGC(mfile.getPath(), config, indent);
      }
      indent.decr(); */
    }

    System.out.printf("done%n");
  }

  private void openGC(String indexFilename, FeatureCollectionConfig config, Indent indent) throws IOException {
    if (!indexFilename.endsWith(".ncx4")) return;

    try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(indexFilename, config, true, logger)) {
      Assert.assertNotNull(indexFilename, gc);
      System.out.printf("%sindex filename = %s %n", indent, gc.getLocation());

      int ndatasets = gc.getDatasets().size();
      if (isTwoD)
        Assert.assertEquals(2, ndatasets);

      indent.incr();
      for (GribCollectionImmutable.Dataset dataset : gc.getDatasets()) {
        System.out.printf("%sdataset = %s %n", indent, dataset.getType());
      }

      for (MFile mfile : gc.getFiles()) {
        openGC(mfile.getPath(), config, indent);
      }

      indent.decr();
    }
    System.out.printf("%n");
  }

}
