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
import ucar.ma2.*;
import ucar.nc2.Dimension;
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
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Look for missing data in Grib Collections.
 * <p/>
 * Indicates that coordinates are not matching, because DGEX_CONUS is dense (has data for each coordinate).
 * Note that not all grib collections will be dense.
 *
 * @author John
 * @since 10/13/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollections {

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
  public void testGC_Grib2() throws IOException {
    Count count = read(TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/GFS_Global_2p5deg_20150301_1200.grib2.ncx3");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assert count.nread == 29567 : count.nread;
    assert count.nmiss == 596;
    assert count.nerrs == 0;
  }

  @Test
  public void testPofG_Grib2() throws IOException {
    Count count = read(TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assert count.nread == 172166 : count.nread;
    assert count.nmiss == 5023;
    assert count.nerrs == 0;
  }

  //// ncss/GFS/CONUS_80km/GFS_CONUS_80km-CONUS_80km.ncx3 has lots of missing records
  @Test
  public void testGC_Grib1() throws IOException {
    Count count = read(TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141024/GFS_CONUS_80km_20141024_0000.grib1.ncx3");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assert count.nread == 7122 : count.nread;
    assert count.nmiss == 153;
    assert count.nerrs == 0;
  }

  @Test
  public void testPofG_Grib1() throws IOException {
    Count count = read(TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141024/gfsConus80_46-20141024.ncx3");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assert count.nread == 36216 : count.nread;
    assert count.nmiss == 771;
    assert count.nerrs == 0;
  }

  @Test
  public void testPofP_Grib1() throws IOException {
    Count count = read(TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_46.ncx3");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assert count.nread == 50864 : count.nread;
    assert count.nmiss == 1081;
    assert count.nerrs == 0;
  }

  @Test
  public void problem() throws IOException {

    long start = System.currentTimeMillis();
    // String filename = "B:/rdavm/ds083.2/grib1/ds083.2_Aggregation-grib1.ncx3";
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3";
    try (GridDataset gds = GridDataset.open(filename)) {
      GridDatatype gdt = gds.findGridByName("Best/Latent_heat_net_flux_surface_Mixed_intervals_Average");
      assert gdt != null;

      int n = 22;
      int first = 23;
      double sum = 0;
      for (int time=first; time < first+n; time++) {
        Array data = gdt.readDataSlice(0, -0, time, 0, -1, -1);
        sum += MAMath.sumDouble(data);
      }
      System.out.printf("sum = %s%n", sum);

      long took = System.currentTimeMillis() - start;
      float r = ((float) took) / n;
      System.out.printf("%n   that took %d secs total, %d records %f msecs per record%n", took / 1000, n, r);
    }
  }


  @Test
  // RandomAccessFile gets opened 1441 times (!) for PofGC
  public void openFileProblem() throws IOException {

    long start = System.currentTimeMillis();
    // String filename = "B:/rdavm/ds083.2/grib1/ds083.2_Aggregation-grib1.ncx3";
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3";
    try (GridDataset gds = GridDataset.open(filename)) {
      GridDatatype gdt = gds.findGridByName("Best/Latent_heat_net_flux_surface_Mixed_intervals_Average");
      assert gdt != null;

      int n = 5;
      int first = 17;
      double sum = 0;
      for (int time = first; time < first + n; time++) {
        Array data = gdt.readDataSlice(0, -0, time, 0, -1, -1);
        sum += MAMath.sumDouble(data);
      }
      System.out.printf("sum = %s%n", sum);

      long took = System.currentTimeMillis() - start;
      float r = ((float) took) / n;
      System.out.printf("%n   that took %d secs total, %d records %f msecs per record%n", took / 1000, n, r);
    }
  }

  ///////////////////////////////////////////////////////////////

  public static Count read(String filename) {
    long start = System.currentTimeMillis();
    System.out.println("\n\nReading File " + filename);
    Count allCount = new Count();
    try (GridDataset gds = GridDataset.open(filename)) {
      for (GridDatatype gdt : gds.getGrids()) {
        Count count = read(gdt);
        System.out.printf("%80s == %d/%d%n", gdt.getFullName(), count.nmiss, count.nread);
        allCount.add(count);
      }
      long took = System.currentTimeMillis() - start;
      float r = ((float) took) / allCount.nread;
      System.out.printf("%n%80s == %d/%d%n", "total", allCount.nmiss, allCount.nread);
      System.out.printf("%n   that took %d secs total, %f msecs per record%n", took / 1000, r);

    } catch (IOException ioe) {
      ioe.printStackTrace();
      Formatter out = new Formatter(System.out);
      GribCdmIndex.gribCollectionCache.showCache(out);
    }

    return allCount;
  }

  public static Count read(GridDatatype gdt) throws IOException {
    Dimension rtDim = gdt.getRunTimeDimension();
    Dimension tDim = gdt.getTimeDimension();
    Dimension zDim = gdt.getZDimension();

    Count count = new Count();
    if (rtDim != null) {
      for (int rt = 0; rt < rtDim.getLength(); rt++)
        read(gdt, count, rt, tDim, zDim);
    } else {
      read(gdt, count, -1, tDim, zDim);
    }
    return count;
  }

  private static void read(GridDatatype gdt, Count count, int rtIndex, Dimension timeDim, Dimension zDim) throws IOException {
    if (timeDim != null) {
      for (int t = 0; t < timeDim.getLength(); t++)
        read(gdt, count, rtIndex, t, zDim);
    } else {
      read(gdt, count, rtIndex, -1, zDim);
    }
  }


  private static void read(GridDatatype gdt, Count count, int rtIndex, int tIndex, Dimension zDim) throws IOException {
    if (zDim != null) {
      for (int z = 0; z < zDim.getLength(); z++)
        read(gdt, count, rtIndex, tIndex, z);
    } else {
      read(gdt, count, rtIndex, tIndex, -1);
    }
  }

  private static void read(GridDatatype gdt, Count count, int rtIndex, int tIndex, int zIndex) throws IOException {
    // int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index
    Array data = gdt.readDataSlice(rtIndex, -1, tIndex, zIndex, -1, -1);
    /* if (data.getSize() != 1 || data.getRank() != 0) {
      System.out.printf("%s size=%d rank=%d%n", gdt.getFullName(), data.getSize(), data.getRank());
      gdt.readDataSlice(rtIndex, -1, tIndex, zIndex, 10, 10); // debug
    }  */

    // subset the array by striding x,y by 100
    Array dataSubset;
    try {
      Section s = new Section(data.getShape());
      List<Range> ranges = s.getRanges();
      int rank = ranges.size();
      assert rank >= 2;
      List<Range> subset = new ArrayList<>(rank);
      for (int i = 0; i < rank; i++) {
        if (i < rank - 2)
          subset.add(ranges.get(i));
        else {
          Range r = ranges.get(i);
          Range strided = new Range(r.first(), r.last(), 33);
          subset.add(strided);
        }
      }
      dataSubset = data.section(subset);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      return;
    }
    // System.out.printf("data=%d subset=%d%n", data.getSize(), dataSubset.getSize());

    // they all have to be missing values
    boolean isMissing = true;
    while (dataSubset.hasNext()) {
      float val = dataSubset.nextFloat();
      if (!Float.isNaN(val))
        isMissing = false;
    }
    if (isMissing)
      count.nmiss++;
    count.nread++;
  }

  static public class Count {
    int nread;
    int nmiss;
    int nerrs;

    void add(Count c) {
      nread += c.nread;
      nmiss += c.nmiss;
      nerrs += c.nerrs;
    }
  }
}
