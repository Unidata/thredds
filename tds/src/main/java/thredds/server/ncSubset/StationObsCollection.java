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

package thredds.server.ncSubset;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import ucar.ma2.StructureData;
import ucar.ma2.Array;
import ucar.unidata.geoloc.Station;
import ucar.nc2.dt.point.WriterStationObsDataset;
import ucar.nc2.dt.point.StationObsDatasetInfo;
import ucar.nc2.dt.point.WriterCFStationObsDataset;
import ucar.nc2.dt.StationObsDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.dt.DataIterator;
import ucar.nc2.dt.StationObsDatatype;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import thredds.catalog.XMLEntityResolver;
import org.jdom.Document;
import org.jdom.Element;
import ucar.unidata.util.Format;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.servlet.http.HttpServletResponse;

/**
 * @deprecated use cdmremote
 */
public class StationObsCollection {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StationObsCollection.class);
  static private org.slf4j.Logger cacheLogger = org.slf4j.LoggerFactory.getLogger("cacheLogger");

  private static boolean debug = false, debugDetail = false;
  private static long timeToScan = 0;

  private String archiveDir, realtimeDir;
  private ArrayList<Dataset> datasetList;
  private List<VariableSimpleIF> variableList;
  private DateFormatter format = new DateFormatter();

  private boolean isRealtime, isReady;
  private Date start, end;
  private Timer timer;
  private ReadWriteLock lock = new ReentrantReadWriteLock();

  public StationObsCollection(String archiveDir, String realtimeDir) {
    this.archiveDir = archiveDir;
    this.realtimeDir = realtimeDir;
    this.isRealtime = (realtimeDir != null);

    if (isRealtime) {
      timer = new Timer("StationObsCollection.Rescan");
      Calendar c = Calendar.getInstance(); // contains current startup time
      c.setTimeZone(TimeZone.getDefault()); // local time
      c.add(Calendar.HOUR_OF_DAY, 24); // start tommorrow
      c.set(Calendar.HOUR_OF_DAY, 1); // at 1 AM
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      timer.schedule(new ReinitTask(), 1000 * 5); // do in 5 secs
      timer.schedule(new ReinitTask(), c.getTime(), (long) 1000 * 60 * 60 * 24); // repeat once a day
      cacheLogger.info("StationObsCollection timer set to run at " + c.getTime());
    } else {
      initArchiveOnly();
      isReady = true;
    }
  }

  public void close() {
    closeDatasets();
    if (timer != null)
      timer.cancel();
  }

  private void closeDatasets() {
    if (datasetList == null)
      return;

    for (Dataset ds : datasetList) {
      try {
        ds.sod.close();
      } catch (IOException ioe) {
        log.warn("Couldnt close " + ds.sod, ioe);
      }
    }

    if (timer != null)
      timer.cancel();
  }

  private class ReinitTask extends TimerTask {
    public void run() {
      cacheLogger.info("StationObsCollection.reinit to " + archiveDir + " from " + realtimeDir + " at " + format.toDateTimeString(new Date()));
      init();
    }
  }

  public String getName() {
    return archiveDir + "/" + realtimeDir;
  }

  public boolean isReady() {
    return isReady;
  }

  public ArrayList<Dataset> getDatasets() {
    return datasetList;
  }

  // read archive files - these have been rewritten for efficiency, and dont change in realtime
  private void initArchiveOnly() {
    CollectionManager archive = new CollectionManager(archiveDir, ff, dateFormatString);
    datasetList = new ArrayList<Dataset>();
    stationList = null;
    variableList = null;

    int count = 0;
    long size = 0;

    // each one becomes a Dataset and added to the list
    ArrayList<CollectionManager.MyFile> list = new ArrayList<CollectionManager.MyFile>(archive.getList());
    for (CollectionManager.MyFile myfile : list) {

      // otherwise, add an sobsDataset
      try {
        Dataset ds = new Dataset(myfile.file, false);
        datasetList.add(ds);
        count++;
        size += myfile.file.length();

        if (null == stationList)
          stationList = new ArrayList<Station>(ds.sod.getStations());

        if (null == variableList)
          variableList = new ArrayList<VariableSimpleIF>(ds.sod.getDataVariables());

      } catch (IOException e) {
        cacheLogger.error("Cant open " + myfile, e);
      }
    }

    Collections.sort(datasetList);
    int n = datasetList.size();
    start = datasetList.get(0).time_start;
    end = datasetList.get(n - 1).time_end;

    size = size / 1000 / 1000;
    cacheLogger.info("Reading directory " + archive + " # files = " + count + " size= " + size + " Mb");
  }


  ////////////////////////////////////////////
  // keep track of the available datasets LOOK should be configurable
  private String dateFormatString = "yyyyMMdd_HHmm";
  private FileFilter ff = new SuffixFileFilter(".nc");

  private void init() {
    CollectionManager archive = new CollectionManager(archiveDir, ff, dateFormatString);
    CollectionManager realtime = new CollectionManager(realtimeDir, ff, dateFormatString);

    makeArchiveFiles(archive, realtime);
    try {
      lock.writeLock().lock(); // wait till no readers

      // close all open datasets
      closeDatasets();

      // create new list
      ArrayList<Dataset> newList = new ArrayList<Dataset>();
      initArchive(archive, newList);
      initRealtime(archive, realtime, newList);
      Collections.sort(newList);

      // make this one the operational one
      datasetList = newList;
      int n = datasetList.size();
      if (n > 0) {
        start = datasetList.get(0).time_start;
        end = datasetList.get(n - 1).time_end;
      }

    } finally {
      lock.writeLock().unlock();
    }

    datasetDesc = null; // mostly to create new time range
    isReady = true;
  }

  // make new archive files
  private void makeArchiveFiles(CollectionManager archive, CollectionManager realtime) {

    // the set of files in realtime collection that come after the archive files
    List<CollectionManager.MyFile> realtimeList = realtime.after(archive.getLatest());
    if (realtimeList.size() < 2)
      return;

    // leave latest one alone
    int n = realtimeList.size();
    Collections.sort(realtimeList);
    realtimeList.remove(n - 1);

    // for the rest, convert to a more efficient format
    for (CollectionManager.MyFile myfile : realtimeList) {
      String filename = myfile.file.getName();
      String fileIn = realtimeDir + "/" + filename;
      String fileOut = archiveDir + "/" + filename;

      try {
        long start = System.currentTimeMillis();
        cacheLogger.info("StationObsCollection: write " + fileIn + " to archive " + fileOut);
        WriterStationObsDataset.rewrite(fileIn, fileOut);
        archive.add(new File(fileOut), myfile.date);
        long took = System.currentTimeMillis() - start;
        cacheLogger.info("  that took= " + (took / 1000) + " secs");
      } catch (IOException e) {
        cacheLogger.error("StationObsCollection: write failed (" + fileIn + " to archive " + fileOut + ")", e);
      }
    }
  }

  // read archive files - these have been rewritten for efficiency, and dont change in realtime
  private void initArchive(CollectionManager archive, ArrayList<Dataset> newList) {
    // LOOK can these change ?
    stationList = null;
    variableList = null;

    Calendar c = Calendar.getInstance(); // contains current startup time
    c.add(Calendar.HOUR_OF_DAY, -8 * 24); // 7 or 8 days ago
    Date firstDate = c.getTime();
    cacheLogger.info("StationObsCollection: delete files before " + firstDate);

    int count = 0;
    long size = 0;

    // each one becomes a Dataset and added to the list
    ArrayList<CollectionManager.MyFile> list = new ArrayList<CollectionManager.MyFile>(archive.getList());
    for (CollectionManager.MyFile myfile : list) {
      // delete old files
      if (myfile.date.before(firstDate)) {
        myfile.file.delete();
        boolean ok = archive.remove(myfile);
        cacheLogger.info("StationObsCollection: Deleted archive file " + myfile + " ok= " + ok);
        continue;
      }

      // otherwise, add an sobsDataset
      try {
        Dataset ds = new Dataset(myfile.file, false);
        newList.add(ds);
        count++;
        size += myfile.file.length();

        if (null == stationList)
          stationList = new ArrayList<Station>(ds.sod.getStations());

        if (null == variableList)
          variableList = new ArrayList<VariableSimpleIF>(ds.sod.getDataVariables());

      } catch (IOException e) {
        cacheLogger.error("Cant open " + myfile, e);
      }
    }

    size = size / 1000 / 1000;

    cacheLogger.info("Reading directory " + archive + " # files = " + count + " size= " + size + " Mb");
  }

  // the archive files - these have been rewritten for efficiency, and dont change in realtime
  private void initRealtime(CollectionManager archive, CollectionManager realtime, ArrayList<Dataset> newList) {
    long size = 0;
    int count = 0;

    List<CollectionManager.MyFile> realtimeList = realtime.after(archive.getLatest());
    if (realtimeList.size() == 0)
      return;

    // each one becomes a Dataset and added to the list
    for (CollectionManager.MyFile myfile : realtimeList) {
      try {
        Dataset ds = new Dataset(myfile.file, true);
        newList.add(ds);
        count++;
        size += myfile.file.length();


        if (null == stationList)
          stationList = new ArrayList<Station>(ds.sod.getStations());

        if (null == variableList)
          variableList = new ArrayList<VariableSimpleIF>(ds.sod.getDataVariables());

      } catch (IOException e) {
        cacheLogger.error("Cant open " + myfile, e);
      }
    }

    size = size / 1000 / 1000;

    cacheLogger.info("Reading realtime directory " + realtime + " # files = " + count + " total file sizes = " + size + " Mb");
  }

  class Dataset implements Comparable {
    String filename, name;
    StationObsDataset sod;
    Date time_start;
    Date time_end;
    boolean mayChange;

    Dataset(File file, boolean mayChange) throws IOException {
      this.filename = file.getAbsolutePath();
      this.name = file.getName();
      this.mayChange = mayChange;

      this.sod = get();
      this.time_start = sod.getStartDate();
      this.time_end = sod.getEndDate();

      if (debug)
        System.out.println(" add " + this);
    }

    StationObsDataset get() throws IOException {
      if (sod == null) {
        StringBuilder sbuff = new StringBuilder();
        if (debug) System.out.println("StationObsDataset open " + filename);
        sod = (StationObsDataset) TypedDatasetFactory.open(FeatureType.STATION, filename, null, sbuff);
        if (null == sod) {
          log.info("Cant open " + filename + "; " + sbuff);
          return null;
        }
      } else if (mayChange) {
        NetcdfFile ncfile = sod.getNetcdfFile();
        ncfile.syncExtend();
      }

      return sod;
    }

    public int compareTo(Object o) {
      Dataset od = (Dataset) o;
      return time_start.compareTo(od.time_start);
    }

    public String toString() {
      return "StationObsDataset " + filename + " start= " + format.toDateTimeString(time_start) +
              " end= " + format.toDateTimeString(time_end);
    }
  }

  ////////////////////////////////////////////////////
  // Dataset Description

  private String datasetName = "/thredds/ncss/metars";
  private Document datasetDesc;

  public Document getDoc() throws IOException {
    if (datasetDesc != null)
      return datasetDesc;

    Dataset ds = datasetList.get(0);
    if (debug) System.out.println("getDoc open " + ds.filename);
    StationObsDataset sod = ds.get();
    StationObsDatasetInfo info = new StationObsDatasetInfo(sod, null);
    Document doc = info.makeStationObsDatasetDocument();
    Element root = doc.getRootElement();

    // fix the location
    root.setAttribute("location", datasetName); // LOOK

    // fix the time range
    Element timeSpan = root.getChild("TimeSpan");
    timeSpan.removeContent();
    DateFormatter format = new DateFormatter();
    timeSpan.addContent(new Element("begin").addContent(format.toDateTimeStringISO(start)));
    timeSpan.addContent(new Element("end").addContent(isRealtime ? "present" : format.toDateTimeStringISO(end)));

    // add pointer to the station list XML
    Element stnList = new Element("stationList");
    stnList.setAttribute("title", "Available Stations", XMLEntityResolver.xlinkNS);
    stnList.setAttribute("href", "/thredds/ncss/metars/stations.xml", XMLEntityResolver.xlinkNS);  // LOOK kludge
    root.addContent(stnList);

    datasetDesc = doc;
    return doc;
  }

  public Document getStationDoc() throws IOException {
    StationObsDataset sod = datasetList.get(0).get();
    StationObsDatasetInfo info = new StationObsDatasetInfo(sod, null);
    return info.makeStationCollectionDocument();
  }

  ///////////////////////////////////////
  // station handling
  private List<Station> stationList;
  private HashMap<String, Station> stationMap;

  /**
   * Determine if any of the given station names are actually in the dataset.
   *
   * @param stns List of station names
   * @return true if list is empty, ie no names are in the actual station list
   * @throws IOException if read error
   */
  public boolean isStationListEmpty(List<String> stns) throws IOException {
    HashMap<String, Station> map = getStationMap();
    for (String stn : stns) {
      if (map.get(stn) != null) return false;
    }
    return true;
  }

  public boolean intersect(DateRange dr) throws IOException {
    return dr.intersects(start, end);
  }

  private List<Station> getStationList() throws IOException {
    return stationList;
  }

  private HashMap<String, Station> getStationMap() throws IOException {
    if (null == stationMap) {
      stationMap = new HashMap<String, Station>();
      List<Station> list = getStationList();
      for (Station station : list) {
        stationMap.put(station.getName(), station);
      }
    }
    return stationMap;
  }

  /**
   * Get the list of station names that are contained within the bounding box.
   *
   * @param boundingBox lat/lon bounding box
   * @return list of station names contained within the bounding box
   * @throws IOException if read error
   */
  public List<String> getStationNames(LatLonRect boundingBox) throws IOException {
    LatLonPointImpl latlonPt = new LatLonPointImpl();
    ArrayList<String> result = new ArrayList<String>();
    List<Station> stations = getStationList();
    for (Station s : stations) {
      latlonPt.set(s.getLatitude(), s.getLongitude());
      if (boundingBox.contains(latlonPt)) {
        result.add(s.getName());
        // boundingBox.contains(latlonPt);   debugging
      }
    }
    return result;
  }

  /**
   * Find the station closest to the specified point.
   * The metric is (lat-lat0)**2 + (cos(lat0)*(lon-lon0))**2
   *
   * @param lat latitude value
   * @param lon longitude value
   * @return name of station closest to the specified point
   * @throws IOException if read error
   */
  public String findClosestStation(double lat, double lon) throws IOException {
    double cos = Math.cos(Math.toRadians(lat));
    List<Station> stations = getStationList();
    Station min_station = stations.get(0);
    double min_dist = Double.MAX_VALUE;

    for (Station s : stations) {
      double lat1 = s.getLatitude();
      double lon1 = LatLonPointImpl.lonNormal(s.getLongitude(), lon);
      double dy = Math.toRadians(lat - lat1);
      double dx = cos * Math.toRadians(lon - lon1);
      double dist = dy * dy + dx * dx;
      if (dist < min_dist) {
        min_dist = dist;
        min_station = s;
      }
    }
    return min_station.getName();
  }

  ////////////////////////////////////////////////////////
  // scanning

  // scan all data in the file, records that pass the dateRange and predicate match are acted on

  private void scanAll(Dataset ds, DateRange range, Predicate p, Action a, Limit limit) throws IOException {
    StringBuilder sbuff = new StringBuilder();
    StationObsDataset sod = ds.get();
    if (debug) System.out.println("scanAll open " + ds.filename);
    if (null == sod) {
      log.info("Cant open " + ds.filename + "; " + sbuff);
      return;
    }

    DataIterator iter = sod.getDataIterator(0);
    while (iter.hasNext()) {
      StationObsDatatype sobs = (StationObsDatatype) iter.nextData();

      // date filter
      if (null != range) {
        Date obs = sobs.getObservationTimeAsDate();
        if (!range.included(obs))
          continue;
      }

      StructureData sdata = sobs.getData();
      if ((p == null) || p.match(sdata)) {
        a.act(sod, sobs, sdata);
        limit.matches++;
      }

      limit.count++;
      if (limit.count > limit.limit) break;
      if (debug && (limit.count % 10000 == 0)) System.out.println(" did " + limit.count);
    }

  }

  // scan data for the list of stations, in order
  // records that pass the dateRange and predicate match are acted on
  private void scanStations(Dataset ds, List<String> stns, DateRange range, Predicate p, Action a, Limit limit) throws IOException {
    StringBuilder sbuff = new StringBuilder();

    StationObsDataset sod = ds.get();
    if (debug) System.out.println("scanStations open " + ds.filename);
    if (null == sod) {
      log.info("Cant open " + ds.filename + "; " + sbuff);
      return;
    }

    for (String stn : stns) {
      Station s = sod.getStation(stn);
      if (s == null) {
        log.warn("Cant find station " + s);
        continue;
      }
      if (debugDetail) System.out.println("stn " + s.getName());


      DataIterator iter = sod.getDataIterator(s);
      while (iter.hasNext()) {
        StationObsDatatype sobs = (StationObsDatatype) iter.nextData();

        // date filter
        if (null != range) {
          Date obs = sobs.getObservationTimeAsDate();
          if (!range.included(obs))
            continue;
        }

        // general predicate filter
        StructureData sdata = sobs.getData();
        if ((p == null) || p.match(sdata)) {
          a.act(sod, sobs, sdata);
          limit.matches++;
        }

        limit.count++;
        if (limit.count > limit.limit) break;
      }
    }

  }

  // scan all data in the file, first eliminate any that dont pass the predicate
  // for each station, track the closest record to the given time
  // then act on those
  private void scanAll(Dataset ds, DateType time, Predicate p, Action a, Limit limit) throws IOException {
    StringBuilder sbuff = new StringBuilder();

    HashMap<Station, StationDataTracker> map = new HashMap<Station, StationDataTracker>();
    long wantTime = time.getDate().getTime();

    StationObsDataset sod = ds.get();
    if (debug) System.out.println("scanAll open " + ds.filename);
    if (null == sod) {
      log.info("Cant open " + ds.filename + "; " + sbuff);
      return;
    }

    DataIterator iter = sod.getDataIterator(0);
    while (iter.hasNext()) {
      StationObsDatatype sobs = (StationObsDatatype) iter.nextData();

      // general predicate filter
      if (p != null) {
        StructureData sdata = sobs.getData();
        if (!p.match(sdata))
          continue;
      }

      // find closest time for this station
      long obsTime = sobs.getObservationTimeAsDate().getTime();
      long diff = Math.abs(obsTime - wantTime);

      Station s = sobs.getStation();
      StationDataTracker track = map.get(s);
      if (track == null) {
        map.put(s, new StationDataTracker(sobs, diff));
      } else {
        if (diff < track.timeDiff) {
          track.sobs = sobs;
          track.timeDiff = diff;
        }
      }
    }

    for (Station s : map.keySet()) {
      StationDataTracker track = map.get(s);
      a.act(sod, track.sobs, track.sobs.getData());
      limit.matches++;

      limit.count++;
      if (limit.count > limit.limit) break;
    }

  }

  private class StationDataTracker {
    StationObsDatatype sobs;
    long timeDiff = Long.MAX_VALUE;

    StationDataTracker(StationObsDatatype sobs, long timeDiff) {
      this.sobs = sobs;
      this.timeDiff = timeDiff;
    }
  }


  // scan data for the list of stations, in order
  // eliminate records  that dont pass the predicate
  // for each station, track the closest record to the given time, then act on those
  private void scanStations(Dataset ds, List<String> stns, DateType time, Predicate p, Action a, Limit limit) throws IOException {
    StringBuilder sbuff = new StringBuilder();

    StationObsDataset sod = ds.get();
    if (null == sod) {
      log.info("Cant open " + ds.filename + "; " + sbuff);
      return;
    }

    long wantTime = time.getDate().getTime();

    for (String stn : stns) {
      Station s = sod.getStation(stn);
      if (s == null) {
        log.warn("Cant find station " + s);
        continue;
      }

      StationObsDatatype sobsBest = null;
      long timeDiff = Long.MAX_VALUE;

      // loop through all data for this station, take the obs with time closest
      DataIterator iter = sod.getDataIterator(s);
      while (iter.hasNext()) {
        StationObsDatatype sobs = (StationObsDatatype) iter.nextData();

        // general predicate filter
        if (p != null) {
          StructureData sdata = sobs.getData();
          if (!p.match(sdata))
            continue;
        }

        long obsTime = sobs.getObservationTimeAsDate().getTime();
        long diff = Math.abs(obsTime - wantTime);
        if (diff < timeDiff) {
          sobsBest = sobs;
          timeDiff = diff;
        }
      }

      if (sobsBest != null) {
        a.act(sod, sobsBest, sobsBest.getData());
        limit.matches++;
      }

      limit.count++;
      if (limit.count > limit.limit) break;
    }

  }

  private interface Predicate {
    boolean match(StructureData sdata);
  }

  private interface Action {
    void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException;
  }

  private class Limit {
    int count;
    int limit = Integer.MAX_VALUE;
    int matches;
  }

  ////////////////////////////////////////////////////////////////
  // date filter

  private List<Dataset> filterDataset(DateRange range) {
    if (range == null)
      return datasetList;

    List<Dataset> result = new ArrayList<Dataset>();
    for (Dataset ds : datasetList) {
      if (range.intersects(ds.time_start, ds.time_end))
        result.add(ds);
    }
    return result;
  }

  Dataset filterDataset(DateType want) {
    if (want.isPresent())
      return datasetList.get(datasetList.size() - 1);

    Date time = want.getDate();
    for (Dataset ds : datasetList) {
      if (time.before(ds.time_end) && time.after(ds.time_start)) {
        return ds;
      }
      if (time.equals(ds.time_end) || time.equals(ds.time_start)) {
        return ds;
      }
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////
  // writing

  //private File netcdfResult = new File("C:/temp/sobs.nc");

  public File writeNetcdf(QueryParams qp) throws IOException {
    WriterNetcdf w = (WriterNetcdf) write(qp, null);
    return w.netcdfResult;
  }

  public Writer write(QueryParams qp, HttpServletResponse res) throws IOException {
    long start = System.currentTimeMillis();
    Limit counter = new Limit();

    List<String> vars = qp.vars;
    List<String> stns = qp.stns;
    DateRange range = qp.getDateRange();
    DateType time = qp.time;
    String type = qp.acceptType;

    Writer w;
    if (type.equals(QueryParams.RAW)) {
      w = new WriterRaw(qp, vars, res.getWriter());
    } else if (type.equals(QueryParams.XML)) {
      w = new WriterXML(qp, vars, res.getWriter());
    } else if (type.equals(QueryParams.CSV)) {
      w = new WriterCSV(qp, vars, res.getWriter());
    } else if (type.equals(QueryParams.NETCDF)) {
      w = new WriterNetcdf(qp, vars);
    } else if (type.equals(QueryParams.NETCDFS)) {
      w = new WriterNetcdfStream(qp, vars, res.getOutputStream());
    } else if (type.equals(QueryParams.CdmRemote)) {
      w = new WriterCdmRemote(qp, vars, res.getOutputStream());
    } else {
      log.error("Unknown writer type = " + type);
      return null;
    }

    Collections.sort(stns);
    w.header(stns);

    boolean useAll = stns.size() == 0;
    Action act = w.getAction();
    try {
      lock.readLock().lock(); // wait till no writer

      if (null == time) {
        // use range, null means all
        List<Dataset> need = filterDataset(range);
        for (Dataset ds : need) {
          if (useAll)
            scanAll(ds, range, null, act, counter);
          else
            scanStations(ds, stns, range, null, act, counter);
        }

      } else {
        // match specific time point
        Dataset ds = filterDataset(time);
        if (useAll)
          scanAll(ds, time, null, act, counter);
        else
          scanStations(ds, stns, time, null, act, counter);
      }

    } finally {
      lock.readLock().unlock();
    }

    w.trailer();

    if (debug) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\nread " + counter.count + " records; match and write " + counter.matches + " raw records");
      System.out.println("that took = " + took + " msecs");

      if (timeToScan > 0) {
        long writeTime = took - timeToScan;
        double mps = 1000 * counter.matches / writeTime;
        System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
      }
    }

    return w;
  }

  abstract class Writer {
    abstract void header(List<String> stns);

    abstract Action getAction();

    abstract void trailer();

    QueryParams qp;
    List<String> varNames;
    java.io.PrintWriter writer;
    DateFormatter format = new DateFormatter();
    int count = 0;

    Writer(QueryParams qp, List<String> varNames, final java.io.PrintWriter writer) {
      this.qp = qp;
      this.varNames = varNames;
      this.writer = writer;
    }

    List<VariableSimpleIF> getVars(List<String> varNames, List<VariableSimpleIF> dataVariables) {
      List<VariableSimpleIF> result = new ArrayList<VariableSimpleIF>();
      for (VariableSimpleIF v : dataVariables) {
        if ((varNames == null) || varNames.contains(v.getShortName()))
          result.add(v);
      }
      return result;
    }
  }


  class WriterNetcdf extends Writer {
    File netcdfResult;
    WriterStationObsDataset sobsWriter;
    List<ucar.unidata.geoloc.Station> stnList;
    List<VariableSimpleIF> varList;

    WriterNetcdf(QueryParams qp, List<String> varNames) throws IOException {
      super(qp, varNames, null);

      netcdfResult = File.createTempFile("ncss", ".nc");
      sobsWriter = new WriterStationObsDataset(netcdfResult.getAbsolutePath(), "Extracted data from Unidata/TDS Metar dataset");

      if ((varNames == null) || (varNames.size() == 0)) {
        varList = variableList;
      } else {
        varList = new ArrayList<VariableSimpleIF>(varNames.size());
        for (VariableSimpleIF v : variableList) {
          if (varNames.contains(v.getShortName()))
            varList.add(v);
        }
      }
    }

    public void header(List<String> stns) {
      try {
        getStationMap();

        if (stns.size() == 0)
          stnList = stationList;
        else {
          stnList = new ArrayList<ucar.unidata.geoloc.Station>(stns.size());

          for (String s : stns) {
            stnList.add(stationMap.get(s));
          }
        }

        sobsWriter.writeHeader(stnList, varList);
      } catch (IOException e) {
        log.error("WriterNetcdf.header", e);
      }
    }

    public void trailer() {
      try {
        sobsWriter.finish();
      } catch (IOException e) {
        log.error("WriterNetcdf.trailer", e);
      }
    }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          sobsWriter.writeRecord(sobs, sdata);
          count++;
        }
      };
    }
  }

  class WriterNetcdfStream extends Writer {
    WriterCFStationObsDataset sobsWriter;
    DataOutputStream out;
    List<ucar.unidata.geoloc.Station> stnList;
    List<VariableSimpleIF> varList;

    WriterNetcdfStream(QueryParams qp, List<String> varNames, OutputStream os) throws IOException {
      super(qp, varNames, null);

      out = new DataOutputStream( os);
      sobsWriter = new WriterCFStationObsDataset(out, "Extracted data from Unidata/TDS Metar dataset");

      if ((varNames == null) || (varNames.size() == 0)) {
        varList = variableList;
      } else {
        varList = new ArrayList<VariableSimpleIF>(varNames.size());
        for (VariableSimpleIF v : variableList) {
          if (varNames.contains(v.getShortName()))
            varList.add(v);
        }
      }
    }

    public void header(List<String> stns) {
      try {
        getStationMap();

        if (stns.size() == 0)
          stnList = stationList;
        else {
          stnList = new ArrayList<ucar.unidata.geoloc.Station>(stns.size());

          for (String s : stns) {
            stnList.add(stationMap.get(s));
          }
        }

        sobsWriter.writeHeader(stnList, varList, -1);
      } catch (IOException e) {
        log.error("WriterNetcdf.header", e);
      }
    }

    public void trailer() {
      try {
        sobsWriter.finish();
        out.flush();
      } catch (IOException e) {
        log.error("WriterNetcdf.trailer", e);
      }
    }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          sobsWriter.writeRecord(sobs, sdata);
          count++;
        }
      };
    }
  }


  class WriterCdmRemote extends Writer {
    WriterCFStationObsDataset sobsWriter;
    DataOutputStream out;
    List<ucar.unidata.geoloc.Station> stnList;
    List<VariableSimpleIF> varList;

    WriterCdmRemote(QueryParams qp, List<String> varNames, OutputStream os) throws IOException {
      super(qp, varNames, null);

      out = new DataOutputStream( os);
      sobsWriter = new WriterCFStationObsDataset(out, "Extracted data from Unidata/TDS Metar dataset");

      if ((varNames == null) || (varNames.size() == 0)) {
        varList = variableList;
      } else {
        varList = new ArrayList<VariableSimpleIF>(varNames.size());
        for (VariableSimpleIF v : variableList) {
          if (varNames.contains(v.getShortName()))
            varList.add(v);
        }
      }
    }

    public void header(List<String> stns) {
      try {
        getStationMap();

        if (stns.size() == 0)
          stnList = stationList;
        else {
          stnList = new ArrayList<ucar.unidata.geoloc.Station>(stns.size());

          for (String s : stns) {
            stnList.add(stationMap.get(s));
          }
        }

        sobsWriter.writeHeader(stnList, varList, -1);
      } catch (IOException e) {
        log.error("WriterNetcdf.header", e);
      }
    }

    public void trailer() {
      try {
        sobsWriter.finish();
        out.flush();
      } catch (IOException e) {
        log.error("WriterNetcdf.trailer", e);
      }
    }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          sobsWriter.writeRecord(sobs, sdata);
          count++;
        }
      };
    }
  }


  class WriterRaw extends Writer {

    WriterRaw(QueryParams qp, List<String> vars, final java.io.PrintWriter writer) {
      super(qp, vars, writer);
    }

    public void header(List<String> stns) {
    }

    public void trailer() {
      writer.flush();
    }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          writer.print( format.toDateTimeStringISO(sobs.getObservationTimeAsDate()));
          writer.print( "= ");
          String report = sdata.getScalarString("report");
          writer.println(report);
          count++;
        }
      };
    }
  }

  class WriterXML extends Writer {
    XMLStreamWriter staxWriter;

    WriterXML(QueryParams qp, List<String> vars, final java.io.PrintWriter writer) {
      super(qp, vars, writer);
      XMLOutputFactory f = XMLOutputFactory.newInstance();
      try {
        staxWriter = f.createXMLStreamWriter(writer);
      } catch (XMLStreamException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    public void header(List<String> stns) {
      try {
        staxWriter.writeStartDocument("UTF-8", "1.0");
        staxWriter.writeCharacters("\n");        
        staxWriter.writeStartElement("metarCollection");
        staxWriter.writeAttribute("dataset", datasetName);
        staxWriter.writeCharacters("\n ");
      } catch (XMLStreamException e) {
        throw new RuntimeException(e.getMessage());
      }

      //writer.println("<?xml version='1.0' encoding='UTF-8'?>");
      //writer.println("<metarCollection dataset='"+datasetName+"'>\n");
    }

    public void trailer() {
      try {
        staxWriter.writeEndElement();
        staxWriter.writeCharacters("\n");
        staxWriter.writeEndDocument();
        staxWriter.close();
      } catch (XMLStreamException e) {
        throw new RuntimeException(e.getMessage());
      }
      writer.flush();
    }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          ucar.unidata.geoloc.Station s = sobs.getStation();

          try {
            staxWriter.writeStartElement("metar");
            staxWriter.writeAttribute("date", format.toDateTimeStringISO(sobs.getObservationTimeAsDate()));
            staxWriter.writeCharacters("\n  ");

            staxWriter.writeStartElement("station");
            staxWriter.writeAttribute("name", s.getName());
            staxWriter.writeAttribute("latitude", Format.dfrac(s.getLatitude(), 3));
            staxWriter.writeAttribute("longitude", Format.dfrac(s.getLongitude(), 3));
            if (!Double.isNaN(s.getAltitude()))
              staxWriter.writeAttribute("altitude", Format.dfrac(s.getAltitude(), 0));
            if (s.getDescription() != null)
              staxWriter.writeCharacters(s.getDescription());
            staxWriter.writeEndElement();
            staxWriter.writeCharacters("\n ");

            List<VariableSimpleIF> vars = getVars(varNames, sod.getDataVariables());
            for (VariableSimpleIF var : vars) {
              staxWriter.writeCharacters(" ");
              staxWriter.writeStartElement("data");
              staxWriter.writeAttribute("name", var.getShortName());
              if (var.getUnitsString() != null)
                staxWriter.writeAttribute("units", var.getUnitsString());

              Array sdataArray = sdata.getArray(var.getShortName());
              String ss = sdataArray.toString();
              Class elemType = sdataArray.getElementType();
              if ((elemType == String.class) || (elemType == char.class) || (elemType == StructureData.class))
                ss = ucar.nc2.util.xml.Parse.cleanCharacterData(ss); // make sure no bad chars
              staxWriter.writeCharacters(ss);
              staxWriter.writeEndElement();
              staxWriter.writeCharacters("\n ");
            }
            staxWriter.writeEndElement();
            staxWriter.writeCharacters("\n");
            count++;
          } catch (XMLStreamException e) {
            throw new RuntimeException(e.getMessage());
          }
        }
      };
    }
  }

  class WriterCSV extends Writer {
    boolean headerWritten = false;
    List<VariableSimpleIF> validVars;

    WriterCSV(QueryParams qp, List<String> stns, final java.io.PrintWriter writer) {
      super(qp, stns, writer);
    }

    public void header(List<String> stns) {
    }

    public void trailer() {
      writer.flush();
    }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          if (!headerWritten) {
            writer.print("time,station,latitude[unit=\"degrees_north\"],longitude[unit=\"degrees_east\"]");
            validVars = getVars(varNames, sod.getDataVariables());
            for (VariableSimpleIF var : validVars) {
              writer.print(",");
              writer.print(var.getShortName());
              if (var.getUnitsString() != null)
                writer.print("[unit=\"" + var.getUnitsString() + "\"]");
            }
            writer.println();
            headerWritten = true;
          }

          ucar.unidata.geoloc.Station s = sobs.getStation();

          writer.print(format.toDateTimeStringISO(sobs.getObservationTimeAsDate()));
          writer.print(',');
          writer.print(s.getName());
          writer.print(',');
          writer.print(Format.dfrac(s.getLatitude(), 3));
          writer.print(',');
          writer.print(Format.dfrac(s.getLongitude(), 3));

          for (VariableSimpleIF var : validVars) {
            writer.print(',');
            Array sdataArray = sdata.getArray(var.getShortName());
            writer.print(sdataArray.toString());
          }
          writer.println();
          count++;
        }
      };
    }
  }

  static public void main(String args[]) throws IOException {
    //getFiles("R:/testdata2/station/ldm/metar/");
    // StationObsCollection soc = new StationObsCollection("C:/data/metars/", false);
  }

}
