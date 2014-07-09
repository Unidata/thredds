/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.point.writer;

import java.io.IOException;
import java.util.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/**
 * Write a CF "Discrete Sample" station file.
 * Example H.7. Timeseries of station data in the indexed ragged array representation.
 *
 * <p/>
 * <pre>
 *   writeHeader()
 *   iterate { writeRecord() }
 *   finish()
 * </pre>
 *
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8340320"
 * @author caron
 * @since Aug 19, 2009
 */
public class WriterCFStationCollection extends CFPointWriter {
  private static final String stationStructName = "station";
  private static final String stationDimName = "station";

  private static final String idName = "station_id";
  private static final String descName = "station_description";
  private static final String wmoName = "wmo_id";
  private static final String stationIndexName = "stationIndex";
  private static final boolean debug = false;

  //////////////////////////////////////////////////////////
  private int name_strlen = 1, desc_strlen = 1, wmo_strlen = 1;

  protected Structure stationStruct;  // used for netcdf4 extended
  private boolean useDesc = false;
  private boolean useAlt = false;
  private boolean useWmoId = false;

  private Map<String, Variable> dataMap  = new HashMap<>();
  private Map<String, Variable> stnMap  = new HashMap<>();

  private Formatter coordNames = new Formatter();


  /* public WriterCFStationCollection(String fileOut, String title) throws IOException {
    this(fileOut, Arrays.asList(new Attribute(CDM.TITLE, title)), new CFPointWriterConfig(null));
  } */
  
  public WriterCFStationCollection(NetcdfFileWriter.Version version, String fileOut, List<Attribute> atts) throws IOException {
    super(fileOut, atts, new CFPointWriterConfig(version));
  }

  public WriterCFStationCollection(String fileOut, List<Attribute> atts, CFPointWriterConfig config) throws IOException {
    super(fileOut, atts, config);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.timeSeries.name()));
  }

  public void writeHeader(List<ucar.unidata.geoloc.Station> stns, List<VariableSimpleIF> dataVars, DateUnit timeUnit, String altUnits, StationPointFeature spf) throws IOException {
    this.dataVars = dataVars;
    this.altUnits = altUnits;
    if (noUnlimitedDimension)
      recordDim = writer.addDimension(null, recordDimName, config.recDimensionLength);
    else
      recordDim = writer.addUnlimitedDimension(recordDimName);

    StructureData obsData = spf.getFeatureData();
    StationFeature sf = spf.getStation();
    StructureData stnData = sf.getFeatureData();

    List<VariableSimpleIF> coords = new ArrayList<>();
    coords.add(VariableSimpleImpl.makeScalar(timeName, "time of measurement", timeUnit.getUnitsString(), DataType.DOUBLE));
    coords.add(VariableSimpleImpl.makeScalar(stationIndexName, "station index for this observation record", null, DataType.INT)
            .add(new Attribute(CF.INSTANCE_DIMENSION, stationDimName)));

    if (writer.getVersion().isExtendedModel()) {
      addStations(stns, stnData, true);
      record = (Structure) writer.addVariable(null, recordName, DataType.STRUCTURE, recordDimName);
      addCoordinatesExtended(record, coords);
      addDataVariablesExtended(obsData, coordNames.toString());
      record.calcElementSize();
      writer.create();
      writeStationDataExtended(stns);

    } else {
      addStations(stns, stnData, false);
      addCoordinatesClassic(recordDim, coords, dataMap);
      addDataVariablesClassic(recordDim, obsData, dataMap, coordNames.toString());
      writer.create();
      record = writer.addRecordStructure(); // netcdf3

      /* lat = writer.findVariable(latName);
      lon = writer.findVariable(lonName);
      alt = writer.findVariable(altName);
      id = writer.findVariable(idName);
      wmoId = writer.findVariable(wmoName);
      desc = writer.findVariable(descName);

      time = writer.findVariable(timeName);
      stationIndex = writer.findVariable(stationIndexName); */

      writeStationData(stns); // write out the station info
    }

  }

  private void addStations(List<ucar.unidata.geoloc.Station> stnList, StructureData stnData, boolean isExtended) throws IOException {
    int nstns = stnList.size();

    // see if there's altitude, wmoId for any stations
    for (Station stn : stnList) {
      if (!Double.isNaN(stn.getAltitude()))
        useAlt = true;
      if ((stn.getWmoId() != null) && (stn.getWmoId().trim().length() > 0))
        useWmoId = true;
      if ((stn.getDescription() != null) && (stn.getDescription().trim().length() > 0))
        useDesc = true;
    }

    // find string lengths
    for (Station station : stnList) {
      name_strlen = Math.max(name_strlen, station.getName().length());
      if (useDesc) desc_strlen = Math.max(desc_strlen, station.getDescription().length());
      if (useWmoId) wmo_strlen = Math.max(wmo_strlen, station.getWmoId().length());
    }

    llbb = getBoundingBox(stnList); // gets written in super.finish();

    // add the dimensions : extendded model can use an unlimited dimension
    //Dimension stationDim = isExtended ? writer.addDimension(null, stationDimName, 0, true, true, false) : writer.addDimension(null, stationDimName, nstns);
    Dimension stationDim = writer.addDimension(null, stationDimName, nstns);

    List<VariableSimpleIF> stnVars = new ArrayList<>();
    stnVars.add(VariableSimpleImpl.makeScalar(latName, "station latitude", CDM.LAT_UNITS, DataType.DOUBLE));
    stnVars.add(VariableSimpleImpl.makeScalar(lonName, "station longitude", CDM.LON_UNITS, DataType.DOUBLE));
    coordNames.format("%s %s %s", timeName, latName, lonName);

    if (useAlt) {
      stnVars.add(VariableSimpleImpl.makeScalar(altName, "station altitude", altUnits, DataType.DOUBLE)
              .add(new Attribute(CF.STANDARD_NAME, CF.SURFACE_ALTITUDE))
              .add(new Attribute(CF.POSITIVE, CF1Convention.getZisPositive(altName, altUnits))));
      coordNames.format(" %s", altName);
    }

    stnVars.add(VariableSimpleImpl.makeString(idName, "station identifier", null, name_strlen)
            .add(new Attribute(CF.CF_ROLE, CF.TIMESERIES_ID)));         // station_id:cf_role = "timeseries_id";

    if (useDesc) stnVars.add(VariableSimpleImpl.makeString(descName, "station description", null, desc_strlen)
            .add(new Attribute(CF.STANDARD_NAME, CF.PLATFORM_NAME)));

    if (useWmoId) stnVars.add(VariableSimpleImpl.makeString(wmoName, "station WMO id", null, wmo_strlen)
            .add(new Attribute(CF.STANDARD_NAME, CF.PLATFORM_ID)));

    for (StructureMembers.Member m : stnData.getMembers()) {
      if (getDataVar(m.getName()) != null)
        stnVars.add(new VariableSimpleAdapter(m));
    }

    if (isExtended) {
      stationStruct = (Structure) writer.addVariable(null, stationStructName, DataType.STRUCTURE, stationDimName);
      addCoordinatesExtended(stationStruct, stnVars);
    } else {
      addCoordinatesClassic(stationDim, stnVars, stnMap);
    }

  }

  // this writes all the stations - could just write out as stations come in from data records
  private HashMap<String, Integer> stationMap;
  private void writeStationData(List<ucar.unidata.geoloc.Station> stnList) throws IOException {
    int nstns = stnList.size();
    stationMap = new HashMap<>(2 * nstns);
    if (debug) System.out.println("stationMap created");

    // now write the station data
    ArrayDouble.D1 latArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 lonArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 altArray = new ArrayDouble.D1(nstns);
    ArrayObject.D1 idArray = new ArrayObject.D1(String.class, nstns);
    ArrayObject.D1 descArray = new ArrayObject.D1(String.class, nstns);
    ArrayObject.D1 wmoArray = new ArrayObject.D1(String.class, nstns);

    for (int i = 0; i < stnList.size(); i++) {
      ucar.unidata.geoloc.Station stn = stnList.get(i);
      stationMap.put(stn.getName(), i);

      latArray.set(i, stn.getLatitude());
      lonArray.set(i, stn.getLongitude());
      if (useAlt) altArray.set(i, stn.getAltitude());

      idArray.set(i, stn.getName());
      if (useDesc) descArray.set(i, stn.getDescription());
      if (useWmoId) wmoArray.set(i, stn.getWmoId());
    }

    try {
      writer.write(stnMap.get(latName), latArray);
      writer.write(stnMap.get(lonName), lonArray);
      if (useAlt) writer.write(stnMap.get(altName), altArray);
      writer.writeStringData(stnMap.get(idName), idArray);
      if (useDesc) writer.writeStringData(stnMap.get(descName), descArray);
      if (useWmoId) writer.writeStringData(stnMap.get(wmoName), wmoArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private void writeStationDataExtended(List<ucar.unidata.geoloc.Station> stnList) throws IOException {
    int nstns = stnList.size();
    stationMap = new HashMap<>(2 * nstns);
    if (debug) System.out.println("stationMap created");

    // now write the station data
    ArrayDouble.D1 latArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 lonArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 altArray = new ArrayDouble.D1(nstns);
    ArrayChar.D2 idArray = new ArrayChar.D2(nstns, name_strlen);
    ArrayChar.D2 descArray = new ArrayChar.D2(nstns, desc_strlen);
    ArrayChar.D2 wmoArray = new ArrayChar.D2(nstns, wmo_strlen);

    for (int i = 0; i < stnList.size(); i++) {
      ucar.unidata.geoloc.Station stn = stnList.get(i);
      stationMap.put(stn.getName(), i);

      latArray.set(i, stn.getLatitude());
      lonArray.set(i, stn.getLongitude());
      if (useAlt) altArray.set(i, stn.getAltitude());

      idArray.setString(i, stn.getName());
      if (useDesc) descArray.setString(i, stn.getDescription());
      if (useWmoId) wmoArray.setString(i, stn.getWmoId());
    }

    ArrayStructureMA ma = new ArrayStructureMA(stationStruct.makeStructureMembers(), new int[] {nstns});
    ma.setMemberArray(latName, latArray);
    ma.setMemberArray(lonName, lonArray);
    if (useAlt) ma.setMemberArray(altName, altArray);
    ma.setMemberArray(idName, idArray);
    if (useDesc) ma.setMemberArray(descName, descArray);
    if (useWmoId) ma.setMemberArray(wmoName, wmoArray);

    try {
      writer.write(stationStruct, ma);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

  }


  private int recno = 0;
  //private ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);
  //private ArrayInt.D1 parentArray = new ArrayInt.D1(1);

  public void writeRecord(Station s, PointFeature sobs, StructureData sdata) throws IOException {
    writeRecord(s.getName(), sobs.getObservationTime(), sobs.getObservationTimeAsCalendarDate(), sdata);
  }

  public void writeRecord(String stnName, double timeCoordValue, CalendarDate obsDate, StructureData sdata) throws IOException {
    trackBB(null, obsDate);

    Integer parentIndex = stationMap.get(stnName);
    if (parentIndex == null)
      throw new RuntimeException("Cant find station " + stnName);

    StructureDataScalar coords = new StructureDataScalar("Coords");
    coords.addMember(timeName, null, null, DataType.DOUBLE, false, timeCoordValue);
    coords.addMember(stationIndexName, null, null, DataType.INT, false, parentIndex);

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(coords); // coords first so it takes precedence
    sdall.add(sdata);

    // write the recno record
    int[] origin = new int[1];
    origin[0] = recno;
    try {
      boolean useStructure = isExtendedModel || (writer.getVersion() == NetcdfFileWriter.Version.netcdf3 && config.recDimensionLength < 0);
      if (useStructure)
        super.writeStructureData(record, origin, sdall);
      else {
        super.writeStructureDataClassic(dataMap, origin, sdall);
      }

/*      boolean useStructure = isExtendedModel || (writer.getVersion() == NetcdfFileWriter.Version.netcdf3 && config.recDimensionLength < 0);
      super.writeStructureData(useStructure, record, origin, sdall);

      if (!isExtendedModel) {
        timeArray.set(0, timeCoordValue);
        parentArray.set(0, parentIndex);

        writer.write(time, origin, timeArray);
        writer.write(stationIndex, origin, parentArray);
      }   */

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    recno++;
  }

  private LatLonRect getBoundingBox(List stnList) {
    ucar.unidata.geoloc.Station s = (ucar.unidata.geoloc.Station) stnList.get(0);
    LatLonPointImpl llpt = new LatLonPointImpl();
    llpt.set(s.getLatitude(), s.getLongitude());
    LatLonRect rect = new LatLonRect(llpt, .001, .001);

    for (int i = 1; i < stnList.size(); i++) {
      s = (ucar.unidata.geoloc.Station) stnList.get(i);
      llpt.set(s.getLatitude(), s.getLongitude());
      rect.extend(llpt);
    }

    return rect;
  }

  ////////////////////////////

  public static void main(String args[]) throws IOException {
    long start = System.currentTimeMillis();
    String outputFile = "G:/work/manross/split/872d794d.bufr.nc";
    String fDataset = "G:/work/manross/split/872d794d.bufr";

    System.out.println("WriterCFStationCollection from "+fDataset+" to "+outputFile);

        // open point dataset
    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, fDataset, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", fDataset, out);
      assert false;
    }
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;


    int count = CFPointWriter.writeFeatureCollection(fdpoint, outputFile, NetcdfFileWriter.Version.netcdf3);
    System.out.printf(" nrecords written = %d%n%n", count);


    long took = System.currentTimeMillis() - start;
    System.out.println("That took = " + took);

  }

}