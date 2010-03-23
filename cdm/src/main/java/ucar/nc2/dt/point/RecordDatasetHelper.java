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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.StructurePseudo;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.*;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.ma2.StructureData;
import ucar.ma2.DataType;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureDataIterator;

import java.io.*;
import java.util.*;

/**
 * Helper class for using the netcdf-3 record dimension. Can be used for PointObs or StationObs.
 *
 * @deprecated use ucar.nc2.ft.*
 * @author caron
 */

public class RecordDatasetHelper {
  protected NetcdfDataset ncfile;
  protected String obsTimeVName, nomTimeVName;
  protected String stnIdVName, stnNameVName, stnDescVName;
  protected String latVName, lonVName, altVName;
  protected DataType stationIdType;

  protected Map<Object,ucar.unidata.geoloc.Station> stnHash;
  protected StructureDS recordVar;
  protected Dimension obsDim;

  protected LatLonRect boundingBox;
  protected double minDate, maxDate;
  protected DateUnit timeUnit;

  protected double altScaleFactor = 1.0;

  protected StringBuffer errs = null;
  protected boolean showErrors = true;

  /**
   * Constructor.
   * @param ncfile the netccdf file
   * @param typedDataVariables list of data variables; all record variables will be added to this list, except . You
   *    can remove extra
   * @param obsTimeVName observation time variable name (required)
   * @param nomTimeVName nominal time variable name (may be null)
   * @throws IllegalArgumentException if ncfile has no unlimited dimension.
   */
  public RecordDatasetHelper(NetcdfDataset ncfile, String obsTimeVName, String nomTimeVName, List typedDataVariables) {
    this(ncfile, obsTimeVName, nomTimeVName, typedDataVariables, null, null);
  }

  public RecordDatasetHelper( NetcdfDataset ncfile, String obsTimeVName, String nomTimeVName, List typedDataVariables,
          StringBuffer errBuffer )  {
    this( ncfile, obsTimeVName, nomTimeVName, typedDataVariables, null, errBuffer );
  }
  /**
   * Constructor.
   * @param ncfile the netccdf file
   * @param typedDataVariables list of data variables; all record variables will be added to this list, except . You
   *    can remove extra
   * @param obsTimeVName observation time variable name (required)
   * @param nomTimeVName nominal time variable name (may be null)
   * @throws IllegalArgumentException if ncfile has no unlimited dimension and recDimName is null.
   */
  public RecordDatasetHelper(NetcdfDataset ncfile, String obsTimeVName, String nomTimeVName, List typedDataVariables,
          String recDimName, StringBuffer errBuffer) {
    this.ncfile = ncfile;
    this.obsTimeVName = obsTimeVName;
    this.nomTimeVName = nomTimeVName;
    this.errs = errBuffer;

    // check if we already have a structure vs if we have to add it.

    if (this.ncfile.hasUnlimitedDimension()) {
      this.ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      this.recordVar = (StructureDS) this.ncfile.getRootGroup().findVariable("record");
      this.obsDim = ncfile.getUnlimitedDimension();

    } else {
      if (recDimName == null)
        throw new IllegalArgumentException("File <" + this.ncfile.getLocation() +
                "> has no unlimited dimension, specify psuedo record dimension with observationDimension global attribute.");
      this.obsDim = this.ncfile.getRootGroup().findDimension(recDimName);
      this.recordVar = new StructureDS( null, new StructurePseudo(this.ncfile, null, "record", obsDim)); // true
    }

    // create member variables
    List<Variable> recordMembers = ncfile.getVariables();
    for (Variable v : recordMembers) {
      if (v == recordVar) continue;
      if (v.isScalar()) continue;
      if (v.getDimension(0) == this.obsDim)
        typedDataVariables.add(v);
    }

    // need the time units
    Variable timeVar = ncfile.findVariable(obsTimeVName);
    String timeUnitString = ncfile.findAttValueIgnoreCase(timeVar, "units", "seconds since 1970-01-01");
    try {
      timeUnit = new DateUnit(timeUnitString);
    } catch (Exception e) {
      if (null != errs) errs.append("Error on string = " + timeUnitString + " == " + e.getMessage()+"\n");
      try {
        timeUnit = new DateUnit("seconds since 1970-01-01");
      } catch (Exception e1) {
        // cant happen
      }
    }
  }

  /**
   * Set extra information used by station obs datasets.
   * @param stnIdVName the obs variable that is used to find the station in the stnHash; may be type  int or a String (char).
   * @param stnDescVName optional station var containing station description
   */
  public void setStationInfo( String stnIdVName, String stnDescVName) {
    this.stnIdVName = stnIdVName;
    this.stnDescVName = stnDescVName;

    Variable stationVar = ncfile.findVariable( stnIdVName);
    stationIdType = stationVar.getDataType();
  }

  public void setLocationInfo( String latVName, String lonVName, String altVName) {
    this.latVName = latVName;
    this.lonVName = lonVName;
    this.altVName = altVName;

    // check for meter conversion
    if (altVName != null) {
      Variable v = ncfile.findVariable( altVName);
      String units = ncfile.findAttValueIgnoreCase(v, "units", null);
      if (units != null)
        try {
          altScaleFactor = PointObsDatasetImpl.getMetersConversionFactor( units);
        } catch (Exception e) {
          if (errs != null) errs.append (e.getMessage());
        }
    }
  }

  public Structure getRecordVar() {
    return( this.recordVar);
  }
  public int getRecordCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
  }

  public void setTimeUnit( DateUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  public DateUnit getTimeUnit() {
    return( this.timeUnit );
  }

  /**
   * This reads through all the records in the dataset, and constructs a list of
   * RecordPointObs or RecordStationObs. It does not cache the data.
   * <p>If stnIdVName is not null, its a StationDataset, then construct a Station HashMap of StationImpl
   * objects. Add the RecordStationObs into the list of obs for that station.
   *
   * @param cancel allow user to cancel
   * @return List of RecordPointObs or RecordStationObs
   * @throws IOException
   */
  public ArrayList readAllCreateObs(CancelTask cancel) throws IOException {

    // see if its a station or point dataset
    boolean hasStations = stnIdVName != null;
    if (hasStations)
      stnHash = new HashMap<Object,ucar.unidata.geoloc.Station>();

    // get min and max date and lat,lon
    double minDate = Double.MAX_VALUE;
    double maxDate = -Double.MAX_VALUE;

    double minLat = Double.MAX_VALUE;
    double maxLat = -Double.MAX_VALUE;

    double minLon = Double.MAX_VALUE;
    double maxLon = -Double.MAX_VALUE;

    // read all the data, create a RecordObs
    ArrayList records = new ArrayList();
    int recno = 0;
    StructureDataIterator ii = recordVar.getStructureIterator();
    while (ii.hasNext()) {
      StructureData sdata = ii.next();
      StructureMembers members = sdata.getStructureMembers();

      Object stationId = null;
      if (hasStations) {
        if ( stationIdType == DataType.INT) {
          int stationNum = sdata.getScalarInt(stnIdVName);
          stationId = new Integer(stationNum);
        } else
          stationId = sdata.getScalarString( stnIdVName).trim();
      }

      String desc = (stnDescVName == null) ? null : sdata.getScalarString(stnDescVName);
      double lat = sdata.convertScalarDouble(latVName);
      double lon = sdata.convertScalarDouble(lonVName);
      double alt = (altVName == null) ? Double.NaN : altScaleFactor * sdata.convertScalarDouble(altVName);
      double obsTime = sdata.convertScalarDouble(members.findMember( obsTimeVName));
      double nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarDouble( members.findMember( nomTimeVName));

      //double obsTime = sdata.convertScalarDouble( members.findMember( obsTimeVName) );
      //double nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarDouble( members.findMember( nomTimeVName));

      if (hasStations) {
        StationImpl stn = (StationImpl) stnHash.get(stationId);
        if (stn == null) {
          stn = new StationImpl(stationId.toString(), desc, lat, lon, alt);
          stnHash.put(stationId, stn);
        }
        RecordStationObs stnObs = new RecordStationObs( stn, obsTime, nomTime, recno);
        records.add( stnObs);
        stn.addObs( stnObs);

      } else {
        records.add( new RecordPointObs( new ucar.unidata.geoloc.EarthLocationImpl(lat, lon, alt), obsTime, nomTime, recno));
      }

      // track date range and bounding box
      minDate = Math.min( minDate, obsTime);
      maxDate = Math.max( maxDate, obsTime);

      minLat = Math.min( minLat, lat);
      maxLat = Math.max( maxLat, lat);
      minLon = Math.min( minLon, lon);
      maxLon = Math.max( maxLon, lon);

      recno++;
      if ((cancel != null) && cancel.isCancel()) return null;
    }
    boundingBox = new LatLonRect( new LatLonPointImpl( minLat, minLon), new LatLonPointImpl( maxLat, maxLon));

    return records;
  }

  // make structure variable names to shortNames so StructureData sdata can 
  // access it members
  public void setShortNames( String latVName, String lonVName, String altVName, String obsTimeVName, String nomTimeVName){
    this.latVName =  latVName;
    this.lonVName = lonVName;
    this.altVName = altVName;
    this.obsTimeVName = obsTimeVName;
    this.nomTimeVName = nomTimeVName;
  }

  private boolean debugBB = false;
  public List getData(ArrayList records, LatLonRect boundingBox, CancelTask cancel) throws IOException {
    if (debugBB) System.out.println("Want bb= "+boundingBox);
    ArrayList result = new ArrayList();
    for (int i = 0; i < records.size(); i++) {
      RecordDatasetHelper.RecordPointObs r =  (RecordDatasetHelper.RecordPointObs) records.get(i);
      if (boundingBox.contains(r.getLatLon())) {
        if (debugBB) System.out.println(" ok latlon= "+r.getLatLon());
        result.add( r);
      }
      if ((cancel != null) && cancel.isCancel()) return null;
    }
    return result;
  }

  // return List<PointObsDatatype>
  public List getData(ArrayList records, LatLonRect boundingBox, double startTime, double endTime, CancelTask cancel) throws IOException {
    if (debugBB) System.out.println("Want bb= "+boundingBox);
    ArrayList result = new ArrayList();
    for (int i = 0; i < records.size(); i++) {
      RecordDatasetHelper.RecordPointObs r =  (RecordDatasetHelper.RecordPointObs) records.get(i);
      if (boundingBox.contains(r.getLatLon())) {
        if (debugBB) System.out.println(" ok latlon= "+r.getLatLon());
        double timeValue = r.getObservationTime();
        if ((timeValue >= startTime) && (timeValue <= endTime))
          result.add( r);
      }
      if ((cancel != null) && cancel.isCancel()) return null;
    }
    return result;
  }

  //////////////////////////////////////////////////////////////////////////////////////

  public class RecordPointObs extends PointObsDatatypeImpl {
    protected int recno;
    protected LatLonPointImpl llpt = null;
    protected StructureData sdata;

    protected RecordPointObs() {
    }

    /**
     * Constructor for the case where you keep track of the location, time of each record, but not the data.
     */
    protected RecordPointObs( ucar.unidata.geoloc.EarthLocation location, double obsTime, double nomTime, int recno) {
      super( location, obsTime, nomTime);
      this.recno = recno;
    }

    /**
     * Constructor for when you already have the StructureData and want to wrap it in a StationObsDatatype
     * @param recno record number LOOK why do we need ??
     * @param sdata the structure data
     */
    public RecordPointObs(int recno, StructureData sdata) {
      this.recno = recno;
      this.sdata = sdata;

      StructureMembers members = sdata.getStructureMembers();
      obsTime = sdata.convertScalarDouble( members.findMember( obsTimeVName));
      nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarDouble( members.findMember( nomTimeVName));

      // obsTime = sdata.convertScalarDouble( members.findMember(obsTimeVName) );
      //nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarDouble( members.findMember(nomTimeVName));

      double lat = sdata.convertScalarDouble(latVName);
      double lon = sdata.convertScalarDouble(lonVName);
      double alt = (altVName == null) ? Double.NaN : altScaleFactor * sdata.convertScalarDouble(altVName);
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
      if (null != sdata) return sdata;

      try {
        return recordVar.readStructure(recno);
      } catch (ucar.ma2.InvalidRangeException e) {
        e.printStackTrace();
        throw new IOException(e.getMessage());
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////

  public class RecordStationObs extends RecordPointObs implements StationObsDatatype {
    private ucar.unidata.geoloc.Station station;

    /**
     * Constructor for the case where you keep track of the station, time of each record, but the data reading is deferred.
     * @param station data is for this Station
     * @param obsTime observation time
     * @param nomTime nominal time (may be NaN)
     * @param recno data is at this record number
     */
    protected RecordStationObs( ucar.unidata.geoloc.Station station, double obsTime, double nomTime, int recno) {
      super( station, obsTime, nomTime, recno);
      this.station = station;
    }

    /**
     * Constructor for when you already have the StructureData and want to wrap it in a StationObsDatatype
     * @param recno record number LOOK why do we need ??
     * @param sdata the structure data
     */
    protected RecordStationObs(int recno, StructureData sdata) {
      this.recno = recno;
      this.sdata = sdata;

      StructureMembers members = sdata.getStructureMembers();

      obsTime = sdata.convertScalarDouble( members.findMember( obsTimeVName));
      nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarDouble( members.findMember( nomTimeVName));

      //obsTime = sdata.convertScalarDouble( members.findMember(obsTimeVName) );
      //nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarDouble( members.findMember(nomTimeVName));

      Object stationId;
      if ( stationIdType == DataType.INT) {
        stationId = sdata.getScalarInt(stnIdVName);
      } else
        stationId = sdata.getScalarString( stnIdVName).trim();

      station = (ucar.unidata.geoloc.Station) stnHash.get( stationId);
      location = station;
      if (station == null) {
        if (null != errs)
          errs.append(" cant find station = <").append(stationId).append(">" + "when reading record ").append(recno).append("\n");
        if (showErrors)
          System.out.println(" cant find station = <"+stationId+">"+ "when reading record "+recno);
      }
    }

    /**
     * Constructor for when you already have the StructureData and want to wrap it in a StationObsDatatype
     * @param station data is for this Station
     * @param obsTime observation time
     * @param nomTime nominal time (may be NaN)
     * @param sdata the structure data
     */
    protected RecordStationObs(ucar.unidata.geoloc.Station station, double obsTime, double nomTime, StructureData sdata) {
      this.station = station;
      this.location = station;
      this.obsTime = obsTime;
      this.nomTime = nomTime;
      this.sdata = sdata;
    }

    /**
     * Constructor for when you already have the StructureData and want to wrap it in a StationObsDatatype
     * @param station data is for this Station
     * @param sdata the structure data
     */
    protected RecordStationObs(ucar.unidata.geoloc.Station station, StructureData sdata) {
      this.station = station;
      this.location = station;
      this.sdata = sdata;
      StructureMembers members = sdata.getStructureMembers();
      obsTime = sdata.convertScalarDouble( members.findMember( obsTimeVName));
      nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarDouble( members.findMember( nomTimeVName));

      //obsTime = sdata.convertScalarDouble( members.findMember(obsTimeVName) );
      //nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarDouble( members.findMember(nomTimeVName));
    }

    public ucar.unidata.geoloc.Station getStation() { return station; }

    public StructureData getData() throws IOException {
      if (null != sdata) return sdata;

      try {
        return recordVar.readStructure(recno); // note we dont cache sdata
      } catch (ucar.ma2.InvalidRangeException e) {
        e.printStackTrace();
        throw new IOException(e.getMessage());
      }
    }
  }

}
