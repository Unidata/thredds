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

package ucar.nc2.grib.grib2;

import thredds.catalog.DataFormatType;
import thredds.inventory.CollectionManager;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.grib.*;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.StringUtil2;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.Formatter;

/**
 * Grib-2 Collection IOSP.
 * Handles both collections and single GRIB files.
 *
 * @author caron
 * @since 4/6/11
 */
public class Grib2Iosp extends GribIosp {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2Iosp.class);
  static private final boolean debugTime = false, debugRead = false, debugName = false;
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

    if (vindex.intvName != null && !vindex.intvName.isEmpty()) {
      f.format("_%s", vindex.intvName);
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

    if (vindex.intvName != null  && !vindex.intvName.isEmpty()) {
      if (vindex.intvName.equals("Mixed_intervals"))
        f.format("_Imixed");
      else
        f.format("_I%s", vindex.intvName);
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

    if (vindex.intvType >= 0 && vindex.intvName != null && !vindex.intvName.isEmpty()) {
      String intvName = cust.getIntervalNameShort(vindex.intvType);
      if (intvName == null || intvName.equalsIgnoreCase("Missing")) intvName = cust.getIntervalNameShort(vindex.intvType);
      if (intvName == null) f.format(" (%s)", vindex.intvName);
      else f.format(" (%s %s)", vindex.intvName, intvName);

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

  private Grib2TimePartition timePartition;
  private GribCollection gribCollection;
  private Grib2Customizer cust;
  private GribCollection.GroupHcs gHcs;
  private boolean isTimePartitioned;
  private boolean owned; // if Iosp is owned by GribCollection; affects close()

  // accept grib2 or ncx files
  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    byte[] b = new byte[Grib2CollectionBuilder.MAGIC_START.length()];
    raf.readFully(b);
    String magic = new String(b);
    if (magic.equals(Grib2CollectionBuilder.MAGIC_START)) return true;
    if (magic.equals(Grib2TimePartitionBuilder.MAGIC_START)) return true;  // so must be same length as Grib2CollectionBuilder.MAGIC_START

    // check for GRIB2 file
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

  public Grib2Iosp(GribCollection.GroupHcs gHcs) {
    this.gHcs = gHcs;
    this.owned = true;
  }

  public Grib2Iosp(GribCollection gc) {
    this.gribCollection = gc;
    this.owned = true;
  }

  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);

    boolean isGribFile = (raf != null) && Grib2RecordScanner.isValidFile(raf);
    if (isGribFile) {
      this.gribCollection = GribIndex.makeGribCollectionFromSingleFile(false, raf, gribConfig, CollectionManager.Force.test, logger);
      cust = Grib2Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), gribCollection.getLocal());
    }

    if (gHcs != null) { // just use the one group that was set in the constructor
      this.gribCollection = gHcs.getGribCollection();
      if (this.gribCollection instanceof Grib2TimePartition) {
        isTimePartitioned = true;
        timePartition = (Grib2TimePartition) gribCollection;
      }
      cust = Grib2Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), gribCollection.getLocal());
      addGroup(ncfile, gHcs, false);

    } else if (gribCollection != null) { // use the gribCollection that was set in the constructor
      if (this.gribCollection instanceof Grib2TimePartition) {
        isTimePartitioned = true;
        timePartition = (Grib2TimePartition) gribCollection;
      }
      cust = Grib2Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), gribCollection.getLocal());

      List<GribCollection.GroupHcs> groups = new ArrayList<GribCollection.GroupHcs>(gribCollection.getGroups());
      Collections.sort(groups);
      boolean useGroups = groups.size() > 1;
      for (GribCollection.GroupHcs g : groups)
        addGroup(ncfile, g, useGroups);

    } else { // otherwise, its an ncx file : read in entire collection

      raf.seek(0);
      byte[] b = new byte[Grib2TimePartitionBuilder.MAGIC_START.length()];
      raf.readFully(b);
      String magic = new String(b);
      isTimePartitioned = magic.equals(Grib2TimePartitionBuilder.MAGIC_START);

      String location = raf.getLocation();
      File f = new File(location);
      int pos = f.getName().lastIndexOf(".");
      String name = (pos > 0) ? f.getName().substring(0, pos) : f.getName();

      if (isTimePartitioned) {
        timePartition = Grib2TimePartitionBuilder.createFromIndex(name, null, raf, logger);
        gribCollection = timePartition;
      } else {
        gribCollection = Grib2CollectionBuilder.createFromIndex(name, null, raf, gribConfig, logger);
      }

      cust = Grib2Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), gribCollection.getLocal());

      List<GribCollection.GroupHcs> groups = new ArrayList<GribCollection.GroupHcs>(gribCollection.getGroups());
      Collections.sort(groups);
      boolean useGroups = groups.size() > 1;
      for (GribCollection.GroupHcs g : groups)
        addGroup(ncfile, g, useGroups);
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
    ncfile.addAttribute(null, new Attribute(CDM.HISTORY, "Read using CDM IOSP Grib2Collection"));
    ncfile.addAttribute(null, new Attribute(CF.FEATURE_TYPE, FeatureType.GRID.name()));
    ncfile.addAttribute(null, new Attribute(CDM.FILE_FORMAT, getFileTypeId()));

    for (Parameter p : gribCollection.getParams())
      ncfile.addAttribute(null, new Attribute(p));
  }

  private void addGroup(NetcdfFile ncfile, GribCollection.GroupHcs gHcs, boolean useGroups) {
    gHcs.assignVertNames( cust);  // LOOK thread safety vc.setName()
    GdsHorizCoordSys hcs = gHcs.hcs;
    String grid_mapping = hcs.getName()+"_Projection";

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

    // CurvilinearOrthogonal - lat and lon fields must be present in the file
    boolean is2D = Grib2Utils.isLatLon2D(gHcs.hcs.template, gribCollection.getCenter());
    if (is2D) {
      horizDims = "lat lon";  // LOOK: orthogonal curvilinear

      // LOOK - assume same number of points for all grids
      ncfile.addDimension(g, new Dimension("lon", hcs.nx));
      ncfile.addDimension(g, new Dimension("lat", hcs.ny));

    } else if (Grib2Utils.isLatLon(gHcs.hcs.template, gribCollection.getCenter())) {
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
        dims.append(" ").append(vc.getName().toLowerCase());

      dims.append(" ").append(horizDims);

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

      if (is2D) {
        String s = searchCoord(gribCollection, Grib2Utils.getLatLon2DcoordType(desc), gHcs.varIndex);
        if (s == null) { // its a lat/lon coordinate
          v.setDimensions(horizDims); // LOOK make this 2D and munge the units
          String units = desc.contains("Latitude of") ? CDM.LAT_UNITS : CDM.LON_UNITS;
          v.addAttribute(new Attribute(CDM.UNITS, units));

        } else {
          v.addAttribute(new Attribute(CF.COORDINATES, s));
        }
      } else {
        if (!hcs.isLatLon()) v.addAttribute(new Attribute(CF.GRID_MAPPING, grid_mapping));
      }

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

      v.addAttribute(new Attribute("Grib2_Level_Type", vindex.levelType));
      if ( vindex.intvName != null && vindex.intvName.length() != 0)
        v.addAttribute(new Attribute(CDM.TIME_INTERVAL, vindex.intvName));
      if (vindex.intvType >= 0) {
        v.addAttribute(new Attribute("Grib2_Statistical_Interval_Type", vindex.intvType));
        GribStatType statType = cust.getStatType(vindex.intvType);
        if (statType != null) {
          CF.CellMethods cm = GribStatType.getCFCellMethod( statType);
          if (cm != null) v.addAttribute(new Attribute("cell_methods", tcName + ": " + cm.toString()));
        }
      }
      if (vindex.ensDerivedType >= 0)
        v.addAttribute(new Attribute("Grib2_Ensemble_Derived_Type", vindex.ensDerivedType));
      else if (vindex.probabilityName != null && vindex.probabilityName.length() > 0) {
        v.addAttribute(new Attribute("Grib2_Probability_Type", vindex.probType));
        v.addAttribute(new Attribute("Grib2_Probability_Name", vindex.probabilityName));
      }
      if (vindex.genProcessType >= 0)
        v.addAttribute(new Attribute("Grib2_Generating_Process_Type", cust.getGeneratingProcessTypeName(vindex.genProcessType)));

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
      String intvName = cust.getIntervalName(tc.getCode());
      if (intvName != null)
        v.addAttribute(new Attribute(CDM.LONG_NAME, intvName));

      Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName + "_bounds", DataType.INT, tcName + " 2"));
      v.addAttribute(new Attribute(CF.BOUNDS, tcName + "_bounds"));
      bounds.addAttribute(new Attribute(CDM.UNITS, tc.getUnits()));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));

      // coordinate intervals
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
      String vcName = vc.getName().toLowerCase();
      ncfile.addDimension(g, new Dimension(vcName, n));
      Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, vcName, DataType.FLOAT, vcName));
      if (vc.getUnits() != null) {
        v.addAttribute(new Attribute(CDM.UNITS, vc.getUnits()));
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
    if (isTimePartitioned)
      result = readDataFromPartition(v2, section);
    else
      result = readDataFromCollection(v2, section);

    long took = System.currentTimeMillis() - start;
    if (debugTime) System.out.println("  read data took=" + took + " msec ");
    return result;
  }

  // LOOK this is by Variable - might want to do over variables, so only touch a file once, if multiple variables in a file
  @Override
  public long streamToByteChannel(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    long start = System.currentTimeMillis();

    long result;
    //if (isTimePartitioned)
   //   result = streamDataFromPartition(v2, section, channel);
    //else
      result = streamDataFromCollection(v2, section, channel);

    long took = System.currentTimeMillis() - start;
    if (debugTime) System.out.println("  read data took=" + took + " msec ");
    return result;
  }

  private Array readDataFromPartition(Variable v2, Section section) throws IOException, InvalidRangeException {
    TimePartition.VariableIndexPartitioned vindexP = (TimePartition.VariableIndexPartitioned) v2.getSPobject(); // the variable in the tp collection

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
      GribCollection.VariableIndex vindex = vindexP.getVindex(partno); // the variable in this partition

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
            if (recordIndex < vindex.records.length)  {
              GribCollection.Record record = vindex.records[recordIndex];
              dataReader.addRecord(vindex, partno, record.fileno, record.pos, record.bmsPos, resultIndex);  // add this record to be read

            } else {
              Formatter f = new Formatter();
              f.format("recordIndex=%d size=%d%n", recordIndex,  vindex.records.length);
              if (flag == 0) f.format("time=%d, ens=%d, level=%d, nens=%d, nverts=%d", val.getIndex(), ensIdx, levelIdx, vindex.nens, vindex.nverts);
              else  f.format("time=%d, ens=%d, level=%d, flag=%d, nens=%s, vert=%s ensp=%s, vertp=%s", val.getIndex(), ensIdx, levelIdx, flag,
                      vindex.getEnsCoord(), vindex.getVertCoord(), vindexP.getEnsCoord(), vindexP.getVertCoord());

              logger.error("recordIndex out of bounds: "+f.toString());
            }
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

    void addRecord(GribCollection.VariableIndex vindex, int partno, int fileno, long drsPos, long bmsPos, int resultIndex) {
      records.add(new DataRecord(partno, vindex, resultIndex, fileno, drsPos, bmsPos));
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

        if (dr.drsPos == GribCollection.MISSING_RECORD) continue;

        if (debugRead) { // for validation
          show( Grib2RecordScanner.findRecordByDrspos(rafData, dr.drsPos), dr.drsPos);
        }

        float[] data = Grib2Record.readData(rafData, dr.drsPos, dr.bmsPos, dr.vindex.group.hcs.gdsNumberPoints, dr.vindex.group.hcs.scanMode,
                dr.vindex.group.hcs.nxRaw,  dr.vindex.group.hcs.nyRaw, dr.vindex.group.hcs.nptsInLine);
        dataReceiver.addData(data, dr.resultIndex, dr.vindex.group.hcs.nx);
      }
      if (rafData != null) rafData.close();
    }

    private class DataRecord implements Comparable<DataRecord> {
      int partno; // partition index
      GribCollection.VariableIndex vindex; // the vindex of the partition
      int resultIndex; // where does this record go in the result array?
      int fileno;
      long drsPos;
      long bmsPos;  // if non zero, use alernate bms

      DataRecord(int partno, GribCollection.VariableIndex vindex, int resultIndex, int fileno, long drsPos, long bmsPos) {
        this.partno = partno;
        this.vindex = vindex;
        this.resultIndex = resultIndex;
        this.fileno = fileno;
        this.drsPos = (drsPos == 0) ? GribCollection.MISSING_RECORD : drsPos; // 0 also means missing in Grib2
        this.bmsPos = bmsPos;
      }

      @Override
      public int compareTo(DataRecord o) {
        int r = Misc.compare(partno, o.partno);
        if (r != 0) return r;
        r = Misc.compare(fileno, o.fileno);
        if (r != 0) return r;
        return Misc.compare(drsPos, o.drsPos);
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

  private long streamDataFromCollection(Variable v, Section section, WritableByteChannel channel) throws IOException, InvalidRangeException {
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

    ChannelReceiver dataReceiver = new ChannelReceiver(channel, yRange, xRange);
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
    return 0; // LOOK
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
      records.add(new DataRecord(timeIdx, ensIdx, levIdx, resultIndex, record.fileno, record.pos, record.bmsPos));
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
          show(Grib2RecordScanner.findRecordByDrspos(rafData, dr.drsPos), dr.drsPos);
        }

        float[] data = Grib2Record.readData(rafData, dr.drsPos, dr.bmsPos, vindex.group.hcs.gdsNumberPoints, vindex.group.hcs.scanMode,
                vindex.group.hcs.nxRaw, vindex.group.hcs.nyRaw, vindex.group.hcs.nptsInLine);
        dataReceiver.addData(data, dr.resultIndex, vindex.group.hcs.nx);
      }
      if (rafData != null) rafData.close();
    }

    private class DataRecord implements Comparable<DataRecord> {
      int ensIdx, timeIdx, levIdx;
      int resultIndex; // index in the ens / time / vert array
      int fileno;
      long drsPos;
      long bmsPos;

      DataRecord(int timeIdx, int ensIdx, int levIdx, int resultIndex, int fileno, long drsPos, long bmsPos) {
        this.ensIdx = ensIdx;
        this.timeIdx = timeIdx;
        this.levIdx = levIdx;
        this.resultIndex = resultIndex;
        this.fileno = fileno;
        this.drsPos = drsPos;
        this.bmsPos = bmsPos;
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

    Array getArray() {
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
  }

  private void show(Grib2Record gr, long pos) {
    if (gr != null) {
       Formatter f = new Formatter();
       f.format("File=%s%n", raf.getLocation());
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

}
