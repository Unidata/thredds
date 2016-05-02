/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.inventory;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.filter.StreamFilter;
import thredds.inventory.partition.DirectoryCollection;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Test CollectionManager implementations
 *
 * @author caron
 * @since 6/20/11
 */
@Category(NeedsCdmUnitTest.class)
public class TestDcm {

  @Test
  public void testScan() throws IOException {
    // count scanned files
    Formatter f = new Formatter(System.out);
    CollectionManager dcm = MFileCollectionManager.open("testScan", TestDir.cdmUnitTestDir + "agg/narr/narr-a_221_#yyyyMMdd_HHmm#.*grb$", null, f);
    dcm.scan(true);
    List<MFile> fileList = (List<MFile>) Misc.getList(dcm.getFilesSorted());
    assert fileList.size() ==  3 : dcm;

    // check date extractor
    int count = 0;
    String[] result = new String[] {"2000-01-18T12:00:00", "2000-01-19T00:00:00", "2000-01-20T12:00:00"};
    for (MFile mfile : dcm.getFilesSorted()) {
      CalendarDate de = dcm.extractDate(mfile);
      System.out.printf("  %s == %s%n", mfile.getPath(), de);
      assert de.toString().startsWith(result[count]);
      count++;
    }
  }

  @Test
  @Ignore("tests fail on jenkins due to file permisssions")
  public void testScanOlderThan() throws IOException, InterruptedException {
    Formatter f = new Formatter(System.out);
    CollectionManager dcm = MFileCollectionManager.open("testScanOlderThan", TestDir.cdmUnitTestDir + "agg/updating/.*nc$", null, f);
    dcm.scan(true);
    List<MFile> fileList = (List<MFile>) Misc.getList(dcm.getFilesSorted());
    assert fileList.size() ==  3 : dcm;

    assert touch(TestDir.cdmUnitTestDir + "agg/updating/extra.nc");

    dcm = MFileCollectionManager.open("testScanOlderThan", TestDir.cdmUnitTestDir + "agg/updating/.*nc$", "10 sec", f);
    dcm.scan(true);
    fileList = (List<MFile>) Misc.getList(dcm.getFilesSorted());
    assert fileList.size() ==  2 : dcm;
  }

  @Test
  @Ignore("tests fail on jenkins due to file permisssions")
  public void testScanFromConfig() throws IOException {
    //public FeatureCollectionConfig(String name, FeatureCollectionType fcType, String spec, String dateFormatMark, String olderThan, String recheckAfter,
    //                               String timePartition, String useIndexOnlyS, Element innerNcml) {

    //   public FeatureCollectionConfig(String name, String path, FeatureCollectionType fcType, String spec,
   //                                  String dateFormatMark, String olderThan,
   //                                  String timePartition, String useIndexOnlyS, Element innerNcml) {

    FeatureCollectionConfig config = new FeatureCollectionConfig("testScanFromConfig", "path", FeatureCollectionType.FMRC,
            TestDir.cdmUnitTestDir + "agg/updating/.*nc$", null, null, "10 sec", null, null);

    assert touch(TestDir.cdmUnitTestDir + "agg/updating/extra.nc");

    // count scanned files
    Formatter f = new Formatter(System.out);
    MFileCollectionManager dcm = new MFileCollectionManager(config, f, null);
    dcm.scan(true);
    List<MFile> fileList = (List<MFile>) Misc.getList(dcm.getFilesSorted());
    assert fileList.size() ==  2 : dcm;
  }

  @Test
  @Ignore("tests fail on jenkins due to file permisssions")
  public void testOlderThanInDirectoryCollection() throws IOException {
    //   public FeatureCollectionConfig(String name, String path, FeatureCollectionType fcType, String spec,
   //                                  String dateFormatMark, String olderThan,
   //                                  String timePartition, String useIndexOnlyS, Element innerNcml) {

    FeatureCollectionConfig config = new FeatureCollectionConfig("testOlderThanInDirectoryCollection", "path", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "datasets/NDFD-CONUS-5km/.*grib2", null, null, "30 sec", null, null);

    Formatter errlog = new Formatter(System.out);
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);

    String fileToTouch =  specp.getRootDir() + "/NDFD_CONUS_5km_20131213_1200.grib2";
    assert touch(fileToTouch);

    // count scanned files
    // String topCollectionName, String topDirS, String olderThan, org.slf4j.Logger logger
    Logger logger = LoggerFactory.getLogger("testOlderThanInDirectoryCollection");
    DirectoryCollection dcm = new DirectoryCollection("topCollectionName", specp.getRootDir(), true, config.olderThan, logger);
    dcm.setStreamFilter(new StreamFilter(java.util.regex.Pattern.compile(".*grib2"), true));

    List<String> fileList = dcm.getFilenames();
    for (String name : fileList)
    System.out.printf("%s%n", name);
    assert fileList.size() ==  3 : fileList.size()+" !=  3 in "+specp.getRootDir();
  }


  boolean touch(String who) {
    File file = new File(who);
    assert file.exists();
    boolean ok = file.setLastModified(System.currentTimeMillis()); // touch
    if (!ok)
      System.out.printf("**Cant touch %s%n", who);
    return ok;
  }

}
