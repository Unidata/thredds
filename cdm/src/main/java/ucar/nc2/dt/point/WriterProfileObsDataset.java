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

package ucar.nc2.dt.point;

import ucar.nc2.dt.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.ma2.*;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.StringUtil;

import java.util.*;
import java.io.*;

/**
 * Write ProfileObsDataset in Unidata Station Obs COnvention.
 *
 * @author caron
 */
public class WriterProfileObsDataset {
  private static final String recordDimName = "record";
  private static final String stationDimName = "station";
  private static final String profileDimName = "profile";
  private static final String latName = "latitude";
  private static final String lonName = "longitude";
  private static final String altName = "altitude";
  private static final String timeName = "time";
  private static final String idName = "station_id";
  private static final String descName = "station_description";
  private static final String wmoName = "wmo_id";

  private static final String firstProfileName = "firstProfile";
  private static final String nextProfileName = "nextProfile";
  private static final String numProfilesName = "numProfiles";
  private static final String numProfilesTotalName = "numProfilesTotal";

  private static final String firstObsName = "firstChild";
  private static final String numObsName = "numChildren";
  private static final String nextObsName = "nextChild";
  private static final String parentStationIndex = "station_index";
  private static final String parentProfileIndex = "profile_index";
  // private static final String timeStrLenDim = "time_strlen";


  private DateFormatter dateFormatter = new DateFormatter();
  private int name_strlen = 1, desc_strlen = 1, wmo_strlen = 1;

  private NetcdfFileWriteable ncfile;
  private String title;

  private Set<Dimension> dimSet = new HashSet<Dimension>();
  private List<Dimension> recordDims = new ArrayList<Dimension>();
  private List<Dimension> stationDims = new ArrayList<Dimension>();
  private List<Dimension> profileDims = new ArrayList<Dimension>();
  private List<ucar.unidata.geoloc.Station> stnList;
  private Date minDate = null;
  private Date maxDate = null;

  private int nprofiles;
  private int profileIndex = 0;

  private boolean useAlt = false;
  private boolean useWmoId = false;

  private boolean debug = false;

  public WriterProfileObsDataset(String fileOut, String title) throws IOException {
    ncfile = NetcdfFileWriteable.createNew(fileOut, false);
    ncfile.setFill( false);
    this.title = title;
  }

  public void setLength(long size) {
    ncfile.setLength( size);
  }

  public void writeHeader(List<ucar.unidata.geoloc.Station> stns, List<VariableSimpleIF> vars, int nprofiles, String altVarName) throws IOException {
    createGlobalAttributes();
    createStations(stns);
    createProfiles(nprofiles);

    // dummys, update in finish()
    ncfile.addGlobalAttribute("zaxis_coordinate", altVarName);
    ncfile.addGlobalAttribute("time_coverage_start", dateFormatter.toDateTimeStringISO(new Date()));
    ncfile.addGlobalAttribute("time_coverage_end", dateFormatter.toDateTimeStringISO(new Date()));

    createDataVariables(vars);

    ncfile.create(); // done with define mode

    writeStationData(stns); // write out the station info

    // now write the observations
    if (! (Boolean) ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE))
      throw new IllegalStateException("can't add record variable");
  }

  private void createGlobalAttributes() {
    ncfile.addGlobalAttribute("Conventions", "Unidata Observation Dataset v1.0");
    ncfile.addGlobalAttribute("cdm_datatype", "Profile");
    ncfile.addGlobalAttribute("title", title);
    ncfile.addGlobalAttribute("desc", "Extracted by THREDDS/Netcdf Subset Service");
    /* ncfile.addGlobalAttribute("observationDimension", recordDimName);
    ncfile.addGlobalAttribute("stationDimension", stationDimName);
    ncfile.addGlobalAttribute("latitude_coordinate", latName);
    ncfile.addGlobalAttribute("longitude_coordinate", lonName);
    ncfile.addGlobalAttribute("time_coordinate", timeName); */
  }

  private void createStations(List<ucar.unidata.geoloc.Station> stnList) throws IOException {
    int nstns = stnList.size();

    // see if there's altitude, wmoId for any stations
    for (int i = 0; i < nstns; i++) {
      ucar.unidata.geoloc.Station stn = stnList.get(i);

      //if (!Double.isNaN(stn.getAltitude()))
      //  useAlt = true;
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
      if (useWmoId) wmo_strlen = Math.max(wmo_strlen, station.getName().length());
    }

    LatLonRect llbb = getBoundingBox(stnList);
    ncfile.addGlobalAttribute("geospatial_lat_min", Double.toString(llbb.getLowerLeftPoint().getLatitude()));
    ncfile.addGlobalAttribute("geospatial_lat_max", Double.toString(llbb.getUpperRightPoint().getLatitude()));
    ncfile.addGlobalAttribute("geospatial_lon_min", Double.toString(llbb.getLowerLeftPoint().getLongitude()));
    ncfile.addGlobalAttribute("geospatial_lon_max", Double.toString(llbb.getUpperRightPoint().getLongitude()));

    // add the dimensions
    Dimension recordDim = ncfile.addUnlimitedDimension(recordDimName);
    recordDims.add(recordDim);

    Dimension stationDim = ncfile.addDimension(stationDimName, nstns);
    stationDims.add(stationDim);

    // add the station Variables using the station dimension
    Variable v = ncfile.addVariable(latName, DataType.DOUBLE, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("units", "degrees_north"));
    ncfile.addVariableAttribute(v, new Attribute("long_name", "station latitude"));

    v = ncfile.addVariable(lonName, DataType.DOUBLE, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("units", "degrees_east"));
    ncfile.addVariableAttribute(v, new Attribute("long_name", "station longitude"));

    if (useAlt) {
      v = ncfile.addVariable(altName, DataType.DOUBLE, stationDimName);
      ncfile.addVariableAttribute(v, new Attribute("units", "meters"));
      ncfile.addVariableAttribute(v, new Attribute("long_name", "station altitude"));
    }

    v = ncfile.addStringVariable(idName, stationDims, name_strlen);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "station identifier"));

    v = ncfile.addStringVariable(descName, stationDims, desc_strlen);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "station description"));

    if (useWmoId) {
      v = ncfile.addStringVariable(wmoName, stationDims, wmo_strlen);
      ncfile.addVariableAttribute(v, new Attribute("long_name", "station WMO id"));
    }

    v = ncfile.addVariable(numProfilesName, DataType.INT, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "number of profiles in linked list for this station"));

    v = ncfile.addVariable(firstProfileName, DataType.INT, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "index of first profile in linked list for this station"));
  }

  private void createProfiles(int nprofiles) throws IOException {
    this.nprofiles = nprofiles;

    // add the dimensions
    Dimension profileDim = ncfile.addDimension(profileDimName, nprofiles);
    profileDims.add(profileDim);

    Variable v = ncfile.addVariable(numObsName, DataType.INT, profileDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "number of children in linked list for this profile"));

    v = ncfile.addVariable(numProfilesTotalName, DataType.INT, "");
    ncfile.addVariableAttribute(v, new Attribute("long_name", "number of valid profiles"));

    v = ncfile.addVariable(firstObsName, DataType.INT, profileDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "record number of first obs in linked list for this profile"));

    // time variable
    Variable timeVar = ncfile.addStringVariable(timeName, profileDims, 20);
    ncfile.addVariableAttribute(timeVar, new Attribute("long_name", "ISO-8601 Date - time of observation"));

    v = ncfile.addVariable(parentStationIndex, DataType.INT, profileDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "index of parent station"));

    v = ncfile.addVariable(nextProfileName, DataType.INT, profileDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "index of next profile in linked list for this station"));
  }



  private void createDataVariables(List<VariableSimpleIF> dataVars) throws IOException {

    /* height variable
    Variable heightVar = ncfile.addStringVariable(altName, recordDims, 20);
    ncfile.addVariableAttribute(heightVar, new Attribute("long_name", "height of observation"));
    ncfile.addVariableAttribute(heightVar, new Attribute("units", altUnits));  */

    Variable v = ncfile.addVariable(parentProfileIndex, DataType.INT, recordDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "index of parent profile"));

    v = ncfile.addVariable(nextObsName, DataType.INT, recordDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "record number of next obs in linked list for this profile"));


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
      StringBuffer dimNames = new StringBuffer(recordDimName);
      for (Dimension d : dims) {
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getName());
      }
      Variable newVar = ncfile.addVariable(oldVar.getName(), oldVar.getDataType(), dimNames.toString());

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts) {
        ncfile.addVariableAttribute(newVar, att);
      }
    }

  }

  private HashMap<String, StationTracker> stationMap;

  private class StationTracker {
    int numChildren = 0;
    int lastChild = -1;
    int parent_index;
    List<Integer> link = new ArrayList<Integer>(); // profile index
    HashMap<Date, ProfileTracker> profileMap;

    StationTracker(int parent_index) {
      this.parent_index = parent_index;
      this.profileMap = new HashMap<Date, ProfileTracker>();
    }
  }

  private class ProfileTracker {
    int numChildren = 0;
    int lastChild = -1;
    int parent_index;
    List<Integer> link = new ArrayList<Integer>(); // obs index

    ProfileTracker(int parent_index) {
      this.parent_index = parent_index;
    }
  }

  private void writeStationData(List<ucar.unidata.geoloc.Station> stnList) throws IOException {
    this.stnList = stnList;
    int nstns = stnList.size();
    stationMap = new HashMap<String, StationTracker>(2 * nstns);
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
      stationMap.put(stn.getName(), new StationTracker(i));

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
    // finish global variables
    ArrayInt.D0 totalArray = new ArrayInt.D0();
    totalArray.set(profileIndex);
    try {
      ncfile.write(numProfilesTotalName, totalArray);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    // finish the station data
    int nstns = stnList.size();
    ArrayInt.D1 firstProfileArray = new ArrayInt.D1(nstns);
    ArrayInt.D1 numProfileArray = new ArrayInt.D1(nstns);
    ArrayInt.D1 nextProfileArray = new ArrayInt.D1(nprofiles);

    for (int i = 0; i < stnList.size(); i++) {
      ucar.unidata.geoloc.Station stn = stnList.get(i);
      StationTracker tracker = stationMap.get(stn.getName());

      numProfileArray.set(i, tracker.numChildren);

      int first = (tracker.link.size() > 0) ? tracker.link.get(0) : -1;
      firstProfileArray.set(i, first);

      if (tracker.link.size() > 0) {
        // construct forward link
        List<Integer> nextList = tracker.link;
        for (int j = 0; j < nextList.size() - 1; j++) {
          Integer curr = nextList.get(j);
          Integer next = nextList.get(j + 1);
          nextProfileArray.set(curr, next);
        }
        Integer curr = nextList.get(nextList.size() - 1);
        nextProfileArray.set(curr, -1);
      }
    }

    try {
      ncfile.write(firstProfileName, firstProfileArray);
      ncfile.write(numProfilesName, numProfileArray);
      ncfile.write(nextProfileName, nextProfileArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    // finish the profile data
    ArrayInt.D1 nextObsArray = new ArrayInt.D1(recno);
    ArrayInt.D1 firstObsArray = new ArrayInt.D1(nprofiles);
    ArrayInt.D1 numObsArray = new ArrayInt.D1(nprofiles);

    for (int i = 0; i < stnList.size(); i++) {
      ucar.unidata.geoloc.Station stn = stnList.get(i);
      StationTracker stnTracker = stationMap.get(stn.getName());

      Set<Date> dates = stnTracker.profileMap.keySet();
      for (Date date : dates) {
        ProfileTracker proTracker = stnTracker.profileMap.get(date);
        int trackerIndex = proTracker.parent_index;
        numObsArray.set(trackerIndex, proTracker.numChildren);

        int first = (proTracker.link.size() > 0) ? proTracker.link.get(0) : -1;
        firstObsArray.set(trackerIndex, first);

        if (proTracker.link.size() > 0) {
          // construct forward link
          List<Integer> nextList = proTracker.link;
          for (int j = 0; j < nextList.size() - 1; j++) {
            Integer curr = nextList.get(j);
            Integer next = nextList.get(j + 1);
            nextObsArray.set(curr, next);
          }
          Integer curr = nextList.get(nextList.size() - 1);
          nextObsArray.set(curr, -1);
        }
      }
    }

    try {
      ncfile.write(firstObsName, firstObsArray);
      ncfile.write(numObsName, numObsArray);
      ncfile.write(nextObsName, nextObsArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    // finish the obs data

    ncfile.updateAttribute(null, new Attribute("time_coverage_start", dateFormatter.toDateTimeStringISO(minDate)));
    ncfile.updateAttribute(null, new Attribute("time_coverage_end", dateFormatter.toDateTimeStringISO(maxDate)));
  }

  private int recno = 0;
  private ArrayObject.D1 timeArray = new ArrayObject.D1(String.class, 1);
  private ArrayInt.D1 prevArray = new ArrayInt.D1(1);
  private ArrayInt.D1 parentArray = new ArrayInt.D1(1);
  private int[] origin = new int[1];
  private int[] originTime = new int[2];

  public void writeRecord(StationObsDatatype sobs, StructureData sdata) throws IOException {
    if (debug) System.out.println("sobs= " + sobs + "; station = " + sobs.getStation());
    writeRecord(sobs.getStation().getName(), sobs.getObservationTimeAsDate(), sdata);
  }

  public void writeRecord(String stnName, Date obsDate, StructureData sdata) throws IOException {
    StationTracker stnTracker = stationMap.get(stnName);
    ProfileTracker proTracker = stnTracker.profileMap.get(obsDate);

    if (proTracker == null) {
      proTracker = new ProfileTracker(profileIndex);
      stnTracker.profileMap.put( obsDate, proTracker);

      stnTracker.link.add(profileIndex);
      stnTracker.lastChild = profileIndex;
      stnTracker.numChildren++;

      try {
        originTime[0] = profileIndex; // 2d index
        timeArray.set(0, dateFormatter.toDateTimeStringISO(obsDate));
        parentArray.set(0, stnTracker.parent_index);
        ncfile.writeStringData(timeName, originTime, timeArray);
        ncfile.write(parentStationIndex, originTime, parentArray);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        throw new IllegalStateException(e);
      }

      profileIndex++;
    }

    // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
    ArrayStructureW sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
    sArray.setStructureData(sdata, 0);

    // track the min and max date
    if ((minDate == null) || minDate.after(obsDate)) minDate = obsDate;
    if ((maxDate == null) || maxDate.before(obsDate)) maxDate = obsDate;

    timeArray.set(0, dateFormatter.toDateTimeStringISO(obsDate));
    parentArray.set(0, proTracker.parent_index);

    proTracker.link.add(recno);
    proTracker.lastChild = recno; // not currently used
    proTracker.numChildren++;

    // write the recno record
    origin[0] = recno; // 1d index
    originTime[0] = recno; // 2d index
    try {
      ncfile.write("record", origin, sArray);
      ncfile.write(parentProfileIndex, originTime, parentArray);
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

  public static void main(String args[]) throws Exception {
    long start = System.currentTimeMillis();
    Map<String, ucar.unidata.geoloc.Station> staHash = new HashMap<String,ucar.unidata.geoloc.Station>();

    String location = "R:/testdata/sounding/netcdf/Upperair_20070401_0000.nc";
    NetcdfDataset ncfile =  NetcdfDataset.openDataset(location);
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    // look through record varibles, for those that have "manLevel" dimension
    // make a StructureData object for those
    StructureMembers sm = new StructureMembers("manLevel");
    Dimension manDim = ncfile.findDimension("manLevel");
    Structure record = (Structure) ncfile.findVariable("record");
    List<Variable> allList = record.getVariables();
    List<VariableSimpleIF> varList = new ArrayList<VariableSimpleIF>();
    for (Variable v : allList) {
      if ((v.getRank() == 1) && v.getDimension(0).equals(manDim)) {
        // public VariableDS(NetcdfDataset ds, Group group, Structure parentStructure, String shortName, DataType dataType,
        // String dims, String units, String desc) {
        varList.add( new VariableDS(ncfile, null, null, v.getShortName(), v.getDataType(), "", v.getUnitsString(), v.getDescription()));
        //(String name, String desc, String units, DataType dtype, int []shape)
        sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType() , new int[0]); // scalar
      }
    }

    ArrayStructureMA manAS = new ArrayStructureMA(sm, new int[] {manDim.getLength()} );

    // need the date units
    Variable time = ncfile.findVariable("synTime");
    String timeUnits  = ncfile.findAttValueIgnoreCase(time, "units", null);
    timeUnits = StringUtil.remove(timeUnits, '(');  // crappy fsl'ism
    timeUnits = StringUtil.remove(timeUnits, ')');
    DateUnit timeUnit = new DateUnit(timeUnits);

    // extract stations
    int nrecs = 0;
    StructureDataIterator iter = record.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sdata = iter.next();
      String name = sdata.getScalarString("staName");
      ucar.unidata.geoloc.Station s = staHash.get(name);
      if (s == null) {
        float lat = sdata.convertScalarFloat("staLat");
        float lon = sdata.convertScalarFloat("staLon");
        float elev = sdata.convertScalarFloat("staElev");
        s = new StationImpl(name, "", lat, lon, elev);
        staHash.put(name, s);
      }
      nrecs++;
    }
    List<ucar.unidata.geoloc.Station> stnList = Arrays.asList( staHash.values().toArray( new ucar.unidata.geoloc.Station[staHash.size()]) );
    Collections.sort(stnList);

    // create the writer
    WriterProfileObsDataset writer = new WriterProfileObsDataset(location+".out", "rewrite "+location);
    writer.writeHeader(stnList, varList, nrecs, "prMan");

    // extract records
    iter = record.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sdata = iter.next();
      String name = sdata.getScalarString("staName");
      double timeValue =  sdata.convertScalarDouble("synTime");
      Date date = timeUnit.makeDate(timeValue);

      // transfer to the ArrayStructure
      List<String> names = sm.getMemberNames();
      for (String mname : names) {
        manAS.setMemberArray( mname, sdata.getArray( mname));
      }

      // each level is weritten as a seperate structure
      int numMand =  sdata.getScalarInt("numMand");
      if (numMand >= manDim.getLength())
        continue;

      for (int i = 0; i < numMand; i++) {
        StructureData useData = manAS.getStructureData(i);
        writer.writeRecord(name, date , useData);
      }

    }

    writer.finish();

    long took = System.currentTimeMillis() - start;
    System.out.println("That took = " + took);
  }

}
