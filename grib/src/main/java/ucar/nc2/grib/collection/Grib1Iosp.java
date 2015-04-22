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

import ucar.nc2.constants.DataFormatType;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.coord.CoordinateTimeAbstract;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib1.tables.Grib1ParamTables;
import ucar.nc2.grib.grib1.tables.Grib1WmoTimeType;
import ucar.nc2.*;
import ucar.nc2.grib.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.http.HTTPRandomAccessFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;

/**
 * Grib-2 Collection IOSP, ver2.
 * Handles both collections and single GRIB files.
 *
 * @author caron
 * @since 4/6/11
 */
public class Grib1Iosp extends GribIosp {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2Iosp.class);

    ///////////////////////////////////////////////////

   // use defaults
  //public static int cdmVariableHash(Grib1Customizer cust, Grib1Record gr) {
  //  return cdmVariableHash(cust, gr, 0, FeatureCollectionConfig.useTableVersionDef, FeatureCollectionConfig.intvMergeDef, FeatureCollectionConfig.useCenterDef);
  //}


  /*
   * A hash code to group records into a CDM variable
   * Herein lies the semantics of a variable object identity.
   * Read it and weep.
   *
   * @param cust     customizer
   * @param gr       grib record
   * @param gdsHash   can override the gdsHash
   * @param useTableVersion  use pdss.getTableVersion(), default is false
   * @param intvMerge        put all intervals together, default true
   * @param useCenter        use center id when param no > 127, default is false
   *
   * @return this record's hash code, to group like records into a variable
  public static int cdmVariableHash(Grib1Customizer cust, Grib1Record gr, int gdsHash, boolean useTableVersion, boolean intvMerge, boolean useCenter) {
    int result = 17;

    Grib1SectionProductDefinition pdss = gr.getPDSsection();
    result += result * 31 + pdss.getParameterNumber();

    Grib1SectionGridDefinition gdss = gr.getGDSsection();
    if (gdsHash == 0)
      gdsHash = gdss.getGDS().hashCode(); // the horizontal grid
    result += result * 31 + gdsHash;

    result += result * 31 + pdss.getLevelType();
    if (cust.isLayer(pdss.getLevelType())) result += result * 31 + 1;

    if (useTableVersion)
      result += result * 31 + pdss.getTableVersion();

    Grib1ParamTime ptime = gr.getParamTime(cust);
    if (ptime.isInterval()) {
      if (!intvMerge) result += result * 31 + ptime.getIntervalSize();  // create new variable for each interval size
      if (ptime.getStatType() != null) result += result * 31 + ptime.getStatType().ordinal(); // create new variable for each stat type
    }

    // if useCenter, and this uses any local tables, then we have to add the center id, and subcenter if present
    if (useCenter && pdss.getParameterNumber() > 127) {
      result += result * 31 + pdss.getCenter();
      if (pdss.getSubCenter() > 0)
        result += result * 31 + pdss.getSubCenter();
    }
    return result;
  }  */

  ///////////////////////////////////////////////////
  // create variable names

  /*
   http://www.ncl.ucar.edu/Document/Manuals/Ref_Manual/NclFormatSupport.shtml#GRIB
   The following section gives the algorithm NCL uses to assign names to GRIB1 variables.

   GRIB1 data variable name encoding:

     if entry matching parameter table version and parameter number is found (either in built-in or user-supplied table)
       and entry contains a short name for the parameter:
         if recognized as probability product:
           <probability_parameter_short_name>_<subject_variable_short_name> (ex: PROB_A_PCP)
         else:
           <parameter_short_name> (ex: TMP)
     else:
        VAR_<parameter_number> (ex: VAR_179)

     if pre-defined grid:
        _<pre-defined_grid_number> (ex: TMP_6)
     else if grid defined in GDS (Grid Description Section):
        _GDS<grid_type_number> (ex: TMP_GDS4)

     _<level_type_abbreviation> (ex: TMP_GDS4_ISBL)

     if not statistically processed variable and not duplicate name the name is complete at this point.

     if statistically-processed variable with constant specified statistical processing duration:
           _<statistical_processing_type_abbreviation><statistical_processing_duration><duration_units> (ex: ACPCP_44_SFC_acc6h)
     else if statistically-processed variable with no specified processing duration
        _<statistical_processing_type_abbreviation> (ex: A_PCP_192_SFC_acc)

     if variable name is duplicate of existing variable name (this should not normally occur):
        _n (where n begins with 1 for first duplicate) (ex: TMP_GDS4_ISBL_1)

 Notes:
   * Probability products are properly recognized in version 4.3.0 or later.
   * NCL uses the generic construction VAR_<parameter_number> in two situations:
     - The entry in the applicable published table contains no short name suitable for use as a component of an NCL variable name.
       Users should expect that later revisions to the table may result in the parameter receiving a short name, causing the name to change.
     - There is no recognized entry for the parameter number. In this case, NCL outputs a warning message. The parameter index
       could be unrecognized for several reasons:
         > No parameter table has been supplied for the originating center and the index is greater than 127. (The default GRIB parameter table
           properly applies only to indexes less than 128.)
         > The index is not present in the applicable parameter table, perhaps because the table is out of date or is otherwise incorrect.
         > The GRIB file has been generated incorrectly, perhaps specifying a wrong parameter table or a non-existent index.

   * Pre-defined grids are enumerated in Table B of the NCEP GRIB1 documentation.
   * GDS Grids types are listed in Table 6 of the NCEP GRIB1 documentation.
   * Level type abbreviations are taken from Table 3 of the NCEP GRIB1 documentation.
   * The abbreviations corresponding to the supported statistical processing methods are:
       ave - average
       acc - accumulation
       dif - difference
   * Note that the duration period and units abbreviation were added in NCL version 4.2.0.a028 in order to handle GRIB files with
     more than one time duration for otherwise identical variables. This is an unavoidable incompatibility for GRIB file variable
     names relative to earlier versions.

   Variable name is:

    VAR_%d-%d-%d-%d[_L%d][_layer][_I%s_S%d]

    where:
     %d-%d-%d-%d = center-subcenter-tableVersion-paramNo
     L%d = level type  (octet 10 of PDS)
     _layer = added if its a vertical layer
     S%d = stat type (octet 21 of PDS)
  */

 /*
  public String makeVariableName(Grib1SectionProductDefinition pds) {
    return makeVariableNameFromTables(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber(),
             pds.getLevelType(), isLayer(pds.getLevelType()), pds.getTimeRangeIndicator(), null);
  }*/

  @Override
  public String makeVariableName(GribCollectionImmutable.VariableIndex v) {
    return makeVariableNameFromTables(gribCollection.getCenter(), gribCollection.getSubcenter(), v.getTableVersion(), v.getParameter(),
            v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName());
  }

  @Override
  public String makeVariableNameFromRecord(GribCollectionImmutable.VariableIndex v) {
    return makeVariableNameFromRecord(gribCollection.getCenter(), gribCollection.getSubcenter(),  v.getTableVersion(), v.getParameter(),
                v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName());
  }

  private String makeVariableNameFromRecord(int center, int subcenter, int tableVersion, int paramNo,
                                 int levelType, boolean isLayer, int intvType, String intvName) {
    Formatter f = new Formatter();

    f.format("VAR_%d-%d-%d-%d", center, subcenter, tableVersion, paramNo);  // "VAR_7-15--1-20_L1";

    if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_L%d", levelType); // code table 4.5
      if (isLayer) f.format("_layer");
    }

    if (intvType >= 0) {
      if (intvName != null) {
        if (intvName.equals(CoordinateTimeAbstract.MIXED_INTERVALS))
          f.format("_Imixed");
        else
          f.format("_I%s", intvName);
      }
      f.format("_S%s", intvType);
    }

    return f.toString();
  }

  public static String makeVariableName(Grib1Customizer cust, FeatureCollectionConfig.GribConfig gribConfig, Grib1SectionProductDefinition pds) {
    return makeVariableNameFromTables(cust, gribConfig, pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber(),
             pds.getLevelType(), cust.isLayer(pds.getLevelType()), pds.getTimeRangeIndicator(), null);
  }

  private static String makeVariableNameFromTables(Grib1Customizer cust, FeatureCollectionConfig.GribConfig gribConfig, int center, int subcenter, int version, int paramNo,
                                 int levelType, boolean isLayer, int intvType, String intvName) {
    Formatter f = new Formatter();

    Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo); // code table 2
    if (param == null) {
      f.format("VAR%d-%d-%d-%d", center, subcenter, version, paramNo);
    } else {
      if (param.useName())
        f.format("%s", param.getName());
      else
        f.format("%s", GribUtils.makeNameFromDescription(param.getDescription()));
    }

    if (gribConfig.useTableVersion) {
      f.format("_TableVersion%d", version);
    }

    if (gribConfig.useCenter) {
      f.format("_Center%d", center);
    }

    if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_%s", cust.getLevelNameShort(levelType)); // code table 3
      if (isLayer) f.format("_layer");
    }

    if (intvType >= 0) {
      GribStatType stat = Grib1WmoTimeType.getStatType(intvType);
      if (stat != null) {
        if (intvName != null) f.format("_%s", intvName);
        f.format("_%s", stat.name());
      } else {
        if (intvName != null) f.format("_%s", intvName);
        f.format("_%d", intvType);
      }
    }

    return f.toString();
  }

  private String makeVariableNameFromTables(int center, int subcenter, int version, int paramNo, int levelType, boolean isLayer, int intvType, String intvName) {
    return makeVariableNameFromTables(cust, config.gribConfig, center, subcenter, version, paramNo, levelType, isLayer, intvType, intvName);
  }

  @Override
  public String makeVariableLongName(GribCollectionImmutable.VariableIndex v) {
    return makeVariableLongName(gribCollection.getCenter(), gribCollection.getSubcenter(), v.getTableVersion(), v.getParameter(),
            v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName(), v.getProbabilityName());
  }

  public String makeVariableLongName(int center, int subcenter, int version, int paramNo, int levelType, boolean isLayer, int intvType,
                                     String intvName, String probabilityName) {
    Formatter f = new Formatter();

    boolean isProb = (probabilityName != null && probabilityName.length() > 0);
    if (isProb)
      f.format("Probability ");

    Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo);
    if (param == null)
      f.format("Unknown Parameter %d-%d-%d-%d", center, subcenter, version, paramNo);
    else
      f.format("%s", param.getDescription());

    if (intvType >= 0) {
      GribStatType stat = Grib1WmoTimeType.getStatType(intvType);
      if (stat != null) f.format(" (%s %s)", intvName, stat.name());
      else if (intvName != null && intvName.length() > 0) f.format(" (%s)", intvName);
    }

    if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format(" @ %s", cust.getLevelDescription(levelType));
      if (isLayer) f.format(" layer");
    }

    return f.toString();
  }

  @Override
  protected String makeVariableUnits(GribCollectionImmutable.VariableIndex vindex) {
    return makeVariableUnits(gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.getTableVersion(), vindex.getParameter());
  }

  public String makeVariableUnits(int center, int subcenter, int version, int paramNo) {
    Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo);
    String val = (param == null) ? "" : param.getUnit();
    return (val == null) ? "" : val;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Grib1Customizer cust;

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    if (raf instanceof HTTPRandomAccessFile) { // only do remote if memory resident
      if (raf.length() > raf.getBufferSize())
        return false;

    } else {                                  // wont accept remote index
      GribCdmIndex.GribCollectionType type = GribCdmIndex.getType(raf);
      if (type == GribCdmIndex.GribCollectionType.GRIB1) return true;
      if (type == GribCdmIndex.GribCollectionType.Partition1) return true;
    }

    // check for GRIB1 data file
    return Grib1RecordScanner.isValidFile(raf);
  }

  @Override
  public String getFileTypeId() {
    return DataFormatType.GRIB1.getDescription();
  }

  @Override
  public String getFileTypeDescription() {
    return "GRIB1 Collection";
  }

  // public no-arg constructor for reflection
  public Grib1Iosp() {
    super(true, logger);
  }

  public Grib1Iosp(GribCollectionImmutable.GroupGC gHcs, GribCollectionImmutable.Type gtype) {
    super(true, logger);
    this.gHcs = gHcs;
    this.owned = true;
    this.gtype = gtype;
  }

  public Grib1Iosp(GribCollectionImmutable gc) {
    super(true, logger);
    this.gribCollection = gc;
    this.owned = true;
  }

  @Override
  protected ucar.nc2.grib.GribTables createCustomizer() throws IOException {
    Grib1ParamTables tables = (config.gribConfig.paramTable != null) ? Grib1ParamTables.factory(config.gribConfig.paramTable) :
            Grib1ParamTables.factory(config.gribConfig.paramTablePath, config.gribConfig.lookupTablePath); // so an iosp message must be received before the open()

    cust = Grib1Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), tables);
    return cust;
  }

  @Override
  protected String getVerticalCoordDesc(int vc_code) {
    return cust.getLevelDescription(vc_code);
  }

  @Override
  protected GribTables.Parameter getParameter(GribCollectionImmutable.VariableIndex vindex) {
    return cust.getParameter(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getVersion(), vindex.getParameter());
  }

  @Override
  protected void addVariableAttributes(Variable v, GribCollectionImmutable.VariableIndex vindex) {

    // Grib attributes
    v.addAttribute(new Attribute(VARIABLE_ID_ATTNAME, makeVariableNameFromRecord(vindex)));
    v.addAttribute(new Attribute("Grib1_Center", gribCollection.getCenter()));
    v.addAttribute(new Attribute("Grib1_Subcenter", gribCollection.getSubcenter()));
    v.addAttribute(new Attribute("Grib1_TableVersion", vindex.getTableVersion()));
    v.addAttribute(new Attribute("Grib1_Parameter", vindex.getParameter()));
    Grib1Parameter param = cust.getParameter(gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.getTableVersion(), vindex.getParameter());
    if (param != null && param.getName() != null)
      v.addAttribute(new Attribute("Grib1_Parameter_Name", param.getName()));

    if (vindex.getLevelType() != GribNumbers.MISSING)
      v.addAttribute(new Attribute("Grib1_Level_Type", vindex.getLevelType()));
    String ldesc = cust.getLevelDescription(vindex.getLevelType());
    if (ldesc != null)
      v.addAttribute(new Attribute("Grib1_Level_Desc", ldesc));


    String timeTypeName = cust.getTimeTypeName(vindex.getIntvType());
    if ( timeTypeName != null && timeTypeName.length() != 0) {
      v.addAttribute(new Attribute("Grib1_Interval_Type", vindex.getIntvType()));
      v.addAttribute(new Attribute("Grib1_Interval_Name", timeTypeName));
    }

    if (vindex.getEnsDerivedType() >= 0)
      v.addAttribute(new Attribute("Grib1_Ensemble_Derived_Type", vindex.getEnsDerivedType()));
    else if (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0)
      v.addAttribute(new Attribute("Grib1_Probability_Type", vindex.getProbabilityName()));
  }

  @Override
  protected void show(RandomAccessFile rafData, long dataPos) throws IOException {
    rafData.seek(dataPos);
    Grib1Record gr = new Grib1Record(rafData);
    Formatter f = new Formatter();
    f.format("File=%s%n", rafData.getLocation());
    Grib1SectionProductDefinition pds = gr.getPDSsection();
    Grib1Parameter param = cust.getParameter(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber());
    f.format("  Parameter=%s%n", param);
    f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
    Grib1ParamTime ptime = gr.getParamTime(cust);
    f.format("  ForecastTime=%d%n", ptime.getForecastTime());
    if (ptime.isInterval()) {
      int tinv[] = ptime.getInterval();
      f.format("  TimeInterval=(%d,%d)%n", tinv[0], tinv[1]);
    }
    f.format("%n");
    gr.getPDSsection().showPds(cust, f);
    System.out.printf("%nGrib1Record.readData at drsPos %d = %s%n", dataPos, f.toString());
  }


  @Override
  protected float[] readData(RandomAccessFile rafData, DataRecord dr) throws IOException {
    return Grib1Record.readData(rafData, dr.dataPos);
  }

  public Object getLastRecordRead() {
    return Grib1Record.lastRecordRead;
  }

  public void clearLastRecordRead() {
    Grib1Record.lastRecordRead = null;
  }


  public Object getGribCustomizer() {
    return cust;
  }

  public static void main(String[] args) {
      int pno = 121;
      int result = 823375026;
      result += result * 37 + 1;  // 1223479917
      result += result * 37 + pno;  // 1223479917

      int result2 = 823375026;  // 1223479917
      result2 += result2 * 37 + 4;
      result2 += result2 * 37 + pno;

      System.out.printf("%d,%d%n", result, result2);

      // Arrays.hashCode(new Object[] {1, 2, 3});
    }

}
