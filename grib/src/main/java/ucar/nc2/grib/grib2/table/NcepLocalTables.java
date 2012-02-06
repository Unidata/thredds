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

import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib1.tables.NcepTables;
import ucar.nc2.grib.grib2.Grib2Parameter;
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

  NcepLocalTables(int center, int subCenter, int masterVersion, int localVersion) {
    super(center, subCenter, masterVersion, localVersion);
    initCodes();
  }

  // temp for cfsr
  public TimeCoord.TinvDate getForecastTimeIntervalCfsrMonthly(Grib2Record gr) {
    if (!gr.getPDS().isInterval()) return null;
    Grib2Pds pds = gr.getPDS();
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) pds;
    int timeUnitOrg = pds.getTimeUnit();

    /*     Octet(s)	Description
        47	From NCEP Code Table 4.10
        48	Should be ignored
        49	Should be ignored
        50-53	Number of grids used in the average
        54	Should be ignored
        55-58	This is "P2" from the GRIB1 format
        59	From NCEP Code Table 4.10
        60	Should be ignored
        61	Should be ignored
        62-65	This is "P2 minus P1"; P1 and P2 are fields from the GRIB1 format
        66	Should be ignored
        67-70	Should be ignored */

    int statType = pds.getOctet(47);
    int statType2 = pds.getOctet(59);
    int ngrids = GribNumbers.int4(pds.getOctet(50), pds.getOctet(51), pds.getOctet(52), pds.getOctet(53));
    int p2 = GribNumbers.int4(pds.getOctet(55), pds.getOctet(56), pds.getOctet(57), pds.getOctet(58));
    int p2mp1 = GribNumbers.int4(pds.getOctet(62), pds.getOctet(63), pds.getOctet(64), pds.getOctet(65));

    return super.getForecastTimeInterval(gr);
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
  public String getVariableName(int discipline, int category, int parameter) {
    if ((category <= 191) && (parameter <= 191))
      return super.getVariableName(discipline, category, parameter);

    GribTables.Parameter te = getParameter(discipline, category, parameter);
    if (te == null)
      return super.getVariableName(discipline, category, parameter);
    else
      return te.getName();
  }

  public GribTables.Parameter getParameterOld(int discipline, int category, int number) {
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
    if (makeHash(discipline, category, number) == makeHash(0, 19, 242))
      return getParameter(0, 1, 242);

    return NcepLocalParams.getParameter(discipline, category, number);
  }

  @Override
  public GribTables.Parameter getParameter(int discipline, int category, int number) {
    /* email from boi.vuong@noaa.gov 1/19/2012
     "I find that the parameter 2-4-3 (Haines Index) now is parameter 2 in WMO version 8.
      The NAM fire weather nested  will take change in next implementation of cnvgrib (NCEP conversion program)."  */
    if (makeHash(discipline, category, number) == makeHash(2,4,3))
      return getParameter(2,4,2);

    /* email from boi.vuong@noaa.gov 1/26/2012
     The parameter 0-19-242 (Relative Humidity with Respect to Precipitable Water)  was in http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-1.shtml
     It was a mistake in table conversion (from grib1 to grib2) in cnvgrib. It will be fixed in next implementation of cnvgrib in June or July, 2012.
     RHPW  in grib1 in table 129 parameter 230  and in grib2 in 0-1-242  */
    if (makeHash(discipline, category, number) == makeHash(0, 19, 242))
      return getParameter(0, 1, 242);

    Grib2Parameter plocal = NcepLocalParams.getParameter(discipline, category, number);

    if ((category <= 191) && (number <= 191))  {
      GribTables.Parameter pwmo = WmoCodeTable.getParameterEntry(discipline, category, number);
      if (plocal == null) return pwmo;

      // allow local table to override all but name, units
      plocal.name = pwmo.getName();
      plocal.unit = pwmo.getUnit();
    }

    return plocal;
  }

  @Override
  public String getTableValue(String tableName, int code) {
     if ((code < 192) || (code > 254) || tableName.equals("4.0"))
      return WmoCodeTable.getTableValue(tableName, code);

    return codeMap.get(tableName + "." + code);
  }

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
      case 255:
        return "Interval";
      default:
        return super.getIntervalNameShort(id);
    }
  }

  /////////////////////////////////////////////////////////////////
  // generating process ids for NCEP
  // GRIB1 TableA - can share (?)
  private static Map<Integer, String> genProcessMap;  // shared by all instances

  public String getGeneratingProcessName(int genProcess) {
    if (genProcessMap == null) genProcessMap = NcepTables.getNcepGenProcess();
    if (genProcessMap == null) return null;
    return genProcessMap.get(genProcess);
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
    Map<String, Grib2Parameter> org = NcepLocalParamsVeryOld.getParamMap();
    for (String skey : org.keySet()) {
      Grib2Parameter p = org.get(skey);
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
