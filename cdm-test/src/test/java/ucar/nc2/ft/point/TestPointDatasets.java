/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.nc2.ft.point;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.NCdumpW;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.*;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Test PointFeatureTypes.
 *
 * @author caron
 * @since Dec 16, 2008
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestPointDatasets {

  public static List<Object[]> getAllFilesInDirectory(String topdir, FileFilter filter) {
    List<FileSort> files = new ArrayList<>();
    File topDir = new File(topdir);
    for (File f : topDir.listFiles()) {
      if (filter != null && !filter.accept(f)) continue;
      files.add( new FileSort(f));
    }
    Collections.sort(files);

    List<Object[]> result = new ArrayList<>();
    for (FileSort f : files) {
      result.add(new Object[] {f.path, FeatureType.ANY_POINT});
      System.out.printf("%s%n", f.path);
    }

    return result;
  }

  private static class FileSort implements Comparable<FileSort> {
    String path;
    int order = 10;

    FileSort(File f) {
      this.path = f.getPath();
      String name = f.getName().toLowerCase();
      if (name.contains("point")) order = 1;
      else if (name.contains("stationprofile")) order = 5;
      else if (name.contains("station")) order = 2;
      else if (name.contains("profile")) order = 3;
      else if (name.contains("traj")) order = 4;
      else if (name.contains("section")) order = 6;
    }

    @Override
    public int compareTo(FileSort o) {
      return order - o.order;
    }
  }

  //////////////////////////////////////////////////////////////////////

  public static String topdir = TestDir.cdmUnitTestDir;
  private static final boolean showStructureData = false;

  public static List<Object[]> getCFDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{topdir + "cfPoint/point/filtered_apriori_super_calibrated_binned1.nc", FeatureType.POINT, 1001});
    result.add(new Object[]{topdir + "cfPoint/point/nmcbob.shp.nc", FeatureType.POINT, 1196});
    result.add(new Object[]{topdir + "cfPoint/station/rig_tower.2009-02-01.ncml", FeatureType.STATION, 17280});
    result.add(new Object[]{topdir + "cfPoint/station/billNewDicast.nc", FeatureType.STATION, 78912});
    result.add(new Object[]{topdir + "cfPoint/station/billOldDicast.nc", FeatureType.STATION, 19728});
    result.add(new Object[]{topdir + "cfPoint/station/sampleDataset.nc", FeatureType.STATION, 1728});
    result.add(new Object[]{topdir + "cfPoint/trajectory/rt_20090512_willy2.ncml", FeatureType.TRAJECTORY, 53176});
    result.add(new Object[]{topdir + "cfPoint/trajectory/p1140004.ncml", FeatureType.TRAJECTORY, 245});
    result.add(new Object[]{topdir + "cfPoint/stationProfile/timeSeriesProfile-Ragged-SingleStation-H.5.3.nc", FeatureType.STATION_PROFILE, 40});

        // CF 1.0 multidim with dimensions reversed
    //testPointDataset(topdir+"cfPoint/station/solrad_point_pearson.ncml", FeatureType.STATION, true);

    return result;
  }

  public static List<Object[]> getPlugDatasets() {
    List<Object[]> result = new ArrayList<>();

    // cosmic
    result.add(new Object[]{topdir + "ft/trajectory/cosmic/wetPrf_C005.2007.294.16.22.G17_0001.0002_nc", FeatureType.TRAJECTORY, 383});
    // ndbc
    result.add(new Object[]{topdir + "ft/station/ndbc/41001h1976.nc", FeatureType.STATION, 1405});
    result.add(new Object[]{topdir + "ft/station/suomi/suoHWV_2006.105.00.00.0060_nc", FeatureType.STATION, 124});
    result.add(new Object[]{topdir + "ft/station/suomi/gsuPWV_2006.105.00.00.1440_nc", FeatureType.STATION, 4848});
    // fsl wind profilers
    result.add(new Object[]{topdir + "ft/stationProfile/PROFILER_RASS_01hr_20091027_1500.nc", FeatureType.STATION_PROFILE, 198});
    result.add(new Object[]{topdir + "ft/stationProfile/PROFILER_RASS_06min_20091028_2318.nc", FeatureType.STATION_PROFILE, 198});
    result.add(new Object[]{topdir + "ft/stationProfile/PROFILER_wind_01hr_20091024_1200.nc", FeatureType.STATION_PROFILE, 1728});
    result.add(new Object[]{topdir + "ft/stationProfile/PROFILER_wind_06min_20091030_2330.nc", FeatureType.STATION_PROFILE, 2088});
    //gempack sounding
    result.add(new Object[]{topdir + "ft/sounding/gempak/19580807_upa.ncml", FeatureType.STATION_PROFILE, 8769});
    // gempak surface
    result.add(new Object[]{topdir + "ft/point/gempak/2009103008_sb.gem", FeatureType.POINT, 3337});
    result.add(new Object[]{topdir + "ft/point/gempak/2009110100_ship.gem", FeatureType.POINT, 938});
    result.add(new Object[]{topdir + "ft/station/gempak/20091030_syn.gem", FeatureType.POINT, 55856});
    result.add(new Object[]{topdir + "ft/station/gempak/20091030_syn.gem", FeatureType.STATION, 28328});
    // netcdf buoy / synoptic / metars ( robb's perl decoder output)
    result.add(new Object[]{topdir + "ft/point/netcdf/Surface_METAR_latest.nc", FeatureType.POINT, 7});
    result.add(new Object[]{topdir + "ft/point/netcdf/Surface_Buoy_20090921_0000.nc", FeatureType.POINT, 32452});
    result.add(new Object[]{topdir + "ft/point/netcdf/Surface_Synoptic_20090921_0000.nc", FeatureType.POINT, 1516});
    //RAF-Nimbus
    result.add(new Object[]{topdir + "ft/trajectory/aircraft/135_ordrd.nc", FeatureType.TRAJECTORY, 7741});
    result.add(new Object[]{topdir + "ft/trajectory/aircraft/raftrack.nc", FeatureType.TRAJECTORY, 8157});
    // Madis
    result.add(new Object[]{topdir + "ft/trajectory/acars/acars_20091109_0800.nc", FeatureType.TRAJECTORY, 5063});
    result.add(new Object[]{topdir + "ft/point/netcdf/19981110_1200", FeatureType.POINT, 2499});
    result.add(new Object[]{topdir + "ft/station/madis2/hydro/20050729_1200", FeatureType.STATION, 1374});
    result.add(new Object[]{topdir + "ft/sounding/netcdf/20070612_1200", FeatureType.STATION_PROFILE, 1788});
    // unidata point obs
    result.add(new Object[]{topdir + "ft/station/200501q3h-gr.nc", FeatureType.STATION, 5023});
    result.add(new Object[]{topdir + "ft/point/netcdf/20080814_LMA.ncml", FeatureType.POINT, 277477});
    // nldn
    result.add(new Object[]{topdir + "ft/point/200929100.ingest", FeatureType.POINT, 1165});
    // uspln
    result.add(new Object[]{topdir + "ft/point/uspln_20061023.18", FeatureType.POINT, 3483});

    // FslRaob
    // assert 63 == checkPointDataset(TestDir.testdataDir + "sounding/netcdf/raob_soundings20216.cdf", FeatureType.STATION_PROFILE, false);
    //assert 4638 == checkPointDataset(TestDir.testdataDir + "sounding/netcdf/Upperair_20060621_0000.nc", FeatureType.STATION_PROFILE, false);

    return result;
  }

  public static List<Object[]> getGempakDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{topdir + "ft/sounding/gempak/19580807_upa.ncml", FeatureType.STATION_PROFILE, 8769});

    // (GEMPAK IOSP) stn = psuedoStruct, obs = multidim Structure, time(time) as extraJoin
    //checkPointDataset(TestDir.cdmUnitTestDir + "formats/gempak/surface/19580807_sao.gem", FeatureType.STATION, true);

    // stationAsPoint (GEMPAK IOSP) stn = psuedoStruct, obs = multidim Structure, time(time) as extraJoin
    //testPointDataset(TestDir.cdmUnitTestDir + "formats/gempak/surface/20090521_sao.gem", FeatureType.POINT, true);

    //testGempakAll(TestDir.cdmUnitTestDir + "formats/gempak/surface/20090524_sao.gem");
    //testGempakAll(TestDir.cdmUnitTestDir+"C:/data/ft/station/09052812.sf");

    //testPointDataset("collection:C:/data/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", FeatureType.STATION, true);
    //checkPointDataset("collection:D:/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", FeatureType.STATION, true);

    return result;
  }


  public static List<Object[]> getMiscDatasets() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/ldm/04061912_buoy.nc", FeatureType.POINT, 218});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/netcdf/Surface_Buoy_20090921_0000.nc", FeatureType.POINT, 32452});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/multiStationMultiVar.ncml", FeatureType.STATION, 15});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "cfPoint/station/sampleDataset.nc", FeatureType.STATION, 1728});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/200501q3h-gr.nc", FeatureType.STATION, 5023});  // */
    return result;
  }

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.addAll(getCFDatasets());
    result.addAll(getPlugDatasets());
    result.addAll(getGempakDatasets());
    result.addAll(getMiscDatasets());

    return result;
  }

  String location;
  FeatureType ftype;
  int countExpected;
  boolean show = false;

  public TestPointDatasets(String location, FeatureType ftype, int countExpected) {
    this.location = location;
    this.ftype = ftype;
    this.countExpected = countExpected;
  }

  @Test
  public void checkPointDataset() throws IOException {
    assert countExpected == checkPointDataset(location, ftype, show);
  }

  public static int checkPointDataset(String location, FeatureType type, boolean show) throws IOException {
    File fileIn = new File(location);
    String absIn = fileIn.getCanonicalPath();
    absIn = StringUtil2.replace(absIn, "\\", "/");

    System.out.printf("================ TestPointFeatureCollection read %s %n", absIn);

    Formatter out = new Formatter();
    try (FeatureDataset fdataset = FeatureDatasetFactoryManager.open(type, location, null, out)) {
      if (fdataset == null) {
        System.out.printf("**failed on %s %n --> %s %n", location, out);
        assert false;
      }

      // FeatureDataset
      if (show) {
        System.out.printf("----------- testPointDataset getDetailInfo -----------------%n");
        fdataset.getDetailInfo(out);
        System.out.printf("%s %n", out);
      } else {
        System.out.printf("  Feature Type %s %n", fdataset.getFeatureType());
      }

      return checkPointDataset(fdataset, show);
    }
  }


  public static int checkPointDataset(FeatureDataset fdataset, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;

    Date d1 = fdataset.getStartDate();
    Date d2 = fdataset.getEndDate();
    if ((d1 != null) && (d2 != null))
      assert d1.before(d2) || d1.equals(d2);

    List<VariableSimpleIF> dataVars = fdataset.getDataVariables();
    assert dataVars != null;
    for (VariableSimpleIF v : dataVars) {
      assert null != fdataset.getDataVariable(v.getShortName());
    }

    // FeatureDatasetPoint
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (fc instanceof PointFeatureCollection) || (fc instanceof NestedPointFeatureCollection) : fc.getClass().getName();

      if (fc instanceof PointFeatureCollection) {
        PointFeatureCollection pfc = (PointFeatureCollection) fc;
        count = checkPointFeatureCollection(pfc, show);
        System.out.println("PointFeatureCollection getData count= " + count + " size= " + pfc.size());
        assert count == pfc.size();

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        count = checkStationFeatureCollection((StationTimeSeriesFeatureCollection) fc);
        //testNestedPointFeatureCollection((StationTimeSeriesFeatureCollection) fc, show);

      } else if (fc instanceof StationProfileFeatureCollection) {
        count = checkStationProfileFeatureCollection((StationProfileFeatureCollection) fc, show);
        if (showStructureData) showStructureData((StationProfileFeatureCollection) fc);

      } else if (fc instanceof SectionFeatureCollection) {
        count = checkSectionFeatureCollection((SectionFeatureCollection) fc, show);

      } else if (fc instanceof ProfileFeatureCollection) {
        count = checkProfileFeatureCollection((ProfileFeatureCollection) fc, show);

      } else {
        count = checkNestedPointFeatureCollection((NestedPointFeatureCollection) fc, show);
      }
    }

    long took = System.currentTimeMillis() - start;
    System.out.printf(" nobs=%d took= %d msec%n", count, took);
    return count;
  }

  static int checkNestedPointFeatureCollection(NestedPointFeatureCollection npfc, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    PointFeatureCollectionIterator iter = npfc.getPointFeatureCollectionIterator(-1);
    while (iter.hasNext()) {
      PointFeatureCollection pfc = iter.next();
      if (show)
        System.out.printf(" PointFeatureCollection=%s %n", pfc);
      count += checkPointFeatureCollection(pfc, show);
    }
    long took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testNestedPointFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    return count;
  }

  static int checkStationProfileFeatureCollection(StationProfileFeatureCollection stationProfileFeatureCollection, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    stationProfileFeatureCollection.resetIteration();
    while (stationProfileFeatureCollection.hasNext()) {
      ucar.nc2.ft.StationProfileFeature spf = stationProfileFeatureCollection.next();
      List<Date> times = spf.getTimes();
      if (show) {
        System.out.printf("times= ");
        for (Date t : times) System.out.printf("%s, ", t);
        System.out.printf("%n");
      }

      spf.resetIteration();
      while (spf.hasNext()) {
        ucar.nc2.ft.ProfileFeature pf = spf.next();
        assert pf.getName() != null;
        //assert pf.getTime() != null;

        if (show)
          System.out.printf(" ProfileFeature=%s %n", pf);
        count += checkPointFeatureCollection(pf, show);
      }
    }
    long took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testStationProfileFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    return count;
  }

  static int checkSectionFeatureCollection(SectionFeatureCollection sectionFeatureCollection, boolean show) throws IOException {
     long start = System.currentTimeMillis();
     int count = 0;
     sectionFeatureCollection.resetIteration();
     while (sectionFeatureCollection.hasNext()) {
       ucar.nc2.ft.SectionFeature spf = sectionFeatureCollection.next();

       spf.resetIteration();
       while (spf.hasNext()) {
         ucar.nc2.ft.ProfileFeature pf = spf.next();
         assert pf.getName() != null;
         // assert pf.getTime() != null;

         if (show)
           System.out.printf(" ProfileFeature=%s %n", pf);
         count += checkPointFeatureCollection(pf, show);
       }
     }
     long took = System.currentTimeMillis() - start;
     if (show)
       System.out.println(" testStationProfileFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
     return count;
   }

  static int checkProfileFeatureCollection(ProfileFeatureCollection profileFeatureCollection, boolean show) throws IOException {
     long start = System.currentTimeMillis();
     int count = 0;
     profileFeatureCollection.resetIteration();
     while (profileFeatureCollection.hasNext()) {
       ucar.nc2.ft.ProfileFeature pf = profileFeatureCollection.next();
       assert pf.getName() != null;
       // assert pf.getTime() != null;
       count += checkPointFeatureCollection(pf, show);
     }
     long took = System.currentTimeMillis() - start;
     if (show)
       System.out.println(" testStationProfileFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
     return count;
   }

  static void showStructureData(StationProfileFeatureCollection stationProfileFeatureCollection) throws IOException {
    PrintWriter pw = new PrintWriter(System.out);

    stationProfileFeatureCollection.resetIteration();
    while (stationProfileFeatureCollection.hasNext()) {
      ucar.nc2.ft.StationProfileFeature stationProfile = stationProfileFeatureCollection.next();
      System.out.printf("stationProfile=%d %n", stationProfile.hashCode());
      stationProfile.resetIteration();
      while (stationProfile.hasNext()) {
        ucar.nc2.ft.ProfileFeature profile = stationProfile.next();
        System.out.printf("-profile=%d %n", profile.hashCode());

        profile.resetIteration();
        while (profile.hasNext()) {
          ucar.nc2.ft.PointFeature pointFeature = profile.next();
          System.out.printf("--pointFeature=%d %n", pointFeature.hashCode());
          StructureData sdata = pointFeature.getDataAll();
          NCdumpW.printStructureData(pw, sdata);
        }
      }
    }
  }

  static int checkPointFeatureCollection(PointFeatureCollection pfc, boolean show) throws IOException {
    if (show) {
      System.out.printf("----------- testPointFeatureCollection -----------------%n");
      System.out.println(" test PointFeatureCollection " + pfc.getName());
      System.out.println(" calcBounds");
    }
    pfc.calcBounds();
    if (show) {
      System.out.println("  bb= " + pfc.getBoundingBox());
      System.out.println("  dateRange= " + pfc.getDateRange());
      System.out.println("  npts= " + pfc.size());
    }

    int n = pfc.size();
    if (n <= 0) {
      System.out.println("  empty " + pfc.getName());
      //pfc.calcBounds();
      return 0; // empty
    }

    LatLonRect bb = pfc.getBoundingBox();
    assert bb != null;
    DateRange dr = pfc.getDateRange();
    assert dr != null;

    // read all the data - check that it is contained in the bbox, dateRange
    if (show) System.out.println(" complete iteration");
    long start = System.currentTimeMillis();
    int count = 0;
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      checkPointFeature(pf, pfc.getTimeUnit());
      assert bb.contains(pf.getLocation().getLatLon()) : pf.getLocation().getLatLon();
      if (!dr.contains(pf.getObservationTimeAsDate()))
        System.out.printf("  date out of Range= %s on %s %n", pf.getObservationTimeAsDate(), pfc.getName());
      count++;
    }
    long took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testPointFeatureCollection complete count= " + count + " full iter took= " + took + " msec");

    // subset with a bounding box, test result is in the bounding box
    LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
    PointFeatureCollection subset = pfc.subset(bb2, (CalendarDateRange) null);
    if (show) System.out.println(" subset bb= " + bb2.toString2());

    start = System.currentTimeMillis();
    int counts = 0;
    PointFeatureIterator iters = subset.getPointFeatureIterator(-1);
    while (iters.hasNext()) {
      PointFeature pf = iters.next();
      assert pf != null;
      assert pf.getLocation() != null;

      assert bb2.contains(pf.getLocation().getLatLon()) : bb2.toString2() + " does not contains point " + pf.getLocation().getLatLon();
      //System.out.printf(" contains point %s%n",pf.getLocation().getLatLon());

      checkPointFeature(pf, pfc.getTimeUnit());
      counts++;
    }
    took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testPointFeatureCollection subset count= " + counts + " full iter took= " + took + " msec");

    return count;
  }

  // check that the location and times are filled out
  // read and test the data
  static private void checkPointFeature(PointFeature pobs, DateUnit timeUnit) throws java.io.IOException {

    EarthLocation loc = pobs.getLocation();
    assert loc != null;

    assert null != pobs.getNominalTimeAsDate();
    assert null != pobs.getObservationTimeAsDate();

    assert timeUnit.makeDate(pobs.getNominalTime()).equals(pobs.getNominalTimeAsDate());
    assert timeUnit.makeDate(pobs.getObservationTime()).equals(pobs.getObservationTimeAsDate());


    StructureData sdata = pobs.getDataAll();
    assert null != sdata;
    checkData(sdata);
  }

  // read each field, check datatype
  static private void checkData(StructureData sdata) {

    for (StructureMembers.Member member : sdata.getMembers()) {
      DataType dt = member.getDataType();
      if (dt == DataType.FLOAT) {
        sdata.getScalarFloat(member);
        sdata.getJavaArrayFloat(member);
      } else if (dt == DataType.DOUBLE) {
        sdata.getScalarDouble(member);
        sdata.getJavaArrayDouble(member);
      } else if (dt == DataType.BYTE) {
        sdata.getScalarByte(member);
        sdata.getJavaArrayByte(member);
      } else if (dt == DataType.SHORT) {
        sdata.getScalarShort(member);
        sdata.getJavaArrayShort(member);
      } else if (dt == DataType.INT) {
        sdata.getScalarInt(member);
        sdata.getJavaArrayInt(member);
      } else if (dt == DataType.LONG) {
        sdata.getScalarLong(member);
        sdata.getJavaArrayLong(member);
      } else if (dt == DataType.CHAR) {
        sdata.getScalarChar(member);
        sdata.getJavaArrayChar(member);
        sdata.getScalarString(member);
      } else if (dt == DataType.STRING) {
        sdata.getScalarString(member);
      }

      if ((dt != DataType.STRING) && (dt != DataType.CHAR) && (dt != DataType.STRUCTURE) && (dt != DataType.SEQUENCE)) {
        sdata.convertScalarFloat(member.getName());
      }

    }
  }

  ////////////////////////////////////////////////////////////

  static int checkStationFeatureCollection(StationTimeSeriesFeatureCollection sfc) throws IOException {
    System.out.printf("--------------------------\nComplete Iteration for %s %n", sfc.getName());
    int countStns = countLocations(sfc);

    // try a subset
    LatLonRect bb = sfc.getBoundingBox();
    assert bb != null;
    LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
    System.out.println("Subset= " + bb2.toString2());
    StationTimeSeriesFeatureCollection sfcSub = sfc.subset(bb2);
    int countSub = countLocations(sfcSub);
    assert countSub <= countStns;

    System.out.println("Flatten= " + bb2.toString2());
    PointFeatureCollection flatten = sfc.flatten(bb2, (CalendarDateRange) null);
    int countFlat = countLocations(flatten);
    assert countFlat <= countStns;

    flatten = sfc.flatten(null, (CalendarDateRange) null);
    return countObs(flatten);
  }

  static int countLocations(StationTimeSeriesFeatureCollection sfc) throws IOException {
    System.out.printf(" Station List Size = %d %n", sfc.getStations().size());

    // check uniqueness
    Map<String, StationTimeSeriesFeature> stns = new HashMap<>(5000);
    Map<MyLocation, StationTimeSeriesFeature> locs = new HashMap<>(5000);

    sfc.resetIteration();
    while (sfc.hasNext()) {
      StationTimeSeriesFeature sf = sfc.next();
      StationTimeSeriesFeature other = stns.get(sf.getName());
      if (other != null) {
        System.out.printf("  duplicate name = %s %n", sf);
        System.out.printf("   of = %s %n", other);
      } else
        stns.put(sf.getName(), sf);

      MyLocation loc = new MyLocation(sf);
      StationTimeSeriesFeature already = locs.get(loc);
      if (already != null) {
        System.out.printf("  duplicate location %s(%s) of %s(%s) %n", sf.getName(), sf.getDescription(),
                already.getName(), already.getDescription());
      } else
        locs.put(loc, sf);
    }

    System.out.printf(" unique locs = %d %n", locs.size());
    System.out.printf(" unique stns = %d %n", stns.size());

    return stns.size();
  }


  /////////////////////////////////////////////////////////


  public static void checkLocation(String location, FeatureType type, boolean show) throws IOException {
    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(type, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", location, out);
      assert false;
    }
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    List<FeatureCollection> collectionList = fdpoint.getPointFeatureCollectionList();

    FeatureCollection fc = collectionList.get(0);

    if (fc instanceof PointFeatureCollection) {
      PointFeatureCollection pfc = (PointFeatureCollection) fc;
      countLocations(pfc);

      LatLonRect bb = pfc.getBoundingBox();
      LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
      PointFeatureCollection subset = pfc.subset(bb2, (CalendarDateRange) null);
      countLocations(subset);

    } else if (fc instanceof StationTimeSeriesFeatureCollection) {
      StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) fc;
      PointFeatureCollection pfcAll = sfc.flatten(null, (CalendarDateRange) null);
      System.out.printf("Unique Locations all = %d %n", countLocations(pfcAll));

      LatLonRect bb = sfc.getBoundingBox();
      assert bb != null;
      LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
      PointFeatureCollection pfcSub = sfc.flatten(bb2, (CalendarDateRange) null);
      System.out.printf("Unique Locations sub1 = %d %n", countLocations(pfcSub));

      StationTimeSeriesFeatureCollection sfcSub = sfc.subset(bb2);
      PointFeatureCollection pfcSub2 = sfcSub.flatten(null, (CalendarDateRange) null);
      System.out.printf("Unique Locations sub2 = %d %n", countLocations(pfcSub2));

      // Dons
      sfc = sfc.subset(bb2);
      PointFeatureCollection subDon = sfc.flatten(bb2, (CalendarDateRange) null);
      System.out.printf("Unique Locations subDon = %d %n", countLocations(subDon));
    }

  }

  static int countLocations(PointFeatureCollection pfc) throws IOException {
    int count = 0;
    Set<MyLocation> locs = new HashSet<>(80000);
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      MyLocation loc = new MyLocation(pf.getLocation());
      if (!locs.contains(loc)) locs.add(loc);
      count++;
      //if (count % 1000 == 0) System.out.printf("Count %d%n", count);
    }

    System.out.printf("Count Points  = %d Unique points = %d %n", count, locs.size());
    return locs.size();

    //The problem is that all the locations are coming up with the same value.  This:
    //always returns the same lat/lon/alt (of the first observation).
    //(pos was populated going through the PointFeatureIterator).

  }

  static int countObs(PointFeatureCollection pfc) throws IOException {
    int count = 0;
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      StructureData sd = pf.getDataAll();
      count++;
    }
    return count;
  }

  private static class MyLocation {
    double lat, lon, alt;

    public MyLocation(EarthLocation from) {
      this.lat = from.getLatitude();
      this.lon = from.getLongitude();
      this.alt = Double.isNaN(from.getAltitude()) ? 0.0 : from.getAltitude();
    }

    @Override
    public boolean equals(Object oo) {
      if (this == oo) return true;
      if (!(oo instanceof MyLocation)) return false;
      MyLocation other = (MyLocation) oo;
      return (lat == other.lat) && (lon == other.lon) && (alt == other.alt);
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        result += 37 * result + lat * 10000;
        result += 37 * result + lon * 10000;
        result += 37 * result + alt * 10000;
        hashCode = result;
      }
      return hashCode;
    }

    private int hashCode = 0;
  }

}
