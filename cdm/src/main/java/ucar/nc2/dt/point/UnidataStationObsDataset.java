// $Id: UnidataStationObsDataset.java,v 1.18 2006/05/31 20:51:11 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.*;

import java.io.*;
import java.util.*;

/**
 * This handles station datasets in "Unidata Observation Dataset v1.0"
 *
 * @author John Caron
 * @version $Id: UnidataStationObsDataset.java,v 1.18 2006/05/31 20:51:11 caron Exp $
 */

public class UnidataStationObsDataset extends StationObsDatasetImpl {

  static public boolean isValidFile(NetcdfFile ds) {
    if ( !ds.findAttValueIgnoreCase(null, "cdm_datatype", "").equalsIgnoreCase("Station"))
      return false;

    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equalsIgnoreCase("Unidata Observation Dataset v1.0"))
        return true;
    }

    return false;
  }

  private Variable latVar, lonVar, altVar, timeVar, timeNominalVar;
  private Variable lastVar, prevVar, firstVar, nextVar, numChildrenVar;
  private Variable stationIndexVar, stationIdVar, stationDescVar, numStationsVar;
  private boolean isForwardLinkedList, isBackwardLinkedList, isContiguousList;
  private Structure recordVar;
  private RecordDatasetHelper recordHelper;
  private boolean debugRead = false;

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
    if (altVar == null)
      throw new IllegalStateException("Missing altitude coordinate variable");
    if  (timeVar == null)
      throw new IllegalStateException("Missing time coordinate variable");

    // variables that link the structures together
    timeNominalVar = UnidataObsDatasetHelper.findVariable( ds, "time_nominal");
    lastVar = UnidataObsDatasetHelper.findVariable( ds, "lastChild");
    prevVar = UnidataObsDatasetHelper.findVariable( ds, "prevChild");
    firstVar = UnidataObsDatasetHelper.findVariable( ds, "firstChild");
    nextVar = UnidataObsDatasetHelper.findVariable( ds, "nextChild");
    numChildrenVar = UnidataObsDatasetHelper.findVariable( ds, "numChildren");
    stationIndexVar = UnidataObsDatasetHelper.findVariable( ds, "parent_index");

    if (stationIndexVar == null)
      throw new IllegalStateException("Missing parent_index variable");

    isForwardLinkedList = (nextVar != null);
    isBackwardLinkedList = (prevVar != null);

    if (isForwardLinkedList) {
      if (firstVar == null)
        throw new IllegalStateException("Missing firstChild variable");

    } else if (isBackwardLinkedList) {
      if (lastVar == null)
        throw new IllegalStateException("Missing lastChild variable");

    } else  {
      if (firstVar == null)
        throw new IllegalStateException("Missing firstChild variable");
       if (numChildrenVar == null)
        throw new IllegalStateException("Missing numChildren variable");
      isContiguousList = true;
    }

    // station variables
    stationIdVar = UnidataObsDatasetHelper.findVariable( ds, "station_id");
    stationDescVar = UnidataObsDatasetHelper.findVariable( ds, "station_description");
    numStationsVar = UnidataObsDatasetHelper.findVariable( ds, "number_stations");

    if (stationIdVar == null)
      throw new IllegalStateException("Missing station id variable");

    // fire up the record helper - LOOK assumes its the record dimension
    recordHelper = new RecordDatasetHelper(ds, timeVar.getName(), timeNominalVar == null ? null : timeNominalVar.getName(),
        dataVariables, parseInfo);
    recordHelper.setStationInfo( stationIndexVar.getName(), stationDescVar == null ? null : stationDescVar.getName());

    removeDataVariable(timeVar.getName());
    if (timeNominalVar != null)
      removeDataVariable(timeNominalVar.getName());
    if (prevVar != null)
      removeDataVariable(prevVar.getName());
    if (nextVar != null)
      removeDataVariable(nextVar.getName());

    recordVar = recordHelper.recordVar;
    timeUnit = recordHelper.timeUnit;

    // get the station info
/*
    ArrayChar stationIdArray = (ArrayChar) stationIdVar.read();
*/
    Array stationIdArray = (Array) stationIdVar.read();
    ArrayChar stationDescArray = null;
    if (stationDescVar != null)
      stationDescArray = (ArrayChar) stationDescVar.read();

    Array latArray = latVar.read();
    Array lonArray = lonVar.read();
    Array elevArray = altVar.read();
    Array firstRecordArray = (isForwardLinkedList || isContiguousList) ? firstVar.read() : lastVar.read();

    Array numChildrenArray = null;
    if (numChildrenVar != null)
      numChildrenArray = numChildrenVar.read();

    // how many are valid stations ?
    Dimension stationDim = UnidataObsDatasetHelper.findDimension(ds, "station");
    int n = 0;
    if (numStationsVar != null)
      n = numStationsVar.readScalarInt();
    else
      n = stationDim.getLength();

    // loop over stations
    Index ima = latArray.getIndex();
    recordHelper.stnHash = new HashMap( 2*n);
    for (int i = 0; i<n; i++) {
      ima.set(i);

      String stationName;
      String stationDesc;
      if( stationIdArray instanceof ArrayChar ) {
           stationName = ((ArrayChar)stationIdArray).getString(i).trim();
           stationDesc = (stationDescVar != null) ? stationDescArray.getString(i).trim() : null;
      } else {
           stationName = (String)stationIdArray.getObject( ima );
           stationDesc = (stationDescVar != null) ? (String)stationDescArray.getObject( ima ) : null;
      }

      UnidataStationImpl bean = new UnidataStationImpl(stationName, stationDesc,
          latArray.getFloat(ima),
          lonArray.getFloat(ima),
          elevArray.getFloat(ima),
          firstRecordArray.getInt(ima),
          (numChildrenVar != null) ? numChildrenArray.getInt(ima) : -1
      );

      stations.add(bean);
      recordHelper.stnHash.put(new Integer(i), bean);
    }
    setBoundingBox();

    // get min, max date LOOK
    startDate = UnidataObsDatasetHelper.getStartDate( ds);
    endDate = UnidataObsDatasetHelper.getEndDate( ds);
    boundingBox = UnidataObsDatasetHelper.getBoundingBox( ds);

    // kludge
    if (null == startDate) {
      Variable minTimeVar = ds.findVariable("minimum_time_observation");
      int minTimeValue = minTimeVar.readScalarInt();
      startDate = timeUnit.makeDate( minTimeValue);
    }

    if (null == endDate) {
      Variable maxTimeVar = ds.findVariable("maximum_time_observation");
      int maxTimeValue = maxTimeVar.readScalarInt();
      endDate = timeUnit.makeDate( maxTimeValue);
    }

    title = ds.findAttValueIgnoreCase(null, "title", "Station Data from NWS");
    desc = ds.findAttValueIgnoreCase(null, "description", "Station Data from NWS distributed through the Unidata IDD realtime datastream. " +
      "Decoded into netCDF files by metar2nc (new)");
  }

  protected void setTimeUnits() {}
  protected void setStartDate() {}
  protected void setEndDate() {}
  protected void setBoundingBox() {
    boundingBox = stationHelper.getBoundingBox();
  }

  public List getData(CancelTask cancel) throws IOException {
    ArrayList allData = new ArrayList();
    for (int i=0; i<getDataCount(); i++) {
      allData.add( makeObs(i));
      if ((cancel != null) && cancel.isCancel())
        return null;
    }
    return allData;
  }

  public int getDataCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
  }

  public List getData( Station s, CancelTask cancel) throws IOException {
    return ((UnidataStationImpl)s).getObservations();
  }

  private class UnidataStationImpl extends StationImpl {
    private int firstRecord;
    private Variable next;

    private UnidataStationImpl( String name, String desc, double lat, double lon, double elev, int firstRecord, int count) {
      super( name, desc, lat, lon, elev, count);
      this.firstRecord = firstRecord;

      next = (isForwardLinkedList) ? nextVar : prevVar;
    }

    protected ArrayList readObservations() throws IOException {
      ArrayList obs = new ArrayList();
      int recno = firstRecord;
      int end = firstRecord + count -1;
      int nextRecord = firstRecord;

      while (recno >= 0) {
        try {
          if (debugRead) {
              System.out.println(name + " try to read at record "+recno);
          }
          StructureData sdata = recordVar.readStructure(recno);
          if( isContiguousList ) {
              if( nextRecord++ > end )
                 break;
          } else {
              nextRecord = sdata.getScalarInt( next.getName());
          }
          //float obsTime = sdata.getScalarFloat( timeVar.getName());
          float obsTime = sdata.getScalarFloat( timeVar.getShortName());
          float nomTime = (timeNominalVar != null) ? sdata.getScalarFloat( timeNominalVar.getName()) : 0.0f;

          obs.add( 0, recordHelper.new RecordStationObs( this, obsTime, nomTime, recno));
          recno = nextRecord;
        }
        catch (ucar.ma2.InvalidRangeException e) {
          e.printStackTrace();
          throw new IOException( e.getMessage());
        }
      }

      return obs;
    }
  }

  protected StationObsDatatype makeObs(int recno) throws IOException {
    try {
      StructureData sdata = recordVar.readStructure(recno);
      //System.out.println( "stationIndexVar.getName()"+ stationIndexVar.getName());
      //int stationIndex = sdata.getScalarInt(stationIndexVar.getName());
      int stationIndex = sdata.getScalarInt(stationIndexVar.getShortName());
      Station station = (Station) stations.get(stationIndex);
      if (station == null)
        parseInfo.append("cant find station at index = "+stationIndex+"\n");

      float obsTime = sdata.getScalarFloat( timeVar.getName());
      float nomTime = (timeNominalVar != null) ? sdata.getScalarFloat( timeNominalVar.getName()) : 0.0f;

      return recordHelper.new RecordStationObs( station, obsTime, nomTime, recno);

    } catch (ucar.ma2.InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException( e.getMessage());
    }
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new StationDatatypeIterator(recordHelper.recordVar, bufferSize);
  }

  private class StationDatatypeIterator extends DatatypeIterator {
    protected Object makeDatatypeWithData(int recnum, StructureData sdata) {
      return recordHelper.new RecordStationObs( recnum, sdata);
    }

    StationDatatypeIterator(Structure struct, int bufferSize) {
      super( struct, bufferSize);
    }
  }

}
