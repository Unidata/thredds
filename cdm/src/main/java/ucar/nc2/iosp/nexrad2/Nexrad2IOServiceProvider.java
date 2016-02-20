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
package ucar.nc2.iosp.nexrad2;

import ucar.nc2.constants.DataFormatType;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import static ucar.nc2.iosp.nexrad2.Level2Record.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

/**
 * An IOServiceProvider for NEXRAD level II files.
 *
 * @author caron
 */
public class Nexrad2IOServiceProvider extends AbstractIOServiceProvider {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Nexrad2IOServiceProvider.class);
  static private final int MISSING_INT = -9999;
  static private final float MISSING_FLOAT = Float.NaN;


  public boolean isValidFile( RandomAccessFile raf) throws IOException {
    try {
      raf.seek(0);
      String test = raf.readString(8);
      return test.equals( Level2VolumeScan.ARCHIVE2) || test.equals( Level2VolumeScan.AR2V0001) ||
             test.equals( Level2VolumeScan.AR2V0003)|| test.equals( Level2VolumeScan.AR2V0004) ||
             test.equals( Level2VolumeScan.AR2V0002) || test.equals( Level2VolumeScan.AR2V0006) ||
             test.equals( Level2VolumeScan.AR2V0007);
    } catch (IOException ioe) {
      return false;
    }
  }

 // private Dimension radialDim;
  private double radarRadius;
  private Variable v0, v1;
  private DateFormatter formatter = new DateFormatter();
  private boolean overMidNight = false;

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);
    NexradStationDB.init();

    Level2VolumeScan volScan = new Level2VolumeScan( raf, cancelTask); // note raf may change when compressed
    this.raf = volScan.raf;
    this.location = volScan.raf.getLocation();

    if (volScan.hasDifferentDopplarResolutions())
      throw new IllegalStateException("volScan.hasDifferentDopplarResolutions");

    if( volScan.hasHighResolutions(0)) {

        if(volScan.getHighResReflectivityGroups() != null)
            makeVariable2( ncfile, Level2Record.REFLECTIVITY_HIGH, "Reflectivity", "Reflectivity", "R", volScan);
        if( volScan.getHighResVelocityGroups() != null)
            makeVariable2( ncfile, Level2Record.VELOCITY_HIGH, "RadialVelocity", "Radial Velocity", "V", volScan);

        if( volScan.getHighResSpectrumGroups() != null) {
            List<List<Level2Record>> gps = volScan.getHighResSpectrumGroups();
            List<Level2Record> gp = gps.get(0);
            Level2Record record = gp.get(0);
            if(v1 != null)
                makeVariableNoCoords( ncfile, Level2Record.SPECTRUM_WIDTH_HIGH, "SpectrumWidth_HI", "Radial Spectrum_HI", v1, record);
            if(v0 != null)
                makeVariableNoCoords( ncfile, Level2Record.SPECTRUM_WIDTH_HIGH, "SpectrumWidth", "Radial Spectrum", v0, record);
        }
    }

    List<List<Level2Record>> gps = volScan.getHighResDiffReflectGroups();
    if( gps != null) {
        makeVariable2( ncfile, Level2Record.DIFF_REFLECTIVITY_HIGH, "DifferentialReflectivity", "Differential Reflectivity", "D", volScan);
    }

    gps = volScan.getHighResCoeffocientGroups();
    if(gps != null) {
        makeVariable2( ncfile, Level2Record.CORRELATION_COEFFICIENT, "CorrelationCoefficient", "Correlation Coefficient", "C", volScan);
    }

    gps = volScan.getHighResDiffPhaseGroups();
    if( gps != null) {
        makeVariable2( ncfile, Level2Record.DIFF_PHASE, "DifferentialPhase", "Differential Phase", "P", volScan);
    }

    gps = volScan.getReflectivityGroups();
    if( gps != null) {
        makeVariable( ncfile, Level2Record.REFLECTIVITY, "Reflectivity", "Reflectivity", "R", volScan.getReflectivityGroups(), 0, volScan);
        int velocity_type =  (volScan.getDopplarResolution() == Level2Record.DOPPLER_RESOLUTION_HIGH_CODE) ? Level2Record.VELOCITY_HI : Level2Record.VELOCITY_LOW;
        Variable v = makeVariable( ncfile, velocity_type, "RadialVelocity", "Radial Velocity", "V", volScan.getVelocityGroups(), 0, volScan);
        gps = volScan.getVelocityGroups();
        List<Level2Record> gp = gps.get(0);
        Level2Record record = gp.get(0);
        makeVariableNoCoords( ncfile, Level2Record.SPECTRUM_WIDTH, "SpectrumWidth", "Spectrum Width", v, record);
    }
    if (volScan.getStationId() != null) {
      ncfile.addAttribute(null, new Attribute("Station", volScan.getStationId()));
      ncfile.addAttribute(null, new Attribute("StationName", volScan.getStationName()));
      ncfile.addAttribute(null, new Attribute("StationLatitude", volScan.getStationLatitude()));
      ncfile.addAttribute(null, new Attribute("StationLongitude", volScan.getStationLongitude()));
      ncfile.addAttribute(null, new Attribute("StationElevationInMeters", volScan.getStationElevation()));

      double latRadiusDegrees = Math.toDegrees( radarRadius / ucar.unidata.geoloc.Earth.getRadius());
      ncfile.addAttribute(null, new Attribute("geospatial_lat_min", volScan.getStationLatitude() - latRadiusDegrees));
      ncfile.addAttribute(null, new Attribute("geospatial_lat_max", volScan.getStationLatitude() + latRadiusDegrees));
      double cosLat = Math.cos( Math.toRadians(volScan.getStationLatitude()));
      double lonRadiusDegrees = Math.toDegrees( radarRadius / cosLat / ucar.unidata.geoloc.Earth.getRadius());
      ncfile.addAttribute(null, new Attribute("geospatial_lon_min", volScan.getStationLongitude() - lonRadiusDegrees));
      ncfile.addAttribute(null, new Attribute("geospatial_lon_max", volScan.getStationLongitude() + lonRadiusDegrees));


          // add a radial coordinate transform (experimental)
        /*
      Variable ct = new Variable(ncfile, null, null, "radialCoordinateTransform");
      ct.setDataType(DataType.CHAR);
      ct.setDimensions(""); // scalar
      ct.addAttribute( new Attribute("transform_name", "Radial"));
      ct.addAttribute( new Attribute("center_latitude", volScan.getStationLatitude()));
      ct.addAttribute( new Attribute("center_longitude", volScan.getStationLongitude()));
      ct.addAttribute( new Attribute("center_elevation", volScan.getStationElevation()));
      ct.addAttribute( new Attribute(_Coordinate.TransformType, "Radial"));
      ct.addAttribute( new Attribute(_Coordinate.AxisTypes, "RadialElevation RadialAzimuth RadialDistance"));

      Array data = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[0], new char[] {' '});
      ct.setCachedData(data, true);
      ncfile.addVariable(null, ct);
      */
    }

    DateFormatter formatter = new DateFormatter();

    ncfile.addAttribute(null, new Attribute(CDM.CONVENTIONS, _Coordinate.Convention));
    ncfile.addAttribute(null, new Attribute("format", volScan.getDataFormat()));
    ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.RADIAL.toString()));
    Date d = getDate(volScan.getTitleJulianDays(), volScan.getTitleMsecs());
    ncfile.addAttribute(null, new Attribute("base_date", formatter.toDateOnlyString(d)));

    ncfile.addAttribute(null, new Attribute("time_coverage_start", formatter.toDateTimeStringISO(d)));
    ncfile.addAttribute(null, new Attribute("time_coverage_end", formatter.toDateTimeStringISO(volScan.getEndDate())));

    ncfile.addAttribute(null, new Attribute(CDM.HISTORY, "Direct read of Nexrad Level 2 file into CDM"));
    ncfile.addAttribute(null, new Attribute("DataType", "Radial"));

    ncfile.addAttribute(null, new Attribute("Title", "Nexrad Level 2 Station "+volScan.getStationId()+" from "+
        formatter.toDateTimeStringISO(volScan.getStartDate()) + " to " +
        formatter.toDateTimeStringISO(volScan.getEndDate())));

    ncfile.addAttribute(null, new Attribute("Summary", "Weather Surveillance Radar-1988 Doppler (WSR-88D) "+
        "Level II data are the three meteorological base data quantities: reflectivity, mean radial velocity, and "+
        "spectrum width."));

    ncfile.addAttribute(null, new Attribute("keywords", "WSR-88D; NEXRAD; Radar Level II; reflectivity; mean radial velocity; spectrum width"));

    ncfile.addAttribute(null, new Attribute("VolumeCoveragePatternName",
      getVolumeCoveragePatternName(volScan.getVCP())));
    ncfile.addAttribute(null, new Attribute("VolumeCoveragePattern", volScan.getVCP()));
    ncfile.addAttribute(null, new Attribute("HorizontalBeamWidthInDegrees", (double) HORIZONTAL_BEAM_WIDTH));

    ncfile.finish();
  }

  public void makeVariable2(NetcdfFile ncfile, int datatype, String shortName, String longName, String abbrev, Level2VolumeScan vScan) throws IOException {
      List<List<Level2Record>> groups = null;

      if( shortName.startsWith("Reflectivity"))
        groups = vScan.getHighResReflectivityGroups();
      else if( shortName.startsWith("RadialVelocity"))
        groups = vScan.getHighResVelocityGroups();
      else if( shortName.startsWith("DifferentialReflectivity"))
        groups = vScan.getHighResDiffReflectGroups();
      else if( shortName.startsWith("CorrelationCoefficient"))
        groups = vScan.getHighResCoeffocientGroups();
      else if( shortName.startsWith("DifferentialPhase"))
        groups = vScan.getHighResDiffPhaseGroups();
      else
        throw new IllegalStateException("Bad group: " + shortName);

      int nscans = groups.size();

    if (nscans == 0) {
      throw new IllegalStateException("No data for "+shortName);
    }

    List<List<Level2Record>> firstGroup = new ArrayList<List<Level2Record>>(groups.size());
    List<List<Level2Record>> secondGroup = new ArrayList<List<Level2Record>>(groups.size());

    for(int i = 0; i < nscans; i++) {
        List<Level2Record> o = groups.get(i);
        Level2Record firstRecord = (Level2Record) o.get(0);
        int ol = o.size();
        
        if(ol >= 720 )
            firstGroup.add(o);
        else if(ol <= 360)
            secondGroup.add(o);
        else if( firstRecord.getGateCount(REFLECTIVITY_HIGH) > 500 || firstRecord.getGateCount(VELOCITY_HIGH) > 1000)
            firstGroup.add(o);
        else
            secondGroup.add(o);
    }
    if(firstGroup != null && firstGroup.size() > 0)
        v1 = makeVariable(ncfile, datatype, shortName + "_HI", longName + "_HI",  abbrev + "_HI", firstGroup, 1, vScan);
    if(secondGroup != null && secondGroup.size() > 0)
        v0 = makeVariable(ncfile, datatype, shortName, longName,  abbrev, secondGroup, 0, vScan);

  }

  public int getMaxRadials(List groups) {
      int maxRadials = 0;
      for (int i = 0; i < groups.size(); i++) {
        ArrayList group = (ArrayList) groups.get(i);
        maxRadials = Math.max(maxRadials, group.size());
      }
      return maxRadials;
  }

  public Variable makeVariable(NetcdfFile ncfile, int datatype, String shortName,
                               String longName, String abbrev, List<List<Level2Record>> groups,
                               int rd) throws IOException {
      return makeVariable(ncfile, datatype, shortName, longName, abbrev, groups, rd, null);
  }

  public Variable makeVariable(NetcdfFile ncfile, int datatype, String shortName,
                               String longName, String abbrev, List<List<Level2Record>> groups,
                               int rd, Level2VolumeScan volScan) throws IOException {
    int nscans = groups.size();

    if (nscans == 0) {
      throw new IllegalStateException("No data for "+shortName+" file= "+ncfile.getLocation());
    }

    // get representative record
    List<Level2Record> firstGroup = groups.get(0);
    Level2Record firstRecord = firstGroup.get(0);
    int ngates = firstRecord.getGateCount(datatype);

    String scanDimName = "scan"+abbrev;
    String gateDimName = "gate"+abbrev;
    String radialDimName = "radial"+abbrev;
    Dimension scanDim = new Dimension(scanDimName, nscans);
    Dimension gateDim = new Dimension(gateDimName, ngates);
    Dimension radialDim = new Dimension(radialDimName, volScan.getMaxRadials(rd), true);
    ncfile.addDimension( null, scanDim);
    ncfile.addDimension( null, gateDim);
    ncfile.addDimension( null, radialDim);

    List<Dimension> dims = new ArrayList<Dimension>();
    dims.add( scanDim);
    dims.add( radialDim);
    dims.add( gateDim);

    Variable v = new Variable(ncfile, null, null, shortName);
    if(datatype == DIFF_PHASE){
        v.setDataType(DataType.SHORT);
    } else {
        v.setDataType(DataType.BYTE);
    }

    v.setDimensions(dims);
    ncfile.addVariable(null, v);

    v.addAttribute( new Attribute(CDM.UNITS, getDatatypeUnits(datatype)));
    v.addAttribute( new Attribute(CDM.LONG_NAME, longName));


    byte[] b = new byte[2];
    b[0] = MISSING_DATA;
    b[1] = BELOW_THRESHOLD;
    Array missingArray = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[] {2}, b);

    v.addAttribute( new Attribute(CDM.MISSING_VALUE, missingArray));
    v.addAttribute( new Attribute("signal_below_threshold", BELOW_THRESHOLD));
    v.addAttribute( new Attribute(CDM.SCALE_FACTOR, firstRecord.getDatatypeScaleFactor(datatype)));
    v.addAttribute( new Attribute(CDM.ADD_OFFSET, firstRecord.getDatatypeAddOffset(datatype)));
    v.addAttribute( new Attribute(CDM.UNSIGNED, "true"));
    if(rd == 1) {
       v.addAttribute( new Attribute("SNR_threshold" ,firstRecord.getDatatypeSNRThreshhold(datatype)));
    }
    v.addAttribute( new Attribute("range_folding_threshold" ,firstRecord.getDatatypeRangeFoldingThreshhold(datatype)));

    List<Dimension> dim2 = new ArrayList<Dimension>();
    dim2.add( scanDim);
    dim2.add( radialDim);

    // add time coordinate variable
    String timeCoordName = "time"+abbrev;
    Variable timeVar = new Variable(ncfile, null, null, timeCoordName);
    timeVar.setDataType(DataType.INT);
    timeVar.setDimensions(dim2);
    ncfile.addVariable(null, timeVar);

    // int julianDays = volScan.getTitleJulianDays();
    // Date d = Level2Record.getDate( julianDays, 0);
    // Date d = getDate(volScan.getTitleJulianDays(), volScan.getTitleMsecs());
    Date d = getDate(volScan.getTitleJulianDays(), 0);  // times are msecs from midnight
    String units = "msecs since "+formatter.toDateTimeStringISO(d);

    timeVar.addAttribute( new Attribute(CDM.LONG_NAME, "time of each ray"));
    timeVar.addAttribute( new Attribute(CDM.UNITS, units));
    timeVar.addAttribute( new Attribute(CDM.MISSING_VALUE, MISSING_INT));
    timeVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    // add elevation coordinate variable
    String elevCoordName = "elevation"+abbrev;
    Variable elevVar = new Variable(ncfile, null, null, elevCoordName);
    elevVar.setDataType(DataType.FLOAT);
    elevVar.setDimensions(dim2);
    ncfile.addVariable(null, elevVar);

    elevVar.addAttribute( new Attribute(CDM.UNITS, "degrees"));
    elevVar.addAttribute( new Attribute(CDM.LONG_NAME, "elevation angle in degres: 0 = parallel to pedestal base, 90 = perpendicular"));
    elevVar.addAttribute( new Attribute(CDM.MISSING_VALUE, MISSING_FLOAT));
    elevVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString()));

    // add azimuth coordinate variable
    String aziCoordName = "azimuth"+abbrev;
    Variable aziVar = new Variable(ncfile, null, null, aziCoordName);
    aziVar.setDataType(DataType.FLOAT);
    aziVar.setDimensions(dim2);
    ncfile.addVariable(null, aziVar);

    aziVar.addAttribute( new Attribute(CDM.UNITS, "degrees"));
    aziVar.addAttribute( new Attribute(CDM.LONG_NAME, "azimuth angle in degrees: 0 = true north, 90 = east"));
    aziVar.addAttribute( new Attribute(CDM.MISSING_VALUE, MISSING_FLOAT));
    aziVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RadialAzimuth.toString()));

    // add gate coordinate variable
    String gateCoordName = "distance"+abbrev;
    Variable gateVar = new Variable(ncfile, null, null, gateCoordName);
    gateVar.setDataType(DataType.FLOAT);
    gateVar.setDimensions(gateDimName);
    Array data = Array.makeArray( DataType.FLOAT, ngates,
        (double) firstRecord.getGateStart(datatype), (double) firstRecord.getGateSize(datatype));
    gateVar.setCachedData( data, false);
    ncfile.addVariable(null, gateVar);
    radarRadius = firstRecord.getGateStart(datatype) + ngates * firstRecord.getGateSize(datatype);

    gateVar.addAttribute( new Attribute(CDM.UNITS, "m"));
    gateVar.addAttribute( new Attribute(CDM.LONG_NAME, "radial distance to start of gate"));
    gateVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RadialDistance.toString()));

    // add number of radials variable
    String nradialsName = "numRadials"+abbrev;
    Variable nradialsVar = new Variable(ncfile, null, null, nradialsName);
    nradialsVar.setDataType(DataType.INT);
    nradialsVar.setDimensions(scanDim.getShortName());
    nradialsVar.addAttribute( new Attribute(CDM.LONG_NAME, "number of valid radials in this scan"));
    ncfile.addVariable(null, nradialsVar);

    // add number of gates variable
    String ngateName = "numGates"+abbrev;
    Variable ngateVar = new Variable(ncfile, null, null, ngateName);
    ngateVar.setDataType(DataType.INT);
    ngateVar.setDimensions(scanDim.getShortName());
    ngateVar.addAttribute( new Attribute(CDM.LONG_NAME, "number of valid gates in this scan"));
    ncfile.addVariable(null, ngateVar);

    makeCoordinateDataWithMissing( datatype, timeVar, elevVar, aziVar, nradialsVar, ngateVar, groups);

    // back to the data variable
    String coordinates = timeCoordName+" "+elevCoordName +" "+ aziCoordName+" "+gateCoordName;
    v.addAttribute( new Attribute(_Coordinate.Axes, coordinates));

    // make the record map
    int nradials = radialDim.getLength();
    Level2Record[][] map = new Level2Record[nscans][nradials];
    for (int i = 0; i < groups.size(); i++) {
      Level2Record[] mapScan = map[i];
      List<Level2Record> group = groups.get(i);
      for (Level2Record r : group) {
        int radial = r.radial_num - 1;
        if (radial >= nradials) {
          radial %= nradials;
        }
        mapScan[radial] = r;
      }
    }

    Vgroup vg = new Vgroup(datatype, map);
    v.setSPobject( vg);

    return v;
   }

  private void makeVariableNoCoords(NetcdfFile ncfile, int datatype, String shortName, String longName, Variable from,
                                    Level2Record record) {

    // get representative record

    Variable v = new Variable(ncfile, null, null, shortName);
    v.setDataType(DataType.BYTE);
    v.setDimensions( from.getDimensions());
    ncfile.addVariable(null, v);

    v.addAttribute( new Attribute(CDM.UNITS, getDatatypeUnits(datatype)));
    v.addAttribute( new Attribute(CDM.LONG_NAME, longName));

    byte[] b = new byte[2];
    b[0] = MISSING_DATA;
    b[1] = BELOW_THRESHOLD;
    Array missingArray = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[]{2}, b);
    v.addAttribute( new Attribute(CDM.MISSING_VALUE, missingArray));
    v.addAttribute( new Attribute("signal_below_threshold", BELOW_THRESHOLD));
    v.addAttribute( new Attribute(CDM.SCALE_FACTOR, record.getDatatypeScaleFactor(datatype)));
    v.addAttribute( new Attribute(CDM.ADD_OFFSET, record.getDatatypeAddOffset(datatype)));
    v.addAttribute( new Attribute(CDM.UNSIGNED, "true"));
    if(datatype == Level2Record.SPECTRUM_WIDTH_HIGH){
       v.addAttribute( new Attribute("SNR_threshold" ,record.getDatatypeSNRThreshhold(datatype)));
    }
    v.addAttribute( new Attribute("range_folding_threshold" ,record.getDatatypeRangeFoldingThreshhold(datatype)));

    Attribute fromAtt = from.findAttribute(_Coordinate.Axes);
    v.addAttribute( new Attribute(_Coordinate.Axes, fromAtt));

    Vgroup vgFrom = (Vgroup) from.getSPobject();
    Vgroup vg = new Vgroup(datatype, vgFrom.map);
    v.setSPobject( vg);
  }

  private void makeCoordinateDataWithMissing(int datatype, Variable time, Variable elev, Variable azi, Variable nradialsVar,
                                  Variable ngatesVar, List groups) {

    Array timeData = Array.factory( time.getDataType().getPrimitiveClassType(), time.getShape());
    Index timeIndex = timeData.getIndex();

    Array elevData = Array.factory( elev.getDataType().getPrimitiveClassType(), elev.getShape());
    Index elevIndex = elevData.getIndex();

    Array aziData = Array.factory( azi.getDataType().getPrimitiveClassType(), azi.getShape());
    Index aziIndex = aziData.getIndex();

    Array nradialsData = Array.factory( nradialsVar.getDataType().getPrimitiveClassType(), nradialsVar.getShape());
    IndexIterator nradialsIter = nradialsData.getIndexIterator();

    Array ngatesData = Array.factory( ngatesVar.getDataType().getPrimitiveClassType(), ngatesVar.getShape());
    IndexIterator ngatesIter = ngatesData.getIndexIterator();

    // first fill with missing data
    IndexIterator ii = timeData.getIndexIterator();
    while (ii.hasNext())
      ii.setIntNext(MISSING_INT);

    ii = elevData.getIndexIterator();
    while (ii.hasNext())
      ii.setFloatNext(MISSING_FLOAT);

    ii = aziData.getIndexIterator();
    while (ii.hasNext())
      ii.setFloatNext(MISSING_FLOAT);

        // now set the  coordinate variables from the Level2Record radial
    int last_msecs = Integer.MIN_VALUE;
    int nscans = groups.size();

    for (int scan = 0; scan < nscans; scan++) {
      List scanGroup = (List) groups.get(scan);
      int nradials = scanGroup.size();

      Level2Record first = null;
      for (int j = 0; j < nradials; j++) {
        Level2Record r =  (Level2Record) scanGroup.get(j);
        if (first == null) first = r;

        int radial = r.radial_num-1;
        if (radial >= nradials) {
          radial %= nradials;
        }
        if(last_msecs != Integer.MIN_VALUE && (last_msecs - r.data_msecs ) > 80000000 ) {
             overMidNight = true;
        }
        if(overMidNight)
            timeData.setInt( timeIndex.set(scan, radial), r.data_msecs + 24 * 3600 * 1000);
        else
            timeData.setInt( timeIndex.set(scan, radial), r.data_msecs);
        elevData.setFloat( elevIndex.set(scan, radial), r.getElevation());
        aziData.setFloat( aziIndex.set(scan, radial), r.getAzimuth());

        if (r.data_msecs < last_msecs && !overMidNight)
            logger.warn("makeCoordinateData time out of order: " +
                    r.data_msecs + " before " + last_msecs);

        last_msecs = r.data_msecs;
      }

      nradialsIter.setIntNext( nradials);
      if (first != null) ngatesIter.setIntNext( first.getGateCount( datatype));
    }

    time.setCachedData( timeData, false);
    elev.setCachedData( elevData, false);
    azi.setCachedData( aziData, false);
    nradialsVar.setCachedData( nradialsData, false);
    ngatesVar.setCachedData( ngatesData, false);
  }

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Vgroup vgroup = (Vgroup) v2.getSPobject();    Range scanRange = section.getRange(0);
    Range radialRange = section.getRange(1);
    Range gateRange = section.getRange(2);

    Array data = Array.factory(v2.getDataType().getPrimitiveClassType(), section.getShape());
    IndexIterator ii = data.getIndexIterator();

    for (int i=scanRange.first(); i<=scanRange.last(); i+= scanRange.stride()) {
      Level2Record[] mapScan = vgroup.map[i];
      readOneScan(mapScan, radialRange, gateRange, vgroup.datatype, ii);
    }

    return data;
  }

  private void readOneScan(Level2Record[] mapScan, Range radialRange, Range gateRange, int datatype, IndexIterator ii) throws IOException {
    for (int i=radialRange.first(); i<=radialRange.last(); i+= radialRange.stride()) {
      Level2Record r = mapScan[i];
      readOneRadial(r, datatype, gateRange, ii);
    }
  }

  private void readOneRadial(Level2Record r, int datatype, Range gateRange, IndexIterator ii) throws IOException {
    if (r == null) {
      for (int i=gateRange.first(); i<=gateRange.last(); i+= gateRange.stride())
        ii.setByteNext( MISSING_DATA);
      return;
    }
    r.readData(raf, datatype, gateRange, ii);
  }

  private static class Vgroup {
    Level2Record[][] map;
    int datatype;

    Vgroup( int datatype, Level2Record[][] map) {
      this.datatype = datatype;
      this.map = map;
    }
  }

  /////////////////////////////////////////////////////////////////////


  public String getFileTypeId() {
    return DataFormatType.NEXRAD2.getDescription();
  }

  public String getFileTypeDescription() {
    return "NEXRAD Level-II Base Data";
  }

}