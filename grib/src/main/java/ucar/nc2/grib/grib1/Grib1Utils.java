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
import ucar.nc2.grib.grib1.tables.Grib1Parameter;
import ucar.nc2.grib.grib1.tables.Grib1Tables;
import ucar.nc2.wmo.CommonCodeTable;

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
  static public String makeVariableName(Grib1Tables tables, int center, int subcenter, int version, int paramNo,
                                        int levelType, int intvType) {
    Formatter f = new Formatter();

    Grib1Parameter param = tables.getParameter(center, subcenter, version, paramNo);
    if (param == null) {
      f.format("VAR%d-%d-%d-%d", center, subcenter, version, paramNo);
    } else {
      if (param.useName())
        f.format("%s", param.getName());
      else
        f.format("%s", GribUtils.makeNameFromDescription(param.getDescription()));
    }

    if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_%s", tables.getLevelNameShort(levelType)); // code table 3
      // if (vindex.isLayer) f.format("_layer"); LOOK ? assumes that cant have two variables on same vertical type, differing only by isLayer
    }

    if (intvType >= 0) {
      Grib1ParamTime.StatType stype = Grib1ParamTime.getStatType(intvType);
      if (stype != null)
        f.format("_%s", stype.name());
    }

    return f.toString();
  }

  static public String makeVariableLongName(Grib1Tables tables, int center, int subcenter, int version, int paramNo,
                                            int levelType, int intvType, boolean isLayer, String probabilityName) {
    Formatter f = new Formatter();

    boolean isProb = (probabilityName != null && probabilityName.length() > 0);
    if (isProb)
      f.format("Probability ");

    Grib1Parameter param = tables.getParameter(center, subcenter, version, paramNo);
    if (param == null)
      f.format("Unknown Parameter %d-%d-%d-%d", center, subcenter, version, paramNo);
    else
      f.format("%s", param.getDescription());

    if (intvType >= 0) {
      Grib1ParamTime.StatType stat = Grib1ParamTime.getStatType(intvType);
      if (stat != null) f.format(" (%s)", stat.name());
    }

    if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format(" @ %s", tables.getLevelNameShort(levelType));
      if (isLayer) f.format(" layer");
    }

    return f.toString();
  }

  static public String makeVariableUnits(Grib1Tables tables, int center, int subcenter, int version, int paramNo) {
    Grib1Parameter param = tables.getParameter(center, subcenter, version, paramNo);
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

  /**
   * Currently only defined for center 7 NCEP
   *
   * @param center     center id
   * @param genProcess generating process id (pds octet 6)
   * @return generating process name, or null if unknown
   */
  public static final String getTypeGenProcessName(int center, int genProcess) {
    if (center == 7) {
      switch (genProcess) {

        case 2:
          return "Ultra Violet Index Model";

        case 3:
          return "NCEP/ARL Transport and Dispersion Model";

        case 4:
          return "NCEP/ARL Smoke Model";

        case 5:
          return "Satellite Derived Precipitation and temperatures, from IR";

        case 6:
          return "NCEP/ARL Dust Model";

        case 10:
          return "Global Wind-Wave Forecast Model";

        case 11:
          return "Global Multi-Grid Wave Model";

        case 12:
          return "Probabilistic Storm Surge";

        case 19:
          return "Limited-area Fine Mesh (LFM) analysis";

        case 25:
          return "Snow Cover Analysis";

        case 30:
          return "Forecaster generated field";

        case 31:
          return "Value added post processed field";

        case 39:
          return "Nested Grid forecast Model (NGM)";

        case 42:
          return "Global Optimum Interpolation Analysis (GOI) from GFS model";

        case 43:
          return "Global Optimum Interpolation Analysis (GOI) from Final run";

        case 44:
          return "Sea Surface Temperature Analysis";

        case 45:
          return "Coastal Ocean Circulation Model";

        case 46:
          return "HYCOM - Global";

        case 47:
          return "HYCOM - North Pacific basin";

        case 48:
          return "HYCOM - North Atlantic basin";

        case 49:
          return "Ozone Analysis from TIROS Observations";

        case 52:
          return "Ozone Analysis from Nimbus 7 Observations";

        case 53:
          return "LFM-Fourth Order Forecast Model";

        case 64:
          return "Regional Optimum Interpolation Analysis (ROI)";

        case 68:
          return "80 wave triangular, 18-layer Spectral model from GFS model";

        case 69:
          return "80 wave triangular, 18 layer Spectral model from Medium Range Forecast run";

        case 70:
          return "Quasi-Lagrangian Hurricane Model (QLM)";

        case 73:
          return "Fog Forecast model - Ocean Prod. Center";

        case 74:
          return "Gulf of Mexico Wind/Wave";

        case 75:
          return "Gulf of Alaska Wind/Wave";

        case 76:
          return "Bias corrected Medium Range Forecast";

        case 77:
          return "126 wave triangular, 28 layer Spectral model from GFS model";

        case 78:
          return "126 wave triangular, 28 layer Spectral model from Medium Range Forecast run";

        case 79:
          return "Backup from the previous run";

        case 80:
          return "62 wave triangular, 28 layer Spectral model from Medium Range Forecast run";

        case 81:
          return "Analysis from GFS";

        case 82:
          return "Analysis from Global Data Assimilation System";

        case 84:
          return "MESO NAM Model";

        case 86:
          return "RUC Model, from Forecast Systems Lab (isentropic; scale: 60km at 40N)";

        case 87:
          return "CAC Ensemble Forecasts from Spectral (ENSMB)";

        case 88:
          return "NOAA Wave Watch III (NWW3) Ocean Wave Model";

        case 89:
          return "Non-hydrostatic Meso Model (NMM)";

        case 90:
          return "62 wave triangular, 28 layer spectral model extension of the Medium Range Forecast run";

        case 91:
          return "62 wave triangular, 28 layer spectral model extension of the GFS model";

        case 92:
          return "62 wave triangular, 28 layer spectral model run from the Medium Range Forecast final analysis";

        case 93:
          return "62 wave triangular, 28 layer spectral model run from the T62 GDAS analysis of the Medium Range Forecast run";

        case 94:
          return "T170/L42 Global Spectral Model from MRF run";

        case 95:
          return "T126/L42 Global Spectral Model from MRF run";

        case 96:
          return "Global Forecast System Model (formerly known as the Aviation)";

        case 98:
          return "Climate Forecast System Model -- Atmospheric model (GFS) coupled to a multi level ocean model.";

        case 99:
          return "Miscellaneous Test ID";

        case 100:
          return "RUC Surface Analysis (scale: 60km at 40N)";

        case 101:
          return "RUC Surface Analysis (scale: 40km at 40N)";

        case 105:
          return "RUC Model from FSL (isentropic; scale: 20km at 40N)";

        case 107:
          return "Global Ensemble Forecast System";

        case 108:
          return "LAMP";

        case 109:
          return "RTMA (Real Time Mesoscale Analysis)";

        case 110:
          return "NAM Model - 15km version";

        case 111:
          return "NAM model, generic resolution (Used in SREF processing)";

        case 112:
          return "WRF-NMM model, generic resolution NMM=Nondydrostatic Mesoscale Model (NCEP)";

        case 113:
          return "Products from NCEP SREF processing";

        case 114:
          return "NAEFS Products from joined NCEP, CMC global ensembles";

        case 115:
          return "Downscaled GFS from NAM eXtension";

        case 116:
          return "WRF-EM model, generic resolution EM - Eulerian Mass-core (NCAR - aka Advanced Research WRF)";

        case 120:
          return "Ice Concentration Analysis";

        case 121:
          return "Western North Atlantic Regional Wave Model";

        case 122:
          return "Alaska Waters Regional Wave Model";

        case 123:
          return "North Atlantic Hurricane Wave Model";

        case 124:
          return "Eastern North Pacific Regional Wave Model";

        case 125:
          return "North Pacific Hurricane Wave Model";

        case 126:
          return "Sea Ice Forecast Model";

        case 127:
          return "Lake Ice Forecast Model";

        case 128:
          return "Global Ocean Forecast Model";

        case 129:
          return "Global Ocean Data Analysis System (GODAS)";

        case 130:
          return "Merge of fields from the RUC, NAM, and Spectral Model";

        case 131:
          return "Great Lakes Wave Model";

        case 140:
          return "North American Regional Reanalysis (NARR)";

        case 141:
          return "Land Data Assimilation and Forecast System";

        case 150:
          return "NWS River Forecast System (NWSRFS)";

        case 151:
          return "NWS Flash Flood Guidance System (NWSFFGS)";

        case 152:
          return "WSR-88D Stage II Precipitation Analysis";

        case 153:
          return "WSR-88D Stage III Precipitation Analysis";

        case 180:
          return "Quantitative Precipitation Forecast generated by NCEP";

        case 181:
          return "River Forecast Center Quantitative Precipitation Forecast mosaic generated by NCEP";

        case 182:
          return "River Forecast Center Quantitative Precipitation estimate mosaic generated by NCEP";

        case 183:
          return "NDFD product generated by NCEP/HPC";

        case 184:
          return "Climatological Calibrated Precipitation Analysis - CCPA";

        case 190:
          return "National Convective Weather Diagnostic generated by NCEP/AWC";

        case 191:
          return "Current Icing Potential automated product generated by NCEP/AWC";

        case 192:
          return "Analysis product from NCEP/AWC";

        case 193:
          return "Forecast product from NCEP/AWC";

        case 195:
          return "Climate Data Assimilation System 2 (CDAS2)";

        case 196:
          return "Climate Data Assimilation System 2 (CDAS2) - used for regeneration runs";

        case 197:
          return "Climate Data Assimilation System (CDAS)";

        case 198:
          return "Climate Data Assimilation System (CDAS) - used for regeneration runs";

        case 199:
          return "Climate Forecast System Reanalysis (CFSR)";

        case 200:
          return "CPC Manual Forecast Product";

        case 201:
          return "CPC Automated Product";

        case 210:
          return "EPA Air Quality Forecast - Currently North East US domain";

        case 211:
          return "EPA Air Quality Forecast - Currently Eastern US domain";

        case 215:
          return "SPC Manual Forecast Product";

        case 220:
          return "NCEP/OPC automated product";

        case 255:
          return "Missing";

        default:
          //return "Unknown "+ Integer.toString( model );
          return null;
      }
    }

    // TABLE A - GENERATING PROCESS OR MODEL - ORIGINATING CENTER 9
    //        * from John Halquist <John.Halquist@noaa.gov> 9/12/2011
    if (center == 9) {
      switch (genProcess) {
        case 150:
          return "NWS River Forecast System (NWSRFS)";
        case 151:
          return "NWS Flash Flood Guidance System (NWSFFGS)";
        case 152:
          return "Quantitative Precipitation Estimation (QPE) - 1 hr dur";
        case 154:
          return "Quantitative Precipitation Estimation (QPE) - 6 hr dur";
        case 155:
          return "Quantitative Precipitation Estimation (QPE) - 24hr dur";
        case 156:
          return "Process 1 (P1) Precipitation Estimation - automatic";
        case 157:
          return "Process 1 (P1) Precipitation Estimation - manual";
        case 158:
          return "Process 2 (P2) Precipitation Estimation - automatic";
        case 159:
          return "Process 2 (P2) Precipitation Estimation - manual";
        case 160:
          return "Multisensor Precipitation Estimation (MPE) - automatic";
        case 161:
          return "Multisensor Precipitation Estimation (MPE) - manual";
        case 165:
          return "Enhanced MPE - automatic";
        case 166:
          return "Bias Enhanced MPE - automatic";
        case 170:
          return "Post Analysis of Precipitation Estimation (aggregate)";
        case 171:
          return "XNAV Aggregate Precipitation Estimation";
        case 172:
          return "Mountain Mapper Precipitation Estimation";
        case 180:
          return "Quantitative Precipitation Forecast (QPF)";
        case 185:
          return "NOHRSC_OPPS";
        case 190:
          return "Satellite Autoestimator Precipitation";
        case 191:
          return "Satellite Interactive Flash Flood Analyzer (IFFA)";
      }
    }

    return null;
  }

  /* TABLE C - SUB-CENTERS FOR CENTER 9  US NWS FIELD STATIONS
  * from John Halquist <John.Halquist@noaa.gov> 9/12/2011
  */
  public static final String getSubCenterName(int center, int subcenter) {
    if (center == 9) {
      switch (subcenter) {
        case 150:
          return "KTUA: Arkansas-Red River RFC Tulsa OK";
        case 151:
          return "PACR: Alaska-Pacific RFC Anchorage AK";
        case 152:
          return "KSTR: return Colorado Basin RFC Salt Lake City UT";
        case 153:
          return "KRSA: California-Nevada RFC Sacramento CA";
        case 154:
          return "KORN: Lower Mississippi RFC Slidel LA";
        case 155:
          return "KRHA: Middle Atlantic RFC State College PA";
        case 156:
          return "KKRF: Missouri Basin RFC Pleasant Hill MO";
        case 157:
          return "KMSR: North Central RFC  Chanhassen MN";
        case 158:
          return "KTAR: Northeast RFC Taunton MA";
        case 159:
          return "KPTR: Northwest RFC Portland OR";
        case 160:
          return "KTIR: Ohio Basin RFC Wilmington OH";
        case 161:
          return "KALR: Southeast RFC Peachtree GA";
        case 162:
          return "KFWR: West Gulf RFC Fort Worth TX";
        case 163:
          return "NOHR: Chanhassen MN";
        case 170:
          return "KNES: Satellite Analysis Branch";
        case 200:
          return "KOHD: Office of Hydrologic Development";
      }
    }

    return CommonCodeTable.getSubCenterName(center, subcenter);
  }

  static public String getVariableName(Grib1Record record) {
    Grib1SectionProductDefinition pds = record.getPDSsection();
    return pds.getCenter()+"-"+pds.getSubCenter()+"-"+pds.getTableVersion()+"-"+pds.getParameterNumber();
  }

}
