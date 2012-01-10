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

import ucar.nc2.constants.CF;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;

import java.util.*;

/**
 * NCEP local tables
 *
 * @author caron
 * @since 4/3/11
 */
public class NcepLocalTables extends Grib2Tables {

  NcepLocalTables(int center, int subCenter, int masterVersion, int localVersion) {
    super(center, subCenter, masterVersion, localVersion);
    initCodes();
    initLocalTable();
  }

  @Override
  public List getParameters() {
    List<TableEntry> result = new ArrayList<TableEntry>();
    for (TableEntry p : local.values()) result.add(p);
    Collections.sort(result);
    return result;
  }

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
    if ((category <= 191) && (number <= 191))
      return WmoCodeTable.getParameterEntry(discipline, category, number);

    // return NcepCodeTable.getParameterFromCurrent(discipline, category, number);
    return local.get(makeHash(discipline, category, number));

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


  @Override
  public int[] getForecastTimeInterval(Grib2Record gr) {
    if (!gr.getPDS().isInterval()) return null;
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) gr.getPDS();
    Grib2Pds.TimeInterval[] ti = pdsIntv.getTimeIntervals();
    if (ti.length == 1) return super.getForecastTimeInterval(gr);

    // LOOK this is hack for CFSR monthly combobulation
    Grib2Pds.TimeInterval tiUse = ti[0];
    int[] result = new int[2];
    result[0] = pdsIntv.getForecastTime();
    result[1] = pdsIntv.getForecastTime() + tiUse.timeRangeLength * tiUse.timeIncrement; // kludge
    return result;
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

  private void initLocalTable() {
    add(0, 0, 192, "SNOHF", "Snow Phase Change Heat Flux", "W/(m^2)");
    add(0, 0, 193, "TTRAD", "Temperature tendency by all radiation", "K/s");
    add(0, 0, 194, "REV", "Relative Error Variance", "-");
    add(0, 0, 195, "LRGHR", "Large Scale Condensate Heating rate", "K/s");
    add(0, 0, 196, "CNVHR", "Deep Convective Heating rate", "K/s");
    add(0, 0, 197, "THFLX", "Total Downward Heat Flux at Surface", "W/(m^2)");
    add(0, 0, 198, "TTDIA", "Temperature Tendency By All Physics", "K/s");
    add(0, 0, 199, "TTPHY", "Temperature Tendency By Non-radiation Physics", "K/s");
    add(0, 0, 200, "TSD1D", "Standard Dev. of IR Temp. over 1x1 deg. area", "K");
    add(0, 0, 201, "SHAHR", "Shallow Convective Heating rate", "K/s");
    add(0, 0, 202, "VDFHR", "Vertical Diffusion Heating rate", "K/s");
    add(0, 0, 203, "THZ0", "Potential temperature at top of viscous sublayer", "K");
    add(0, 0, 204, "TCHP", "Tropical Cyclone Heat Potential", "J/(m^2*K)");

    add(0, 1, 192, "CRAIN", "Categorical Rain", "0=no; 1=yes");
    add(0, 1, 193, "CFRZR", "Categorical Freezing Rain", "0=no; 1=yes");
    add(0, 1, 194, "CICEP", "Categorical Ice Pellets", "0=no; 1=yes");
    add(0, 1, 195, "CSNOW", "Categorical Snow", "0=no; 1=yes");
    add(0, 1, 196, "CPRAT", "Convective Precipitation Rate", "kg/(m^2*s)");
    add(0, 1, 197, "MCONV", "Horizontal Moisture Divergence", "kg/(kg*s)");
    add(0, 1, 198, "MINRH", "Minimum Relative Humidity", "%");
    add(0, 1, 199, "PEVAP", "Potential Evaporation", "kg/(m^2)");
    add(0, 1, 200, "PEVPR", "Potential Evaporation Rate", "W/(m^2)");
    add(0, 1, 201, "SNOWC", "Snow Cover", "%");
    add(0, 1, 202, "FRAIN", "Rain Fraction of Total Liquid Water", "-");

    add(0, 1, 203, "RIME", "Rime Factor", "-");  /* FRIME -> RIME 12/29/2005 */
    add(0, 1, 204, "TCOLR", "Total Column Integrated Rain", "kg/(m^2)");
    add(0, 1, 205, "TCOLS", "Total Column Integrated Snow", "kg/(m^2)");
    add(0, 1, 206, "TIPD", "Total Icing Potential Diagnostic", "-");
    add(0, 1, 207, "NCIP", "Number concentration for ice particles", "-");
    add(0, 1, 208, "SNOT", "Snow temperature", "K");
    add(0, 1, 209, "TCLSW", "Total column-integrated supercooled liquid water", "kg/(m^2)");
    add(0, 1, 210, "TCOLM", "Total column-integrated melting ice", "kg/(m^2)");
    add(0, 1, 211, "EMNP", "Evaporation - Precipitation", "cm/day");
    add(0, 1, 212, "SBSNO", "Sublimation (evaporation from snow)", "W/(m^2)");
    add(0, 1, 213, "CNVMR", "Deep Convective Moistening Rate", "kg/(kg*s)");
    add(0, 1, 214, "SHAMR", "Shallow Convective Moistening Rate", "kg/(kg*s)");
    add(0, 1, 215, "VDFMR", "Vertical Diffusion Moistening Rate", "kg/(kg*s)");
    add(0, 1, 216, "CONDP", "Condensation Pressure of Parcali Lifted From Indicate Surface", "Pa");
    add(0, 1, 217, "LRGMR", "Large scale moistening rate", "kg/(kg/s)");
    add(0, 1, 218, "QZ0", "Specific humidity at top of viscous sublayer", "kg/kg");
    add(0, 1, 219, "QMAX", "Maximum specific humidity at 2m", "kg/kg");
    add(0, 1, 220, "QMIN", "Minimum specific humidity at 2m", "kg/kg");
    add(0, 1, 221, "ARAIN", "Liquid precipitation (rainfall)", "kg/(m^2)");
    add(0, 1, 222, "SNOWT", "Snow temperature, depth-avg", "K");
    add(0, 1, 223, "APCPN", "Total precipitation (nearest grid point)", "kg/(m^2)");
    add(0, 1, 224, "ACPCPN", "Convective precipitation (nearest grid point)", "kg/(m^2)");
    add(0, 1, 225, "FRZR", "Freezing rain", "kg/(m^2)");
    add(0, 1, 226, "PWTHER", "Predominant Weather", "-");

    add(0, 2, 192, "VWSH", "Vertical speed sheer", "1/s");
    add(0, 2, 193, "MFLX", "Horizontal Momentum Flux", "N/(m^2)");
    add(0, 2, 194, "USTM", "U-Component Storm Motion", "m/s");
    add(0, 2, 195, "VSTM", "V-Component Storm Motion", "m/s");
    add(0, 2, 196, "CD", "Drag Coefficient", "-");
    add(0, 2, 197, "FRICV", "Frictional Velocity", "m/s");
    add(0, 2, 198, "LAUV", "Latitude of U Wind Component of Velocity", "deg");
    add(0, 2, 199, "LOUV", "Longitude of U Wind Component of Velocity", "deg");
    add(0, 2, 200, "LAVV", "Latitude of V Wind Component of Velocity", "deg");
    add(0, 2, 201, "LOVV", "Longitude of V Wind Component of Velocity", "deg");
    add(0, 2, 202, "LAPP", "Latitude of Pressure Point", "deg");
    add(0, 2, 203, "LOPP", "Longitude of Pressure Point", "deg");
    add(0, 2, 204, "VEDH", "Vertical Eddy Diffusivity Heat exchange", "m^2/s");
    add(0, 2, 205, "COVMZ", "Covariance between Meridional and Zonal Components of the wind", "m^2/s^2");
    add(0, 2, 206, "COVTZ", "Covariance between Temperature and Zonal Components of the wind", "K*m/s");
    add(0, 2, 207, "COVTM", "Covariance between Temperature and Meridional Components of the wind", "K*m/s");
    add(0, 2, 208, "VDFUA", "Vertical Diffusion Zonal Acceleration", "m/s^2");
    add(0, 2, 209, "VDFVA", "Vertical Diffusion Meridional Acceleration", "m/s^2");
    add(0, 2, 210, "GWDU", "Gravity wave drag zonal acceleration", "m/s^2");
    add(0, 2, 211, "GWDV", "Gravity wave drag meridional acceleration", "m/s^2");
    add(0, 2, 212, "CNVU", "Convective zonal momentum mixing acceleration", "m/s^2");
    add(0, 2, 213, "CNVV", "Convective meridional momentum mixing acceleration", "m/s^2");
    add(0, 2, 214, "WTEND", "Tendency of vertical velocity", "m/s^2");
    add(0, 2, 215, "OMGALF", "Omega (Dp/Dt) divide by density", "K");
    add(0, 2, 216, "CNGWDU", "Convective Gravity wave drag zonal acceleration", "m/s^2");
    add(0, 2, 217, "CNGWDV", "Convective Gravity wave drag meridional acceleration", "m/s^2");
    add(0, 2, 218, "LMV", "Velocity point model surface", "-");
    add(0, 2, 219, "PVMWW", "Potential vorticity (mass-weighted)", "1/(s/m)");
/* Removed 8/19/2008 */ /*           {0, 2, 220, "MFLX", "Momentum flux", "N/m^2");*/

    add(0, 3, 192, "MSLET", "MSLP (Eta model reduction)", "Pa");
    add(0, 3, 193, "5WAVH", "5-Wave Geopotential Height", "gpm");
    add(0, 3, 194, "U-GWD", "Zonal Flux of Gravity Wave Stress", "N/(m^2)");
    add(0, 3, 195, "V-GWD", "Meridional Flux of Gravity Wave Stress", "N/(m^2)");
    add(0, 3, 196, "HPBL", "Planetary Boundary Layer Height", "m");
    add(0, 3, 197, "5WAVA", "5-Wave Geopotential Height Anomaly", "gpm");
    add(0, 3, 198, "MSLMA", "MSLP (MAPS System Reduction)", "Pa");
    add(0, 3, 199, "TSLSA", "3-hr pressure tendency (Std. Atmos. Reduction)", "Pa/s");
    add(0, 3, 200, "PLPL", "Pressure of level from which parcel was lifted", "Pa");
    add(0, 3, 201, "LPSX", "X-gradient of Log Pressure", "1/m");
    add(0, 3, 202, "LPSY", "Y-gradient of Log Pressure", "1/m");
    add(0, 3, 203, "HGTX", "X-gradient of Height", "1/m");
    add(0, 3, 204, "HGTY", "Y-gradient of Height", "1/m");
    add(0, 3, 205, "LAYTH", "Layer Thickness", "m");
    add(0, 3, 206, "NLGSP", "Natural Log of Surface Pressure", "ln(kPa)");
    add(0, 3, 207, "CNVUMF", "Convective updraft mass flux", "kg/m^2/s");
    add(0, 3, 208, "CNVDMF", "Convective downdraft mass flux", "kg/m^2/s");
    add(0, 3, 209, "CNVDEMF", "Convective detrainment mass flux", "kg/m^2/s");
    add(0, 3, 210, "LMH", "Mass point model surface", "-");
    add(0, 3, 211, "HGTN", "Geopotential height (nearest grid point)", "gpm");
    add(0, 3, 212, "PRESN", "Pressure (nearest grid point)", "Pa");

    add(0, 4, 192, "DSWRF", "Downward Short-Wave Rad. Flux", "W/(m^2)");
    add(0, 4, 193, "USWRF", "Upward Short-Wave Rad. Flux", "W/(m^2)");
    add(0, 4, 194, "DUVB", "UV-B downward solar flux", "W/(m^2)");
    add(0, 4, 195, "CDUVB", "Clear sky UV-B downward solar flux", "W/(m^2)");
    add(0, 4, 196, "CSDSF", "Clear sky Downward Solar Flux", "W/(m^2)");
    add(0, 4, 197, "SWHR", "Solar Radiative Heating Rate", "K/s");
    add(0, 4, 198, "CSUSF", "Clear Sky Upward Solar Flux", "W/(m^2)");
    add(0, 4, 199, "CFNSF", "Cloud Forcing Net Solar Flux", "W/(m^2)");
    add(0, 4, 200, "VBDSF", "Visible Beam Downward Solar Flux", "W/(m^2)");
    add(0, 4, 201, "VDDSF", "Visible Diffuse Downward Solar Flux", "W/(m^2)");
    add(0, 4, 202, "NBDSF", "Near IR Beam Downward Solar Flux", "W/(m^2)");
    add(0, 4, 203, "NDDSF", "Near IR Diffuse Downward Solar Flux", "W/(m^2)");
    add(0, 4, 204, "DTRF", "Downward Total radiation Flux", "W/(m^2)");
    add(0, 4, 205, "UTRF", "Upward Total radiation Flux", "W/(m^2)");

    add(0, 5, 192, "DLWRF", "Downward Long-Wave Rad. Flux", "W/(m^2)");
    add(0, 5, 193, "ULWRF", "Upward Long-Wave Rad. Flux", "W/(m^2)");
    add(0, 5, 194, "LWHR", "Long-Wave Radiative Heating Rate", "K/s");
    add(0, 5, 195, "CSULF", "Clear Sky Upward Long Wave Flux", "W/(m^2)");
    add(0, 5, 196, "CSDLF", "Clear Sky Downward Long Wave Flux", "W/(m^2)");
    add(0, 5, 197, "CFNLF", "Cloud Forcing Net Long Wave Flux", "W/(m^2)");

    add(0, 6, 192, "CDLYR", "Non-Convective Cloud Cover", "%");
    add(0, 6, 193, "CWORK", "Cloud Work Function", "J/kg");
    add(0, 6, 194, "CUEFI", "Convective Cloud Efficiency", "-");
    add(0, 6, 195, "TCOND", "Total Condensate", "kg/kg");
    add(0, 6, 196, "TCOLW", "Total Column-Integrated Cloud Water", "kg/(m^2)");
    add(0, 6, 197, "TCOLI", "Total Column-Integrated Cloud Ice", "kg/(m^2)");
    add(0, 6, 198, "TCOLC", "Total Column-Integrated Condensate", "kg/(m^2)");
    add(0, 6, 199, "FICE", "Ice fraction of total condensate", "-");
    add(0, 6, 200, "MFLUX", "Convective Cloud Mass Flux", "Pa/s");
    add(0, 6, 201, "SUNSD", "SunShine duration", "s");

    add(0, 7, 192, "LFTX", "Surface Lifted Index", "K");
    add(0, 7, 193, "4LFTX", "Best (4 layer) Lifted Index", "K");
    add(0, 7, 194, "RI", "Richardson Number", "-");
    add(0, 7, 195, "CWDI", "Convective Weather Detection Index", "-");
    add(0, 7, 196, "UVI", "Ultra Violet Index", "W/(m^2)");
    add(0, 7, 197, "UPHL", "Updraft Helicity", "m^2/s^2");
    add(0, 7, 198, "LAI", "Leaf area index", "-");

    add(0, 13, 192, "PMTC", "Particulate matter (coarse)", "g/m^3");
    add(0, 13, 193, "PMTF", "Particulate matter (fine)", "g/m^3");
    add(0, 13, 194, "LPMTF", "Particulate matter (fine)", "log10(g/m^3)");
    add(0, 13, 195, "LIPMF", "Integrated column particulate matter (fine)", "log10(g/m^3)");

    add(0, 14, 192, "O3MR", "Ozone Mixing Ratio", "kg/kg");
    add(0, 14, 193, "OZCON", "Ozone Concentration", "PPB");
    add(0, 14, 194, "OZCAT", "Categorical Ozone Concentration", "-");
    add(0, 14, 195, "VDFOZ", "Ozone Vertical Diffusion", "kg/kg/s");
    add(0, 14, 196, "POZ", "Ozone Production", "kg/kg/s");
    add(0, 14, 197, "TOZ", "Ozone Tendency", "kg/kg/s");
    add(0, 14, 198, "POZT", "Ozone Production from Temperature Term", "kg/kg/s");
    add(0, 14, 199, "POZO", "Ozone Production from Column Ozone Term", "kg/kg/s");
    add(0, 14, 200, "OZMAX1", "Ozone Daily Max from 1-hour Average", "ppbV");
    add(0, 14, 201, "OZMAX8", "Ozone Daily Max from 8-hour Average", "ppbV");
    add(0, 14, 202, "PDMAX1", "PM 2.5 Daily Max from 1-hour Average", "ug/(m^3)");
    add(0, 14, 203, "PDMAX24", "PM 2.5 Daily Max from 24-hour Average", "ug/(m^3)");

    add(0, 16, 192, "REFZR", "Derived radar reflectivity backscatter from rain", "mm^6/m^3");
    add(0, 16, 193, "REFZI", "Derived radar reflectivity backscatter from ice", "mm^6/m^3");
    add(0, 16, 194, "REFZC", "Derived radar reflectivity backscatter from parameterized convection", "mm^6/m^3");
    add(0, 16, 195, "REFD", "Derived radar reflectivity", "dB");
    add(0, 16, 196, "REFC", "Maximum / Composite radar reflectivity", "dB");
    add(0, 16, 197, "RETOP", "Radar Echo Top (18.3 DBZ)", "m");

    add(0, 17, 192, "LTNG", "Lightning", "-");

    add(0, 19, 192, "MXSALB", "Maximum Snow Albedo", "%");
    add(0, 19, 193, "SNFALB", "Snow-Free Albedo", "%");
    add(0, 19, 194, "SRCONO", "Slight risk convective outlook", "categorical");
    add(0, 19, 195, "MRCONO", "Moderate risk convective outlook", "categorical");
    add(0, 19, 196, "HRCONO", "High risk convective outlook", "categorical");
    add(0, 19, 197, "TORPROB", "Tornado probability", "%");
    add(0, 19, 198, "HAILPROB", "Hail probability", "%");
    add(0, 19, 199, "WINDPROB", "Wind probability", "%");
    add(0, 19, 200, "STORPROB", "Significant Tornado probability", "%");
    add(0, 19, 201, "SHAILPRO", "Significant Hail probability", "%");
    add(0, 19, 202, "SWINDPRO", "Significant Wind probability", "%");
    add(0, 19, 203, "TSTMC", "Categorical Thunderstorm", "0=no; 1=yes");
    add(0, 19, 204, "MIXLY", "Number of mixed layers next to surface", "integer");
    add(0, 19, 205, "FLGHT", "Flight Category", "-");
    add(0, 19, 206, "CICEL", "Confidence Ceiling", "-");
    add(0, 19, 207, "CIVIS", "Confidence Visibility", "-");
    add(0, 19, 208, "CIFLT", "Confidence Flight Category", "-");
    add(0, 19, 209, "LAVNI", "Low Level aviation interest", "-");
    add(0, 19, 210, "HAVNI", "High Level aviation interest", "-");
    add(0, 19, 211, "SBSALB", "Visible; Black Sky Albedo", "%");
    add(0, 19, 212, "SWSALB", "Visible; White Sky Albedo", "%");
    add(0, 19, 213, "NBSALB", "Near IR; Black Sky Albedo", "%");
    add(0, 19, 214, "NWSALB", "Near IR; White Sky Albedo", "%");
    add(0, 19, 215, "PRSVR", "Total Probability of Severe Thunderstorms (Days 2,3)", "%");
    add(0, 19, 216, "PRSIGSVR", "Total Probability of Extreme Severe Thunderstorms (Days 2,3)", "%");
    add(0, 19, 217, "SIPD", "Supercooled Large Droplet Icing", "0=None; 1=Light; 2=Moderate; 3=Severe; 4=Trace; 5=Heavy; 255=missing");
    add(0, 19, 218, "EPSR", "Radiative emissivity", "");
    add(0, 19, 219, "TPFI", "Turbulence potential forecast index", "-");
    add(0, 19, 220, "", "Reserved", "-");
    add(0, 19, 221, "", "Reserved", "-");
    add(0, 19, 222, "", "Reserved", "-");
    add(0, 19, 223, "", "Reserved", "-");
    add(0, 19, 224, "", "Reserved", "-");
    add(0, 19, 225, "", "Reserved", "-");
    add(0, 19, 226, "", "Reserved", "-");
    add(0, 19, 227, "", "Reserved", "-");
    add(0, 19, 228, "", "Reserved", "-");
    add(0, 19, 229, "", "Reserved", "-");
    add(0, 19, 230, "", "Reserved", "-");
    add(0, 19, 231, "", "Reserved", "-");
/* These stopped being used? 3/26/2008 */
/*
            add(0, 19, 217, "MEIP", "Mean Icing Potential", "kg/m^2");
            {0, 19, 218, "MAIP", "Maximum Icing Potential", "kg/m^2");
            {0, 19, 219, "MECTP", "Mean in-Cloud Turbulence Potential", "kg/m^2");
            {0, 19, 220, "MACTP", "Max in-Cloud Turbulence Potential", "kg/m^2");
            {0, 19, 221, "MECAT", "Mean Cloud Air Turbulence Potential", "kg/m^2");
            {0, 19, 222, "MACAT", "Maximum Cloud Air Turbulence Potential", "kg/m^2");
            {0, 19, 223, "CBHE", "Cumulonimbus Horizontal Extent", "%");
            {0, 19, 224, "PCBB", "Pressure at Cumulonimbus Base", "Pa");
            {0, 19, 225, "PCBT", "Pressure at Cumulonimbus Top", "Pa");
            {0, 19, 226, "PECBB", "Pressure at Embedded Cumulonimbus Base", "Pa");
            {0, 19, 227, "PECBT", "Pressure at Embedded Cumulonimbus Top", "Pa");
            {0, 19, 228, "HCBB", "ICAO Height at Cumulonimbus Base", "m");
            {0, 19, 229, "HCBT", "ICAO Height at Cumulonimbus Top", "m");
            {0, 19, 230, "HECBB", "ICAO Height at Embedded Cumulonimbus Base", "m");
            {0, 19, 231, "HECBT", "ICAO Height at Embedded Cumulonimbus Top", "m");
*/
    add(0, 19, 232, "VAFTD", "Volcanic Ash Forecast Transport and Dispersion", "log10(kg/m^3)");

    add(0, 191, 192, "NLAT", "Latitude (-90 to 90)", "deg");
    add(0, 191, 193, "ELON", "East Longitude (0 to 360)", "deg");
    add(0, 191, 194, "TSEC", "Seconds prior to initial reference time", "s");
    add(0, 191, 195, "MLYNO", "Model Layer number (From bottom up)", "");
    add(0, 191, 196, "NLATN", "Latitude (nearest neighbor) (-90 to 90)", "deg");
    add(0, 191, 197, "ELONN", "East longitude (nearest neighbor) (0 to 360)", "deg");

/* table 4.2 : 0.192 according to NCEP is "Covariance". */
    add(0, 192, 1, "COVZM", "Covariance between zonal and meridional components of the wind", "m^2/s^2");
    add(0, 192, 2, "COVTZ", "Covariance between zonal component of the wind and temperature", "K*m/s");
    add(0, 192, 3, "COVTM", "Covariance between meridional component of the wind and temperature", "K*m/s");
    add(0, 192, 4, "COVTW", "Covariance between temperature and vertical component of the wind", "K*m/s");
    add(0, 192, 5, "COVZZ", "Covariance between zonal and zonal components of the wind", "m^2/s^2");
    add(0, 192, 6, "COVMM", "Covariance between meridional and meridional components of the wind", "m^2/s^2");
    add(0, 192, 7, "COVQZ", "Covariance between specific humidity and zonal components of the wind", "kg/kg*m/s");
    add(0, 192, 8, "COVQM", "Covariance between specific humidity and meridional components of the wind", "kg/kg*m/s");
    add(0, 192, 9, "COVTVV", "Covariance between temperature and vertical components of the wind", "K*Pa/s");
    add(0, 192, 10, "COVQVV", "Covariance between specific humidity and vertical components of the wind", "kg/kg*Pa/s");
    add(0, 192, 11, "COVPSPS", "Covariance between surface pressure and surface pressure", "Pa*Pa");
    add(0, 192, 12, "COVQQ", "Covariance between specific humidity and specific humidity", "kg/kg*kg/kg");
    add(0, 192, 13, "COVVVVV", "Covariance between vertical and vertical components of the wind", "Pa^2/s^2");
    add(0, 192, 14, "COVTT", "Covariance between temperature and temperature", "K*K");

    add(1, 0, 192, "BGRUN", "Baseflow-Groundwater Runoff", "kg/(m^2)");
    add(1, 0, 193, "SSRUN", "Storm Surface Runoff", "kg/(m^2)");

    add(1, 1, 192, "CPOZP", "Probability of Freezing Precipitation", "%");
    add(1, 1, 193, "CPOFP", "Probability of Frozen Precipitation", "%");
    add(1, 1, 194, "PPFFG", "Probability of precipitation exceeding flash flood guidance values", "%");
    add(1, 1, 195, "CWR", "Probability of Wetting Rain; exceeding in 0.1 inch in a given time period", "%");

    add(2, 0, 192, "SOILW", "Volumetric Soil Moisture Content", "Fraction");
    add(2, 0, 193, "GFLUX", "Ground Heat Flux", "W/(m^2)");
    add(2, 0, 194, "MSTAV", "Moisture Availability", "%");
    add(2, 0, 195, "SFEXC", "Exchange Coefficient", "(kg/(m^3))(m/s)");
    add(2, 0, 196, "CNWAT", "Plant Canopy Surface Water", "kg/(m^2)");
    add(2, 0, 197, "BMIXL", "Blackadar's Mixing Length Scale", "m");
    add(2, 0, 198, "VGTYP", "Vegetation Type", "0..13");
    add(2, 0, 199, "CCOND", "Canopy Conductance", "m/s");
    add(2, 0, 200, "RSMIN", "Minimal Stomatal Resistance", "s/m");
    add(2, 0, 201, "WILT", "Wilting Point", "Fraction");
    add(2, 0, 202, "RCS", "Solar parameter in canopy conductance", "Fraction");
    add(2, 0, 203, "RCT", "Temperature parameter in canopy conductance", "Fraction");
    add(2, 0, 204, "RCQ", "Humidity parameter in canopy conductance", "Fraction");
    add(2, 0, 205, "RCSOL", "Soil moisture parameter in canopy conductance", "Fraction");
    add(2, 0, 206, "RDRIP", "Rate of water dropping from canopy to ground", "unknown");
    add(2, 0, 207, "ICWAT", "Ice-free water surface", "%");
    add(2, 0, 208, "AKHS", "Surface exchange coefficients for T and Q divided by delta z", "m/s");
    add(2, 0, 209, "AKMS", "Surface exchange coefficients for U and V divided by delta z", "m/s");
    add(2, 0, 210, "VEGT", "Vegetation canopy temperature", "K");
    add(2, 0, 211, "SSTOR", "Surface water storage", "K g/m^2");
    add(2, 0, 212, "LSOIL", "Liquid soil moisture content (non-frozen)", "K g/m^2");
    add(2, 0, 213, "EWATR", "Open water evaporation (standing water)", "W/m^2");
    add(2, 0, 214, "GWREC", "Groundwater recharge", "kg/m^2");
    add(2, 0, 215, "QREC", "Flood plain recharge", "kg/m^2");
    add(2, 0, 216, "SFCRH", "Roughness length for heat", "m");
    add(2, 0, 217, "NDVI", "Normalized difference vegetation index", "-");
    add(2, 0, 218, "LANDN", "Land-sea coverage (nearest neighbor)", "0=sea; 1=land");
    add(2, 0, 219, "AMIXL", "Asymptotic mixing length scale", "m");
    add(2, 0, 220, "WVINC", "Water vapor added by precip assimilation", "kg/m^2");
    add(2, 0, 221, "WCINC", "Water condensate added by precip assimilation", "kg/m^2");
    add(2, 0, 222, "WVCONV", "Water vapor flux convergence (vertical int)", "kg/m^2");
    add(2, 0, 223, "WCCONV", "Water condensate flux convergence (vertical int)", "kg/m^2");
    add(2, 0, 224, "WVUFLX", "Water vapor zonal flux (vertical int)", "kg/m^2");
    add(2, 0, 225, "WVVFLX", "Water vapor meridional flux (vertical int)", "kg/m^2");
    add(2, 0, 226, "WCUFLX", "Water condensate zonal flux (vertical int)", "kg/m^2");
    add(2, 0, 227, "WCVFLX", "Water condensate meridional flux (vertical int)", "kg/m^2");
    add(2, 0, 228, "ACOND", "Aerodynamic conductance", "m/s");
    add(2, 0, 229, "EVCW", "Canopy water evaporation", "W/(m^2)");
    add(2, 0, 230, "TRANS", "Transpiration", "W/(m^2)");

    add(2, 3, 192, "SOILL", "Liquid Volumetric Soil Moisture (non Frozen)", "Proportion");
    add(2, 3, 193, "RLYRS", "Number of Soil Layers in Root Zone", "-");
    add(2, 3, 194, "SLTYP", "Surface Slope Type", "Index");
    add(2, 3, 195, "SMREF", "Transpiration Stress-onset (soil moisture)", "Proportion");
    add(2, 3, 196, "SMDRY", "Direct Evaporation Cease (soil moisture)", "Proportion");
    add(2, 3, 197, "POROS", "Soil Porosity", "Proportion");
    add(2, 3, 198, "EVBS", "Direct evaporation from bare soil", "W/m^2");
    add(2, 3, 199, "LSPA", "Land Surface Precipitation Accumulation", "kg/m^2");
    add(2, 3, 200, "BARET", "Bare soil surface skin temperature", "K");
    add(2, 3, 201, "AVSFT", "Average surface skin temperature", "K");
    add(2, 3, 202, "RADT", "Effective radiative skin temperature", "K");
    add(2, 3, 203, "FLDCP", "Field Capacity", "Fraction");

/* ScatEstUWind -> USCT, ScatEstVWind -> VSCT as of 7/5/2006 (pre 1.80) */
    add(3, 1, 192, "USCT", "Scatterometer Estimated U Wind", "m/s");
    add(3, 1, 193, "VSCT", "Scatterometer Estimated V Wind", "m/s");

/* table 4.2 : 3.192 according to NCEP is "Forecast Satellite Imagery". */
    add(3, 192, 0, "SBT122", "Simulated Brightness Temperature for GOES 12, Channel 2", "K");
    add(3, 192, 1, "SBT123", "Simulated Brightness Temperature for GOES 12, Channel 3", "K");
    add(3, 192, 2, "SBT124", "Simulated Brightness Temperature for GOES 12, Channel 4", "K");
    add(3, 192, 3, "SBT125", "Simulated Brightness Temperature for GOES 12, Channel 5", "K");
    add(3, 192, 4, "SBC123", "Simulated Brightness Counts for GOES 12, Channel 3", "numeric");
    add(3, 192, 5, "SBC124", "Simulated Brightness Counts for GOES 12, Channel 4", "numeric");

    add(10, 0, 192, "WSTP", "Wave Steepness", "0");

/* The following entry was moved to 10,3,196 */
/*
           add(10, 1, 192, "P2OMLT", "Ocean Mixed Layer Potential Density (Reference 2000m)", "kg/(m^3)");
*/
    add(10, 1, 192, "OMLU", "Ocean Mixed Layer U Velocity", "m/s");
    add(10, 1, 193, "OMLV", "Ocean Mixed Layer V Velocity", "m/s");
    add(10, 1, 194, "UBARO", "Barotropic U Velocity", "m/s");
    add(10, 1, 195, "VBARO", "Barotropic V Velocity", "m/s");

    /* Arthur Added this to both NDFD and NCEP local tables. (5/1/2006) */
    add(10, 3, 192, "SURGE", "Hurricane Storm Surge", "m");
    add(10, 3, 193, "ETSRG", "Extra Tropical Storm Surge", "m");
    add(10, 3, 194, "ELEV", "Ocean Surface Elevation Relative to Geoid", "m");
    add(10, 3, 195, "SSHG", "Sea Surface Height Relative to Geoid", "m");
/* The following entry were moved to 10,4,192, 10,4,193 */
/*
           add(10, 3, 196, "WTMPC", "3-D Temperature", "deg C");
           add(10, 3, 197, "SALIN", "3-D Salinity", "");
*/
    add(10, 3, 196, "P2OMLT", "Ocean Mixed Layer Potential Density (Reference 2000m)", "kg/(m^3)");
    add(10, 3, 197, "AOHFLX", "Net Air-Ocean Heat Flux", "W/(m^2)");
    add(10, 3, 198, "ASHFL", "Assimilative Heat Flux", "W/(m^2)");
    add(10, 3, 199, "SSTT", "Surface Temperature Trend", "degree/day");
    add(10, 3, 200, "SSST", "Surface Salinity Trend", "psu/day");
    add(10, 3, 201, "KENG", "Kinetic Energy", "J/kg");
    add(10, 3, 202, "SLTFL", "Salt Flux", "kg/(m^2*s)");

    add(10, 3, 242, "TCSRG20", "20% Tropical Cyclone Storm Surge Exceedance", "m");
    add(10, 3, 243, "TCSRG30", "30% Tropical Cyclone Storm Surge Exceedance", "m");
    add(10, 3, 244, "TCSRG40", "40% Tropical Cyclone Storm Surge Exceedance", "m");
    add(10, 3, 245, "TCSRG50", "50% Tropical Cyclone Storm Surge Exceedance", "m");
    add(10, 3, 246, "TCSRG60", "60% Tropical Cyclone Storm Surge Exceedance", "m");
    add(10, 3, 247, "TCSRG70", "70% Tropical Cyclone Storm Surge Exceedance", "m");
    add(10, 3, 248, "TCSRG80", "80% Tropical Cyclone Storm Surge Exceedance", "m");
    add(10, 3, 249, "TCSRG90", "90% Tropical Cyclone Storm Surge Exceedance", "m");

    add(10, 4, 192, "WTMPC", "3-D Temperature", "deg C");
    add(10, 4, 193, "SALIN", "3-D Salinity", "");
    add(10, 4, 194, "BKENG", "Barotropic Kinetic Energy", "J/kg");
    add(10, 4, 195, "DBSS", "Geometric Depth Below Sea Surface", "m");
    add(10, 4, 196, "INTFD", "Interface Depths", "m");
    add(10, 4, 197, "OHC", "Ocean Heat Content", "J/m^2");
  }

  private void add(int discipline, int category, int number, String abbrev, String name, String unit) {
    local.put(makeHash(discipline, category, number), new TableEntry(discipline, category, number, name, unit, abbrev));
  }

  private final Map<Integer, TableEntry> local = new HashMap<Integer, TableEntry>(100);

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
    tables.initLocalTable();
    for (int key : tables.local.keySet()) {
      Grib2Tables.Parameter p = tables.local.get(key);
      map.put(key, new CompTable(key, p, null));
    }

    NcepCodeTable.init();
    Map<String, TableEntry> org = NcepCodeTable.getParamMap();
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

    ArrayList<Integer> keys = new ArrayList<Integer>();
    for (int key : map.keySet()) keys.add(key);
    Collections.sort(keys);
    for (int key : keys) {
      CompTable ct = map.get(key);
      System.out.printf("%s%n", ct.local);
      System.out.printf("%s%n%n", ct.org);
    }
  }

  public CF.CellMethods convertTable4_10(int code) {
    if (code < 192) return super.convertTable4_10(code);
    switch (code) {
      case 193:
      case 194:
      case 195:
      case 196:
      case 197:
      case 198:
      case 199:
      case 200:
        return CF.CellMethods.mean;
      //case 201: return	"RootMeanSquare";
      case 202:
      case 203:
        return CF.CellMethods.standard_deviation;
      case 204:
        return CF.CellMethods.sum; // "Accumulation";
      case 205:
      case 206:
      case 207:
        return CF.CellMethods.mean;
      default:
        return null;
    }
  }

}

/*
 192
  Climatological Mean Value: multiple year averages of quantities which are themselves means over some period of time (P2) less than a year.
The reference time (R) indicates the date and time of the start of a period of time, given by R to R + P2, over which a mean is formed;
N indicates the number of such period-means that are averaged together to form the climatological value, assuming that the N period-mean
fields are separated by one year. The reference time indicates the start of the N-year climatology. N is given in octets 22-23 of the PDS.
  If P1 = 0 then the data averaged in the basic interval P2 are assumed to be continuous, i.e., all available data are simply averaged together.
  If P1 = 1 (the units of time - octet 18, code table 4 - are not relevant here) then the data averaged together in the basic interval P2 are
valid only at the time (hour, minute) given in the reference time, for all the days included in the P2 period. The units of P2 are given by
the contents of octet 18 and Table 4.

193
Average of N forecasts (or initialized analyses); each product has forecast period of P1 (P1=0 for initialized analyses); products have reference
times at intervals of P2, beginning at the given reference time.

194
Average of N uninitialized analyses, starting at reference time, at intervals of P2.

195
Average of forecast accumulations. P1 = start of accumulation period. P2 = end of accumulation period. Reference time is the start time of the
first forecast, other forecasts at 24-hour intervals. Number in Ave = number of forecasts used.

196
Average of successive forecast accumulations. P1 = start of accumulation period. P2 = end of accumulation period. Reference time is the start
time of the first forecast, other forecasts at (P2 - P1) intervals. Number in Ave = number of forecasts used

197
Average of forecast averages. P1 = start of averaging period. P2 = end of averaging period. Reference time is the start time of the first forecast,
other forecasts at 24-hour intervals. Number in Ave = number of forecast used

198
Average of successive forecast averages. P1 = start of averaging period. P2 = end of averaging period. Reference time is the start time of the
first forecast, other forecasts at (P2 - P1) intervals. Number in Ave = number of forecasts used

199
Climatological Average of N analyses, each a year apart, starting from initial time R and for the period from R+P1 to R+P2.

200
Climatological Average of N forecasts, each a year apart, starting from initial time R and for the period from R+P1 to R+P2.

201
Climatological Root Mean Square difference between N forecasts and their verifying analyses, each a year apart, starting with initial time R and
for the period from R+P1 to R+P2.

202
Climatological Standard Deviation of N forecasts from the mean of the same N forecasts, for forecasts one year apart. The first forecast starts
wtih initial time R and is for the period from R+P1 to R+P2.

203
Climatological Standard Deviation of N analyses from the mean of the same N analyses, for analyses one year apart. The first analyses is valid
for  period R+P1 to R+P2.

204
Average of forecast accumulations. P1 = start of accumulation period. P2 = end of accumulation period. Reference time is the start time of the
first forecast, other forecasts at 6-hour intervals. Number in Ave = number of forecast used

205
Average of forecast averages. P1 = start of averaging period. P2 = end of averaging period. Reference time is the start time of the first forecast,
other forecasts at 6-hour intervals. Number in Ave = number of forecast used

206
Average of forecast accumulations. P1 = start of accumulation period. P2 = end of accumulation period. Reference time is the start time of the
first forecast, other forecasts at 12-hour intervals. Number in Ave = number of forecast used

207
Average of forecast averages. P1 = start of averaging period. P2 = end of averaging period. Reference time is the start time of the first forecast,
other forecasts at 12-hour intervals. Number in Ave = number of forecast used
*/

