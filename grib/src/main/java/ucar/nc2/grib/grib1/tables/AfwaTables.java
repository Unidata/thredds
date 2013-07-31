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

  AfwaTables(Grib1ParamTables tables) {
    super(57, tables);
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
