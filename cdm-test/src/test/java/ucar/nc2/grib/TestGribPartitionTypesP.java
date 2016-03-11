/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.grib;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MFile;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.util.Indent;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
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

  private static final String topdir = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80";
  private static final String spec = topdir + "/**/.*grib1";
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");

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
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
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
