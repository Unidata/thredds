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

package ucar.nc2.grib.collection;

import thredds.catalog.DataFormatType;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.time.CalendarPeriod;
import ucar.sparr.Coordinate;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.StringUtil2;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.Formatter;

/**
 * Grib-2 Collection IOSP, ver2.
 * Handles both collections and single GRIB files.
 *
 * @author caron
 * @since 4/6/11
 */
public class Grib2Iosp extends GribIosp {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2Iosp.class);
  static private final boolean debugTime = false, debugRead = true, debugName = false;
  static private boolean useGenType = false; // LOOK dummy for now
  
  static public String makeVariableName(Grib2Customizer tables, GribCollection gribCollection, GribCollection.VariableIndex vindex) {
    return makeVariableNameFromTable(tables, gribCollection,  vindex);
  }

  static public String makeVariableNameFromTable(Grib2Customizer tables, GribCollection gribCollection, GribCollection.VariableIndex vindex) {
    Formatter f = new Formatter();

    GribTables.Parameter param = tables.getParameter(vindex.discipline, vindex.category, vindex.parameter);

    if (param == null) {
      f.format("VAR%d-%d-%d_FROM_%d-%d-%d", vindex.discipline, vindex.category, vindex.parameter, gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.tableVersion);
    } else {
      f.format("%s", GribUtils.makeNameFromDescription(param.getName()));
    }

    if (!useGenType && (vindex.genProcessType == 6 || vindex.genProcessType == 7)) { // LOOK
      f.format("_error");  // its an "error" type variable - add to name
    }

    if (vindex.levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_%s", tables.getLevelNameShort(vindex.levelType)); // vindex.levelType); // code table 4.5
      if (vindex.isLayer) f.format("_layer");
    }

    String intvName = vindex.getTimeIntvName();
    if (intvName != null && !intvName.isEmpty()) {
      f.format("_%s", intvName);
    }

    if (vindex.intvType >= 0) {
      String statName = tables.getIntervalNameShort(vindex.intvType);
      if (statName != null) f.format("_%s", statName);
    }

    if (vindex.ensDerivedType >= 0) {
      f.format("_%s", tables.getProbabilityNameShort(vindex.ensDerivedType));
    }

    else if (vindex.probabilityName != null && vindex.probabilityName.length() > 0) {
      String s = StringUtil2.substitute(vindex.probabilityName, ".", "p");
      f.format("_probability_%s", s);
    }

    if (vindex.genProcessType >= 0 && useGenType) {
      String genType = tables.getGeneratingProcessTypeName(vindex.genProcessType);
      String s = StringUtil2.substitute(genType, " ", "_");
      f.format("_%s", s);
    }

    return f.toString();
  }

  /* http://www.ncl.ucar.edu/Document/Manuals/Ref_Manual/NclFormatSupport.shtml#GRIB
    GRIB2 data variable name encoding

    (Note: examples show intermediate steps in the formation of the name)

    if production status is TIGGE test or operational and matches entry in TIGGE table:
       <parameter_short_name> (ex: t)
    else if entry matching product discipline, parameter category, and parameter number is found:
       <parameter_short_name> (ex: TMP)
    else:
       VAR_<product_discipline_number>_<parameter_category_number>_<parameter_number> (ex: VAR_3_0_9)

    _P<product_definition_template_number> (ex: TMP_P0)

    if single level type:
       _L<level_type_number> (ex: TMP_P0_L103)
    else if two levels of the same type:
       _2L<level_type_number> (ex: TMP_P0_2L106)
    else if two levels of different types:
       _2L<_first_level_type_number>_<second_level_type_number> (ex: LCLD_P0_2L212_213)

    if grid type is supported (fully or partially):
       _G<grid_abbreviation><grid_number> (ex: UGRD_P0_L108_GLC0)
    else:
       _G<grid_number> (ex: UGRD_P0_2L104_G0)

    if not statistically processed variable and not duplicate name the name is complete at this point.

    if statistically-processed variable and constant statistical processing duration:
       if statistical processing type is defined:
          _<statistical_processing_type_abbreviation><statistical_processing_duration><duration_units> (ex: APCP_P8_L1_GLL0_acc3h)
       else
          _<statistical_processing_duration><duration_units> (ex: TMAX_P8_L103_GCA0_6h)
    else if statistically-processed variable and variable-duration processing always begins at initial time:
       _<statistical_processing_type_abbreviation> (ex: ssr_P11_GCA0_acc)

    if variable name is duplicate of existing variable name (this should not normally occur):
       _n (where n begins with 1 for first duplicate) (ex: TMAX_P8_L103_GCA0_6h_1)

     VAR_%d-%d-%d[_error][_L%d][_layer][_I%s_S%d][_D%d][_Prob_%s]
      %d-%d-%d = discipline-category-paramNo
      L = level type
      S = stat type
      D = derived type
   */
  static public String makeVariableNameFromRecord(GribCollection.VariableIndex vindex) {
    Formatter f = new Formatter();

    f.format("VAR_%d-%d-%d", vindex.discipline, vindex.category, vindex.parameter);

    if (!useGenType && (vindex.genProcessType == 6 || vindex.genProcessType == 7)) { // LOOK
      f.format("_error");  // its an "error" type variable - add to name
    }

    if (vindex.levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_L%d", vindex.levelType); // code table 4.5
      if (vindex.isLayer) f.format("_layer");
    }

    String intvName = vindex.getTimeIntvName();
    if (intvName != null  && !intvName.isEmpty()) {
      if (intvName.equals("Mixed_intervals"))
        f.format("_Imixed");
      else
        f.format("_I%s", intvName);
    }

    if (vindex.intvType >= 0) {
      f.format("_S%s", vindex.intvType);
    }

    if (vindex.ensDerivedType >= 0) {
      f.format("_D%d", vindex.ensDerivedType);
    }

    else if (vindex.probabilityName != null && vindex.probabilityName.length() > 0) {
      String s = StringUtil2.substitute(vindex.probabilityName, ".", "p");
      f.format("_Prob_%s", s);
    }

    return f.toString();
  }

  static public String makeVariableLongName(Grib2Customizer cust, GribCollection.VariableIndex vindex) {
    Formatter f = new Formatter();

    boolean isProb = (vindex.probabilityName != null && vindex.probabilityName.length() > 0);
    if (isProb)
      f.format("Probability ");

    GribTables.Parameter gp = cust.getParameter(vindex.discipline, vindex.category, vindex.parameter);
    if (gp == null)
      f.format("Unknown Parameter %d-%d-%d", vindex.discipline, vindex.category, vindex.parameter);
    else
      f.format("%s", gp.getName());

    String vintvName = vindex.getTimeIntvName();
    if (vindex.intvType >= 0 && vintvName != null && !vintvName.isEmpty()) {
      String intvName = cust.getIntervalNameShort(vindex.intvType);
      if (intvName == null || intvName.equalsIgnoreCase("Missing"))
        intvName = cust.getIntervalNameShort(vindex.intvType);
      if (intvName == null) f.format(" (%s)", vintvName);
      else f.format(" (%s %s)", vintvName, intvName);

    } else if (vindex.intvType >= 0) {
      String intvName = cust.getIntervalNameShort(vindex.intvType);
      f.format(" (%s)", intvName);
    }

    if (vindex.ensDerivedType >= 0)
      f.format(" (%s)", cust.getTableValue("4.7", vindex.ensDerivedType));

    else if (isProb)
      f.format(" %s %s", vindex.probabilityName, getVindexUnits(cust, vindex)); // add data units here

    if (!useGenType && (vindex.genProcessType == 6 || vindex.genProcessType == 7)) {
      f.format(" error");
    } else if (useGenType && vindex.genProcessType >= 0) {
      f.format(" %s", cust.getGeneratingProcessTypeName(vindex.genProcessType));
    }

    if (vindex.levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format(" @ %s", cust.getTableValue("4.5", vindex.levelType));
      if (vindex.isLayer) f.format(" layer");
    }

    return f.toString();
  }

  static public String makeVariableUnits(Grib2Customizer tables, GribCollection.VariableIndex vindex) {
    if (vindex.probabilityName != null && vindex.probabilityName.length() > 0) return "%";
    return getVindexUnits(tables, vindex);
  }

  static private String getVindexUnits(Grib2Customizer tables, GribCollection.VariableIndex vindex) {
    GribTables.Parameter gp = tables.getParameter(vindex.discipline, vindex.category, vindex.parameter);
    String val = (gp == null) ? "" : gp.getUnit();
    return (val == null) ? "" : val;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // private PartitionCollection gribPartition;
  private GribCollection gribCollection;
  private Grib2Customizer cust;
  private GribCollection.GroupHcs gHcs;
  private GribCollection.Type gtype; // only used if gHcs was set
  private boolean isPartitioned;
  private boolean owned; // if Iosp is owned by GribCollection; affects close()

  // accept grib2 or ncx2 files
  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    GribCdmIndex2.GribCollectionType type = GribCdmIndex2.getType(raf);
    if (type == GribCdmIndex2.GribCollectionType.GRIB2) return true;
    if (type == GribCdmIndex2.GribCollectionType.Partition2) return true;

    // check for GRIB2 data file
    return Grib2RecordScanner.isValidFile(raf);
  }

  @Override
  public String getFileTypeId() {
    return DataFormatType.GRIB2.toString();
  }

  @Override
  public String getFileTypeDescription() {
    return "GRIB2 Collection";
  }

  // public no-arg constructor for reflection
  public Grib2Iosp() {
  }

  public Grib2Iosp(GribCollection.GroupHcs gHcs, GribCollection.Type gtype) {
    this.gHcs = gHcs;
    this.owned = true;
    this.gtype = gtype;
  }

  // LOOK more likely we will set an individual dataset
  public Grib2Iosp(GribCollection gc) {
    this.gribCollection = gc;
    this.owned = true;
  }

  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);

    if (gHcs != null) { // just use the one group that was set in the constructor
      this.gribCollection = gHcs.getGribCollection();
      if (this.gribCollection instanceof Grib2Partition)
        isPartitioned = true;
      cust = Grib2Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), gribCollection.getLocal());

      addGroup(ncfile, ncfile.getRootGroup(), gHcs, gtype, false);

    } else if (gribCollection == null) { // may have been set in the constructor

      this.gribCollection = GribCdmIndex2.makeGribCollectionFromRaf(false, raf, gribConfig, CollectionUpdateType.test, logger);
      if (gribCollection == null)
        throw new IllegalStateException("Not a GRIB2 data file or ncx2 file");

      if (this.gribCollection instanceof Grib2Partition)
        isPartitioned = true;
      cust = Grib2Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), gribCollection.getLocal());

      for (GribCollection.Dataset ds : gribCollection.getDatasets()) {
        Group datasetGroup = new Group(ncfile, null, ds.getType().toString());
        ncfile.addGroup(null, datasetGroup);

        List<GribCollection.GroupHcs> groups = new ArrayList<>(ds.getGroups());
        Collections.sort(groups);
        boolean useGroups = groups.size() > 1;
        for (GribCollection.GroupHcs g : groups)
          addGroup(ncfile, datasetGroup, g, ds.getType(), useGroups);
      }
    }

    String val = CommonCodeTable.getCenterName(gribCollection.getCenter(), 2);
    ncfile.addAttribute(null, new Attribute(GribUtils.CENTER, val == null ? Integer.toString(gribCollection.getCenter()) : val));
    val = cust.getSubCenterName(gribCollection.getCenter(), gribCollection.getSubcenter());
    ncfile.addAttribute(null, new Attribute(GribUtils.SUBCENTER, val == null ? Integer.toString(gribCollection.getSubcenter()) : val));
    ncfile.addAttribute(null, new Attribute(GribUtils.TABLE_VERSION, gribCollection.getMaster() + "," + gribCollection.getLocal())); // LOOK

    val = cust.getTableValue("4.3", gribCollection.getGenProcessType());
    if (val != null)
      ncfile.addAttribute(null, new Attribute("Type_of_generating_process", val));
    val = cust.getGeneratingProcessName(gribCollection.getGenProcessId());
    if (val != null)
      ncfile.addAttribute(null, new Attribute("Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre", val));
    val = cust.getGeneratingProcessName(gribCollection.getBackProcessId());
    if (val != null)
      ncfile.addAttribute(null, new Attribute("Background_generating_process_identifier_defined_by_originating_centre", val));

    ncfile.addAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.6"));
    ncfile.addAttribute(null, new Attribute(CDM.HISTORY, "Read using CDM IOSP Grib2Collection v2"));
    ncfile.addAttribute(null, new Attribute(CF.FEATURE_TYPE, FeatureType.GRID.name()));
    ncfile.addAttribute(null, new Attribute(CDM.FILE_FORMAT, getFileTypeId()));

    if (gribCollection.getParams() != null)
      for (Parameter p : gribCollection.getParams())
        ncfile.addAttribute(null, new Attribute(p));
  }

  private void addGroup(NetcdfFile ncfile, Group parent, GribCollection.GroupHcs gHcs, GribCollection.Type gctype, boolean useGroups) {

    Group g;
    if (useGroups) {
      g = new Group(ncfile, parent, gHcs.getId());
      g.addAttribute(new Attribute(CDM.LONG_NAME, gHcs.getDescription()));
      try {
        ncfile.addGroup(parent, g);
      } catch (Exception e) {
        logger.warn("Duplicate Group - skipping");
        return;
      }
    } else {
      g = parent;
    }

    makeGroup(ncfile, g, gHcs, gctype);
  }

  private void makeGroup(NetcdfFile ncfile, Group g, GribCollection.GroupHcs gHcs, GribCollection.Type gctype) {
    GdsHorizCoordSys hcs = gHcs.getGdsHorizCoordSys();
    String grid_mapping = hcs.getName()+"_Projection";

    String horizDims;

    // CurvilinearOrthogonal - lat and lon fields must be present in the file
    boolean isLatLon2D = Grib2Utils.isLatLon2D(hcs.template, gribCollection.getCenter());
    if (isLatLon2D) {
      horizDims = "lat lon";  // LOOK: orthogonal curvilinear

      // LOOK - assume same number of points for all grids
      ncfile.addDimension(g, new Dimension("lon", hcs.nx));
      ncfile.addDimension(g, new Dimension("lat", hcs.ny));

    } else if (Grib2Utils.isLatLon(hcs.template, gribCollection.getCenter())) {
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

    boolean is2Dtime = (gctype == GribCollection.Type.TwoD);
    CoordinateRuntime runtime = null;
    for (Coordinate coord : gHcs.coords) {
      Coordinate.Type ctype = coord.getType();
      switch (ctype) {
        case runtime:
          runtime = (CoordinateRuntime) coord;
          makeRuntimeCoordinate(ncfile, g, (CoordinateRuntime) coord);
          break;
        case timeIntv:
          if (is2Dtime) makeTimeCoordinate2D(ncfile, g, coord, runtime);
          else makeTimeCoordinate1D(ncfile, g, (CoordinateTimeIntv) coord, runtime);
          break;
        case time:
          if (is2Dtime) makeTimeCoordinate2D(ncfile, g, coord, runtime);
          else makeTimeCoordinate1D(ncfile, g, (CoordinateTime)  coord, runtime);
          break;
        case vert:
          makeVerticalCoordinate(ncfile, g, (CoordinateVert) coord);
          break;
        case time2D:
          makeTime2D(ncfile, g, (CoordinateTime2D) coord, is2Dtime);
          break;
      }
    }

    /* int ccount = 0;
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
    } */

    for (GribCollection.VariableIndex vindex : gHcs.variList) {

      Formatter dims = new Formatter();
      Formatter coords = new Formatter();

      for (Coordinate coord : vindex.getCoordinates()) {
        if (!is2Dtime && coord.getType() == Coordinate.Type.runtime) continue; // skip reference time
        dims.format("%s ", coord.getName());
        coords.format("%s ", coord.getName());
      }
      dims.format("%s", horizDims);

      String vname = makeVariableName(cust, gribCollection, vindex);
      Variable v = new Variable(ncfile, g, null, vname, DataType.FLOAT, dims.toString());
      ncfile.addVariable(g, v);
      if (debugName) System.out.printf("added %s%n",vname);

      String desc = makeVariableLongName(cust, vindex);
      v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
      v.addAttribute(new Attribute(CDM.UNITS, makeVariableUnits(cust, vindex)));
      v.addAttribute(new Attribute(CDM.MISSING_VALUE, Float.NaN));

      GribTables.Parameter gp = cust.getParameter(vindex.discipline, vindex.category, vindex.parameter);
      if (gp != null) {
        if (gp.getDescription() != null)
          v.addAttribute(new Attribute(CDM.DESCRIPTION, gp.getDescription()));
        if (gp.getAbbrev() != null)
          v.addAttribute(new Attribute(CDM.ABBREV, gp.getAbbrev()));
      }

      if (isLatLon2D) {
        String s = searchCoord(gribCollection, Grib2Utils.getLatLon2DcoordType(desc), gHcs.variList);
        if (s == null) { // its a lat/lon coordinate
          v.setDimensions(horizDims); // LOOK make this 2D and munge the units
          String units = desc.contains("Latitude of") ? CDM.LAT_UNITS : CDM.LON_UNITS;
          v.addAttribute(new Attribute(CDM.UNITS, units));

        } else {
          coords.format("%s ", s);
        }
      } else {
        if (!hcs.isLatLon()) v.addAttribute(new Attribute(CF.GRID_MAPPING, grid_mapping));
      }
      v.addAttribute(new Attribute(CF.COORDINATES, coords.toString()));

      String intvName = vindex.getTimeIntvName();
      if ( intvName != null && intvName.length() != 0)
        v.addAttribute(new Attribute(CDM.TIME_INTERVAL, intvName));

      v.addAttribute(new Attribute(VARIABLE_ID_ATTNAME, makeVariableNameFromRecord(vindex)));
      int[] param = new int[] {vindex.discipline,vindex.category,vindex.parameter};
      v.addAttribute(new Attribute("Grib2_Parameter", Array.factory(param)));
      String disc =  cust.getTableValue("0.0", vindex.discipline);
      if (disc != null) v.addAttribute(new Attribute("Grib2_Parameter_Discipline", disc));
      String cat =  cust.getCategory(vindex.discipline, vindex.category);
      if (cat != null)
        v.addAttribute(new Attribute("Grib2_Parameter_Category", cat));
      Grib2Customizer.Parameter entry = cust.getParameter(vindex.discipline,vindex.category,vindex.parameter);
      if (entry != null) v.addAttribute(new Attribute("Grib2_Parameter_Name", entry.getName()));

      if (vindex.levelType != GribNumbers.MISSING) {
        String levelTypeName = cust.getLevelName(vindex.levelType);
        if (levelTypeName != null)
          v.addAttribute(new Attribute("Grib2_Level_Type", levelTypeName));
        else
          v.addAttribute(new Attribute("Grib2_Level_Type", vindex.levelType));
      }

      if (vindex.intvType >= 0) {
        GribStatType statType = cust.getStatType(vindex.intvType);   // LOOK find the time coordinate
        if (statType != null) {
          v.addAttribute(new Attribute("Grib2_Statistical_Interval_Type", statType.toString()));
          CF.CellMethods cm = GribStatType.getCFCellMethod( statType);
          Coordinate timeCoord = vindex.getCoordinate(Coordinate.Type.timeIntv);
          if (cm != null && timeCoord != null)
            v.addAttribute(new Attribute("cell_methods", timeCoord.getName() + ": " + cm.toString()));
        } else {
          v.addAttribute(new Attribute("Grib2_Statistical_Interval_Type", vindex.intvType));
        }
      }

      if (vindex.ensDerivedType >= 0)
        v.addAttribute(new Attribute("Grib2_Ensemble_Derived_Type", vindex.ensDerivedType));
      else if (vindex.probabilityName != null && vindex.probabilityName.length() > 0) {
        v.addAttribute(new Attribute("Grib2_Probability_Type", vindex.probType));
        v.addAttribute(new Attribute("Grib2_Probability_Name", vindex.probabilityName));
      }

      if (vindex.genProcessType >= 0) {
        String genProcessTypeName = cust.getGeneratingProcessTypeName(vindex.genProcessType);
        if (genProcessTypeName != null)
          v.addAttribute(new Attribute("Grib2_Generating_Process_Type", genProcessTypeName));
        else
          v.addAttribute(new Attribute("Grib2_Generating_Process_Type", vindex.genProcessType));
      }

      v.setSPobject(vindex);
    }
  }

  private void makeRuntimeCoordinate(NetcdfFile ncfile, Group g, CoordinateRuntime rtc) {
    int n = rtc.getSize();
    String tcName = rtc.getName();
    ncfile.addDimension(g, new Dimension(tcName, n));

    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, tcName));
    v.addAttribute(new Attribute(CDM.UNITS, rtc.getUnit()));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, "forecast_reference_time"));

    String vsName = tcName + "_ISO";
    Variable vs = ncfile.addVariable(g, new Variable(ncfile, g, null, vsName, DataType.STRING, tcName));
    vs.addAttribute(new Attribute(CDM.UNITS, "ISO8601"));

    // coordinate values
    String[] dataS = new String[n];
    int count = 0;
    for (CalendarDate val : rtc.getRuntimesSorted()) {
      dataS[count++] = val.toString();
    }
    vs.setCachedData(Array.factory(DataType.STRING, new int[]{n}, dataS));

    // now convert to udunits
    double[] data = new double[n];
    count = 0;
    for (double val : rtc.getRuntimesUdunits()) {
      data[count++] = val;
    }
    v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{n}, data));
  }

  private void makeTime2D(NetcdfFile ncfile, Group g, CoordinateTime2D time2D, boolean is2Dtime) {
     CoordinateRuntime runtime = time2D.getRuntimeCoordinate();
     //makeRuntimeCoordinate(ncfile, g, runtime);

     int nruns = runtime.getSize();
     int ntimes = time2D.getNtimes();
     String tcName = time2D.getName();
     String dims = is2Dtime ? runtime.getName()+" "+tcName : tcName;
     ncfile.addDimension(g, new Dimension(tcName, ntimes));
     Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
     String units = runtime.getUnit(); // + " since " + runtime.getFirstDate();
     v.addAttribute(new Attribute(CDM.UNITS, units));
     v.addAttribute(new Attribute(CF.STANDARD_NAME, "time"));
     v.addAttribute(new Attribute(CDM.LONG_NAME, is2Dtime ? "forecast times (2D)" : "forecast times (best)"));

     double[] data = new double[nruns * ntimes];
     for (int i=0; i<nruns * ntimes; i++) data[i] = Double.NaN;

     // coordinate values
     List<Coordinate> times = time2D.getTimes();
     if (!time2D.isTimeInterval()) {
       int runIdx = 0;
       for (Coordinate time : times) {
         CoordinateTime coordTime = (CoordinateTime) time;
         int timeIdx = 0;
         for (int val : coordTime.getOffsetSorted()) {
           data[runIdx*ntimes + timeIdx] = val + time2D.getOffset(runIdx);
           timeIdx++;
         }
         runIdx++;
       }
       v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{nruns, ntimes}, data));

     } else {

       int runIdx = 0;
       for (Coordinate time : times) {
         CoordinateTimeIntv coordTime = (CoordinateTimeIntv) time;
         int timeIdx = 0;
         for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals()) {
           data[runIdx*ntimes + timeIdx] = tinv.getBounds2() + time2D.getOffset(runIdx); // use upper bounds for coord value
           timeIdx++;
         }
         runIdx++;
       }
       v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{nruns, ntimes}, data));

       // bounds
       String intvName = cust.getIntervalName(time2D.getCode());
       if (intvName != null)
         v.addAttribute(new Attribute(CDM.LONG_NAME, intvName));
       String bounds_name = tcName + "_bounds";

       Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, bounds_name, DataType.DOUBLE, dims + " 2"));
       v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
       bounds.addAttribute(new Attribute(CDM.UNITS, units));
       bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));

       data = new double[nruns * ntimes * 2];
       for (int i=0; i<nruns * ntimes * 2; i++) data[i] = Double.NaN;

       runIdx = 0;
       for (Coordinate time : times) {
         CoordinateTimeIntv coordTime = (CoordinateTimeIntv) time;
         int timeIdx = 0;
         for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals()) {
           data[runIdx*ntimes + timeIdx] = tinv.getBounds1() + time2D.getOffset(runIdx);
           data[runIdx*ntimes + timeIdx+1] = tinv.getBounds2() + time2D.getOffset(runIdx);
           timeIdx++;
         }
         runIdx++;
       }
       int[] shape = is2Dtime ? new int[]{nruns, ntimes, 2} : new int[]{ntimes, 2};
       bounds.setCachedData(Array.factory(DataType.DOUBLE, shape, data));
     }
   }

  private void makeTimeCoordinate2D(NetcdfFile ncfile, Group g, Coordinate tc, CoordinateRuntime runtime) {
    int nruns = runtime.getSize();
    int ntimes = tc.getSize();
    String tcName = tc.getName();
    String dims = runtime.getName()+" "+tc.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = tc.getUnit()+ " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, "time"));

    double[] data = new double[nruns * ntimes];
    int count = 0;

    // coordinate values
    if (tc instanceof CoordinateTime) {
      CoordinateTime coordTime = (CoordinateTime) tc;
      CalendarPeriod period = coordTime.getPeriod();
      for (CalendarDate cd : runtime.getRuntimesSorted()) {
        double offset = period.getOffset(runtime.getFirstDate(), cd);
        for (int val : coordTime.getOffsetSorted())
          data[count++] = offset + val;
      }
      v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{nruns, ntimes}, data));

    } else if (tc instanceof CoordinateTimeIntv) {
      CoordinateTimeIntv coordTime = (CoordinateTimeIntv) tc;
      CalendarPeriod period = coordTime.getPeriod();

      // use upper bounds for coord value
      for (CalendarDate cd : runtime.getRuntimesSorted()) {
        double offset = period.getOffset(runtime.getFirstDate(), cd);
        for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals())
          data[count++] = offset + tinv.getBounds2();
      }
      v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{nruns, ntimes}, data));

      // bounds
      String intvName = cust.getIntervalName(tc.getCode());
      if (intvName != null)
        v.addAttribute(new Attribute(CDM.LONG_NAME, intvName));
      String bounds_name = tcName + "_bounds";

      Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, bounds_name, DataType.DOUBLE, dims + " 2"));
      v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
      bounds.addAttribute(new Attribute(CDM.UNITS, units));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));

      data = new double[nruns * ntimes * 2];
      count = 0;
      for (CalendarDate cd : runtime.getRuntimesSorted()) {
        double offset = period.getOffset(runtime.getFirstDate(), cd);
        for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals()) {
          data[count++] = offset + tinv.getBounds1();
          data[count++] = offset + tinv.getBounds2();
        }
      }
      bounds.setCachedData(Array.factory(DataType.DOUBLE, new int[]{nruns, ntimes, 2}, data));
    }
  }

  private void makeTimeCoordinate1D(NetcdfFile ncfile, Group g, CoordinateTime coordTime, CoordinateRuntime runtime) {
    int ntimes = coordTime.getSize();
    String tcName = coordTime.getName();
    String dims = coordTime.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = coordTime.getUnit()+ " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, "time"));

    double[] data = new double[ntimes];
    int count = 0;

    // coordinate values
    for (int val : coordTime.getOffsetSorted())
      data[count++] = val;
    v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ntimes}, data));
  }

  private void makeTimeCoordinate1D(NetcdfFile ncfile, Group g, CoordinateTimeIntv coordTime, CoordinateRuntime runtime) {
    int ntimes = coordTime.getSize();
    String tcName = coordTime.getName();
    String dims = coordTime.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = coordTime.getUnit()+ " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, "time"));

    double[] data = new double[ntimes];
    int count = 0;

    CalendarPeriod period = coordTime.getPeriod();

  // use upper bounds for coord value
    for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals())
      data[count++] = tinv.getBounds2();
    v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ntimes}, data));

    // bounds
    String intvName = cust.getIntervalName(coordTime.getCode());
    if (intvName != null)
      v.addAttribute(new Attribute(CDM.LONG_NAME, intvName));
    String bounds_name = tcName + "_bounds";

    Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, bounds_name, DataType.DOUBLE, dims + " 2"));
    v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
    bounds.addAttribute(new Attribute(CDM.UNITS, units));
    bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));

    data = new double[ntimes * 2];
    count = 0;
    for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals()) {
      data[count++] = tinv.getBounds1();
      data[count++] = tinv.getBounds2();
    }
    bounds.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ntimes, 2}, data));
  }

  private void makeVerticalCoordinate(NetcdfFile ncfile, Group g, CoordinateVert vc) {
      int n = vc.getSize();
      String vcName = vc.getName().toLowerCase();
      ncfile.addDimension(g, new Dimension(vcName, n));
      Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, vcName, DataType.FLOAT, vcName));
      if (vc.getUnit() != null) {
        v.addAttribute(new Attribute(CDM.UNITS, vc.getUnit()));
        String desc = cust.getTableValue("4.5", vc.getCode());
        if (desc != null) v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
        v.addAttribute(new Attribute(CF.POSITIVE, vc.isPositiveUp() ? CF.POSITIVE_UP : CF.POSITIVE_DOWN));
      }

      v.addAttribute(new Attribute("Grib2_level_type", vc.getCode()));
      VertCoord.VertUnit vu = Grib2Utils.getLevelUnit(vc.getCode());
      if (vu != null) {
        if (vu.getDatum() != null)
          v.addAttribute(new Attribute("datum", vu.getDatum()));
      }

      if (vc.isLayer()) {
        float[] data = new float[n];
        int count = 0;
        for (VertCoord.Level val : vc.getLevelSorted())
          data[count++] = (float) (val.getValue1() + val.getValue2()) / 2;
        v.setCachedData(Array.factory(DataType.FLOAT, new int[]{n}, data));

        Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, vcName + "_bounds", DataType.FLOAT, vcName + " 2"));
        v.addAttribute(new Attribute(CF.BOUNDS, vcName + "_bounds"));
        bounds.addAttribute(new Attribute(CDM.UNITS, vc.getUnit()));
        bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + vcName));

        data = new float[2 * n];
        count = 0;
        for (VertCoord.Level level : vc.getLevelSorted()) {
          data[count++] = (float) level.getValue1();
          data[count++] = (float) level.getValue2();
        }
        bounds.setCachedData(Array.factory(DataType.FLOAT, new int[]{n, 2}, data));

      } else {
        float[] data = new float[n];
        int count = 0;
        for (VertCoord.Level val : vc.getLevelSorted())
          data[count++] = (float) val.getValue1();
        v.setCachedData(Array.factory(DataType.FLOAT, new int[]{n}, data));
      }
  }


  private String searchCoord(GribCollection gc, Grib2Utils.LatLonCoordType type, List<GribCollection.VariableIndex> list) {
    if (type == null) return null;

    GribCollection.VariableIndex lat, lon;
    switch (type) {
      case U:
        lat = searchCoord(list, 198);
        lon = searchCoord(list, 199);
        return (lat != null && lon != null) ? makeVariableName(cust, gc, lat) + " "+ makeVariableName(cust, gc, lon) : null;
      case V:
        lat = searchCoord(list, 200);
        lon = searchCoord(list, 201);
        return (lat != null && lon != null) ? makeVariableName(cust, gc, lat) + " "+ makeVariableName(cust, gc, lon) : null;
      case P:
        lat = searchCoord(list, 202);
        lon = searchCoord(list, 203);
        return (lat != null && lon != null) ? makeVariableName(cust, gc, lat) + "  "+ makeVariableName(cust, gc, lon) : null;
    }
    return null;
  }

  private GribCollection.VariableIndex searchCoord(List<GribCollection.VariableIndex> list, int p) {
    for (GribCollection.VariableIndex vindex : list) {
      if ((vindex.discipline == 0) && (vindex.category == 2) && (vindex.parameter == p))
        return vindex;
    }
    return null;
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
    if (isPartitioned)
      result = readDataFromPartition(v2, section, null);
    else
      result = readDataFromCollection(v2, section, null);

    long took = System.currentTimeMillis() - start;
    if (debugTime) System.out.println("  read data took=" + took + " msec ");
    return result;
  }

  // LOOK this is by Variable - might want to do over variables, so only touch a file once, if multiple variables in a file
  @Override
  public long streamToByteChannel(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    long start = System.currentTimeMillis();

    /* if (isPartitioned)
      streamDataFromPartition(v2, section, channel);
    else */
      readDataFromCollection(v2, section, channel);

    long took = System.currentTimeMillis() - start;
    if (debugTime) System.out.println("  read data took=" + took + " msec ");
    return 0;
  }

///////////////////////////////////////////////////////

  private Array readDataFromCollection(Variable v, Section section, WritableByteChannel channel) throws IOException, InvalidRangeException {
    GribCollection.VariableIndex vindex = (GribCollection.VariableIndex) v.getSPobject();

    // first time, read records and keep in memory
    vindex.readRecords();

    int sectionLen = section.getRank();
    Range yRange = section.getRange(sectionLen - 2);
    Range xRange = section.getRange(sectionLen-1);

    //int[] otherShape = new int[sectionLen-2];
    //System.arraycopy(vindex.getSparseArray().getShape(), 0, otherShape, 0, sectionLen-2);  // LOOK WRONG
    Section sectionWanted = section.subSection(0, sectionLen-2); // all but x, y
    Section.Iterator iterWanted = sectionWanted.getIterator(v.getShape());
    int[] indexWanted = new int[sectionLen-2];                              // place to put the iterator result

     // collect all the records that need to be read
    DataReader dataReader = new DataReader(vindex);
    int count = 0;
    while (iterWanted.hasNext()) {
      int sourceIndex = iterWanted.next(indexWanted);
      dataReader.addRecord(sourceIndex, count++);
    }

    /*
    DataReaderPartitioned dataReader = new DataReaderPartitioned();
    int resultPos = 0;
    while (iterWanted.hasNext()) {
      iterWanted.next(indexWanted);   // returns the vindexP index in indexWanted aaray

      PartitionCollection.DataRecord record = vindexP.getDataRecord(indexWanted);
      if (record == null) {
        vindexP.getDataRecord(indexWanted); // debug
        resultPos++;
        continue;
      }
      record.resultIndex = resultPos;
      dataReader.addRecord(record);
      resultPos++;
    }
    */

    // sort by file and position, then read
    DataReceiverIF dataReceiver = channel == null ? new DataReceiver(section, yRange, xRange) : new ChannelReceiver(channel, yRange, xRange);
    dataReader.read(dataReceiver);
    return dataReceiver.getArray();
  }

  private class DataReader {
    GribCollection.VariableIndex vindex;
    List<DataRecord> records = new ArrayList<>();

    private DataReader(GribCollection.VariableIndex vindex) {
      this.vindex = vindex;
    }

    void addRecord(int sourceIndex, int resultIndex) {
      GribCollection.Record record = vindex.getSparseArray().getContent(sourceIndex);
      if (record != null)
        records.add(new DataRecord(resultIndex, record.fileno, record.pos, record.bmsPos, record.scanMode));
    }

    void read(DataReceiverIF dataReceiver) throws IOException {
      Collections.sort(records);

      int currFile = -1;
      RandomAccessFile rafData = null;
      for (DataRecord dr : records) {
        if (dr.fileno != currFile) {
          if (rafData != null) rafData.close();
          rafData = gribCollection.getDataRaf(dr.fileno);
          currFile = dr.fileno;
        }

        if (dr.drsPos == GribCollection.MISSING_RECORD) continue;

        if (debugRead) { // for validation
          show(rafData, Grib2RecordScanner.findRecordByDrspos(rafData, dr.drsPos), dr.drsPos);
        }

        GdsHorizCoordSys hcs = vindex.group.getGdsHorizCoordSys();
        int scanMode = (dr.scanMode == Grib2Index.ScanModeMissing) ? hcs.scanMode : dr.scanMode;
        float[] data = Grib2Record.readData(rafData, dr.drsPos, dr.bmsPos, hcs.gdsNumberPoints, scanMode,
                hcs.nxRaw, hcs.nyRaw, hcs.nptsInLine);
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }
      if (rafData != null) rafData.close();
    }

    private class DataRecord implements Comparable<DataRecord> {
      int resultIndex; // index in the ens / time / vert array
      int fileno;
      long drsPos;
      long bmsPos;
      int scanMode;

      DataRecord(int resultIndex, int fileno, long drsPos, long bmsPos, int scanMode) {
        this.resultIndex = resultIndex;
        this.fileno = fileno;
        this.drsPos = drsPos;
        this.bmsPos = bmsPos;
        this.scanMode = scanMode;
      }

      @Override
      public int compareTo(DataRecord o) {
        int r = Misc.compare(fileno, o.fileno);
        if (r != 0) return r;
        return Misc.compare(drsPos, o.drsPos);
      }
    }
  }

  private interface DataReceiverIF {
    void addData(float[] data, int resultIndex, int nx) throws IOException;
    Array getArray();
  }

  private class DataReceiver implements DataReceiverIF {
    private Array dataArray;
    private Range yRange, xRange;
    private int horizSize;

    DataReceiver(Section section, Range yRange, Range xRange) {
      dataArray = Array.factory(DataType.FLOAT, section.getShape());
      this.yRange = yRange;
      this.xRange = xRange;
      this.horizSize = yRange.length() * xRange.length();

      // prefill with NaNs, to deal with missing data
      IndexIterator iter = dataArray.getIndexIterator();
      while (iter.hasNext())
        iter.setFloatNext(Float.NaN);
    }

    public void addData(float[] data, int resultIndex, int nx) throws IOException {
      int start = resultIndex * horizSize;
      int count = 0;
      for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
        for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
          int dataIdx = y * nx + x;
          if (dataIdx >= data.length)
            System.out.println("HEY");
          dataArray.setFloat(start + count, data[dataIdx]);
          count++;
        }
      }
    }

    public Array getArray() {
      return dataArray;
    }
  }

  private class ChannelReceiver implements DataReceiverIF {
    private WritableByteChannel channel;
    private DataOutputStream outStream;
    private Range yRange, xRange;

    ChannelReceiver(WritableByteChannel channel, Range yRange, Range xRange) {
      this.channel = channel;
      this.outStream = new DataOutputStream(Channels.newOutputStream(channel));
      this.yRange = yRange;
      this.xRange = xRange;
    }

    public void addData(float[] data, int resultIndex, int nx) throws IOException {
       // LOOK: write some ncstream header
      // outStream.write(header);

      // now write the data
      for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
        for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
          int dataIdx = y * nx + x;
          outStream.writeFloat(data[dataIdx]);
        }
      }
    }

    public Array getArray() {
      return null;
    }
  }

  private void show(RandomAccessFile rafData, Grib2Record gr, long pos) {
    if (gr != null) {
       Formatter f = new Formatter();
       f.format("File=%s%n", rafData.getLocation());
       f.format("  Parameter=%s%n", cust.getVariableName(gr));
       f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
       f.format("  ForecastDate=%s%n", cust.getForecastDate(gr));
       TimeCoord.TinvDate tinv = cust.getForecastTimeInterval(gr);
       if (tinv != null) f.format("  TimeInterval=%s%n",tinv);
       f.format("  ");
       gr.getPDS().show(f);
       System.out.printf("%nGrib2Record.readData at drsPos %d = %s%n", pos, f.toString());
     }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  private Array readDataFromPartition(Variable v, Section section, WritableByteChannel channel) throws IOException, InvalidRangeException {
    PartitionCollection.VariableIndexPartitioned vindexP = (PartitionCollection.VariableIndexPartitioned) v.getSPobject(); // the variable in the partition collection

    //boolean hasRuntime = vindexP.isTwod;

    int sectionLen = section.getRank();
    Range yRange = section.getRange(sectionLen - 2);  // last 2
    Range xRange = section.getRange(sectionLen-1);
    Section sectionWanted = section.subSection(0, sectionLen - 2); // all but x, y
    Section.Iterator iterWanted = sectionWanted.getIterator(v.getShape());  // iterator over wanted indices in vindexP
    int[] indexWanted = new int[sectionLen-2];                              // place to put the iterator result

        // collect all the records that need to be read
    DataReaderPartitioned dataReader = new DataReaderPartitioned();
    int resultPos = 0;
    while (iterWanted.hasNext()) {
      iterWanted.next(indexWanted);   // returns the vindexP index in indexWanted aaray

      PartitionCollection.DataRecord record = vindexP.getDataRecord(indexWanted);
      if (record == null) {
        vindexP.getDataRecord(indexWanted); // debug
        resultPos++;
        continue;
      }
      record.resultIndex = resultPos;
      dataReader.addRecord(record);
      resultPos++;
    }

 /*
     if (hasRuntime) {  // 2d

      Range runtimeRange = section.getRange(0);   // for the moment, assume always partitioned on runtime
      Section otherSection = section.subSection(1, sectionLen - 2); // all but x, y; so this is time, vert, ens

      // collect all the records from this partition that need to be read
      int resultPos = 0;
      for (int runtimeIdx = runtimeRange.first(); runtimeIdx <= runtimeRange.last(); runtimeIdx += runtimeRange.stride()) {
        int partno = vindexP.getPartition2D(runtimeIdx);
        GribCollection.VariableIndex vindex = vindexP.getVindex(partno); // the component variable in this partition

        int[] otherShape = new int[sectionLen-3];
        System.arraycopy(vindex.sa.getShape(), 1, otherShape, 0, sectionLen-3);
        Section.Iterator iter = otherSection.getIterator(otherShape);

         // collect all the records that need to be read
        while (iter.hasNext()) {
          int sourceIndex = iter.next(null);
          //LOOK this is wrong, sourceIndex is from full variable. what is the index into the component variable ??

          dataReader.addRecord(partno, vindex, sourceIndex, resultPos++);
        }
      }

    } else { // 1
      Range timeRange = section.getRange(0);
      Section otherSection = section.subSection(1, sectionLen-2); // all but x, y

      // collect all the records from this partition that need to be read
      int resultPos = 0;
      for (int timeIdx = timeRange.first(); timeIdx <= timeRange.last(); timeIdx += timeRange.stride()) {
        int partno = vindexP.getPartition1D(timeIdx);
        if (partno < 0) continue; // missing
        GribCollection.VariableIndex vindex = vindexP.getVindex(partno); // the variable in this partition

        int[] otherShape = new int[sectionLen-3];
        System.arraycopy(vindex.sa.getShape(), 1, otherShape, 0, sectionLen-3);
        Section.Iterator iter = otherSection.getIterator(otherShape);

         // collect all the records that need to be read
        while (iter.hasNext()) {
          int sourceIndex = iter.next(null);
          dataReader.addRecord(partno, vindex, sourceIndex, resultPos++);
        }
      }

    }
  */

    // sort by file and position, then read
        // sort by file and position, then read
    DataReceiverIF dataReceiver = channel == null ? new DataReceiver(section, yRange, xRange) : new ChannelReceiver(channel, yRange, xRange);
    dataReader.read(dataReceiver);

    // close partitions as needed
    vindexP.cleanup();

    return dataReceiver.getArray();
  }

  private class DataReaderPartitioned {
    List<PartitionCollection.DataRecord> records = new ArrayList<>();

    void addRecord(PartitionCollection.DataRecord dr) {
      if (dr != null) records.add(dr);
    }

    void read(DataReceiverIF dataReceiver) throws IOException {
      Collections.sort(records);

      PartitionCollection.DataRecord lastRecord = null;
      RandomAccessFile rafData = null;

      for (PartitionCollection.DataRecord dr : records) {
        if ((rafData == null) || !dr.usesSameFile(lastRecord)) {
          if (rafData != null) rafData.close();
          rafData = dr.usePartition.getRaf(dr.partno, dr.fileno);
        }
        lastRecord = dr;

        if (dr.drsPos == GribCollection.MISSING_RECORD) continue;

        if (debugRead) { // for validation
          show( rafData, Grib2RecordScanner.findRecordByDrspos(rafData, dr.drsPos), dr.drsPos);
        }

        //GdsHorizCoordSys hcs = dr.vindex.group.getGdsHorizCoordSys();
        GdsHorizCoordSys hcs = dr.hcs;
        int scanMode = (dr.scanMode == Grib2Index.ScanModeMissing) ? hcs.scanMode : dr.scanMode;
        float[] data = Grib2Record.readData(rafData, dr.drsPos, dr.bmsPos, hcs.gdsNumberPoints, scanMode,
                hcs.nxRaw, hcs.nyRaw, hcs.nptsInLine);
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }
      if (rafData != null) rafData.close();
    }
  }

}
