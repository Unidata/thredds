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
import ucar.nc2.constants.AxisType;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.units.DateUnit;
import ucar.nc2.dt.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.StructureDS;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.ma2.ArrayStructure;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureData;

import java.util.*;
import java.io.IOException;

/**
 * Handles datasets using Dapper doubley nested sequences.
 *
 * @author caron
 */
public class DapperDataset extends PointObsDatasetImpl implements TypedDatasetFactoryIF {
  static private final String ID = "_id";

  static public boolean isValidFile(NetcdfFile ds) {
    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equalsIgnoreCase("epic-insitu-1.0"))
        return true;
    }

    return false;
  }

  static public PointObsDataset factory(NetcdfDataset ds) throws IOException {
    Variable latVar = null, timeVar = null;

    // identify key variables
    List axes = ds.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      if (axis.getAxisType() == AxisType.Lat)
        latVar = axis;
      if (axis.getAxisType() == AxisType.Time)
        timeVar = axis;
    }

    // lat, lon are always in the outer; gotta use name to fetch wrapping variable
    Structure outerSequence = getWrappingParent(ds, latVar);

    // depth may be in inner or outer
    boolean isProfile = getWrappingParent(ds, timeVar) == outerSequence;
    if (isProfile)
      return new DapperPointDataset( ds);
    else
      return new DapperStationDataset( ds);
  }

  static private StructureDS getWrappingParent( NetcdfDataset ds, Variable v) {
    String name = v.getParentStructure().getName();
    return (StructureDS) ds.findVariable(name);
  }

  /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new DapperDataset( ncd);
  }

  public DapperDataset() {}

  /////////////////////////////////////////////////
  protected DODSNetcdfFile dodsFile;
  protected Variable latVar, lonVar, altVar, timeVar;
  protected StructureDS innerSequence, outerSequence;
  protected boolean isProfile = false, fatal = false;

  public DapperDataset(NetcdfDataset ds) throws IOException {
    super(ds);

    // identify key variables
    List axes = ds.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      if (axis.getAxisType() == AxisType.Lat)
        latVar = axis;
      if (axis.getAxisType() == AxisType.Lon)
        lonVar = axis;
      if (axis.getAxisType() == AxisType.Height)
        altVar = axis;
      if (axis.getAxisType() == AxisType.Time)
        timeVar = axis;
    }

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

    // lat, lon are always in the outer; gotta use name to fetch wrapping variable
    outerSequence = getWrappingParent(ds, latVar);

    // depth may be in inner or outer
    boolean isProfile = getWrappingParent(ds, timeVar) == outerSequence;
    innerSequence = isProfile ? getWrappingParent(ds, altVar) : getWrappingParent(ds, timeVar);

    // Need the underlying DODSNetcdfFile
    NetcdfFile refFile = ds.getReferencedFile();
    while (dodsFile == null) {
      if (refFile instanceof DODSNetcdfFile)
        dodsFile = (DODSNetcdfFile) refFile;
      else if (refFile instanceof NetcdfDataset)
        refFile = ((NetcdfDataset)refFile).getReferencedFile();
      else
        throw new IllegalArgumentException("Must be a DODSNetcdfFile");
    }

    // create member variables
    List recordMembers = outerSequence.getVariables();
    for (int i = 0; i < recordMembers.size(); i++) {
      Variable v = (Variable) recordMembers.get(i);
      dataVariables.add( v);
    }

    recordMembers = innerSequence.getVariables();
    for (int i = 0; i < recordMembers.size(); i++) {
      Variable v = (Variable) recordMembers.get(i);
      dataVariables.add( v);
    }

    dataVariables.remove(latVar);
    dataVariables.remove(lonVar);
    dataVariables.remove(altVar);
    dataVariables.remove(timeVar);
    dataVariables.remove(innerSequence);

    dataVariables.remove( ds.findVariable("_id"));
    dataVariables.remove( ds.findVariable("attributes"));
    dataVariables.remove( ds.findVariable("variable_attributes"));
    setBoundingBox();

    try {
      timeUnit = new DateUnit(timeVar.getUnitsString());
    } catch (Exception e) {
      parseInfo.append("Bad time units= "+ timeVar.getUnitsString());
      fatal = true;
    }

    Attribute time_range = ncfile.findGlobalAttribute("time_range");
    double time_start = time_range.getNumericValue(0).doubleValue();
    double time_end = time_range.getNumericValue(1).doubleValue();

    startDate = timeUnit.makeDate(time_start);
    endDate = timeUnit.makeDate(time_end);

    title = ds.findAttValueIgnoreCase(null,"title","");
    desc = ds.findAttValueIgnoreCase(null,"description", "");
  }

  protected void setTimeUnits() {}
  protected void setStartDate() {}
  protected void setEndDate() {}

  protected void setBoundingBox() {
    Attribute lon_range = ncfile.findGlobalAttribute("lon_range");
    double lon_start = lon_range.getNumericValue(0).doubleValue();
    double lon_end = lon_range.getNumericValue(1).doubleValue();

    Attribute lat_range = ncfile.findGlobalAttribute("lat_range");
    double lat_start = lat_range.getNumericValue(0).doubleValue();
    double lat_end = lat_range.getNumericValue(1).doubleValue();

    boundingBox = new LatLonRect(new LatLonPointImpl(lat_start, lon_start), new LatLonPointImpl(lat_end, lon_end));
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  public int getDataCount() {
    return -1;
  }

  public List getData(CancelTask cancel) throws IOException {
    String CE = outerSequence.getName();
    ArrayStructure as = (ArrayStructure) dodsFile.readWithCE(outerSequence, CE);
    extractMembers(as);
    int n = (int) as.getSize();
    List<SeqPointObs> dataList = new ArrayList<SeqPointObs>(n);
    for (int i=0; i<n; i++)
      dataList.add( new SeqPointObs( i, as.getStructureData(i)));
    return dataList;
  }

  public List getData(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    String CE = outerSequence.getName() + "&" + makeBB( boundingBox);
    ArrayStructure as = (ArrayStructure) dodsFile.readWithCE(outerSequence, CE);
    extractMembers(as);
    int n = (int) as.getSize();
    ArrayList dataList = new ArrayList(n);
    for (int i=0; i<n; i++)
      dataList.add( new SeqPointObs( i, as.getStructureData(i)));
    return dataList;

  }

  public List getData(LatLonRect boundingBox, Date start, Date end, CancelTask cancel) throws IOException {
    String CE = outerSequence.getName() + "&" + makeBB( boundingBox) + "&"+ makeTimeRange( start, end);
    ArrayStructure as = (ArrayStructure) dodsFile.readWithCE(outerSequence, CE);
    extractMembers(as);

    int n = (int) as.getSize();
    ArrayList dataList = new ArrayList(n);
    for (int i=0; i<n; i++)
      dataList.add( new SeqPointObs( i, as.getStructureData(i)));
    return dataList;
  }

  private String makeBB( LatLonRect bb) {
    return latVar.getName()+">="+bb.getLowerLeftPoint().getLatitude()+"&"+
           latVar.getName()+"<="+bb.getUpperRightPoint().getLatitude()+"&"+
           lonVar.getName()+">="+bb.getLowerLeftPoint().getLongitude()+"&"+
           lonVar.getName()+"<="+bb.getUpperRightPoint().getLongitude();
  }

  private String makeTimeRange( Date start, Date end) {
    double startValue = timeUnit.makeValue(start);
    double endValue = timeUnit.makeValue(end);
    return timeVar.getName()+">="+startValue+"&"+   // LOOK
           timeVar.getName()+"<="+endValue;
  }

  private StructureMembers.Member latMember, lonMember, innerMember, altMember, timeMember;
  private void extractMembers( ArrayStructure as) {
    StructureMembers members = as.getStructureMembers();
    latMember = members.findMember(latVar.getShortName());
    lonMember = members.findMember(lonVar.getShortName());
    innerMember = members.findMember(innerSequence.getShortName());

    StructureData first = as.getStructureData(0);
    StructureData innerFirst = first.getScalarStructure(innerMember);
    StructureMembers innerMembers = innerFirst.getStructureMembers();

    if (isProfile) {
      timeMember = members.findMember(timeVar.getShortName());
      altMember = innerMembers.findMember(altVar.getShortName());
    } else {
      timeMember = innerMembers.findMember(timeVar.getShortName());
      altMember = members.findMember(altVar.getShortName());
    }
  }

  // return List of Station
  public void readStations(List stations) throws IOException {
    String CE = latVar.getShortName()+","+lonVar.getShortName()+","+altVar.getShortName()+
      ","+ID;

    ArrayStructure as = (ArrayStructure) dodsFile.readWithCE(outerSequence, CE);
    StructureMembers members = as.getStructureMembers();
    StructureMembers.Member latMember = members.findMember(latVar.getShortName());
    StructureMembers.Member lonMember = members.findMember(lonVar.getShortName());
    StructureMembers.Member altMember = members.findMember(altVar.getShortName());
    StructureMembers.Member idMember = members.findMember(ID);

    int n = (int) as.getSize();
    for (int i=0; i<n; i++) {
      StructureData sdata = as.getStructureData(i);
      double lat = sdata.convertScalarDouble(latMember);
      double lon = sdata.convertScalarDouble(lonMember);
      double alt = sdata.convertScalarDouble(altMember);
      int id = sdata.getScalarInt(idMember);

      StationImpl s = new StationImpl(Integer.toString(id), "Station"+i,lat, lon, alt);
      stations.add(s);
    }
  }

  // return List of PointObsDatatype
  public List readStationData(ucar.unidata.geoloc.Station s, CancelTask cancel) throws IOException {
    String CE = outerSequence.getShortName()+"."+innerSequence.getShortName()+"&"+
      outerSequence.getShortName()+"."+ID+"="+s.getName();
    ArrayStructure as = (ArrayStructure) dodsFile.readWithCE(outerSequence, CE);

    /* unwrap the outer structure
    StructureMembers outerMembers = as.getStructureMembers();
    StructureMembers.Member outerMember = outerMembers.findMember(outerSequence.getShortName()); */
    StructureData outerStructure = as.getStructureData(0); 

    // get at the inner sequence
    ArrayStructure asInner = (ArrayStructure) outerStructure.getArray(innerSequence.getShortName());
    StructureMembers innerMembers = asInner.getStructureMembers();
    StructureMembers.Member timeMember = innerMembers.findMember(timeVar.getShortName());

    int n = (int) asInner.getSize();
    ArrayList stationData = new ArrayList(n);
    for (int i=0; i<n; i++) {
      StructureData sdata = asInner.getStructureData(i);
      double obsTime = sdata.convertScalarDouble(timeMember);
      stationData.add( new SeqStationObs(s, obsTime, sdata));
    }
    return stationData;
  }

  ////////////////////////////////////////////////////////////
  public class SeqPointObs extends PointObsDatatypeImpl {
    protected int recno;
    protected LatLonPointImpl llpt = null;
    protected StructureData sdata;


    /**
     * Constructor for the case where you keep track of the location, time of each record, but not the data.
     */
    protected SeqPointObs( ucar.unidata.geoloc.EarthLocation location, double obsTime, double nomTime, int recno) {
      super( location, obsTime, nomTime);
      this.recno = recno;
    }

    /**
     * Constructor for when you already have the StructureData and want to wrap it in a StationObsDatatype
     * @param recno record number LOOK why do we need ??
     * @param sdata the structure data
     */
    public SeqPointObs(int recno, StructureData sdata) {
      this.recno = recno;
      this.sdata = sdata;

      double lat = sdata.convertScalarDouble(latMember);
      double lon = sdata.convertScalarDouble(lonMember);
      StructureData inner = sdata.getScalarStructure(innerMember);

      double alt = 0.0;

      if (isProfile) {
        obsTime = sdata.convertScalarDouble(timeMember);
        alt = inner.convertScalarDouble(altMember);
      } else {
        obsTime = inner.convertScalarDouble(timeMember);
        alt = sdata.convertScalarDouble(altMember);
      }

      nomTime = obsTime;
      location = new ucar.unidata.geoloc.EarthLocationImpl( lat, lon, alt);
    }

    public LatLonPoint getLatLon() {
      if (llpt == null)
         llpt = new LatLonPointImpl( location.getLatitude(), location.getLongitude());
      return llpt;
    }

    public Date getNominalTimeAsDate() {
      return timeUnit.makeDate( getNominalTime());
    }

    public Date getObservationTimeAsDate() {
      return timeUnit.makeDate( getObservationTime());
    }

    public StructureData getData() throws IOException {
      return sdata;
    }
  }


  public class SeqStationObs extends StationObsDatatypeImpl {
    protected StructureData sdata;

    /**
     * Constructor for when you already have the StructureData and want to wrap it in a StationObsDatatype
     * @param sdata the structure data
     */
    public SeqStationObs(ucar.unidata.geoloc.Station s, double obsTime, StructureData sdata) {
      super(s, obsTime, obsTime);
      this.sdata = sdata;
    }

    public Date getNominalTimeAsDate() {
      return timeUnit.makeDate( getNominalTime());
    }

    public Date getObservationTimeAsDate() {
      return timeUnit.makeDate( getObservationTime());
    }

    public StructureData getData() throws IOException {
      return sdata;
    }
  }


  /////////////////////////

   public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new DataIteratorAdapter( getData( (CancelTask) null).iterator()); // LOOK
  }

  //////////////////////////////
  private static class DapperPointDataset extends PointObsDatasetImpl {
    DapperDataset dd;
    DapperPointDataset(NetcdfDataset ds) throws IOException {
      super(ds);
      dd = new DapperDataset(ds);
    }

    protected void setTimeUnits() {}
    protected void setStartDate() {}
    protected void setEndDate() {}
    protected void setBoundingBox() {}

    public List getData(CancelTask cancel) throws IOException {
      return dd.getData( cancel);
    }

    public int getDataCount() {
      return dd.getDataCount();
    }

    public List getData(LatLonRect boundingBox, CancelTask cancel) throws IOException {
      return dd.getData( boundingBox, cancel);
    }

    public List getData(LatLonRect boundingBox, Date start, Date end, CancelTask cancel) throws IOException {
      return dd.getData( boundingBox, start, end, cancel);
    }

    public DataIterator getDataIterator(int bufferSize) throws IOException {
      return dd.getDataIterator( bufferSize);
    }
  }

  /////////////////////////////
  private static class DapperStationDataset extends StationObsDatasetImpl {
    DapperDataset dd;
    DapperStationDataset(NetcdfDataset ds) throws IOException {
      super(ds);
      dd = new DapperDataset(ds);

      // read the stations
      dd.readStations(stations);
    }


  public List getData(ucar.unidata.geoloc.Station s, CancelTask cancel) throws IOException {
    return dd.readStationData(s, cancel);
  }

    /////////////////////////////
    protected void setTimeUnits() {}
    protected void setStartDate() {}
    protected void setEndDate() {}
    protected void setBoundingBox() {}

    public List getData(CancelTask cancel) throws IOException {
      return dd.getData( cancel);
    }

    public int getDataCount() {
      return dd.getDataCount();
    }

    public List getData(LatLonRect boundingBox, CancelTask cancel) throws IOException {
      return dd.getData( boundingBox, cancel);
    }

    public List getData(LatLonRect boundingBox, Date start, Date end, CancelTask cancel) throws IOException {
      return dd.getData( boundingBox, start, end, cancel);
    }

    public DataIterator getDataIterator(int bufferSize) throws IOException {
      return dd.getDataIterator( bufferSize);
    }

  }


  public static void main(String args[]) throws IOException {
    //String url = "http://dapper.pmel.noaa.gov/dapper/epic/puget_prof_ctd.cdp";
    String url = "http://dapper.pmel.noaa.gov/dapper/epic/woce_sl_time_monthly.cdp";
    NetcdfDataset ncd = NetcdfDataset.openDataset( url);
    DapperDataset.factory(ncd);
  }


}

