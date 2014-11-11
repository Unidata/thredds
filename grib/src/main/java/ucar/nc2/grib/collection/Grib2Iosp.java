/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.grib.collection;

import thredds.catalog.DataFormatType;
import ucar.coord.CoordinateTimeAbstract;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
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
  static private final boolean debugTime = false, debugRead = false, debugName = false;
 // static private boolean useGenType = false; // LOOK dummy for now

  static public String makeVariableNameFromTable(Grib2Customizer tables, GribCollectionImmutable gribCollection, 
                                                 GribCollectionImmutable.VariableIndex vindex, boolean useGenType) {
    Formatter f = new Formatter();

    GribTables.Parameter param = tables.getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());

    if (param == null) {
      f.format("VAR%d-%d-%d_FROM_%d-%d-%d", vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter(), gribCollection.getCenter(), 
              gribCollection.getSubcenter(), vindex.getTableVersion());
    } else {
      f.format("%s", GribUtils.makeNameFromDescription(param.getName()));
    }

    if (vindex.getGenProcessType() == 6 || vindex.getGenProcessType() == 7) {
      f.format("_error");  // its an "error" type variable - add to name

    } else if (useGenType && vindex.getGenProcessType() >= 0) {
        String genType = tables.getGeneratingProcessTypeName(vindex.getGenProcessType());
        String s = StringUtil2.substitute(genType, " ", "_");
        f.format("_%s", s);
    }

    if (vindex.getLevelType() != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_%s", tables.getLevelNameShort(vindex.getLevelType())); // vindex.getLevelType()); // code table 4.5
      if (vindex.isLayer()) f.format("_layer");
    }

    String intvName = vindex.getIntvName();
    if (intvName != null && !intvName.isEmpty()) {
      f.format("_%s", intvName);
    }

    if (vindex.getIntvType() >= 0) {
      String statName = tables.getStatisticNameShort(vindex.getIntvType());
      if (statName != null) f.format("_%s", statName);
    }

    if (vindex.getEnsDerivedType() >= 0) {
      f.format("_%s", tables.getProbabilityNameShort(vindex.getEnsDerivedType()));
    } else if (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0) {
      String s = StringUtil2.substitute(vindex.getProbabilityName(), ".", "p");
      f.format("_probability_%s", s);
    } else if (vindex.isEnsemble()) {
      f.format("_ens");
    }

    return f.toString();
  }

  static public String makeVariableLongName(Grib2Customizer cust, GribCollectionImmutable.VariableIndex vindex, boolean useGenType) {
    Formatter f = new Formatter();

    boolean isProb = (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0);
    if (isProb)
      f.format("Probability ");

    GribTables.Parameter gp = cust.getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
    if (gp == null)
      f.format("Unknown Parameter %d-%d-%d", vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
    else
      f.format("%s", gp.getName());

    if (vindex.getIntvType() >= 0 && vindex.getIntvName() != null && !vindex.getIntvName().isEmpty()) {
      String intvName = cust.getStatisticNameShort(vindex.getIntvType());
      if (intvName == null || intvName.equalsIgnoreCase("Missing")) intvName = cust.getStatisticNameShort(vindex.getIntvType());
      if (intvName == null) f.format(" (%s)", vindex.getIntvName());
      else f.format(" (%s %s)", vindex.getIntvName(), intvName);

    } else if (vindex.getIntvType() >= 0) {
      String intvName = cust.getStatisticNameShort(vindex.getIntvType());
      f.format(" (%s)", intvName);
    }

    if (vindex.getEnsDerivedType() >= 0)
      f.format(" (%s)", cust.getTableValue("4.7", vindex.getEnsDerivedType()));

    else if (isProb)
      f.format(" %s %s", vindex.getProbabilityName(), getVindexUnits(cust, vindex)); // add data units here

    if (vindex.getGenProcessType() == 6 || vindex.getGenProcessType() == 7) {
      f.format(" error");  // its an "error" type variable - add to name

    } else if (useGenType && vindex.getGenProcessType() >= 0) {
      f.format(" %s", cust.getGeneratingProcessTypeName(vindex.getGenProcessType()));
    }

    if (vindex.getLevelType() != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format(" @ %s", cust.getTableValue("4.5", vindex.getLevelType()));
      if (vindex.isLayer()) f.format(" layer");
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
  @Override
  protected String makeVariableNameFromRecord(GribCollectionImmutable.VariableIndex vindex) {
    Formatter f = new Formatter();

    f.format("VAR_%d-%d-%d", vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());

    if (vindex.getGenProcessType() == 6 || vindex.getGenProcessType() == 7) {
      f.format("_error");  // its an "error" type variable - add to name
    }

    if (vindex.getLevelType() != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_L%d", vindex.getLevelType()); // code table 4.5
      if (vindex.isLayer()) f.format("_layer");
    }

    String intvName = vindex.getIntvName();
    if (intvName != null && !intvName.isEmpty()) {
      if (intvName.equals(CoordinateTimeAbstract.MIXED_INTERVALS))
        f.format("_Imixed");
      else
        f.format("_I%s", intvName);
    }

    if (vindex.getIntvType() >= 0) {
      f.format("_S%s", vindex.getIntvType());
    }

    if (vindex.getEnsDerivedType() >= 0) {
      f.format("_D%d", vindex.getEnsDerivedType());
    } else if (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0) {
      String s = StringUtil2.substitute(vindex.getProbabilityName(), ".", "p");
      f.format("_Prob_%s", s);
    }

    return f.toString();
  }


  @Override
  protected String makeVariableName(GribCollectionImmutable.VariableIndex vindex) {
    return makeVariableNameFromTable(cust, gribCollection, vindex, false);  // LOOK where to get useGenType ?
  }

  @Override
  protected String makeVariableLongName(GribCollectionImmutable.VariableIndex vindex) {
    return makeVariableLongName(cust, vindex, false);                       // LOOK where to get useGenType ?
  }

  @Override
  protected String makeVariableUnits(GribCollectionImmutable.VariableIndex vindex) {
    return makeVariableUnits(cust, vindex);
  }

  static public String makeVariableUnits(Grib2Customizer tables, GribCollectionImmutable.VariableIndex vindex) {
    if (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0) return "%";
    return getVindexUnits(tables, vindex);
  }

  static private String getVindexUnits(Grib2Customizer tables, GribCollectionImmutable.VariableIndex vindex) {
    GribTables.Parameter gp = tables.getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
    String val = (gp == null) ? "" : gp.getUnit();
    return (val == null) ? "" : val;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Grib2Customizer cust;

  // accept grib2 or ncx2 files
  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    GribCdmIndex.GribCollectionType type = GribCdmIndex.getType(raf);
    if (type == GribCdmIndex.GribCollectionType.GRIB2) return true;
    if (type == GribCdmIndex.GribCollectionType.Partition2) return true;

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
    super(false, logger);
  }

  public Grib2Iosp(GribCollectionImmutable.GroupGC gHcs, GribCollection.Type gtype) {
    super(false, logger);
    this.gHcs = gHcs;
    this.owned = true;
    this.gtype = gtype;
  }

  // LOOK more likely we will set an individual dataset
  public Grib2Iosp(GribCollectionImmutable gc) {
    super(false, logger);
    this.gribCollection = gc;
    this.owned = true;
  }

  protected String getIntervalName(int id) {
    return cust.getStatisticName(id);
  }

  @Override
  protected ucar.nc2.grib.GribTables createCustomizer() {
    cust = Grib2Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), gribCollection.getLocal(),
            gribCollection.getGenProcessId());
    return cust;
  }

  @Override
  protected void addGlobalAttributes(NetcdfFile ncfile) {
    String val = cust.getTableValue("4.3", gribCollection.getGenProcessType());
    if (val != null)
      ncfile.addAttribute(null, new Attribute("Type_of_generating_process", val));
    val = cust.getGeneratingProcessName(gribCollection.getGenProcessId());
    if (val != null)
      ncfile.addAttribute(null, new Attribute("Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre", val));
    val = cust.getGeneratingProcessName(gribCollection.getBackProcessId());
    if (val != null)
      ncfile.addAttribute(null, new Attribute("Background_generating_process_identifier_defined_by_originating_centre", val));
  }

  @Override
  protected String getVerticalCoordDesc(int vc_code) {
    return cust.getTableValue("4.5", vc_code);
  }

  @Override
  protected GribTables.Parameter getParameter(GribCollectionImmutable.VariableIndex vindex) {
    return cust.getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
  }

  @Override
  protected void addVariableAttributes(Variable v, GribCollectionImmutable.VariableIndex vindex) {

    v.addAttribute(new Attribute(VARIABLE_ID_ATTNAME, makeVariableNameFromRecord(vindex)));
    int[] param = new int[]{vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter()};
    v.addAttribute(new Attribute("Grib2_Parameter", Array.factory(param)));
    String disc = cust.getTableValue("0.0", vindex.getDiscipline());
    if (disc != null) v.addAttribute(new Attribute("Grib2_Parameter_Discipline", disc));
    String cat = cust.getCategory(vindex.getDiscipline(), vindex.getCategory());
    if (cat != null)
      v.addAttribute(new Attribute("Grib2_Parameter_Category", cat));
    Grib2Customizer.Parameter entry = cust.getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
    if (entry != null) v.addAttribute(new Attribute("Grib2_Parameter_Name", entry.getName()));

    if (vindex.getLevelType() != GribNumbers.MISSING) {
      String levelTypeName = cust.getLevelName(vindex.getLevelType());
      if (levelTypeName != null)
        v.addAttribute(new Attribute("Grib2_Level_Type", levelTypeName));
      else
        v.addAttribute(new Attribute("Grib2_Level_Type", vindex.getLevelType()));
    }

    if (vindex.getEnsDerivedType() >= 0)
      v.addAttribute(new Attribute("Grib2_Ensemble_Derived_Type", vindex.getEnsDerivedType()));
    else if (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0) {
      v.addAttribute(new Attribute("Grib2_Probability_Type", vindex.getProbType()));
      v.addAttribute(new Attribute("Grib2_Probability_Name", vindex.getProbabilityName()));
    }

    if (vindex.getGenProcessType() >= 0) {
      String genProcessTypeName = cust.getGeneratingProcessTypeName(vindex.getGenProcessType());
      if (genProcessTypeName != null)
        v.addAttribute(new Attribute("Grib2_Generating_Process_Type", genProcessTypeName));
      else
        v.addAttribute(new Attribute("Grib2_Generating_Process_Type", vindex.getGenProcessType()));
    }
  }

  @Override
  protected void show(RandomAccessFile rafData, long pos) throws IOException {
    Grib2Record gr = Grib2RecordScanner.findRecordByDrspos(rafData, pos);
    if (gr != null) {
      Formatter f = new Formatter();
      f.format("File=%s%n", rafData.getLocation());
      f.format("  Parameter=%s%n", cust.getVariableName(gr));
      f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
      f.format("  ForecastDate=%s%n", cust.getForecastDate(gr));
      TimeCoord.TinvDate tinv = cust.getForecastTimeInterval(gr);
      if (tinv != null) f.format("  TimeInterval=%s%n", tinv);
      f.format("  ");
      gr.getPDS().show(f);
      System.out.printf("%nGrib2Record.readData at drsPos %d = %s%n", pos, f.toString());
    }
  }

  @Override
  protected float[] readData(RandomAccessFile rafData, GribIosp.DataRecord dr) throws IOException {
    GdsHorizCoordSys hcs = dr.hcs;
    int scanMode = (dr.scanMode == Grib2Index.ScanModeMissing) ? hcs.scanMode : dr.scanMode;
    return Grib2Record.readData(rafData, dr.dataPos, dr.bmsPos, hcs.gdsNumberPoints, scanMode,
            hcs.nxRaw, hcs.nyRaw, hcs.getNptsInLine());
  }

  public Object getLastRecordRead() {
    return Grib2Record.lastRecordRead;
  }

  public void clearLastRecordRead() {
    Grib2Record.lastRecordRead = null;
  }

  public Object getGribCustomizer() {
    return cust;
  }
}
