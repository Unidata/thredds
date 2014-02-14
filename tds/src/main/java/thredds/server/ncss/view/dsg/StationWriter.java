/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.ncss.view.dsg;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import thredds.server.ncss.controller.NcssController;
import thredds.server.ncss.exception.FeaturesNotFoundException;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.server.ncss.view.gridaspoint.NetCDFPointDataWriter;
import thredds.util.ContentType;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
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
import ucar.nc2.units.DateType;
import ucar.nc2.util.IO;
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
public class StationWriter extends AbstractWriter {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StationWriter.class);

  private static final boolean debug = false, debugDetail = false;

  private final StationTimeSeriesFeatureCollection sfc;
  private StationWriter.Writer writer;

  public static StationWriter stationWriterFactory(FeatureDatasetPoint fd, StationTimeSeriesFeatureCollection sfc, NcssParamsBean qb, ucar.nc2.util.DiskCache2 diskCache, OutputStream out, SupportedFormat format) throws IOException, ParseException, NcssException {
    StationWriter sw = new StationWriter(fd, sfc, qb, diskCache);
    sw.writer = sw.getWriterForFormat(out, format);
    return sw;
  }

  private StationWriter(FeatureDatasetPoint fd, StationTimeSeriesFeatureCollection sfc, NcssParamsBean qb, ucar.nc2.util.DiskCache2 diskCache) throws IOException, NcssException {
    super(fd, qb, diskCache);
    this.sfc = sfc;
  }

  /*private boolean contains(StationTimeSeriesFeatureCollection sfc, String[] stnNames) {
    for (String name : stnNames) {
      if (sfc.getStation(name) != null) return true;
    }
    return false;
  }*/


  ////////////////////////////////////////////////////////////////
  // writing
  /*public File writeNetcdf(SupportedFormat format) throws IOException, ParseException, NcssException {
    //WriterNetcdf w = (WriterNetcdf) write(null, format);
	  WriterNetcdf w = (WriterNetcdf) getWriterForFormat(null, format);
    return w.netcdfResult;
  }*/

  /// ------- new stuff for decoupling things ----///


  private Writer getWriterForFormat(OutputStream out, SupportedFormat format) throws IOException, ParseException, NcssException {

    Writer w;

    switch (format) {
      case XML_STREAM:
      case XML_FILE:
        w = new WriterXML(new PrintWriter(out), format.isStream());
        break;

      case CSV_STREAM:
      case CSV_FILE:
        w = new WriterCSV(new PrintWriter(out), format.isStream());
        break;

      case NETCDF3:
        w = new WriterNetcdf(NetcdfFileWriter.Version.netcdf3, out);
        break;
      case NETCDF4:
        w = new WriterNetcdf(NetcdfFileWriter.Version.netcdf4, out);
        break;
      default:
        log.error("Unknown result type = " + format.getFormatName());
        return null;
    }

    return w;
  }


  public HttpHeaders getHttpHeaders(FeatureDataset fd, SupportedFormat format, String datasetPath) {
    return writer.getHttpHeaders(datasetPath);
  }

  public void write() throws ParseException, IOException, NcssException {

    Limit counter = new Limit();

    // spatial: all, bb, point, stns
    PointFeatureCollection pfc = null;

    List<Station> stns = writer.getStationsInSubset();

    if (stns.isEmpty())
      throw new FeaturesNotFoundException("Features not found");

    List<String> stations = new ArrayList<String>();
    for (Station st : stns)
      stations.add(st.getName());

    // LOOK should we always flatten ??
    pfc = sfc.flatten(stations, wantRange, null);

	    /*
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
	    	
	    }*/

    Action act = writer.getAction();
    writer.header();

    if (qb.getTime() != null) {
      scanForClosestTime(pfc, new DateType(qb.getTime(), null, null), null, act, counter);

    } else {
      scan(pfc, wantRange, null, act, counter);
    }

    writer.trailer();
  }


  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  
  
  /*public Writer write_old(PrintWriter writer, SupportedFormat format) throws IOException, ParseException, NcssException {
    long start = System.currentTimeMillis();
    Limit counter = new Limit();
    //counter.limit = 150;

    Writer w;
    
    switch(format){    
    	case XML:
    		w = new WriterXML(writer);
    		break;
    	case CSV:
    		w = new WriterCSV(writer);
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
    }    
    
    Action act = w.getAction();
    w.header();

    if (qb.getTime() != null) { 
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
  }*/



  /*
   * Get the list of station names that are contained within the bounding box.
   *
   * @param boundingBox lat/lon bounding box
   * @return list of station names contained within the bounding box
   * @throws IOException if read error
   */
  /*private List<String> getStationNames(LatLonRect boundingBox) throws IOException {
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
  }*/

  /*
   * Find the station closest to the specified point.
   * The metric is (lat-lat0)**2 + (cos(lat0)*(lon-lon0))**2
   *
   * @param lat latitude value
   * @param lon longitude value
   * @return name of station closest to the specified point
   * @throws IOException if read error
   */
  private Station findClosestStation(LatLonPoint pt) throws IOException {
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

  /*private boolean intersect(DateRange dr) throws IOException {
    return dr.intersects(new Date(start.getMillis() ), new Date(end.getMillis()));
  }*/

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
        if (!range.includes(obsDate)) continue;

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

      Station s = ((StationPointFeature) pf).getStation();
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

  private abstract class Writer {

    protected List<Station> wantStations = new ArrayList<Station>();

    abstract void header() throws IOException;

    abstract Action getAction();

    abstract void trailer() throws IOException;

    abstract HttpHeaders getHttpHeaders(String pathInfo);

    java.io.PrintWriter writer;
    int count = 0;

    Writer(final java.io.PrintWriter writer) {
      this.writer = writer; // LOOK what about buffering?
    }

    protected List<Station> getStationsInSubset() throws IOException {

      // verify SpatialSelection has some stations
      if (qb.hasStations()) {
        List<String> stnNames = qb.getStns();

        if (stnNames.get(0).equals("all"))
          wantStations = sfc.getStations();
        else
          wantStations = sfc.getStations(stnNames);

      } else if (qb.hasLatLonBB()) {

        if (qb.getSouth() == null || qb.getNorth() == null || qb.getEast() == null || qb.getWest() == null) {
          wantStations = sfc.getStations(); //Wants all
        } else {
          LatLonRect llrect = new LatLonRect(new LatLonPointImpl(qb.getSouth(), qb.getWest()), new LatLonPointImpl(qb.getNorth(), qb.getEast()));
          wantStations = sfc.getStations(llrect);
        }

      } else if (qb.hasLatLonPoint()) {
        Station closestStation = findClosestStation(new LatLonPointImpl(qb.getLatitude(), qb.getLongitude()));
        List<String> stnList = new ArrayList<String>();
        stnList.add(closestStation.getName());
        wantStations = sfc.getStations(stnList);

      } else { //Want all
        wantStations = sfc.getStations();
      }

      return wantStations;
    }

  }

  private class WriterNetcdf extends Writer {
    File netcdfResult;
    WriterCFStationCollection cfWriter;
    boolean headerWritten = false;
    //private List<Station> wantStations;
    private NetcdfFileWriter.Version version;
    private OutputStream out;

    WriterNetcdf(NetcdfFileWriter.Version version, OutputStream out) throws IOException {
      this(version);
      this.out = out;
    }

    WriterNetcdf(NetcdfFileWriter.Version version) throws IOException {
      super(null);
      this.version = version;
      netcdfResult = diskCache.createUniqueFile("cdmSW", ".nc");
      List<Attribute> atts = new ArrayList<Attribute>();
      atts.add(new Attribute(CDM.TITLE, "Extracted data from TDS using CDM remote subsetting"));
      cfWriter = new WriterCFStationCollection(version, netcdfResult.getAbsolutePath(), atts);

    }

    void header() {
    }

    void trailer() throws IOException {
      if (!headerWritten)
        throw new IllegalStateException("no data was written");

      cfWriter.finish();
      //Copy the file in to the OutputStream
      try {
        IO.copyFileB(netcdfResult, out, 60000);
      } catch (IOException ioe) {
        log.error("Error copying result to the output stream", ioe);
      }

    }

    HttpHeaders getHttpHeaders(String pathInfo) {

      HttpHeaders httpHeaders = new HttpHeaders();
      //String pathInfo = fd.getTitle();
      String fileName = NetCDFPointDataWriter.getFileNameForResponse(version, pathInfo);
      String url = NcssRequestUtils.getTdsContext().getContextPath() + NcssController.getServletCachePath() + "/" + fileName;
      if (version == NetcdfFileWriter.Version.netcdf3)
        httpHeaders.set(ContentType.HEADER, ContentType.netcdf.getContentHeader());

      if (version == NetcdfFileWriter.Version.netcdf4)
        httpHeaders.set(ContentType.HEADER, ContentType.netcdf4.getContentHeader());

      httpHeaders.set("Content-Location", url);
      httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

      return httpHeaders;
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

          if (version == NetcdfFileWriter.Version.netcdf3)
            cfWriter.writeRecord(sfc.getStation(pf), pf, sdata);

          if (version == NetcdfFileWriter.Version.netcdf4)
            cfWriter.writeStructure(sfc.getStation(pf), pf, sdata);

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

    void header() throws IOException {
      // PointStream.writeMagic(out, PointStream.MessageType.Start);  // LOOK - not ncstream protocol
    }

    void trailer() throws IOException {
      PointStream.writeMagic(out, PointStream.MessageType.End);
      out.flush();
    }

    HttpHeaders getHttpHeaders(String pathInfo) {
      return new HttpHeaders();
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

    void header() {
    }

    void trailer() {
      writer.flush();
    }

    public HttpHeaders getHttpHeaders(String pathInfo) {
      return new HttpHeaders();
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

    private XMLStreamWriter staxWriter;
    private boolean isStream;

    WriterXML(final java.io.PrintWriter writer, boolean isStream) {
      super(writer);
      this.isStream = isStream;
      XMLOutputFactory f = XMLOutputFactory.newInstance();
      try {
        staxWriter = f.createXMLStreamWriter(writer);
      } catch (XMLStreamException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    void header() {
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

    void trailer() {
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

    HttpHeaders getHttpHeaders(String pathInfo) {
      HttpHeaders httpHeaders = new HttpHeaders();

      if (!isStream) {
        httpHeaders.set("Content-Location", pathInfo);
        httpHeaders.set("Content-Disposition", "attachment; filename=\"" + NcssRequestUtils.nameFromPathInfo(pathInfo) + ".xml\"");
      }

      httpHeaders.set(ContentType.HEADER, ContentType.xml.getContentHeader());
      // httpHeaders.setContentType(MediaType.APPLICATION_XML);
      return httpHeaders;
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

    private boolean isStream;

    WriterCSV(final java.io.PrintWriter writer, boolean isStream) {
      super(writer);
      this.isStream = isStream;
    }

    void header() {
      writer.print("time,station,latitude[unit=\"degrees_north\"],longitude[unit=\"degrees_east\"]");
      for (VariableSimpleIF var : wantVars) {
        writer.print(",");
        writer.print(var.getShortName());
        if (var.getUnitsString() != null)
          writer.print("[unit=\"" + var.getUnitsString() + "\"]");
      }
      writer.println();
    }

    void trailer() {
      writer.flush();
    }

    HttpHeaders getHttpHeaders(String pathInfo) {
      HttpHeaders httpHeaders = new HttpHeaders();

      if (!isStream) {
        httpHeaders.set("Content-Location", pathInfo);
        httpHeaders.set("Content-Disposition", "attachment; filename=\"" + NcssRequestUtils.nameFromPathInfo(pathInfo) + ".csv\"");
        httpHeaders.add(ContentType.HEADER, ContentType.csv.getContentHeader());
      } else {
        httpHeaders.add(ContentType.HEADER, ContentType.text.getContentHeader());
      }

      return httpHeaders;
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

//  static public void main(String args[]) throws IOException {
//    //getFiles("R:/testdata2/station/ldm/metar/");
//    // StationObsCollection soc = new StationObsCollection("C:/data/metars/", false);
//  }

}

