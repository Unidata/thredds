/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test GribCdmIndex scanning
 *
 * @author caron
 * @since 10/15/2014
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGribCdmIndexUpdating {

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // file partition
    String dataDir = TestDir.cdmUnitTestDir + "gribCollections/changing/filePartition/";
    FeatureCollectionConfig config = new FeatureCollectionConfig("TestGribCdmIndex", "changing/filePartition", FeatureCollectionType.GRIB1,
            dataDir + "GFS_CONUS_80km_#yyyyMMdd_HHmm#.grib1", null, null, null, "file", null);
    // String dataDir, String newModel, FeatureCollectionConfig config, String indexFile, String varIdValue, int orgLen
    result.add(new Object[]{dataDir, "GFS_CONUS_80km_20141024_1200.grib1", config, "TestGribCdmIndex.ncx3", "VAR_7-0-2-52_L100", 4, 3});

    /* directory partition
    dataDir = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/";
    config = new FeatureCollectionConfig("TestGribCdmIndex", "GFS_CONUS_80km/TestGribCdmIndex", FeatureCollectionType.GRIB1,
            dataDir + "GFS_CONUS_80km_#yyyyMMdd_HHmm#.grib1", null, null, null, null, null);   rm
    result.add(new Object[]{dataDir, "GFS_CONUS_80km_20120227_0000.grib1", config, "TestGribCdmIndex-CONUS_80km.ncx2", "VAR_7-0-2-52_L100", 10, 9});  // */

    // directory partition
    //String dataDir2 = "B:/motherlode/rfc/";
    //FeatureCollectionConfig config2 = new FeatureCollectionConfig("RFC", "grib/NPVU/RFC", FeatureCollectionType.GRIB1,
    //        "B:/motherlode/rfc/**/.*grib1$", "yyyyMMdd#.grib1#", null, "directory", null, null);
    //result.add(new Object[]{dataDir2, "kalr/NPVU_RFC_KALR_NWS_152_20141003.grib1", config2, "RFC-rfc.ncx2", "VAR_9-161-128-237_L1_I1_Hour_S4", 336, 310});

    return result;
  }


  ///////////////////////////////////////

  String dataDir;
  String newModel;
  FeatureCollectionConfig config;
  String indexFile;
  String varIdValue;
  int orgLen, remLen;

  public TestGribCdmIndexUpdating(String dataDir, String newModel, FeatureCollectionConfig config, String indexFile, String varIdValue, int orgLen, int remLen) {
    this.dataDir = dataDir;
    this.newModel = newModel;
    this.config = config;
    this.indexFile = indexFile;
    this.varIdValue = varIdValue;
    this.orgLen = orgLen;
    this.remLen = remLen;
  }

  @Test
  public void testRemoveFileFromCollectionAlways() throws IOException {
    testRemoveFileFromCollection(CollectionUpdateType.always, orgLen, remLen);
  }

  @Test
  public void testRemoveFileFromCollectionTest() throws IOException {
    testRemoveFileFromCollection(CollectionUpdateType.test, orgLen, remLen);
  }

  @Test
  public void testRemoveFileFromCollectionTestOnly() throws IOException {
    testRemoveFileFromCollection(CollectionUpdateType.testIndexOnly, orgLen, orgLen);
  }

    // test when a file gets removed from a collection, ie GribCollectionBuilder
  private void testRemoveFileFromCollection(CollectionUpdateType updateType, int orgLen, int remLen) throws IOException {

    System.out.printf("testRemoveFileFromCollection = %s%n", updateType);

    File newModelFile = new File(dataDir + newModel);
    String newModelSave = dataDir + newModel + ".save";
    File newModelFileSave = new File(newModelSave);

    try {
      // remove one of the files from the scan
      if (newModelFile.exists() && !newModelFileSave.exists()) {
        boolean ok = newModelFile.renameTo(newModelFileSave);
        if (!ok) throw new IOException("cant rename file " + newModelFile);
      } else if (!newModelFile.exists() && newModelFileSave.exists()) {
        System.out.println("already renamed");
      } else if (!newModelFile.exists() && !newModelFileSave.exists()) {
        throw new IOException("missing " + newModelFile.getPath());
      } else if (newModelFile.exists() && newModelFileSave.exists()) {
        boolean ok = newModelFile.delete();
        if (!ok) throw new IOException("cant delete file " + newModelFile.getPath());
      }

      // index this
      org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
      boolean changed = GribCdmIndex.updateGribCollection(config, updateType, logger);
      System.out.printf("changed = %s%n", changed);

      // open the resulting index
      try (NetcdfFile ncfile = NetcdfFile.open(dataDir + indexFile)) {
        System.out.printf("opened = %s%n", ncfile.getLocation());
        Group g = ncfile.findGroup("TwoD");
        Variable v = ncfile.findVariableByAttribute(g, "Grib_Variable_Id", varIdValue);
        assert v != null;
        System.out.printf("  Variable=%s%n", v.getFullName());
        Dimension dim0 = v.getDimension(0);
        assert dim0.getLength() == remLen : dim0.getLength() + " should be " + remLen;
      }

      // new file arrives
      boolean ok = newModelFileSave.renameTo(newModelFile);
      if (!ok)
        throw new IOException("cant rename file");

      // redo the index
      boolean changed2 = GribCdmIndex.updateGribCollection(config, updateType, logger);
      System.out.printf("changed2 = %s%n", changed2);

      // open the resulting index
      try (NetcdfFile ncfile = NetcdfFile.open(dataDir + indexFile)) {
        System.out.printf("opened = %s%n", ncfile.getLocation());
        Group g = ncfile.findGroup("TwoD");
        Variable v = ncfile.findVariableByAttribute(g, "Grib_Variable_Id", varIdValue);
        assert v != null;

        Dimension dim0 = v.getDimension(0);
        assert dim0.getLength() == orgLen : dim0.getLength() + " should be " + orgLen;
      }

    } finally {  // leave it it the way we found it
        boolean ok = newModelFileSave.renameTo(newModelFile);
    }

  }


}
