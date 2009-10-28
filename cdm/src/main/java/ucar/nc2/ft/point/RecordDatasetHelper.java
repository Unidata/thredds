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

package ucar.nc2.ft.point;

import ucar.ma2.StructureData;
import ucar.ma2.DataType;
import ucar.ma2.StructureMembers;

import ucar.nc2.*;
import ucar.nc2.ft.*;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.dataset.StructurePseudoDS;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateFormatter;

import ucar.unidata.geoloc.*;

import java.io.*;
import java.util.*;
import java.text.ParseException;

/**
 * Helper class for using the netcdf-3 record dimension.
 *
 * @author caron
 * @since Feb 29, 2008
 */

public class RecordDatasetHelper {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RecordDatasetHelper.class);

  protected NetcdfDataset ncfile;
  protected String obsTimeVName, nomTimeVName;
  protected String latVName, lonVName, zcoordVName, zcoordUnits;

  protected String stnIdVName, stnIndexVName, stnDescVName;
  protected StationHelper stationHelper;
  protected DataType stationIdType;

  protected StructureDS recordVar;
  protected Dimension obsDim;

  protected LatLonRect boundingBox;
  protected double minDate, maxDate;
  protected DateUnit timeUnit;

  protected double altScaleFactor = 1.0;

  protected Formatter errs = null;
  protected boolean showErrors = true;

  /**
   * Constructor.
   *
   * @param ncfile             the netccdf file
   * @param typedDataVariables list of data variables; all record variables will be added to this list, except . You
   *                           can remove extra
   * @param obsTimeVName       observation time variable name (required)
   * @param nomTimeVName       nominal time variable name (may be null)
   * @throws IllegalArgumentException if ncfile has no unlimited dimension and recDimName is null.
   */
  public RecordDatasetHelper(NetcdfDataset ncfile, String obsTimeVName, String nomTimeVName, List<VariableSimpleIF> typedDataVariables,
                             String recDimName, Formatter errBuffer) {
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
      this.recordVar = new StructurePseudoDS(this.ncfile, null, "record", null, obsDim);
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
      if (null != errs)
        errs.format("Error on string = %s == %s\n", timeUnitString, e.getMessage());
      try {
        timeUnit = new DateUnit("seconds since 1970-01-01");
      } catch (Exception e1) {
        // cant happen
      }
    }
  }

  /**
   * Set extra information used by station obs datasets.
   * Use stnIdVName or stnIndexVName.
   * @param stnIdVName   the obs variable that is used to find the station in the stnHash; may be type  int or a String (char).
   * @param stnDescVName optional station var containing station description
   */
  public void setStationInfo(String stnIdVName, String stnDescVName, String stnIndexVName, StationHelper stationHelper) {
    this.stnIdVName = stnIdVName;
    this.stnDescVName = stnDescVName;
    this.stnIndexVName = stnIndexVName;
    this.stationHelper = stationHelper;

    if (stnIdVName != null) {
      Variable stationVar = ncfile.findVariable(stnIdVName);
      stationIdType = stationVar.getDataType();
    }
  }

  public void setLocationInfo(String latVName, String lonVName, String zcoordVName) {
    this.latVName = latVName;
    this.lonVName = lonVName;
    this.zcoordVName = zcoordVName;

    // check for meter conversion
    if (zcoordVName != null) {
      Variable v = ncfile.findVariable(zcoordVName);
      zcoordUnits = ncfile.findAttValueIgnoreCase(v, "units", null);
      if (zcoordUnits != null)
        try {
          altScaleFactor = getMetersConversionFactor(zcoordUnits);
        } catch (Exception e) {
          if (errs != null) errs.format("%s", e.getMessage());
        }
    }
  }

  // make structure variable names to shortNames so StructureData sdata can
  // access it members
  public void setShortNames(String latVName, String lonVName, String altVName, String obsTimeVName, String nomTimeVName) {
    this.latVName = latVName;
    this.lonVName = lonVName;
    this.zcoordVName = altVName;
    this.obsTimeVName = obsTimeVName;
    this.nomTimeVName = nomTimeVName;
  }

  protected static double getMetersConversionFactor(String unitsString) throws Exception {
    SimpleUnit unit = SimpleUnit.factoryWithExceptions(unitsString);
    return unit.convertTo(1.0, SimpleUnit.meterUnit);
  }

  public Structure getRecordVar() {
    return (this.recordVar);
  }

  public int getRecordCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
  }

  public void setTimeUnit(DateUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  public DateUnit getTimeUnit() {
    return this.timeUnit;
  }

  public LatLonPoint getLocation(StructureData sdata) {
    StructureMembers members = sdata.getStructureMembers();
    double lat = sdata.convertScalarDouble( members.findMember( latVName));
    double lon = sdata.convertScalarDouble( members.findMember(lonVName));
    return new LatLonPointImpl(lat, lon);
  }

  public double getLatitude(StructureData sdata) {
    StructureMembers members = sdata.getStructureMembers();
    return sdata.convertScalarDouble( members.findMember( latVName));
  }

  public double getLongitude(StructureData sdata) {
    StructureMembers members = sdata.getStructureMembers();
    return sdata.convertScalarDouble( members.findMember( lonVName));
  }

  public double getZcoordinate(StructureData sdata) {
    StructureMembers members = sdata.getStructureMembers();
    return (zcoordVName == null) ? Double.NaN : sdata.convertScalarDouble(members.findMember(zcoordVName));
  }

  public String getZcoordUnits() {
    return zcoordUnits;
  }

  public Date getObservationTimeAsDate(StructureData sdata) {
    return timeUnit.makeDate( getObservationTime(sdata));
  }

  public double getObservationTime(StructureData sdata) {
    return getTime( sdata.findMember(obsTimeVName), sdata);
  }

  private DateFormatter formatter;
  private double getTime(StructureMembers.Member timeVar, StructureData sdata) {
    if (timeVar == null) return 0.0;

    if ((timeVar.getDataType() == DataType.CHAR) || (timeVar.getDataType() == DataType.STRING)) {
      String time = sdata.getScalarString(timeVar);
      if (null == formatter) formatter = new DateFormatter();
      Date date;
      try {
        date = formatter.isoDateTimeFormat(time);
      } catch (ParseException e) {
        log.error("Cant parse date - not ISO formatted, = "+time);
        return 0.0;
      }
      return date.getTime() / 1000.0;

    } else {
      return sdata.convertScalarDouble(timeVar);
    }
  }

  /*
   * This reads through all the records in the dataset, and constructs a list of
   * RecordPointObs or RecordStationObs. It does not cache the data.
   * <p>If stnIdVName is not null, its a StationDataset, then construct a Station HashMap of StationImpl
   * objects. Add the RecordStationObs into the list of obs for that station.
   *
   * @param cancel allow user to cancel
   * @return List of RecordPointObs or RecordStationObs
   * @throws IOException on read error
   *
  public List<RecordPointObs> readAllCreateObs(CancelTask cancel) throws IOException {

    // see if its a station or point dataset
    boolean hasStations = stnIdVName != null;
    if (hasStations)
      stnHash = new HashMap<Object, Station>();

    // get min and max date and lat,lon
    double minDate = Double.MAX_VALUE;
    double maxDate = -Double.MAX_VALUE;

    double minLat = Double.MAX_VALUE;
    double maxLat = -Double.MAX_VALUE;

    double minLon = Double.MAX_VALUE;
    double maxLon = -Double.MAX_VALUE;

    // read all the data, create a RecordObs
    StructureMembers members = null;
    List<RecordPointObs> records = new ArrayList<RecordPointObs>();
    int recno = 0;
    Structure.Iterator ii = recordVar.getStructureIterator();
    while (ii.hasNext()) {
      StructureData sdata = ii.next();
      if (members == null)
        members = sdata.getStructureMembers();

      Object stationId = null;
      if (hasStations) {
        if (stationIdType == DataType.INT) {
          stationId = sdata.getScalarInt(stnIdVName);
        } else
          stationId = sdata.getScalarString(stnIdVName).trim();
      }

      String desc = (stnDescVName == null) ? null : sdata.getScalarString(stnDescVName);
      double lat = sdata.getScalarDouble(latVName);
      double lon = sdata.getScalarDouble(lonVName);
      double alt = (altVName == null) ? 0.0 : altScaleFactor * sdata.getScalarDouble(altVName);
      double obsTime = sdata.convertScalarDouble(members.findMember(obsTimeVName));
      double nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarDouble(members.findMember(nomTimeVName));

      //double obsTime = sdata.convertScalarDouble( members.findMember( obsTimeVName) );
      //double nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarDouble( members.findMember( nomTimeVName));

      if (hasStations) {
        Station stn = stnHash.get(stationId);
        if (stn == null) {
          stn = new Station(stationId.toString(), desc, lat, lon, alt);
          stnHash.put(stationId, stn);
        }
        RecordStationObs stnObs = new RecordStationObs(stn, obsTime, nomTime, timeUnit, recno);
        records.add(stnObs);
        //stn.addObs( stnObs);

      } else {
        records.add(new RecordPointObs(new EarthLocation(lat, lon, alt), obsTime, nomTime, timeUnit, recno));
      }

      // track date range and bounding box
      minDate = Math.min(minDate, obsTime);
      maxDate = Math.max(maxDate, obsTime);

      minLat = Math.min(minLat, lat);
      maxLat = Math.max(maxLat, lat);
      minLon = Math.min(minLon, lon);
      maxLon = Math.max(maxLon, lon);

      recno++;
      if ((cancel != null) && cancel.isCancel()) return null;
    }
    boundingBox = new LatLonRect(new LatLonPointImpl(minLat, minLon), new LatLonPointImpl(maxLat, maxLon));

    return records;
  }

  /* private boolean debugBB = false;
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
  }  */

  //////////////////////////////////////////////////////////////////////////////////////
  public PointFeature factory(StationImpl s, StructureData sdata, int recno) {
    if (s == null)
      return new RecordPointObs(sdata, recno);
    else
      return new RecordStationObs(s, sdata, recno);
  }

  class RecordPointObs extends PointFeatureImpl {
    protected int recno;
    protected StructureData sdata;

    public RecordPointObs(int recno) {
      super(RecordDatasetHelper.this.timeUnit);
      this.recno = recno;
    }

    // Constructor for the case where you keep track of the location, time of each record, but not the data.
    protected RecordPointObs(EarthLocation location, double obsTime, double nomTime, DateUnit timeUnit, int recno) {
      super(location, obsTime, nomTime, timeUnit);
      this.recno = recno;
    }

    // Constructor for when you already have the StructureData and want to wrap it in a StationObsDatatype
    public RecordPointObs(StructureData sdata, int recno) {
      super(RecordDatasetHelper.this.timeUnit);
      this.sdata = sdata;
      this.recno = recno;

      StructureMembers members = sdata.getStructureMembers();
      obsTime = getTime(members.findMember(obsTimeVName), sdata);
      nomTime = (nomTimeVName == null) ? obsTime : getTime(members.findMember(nomTimeVName), sdata);

      // this assumes the lat/lon/alt is stored in the obs record
      double lat = sdata.convertScalarDouble(members.findMember(latVName));
      double lon = sdata.convertScalarDouble(members.findMember(lonVName));
      double alt = (zcoordVName == null) ? 0.0 : altScaleFactor * sdata.convertScalarDouble(members.findMember(zcoordVName));
      location = new EarthLocationImpl(lat, lon, alt);
    }

    public String getId() {
      return Integer.toString(recno);
    }

    public LatLonPoint getLatLon() {
      return new LatLonPointImpl(location.getLatitude(), location.getLongitude());
    }

    public StructureData getData() throws IOException {
      if (null == sdata) {
        try {
          // deal with files that are updating // LOOK kludge?
          if (recno > getRecordCount()) {
            int n = getRecordCount();
            ncfile.syncExtend();
            log.info("RecordPointObs.getData recno=" + recno + " > " + n + "; after sync= " + getRecordCount());
          }

          sdata = recordVar.readStructure(recno);
        } catch (ucar.ma2.InvalidRangeException e) {
          e.printStackTrace();
          throw new IOException(e.getMessage());
        }
      }
      return sdata;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////

  // a PointObs with the location info stored as a Station
  class RecordStationObs extends RecordPointObs {
    private Station station;

    /**
     * Constructor for the case where you keep track of the station, time of each record, but the data reading is deferred.
     *
     * @param station data is for this Station
     * @param obsTime observation time
     * @param nomTime nominal time (may be NaN)
     * @param recno   data is at this record number
     */
    protected RecordStationObs(Station station, double obsTime, double nomTime, DateUnit timeUnit, int recno) {
      super(station, obsTime, nomTime, timeUnit, recno);
      this.station = station;
    }

    // Constructor for when you have everything
    protected RecordStationObs(Station station, double obsTime, double nomTime, StructureData sdata, int recno) {
      super(recno);
      this.station = station;
      this.location = station;
      this.obsTime = obsTime;
      this.nomTime = nomTime;
      this.sdata = sdata;
    }

    // Constructor for when you already have the StructureData and Station, and calculate times
    protected RecordStationObs(Station station, StructureData sdata, int recno) {
      super(recno);
      this.station = station;
      this.location = station;
      this.sdata = sdata;

      StructureMembers members = sdata.getStructureMembers();
      obsTime = getTime(members.findMember(obsTimeVName), sdata);
      nomTime = (nomTimeVName == null) ? obsTime : getTime(members.findMember(nomTimeVName), sdata);
    }

    // Constructor for when you already have the StructureData, and need to find Station and times
    protected RecordStationObs(StructureData sdata, int recno, boolean useId) {
      super(recno);
      this.recno = recno;
      this.sdata = sdata;
      this.timeUnit = RecordDatasetHelper.this.timeUnit;

      StructureMembers members = sdata.getStructureMembers();
      obsTime = getTime(members.findMember(obsTimeVName), sdata);
      nomTime = (nomTimeVName == null) ? obsTime : getTime(members.findMember(nomTimeVName), sdata);

      if (useId) {
        // this assumes the station id/name is stored in the obs record
        String stationId;
        if (stationIdType == DataType.INT) {
          stationId = Integer.toString( sdata.getScalarInt(stnIdVName));
        } else
          stationId = sdata.getScalarString(stnIdVName).trim();
        station = stationHelper.getStation(stationId);
        if (null != errs) errs.format(" cant find station id = <%s> when reading record %d\n", stationId, recno);
        log.error(" cant find station id = <"+stationId+"> when reading record "+recno);

      } else {
        // use a station index
        List<Station> stations = stationHelper.getStations();
        int stationIndex = sdata.getScalarInt(stnIndexVName);
        if (stationIndex < 0 || stationIndex >= stations.size()) {
          if (null != errs) errs.format(" cant find station at index =%d when reading record %d\n", stationIndex, recno);
          log.error("cant find station at index = "+stationIndex+" when reading record "+recno);
        } else
          station = stations.get(stationIndex);
      }

      location = station;
    }

  }

}
