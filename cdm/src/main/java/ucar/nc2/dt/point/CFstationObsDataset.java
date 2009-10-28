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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.StructurePseudo;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.*;

import java.io.*;
import java.util.*;
import java.text.ParseException;

/**
 * This handles station datasets in "Unidata Observation Dataset v1.0"
 * Read StationObsDataset in "CF" experimental point/ungridded convention.
 *
 * @author caron
 */

public class CFstationObsDataset extends StationObsDatasetImpl implements TypedDatasetFactoryIF {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UnidataStationObsDataset.class);

  static public boolean isValidFile(NetcdfFile ds) {
    if (!ds.findAttValueIgnoreCase(null, "cdm_datatype", "").equalsIgnoreCase(FeatureType.STATION.toString()))
      return false;

    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equalsIgnoreCase("CF-1.0"))
        return true;
    }

    return false;
  }

  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) {
    return isValidFile(ds);
  }

  public TypedDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new CFstationObsDataset(ncd);
  }

  public CFstationObsDataset() {
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Variable latVar, lonVar, altVar, timeVar;
  private Variable lastVar, prevVar, firstVar, nextVar, numChildrenVar;
  private Variable stationIndexVar, stationIdVar, stationDescVar, numStationsVar;
  private boolean hasForwardLinkedList, hasBackwardLinkedList, hasContiguousList;
  private Structure recordVar;
  private RecordDatasetHelper recordHelper;
  private boolean debugRead = false;

  private int firstRecord = 0;

  public CFstationObsDataset(NetcdfDataset ds) throws IOException {
    super(ds);

    // coordinate variables
    latVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Lat);
    lonVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Lon);
    altVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Height);
    timeVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Time);

    if (latVar == null)
      throw new IllegalStateException("Missing latitude variable");
    if (lonVar == null)
      throw new IllegalStateException("Missing longitude coordinate variable");
    if (timeVar == null)
      throw new IllegalStateException("Missing time coordinate variable");

    // variables that link the structures together
    lastVar = UnidataObsDatasetHelper.findVariable(ds, "lastChild");
    prevVar = UnidataObsDatasetHelper.findVariable(ds, "prevChild");
    firstVar = UnidataObsDatasetHelper.findVariable(ds, "firstChild");
    nextVar = UnidataObsDatasetHelper.findVariable(ds, "nextChild");
    numChildrenVar = UnidataObsDatasetHelper.findVariable(ds, "numChildren");
    stationIndexVar = UnidataObsDatasetHelper.findVariable(ds, "parent_index");

    if (stationIndexVar == null)
      throw new IllegalStateException("Missing parent_index variable");

    hasForwardLinkedList = (firstVar != null) && (nextVar != null);
    hasBackwardLinkedList = (lastVar != null) && (prevVar != null);
    hasContiguousList =  (firstVar != null) && (numChildrenVar != null); // ??

    // station variables
    stationIdVar = UnidataObsDatasetHelper.findVariable(ds, "station_id");
    stationDescVar = UnidataObsDatasetHelper.findVariable(ds, "station_description");
    numStationsVar = UnidataObsDatasetHelper.findVariable(ds, "number_stations");

    if (stationIdVar == null)
      throw new IllegalStateException("Missing station id variable");

    // fire up the record helper - LOOK assumes its the record dimension
    recordHelper = new RecordDatasetHelper(ds, timeVar.getName(), null, dataVariables, parseInfo);
    recordHelper.setStationInfo(stationIndexVar.getName(), stationDescVar == null ? null : stationDescVar.getName());

    removeDataVariable(stationIndexVar.getName());
    removeDataVariable(timeVar.getName());
    if (prevVar != null)
      removeDataVariable(prevVar.getName());
    if (nextVar != null)
      removeDataVariable(nextVar.getName());

    recordVar = recordHelper.recordVar;
    timeUnit = recordHelper.timeUnit;

    title = ds.findAttValueIgnoreCase(null, "title", "");
    desc = ds.findAttValueIgnoreCase(null, "description", "");

    readStationTable();
  }

  private void readStationTable() throws IOException {
    Dimension stationDim = ncfile.findDimension("station");
    StructurePseudo stationTable = new StructurePseudo( ncfile, null, "stationTable", stationDim);
    ArrayStructure stationData = (ArrayStructure) stationTable.read();

    // how many are valid stations ?
    int nstations = 0;
    if (numStationsVar != null)
      nstations = numStationsVar.readScalarInt();
    else
      nstations = stationDim.getLength();

    recordHelper.stnHash = new HashMap<Object,ucar.unidata.geoloc.Station>(2 * nstations);
    for (int i=0; i<nstations; i++) {
      StructureData sdata = stationData.getStructureData(i);

      CFStationImpl bean = new CFStationImpl(
              sdata.getScalarString(stationIdVar.getName()),
              sdata.getScalarString(stationDescVar.getName()),
              sdata.convertScalarDouble(latVar.getName()),
              sdata.convertScalarDouble(lonVar.getName()),
              sdata.convertScalarDouble(altVar.getName())
      );

      stations.add(bean);
      recordHelper.stnHash.put(i, bean);
    }
  }

  private void readStationIndex() throws IOException {
    Array stationIndexArray = stationIndexVar.read();


    Dimension stationDim = ncfile.findDimension("station");
    StructurePseudo stationTable = new StructurePseudo( ncfile, null, "stationTable", stationDim);
    ArrayStructure stationData = (ArrayStructure) stationTable.read();

    // how many are valid stations ?
    int nstations = 0;
    if (numStationsVar != null)
      nstations = numStationsVar.readScalarInt();
    else
      nstations = stationDim.getLength();

    recordHelper.stnHash = new HashMap<Object,ucar.unidata.geoloc.Station>(2 * nstations);
    for (int i=0; i<nstations; i++) {
      StructureData sdata = stationData.getStructureData(i);

      CFStationImpl bean = new CFStationImpl(
              sdata.getScalarString(stationIdVar.getName()),
              sdata.getScalarString(stationDescVar.getName()),
              sdata.convertScalarDouble(latVar.getName()),
              sdata.convertScalarDouble(lonVar.getName()),
              sdata.convertScalarDouble(altVar.getName())
      );

      stations.add(bean);
      recordHelper.stnHash.put(i, bean);
    }
  }

  /* private void readStations() throws IOException {

    // get min, max date
    startDate = UnidataObsDatasetHelper.getStartDate(ncfile);
    endDate = UnidataObsDatasetHelper.getEndDate(ncfile);
    boundingBox = UnidataObsDatasetHelper.getBoundingBox(ncfile);
    if (null == boundingBox)
      setBoundingBox();

    // kludge Robb not following spec
    if (null == startDate) {
      Variable minTimeVar = ds.findVariable("minimum_time_observation");
      int minTimeValue = minTimeVar.readScalarInt();
      startDate = timeUnit.makeDate(minTimeValue);
    }

    if (null == endDate) {
      Variable maxTimeVar = ds.findVariable("maximum_time_observation");
      int maxTimeValue = maxTimeVar.readScalarInt();
      endDate = timeUnit.makeDate(maxTimeValue);
    }

    Array stationIdArray = stationIdVar.read();
    ArrayChar stationDescArray = null;
    if (stationDescVar != null)
      stationDescArray = (ArrayChar) stationDescVar.read();

    Array latArray = latVar.read();
    Array lonArray = lonVar.read();
    Array elevArray = (altVar != null) ? altVar.read() : null;
    Array firstRecordArray = (isForwardLinkedList || isContiguousList) ? firstVar.read() : lastVar.read();

    Array numChildrenArray = null;
    if (numChildrenVar != null)
      numChildrenArray = numChildrenVar.read();

    // how many are valid stations ?
    Dimension stationDim = UnidataObsDatasetHelper.findDimension(ncfile, "station");
    int n = 0;
    if (numStationsVar != null)
      n = numStationsVar.readScalarInt();
    else
      n = stationDim.getLength();

    // loop over stations
    Index ima = latArray.getIndex();
    recordHelper.stnHash = new HashMap(2 * n);
    for (int i = 0; i < n; i++) {
      ima.set(i);

      String stationName;
      String stationDesc;
      if (stationIdArray instanceof ArrayChar) {
        stationName = ((ArrayChar) stationIdArray).getString(i).trim();
        stationDesc = (stationDescVar != null) ? stationDescArray.getString(i) : null;
        if (stationDesc != null) stationDesc = stationDesc.trim();
      } else {
        stationName = (String) stationIdArray.getObject(ima);
        stationDesc = (stationDescVar != null) ? (String) stationDescArray.getObject(ima) : null;
      }

      UnidataStationImpl bean = new UnidataStationImpl(stationName, stationDesc,
              latArray.getFloat(ima),
              lonArray.getFloat(ima),
              (altVar != null) ? elevArray.getFloat(ima) : Double.NaN,
              firstRecordArray.getInt(ima),
              (numChildrenVar != null) ? numChildrenArray.getInt(ima) : -1
      );

      stations.add(bean);
      recordHelper.stnHash.put(new Integer(i), bean);
    }
  } */

  protected void setTimeUnits() {
  }

  protected void setStartDate() {
  }

  protected void setEndDate() {
  }

  protected void setBoundingBox() {
    boundingBox = stationHelper.getBoundingBox();
  }

  public List getData(CancelTask cancel) throws IOException {
    ArrayList allData = new ArrayList();
    int n = getDataCount();
    /*for (int i = 0; i < n; i++) {
      StationObsDatatype obs = makeObs(i, false, null);
      if (obs != null)
        allData.add(obs);
      if ((cancel != null) && cancel.isCancel())
        return null;
    }*/
    return allData;
  }

  public int getDataCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
  }

  public List getData(ucar.unidata.geoloc.Station s, CancelTask cancel) throws IOException {
    return ((CFStationImpl) s).getObservations();
  }

  private class CFStationImpl extends StationImpl {

    private CFStationImpl(String name, String desc, double lat, double lon, double elev) {
      super(name, desc, lat, lon, elev, -1);
    }

    protected ArrayList readObservations() throws IOException {
      ArrayList obs = new ArrayList();
      int recno = firstRecord;
      int end = firstRecord + count - 1;
      int nextRecord = firstRecord;

      while (recno >= 0) {
        try {
          // deal with files that are updating
          if (recno > getDataCount()) {
            int n = getDataCount();
            ncfile.syncExtend();  // LOOK kludge?
            log.info("UnidataStationObsDataset.makeObs recno=" + recno + " > " + n + "; after sync= " + getDataCount());
          }
          StructureData sdata = recordVar.readStructure(recno);
          /* if (isContiguousList) {
            if (nextRecord++ > end)
              break;
          } else {
            nextRecord = sdata.getScalarInt(next.getName());
          }
          double obsTime = getTime(timeVar, sdata);
          double nomTime = getTime(timeNominalVar, sdata); */

          //obs.add(recordHelper.new RecordStationObs(this, obsTime, nomTime, recno));
          recno = nextRecord;

        } catch (ucar.ma2.InvalidRangeException e) {
          log.error("UnidataStationObsDataset.readObservation recno=" + recno, e);
          throw new IOException(e.getMessage());

        /* } catch (ParseException e) {
          log.error("UnidataStationObsDataset.readObservation recno=" + recno, e);
          throw new IOException(e.getMessage()); */
        }
      }

      Collections.sort(obs);
      return obs;
    }

    DataIterator iterator() {
      return new StationIterator();
    }

    DataIterator iterator(Date start, Date end) {
      return new StationIterator(start, end);
    }

    private class StationIterator implements DataIterator {
      int nextRecno = firstRecord;
      int last = firstRecord + count - 1; // contiguous only
      double startTime, endTime;
      boolean hasDateRange;

      StationIterator() {
      }

      StationIterator(Date start, Date end) {
        startTime = timeUnit.makeValue(start);
        endTime = timeUnit.makeValue(end);
        hasDateRange = true;
      }

      public boolean hasNext() {
        return (nextRecno >= 0);
      }

      public Object nextData() throws IOException {
        RecordDatasetHelper.RecordStationObs sobs = makeObs(nextRecno, true, null);
        if (!sobs.getStation().getName().equals(getName()))
          throw new IllegalStateException("BAD Station link ("+nextRecno+") station name="+sobs.getStation().getName()+" should be "+getName());

        /* if (isContiguousList) {
          nextRecno++;
          if (nextRecno > last)
            nextRecno = -1;
        } else {
          nextRecno = sobs.sdata.getScalarInt(next.getName());
        } */
        if (hasDateRange) {
          double timeValue = sobs.getObservationTime();
          if ((timeValue < startTime) || (timeValue > endTime))
            return nextData();
        }
        return sobs;
      }

      public Object next() {
        try {
          return nextData();
        } catch (IOException e) {
          log.error("CFStationObsDataset.StationIterator.next recno=" + nextRecno, e);
          throw new IllegalStateException(e.getMessage()); // not really an illegal state...
        }
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
  }

  protected RecordDatasetHelper.RecordStationObs makeObs(int recno, boolean storeData, StructureData sdata) throws IOException {
    try {
      // deal with files that are updating
      if (recno > getDataCount()) {
        int n = getDataCount();
        ncfile.syncExtend();  // LOOK kludge?
        log.info("UnidataStationObsDataset.makeObs recno=" + recno + " > " + n + "; after sync= " + getDataCount());
      }

      if (null == sdata)
        sdata = recordVar.readStructure(recno);

      // find the station
      int stationIndex = sdata.getScalarInt(stationIndexVar.getShortName());
      if (stationIndex < 0 || stationIndex >= stations.size()) {
        parseInfo.append("cant find station at index = " + stationIndex + "\n");
        return null;
      }

      ucar.unidata.geoloc.Station station = (ucar.unidata.geoloc.Station) stations.get(stationIndex);
      if (station == null) {
        parseInfo.append("cant find station at index = " + stationIndex + "\n");
        return null;
      }

      double obsTime = getTime(timeVar, sdata);
      double nomTime = obsTime;

      if (storeData)
        return recordHelper.new RecordStationObs(station, obsTime, nomTime, sdata);
      else
        return recordHelper.new RecordStationObs(station, obsTime, nomTime, recno);

    } catch (ucar.ma2.InvalidRangeException e) {
      log.error("CFStationObsDataset.makeObs recno=" + recno, e);
      throw new IOException(e.getMessage());

    } catch (ParseException e) {
      log.error("CFStationObsDataset.makeObs recno=" + recno, e);
      throw new IOException(e.getMessage());
    }
  }

  public DataIterator getDataIterator(ucar.unidata.geoloc.Station s) {
    return ((CFStationImpl) s).iterator();
  }

  public DataIterator getDataIterator(ucar.unidata.geoloc.Station s, Date start, Date end) {
    return ((CFStationImpl) s).iterator(start, end);
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new StationDatatypeIterator(recordHelper.recordVar, bufferSize);
  }

  private class StationDatatypeIterator extends DatatypeIterator {

    protected Object makeDatatypeWithData(int recnum, StructureData sdata) throws IOException {
      return makeObs(recnum, true, sdata);
    }

    StationDatatypeIterator(Structure struct, int bufferSize) {
      super(struct, bufferSize);
    }
  }


}
