/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.server.ncSubset;

import ucar.ma2.StructureData;
import ucar.ma2.Array;
import ucar.nc2.dt.*;
import ucar.nc2.dt.point.StationObsDatasetWriter;
import ucar.nc2.dt.point.StationObsDatasetInfo;
import ucar.nc2.VariableIF;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.Format;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import thredds.datatype.DateRange;
import thredds.datatype.DateType;
import thredds.catalog.XMLEntityResolver;
import org.jdom.Document;
import org.jdom.Element;

public class StationObsCollection {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StationObsCollection.class);
  static private org.slf4j.Logger cacheLogger = org.slf4j.LoggerFactory.getLogger("cacheLogger");

  private static boolean debug = true, debugDetail = false;
  private static long timeToScan = 0;

  private String archiveDir, realtimeDir;
  private ArrayList<Dataset> datasetList;
  private List<VariableSimpleIF> variableList;
  private DateFormatter format = new DateFormatter();

  private boolean isRealtime;
  private Date start, end;
  private Timer timer;
  private ReadWriteLock lock = new ReentrantReadWriteLock();

  public StationObsCollection(String archiveDir, String realtimeDir) {
    this.archiveDir = archiveDir;
    this.realtimeDir = realtimeDir;
    this.isRealtime = (realtimeDir != null);

    if (isRealtime) { // LOOK what if not realtime ??
      timer = new Timer("StationObsCollection.Rescan");
      Calendar c = Calendar.getInstance(); // contains current startup time
      c.add(Calendar.HOUR, 24); // start tommorrow
      c.set(Calendar.HOUR, 1); // at 1 AM
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      timer.schedule(new ReinitTask(), 1000 * 5); // do in 5 secs
      timer.schedule(new ReinitTask(), c.getTime(), (long) 1000 * 60 * 60 * 24); // repeat once a day
      cacheLogger.info("StationObsCollection timer set to run at " + c.getTime());
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
      cacheLogger.info("StationObsCollection.reinit at " + format.toDateTimeString(new Date()));
      init();
    }
  }

  public String getName() {
    return archiveDir + "/" + realtimeDir;
  }

  public ArrayList<Dataset> getDatasets() {
    return datasetList;
  }

  ////////////////////////////////////////////
  // keep track of the available datasets LOOK should be configurable
  private String dateFormatString = "yyyyMMdd_HHmm";
  private FileFilter ff = new org.apache.commons.io.filefilter.SuffixFileFilter(".nc");

  private void init() {
    CollectionManager archive = new CollectionManager( archiveDir, ff, dateFormatString);
    CollectionManager realtime = new CollectionManager( realtimeDir, ff, dateFormatString);

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
      start = datasetList.get(0).time_start;
      end = datasetList.get(n - 1).time_end;

    } finally {
      lock.writeLock().unlock();
    }

    datasetDesc = null; // mostly to create new time range
  }

  // make new archive files
  private void makeArchiveFiles(CollectionManager archive, CollectionManager realtime) {

    // the set of files in realtime collection that come after the archive files
    List<CollectionManager.MyFile> realtimeList = realtime.after( archive.getLatest());
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
        cacheLogger.info("StationObsCollection: write " + fileIn + " to archive " + fileOut);
        StationObsDatasetWriter.rewrite(fileIn, fileOut);
        archive.add(new File(fileOut), myfile.date);
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
    c.add(Calendar.HOUR, -8 * 24); // 7 or 8 days ago
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
        cacheLogger.info("StationObsCollection: Deleted archive file " + myfile+" ok= "+ok);
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

    List<CollectionManager.MyFile> realtimeList = realtime.after( archive.getLatest());
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
        StringBuffer sbuff = new StringBuffer();
        if (debug) System.out.println("StationObsDataset open " + filename);
        sod = (StationObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.STATION, filename, null, sbuff);
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
    root.setAttribute("location", "/thredds/ncss/metar");

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

  private void scanAll(Dataset ds, DateRange range, Predicate p, Action a, Limit limit) throws IOException {
    StringBuffer sbuff = new StringBuffer();
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
    }

  }

  private void scanStations(Dataset ds, List<String> stns, DateRange range, Predicate p, Action a, Limit limit) throws IOException {
    StringBuffer sbuff = new StringBuffer();

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

  private void scanAll(Dataset ds, DateType time, Predicate p, Action a, Limit limit) throws IOException {
    StringBuffer sbuff = new StringBuffer();

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


  private void scanStations(Dataset ds, List<String> stns, DateType time, Predicate p, Action a, Limit limit) throws IOException {
    StringBuffer sbuff = new StringBuffer();

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
      if (range.intersect(ds.time_start, ds.time_end))
        result.add(ds);
    }
    return result;
  }

  Dataset filterDataset(DateType time) {
    if (time.isPresent())
      return datasetList.get(datasetList.size() - 1);

    for (Dataset ds : datasetList) {
      if (time.before(ds.time_end) && time.after(ds.time_start))
        return ds;
      if (time.equals(ds.time_end) || time.equals(ds.time_start))
        return ds;
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////
  // writing

  //private File netcdfResult = new File("C:/temp/sobs.nc");

  public File writeNetcdf(List<String> vars, List<String> stns, DateRange range, DateType time) throws IOException {
    WriterNetcdf w = (WriterNetcdf) write(vars, stns, range, time, QueryParams.NETCDF, null);
    return w.netcdfResult;
  }


  public Writer write(List<String> vars, List<String> stns, DateRange range, DateType time, String type, java.io.PrintWriter pw) throws IOException {
    long start = System.currentTimeMillis();
    Limit counter = new Limit();

    Writer w;
    if (type.equals(QueryParams.RAW)) {
      w = new WriterRaw(vars, pw);
    } else if (type.equals(QueryParams.XML)) {
      w = new WriterXML(vars, pw);
    } else if (type.equals(QueryParams.CSV)) {
      w = new WriterCSV(vars, pw);
    } else if (type.equals(QueryParams.NETCDF)) {
      w = new WriterNetcdf(vars, pw);
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
        Dataset need = filterDataset(time);
        if (useAll)
          scanAll(need, time, null, act, counter);
        else
          scanStations(need, stns, time, null, act, counter);
      }

    } finally {
      lock.readLock().unlock();
    }

    w.trailer();

    if (pw != null) pw.flush();

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

    List<String> varNames;
    java.io.PrintWriter writer;
    DateFormatter format = new DateFormatter();

    Writer(List<String> varNames, final java.io.PrintWriter writer) {
      this.varNames = varNames;
      this.writer = writer;
    }

    List<VariableIF> getVars(List<String> varNames, List dataVariables) {
      List<VariableIF> result = new ArrayList<VariableIF>();
      for (Object dataVariable : dataVariables) {
        VariableIF v = (VariableIF) dataVariable;
        if ((varNames == null) || varNames.contains(v.getName()))
          result.add(v);
      }
      return result;
    }
  }


  class WriterNetcdf extends Writer {
    File netcdfResult;
    StationObsDatasetWriter sobsWriter;
    List<Station> stnList;
    List<VariableSimpleIF> varList;

    WriterNetcdf(List<String> varNames, final java.io.PrintWriter writer) throws IOException {
      super(varNames, writer);

      netcdfResult = File.createTempFile("ncss", ".nc");
      sobsWriter = new StationObsDatasetWriter(netcdfResult.getAbsolutePath());

      if ((varNames == null) || (varNames.size() == 0)) {
        varList = variableList;
      } else {
        varList = new ArrayList<VariableSimpleIF>(varNames.size());
        for (VariableSimpleIF v : variableList) {
          if (varNames.contains(v.getName()))
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
          stnList = new ArrayList<Station>(stns.size());

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
        }
      };
    }
  }

  class WriterRaw extends Writer {

    WriterRaw(List<String> vars, final java.io.PrintWriter writer) {
      super(vars, writer);
    }

    public void header(List<String> stns) {
    }

    public void trailer() {
    }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          String report = sdata.getScalarString("report");
          writer.println(report);
        }
      };
    }
  }


  class WriterXML extends Writer {

    WriterXML(List<String> vars, final java.io.PrintWriter writer) {
      super(vars, writer);
    }

    public void header(List<String> stns) {
      writer.println("<?xml version='1.0' encoding='UTF-8'?>");
      writer.println("<metarCollection dataset='name'>\n");
    }

    public void trailer() {
      writer.println("</metarCollection>");
    }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          Station s = sobs.getStation();

          writer.print("  <metar date='");
          writer.print(format.toDateTimeStringISO(sobs.getObservationTimeAsDate()));
          writer.println("'>");

          writer.print("    <station name='" + s.getName() +
                  "' latitude='" + Format.dfrac(s.getLatitude(), 3) +
                  "' longitude='" + Format.dfrac(s.getLongitude(), 3));
          if (!Double.isNaN(s.getAltitude()))
            writer.print("' altitude='" + Format.dfrac(s.getAltitude(), 0));
          if (s.getDescription() != null) {
            writer.println("'>");
            writer.print(s.getDescription());
            writer.println("</station>");
          } else {
            writer.println("'/>");
          }


          List<VariableIF> vars = getVars(varNames, sod.getDataVariables());
          for (VariableIF var : vars) {
            Attribute unitAtt = var.findAttribute("units");
            writer.print("    <data name='" + var.getName());
            if (unitAtt != null)
              writer.print("' units='" + unitAtt.getStringValue());
            writer.print("'>");
            Array sdataArray = sdata.getArray(var.getName());
            writer.println(sdataArray.toString() + "</data>");
          }
          writer.println("  </metar>");
        }
      };
    }
  }

  class WriterCSV extends Writer {
    boolean headerWritten = false;
    List<VariableIF> validVars;

    WriterCSV(List<String> stns, final java.io.PrintWriter writer) {
      super(stns, writer);
    }

    public void header(List<String> stns) {
    }

    public void trailer() {
    }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          if (!headerWritten) {
            writer.print("time,station,latitude,longitude");
            validVars = getVars(varNames, sod.getDataVariables());
            for (VariableIF var : validVars) {
              writer.print(",");
              writer.print(var.getName());
            }
            writer.println();
            headerWritten = true;
          }

          Station s = sobs.getStation();

          writer.print(format.toDateTimeStringISO(sobs.getObservationTimeAsDate()));
          writer.print(',');
          writer.print(s.getName());
          writer.print(',');
          writer.print(Format.dfrac(s.getLatitude(), 3));
          writer.print(',');
          writer.print(Format.dfrac(s.getLongitude(), 3));

          for (VariableIF var : validVars) {
            writer.print(',');
            Array sdataArray = sdata.getArray(var.getName());
            writer.print(sdataArray.toString());
          }
          writer.println();
        }
      };
    }
  }

  ///////////////////////////////////////////////////////////

  /* public void timeNetcdf() throws IOException {

    Limit limit = new Limit();
    Predicate p = new Predicate() {
      public boolean match(StructureData sdata) {
        return (sdata.getScalarFloat("wind_peak_speed") > 10);
      }
    };
    Action act = new Action() {
      public void act(StructureData sdata) {
      }
    };

    long start = System.currentTimeMillis();
    for (int i = 0; i < fileList.size(); i++) {
      String s = fileList.get(i);
      scanAll(s, p, act, limit);
    }

    long took = System.currentTimeMillis() - start;
    double mps = 1000 * limit.count / took;
    System.out.println("\nscanAllNetcdf successfully read " + limit.count + " records; found " + limit.matches + " matches");
    System.out.println("that took = " + took + " msecs; messages/sec = " + mps);

    timeToScan = took;
  }

  public void timeNetcdfStation() throws IOException {

    Limit limit = new Limit();
    Predicate p = new Predicate() {
      public boolean match(StructureData sdata) {
        return true;
      }
    };
    Action act = new Action() {
      public void act(StructureData sdata) {
      }
    };

    long start = System.currentTimeMillis();
    List<String> stns = new ArrayList<String>();
    stns.add("ACK");

    for (int i = 0; i < fileList.size(); i++) {
      String s = fileList.get(i);
      scanStations(s, stns, p, act, limit);
    }

    long took = System.currentTimeMillis() - start;
    double mps = 1000 * limit.count / took;
    System.out.println("\ntimeNetcdfStation successfully read " + limit.count + " records; found " + limit.matches + " matches");
    System.out.println("that took = " + took + " msecs; obs/sec= " + mps);
  }

  private static DataOutputStream xout;

  public void writeXML() throws IOException {
    xout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("C:/temp/temp2.xml"), 10000));
    xout.writeBytes("<?xml version='1.0' encoding='UTF-8'?>\n");
    xout.writeBytes("<metarCollection dataset='name'>\n");

    Limit limit = new Limit();
    Predicate p = new Predicate() {
      public boolean match(StructureData sdata) {
        return (sdata.getScalarFloat("wind_peak_speed") > 10);
      }
    };

    Action act = new Action() {
      public void act(StructureData sdata) throws IOException {
        xout.writeBytes("  <metar>\n");
        for (Iterator iter = sdata.getMembers().iterator(); iter.hasNext();) {
          StructureMembers.Member m = (StructureMembers.Member) iter.next();
          xout.writeBytes("    <data name='" + m.getName() + "'>");

          Array sdataArray = sdata.getArray(m);
          // System.out.println(m.getName()+" "+m.getDataType()+" "+sdataArray.toString());
          xout.writeBytes(sdataArray.toString() + "</data>\n");
        }

        xout.writeBytes("  </metar>\n");
      }
    };

    long start = System.currentTimeMillis();

    for (int i = 0; i < fileList.size(); i++) {
      String s = fileList.get(i);
      scanAll(s, p, act, limit);
    }

    xout.writeBytes("</metarCollection>\n");
    xout.close();

    long took = System.currentTimeMillis() - start;
    System.out.println("\nscanAllNetcdf  read " + limit.count + " records; match and write " + limit.matches + " XML records");
    System.out.println("that took = " + took + " msecs");

    long writeTime = took - timeToScan;
    double mps = 1000 * limit.matches / writeTime;
    System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
  }

  private BufferedOutputStream fout;

  public void writeRaw() throws IOException {
    fout = new BufferedOutputStream(new FileOutputStream("C:/temp/raw.txt"), 10000);

    Limit limit = new Limit();
    Predicate p = new Predicate() {
      public boolean match(StructureData sdata) {
        return (sdata.getScalarFloat("wind_peak_speed") > 10);
      }
    };

    Action act = new Action() {
      public void act(StructureData sdata) throws IOException {
        String report = sdata.getScalarString("report");
        fout.write(report.getBytes());
        fout.write((int) '\n');
      }
    };

    long start = System.currentTimeMillis();

    for (int i = 0; i < fileList.size(); i++) {
      String s = fileList.get(i);
      scanAll(s, p, act, limit);
    }

    fout.close();

    long took = System.currentTimeMillis() - start;
    System.out.println("\nscanAllNetcdf  read " + limit.count + " records; match and write " + limit.matches + " raw records");
    System.out.println("that took = " + took + " msecs");

    long writeTime = took - timeToScan;
    double mps = 1000 * limit.matches / writeTime;
    System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
  }

  /* public static void  addData(StructureData sdata) {

   Element elem = new Element("metar");
   for (Iterator iter = sdata.getMembers().iterator(); iter.hasNext();) {
     StructureMembers.Member m = (StructureMembers.Member) iter.next();
     Element dataElem = new Element("data");
     dataElem.setAttribute("name", m.getName());
     elem.addContent(dataElem);

     Array sdataArray = sdata.getArray(m);
     // System.out.println(m.getName()+" "+m.getDataType()+" "+sdataArray.toString());
     dataElem.addContent(sdataArray.toString());
   }

   rootElem.addContent(elem);
 } */
  /*

 private SimpleDateFormat dateFormat;

 public MetarCollection( String dirLocation) {
    dateFormat = new java.text.SimpleDateFormat("yyyyMMdd_HHmm");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

   private class DatasetWrap implements Comparable {
   String location;
   Date start, end;

   DatasetWrap( String location) {
     this.location = location;
     int pos0 = location.lastIndexOf("Surface_METAR_");
     int pos1 = location.lastIndexOf(".");
     String dateString = location.substring(pos0+14, pos1);
     Date nominal = null;
     try {
       nominal = dateFormat.parse( dateString);
     } catch (ParseException e) {
       throw new IllegalStateException(e.getMessage());
     }
     Calendar c = Calendar.getInstance( TimeZone.getTimeZone("GMT"));
     c.setTime( nominal);
     c.add( Calendar.HOUR, -1);
     start = c.getTime();
     c.setTime( nominal);
     c.add( Calendar.HOUR, 24);
     end = c.getTime();
     // System.out.println("  "+dateString+" = "+start+" end= "+end);
   }

   public boolean contains( Date startWant, Date endWant) {
     if (start.after( endWant)) return false;
     if (startWant.after( end)) return false;
     return true;
   }

   public int compareTo(Object o) {
     DatasetWrap dw = (DatasetWrap) o;
     return start.compareTo( dw.start);
   }
 } */


  static public void main(String args[]) throws IOException {
    //getFiles("R:/testdata/station/ldm/metar/");
    // StationObsCollection soc = new StationObsCollection("C:/data/metars/", false);
  }

}
