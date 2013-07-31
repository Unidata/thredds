package ucar.nc2.grib.grib1.tables;

import ucar.nc2.grib.GribLevelType;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.grib1.Grib1ParamLevel;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;

import java.util.HashMap;

/**
 * Air Force Weather Center (center 57)
 *
 * @author caron
 * @since 7/31/13
 */
public class AfwaTables extends Grib1Customizer {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AfwaTables.class);

  private static HashMap<Integer, GribLevelType> levelTypesMap;  // shared by all instances
  private static HashMap<Integer, String> genProcessMap;  // shared by all instances
  private static HashMap<Integer, String> subcenterMap;  // shared by all instances

  AfwaTables(Grib1ParamTables tables) {
    super(57, tables);
  }

  // subcenters

  @Override
  public String getSubCenterName(int subcenter) {
    if (subcenterMap == null) makeSubcenterMap();
    return subcenterMap.get(subcenter);
  }

  static private void makeSubcenterMap() {
    subcenterMap = new HashMap<Integer, String>(100);

    subcenterMap.put(0, "AFWA Primary Table");
    subcenterMap.put(1, "	AFWA Numerical Weather Prediction Models Products");
    subcenterMap.put(2, "AFWA Special Environmental Models Products");
    subcenterMap.put(3, "AFWA Space Products");
    subcenterMap.put(4, "AFWA Ensemble Products");
    subcenterMap.put(6, "AFWA Aerosol Products");
    subcenterMap.put(7, "CDFS-II Aerosol Model");
    subcenterMap.put(8, "AFWA Volcano Products");
    subcenterMap.put(10, "14 WS (Formerly Air Force Combat Climatology Center (AFCCC))");
    subcenterMap.put(11, "14 WS Numerical Weather Prediction Models Products");
    subcenterMap.put(15, "15 OWS (Scott)");
    subcenterMap.put(17, "17 OWS (Hickam)");
    subcenterMap.put(20, "20 OWS (Yokota)");
    subcenterMap.put(21, "21 OWS (Sembach)");
    subcenterMap.put(25, "25 OWS (Davis-Monthan)");
    subcenterMap.put(26, "26 OWS (Barksdale)");
    subcenterMap.put(27, "27 OWS (Offutt)");
    subcenterMap.put(28, "28 OWS (Shaw)");
    subcenterMap.put(29, "Air Force Tactical Application Center (AFTAC) ");
    subcenterMap.put(90, "AFWA 4DF 1");
    subcenterMap.put(91, "AFWA 4DF 2");
    subcenterMap.put(92, "AFWA 4DF 3");
    subcenterMap.put(93, "AFWA 4DF 4");
    subcenterMap.put(94, "AFWA 4DF 5");
    subcenterMap.put(110, "Air Force Research Lab (AFRL) Office of Research and Applications");
    subcenterMap.put(130, "Patrick AFB OWS Eastern Test Range");
    subcenterMap.put(150, "IPDS Satellite");
  }

  // gen process
  @Override
  public String getGeneratingProcessName(int genProcess) {
    if (genProcessMap == null) makeGenProcessMap();
    return genProcessMap.get(genProcess);
  }

  static private void makeGenProcessMap() {
    genProcessMap = new HashMap<Integer, String>(100);
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
  protected VertCoord.VertUnit makeVertUnit(int code) {
    GribLevelType lt = getLevelType(code);
    return (lt != null) ? lt  : super.makeVertUnit(code);
  }

  @Override
  public String getLevelNameShort(int code) {
    GribLevelType lt = getLevelType(code);
    return (lt == null) ? super.getLevelNameShort(code) : lt.getAbbrev();
  }

  @Override
  public String getLevelDescription(int code) {
    GribLevelType lt = getLevelType(code);
    return (lt == null) ? super.getLevelDescription(code) : lt.getDesc();
  }

  @Override
  public String getLevelUnits(int code) {
    GribLevelType lt = getLevelType(code);
    return (lt == null) ? super.getLevelUnits(code) : lt.getUnits();
  }

  @Override
  public boolean isLayer(int code) {
    GribLevelType lt = getLevelType(code);
    return (lt == null) ? super.isLayer(code) : lt.isLayer();
  }

  @Override
  public boolean isPositiveUp(int code) {
    GribLevelType lt = getLevelType(code);
    return (lt == null) ? super.isPositiveUp(code) : lt.isPositiveUp();
  }

  @Override
  public String getLevelDatum(int code) {
    GribLevelType lt = getLevelType(code);
    return (lt == null) ? super.getLevelDatum(code) : lt.getDatum();
  }

  protected GribLevelType getLevelType(int code) {
    if (levelTypesMap == null) makeLevelTypesMap();
    return levelTypesMap.get(code);
  }
  
  static private void makeLevelTypesMap() {
    levelTypesMap = new HashMap<Integer, GribLevelType>(100);
    // (int code, String desc, String abbrev, String units, String datum, boolean isPositiveUp, boolean isLayer)
    levelTypesMap.put(21,  new GribLevelType(21,  "RTNEPH cloud layer", "RTNEPH", "", null, true, true));
    levelTypesMap.put(210, new GribLevelType(210, "Isobaric Surface", "ISBP", "", null, false, false));
    levelTypesMap.put(211, new GribLevelType(211, "Boundary layer cloud bottom level", "BCBL", "", null, false, false));
    levelTypesMap.put(212, new GribLevelType(212, "Boundary layer cloud top level", "BCTL", "", null, false, false));
    levelTypesMap.put(213, new GribLevelType(213, "Boundary layer cloud layer", "BCY", "", null, false, true));
    levelTypesMap.put(214, new GribLevelType(214, "Low cloud bottom level", "LCBL", "", null, false, false));
    levelTypesMap.put(215, new GribLevelType(215, "Low cloud top level", "LCTL", "", null, false, false));
    levelTypesMap.put(216, new GribLevelType(216, "Low cloud layer", "LCY", "", null, false, true));
    levelTypesMap.put(217, new GribLevelType(217, "Highest tropospheric freezing level", "HTFL", "K", null, false, false));
    levelTypesMap.put(218, new GribLevelType(218, "Layer between two temperature levels", "DEGY", "K", null, false, true));
    levelTypesMap.put(222, new GribLevelType(222, "Middle cloud bottom level", "MCBL", "", null, false, false));
    levelTypesMap.put(223, new GribLevelType(223, "Middle cloud top level", "MCTL", "", null, false, false));
    levelTypesMap.put(224, new GribLevelType(224, "Middle cloud layer", "MCY", "", null, false, true));
    levelTypesMap.put(232, new GribLevelType(232, "High cloud bottom level", "HCBL", "", null, false, false));
    levelTypesMap.put(233, new GribLevelType(233, "High cloud top level", "HCTL", "", null, false, false));
    levelTypesMap.put(234, new GribLevelType(234, "High cloud layer", "HCY", "", null, false, true));
    levelTypesMap.put(242, new GribLevelType(242, "Convective cloud bottom level", "CCBL", "", null, false, false));
    levelTypesMap.put(243, new GribLevelType(243, "Convective cloud top level", "CCTL", "", null, false, false));
    levelTypesMap.put(244, new GribLevelType(244, "Convective cloud layer", "CCY", "", null, false, true));
    levelTypesMap.put(246, new GribLevelType(246, "Specified height level above MSL", "HTIO", "km", "msl", false, false));
    levelTypesMap.put(251, new GribLevelType(251, "Layer between ground and 850 hPa level", "PTLR", "", null, false, true));
    levelTypesMap.put(252, new GribLevelType(252, "Layer between lowest soil layer (layer 112) and 800cm", "SBLR", "", null, false, true));
  }

}
