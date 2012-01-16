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

import junit.framework.TestCase;
import ucar.nc2.TestAll;
import ucar.nc2.util.Misc;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Test DatasetCollectionManager
 *
 * @author caron
 * @since 6/20/11
 */
public class TestDcm extends TestCase {
  public TestDcm( String name) {
    super(name);
  }

  public void testScan() throws IOException {
    // count scanned files
    Formatter f = new Formatter(System.out);
    MFileCollectionManager dcm = MFileCollectionManager.open(TestAll.cdmUnitTestDir + "agg/narr/narr-a_221_#yyyyMMdd_HHmm#.*grb$", null, f);
    dcm.scan(true);
    List<MFile> fileList = (List<MFile>) Misc.getList(dcm.getFiles());
    assert fileList.size() ==  3 : dcm;

    // check date extractor
    int count = 0;
    String[] result = new String[] {"2000-01-18T12:00:00.000Z", "2000-01-19T00:00:00.000Z", "2000-01-20T12:00:00.000Z"};
    for (MFile mfile : dcm.getFiles()) {
      System.out.printf("  %s == %s%n", mfile.getPath(), dcm.extractRunDate(mfile));
      assert dcm.extractRunDate(mfile).toString().equals(result[count++]);
    }
  }

  public void testScanOlderThan() throws IOException, InterruptedException {
    Formatter f = new Formatter(System.out);
    MFileCollectionManager dcm = MFileCollectionManager.open(TestAll.cdmUnitTestDir + "agg/updating/.*nc$", null, f);
    dcm.scan(true);
    List<MFile> fileList = (List<MFile>) Misc.getList(dcm.getFiles());
    assert fileList.size() ==  3 : dcm;

    assert touch(TestAll.cdmUnitTestDir + "agg/updating/extra.nc");

    dcm = MFileCollectionManager.open(TestAll.cdmUnitTestDir + "agg/updating/.*nc$", "10 sec", f);
    dcm.scan(true);
    fileList = (List<MFile>) Misc.getList(dcm.getFiles());
    assert fileList.size() ==  2 : dcm;

    for (MFile mfile : dcm.getFiles()) {
      System.out.printf("  %s == %s%n", mfile.getPath(), dcm.extractRunDate(mfile));
    }
  }

  public void testScanFromConfig() throws IOException {
    FeatureCollectionConfig config = new  FeatureCollectionConfig("testScanFromConfig", TestAll.cdmUnitTestDir + "agg/updating/.*nc$",
            null, "10 sec", null, null, null, null);

    assert touch(TestAll.cdmUnitTestDir + "agg/updating/extra.nc");

    // count scanned files
    Formatter f = new Formatter(System.out);
    MFileCollectionManager dcm = new MFileCollectionManager(config, f);
    dcm.scan(true);
    List<MFile> fileList = (List<MFile>) Misc.getList(dcm.getFiles());
    assert fileList.size() ==  2 : dcm;

    // check date extractor
    for (MFile mfile : dcm.getFiles()) {
      System.out.printf("  %s == %s%n", mfile.getPath(), dcm.extractRunDate(mfile));
    }
  }

  boolean touch(String who) {
    File file = new File(who);
    assert file.exists();
    return file.setLastModified(System.currentTimeMillis()); // touch
  }
}
