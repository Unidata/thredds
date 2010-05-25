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

package ucar.nc2.ft;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.*;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.TestAll;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.LatLonPointImpl;

/**
 * Test PointFeatureTypes.
 *
 * @author caron
 * @since Dec 16, 2008
 */
public class TestPointFeatureTypes extends TestCase {
  private static String topdir = TestAll.testdataDir + "cdmUnitTest/";
  private static boolean showStructureData = false;

  public TestPointFeatureTypes(String name) {
    super(name);
  }

  public void testAll() throws IOException {
    scanDir(topdir + "cfPoint/", new MyFileFilter());
    //scanDir(ucar.nc2.TestAll.testdataDir + "station/");
  }

  private void scanDir(String dir, FileFilter ff) throws IOException {
    TestAll.actOnAll(dir, ff, new TestAll.Act() {

      public int doAct(String filename) throws IOException {
        checkPointDataset(filename, FeatureType.ANY_POINT, true);
        //testPointDataset(filename, FeatureType.POINT, true);
        return 1;
      }
    });
  }

  class MyFileFilter implements FileFilter {
    public boolean accept(File pathname) {
      String path = pathname.getPath();
      // eliminate ones that have been replaced by ncml
      if (new File(path + ".ncml").exists()) return false;
      if (path.endsWith(".ncml")) return true;
      int pos = path.lastIndexOf(".");
      if (new File(path.substring(0, pos) + ".ncml").exists()) return false;
      return true;
    }
  }

  // these are internal, synthetic (ncml) datasets with (almost) all possible combinations for proposed CF point obs
  String CFpointObs_topdir = TestAll.cdmLocalTestDataDir + "point/";
  public void testCFcanonical() throws IOException {
    assert 3 == checkPointDataset(CFpointObs_topdir + "point.ncml", FeatureType.POINT, false);
    assert 3 == checkPointDataset(CFpointObs_topdir + "pointUnlimited.nc", FeatureType.POINT, false);
    assert 4 == checkPointDataset(CFpointObs_topdir + "pointMissing.ncml", FeatureType.POINT, false);

    assert 3 == checkPointDataset(CFpointObs_topdir + "stationSingle.ncml", FeatureType.STATION, false);
    assert 3 == checkPointDataset(CFpointObs_topdir + "stationSingleWithZlevel.ncml", FeatureType.STATION, false);
    assert 15 == checkPointDataset(CFpointObs_topdir + "stationMultidim.ncml", FeatureType.STATION, false);
    assert 15 == checkPointDataset(CFpointObs_topdir + "stationMultidimTimeJoin.ncml", FeatureType.STATION, false);
    assert 15 == checkPointDataset(CFpointObs_topdir + "stationMultidimUnlimited.nc", FeatureType.STATION, false);
    assert 12 == checkPointDataset(CFpointObs_topdir + "stationMultidimMissingTime.ncml", FeatureType.STATION, false);
    assert 9 == checkPointDataset(CFpointObs_topdir + "stationMultidimMissingId.ncml", FeatureType.STATION, true);
    assert 12 == checkPointDataset(CFpointObs_topdir + "stationMultidimMissingIdString.ncml", FeatureType.STATION, true);
    assert 6 == checkPointDataset(CFpointObs_topdir + "stationRaggedContig.ncml", FeatureType.STATION, false);
    assert 6 == checkPointDataset(CFpointObs_topdir + "stationRaggedIndex.ncml", FeatureType.STATION, false);
    assert 5 == checkPointDataset(CFpointObs_topdir + "stationRaggedMissing.ncml", FeatureType.STATION, false);
    assert 13 == checkPointDataset(CFpointObs_topdir + "stationFlat.ncml", FeatureType.STATION, false);
    assert 13 == checkPointDataset(CFpointObs_topdir + "stationFlat.nc", FeatureType.STATION, false);

    assert 10 == checkPointDataset(CFpointObs_topdir + "trajSingle.ncml", FeatureType.TRAJECTORY, false);
    assert 20 == checkPointDataset(CFpointObs_topdir + "trajMultidim.ncml", FeatureType.TRAJECTORY, false);
    assert 30 == checkPointDataset(CFpointObs_topdir + "trajMultidimMissingId.ncml", FeatureType.TRAJECTORY, false);
    assert 18 == checkPointDataset(CFpointObs_topdir + "trajMultidimMissingTime.ncml", FeatureType.TRAJECTORY, false);
    assert 6 == checkPointDataset(CFpointObs_topdir + "trajRaggedContig.ncml", FeatureType.TRAJECTORY, false);
    assert 6 == checkPointDataset(CFpointObs_topdir + "trajRaggedIndex.ncml", FeatureType.TRAJECTORY, false);
    assert 5 == checkPointDataset(CFpointObs_topdir + "trajRaggedMissing.ncml", FeatureType.TRAJECTORY, false);

    assert 13 ==  checkPointDataset(CFpointObs_topdir + "profileSingle.ncml", FeatureType.PROFILE, false);
    assert 12 ==  checkPointDataset(CFpointObs_topdir + "profileSingleTimeJoin.ncml", FeatureType.PROFILE, false);
    assert 50 ==  checkPointDataset(CFpointObs_topdir + "profileMultidim.ncml", FeatureType.PROFILE, false);
    assert 50 ==  checkPointDataset(CFpointObs_topdir + "profileMultidimTimeJoin.ncml", FeatureType.PROFILE, false);
    assert 50 ==  checkPointDataset(CFpointObs_topdir + "profileMultidimZJoin.ncml", FeatureType.PROFILE, false);
    assert 50 ==  checkPointDataset(CFpointObs_topdir + "profileMultidimTimeZJoin.ncml", FeatureType.PROFILE, false);
    assert 40 ==  checkPointDataset(CFpointObs_topdir + "profileMultidimMissingId.ncml", FeatureType.PROFILE, false);
    assert 14 == checkPointDataset(CFpointObs_topdir + "profileMultidimMissingAlt.ncml", FeatureType.PROFILE, false);
    assert 6 ==  checkPointDataset(CFpointObs_topdir + "profileRaggedContig.ncml", FeatureType.PROFILE, false);
    assert 6 ==  checkPointDataset(CFpointObs_topdir + "profileRaggedContigTimeJoin.ncml", FeatureType.PROFILE, false);
    assert 22 ==  checkPointDataset(CFpointObs_topdir + "profileRaggedIndex.ncml", FeatureType.PROFILE, false);
    assert 22 ==  checkPointDataset(CFpointObs_topdir + "profileRaggedIndexTimeJoin.ncml", FeatureType.PROFILE, false);

    assert 9 == checkPointDataset(CFpointObs_topdir + "stationProfileSingle.ncml", FeatureType.STATION_PROFILE, false);
    assert 9 == checkPointDataset(CFpointObs_topdir + "stationProfileSingleTimeJoin.ncml", FeatureType.STATION_PROFILE, false);
    assert 18 == checkPointDataset(CFpointObs_topdir + "stationProfileMultidim.ncml", FeatureType.STATION_PROFILE, false);
    assert 18 == checkPointDataset(CFpointObs_topdir + "stationProfileMultidimUnlimited.nc", FeatureType.STATION_PROFILE, false);
    assert 24 == checkPointDataset(CFpointObs_topdir + "stationProfileMultidimJoinZ.ncml", FeatureType.STATION_PROFILE, false);
    assert 18 == checkPointDataset(CFpointObs_topdir + "stationProfileMultidimJoinTime.ncml", FeatureType.STATION_PROFILE, false);
    assert 36 == checkPointDataset(CFpointObs_topdir + "stationProfileMultidimJoinTimeAndZ.ncml", FeatureType.STATION_PROFILE, false);
    assert 27 == checkPointDataset(CFpointObs_topdir + "stationProfileMultidimMissingId.ncml", FeatureType.STATION_PROFILE, false);
    assert 27 == checkPointDataset(CFpointObs_topdir + "stationProfileMultidimMissingIdString.ncml", FeatureType.STATION_PROFILE, true);
    assert 16 == checkPointDataset(CFpointObs_topdir + "stationProfileMultidimMissingTime.ncml", FeatureType.STATION_PROFILE, true);
    assert 15 == checkPointDataset(CFpointObs_topdir + "stationProfileMultidimMissingAlt.ncml", FeatureType.STATION_PROFILE, true);
    assert 14 == checkPointDataset(CFpointObs_topdir + "stationProfileRagged.ncml", FeatureType.STATION_PROFILE, false);
    assert 14 == checkPointDataset(CFpointObs_topdir + "stationProfileRaggedJoinTime.ncml", FeatureType.STATION_PROFILE, false);

    // forget flat for now
    //assert 420 == checkPointDataset(CFpointObs_topdir + "stationProfileFlat.ncml", FeatureType.STATION_PROFILE, false);
    //assert 420 == checkPointDataset(CFpointObs_topdir + "stationProfileFlat.nc", FeatureType.STATION_PROFILE, false);

    assert 100 == checkPointDataset(CFpointObs_topdir + "sectionMultidim.ncml", FeatureType.SECTION, false);
    assert 100 == checkPointDataset(CFpointObs_topdir + "sectionMultidimJoinZ.ncml", FeatureType.SECTION, false);
    assert 100 == checkPointDataset(CFpointObs_topdir + "sectionMultidimMissingId.ncml", FeatureType.SECTION, false);
    assert 100 == checkPointDataset(CFpointObs_topdir + "sectionMultidimMissingIdString.ncml", FeatureType.SECTION, false);
    assert 28 == checkPointDataset(CFpointObs_topdir + "sectionMultidimMissingTime.ncml", FeatureType.SECTION, false);
    assert 18 == checkPointDataset(CFpointObs_topdir + "sectionMultidimMissingAlt.ncml", FeatureType.SECTION, false);
    assert 50 == checkPointDataset(CFpointObs_topdir + "sectionSingle.ncml", FeatureType.SECTION, false);
    assert 12 == checkPointDataset(CFpointObs_topdir + "sectionRagged.ncml", FeatureType.SECTION, false);

    // forget flat for now
    //assert 420 == checkPointDataset(CFpointObs_topdir + "sectionFlat.ncml", FeatureType.SECTION, false);
    //assert 420 == checkPointDataset(CFpointObs_topdir + "sectionFlat.nc", FeatureType.SECTION, false);
  }

  public void testProblem() throws IOException {
    // not picking up missing value
    // int n = checkPointDataset(CFpointObs_topdir + "stationRaggedMissing.ncml", FeatureType.STATION, false);

    //int n = checkPointDataset(topdir + "cfPoint/station/kunicki.nc4", FeatureType.STATION, true);
    //System.out.printf("n=%d%n", n);

    assert 1516 == checkPointDataset(topdir + "ft/point/Surface_Synoptic_20090921_0000.nc", FeatureType.POINT, false);
    //System.out.printf("n=%d%n", n);

  }

  public void testCF() throws IOException {

    /////// POINT
    // CF 1.1 psuedo-structure
    assert 1001 == checkPointDataset(topdir + "cfPoint/point/filtered_apriori_super_calibrated_binned1.nc", FeatureType.POINT, true);

    // CF 1.5 psuedo-structure
    assert 1196 == checkPointDataset(topdir + "cfPoint/point/nmcbob.shp.nc", FeatureType.POINT, true);

    /////// STATION
    // CF 1.3 ragged contiguous, single station
    assert 17280 == checkPointDataset(topdir + "cfPoint/station/rig_tower.2009-02-01.ncml", FeatureType.STATION, true);

    // CF 1.5 station unlimited, multidim
    assert 78912 == checkPointDataset(topdir + "cfPoint/station/billNewDicast.nc", FeatureType.STATION, true);

    // CF 1.5 station regular (not unlimited), multidim
    assert 19728 == checkPointDataset(topdir + "cfPoint/station/billOldDicast.nc", FeatureType.STATION, true);

    // CF 1.0 multidim with dimensions reversed
    //testPointDataset(topdir+"cfPoint/station/solrad_point_pearson.ncml", FeatureType.STATION, true);

    // CF 1.5 multidim stations, stn dim unlimited, must distinguish station table from obs.
    assert 1728 == checkPointDataset(topdir + "cfPoint/station/sampleDataset.nc", FeatureType.STATION, true);

    // CF 1.5 single trajectory (prob actually multiple - flat)
    assert 53176 == checkPointDataset(topdir + "cfPoint/trajectory/rt_20090512_willy2.ncml", FeatureType.TRAJECTORY, true);

    // CF 1.5 single trajectory (prob actually profile)
    assert 245 == checkPointDataset(topdir + "cfPoint/trajectory/p1140004.ncml", FeatureType.TRAJECTORY, true);

    // netcdf-4 with structure, from tom: tkunicki@usgs.gov
    // assert 6 == checkPointDataset(topdir + "cfPoint/station/kunicki.nc4", FeatureType.STATION, true);
  }

  public void testPlug() throws IOException {

    // cosmic
    assert 383 == checkPointDataset(topdir + "ft/trajectory/cosmic/wetPrf_C005.2007.294.16.22.G17_0001.0002_nc", FeatureType.TRAJECTORY, true);

    // ndbc
    assert 1405 == checkPointDataset(topdir + "ft/station/nbdc/41001h1976.nc", FeatureType.STATION, false);

    // suomi
    assert 124 == checkPointDataset(topdir + "ft/station/suomi/suoHWV_2006.105.00.00.0060_nc", FeatureType.STATION, false);
    assert 4848 == checkPointDataset(topdir + "ft/station/suomi/gsuPWV_2006.105.00.00.1440_nc", FeatureType.STATION, false);

    // fsl wind profilers
    assert 198 == checkPointDataset(topdir + "ft/station_profile/PROFILER_RASS_01hr_20091027_1500.nc", FeatureType.STATION_PROFILE, false);
    assert 198 == checkPointDataset(topdir + "ft/station_profile/PROFILER_RASS_06min_20091028_2318.nc", FeatureType.STATION_PROFILE, false);
    assert 1728 == checkPointDataset(topdir + "ft/station_profile/PROFILER_wind_01hr_20091024_1200.nc", FeatureType.STATION_PROFILE, false);
    assert 2088 == checkPointDataset(topdir + "ft/station_profile/PROFILER_wind_06min_20091030_2330.nc", FeatureType.STATION_PROFILE, false);

    //gempack sounding
    assert 8769 == checkPointDataset(TestAll.testdataDir+"sounding/gempak/19580807_upa.ncml", FeatureType.STATION_PROFILE, false);

    // gempak surface
    assert 3337 == checkPointDataset(topdir + "ft/point/2009103008_sb.gem", FeatureType.POINT, false);
    assert 938 == checkPointDataset(topdir + "ft/point/2009110100_ship.gem", FeatureType.POINT, false);
    assert 55856 == checkPointDataset(topdir + "ft/point/20091030_syn.gem", FeatureType.POINT, false);
    assert 28328 == checkPointDataset(topdir + "ft/point/20091030_syn.gem", FeatureType.STATION, false);

    // netcdf buoy / synoptic / metars ( robb's perl decoder output)
    assert 7 == checkPointDataset(topdir + "ft/point/Surface_METAR_latest.nc", FeatureType.POINT, false);
    assert 32452 == checkPointDataset(topdir + "ft/point/Surface_Buoy_20090921_0000.nc", FeatureType.POINT, false);
    assert 1516 == checkPointDataset(topdir + "ft/point/Surface_Synoptic_20090921_0000.nc", FeatureType.POINT, false);

    //RAF-Nimbus
    assert 7741 == checkPointDataset(topdir + "ft/trajectory/raf/135_ordrd.nc", FeatureType.TRAJECTORY, false);
    assert 8157 == checkPointDataset(topdir + "ft/trajectory/raf/raftrack.nc", FeatureType.TRAJECTORY, false);

    // Madis
    assert 5063 == checkPointDataset(topdir + "ft/trajectory/acars/acars_20091109_0800.nc", FeatureType.TRAJECTORY, false);
    assert 2499 == checkPointDataset(TestAll.testdataDir + "point/netcdf/19981110_1200", FeatureType.POINT, false);
    assert 1374 == checkPointDataset(TestAll.testdataDir + "station/madis2/hydro/20050729_1200", FeatureType.STATION, false);
    assert 1788 == checkPointDataset(TestAll.testdataDir + "sounding/netcdf/20070612_1200", FeatureType.STATION_PROFILE, false);

    // FslRaob
    // assert 63 == checkPointDataset(TestAll.testdataDir + "sounding/netcdf/raob_soundings20216.cdf", FeatureType.STATION_PROFILE, false);
    //assert 4638 == checkPointDataset(TestAll.testdataDir + "sounding/netcdf/Upperair_20060621_0000.nc", FeatureType.STATION_PROFILE, false);

    // unidata point obs
    assert 5023 == checkPointDataset(TestAll.testdataDir + "point/netcdf/200501q3h-gr.nc", FeatureType.STATION, false);
    assert 277477 == checkPointDataset(TestAll.testdataDir + "point/netcdf/20080814_LMA.ncml", FeatureType.POINT, false);

    // nldn
    assert 1165 == checkPointDataset(TestAll.testdataDir + "lightning/nldn/200929100.ingest", FeatureType.POINT, false);

    // uspln
    assert 3483 == checkPointDataset(TestAll.testdataDir + "lightning/uspln/uspln_20061023.18", FeatureType.POINT, false);

  }

  public void testGempak() throws IOException {
    assert 8769 == checkPointDataset(TestAll.testdataDir+"sounding/gempak/19580807_upa.ncml", FeatureType.STATION_PROFILE, false);
    
    // (GEMPAK IOSP) stn = psuedoStruct, obs = multidim Structure, time(time) as extraJoin
    checkPointDataset(TestAll.cdmUnitTestDir + "formats/gempak/surface/19580807_sao.gem", FeatureType.STATION, true);

    // stationAsPoint (GEMPAK IOSP) stn = psuedoStruct, obs = multidim Structure, time(time) as extraJoin
    //testPointDataset(TestAll.cdmUnitTestDir + "formats/gempak/surface/20090521_sao.gem", FeatureType.POINT, true);

    testGempakAll(TestAll.cdmUnitTestDir + "formats/gempak/surface/20090524_sao.gem");
    //testGempakAll(TestAll.cdmUnitTestDir+"C:/data/ft/station/09052812.sf");
  }

  public void testGempakAll(String filename) throws IOException {
    checkPointDataset(filename, FeatureType.ANY_POINT, true);
    checkLocation(filename, FeatureType.ANY_POINT, true);
    //testPointDataset(filename, FeatureType.POINT, true);
    //testLocation(filename, FeatureType.POINT, true);
  }


  public void utestGempakProblem() throws Exception {
    NetcdfFile.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("NetcdfFile/showRequest"));

    testDon2("Y:\\ldm\\gempak\\surface/20091023_sao.gem", true);
    //testDon2("Q:/cdmUnitTest/formats/gempak/surface/20090521_sao.gem", true);
    //testPointVsAny("Q:/cdmUnitTest/formats/gempak/surface/20090521_sao.gem", true);
  }


  public void utestCollectionGempak() throws IOException {
    //testPointDataset("collection:C:/data/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", FeatureType.STATION, true);
    checkPointDataset("collection:D:/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", FeatureType.STATION, true);
  }

  public void utestCollection() throws IOException {
    //Surface_METAR_20070326_0000.nc
    checkPointDataset("collection:C:/data/datasets/metars/Surface_METAR_#yyyyMMdd_HHmm#.nc", FeatureType.STATION, true);
    //testPointDataset("collection:D:/datasets/metars/Surface_METAR_#yyyyMMdd_HHmm#.nc", FeatureType.STATION, true);
  }

  public void utestCdmRemote() throws IOException {
    //testPointDataset("cdmremote:http://motherlode.ucar.edu:8081/thredds/cdmremote/idd/metar/gempak", FeatureType.STATION, true);
    checkPointDataset("cdmremote:http://localhost:8080/thredds/cdmremote/idd/metar/ncdecodedLocal", FeatureType.STATION, true);
  }

  public void utestCdmRemoteCollection() throws Exception {
    //testDon3("cdmremote:http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak", false);
    while (true) {
      // testDon2("cdmremote:http://localhost:8080/thredds/cdmremote/idd/metar/gempakLocal", false);
      testDon2("cdmremote:http://motherlode.ucar.edu:8081/thredds/cdmremote/idd/metar/gempak", true);
      Thread.sleep(60 * 1000);
    }

    //testDons("cdmremote:http://motherlode.ucar.edu:8081/thredds/cdmremote/idd/metar/gempak", false);
    //testDons("collection:C:/data/datasets/metars/Surface_METAR_#yyyyMMdd_HHmm#.nc", true);
    //testDons("C:/data/datasets/metars/Surface_METAR_20070326_0000.nc", true);
    //testDons("cdmremote:http://localhost:8080/thredds/cdmremote/idd/metar/ncdecodedLocalHome", true);
    //testPointDataset("cdmremote:http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak", FeatureType.STATION, true);
    //testPointDataset("cdmremote:http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak", FeatureType.ANY_POINT, true);
    //testPointDataset("cdmremote:http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak", FeatureType.POINT, true);
  }

  public void utestCdmRemoteCollectionSubsets() throws IOException {
    Formatter f = new Formatter();
    //String location = "cdmremote:http://localhost:8080/thredds/cdmremote/gempakSurface.xml/collection";
    String location = "cdmremote:http://localhost:8080/thredds/cdmremote/idd/metar/gempakLocal";
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.STATION, location, null, f);
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    assert fdpoint.getPointFeatureCollectionList().size() == 1;
    FeatureCollection fc = fdpoint.getPointFeatureCollectionList().get(0);
    assert (fc instanceof StationTimeSeriesFeatureCollection);
    StationTimeSeriesFeatureCollection stnc = (StationTimeSeriesFeatureCollection) fc;

    Station stn = stnc.getStation("04V");
    assert (stn != null);
    StationTimeSeriesFeature stnFeature = stnc.getStationFeature(stn);
    assert (stnFeature != null);
    stnFeature.calcBounds();
    int n = stnFeature.size();
    System.out.printf(" n=%d from %s ", n, stnFeature);

    checkPointFeatureCollection(stnFeature, true);

    fdataset.close();
  }
  

  private int checkPointDataset(String location, FeatureType type, boolean show) throws IOException {
    System.out.printf("================ TestPointFeatureCollection read %s %n", location);
    long start = System.currentTimeMillis();

    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(type, location, null, out);
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

    Date d1 = fdataset.getStartDate();
    Date d2 = fdataset.getEndDate();
    if ((d1 != null) && (d2 != null))
      assert d1.before(d2) || d1.equals(d2);

    List dataVars = fdataset.getDataVariables();
    assert dataVars != null;
    for (int i = 0; i < dataVars.size(); i++) {
      VariableSimpleIF v = (VariableSimpleIF) dataVars.get(i);
      assert null != fdataset.getDataVariable(v.getShortName());
    }

    // FeatureDatasetPoint
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    int count = 0;
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
        if (showStructureData) showStructureData((StationProfileFeatureCollection) fc );

      } else if (fc instanceof SectionFeatureCollection) {
        count = checkSectionFeatureCollection((SectionFeatureCollection) fc, show);

      } else if (fc instanceof ProfileFeatureCollection) {
        count = checkProfileFeatureCollection((ProfileFeatureCollection) fc, show);

      } else {
        count = checkNestedPointFeatureCollection((NestedPointFeatureCollection) fc, show);
      }
    }

    fdataset.close();
    long took = System.currentTimeMillis() - start;
    System.out.printf(" nobs=%d took= %d msec%n", count, took);

    return count;
  }

  int checkNestedPointFeatureCollection(NestedPointFeatureCollection npfc, boolean show) throws IOException {
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

  int checkStationProfileFeatureCollection(StationProfileFeatureCollection stationProfileFeatureCollection, boolean show) throws IOException {
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

  int checkSectionFeatureCollection(SectionFeatureCollection sectionFeatureCollection, boolean show) throws IOException {
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

  int checkProfileFeatureCollection(ProfileFeatureCollection profileFeatureCollection, boolean show) throws IOException {
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

   void showStructureData(StationProfileFeatureCollection stationProfileFeatureCollection) throws IOException {
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
          StructureData sdata = pointFeature.getData();
          NCdumpW.printStructureData(pw, sdata);
        }
      }
    }
  }



  int checkPointFeatureCollection(PointFeatureCollection pfc, boolean show) throws IOException {
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
      checkPointFeature(pf);
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
    PointFeatureCollection subset = pfc.subset(bb2, null);
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

      checkPointFeature(pf);
      counts++;
    }
    took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testPointFeatureCollection subset count= " + counts + " full iter took= " + took + " msec");

    return count;
  }

  // check that the location and times are filled out
  // read and test the data
  private void checkPointFeature(PointFeature pobs) throws java.io.IOException {

    EarthLocation loc = pobs.getLocation();
    assert loc != null;

    assert null != pobs.getNominalTimeAsDate();
    assert null != pobs.getObservationTimeAsDate();

    DateUnit timeUnit = pobs.getTimeUnit();
    assert timeUnit.makeDate(pobs.getNominalTime()).equals(pobs.getNominalTimeAsDate());
    assert timeUnit.makeDate(pobs.getObservationTime()).equals(pobs.getObservationTimeAsDate());

    StructureData sdata = pobs.getData();
    assert null != sdata;
    checkData(sdata);
  }

  // read each field, check datatype
  private void checkData(StructureData sdata) {

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

  int checkStationFeatureCollection(StationTimeSeriesFeatureCollection sfc) throws IOException {
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
    PointFeatureCollection flatten = sfc.flatten(bb2, null);
    int countFlat = countLocations(flatten);
    assert countFlat <= countStns;

    flatten = sfc.flatten(null, null);
    return countObs(flatten);
  }

  int countLocations(StationTimeSeriesFeatureCollection sfc) throws IOException {
    System.out.printf(" Station List Size = %d %n", sfc.getStations().size());

    // check uniqueness
    Map<String, StationTimeSeriesFeature> stns = new HashMap<String, StationTimeSeriesFeature>(5000);
    Map<MyLocation, StationTimeSeriesFeature> locs = new HashMap<MyLocation, StationTimeSeriesFeature>(5000);

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


  void checkLocation(String location, FeatureType type, boolean show) throws IOException {
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
      PointFeatureCollection subset = pfc.subset(bb2, null);
      countLocations(subset);

    } else if (fc instanceof StationTimeSeriesFeatureCollection) {
      StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) fc;
      PointFeatureCollection pfcAll = sfc.flatten(null, null);
      System.out.printf("Unique Locations all = %d %n", countLocations(pfcAll));

      LatLonRect bb = sfc.getBoundingBox();
      assert bb != null;
      LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
      PointFeatureCollection pfcSub = sfc.flatten(bb2, null);
      System.out.printf("Unique Locations sub1 = %d %n", countLocations(pfcSub));

      StationTimeSeriesFeatureCollection sfcSub = sfc.subset(bb2);
      PointFeatureCollection pfcSub2 = sfcSub.flatten(null, null);
      System.out.printf("Unique Locations sub2 = %d %n", countLocations(pfcSub2));

      // Dons
      sfc = sfc.subset(bb2);
      PointFeatureCollection subDon = sfc.flatten(bb2, null);
      System.out.printf("Unique Locations subDon = %d %n", countLocations(subDon));
    }

  }

  int countLocations(PointFeatureCollection pfc) throws IOException {
    int count = 0;
    Set<MyLocation> locs = new HashSet<MyLocation>(80000);
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

  int countObs(PointFeatureCollection pfc) throws IOException {
    int count = 0;
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      StructureData sd = pf.getData();
      count++;
    }
    return count;
  }

  private class MyLocation {
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

  /////////////////////////////////////////////////////////

  private void testPointVsAny(String file, boolean showIO) throws IOException {
    long start = System.currentTimeMillis();
    if (showIO)
      ucar.unidata.io.RandomAccessFile.setDebugAccess(true);

    LatLonRect llr = new LatLonRect(new LatLonPointImpl(33.4, -92.2), new LatLonPointImpl(47.9, -75.89));
    System.out.println("subset box = " + llr);

    Formatter buf = new Formatter();
    FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.POINT, file, null, buf);
    if (pods != null) {
      System.out.println("================\nOpen as POINT");
      readAll(pods, llr, "POINT", true);
    }

    pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.ANY_POINT, file, null, buf);
    if (pods != null) {
      System.out.println("======================\nOpen as ANY_POINT");
      readAll(pods, llr, "ANY_POINT", true);
    }

  }

  private void readAll(FeatureDatasetPoint pods, LatLonRect llr, String what, boolean showIO) throws IOException {
    long start = System.currentTimeMillis();
    if (showIO)
      ucar.unidata.io.RandomAccessFile.setDebugAccess(true);

    List<FeatureCollection> collectionList = pods.getPointFeatureCollectionList();
    FeatureCollection fc = collectionList.get(0);

    PointFeatureCollection collection = null;
    if (fc instanceof PointFeatureCollection) {
      collection = (PointFeatureCollection) fc;
      collection = collection.subset(llr, null);
      what += ".subset";

    } else if (fc instanceof NestedPointFeatureCollection) {
      NestedPointFeatureCollection npfc = (NestedPointFeatureCollection) fc;
      collection = npfc.flatten(llr, null);
      what += ".flatten";
    } else {
      throw new IllegalArgumentException("Can't handle collection of type " + fc.getClass().getName());
    }

    PointFeatureIterator dataIterator = collection.getPointFeatureIterator(-1);
    try {
      int numObs = 0;
      while (dataIterator.hasNext()) {
        PointFeature po = (PointFeature) dataIterator.next();
        if (numObs % 1000 == 0)
          System.out.printf("%d el = %s %n", numObs, po.getLocation());
        numObs++;
      }

      long took = System.currentTimeMillis() - start;
      System.out.printf("%s took %d msecs nobs = %d%n  seeks= %d Mbytes read= %d%n", what, took, numObs,
              ucar.unidata.io.RandomAccessFile.getDebugNseeks(), ucar.unidata.io.RandomAccessFile.getDebugNbytes()/(1000 * 1000));
    } finally {
      if (dataIterator != null)
        dataIterator.finish();
    }
  }



  private void testDons(String file, boolean showTime) throws IOException {
    long start = System.currentTimeMillis();
    if (showTime) {
      ucar.unidata.io.RandomAccessFile.setDebugAccess(true);
    }

    Formatter buf = new Formatter();
    FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.POINT, file, null, buf);
    if (pods == null) {  // try as ANY_POINT
      System.out.println("trying as ANY_POINT");
      pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
              ucar.nc2.constants.FeatureType.ANY_POINT, file, null, buf);
    } else
      System.out.println("Open as POINT");

    if (pods == null) {
      throw new IOException("can't open file error=" + buf);
    }

    List<FeatureCollection> collectionList = pods.getPointFeatureCollectionList();
    FeatureCollection fc = collectionList.get(0);
    LatLonRect llr = new LatLonRect(new LatLonPointImpl(33.4, -92.2), new LatLonPointImpl(47.9, -75.89));
    System.out.println("llr = " + llr);

    PointFeatureCollection collection = null;
    if (fc instanceof PointFeatureCollection) {
      collection = (PointFeatureCollection) fc;
      if (llr != null) {
        collection = collection.subset(llr, null);
      }
    } else if (fc instanceof NestedPointFeatureCollection) {
      NestedPointFeatureCollection npfc = (NestedPointFeatureCollection) fc;
      // npfc = npfc.subset(llr);
      collection = npfc.flatten(llr, null);
    } else {
      throw new IllegalArgumentException("Can't handle collection of type " + fc.getClass().getName());
    }

    PointFeatureIterator dataIterator = collection.getPointFeatureIterator(-1);
    try {
      int numObs = 0;
      while (dataIterator.hasNext()) {
        PointFeature po = (PointFeature) dataIterator.next();
        numObs++;
        ucar.unidata.geoloc.EarthLocation el = po.getLocation();
        assert llr.contains(el.getLatLon()) : el.getLatLon();
        if (numObs % 1000 == 0)
          System.out.printf("%d el = %s %n", numObs, el);
      }

      long took = System.currentTimeMillis() - start;
      System.out.printf("response took %d msecs nobs = %d%n  seeks= %d nbytes read= %d%n", took, numObs,
              ucar.unidata.io.RandomAccessFile.getDebugNseeks(), ucar.unidata.io.RandomAccessFile.getDebugNbytes());
    } finally {
      if (dataIterator != null)
        dataIterator.finish();
    }
    long took = System.currentTimeMillis() - start;
    System.out.printf("%ntotal response took %d msecs%n", took);
  }

  private void testDon2(String file, boolean usePresent) throws Exception {
    //ucar.unidata.io.RandomAccessFile.setDebugAccess(true);

    Formatter buf = new Formatter();
    FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
            ucar.nc2.constants.FeatureType.ANY_POINT, file, null, buf);
    if (pods == null) {
      throw new IOException("can't open file " + file);
    }
    //pods.calcBounds();
    System.out.printf("Opened file %s%n dateRange= %s", file, pods.getDateRange());

    List<FeatureCollection> collectionList = pods.getPointFeatureCollectionList();
    FeatureCollection fc = collectionList.get(0);
    LatLonRect llr = new LatLonRect(new LatLonPointImpl(33.4, -92.2), new LatLonPointImpl(47.9, -75.89));
    System.out.println("llr = " + llr);

    DateRange dr;
    if (usePresent) {
      //Date now = new Date();
      //Date ago = new Date(now.getTime()-3600000);
      dr = new DateRange(null, new DateType(true, null), new TimeDuration("2 hour"), null);

      //dr = new DateRange(null, new DateType(true, null), new TimeDuration("1 hour"), null);
      //dr = new DateRange(dr.getStart().getDate(), dr.getEnd().getDate()); // get rid of reletive time
    } else {
      Date startd = pods.getDateRange().getStart().getDate();
      dr = new DateRange(startd, new TimeDuration("1 hour"));
    }
    System.out.println("date range = " + dr);

    long start = System.currentTimeMillis();
    PointFeatureCollection collection = null;
    if (fc instanceof PointFeatureCollection) {
      collection = (PointFeatureCollection) fc;
      if (llr != null) {
        collection = collection.subset(llr, dr);
      }
    } else if (fc instanceof NestedPointFeatureCollection) {
      NestedPointFeatureCollection npfc = (NestedPointFeatureCollection) fc;
      collection = npfc.flatten(llr, dr);
    } else {
      throw new IllegalArgumentException("Can't handle collection of type " + fc.getClass().getName());
    }

    DateRange track = null;
    DateFormatter df = new DateFormatter();
    PointFeatureIterator dataIterator = collection.getPointFeatureIterator(-1);
    int numObs = 0;
    while (dataIterator.hasNext()) {
      PointFeature po = (PointFeature) dataIterator.next();
      numObs++;
      ucar.unidata.geoloc.EarthLocation el = po.getLocation();
      StructureData structure = po.getData();
      assert llr.contains(el.getLatLon()) : el.getLatLon() + " not in " + llr;

      Date obsDate = po.getObservationTimeAsDate();
      assert dr.included(obsDate) : df.toDateTimeString(obsDate) + " not in " + dr;
      if (numObs % 1000 == 0)
        System.out.printf("%d el = %s %s %n", numObs, el, df.toDateTimeString(obsDate));

      if (track == null) track = new DateRange(obsDate, obsDate);
      else track.extend(obsDate);
    }
    dataIterator.finish();

    long took = System.currentTimeMillis() - start;
    System.out.printf("%ntotal response took %d msecs nobs = %d range=%s %n==============%n", took, numObs, track);
  }

  private void testDon3(String file, boolean usePresent) throws Exception {

    long start = System.currentTimeMillis();
    Formatter buf = new Formatter();
    for (int i = 0; i < 10; i++) {
      FeatureDatasetPoint pods =
              (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                      ucar.nc2.constants.FeatureType.POINT, file, null, buf);
      if (pods == null) {  // try as ANY_POINT
        System.out.println("trying as ANY_POINT");
        pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                ucar.nc2.constants.FeatureType.ANY_POINT, file, null, buf);
      }
      if (pods == null) {
        throw new Exception("can't open file");
      }
      List<FeatureCollection> collectionList = pods.getPointFeatureCollectionList();
      FeatureCollection fc = collectionList.get(0);
      LatLonRect llr = new LatLonRect(new LatLonPointImpl(33.4, -92.2), new LatLonPointImpl(47.9, -75.89));
      System.out.println("llr = " + llr);
      Date now = new Date();
      Date ago = new Date(now.getTime() - 3600000);
      DateRange dr = new DateRange(ago, now);

      PointFeatureCollection collection = null;
      if (fc instanceof PointFeatureCollection) {
        collection = (PointFeatureCollection) fc;
        if (llr != null) {
          collection = collection.subset(llr, dr);
        }
      } else if (fc instanceof NestedPointFeatureCollection) {
        NestedPointFeatureCollection npfc = (NestedPointFeatureCollection) fc;
        collection = npfc.flatten(llr, dr);
      } else {
        throw new IllegalArgumentException("Can't handle collection of type " + fc.getClass().getName());
      }

      PointFeatureIterator dataIterator = collection.getPointFeatureIterator(-1);
      int numObs = 0;
      while (dataIterator.hasNext()) {
        PointFeature po = (PointFeature) dataIterator.next();
        numObs++;
        ucar.unidata.geoloc.EarthLocation el = po.getLocation();
        StructureData structure = po.getData();
        assert llr.contains(el.getLatLon()) : el.getLatLon();
        assert dr.included(po.getNominalTimeAsDate());
        if (numObs % 1000 == 0)
          System.out.printf("%d el = %s %n", numObs, el);
      }
      dataIterator.finish();

      long took = System.currentTimeMillis() - start;
      System.out.printf("%ntotal response took %d msecs nobs = %d%n  seeks= %d nbytes read= %d%n", took, numObs,
              ucar.unidata.io.RandomAccessFile.getDebugNseeks(), ucar.unidata.io.RandomAccessFile.getDebugNbytes());
      Thread.sleep(6000);
    }
  }

  private void testGempakMissing(String file) throws Exception {

    Formatter buf = new Formatter();
    FeatureDatasetPoint pods =
        (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.STATION, file, null, buf);
    if (pods == null)
      throw new Exception("can't open file");

    List<FeatureCollection> collectionList = pods.getPointFeatureCollectionList();
    StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) collectionList.get(0);

    int count = 0;
    int countMissing = 0;
    sfc.resetIteration();
    while (sfc.hasNext()) {
      StationTimeSeriesFeature sf = sfc.next();

      sf.resetIteration();
      while (sf.hasNext()) {
        PointFeature pf = sf.next();
        StructureData sdata = pf.getData();
        byte bval = sdata.getScalarByte("_isMissing");
        if (bval == 1) countMissing++;
        count++;
      }
    }

    double ratio = ((double)countMissing)/count;
    System.out.printf("countMissing=%d total=%d ratio=%f %n", countMissing, count, ratio);
  }

  public void utestYuan() throws IOException {
    Formatter buf = new Formatter();
    FeatureDatasetPoint pods =
        (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.TRAJECTORY,
                "Q:/profile/cosmic/atmPhs_C001.2009.321.23.20.G16_0001.0001_nc", null, buf);

    TrajectoryFeatureCollection tc = (TrajectoryFeatureCollection) pods.getPointFeatureCollectionList().get(0);
    //TrajectoryFeatureCollection tcs = tc.subset("var1, var2, var3");
    while (tc.hasNext()) {
      int count = 0;
      TrajectoryFeature tf = tc.next();
      PointFeatureIterator pfi = tf.getPointFeatureIterator(5000);
      while (pfi.hasNext()) {
        PointFeature pf = pfi.next();
        System.out.printf("%d ", count);
        count++;
      }
    }

    pods.close();

  }

  public static void main(String arg[]) throws Exception {
    TestPointFeatureTypes test = new TestPointFeatureTypes("");
    test.testGempakMissing("C:\\data\\ft\\station\\20090524_sao.gem");
  }


}

