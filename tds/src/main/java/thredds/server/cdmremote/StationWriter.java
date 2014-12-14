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

package thredds.server.cdmremote;

import thredds.server.cdmremote.params.CdmrfQueryBean;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ft.point.remote.PointStream;
import ucar.nc2.ft.point.remote.PointStreamProto;
import ucar.nc2.ft.point.writer.WriterCFStationCollection;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeUnit;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.Format;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * CdmrFeature subsetting for station data.
 * thread safety: new object for each request
 *
 * @author caron
 * @since Aug 19, 2009
 */
public class StationWriter {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StationWriter.class);

  private static final boolean debug = false, debugDetail = false;

  private final FeatureDatasetPoint fd;
  private final StationTimeSeriesFeatureCollection sfc;
  //private final CdmRemoteQueryBean qb;
  private final CdmrfQueryBean qb;
  
  private final Date start, end;

  private List<VariableSimpleIF> wantVars;
  private DateRange wantRange;
  private ucar.nc2.util.DiskCache2 diskCache;

  public StationWriter(FeatureDatasetPoint fd, StationTimeSeriesFeatureCollection sfc, CdmrfQueryBean qb, ucar.nc2.util.DiskCache2 diskCache) throws IOException {
    this.fd = fd;
    this.sfc = sfc;
    this.qb = qb;
    this.diskCache = diskCache;

    start = fd.getStartDate();
    end = fd.getEndDate();
  }

  public boolean validate(HttpServletResponse res) throws IOException {

    // verify TemporalSelection intersects
    if (qb.getTemporalSelection() == CdmrfQueryBean.TemporalSelection.range) {
      wantRange = qb.getDateRange();
      DateRange haveRange = fd.getDateRange();
      if ((haveRange != null) && !haveRange.intersects(wantRange)) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "ERROR: This dataset does not include the requested time range= " + wantRange +
                "\ndataset time range = " + haveRange);
        return false;
      }
    }

    // restrict to these variables
    List<? extends VariableSimpleIF> dataVars = fd.getDataVariables();
    String[] vars = qb.getVarNames();
    List<String> varNames = (vars == null) ? null : Arrays.asList(vars);
    if ((varNames == null) || (varNames.size() == 0)) {
      wantVars = new ArrayList<>(dataVars);
    } else {
      wantVars = new ArrayList<>();
      for (VariableSimpleIF v : dataVars) {
        if (varNames.contains(v.getShortName())) // LOOK N**2
          wantVars.add(v);
      }
    }

    // verify SpatialSelection has some stations
    if (qb.getSpatialSelection() == CdmrfQueryBean.SpatialSelection.bb) {
      LatLonRect bb = sfc.getBoundingBox();
      if ((bb != null) && (bb.intersect(qb.getLatLonRect()) == null)) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "ERROR: Bounding Box contains no stations; bb= " + qb.getLatLonRect());
        return false;
      }
      //System.out.printf("sfc.flatten0 wantRange= %s on %s %n", wantRange, fd.getLocation());
      // pfc = sfc.flatten(qb.getLatLonRect(), wantRange);

    } else if (qb.getSpatialSelection() == CdmrfQueryBean.SpatialSelection.stns) {
      if (!contains(sfc, qb.getStnNames())) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "ERROR: No valid stations specified = " + qb.getStn());
        return false;
      }
      //System.out.printf("sfc.flatten1 wantRange= %s on %s %n", wantRange, fd.getLocation());
      // List<String> wantStns = Arrays.asList(qb.getStnNames());
      //pfc = sfc.flatten(wantStns, wantRange, null);

    } else {
      //System.out.printf("sfc.flatten2 wantRange= %s on %s %n", wantRange, fd.getLocation());
      //pfc = sfc.flatten(null, wantRange, null);
    }

    return true;
  }

  private boolean contains(StationTimeSeriesFeatureCollection sfc, String[] stnNames) {
    for (String name : stnNames) {
      if (sfc.getStation(name) != null) return true;
    }
    return false;
  }


  ////////////////////////////////////////////////////////////////
  // writing

  public File writeNetcdf() throws IOException {
    WriterNetcdf w = (WriterNetcdf) write(null);
    return w.netcdfResult;
  }

  public Writer write(HttpServletResponse res) throws IOException {
    long start = System.currentTimeMillis();
    Limit counter = new Limit();
    //counter.limit = 150;

    // which writer, based on desired response
    CdmrfQueryBean.ResponseType resType = qb.getResponseType();
    Writer w;
    if (resType == CdmrfQueryBean.ResponseType.xml) {
      w = new WriterXML(res.getWriter());
    } else if (resType == CdmrfQueryBean.ResponseType.csv) {
      w = new WriterCSV(res.getWriter());
    } else if (resType == CdmrfQueryBean.ResponseType.netcdf) {
      w = new WriterNetcdf();
    } else if (resType == CdmrfQueryBean.ResponseType.ncstream) {
      w = new WriterNcstream(res.getOutputStream());
    } else {
      log.error("Unknown result type = " + resType);
      return null;
    }

    // for closet time, set wantRange to the time LOOK - do we need +- increment ??
    if (qb.getTemporalSelection() == CdmrfQueryBean.TemporalSelection.point) {
      TimeUnit hour = null;
      try {
        hour = new TimeUnit(1, "hour");
      } catch (Exception e) {
        e.printStackTrace();
        log.error("bad time unit", e);
        return null;
      }
      DateType startR =  qb.getTimePoint().subtract(hour);
      DateType endR =  qb.getTimePoint().add(hour);
      wantRange = new DateRange(startR.getDate(), endR.getDate());
    }

    // time: all, range, point
    // spatial: all, bb, point, stns
    //StationTimeSeriesFeatureCollection useFc = sfc;
    PointFeatureCollection pfc = null;
    switch (qb.getSpatialSelection()) {

        case all:
            pfc = sfc.flatten(null,  CalendarDateRange.of(wantRange));
            break;

        case bb:
            //useFc = sfc.subset(qb.getLatLonRect());
            pfc = sfc.flatten(qb.getLatLonRect(), CalendarDateRange.of(wantRange));
            break;

        case point:
            Station closestStation = findClosestStation(qb.getLatlonPoint());
            List<String> stn = new ArrayList<String>();
            stn.add(closestStation.getName());
            //useFc = sfc.subset(stn);
            pfc = sfc.flatten(stn, CalendarDateRange.of(wantRange), null);
            break;

        case stns:
            //List<Station> wantStns = getStationList(qb.getStnNames());
            //useFc = sfc.subset(stns);
            List<String> wantStns = Arrays.asList(qb.getStnNames());
            pfc = sfc.flatten(wantStns, CalendarDateRange.of(wantRange), null);
            break;
    }

    Action act = w.getAction();
    w.header();

    if (qb.getTemporalSelection() == CdmrfQueryBean.TemporalSelection.point) {
      scanForClosestTime(pfc, qb.getTimePoint(), null, act, counter);

    } else {
      scan(pfc, wantRange, null, act, counter);
    }

    w.trailer();

    if (debug) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\nread " + counter.count + " records; match and write " + counter.matches + " raw records");
      System.out.println("that took = " + took + " msecs");

      long timeToScan = 0; // ??
      if (timeToScan > 0) {
        long writeTime = took - timeToScan;
        double mps = 1000.0 * counter.matches / writeTime;
        System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
      }
    }

    return w;
  }

  ///////////////////////////////////////
  // station handling
  private HashMap<String, Station> stationMap;

  /*
   * Determine if any of the given station names are actually in the dataset.
   *
   * @param stns List of station names
   * @return true if list is empty, ie no names are in the actual station list
   * @throws IOException if read error
   */
  private boolean isStationListEmpty(List<String> stns) throws IOException {
    makeStationMap();
    for (String stn : stns) {
      if (stationMap.get(stn) != null) return false;
    }
    return true;
  }

  private List<Station> getStationList(String[] stnNames) throws IOException {
    makeStationMap();

    List<Station> result = new ArrayList<>(stnNames.length);
    for (String s : stnNames) {
      Station stn = stationMap.get(s);
      if (stn != null)
        result.add(stn);
    }

    return result;
  }

  private void makeStationMap() throws IOException {
    if (null == stationMap) {
      stationMap = new HashMap<>();
      for (Station station : sfc.getStations()) {
        stationMap.put(station.getName(), station);
      }
    }
  }

  /*
   * Get the list of station names that are contained within the bounding box.
   *
   * @param boundingBox lat/lon bounding box
   * @return list of station names contained within the bounding box
   * @throws IOException if read error
   */
  public List<String> getStationNames(LatLonRect boundingBox) throws IOException {
    LatLonPointImpl latlonPt = new LatLonPointImpl();
    ArrayList<String> result = new ArrayList<>();
    for (Station s : sfc.getStations()) {
      latlonPt.set(s.getLatitude(), s.getLongitude());
      if (boundingBox.contains(latlonPt)) {
        result.add(s.getName());
        // boundingBox.contains(latlonPt);   debugging
      }
    }
    return result;
  }

  /*
   * Find the station closest to the specified point.
   * The metric is (lat-lat0)**2 + (cos(lat0)*(lon-lon0))**2
   *
   * @param lat latitude value
   * @param lon longitude value
   * @return name of station closest to the specified point
   * @throws IOException if read error
   */
  public Station findClosestStation(LatLonPoint pt) throws IOException {
    double lat = pt.getLatitude();
    double lon = pt.getLongitude();
    double cos = Math.cos(Math.toRadians(lat));
    List<Station> stations = sfc.getStations();
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
    return min_station;
  }

  /////////////////////

  public boolean intersect(DateRange dr) throws IOException {
    return dr.intersects(start, end);
  }

  ////////////////////////////////////////////////////////
  // scanning flattened collection

  // scan PointFeatureCollection, records that pass the predicate match are acted on, within limits
  private void scan(PointFeatureCollection collection, DateRange range, Predicate p, Action a, Limit limit) throws IOException {

    collection.resetIteration();
    while (collection.hasNext()) {
      PointFeature pf = collection.next();

      if (range != null) {
        Date obsDate = pf.getObservationTimeAsDate(); // LOOK: needed?
        if (!range.contains(obsDate)) continue;
      }
      limit.count++;

      StructureData sdata = pf.getData();
      if ((p == null) || p.match(sdata)) {
        a.act(pf, sdata);
        limit.matches++;
      }

      if (limit.matches > limit.limit) {
        collection.finish();
        break;
      }
      if (debugDetail && (limit.matches % 50 == 0)) System.out.println(" matches " + limit.matches);
    }
    collection.finish();
  }

  // scan all data in the file, first eliminate any that dont pass the predicate
  // for each station, track the closest record to the given time
  // then act on those
  private void scanForClosestTime(PointFeatureCollection collection, DateType time, Predicate p, Action a, Limit limit) throws IOException {

    HashMap<String, StationDataTracker> map = new HashMap<>();
    long wantTime = time.getDate().getTime();

    collection.resetIteration();
    while (collection.hasNext()) {
      PointFeature pf = collection.next();
      //System.out.printf("%s%n", pf);

      // general predicate filter
      if (p != null) {
        StructureData sdata = pf.getData();
        if (!p.match(sdata))
          continue;
      }

      // find closest time for this station
      long obsTime = pf.getObservationTimeAsDate().getTime();
      long diff = Math.abs(obsTime - wantTime);

      Station s = ((StationPointFeature)pf).getStation();
      StationDataTracker track = map.get(s.getName());
      if (track == null) {
        map.put(s.getName(), new StationDataTracker(pf, diff));
      } else {
        if (diff < track.timeDiff) {
          track.sobs = pf;
          track.timeDiff = diff;
        }
      }
    }

    for (Map.Entry<String, StationDataTracker> entry : map.entrySet()) {
      StationDataTracker track = entry.getValue();
      a.act(track.sobs, track.sobs.getData());
      limit.matches++;

      limit.count++;
      if (limit.count > limit.limit) break;
    }

  }

  private static class StationDataTracker {
    PointFeature sobs;
    long timeDiff = Long.MAX_VALUE;

    StationDataTracker(PointFeature sobs, long timeDiff) {
      this.sobs = sobs;
      this.timeDiff = timeDiff;
    }
  }


  // scan StationTimeSeriesFeatureCollection, records that pass the DateRange and Predicate match are acted on, within limits
  private void scan(StationTimeSeriesFeatureCollection collection, DateRange range, Predicate p, Action a, Limit limit) throws IOException {

    while (collection.hasNext()) {
      StationTimeSeriesFeature sf = collection.next();

      while (sf.hasNext()) {
        PointFeature pf = sf.next();

        if (range != null) {
          Date obsDate = pf.getObservationTimeAsDate();
          if (!range.contains(obsDate)) continue;
        }
        limit.count++;

        StructureData sdata = pf.getData();
        if ((p == null) || p.match(sdata)) {
          a.act(pf, sdata);
          limit.matches++;
        }

        if (limit.matches > limit.limit) {
          sf.finish();
          break;
        }
        if (debugDetail && (limit.matches % 50 == 0)) System.out.println(" matches " + limit.matches);
      }

      if (limit.matches > limit.limit) {
        collection.finish();
        break;
      }
    }

  }

  ////////////////////////////////////////////////////////
  // scanning station collection

  /* scan data for the list of stations, in order
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

  } */

  // scan all data in the file, first eliminate any that dont pass the predicate
  // for each station, track the closest record to the given time
  /* then act on those
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

  }  */

  ////////////////////////////////////////////////////////////////
  /* date filter

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
  }  */

  private interface Predicate {
    boolean match(StructureData sdata);
  }

  private interface Action {
    void act(PointFeature pf, StructureData sdata) throws IOException;
  }

  static private class Limit {
    int count;   // how many scanned
    int limit = Integer.MAX_VALUE; // max matches
    int matches; // how want matched
  }

  public abstract class Writer {
    abstract void header() throws IOException;

    abstract Action getAction();

    abstract void trailer() throws IOException;

    java.io.PrintWriter writer;
    int count = 0;

    Writer(final java.io.PrintWriter writer) {
      this.writer = writer; // LOOK what about buffering?
    }
  }

  class WriterNetcdf extends Writer {
    File netcdfResult;
    WriterCFStationCollection cfWriter;
    boolean headerWritten = false;
    private List<StationFeature> wantStations;

    WriterNetcdf() throws IOException {
      super(null);

      netcdfResult = diskCache.createUniqueFile("cdmSW", ".nc");
      List<Attribute> atts = new ArrayList<>();
      atts.add(new Attribute(CDM.TITLE, "Extracted data from TDS using CDM remote subsetting"));
      cfWriter = null; // new WriterCFStationCollection(null, netcdfResult.getAbsolutePath(), atts);

      // verify SpatialSelection has some stations
      if (qb.getSpatialSelection() == CdmrfQueryBean.SpatialSelection.bb) {
        wantStations = sfc.getStationFeatures(qb.getLatLonRect());

      } else if (qb.getSpatialSelection() == CdmrfQueryBean.SpatialSelection.stns) {
        List<String> stnNames = Arrays.asList(qb.getStnNames());
        wantStations = sfc.getStationFeatures(stnNames);

      } else {
        wantStations = sfc.getStationFeatures();
      }

    }

    public void header() {
    }

    public void trailer() throws IOException {
      cfWriter.finish();
    }

    Action getAction() {
      return new Action() {
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          if (!headerWritten) {
            try {
              // LOOK cfWriter.writeHeader(wantStations, wantVars, sfc.getTimeUnit(), sfc.getAltUnits(), null);
              cfWriter.writeHeader(wantStations, (StationPointFeature) pf);
              headerWritten = true;
            } catch (IOException e) {
              log.error("WriterNetcdf.header", e);
            }
          }

          cfWriter.writeRecord(sfc.getStation(pf), pf, sdata);
          count++;
        }
      };
    }
  }

  class WriterNcstream extends Writer {
    OutputStream out;

    WriterNcstream(OutputStream os) throws IOException {
      super(null);
      out = os;
    }

    public void header() throws IOException {
      // PointStream.writeMagic(out, PointStream.MessageType.Start);  // LOOK - not ncstream protocol
    }

    public void trailer() throws IOException {
        PointStream.writeMagic(out, PointStream.MessageType.End);
        out.flush();
    }

    Action getAction() {
      return new Action() {
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          try {
            if (count == 0) {  // first time : need a point feature so cant do it in header
              PointStreamProto.PointFeatureCollection proto = PointStream.encodePointFeatureCollection(fd.getLocation(), sfc.getTimeUnit().getTimeUnitString(), pf);
              byte[] b = proto.toByteArray();
              PointStream.writeMagic(out, PointStream.MessageType.PointFeatureCollection);
              NcStream.writeVInt(out, b.length);
              out.write(b);
            }

            PointStreamProto.PointFeature pfp = PointStream.encodePointFeature(pf);
            byte[] b = pfp.toByteArray();
            PointStream.writeMagic(out, PointStream.MessageType.PointFeature);
            NcStream.writeVInt(out, b.length);
            out.write(b);
            count++;

          } catch (Throwable t) {
            String mess = t.getMessage();
            if (mess == null) mess = t.getClass().getName();
            NcStreamProto.Error err = NcStream.encodeErrorMessage(t.getMessage());
            byte[] b = err.toByteArray();
            PointStream.writeMagic(out, PointStream.MessageType.Error);
            NcStream.writeVInt(out, b.length);
            out.write(b);

            throw new IOException(t);
          }
        }
      };
    }
  }


  class WriterRaw extends Writer {

    WriterRaw(final java.io.PrintWriter writer) {
      super(writer);
    }

    public void header() {
    }

    public void trailer() {
      writer.flush();
    }

    Action getAction() {
      return new Action() {
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          writer.print(CalendarDateFormatter.toDateTimeString(pf.getObservationTimeAsCalendarDate()));
          writer.print("= ");
          String report = sdata.getScalarString("report");
          writer.println(report);
          count++;
        }
      };
    }
  }

  class WriterXML extends Writer {
    XMLStreamWriter staxWriter;

    WriterXML(final java.io.PrintWriter writer) {
      super(writer);
      XMLOutputFactory f = XMLOutputFactory.newInstance();
      try {
        staxWriter = f.createXMLStreamWriter(writer);
      } catch (XMLStreamException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    public void header() {
      try {
        staxWriter.writeStartDocument("UTF-8", "1.0");
        staxWriter.writeCharacters("\n");
        staxWriter.writeStartElement("stationFeatureCollection");
        //staxWriter.writeAttribute("dataset", datasetName);
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
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          Station s = sfc.getStation(pf);

          try {
            staxWriter.writeStartElement("pointFeature");
            staxWriter.writeAttribute("date", CalendarDateFormatter.toDateTimeString(pf.getObservationTimeAsCalendarDate()));
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

            for (VariableSimpleIF var : wantVars) {
              staxWriter.writeCharacters(" ");
              staxWriter.writeStartElement("data");
              staxWriter.writeAttribute("name", var.getShortName());
              if (var.getUnitsString() != null)
                staxWriter.writeAttribute(CDM.UNITS, var.getUnitsString());

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

    WriterCSV(final java.io.PrintWriter writer) {
      super(writer);
    }

    public void header() {
      writer.print("time,station,latitude[unit=\"degrees_north\"],longitude[unit=\"degrees_east\"]");
      for (VariableSimpleIF var : wantVars) {
        writer.print(",");
        writer.print(var.getShortName());
        if (var.getUnitsString() != null)
          writer.print("[unit=\"" + var.getUnitsString() + "\"]");
      }
      writer.println();
    }

    public void trailer() {
      writer.flush();
    }

    Action getAction() {
      return new Action() {
        public void act(PointFeature pf, StructureData sdata) throws IOException {
          Station s = sfc.getStation(pf);

          writer.print(CalendarDateFormatter.toDateTimeString(pf.getObservationTimeAsCalendarDate()));
          writer.print(',');
          writer.print(s.getName());
          writer.print(',');
          writer.print(Format.dfrac(s.getLatitude(), 3));
          writer.print(',');
          writer.print(Format.dfrac(s.getLongitude(), 3));

          for (VariableSimpleIF var : wantVars) {
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

