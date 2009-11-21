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
package ucar.nc2.iosp.misc;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.CancelTask;
import ucar.ma2.*;

import java.io.IOException;
import java.io.EOFException;
import java.util.*;
import java.nio.ByteBuffer;

/**
 * NMC Office Note 29
 *
 * @author caron
 * @since Feb 22, 2008
 */
public class NmcObsLegacy extends AbstractIOServiceProvider {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NmcObsLegacy.class);

  private RandomAccessFile raf;
  private NetcdfFile ncfile;
  //private Structure reportVar;

  private List<Station> stations = new ArrayList<Station>();
  private List<Report> reports  = new ArrayList<Report>();
  //private Map<String, List<Report>> map = new HashMap<String, List<Report>>();
  //private List<String> stations;

  // private int nobs = 0, nstations = 0;
  private Calendar cal = null;
  private DateFormatter dateFormatter = new DateFormatter();
  private Date refDate; // from the header
  private String refString; // debug

  private List<StructureCode> catStructures = new ArrayList<StructureCode>(10);

  private boolean showObs = false, showSkip = false, showOverflow = false, showData = false,
      showHeader = false, showTime = false;
  private boolean readData = false, summarizeData = false, showTimes = false;
  private boolean checkType = false, checkSort = false, checkPositions = false;

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    if (raf.length() < 60) return false;
    byte[] h = raf.readBytes(60);

    // 32 - 56 are X's
    for (int i = 32; i < 56; i++)
      if (h[i] != (byte) 'X') return false;

    try {
      short hour = Short.parseShort(new String(h, 0, 2));
      short minute = Short.parseShort(new String(h, 2, 2));
      short year = Short.parseShort(new String(h, 4, 2));
      short month = Short.parseShort(new String(h, 6, 2));
      short day = Short.parseShort(new String(h, 8, 2));

      if ((hour < 0) || (hour > 24)) return false;
      if ((minute < 0) || (minute > 60)) return false;
      if ((year < 0) || (year > 100)) return false;
      if ((month < 0) || (month > 12)) return false;
      if ((day < 0) || (day > 31)) return false;

    } catch (Exception e) {
      return false;
    }

    return true;
  }

    public String getFileTypeId() {
      return "NMCon29";
    }

    public String getFileTypeDescription() {
      return "NMC Office Note 29";
    }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;

    init();

    ncfile.addAttribute(null, new Attribute("history", "direct read of NMC ON29 by CDM"));
    ncfile.addAttribute(null, new Attribute("Conventions", "Unidata"));
    ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.STATION_PROFILE.toString()));

    try {
      ncfile.addDimension(null, new Dimension("station", stations.size()));
      Structure station = makeStationStructure();
      ncfile.addVariable(null, station);

      ncfile.addDimension(null, new Dimension("report", reports.size()));
      Structure reportIndexVar = makeReportIndexStructure();
      ncfile.addVariable(null, reportIndexVar);

      Structure reportVar = makeReportStructure();
      ncfile.addVariable(null, reportVar);

    } catch (InvalidRangeException e) {
      log.error("open ON29 File", e);
      throw new IllegalStateException(e.getMessage());
    }
  }

  public void close() throws IOException {
    raf.close();
  }

  public Array readData(Variable v, Section section) throws IOException, InvalidRangeException {
    if (v.getName().equals("station"))
      return readStation(v, section);

    else if (v.getName().equals("report"))
      return readReport(v, section);

    else if (v.getName().equals("reportIndex"))
      return readReportIndex(v, section);

    throw new IllegalArgumentException("Unknown variable name= "+v.getName());
  }

  ///////////////////////////////////////////////////////////////////////////////////

  private Structure makeStationStructure() throws IOException, InvalidRangeException {
    Structure station = new Structure(ncfile, null, null, "station");
    station.setDimensions("station");
    station.addAttribute(new Attribute("long_name", "unique stations within this file"));

    int pos = 0;
    Variable v = station.addMemberVariable(new Variable(ncfile, null, station, "stationName", DataType.CHAR, ""));
    v.setDimensionsAnonymous(new int[]{6});
    v.addAttribute(new Attribute("long_name", "name of station"));
    v.addAttribute(new Attribute("standard_name", "station_name"));
    v.setSPobject(new Vinfo(pos));
    pos += 6;

    v = station.addMemberVariable(new Variable(ncfile, null, station, "lat", DataType.FLOAT, ""));
    v.addAttribute(new Attribute("units", "degrees_north"));
    v.addAttribute(new Attribute("long_name", "geographic latitude"));
    v.addAttribute(new Attribute("accuracy", "degree/100"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    v.setSPobject(new Vinfo(pos));
    pos += 4;

    v = station.addMemberVariable(new Variable(ncfile, null, station, "lon", DataType.FLOAT, ""));
    v.addAttribute(new Attribute("units", "degrees_east"));
    v.addAttribute(new Attribute("long_name", "geographic longitude"));
    v.addAttribute(new Attribute("accuracy", "degree/100"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    v.setSPobject(new Vinfo(pos));
    pos += 4;

    v = station.addMemberVariable(new Variable(ncfile, null, station, "elev", DataType.FLOAT, ""));
    v.addAttribute(new Attribute("units", "meters"));
    v.addAttribute(new Attribute("long_name", "station elevation above MSL"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
    v.setSPobject(new Vinfo(pos));
    pos += 4;

    v = station.addMemberVariable(new Variable(ncfile, null, station, "nrecords", DataType.INT, ""));
    v.addAttribute(new Attribute("long_name", "number of records"));
    v.addAttribute(new Attribute("standard_name", "npts"));
    v.setSPobject(new Vinfo(pos));
    pos += 4;

    return station;
  }

  private Structure makeReportIndexStructure() throws InvalidRangeException, IOException {
    Structure reportIndex = new Structure(ncfile, null, null, "reportIndex");
    reportIndex.setDimensions("report");

    reportIndex.addAttribute(new Attribute("long_name", "index on report - in memory"));
    int pos = 0;

    Variable v = reportIndex.addMemberVariable(new Variable(ncfile, null, reportIndex, "stationName", DataType.CHAR, ""));
    v.setDimensionsAnonymous(new int[]{6});
    v.addAttribute(new Attribute("long_name", "name of station"));
    v.addAttribute(new Attribute("standard_name", "station_name"));
    v.setSPobject(new Vinfo(pos));
    pos += 6;

    v = reportIndex.addMemberVariable(new Variable(ncfile, null, reportIndex, "time", DataType.INT, ""));
    v.addAttribute(new Attribute("units", "secs since 1970-01-01 00:00"));
    v.addAttribute(new Attribute("long_name", "observation time"));
    v.setSPobject(new Vinfo(pos));                 
    pos += 4;

    return reportIndex;
  }


  private Structure makeReportStructure() throws InvalidRangeException, IOException {
    Structure report = new Structure(ncfile, null, null, "report");
    report.setDimensions("report");
    report.addAttribute(new Attribute("long_name", "ON29 observation report"));
    int pos = 0;

    Variable v = report.addMemberVariable(new Variable(ncfile, null, report, "time", DataType.INT, ""));
    v.addAttribute(new Attribute("units", "secs since 1970-01-01 00:00"));
    v.addAttribute(new Attribute("long_name", "observation time"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    v.setSPobject(new Vinfo(pos));
    pos += 4;

    v = report.addMemberVariable(new Variable(ncfile, null, report, "timeISO", DataType.CHAR, ""));
    v.setDimensionsAnonymous(new int[]{20});
    v.addAttribute(new Attribute("long_name", "ISO formatted date/time"));
    v.setSPobject(new Vinfo(pos));
    pos += 20;

    v = report.addMemberVariable(new Variable(ncfile, null, report, "reportType", DataType.SHORT, ""));
    v.addAttribute(new Attribute("long_name", "report type from Table R.1"));
    v.setSPobject(new Vinfo(pos));
    pos += 2;

    // only for ON29
    v = report.addMemberVariable(new Variable(ncfile, null, report, "instType", DataType.SHORT, ""));
    v.addAttribute(new Attribute("long_name", "instrument type from Table R.2"));
    v.setSPobject(new Vinfo(pos));
    pos += 2;

    v = report.addMemberVariable(new Variable(ncfile, null, report, "reserved", DataType.CHAR, ""));
    v.setDimensionsAnonymous(new int[]{7});
    v.addAttribute(new Attribute("long_name", "reserved characters"));
    v.setSPobject(new Vinfo(pos));
    pos += 7;

    List<Record> records = firstReport.readData(); // for the moment, we will use the first report as the exemplar
    pos = makeInnerSequence(report, records, 1, pos);
    pos = makeInnerSequence(report, records, 2, pos);
    pos = makeInnerSequence(report, records, 3, pos);
    pos = makeInnerSequence(report, records, 4, pos);
    pos = makeInnerSequence(report, records, 5, pos);
    pos = makeInnerSequence(report, records, 7, pos);
    pos = makeInnerSequence(report, records, 8, pos);
    pos = makeInnerSequence(report, records, 51, pos);
    pos = makeInnerSequence(report, records, 52, pos);
    report.calcElementSize(); // recalc since we added new members

    return report;
  }

  private int makeInnerSequence( Structure reportVar, List<Record> records, int code, int obs_pos) throws InvalidRangeException {

    for (Record record : records) {
      if (record.code == code) {
        Entry first = record.entries[0];
        Structure s = first.makeStructure(reportVar);
        s.setSPobject(new Vinfo(obs_pos));
        obs_pos += 4;
        reportVar.addMemberVariable(s);
        catStructures.add(new StructureCode(s, code));
        break;
      }
    }
    return obs_pos;
  }

  private class Vinfo {
    int offset;

    Vinfo(int offset) {
      this.offset = offset;
    }
  }

  private class StructureCode {
    Structure s;
    int code;

    StructureCode(Structure s, int code) {
      this.s = s;
      this.code = code;
    }
  }

  /////////////////////////////////////////////////////////////

  private Array readStation(Variable v, Section section) throws IOException, InvalidRangeException {
    Structure s = (Structure) v;
    StructureMembers members = s.makeStructureMembers();
    for (Variable v2 : s.getVariables()) {
      Vinfo vinfo = (Vinfo) v2.getSPobject();
      StructureMembers.Member m = members.findMember(v2.getShortName());
      if (vinfo != null) {
        m.setDataParam(vinfo.offset);
        //m.setVariableInfo( vinfo.size);
      }
    }

    int size = (int) section.computeSize();
    ArrayStructureBB abb = new ArrayStructureBB(members, new int[]{size});
    ByteBuffer bb = abb.getByteBuffer();

    Range r = section.getRange(0);
    for (int i = r.first(); i <= r.last(); i += r.stride()) {
      Station station = stations.get(i);
      bb.put(station.r.stationId.getBytes());
      bb.putFloat(station.r.lat);
      bb.putFloat(station.r.lon);
      bb.putFloat(station.r.elevMeters);
      bb.putInt(station.nreports);
    }

    return abb;
  }

   public Array readReportIndex(Variable v, Section section) throws IOException, InvalidRangeException {
    Structure s = (Structure) v;
    StructureMembers members = s.makeStructureMembers();
    for (Variable v2 : s.getVariables()) {
      Vinfo vinfo = (Vinfo) v2.getSPobject();
      StructureMembers.Member m = members.findMember(v2.getShortName());
      m.setDataParam(vinfo.offset);
    }

    int size = (int) section.computeSize();
    ArrayStructureBB abb = new ArrayStructureBB(members, new int[]{size});
    ByteBuffer bb = abb.getByteBuffer();

    Range r = section.getRange(0);
    for (int i = r.first(); i <= r.last(); i += r.stride()) {
      Report report = reports.get(i);
      report.loadIndexData(bb);
    }

    return abb;
  }

  public Array readReport(Variable v, Section section) throws IOException, InvalidRangeException {
    Structure s = (Structure) v;
    StructureMembers members = s.makeStructureMembers();
    for (Variable v2 : s.getVariables()) {
      Vinfo vinfo = (Vinfo) v2.getSPobject();
      StructureMembers.Member m = members.findMember(v2.getShortName());
      m.setDataParam(vinfo.offset);
    }

    int size = (int) section.computeSize();
    ArrayStructureBB abb = new ArrayStructureBB(members, new int[]{size});
    ByteBuffer bb = abb.getByteBuffer();

    Range r = section.getRange(0);
    for (int i = r.first(); i <= r.last(); i += r.stride()) {
      Report report = reports.get(i);
      report.loadStructureData(abb, bb);
    }

    return abb;
  }

  /* private class ReportIterator implements StructureDataIterator {
    List<Report> reports;
    Iterator<Report> iter;
    StructureMembers members;

    ReportIterator(List<Report> reports) {
      this.reports = reports;
      iter = reports.iterator();

      members = reportVar.makeStructureMembers();
      for (Variable v2 : reportVar.getVariables()) {
        Vinfo vinfo = (Vinfo) v2.getSPobject();
        StructureMembers.Member m = members.findMember(v2.getShortName());
        m.setDataParam(vinfo.offset);
      }
    }

    public boolean hasNext() throws IOException {
      return iter.hasNext();
    }

    public StructureData next() throws IOException {
      Report r = iter.next();

      // LOOK should optimize - read 10 at a time or something ???
      ArrayStructureBB abb = new ArrayStructureBB(members, new int[]{1});
      ByteBuffer bb = abb.getByteBuffer();
      bb.position(0);
      r.loadStructureData(abb, bb);
      return abb.getStructureData(0);
    }

    public void setBufferSize(int bytes) {
    }

    public StructureDataIterator reset() {
      iter = reports.iterator();
      return this;
    }
  } */



  private Report firstReport = null;

  private void init() throws IOException {
    int badPos = 0;
    int badType = 0;
    short firstType = -1;

    raf.seek(0);
    readHeader(raf);

    // read through all the reports, construct unique stations
    Map<String,Station> map = new HashMap<String,Station>();
    while (true) {
      Report report = new Report();
      if (!report.readId(raf)) break;

      if (firstReport == null) {
        firstReport = report;
        firstType = firstReport.reportType;
      }

      if (checkType && (report.reportType != firstType)) {
        System.out.println(report.stationId + " type: " + report.reportType + " not " + firstType);
        badType++;
      }

      Station stn = map.get(report.stationId);
      if (stn == null) {
        stn = new Station(report);
        map.put(report.stationId, stn);
        stations.add( stn);

      } else {
        stn.nreports++;

        if (checkPositions) {
          Report first = reports.get(0);
          if (first.lat != report.lat) {
            System.out.println(report.stationId + " lat: " + first.lat + " !=" + report.lat);
            badPos++;
          }
          if (first.lon != report.lon)
            System.out.println(report.stationId + " lon: " + first.lon + " !=" + report.lon);
          if (first.elevMeters != report.elevMeters)
            System.out.println(report.stationId + " elev: " + first.elevMeters + " !=" + report.elevMeters);
        }
      }

      reports.add(report);
    }

    Collections.sort(stations);


    if (checkPositions)
      System.out.println("\nnon matching lats= " + badPos);
    if (checkType)
      System.out.println("\nnon matching reportTypes= " + badType);

    //System.out.println(firstReport);
    //firstReport.show();
    //firstReport.readData();

    /* Set<String> keys = map.keySet();
    if (showTimes || readData || checkSort) {
      int unsorted = 0;

      for (String key : keys) {
        List<Report> reports = map.get(key);
        if (showTimes) System.out.print("Station " + key + ": ");
        if (summarizeData) System.out.println("Station " + key + " :");
        Report last = null;
        for (Report r : reports) {
          if ((last != null) && last.date.after(r.date)) {
            System.out.println("***NOT ORDERED " + key +
                " last=" + dateFormatter.toDateTimeStringISO(last.date) + "(" + last.filePos + ")" +
                " next =" + dateFormatter.toDateTimeStringISO(r.date) + "(" + r.filePos + ")");
            unsorted++;
          }
          last = r;

          if (showTimes) System.out.print(dateFormatter.toDateTimeStringISO(r.date) + " ");
          if (readData || summarizeData) {
            List<Record> cats = r.readData();
            if (summarizeData) {
              System.out.print("  " + r.obsTime + ": (");
              for (Record cat : cats)
                System.out.print(cat.code + "/" + cat.nlevels + " ");
              System.out.println(")");
            }
          }

        }
        if (showTimes) System.out.println();
      }
      if (checkSort)
        System.out.println("\nunsorted= " + unsorted);

    }
    nstations = keys.size(); */

    // System.out.println("\nnreports= " + reports.size() + " nstations= " + stations.size());
  }

  private class Station implements Comparable<Station> {
    String name;
    Report r;
    int nreports;
    Station(Report r) {
      this.name = r.stationId;
      this.r = r;
      this.nreports=1;
    }

    public int compareTo(Station o) {
      return name.compareTo(o.name);
    }
  }

  private class Report {
    float lat, lon, elevMeters;
    String stationId;
    byte[] reserved = new byte[7];
    short reportType, instType, obsTime;
    int reportLen;
    long filePos;
    Date date;
    String rString; // refString, for debugging

    boolean readId(RandomAccessFile raf) throws IOException {

      filePos = raf.getFilePointer();
      byte[] reportId = raf.readBytes(40);
      String latS = new String(reportId, 0, 5);

      if (latS.equals("END R")) {
        raf.skipBytes(-40);
        endRecord(raf);

        filePos = raf.getFilePointer();
        reportId = raf.readBytes(40);
        latS = new String(reportId, 0, 5);
      }
      if (latS.equals("ENDOF")) {
        raf.skipBytes(-40);
        if (!endFile(raf)) return false;

        filePos = raf.getFilePointer();
        reportId = raf.readBytes(40);
        latS = new String(reportId, 0, 5);
      }

      //System.out.println("ReportId start at " + start);
      try {
        lat = (float) (.01 * Float.parseFloat(latS));
        lon = (float) (360.0 - .01 * Float.parseFloat(new String(reportId, 5, 5)));

        stationId = new String(reportId, 10, 6);
        obsTime = Short.parseShort(new String(reportId, 16, 4));
        System.arraycopy(reportId, 20, reserved, 0, 7);
        reportType = Short.parseShort(new String(reportId, 27, 3));
        elevMeters = Float.parseFloat(new String(reportId, 30, 5));
        instType = Short.parseShort(new String(reportId, 35, 2));
        reportLen = 10 * Integer.parseInt(new String(reportId, 37, 3));

        cal.setTime(refDate);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (obsTime / 100 > hour + 4) // if greater than 4 hours from reference time
          cal.add(Calendar.DAY_OF_MONTH, -1); // subtract a day LOOK
        cal.set(Calendar.HOUR_OF_DAY, obsTime / 100);
        cal.set(Calendar.MINUTE, 6 * (obsTime % 100));
        date = cal.getTime();
        rString = refString; // temp debugg

        if (showObs) System.out.println(this);
        else if (showTime) System.out.print("  time=" + obsTime + " date= " + dateFormatter.toDateTimeString(date));

        //nobs++;
        raf.skipBytes(reportLen - 40);
        return reportLen < 30000;

      } catch (Exception e) {
        System.out.println("BAD reportId=" + new String(reportId));
        System.out.println("ReportId start at " + filePos);
        e.printStackTrace();
        System.exit(1);
        return false;
      }
    }

    public String toString() {
      return "Report " + " stationId=" + stationId + " lat=" + lat + " lon=" + lon +
          " obsTime=" + obsTime + " date= " + dateFormatter.toDateTimeStringISO(date) +
          " reportType=" + reportType + " elevMeters=" + elevMeters + " instType=" + instType + " reserved=" + new String(reserved) +
          " start=" + filePos + " reportLen=" + reportLen;
    }

    // heres where the data for this Report is read into memory
    List<Record> readData() throws IOException {
      List<Record> records = new ArrayList<Record>();

      raf.seek(filePos + 40);
      byte[] b = raf.readBytes(reportLen - 40);
      if (showData) System.out.println("\n" + new String(b));
      if (showData) System.out.println(this);

      int offset = 0;
      while (true) {
        Record record = new Record();
        offset = record.read(b, offset);
        records.add(record);
        if (record.next >= reportLen / 10) break;
      }

      return records;
    }

    void show(RandomAccessFile raf) throws IOException {
      raf.seek(filePos);
      byte[] b = raf.readBytes(40);
      System.out.println(new String(b));
    }

    void loadIndexData(ByteBuffer bb) throws IOException {
      bb.put(stationId.getBytes());
      bb.putInt((int) (date.getTime() / 1000));
    }

    void loadStructureData(ArrayStructureBB abb, ByteBuffer bb) throws IOException {
      bb.putInt((int) (date.getTime() / 1000));
      bb.put(dateFormatter.toDateTimeStringISO(date).getBytes());
      bb.putShort(reportType);
      bb.putShort(instType);
      bb.put(reserved);

      List<Record> records = readData();
      for (StructureCode sc : catStructures)
        loadInnerSequence(abb, bb, records, sc.s, sc.code);
    }

    private void loadInnerSequence(ArrayStructureBB abb, ByteBuffer bb, List<Record> records, Structure useStructure, int code) {

      for (Record record : records) {
        if (record.code == code) {
          CatIterator iter = new CatIterator(record.entries, useStructure);
          ArraySequence seq = new ArraySequence(iter.members, iter, record.entries.length);
          int index = abb.addObjectToHeap(seq);
          bb.putInt(index);
          return;
        }
      }

      // need an empty one
      CatIterator iter = new CatIterator(new Entry[0], useStructure);
      ArraySequence seq = new ArraySequence(iter.members, iter, -1); // ??
      int index = abb.addObjectToHeap(seq);
      bb.putInt(index);
    }

    private class CatIterator implements StructureDataIterator {
      Entry[] entries;
      int count = 0;
      StructureMembers members;

      CatIterator(Entry[] entries, Structure useStructure) {
        this.entries = entries;

        members = useStructure.makeStructureMembers();
        for (Variable v2 : useStructure.getVariables()) {
          Vinfo vinfo = (Vinfo) v2.getSPobject();
          StructureMembers.Member m = members.findMember(v2.getShortName());
          m.setDataParam(vinfo.offset);
        }
      }

      @Override
      public boolean hasNext() throws IOException {
        return count < entries.length;
      }

      @Override
      public StructureData next() throws IOException {
        Entry entry = entries[count++];

        // LOOK should read 10 at a time or something ???
        ArrayStructureBB abb = new ArrayStructureBB(members, new int[]{1});
        ByteBuffer bb = abb.getByteBuffer();
        bb.position(0);
        entry.loadStructureData(bb);
        return abb.getStructureData(0);
      }

      @Override
      public void setBufferSize(int bytes) {
      }

      @Override
      public StructureDataIterator reset() {
        count = 0;
        return this;
      }

      @Override
      public int getCurrentRecno() {
        return count - 1;
      }
    }

  }

  // a record has a variable number of entries, which are all of one "category" type
  private class Record {
    int code, next, nlevels, nbytes;
    Entry[] entries;

    int read(byte[] b, int offset) throws IOException {

      code = Integer.parseInt(new String(b, offset, 2));
      next = Integer.parseInt(new String(b, offset + 2, 3));
      nlevels = Integer.parseInt(new String(b, offset + 5, 2));
      nbytes = readIntWithOverflow(b, offset + 7, 3);
      if (showData) System.out.println("\n" + this);

      offset += 10;

      if (code == 1) {
        if (showData) System.out.println(catNames[1] + ":");
        entries = new Cat01[nlevels];
        for (int i = 0; i < nlevels; i++) {
          entries[i] = new Cat01(b, offset, i);
          if (showData) System.out.println(" " + i + ": " + entries[i]);
          offset += 22;
        }
      } else if (code == 2) {
        if (showData) System.out.println(catNames[2] + ":");
        entries = new Cat02[nlevels];
        for (int i = 0; i < nlevels; i++) {
          entries[i] = new Cat02(b, offset);
          if (showData) System.out.println(" " + i + ": " + entries[i]);
          offset += 15;
        }
      } else if (code == 3) {
        if (showData) System.out.println(catNames[3] + ":");
        entries = new Cat03[nlevels];
        for (int i = 0; i < nlevels; i++) {
          entries[i] = new Cat03(b, offset);
          if (showData) System.out.println(" " + i + ": " + entries[i]);
          offset += 13;
        }
      } else if (code == 4) {
        if (showData) System.out.println(catNames[4] + ":");
        entries = new Cat04[nlevels];
        for (int i = 0; i < nlevels; i++) {
          entries[i] = new Cat04(b, offset);
          if (showData) System.out.println(" " + i + ": " + entries[i]);
          offset += 13;
        }
      } else if (code == 5) {
        if (showData) System.out.println(catNames[5] + ":");
        entries = new Cat05[nlevels];
        for (int i = 0; i < nlevels; i++) {
          entries[i] = new Cat05(b, offset);
          if (showData) System.out.println(" " + i + ": " + entries[i]);
          offset += 22;
        }
      } else if (code == 7) {
        if (showData) System.out.println(catNames[7] + ":");
        entries = new Cat07[nlevels];
        for (int i = 0; i < nlevels; i++) {
          entries[i] = new Cat07(b, offset);
          if (showData) System.out.println(" " + i + ": " + entries[i]);
          offset += 10;
        }
      } else if (code == 8) {
        if (showData) System.out.println(catNames[8] + ":");
        entries = new Cat08[nlevels];
        for (int i = 0; i < nlevels; i++) {
          entries[i] = new Cat08(b, offset);
          if (showData) System.out.println(" " + i + ": " + entries[i]);
          offset += 10;
        }
      } else if (code == 51) {
        if (showData) System.out.println(catNames[10] + ":");
        entries = new Cat51[nlevels];
        for (int i = 0; i < nlevels; i++) {
          entries[i] = new Cat51(b, offset);
          if (showData) System.out.println(" " + i + ": " + entries[i]);
          offset += 60;
        }
      } else if (code == 52) {
        if (showData) System.out.println(catNames[10] + ":");
        entries = new Cat52[nlevels];
        for (int i = 0; i < nlevels; i++) {
          entries[i] = new Cat52(b, offset);
          if (showData) System.out.println(" " + i + ": " + entries[i]);
          offset += 40;
        }
      } else {
        throw new UnsupportedOperationException("code= " + code);
      }

      // must be multiple of 10
      int skip = offset % 10;
      if (skip > 0)
        offset += (10 - skip);
      return offset;
    }

    public String toString() {
      return "Category/Group " + " code=" + code + " next= " + next + " nlevels=" + nlevels + " nbytes=" + nbytes;
    }
  }

  private int readIntWithOverflow(byte[] b, int offset, int len) {

    String s = new String(b, offset, len);
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      if (showOverflow) System.out.println("OVERFLOW=" + s);
      return 0;
    }
  }

  private String[] catNames = new String[]{"",
      "Category 01: mandatory constant-pressure data",
      "Category 02: temperature/dewpoint at variable pressure-levels ",
      "Category 03: wind at variable pressure-levels ",
      "Category 04: wind at variable height-levels ",
      "Category 05: tropopause data", "",
      "Category 07: cloud cover",
      "Category 08: additional data", "", "",
      "Category 51: surface Data",
      "Category 52: ship surface Data"};


  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private static float[] mandPressureLevel = new float[]{1000, 850, 700, 500, 400, 300, 250, 200, 150, 100,
      70, 50, 30, 20, 10, 7, 5, 3, 2, 1};

  private abstract class Entry {
    abstract Structure makeStructure(Structure parent) throws InvalidRangeException;

    abstract void loadStructureData(ByteBuffer bb);
  }

  private class Cat01 extends Entry {
    short windDir, windSpeed;
    float geopot, press, temp, dewp;
    byte[] quality = new byte[4];

    Cat01(byte[] b, int offset, int level) throws IOException {
      press = mandPressureLevel[level];
      geopot = Float.parseFloat(new String(b, offset, 5));
      temp = .1f * Float.parseFloat(new String(b, offset + 5, 4));
      dewp = .1f * Float.parseFloat(new String(b, offset + 9, 3));
      windDir = Short.parseShort(new String(b, offset + 12, 3));
      windSpeed = Short.parseShort(new String(b, offset + 15, 3));
      System.arraycopy(b, offset + 18, quality, 0, 4);
    }

    public String toString() {
      return "Cat01: press= " + press + " geopot=" + geopot + " temp= " + temp + " dewp=" + dewp + " windDir=" + windDir +
          " windSpeed=" + windSpeed + " qs=" + new String(quality);
    }

    Structure makeStructure(Structure parent) throws InvalidRangeException {
      Sequence seq = new Sequence(ncfile, null, parent, "mandatoryLevels");
      seq.addAttribute(new Attribute("long_name", catNames[1]));

      int pos = 0;

      Variable v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pressure", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "mbars"));
      v.addAttribute(new Attribute("long_name", "pressure level"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "geopotential", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "meter"));
      v.addAttribute(new Attribute("long_name", "geopotential"));
      v.addAttribute(new Attribute("missing_value", 99999.0f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "temperature", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "temperature"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 999.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "dewpoint", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "dewpoint depression"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 99.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "windDir", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "degrees"));
      v.addAttribute(new Attribute("long_name", "wind direction"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "windSpeed", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "knots"));
      v.addAttribute(new Attribute("long_name", "wind speed"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "qualityFlags", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{4});
      v.addAttribute(new Attribute("long_name", "quality marks: 0=geopot, 1=temp, 2=dewpoint, 3=wind"));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      return seq;
    }

    void loadStructureData(ByteBuffer bb) {
      bb.putFloat(press);
      bb.putFloat(geopot);
      bb.putFloat(temp);
      bb.putFloat(dewp);
      bb.putShort(windDir);
      bb.putShort(windSpeed);
      bb.put(quality);
    }
  }

  private class Cat02 extends Entry {
    float press, temp, dewp;
    byte[] quality = new byte[3];
    String qs;

    Cat02(byte[] b, int offset) throws IOException {
      press = .1f * Float.parseFloat(new String(b, offset, 5));
      temp = .1f * Float.parseFloat(new String(b, offset + 5, 4));
      dewp = .1f * Float.parseFloat(new String(b, offset + 9, 3));
      System.arraycopy(b, offset + 12, quality, 0, 3);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat02: press=" + press + " temp= " + temp + " dewp=" + dewp + " qs=" + qs;
    }

    Structure makeStructure(Structure parent) throws InvalidRangeException {
      Sequence seq = new Sequence(ncfile, null, parent, "tempPressureLevels");
      seq.addAttribute(new Attribute("long_name", catNames[2]));

      int pos = 0;

      Variable v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pressure", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "mbars"));
      v.addAttribute(new Attribute("long_name", "pressure level"));
      v.addAttribute(new Attribute("accuracy", "mbar/10"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "temperature", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "temperature"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 999.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "dewpoint", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "dewpoint depression"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 99.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "qualityFlags", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{3});
      v.addAttribute(new Attribute("long_name", "quality marks: 0=pressure, 1=temp, 2=dewpoint"));
      v.setSPobject(new Vinfo(pos));
      pos += 3;

      return seq;
    }

    void loadStructureData(ByteBuffer bb) {
      bb.putFloat(press);
      bb.putFloat(temp);
      bb.putFloat(dewp);
      bb.put(quality);
    }
  }

  private class Cat03 extends Entry {
    float press;
    short windDir, windSpeed;
    byte[] quality;
    String qs;

    Cat03(byte[] b, int offset) throws IOException {
      press = .1f * Float.parseFloat(new String(b, offset, 5));
      windDir = Short.parseShort(new String(b, offset + 5, 3));
      windSpeed = Short.parseShort(new String(b, offset + 8, 3));
      quality = new byte[2];
      System.arraycopy(b, offset + 11, quality, 0, 2);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat03: press=" + press + " windDir=" + windDir + " windSpeed=" + windSpeed + " qs=" + qs;
    }

    Structure makeStructure(Structure parent) throws InvalidRangeException {
      Sequence seq = new Sequence(ncfile, null, parent, "windPressureLevels");
      seq.addAttribute(new Attribute("long_name", catNames[3]));

      int pos = 0;

      Variable v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pressure", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "mbars"));
      v.addAttribute(new Attribute("long_name", "pressure level"));
      v.addAttribute(new Attribute("accuracy", "mbar/10"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "windDir", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "degrees"));
      v.addAttribute(new Attribute("long_name", "wind direction"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "windSpeed", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "knots"));
      v.addAttribute(new Attribute("long_name", "wind speed"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "qualityFlags", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "quality marks: 0=pressure, 1=wind"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      return seq;
    }

    void loadStructureData(ByteBuffer bb) {
      bb.putFloat(press);
      bb.putShort(windDir);
      bb.putShort(windSpeed);
      bb.put(quality);
    }
  }

  private class Cat04 extends Entry {
    float geopot;
    short windDir, windSpeed;
    byte[] quality;
    String qs;

    Cat04(byte[] b, int offset) throws IOException {
      geopot = Float.parseFloat(new String(b, offset, 5));
      windDir = Short.parseShort(new String(b, offset + 5, 3));
      windSpeed = Short.parseShort(new String(b, offset + 8, 3));
      quality = new byte[2];
      System.arraycopy(b, offset + 11, quality, 0, 2);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat04: geopot=" + geopot + " windDir=" + windDir + " windSpeed=" + windSpeed + " qs=" + qs;
    }

    Structure makeStructure(Structure parent) throws InvalidRangeException {
      Sequence seq = new Sequence(ncfile, null, parent, "windHeightLevels");
      seq.addAttribute(new Attribute("long_name", catNames[4]));

      int pos = 0;

      Variable v = seq.addMemberVariable(new Variable(ncfile, null, parent, "geopotential", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "meter"));
      v.addAttribute(new Attribute("long_name", "geopotential"));
      v.addAttribute(new Attribute("missing_value", 99999.0f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "windDir", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "degrees"));
      v.addAttribute(new Attribute("long_name", "wind direction"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "windSpeed", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "knots"));
      v.addAttribute(new Attribute("long_name", "wind speed"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "qualityFlags", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "quality marks: 0=geopot, 1=wind"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      return seq;
    }

    void loadStructureData(ByteBuffer bb) {
      bb.putFloat(geopot);
      bb.putShort(windDir);
      bb.putShort(windSpeed);
      bb.put(quality);
    }
  }

  private class Cat05 extends Entry {
    float press, temp, dewp;
    short windDir, windSpeed;
    byte[] quality;
    String qs;

    Cat05(byte[] b, int offset) throws IOException {
      press = .1f * Float.parseFloat(new String(b, offset, 5));
      temp = .1f * Float.parseFloat(new String(b, offset + 5, 4));
      dewp = .1f * Float.parseFloat(new String(b, offset + 9, 3));
      windDir = Short.parseShort(new String(b, offset + 12, 3));
      windSpeed = Short.parseShort(new String(b, offset + 15, 3));
      quality = new byte[4];
      System.arraycopy(b, offset + 18, quality, 0, 4);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat05: press= " + press + " temp= " + temp + " dewp=" + dewp + " windDir=" + windDir +
          " windSpeed=" + windSpeed + " qs=" + qs;
    }

    Structure makeStructure(Structure parent) throws InvalidRangeException {
      Sequence seq = new Sequence(ncfile, null, parent, "tropopause");
      seq.addAttribute(new Attribute("long_name", catNames[5]));

      int pos = 0;

      Variable v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pressure", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "mbars"));
      v.addAttribute(new Attribute("long_name", "pressure level"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "temperature", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "temperature"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 999.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "dewpoint", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "dewpoint depression"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 99.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "windDir", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "degrees"));
      v.addAttribute(new Attribute("long_name", "wind direction"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "windSpeed", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "knots"));
      v.addAttribute(new Attribute("long_name", "wind speed"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "qualityFlags", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{4});
      v.addAttribute(new Attribute("long_name", "quality marks: 0=pressure, 1=temp, 2=dewpoint, 3=wind"));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      return seq;
    }

    void loadStructureData(ByteBuffer bb) {
      bb.putFloat(press);
      bb.putFloat(temp);
      bb.putFloat(dewp);
      bb.putShort(windDir);
      bb.putShort(windSpeed);
      bb.put(quality);
    }
  }

  private class Cat07 extends Entry {
    float press;
    short percentClouds;
    byte[] quality;
    String qs;

    Cat07(byte[] b, int offset) throws IOException {
      press = .1f * Float.parseFloat(new String(b, offset, 5));
      percentClouds = Short.parseShort(new String(b, offset + 5, 3));
      quality = new byte[2];
      System.arraycopy(b, offset + 8, quality, 0, 2);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat07: press=" + press + " percentClouds=" + percentClouds + " qs=" + qs;
    }

    Structure makeStructure(Structure parent) throws InvalidRangeException {
      Sequence seq = new Sequence(ncfile, null, parent, "clouds");
      seq.addAttribute(new Attribute("long_name", catNames[7]));

      int pos = 0;

      Variable v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pressure", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "mbars"));
      v.addAttribute(new Attribute("long_name", "pressure level"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "percentClouds", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", ""));
      v.addAttribute(new Attribute("long_name", "amount of cloudiness (%)"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "qualityFlags", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "quality marks: 0=pressure, 1=percentClouds"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      return seq;
    }

    void loadStructureData(ByteBuffer bb) {
      bb.putFloat(press);
      bb.putShort(percentClouds);
      bb.put(quality);
    }
  }

  private class Cat08 extends Entry {
    int data;
    short table101code;
    byte[] quality;
    String qs;

    Cat08(byte[] b, int offset) throws IOException {
      data = Integer.parseInt(new String(b, offset, 5));
      table101code = Short.parseShort(new String(b, offset + 5, 3));
      quality = new byte[2];
      System.arraycopy(b, offset + 8, quality, 0, 2);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat08: data=" + data + " table101code=" + table101code + " qs=" + qs;
    }

    Structure makeStructure(Structure parent) throws InvalidRangeException {
      Sequence seq = new Sequence(ncfile, null, parent, "otherData");
      seq.addAttribute(new Attribute("long_name", catNames[8]));

      int pos = 0;

      Variable v = seq.addMemberVariable(new Variable(ncfile, null, parent, "data", DataType.INT, ""));
      v.addAttribute(new Attribute("long_name", "additional data specified in table 101.1"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "table101code", DataType.SHORT, ""));
      v.addAttribute(new Attribute("long_name", "code figure from table 101"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "indicatorFlags", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "quality marks: 0=data, 1=form"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      return seq;
    }

    void loadStructureData(ByteBuffer bb) {
      bb.putInt(data);
      bb.putShort(table101code);
      bb.put(quality);
    }
  }

  private class Cat51 extends Entry {
    short windDir, windSpeed;
    float pressSeaLevel, pressStation, geopot, press, temp, dewp, maxTemp, minTemp, pressureTendency;
    byte[] quality = new byte[4];
    byte pastWeatherW2, pressureTendencyChar;
    byte[] horizVis = new byte[3];
    byte[] presentWeather = new byte[3];
    byte[] pastWeatherW1 = new byte[2];
    byte[] fracCloudN = new byte[2];
    byte[] fracCloudNh = new byte[2];
    byte[] cloudCl = new byte[2];
    byte[] cloudBaseHeight = new byte[2];
    byte[] cloudCm = new byte[2];
    byte[] cloudCh = new byte[2];

    Cat51(byte[] b, int offset) throws IOException {
      pressSeaLevel = Float.parseFloat(new String(b, offset, 5));
      pressStation = Float.parseFloat(new String(b, offset + 5, 5));
      windDir = Short.parseShort(new String(b, offset + 10, 3));
      windSpeed = Short.parseShort(new String(b, offset + 13, 3));
      temp = .1f * Float.parseFloat(new String(b, offset + 16, 4));
      dewp = .1f * Float.parseFloat(new String(b, offset + 20, 3));
      maxTemp = .1f * Float.parseFloat(new String(b, offset + 23, 4));
      minTemp = .1f * Float.parseFloat(new String(b, offset + 27, 4));
      System.arraycopy(b, offset + 31, quality, 0, 4);

      pastWeatherW2 = b[offset + 35];
      System.arraycopy(b, offset + 36, horizVis, 0, 3);
      System.arraycopy(b, offset + 39, presentWeather, 0, 3);
      System.arraycopy(b, offset + 42, pastWeatherW1, 0, 2);
      System.arraycopy(b, offset + 44, fracCloudN, 0, 2);
      System.arraycopy(b, offset + 46, fracCloudNh, 0, 2);
      System.arraycopy(b, offset + 48, cloudCl, 0, 2);
      System.arraycopy(b, offset + 50, cloudBaseHeight, 0, 2);
      System.arraycopy(b, offset + 52, cloudCm, 0, 2);
      System.arraycopy(b, offset + 54, cloudCh, 0, 2);
      pressureTendencyChar = b[offset + 56];
      pressureTendency = .1f * Float.parseFloat(new String(b, offset + 57, 3));
    }

    public String toString() {
      return "Cat51: press= " + press + " geopot=" + geopot + " temp= " + temp + " dewp=" + dewp + " windDir=" + windDir +
          " windSpeed=" + windSpeed + " qs=" + new String(quality) + " pressureTendency=" + pressureTendency;
    }

    Structure makeStructure(Structure parent) throws InvalidRangeException {
      Sequence seq = new Sequence(ncfile, null, parent, "surfaceData");
      seq.addAttribute(new Attribute("long_name", catNames[11]));

      int pos = 0;

      Variable v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pressureSeaLevel", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "mbars"));
      v.addAttribute(new Attribute("long_name", "sea level pressure"));
      v.addAttribute(new Attribute("accuracy", "mbars/10"));
      v.addAttribute(new Attribute("missing_value", 9999.9f));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pressure", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "mbars"));
      v.addAttribute(new Attribute("long_name", "station pressure"));
      v.addAttribute(new Attribute("accuracy", "mbars/10"));
      v.addAttribute(new Attribute("missing_value", 9999.9f));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "windDir", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "degrees"));
      v.addAttribute(new Attribute("long_name", "wind direction"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "windSpeed", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "knots"));
      v.addAttribute(new Attribute("long_name", "wind speed"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "temperature", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "air temperature"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 999.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "dewpoint", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "dewpoint depression"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 99.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "temperatureMax", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "maximum temperature"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 999.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "temperatureMin", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "minimum temperature"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 999.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "qualityFlags", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{4});
      v.addAttribute(new Attribute("long_name", "quality marks: 0=pressureSeaLevel, 1=pressure, 2=wind, 3=temperature"));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pastWeatherW2", DataType.CHAR, ""));
      v.addAttribute(new Attribute("long_name", "past weather (W2): WMO table 4561"));
      v.setSPobject(new Vinfo(pos));
      pos += 1;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "horizViz", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{3});
      v.addAttribute(new Attribute("long_name", "horizontal visibility: WMO table 4300"));
      v.setSPobject(new Vinfo(pos));
      pos += 3;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "presentWeatherWW", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{3});
      v.addAttribute(new Attribute("long_name", "present weather (WW): WMO table 4677"));
      v.setSPobject(new Vinfo(pos));
      pos += 3;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pastWeatherW1", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "past weather (WW): WMO table 4561"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "cloudFractionN", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "cloud fraction (N): WMO table 2700"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "cloudFractionNh", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "cloud fraction (Nh): WMO table 2700"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "cloudFractionCL", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "cloud fraction (CL): WMO table 0513"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "cloudHeightCL", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "cloud base height above ground (h): WMO table 1600"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "cloudFractionCM", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "cloud fraction (CM): WMO table 0515"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "cloudFractionCH", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "cloud fraction (CH): WMO table 0509"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pressureTendencyCharacteristic", DataType.CHAR, ""));
      v.addAttribute(new Attribute("long_name", "pressure tendency characteristic for 3 hours previous to obs time: WMO table 0200"));
      v.setSPobject(new Vinfo(pos));
      pos += 1;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "pressureTendency", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "mbars"));
      v.addAttribute(new Attribute("long_name", "pressure tendency magnitude"));
      v.addAttribute(new Attribute("accuracy", "mbars/10"));
      v.addAttribute(new Attribute("missing_value", 99.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      return seq;
    }

    void loadStructureData(ByteBuffer bb) {
      bb.putFloat(pressSeaLevel);
      bb.putFloat(pressStation);
      bb.putShort(windDir);
      bb.putShort(windSpeed);
      bb.putFloat(temp);
      bb.putFloat(dewp);
      bb.putFloat(maxTemp);
      bb.putFloat(minTemp);
      bb.put(quality);
      bb.put(pastWeatherW2);
      bb.put(horizVis);
      bb.put(presentWeather);
      bb.put(pastWeatherW1);
      bb.put(fracCloudN);
      bb.put(fracCloudNh);
      bb.put(cloudCl);
      bb.put(cloudBaseHeight);
      bb.put(cloudCm);
      bb.put(cloudCh);
      bb.put(pressureTendencyChar);
      bb.putFloat(pressureTendency);
    }

  }

  private class Cat52 extends Entry {
    short snowDepth, wavePeriod, waveHeight, waveSwellPeriod, waveSwellHeight;
    float precip6hours, precip24hours, sst, waterEquiv;
    byte precipDuration, shipCourse;
    byte[] waveDirection = new byte[2];
    byte[] special = new byte[2];
    byte[] special2 = new byte[2];
    byte[] shipSpeed = new byte[2];

    Cat52(byte[] b, int offset) throws IOException {
      precip6hours = .01f * Float.parseFloat(new String(b, offset, 4));
      snowDepth = Short.parseShort(new String(b, offset + 4, 3));
      precip24hours = .01f * Float.parseFloat(new String(b, offset + 7, 4));
      precipDuration = b[offset + 11];
      wavePeriod = Short.parseShort(new String(b, offset + 12, 2));
      waveHeight = Short.parseShort(new String(b, offset + 14, 2));
      System.arraycopy(b, offset + 16, waveDirection, 0, 2);
      waveSwellPeriod = Short.parseShort(new String(b, offset + 18, 2));
      waveSwellHeight = Short.parseShort(new String(b, offset + 20, 2));
      sst = .1f * Float.parseFloat(new String(b, offset + 22, 4));
      System.arraycopy(b, offset + 26, special, 0, 2);
      System.arraycopy(b, offset + 28, special2, 0, 2);
      shipCourse = b[offset + 30];
      System.arraycopy(b, offset + 31, shipSpeed, 0, 2);
      waterEquiv = .001f * Float.parseFloat(new String(b, offset + 33, 7));
    }

    void loadStructureData(ByteBuffer bb) {
      bb.putFloat(precip6hours);
      bb.putShort(snowDepth);
      bb.putFloat(precip24hours);
      bb.put(precipDuration);
      bb.putShort(wavePeriod);
      bb.putShort(waveHeight);
      bb.put(waveDirection);
      bb.putShort(waveSwellPeriod);
      bb.putShort(waveSwellHeight);
      bb.putFloat(sst);
      bb.put(special);
      bb.put(special2);
      bb.put(shipCourse);
      bb.put(shipSpeed);
      bb.putFloat(waterEquiv);
    }

    public String toString() {
      return "Cat52: precip6hours= " + precip6hours + " precip24hours=" + precip24hours + " sst= " + sst + " waterEquiv=" + waterEquiv +
          " snowDepth=" + snowDepth + " wavePeriod=" + wavePeriod + " waveHeight=" + waveHeight +
          " waveSwellPeriod=" + waveSwellPeriod + " waveSwellHeight=" + waveSwellHeight;
    }

    Structure makeStructure(Structure parent) throws InvalidRangeException {
      Sequence seq = new Sequence(ncfile, null, parent, "surfaceData2");
      seq.addAttribute(new Attribute("long_name", catNames[12]));

      int pos = 0;

      Variable v = seq.addMemberVariable(new Variable(ncfile, null, parent, "precip6hours", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "inch"));
      v.addAttribute(new Attribute("long_name", "precipitation past 6 hours"));
      v.addAttribute(new Attribute("accuracy", "inch/100"));
      v.addAttribute(new Attribute("missing_value", 99.99f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "snowDepth", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "inch"));
      v.addAttribute(new Attribute("long_name", "total depth of snow on ground"));
      v.addAttribute(new Attribute("missing_value", (short) 999));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "precip24hours", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "inch"));
      v.addAttribute(new Attribute("long_name", "precipitation past 24 hours"));
      v.addAttribute(new Attribute("accuracy", "inch/100"));
      v.addAttribute(new Attribute("missing_value", 99.99f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "precipDuration", DataType.BYTE, ""));
      v.addAttribute(new Attribute("units", "6 hours"));
      v.addAttribute(new Attribute("long_name", "duration of precipitation observation"));
      v.addAttribute(new Attribute("missing_value", 9));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "wavePeriod", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "second"));
      v.addAttribute(new Attribute("long_name", "period of waves"));
      v.addAttribute(new Attribute("missing_value", (short) 99));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "waveHeight", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "meter/2"));
      v.addAttribute(new Attribute("long_name", "height of waves"));
      v.addAttribute(new Attribute("missing_value", (short) 99));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "swellWaveDir", DataType.CHAR, ""));
      v.setDimensionsAnonymous(new int[]{2});
      v.addAttribute(new Attribute("long_name", "direction from which swell waves are moving: WMO table 0877"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "swellWavePeriod", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "second"));
      v.addAttribute(new Attribute("long_name", "period of swell waves"));
      v.addAttribute(new Attribute("missing_value", (short) 99));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "swellWaveHeight", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "meter/2"));
      v.addAttribute(new Attribute("long_name", "height of waves"));
      v.addAttribute(new Attribute("missing_value", (short) 99));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "sst", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "sea surface temperature"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));
      v.addAttribute(new Attribute("missing_value", 999.9f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "special", DataType.CHAR, ""));
      v.addAttribute(new Attribute("long_name", "special phenomena - general"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "specialDetail", DataType.CHAR, ""));
      v.addAttribute(new Attribute("long_name", "special phenomena - detailed"));
      v.setSPobject(new Vinfo(pos));
      pos += 2;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "shipCourse", DataType.CHAR, ""));
      v.addAttribute(new Attribute("long_name", "ships course: WMO table 0700"));
      v.setSPobject(new Vinfo(pos));
      pos += 1;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "shipSpeed", DataType.CHAR, ""));
      v.addAttribute(new Attribute("long_name", "ships average speed: WMO table 4451"));
      v.setSPobject(new Vinfo(pos));
      pos += 1;

      v = seq.addMemberVariable(new Variable(ncfile, null, parent, "waterEquiv", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "inch"));
      v.addAttribute(new Attribute("long_name", "water equivalent of snow and/or ice"));
      v.addAttribute(new Attribute("accuracy", "inch/100"));
      v.addAttribute(new Attribute("missing_value", 99999.99f));
      v.setSPobject(new Vinfo(pos));
      pos += 4;

      return seq;
    }

  }

  private boolean endRecord(RandomAccessFile raf) throws IOException {
    if (showSkip) System.out.print(" endRecord start at " + raf.getFilePointer());

    int skipped = 0;
    String endRecord = new String(raf.readBytes(10));
    while (endRecord.equals("END RECORD")) {
      endRecord = new String(raf.readBytes(10));
      skipped++;
    }
    if (showSkip) System.out.println(" last 10 chars= " + endRecord + " skipped= " + skipped);
    return true;
  }

  private boolean endFile(RandomAccessFile raf) throws IOException {
    if (showSkip) System.out.println(" endFile start at " + raf.getFilePointer());

    String endRecord = new String(raf.readBytes(10));
    while (endRecord.equals("ENDOF FILE")) {
      endRecord = new String(raf.readBytes(10));
    }

    try {
      while (raf.read() != (int) 'X') ; //find where X's start
      while (raf.read() == (int) 'X') ; //skip X's till you run out
      raf.skipBytes(-1); // go back one
      readHeader(raf);
      return true;

    } catch (EOFException e) {
      return false;
    }
  }

  private void readHeader(RandomAccessFile raf) throws IOException {
    byte[] h = raf.readBytes(60);

    // 12 00 070101
    short hour = Short.parseShort(new String(h, 0, 2));
    short minute = Short.parseShort(new String(h, 2, 2));
    short year = Short.parseShort(new String(h, 4, 2));
    short month = Short.parseShort(new String(h, 6, 2));
    short day = Short.parseShort(new String(h, 8, 2));

    int fullyear = (year > 30) ? 1900 + year : 2000 + year;

    if (cal == null) {
      cal = Calendar.getInstance();
      cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    cal.clear();
    cal.set(fullyear, month - 1, day, hour, minute);
    refDate = cal.getTime();
    refString = new String(h, 0, 10);

    if (showHeader) System.out.println("\nhead=" + new String(h) +
        " date= " + dateFormatter.toDateTimeString(refDate));

    int b, count = 0;
    while ((b = raf.read()) == (int) 'X') count++;
    char c = (char) b;
    if (showSkip) System.out.println(" b=" + b + " c=" + c + " at " + raf.getFilePointer() + " skipped= " + count);
    raf.skipBytes(-1); // go back one
  }

  static class MyNetcdfFile extends NetcdfFile {
    MyNetcdfFile(NmcObsLegacy iosp) {
      this.spi = iosp;
    }
  }

  static public void main(String args[]) throws IOException, InvalidRangeException {
    String filename = "C:/data/cadis/tempting";
    //String filename = "C:/data/cadis/Y94179";
    //String filename = "C:/data/cadis/Y94132";
    NmcObsLegacy iosp = new NmcObsLegacy();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    NetcdfFile ncfile = new MyNetcdfFile(iosp);
    ncfile.setLocation(filename);
    iosp.open(raf, ncfile, null);
    System.out.println("\n" + ncfile);

    Variable v = ncfile.findVariable("station");
    Array data = v.read(new Section().appendRange(0, 1));
    System.out.println(NCdumpW.printArray(data, "station", null));

    v = ncfile.findVariable("report");
    data = v.read(new Section().appendRange(0, 0));
    System.out.println(NCdumpW.printArray(data, "report", null));

    v = ncfile.findVariable("reportIndex");
    data = v.read();
    System.out.println(NCdumpW.printArray(data, "reportIndex", null));

    iosp.close();
  }
}
