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
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.*;

import java.io.*;
import java.util.*;
import java.text.ParseException;

/**
 * This handles station datasets in "Unidata Observation Dataset v1.0" implemented as linked or contiguous lists.
 * For "multidimensional Structures" use UnidataStationObsMultidimDataset
 *
 * @author caron
 */

public class UnidataStationObsDataset extends StationObsDatasetImpl implements TypedDatasetFactoryIF {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UnidataStationObsDataset.class);

  static public boolean isValidFile(NetcdfFile ds) {
    if (!ds.findAttValueIgnoreCase(null, "cdm_data_type", "").equalsIgnoreCase(FeatureType.STATION.toString()) &&
            !ds.findAttValueIgnoreCase(null, "cdm_datatype", "").equalsIgnoreCase(FeatureType.STATION.toString()))
      return false;

    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    boolean convOk = false;
    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equalsIgnoreCase("Unidata Observation Dataset v1.0"))
        convOk = true;
    }
    if (!convOk) return false;

    // must have this field to be a linked list
    Variable stationIndexVar = UnidataObsDatasetHelper.findVariable(ds, "parent_index");
    if (stationIndexVar == null) return false;

    return true;
  }

  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) {
    return isValidFile(ds);
  }

  public TypedDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new UnidataStationObsDataset(ncd);
  }

  public UnidataStationObsDataset() {
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Variable latVar, lonVar, altVar, timeVar, timeNominalVar;
  private Variable lastVar, prevVar, firstVar, nextVar, numChildrenVar;
  private Variable stationIndexVar, stationIdVar, stationDescVar, numStationsVar;
  private boolean isForwardLinkedList, isBackwardLinkedList, isContiguousList;
  private Structure recordVar;
  private RecordDatasetHelper recordHelper;

  public UnidataStationObsDataset(NetcdfDataset ds) throws IOException {
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
    timeNominalVar = UnidataObsDatasetHelper.findVariable(ds, "time_nominal");
    lastVar = UnidataObsDatasetHelper.findVariable(ds, "lastChild");
    prevVar = UnidataObsDatasetHelper.findVariable(ds, "prevChild");
    firstVar = UnidataObsDatasetHelper.findVariable(ds, "firstChild");
    nextVar = UnidataObsDatasetHelper.findVariable(ds, "nextChild");
    numChildrenVar = UnidataObsDatasetHelper.findVariable(ds, "numChildren");
    stationIndexVar = UnidataObsDatasetHelper.findVariable(ds, "parent_index");

    isForwardLinkedList = (firstVar != null) && (nextVar != null);
    isBackwardLinkedList = (lastVar != null) && (prevVar != null);
    isContiguousList = !isForwardLinkedList && !isBackwardLinkedList && (firstVar != null) && (numChildrenVar != null);

    if (!isForwardLinkedList && !isBackwardLinkedList && !isContiguousList) {
      if (firstVar != null)
        throw new IllegalStateException("Missing nextVar (linked list) or numChildrenVar (contiguous list) variable");
      if (lastVar != null)
        throw new IllegalStateException("Missing prevVar (linked list) variable");
      if (nextVar != null)
        throw new IllegalStateException("Missing firstVar (linked list) variable");
      if (prevVar != null)
        throw new IllegalStateException("Missing lastVar (linked list) variable");
    }

    // station variables
    stationIdVar = UnidataObsDatasetHelper.findVariable(ds, "station_id");
    stationDescVar = UnidataObsDatasetHelper.findVariable(ds, "station_description");
    numStationsVar = UnidataObsDatasetHelper.findVariable(ds, "number_stations");

    if (stationIdVar == null)
      throw new IllegalStateException("Missing station id variable");

    // fire up the record helper - LOOK assumes its the record dimension
    recordHelper = new RecordDatasetHelper(ds, timeVar.getName(), timeNominalVar == null ? null : timeNominalVar.getName(),
            dataVariables, parseInfo);
    recordHelper.setStationInfo(stationIndexVar.getName(), stationDescVar == null ? null : stationDescVar.getName());

    removeDataVariable(stationIndexVar.getName());
    removeDataVariable(timeVar.getName());
    if (timeNominalVar != null)
      removeDataVariable(timeNominalVar.getName());
    if (prevVar != null)
      removeDataVariable(prevVar.getName());
    if (nextVar != null)
      removeDataVariable(nextVar.getName());

    recordVar = recordHelper.recordVar;
    timeUnit = recordHelper.timeUnit;

    readStations(); // LOOK try to defer this

    // get min, max date
    startDate = UnidataObsDatasetHelper.getStartDate(ds);
    endDate = UnidataObsDatasetHelper.getEndDate(ds);
    boundingBox = UnidataObsDatasetHelper.getBoundingBox(ds);
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

    title = ds.findAttValueIgnoreCase(null, "title", "");
    desc = ds.findAttValueIgnoreCase(null, "description", "");
  }

  private void readStations() throws IOException {
    // get the station info

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
    recordHelper.stnHash = new HashMap<Object, ucar.unidata.geoloc.Station>(2 * n);
    for (int i = 0; i < n; i++) {
      ima.set(i);

      String stationName;
      String stationDesc;
      if (stationIdArray instanceof ArrayChar) {
        stationName = ((ArrayChar) stationIdArray).getString(i).trim();
        stationDesc = (stationDescVar != null) ? stationDescArray.getString(i) : null;
        if (stationDesc != null) stationDesc = stationDesc.trim();
      } else {
        stationName = stationIdArray.getObject(ima).toString();
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
      recordHelper.stnHash.put(i, bean);
    }
  }

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
    List<StationObsDatatype> allData = new ArrayList<StationObsDatatype>();
    int n = getDataCount();
    for (int i = 0; i < n; i++) {
      StationObsDatatype obs = makeObs(i, false, null);
      if (obs != null)
        allData.add(obs);
      if ((cancel != null) && cancel.isCancel())
        return null;
    }
    return allData;
  }

  public int getDataCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
  }

  public List getData(ucar.unidata.geoloc.Station s, CancelTask cancel) throws IOException {
    return ((UnidataStationImpl) s).getObservations();
  }

  public void checkLinks(StringBuffer sbuff) throws IOException {
    int countErrs = 0;

    if (isBackwardLinkedList) {
      Array lastArray = lastVar.read();
      Array prevArray = prevVar.read();
      Array stnArray = stationIndexVar.read();

      Index prevIndex = prevArray.getIndex();
      Index stnIndex = stnArray.getIndex();

      int stnIdx = 0;
      IndexIterator stnIter = lastArray.getIndexIterator();
      while (stnIter.hasNext()) {
        Set<Integer> records = new HashSet<Integer>(500);
        // for each station, follow the links
        int recNo = stnIter.getIntNext();
        System.out.print("Station "+stnIdx);
        while (recNo >= 0) {
          System.out.print(" "+recNo);
          records.add(recNo);
          int stnFromRecord = stnArray.getInt( stnIndex.set(recNo));
          if (stnFromRecord != stnIdx) {
            sbuff.append("recno ").append(recNo).append(" has bad station index\n");
            countErrs++;
            if (countErrs > 10) return;
          }
          // get next one
          recNo = prevArray.getInt( prevIndex.set(recNo));
          if (records.contains(recNo)) {
            sbuff.append("stn ").append(stnIdx).append(" has circular links\n");
            countErrs++;
            if (countErrs > 10) return;
            break;
          }
        }
        System.out.println();
        stnIdx++;
      }
    }
    sbuff.append("done");
  }

  public static void main(String args[]) throws IOException {
    //String filename = "C:/data/199707010200.CHRTOUT_DOMAIN2";
    String filename = "C:/data/199707010000.LAKEOUT_DOMAIN2";
    UnidataStationObsDataset ods = new UnidataStationObsDataset( NetcdfDataset.openDataset(filename));
    StringBuffer sbuff = new StringBuffer(50 * 1000);
    ods.checkLinks(sbuff);
    System.out.println("\n\n"+sbuff.toString());
  }

  ////////////////////////////////////////////////////////

  private class UnidataStationImpl extends StationImpl {
    private int firstRecord;
    private Variable next;

    private UnidataStationImpl(String name, String desc, double lat, double lon, double elev, int firstRecord, int count) {
      super(name, desc, lat, lon, elev, count);
      this.firstRecord = firstRecord;

      next = (isForwardLinkedList) ? nextVar : prevVar;
    }

    protected List<StationObsDatatype> readObservations() throws IOException {
      List<StationObsDatatype> obs = new ArrayList<StationObsDatatype>();
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
          if (isContiguousList) {
            if (nextRecord++ > end)
              break;
          } else {
            nextRecord = sdata.getScalarInt(next.getName());
          }
          double obsTime = getTime(timeVar, sdata);
          double nomTime = (timeNominalVar == null) ? obsTime : getTime(timeNominalVar, sdata);

          obs.add(recordHelper.new RecordStationObs(this, obsTime, nomTime, recno));
          recno = nextRecord;

        } catch (ucar.ma2.InvalidRangeException e) {
          log.error("UnidataStationObsDataset.readObservation recno=" + recno, e);
          throw new IOException(e.getMessage());

        } catch (ParseException e) {
          log.error("UnidataStationObsDataset.readObservation recno=" + recno, e);
          throw new IOException(e.getMessage());
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

        if (isContiguousList) {
          nextRecno++;
          if (nextRecno > last)
            nextRecno = -1;
        } else {
          nextRecno = sobs.sdata.getScalarInt(next.getName());
        }
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
          log.error("UnidataStationObsDataset.StationIterator.next recno=" + nextRecno, e);
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
        parseInfo.append("cant find station at index = ").append(stationIndex).append("\n");
        return null;
      }

      ucar.unidata.geoloc.Station station = (ucar.unidata.geoloc.Station) stations.get(stationIndex);
      if (station == null) {
        parseInfo.append("cant find station at index = ").append(stationIndex).append("\n");
        return null;
      }

      double obsTime = getTime(timeVar, sdata);
      double nomTime = (timeNominalVar == null) ? obsTime : getTime(timeNominalVar, sdata);

      if (storeData)
        return recordHelper.new RecordStationObs(station, obsTime, nomTime, sdata);
      else
        return recordHelper.new RecordStationObs(station, obsTime, nomTime, recno);

    } catch (ucar.ma2.InvalidRangeException e) {
      log.error("UnidataStationObsDataset.makeObs recno=" + recno, e);
      throw new IOException(e.getMessage());

    } catch (ParseException e) {
      log.error("UnidataStationObsDataset.makeObs recno=" + recno, e);
      throw new IOException(e.getMessage());
    }
  }

  public DataIterator getDataIterator(ucar.unidata.geoloc.Station s) {
    return ((UnidataStationImpl) s).iterator();
  }

  public DataIterator getDataIterator(ucar.unidata.geoloc.Station s, Date start, Date end) {
    return ((UnidataStationImpl) s).iterator(start, end);
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
