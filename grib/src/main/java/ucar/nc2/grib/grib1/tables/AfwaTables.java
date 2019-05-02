/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1.tables;

import javax.annotation.Nullable;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.coord.VertCoordType;
import ucar.nc2.grib.grib1.Grib1ParamLevel;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Air Force Weather Center (center 57)
 *
 * @author caron
 * @since 7/31/13
 */
public class AfwaTables extends Grib1Customizer {
  private static Map<Integer, VertCoordType> levelTypesMap;  // shared by all instances
  private static Map<Integer, String> genProcessMap;  // shared by all instances
  private static Map<Integer, String> subcenterMap;  // shared by all instances

  AfwaTables(Grib1ParamTables tables) {
    super(57, tables);
  }

  // subcenters

  @Override
  public String getSubCenterName(int subcenter) {
    if (subcenterMap == null)
      subcenterMap = makeSubcenterMap();
    return subcenterMap.get(subcenter);
  }

  private static Map<Integer, String> makeSubcenterMap() {
    Map<Integer, String> result = new HashMap<>(100);

    result.put(0, "AFWA Primary Table");
    result.put(1, "AFWA Numerical Weather Prediction Models Products");
    result.put(2, "AFWA Special Environmental Models Products");
    result.put(3, "AFWA Space Products");
    result.put(4, "AFWA Ensemble Products");
    result.put(6, "AFWA Aerosol Products");
    result.put(7, "CDFS-II Aerosol Model");
    result.put(8, "AFWA Volcano Products");
    result.put(10, "14 WS (Formerly Air Force Combat Climatology Center (AFCCC))");
    result.put(11, "14 WS Numerical Weather Prediction Models Products");
    result.put(15, "15 OWS (Scott)");
    result.put(17, "17 OWS (Hickam)");
    result.put(20, "20 OWS (Yokota)");
    result.put(21, "21 OWS (Sembach)");
    result.put(25, "25 OWS (Davis-Monthan)");
    result.put(26, "26 OWS (Barksdale)");
    result.put(27, "27 OWS (Offutt)");
    result.put(28, "28 OWS (Shaw)");
    result.put(29, "Air Force Tactical Application Center (AFTAC) ");
    result.put(90, "AFWA 4DF 1");
    result.put(91, "AFWA 4DF 2");
    result.put(92, "AFWA 4DF 3");
    result.put(93, "AFWA 4DF 4");
    result.put(94, "AFWA 4DF 5");
    result.put(110, "Air Force Research Lab (AFRL) Office of Research and Applications");
    result.put(130, "Patrick AFB OWS Eastern Test Range");
    result.put(150, "IPDS Satellite");

    return Collections.unmodifiableMap(result);  // all at once - thread safe
  }

  // gen process
  @Override
  @Nullable
  public String getGeneratingProcessName(int genProcess) {
    if (genProcessMap == null) makeGenProcessMap();
    return genProcessMap.get(genProcess);
  }

  private static void makeGenProcessMap() {
    genProcessMap = new HashMap<>(100);
    genProcessMap.put(10, "Mesoscale Model 5 (MM5)");
    genProcessMap.put(11, "Weather Research and Forecasting Model (WRF)");
    genProcessMap.put(12, "Multivariate Optimum Interpolation Model (MVOI)");
    genProcessMap.put(13, "Three Dimensional Variate Model (3DVAR)");
    genProcessMap.put(14, "Weather Research and Forecasting Model Chemical & Aerosol (WRF-CHEM)");
    genProcessMap.put(25, "Snow Depth Model (SNODEP) (Manually modified)");
    genProcessMap.put(26, "Real Time Nephanalysis Model (RTNEPH)");
    genProcessMap.put(27, "Surface Temperature Model (SFCTMP)");
    genProcessMap.put(28, "Advect Cloud Model (ADVCLD)");
    genProcessMap.put(29, "Worldwide Merged Cloud Analysis (WWMCA) (Manually modified)");
    genProcessMap.put(30, "Short Range Cloud Forecast (SRCF)");
    genProcessMap.put(31, "Long Range Cloud Forecast (LRCF)");
    genProcessMap.put(32, "Worldwide Merged Cloud Analysis (WWMCA) (Not modified)");
    genProcessMap.put(33, "Diagnostic Cloud Forecast v. 3 (DCF3) 3-layer");
    genProcessMap.put(34, "Stochastic Cloud Forecast Model (SCFM)");
    genProcessMap.put(35, "Snow Depth Model (SNODEP) (Not modified)");
    genProcessMap.put(36, "Diagnostic Cloud Forecast v. 3 (DCF3) 5-layer");
    genProcessMap.put(39, "Diagnostic Cloud Forecast v. 3 (DCF3) P-layer");
    genProcessMap.put(40, "Diagnostic Cloud Forecast v. 4 (DCF4)");
    genProcessMap.put(50, "Dust Transport Application (DTA)");
    genProcessMap.put(55, "Aerosol Model (CDFS-II)");
    genProcessMap.put(75, "Advanced Climate Modeling and Environmental Simulations (ACMES)");
    genProcessMap.put(86, "Geostationary satellite-based precipitation model (GEO_PRECIP)");
    genProcessMap.put(87, "Agricultural Meteorology (AGRMET)");
    genProcessMap.put(88, "Land Information System (LIS)");
    genProcessMap.put(96, "Array-flipped (to NOGAPS grid structure) Global Forecast System (GFS)");
    genProcessMap.put(99, "Ensemble Post-Processor");
    genProcessMap.put(100, "Ionospheric Forecast Model (IFM)");
    genProcessMap.put(101, "Parameterized Real-Time Specification Model (PRISM)");
    genProcessMap.put(125, "Snow Depth Climatology");
    genProcessMap.put(127, "Unified Model (UM)");
    genProcessMap.put(200, "NOGAPS post-processed by AFWA");
    genProcessMap.put(201, "GFS post processed by AFWA");
    genProcessMap.put(204, "FNMOC NOGAPS geographically subsected by AFWA");
    genProcessMap.put(250, "Quality Control Display Process (SCIF only)");
  }

  /// levels
  @Override
  public Grib1ParamLevel getParamLevel(Grib1SectionProductDefinition pds) {
    int levelType = pds.getLevelType();
    int pds11 = pds.getLevelValue1();
    int pds12 = pds.getLevelValue2();
    int pds1112 = pds11 << 8 | pds12;

    switch (levelType) {
      case 210:
        return new Grib1ParamLevel(this, levelType, (float) pds1112, GribNumbers.MISSING);

      case 218:
         return new Grib1ParamLevel(this, levelType, (float) pds11 + 200, (float) pds12 + 200);

      case 246:
        return new Grib1ParamLevel(this, levelType, (float) pds1112, GribNumbers.MISSING);

       default:
        return new Grib1ParamLevel(this, pds);
    }
  }

  @Override
  protected VertCoordType getLevelType(int code) {
    if (levelTypesMap == null) makeLevelTypesMap();
    VertCoordType levelType = levelTypesMap.get(code);
    if (levelType != null) return levelType;
    return super.getLevelType(code);
  }
  
  private static void makeLevelTypesMap() {
    levelTypesMap = new HashMap<>(100);
    // (int code, String desc, String abbrev, String units, String datum, boolean isPositiveUp, boolean isLayer)
    levelTypesMap.put(21,  new VertCoordType(21,  "RTNEPH cloud layer", "RTNEPH", "", null, true, true));
    levelTypesMap.put(210, new VertCoordType(210, "Isobaric Surface", "ISBP", "", null, false, false));
    levelTypesMap.put(211, new VertCoordType(211, "Boundary layer cloud bottom level", "BCBL", "", null, false, false));
    levelTypesMap.put(212, new VertCoordType(212, "Boundary layer cloud top level", "BCTL", "", null, false, false));
    levelTypesMap.put(213, new VertCoordType(213, "Boundary layer cloud layer", "BCY", "", null, false, true));
    levelTypesMap.put(214, new VertCoordType(214, "Low cloud bottom level", "LCBL", "", null, false, false));
    levelTypesMap.put(215, new VertCoordType(215, "Low cloud top level", "LCTL", "", null, false, false));
    levelTypesMap.put(216, new VertCoordType(216, "Low cloud layer", "LCY", "", null, false, true));
    levelTypesMap.put(217, new VertCoordType(217, "Highest tropospheric freezing level", "HTFL", "K", null, false, false));
    levelTypesMap.put(218, new VertCoordType(218, "Layer between two temperature levels", "DEGY", "K", null, false, true));
    levelTypesMap.put(222, new VertCoordType(222, "Middle cloud bottom level", "MCBL", "", null, false, false));
    levelTypesMap.put(223, new VertCoordType(223, "Middle cloud top level", "MCTL", "", null, false, false));
    levelTypesMap.put(224, new VertCoordType(224, "Middle cloud layer", "MCY", "", null, false, true));
    levelTypesMap.put(232, new VertCoordType(232, "High cloud bottom level", "HCBL", "", null, false, false));
    levelTypesMap.put(233, new VertCoordType(233, "High cloud top level", "HCTL", "", null, false, false));
    levelTypesMap.put(234, new VertCoordType(234, "High cloud layer", "HCY", "", null, false, true));
    levelTypesMap.put(242, new VertCoordType(242, "Convective cloud bottom level", "CCBL", "", null, false, false));
    levelTypesMap.put(243, new VertCoordType(243, "Convective cloud top level", "CCTL", "", null, false, false));
    levelTypesMap.put(244, new VertCoordType(244, "Convective cloud layer", "CCY", "", null, false, true));
    levelTypesMap.put(246, new VertCoordType(246, "Specified height level above MSL", "HTIO", "km", "msl", false, false));
    levelTypesMap.put(251, new VertCoordType(251, "Layer between ground and 850 hPa level", "PTLR", "", null, false, true));
    levelTypesMap.put(252, new VertCoordType(252, "Layer between lowest soil layer (layer 112) and 800cm", "SBLR", "", null, false, true));
  }

}
