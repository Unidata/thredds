/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.inventory;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.filter.StreamFilter;
import thredds.inventory.partition.DirectoryCollection;
import thredds.inventory.partition.TimePartition;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;

@Category(NeedsCdmUnitTest.class)
public class TestMCollection {
  org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");

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
