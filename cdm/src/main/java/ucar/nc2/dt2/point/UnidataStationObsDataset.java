/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2.dt2.point;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateRange;
import ucar.nc2.dt2.*;

import ucar.nc2.constants.DataType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.geoloc.LatLonRect;

import java.io.*;
import java.util.*;

/**
 * This handles station datasets in "Unidata Observation Dataset v1.0" implemented as linked or contiguous lists.
 * For "multidimensional Structures" use UnidataStationObsMultidimDataset
 *
 * @author caron
 */

public class UnidataStationObsDataset extends StationObsDatasetImpl {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UnidataStationObsDataset.class);

  static public boolean isValidFile(NetcdfFile ds) {
    if (!ds.findAttValueIgnoreCase(null, "cdm_data_type", "").equalsIgnoreCase(ucar.nc2.constants.DataType.STATION.toString()) &&
            !ds.findAttValueIgnoreCase(null, "cdm_datatype", "").equalsIgnoreCase(DataType.STATION.toString()))
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

  public FeatureDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    return new UnidataStationObsDataset(ncd);
  }


  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Variable lastVar;
  private Variable prevVar;
  private Variable firstVar;
  private Variable nextVar;
  private Variable stationIndexVar;
  private boolean isForwardLinkedList, isBackwardLinkedList, isContiguousList;
  private RecordDatasetHelper recordHelper;

  private DateRange filter_date;
  private LatLonRect filter_bb;
  private List<Station> filter_stations;

  // copy constructor
  UnidataStationObsDataset(UnidataStationObsDataset from, LatLonRect filter_bb, DateRange filter_date) throws IOException {
    super(from, filter_bb, filter_date);
    this.lastVar = from.lastVar;
    this.prevVar = from.prevVar;
    this.firstVar = from.firstVar;
    this.nextVar = from.nextVar;
    this.stationIndexVar = from.stationIndexVar;
    this.isForwardLinkedList = from.isForwardLinkedList;
    this.isBackwardLinkedList = from.isBackwardLinkedList;
    this.isContiguousList = from.isContiguousList;
    this.recordHelper = from.recordHelper;

    // compose bounding box filter
    if (from.filter_bb == null)
      this.filter_bb = filter_bb;
    else
      this.filter_bb = (filter_bb == null) ? from.filter_bb : from.filter_bb.intersect( filter_bb);

    if (this.filter_bb != null)
      filter_stations = stationHelper.getStations(this.filter_bb);

    // compose date range filter
    if (from.filter_date == null)
      this.filter_date = filter_date;
    else
      this.filter_date = (filter_date == null) ? from.filter_date : from.filter_date.intersect( filter_date);
  }

  public UnidataStationObsDataset() {
  }

  public UnidataStationObsDataset(NetcdfDataset ds) throws IOException {
    super(ds);

    // coordinate variables
    Variable latVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Lat);
    Variable lonVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Lon);
    Variable altVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Height);
    Variable timeVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Time);

    if (latVar == null)
      throw new IllegalStateException("Missing latitude variable");
    if (lonVar == null)
      throw new IllegalStateException("Missing longitude coordinate variable");
    if (timeVar == null)
      throw new IllegalStateException("Missing time coordinate variable");

    // variables that link the structures together
    Variable timeNominalVar = UnidataObsDatasetHelper.findVariable(ds, "time_nominal");
    lastVar = UnidataObsDatasetHelper.findVariable(ds, "lastChild");
    prevVar = UnidataObsDatasetHelper.findVariable(ds, "prevChild");
    firstVar = UnidataObsDatasetHelper.findVariable(ds, "firstChild");
    nextVar = UnidataObsDatasetHelper.findVariable(ds, "nextChild");
    Variable numChildrenVar = UnidataObsDatasetHelper.findVariable(ds, "numChildren");
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
    Variable stationIdVar = UnidataObsDatasetHelper.findVariable(ds, "station_id");
    Variable stationDescVar = UnidataObsDatasetHelper.findVariable(ds, "station_description");
    Variable numStationsVar = UnidataObsDatasetHelper.findVariable(ds, "number_stations");

    if (stationIdVar == null)
      throw new IllegalStateException("Missing station id variable");

    // fire up the record helper - LOOK assumes its the record dimension
    recordHelper = new RecordDatasetHelper(ds, timeVar.getShortName(),
        timeNominalVar == null ? null : timeNominalVar.getShortName(),
        dataVariables, null, parseInfo);
    recordHelper.setStationInfo(stationIndexVar.getShortName(),
        stationDescVar == null ? null : stationDescVar.getShortName(),
        stationIndexVar.getShortName(),
        stationHelper);

    removeDataVariable(stationIndexVar.getName());
    removeDataVariable(timeVar.getName());
    if (timeNominalVar != null)
      removeDataVariable(timeNominalVar.getName());
    if (prevVar != null)
      removeDataVariable(prevVar.getName());
    if (nextVar != null)
      removeDataVariable(nextVar.getName());

    //recordVar = recordHelper.recordVar;
    //timeUnit = recordHelper.timeUnit;

    // get min, max date
    Date startDate = UnidataObsDatasetHelper.getStartDate( ds, recordHelper.timeUnit);
    Date endDate = UnidataObsDatasetHelper.getEndDate( ds, recordHelper.timeUnit);

    // kludge Robb not following spec
    if (null == startDate) {
      Variable minTimeVar = ds.findVariable("minimum_time_observation");
      int minTimeValue = minTimeVar.readScalarInt();
      startDate = recordHelper.timeUnit.makeDate(minTimeValue);
    }
    if (null == endDate) {
      Variable maxTimeVar = ds.findVariable("maximum_time_observation");
      int maxTimeValue = maxTimeVar.readScalarInt();
      endDate = recordHelper.timeUnit.makeDate(maxTimeValue);
    }

    setDateRange( new DateRange(startDate, endDate));
    setBoundingBox( UnidataObsDatasetHelper.getBoundingBox(ds));

    title = ds.findAttValueIgnoreCase(null, "title", "");
    desc = ds.findAttValueIgnoreCase(null, "description", "");

    // get the station info
    // LOOK try to defer this ??

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
    int n;
    if (numStationsVar != null)
      n = numStationsVar.readScalarInt();
    else
      n = stationDim.getLength();

    // loop over stations
    Index ima = latArray.getIndex();
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

      stationHelper.addStation(bean);
    }
  }

  public int getDataCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
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

  public List<Station> getStations() {
    return stationHelper.getStations();
  }

  public List<Station> getStations(LatLonRect boundingBox) throws IOException {
    return stationHelper.getStations(boundingBox);
  }

  public Station getStation(String name) {
    return stationHelper.getStation(name);
  }

  public TimeSeriesCollection subset(Station s) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public TimeSeriesCollection subset(Station s, DateRange dateRange) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public StationCollection subset(List<Station> stations) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public PointCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new UnidataStationObsDataset( this, boundingBox, dateRange);
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {

    if (filter_stations != null) {
      return new StationListIterator();

    } else {
      StructureDataIterator.Filter filter = null;
      if (filter_date != null)
        filter = new StructureDataIterator.Filter() {
          public boolean filter(StructureData sdata) {
            Date d = recordHelper.getObservationDate(sdata);
            return filter_date.included(d);
          }
        };
      return new StationDatatypeIterator(recordHelper.recordVar, bufferSize, filter);
    }
  }

  public DataCost getDataCost() {
    return new DataCost(recordHelper.getRecordCount(), -1);
  }

  private class StationDatatypeIterator extends StructureDataIterator {
    protected Object makeDatatypeWithData(int recnum, StructureData sdata) {
      return recordHelper.new RecordStationObs( recnum, sdata, true);
    }
    StationDatatypeIterator(Structure struct, int bufferSize, StructureDataIterator.Filter filter) {
      super( struct, bufferSize, filter);
    }
  }

  private class StationListIterator implements DataIterator {
    Iterator<Station> stationIter;
    DataIterator dataIterator;

    StationListIterator() {
      stationIter = stationHelper.getStations().iterator();
      UnidataStationImpl stn = (UnidataStationImpl) stationIter.next();
      dataIterator = stn.iterator(filter_date);
    }

    public boolean hasNext() throws IOException {
      if (dataIterator.hasNext()) return true;
      while (stationIter.hasNext()) {
        UnidataStationImpl stn = (UnidataStationImpl) stationIter.next();
        dataIterator = stn.iterator(filter_date);
        if (dataIterator.hasNext()) return true;
      }
      return false;
    }

    public Object nextData() throws IOException {
      return dataIterator.nextData();
    }
  }

  ////////////////////////////////////////////////////////

  // a Station that can follow linked lists of observations
  private class UnidataStationImpl extends Station {
    private int firstRecord;
    private Variable next;
    private int count;

    private UnidataStationImpl(String name, String desc, double lat, double lon, double elev, int firstRecord, int count) {
      super(name, desc, lat, lon, elev);
      this.firstRecord = firstRecord;
      this.count = count;

      next = (isForwardLinkedList) ? nextVar : prevVar;
    }

    DataIterator iterator() {
      return new StationIterator();
    }

    DataIterator iterator(DateRange dateRange) {
      if (dateRange == null)
        return new StationIterator();
      else
        return new StationIterator(dateRange.getStart().getDate(), dateRange.getEnd().getDate());
    }

    // LOOK no guarentee of time ordering
    private class StationIterator implements DataIterator {
      int nextRecno = firstRecord;
      int last = firstRecord + count - 1; // contiguous only
      double startTime, endTime;
      boolean hasDateRange;

      StationIterator() {
      }

      StationIterator(Date start, Date end) {
        startTime = recordHelper.timeUnit.makeValue(start);
        endTime = recordHelper.timeUnit.makeValue(end);
        hasDateRange = true;
      }

      public boolean hasNext() {
        return (nextRecno >= 0);
      }

      public Object nextData() throws IOException {
        // deal with files that are updating
        if (nextRecno > getDataCount()) {
          int n = getDataCount();
          ncfile.syncExtend();  // LOOK kludge?
          log.info("UnidataStationObsDataset.makeObs recno=" + nextRecno + " > " + n + "; after sync= " + getDataCount());
        }

        StructureData sdata;
        try {
          sdata = recordHelper.recordVar.readStructure(nextRecno);
        } catch (ucar.ma2.InvalidRangeException e) {
          log.error("UnidataStationObsDataset.makeObs recno=" + nextRecno, e);
          throw new IOException(e.getMessage());
        }

        RecordDatasetHelper.RecordStationObs sobs = recordHelper.new RecordStationObs(UnidataStationImpl.this, sdata);
        checkStation(sdata, nextRecno);

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
    }

    private void checkStation(StructureData sdata, int recno) {
      List<Station> stations = stationHelper.getStations();
      int stationIndex = sdata.getScalarInt(stationIndexVar.getShortName());
      if (stationIndex < 0 || stationIndex >= stations.size()) {
        log.error("Illegal Station index = "+stationIndex+" when reading obs record "+recno);
      } else {
        Station station = stations.get(stationIndex);
        if (!station.getName().equals(getName()))
          log.error("Obs link doesnt match Station, index = "+stationIndex+" when reading obs record "+recno);
      }
    }
  }

  public static void main(String args[]) throws IOException {
    //String filename = "C:/data/199707010200.CHRTOUT_DOMAIN2";
    String filename = "C:/data/199707010000.LAKEOUT_DOMAIN2";
    UnidataStationObsDataset ods = new UnidataStationObsDataset( NetcdfDataset.openDataset(filename));
    StringBuffer sbuff = new StringBuffer(50 * 1000);
    ods.checkLinks(sbuff);
    System.out.println("\n\n"+sbuff.toString());
  }

}
