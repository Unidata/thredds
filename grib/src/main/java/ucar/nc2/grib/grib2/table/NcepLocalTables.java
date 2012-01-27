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

package ucar.nc2.grib.grib2.table;

import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;

import java.util.*;

/**
 * NCEP local tables
 *
 * @author caron
 * @since 4/3/11
 */
public class NcepLocalTables extends Grib2Customizer {
  // final Map<Integer, Grib2Tables.TableEntry> local;

  NcepLocalTables(int center, int subCenter, int masterVersion, int localVersion) {
    super(center, subCenter, masterVersion, localVersion);
    initCodes();
  }

  /* @Override
  public List getParameters() {
    List<TableEntry> result = new ArrayList<TableEntry>();
    for (TableEntry p : local.values()) result.add(p);
    Collections.sort(result);
    return result;
  } */

  @Override
  public String getVariableName(int discipline, int category, int parameter) {
    if ((category <= 191) && (parameter <= 191))
      return super.getVariableName(discipline, category, parameter);

    GribTables.Parameter te = getParameter(discipline, category, parameter);
    if (te == null)
      return super.getVariableName(discipline, category, parameter);
    else
      return te.getName();
  }

  @Override
  public GribTables.Parameter getParameter(int discipline, int category, int number) {
    if ((category <= 191) && (number <= 191)) {
      GribTables.Parameter p = WmoCodeTable.getParameterEntry(discipline, category, number);
      if (p != null) return p; // allow ncep to use values not already in use by WMO (!)
    }

    /* email from boi.vuong@noaa.gov 1/19/2012
     "I find that the parameter 2-4-3 (Haines Index) now is parameter 2 in WMO version 8.
      The NAM fire weather nested  will take change in next implementation of cnvgrib (NCEP conversion program)."  */
    if (makeHash(discipline, category, number) == makeHash(2,4,3))
      return getParameter(2,4,2);

    /* email from boi.vuong@noaa.gov 1/26/2012
     The parameter 0-19-242 (Relative Humidity with Respect to Precipitable Water)  was in http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-1.shtml
     It was a mistake in table conversion (from grib1 to grib2) in cnvgrib. It will be fixed in next implementation of cnvgrib in June or July, 2012.
     RHPW  in grib1 in table 129 parameter 230  and in grib2 in 0-1-242  */
    if (makeHash(discipline, category, number) == makeHash(0,19,242))
      return getParameter(0,1,242);

    return NcepLocalParams.getParameter(discipline, category, number);
  }

  @Override
  public String getTableValue(String tableName, int code) {
    if (tableName.equals("ProcessId")) {
      if (processIdMap == null) initProcessIdMap();
      return processIdMap.get(code);
    }

    if ((code < 192) || (code > 254) || tableName.equals("4.0"))
      return WmoCodeTable.getTableValue(tableName, code);

    // return NcepCodeTable.getTableValueFromCurrent(tableName, code);
    return codeMap.get(tableName + "." + code);

  }

  /* @Override
  public TimeCoord.TinvDate getForecastTimeInterval(Grib2Record gr) {
    if (!gr.getPDS().isInterval()) return null;
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) gr.getPDS();
    Grib2Pds.TimeInterval[] ti = pdsIntv.getTimeIntervals();
    if (ti.length == 1) return super.getForecastTimeInterval(gr);

    // LOOK this is hack for CFSR monthly combobulation
    Grib2Pds.TimeInterval tiUse = ti[0];
    int[] result = new int[2];
    result[0] = pdsIntv.getForecastTime();
    result[1] = pdsIntv.getForecastTime() + tiUse.timeRangeLength * tiUse.timeIncrement; // kludge
    return new TimeCoord.Tinv(result[0], result[1]);
  }  */

  @Override
  public String getLevelNameShort(int id) {
    if (id < 192) return super.getLevelNameShort(id);
    switch (id) {
      case 200:
        return "Entire_atmosphere";
      case 201:
        return "Entire_ocean";
      case 204:
        return "Highest_tropospheric_freezing";
      case 206:
        return "Grid_scale_cloud_bottom";
      case 207:
        return "Grid_scale_cloud_top";
      case 209:
        return "Boundary_layer_cloud_bottom";
      case 210:
        return "Boundary_layer_cloud_top";
      case 211:
        return "Boundary_layer_cloud";
      case 212:
        return "Low_cloud_bottom";
      case 213:
        return "Low_cloud_top";
      case 214:
        return "Low_cloud";
      case 215:
        return "Cloud_ceiling";
      case 220:
        return "Planetary_boundary";
      case 221:
        return "Between_two_hybrids";
      case 222:
        return "Middle_cloud_bottom";
      case 223:
        return "Middle_cloud_top";
      case 224:
        return "Middle_cloud";
      case 232:
        return "High_cloud_bottom";
      case 233:
        return "High_cloud_top";
      case 234:
        return "High_cloud";
      case 235:
        return "Ocean_isotherm";
      case 236:
        return "Layer_between_two_depths_below_ocean";
      case 237:
        return "Bottom_of_ocean_mixed";
      case 238:
        return "Bottom_of_ocean_isothermal";
      case 239:
        return "Ocean_surface_and_26C_isothermal";
      case 240:
        return "Ocean_mixed";
      case 241:
        return "Ordered_sequence_of_data";
      case 242:
        return "Convective_cloud_bottom";
      case 243:
        return "Convective_cloud_top";
      case 244:
        return "Convective_cloud";
      case 245:
        return "Lowest_level_of_the_wet_bulb_zero";
      case 246:
        return "Maximum_equivalent_potential_temperature";
      case 247:
        return "Equilibrium";
      case 248:
        return "Shallow_convective_cloud_bottom";
      case 249:
        return "Shallow_convective_cloud_top";
      case 251:
        return "Deep_convective_cloud_bottom";
      case 252:
        return "Deep_convective_cloud_top";
      case 253:
        return "Lowest_level_water_layer";
      case 254:
        return "Highest_level_water_layer";
      default:
        return super.getLevelNameShort(id);
    }
  }

  @Override
  public String getIntervalNameShort(int id) {
    if (id < 192) return super.getIntervalNameShort(id);
    switch (id) {
      case 192:
        return "ClimatologicalMeanValue";
      case 193:
        return "AverageNforecasts";
      case 194:
        return "AverageNanalysis";
      case 195:
        // Average of forecast accumulations. P1 = start of accumulation period. P2 = end of accumulation period. Reference time is the start time of the
        // first forecast, other forecasts at 24-hour intervals. Number in Ave = number of forecasts used.
        return "AverageAccum-24hourIntv";
      case 196:
        return "AverageForecastSuccessiveAccumulations";
      case 197:
        // Average of forecast averages. P1 = start of averaging period. P2 = end of averaging period. Reference time is the start time of the first forecast,
        // other forecasts at 24-hour intervals. Number in Ave = number of forecast used
        return "AverageAvg-24hourIntv";
      case 198:
        return "AverageForecastSuccessiveAverages";
      case 199:
        return "ClimatologicalAverageNanalysis";
      case 200:
        return "ClimatologicalAverageNforecasts";
      case 201:
        return "ClimatologicalRMSdiffNforecasts";
      case 202:
        return "ClimatologicalStandardDeviationNforecasts";
      case 203:
        return "ClimatologicalStandardDeviationNanalyses";
      case 204:
        return "AverageForecastAccumulations-204";
      case 205:
        return "AverageForecastAverages-205";
      case 206:
        return "AverageForecastAccumulations-206";
      case 207:
        return "AverageForecastAverages-207";
      default:
        return super.getIntervalNameShort(id);
    }
  }

  /////////////////////////////////////////////////////////////////
  // generating process ids for NCEP

  private Map<Integer, String> processIdMap = null;

  private void initProcessIdMap() {
    Map<Integer, String> map = new HashMap<Integer, String>(300);

    /* see: http://www.nco.ncep.noaa.gov/pmb/docs/on388/tablea.html */
    map.put(2, "Ultra Violet Index Model");
    map.put(3, "NCEP/ARL Transport and Dispersion Model");
    map.put(4, "NCEP/ARL Smoke Model");
    map.put(5, "Satellite Derived Precipitation and temperatures, from IR");
    map.put(6, "NCEP/ARL Dust Model");
    map.put(10, "Global Wind-Wave Forecast Model");
    map.put(11, "Global Multi-Grid Wave Model (Static Grids)");
    map.put(12, "Probabilistic Storm Surge");
    map.put(19, "Limited-area Fine Mesh (LFM) analysis");
    map.put(25, "Snow Cover Analysis");
    map.put(30, "Forecaster generated field");
    map.put(31, "Value added post processed field");
    map.put(39, "Nested Grid forecast Model (NGM)");
    map.put(42, "Global Optimum Interpolation Analysis (GOI) from GFS model");
    map.put(43, "Global Optimum Interpolation Analysis (GOI) from 'Final' run");
    map.put(44, "Sea Surface Temperature Analysis");
    map.put(45, "Coastal Ocean Circulation Model");
    map.put(46, "HYCOM - Global");
    map.put(47, "HYCOM - North Pacific basin");
    map.put(48, "HYCOM - North Atlantic basin");
    map.put(49, "Ozone Analysis from TIROS Observations");
    map.put(52, "Ozone Analysis from Nimbus 7 Observations");
    map.put(53, "LFM-Fourth Order Forecast Model");
    map.put(64, "Regional Optimum Interpolation Analysis (ROI)");
    map.put(68, "80 wave triangular, 18-layer Spectral model from GFS model");
    map.put(69, "80 wave triangular, 18 layer Spectral model from 'Medium Range Forecast' run");
    map.put(70, "Quasi-Lagrangian Hurricane Model (QLM)");
    map.put(73, "Fog Forecast model - Ocean Prod. Center");
    map.put(74, "Gulf of Mexico Wind/Wave");
    map.put(75, "Gulf of Alaska Wind/Wave");
    map.put(76, "Bias corrected Medium Range Forecast");
    map.put(77, "126 wave triangular, 28 layer Spectral model from GFS model");
    map.put(78, "126 wave triangular, 28 layer Spectral model from 'Medium Range Forecast' run");
    map.put(79, "Backup from the previous run");
    map.put(80, "62 wave triangular, 28 layer Spectral model from 'Medium Range Forecast' run");
    map.put(81, "Analysis from GFS (Global Forecast System)");
    map.put(82, "Analysis from GDAS (Global Data Assimilation System)");
    map.put(84, "MESO ETA Model (currently 12 km)");
    map.put(86, "RUC Model from FSL (isentropic; scale: 60km at 40N)");
    map.put(87, "CAC Ensemble Forecasts from Spectral (ENSMB)");
    map.put(88, "NOAA Wave Watch III (NWW3) Ocean Wave Model");
    map.put(89, "Non-hydrostatic Meso Model (NMM) Currently 8 km)");
    map.put(90, "62 wave triangular, 28 layer spectral model extension of the 'Medium Range Forecast' run");
    map.put(91, "62 wave triangular, 28 layer spectral model extension of the GFS model");
    map.put(92, "62 wave triangular, 28 layer spectral model run from the 'Medium Range Forecast' final analysis");
    map.put(93, "62 wave triangular, 28 layer spectral model run from the T62 GDAS analysis of the 'Medium Range Forecast' run");
    map.put(94, "T170/L42 Global Spectral Model from MRF run");
    map.put(95, "T126/L42 Global Spectral Model from MRF run");
    map.put(96, "Global Forecast System Model");
    map.put(98, "Climate Forecast System Model");
    map.put(100, "RUC Surface Analysis (scale: 60km at 40N)");
    map.put(101, "RUC Surface Analysis (scale: 40km at 40N)");
    map.put(105, "RUC Model from FSL (isentropic; scale: 20km at 40N)");
    map.put(107, "Global Ensemble Forecast System (GEFS)");
    map.put(108, "LAMP");
    map.put(109, "RTMA (Real Time Mesoscale Analysis)");
    map.put(110, "NAM Model - 15km version");
    map.put(111, "NAM model, generic resolution");
    map.put(112, "WRF-NMM (Nonhydrostatic Mesoscale Model) model, generic resolution");
    map.put(113, "Products from NCEP SREF processing");
    map.put(114, "NAEFS Products from joined NCEP, CMC global ensembles");
    map.put(115, "Downscaled GFS from NAM eXtension");
    map.put(116, "WRF-EM (Eulerian Mass-core) model, generic resolution ");
    map.put(120, "Ice Concentration Analysis");
    map.put(121, "Western North Atlantic Regional Wave Model");
    map.put(122, "Alaska Waters Regional Wave Model");
    map.put(123, "North Atlantic Hurricane Wave Model");
    map.put(124, "Eastern North Pacific Regional Wave Model");
    map.put(125, "North Pacific Hurricane Wave Model");
    map.put(126, "Sea Ice Forecast Model");
    map.put(127, "Lake Ice Forecast Model");
    map.put(128, "Global Ocean Forecast Model");
    map.put(129, "Global Ocean Data Analysis System (GODAS)");
    map.put(130, "Merge of fields from the RUC, NAM, and Spectral Model");
    map.put(131, "Great Lakes Wave Model");
    map.put(140, "North American Regional Reanalysis (NARR)");
    map.put(141, "Land Data Assimilation and Forecast System");
    map.put(150, "NWS River Forecast System (NWSRFS)");
    map.put(151, "NWS Flash Flood Guidance System (NWSFFGS)");
    map.put(152, "WSR-88D Stage II Precipitation Analysis");
    map.put(153, "WSR-88D Stage III Precipitation Analysis");
    map.put(180, "Quantitative Precipitation Forecast");
    map.put(181, "River Forecast Center Quantitative Precipitation Forecast mosaic");
    map.put(182, "River Forecast Center Quantitative Precipitation estimate mosaic");
    map.put(183, "NDFD product generated by NCEP/HPC");
    map.put(184, "Climatological Calibrated Precipitation Analysis - CCPA");
    map.put(190, "National Convective Weather Diagnostic");
    map.put(191, "Current Icing Potential automated product");
    map.put(192, "Analysis product from NCEP/AWC");
    map.put(193, "Forecast product from NCEP/AWC");
    map.put(195, "Climate Data Assimilation System 2 (CDAS2)");
    map.put(196, "Climate Data Assimilation System 2 (CDAS2)");
    map.put(197, "Climate Data Assimilation System (CDAS)");
    map.put(198, "Climate Data Assimilation System (CDAS)");
    map.put(199, "Climate Forecast System Reanalysis (CFSR)");
    map.put(200, "CPC Manual Forecast Product");
    map.put(201, "CPC Automated Product");
    map.put(210, "EPA Air Quality Forecast");
    map.put(211, "EPA Air Quality Forecast");
    map.put(215, "SPC Manual Forecast Product");
    map.put(220, "NCEP/OPC automated product");

    processIdMap = map;

  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // NCEP local tables taken from degrib code
  // validated against /resources/grib2/tablesOld/grib2StdQuantities.xml

    /* from degrib:
Updated this table last on 12/29/2005
Based on:
http://www.nco.ncep.noaa.gov/pmb/docs/grib2/GRIB2_parmeter_conversion_table.html
Better source is:
http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-1.shtml
http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2.shtml
For abreviations see:
http://www.nco.ncep.noaa.gov/pmb/docs/on388/table2.html

Updated again on 2/14/2006
Updated again on 3/15/2006
Updated again on 3/26/2008
*/

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private Map<String, String> codeMap = new HashMap<String, String>(400);

  // see http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml
  private Map<String, String> initCodes() {
    codeMap.put("3.1.204", "Curvilinear_Orthogonal");

    codeMap.put("4.3.192", "Forecast Confidence Indicator");
    codeMap.put("4.3.193", "Bias Corrected Ensemble Forecast");

    codeMap.put("4.5.200", "Entire atmosphere layer");
    codeMap.put("4.5.201", "Entire ocean layer");
    codeMap.put("4.5.204", "Highest tropospheric freezing level");
    codeMap.put("4.5.206", "Grid scale cloud bottom level");
    codeMap.put("4.5.207", "Grid scale cloud top level");
    codeMap.put("4.5.209", "Boundary layer cloud bottom level");
    codeMap.put("4.5.210", "Boundary layer cloud top level");
    codeMap.put("4.5.211", "Boundary layer cloud layer");
    codeMap.put("4.5.212", "Low cloud bottom level");
    codeMap.put("4.5.213", "Low cloud top level");
    codeMap.put("4.5.214", "Low cloud layer");
    codeMap.put("4.5.215", "Cloud ceiling");
    codeMap.put("4.5.220", "Planetary Boundary Layer");
    codeMap.put("4.5.221", "Layer Between Two Hybrid Levels");
    codeMap.put("4.5.222", "Middle cloud bottom level");
    codeMap.put("4.5.223", "Middle cloud top level");
    codeMap.put("4.5.224", "Middle cloud layer");
    codeMap.put("4.5.232", "High cloud bottom level");
    codeMap.put("4.5.233", "High cloud top level");
    codeMap.put("4.5.234", "High cloud layer");
    codeMap.put("4.5.235", "Ocean isotherm level");
    codeMap.put("4.5.236", "Layer between two depths below ocean surface");
    codeMap.put("4.5.237", "Bottom of ocean mixed layer");
    codeMap.put("4.5.238", "Bottom of ocean isothermal layer");
    codeMap.put("4.5.239", "Layer Ocean Surface and 26C Ocean Isothermal Level");
    codeMap.put("4.5.240", "Ocean Mixed Layer");
    codeMap.put("4.5.241", "Ordered Sequence of Data");
    codeMap.put("4.5.242", "Convective cloud bottom level");
    codeMap.put("4.5.243", "Convective cloud top level");
    codeMap.put("4.5.244", "Convective cloud layer");
    codeMap.put("4.5.245", "Lowest level of the wet bulb zero");
    codeMap.put("4.5.246", "Maximum equivalent potential temperature level");
    codeMap.put("4.5.247", "Equilibrium level");
    codeMap.put("4.5.248", "Shallow convective cloud bottom level");
    codeMap.put("4.5.249", "Shallow convective cloud top level");
    codeMap.put("4.5.251", "Deep convective cloud bottom level");
    codeMap.put("4.5.252", "Deep convective cloud top level");
    codeMap.put("4.5.253", "Lowest bottom level of supercooled liquid water layer");
    codeMap.put("4.5.254", "Highest top level of supercooled liquid water layer");

    codeMap.put("4.6.192", "Perturbed Ensemble Member");

    codeMap.put("4.7.192", "Unweighted Mode of All Members");
    codeMap.put("4.7.193", "Percentile value (10%) of All Members");
    codeMap.put("4.7.194", "Percentile value (50%) of All Members");
    codeMap.put("4.7.195", "Percentile value (90%) of All Members");

    codeMap.put("4.10.192", "Climatological Mean Value");
    codeMap.put("4.10.193", "Average of N forecasts");
    codeMap.put("4.10.194", "Average of N uninitialized analyses");
    codeMap.put("4.10.195", "Average of forecast accumulations");
    codeMap.put("4.10.196", "Average of successive forecast accumulations");
    codeMap.put("4.10.197", "Average of forecast averages");
    codeMap.put("4.10.198", "Average of successive forecast averages");
    codeMap.put("4.10.199", "Climatological Average of N analyses, each a year apart");
    codeMap.put("4.10.200", "Climatological Average of N forecasts, each a year apart");
    codeMap.put("4.10.201", "Climatological Root Mean Square difference between N forecasts and their verifying analyses, each a year apart");
    codeMap.put("4.10.202", "Climatological Standard Deviation of N forecasts from the mean of the same N forecasts, for forecasts one year apart");
    codeMap.put("4.10.203", "Climatological Standard Deviation of N analyses from the mean of the same N analyses, for analyses one year apart");
    codeMap.put("4.10.204", "Average of forecast accumulations");
    codeMap.put("4.10.205", "Average of forecast averages");
    codeMap.put("4.10.206", "Average of forecast accumulations");
    codeMap.put("4.10.207", "Average of forecast averages");

    return codeMap;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static class CompTable {
    int key;
    GribTables.Parameter local;
    GribTables.Parameter org;

    private CompTable(int key, Parameter local, Parameter org) {
      this.key = key;
      this.local = local;
      this.org = org;
    }
  }

  public static void main(String arg[]) {
    Map<Integer, CompTable> map = new HashMap<Integer, CompTable>(500);

    NcepLocalTables tables = new NcepLocalTables(0, 0, 0, 0);
    NcepLocalParamsOld ncepOld = new NcepLocalParamsOld();

    for (int key : ncepOld.local.keySet()) {
      Grib2Customizer.Parameter p = ncepOld.local.get(key);
      map.put(key, new CompTable(key, p, null));
    }

    NcepLocalParamsVeryOld.init();
    Map<String, TableEntry> org = NcepLocalParamsVeryOld.getParamMap();
    for (String skey : org.keySet()) {
      TableEntry p = org.get(skey);
      int key = makeHash(p.discipline, p.category, p.number);
      CompTable ct = map.get(key);
      if (ct == null) {
        map.put(key, new CompTable(key, null, p));
      } else {
        ct.org = p;
      }
    }

    System.out.printf("NcepLocalTables%nNcepLocalParamsOld%n%n");
    ArrayList<Integer> keys = new ArrayList<Integer>();
    for (int key : map.keySet()) keys.add(key);
    Collections.sort(keys);
    for (int key : keys) {
      CompTable ct = map.get(key);
      System.out.printf("%s%n", ct.local);
      System.out.printf("%s%n%n", ct.org);
    }
  }

}
