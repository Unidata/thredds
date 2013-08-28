/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.ncSubset.view;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.params.PointDataRequestParamsBean;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ft.point.remote.PointStream;
import ucar.nc2.ft.point.remote.PointStreamProto;
import ucar.nc2.ft.point.writer.WriterCFStationCollection;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.Format;

/**
 * NCSS subsetting for station data.
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
  private final PointDataRequestParamsBean qb;
  
  private final CalendarDate start, end;

  private List<VariableSimpleIF> wantVars;
  //private DateRange wantRange;
  private CalendarDateRange wantRange;
  private ucar.nc2.util.DiskCache2 diskCache;

  public StationWriter(FeatureDatasetPoint fd, StationTimeSeriesFeatureCollection sfc, PointDataRequestParamsBean qb, ucar.nc2.util.DiskCache2 diskCache) throws IOException 
  {
    this.fd = fd;
    this.sfc = sfc;
    this.qb = qb;
    this.diskCache = diskCache;

    start = fd.getCalendarDateStart();    
    end = fd.getCalendarDateEnd();
  }

  private boolean contains(StationTimeSeriesFeatureCollection sfc, String[] stnNames) {
    for (String name : stnNames) {
      if (sfc.getStation(name) != null) return true;
    }
    return false;
  }


  ////////////////////////////////////////////////////////////////
  // writing
  public File writeNetcdf(SupportedFormat format) throws IOException, ParseException, NcssException {
    WriterNetcdf w = (WriterNetcdf) write(null, format);
    return w.netcdfResult;
  }

  public Writer write(HttpServletResponse res, SupportedFormat format) throws IOException, ParseException, NcssException {
    long start = System.currentTimeMillis();
    Limit counter = new Limit();
    //counter.limit = 150;

    Writer w;
    
    switch(format){    
    	case XML:
    		w = new WriterXML(res.getWriter());
    		break;
    	case CSV:
    		w = new WriterCSV(res.getWriter());
    		break;
    	case NETCDF3:
    		w = new WriterNetcdf(NetcdfFileWriter.Version.netcdf3);
    		break;
    	case NETCDF4:
    		w = new WriterNetcdf(NetcdfFileWriter.Version.netcdf4);
    		break;
	default:
		log.error("Unknown result type = " + format.getFormatName());
        return null;    	    
    }
       
    // for closest time, set wantRange to the time LOOK - do we need +- increment ??
    if (qb.getTime() != null ) { //Means we want just one single time
      CalendarDate  startR = CalendarDate.parseISOformat(null, qb.getTime() );      
      startR = startR.subtract(CalendarPeriod.Hour);
      CalendarDate endR = CalendarDate.parseISOformat(null, qb.getTime() );
      endR = endR.add(CalendarPeriod.Hour);
      //wantRange = new DateRange( new Date(startR.getMillis()), new Date(endR.getMillis()));
      wantRange = CalendarDateRange.of( new Date(startR.getMillis()), new Date(endR.getMillis()));
      
    }else{
    	//Time range: need to compute the time range from the params
    	if(qb.getTemporal() != null && qb.getTemporal().equals("all")){
    		//Full time range -->CHECK: wantRange=null means all range??? 
    		
    	}else{
    		if(qb.getTime_start() != null && qb.getTime_end() != null  ){
    			CalendarDate  startR = CalendarDate.parseISOformat(null, qb.getTime_start() );
    			CalendarDate  endR = CalendarDate.parseISOformat(null, qb.getTime_end() );
    			//wantRange = new DateRange( new Date(startR.getMillis()), new Date(endR.getMillis()));;
    			wantRange = CalendarDateRange.of( new Date(startR.getMillis()), new Date(endR.getMillis()));;
    		}else if(qb.getTime_start() != null && qb.getTime_duration() != null) {
    			CalendarDate  startR = CalendarDate.parseISOformat(null, qb.getTime_start() );
    			TimeDuration td = TimeDuration.parseW3CDuration(qb.getTime_duration());
    			//wantRange = new DateRange( new Date(startR.getMillis()), td);
    			wantRange = new CalendarDateRange(startR, (long)td.getValueInSeconds());
    		}else if(qb.getTime_end() != null && qb.getTime_duration() != null){
    			CalendarDate  endR = CalendarDate.parseISOformat(null, qb.getTime_end() );
    			TimeDuration td = TimeDuration.parseW3CDuration(qb.getTime_duration());
    			//wantRange = new DateRange(null, new DateType( qb.getTime_end(), null, null), td, null);
    			wantRange = new CalendarDateRange(endR, (long)td.getValueInSeconds() * (-1));
    		}
    	}
    	
    }

    // spatial: all, bb, point, stns
    PointFeatureCollection pfc = null;
    //Subsetting type
    String stn = qb.getSubset();
    if(stn != null){
    	if( stn.equals("all") ){
    		pfc = sfc.flatten(null,  wantRange);
    	
    	}else if( stn.equals("bb") ){    		
    		LatLonRect llrect = new LatLonRect(new LatLonPointImpl(qb.getSouth(), qb.getWest()), new LatLonPointImpl(qb.getNorth(), qb.getEast())  );
    		pfc = sfc.flatten(llrect, wantRange);
    		
    	}else if(stn.equals("stn")){
    		// stns
            List<String> wantStns = qb.getStns();
            pfc = sfc.flatten(wantStns, wantRange, null);
    	}
    }else{    
    	//stn=null --> means request is point, want closest to lat,lon
    	// point
    	Station closestStation = findClosestStation(new LatLonPointImpl( qb.getLatitude(), qb.getLongitude() ));
    	List<String> stnList = new ArrayList<String>();
    	stnList.add(closestStation.getName());
    	//useFc = sfc.subset(stn);
    	pfc = sfc.flatten(stnList, wantRange, null);    
    }
    
    
    //Wanted vars
    // restrict to these variables
    List<? extends VariableSimpleIF> dataVars = fd.getDataVariables();
      
    Map<String, VariableSimpleIF> dataVarsMap = new HashMap<String, VariableSimpleIF>(); 
    for(VariableSimpleIF v : dataVars ){
    	dataVarsMap.put(v.getShortName(), v);
    }
    
    List<String> varNames = qb.getVar();
    
    //if ((varNames == null) || (varNames.size() == 0) || varNames.equals("all")) {
    if (varNames.equals("all")) {
      wantVars = new ArrayList<VariableSimpleIF>(dataVars);
    } else {
    	List<String> allVars = new ArrayList<String>( dataVarsMap.keySet());
    	wantVars = new ArrayList<VariableSimpleIF>();
    	for(String v : varNames){
    		if( allVars.contains(v) ){
    			VariableSimpleIF var = dataVarsMap.get(v);
    			wantVars.add(var);
    		}else{
    			throw new VariableNotContainedInDatasetException("Variable: "+v+" is not contained in the requested dataset");
    		}
    	}
    	    	
//      for (VariableSimpleIF v : dataVars) {
//        if (varNames.contains(v.getShortName())) // LOOK N**2
//          wantVars.add(v);        
//      }
    }    
    
 
		
    

    Action act = w.getAction();
    w.header();

    //if (qb.getTemporalSelection() == CdmrfQueryBean.TemporalSelection.point) {
    //  scanForClosestTime(pfc, qb.getTimePoint(), null, act, counter);
    if (qb.getTime() != null) { //?? again
          scanForClosestTime(pfc, new DateType(qb.getTime(), null, null) , null, act, counter);    

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
        double mps = 1000 * counter.matches / writeTime;
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

    List<Station> result = new ArrayList<Station>(stnNames.length);
    for (String s : stnNames) {
      Station stn = stationMap.get(s);
      if (stn != null)
        result.add(stn);
    }

    return result;
  }

  private void makeStationMap() throws IOException {
    if (null == stationMap) {
      stationMap = new HashMap<String, Station>();
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
    ArrayList<String> result = new ArrayList<String>();
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
    return dr.intersects(new Date(start.getMillis() ), new Date(end.getMillis()));
  }

  ////////////////////////////////////////////////////////
  // scanning flattened collection

  // scan PointFeatureCollection, records that pass the predicate match are acted on, within limits
  private void scan(PointFeatureCollection collection, CalendarDateRange range, Predicate p, Action a, Limit limit) throws IOException {

    collection.resetIteration();
    while (collection.hasNext()) {
      PointFeature pf = collection.next();

      if (range != null) {
        //Date obsDate = pf.getObservationTimeAsDate(); // LOOK: needed?
    	CalendarDate obsDate = pf.getObservationTimeAsCalendarDate();  
    	//if (!range.contains(obsDate)) continue;
        if (!range.includes( obsDate )) continue;
        
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

    HashMap<String, StationDataTracker> map = new HashMap<String, StationDataTracker>();
    //long wantTime = time.getDate().getTime();
    long wantTime = time.getCalendarDate().getMillis();

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
      //long obsTime = pf.getObservationTimeAsDate().getTime();
      long obsTime = pf.getObservationTimeAsCalendarDate().getMillis();
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

    for (String name : map.keySet()) {
      StationDataTracker track = map.get(name);
      a.act(track.sobs, track.sobs.getData());
      limit.matches++;

      limit.count++;
      if (limit.count > limit.limit) break;
    }

  }

  private class StationDataTracker {
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



  private interface Predicate {
    boolean match(StructureData sdata);
  }

  private interface Action {
    void act(PointFeature pf, StructureData sdata) throws IOException;
  }

  private class Limit {
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
    private List<Station> wantStations;

    WriterNetcdf(NetcdfFileWriter.Version version) throws IOException {
      super(null);

      netcdfResult = diskCache.createUniqueFile("cdmSW", ".nc");
      List<Attribute> atts = new ArrayList<Attribute>();
      atts.add(new Attribute(CDM.TITLE, "Extracted data from TDS using CDM remote subsetting"));
      cfWriter = new WriterCFStationCollection(version, netcdfResult.getAbsolutePath(), atts);

      // verify SpatialSelection has some stations
      if (qb.getSubset().equals("bb") ) {
  		LatLonRect llrect = new LatLonRect(new LatLonPointImpl(qb.getSouth(), qb.getWest()), new LatLonPointImpl(qb.getNorth(), qb.getEast())  );
  		wantStations = sfc.getStations( llrect );
        //wantStations = sfc.getStations(qb.getLatLonRect());

      } else if (qb.getSubset().equals("stns")) {
        List<String> stnNames = qb.getStns();
        wantStations = sfc.getStations(stnNames);

      } else {
        wantStations = sfc.getStations();
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
              cfWriter.writeHeader(wantStations, wantVars, pf.getTimeUnit(), null);
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
              PointStreamProto.PointFeatureCollection proto = PointStream.encodePointFeatureCollection(fd.getLocation(), pf);
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

