// $Id:Cinrad2IOServiceProvider.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.cinrad;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.nexrad2.NexradStationDB;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

/**
 * An IOServiceProvider for CINRAD level II files.
 *
 *
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class Cinrad2IOServiceProvider implements IOServiceProvider {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Cinrad2IOServiceProvider.class);
  static private final int MISSING_INT = -9999;
  static private final float MISSING_FLOAT = Float.NaN;

  public boolean isValidFile( RandomAccessFile raf) {
    try {
      String loc = raf.getLocation();
      int posFirst = loc.lastIndexOf('/') + 1;
      if (posFirst < 0) posFirst = 0;
      String stationId = loc.substring(posFirst,posFirst+4);
      NexradStationDB.init();
      NexradStationDB.Station station = NexradStationDB.get("K"+ stationId);
      if(station != null )
          return true;
      else 
        return false;
    } catch (IOException ioe) {
        return false;
    }

  }

  private Cinrad2VolumeScan volScan;
  private Dimension radialDim;
  private double radarRadius;
  private DateFormatter formatter = new DateFormatter();

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    NexradStationDB.init();

    volScan = new Cinrad2VolumeScan( raf, cancelTask);
    if (volScan.hasDifferentDopplarResolutions())
      throw new IllegalStateException("volScan.hasDifferentDopplarResolutions");

    radialDim = new Dimension("radial", volScan.getMaxRadials(), true);
    ncfile.addDimension( null, radialDim);

    makeVariable( ncfile, Cinrad2Record.REFLECTIVITY, "Reflectivity", "Reflectivity", "R", volScan.getReflectivityGroups());
    int velocity_type =  (volScan.getDopplarResolution() == Cinrad2Record.DOPPLER_RESOLUTION_HIGH_CODE) ? Cinrad2Record.VELOCITY_HI : Cinrad2Record.VELOCITY_LOW;
    Variable v = makeVariable( ncfile, velocity_type, "RadialVelocity", "Radial Velocity", "V", volScan.getVelocityGroups());
    makeVariableNoCoords( ncfile, Cinrad2Record.SPECTRUM_WIDTH, "SpectrumWidth", "Spectrum Width", v);

    if (volScan.getStationId() != null) {
      ncfile.addAttribute(null, new Attribute("Station", volScan.getStationId()));
      ncfile.addAttribute(null, new Attribute("StationName", volScan.getStationName()));
      ncfile.addAttribute(null, new Attribute("StationLatitude", new Double(volScan.getStationLatitude())));
      ncfile.addAttribute(null, new Attribute("StationLongitude", new Double(volScan.getStationLongitude())));
      ncfile.addAttribute(null, new Attribute("StationElevationInMeters", new Double(volScan.getStationElevation())));

      double latRadiusDegrees = Math.toDegrees( radarRadius / ucar.unidata.geoloc.Earth.getRadius());
      ncfile.addAttribute(null, new Attribute("geospatial_lat_min", new Double(volScan.getStationLatitude() - latRadiusDegrees)));
      ncfile.addAttribute(null, new Attribute("geospatial_lat_max", new Double(volScan.getStationLatitude() + latRadiusDegrees)));
      double cosLat = Math.cos( Math.toRadians(volScan.getStationLatitude()));
      double lonRadiusDegrees = Math.toDegrees( radarRadius / cosLat / ucar.unidata.geoloc.Earth.getRadius());
      ncfile.addAttribute(null, new Attribute("geospatial_lon_min", new Double(volScan.getStationLongitude() - lonRadiusDegrees)));
      ncfile.addAttribute(null, new Attribute("geospatial_lon_max", new Double(volScan.getStationLongitude() + lonRadiusDegrees)));


          // add a radial coordinate transform (experimental)
      Variable ct = new Variable(ncfile, null, null, "radialCoordinateTransform");
      ct.setDataType(DataType.CHAR);
      ct.setDimensions(""); // scalar
      ct.addAttribute( new Attribute("transform_name", "Radial"));
      ct.addAttribute( new Attribute("center_latitude", new Double(volScan.getStationLatitude())));
      ct.addAttribute( new Attribute("center_longitude", new Double(volScan.getStationLongitude())));
      ct.addAttribute( new Attribute("center_elevation", new Double(volScan.getStationElevation())));
      ct.addAttribute( new Attribute(_Coordinate.TransformType, "Radial"));
      ct.addAttribute( new Attribute(_Coordinate.AxisTypes, "RadialElevation RadialAzimuth RadialDistance"));

      Array data = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[0], new char[] {' '});
      ct.setCachedData(data, true);
      ncfile.addVariable(null, ct);
    }

    DateFormatter formatter = new DateFormatter();

    ncfile.addAttribute(null, new Attribute("Conventions", _Coordinate.Convention));
    ncfile.addAttribute(null, new Attribute("format", volScan.getDataFormat()));
    //Date d = Cinrad2Record.getDate(volScan.getTitleJulianDays(), volScan.getTitleMsecs());
    //ncfile.addAttribute(null, new Attribute("base_date", formatter.toDateOnlyString(d)));

    ncfile.addAttribute(null, new Attribute("time_coverage_start", formatter.toDateTimeStringISO(volScan.getStartDate())));; //.toDateTimeStringISO(d)));
    ncfile.addAttribute(null, new Attribute("time_coverage_end", formatter.toDateTimeStringISO(volScan.getEndDate())));

    ncfile.addAttribute(null, new Attribute("history", "direct read of Nexrad Level 2 file into NetCDF-Java 2.2 API"));
    ncfile.addAttribute(null, new Attribute("DataType", "Radial"));

    ncfile.addAttribute(null, new Attribute("Title", "Nexrad Level 2 Station "+volScan.getStationId()+" from "+
        formatter.toDateTimeStringISO(volScan.getStartDate()) + " to " +
        formatter.toDateTimeStringISO(volScan.getEndDate())));

    ncfile.addAttribute(null, new Attribute("Summary", "Weather Surveillance Radar-1988 Doppler (WSR-88D) "+
        "Level II data are the three meteorological base data quantities: reflectivity, mean radial velocity, and "+
        "spectrum width."));

    ncfile.addAttribute(null, new Attribute("keywords", "WSR-88D; NEXRAD; Radar Level II; reflectivity; mean radial velocity; spectrum width"));

    ncfile.addAttribute(null, new Attribute("VolumeCoveragePatternName",
      Cinrad2Record.getVolumeCoveragePatternName(volScan.getVCP())));
    ncfile.addAttribute(null, new Attribute("VolumeCoveragePattern", new Integer(volScan.getVCP())));
    ncfile.addAttribute(null, new Attribute("HorizonatalBeamWidthInDegrees", new Double(Cinrad2Record.HORIZONTAL_BEAM_WIDTH)));

    ncfile.finish();
  }

  public Variable makeVariable(NetcdfFile ncfile, int datatype, String shortName, String longName, String abbrev, List groups) throws IOException {
    int nscans = groups.size();

    if (nscans == 0) {
      throw new IllegalStateException("No data for "+shortName);
    }

    // get representative record
    List firstGroup =  (List) groups.get(0);
    Cinrad2Record firstRecord = (Cinrad2Record) firstGroup.get(0);
    int ngates = firstRecord.getGateCount(datatype);

    String scanDimName = "scan"+abbrev;
    String gateDimName = "gate"+abbrev;
    Dimension scanDim = new Dimension(scanDimName, nscans, true);
    Dimension gateDim = new Dimension(gateDimName, ngates, true);
    ncfile.addDimension( null, scanDim);
    ncfile.addDimension( null, gateDim);

    ArrayList dims = new ArrayList();
    dims.add( scanDim);
    dims.add( radialDim);
    dims.add( gateDim);

    Variable v = new Variable(ncfile, null, null, shortName);
    v.setDataType(DataType.BYTE);
    v.setDimensions(dims);
    ncfile.addVariable(null, v);

    v.addAttribute( new Attribute("units", Cinrad2Record.getDatatypeUnits(datatype)));
    v.addAttribute( new Attribute("long_name", longName));


    byte[] b = new byte[2];
    b[0] = Cinrad2Record.MISSING_DATA;
    b[1] = Cinrad2Record.BELOW_THRESHOLD;
    Array missingArray = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[] {2}, b);

    v.addAttribute( new Attribute("missing_value", missingArray));
    v.addAttribute( new Attribute("signal_below_threshold", new Byte( Cinrad2Record.BELOW_THRESHOLD)));
    v.addAttribute( new Attribute("scale_factor", new Float( Cinrad2Record.getDatatypeScaleFactor(datatype))));
    v.addAttribute( new Attribute("add_offset", new Float( Cinrad2Record.getDatatypeAddOffset(datatype))));
    v.addAttribute( new Attribute("_unsigned", "true"));

    ArrayList dim2 = new ArrayList();
    dim2.add( scanDim);
    dim2.add( radialDim);

    // add time coordinate variable
    String timeCoordName = "time"+abbrev;
    Variable timeVar = new Variable(ncfile, null, null, timeCoordName);
    timeVar.setDataType(DataType.INT);
    timeVar.setDimensions(dim2);
    ncfile.addVariable(null, timeVar);


    // int julianDays = volScan.getTitleJulianDays();
    // Date d = Cinrad2Record.getDate( julianDays, 0);
    Date d = Cinrad2Record.getDate(volScan.getTitleJulianDays(), volScan.getTitleMsecs());
    String units = "msecs since "+formatter.toDateTimeStringISO(d);

    timeVar.addAttribute( new Attribute("long_name", "time since base date"));
    timeVar.addAttribute( new Attribute("units", units));
    timeVar.addAttribute( new Attribute("missing_value", new Integer(MISSING_INT)));
    timeVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    // add elevation coordinate variable
    String elevCoordName = "elevation"+abbrev;
    Variable elevVar = new Variable(ncfile, null, null, elevCoordName);
    elevVar.setDataType(DataType.FLOAT);
    elevVar.setDimensions(dim2);
    ncfile.addVariable(null, elevVar);

    elevVar.addAttribute( new Attribute("units", "degrees"));
    elevVar.addAttribute( new Attribute("long_name", "elevation angle in degres: 0 = parallel to pedestal base, 90 = perpendicular"));
    elevVar.addAttribute( new Attribute("missing_value", new Float(MISSING_FLOAT)));
    elevVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString()));

    // add azimuth coordinate variable
    String aziCoordName = "azimuth"+abbrev;
    Variable aziVar = new Variable(ncfile, null, null, aziCoordName);
    aziVar.setDataType(DataType.FLOAT);
    aziVar.setDimensions(dim2);
    ncfile.addVariable(null, aziVar);

    aziVar.addAttribute( new Attribute("units", "degrees"));
    aziVar.addAttribute( new Attribute("long_name", "azimuth angle in degrees: 0 = true north, 90 = east"));
    aziVar.addAttribute( new Attribute("missing_value", new Float(MISSING_FLOAT)));
    aziVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RadialAzimuth.toString()));

    // add gate coordinate variable
    String gateCoordName = "distance"+abbrev;
    Variable gateVar = new Variable(ncfile, null, null, gateCoordName);
    gateVar.setDataType(DataType.FLOAT);
    gateVar.setDimensions(gateDimName);
    Array data = NetcdfDataset.makeArray( DataType.FLOAT, ngates,
        (double) firstRecord.getGateStart(datatype), (double) firstRecord.getGateSize(datatype));
    gateVar.setCachedData( data, false);
    ncfile.addVariable(null, gateVar);
    radarRadius = firstRecord.getGateStart(datatype) + ngates * firstRecord.getGateSize(datatype);

    gateVar.addAttribute( new Attribute("units", "m"));
    gateVar.addAttribute( new Attribute("long_name", "radial distance to start of gate"));
    gateVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RadialDistance.toString()));

    // add number of radials variable
    String nradialsName = "numRadials"+abbrev;
    Variable nradialsVar = new Variable(ncfile, null, null, nradialsName);
    nradialsVar.setDataType(DataType.INT);
    nradialsVar.setDimensions(scanDim.getName());
    nradialsVar.addAttribute( new Attribute("long_name", "number of valid radials in this scan"));
    ncfile.addVariable(null, nradialsVar);

    // add number of gates variable
    String ngateName = "numGates"+abbrev;
    Variable ngateVar = new Variable(ncfile, null, null, ngateName);
    ngateVar.setDataType(DataType.INT);
    ngateVar.setDimensions(scanDim.getName());
    ngateVar.addAttribute( new Attribute("long_name", "number of valid gates in this scan"));
    ncfile.addVariable(null, ngateVar);

    makeCoordinateDataWithMissing( datatype, timeVar, elevVar, aziVar, nradialsVar, ngateVar, groups);

    // back to the data variable
    String coordinates = timeCoordName+" "+elevCoordName +" "+ aziCoordName+" "+gateCoordName;
    v.addAttribute( new Attribute(_Coordinate.Axes, coordinates));

    // make the record map
    int nradials = radialDim.getLength();
    Cinrad2Record[][] map = new Cinrad2Record[nscans][nradials];
    for (int i = 0; i < groups.size(); i++) {
      Cinrad2Record[] mapScan = map[i];
      List group = (List) groups.get(i);
      for (int j = 0; j < group.size(); j++) {
        Cinrad2Record r =  (Cinrad2Record) group.get(j);
        int radial = r.radial_num-1;
        mapScan[radial] = r;
      }
    }

    Vgroup vg = new Vgroup(datatype, map);
    v.setSPobject( vg);

    return v;
   }

  private void makeVariableNoCoords(NetcdfFile ncfile, int datatype, String shortName, String longName, Variable from) {

    Variable v = new Variable(ncfile, null, null, shortName);
    v.setDataType(DataType.BYTE);
    v.setDimensions( from.getDimensions());
    ncfile.addVariable(null, v);

    v.addAttribute( new Attribute("units", Cinrad2Record.getDatatypeUnits(datatype)));
    v.addAttribute( new Attribute("long_name", longName));

    byte[] b = new byte[2];
    b[0] = Cinrad2Record.MISSING_DATA;
    b[1] = Cinrad2Record.BELOW_THRESHOLD;
    Array missingArray = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[] {2}, b);

    v.addAttribute( new Attribute("missing_value", missingArray));
    v.addAttribute( new Attribute("signal_below_threshold", new Byte( Cinrad2Record.BELOW_THRESHOLD)));
    v.addAttribute( new Attribute("scale_factor", new Float( Cinrad2Record.getDatatypeScaleFactor(datatype))));
    v.addAttribute( new Attribute("add_offset", new Float( Cinrad2Record.getDatatypeAddOffset(datatype))));
    v.addAttribute( new Attribute("_unsigned", "true"));

    Attribute fromAtt = from.findAttribute(_Coordinate.Axes);
    v.addAttribute( new Attribute(_Coordinate.Axes, fromAtt));

    Vgroup vgFrom = (Vgroup) from.getSPobject();
    Vgroup vg = new Vgroup(datatype, vgFrom.map);
    v.setSPobject( vg);
  }

  private void makeCoordinateData(int datatype, Variable time, Variable elev, Variable azi, Variable nradialsVar,
                                  Variable ngatesVar, List groups) {

    Array timeData = Array.factory( time.getDataType().getPrimitiveClassType(), time.getShape());
    IndexIterator timeDataIter = timeData.getIndexIterator();

    Array elevData = Array.factory( elev.getDataType().getPrimitiveClassType(), elev.getShape());
    IndexIterator elevDataIter = elevData.getIndexIterator();

    Array aziData = Array.factory( azi.getDataType().getPrimitiveClassType(), azi.getShape());
    IndexIterator aziDataIter = aziData.getIndexIterator();

    Array nradialsData = Array.factory( nradialsVar.getDataType().getPrimitiveClassType(), nradialsVar.getShape());
    IndexIterator nradialsIter = nradialsData.getIndexIterator();

    Array ngatesData = Array.factory( ngatesVar.getDataType().getPrimitiveClassType(), ngatesVar.getShape());
    IndexIterator ngatesIter = ngatesData.getIndexIterator();


    int last_msecs = Integer.MIN_VALUE;
    int nscans = groups.size();
    int maxRadials = volScan.getMaxRadials();
    for (int i = 0; i < nscans; i++) {
      List scanGroup = (List) groups.get(i);
      int nradials = scanGroup.size();

      Cinrad2Record first = null;
      for (int j = 0; j < nradials; j++) {
        Cinrad2Record r =  (Cinrad2Record) scanGroup.get(j);
        if (first == null) first = r;

        timeDataIter.setIntNext( r.data_msecs);
        elevDataIter.setFloatNext( r.getElevation());
        aziDataIter.setFloatNext( r.getAzimuth());

        if (r.data_msecs < last_msecs) logger.warn("makeCoordinateData time out of order "+r.data_msecs);
        last_msecs = r.data_msecs;
      }

      for (int j = nradials; j < maxRadials; j++) {
        timeDataIter.setIntNext( MISSING_INT);
        elevDataIter.setFloatNext( MISSING_FLOAT);
        aziDataIter.setFloatNext( MISSING_FLOAT);
      }

      nradialsIter.setIntNext( nradials);
      ngatesIter.setIntNext( first.getGateCount( datatype));
    }

    time.setCachedData( timeData, false);
    elev.setCachedData( elevData, false);
    azi.setCachedData( aziData, false);
    nradialsVar.setCachedData( nradialsData, false);
    ngatesVar.setCachedData( ngatesData, false);
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

    // now set the  coordinate variables from the Cinrad2Record radial
    int last_msecs = Integer.MIN_VALUE;
    int nscans = groups.size();
    try {
    for (int scan = 0; scan < nscans; scan++) {
      List scanGroup = (List) groups.get(scan);
      int nradials = scanGroup.size();

      Cinrad2Record first = null;
      for (int j = 0; j < nradials; j++) {
        Cinrad2Record r =  (Cinrad2Record) scanGroup.get(j);
        if (first == null) first = r;

        int radial = r.radial_num-1;
        timeData.setInt( timeIndex.set(scan, radial), r.data_msecs);
        elevData.setFloat( elevIndex.set(scan, radial), r.getElevation());
        aziData.setFloat( aziIndex.set(scan, radial), r.getAzimuth());

        if (r.data_msecs < last_msecs) logger.warn("makeCoordinateData time out of order "+r.data_msecs);
        last_msecs = r.data_msecs;
      }

      nradialsIter.setIntNext( nradials);
      ngatesIter.setIntNext( first.getGateCount( datatype));
    } }catch(java.lang.ArrayIndexOutOfBoundsException  ae) {
          logger.debug("Cinrad2IOSP.uncompress ", ae);
    }
    time.setCachedData( timeData, false);
    elev.setCachedData( elevData, false);
    azi.setCachedData( aziData, false);
    nradialsVar.setCachedData( nradialsData, false);
    ngatesVar.setCachedData( ngatesData, false);
  }

  public Array readData(Variable v2, List section) throws IOException, InvalidRangeException {
    Vgroup vgroup = (Vgroup) v2.getSPobject();

    Range scanRange = (Range) section.get(0);
    Range radialRange = (Range) section.get(1);
    Range gateRange = (Range) section.get(2);

    Array data = Array.factory(v2.getDataType().getPrimitiveClassType(), Range.getShape( section));
    IndexIterator ii = data.getIndexIterator();

    for (int i=scanRange.first(); i<=scanRange.last(); i+= scanRange.stride()) {
      Cinrad2Record[] mapScan = vgroup.map[i];
      readOneScan(mapScan, radialRange, gateRange, vgroup.datatype, ii);
    }

    return data;
  }

  private void readOneScan(Cinrad2Record[] mapScan, Range radialRange, Range gateRange, int datatype, IndexIterator ii) throws IOException {
    for (int i=radialRange.first(); i<=radialRange.last(); i+= radialRange.stride()) {
      Cinrad2Record r = mapScan[i];
      readOneRadial(r, datatype, gateRange, ii);
    }
  }

  private void readOneRadial(Cinrad2Record r, int datatype, Range gateRange, IndexIterator ii) throws IOException {
    if (r == null) {
      for (int i=gateRange.first(); i<=gateRange.last(); i+= gateRange.stride())
        ii.setByteNext( Cinrad2Record.MISSING_DATA);
      return;
    }
    r.readData(volScan.raf, datatype, gateRange, ii);
  }

  private class Vgroup {
    Cinrad2Record[][] map;
    int datatype;

    Vgroup( int datatype, Cinrad2Record[][] map) {
      this.datatype = datatype;
      this.map = map;
    }
  }

  /////////////////////////////////////////////////////////////////////

  public Array readNestedData(Variable v2, List section) throws IOException, InvalidRangeException {
    throw new UnsupportedOperationException("Cinrad2 IOSP does not support nested variables");
  }

  public void close() throws IOException {
    volScan.raf.close();
  }

  public boolean syncExtend() { return false; }
  public boolean sync() { return false; }

  public void setSpecial( Object special) {}

  public String toStringDebug(Object o) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public String getDetailInfo() { return ""; }
}