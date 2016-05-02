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

package ucar.nc2.grib;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.collection.PartitionCollectionImmutable;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Formatter;

/**
 * Look for missing data in large Grib Collections.
 * THese numbers wwill be different if we index with unionRuntimeCoords
 * Indicates that coordinates are not matching,
 *
 * @author John
 * @since 10/13/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionsBig {
  String topdir =  TestDir.cdmUnitTestDir + "gribCollections/rdavm";
  // String topdir =  "B:/rdavm";  // use for local windows to get around samba bug

  @BeforeClass
  static public void before() {
    GribIosp.debugIndexOnlyCount = 0;
    GribCollectionImmutable.countGC = 0;
    PartitionCollectionImmutable.countPC = 0;
    RandomAccessFile.enableDefaultGlobalFileCache();
    RandomAccessFile.setDebugLeaks(true);
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    GribCdmIndex.setGribCollectionCache(new ucar.nc2.util.cache.FileCacheGuava("GribCollectionCacheGuava", 100));
    GribCdmIndex.gribCollectionCache.resetTracking();
  }

  @AfterClass
  static public void after() {
    GribIosp.setDebugFlags(new DebugFlagsImpl());
    Formatter out = new Formatter(System.out);

    FileCacheIF cache = GribCdmIndex.gribCollectionCache;
    if (cache != null) {
      cache.showTracking(out);
      cache.showCache(out);
      cache.clearCache(false);
    }

    FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
    if (rafCache != null) {
      rafCache.showCache(out);
    }

    System.out.printf("            countGC=%7d%n", GribCollectionImmutable.countGC);
    System.out.printf("            countPC=%7d%n", PartitionCollectionImmutable.countPC);
    System.out.printf("    countDataAccess=%7d%n", GribIosp.debugIndexOnlyCount);
    System.out.printf(" total files needed=%7d%n", GribCollectionImmutable.countGC + PartitionCollectionImmutable.countPC + GribIosp.debugIndexOnlyCount);

    FileCache.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
    TestDir.checkLeaks();
    RandomAccessFile.setDebugLeaks(false);
  }


  @Test
  public void testSRC() throws IOException {
    TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/2008/2008.10/ds083.2_Aggregation-2008.10.ncx3");

    // roberto:    that took 6 secs total, 0.196227 msecs per record
    // jenkins:    that took 2 secs total, 0.083042 msecs per record

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);

    assert count.nread == 35464 : count.nread;
    assert count.nmiss == 0;
  }

  @Ignore("total == 366994/418704")
  @Test
  public void testTP() throws IOException {
    TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/2008/ds083.2_Aggregation-2008.ncx3");

    // roberto:
    // jenkins:  that took 32 secs total, 0.077869 msecs per record
    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);

    // LOOK 0/366994/418704
    assert count.nread == 837408 : count.nread;
    assert count.nmiss == 0;
  }

  @Test
  public void testTPofTP() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    TestGribCollections.Count count = TestGribCollections.read(topdir + "/ds083.2/grib1/ds083.2_Aggregation.ncx3");

    // ROBERTO (local drive only, samba fails) that took that took 2177 secs total, 0.156383 msecs per record
    // 2D only      486523/6476133
    // jenkems : that took 560 secs total, 0.079659 msecs per record

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);

    assert count.nerrs == 0;
    assert count.nmiss == 492158;       // 6032888/7034124  vs 973046/13925312 LOOK   6035386/7038851
    assert count.nread == 7038851;
  }

}
