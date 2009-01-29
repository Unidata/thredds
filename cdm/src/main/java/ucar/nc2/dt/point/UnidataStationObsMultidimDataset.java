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
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

/**
 * This handles station datasets in "Unidata Observation Dataset v1.0" implemented as multidimensional Structures.
 * For linked or contiguous lists use UnidataStationObsDataset
 *
 * @author caron
 * @since Dec 8, 2007
 */
public class UnidataStationObsMultidimDataset extends StationObsDatasetImpl implements TypedDatasetFactoryIF {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UnidataStationObsMultidimDataset.class);

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

    if (null == UnidataObsDatasetHelper.findDimension(ds, "station")) return false;

    // this field indicates a linked list
    Variable stationIndexVar = UnidataObsDatasetHelper.findVariable(ds, "parent_index");
    if (stationIndexVar != null) return false;

    return true;
  }

  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) {
    return isValidFile(ds);
  }

  public TypedDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new UnidataStationObsMultidimDataset(ncd);
  }

  public UnidataStationObsMultidimDataset() {
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Dimension stationDim, obsDim;

  private Variable latVar, lonVar, altVar, timeVar, timeNominalVar;
  private Variable stationIdVar, stationDescVar, numStationsVar;
  private StructureMembers structureMembers;

  public UnidataStationObsMultidimDataset(NetcdfDataset ds) throws IOException {
    super(ds);

    stationDim = UnidataObsDatasetHelper.findDimension(ds, "station");
    obsDim = UnidataObsDatasetHelper.findDimension(ds, "observation");
    if (obsDim == null)
      obsDim = ds.getUnlimitedDimension();
    if (obsDim == null)
      throw new IllegalStateException("must specify the observation dimension or use unlimited dimension");

    // coordinate variables
    latVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Lat);
    lonVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Lon);
    altVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Height);
    timeVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Time);
    timeNominalVar = UnidataObsDatasetHelper.findVariable(ds, "time_nominal");

    if (latVar == null)
      throw new IllegalStateException("Missing latitude variable");
    if (lonVar == null)
      throw new IllegalStateException("Missing longitude coordinate variable");
    if (timeVar == null)
      throw new IllegalStateException("Missing time coordinate variable");

    if (!latVar.getDimension(0).equals(stationDim))
      throw new IllegalStateException("latitude variable must use the station dimension");
    if (!lonVar.getDimension(0).equals(stationDim))
      throw new IllegalStateException("longitude variable must use the station dimension");
    if (!timeVar.getDimension(0).equals(stationDim))
      throw new IllegalStateException("time variable must use the station dimension");
    if ((altVar != null) && !altVar.getDimension(0).equals(stationDim))
      throw new IllegalStateException("altitude variable must use the station dimension");
    if ((timeNominalVar != null) && !timeNominalVar.getDimension(0).equals(stationDim))
      throw new IllegalStateException("timeNominal variable must use the station dimension");

    // station variables
    stationIdVar = UnidataObsDatasetHelper.findVariable(ds, "station_id");
    stationDescVar = UnidataObsDatasetHelper.findVariable(ds, "station_description");
    numStationsVar = UnidataObsDatasetHelper.findVariable(ds, "number_stations");

    if (stationIdVar == null)
      throw new IllegalStateException("Missing station id variable");
    if (!stationIdVar.getDimension(0).equals(stationDim))
      throw new IllegalStateException("stationId variable must use the station dimension");

    if ((stationDescVar != null) && !stationDescVar.getDimension(0).equals(stationDim))
      throw new IllegalStateException("stationDesc variable must use the station dimension");

    // create member variables
    structureMembers = new StructureMembers("UnidataStationObsMultidimDataset_obsStructure");
    for (Variable v : ncfile.getVariables()) {
      if (v.getRank() < 2) continue;
      if (v.getDimension(0).equals(this.stationDim) && v.getDimension(1).equals(this.obsDim)) {
        dataVariables.add(v);
        int[] shape = v.getShape();
        shape[0] = 1;
        shape[1] = 1;
        structureMembers.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
      }
    }

    readStations(); // LOOK try to defer this

    // get min, max date
    startDate = UnidataObsDatasetHelper.getStartDate(ds);
    endDate = UnidataObsDatasetHelper.getEndDate(ds);
    boundingBox = UnidataObsDatasetHelper.getBoundingBox(ds);
    if (null == boundingBox)
      setBoundingBox();

    setTimeUnits();

    title = ds.findAttValueIgnoreCase(null, "title", "");
    desc = ds.findAttValueIgnoreCase(null, "description", "");
  }

  private void readStations() throws IOException {
    // get the station info

    Array stationIdArray = stationIdVar.read();
    ArrayChar stationDescArray = null;
    if (stationDescVar != null)
      stationDescArray = (ArrayChar) stationDescVar.read();

    Array latArray = readStationVariable(latVar);
    Array lonArray = readStationVariable(lonVar);
    Array elevArray = (altVar != null) ? readStationVariable(altVar) : null;

    // how many are valid stations ?
    int n = 0;
    if (numStationsVar != null)
      n = numStationsVar.readScalarInt();
    else
      n = stationDim.getLength();

    // loop over stations
    Index ima = stationIdArray.getIndex();
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

      MStationImpl bean = new MStationImpl(stationName, stationDesc,
          latArray.getFloat(ima),
          lonArray.getFloat(ima),
          (altVar != null) ? elevArray.getFloat(ima) : Double.NaN,
          i,
          (int) obsDim.getLength());

      stations.add(bean);
    }
  }

  private Array readStationVariable(Variable svar) throws IOException {
    if (svar.getRank() == 1) return svar.read();
    if (svar.getRank() == 2) {
      int[] shape = svar.getShape();
      shape[1] = 1;
      try {
        return svar.read(new int[2], shape).reduce(1);
      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e.getMessage());
      }
    }
    throw new IllegalStateException("Station variables must have rank 1 or 2");
  }

  protected void setTimeUnits() {
    // need the time units
    String timeUnitString = ncfile.findAttValueIgnoreCase(timeVar, "units", "seconds since 1970-01-01");
    try {
      timeUnit = new DateUnit(timeUnitString);
    } catch (Exception e) {
      parseInfo.append("Error on units = ").append(timeUnitString).append(" == ").append(e.getMessage()).append("\n");
      try {
        timeUnit = new DateUnit("seconds since 1970-01-01");
      } catch (Exception e1) {
        // cant happen
      }
    }
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
    for (ucar.unidata.geoloc.Station s : getStations()) {
      MStationImpl ms = (MStationImpl) s;
      allData.addAll(ms.readObservations());
      if ((cancel != null) && cancel.isCancel())
        return null;
    }
    return allData;
  }

  public int getDataCount() {
    return stationDim.getLength() * obsDim.getLength();
  }

  public List getData(ucar.unidata.geoloc.Station s, CancelTask cancel) throws IOException {
    return ((MStationImpl) s).getObservations();
  }

  public static void main(String args[]) throws IOException {
    //String filename = "C:/data/199707010200.CHRTOUT_DOMAIN2";
    String filename = "C:/data/199707010000.LAKEOUT_DOMAIN2";
    UnidataStationObsDataset ods = new UnidataStationObsDataset(NetcdfDataset.openDataset(filename));
    StringBuffer sbuff = new StringBuffer(50 * 1000);
    ods.checkLinks(sbuff);
    System.out.println("\n\n" + sbuff.toString());
  }

  ////////////////////////////////////////////////////////

  private class MStationImpl extends StationImpl {
    int station_index;

    private MStationImpl(String name, String desc, double lat, double lon, double elev, int station_index, int count) {
      super(name, desc, lat, lon, elev, count);
      this.station_index = station_index;
    }

    protected List<StationObsDatatype> readObservations() throws IOException {
      List<StationObsDatatype> obs = new ArrayList<StationObsDatatype>();

      StationIterator siter = new StationIterator();
      while (siter.hasNext())
        obs.add((StationObsDatatype) siter.nextData());

      Collections.sort(obs);
      return obs;
    }

    DataIterator iterator() {
      return new StationIterator();
    }

    DataIterator iterator(Date start, Date end) {
      return new StationIterator(start, end);
    }

    // iterate over the data in a station
    private class StationIterator implements DataIterator {
      int obsIndex = 0;
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
        return (obsIndex < count - 1);
      }

      public Object nextData() throws IOException {
        if (!hasNext()) return null;

        MStationObsImpl sobs = new MStationObsImpl(MStationImpl.this, station_index, obsIndex, dataVariables, structureMembers);
        obsIndex++;

        if (hasDateRange) {
          double timeValue = sobs.getObservationTime();
          if ((timeValue < startTime) || (timeValue > endTime))
            return nextData(); // LOOK may be null !
        }
        return sobs;
      }

      public Object next() {
        try {
          return nextData();
        } catch (IOException e) {
          throw new IllegalStateException(e.getMessage()); // not really an illegal state...
        }
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
  }

  private class MStationObsImpl extends StationObsDatatypeImpl {
    int stationIndex, obsIndex;
    List<VariableSimpleIF> dataVariables;
    StructureMembers sm;

    MStationObsImpl(ucar.unidata.geoloc.Station s, int stationIndex, int obsIndex, List<VariableSimpleIF> dataVariables, StructureMembers sm) throws IOException {
      super(s, 0, 0);
      this.stationIndex = stationIndex;
      this.obsIndex = obsIndex;
      this.dataVariables = dataVariables;
      this.sm = sm;

      // must read the time
      Array timeData = readData(timeVar, stationIndex, obsIndex);
      obsTime = timeData.getDouble( timeData.getIndex());
    }

    public Date getNominalTimeAsDate() {
      return timeUnit.makeDate(getNominalTime());
    }

    public Date getObservationTimeAsDate() {
      return timeUnit.makeDate(getObservationTime());
    }

    public StructureData getData() throws IOException {
      StructureDataW sdata = new StructureDataW(sm);

      for (VariableSimpleIF vs : dataVariables) {
        Array data = readData((Variable) vs, stationIndex, obsIndex);
        sdata.setMemberData(vs.getShortName(), data);
      }
      return sdata;
    }
  }

  private Array readData(Variable v, int stationIndex, int obsIndex) throws IOException {
    int[] shape = v.getShape();
    int[] origin = new int[v.getRank()];
    origin[0] = stationIndex;
    origin[1] = obsIndex;
    shape[0] = 1;
    shape[1] = 1;

    Array data = null;
    try {
      data = v.read(origin, shape);
    } catch (InvalidRangeException e) {
      throw new IllegalStateException(e.getMessage());
    }
    return data;
  }

  public DataIterator getDataIterator(ucar.unidata.geoloc.Station s) {
    return ((MStationImpl) s).iterator();
  }

  public DataIterator getDataIterator(ucar.unidata.geoloc.Station s, Date start, Date end) {
    return ((MStationImpl) s).iterator(start, end);
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new DataIteratorAdapter(getData().iterator());
  }

}

