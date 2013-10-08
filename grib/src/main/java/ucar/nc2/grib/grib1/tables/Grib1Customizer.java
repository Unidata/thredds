/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1.tables;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import thredds.featurecollection.TimeUnitConverter;
import ucar.grib.GribNumbers;
import ucar.grib.GribResourceReader;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;

/**
 * Interprets grib1 info in a way that may be customized.
 * This class handles the default case, using only standard WMO tables.
 * Subclasses override as needed.
 *
 * Bit of a contradiction, since getParamter() allows different center, subcenter, version (the version is for sure needed)
 * But other tables are fixed by  center.
 *
 * @author caron
 * @since 1/13/12
 */
public class Grib1Customizer implements GribTables {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1Customizer.class);

  static public Grib1Customizer factory(Grib1Record proto, Grib1ParamTables tables) {
    int center = proto.getPDSsection().getCenter();
    int subcenter = proto.getPDSsection().getSubCenter();
    int version = proto.getPDSsection().getTableVersion();
    return factory(center, subcenter, version, tables);
  }

  static public Grib1Customizer factory(int center, int subcenter, int version, Grib1ParamTables tables) {
    if (center == 7) return new NcepTables(tables);
    else if (center == 9) return new NcepRfcTables(tables);
    else if (center == 57) return new AfwaTables(tables);
    else if (center == 58) return new FnmocTables(tables);
    else return new Grib1Customizer(center, tables);
  }

  static public String getSubCenterName(int center, int subcenter) {
    Grib1Customizer cust = Grib1Customizer.factory(center, subcenter, 0, null);
    return cust.getSubCenterName( subcenter);
  }

  ///////////////////////////////////////
  private int center;
  private Grib1ParamTables tables;

  protected Grib1Customizer(int center, Grib1ParamTables tables) {
    this.center = center;
    this.tables = (tables == null) ? new Grib1ParamTables() : tables;
  }

  public int getCenter() {
    return center;
  }

  public Grib1Parameter getParameter(int center, int subcenter, int tableVersion, int param_number) {
    return tables.getParameter(center, subcenter, tableVersion, param_number);
  }

  public String getGeneratingProcessName(int genProcess) {
    return null;
  }

  public String getSubCenterName(int subcenter) {
    return CommonCodeTable.getSubCenterName(center, subcenter);
  }

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

  public String makeVariableName(GribCollection gribCollection, GribCollection.VariableIndex vindex) {
    return makeVariableNameFromTables(gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.tableVersion, vindex.parameter,
            vindex.levelType, vindex.isLayer, vindex.intvType, vindex.intvName);
  }

  public String makeVariableName(Grib1SectionProductDefinition pds) {
    return makeVariableNameFromTables(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber(),
             pds.getLevelType(), isLayer(pds.getLevelType()), pds.getTimeRangeIndicator(), null);
  }

  public String makeVariableNameFromRecord(GribCollection gribCollection, GribCollection.VariableIndex vindex) {
    return makeVariableNameFromRecord(gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.tableVersion, vindex.parameter,
            vindex.levelType, vindex.isLayer, vindex.intvType, vindex.intvName);
  }

  private String makeVariableNameFromRecord(int center, int subcenter, int tableVersion, int paramNo,
                                 int levelType, boolean isLayer, int intvType, String intvName) {
    Formatter f = new Formatter();

    f.format("VAR_%d-%d-%d-%d", center, subcenter, tableVersion, paramNo);

    if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_L%d", levelType); // code table 4.5
      if (isLayer) f.format("_layer");
    }

    if (intvType >= 0) {
      if (intvName != null) {
        if (intvName.equals("Mixed_intervals"))
          f.format("_Imixed");
        else
          f.format("_I%s", intvName);
      }
      f.format("_S%s", intvType);
    }

    return f.toString();
  }

  private String makeVariableNameFromTables(int center, int subcenter, int version, int paramNo,
                                 int levelType, boolean isLayer, int intvType, String intvName) {
    Formatter f = new Formatter();

    Grib1Parameter param = getParameter(center, subcenter, version, paramNo); // code table 2
    if (param == null) {
      f.format("VAR%d-%d-%d-%d", center, subcenter, version, paramNo);
    } else {
      if (param.useName())
        f.format("%s", param.getName());
      else
        f.format("%s", GribUtils.makeNameFromDescription(param.getDescription()));
    }

    if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_%s", getLevelNameShort(levelType)); // code table 3
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

  public String makeVariableLongName(int center, int subcenter, int version, int paramNo,
                                     int levelType, int intvType, String intvName, boolean isLayer, String probabilityName) {
    Formatter f = new Formatter();

    boolean isProb = (probabilityName != null && probabilityName.length() > 0);
    if (isProb)
      f.format("Probability ");

    Grib1Parameter param = getParameter(center, subcenter, version, paramNo);
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
      f.format(" @ %s", getLevelDescription(levelType));
      if (isLayer) f.format(" layer");
    }

    return f.toString();
  }

  public String makeVariableUnits(int center, int subcenter, int version, int paramNo) {
    Grib1Parameter param = getParameter(center, subcenter, version, paramNo);
    String val = (param == null) ? "" : param.getUnit();
    return (val == null) ? "" : val;
  }

  ///////////////////////////////////////////////////
  // time

  public Grib1ParamTime getParamTime(Grib1SectionProductDefinition pds) {
    return new Grib1ParamTime(this, pds);
  }

  // code table 5
  public String getTimeTypeName(int timeRangeIndicator) {
    return Grib1WmoTimeType.getTimeTypeName(timeRangeIndicator);
  }

  public GribStatType getStatType(int timeRangeIndicator) {
    return Grib1WmoTimeType.getStatType(timeRangeIndicator);
  }

  /////////////////////////////////////////
  // level

  public Grib1ParamLevel getParamLevel(Grib1SectionProductDefinition pds) {
    return new Grib1ParamLevel(this, pds);
  }

  public VertCoord.VertUnit getVertUnit(int code) {
    return makeVertUnit(code);
  }

  public boolean is3D(int levelType) {
    return getLevelUnits(levelType) != null;
  }

  // below are the methods a subclass may need to override for levels

  protected VertCoord.VertUnit makeVertUnit(int code) {
    return getLevelType(code);
  }

  @Override
  public String getLevelNameShort(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    String result = (lt == null) ? null : lt.getAbbrev();
    if (result == null) result = "unknownLevel"+levelType;
    return result;
  }

  public String getLevelDescription(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    return (lt == null) ? null : lt.getDesc();
  }

  public boolean isLayer(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    return (lt == null) ? false : lt.isLayer();
  }

  // only for 3D
  public boolean isPositiveUp(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    return (lt == null) ? false : lt.isPositiveUp();
  }

  // only for 3D
  public String getLevelUnits(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    return (lt == null) ? null : lt.getUnits();
  }

  // only for 3D
  public String getLevelDatum(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    return (lt == null) ? null : lt.getDatum();
  }

  /////////////////////////////////////////////
  private TimeUnitConverter timeUnitConverter;

  public void setTimeUnitConverter(TimeUnitConverter timeUnitConverter) {
    this.timeUnitConverter = timeUnitConverter;
  }

  public int convertTimeUnit(int timeUnit) {
    if (timeUnitConverter == null) return timeUnit;
    return timeUnitConverter.convertTimeUnit(timeUnit);
  }

  ////////////////////////////////////////////////////////////////////////

  private HashMap<Integer, GribLevelType> wmoTable3;

  private GribLevelType getLevelType(int code) {
    if (wmoTable3 == null)
      wmoTable3 = readTable3("resources/grib1/wmoTable3.xml");
    if (wmoTable3 == null)
      return null; // fail

    GribLevelType result = wmoTable3.get(code);
    if (result == null)
      result = new GribLevelType(code, "unknown code "+code, null, false);
    return result;
  }

  protected HashMap<Integer, GribLevelType> readTable3(String path) {
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) {
        logger.error("Cant find Table 3 = " + path);
        return null;
      }

      SAXBuilder builder = new SAXBuilder();
      org.jdom2.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, GribLevelType> result = new HashMap<Integer, GribLevelType>(200);
      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        String abbrev = elem1.getChildText("abbrev");
        String units = elem1.getChildText("units");
        String datum = elem1.getChildText("datum");
        boolean isLayer = elem1.getChild("isLayer") != null;
        boolean isPositiveUp = elem1.getChild("isPositiveUp") != null;
        GribLevelType lt = new GribLevelType(code, desc, abbrev, units, datum, isPositiveUp, isLayer);
        result.put(code, lt);
      }

      return result;  // all at once - thread safe

    } catch (IOException ioe) {
      logger.error("Cant read NcepLevelTypes = " + path, ioe);
      return null;

    } catch (JDOMException e) {
      logger.error("Cant parse NcepLevelTypes = " + path, e);
      return null;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  public static void main(String[] args) {
    Grib1Customizer cust = new Grib1Customizer(0, null);
    String units = cust.getLevelUnits(110);
    assert units != null;
  }
}
