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

package ucar.nc2.ft.point.writer;

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.nc2.*;
import ucar.nc2.ft.*;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Station;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

/**
 * Write a CF "Discrete Sample" station file.
 *
 * @author caron
 * @since Aug 19, 2009
 */
public class WriterCFStationCollection {
  private static final String recordDimName = "record";
  private static final String stationDimName = "station";
  private static final String latName = "latitude";
  private static final String lonName = "longitude";
  private static final String altName = "altitude";
  private static final String timeName = "time";
  private static final String idName = "station_id";
  private static final String descName = "station_description";
  private static final String wmoName = "wmo_id";
  private static final String stationIndexName = "stationIndex";
  private static final boolean debug = false;

  private int name_strlen = 1, desc_strlen = 1, wmo_strlen = 1;

  private NetcdfFileWriteable ncfile;
  private String title;

  private Set<Dimension> dimSet = new HashSet<Dimension>(20);
  private List<Dimension> stationDims = new ArrayList<Dimension>(1);
  private Date minDate = null;
  private Date maxDate = null;

  private boolean useAlt = false;
  private boolean useWmoId = false;


  public WriterCFStationCollection(String fileOut, String title) throws IOException {
    ncfile = NetcdfFileWriteable.createNew(fileOut, false);
    ncfile.setFill( false);
    this.title = title;
  }

  public void setLength(long size) {
    ncfile.setLength( size);
  }

  public void writeHeader(List<ucar.unidata.geoloc.Station> stns, List<VariableSimpleIF> vars, DateUnit timeUnit) throws IOException {
    createGlobalAttributes();
    createStations(stns);
    createObsVariables(timeUnit);

    // dummys, update in finish()
    ncfile.addGlobalAttribute("time_coverage_start", CalendarDateFormatter.toDateStringPresent());
    ncfile.addGlobalAttribute("time_coverage_end",  CalendarDateFormatter.toDateStringPresent());
    createDataVariables(vars);

    ncfile.create(); // done with define mode

    writeStationData(stns); // write out the station info

    // now write the observations
    if (! (Boolean) ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE))
      throw new IllegalStateException("can't add record variable");
  }

  private void createGlobalAttributes() {
    ncfile.addGlobalAttribute(CDM.CONVENTIONS, "CF-1.6");
    ncfile.addGlobalAttribute(CF.FEATURE_TYPE, CF.FeatureType.timeSeries.name());
    ncfile.addGlobalAttribute("title", title);
    ncfile.addGlobalAttribute("desc", "Written by TDS/CDM Remote Feature subset service");
  }

  private void createStations(List<ucar.unidata.geoloc.Station> stnList) throws IOException {
    int nstns = stnList.size();

    // see if there's altitude, wmoId for any stations
    for (int i = 0; i < nstns; i++) {
      ucar.unidata.geoloc.Station stn = stnList.get(i);

      if (!Double.isNaN(stn.getAltitude()))
        useAlt = true;
      if ((stn.getWmoId() != null) && (stn.getWmoId().trim().length() > 0))
        useWmoId = true;
    }

    /* if (useAlt)
      ncfile.addGlobalAttribute("altitude_coordinate", altName); */

    // find string lengths
    for (int i = 0; i < nstns; i++) {
      ucar.unidata.geoloc.Station station = stnList.get(i);
      name_strlen = Math.max(name_strlen, station.getName().length());
      desc_strlen = Math.max(desc_strlen, station.getDescription().length());
      if (useWmoId) wmo_strlen = Math.max(wmo_strlen, station.getWmoId().length());
    }

    LatLonRect llbb = getBoundingBox(stnList);
    ncfile.addGlobalAttribute("geospatial_lat_min", Double.toString(llbb.getLowerLeftPoint().getLatitude()));
    ncfile.addGlobalAttribute("geospatial_lat_max", Double.toString(llbb.getUpperRightPoint().getLatitude()));
    ncfile.addGlobalAttribute("geospatial_lon_min", Double.toString(llbb.getLowerLeftPoint().getLongitude()));
    ncfile.addGlobalAttribute("geospatial_lon_max", Double.toString(llbb.getUpperRightPoint().getLongitude()));

    // add the dimensions
    ncfile.addUnlimitedDimension(recordDimName);
    Dimension stationDim = ncfile.addDimension(stationDimName, nstns);
    stationDims.add(stationDim);

    // add the station Variables using the station dimension
    Variable v = ncfile.addVariable(latName, DataType.DOUBLE, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute(CDM.UNITS, "degrees_north"));
    ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "station latitude"));

    v = ncfile.addVariable(lonName, DataType.DOUBLE, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute(CDM.UNITS, "degrees_east"));
    ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "station longitude"));

    if (useAlt) {
      v = ncfile.addVariable(altName, DataType.DOUBLE, stationDimName);
      ncfile.addVariableAttribute(v, new Attribute(CDM.UNITS, "meters"));
      ncfile.addVariableAttribute(v, new Attribute(CF.POSITIVE, CF.POSITIVE_UP));
      ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "station altitude"));
    }

    v = ncfile.addStringVariable(idName, stationDims, name_strlen);
    ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "station identifier"));
    ncfile.addVariableAttribute(v, new Attribute("standard_name", "station_id"));

    v = ncfile.addStringVariable(descName, stationDims, desc_strlen);
    ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "station description"));

    if (useWmoId) {
      v = ncfile.addStringVariable(wmoName, stationDims, wmo_strlen);
      ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "station WMO id"));
    }
  }

  private void createObsVariables(DateUnit timeUnit) throws IOException {
    // time variable
    Variable timeVar = ncfile.addVariable(timeName, DataType.DOUBLE, recordDimName);
    ncfile.addVariableAttribute(timeVar, new Attribute(CDM.UNITS, timeUnit.getUnitsString()));
    ncfile.addVariableAttribute(timeVar, new Attribute(CDM.LONG_NAME, "time of measurement"));

    Variable v = ncfile.addVariable(stationIndexName, DataType.INT, recordDimName);
    ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "station index for this observation record"));
    ncfile.addVariableAttribute(v, new Attribute("standard_name", "ragged_parentIndex"));
  }

  private void createDataVariables(List<VariableSimpleIF> dataVars) throws IOException {
    String coordNames = latName +" "+ lonName +" " + altName +" " + timeName;

    // find all dimensions needed by the data variables
    for (VariableSimpleIF var : dataVars) {
      List<Dimension> dims = var.getDimensions();
      dimSet.addAll(dims);
    }

    // add them
    for (Dimension d : dimSet) {
      if (!d.isUnlimited())
        ncfile.addDimension(d.getName(), d.getLength(), d.isShared(), false, d.isVariableLength());
    }

    // add the data variables all using the record dimension
    for (VariableSimpleIF oldVar : dataVars) {
      List<Dimension> dims = oldVar.getDimensions();
      StringBuilder dimNames = new StringBuilder(recordDimName);
      for (Dimension d : dims) {
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getName());
      }
      Variable newVar = ncfile.addVariable(oldVar.getShortName(), oldVar.getDataType(), dimNames.toString());

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts) {
        newVar.addAttribute( att);
      }
      newVar.addAttribute( new Attribute(CF.COORDINATES, coordNames));
    }


  }

  private HashMap<String, Integer> stationMap;

  private void writeStationData(List<ucar.unidata.geoloc.Station> stnList) throws IOException {
    int nstns = stnList.size();
    stationMap = new HashMap<String, Integer>(2 * nstns);
    if (debug) System.out.println("stationMap created");

    // now write the station data
    ArrayDouble.D1 latArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 lonArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 altArray = new ArrayDouble.D1(nstns);
    ArrayObject.D1 idArray = new ArrayObject.D1(String.class, nstns);
    ArrayObject.D1 descArray = new ArrayObject.D1(String.class, nstns);
    ArrayObject.D1 wmoArray = new ArrayObject.D1(String.class, nstns);

    for (int i = 0; i < stnList.size(); i++) {
      ucar.unidata.geoloc.Station stn = stnList.get(i);
      stationMap.put(stn.getName(), i);

      latArray.set(i, stn.getLatitude());
      lonArray.set(i, stn.getLongitude());
      if (useAlt) altArray.set(i, stn.getAltitude());

      idArray.set(i, stn.getName());
      descArray.set(i, stn.getDescription());
      if (useWmoId) wmoArray.set(i, stn.getWmoId());
    }

    try {
      ncfile.write(latName, latArray);
      ncfile.write(lonName, lonArray);
      if (useAlt) ncfile.write(altName, altArray);
      ncfile.writeStringData(idName, idArray);
      ncfile.writeStringData(descName, descArray);
      if (useWmoId) ncfile.writeStringData(wmoName, wmoArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private void writeDataFinish() throws IOException {
    /* ArrayInt.D1 nextChildArray = new ArrayInt.D1(recno);

    int nstns = stnList.size();
    ArrayInt.D1 firstArray = new ArrayInt.D1(nstns);
    ArrayInt.D1 lastArray = new ArrayInt.D1(nstns);
    ArrayInt.D1 numArray = new ArrayInt.D1(nstns);

    for (int i = 0; i < stnList.size(); i++) {
      ucar.unidata.geoloc.Station stn = stnList.get(i);
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
    }   */

    // if there is no data
    if (minDate == null) minDate = new Date();
    if (maxDate == null) maxDate = new Date();

    ncfile.updateAttribute(null, new Attribute("time_coverage_start",  CalendarDateFormatter.toDateTimeString(minDate)));
    ncfile.updateAttribute(null, new Attribute("time_coverage_end",  CalendarDateFormatter.toDateTimeString(maxDate)));

  }

  private int recno = 0;
  private ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);
  //private ArrayInt.D1 prevArray = new ArrayInt.D1(1);
  private ArrayInt.D1 parentArray = new ArrayInt.D1(1);
  private int[] origin = new int[1];

  public void writeRecord(Station s, PointFeature sobs, StructureData sdata) throws IOException {
    writeRecord(s.getName(), sobs.getObservationTime(), sobs.getObservationTimeAsDate(), sdata);
  }

  public void writeRecord(String stnName, double timeCoordValue, Date obsDate, StructureData sdata) throws IOException {
    Integer parentIndex = stationMap.get(stnName);
    if (parentIndex == null)
      throw new RuntimeException("Cant find station "+stnName);

    // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
    ArrayStructureW sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
    sArray.setStructureData(sdata, 0);

    // date is handled specially
    if ((minDate == null) || minDate.after(obsDate)) minDate = obsDate;
    if ((maxDate == null) || maxDate.before(obsDate)) maxDate = obsDate;

    timeArray.set(0, timeCoordValue);
    parentArray.set(0, parentIndex);

    // write the recno record
    origin[0] = recno;
    try {
      ncfile.write("record", origin, sArray);
      ncfile.write(timeName, origin, timeArray);
      ncfile.write(stationIndexName, origin, parentArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    recno++;
  }

  public void finish() throws IOException {
    writeDataFinish();
    ncfile.close();
  }

  private LatLonRect getBoundingBox(List stnList) {
    ucar.unidata.geoloc.Station s = (ucar.unidata.geoloc.Station) stnList.get(0);
    LatLonPointImpl llpt = new LatLonPointImpl();
    llpt.set(s.getLatitude(), s.getLongitude());
    LatLonRect rect = new LatLonRect(llpt, .001, .001);

    for (int i = 1; i < stnList.size(); i++) {
      s = (ucar.unidata.geoloc.Station) stnList.get(i);
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
  }


  public static void main(String args[]) throws IOException {
    long start = System.currentTimeMillis();

    String location = "C:/data/metars/Surface_METAR_20070513_0000.nc";
    Formatter errlog = new Formatter();
    FeatureDatasetPoint fd = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.STATION, location, null, errlog);
    StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) fd.getPointFeatureCollectionList().get(0);

    String fileOut = "C:/temp/Surface_METAR_20070513_0000.rewrite.nc";
    WriterCFStationDataset writer = new WriterCFStationDataset(fileOut, "test");

    List stns = sfc.getStations();
    List<ucar.unidata.geoloc.Station> stnList = new ArrayList<ucar.unidata.geoloc.Station>();
    ucar.unidata.geoloc.Station s = (ucar.unidata.geoloc.Station) stns.get(0);
    stnList.add(s);

    List<VariableSimpleIF> varList = new ArrayList<VariableSimpleIF>();
    varList.add(fd.getDataVariable("wind_speed"));

    writer.writeHeader(stnList, varList);

    DataIterator iter = sobs.getDataIterator(s);
    while (iter.hasNext()) {
      StationObsDatatype sobsData = (StationObsDatatype) iter.nextData();
      StructureData data = sobsData.getData();
      writer.writeRecord(sobsData, data);
    }

    writer.finish();

    long took = System.currentTimeMillis() - start;
    System.out.println("That took = " + took);
  }

  public static void rewrite(String fileIn, String fileOut) throws IOException {
    long start = System.currentTimeMillis();

    // do it in memory for speed
    NetcdfFile ncfile = NetcdfFile.openInMemory(fileIn);
    NetcdfDataset ncd = new NetcdfDataset( ncfile);

    StringBuilder errlog = new StringBuilder();
    StationObsDataset sobs = (StationObsDataset) TypedDatasetFactory.open(FeatureType.STATION, ncd, null, errlog);

    List<ucar.unidata.geoloc.Station> stns = sobs.getStations();
    List<VariableSimpleIF> vars = sobs.getDataVariables();

    WriterCFStationDataset writer = new WriterCFStationDataset(fileOut, "rewrite "+fileIn);
    File f = new File( fileIn);
    writer.setLength(f.length());
    writer.writeHeader(stns, vars);

    for (ucar.unidata.geoloc.Station s : stns) {
      DataIterator iter = sobs.getDataIterator(s);
      while (iter.hasNext()) {
        StationObsDatatype sobsData = (StationObsDatatype) iter.nextData();
        StructureData data = sobsData.getData();
        writer.writeRecord(sobsData, data);
      }
    }

    writer.finish();

    long took = System.currentTimeMillis() - start;
    System.out.println("Rewrite " + fileIn+" to "+fileOut+ " took = " + took);
  }

  public static void main2(String args[]) throws IOException {
    long start = System.currentTimeMillis();
    String toLocation = "C:/temp2/";
    String fromLocation = "C:/data/metars/";

    if (args.length > 1) {
      fromLocation = args[0];
      toLocation = args[1];
    }
    System.out.println("Rewrite .nc files from "+fromLocation+" to "+toLocation);

    File dir = new File(fromLocation);
    File[] files = dir.listFiles();
    for (File file : files) {
      if (file.getName().endsWith(".nc"))
        rewrite(file.getAbsolutePath(), toLocation + file.getName());
    }

    long took = System.currentTimeMillis() - start;
    System.out.println("That took = " + took);

  }  */
}