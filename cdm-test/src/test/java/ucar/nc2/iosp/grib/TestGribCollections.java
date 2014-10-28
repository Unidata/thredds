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
import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
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
 * Indicates that coordinates are not matching, because DGEX_CONUS is dense (hass data for each coordinate).
 * Note that not all grib collections will be dense.
 *
 * @author John
 * @since 10/13/2014
 */
public class TestGribCollections {

  @BeforeClass
  static public void before() {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    PartitionCollection.initPartitionCache(10, 100, -1);
    GribCollection.initDataRafCache(11, 100, -1);
  }

  @AfterClass
  static public void after() {
    GribIosp.setDebugFlags(new DebugFlagsImpl(""));
    Formatter out = new Formatter(System.out);
    PartitionCollection.getPartitionCache().showCache(out);
    GribCollection.getDataRafCache().showCache(out);
    FileCache.shutdown();
  }

  @Test
  public void testGC_Grib2() throws IOException {
    Count count = read(TestDir.cdmUnitTestDir + "ncss/GFS/Global_onedeg/GFS_Global_onedeg_20120911_1200.grib2.ncx2");

    assert count.nread == 23229;
    assert count.nmiss == 0;
  }

  @Test
  public void testPofG_Grib2() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    Count count = read(TestDir.cdmUnitTestDir + "ncss/GFS/Global_onedeg/GFS_Global_onedeg-Global_onedeg.ncx2");
    TestDir.checkLeaks();

    assert count.nread == 94352;
    assert count.nmiss == 0;
  }

  //// ncss/GFS/CONUS_80km/GFS_CONUS_80km-CONUS_80km.ncx2 has lots of missing records
  @Test
  public void testGC_Grib1() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    Count count = read(TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_1200.grib1.ncx2");
    TestDir.checkLeaks();

    assert count.nread == 7116;
    assert count.nmiss == 200;
  }


  @Test
  public void testPofG_Grib1() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    Count count = read(TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km-CONUS_80km.ncx2");
    TestDir.checkLeaks();

    assert count.nread == 81340;
    assert count.nmiss == 1801;
  }

  // @Test
  // RandomAccessFile gets opened 1441 times (!) for PofGC
  public void openFileProblem() throws IOException {
    RandomAccessFile.setDebugLeaks(true);
    // GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly Grib/indexOnlyShow"));
    String filename = "ncss/GFS/CONUS_80km/GFS_CONUS_80km-CONUS_80km.ncx2";
    try (GridDataset gds = GridDataset.open(TestDir.cdmUnitTestDir + filename)) {
      GridDatatype gdt = gds.findGridByName("TwoD/Absolute_vorticity_isobaric");
      assert gdt != null;
      TestGribCollections.Count count = TestGribCollections.read(gdt);
      System.out.printf("%n%50s == %d/%d%n", "total", count.nmiss, count.nread);
      TestDir.checkLeaks();

      assert count.nread == 1440;
      assert count.nmiss == 631;
    }
  }

  ///////////////////////////////////////////////////////////////

  public static Count read(String filename) {
    long start = System.currentTimeMillis();
    System.out.println("\n\nReading File " + filename);
    Count allCount = new Count();
    try (GridDataset gds = GridDataset.open(filename)) {
      for (GridDatatype gdt: gds.getGrids()) {
        Count count = read(gdt);
        System.out.printf("%80s == %d/%d%n", gdt.getFullName(), count.nmiss, count.nread);
        allCount.add(count);
      }
      long took = System.currentTimeMillis() - start;
      float r = ((float) took) / allCount.nread;
      System.out.printf("%n%80s == %d/%d%n", "total", allCount.nmiss, allCount.nread);
      System.out.printf("%n   that took %d secs total, %f msecs per record%n", took/1000, r);

    } catch (IOException ioe) {
      System.out.printf("%s%n", ioe);
      Formatter out = new Formatter(System.out);
      PartitionCollection.getPartitionCache().showCache(out);
    }

    return allCount;
  }

  public static Count read(GridDatatype gdt) throws IOException {
    Dimension rtDim = gdt.getRunTimeDimension();
    Dimension tDim = gdt.getTimeDimension();
    Dimension zDim = gdt.getZDimension();

    Count count = new Count();
    if (rtDim != null) {
      for (int rt=0; rt<rtDim.getLength(); rt++)
        read(gdt, count, rt, tDim, zDim);
    } else {
      read(gdt, count, -1, tDim, zDim);
    }
    return count;
  }

  private static void read(GridDatatype gdt, Count count, int rtIndex, Dimension timeDim, Dimension zDim) throws IOException {
    if (timeDim != null) {
      for (int t=0; t<timeDim.getLength(); t++)
        read(gdt, count, rtIndex, t, zDim);
    } else {
      read(gdt, count, rtIndex, -1, zDim);
    }
  }


  private static void read(GridDatatype gdt, Count count, int rtIndex, int tIndex, Dimension zDim) throws IOException {
    if (zDim != null) {
      for (int z=0; z<zDim.getLength(); z++)
        read(gdt, count, rtIndex, tIndex, z);
    } else {
      read(gdt, count, rtIndex, tIndex, -1);
    }
  }

  private static void read(GridDatatype gdt, Count count, int rtIndex, int tIndex, int zIndex) throws IOException {
    // int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index
    Array data = gdt.readDataSlice(rtIndex, -1, tIndex, zIndex, 10, 10);
    if (data.getSize() != 1 || data.getRank() != 0) {
      System.out.printf("%s size=%d rank=%d%n", gdt.getFullName(), data.getSize(), data.getRank());
      gdt.readDataSlice(rtIndex, -1, tIndex, zIndex, 10, 10); // debug
    }

    assert data.getSize() == 1 : gdt.getFullName() +" size = "+data.getSize();
    boolean ok = data.hasNext();
    assert ok;
    float val = data.nextFloat();
    //System.out.printf("%s size=%d rank=%d val=%f%n", gdt.getFullName(), data.getSize(), data.getRank(), val);
    if (Float.isNaN(val))
      count.nmiss++;
    count.nread++;
  }

  static public class Count {
    int nread;
    int nmiss;

    void add(Count c) {
      nread += c.nread;
      nmiss += c.nmiss;
    }
  }
}
