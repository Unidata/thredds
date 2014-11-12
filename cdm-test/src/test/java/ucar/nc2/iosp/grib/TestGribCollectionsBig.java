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
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.collection.PartitionCollectionImmutable;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheIF;
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

  @BeforeClass
   static public void before() {
     RandomAccessFile.setDebugLeaks(true);
     GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    PartitionCollectionImmutable.initPartitionCache(50, 700, -1, -1);
   }

   @AfterClass
   static public void after() {
     GribIosp.setDebugFlags(new DebugFlagsImpl());
     FileCacheIF cache = PartitionCollectionImmutable.getPartitionCache();
     if (cache == null) return;

     Formatter out = new Formatter(System.out);
     cache.showCache(out);
     cache.showTracking(out);
     cache.clearCache(false);
     RandomAccessFile.getGlobalFileCache().showCache(out);
     TestDir.checkLeaks();

     System.out.printf("countGC=%d%n", GribCollectionImmutable.countGC);
     System.out.printf("countPC=%d%n", PartitionCollectionImmutable.countPC);

     FileCache.shutdown();
     RandomAccessFile.setDebugLeaks(false);
   }

  @Test
  public void testGC() throws IOException {
    TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/2008/2008.10/fnl_20081003_18_00.grib1.ncx2");
    TestDir.checkLeaks();

    assert count.nread == 286;
    assert count.nmiss == 0;
  }

  @Test
  public void testPofG() throws IOException {
    TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/2008/2008.10/ds083.2_Aggregation-2008.10.ncx2");
    TestDir.checkLeaks();

    // jenkins: that took 18 secs total, 0.261603 msecs per record
    assert count.nread == 70928;
    assert count.nmiss == 0;
  }

  @Test
  public void testPofP() throws IOException {
    try {
      TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/2008/ds083.2_Aggregation-2008.ncx2");
      TestDir.checkLeaks();

      // jenkins:  that took 496 secs total, 0.592712 msecs per record
      // that took 581 secs total, 0.694249 msecs per record (total == 0/837408) (cache size 500)
      assert count.nread == 837408;
      assert count.nmiss == 0;

    } catch (Throwable t) {
      t.printStackTrace();
      // TestDir.checkLeaks();
    }

  }

  // @Test
  public void testPofPofP() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/ds083.2_Aggregation-grib1.ncx2");
    TestDir.checkLeaks();

    // that took 1312 secs total, 0.784602 msecs per record (total == 0/1672528) (cache size 500)
    assert count.nread == 1672528;
    assert count.nmiss == 0;
  }
}
