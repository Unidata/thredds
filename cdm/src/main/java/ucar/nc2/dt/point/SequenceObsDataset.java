// $Id: SequenceObsDataset.java 51 2006-07-12 17:13:13Z caron $
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

import ucar.nc2.*;
import ucar.nc2.dods.*;
import ucar.nc2.dt.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.StructureData;

import java.util.*;
import java.io.IOException;

/**
 * This handles DODS sequences that have station obs data.
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */

public class SequenceObsDataset extends StationObsDatasetImpl implements TypedDatasetFactoryIF  {

  static public boolean isValidFile(NetcdfFile ds) {
    if ( !ds.findAttValueIgnoreCase(null, "cdm_data_type", "").equalsIgnoreCase(FeatureType.STATION.toString()) &&
            !ds.findAttValueIgnoreCase(null, "cdm_datatype", "").equalsIgnoreCase(FeatureType.STATION.toString()))
      return false;

    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equalsIgnoreCase("Unidata Sequence Observation Dataset v1.0"))
        return true;
    }

    return false;
  }

  /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new SequenceObsDataset( ncd, task);
  }

  public SequenceObsDataset() {}

  private Variable latVar, lonVar, altVar, timeVar, timeNominalVar;
  private Variable stationIdVar, stationDescVar, numStationsVar;
  private Structure sequenceVar;
  private SequenceHelper sequenceHelper;
  private boolean debugRead = false;
  private boolean fatal = false;

  public SequenceObsDataset(NetcdfDataset ds, CancelTask task) throws IOException {
    super(ds);

    // identify key variables
    sequenceVar = (Structure) findVariable( ds, "obs_sequence");
    latVar = findVariable( ds, "latitude_coordinate");
    lonVar = findVariable( ds, "longitude_coordinate");
    altVar = findVariable( ds, "zaxis_coordinate");
    timeVar = findVariable( ds, "time_coordinate");

    if (latVar == null) {
      parseInfo.append("Missing latitude variable");
      fatal = true;
    }
    if (lonVar == null) {
      parseInfo.append("Missing longitude variable");
      fatal = true;
    }
    if (altVar == null) {
      parseInfo.append("Missing altitude variable");
    }
     if (timeVar == null) {
      parseInfo.append("Missing time variable");
      fatal = true;
    }

    // variables that link the structures together
    timeNominalVar = findVariable( ds, "time_nominal");

    // station variables
    stationIdVar = findVariable( ds, "station_id");
    stationDescVar = findVariable( ds, "station_description");
    numStationsVar = findVariable( ds, "number_stations");

    if (stationIdVar == null){
      parseInfo.append("Missing station id variable");
      fatal = true;
    }

    /* sequenceHelper = new SequenceHelper(ds, sequenceVar, latVar, lonVar, altVar, timeVar, timeNominalVar,
        dataVariables, parseInfo);
    sequenceHelper.setStationInfo( stationIdVar, stationDescVar);

    //removeDataVariable(timeVar.getName());
    timeUnit = sequenceHelper.timeUnit;

    stations = sequenceHelper.readStations( cancel);
    setBoundingBox();

    startDate = sequenceHelper.minDate;
    endDate = sequenceHelper.maxDate; */

    /*
    Variable minTimeVar = ds.findVariable("minimum_time_observation");
    int minTimeValue = minTimeVar.readScalarInt();
    startDate = timeUnit.makeDate( minTimeValue);

    Variable maxTimeVar = ds.findVariable("maximum_time_observation");
    int maxTimeValue = maxTimeVar.readScalarInt();
    endDate = timeUnit.makeDate( maxTimeValue); */

    title = ds.findAttValueIgnoreCase(null,"title","");
    desc = ds.findAttValueIgnoreCase(null,"description", "");
  }

  private Variable getCoordinate(NetcdfDataset ds, Structure sequenceVar, AxisType a) {
    List varList = ds.getVariables();
    for (int i = 0; i < varList.size(); i++) {
      Variable v = (Variable) varList.get(i);
      String axisType = ds.findAttValueIgnoreCase(v, _Coordinate.AxisType, null);
      if ((axisType != null) && axisType.equals(a.toString()))
        return v;
    }

    varList = sequenceVar.getVariables();
    for (int i = 0; i < varList.size(); i++) {
      Variable v = (Variable) varList.get(i);
      String axisType = ds.findAttValueIgnoreCase(v, _Coordinate.AxisType, null);
      if ((axisType != null) && axisType.equals(a.toString()))
        return v;
    }

    return null;
  }

  private Variable findVariable(NetcdfDataset ds, String name) {
    Variable result = ds.findVariable(name);
    if (result == null) {
      String aname = ds.findAttValueIgnoreCase(null, name+"_variable", null);
      if (aname == null)
        aname = ds.findAttValueIgnoreCase(null, name, null);
      if (aname != null)
        result = ds.findVariable(aname);
    }

    return result;
  }

  private Dimension findDimension(NetcdfDataset ds, String name) {
    Dimension result = ds.findDimension(name);
    if (result == null) {
      String aname = ds.findAttValueIgnoreCase(null, name+"Dimension", null);
      if (aname != null)
        result = ds.findDimension(aname);
    }
    return result;
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
      // allData.add( makeObs(i));
      if ((cancel != null) && cancel.isCancel())
        return null;
    }
    return allData;
  }

  public int getDataCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
  }

  public List getData( ucar.unidata.geoloc.Station s, CancelTask cancel) throws IOException {
    return null; // sequenceHelper.getData( s, cancel);
  }

  /* private class UnidataStationImpl extends StationImpl {
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

      while (recno >= 0) {
        try {
          if (debugRead) System.out.println(name + " try to read at record "+recno);
          StructureData sdata = recordVar.readStructure(recno);
          int nextRecord = sdata.getScalarInt( next.getName());
          float obsTime = sdata.getScalarFloat( timeVar.getName());
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

      int stationIndex = sdata.getScalarInt(stationIndexVar.getName());
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
  } */

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return null; // new SeqDatatypeIterator(sequenceHelper.sequenceVar, bufferSize);
  }

  private class SeqDatatypeIterator extends DatatypeIterator {
    protected Object makeDatatypeWithData(int recnum, StructureData sdata) {
      return null; // recordHelper.new RecordStationObs( recnum, sdata);
    }

    SeqDatatypeIterator(Structure struct, int bufferSize) {
      super( struct, bufferSize);
    }
  }

    public static void main(String args[]) throws IOException {
      DODSNetcdfFile.debugServerCall = true;
      DODSNetcdfFile.debugCE = true;
      DODSNetcdfFile.debugDataResult = true;
      DODSNetcdfFile.debugConvertData = true;

      NetcdfDataset ds = NetcdfDataset.openDataset("C:/data/ncml/oceanwatch.ncml");
      // System.out.println("ds= "+ ds.toString());

      new SequenceObsDataset(ds, null);

    }


}

