/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1;

import thredds.catalog.DataFormatType;
import thredds.inventory.CollectionManager;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib1.tables.Grib1ParamTables;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * IOSP for GRIB1 collections
 *
 * @author John
 * @since 9/5/11
 */
public class Grib1Iosp extends GribIosp {

  static private final float MISSING_VALUE = Float.NaN;
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1Iosp.class);
  static private final boolean debugTime = false, debugRead = false;

  static public String makeVariableLongName(Grib1Customizer cust, GribCollection gribCollection, GribCollection.VariableIndex vindex) {
    return cust.makeVariableLongName(gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.tableVersion, vindex.parameter,
            vindex.levelType, vindex.intvType, vindex.intvName, vindex.isLayer, vindex.probabilityName);
  }

  static public String makeVariableUnits(Grib1Customizer cust, GribCollection gribCollection, GribCollection.VariableIndex vindex) {
    return cust.makeVariableUnits(gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.tableVersion, vindex.parameter);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Grib1TimePartition timePartition;
  private GribCollection gribCollection;
  private Grib1Customizer cust;
  private GribCollection.GroupHcs gHcs;
  private boolean isTimePartitioned;
  private boolean owned; // if Iosp is owned by GribCollection; affects close()

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    byte[] b = new byte[Grib1CollectionBuilder.MAGIC_START.length()];
    raf.readFully(b);
    String magic = new String(b);

    // check if its an ncx file
    if (magic.equals(Grib1CollectionBuilder.MAGIC_START)) return true;
    if (magic.equals(Grib1TimePartitionBuilder.MAGIC_START)) return true;

    // check for GRIB1 file
    return Grib1RecordScanner.isValidFile(raf);
  }

  @Override
  public String getFileTypeId() {
    return DataFormatType.GRIB1.toString();
  }

  @Override
  public String getFileTypeDescription() {
    return "GRIB1 Collection";
  }

  // public no-arg constructor for reflection
  public Grib1Iosp() {
  }

  // called from GribCollection
  public Grib1Iosp(GribCollection.GroupHcs gHcs) {
    this.gHcs = gHcs;
    this.owned = true;
  }

  // called from GribCollection
  public Grib1Iosp(GribCollection gc) {
    this.gribCollection = gc;
    this.owned = true;
  }

  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);
    Grib1ParamTables tables = (gribConfig.paramTable != null) ? Grib1ParamTables.factory(gribConfig.paramTable) :
            Grib1ParamTables.factory(gribConfig.paramTablePath, gribConfig.lookupTablePath); // so an iosp message must be received before the open()

    // create the gbx9 index file if not already there
    boolean isGrib = (raf != null) && Grib1RecordScanner.isValidFile(raf);
    if (isGrib) {
      this.gribCollection = GribIndex.makeGribCollectionFromSingleFile(true, raf, gribConfig, CollectionManager.Force.test, logger);
      cust = Grib1Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getLocal(), tables);
    }

    if (gHcs != null) { // just use the one group that was set in the constructor
      this.gribCollection = gHcs.getGribCollection();
      if (this.gribCollection instanceof Grib1TimePartition) {
        isTimePartitioned = true;
        timePartition = (Grib1TimePartition) gribCollection;
      }
      cust = Grib1Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getLocal(), tables);
      addGroup(ncfile, gHcs, false);

    } else if (gribCollection != null) { // use the gribCollection set in the constructor
      if (this.gribCollection instanceof Grib1TimePartition) {
        isTimePartitioned = true;
        timePartition = (Grib1TimePartition) gribCollection;
      }
      cust = Grib1Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getLocal(), tables);
      boolean useGroups = gribCollection.getGroups().size() > 1;

      List<GribCollection.GroupHcs> groups = new ArrayList<GribCollection.GroupHcs>(gribCollection.getGroups());
      Collections.sort(groups);
      for (GribCollection.GroupHcs g : groups)
        addGroup(ncfile, g, useGroups);

    } else { // the raf is a collection index (ncx) - only on client

      raf.seek(0);
      byte[] b = new byte[Grib1TimePartitionBuilder.MAGIC_START.length()];
      raf.readFully(b);
      String magic = new String(b);
      isTimePartitioned = magic.equals(Grib1TimePartitionBuilder.MAGIC_START);

      String location = raf.getLocation();
      File f = new File(location);
      int pos = f.getName().lastIndexOf(".");
      String name = (pos > 0) ? f.getName().substring(0, pos) : f.getName();

      if (isTimePartitioned) {
        timePartition = Grib1TimePartitionBuilder.createFromIndex(name, null, raf, logger);
        gribCollection = timePartition;
      } else {
        gribCollection = Grib1CollectionBuilder.createFromIndex(name, null, raf, gribConfig, logger);
      }
      cust = Grib1Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getLocal(), tables);

      List<GribCollection.GroupHcs> groups = new ArrayList<GribCollection.GroupHcs>(gribCollection.getGroups());
      Collections.sort(groups);
      boolean useGroups = groups.size() > 1;
      for (GribCollection.GroupHcs g : groups)
        addGroup(ncfile, g, useGroups);
    }

    String val = CommonCodeTable.getCenterName(gribCollection.getCenter(), 2);
    ncfile.addAttribute(null, new Attribute(GribUtils.CENTER, val == null ? Integer.toString(gribCollection.getCenter()) : val));
    val = cust.getSubCenterName(gribCollection.getSubcenter());
    ncfile.addAttribute(null, new Attribute(GribUtils.SUBCENTER, val == null ? Integer.toString(gribCollection.getSubcenter()) : val));
    //ncfile.addAttribute(null, new Attribute("GRIB table version", gribCollection.getLocal()));
    //ncfile.addAttribute(null, new Attribute("GRIB table", gribCollection.getCenter()+"-"+gribCollection.getSubcenter()+"-"+gribCollection.getLocal()));

    val = cust.getGeneratingProcessName(gribCollection.getGenProcessId());
    if (val != null)
      ncfile.addAttribute(null, new Attribute(GribUtils.GEN_PROCESS, val));

    ncfile.addAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.6"));
    ncfile.addAttribute(null, new Attribute(CDM.HISTORY, "Read using CDM IOSP Grib1Collection"));
    ncfile.addAttribute(null, new Attribute(CF.FEATURE_TYPE, FeatureType.GRID.name()));
    ncfile.addAttribute(null, new Attribute(CDM.FILE_FORMAT, getFileTypeId()));

    for (Parameter p : gribCollection.getParams())
      ncfile.addAttribute(null, new Attribute(p));
  }

  private void addGroup(NetcdfFile ncfile, GribCollection.GroupHcs gHcs, boolean useGroups) {
    gHcs.assignVertNames(cust);
    GdsHorizCoordSys hcs = gHcs.hcs;
    String grid_mapping = hcs.getName() + "_Projection";

    Group g;
    if (useGroups) {
      g = new Group(ncfile, null, gHcs.getId());
      g.addAttribute(new Attribute(CDM.LONG_NAME, gHcs.getDescription()));
      try {
        ncfile.addGroup(null, g);
      } catch (Exception e) {
        logger.warn("Duplicate Group - skipping");
        return;
      }
    } else {
      g = ncfile.getRootGroup();
    }

    String horizDims;
    if (hcs == null) {
      logger.error("No GdsHorizCoordSys for gds template {} center {}", gHcs.hcs.template, gribCollection.getCenter());
      throw new IllegalStateException();
    }

    if (hcs.isLatLon()) {
      horizDims = "lat lon";
      ncfile.addDimension(g, new Dimension("lon", hcs.nx));
      ncfile.addDimension(g, new Dimension("lat", hcs.ny));

      Variable cv = ncfile.addVariable(g, new Variable(ncfile, g, null, "lat", DataType.FLOAT, "lat"));
      cv.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
      if (hcs.gaussLats != null)
        cv.setCachedData(hcs.gaussLats); //  LOOK do we need to make a copy?
      else
        cv.setCachedData(Array.makeArray(DataType.FLOAT, hcs.ny, hcs.starty, hcs.dy));

      cv = ncfile.addVariable(g, new Variable(ncfile, g, null, "lon", DataType.FLOAT, "lon"));
      cv.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
      cv.setCachedData(Array.makeArray(DataType.FLOAT, hcs.nx, hcs.startx, hcs.dx));

    } else {
      // make horiz coordsys coordinate variable
      Variable hcsV = ncfile.addVariable(g, new Variable(ncfile, g, null, grid_mapping, DataType.INT, ""));
      hcsV.setCachedData(Array.factory(DataType.INT, new int[0], new int[]{0}));
      for (Parameter p : hcs.proj.getProjectionParameters())
        hcsV.addAttribute(new Attribute(p));

      horizDims = "y x";
      ncfile.addDimension(g, new Dimension("x", hcs.nx));
      ncfile.addDimension(g, new Dimension("y", hcs.ny));

      Variable cv = ncfile.addVariable(g, new Variable(ncfile, g, null, "x", DataType.FLOAT, "x"));
      cv.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
      cv.addAttribute(new Attribute(CDM.UNITS, "km"));
      cv.setCachedData(Array.makeArray(DataType.FLOAT, hcs.nx, hcs.startx, hcs.dx));

      cv = ncfile.addVariable(g, new Variable(ncfile, g, null, "y", DataType.FLOAT, "y"));
      cv.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
      cv.addAttribute(new Attribute(CDM.UNITS, "km"));
      cv.setCachedData(Array.makeArray(DataType.FLOAT, hcs.ny, hcs.starty, hcs.dy));
    }

    for (VertCoord vc : gHcs.vertCoords) {
      addVerticalCoordinate(ncfile, g, vc);
    }

    for (TimeCoord tc : gHcs.timeCoords) {
      addTimeCoordinate(ncfile, g, tc);
    }

    int ccount = 0;
    for (EnsCoord ec : gHcs.ensCoords) {
      int n = ec.getSize();
      String ecName = "ens" + ccount;
      ncfile.addDimension(g, new Dimension(ecName, n));
      Variable v = new Variable(ncfile, g, null, ecName, DataType.INT, ecName);
      ncfile.addVariable(g, v);
      ccount++;
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Ensemble.toString()));

      int[] data = new int[n];
      int count = 0;
      for (EnsCoord.Coord ecc : ec.getCoords())
        data[count++] = ecc.getEnsMember();
      v.setCachedData(Array.factory(DataType.INT, new int[]{n}, data));
    }

    for (GribCollection.VariableIndex vindex : gHcs.varIndex) {
      TimeCoord tc = gHcs.timeCoords.get(vindex.timeIdx);
      VertCoord vc = (vindex.vertIdx < 0) ? null : gHcs.vertCoords.get(vindex.vertIdx);
      EnsCoord ec = (vindex.ensIdx < 0) ? null : gHcs.ensCoords.get(vindex.ensIdx);

      StringBuilder dims = new StringBuilder();

      // canonical order: time, ens, z, y, x
      String tcName = tc.getName();
      dims.append(tcName);

      if (ec != null)
        dims.append(" ").append("ens").append(vindex.ensIdx);

      if (vc != null)
        dims.append(" ").append(vc.getName());  // LOOK was vc.getName().toLowerCase() (!!??)

      dims.append(" ").append(horizDims);

      String vname = cust.makeVariableName(gribCollection, vindex);
      Variable v = new Variable(ncfile, g, null, vname, DataType.FLOAT, dims.toString());
      ncfile.addVariable(g, v);

      String desc = makeVariableLongName(cust, gribCollection, vindex);
      v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
      v.addAttribute(new Attribute(CDM.UNITS, makeVariableUnits(cust, gribCollection, vindex)));
      v.addAttribute(new Attribute(CDM.MISSING_VALUE, MISSING_VALUE));
      if (!hcs.isLatLon()) v.addAttribute(new Attribute(CF.GRID_MAPPING, grid_mapping));

      // Grib attributes
      v.addAttribute(new Attribute(VARIABLE_ID_ATTNAME, cust.makeVariableNameFromRecord(gribCollection, vindex)));
      v.addAttribute(new Attribute("Grib1_Center", gribCollection.center));
      v.addAttribute(new Attribute("Grib1_Subcenter", gribCollection.subcenter));
      v.addAttribute(new Attribute("Grib1_TableVersion", vindex.tableVersion));
      v.addAttribute(new Attribute("Grib1_Parameter", vindex.parameter));
      Grib1Parameter param = cust.getParameter(gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.tableVersion, vindex.parameter);
      if (param != null && param.getName() != null)
        v.addAttribute(new Attribute("Grib1_Parameter_Name", param.getName()));

      v.addAttribute(new Attribute("Grib1_Level_Type", vindex.levelType));
      String ldesc = cust.getLevelDescription(vindex.levelType);
      if (ldesc != null)
        v.addAttribute(new Attribute("Grib1_Level_Desc", ldesc));
      /* else {
        System.out.printf("%s%n", "HEY");
        cust.getLevelDescription(vindex.levelType);
      } */

      if ( vindex.intvName != null && vindex.intvName.length() != 0)
        v.addAttribute(new Attribute(CDM.TIME_INTERVAL, vindex.intvName));
      if (vindex.intvType >= 0) {
        GribStatType statType = cust.getStatType(vindex.intvType);
        if (statType != null) {
          v.addAttribute(new Attribute("Grib1_Statistical_Interval_Type", vindex.intvType));
          v.addAttribute(new Attribute("Grib1_Statistical_Interval_Name", statType.toString()));
          CF.CellMethods cm = GribStatType.getCFCellMethod(statType);
          if (cm != null)
            v.addAttribute(new Attribute("cell_methods", tcName + ": " + cm.toString()));
        }
      }
      if (vindex.ensDerivedType >= 0)
        v.addAttribute(new Attribute("Grib1_Ensemble_Derived_Type", vindex.ensDerivedType));
      else if (vindex.probabilityName != null && vindex.probabilityName.length() > 0)
        v.addAttribute(new Attribute("Grib1_Probability_Type", vindex.probabilityName));

      v.setSPobject(vindex);
    }
  }

  private void addTimeCoordinate(NetcdfFile ncfile, Group g, TimeCoord tc) {
    int n = tc.getSize();
    String tcName = tc.getName();
    ncfile.addDimension(g, new Dimension(tcName, n));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.INT, tcName));
    v.addAttribute(new Attribute(CDM.UNITS, tc.getUnits()));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, "time"));
    v.addAttribute(new Attribute(CDM.LONG_NAME, cust.getTimeTypeName(tc.getCode())));

    // coordinate values
    int[] data = new int[n];
    int count = 0;
    if (tc.isInterval()) {
      for (TimeCoord.Tinv tinv : tc.getIntervals()) data[count++] = tinv.getBounds2();
    } else {
      for (int val : tc.getCoords()) data[count++] = val;
    }
    v.setCachedData(Array.factory(DataType.INT, new int[]{n}, data));

    if (tc.isInterval()) {
      GribStatType statType = cust.getStatType(tc.getCode());
      if (statType == null) {
        v.addAttribute(new Attribute("Grib1_statistical_type", tc.getCode()));
      } else {
        v.addAttribute(new Attribute("Grib1_statistical_type", GribStatType.getStatTypeDescription(statType)));
      }

      // coordinate bounds
      Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName + "_bounds", DataType.INT, tcName + " 2"));
      v.addAttribute(new Attribute(CF.BOUNDS, tcName + "_bounds"));
      bounds.addAttribute(new Attribute(CDM.UNITS, tc.getUnits()));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));

      data = new int[2 * n];
      count = 0;
      for (TimeCoord.Tinv tinv : tc.getIntervals()) {
        data[count++] = tinv.getBounds1();
        data[count++] = tinv.getBounds2();
      }
      bounds.setCachedData(Array.factory(DataType.INT, new int[]{n, 2}, data));
    }
  }


  private void addVerticalCoordinate(NetcdfFile ncfile, Group g, VertCoord vc) {
    int n = vc.getSize();
    String vcName = vc.getName();
    ncfile.addDimension(g, new Dimension(vcName, n));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, vcName, DataType.FLOAT, vcName));
    if (vc.getUnits() != null) {
      v.addAttribute(new Attribute(CDM.UNITS, vc.getUnits()));
      String desc = cust.getLevelDescription(vc.getCode());
      if (desc != null) v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
      v.addAttribute(new Attribute(CF.POSITIVE, vc.isPositiveUp() ? CF.POSITIVE_UP : CF.POSITIVE_DOWN));
    }

    v.addAttribute(new Attribute("Grib1_level_code", vc.getCode()));
    String datum = cust.getLevelDatum(vc.getCode());
    if (datum != null)
      v.addAttribute(new Attribute("datum", datum));

    if (vc.isLayer()) {
      float[] data = new float[n];
      int count = 0;
      for (VertCoord.Level val : vc.getCoords())
        data[count++] = (float) (val.getValue1() + val.getValue2()) / 2;
      v.setCachedData(Array.factory(DataType.FLOAT, new int[]{n}, data));

      Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, vcName + "_bounds", DataType.FLOAT, vcName + " 2"));
      v.addAttribute(new Attribute(CF.BOUNDS, vcName + "_bounds"));
      bounds.addAttribute(new Attribute(CDM.UNITS, vc.getUnits()));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + vcName));

      data = new float[2 * n];
      count = 0;
      for (VertCoord.Level level : vc.getCoords()) {
        data[count++] = (float) level.getValue1();
        data[count++] = (float) level.getValue2();
      }
      bounds.setCachedData(Array.factory(DataType.FLOAT, new int[]{n, 2}, data));

    } else {
      float[] data = new float[n];
      int count = 0;
      for (VertCoord.Level val : vc.getCoords())
        data[count++] = (float) val.getValue1();
      v.setCachedData(Array.factory(DataType.FLOAT, new int[]{n}, data));
    }

    // hybrid nightmare
    if (vc.getCode() == 109) {
      // LOOK WTF?
    }
  }

  @Override
  public void close() throws java.io.IOException {
    if (!owned && gribCollection != null) // klugerino
      gribCollection.close();
    gribCollection = null;
    super.close();
  }

  @Override
  public String getDetailInfo() {
    Formatter f = new Formatter();
    f.format("%s",super.getDetailInfo());
    if (gribCollection != null)
      gribCollection.showIndex(f);
    return f.toString();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    long start = System.currentTimeMillis();

    Array result;
    if (isTimePartitioned)
      result = readDataFromPartition(v2, section);
    else
      result = readDataFromCollection(v2, section);

    long took = System.currentTimeMillis() - start;
    if (debugTime) System.out.println("  read data took=" + took + " msec ");
    return result;
  }

  private Array readDataFromPartition(Variable v2, Section section) throws IOException, InvalidRangeException {
    TimePartition.VariableIndexPartitioned vindexP = (TimePartition.VariableIndexPartitioned) v2.getSPobject();

    // canonical order: time, ens, z, y, x
    int rangeIdx = 0;
    Range timeRange = (section.getRank() > 2) ? section.getRange(rangeIdx++) : new Range(0, 0);
    Range ensRange = (vindexP.ensIdx >= 0) ? section.getRange(rangeIdx++) : new Range(0, 0);
    Range levRange = (vindexP.vertIdx >= 0) ? section.getRange(rangeIdx++) : new Range(0, 0);
    Range yRange = section.getRange(rangeIdx++);
    Range xRange = section.getRange(rangeIdx);

    DataReceiver dataReceiver = new DataReceiver(section, yRange, xRange);
    DataReaderPartitioned dataReader = new DataReaderPartitioned();

    TimeCoordUnion timeCoordP = (TimeCoordUnion) vindexP.getTimeCoord();

    // collect all the records from this partition that need to be read
    for (int timeIdx = timeRange.first(); timeIdx <= timeRange.last(); timeIdx += timeRange.stride()) {

      TimeCoordUnion.Val val = timeCoordP.getVal(timeIdx);
      int partno = val.getPartition();
      GribCollection.VariableIndex vindex = vindexP.getVindex(partno);

      for (int ensIdx = ensRange.first(); ensIdx <= ensRange.last(); ensIdx += ensRange.stride()) {
        for (int levelIdx = levRange.first(); levelIdx <= levRange.last(); levelIdx += levRange.stride()) {

          // where does this record go in the result ??
          int resultIndex = GribCollection.calcIndex(timeRange.index(timeIdx), ensRange.index(ensIdx), levRange.index(levelIdx),
                  ensRange.length(), levRange.length());

          // where does this record come from ??
          int recordIndex = -1;

          int flag = vindexP.flag[partno]; // see if theres a mismatch with vert or ens coordinates
          if (flag == 0) { // no problem
            recordIndex = GribCollection.calcIndex(val.getIndex(), ensIdx, levelIdx, vindex.nens, vindex.nverts);
          } else {  // problem - must match coordinates
            recordIndex = GribCollection.calcIndex(val.getIndex(), ensIdx, levelIdx, flag, vindex.getEnsCoord(), vindex.getVertCoord(),
                    vindexP.getEnsCoord(), vindexP.getVertCoord());
          }

          if (recordIndex >= 0)  {
            GribCollection.Record record = vindex.records[recordIndex];
            dataReader.addRecord(vindex, partno, record.fileno, record.pos, resultIndex);  // add this record to be read
          }
        }
      }
    }

    // sort by file and position, then read
    dataReader.read(dataReceiver);

    // close partitions as needed
    vindexP.cleanup();

    return dataReceiver.getArray();
  }

  private class DataReaderPartitioned {
    List<DataRecord> records = new ArrayList<DataRecord>();

    private DataReaderPartitioned() {
    }

    void addRecord(GribCollection.VariableIndex vindex, int partno, int fileno, long pos, int resultIndex) {
      records.add(new DataRecord(partno, vindex, resultIndex, fileno, pos));
    }

    void read(DataReceiver dataReceiver) throws IOException {
      Collections.sort(records);

      int currPartno = -1;
      int currFile = -1;
      RandomAccessFile rafData = null;
      for (DataRecord dr : records) {
        if (dr.partno != currPartno || dr.fileno != currFile) {
          if (rafData != null) rafData.close();
          rafData = timePartition.getRaf(dr.partno, dr.fileno);
          currFile = dr.fileno;
          currPartno = dr.partno;
        }
        if (dr.pos == GribCollection.MISSING_RECORD) continue; // skip missing data

        if (debugRead) {
          rafData.seek(dr.pos);
          show(new Grib1Record(rafData), dr.pos);
        }

        float[] data = Grib1Record.readData(rafData, dr.pos);
        dataReceiver.addData(data, dr.resultIndex, dr.vindex.group.hcs.nx);
      }
      if (rafData != null) rafData.close();
    }

    private class DataRecord implements Comparable<DataRecord> {
      int partno; // partition index
      GribCollection.VariableIndex vindex; // the vindex of the partition
      int resultIndex; // where does this record go in the result array?
      int fileno;
      long pos;

      DataRecord(int partno, GribCollection.VariableIndex vindex, int resultIndex, int fileno, long pos) {
        this.partno = partno;
        this.vindex = vindex;
        this.resultIndex = resultIndex;
        this.fileno = fileno;
        this.pos = pos;
      }

      @Override
      public int compareTo(DataRecord o) {
        int r = Misc.compare(partno, o.partno);
        if (r != 0) return r;
        r = Misc.compare(fileno, o.fileno);
        if (r != 0) return r;
        return Misc.compare(pos, o.pos);
      }
    }
  }

///////////////////////////////////////////////////////

  private Array readDataFromCollection(Variable v, Section section) throws IOException, InvalidRangeException {
    GribCollection.VariableIndex vindex = (GribCollection.VariableIndex) v.getSPobject();

    // first time, read records and keep in memory
    if (vindex.records == null)
      vindex.readRecords();

    // canonical order: time, ens, z, y, x
    int rangeIdx = 0;
    Range timeRange = (section.getRank() > 2) ? section.getRange(rangeIdx++) : new Range(0, 0);
    Range ensRange = (vindex.ensIdx >= 0) ? section.getRange(rangeIdx++) : new Range(0, 0);
    Range levRange = (vindex.vertIdx >= 0) ? section.getRange(rangeIdx++) : new Range(0, 0);
    Range yRange = section.getRange(rangeIdx++);
    Range xRange = section.getRange(rangeIdx);

    DataReceiver dataReceiver = new DataReceiver(section, yRange, xRange);
    DataReader dataReader = new DataReader(vindex);

    // collect all the records that need to be read
    for (int timeIdx = timeRange.first(); timeIdx <= timeRange.last(); timeIdx += timeRange.stride()) {
      for (int ensIdx = ensRange.first(); ensIdx <= ensRange.last(); ensIdx += ensRange.stride()) {
        for (int levelIdx = levRange.first(); levelIdx <= levRange.last(); levelIdx += levRange.stride()) {
          // where this particular record fits into the result array, modulo horiz
          int resultIndex = GribCollection.calcIndex(timeRange.index(timeIdx), ensRange.index(ensIdx), levRange.index(levelIdx),
                  ensRange.length(), levRange.length());
          dataReader.addRecord(ensIdx, timeIdx, levelIdx, resultIndex);
        }
      }
    }

    // sort by file and position, then read
    dataReader.read(dataReceiver);
    return dataReceiver.getArray();
  }

  private class DataReader {
    GribCollection.VariableIndex vindex;
    List<DataRecord> records = new ArrayList<DataRecord>();

    private DataReader(GribCollection.VariableIndex vindex) {
      this.vindex = vindex;
    }

    void addRecord(int ensIdx, int timeIdx, int levIdx, int resultIndex) {
      int recordIndex = GribCollection.calcIndex(timeIdx, ensIdx, levIdx, vindex.nens, vindex.nverts);
      GribCollection.Record record = vindex.records[recordIndex];
      records.add(new DataRecord(timeIdx, ensIdx, levIdx, resultIndex, record.fileno, record.pos));
    }

    void read(DataReceiver dataReceiver) throws IOException {
      Collections.sort(records);

      int currFile = -1;
      RandomAccessFile rafData = null;
      for (DataRecord dr : records) {
        if (dr.fileno != currFile) {
          if (rafData != null) rafData.close();
          rafData = gribCollection.getDataRaf(dr.fileno);
          currFile = dr.fileno;
        }

        if (dr.pos == GribCollection.MISSING_RECORD) continue;

        if (debugRead) {
          rafData.seek(dr.pos);
          show(new Grib1Record(rafData), dr.pos);
        }

        float[] data = Grib1Record.readData(rafData, dr.pos);
        dataReceiver.addData(data, dr.resultIndex, vindex.group.hcs.nx);
      }
      if (rafData != null) rafData.close();
    }

    private class DataRecord implements Comparable<DataRecord> {
      int ensIdx, timeIdx, levIdx;
      int resultIndex; // index in the ens / time / vert array
      int fileno;
      long pos;

      DataRecord(int timeIdx, int ensIdx, int levIdx, int resultIndex, int fileno, long pos) {
        this.ensIdx = ensIdx;
        this.timeIdx = timeIdx;
        this.levIdx = levIdx;
        this.resultIndex = resultIndex;
        this.fileno = fileno;
        this.pos = pos;
      }

      @Override
      public int compareTo(DataRecord o) {
        int r = Misc.compare(fileno, o.fileno);
        if (r != 0) return r;
        return Misc.compare(pos, o.pos);
      }
    }
  }

  private class DataReceiver {
    Array dataArray;
    Range yRange, xRange;
    int horizSize;

    DataReceiver(Section section, Range yRange, Range xRange) {
      dataArray = Array.factory(DataType.FLOAT, section.getShape());
      this.yRange = yRange;
      this.xRange = xRange;
      this.horizSize = yRange.length() * xRange.length();

      // prefill with NaNs, to deal with missing data
      IndexIterator iter = dataArray.getIndexIterator();
      while (iter.hasNext())
        iter.setFloatNext(MISSING_VALUE);
    }

    void addData(float[] data, int resultIndex, int nx) throws IOException {
      int start = resultIndex * horizSize;
      int count = 0;
      for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
        for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
          int dataIdx = y * nx + x;
          try {
            dataArray.setFloat(start + count, data[dataIdx]);
          } catch (ArrayIndexOutOfBoundsException t) {
            throw t;
          }
          count++;
        }
      }
    }

    Array getArray() {
      return dataArray;
    }
  }

  private void show(Grib1Record gr, long dataPos) {
    if (gr == null) return;
    Formatter f = new Formatter();
    f.format("File=%s%n", raf.getLocation());
    Grib1SectionProductDefinition pds = gr.getPDSsection();
    Grib1Parameter param = cust.getParameter(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber());
    f.format("  Parameter=%s%n", param);
    f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
    Grib1ParamTime ptime = pds.getParamTime(cust);
    f.format("  ForecastTime=%d%n", ptime.getForecastTime());
    if (ptime.isInterval()) {
      int tinv[] = ptime.getInterval();
      f.format("  TimeInterval=(%d,%d)%n", tinv[0], tinv[1]);
    }
    f.format("%n");
    gr.getPDSsection().showPds(cust, f);
    System.out.printf("%nGrib1Record.readData at drsPos %d = %s%n", dataPos, f.toString());
  }
}

