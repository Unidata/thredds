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
package ucar.nc2.iosp.adde;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.Station;
import ucar.nc2.dt.point.StationObsDatasetImpl;
import ucar.nc2.dt.point.StationObsDatatypeImpl;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonPoint;

import java.io.IOException;
import java.util.*;

import edu.wisc.ssec.mcidas.adde.AddePointDataReader;
import edu.wisc.ssec.mcidas.adde.AddeException;
import thredds.catalog.InvAccess;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.ThreddsMetadata;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;

/**
 * An adde "point" dataset.
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public class AddeStationObsDataset extends StationObsDatasetImpl {

  static AddePointDataReader callAdde( String request) throws IOException {
    try {
      System.out.println("Call ADDE request= "+request);
      long start = System.currentTimeMillis();
      AddePointDataReader reader = new AddePointDataReader( request);
      System.out.println(" took= "+(System.currentTimeMillis()-start)+" msec");

      return reader;

    } catch (AddeException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage());
    }
  }

  //////////////////////////////////////////
  private String addeURL;
  private StationDB stationDB;
  private String stationDBlocation;
  private GregorianCalendar calendar;
  private double[] scaleFactor;
  private StructureMembers members;

  private boolean debugHead = false, debugAddeCall = false;

  /**
   * Open an ADDE Station Dataset from an InvAccess, which must be type ADDE and Station.
   *
   * @param access open Invdataset from this access.
   * @throws IOException
   */
  public AddeStationObsDataset(InvAccess access, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    super();
    InvDataset invDs = access.getDataset();
    this.location = (invDs.getID() != null) ? "thredds:"+access.getDataset().getCatalogUrl() :
                                              access.getStandardUrlName();

    addeURL = access.getStandardUrlName();

    // see if we have a stationDB file
    InvDataset invds = access.getDataset();
    String pv = invds.findProperty( "_StationDBlocation");
    if (pv != null) {
      stationDBlocation = InvDatasetImpl.resolve( invds, pv);
    }

    init();

    // Get the bounding box if possible
    ThreddsMetadata.GeospatialCoverage geoCoverage = invds.getGeospatialCoverage();
    if (null != geoCoverage)
      boundingBox = geoCoverage.getBoundingBox();
    else // otherwise, stationHelper constructs from the station locations
      boundingBox = stationHelper.getBoundingBox();

    // get the date range if possible
    DateRange timeCoverage = invds.getTimeCoverage();
    if (timeCoverage != null) {
      startDate = timeCoverage.getStart().getDate();
      endDate = timeCoverage.getEnd().getDate();
    } else {
      startDate = new Date(0); // fake
      endDate = new Date();
    }

    /*    // LOOK maybe its already annotated ??
    LOOK set title, description
    ThreddsDataFactory.annotate( access.getDataset(), this);
    finish(); */
  }

  /**
   * Open an ADDE Station Dataset.
   *
   * @param location location of file. This is a URL string, or a local pathname.
   * @throws IOException
   */
  public AddeStationObsDataset(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    super();
    this.location = location;
    this.addeURL = location;

    init();
    startDate = new Date(0);
    endDate = new Date();
    boundingBox = stationHelper.getBoundingBox();
  }

  private void init() throws IOException {
    members = new StructureMembers("stationObs");

    // used to convert from adde format
    calendar = new GregorianCalendar();
    calendar.setTimeZone(TimeZone.getTimeZone("GMT"));

        // time unit
    timeUnit = DateUnit.getUnixDateUnit();

    try {
      AddePointDataReader reader = callAdde( addeURL);

      String[] params = reader.getParams();
      String[] units = reader.getUnits();
      int[] scales  = reader.getScales();
      scaleFactor = new double[ params.length];

      if (debugHead) System.out.println(" Param  Unit Scale");
      for (int paramNo = 0; paramNo < params.length; paramNo++) {
        //memberNames.add( params[i]);

        if (debugHead) System.out.println(" "+params[paramNo]+" "+units[paramNo]+" "+scales[paramNo]);
        if (scales[paramNo] != 0)
           scaleFactor[paramNo] = 1.0/Math.pow(10.0, (double) scales[paramNo]);

        DataType dt = null;
        if ("CHAR".equals(units[paramNo]))
          dt = DataType.STRING;
        else if (scaleFactor[paramNo] == 0)
          dt = DataType.INT;
        else
          dt = DataType.DOUBLE;

        String unitString = null;
        if ((units[paramNo] != null) && (units[paramNo].length() > 0))
          unitString = visad.jmet.MetUnits.makeSymbol(units[paramNo]);

        AddeTypedDataVariable tdv = new AddeTypedDataVariable( params[paramNo], unitString, dt);
        dataVariables.add( tdv);
        StructureMembers.Member m = members.addMember( tdv.getShortName(), tdv.getDescription(),
          tdv.getUnitsString(), tdv.getDataType(), tdv.getShape());
        m.setDataParam( paramNo);
        members.addMember( m);
      }

    } catch (AddeException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage());
    }
  }

  protected void setTimeUnits() {}
  protected void setStartDate() {}
  protected void setEndDate() {}
  protected void setBoundingBox() {}

  //////////////////////////////////////////////////////////////////
  // read from ADDE

  private ArrayStructureAdde readData(String selectClause, CancelTask cancel) {
    try {
      String urlString = (selectClause == null) ? addeURL+"&num=all" : addeURL+"&num=all&select='"+selectClause+"'";
      AddePointDataReader reader = callAdde( urlString);

      int [][] stationObsData = reader.getData();
      int nparams = stationObsData.length;
      int nobs = stationObsData[0].length;
      if (debugAddeCall) {
        System.out.println("CALL ADDE= "+urlString);
        System.out.println(" nparams= "+nparams+" nobs=" + nobs);
        System.out.println(" size= "+(nparams * nobs * 4)+" bytes");
      }

      return new ArrayStructureAdde( members, new int[] {nobs}, stationObsData, scaleFactor);

    } catch (AddeException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage()); // LOOK BAD
    }
  }

   /* Array makeStationObsArray(String stationName) throws IOException {
    int [][] stationObsData = getData(stationName);
    int nparams = stationObsData.length;
    int nobs = stationObsData[0].length;

    Array result = Array.factory( DataType.STRUCTURE.getPrimitiveClassType(), new int[] {nobs});
    IndexIterator ii = result.getIndexIterator();
    for (int row=0; row<nobs; row++){
      StructureData sd = new AddeStructureData(this, stationObsVar, stationName, nparams, row);
      ii.setObjectNext( sd);
    }
    return result;
  } */

  //////////////////////////////////////////////////////////////////////////////////////////
  // ncfile I/O implemntation
  /* top structure - get stations
  public Array readData(ucar.nc2.Variable v, List section) throws IOException, InvalidRangeException  {
    if (stationDB == null)
      readStations();

    List stations = stationDB.getStations();
    int nstations = stations.size();
    Array result = Array.factory( DataType.STRUCTURE.getPrimitiveClassType(), new int[] {nstations});
    IndexIterator ii = result.getIndexIterator();
    for (int row=0; row<nstations; row++){
      Station s = (Station) stations.get( row);
      StructureData sd = new StationStructData(this, topStructure, stationNameVar, s.getName(), stationObsVar);
      ii.setObjectNext( sd);
    }
    return result;
  }

  // this is for reading variables that are members of structures
  public Array readMemberData(ucar.nc2.Variable v, List section, boolean flatten) throws IOException, InvalidRangeException  {
    if (stationDB == null)
      readStations();

    if (v.getName().equals(stationObsVar.getName())) {
      Range stationRange = (Range) section.get(0);
      Array stationArray = Array.factory( DataType.STRUCTURE.getPrimitiveClassType(), new int[] {stationRange.length()});

      List stations = stationDB.getStations();
      int nstations = stations.size();

      IndexIterator stationIter = stationArray.getIndexIterator();

      for (int row=stationRange.first(); row<=stationRange.last(); row+=stationRange.stride()){
        Station s = (Station) stations.get( row);
        StructureData sd = new StationStructData(this, topStructure, stationNameVar, s.getName(), stationObsVar);
        stationIter.setObjectNext( sd);
      }
      return stationArray;
    }

    return null;
  } */

  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  // heres the StationObsDataset implementation

  public List getStations(CancelTask cancel) throws IOException {
    if (stationDB == null)
      readStations(cancel);
    return stationDB.getStations();
  }

  public int getStationDataCount(Station s) {
    return -1;
  }

  public List getData(CancelTask cancel) throws IOException {
    return null;
  }

  public int getDataCount() {
    return -1;
  }

  // get a list of station obs for this station
  public List getData(Station s, CancelTask cancel) throws IOException {
    ArrayStructure stationData = readData("ID "+s.getName(), cancel);

    ArrayList stationObs = new ArrayList();
    IndexIterator ii = stationData.getIndexIterator();
    while (ii.hasNext()) {
      stationObs.add( new StationObs( (Station) s, (StructureData) ii.getObjectNext()));
    }
    return stationObs;
  }

  public List getData(Station s, Date start, Date end, CancelTask cancel) throws IOException {
    double startTime = timeUnit.makeValue( start);
    double endTime = timeUnit.makeValue( end);
    return stationHelper.getStationObs(s, startTime, endTime, cancel); // LOOK should call ADDE
  }

  public List getData(List stations, CancelTask cancel) throws IOException {
    return stationHelper.getStationObs(stations, cancel); // LOOK should call ADDE
  }

  public List getData(List stations, Date start, Date end, CancelTask cancel) throws IOException {
    double startTime = timeUnit.makeValue( start);
    double endTime = timeUnit.makeValue( end);
    return stationHelper.getStationObs(stations, startTime, endTime, cancel);
  }

  public List getData(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    return stationHelper.getStationObs(boundingBox, cancel);
  }

  public List getData(LatLonRect boundingBox, Date start, Date end, CancelTask cancel) throws IOException {
    double startTime = timeUnit.makeValue( start);
    double endTime = timeUnit.makeValue( end);
    return stationHelper.getStationObs(boundingBox, startTime, endTime, cancel);
  }

  private class AddeTypedDataVariable implements ucar.nc2.VariableSimpleIF {
    String name, units;
    DataType dt;

    AddeTypedDataVariable( String name, String units, DataType dt ) {
      this.name = name;
      this.units = units;
      this.dt = dt;
    }

    public String getName() { return name; }
    public String getShortName() { return name; }

    public String getDescription() {
      return null;
    }

    /* Units of the Variable. These should be udunits compatible if possible */
    public String getUnitsString() {
      return units;
    }

    public int getRank() {
      return 0;
    }

    public int[] getShape() {
      return new int[0];
    }

    public List getDimensions() { return new ArrayList(); }

    public DataType getDataType() {
      return dt;
    }

    public List getAttributes() {
      return new ArrayList();
    }

    public Attribute findAttributeIgnoreCase(String name) {
      return null;
    }

  public int compareTo(VariableSimpleIF o) {
    return getName().compareTo(o.getName());
  }

  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  private static void makeSelectBB( StringBuffer sbuff, LatLonRect bb) {
    // LAT min max;LON min max
    // For ADDE URL's, lon is positive east, on the server, lon is positive west.
    // AddeURLConnection handles the conversion.
    // Format for lat/lon is either decimal degrees or DD:MM:SS

    LatLonPoint ll = bb.getLowerLeftPoint();
    LatLonPoint ur = bb.getUpperRightPoint();
    sbuff.append("LAT ");
    sbuff.append(ll.getLatitude());
    sbuff.append(" ");
    sbuff.append(ur.getLatitude());
    sbuff.append(";LON ");
    sbuff.append(ll.getLongitude());
    sbuff.append(" ");
    sbuff.append(ur.getLongitude());
 }

  private void makeSelectTime( StringBuffer sbuff, Date begin, Date end) {
    // McIDAS stores dates as two separate variables (generally DAY, TIME, but it could be CYD, HMS).
    // In that case, you could do: DAY min max;TIME min max
    // However, this is problematic becuase it does not go from DAY/TIME min to DAY/TIM max, but rather
    // from TIME min/max on each DAY.  To do something like span from 22Z on one day to 4Z on the other, you
    // need to have 2 separate ADDE requests:
    // DAY firstDay;TIME 22 23:59
    // DAY secondDay;TIME 0 4

    sbuff.append("DAY ");
 }

  private void makeSelectStations( StringBuffer sbuff, List stations) {
    // ID id1,id2,id3,...idn
    // However, it will depend on the dataset as to the variable for the station ID.
    // For synoptic data it's IDN (e.g. 72469)
    // for METAR it's ID.
    // For profiler data, there are two variables IDA and IDB where IDA is the first 4 chars and IDB is the second 4.

    sbuff.append("ID ");
    for (int i = 0; i < stations.size(); i++) {
      Station s = (Station) stations.get(i);
      if (i > 0) sbuff.append(",");
      sbuff.append(s.getName());
    }
 }

  private void readStations(CancelTask cancel) throws IOException {
    try {
      if (stationDBlocation != null)
        stationDB = new StationDB(stationDBlocation);
    } catch (IOException ioe) {
      System.out.println("++ AddeStationDataset cant find stationDBlocation= "+ stationDBlocation);
    }

    if (stationDB == null) // otherwise, we have to read all records from server !!!
      stationDB = new StationDB(location, cancel);
  }

  private class StationObs extends StationObsDatatypeImpl {
    private StructureData data;

    StationObs( Station s, StructureData data) {
      this.station = s;
      this.data = data;

      int cyd = data.getScalarInt("DAY");
      int year = cyd /1000;
      int doy = cyd % 1000;
      int hms = data.getScalarInt("TIME");
      int hour = hms / 10000;
      hms %= 10000;
      int min = hms / 100;
      int sec = hms % 100;

      calendar.clear();
      calendar.set(GregorianCalendar.YEAR, year);
      calendar.set(GregorianCalendar.DAY_OF_YEAR, doy);
      calendar.set(GregorianCalendar.HOUR_OF_DAY, hour);
      calendar.set(GregorianCalendar.MINUTE, min);
      calendar.set(GregorianCalendar.SECOND, sec);
      //Date check = calendar.getTime();
      //System.out.println(" check="+DateUnit.getStandardDateString(check));
      obsTime = calendar.getTimeInMillis() / 1000.0;
      nomTime = obsTime; // LOOK get real nominal time
    }

    public StructureData getData() throws IOException  {
      return data;
    }

    public Date getNominalTimeAsDate() {
      return timeUnit.makeDate( getNominalTime());
    }

    public Date getObservationTimeAsDate() {
      return timeUnit.makeDate( getObservationTime());
    }
  }

//////////////////////////////////////////////////////////////////

  static void test(String urlString) {
    try {
      long start = System.currentTimeMillis();
      System.out.println(" get "+urlString);

      AddePointDataReader reader = new AddePointDataReader( urlString);

      System.out.println(" took= "+(System.currentTimeMillis()-start)+" msec");
      System.out.println(reader.toString());

      System.out.println(" Param  Unit Scale");

      String[] params = reader.getParams();
      String[] units = reader.getUnits();
      int[] scales = reader.getScales();
      for (int i = 0; i < params.length; i++) {
        System.out.println(" "+params[i]+" "+units[i]+" "+scales[i]);
      }

      int[][] data = reader.getData();
      System.out.println(" nparams= "+params.length);
      System.out.println(" n= "+data.length);
      System.out.println(" m= "+data[0].length);

    } catch (AddeException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  static public void main( String[] args) {


    //new AddePointDataset("adde://adde.ucar.edu/point?group=rtptsrc&descr=sfchourly&select='row 1'&num=all&param=id lat lon zs&pos=all");
    // new AddePointDataset("adde://adde.ucar.edu/point?group=rtptsrc&descr=sfchourly&num=all&param=ID");
    String loc = "adde://adde.ucar.edu/point?group=rtptsrc&descr=sfchourly&num=10";
    StringBuffer sbuff = new StringBuffer();
    sbuff.append(loc);
    sbuff.append("&num=all&select='");
    makeSelectBB( sbuff, new LatLonRect( new LatLonPointImpl(10.0, 10.0), new LatLonPointImpl(20.0, 20.0)));
    sbuff.append("'");

    test(sbuff.toString());

    try {
      AddeStationObsDataset ads = new AddeStationObsDataset(loc, null);
      System.out.println(loc+" =\n"+ads);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ucar.nc2.dt.DataIterator getDataIterator(int bufferSize) throws IOException {
    return null;
  }

}
