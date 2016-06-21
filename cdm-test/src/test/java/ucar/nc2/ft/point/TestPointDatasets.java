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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
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
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.ft.PointFeatureCCC;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.ProfileFeatureCollection;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.StationProfileFeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.TrajectoryProfileFeature;
import ucar.nc2.ft.TrajectoryProfileFeatureCollection;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

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
    List<Object[]> result = new ArrayList<>();

    File topDir = new File(topdir);
    File[] filea = topDir.listFiles();
    if (filea == null) return result;

    List<FileSort> files = new ArrayList<>();
    for (File f : filea) {
      if (filter != null && !filter.accept(f)) continue;
      files.add( new FileSort(f));
    }
    Collections.sort(files);

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

  static boolean showStructureData = false;
  static boolean showAll = false;

  public static List<Object[]> getCFDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{TestDir.cdmUnitTestDir + "cfPoint/point/filtered_apriori_super_calibrated_binned1.nc", FeatureType.POINT, 1001});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "cfPoint/point/nmcbob.shp.nc", FeatureType.POINT, 1196});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "cfPoint/station/rig_tower.2009-02-01.ncml", FeatureType.STATION, 17280});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "cfPoint/station/billNewDicast.nc", FeatureType.STATION, 78912});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "cfPoint/station/billOldDicast.nc", FeatureType.STATION, 19728});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "cfPoint/station/sampleDataset.nc", FeatureType.STATION, 1728});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "cfPoint/trajectory/rt_20090512_willy2.ncml", FeatureType.TRAJECTORY, 53176});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "cfPoint/trajectory/p1140004.ncml", FeatureType.TRAJECTORY, 245});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "cfPoint/stationProfile/timeSeriesProfile-Ragged-SingleStation-H.5.3.nc", FeatureType.STATION_PROFILE, 40});

        // CF 1.0 multidim with dimensions reversed
    //testPointDataset(TestDir.cdmUnitTestDir+"cfPoint/station/solrad_point_pearson.ncml", FeatureType.STATION, true);

    return result;
  }

  public static List<Object[]> getPlugDatasets() {
    List<Object[]> result = new ArrayList<>();

    // cosmic
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/trajectory/cosmic/wetPrf_C005.2007.294.16.22.G17_0001.0002_nc", FeatureType.TRAJECTORY, 383});
    // ndbc
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/ndbc/41001h1976.nc", FeatureType.STATION, 1405});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/suomi/suoHWV_2006.105.00.00.0060_nc", FeatureType.STATION, 124});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/suomi/gsuPWV_2006.105.00.00.1440_nc", FeatureType.STATION, 4848});
    // fsl wind profilers
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/stationProfile/PROFILER_RASS_01hr_20091027_1500.nc", FeatureType.STATION_PROFILE, 198});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/stationProfile/PROFILER_RASS_06min_20091028_2318.nc", FeatureType.STATION_PROFILE, 198});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/stationProfile/PROFILER_wind_01hr_20091024_1200.nc", FeatureType.STATION_PROFILE, 1728});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/stationProfile/PROFILER_wind_06min_20091030_2330.nc", FeatureType.STATION_PROFILE, 2088});
    // netcdf buoy / synoptic / metars ( robb's perl decoder output)
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/netcdf/Surface_METAR_latest.nc", FeatureType.POINT, 7});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/netcdf/Surface_Buoy_20090921_0000.nc", FeatureType.POINT, 32452});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/netcdf/Surface_Synoptic_20090921_0000.nc", FeatureType.POINT, 1516});
    //RAF-Nimbus
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/trajectory/aircraft/135_ordrd.nc", FeatureType.TRAJECTORY, 7741});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/trajectory/aircraft/raftrack.nc", FeatureType.TRAJECTORY, 8157});
    // Madis
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/trajectory/acars/acars_20091109_0800.nc", FeatureType.TRAJECTORY, 5063});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/netcdf/19981110_1200", FeatureType.POINT, 2499});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/madis2/hydro/20050729_1200", FeatureType.STATION, 1374});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/sounding/netcdf/20070612_1200", FeatureType.STATION_PROFILE, 1788});
    // unidata point obs
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/200501q3h-gr.nc", FeatureType.STATION, 5023});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/netcdf/20080814_LMA.ncml", FeatureType.POINT, 277477});
    // nldn
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/200929100.ingest", FeatureType.POINT, 1165});
    // uspln
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/uspln_20061023.18", FeatureType.POINT, 3483});

    // FslRaob
    // assert 63 == checkPointDataset(TestDir.testdataDir + "sounding/netcdf/raob_soundings20216.cdf", FeatureType.STATION_PROFILE, false);
    //assert 4638 == checkPointDataset(TestDir.testdataDir + "sounding/netcdf/Upperair_20060621_0000.nc", FeatureType.STATION_PROFILE, false);

    return result;
  }

  // lots of trouble - remove for now
  public static List<Object[]> getGempakDatasets() {
    List<Object[]> result = new ArrayList<>();

    //gempack sounding
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/sounding/gempak/19580807_upa.ncml", FeatureType.STATION_PROFILE, 8769});

    // gempak surface
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/gempak/2009103008_sb.gem", FeatureType.POINT, 3337});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/point/gempak/2009110100_ship.gem", FeatureType.POINT, 938});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/gempak/20091030_syn.gem", FeatureType.POINT, 55856});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/gempak/20091030_syn.gem", FeatureType.STATION, 28328});

    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/sounding/gempak/19580807_upa.ncml", FeatureType.STATION_PROFILE, 8769});

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
  public void checkPointFeatureDataset() throws IOException {
    Assert.assertEquals("npoints", countExpected, checkPointFeatureDataset(location, ftype, show));
  }

  ///////////////////////////////////////////////////////////
  // static so can be used outside class

  // return number of PointFeatures
  public static int checkPointFeatureDataset(String location, FeatureType type, boolean show) throws IOException {
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
      if (showAll) {
        System.out.printf("----------- testPointDataset getDetailInfo -----------------%n");
        fdataset.getDetailInfo(out);
        System.out.printf("%s %n", out);
      } else {
        System.out.printf("  Feature Type %s %n", fdataset.getFeatureType());
      }

      return checkPointFeatureDataset(fdataset, show);
    }

  }

  public static int checkPointFeatureDataset(FeatureDataset fdataset, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;

    CalendarDate d1 = fdataset.getCalendarDateStart();
    CalendarDate d2 = fdataset.getCalendarDateEnd();
    if ((d1 != null) && (d2 != null))
      Assert.assertTrue("calendar date min <= max", d1.isBefore(d2) || d1.equals(d2));

    List<VariableSimpleIF> dataVars = fdataset.getDataVariables();
    Assert.assertNotNull("fdataset.getDataVariables()", dataVars);
    for (VariableSimpleIF v : dataVars) {
      Assert.assertNotNull(v.getShortName(), fdataset.getDataVariable(v.getShortName()));
    }

    // FeatureDatasetPoint
    Assert.assertTrue("fdataset instanceof FeatureDatasetPoint", fdataset instanceof FeatureDatasetPoint);
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    for (DsgFeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      checkDsgFeatureCollection(fc);

      if (fc instanceof PointFeatureCollection) {
        PointFeatureCollection pfc = (PointFeatureCollection) fc;
        count = checkPointFeatureCollection(pfc, show);
        Assert.assertEquals("PointFeatureCollection getData count = size", count, pfc.size());

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        count = checkStationFeatureCollection((StationTimeSeriesFeatureCollection) fc);
        //testNestedPointFeatureCollection((StationTimeSeriesFeatureCollection) fc, show);

      } else if (fc instanceof StationProfileFeatureCollection) {
        count = checkStationProfileFeatureCollection((StationProfileFeatureCollection) fc, show);
        if (showStructureData) showStructureData((StationProfileFeatureCollection) fc);

      } else if (fc instanceof TrajectoryProfileFeatureCollection) {
        count = checkSectionFeatureCollection((TrajectoryProfileFeatureCollection) fc, show);

      } else if (fc instanceof ProfileFeatureCollection) {
        count = checkProfileFeatureCollection((ProfileFeatureCollection) fc, show);

      } else  {
        count = checkOther(fc, show);
      }
      checkInfo(fc);
    }

    long took = System.currentTimeMillis() - start;
    System.out.printf(" nobs=%d took= %d msec%n", count, took);
    return count;
  }

  static void checkDsgFeatureCollection( DsgFeatureCollection dsg) throws IOException {
    String what = dsg.getClass().getName();
    Assert.assertNotNull(what + " name", dsg.getName());
    Assert.assertNotNull(what + " featureTYpe", dsg.getCollectionFeatureType());
    Assert.assertNotNull(what + " timeUnit", dsg.getTimeUnit());
    // Assert.assertNotNull(what + " altUnits", dsg.getAltUnits());
    // Assert.assertNotNull(what + " extraVars", dsg.getExtraVariables());
  }

  static void checkInfo( DsgFeatureCollection dsg) throws IOException {
    Assert.assertNotNull(dsg.getBoundingBox());
    Assert.assertNotNull(dsg.getCalendarDateRange());
    Assert.assertNotNull(dsg.size() > 0);
  }

  static int checkPointFeatureCollection(PointFeatureCollection pfc, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int counts = 0;
    for (PointFeature pf : pfc) {
      checkPointFeature(pf, pfc.getTimeUnit());
      counts++;
    }
    long took = System.currentTimeMillis() - start;
    if (show) System.out.println(" testPointFeatureCollection subset count= " + counts + " full iter took= " + took + " msec");

    checkPointFeatureCollectionBB(pfc, show);
    return counts;
  }

  static int checkProfileFeatureCollection(ProfileFeatureCollection profileFeatureCollection, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    Set<String> profileNames = new HashSet<>();
    for (ProfileFeature profile : profileFeatureCollection) {
      checkDsgFeatureCollection(profile);
      Assert.assertNotNull("ProfileFeature time", profile.getTime());
      Assert.assertNotNull("ProfileFeature latlon", profile.getLatLon());
      Assert.assertNotNull("ProfileFeature featureData", profile.getFeatureData());
      Assert.assertTrue(!profileNames.contains(profile.getName()));
      profileNames.add(profile.getName());

      // assert pf.getTime() != null;
      count += checkPointFeatureCollection(profile, show);
    }
    long took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testStationProfileFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    return count;
  }

  static int checkStationFeatureCollection(StationTimeSeriesFeatureCollection sfc) throws IOException {
    System.out.printf("--------------------------\nComplete Iteration for %s %n", sfc.getName());
    int countStns = countLocations(sfc);

    // try a subset
    LatLonRect bb = sfc.getBoundingBox();
    Assert.assertNotNull(bb);
    LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
    System.out.println(" BB Subset= " + bb2.toString2());
    StationTimeSeriesFeatureCollection sfcSub = sfc.subset(bb2);
    int countSub = countLocations(sfcSub);
    Assert.assertTrue(countSub <= countStns);
    System.out.println("  nobs= " + sfcSub.size());

    /* test info
    CollectionInfo info = new DsgCollectionHelper(sfc).calcBounds(); // sets internal values
    Assert.assertNotNull(info);
    CalendarDateRange dr = sfc.getCalendarDateRange();
    Assert.assertNotNull(dr);
    Assert.assertEquals(info.getCalendarDateRange(null), dr);

    // subset(bb, dr);
    long diff = dr.getEnd().getDifferenceInMsecs(dr.getStart());
    CalendarDate mid = dr.getStart().add(diff/2, CalendarPeriod.Field.Millisec);
    CalendarDateRange drsubset = CalendarDateRange.of(dr.getStart(), mid);
    System.out.println(" CalendarDateRange Subset= " + drsubset);
    StationTimeSeriesFeatureCollection sfcSub2 = sfc.subset(bb2, drsubset);
    for (StationTimeSeriesFeature sf : sfcSub2) {
      Assert.assertTrue( bb2.contains(sf.getLatLon()));
      for (PointFeature pf : sf) {
        Assert.assertEquals(sf.getLatLon(), pf.getLocation().getLatLon());
        Assert.assertTrue(drsubset.includes(pf.getObservationTimeAsCalendarDate()));
        // Assert.assertTrue( pf.getClass().getName(), pf instanceof StationFeature);
      }
    }
    System.out.println("  nobs= " + sfcSub2.size());
    Assert.assertTrue(sfcSub2.size() <= sfcSub.size()); // */

    System.out.println("Flatten= " + bb2.toString2());
    PointFeatureCollection flatten = sfc.flatten(bb2, null);
    int countFlat = countLocations(flatten);
    assert countFlat <= countStns;

    flatten = sfc.flatten(null, null,  null);
    return countObs(flatten);
  }

  static int countLocations(StationTimeSeriesFeatureCollection sfc) throws IOException {
    System.out.printf(" Station List Size = %d %n", sfc.getStationFeatures().size());

    // check uniqueness
    Map<String, StationTimeSeriesFeature> stns = new HashMap<>(5000);
    Map<MyLocation, StationTimeSeriesFeature> locs = new HashMap<>(5000);

    int dups = 0;
    for (StationTimeSeriesFeature sf : sfc) {
      StationTimeSeriesFeature other = stns.get(sf.getName());
      if (other != null && dups < 10) {
        System.out.printf("  duplicate name = %s %n", sf);
        System.out.printf("   of = %s %n", other);
        dups++;
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

    System.out.printf(" duplicate names = %d %n", dups);
    System.out.printf(" unique locs = %d %n", locs.size());
    System.out.printf(" unique stns = %d %n", stns.size());

    return stns.size();
  }

  static int checkStationProfileFeatureCollection(StationProfileFeatureCollection stationProfileFeatureCollection, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    for (StationProfileFeature spf : stationProfileFeatureCollection) {
      checkDsgFeatureCollection(spf);
      Assert.assertNotNull("StationProfileFeature latlon", spf.getLatLon());
      Assert.assertNotNull("StationProfileFeature featureData", spf.getFeatureData());

      // iterates through the profile but not the profile data
      List<CalendarDate> times = spf.getTimes();
      if (showAll) {
        System.out.printf("  times= ");
        for (CalendarDate t : times) System.out.printf("%s, ", t);
        System.out.printf("%n");
      }

      Set<String> profileNames = new HashSet<>();
      for (ProfileFeature profile : spf) {
        checkDsgFeatureCollection(profile);
        Assert.assertNotNull("ProfileFeature time", profile.getTime());
        Assert.assertNotNull("ProfileFeature latlon", profile.getLatLon());
        Assert.assertNotNull("ProfileFeature featureData", profile.getFeatureData());
        Assert.assertTrue(!profileNames.contains(profile.getName()));
        profileNames.add(profile.getName());

        if (show) System.out.printf(" ProfileFeature=%s %n", profile.getName());
        count += checkPointFeatureCollection(profile, show);
      }
    }
    long took = System.currentTimeMillis() - start;
    if (show) System.out.println(" testStationProfileFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    return count;
  }

  static int checkSectionFeatureCollection(TrajectoryProfileFeatureCollection sectionFeatureCollection, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    for (TrajectoryProfileFeature section : sectionFeatureCollection) {
      checkDsgFeatureCollection(section);
      Assert.assertNotNull("SectionFeature featureData", section.getFeatureData());

      Set<String> profileNames = new HashSet<>();
      for (ProfileFeature profile : section) {
        checkDsgFeatureCollection(profile);
        Assert.assertNotNull("ProfileFeature time", profile.getTime());
        Assert.assertNotNull("ProfileFeature latlon", profile.getLatLon());
        Assert.assertNotNull("ProfileFeature featureData", profile.getFeatureData());
        Assert.assertTrue(!profileNames.contains(profile.getName()));
        profileNames.add(profile.getName());

        if (show) System.out.printf(" ProfileFeature=%s %n", profile.getName());
        count += checkPointFeatureCollection(profile, show);
      }
    }
    long took = System.currentTimeMillis() - start;
    if (show) System.out.println(" testStationProfileFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    return count;
  }

  // stuff we havent got around to coding specific tests for
  static int checkOther(DsgFeatureCollection dsg, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;

    // we will just run through everything
    try {
      CollectionInfo info  = new DsgCollectionHelper(dsg).calcBounds();
      if (show) System.out.printf(" info=%s%n", info);
      count = info.nobs;

    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }

    long took = System.currentTimeMillis() - start;
    if (show) System.out.println(" testNestedPointFeatureCollection complete count= " + count + " full iter took= " + took + " msec");

    return count;
  }


  /////////////////////////////////////////////


  // check that the location and times are filled out
  // read and test the data
  static private void checkPointFeature(PointFeature pobs, CalendarDateUnit timeUnit) throws java.io.IOException {

    Assert.assertNotNull("PointFeature location", pobs.getLocation());
    Assert.assertNotNull("PointFeature time", pobs.getNominalTimeAsCalendarDate());
    Assert.assertNotNull("PointFeature dataAll", pobs.getDataAll());
    Assert.assertNotNull("PointFeature featureData", pobs.getFeatureData());

    Assert.assertEquals("PointFeature makeCalendarDate", timeUnit.makeCalendarDate(pobs.getObservationTime()), pobs.getObservationTimeAsCalendarDate());

    assert timeUnit.makeCalendarDate(pobs.getObservationTime()).equals(pobs.getObservationTimeAsCalendarDate());
    checkData( pobs.getDataAll());
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

  static void showStructureData(PointFeatureCCC ccc) throws IOException {
    PrintWriter pw = new PrintWriter(System.out);

    IOIterator<PointFeatureCC> iter = ccc.getCollectionIterator();
    while (iter.hasNext()) {
      PointFeatureCC cc = iter.next();
      System.out.printf(" 1.hashCode=%d %n", cc.hashCode());
      IOIterator<PointFeatureCollection> iter2 = cc.getCollectionIterator();
      while (iter2.hasNext()) {
        PointFeatureCollection pfc = iter2.next();
        System.out.printf("  2.hashcode%d %n", pfc.hashCode());

        for (ucar.nc2.ft.PointFeature pointFeature : pfc) {
          System.out.printf("   3.hashcode=%d %n", pointFeature.hashCode());
          StructureData sdata = pointFeature.getDataAll();
          NCdumpW.printStructureData(pw, sdata);
        }
      }
    }
  }

  /////////////////////////////////////////////////////

  static int checkPointFeatureCollectionBB(PointFeatureCollection pfc, boolean show) throws IOException {
    if (show) {
      System.out.printf("----------- testPointFeatureCollection -----------------%n");
      System.out.println(" test PointFeatureCollection " + pfc.getName());
      System.out.println(" calcBounds");
    }
    if (show) {
      System.out.println("  bb= " + pfc.getBoundingBox());
      System.out.println("  dateRange= " + pfc.getCalendarDateRange());
      System.out.println("  npts= " + pfc.size());
    }

    int n = pfc.size();
    if (n == 0) {
      System.out.println("  empty " + pfc.getName());
      return 0; // empty
    }

    LatLonRect bb = pfc.getBoundingBox();
    assert bb != null;
    CalendarDateRange dr = pfc.getCalendarDateRange();
    assert dr != null;

    // read all the data - check that it is contained in the bbox, dateRange
    if (show) System.out.println(" complete iteration");
    long start = System.currentTimeMillis();
    int count = 0;
    for (PointFeature pf : pfc) {
      checkPointFeature(pf, pfc.getTimeUnit());
      if (!bb.contains(pf.getLocation().getLatLon()))
        System.out.printf("  point not in BB = %s on %s %n", pf.getLocation().getLatLon(), pfc.getName());

      if (!dr.includes(pf.getObservationTimeAsCalendarDate()))
        System.out.printf("  date out of Range= %s on %s %n", pf.getObservationTimeAsCalendarDate(), pfc.getName());
      count++;
    }
    long took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testPointFeatureCollection complete count= " + count + " full iter took= " + took + " msec");

    // subset with a bounding box, test result is in the bounding box
    LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
    PointFeatureCollection subset = pfc.subset(bb2, null);
    if (show) System.out.println(" subset bb= " + bb2.toString2());
    assert subset != null;

    start = System.currentTimeMillis();
    int counts = 0;
    for (PointFeature pf : subset) {
      LatLonPoint llpt = pf.getLocation().getLatLon();
      if (!bb2.contains(llpt)) {
        System.out.printf("  point not in BB = %s on %s %n", llpt, pfc.getName());
        bb2.contains(llpt);
      }

      checkPointFeature(pf, pfc.getTimeUnit());
      counts++;
    }
    took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testPointFeatureCollection subset count= " + counts + " full iter took= " + took + " msec");

    return count;
  }

  ////////////////////////////////////////////////////////////

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

    List<DsgFeatureCollection> collectionList = fdpoint.getPointFeatureCollectionList();

    DsgFeatureCollection fc = collectionList.get(0);

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
