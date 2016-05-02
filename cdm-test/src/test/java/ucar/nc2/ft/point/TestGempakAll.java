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

package ucar.nc2.ft.point;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

import org.junit.experimental.categories.Category;
import ucar.ma2.StructureData;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NestedPointFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.StationProfileFeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.TrajectoryFeature;
import ucar.nc2.ft.TrajectoryFeatureCollection;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

/**
 * currenty all cruft
 *
 * @author caron
 * @since 7/10/2014
 */
public class TestGempakAll {
  public static String topdir = TestDir.cdmUnitTestDir;

  public void TestDir() throws IOException {
    scanDir(topdir + "cfPoint/", new MyFileFilter());
    //scanDir(ucar.nc2.TestDir.testdataDir + "station/");
  }

  private void scanDir(String dir, FileFilter ff) throws IOException {
    TestDir.actOnAll(dir, ff, new TestDir.Act() {

      public int doAct(String filename) throws IOException {
        TestPointDatasets.checkPointDataset(filename, FeatureType.ANY_POINT, false);
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


  public void testGempakAll(String filename) throws IOException {
    TestPointDatasets.checkPointDataset(filename, FeatureType.ANY_POINT, true);
    TestPointDatasets.checkLocation(filename, FeatureType.ANY_POINT, true);
    //testPointDataset(filename, FeatureType.POINT, true);
    //testLocation(filename, FeatureType.POINT, true);
  }

  public void utestCollection() throws IOException {
    //Surface_METAR_20070326_0000.nc
    TestPointDatasets.checkPointDataset("collection:C:/data/datasets/metars/Surface_METAR_#yyyyMMdd_HHmm#.nc", FeatureType.STATION, true);
    //testPointDataset("collection:D:/datasets/metars/Surface_METAR_#yyyyMMdd_HHmm#.nc", FeatureType.STATION, true);
  }

  @Category(NeedsExternalResource.class)
  public void utestCdmRemote() throws IOException {
    TestPointDatasets.checkPointDataset("cdmremote:http://"+TestDir.threddsTestServer+"/thredds/cdmremote/idd/metar/gempak", FeatureType.STATION, true);
    //checkPointDataset("cdmremote:http://localhost:8080/thredds/cdmremote/idd/metar/ncdecodedLocal", FeatureType.STATION, true);
  }

  @Category(NeedsExternalResource.class)
  public void utestCdmRemoteCollection() throws Exception {
    //testDon3("cdmremote:http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak", false);
    while (true) {
      // testDon2("cdmremote:http://localhost:8080/thredds/cdmremote/idd/metar/gempakLocal", false);
      testDon2("cdmremote:http://"+TestDir.threddsTestServer+"/thredds/cdmremote/idd/metar/gempak", true);
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

    TestPointDatasets.checkPointFeatureCollection(stnFeature, true);

    fdataset.close();
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
        collection = collection.subset(llr, (CalendarDateRange) null);
      }
    } else if (fc instanceof NestedPointFeatureCollection) {
      NestedPointFeatureCollection npfc = (NestedPointFeatureCollection) fc;
      // npfc = npfc.subset(llr);
      collection = npfc.flatten(llr, (CalendarDateRange) null);
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

  public void testAsa() throws IOException {
    Formatter buf = new Formatter();
    FeatureDatasetPoint pods =
        (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.STATION_PROFILE,
                topdir + "cfPoint/stationProfile/timeSeriesProfile-Ragged-SingleStation-H.5.3.nc", null, buf);

    FeatureCollection fc = pods.getPointFeatureCollectionList().get(0);
    System.out.printf("%s%n", fc.getClass().getName());
    StationProfileFeatureCollection tcs = (StationProfileFeatureCollection) fc;
    //TrajectoryFeatureCollection tcs = tc.subset("var1, var2, var3");
    while (tcs.hasNext()) {
      int count = 0;
      StationProfileFeature tf = tcs.next();
      System.out.printf("%s%n", tf);
      PointFeatureCollectionIterator pfi = tf.getPointFeatureCollectionIterator(5000);
      while (pfi.hasNext()) {
        PointFeatureCollection pfc = pfi.next();
        System.out.printf( "%d %s%n ", count, pfc);
        count++;

        int countObs = 0;
        PointFeatureIterator pfi2 = pfc.getPointFeatureIterator(5000);
        while (pfi2.hasNext()) {
          PointFeature pf = pfi2.next();
          System.out.printf("  %s%n", pf);
        }
        System.out.printf("%d%n", countObs);
      }
    }

    pods.close();

  }



  //////////
  ////

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
      collection = collection.subset(llr, (CalendarDateRange) null);
      what += ".subset";

    } else if (fc instanceof NestedPointFeatureCollection) {
      NestedPointFeatureCollection npfc = (NestedPointFeatureCollection) fc;
      collection = npfc.flatten(llr,(CalendarDateRange)  null);
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

}
