/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2.table;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.grib.*;
import ucar.nc2.grib.coord.VertCoordType;
import ucar.nc2.grib.grib1.tables.NcepTables;
import ucar.nc2.grib.grib2.Grib2Parameter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.net.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * NCEP local tables.
 *
 * @author caron
 * @since 4/3/11
 */
class NcepLocalTables extends LocalTables {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NcepLocalTables.class);

  protected final NcepLocalParams ncepLocalParams;
  private final Map<String, String> codeMap = new HashMap<>(400);

  NcepLocalTables(Grib2TableConfig config) {
    super(config);
    this.ncepLocalParams = new NcepLocalParams(config.getPath());
    initCodes();
  }

  @Override
  public String getParamTablePathUsedFor(int discipline, int category, int number) {
    if ((category <= 191) && (number <= 191)) return super.getParamTablePathUsedFor(discipline, category, number);
    return ncepLocalParams.getTablePath(discipline, category);
  }

  private String[] getResourceListing(String path) throws URISyntaxException, IOException {
    Class clazz = this.getClass();
    URL dirURL = clazz.getClassLoader().getResource(path);
    if (dirURL != null && dirURL.getProtocol().equals("file")) {
      return new File(dirURL.toURI()).list();
    }

    if (dirURL == null) {
      //In case of a jar file, we can't actually find a directory.
      //Have to assume the same jar as clazz.
      String me = clazz.getName().replace(".", "/") + ".class";
      dirURL = clazz.getClassLoader().getResource(me);
    }
    if (dirURL == null) {
      throw new UnsupportedOperationException("Cannot list files for path "+path);
    }

    if (dirURL.getProtocol().equals("jar")) {
            /* A JAR path */
      String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
      Enumeration<JarEntry> entries;
      try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
        //gives ALL entries in jar
        entries = jar.entries();
      }
      Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
      while (entries.hasMoreElements()) {
        String name = entries.nextElement().getName();
        if (name.startsWith(path)) { //filter according to the path
          String entry = name.substring(path.length());
          int checkSubdir = entry.indexOf("/");
          if (checkSubdir >= 0) {
            // if it is a subdirectory, we just return the directory name
            entry = entry.substring(0, checkSubdir);
          }
          result.add(entry);
        }
      }
      return result.toArray(new String[0]);
    }

    throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
  }

  @Override
  public ImmutableList<Parameter> getParameters() {
    ImmutableList.Builder<GribTables.Parameter> allParams = ImmutableList.builder();
    try {
      String[] fileNames = getResourceListing(config.getPath());
      if (fileNames == null) return ImmutableList.of();
      for (String fileName : fileNames) {
        File f = new File(fileName);
        if (f.isDirectory()) continue;
        if (!f.getName().contains("Table4.2.")) continue;
        if (!f.getName().endsWith(".xml")) continue;
        try {
          List<GribTables.Parameter> params = ncepLocalParams.getParameters(config.getPath() + f.getPath());
          if (params != null)
            allParams.addAll(params);
        } catch (Exception e) {
          logger.error("Error reading wmo tables", e);
        }
      }
    } catch (URISyntaxException | IOException e) {
      logger.error("NcepLocalTables failed", e);
    }
    return allParams.build();
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
    /* email from boi.vuong@noaa.gov 1/19/2012
     "I find that the parameter 2-4-3 (Haines Index) now is parameter 2 in WMO version 8.
      The NAM fire weather nested  will take change in next implementation of cnvgrib (NCEP conversion program)."  */
    //if (makeHash(discipline, category, number) == makeHash(2,4,3))
    //  return getParameter(2,4,2);

    /* email from boi.vuong@noaa.gov 1/26/2012
     The parameter 0-19-242 (Relative Humidity with Respect to Precipitable Water)  was in http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-1.shtml
     It was a mistake in table conversion (from grib1 to grib2) in cnvgrib. It will be fixed in next implementation of cnvgrib in June or July, 2012.
     RHPW  in grib1 in table 129 parameter 230  and in grib2 in 0-1-242  */
    // if (makeHash(discipline, category, number) == makeHash(0, 19, 242))
    //   return getParameter(0, 1, 242);

    Grib2Parameter plocal = ncepLocalParams.getParameter(discipline, category, number);

    if ((category <= 191) && (number <= 191)) {
      GribTables.Parameter pwmo = WmoParamTable.getParameter(discipline, category, number);
      if (plocal == null) return pwmo;

      // allow local table to override all but name, units
      if (pwmo != null) {
        return new Grib2Parameter(plocal, pwmo.getName(), pwmo.getUnit());
      }
    }

    return plocal;
  }

  @Override
  public GribTables.Parameter getParameterRaw(int discipline, int category, int number) {
     return ncepLocalParams.getParameter(discipline, category, number);
  }

  @Override
  public String getCodeTableValue(String tableName, int code) {
    if (tableName.equals("ProcessId")) {
      return getGeneratingProcessName(code);
    }

    if ((code < 192) || (code > 254) || tableName.equals("4.0"))
      return super.getCodeTableValue(tableName, code);

    return codeMap.get(tableName + "." + code);
  }

  ////////////////////////////////////////////////////////////////////
  // Vert

  @Override
  public VertCoordType getVertUnit(int code) {

    switch (code) {
      case 235:
        return new VertCoordType(code, "0.1 C", null, true);

      case 237:
        return new VertCoordType(code, "m", null, true);

      case 238:
        return new VertCoordType(code, "m", null, true);

      case 241:
        return new VertCoordType(code, "count", null, true);   // eg see NCEP World Watch datasets

      default:
        return super.getVertUnit(code);
    }
  }

  @Override
  public String getLevelNameShort(int id) {
    switch (id) {
      case 200:
        return "entire_atmosphere_single_layer";
      case 201:
        return "entire_ocean_single_layer";
      case 204:
        return "highest_tropospheric_freezing";
      case 206:
        return "grid_scale_cloud_bottom";
      case 207:
        return "grid_scale_cloud_top";
      case 209:
        return "boundary_layer_cloud_bottom";
      case 210:
        return "boundary_layer_cloud_top";
      case 211:
        return "boundary_layer_cloud";
      case 212:
        return "low_cloud_bottom";
      case 213:
        return "low_cloud_top";
      case 214:
        return "low_cloud";
      case 215:
        return "cloud_ceiling";
      case 220:
        return "planetary_boundary";
      case 221:
        return "between_two_hybrids";
      case 222:
        return "middle_cloud_bottom";
      case 223:
        return "middle_cloud_top";
      case 224:
        return "middle_cloud";
      case 232:
        return "high_cloud_bottom";
      case 233:
        return "high_cloud_top";
      case 234:
        return "high_cloud";
      case 235:
        return "ocean_isotherm";
      case 236:
        return "layer_between_two_depths_below_ocean";
      case 237:
        return "bottom_of_ocean_mixed";
      case 238:
        return "bottom_of_ocean_isothermal";
      case 239:
        return "ocean_surface_and_26C_isothermal";
      case 240:
        return "ocean_mixed";
      case 241:
        return "ordered_sequence_of_data";
      case 242:
        return "convective_cloud_bottom";
      case 243:
        return "convective_cloud_top";
      case 244:
        return "convective_cloud";
      case 245:
        return "lowest_level_of_the_wet_bulb_zero";
      case 246:
        return "maximum_equivalent_potential_temperature";
      case 247:
        return "equilibrium";
      case 248:
        return "shallow_convective_cloud_bottom";
      case 249:
        return "shallow_convective_cloud_top";
      case 251:
        return "deep_convective_cloud_bottom";
      case 252:
        return "deep_convective_cloud_top";
      case 253:
        return "lowest_level_water_layer";
      case 254:
        return "highest_level_water_layer";
      default:
        return super.getLevelNameShort(id);
    }
  }

  //////////////////////////////////////////////////////////////

  @Override
  public String getStatisticNameShort(int id) {
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
        // Average of forecast averages. P1 = start of averaging period. P2 = end of averaging period. Reference time is the start time of the
        // first forecast, other forecasts at 6-hour intervals. Number in Ave = number of forecast used
        return "AverageAvg-6hourIntv";
      case 206:
        return "AverageForecastAccumulations-206";
      case 207:
        return "AverageForecastAverages-207";
      case 255:
        return "Interval";
      default:
        return super.getStatisticNameShort(id);
    }
  }

  @Override
  @Nullable
  public GribStatType getStatType(int id) {
    if (id < 192) return super.getStatType(id);
    switch (id) {  // LOOK not correct
      case 192:
      case 193:
      case 194:
      case 195:
      case 196:
      case 197:
      case 198:
      case 199:
      case 200:
      case 204:
      case 205:
      case 206:
      case 207:
        return GribStatType.Average;
      case 201:
        return GribStatType.RootMeanSquare;
      case 202:
      case 203:
        return GribStatType.StandardDeviation;
    }

    return null;
  }

  private static Map<Integer, String> statName;  // shared by all instances

  @Override
  @Nullable
  public String getStatisticName(int id) {
    if (id < 192) return super.getStatisticName(id);
    if (statName == null) statName = initTable410();
    if (statName == null) return null;
    return statName.get(id);
  }

  @Nullable
  private Map<Integer, String> initTable410() {
    String path = config.getPath() + "Table4.10.xml";
    try (InputStream is = GribResourceReader.getInputStream(path)) {
      SAXBuilder builder = new SAXBuilder();
      org.jdom2.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, String> result = new HashMap<>(200);
      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        result.put(code, desc);
      }
      return result;  // all at once - thread safe

    } catch (IOException | JDOMException ioe) {
      logger.error("Cant read  " + path, ioe);
      return null;
    }
  }

  /////////////////////////////////////////////////////////////////
  // generating process ids for NCEP
  // GRIB1 TableA - can share (?)
  private static Map<Integer, String> genProcessMap;  // shared by all instances

  @Override
  @Nullable
  public String getGeneratingProcessName(int genProcess) {
    if (genProcessMap == null) genProcessMap = NcepTables.getNcepGenProcess();
    if (genProcessMap == null) return null;
    return genProcessMap.get(genProcess);
  }

  @Override
  @Nullable
  public String getCategory(int discipline, int category) {
    String catName = ncepLocalParams.getCategory(discipline, category);
    if (catName != null) return catName;
    return super.getCategory(discipline, category);
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
  // Code table overrides.

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

}
