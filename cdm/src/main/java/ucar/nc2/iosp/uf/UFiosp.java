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
package ucar.nc2.iosp.uf;

import ucar.nc2.constants.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;

import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.*;

import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Sep 25, 2008
 * Time: 10:23:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class UFiosp extends AbstractIOServiceProvider {
  static private final int MISSING_INT = -9999;
  static private final float MISSING_FLOAT = Float.NaN;
  protected UFheader headerParser;

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
    UFheader localHeader = new UFheader();
    return (localHeader.isValidFile(raf));
  }

  public String getFileTypeId() {
    return "UniversalRadarFormat";
  }

  public String getFileTypeDescription() {
    return "Universal Radar Format";
  }

  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);

    headerParser = new UFheader();
    headerParser.read(this.raf);
    //Map variables = headerParser.variableGroup;

    for (Map.Entry<String,List<List<Ray>>> entry : headerParser.variableGroup.entrySet()) {
      String key = entry.getKey();
      List<List<Ray>> groups = entry.getValue();
      List<Ray> rays = groups.get(0);
      Ray ray0 = rays.get(0);
      makeVariable(ncfile, ray0.getDatatypeName(key), ray0.getDatatypeName(key), key, groups);
    }


    ncfile.addAttribute(null, new Attribute(CDM.CONVENTIONS, _Coordinate.Convention));
    ncfile.addAttribute(null, new Attribute("format", headerParser.getDataFormat()));
    ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.RADIAL.toString()));
    ncfile.addAttribute(null, new Attribute("instrument_name", headerParser.getRadarName()));
    ncfile.addAttribute(null, new Attribute("site_name", headerParser.getSiteName()));
    //Date d = Cinrad2Record.getDate(volScan.getTitleJulianDays(), volScan.getTitleMsecs());
    //ncfile.addAttribute(null, new Attribute("base_date", formatter.toDateOnlyString(d)));
    ncfile.addAttribute(null, new Attribute("StationLatitude", (double) headerParser.getStationLatitude()));
    ncfile.addAttribute(null, new Attribute("StationLongitude", (double) headerParser.getStationLongitude()));
    ncfile.addAttribute(null, new Attribute("StationElevationInMeters", (double) headerParser.getStationElevation()));
    ncfile.addAttribute(null, new Attribute("time_coverage_start", formatter.toDateTimeStringISO(headerParser.getStartDate())));
    //.toDateTimeStringISO(d)));
    ncfile.addAttribute(null, new Attribute("time_coverage_end", formatter.toDateTimeStringISO(headerParser.getEndDate())));
    double latRadiusDegrees = Math.toDegrees(radarRadius / ucar.unidata.geoloc.Earth.getRadius());
    ncfile.addAttribute(null, new Attribute("geospatial_lat_min", headerParser.getStationLatitude() - latRadiusDegrees));
    ncfile.addAttribute(null, new Attribute("geospatial_lat_max", headerParser.getStationLatitude() + latRadiusDegrees));
    double cosLat = Math.cos(Math.toRadians(headerParser.getStationLatitude()));
    double lonRadiusDegrees = Math.toDegrees(radarRadius / cosLat / ucar.unidata.geoloc.Earth.getRadius());
    ncfile.addAttribute(null, new Attribute("geospatial_lon_min", headerParser.getStationLongitude() - lonRadiusDegrees));
    ncfile.addAttribute(null, new Attribute("geospatial_lon_max", headerParser.getStationLongitude() + lonRadiusDegrees));
    ncfile.addAttribute(null, new Attribute(CDM.HISTORY, "Direct read of UF Radar by CDM (version 4.5)"));
    ncfile.addAttribute(null, new Attribute("DataType", "Radial"));

    ncfile.addAttribute(null, new Attribute("Title", "Radar Data from station" +
            " " + headerParser.getRadarName() + " from " +
            formatter.toDateTimeStringISO(headerParser.getStartDate()) + " to " +
            formatter.toDateTimeStringISO(headerParser.getEndDate())));

    ncfile.addAttribute(null, new Attribute("SweepMode", headerParser.getSweepMode()));

    // ncfile.addAttribute(null, new Attribute("VolumeCoveragePattern", new Integer(headerParser.getVCP())));
    // ncfile.addAttribute(null, new Attribute("HorizonatalBeamWidthInDegrees", new Double(headerParser.getHorizontalBeamWidth(abbrev))));

    ncfile.finish();
  }


  private DateFormatter formatter = new DateFormatter();
  private double radarRadius = 100000.0;

  public Variable makeVariable(NetcdfFile ncfile, String shortName, String longName,
                               String abbrev, List<List<Ray>> groups) throws IOException {
    int nscans = groups.size();

    if (nscans == 0) {
      throw new IllegalStateException("No data for " + shortName);
    }

    // get representative record
    List<Ray> firstGroup = groups.get(0);
    Ray firstRay = firstGroup.get(0);
    int ngates = firstRay.getGateCount(abbrev);

    String scanDimName = "scan" + abbrev;
    String gateDimName = "gate" + abbrev;
    String radialDimName = "radial" + abbrev;
    Dimension scanDim = new Dimension(scanDimName, nscans);
    Dimension gateDim = new Dimension(gateDimName, ngates);
    Dimension radialDim = new Dimension(radialDimName, headerParser.getMaxRadials(), true);
    ncfile.addDimension(null, scanDim);
    ncfile.addDimension(null, gateDim);
    ncfile.addDimension(null, radialDim);

    List<Dimension> dims = new ArrayList<>();
    dims.add(scanDim);
    dims.add(radialDim);
    dims.add(gateDim);

    Variable v = new Variable(ncfile, null, null, shortName + abbrev);
    v.setDataType(DataType.SHORT);
    v.setDimensions(dims);
    ncfile.addVariable(null, v);

    v.addAttribute(new Attribute(CDM.UNITS, firstRay.getDatatypeUnits(abbrev)));
    v.addAttribute(new Attribute(CDM.LONG_NAME, longName));
    v.addAttribute(new Attribute("abbrev", abbrev));
    v.addAttribute(new Attribute(CDM.MISSING_VALUE, firstRay.getMissingData()));
    v.addAttribute(new Attribute("signal_below_threshold", firstRay.getDatatypeRangeFoldingThreshhold(abbrev)));
    v.addAttribute(new Attribute(CDM.SCALE_FACTOR, firstRay.getDatatypeScaleFactor(abbrev)));
    v.addAttribute(new Attribute(CDM.ADD_OFFSET, firstRay.getDatatypeAddOffset(abbrev)));
    // v.addAttribute( new Attribute("_Unsigned", "false"));

    v.addAttribute(new Attribute("range_folding_threshold", firstRay.getDatatypeRangeFoldingThreshhold(abbrev)));

    List<Dimension> dim2 = new ArrayList<>();
    dim2.add(scanDim);
    dim2.add(radialDim);

    // add time coordinate variable
    String timeCoordName = "time" + abbrev;
    Variable timeVar = new Variable(ncfile, null, null, timeCoordName);
    timeVar.setDataType(DataType.INT);
    timeVar.setDimensions(dim2);
    ncfile.addVariable(null, timeVar);


    // int julianDays = volScan.getTitleJulianDays();
    // Date d = Level2Record.getDate( julianDays, 0);
    Date d = firstRay.getDate();
    String units = "msecs since " + formatter.toDateTimeStringISO(d);

    timeVar.addAttribute(new Attribute(CDM.LONG_NAME, "time since base date"));
    timeVar.addAttribute(new Attribute(CDM.UNITS, units));
    timeVar.addAttribute(new Attribute(CDM.MISSING_VALUE, firstRay.getMissingData()));
    timeVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    // add elevation coordinate variable
    String elevCoordName = "elevation" + abbrev;
    Variable elevVar = new Variable(ncfile, null, null, elevCoordName);
    elevVar.setDataType(DataType.FLOAT);
    elevVar.setDimensions(dim2);
    ncfile.addVariable(null, elevVar);

    elevVar.addAttribute(new Attribute(CDM.UNITS, "degrees"));
    elevVar.addAttribute(new Attribute(CDM.LONG_NAME, "elevation angle in degrees: 0 = parallel to pedestal base, 90 = perpendicular"));
    elevVar.addAttribute(new Attribute(CDM.MISSING_VALUE, firstRay.getMissingData()));
    elevVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString()));

    // add azimuth coordinate variable
    String aziCoordName = "azimuth" + abbrev;
    Variable aziVar = new Variable(ncfile, null, null, aziCoordName);
    aziVar.setDataType(DataType.FLOAT);
    aziVar.setDimensions(dim2);
    ncfile.addVariable(null, aziVar);

    aziVar.addAttribute(new Attribute(CDM.UNITS, "degrees"));
    aziVar.addAttribute(new Attribute(CDM.LONG_NAME, "azimuth angle in degrees: 0 = true north, 90 = east"));
    aziVar.addAttribute(new Attribute(CDM.MISSING_VALUE, firstRay.getMissingData()));
    aziVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RadialAzimuth.toString()));

    // add gate coordinate variable
    String gateCoordName = "distance" + abbrev;
    Variable gateVar = new Variable(ncfile, null, null, gateCoordName);
    gateVar.setDataType(DataType.FLOAT);
    gateVar.setDimensions(gateDimName);
    Array data = Array.makeArray(DataType.FLOAT, ngates,
            (double) firstRay.getGateStart(abbrev), (double) firstRay.getGateSize(abbrev));
    gateVar.setCachedData(data, false);
    ncfile.addVariable(null, gateVar);
    //      radarRadius = firstRay.getGateStart(datatype) + ngates * firstRay.getGateSize(datatype);

    gateVar.addAttribute(new Attribute(CDM.UNITS, "m"));
    gateVar.addAttribute(new Attribute(CDM.LONG_NAME, "radial distance to start of gate"));
    gateVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RadialDistance.toString()));

    // add number of radials variable
    String nradialsName = "numRadials" + abbrev;
    Variable nradialsVar = new Variable(ncfile, null, null, nradialsName);
    nradialsVar.setDataType(DataType.INT);
    nradialsVar.setDimensions(scanDim.getShortName());
    nradialsVar.addAttribute(new Attribute(CDM.LONG_NAME, "number of valid radials in this scan"));
    ncfile.addVariable(null, nradialsVar);

    // add number of gates variable
    String ngateName = "numGates" + abbrev;
    Variable ngateVar = new Variable(ncfile, null, null, ngateName);
    ngateVar.setDataType(DataType.INT);
    ngateVar.setDimensions(scanDim.getShortName());
    ngateVar.addAttribute(new Attribute(CDM.LONG_NAME, "number of valid gates in this scan"));
    ncfile.addVariable(null, ngateVar);

    makeCoordinateDataWithMissing(abbrev, timeVar, elevVar, aziVar, nradialsVar, ngateVar, groups);

    // back to the data variable
    String coordinates = timeCoordName + " " + elevCoordName + " " + aziCoordName + " " + gateCoordName;
    v.addAttribute(new Attribute(_Coordinate.Axes, coordinates));

    // make the ray map
    int nradials = radialDim.getLength();
    Ray[][] map = new Ray[nscans][nradials];
    for (int i = 0; i < groups.size(); i++) {
      Ray[] mapScan = map[i];
      List<Ray> group = groups.get(i);
      int radial = 0;
      for (Ray r : group) {
        mapScan[radial] = r;
        radial++;
      }
    }

    Vgroup vg = new Vgroup(abbrev, map);
    v.setSPobject(vg);

    return v;
  }

  private void makeCoordinateDataWithMissing(String abbrev, Variable time, Variable elev, Variable azi, Variable nradialsVar,
                                             Variable ngatesVar, List<List<Ray>> groups) {

    Array timeData = Array.factory(time.getDataType().getPrimitiveClassType(), time.getShape());
    Index timeIndex = timeData.getIndex();

    Array elevData = Array.factory(elev.getDataType().getPrimitiveClassType(), elev.getShape());
    Index elevIndex = elevData.getIndex();

    Array aziData = Array.factory(azi.getDataType().getPrimitiveClassType(), azi.getShape());
    Index aziIndex = aziData.getIndex();

    Array nradialsData = Array.factory(nradialsVar.getDataType().getPrimitiveClassType(), nradialsVar.getShape());
    IndexIterator nradialsIter = nradialsData.getIndexIterator();

    Array ngatesData = Array.factory(ngatesVar.getDataType().getPrimitiveClassType(), ngatesVar.getShape());
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

    int nscans = groups.size();
    try {
      for (int scan = 0; scan < nscans; scan++) {
        List<Ray> scanGroup = groups.get(scan);
        int nradials = scanGroup.size();

        int radial = 0;
        boolean needFirst = true;
        for (Ray r : scanGroup) {
          if (needFirst)
          {
              ngatesIter.setIntNext(r.getGateCount(abbrev));
              needFirst = false;
          }

          // int radial = r.uf_header2.rayNumber ;
          // if(scan == 0) System.out.println("AZI " + r.getAzimuth());
          timeData.setLong(timeIndex.set(scan, radial), r.data_msecs);
          elevData.setFloat(elevIndex.set(scan, radial), r.getElevation());
          aziData.setFloat(aziIndex.set(scan, radial), r.getAzimuth());
          radial++;
        }

        nradialsIter.setIntNext(nradials);
      }
    } catch (java.lang.ArrayIndexOutOfBoundsException ae) {

    }
    time.setCachedData(timeData, false);
    elev.setCachedData(elevData, false);
    azi.setCachedData(aziData, false);
    nradialsVar.setCachedData(nradialsData, false);
    ngatesVar.setCachedData(ngatesData, false);
  }

  private static class Vgroup {
    Ray[][] map;
    String abbrev;

    Vgroup(String abbrev, Ray[][] map) {
      this.abbrev = abbrev;
      this.map = map;
    }
  }

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Vgroup vgroup = (Vgroup) v2.getSPobject();

    Range scanRange = section.getRange(0);
    Range radialRange = section.getRange(1);
    Range gateRange = section.getRange(2);

    Array data = Array.factory(v2.getDataType().getPrimitiveClassType(), section.getShape());
    IndexIterator ii = data.getIndexIterator();

    for (int i = scanRange.first(); i <= scanRange.last(); i += scanRange.stride()) {
      Ray[] mapScan = vgroup.map[i];
      readOneScan(mapScan, radialRange, gateRange, vgroup.abbrev, ii);
    }

    return data;
  }

  private void readOneScan(Ray[] mapScan, Range radialRange, Range gateRange, String abbrev, IndexIterator ii) throws IOException {
    for (int i = radialRange.first(); i <= radialRange.last(); i += radialRange.stride()) {
      Ray r = mapScan[i];
      readOneRadial(r, abbrev, gateRange, ii);
    }
  }

  private void readOneRadial(Ray r, String abbrev, Range gateRange, IndexIterator ii) throws IOException {
    if (r == null) {
      for (int i = gateRange.first(); i <= gateRange.last(); i += gateRange.stride())
        ii.setShortNext(headerParser.getMissingData());
      return;
    }
    r.readData(raf, abbrev, gateRange, ii);
  }



}
