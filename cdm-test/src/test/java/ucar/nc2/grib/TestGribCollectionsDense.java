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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
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
 * Look for missing data in Grib Collections.
 *
 * Indicates that coordinates are not matching, because DGEX_CONUS is dense (has data for each coordinate).
 * Note that not all grib collections will be dense.
 *
 * @author John
 * @since 10/13/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionsDense {

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
  public void testLeaf() throws IOException {
    TestGribCollections.Count count = TestGribCollections.read(TestDir.cdmUnitTestDir + "gribCollections/dgex/20141011/DGEX_CONUS_12km_20141011_0600.grib2");
    assert count.nread == 1009;
    assert count.nmiss == 0;
  }

  @Test
  public void testGC() throws IOException {
    TestGribCollections.Count count = TestGribCollections.read(TestDir.cdmUnitTestDir + "gribCollections/dgex/20141011/DGEX_CONUS_12km_20141011_1800.grib2.ncx3");
    assert count.nread == 1009;
    assert count.nmiss == 0;
  }

  @Test
  public void testPofG() throws IOException {
    TestGribCollections.Count count = TestGribCollections.read(TestDir.cdmUnitTestDir + "gribCollections/dgex/20141011/dgex_46-20141011.ncx3");
    assert count.nread == 3140;
    assert count.nmiss == 0;
  }

  @Test
  public void testPofP() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    TestGribCollections.Count count = TestGribCollections.read(TestDir.cdmUnitTestDir + "gribCollections/dgex/dgex_46.ncx3");
    TestDir.checkLeaks();
    assert count.nread == 5384;
    assert count.nmiss == 0;
  }

  @Test
  public void problem() throws IOException {
    String filename = "gribCollections/dgex/dgex_46.ncx3";
    //String filename = "/gribCollections/dgex/20141011/DGEX-test-20141011.ncx3";
    try (GridDataset gds = GridDataset.open(TestDir.cdmUnitTestDir + filename)) {
      GridDatatype gdt = gds.findGridByName("Best/Total_precipitation_surface_6_Hour_Accumulation");
      assert gdt != null;
      TestGribCollections.Count count = TestGribCollections.read(gdt);
      System.out.printf("%n%50s == %d/%d%n", "total", count.nmiss, count.nread);
      assert count.nread == 24;
      assert count.nmiss == 0;
    }
  }

}
