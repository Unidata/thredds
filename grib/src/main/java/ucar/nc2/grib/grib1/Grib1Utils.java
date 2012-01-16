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


import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.grib1.tables.Grib1TimeTypeTable;

import java.util.Formatter;

/**
 * static utilities for Grib-1
 *
 * @author caron
 * @since 8/30/11
 */
public class Grib1Utils {

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
 */
  static public String makeVariableName(Grib1Customizer cust, int center, int subcenter, int version, int paramNo,
                                        int levelType, int intvType, String intvName) {
    Formatter f = new Formatter();

    Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo);
    if (param == null) {
      f.format("VAR%d-%d-%d-%d", center, subcenter, version, paramNo);
    } else {
      if (param.useName())
        f.format("%s", param.getName());
      else
        f.format("%s", GribUtils.makeNameFromDescription(param.getDescription()));
    }

    if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_%s", cust.getLevelNameShort(levelType)); // code table 3
      // if (vindex.isLayer) f.format("_layer"); LOOK ? assumes that cant have two variables on same vertical type, differing only by isLayer
    }

    if (intvType >= 0) {
      Grib1TimeTypeTable.StatType stype = Grib1TimeTypeTable.getStatType(intvType);
      if (stype != null) {
        if (intvName != null) f.format("_%s", intvName);
        f.format("_%s", stype.name());
      }
    }

    return f.toString();
  }

  static public String makeVariableLongName(Grib1Customizer cust, int center, int subcenter, int version, int paramNo,
                                            int levelType, int intvType, String intvName, boolean isLayer, String probabilityName) {
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
      Grib1TimeTypeTable.StatType stat = Grib1TimeTypeTable.getStatType(intvType);
      if (stat != null) f.format(" (%s %s)", intvName, stat.name());
      else if (intvName != null) f.format(" (%s)", intvName);
    }

    if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format(" @ %s", cust.getLevelNameShort(levelType));
      if (isLayer) f.format(" layer");
    }

    return f.toString();
  }

  static public String makeVariableUnits(Grib1Customizer cust, int center, int subcenter, int version, int paramNo) {
    Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo);
    String val = (param == null) ? "" : param.getUnit();
    return (val == null) ? "" : val;
  }

  /* Grib1
    Code table 4 Unit of time
    Code figure Meaning
    0 Minute
    1 Hour
    2 Day
    3 Month
    4 Year
    5 Decade (10 years)
    6 Normal (30 years)
    7 Century (100 years)
    8-9 Reserved
    10 3 hours
    11 6 hours
    12 12 hours
    13 Quarter of an hour
    14 Half an hour
    15-253 Reserved
   */

  static public String getVariableName(Grib1Record record) {
    Grib1SectionProductDefinition pds = record.getPDSsection();
    return pds.getCenter()+"-"+pds.getSubCenter()+"-"+pds.getTableVersion()+"-"+pds.getParameterNumber();
  }

}
