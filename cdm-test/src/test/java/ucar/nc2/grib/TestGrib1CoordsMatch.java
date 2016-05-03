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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.grib.collection.*;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Test Grib1Coords from data Match whats in the ncx3 files
 *
 * @author caron
 * @since 11/5/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestGrib1CoordsMatch {
  private static FeatureCollectionConfig config = new FeatureCollectionConfig(); // default values

  @BeforeClass
  static public void before() {
    GribIosp.debugIndexOnlyCount = 0;
    GribCollectionImmutable.countGC = 0;
    PartitionCollectionImmutable.countPC = 0;
    RandomAccessFile.enableDefaultGlobalFileCache();
    RandomAccessFile.setDebugLeaks(true);
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

  //@Test
  public void problem() throws IOException {
    long start = System.currentTimeMillis();
    String filename = "ncss/GFS/CONUS_80km/GFS_CONUS_80km-CONUS_80km.ncx3";
    try (GridDataset gds = GridDataset.open(TestDir.cdmUnitTestDir + filename)) {
      NetcdfFile ncfile = gds.getNetcdfFile();
      IOServiceProvider iosp = ncfile.getIosp();
      assert iosp instanceof GribIosp;
      iospGrib = (GribIosp) iosp;

      GridDatatype gdt = gds.findGridByName("TwoD/Total_precipitation_surface_Mixed_intervals_Accumulation");
      assert gdt != null;
      TestGribCollections.Count count = read(gdt);
      System.out.printf("%n%50s == %d/%d%n", "total", count.nmiss, count.nread);
      long took = System.currentTimeMillis() - start;
      float r = ((float) took) / count.nread;
      System.out.printf("%n   that took %d secs total, %f msecs per record%n", took / 1000, r);

      assert count.nread == 350;
      assert count.nmiss == 0;
      assert count.nerrs == 0;
    }

  }

  @Test
  public void testGC() throws IOException {
    TestGribCollections.Count count = read(TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141024/GFS_CONUS_80km_20141024_1200.grib1.ncx3");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assert count.nread == 7122;
    assert count.nmiss == 153;
    assert count.nerrs == 0;
  }

  @Test
  public void testPofG() throws IOException {                //ncss/GFS/CONUS_80km/GFS_CONUS_80km-CONUS_80km.ncx2
    TestGribCollections.Count count = read(TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141024/gfsConus80_46-20141024.ncx3");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assert count.nread == 36216;   // 1801/81340 ??
    assert count.nmiss == 771;
    assert count.nerrs == 0;
  }


  @Test
  public void testPofP() throws IOException {
    TestGribCollections.Count count = read(TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_46.ncx3");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assert count.nread == 50864;
    assert count.nmiss == 1081;
    assert count.nerrs == 0;
  }

  @Test
  public void testRdavmDs083p2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/PofP/ds083.2-pofp.ncx3";
    File fileInCache = GribIndexCache.getExistingFileOrCache(filename);
    assert fileInCache != null;
    TestGribCollections.Count count = read( fileInCache.getPath());

    // that took 63 secs total, 1.471143 msecs per record total == 4624/33718/43248
    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assert count.nread == 43248;
    assert count.nmiss == 2112;
    assert count.nerrs == 0;
  }

  /*
  Currently doesnt work with gbx9 files
  @Test
  public void testRdavmDs627p1() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    TestGribCollections.Count count = read("B:/rdavm/ds627.1/GCpass1-union-ds627.1.ncx2");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assert count.nread == 14280;
    assert count.nmiss == 14280;
    assert count.nerrs == 0;
    GribIosp.setDebugFlags(new DebugFlagsImpl(""));
  }  */

  ///////////////////////////////////////////////////////////////
  private GribIosp iospGrib;

  private TestGribCollections.Count read(String filename) {
    long start = System.currentTimeMillis();
    System.out.println("\n\nReading File " + filename);
    TestGribCollections.Count allCount = new TestGribCollections.Count();
    try (GridDataset gds = GridDataset.open(filename)) {
      NetcdfFile ncfile = gds.getNetcdfFile();
      IOServiceProvider iosp = ncfile.getIosp();
      assert iosp instanceof GribIosp;
      iospGrib = (GribIosp) iosp;

      for (GridDatatype gdt: gds.getGrids()) {
        TestGribCollections.Count count = read(gdt);
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
      GribCdmIndex.gribCollectionCache.showCache(out);
    }

    return allCount;
  }

  private GridCoordSystem gdc;
  private String var_desc;
  private Number var_param;
  private Number var_level_type;

  private CalendarDate runtimeCoord;
  private ArrayDouble.D3 timeCoord2DBoundsArray;

  private TestGribCollections.Count read(GridDatatype gdt) throws IOException {
    var_desc = gdt.findAttValueIgnoreCase("description", "");
    var_param = gdt.findAttributeIgnoreCase("Grib1_Parameter").getNumericValue();
    var_level_type = gdt.findAttributeIgnoreCase("Grib1_Level_Type").getNumericValue();

    gdc = gdt.getCoordinateSystem();

    Dimension rtDim = gdt.getRunTimeDimension();
    Dimension tDim = gdt.getTimeDimension();
    Dimension zDim = gdt.getZDimension();

    // loop over runtime
    TestGribCollections.Count count = new TestGribCollections.Count();
    if (rtDim != null) {
      CoordinateAxis1DTime rtcoord = gdc.getRunTimeAxis();

      for (int rt=0; rt<rtDim.getLength(); rt++) {
        runtimeCoord = rtcoord.getCalendarDate(rt);
        readTime(gdt, count, rt, tDim, zDim);
      }

    } else {
      runtimeCoord = null;
      readTime(gdt, count, -1, tDim, zDim);
    }
    timeCoord2DBoundsArray = null;
    return count;
  }

  private boolean hasTime;
  private double timeCoord;
  private double[] timeBounds;
  private boolean isTimeInterval;
  private CalendarDate timeCoordDate;
  private CalendarDate[] timeBoundsDate; // for intervals

  private void readTime(GridDatatype gdt, TestGribCollections.Count count, int rtIndex, Dimension timeDim, Dimension zDim) throws IOException {
    if (timeDim != null) {
      hasTime = true;
      CoordinateAxis tcoord = gdc.getTimeAxis();
      isTimeInterval = tcoord.isInterval();

      if (rtIndex < 0) {
        assert (tcoord instanceof CoordinateAxis1DTime);
        CoordinateAxis1DTime tcoord1D = (CoordinateAxis1DTime) tcoord;

        for (int t = 0; t < timeDim.getLength(); t++) {
          if (isTimeInterval) {
            timeBounds = tcoord1D.getCoordBounds(t);
            timeBoundsDate = tcoord1D.getCoordBoundsDate(t);
          } else {
            timeCoord = tcoord1D.getCoordValue(t);
            timeCoordDate = tcoord1D.getCalendarDate(t);
          }
          readVert(gdt, count, rtIndex, t, zDim);
        }
      } else {
        assert (tcoord instanceof CoordinateAxis2D);
        CoordinateAxis2D tcoord2D = (CoordinateAxis2D) tcoord;
        CoordinateAxisTimeHelper helper = tcoord2D.getCoordinateAxisTimeHelper();
        if (timeCoord2DBoundsArray == null)
          timeCoord2DBoundsArray = tcoord2D.getCoordBoundsArray();
        for (int t = 0; t < timeDim.getLength(); t++) {
          if (isTimeInterval) {
            timeBounds = new double[2];
            timeBounds[0] = timeCoord2DBoundsArray.get(rtIndex, t, 0);
            timeBounds[1] = timeCoord2DBoundsArray.get(rtIndex, t, 1);
            timeBoundsDate = new CalendarDate[2];
            timeBoundsDate[0] = helper.makeCalendarDateFromOffset(timeBounds[0]);
            timeBoundsDate[1] = helper.makeCalendarDateFromOffset(timeBounds[1]);
          } else {
            timeCoord = tcoord2D.getCoordValue(rtIndex, t);
            timeCoordDate = helper.makeCalendarDateFromOffset(timeCoord);
          }
          readVert(gdt, count, rtIndex, t, zDim);
        }

      }

    } else {
      hasTime = false;
      readVert(gdt, count, rtIndex, -1, zDim);
    }
  }

  private boolean hasVert;
  private double vertCoord;
  private double[] edge;
  private boolean isLayer;

  private void readVert(GridDatatype gdt, TestGribCollections.Count count, int rtIndex, int tIndex, Dimension zDim) throws IOException {
    if (zDim != null) {
      hasVert = true;
      CoordinateAxis1D zcoord = gdc.getVerticalAxis();
      isLayer = zcoord.isInterval();
      for (int z=0; z<zDim.getLength(); z++) {
        if (isLayer) {
          edge = zcoord.getCoordBounds(z);
        } else {
          vertCoord = zcoord.getCoordValue(z);
        }
        readAndTest(gdt, count, rtIndex, tIndex, z);
      }
    } else {
      hasVert = false;
      readAndTest(gdt, count, rtIndex, tIndex, -1);
    }
  }

  private boolean show = false;

  private void readAndTest(GridDatatype gdt, TestGribCollections.Count count, int rtIndex, int tIndex, int zIndex) throws IOException {
    iospGrib.clearLastRecordRead();

    Array data = gdt.readDataSlice(rtIndex, -1, tIndex, zIndex, -1, -1);

    Grib1Customizer cust = (Grib1Customizer) iospGrib.getGribCustomizer();
    Grib1Record grib1 = (Grib1Record) iospGrib.getLastRecordRead();
    if (grib1 == null) {
      count.nmiss++;
      count.nread++;
      return;
    }
    Record1Bean bean = new Record1Bean(cust, grib1);
    boolean paramOk = true;

    paramOk &= var_desc.equals(bean.getParamDesc());
    paramOk &=  var_param.intValue() == bean.getParamNo();
    paramOk &=  var_level_type.intValue() == bean.getLevelType();

    boolean runtimeOk = true;
    CalendarDate gribDate = null;
    if (runtimeCoord != null) {
      gribDate = bean.getReferenceDate();
      runtimeOk &= runtimeCoord.equals(gribDate);
    }

    boolean timeOk = true;
    if (hasTime) {
      if (isTimeInterval) {
        timeOk &= bean.isTimeInterval();
        // int[] intv = bean.getTimeInterval();
        //timeOk &= timeBounds[0] == intv[0];  // true if GC
        //timeOk &= timeBounds[1] == intv[1];  // true if GC
        CalendarDate[] dateFromGribRecord = bean.getTimeIntervalDates();
        timeOk &= timeBoundsDate[0].equals(dateFromGribRecord[0]);
        timeOk &= timeBoundsDate[1].equals(dateFromGribRecord[1]);

      } else {
        // timeOk &= timeCoord == bean.getTimeCoordValue();   // true if GC
        CalendarDate dateFromGribRecord = bean.getTimeCoordDate();
        timeOk &= timeCoordDate.equals(dateFromGribRecord);

      }
    }

    boolean vertOk = true;
    if (hasVert) {
      if (isLayer) {
        vertOk &= bean.isLayer();
        vertOk &= edge[0] == bean.getLevelLowValue();
        vertOk &= edge[1] == bean.getLevelHighValue();
      } else {
        vertOk &= vertCoord == bean.getLevelValue();
      }
    }

    boolean ok = paramOk && runtimeOk && timeOk && vertOk;
    if (show || !ok) {
      System.out.printf("%s%n", bean);
    }

    if (!ok)
      count.nerrs++;
    count.nread++;
  }

  public class Record1Bean {
    Grib1Customizer cust;
    Grib1Record gr;
    Grib1SectionGridDefinition gds;
    Grib1SectionProductDefinition pds;
    Grib1ParamLevel plevel;
    Grib1ParamTime ptime;
    Grib1Parameter param;
    int gdsHash;

    public Record1Bean(Grib1Customizer cust, Grib1Record gr) {
      this.cust = cust;
      this.gr = gr;
      gds = gr.getGDSsection();
      pds = gr.getPDSsection();
      plevel = cust.getParamLevel(pds);
      ptime = gr.getParamTime(cust);

      param = cust.getParameter(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber());
      gdsHash = gr.getGDS().hashCode();       // boolean useTableVersion, boolean intvMerge, boolean useCenter
    }

    @Override
    public String toString() {
      final Formatter sb = new Formatter();
      sb.format("Record dataStart=%s%n", gr.getDataSection().getStartingPosition());
      sb.format(" %s%n", param);
      sb.format(" cdmHash=%d%n", 0);
      sb.format(" reftime=%s%n", getReferenceDate());
      sb.format(" time=%s%n", getTimeCoord());
      sb.format(" level=%s type=%s (%d)%n", getLevel(), getLevelName(), getLevelType());
      return sb.toString();
    }

    public String getTableVersion() {
      return pds.getCenter() + "-" + pds.getSubCenter() + "-" + pds.getTableVersion();
    }

    public final CalendarDate getReferenceDate() {
      return pds.getReferenceDate();
    }

    public int getParamNo() {
      return pds.getParameterNumber();
    }

    public final int getLevelType() {
      return pds.getLevelType();
    }

    public String getParamDesc() {
      return (param == null) ? null : param.getDescription();
    }

    public String getName() {
      if (param == null) return null;
      return Grib1Iosp.makeVariableName(cust, config.gribConfig, pds);
    }

    public String getUnit() {
      return (param == null) ? null : param.getUnit();
    }

    public int getGds() {
      return gdsHash;
    }

    public int getGen() {
      return pds.getGenProcess();
    }

    /* public String getSubcenter() {
      return cust.getSubCenterName(pds.getSubCenter());
    } */

    public final String getLevelName() {
      Grib1ParamLevel plevel = cust.getParamLevel(pds);
      return plevel.getNameShort();
    }

    public final String getStatType() {
      Grib1ParamTime ptime = gr.getParamTime(cust);
      GribStatType stype = ptime.getStatType();
      return (stype == null) ? null : stype.name();
    }

    public String getHeader() {
      return new String(gr.getHeader()).trim();
    }

    public String getPeriod() {
      try {
        return GribUtils.getCalendarPeriod(pds.getTimeUnit()).toString();
      } catch (UnsupportedOperationException e) {
        return "Unknown Time Unit = "+ pds.getTimeUnit();
      }
    }

    public String getTimeTypeName() {
      return ptime.getTimeTypeName();
    }

    public int getTimeValue1() {
      return pds.getTimeValue1();
    }

    public int getTimeValue2() {
      return pds.getTimeValue2();
    }

    public int getTimeType() {
      return pds.getTimeRangeIndicator();
    }

    public double getTimeCoordValue() {
      return (double) ptime.getForecastTime();
    }

    public CalendarDate getTimeCoordDate() {
      int timeUnit = cust.convertTimeUnit( pds.getTimeUnit());
      return GribUtils.getValidTime(pds.getReferenceDate(), timeUnit,  ptime.getForecastTime());
    }

    public CalendarDate[] getTimeIntervalDates() {
      int[] intv = getTimeInterval();
      int timeUnit = cust.convertTimeUnit( pds.getTimeUnit());
      CalendarDate[] result = new CalendarDate[2];
      result[0] = GribUtils.getValidTime(pds.getReferenceDate(), timeUnit,  intv[0]);
      result[1] = GribUtils.getValidTime(pds.getReferenceDate(), timeUnit,  intv[1]);
      return result;
    }

    public boolean isTimeInterval() {
      return ptime.isInterval();
    }

    public int[] getTimeInterval() {
       return ptime.getInterval();
     }

     public String getTimeCoord() {
      if (ptime.isInterval()) {
        int[] intv = ptime.getInterval();
        return intv[0] + "-" + intv[1] + "("+ptime.getIntervalSize()+")";
      }
      return Integer.toString(ptime.getForecastTime());
    }

    public String getNIncludeMiss() {
      return pds.getNincluded()+"/"+pds.getNmissing();
    }

    public int getPertNum() {
      return pds.getPerturbationNumber();
    }

    public boolean isLayer() {
      return cust.isLayer(pds.getLevelType());
    }

    public String getLevel() {
      if (cust.isLayer(pds.getLevelType())) {
        return plevel.getValue1() + "-" + plevel.getValue2();
      }
      return Float.toString(plevel.getValue1());
    }

    public double getLevelValue() {
      return plevel.getValue1();
    }

    public double getLevelLowValue() {
      return Math.min(plevel.getValue1(), plevel.getValue2());
    }

    public double getLevelHighValue() {
      return Math.max(plevel.getValue1(), plevel.getValue2());
    }

    public long getLength() {
      return gr.getIs().getMessageLength();
    }

    public long getPos() {
      return gr.getDataSection().getStartingPosition();
    }

    public final int getFile() {
      return gr.getFile();
    }
  }

}
