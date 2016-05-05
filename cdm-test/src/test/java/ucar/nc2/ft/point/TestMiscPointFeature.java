/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

public class TestMiscPointFeature {

  // need a place to test individual datasets
  @Test
  public void testProblemProfile() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "point/profileMultidimZJoin.ncml";
    Assert.assertEquals("npoints", 50, TestPointDatasets.checkPointFeatureDataset(location, FeatureType.PROFILE, true));
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testProblemTraj() throws IOException {
    String location = TestDir.cdmUnitTestDir + "ft/trajectory/cosmic/wetPrf_C005.2007.294.16.22.G17_0001.0002_nc";
    Assert.assertEquals("npoints", 383, TestPointDatasets.checkPointFeatureDataset(location, FeatureType.TRAJECTORY, true));
  }

  @Test
  public void testProblemStation() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/H.2.4.1.ncml";
    Assert.assertEquals("npoints", 100, TestPointDatasets.checkPointFeatureDataset(location, FeatureType.STATION, true));
  }

  @Test
  @Ignore("Dont support multiple lat/lon coordinates for now")
  public void testProblemStationWithPreciseCoords() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/H.2.3.2.ncml";
    Assert.assertEquals("npoints", 100, TestPointDatasets.checkPointFeatureDataset(location, FeatureType.STATION, true));
  }
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGempakStationProfile() throws IOException {
    String location = TestDir.cdmUnitTestDir + "ft/sounding/gempak/19580807_upa.ncml";
    Assert.assertEquals("npoints", 8769, TestPointDatasets.checkPointFeatureDataset(location, FeatureType.STATION_PROFILE, true));
  }

  @Test
  public void testProblemStationProfile() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "point/stationProfileSingle.ncml";
    Assert.assertEquals("npoints", 9, TestPointDatasets.checkPointFeatureDataset(location, FeatureType.STATION_PROFILE, true));
  }

  @Test
  public void testProblemSection() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/H.6.3.1.ncml";
    Assert.assertEquals("npoints", 145, TestPointDatasets.checkPointFeatureDataset(location, FeatureType.TRAJECTORY_PROFILE, true));
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testProblem3() throws IOException {
    String location =  TestDir.cdmUnitTestDir + "ft/stationProfile/PROFILER_RASS_01hr_20091027_1500.nc";
    assert 198 == TestPointDatasets.checkPointFeatureDataset(location, FeatureType.STATION_PROFILE, true);
  }

  @Test
  public void testStationVarLevels() throws Exception {
    String file = TestDir.cdmLocalTestDataDir + "point/stationData2Levels.ncml";
    Formatter buf = new Formatter();
    try (FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.STATION, file, null, buf)) {
      Assert.assertNotNull(pods);
      List<DsgFeatureCollection> collectionList = pods.getPointFeatureCollectionList();
      assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
      DsgFeatureCollection fc = collectionList.get(0);
      assert fc instanceof StationTimeSeriesFeatureCollection;
      StationTimeSeriesFeatureCollection sc = (StationTimeSeriesFeatureCollection) fc;
      List<StationFeature> stations = sc.getStationFeatures();
      for (StationFeature s : stations) {
        StructureData sdata = s.getFeatureData();
        StructureMembers.Member m = sdata.findMember("stnInfo");
        assert m != null : "missing stnInfo";
        assert m.getDataType() == DataType.STRING : "stnInfo not a string";
        System.out.printf("stnInfo=%s%n", sdata.getScalarString(m));
      }

      PointFeatureCollectionIterator iter = sc.getPointFeatureCollectionIterator();
      while (iter.hasNext()) {
        PointFeatureCollection pfc = iter.next();
        assert pfc instanceof StationTimeSeriesFeatureImpl : pfc.getClass().getName();
        StationTimeSeriesFeature s = (StationTimeSeriesFeature) pfc;
        StructureData sdata = s.getFeatureData();
        StructureMembers.Member m = sdata.findMember("stnInfo");
        assert m != null : "missing stnInfo";
        assert m.getDataType() == DataType.STRING : "stnInfo not a string";
        System.out.printf("stnInfo=%s%n", sdata.getScalarString(m));
      }

      PointFeatureCollection pfc = sc.flatten(null, (CalendarDateRange) null, null);
      PointFeatureIterator iter2 = pfc.getPointFeatureIterator();
      while (iter2.hasNext()) {
        PointFeature pf = iter2.next();
        assert pf instanceof StationPointFeature;
        StationPointFeature s = (StationPointFeature) pf;
        StructureData sdata = s.getFeatureData();
        StructureMembers.Member m = sdata.findMember("stnInfo");
        assert m == null : "stnInfo in leaf";

        StructureData sdata2 = s.getDataAll();
        m = sdata2.findMember("stnInfo");
        assert m != null : "missing stnInfo";
        assert m.getDataType() == DataType.STRING : "stnInfo not a string";
        System.out.printf("stnInfo=%s%n", sdata2.getScalarString(m));
      }
    }
  }


  @Test
  public void testStationVarSingle() throws Exception {
    String file = TestDir.cdmLocalTestDataDir + "point/stationSingle.ncml";
    Formatter buf = new Formatter();
    try (FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.STATION, file, null, buf)) {
      List<DsgFeatureCollection> collectionList = pods.getPointFeatureCollectionList();
      assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
      DsgFeatureCollection fc = collectionList.get(0);
      assert fc instanceof StationTimeSeriesFeatureCollection;
      StationTimeSeriesFeatureCollection sc = (StationTimeSeriesFeatureCollection) fc;
      List<StationFeature> stations = sc.getStationFeatures();
      assert (stations.size() > 0) : "No stations";
      Station s = stations.get(0);
      assert s.getName().equals("666") : "name should be '666'";
      assert !Double.isNaN(s.getAltitude()) : "No altitude on station";
      assert s.getDescription() != null : "No description on station";
      assert s.getDescription().equalsIgnoreCase("flabulous") : "description should equal 'flabulous'";
      assert s.getWmoId() != null : "No wmoId on station";
      assert s.getWmoId().equalsIgnoreCase("whoa") : "wmoId should equal 'whoa' but ='"+s.getWmoId()+"'";
    }
  }

  @Test
  public void testStationVarRagged() throws Exception {
    String file = TestDir.cdmLocalTestDataDir + "point/stationRaggedContig.ncml";
    Formatter buf = new Formatter();
    try (FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.STATION, file, null, buf)) {
      List<DsgFeatureCollection> collectionList = pods.getPointFeatureCollectionList();
      assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
      DsgFeatureCollection fc = collectionList.get(0);
      assert fc instanceof StationTimeSeriesFeatureCollection;
      StationTimeSeriesFeatureCollection sc = (StationTimeSeriesFeatureCollection) fc;
      List<StationFeature> stations = sc.getStationFeatures();
      assert (stations.size() == 3) : "Should be 3 stations";
      for (Station s : stations) {
        System.out.printf("%s%n", s);
        assert !Double.isNaN(s.getAltitude()) : "No altitude on station";
        assert s.getDescription() != null  && !s.getDescription().isEmpty() : "No description on station";
        assert s.getWmoId() != null  && !s.getWmoId().isEmpty() : "No wmoId on station";
      }
    }
  }


  @Test
  public void testProfileSingleId() throws Exception {
    String file = TestDir.cdmLocalTestDataDir + "point/profileSingle.ncml";
    Formatter buf = new Formatter();
    try (FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.PROFILE, file, null, buf)) {
      List<DsgFeatureCollection> collectionList = pods.getPointFeatureCollectionList();
      assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
      DsgFeatureCollection fc = collectionList.get(0);
      assert fc instanceof ProfileFeatureCollection;
      ProfileFeatureCollection pc = (ProfileFeatureCollection) fc;
      int count = 0;
      pc.resetIteration();
      while (pc.hasNext()) {
        ProfileFeature pf = pc.next();
        assert pf.getName().equals("666") : pf.getName() + " should be '666'";
        count++;
      }
      assert count == 1;
    }
  }

  @Test
  public void testStationVarMulti() throws Exception {
    String file = TestDir.cdmLocalTestDataDir + "point/stationMultidim.ncml";
    Formatter buf = new Formatter();
    try (FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.STATION, file, null, buf)) {
      List<DsgFeatureCollection> collectionList = pods.getPointFeatureCollectionList();
      assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
      DsgFeatureCollection fc = collectionList.get(0);
      assert fc instanceof StationTimeSeriesFeatureCollection;
      StationTimeSeriesFeatureCollection sc = (StationTimeSeriesFeatureCollection) fc;
      List<StationFeature> stations = sc.getStationFeatures();
      assert (stations.size() == 5) : "Should be 5 stations";
      for (Station s : stations) {
        System.out.printf("%s%n", s);
        assert !Double.isNaN(s.getAltitude()) : "No altitude on station";
        assert s.getDescription() != null  && !s.getDescription().isEmpty() : "No description on station";
        assert s.getWmoId() != null  && !s.getWmoId().isEmpty() : "No wmoId on station";
      }
    }
  }

  @Test
   public void testDataVars() throws Exception {
     String file = TestDir.cdmLocalTestDataDir + "point/stationSingle.ncml";
     Formatter buf = new Formatter();
     try (FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.STATION, file, null, buf)) {
       List<VariableSimpleIF> dataVars = pods.getDataVariables();
       for (VariableSimpleIF dv : dataVars) System.out.printf(" %s%n", dv );
       assert (dataVars.size() == 1) : "Should only be one data var";
       VariableSimpleIF data =  dataVars.get(0);
       assert data.getShortName().equalsIgnoreCase("data");
     }
   }

  @Test
   public void testAltUnits() throws Exception {
     String file = TestDir.cdmLocalTestDataDir + "point/stationRaggedContig.ncml";
     Formatter buf = new Formatter();
     try (FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.STATION, file, null, buf)) {
       List<DsgFeatureCollection> collectionList = pods.getPointFeatureCollectionList();
       assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
       DsgFeatureCollection fc = collectionList.get(0);
       assert fc.getAltUnits() != null : "no Alt Units";
       assert fc.getAltUnits().equalsIgnoreCase("m") : "Alt Units should be 'm'";
     }
   }


  // make sure that try/with tolerates a null return from FeatureDatasetFactoryManager

  @Test
  public void testTryWith() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "testWrite.nc";
    Formatter errlog = new Formatter();
    try (FeatureDataset fdataset = FeatureDatasetFactoryManager.open(null, location, null, errlog)) {
      assert (fdataset == null);
    }
  }

  @Test
  public void testTryWithWrap() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "testWrite.nc";
    NetcdfDataset ncd = NetcdfDataset.openDataset(location);
    Formatter errlog = new Formatter();
    try (FeatureDataset fdataset = FeatureDatasetFactoryManager.wrap(null, ncd, null, errlog)) {
      assert (fdataset == null);
    }
    ncd.close();
  }

  // This is a regression test for TDS-513: https://bugtracking.unidata.ucar.edu/browse/TDS-513
  @Test
  public void testStationProfileMultidim1dTime() throws IOException {
    FeatureType type = FeatureType.STATION_PROFILE;
    String location = TestCFPointDatasets.CFpointObs_topdir + "stationProfileMultidim1dTime.ncml";
    ucar.nc2.util.CancelTask task = null;
    Formatter out = new Formatter();

    FeatureDataset featDset = FeatureDatasetFactoryManager.open(type, location, task, out);
    assert featDset != null && featDset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint featDsetPoint = (FeatureDatasetPoint) featDset;

    List<DsgFeatureCollection> featCols = featDsetPoint.getPointFeatureCollectionList();
    assert !featCols.isEmpty();
    DsgFeatureCollection featCol = featCols.get(0);  // We only care about the first one.

    assert featCol instanceof StationProfileFeatureCollection;
    StationProfileFeatureCollection stationProfileFeatCol = (StationProfileFeatureCollection) featCol;

    assert stationProfileFeatCol.hasNext();
    StationProfileFeature stationProfileFeat = stationProfileFeatCol.next();  // We only care about the first one.

    List<CalendarDate> timesList = stationProfileFeat.getTimes();
    Set<CalendarDate> timesSet = new TreeSet<>(stationProfileFeat.getTimes());  // Nukes dupes.
    Assert.assertEquals(timesList.size(), timesSet.size());  // Assert that the times are unique.
  }

  @Test
  public void testFlatten() throws IOException {  // kunicki
    Formatter formatter = new Formatter(System.err);
    try (FeatureDataset fd = FeatureDatasetFactoryManager.open(FeatureType.STATION, TestDir.cdmLocalTestDataDir + "pointPre1.6/StandardPointFeatureIteratorIssue.ncml", null, formatter)) {
      if (fd != null && fd instanceof FeatureDatasetPoint) {
        FeatureDatasetPoint fdp = (FeatureDatasetPoint) fd;
        DsgFeatureCollection fc = fdp.getPointFeatureCollectionList().get(0);
        if (fc != null && fc instanceof StationTimeSeriesFeatureCollection) {
          StationTimeSeriesFeatureCollection stsfc = (StationTimeSeriesFeatureCollection) fc;
          // subset criteria not important, just want to get data
          // into flattened representation
          PointFeatureCollection pfc = stsfc.flatten(
                  new LatLonRect(new LatLonPointImpl(-90, -180), new LatLonPointImpl(90, 180)),
                  CalendarDateRange.of(CalendarDate.parseISOformat(null, "1900-01-01"), CalendarDate.parseISOformat(null, "2100-01-01")));

          for (PointFeature pf : pfc) {
              // the call to cursor.getParentStructure() in
              // in StandardPointFeatureIterator.makeStation()
              // is returning the observation structure, not the
              // station structure since Cursor.currentIndex = 0
              //Station s = stsfc.getStation(pf);
            StructureData sdata = pf.getFeatureData();
            Assert.assertNotNull(sdata);

            StationFeature stnFeat = stsfc.getStationFeature(pf);
            Assert.assertNotNull(stnFeat);
            StructureData stnData = stnFeat.getFeatureData();
            Assert.assertNotEquals(sdata, stnData);
          }
        }
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGempak() throws Exception {
    String file = TestDir.cdmUnitTestDir + "formats/gempak/surface/09052812.sf";       // Q:/cdmUnitTest/formats/gempak/surface/09052812.sf
    Formatter buf = new Formatter();
    FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.POINT, file, null, buf);
    if (pods == null) {  // try as ANY_POINT
      pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.ANY_POINT, file, null, buf);
    }
    if (pods == null) {
      System.out.printf("can't open file=%s%n error=%s%n", file, buf);
      throw new Exception("can't open file " + file);
    }
    List<DsgFeatureCollection> collectionList = pods.getPointFeatureCollectionList();
    if (collectionList.size() > 1) {
      throw new IllegalArgumentException("Can't handle point data with multiple collections");
    }
    boolean sample;
    for (int time = 0; time < 2; time++) {
      sample = time < 1;
      DsgFeatureCollection fc = collectionList.get(0);
      PointFeatureCollection collection = null;
      LatLonRect llr = new LatLonRect(new LatLonPointImpl(33.4, -92.2), new LatLonPointImpl(47.9, -75.89));
      System.out.println("llr = " + llr);
      if (fc instanceof PointFeatureCollection) {
        collection = (PointFeatureCollection) fc;
        collection = collection.subset(llr, null);

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        StationTimeSeriesFeatureCollection npfc = (StationTimeSeriesFeatureCollection) fc;
        npfc = npfc.subset(llr);
        collection = npfc.flatten(llr, null);

      } else {
        throw new IllegalArgumentException("Can't handle collection of type " + fc.getClass().getName());
      }

      List<PointFeature> pos = new ArrayList<>(100000);
      List<CalendarDate> times = new ArrayList<>(100000);
      for (PointFeature po : collection) {
        pos.add(po);
        times.add(po.getNominalTimeAsCalendarDate());
        if (sample) {
          break;
        }
      }
      int size = pos.size();

      for (PointFeature po : pos) {
        ucar.unidata.geoloc.EarthLocation el = po.getLocation();
        System.out.println("el = " + el);
      }
    }
    pods.close();
  }

}
