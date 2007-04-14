// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import ucar.nc2.units.DateFormatter;
import ucar.ma2.*;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.util.*;
import java.io.IOException;

import thredds.catalog.*;

/**
 * Write StationObsDataset in Unidata Station Obs COnvention.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class StationObsDatasetWriter {
  private static final String recordDimName = "record";
  private static final String stationDimName = "station";
  private static final String latName = "latitude";
  private static final String lonName = "longitude";
  private static final String altName = "altitude";
  private static final String timeName = "time";
  private static final String idName = "station_id";
  private static final String descName = "station_description";
  private static final String wmoName = "wmo_id";

  private static final String firstChildName = "firstChild";
  private static final String lastChildName = "lastChild";
  private static final String numChildName = "numChildren";
  private static final String nextChildName = "nextChild";
  private static final String prevChildName = "prevChild";
  private static final String parentName = "parent_index";
  // private static final String timeStrLenDim = "time_strlen";

  private DateFormatter dateFormatter = new DateFormatter();
  private int name_strlen, desc_strlen, wmo_strlen;

  private NetcdfFileWriteable ncfile;
  private HashSet dimSet = new HashSet();
  private ArrayList recordDims = new ArrayList();
  private ArrayList stationDims = new ArrayList();
  private List stnList;
  private Date minDate = null;
  private Date maxDate = null;

  private boolean useAlt = false;
  private boolean useWmoId = false;
  private boolean isContiguousList = false;

  public StationObsDatasetWriter(String fileOut) {
    ncfile = NetcdfFileWriteable.createNew(fileOut);
  }

  public void writeHeader(List stns, List vars) throws IOException {
    createGlobalAttributes();
    createStations(stns);

    // dummys, update in finish()
    ncfile.addGlobalAttribute("time_coverage_start", dateFormatter.toDateTimeStringISO(new Date()));
    ncfile.addGlobalAttribute("time_coverage_end", dateFormatter.toDateTimeStringISO(new Date()));

    createDataVariables(vars);

    ncfile.create(); // done with define mode

    writeStationData(stns); // write out the station info

    // now write the observations
    if (!ncfile.addRecordStructure())
      throw new IllegalStateException("can't add record variable");
  }

  private void createGlobalAttributes() {
    ncfile.addGlobalAttribute("Conventions", "Unidata Observation Dataset v1.0");
    ncfile.addGlobalAttribute("cdm_datatype", "Station");
    /* ncfile.addGlobalAttribute("observationDimension", recordDimName);
    ncfile.addGlobalAttribute("stationDimension", stationDimName);
    ncfile.addGlobalAttribute("latitude_coordinate", latName);
    ncfile.addGlobalAttribute("longitude_coordinate", lonName);
    ncfile.addGlobalAttribute("time_coordinate", timeName); */
  }

  private void createStations(List stnList) throws IOException {
    int nstns = stnList.size();

    // see if there's altitude, wmoId for any stations
    for (int i = 0; i < nstns; i++) {
      Station stn = (Station) stnList.get(i);

      if (!Double.isNaN(stn.getAltitude()))
        useAlt = true;
      if ((stn.getWmoId() != null) && (stn.getWmoId().trim().length() > 0))
        useWmoId = true;
    }

    /* if (useAlt)
      ncfile.addGlobalAttribute("altitude_coordinate", altName); */

    // find string lengths
    for (int i = 0; i < nstns; i++) {
      Station station = (Station) stnList.get(i);
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

    v = ncfile.addVariable(numChildName, DataType.INT, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "number of children in linked list for this station"));

    v = ncfile.addVariable(lastChildName, DataType.INT, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "record number of last child in linked list for this station"));

    v = ncfile.addVariable(firstChildName, DataType.INT, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "record number of first child in linked list for this station"));

    // time variable
    Variable timeVar = ncfile.addStringVariable(timeName, recordDims, 20);
    ncfile.addVariableAttribute(timeVar, new Attribute("long_name", "ISO-8601 Date"));

    v = ncfile.addVariable(prevChildName, DataType.INT, recordDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "record number of previous child in linked list"));

    v = ncfile.addVariable(parentName, DataType.INT, recordDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "index of parent station"));

    v = ncfile.addVariable(nextChildName, DataType.INT, recordDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "record number of next child in linked list"));

  }

  private void createDataVariables(List dataVars) throws IOException {

    // find all dimensions needed by the data variables
    for (int i = 0; i < dataVars.size(); i++) {
      VariableSimpleIF var = (VariableSimpleIF) dataVars.get(i);
      List dims = var.getDimensions();
      dimSet.addAll(dims);
    }

    // add them
    Iterator iter = dimSet.iterator();
    while (iter.hasNext()) {
      Dimension d = (Dimension) iter.next();
      if (!d.isUnlimited())
        ncfile.addDimension(d.getName(), d.getLength(), d.isShared(), false, d.isVariableLength());
    }

    // add the data variables all using the record dimension
    for (int i = 0; i < dataVars.size(); i++) {
      VariableSimpleIF oldVar = (VariableSimpleIF) dataVars.get(i);
      List dims = oldVar.getDimensions();
      StringBuffer dimNames = new StringBuffer(recordDimName);
      for (int j = 0; j < dims.size(); j++) {
        Dimension d = (Dimension) dims.get(j);
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getName());
      }
      Variable newVar = ncfile.addVariable(oldVar.getName(), oldVar.getDataType(), dimNames.toString());

      List atts = oldVar.getAttributes();
      for (int j = 0; j < atts.size(); j++) {
        Attribute att = (Attribute) atts.get(j);
        ncfile.addVariableAttribute(newVar, att);
      }
    }

  }

  private HashMap stationMap;
  private class StationTracker {
    int numChildren = 0;
    int lastChild = -1;
    int parent_index;
    ArrayList link = new ArrayList();
    StationTracker(int parent_index) {
      this.parent_index = parent_index;
    }
  }

  private void writeStationData(List stnList) throws IOException {
    this.stnList = stnList;
    int nstns = stnList.size();
    stationMap = new HashMap( 2*nstns);

    // now write the station data
    ArrayDouble.D1 latArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 lonArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 altArray = new ArrayDouble.D1(nstns);
    ArrayObject.D1 idArray = new ArrayObject.D1(String.class, nstns);
    ArrayObject.D1 descArray = new ArrayObject.D1(String.class, nstns);
    ArrayObject.D1 wmoArray = new ArrayObject.D1(String.class, nstns);

    for (int i = 0; i < stnList.size(); i++) {
      Station stn = (Station) stnList.get(i);
      stationMap.put( stn.getName(), new StationTracker(i));

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
    ArrayInt.D1 nextChildArray = new ArrayInt.D1(recno);

    int nstns = stnList.size();
    ArrayInt.D1 firstArray = new ArrayInt.D1(nstns);
    ArrayInt.D1 lastArray = new ArrayInt.D1(nstns);
    ArrayInt.D1 numArray = new ArrayInt.D1(nstns);

    for (int i = 0; i < stnList.size(); i++) {
      Station stn = (Station) stnList.get(i);
      StationTracker tracker = (StationTracker) stationMap.get( stn.getName());


      lastArray.set(i, tracker.lastChild);
      numArray.set(i, tracker.numChildren);

      int first = (tracker.link.size() > 0) ? ((Integer) tracker.link.get(0)).intValue() : -1;
      firstArray.set(i, first);

      if (tracker.link.size() > 0) {
        // construct forward link
        List nextList = tracker.link;
        for (int j = 0; j < nextList.size()-1; j++) {
          Integer curr = (Integer) nextList.get(j);
          Integer next = (Integer) nextList.get(j+1);
          nextChildArray.set(curr.intValue(), next.intValue());
        }
        Integer curr = (Integer) nextList.get(nextList.size()-1);
        nextChildArray.set(curr.intValue(), -1);
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
    String name = sobs.getStation().getName();
    StationTracker tracker = (StationTracker) stationMap.get(name);

    // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
    ArrayStructureW  sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
    sArray.setStructureData(sdata, 0);

    // date is handled specially
    Date obsDate = sobs.getObservationTimeAsDate();
    if ((minDate == null) || minDate.after(obsDate)) minDate = obsDate;
    if ((maxDate == null) || maxDate.before(obsDate)) maxDate = obsDate;

    timeArray.set(0, dateFormatter.toDateTimeStringISO(obsDate));
    prevArray.set(0, tracker.lastChild);
    parentArray.set(0, tracker.parent_index);
    tracker.link.add( new Integer(recno));
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
  }

  public void finish() throws IOException {
    writeDataFinish();
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

  // not tested
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
    if (!ncfile.addRecordStructure())
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

    String location = "C:/data/metars/Surface_METAR_20070331_0000.nc";
    StringBuffer errlog = new StringBuffer();
    StationObsDataset sobs = (StationObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.STATION, location, null, errlog);

    String fileOut = "C:/temp/Surface_METAR_20070331_0000.rewrite.nc";
    StationObsDatasetWriter writer = new StationObsDatasetWriter(fileOut);

    List stns = sobs.getStations();
    ArrayList stnList = new ArrayList();
    Station s = (Station) stns.get(0);
    stnList.add( s);

    ArrayList varList = new ArrayList();
    varList.add( sobs.getDataVariable("wind_speed"));

    writer.writeHeader(stnList, varList);

    DataIterator iter = sobs.getDataIterator(s);
    while (iter.hasNext()) {
      StationObsDatatype sobsData = (StationObsDatatype) iter.nextData();
      StructureData data = sobsData.getData();
      writer.writeRecord(sobsData, data);
    }

    writer.finish();
  }

}
