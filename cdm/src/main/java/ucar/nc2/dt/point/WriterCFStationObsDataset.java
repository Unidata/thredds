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

package ucar.nc2.dt.point;

import ucar.nc2.dt.*;
import ucar.nc2.*;
import ucar.nc2.iosp.netcdf3.N3streamWriter;
import ucar.nc2.iosp.netcdf3.N3outputStreamWriter;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.*;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.util.*;
import java.io.*;

/**
 * Write StationObsDataset in "CF" experimental point/ungridded convention.
 * Also experiment with streaming netcdf.
 *
 * @author caron
 */
public class WriterCFStationObsDataset {
  private static final String recordDimName = "record";
  private static final String stationDimName = "station";
  private static final String latName = "latitude";
  private static final String lonName = "longitude";
  private static final String altName = "altitude";
  private static final String idName = "station_id";
  private static final String descName = "station_description";
  private static final String wmoName = "wmo_id";

  private static final String timeName = "time";
  private static final String parentName = "parent_index";

  private DateFormatter dateFormatter = new DateFormatter();
  private int name_strlen, desc_strlen, wmo_strlen;

  private NetcdfFileStream ncfile;
  private String title;

  private Set<Dimension> dimSet = new HashSet<Dimension>();
  private List<Station> stnList;
  private List<Variable> recordVars = new ArrayList<Variable>();
  private Date minDate = null;
  private Date maxDate = null;

  private boolean useAlt = false;
  private boolean useWmoId = false;

  private boolean debug = false;

  public WriterCFStationObsDataset(DataOutputStream stream, String title) {
    ncfile = new NetcdfFileStream(stream);
    this.title = title;
  }

  private class NetcdfFileStream extends NetcdfFile {
    N3outputStreamWriter swriter;
    DataOutputStream stream;

    NetcdfFileStream(DataOutputStream stream) {
      super();
      this.stream = stream;
      swriter = new N3outputStreamWriter(this);
    }

    void writeHeader() throws IOException {
      swriter.writeHeader(stream);
    }

    void writeNonRecordData(String varName, Array data) throws IOException {
      swriter.writeNonRecordData(findVariable(varName), stream, data);
    }

    void writeRecordData(List<Variable> varList) throws IOException {
      swriter.writeRecordData(stream, varList);
    }
  }

  public void writeHeader(List<Station> stns, List<VariableSimpleIF> vars) throws IOException {
    createGlobalAttributes();
    createStations(stns);
    createRecordVariables(vars);

    ncfile.finish(); // done with define mode
    ncfile.writeHeader();
    writeStationData(stns); // write out the station info

    // now write the observations
    //if (! (Boolean) ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE))
    //  throw new IllegalStateException("can't add record variable");
  }

  private void createGlobalAttributes() {
    ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.0"));
    ncfile.addAttribute(null, new Attribute("cdm_datatype", "Station"));
    ncfile.addAttribute(null, new Attribute("title", title));
    //ncfile.addAttribute(null, new Attribute("desc", "Extracted by THREDDS/Netcdf Subset Service"));
    /* ncfile.addGlobalAttribute("observationDimension", recordDimName);
    ncfile.addGlobalAttribute("stationDimension", stationDimName);
    ncfile.addGlobalAttribute("latitude_coordinate", latName);
    ncfile.addGlobalAttribute("longitude_coordinate", lonName);
    ncfile.addGlobalAttribute("time_coordinate", timeName);
    // dummys, update in finish()
    ncfile.addAttribute( null, new Attribute("time_coverage_start", dateFormatter.toDateTimeStringISO(new Date())));
    ncfile.addAttribute( null, new Attribute("time_coverage_end", dateFormatter.toDateTimeStringISO(new Date()))); */
  }

  private void createStations(List<Station> stnList) throws IOException {
    int nstns = stnList.size();

    // see if there's altitude, wmoId for any stations
    for (int i = 0; i < nstns; i++) {
      Station stn = stnList.get(i);

      if (!Double.isNaN(stn.getAltitude()))
        useAlt = true;
      if ((stn.getWmoId() != null) && (stn.getWmoId().trim().length() > 0))
        useWmoId = true;
    }

    /* if (useAlt)
      ncfile.addGlobalAttribute("altitude_coordinate", altName); */

    // find string lengths
    for (int i = 0; i < nstns; i++) {
      Station station = stnList.get(i);
      name_strlen = Math.max(name_strlen, station.getName().length());
      desc_strlen = Math.max(desc_strlen, station.getDescription().length());
      if (useWmoId) wmo_strlen = Math.max(wmo_strlen, station.getName().length());
    }

    LatLonRect llbb = getBoundingBox(stnList);
    ncfile.addAttribute(null, new Attribute("geospatial_lat_min", Double.toString(llbb.getLowerLeftPoint().getLatitude())));
    ncfile.addAttribute(null, new Attribute("geospatial_lat_max", Double.toString(llbb.getUpperRightPoint().getLatitude())));
    ncfile.addAttribute(null, new Attribute("geospatial_lon_min", Double.toString(llbb.getLowerLeftPoint().getLongitude())));
    ncfile.addAttribute(null, new Attribute("geospatial_lon_max", Double.toString(llbb.getUpperRightPoint().getLongitude())));

    // add the dimensions
    ncfile.addDimension(null, new Dimension(recordDimName, 0, true, true, false));
    ncfile.addDimension(null, new Dimension(stationDimName, nstns));

    // add the station Variables using the station dimension
    Variable v = ncfile.addVariable(null, latName, DataType.DOUBLE, stationDimName);
    v.addAttribute(new Attribute("units", "degrees_north"));
    v.addAttribute(new Attribute("long_name", "station latitude"));

    v = ncfile.addVariable(null, lonName, DataType.DOUBLE, stationDimName);
    v.addAttribute(new Attribute("units", "degrees_east"));
    v.addAttribute(new Attribute("long_name", "station longitude"));

    if (useAlt) {
      v = ncfile.addVariable(null, altName, DataType.DOUBLE, stationDimName);
      v.addAttribute(new Attribute("units", "meters"));
      v.addAttribute(new Attribute("long_name", "station altitude"));
      v.addAttribute(new Attribute("positive", "up"));
    }

    v = ncfile.addStringVariable(null, idName, stationDimName, name_strlen);
    v.addAttribute(new Attribute("long_name", "station identifier"));

    v = ncfile.addStringVariable(null, descName, stationDimName, desc_strlen);
    v.addAttribute(new Attribute("long_name", "station description"));

    if (useWmoId) {
      v = ncfile.addStringVariable(null, wmoName, stationDimName, wmo_strlen);
      v.addAttribute(new Attribute("long_name", "station WMO id"));
    }

    /* v = ncfile.addVariable(null, numChildName, DataType.INT, stationDimName);
    v.addAttribute( new Attribute("long_name", "number of children in linked list for this station"));

    v = ncfile.addVariable(null, lastChildName, DataType.INT, stationDimName);
    v.addAttribute( new Attribute("long_name", "record number of last child in linked list for this station"));

    v = ncfile.addVariable(null, firstChildName, DataType.INT, stationDimName);
    v.addAttribute( new Attribute("long_name", "record number of first child in linked list for this station"));  */

  }

  private ArrayInt.D1 timeArray = new ArrayInt.D1(1);
  private ArrayInt.D1 parentArray = new ArrayInt.D1(1);


  private void createRecordVariables(List<VariableSimpleIF> dataVars) {

    // time variable
    Variable timeVar = ncfile.addVariable(null, timeName, DataType.INT, recordDimName);
    timeVar.addAttribute(new Attribute("units", "secs since 1970-01-01 00:00:00"));
    timeVar.addAttribute(new Attribute("long_name", "calendar date"));
    recordVars.add(timeVar);
    timeVar.setCachedData(timeArray, false);

    Variable parentVar = ncfile.addVariable(null, parentName, DataType.INT, recordDimName);
    parentVar.addAttribute(new Attribute("long_name", "index of parent station"));
    recordVars.add(parentVar);
    parentVar.setCachedData(parentArray, false);

    Attribute coordAtt = new Attribute("coordinates", useAlt ? "latitude longitude altitude time" : "latitude longitude time");

    // find all dimensions needed by the data variables
    for (VariableSimpleIF var : dataVars) {
      List<Dimension> dims = var.getDimensions();
      dimSet.addAll(dims);
    }

    // add them
    for (Dimension d : dimSet) {
      if (!d.isUnlimited())
        ncfile.addDimension(null, new Dimension(d.getName(), d.getLength(), d.isShared(), false, d.isVariableLength()));
    }

    // add the data variables all using the record dimension
    for (VariableSimpleIF oldVar : dataVars) {
      List<Dimension> dims = oldVar.getDimensions();
      StringBuffer dimNames = new StringBuffer(recordDimName);
      for (Dimension d : dims) {
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getName());
      }
      Variable newVar = ncfile.addVariable(null, oldVar.getName(), oldVar.getDataType(), dimNames.toString());
      recordVars.add(newVar);

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts)
        newVar.addAttribute(att);
      newVar.addAttribute(coordAtt);
    }

  }

  private HashMap<String, StationTracker> stationMap;

  private class StationTracker {
    int numChildren = 0;
    int lastChild = -1;
    int parent_index;
    List<Integer> link = new ArrayList<Integer>(); // recnums

    StationTracker(int parent_index) {
      this.parent_index = parent_index;
    }
  }

  private void writeStationData(List<Station> stnList) throws IOException {
    this.stnList = stnList;
    int nstns = stnList.size();
    stationMap = new HashMap<String, StationTracker>(2 * nstns);
    if (debug) System.out.println("stationMap created");

    // now write the station data
    ArrayDouble.D1 latArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 lonArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 altArray = new ArrayDouble.D1(nstns);
    ArrayChar.D2 idArray = new ArrayChar.D2(nstns, name_strlen);
    ArrayChar.D2 descArray = new ArrayChar.D2(nstns, desc_strlen);
    ArrayChar.D2 wmoArray = new ArrayChar.D2(nstns, wmo_strlen);

    for (int i = 0; i < stnList.size(); i++) {
      Station stn = stnList.get(i);
      stationMap.put(stn.getName(), new StationTracker(i));

      latArray.set(i, stn.getLatitude());
      lonArray.set(i, stn.getLongitude());
      if (useAlt) altArray.set(i, stn.getAltitude());

      idArray.setString(i, stn.getName());
      descArray.setString(i, stn.getDescription());
      if (useWmoId) wmoArray.setString(i, stn.getWmoId());
    }

    try {
      //public void writeNonRecordVariable(WritableByteChannel channel, Variable v, Array data);

      ncfile.writeNonRecordData(latName, latArray);
      ncfile.writeNonRecordData(lonName, lonArray);
      if (useAlt) ncfile.writeNonRecordData(altName, altArray);
      ncfile.writeNonRecordData(idName, idArray);
      ncfile.writeNonRecordData(descName, descArray);
      if (useWmoId) ncfile.writeNonRecordData(wmoName, wmoArray);

    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  /* private void writeDataFinish() throws IOException {
    ArrayInt.D1 nextChildArray = new ArrayInt.D1(recno);

    int nstns = stnList.size();
    ArrayInt.D1 firstArray = new ArrayInt.D1(nstns);
    ArrayInt.D1 lastArray = new ArrayInt.D1(nstns);
    ArrayInt.D1 numArray = new ArrayInt.D1(nstns);

    for (int i = 0; i < stnList.size(); i++) {
      Station stn = stnList.get(i);
      StationTracker tracker = stationMap.get(stn.getName());


      lastArray.set(i, tracker.lastChild);
      numArray.set(i, tracker.numChildren);

      int first = (tracker.link.size() > 0) ? tracker.link.get(0) : -1;
      firstArray.set(i, first);

      if (tracker.link.size() > 0) {
        // construct forward link
        List<Integer> nextList = tracker.link;
        for (int j = 0; j < nextList.size() - 1; j++) {
          Integer curr = nextList.get(j);
          Integer next = nextList.get(j + 1);
          nextChildArray.set(curr, next);
        }
        Integer curr = nextList.get(nextList.size() - 1);
        nextChildArray.set(curr, -1);
      }
    }

    try {
      ncfile.write(firstChildName, firstArray);
      ncfile.write(lastChildName, lastArray);
      ncfile.write(numChildName, numArray);
      ncfile.write(nextChildName, nextChildArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    // if there is no data
    if (minDate == null) minDate = new Date();
    if (maxDate == null) maxDate = new Date();

    ncfile.updateAttribute(null, new Attribute("time_coverage_start", dateFormatter.toDateTimeStringISO(minDate)));
    ncfile.updateAttribute(null, new Attribute("time_coverage_end", dateFormatter.toDateTimeStringISO(maxDate)));
  }  */

  public void writeRecord(StationObsDatatype sobs, StructureData sdata) throws IOException {
    if (debug) System.out.println("sobs= " + sobs + "; station = " + sobs.getStation());

    StructureMembers sm = sdata.getStructureMembers();

    for (Variable v : recordVars) {
      if (timeName.equals(v.getShortName())) {
        Date d = sobs.getObservationTimeAsDate();
        int secs = (int) (d.getTime() / 1000);
        timeArray.set(0, secs);
      } else if (parentName.equals(v.getShortName())) {
        int stationIndex = stnList.indexOf(sobs.getStation());
        parentArray.set(0, stationIndex);
      } else {
        StructureMembers.Member m = sm.findMember(v.getShortName());
        v.setCachedData(sdata.getArray(m), false);
      }
    }

    ncfile.writeRecordData(recordVars);
  }

  /* public void writeRecord(String stnName, Date obsDate, StructureData sdata) throws IOException {
    StationTracker tracker = stationMap.get(stnName);

    // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
    ArrayStructureW sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
    sArray.setStructureData(sdata, 0);

    // date is handled specially
    if ((minDate == null) || minDate.after(obsDate)) minDate = obsDate;
    if ((maxDate == null) || maxDate.before(obsDate)) maxDate = obsDate;

    timeArray.set(0, dateFormatter.toDateTimeStringISO(obsDate));
    prevArray.set(0, tracker.lastChild);
    parentArray.set(0, tracker.parent_index);
    tracker.link.add(recno);
    tracker.lastChild = recno;
    tracker.numChildren++;

    // write the recno record
    origin[0] = recno;
    originTime[0] = recno;
    try {
      ncfile.write("record", origin, sArray);
      ncfile.writeStringData(timeName, originTime, timeArray);
      ncfile.write(prevChildName, originTime, prevArray);
      ncfile.write(parentName, originTime, parentArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    recno++;
  } */

  public void finish() throws IOException {
    //writeDataFinish();
    ncfile.close();
  }

  private LatLonRect getBoundingBox(List stnList) {
    Station s = (Station) stnList.get(0);
    LatLonPointImpl llpt = new LatLonPointImpl();
    llpt.set(s.getLatitude(), s.getLongitude());
    LatLonRect rect = new LatLonRect(llpt, .001, .001);

    for (int i = 1; i < stnList.size(); i++) {
      s = (Station) stnList.get(i);
      llpt.set(s.getLatitude(), s.getLongitude());
      rect.extend(llpt);
    }

    return rect;
  }

  /* not tested
  private void write(StationObsDataset sobsDataset) throws IOException {
    createGlobalAttributes();
    createStations(sobsDataset.getStations());

    ncfile.addGlobalAttribute("time_coverage_start", dateFormatter.toDateTimeStringISO(sobsDataset.getStartDate()));
    ncfile.addGlobalAttribute("time_coverage_end", dateFormatter.toDateTimeStringISO(sobsDataset.getEndDate()));

    createDataVariables(sobsDataset.getDataVariables());

    // global attributes
    List gatts = sobsDataset.getGlobalAttributes();
    for (int i = 0; i < gatts.size(); i++) {
      Attribute att = (Attribute) gatts.get(i);
      ncfile.addGlobalAttribute(att);
    }

    // done with define mode
    ncfile.create();

    // write out the station info
    writeStationData(sobsDataset.getStations());

    // now write the observations
    if (! (Boolean) ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE))
      throw new IllegalStateException("can't add record variable");

    int[] origin = new int[1];
    int[] originTime = new int[2];
    int recno = 0;
    ArrayStructureW sArray = null;
    ArrayObject.D1 timeArray = new ArrayObject.D1(String.class, 1);

    DataIterator diter = sobsDataset.getDataIterator(1000 * 1000);
    while (diter.hasNext()) {
      StationObsDatatype sobs = (StationObsDatatype) diter.nextData();
      StructureData recordData = sobs.getData();

      // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
      if (sArray == null)
        sArray = new ArrayStructureW(recordData.getStructureMembers(), new int[]{1});
      sArray.setStructureData(recordData, 0);

      // date is handled specially
      timeArray.set(0, dateFormatter.toDateTimeStringISO(sobs.getObservationTimeAsDate()));

      // write the recno record
      origin[0] = recno;
      originTime[0] = recno;
      try {
        ncfile.write("record", origin, sArray);
        ncfile.writeStringData(timeName, originTime, timeArray);

      } catch (InvalidRangeException e) {
        e.printStackTrace();
        throw new IllegalStateException(e);
      }
      recno++;
    }

    ncfile.close();
  } */

  public static void main3(String args[]) throws IOException {
    long start = System.currentTimeMillis();

    String location = "C:/data/metars/Surface_METAR_20070329_0000.nc";
    StringBuffer errlog = new StringBuffer();
    StationObsDataset sobs = (StationObsDataset) TypedDatasetFactory.open(ucar.nc2.constants.DataType.STATION, location, null, errlog);

    String fileOut = "C:/temp/Surface_METAR_20070329_0000.stream.nc";
    FileOutputStream fos = new FileOutputStream(fileOut);
    DataOutputStream out = new DataOutputStream(fos);
    System.out.println("Read " + location + " write to " + fileOut);

    WriterCFStationObsDataset writer = new WriterCFStationObsDataset(out, "test");

    List stns = sobs.getStations();
    List<Station> stnList = new ArrayList<Station>();
    Station s = (Station) stns.get(0);
    stnList.add(s);

    List<VariableSimpleIF> varList = new ArrayList<VariableSimpleIF>();
    varList.add(sobs.getDataVariable("wind_speed"));

    writer.writeHeader(stnList, varList);

    DataIterator iter = sobs.getDataIterator(s);
    while (iter.hasNext()) {
      StationObsDatatype sobsData = (StationObsDatatype) iter.nextData();
      StructureData data = sobsData.getData();
      writer.writeRecord(sobsData, data);
    }

    writer.finish();

    long took = System.currentTimeMillis() - start;
    System.out.println("That took = " + took + " msecs");
  }

  //////////////////////////////////////////////////////////////////////////////////
  public static void rewrite(String fileIn, String fileOut, boolean inMemory, boolean sort) throws IOException {
    System.out.println("Rewrite .nc files from " + fileIn + " to " + fileOut+ "inMem= "+inMemory+" sort= "+sort);

    long start = System.currentTimeMillis();

    // do it in memory for speed
    NetcdfFile ncfile = inMemory ? NetcdfFile.openInMemory(fileIn) : NetcdfFile.open(fileIn);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);

    StringBuffer errlog = new StringBuffer();
    StationObsDataset sobs = (StationObsDataset) TypedDatasetFactory.open(ucar.nc2.constants.DataType.STATION, ncd, null, errlog);

    List<Station> stns = sobs.getStations();
    List<VariableSimpleIF> vars = sobs.getDataVariables();

    FileOutputStream fos = new FileOutputStream(fileOut);
    DataOutputStream out = new DataOutputStream(fos);

    WriterCFStationObsDataset writer = new WriterCFStationObsDataset(out, "rewrite " + fileIn);
    writer.writeHeader(stns, vars);

    if (sort) {
      for (Station s : stns) {
        DataIterator iter = sobs.getDataIterator(s);
        while (iter.hasNext()) {
          StationObsDatatype sobsData = (StationObsDatatype) iter.nextData();
          StructureData data = sobsData.getData();
          writer.writeRecord(sobsData, data);
        }
      }
    } else {
      DataIterator iter = sobs.getDataIterator(1000 * 1000);
      while (iter.hasNext()) {
        StationObsDatatype sobsData = (StationObsDatatype) iter.nextData();
        StructureData data = sobsData.getData();
        writer.writeRecord(sobsData, data);
      }
    }

    writer.finish();

    long took = System.currentTimeMillis() - start;
    System.out.println("Rewrite " + fileIn + " to " + fileOut + " took = " + took);
  }

  public static void main(String args[]) throws IOException {
    String location = "C:/data/metars/Surface_METAR_20070329_0000.nc";

    File file = new File(location);
    rewrite(location, "C:/temp/FU" + file.getName(), false, false);
    rewrite(location, "C:/temp/FS" + file.getName(), false, true);
    rewrite(location, "C:/temp/MU" + file.getName(), true, false);
    rewrite(location, "C:/temp/MS" + file.getName(), true, true);
  }

  public static void main2(String args[]) throws IOException {
    long start = System.currentTimeMillis();
    String toLocation = "C:/temp2/";
    String fromLocation = "C:/data/metars/";

    if (args.length > 1) {
      fromLocation = args[0];
      toLocation = args[1];
    }
    System.out.println("Rewrite .nc files from " + fromLocation + " to " + toLocation);

    File dir = new File(fromLocation);
    File[] files = dir.listFiles();
    for (File file : files) {
      if (file.getName().endsWith(".nc"))
        rewrite(file.getAbsolutePath(), toLocation + file.getName(), true, true);
    }

    long took = System.currentTimeMillis() - start;
    System.out.println("That took = " + took);

  }


}
