/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.grib;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.grib.collection.GribCollection;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.collection.PartitionCollection;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.cache.FileCache;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.Formatter;

/**
 * Look for missing data in Grib Collections.
 *
 * Indicates that coordinates are not matching,
 *
 * @author John
 * @since 10/13/2014
 */
public class TestGribCollectionsBig {
  String topdir =  TestDir.cdmUnitTestDir + "gribCollections/rdavm";
  // String topdir =

  @BeforeClass
  static public void before() {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    PartitionCollection.initPartitionCache(50, 100, 150, -1);
    GribCollection.initDataRafCache(11, 50, -1);
  }

  @AfterClass
  static public void after() {
    GribIosp.setDebugFlags(new DebugFlagsImpl(""));
    Formatter out = new Formatter(System.out);
    PartitionCollection.getPartitionCache().showCache(out);
    PartitionCollection.getPartitionCache().clearCache(false);
    FileCache.shutdown();
    TestDir.checkLeaks();
    System.out.printf("countGC=%d%n", GribCollection.countGC);
    System.out.printf("countPC=%d%n", PartitionCollection.countPC);
  }

  @Test
  public void testGC() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/2008/2008.10/fnl_20081003_18_00.grib1.ncx2");
    //TestDir.checkLeaks();

    assert count.nread == 286;
    assert count.nmiss == 0;
  }

  @Test
  public void testPofG() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/2008/2008.10/ds083.2_Aggregation-2008.10.ncx2");
    //TestDir.checkLeaks();

    assert count.nread == 70928;
    assert count.nmiss == 0;
  }

  @Test
  public void testPofP() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/2008/ds083.2_Aggregation-2008.ncx2");
    // TestDir.checkLeaks();

    //assert count.nread > 70000;
    //assert count.nmiss == 0;
  }

  //@Test
  public void testPofPofP() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/ds083.2_Aggregation-grib1.ncx2");
    TestDir.checkLeaks();

    assert count.nread > 70000;
    assert count.nmiss == 0;
  }
}
