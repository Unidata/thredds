/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1.tables;

import javax.annotation.Nullable;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import thredds.featurecollection.TimeUnitConverter;
import ucar.nc2.grib.*;
import ucar.nc2.grib.coord.VertCoordType;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.wmo.CommonCodeTable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interprets grib1 info in a way that may be customized.
 * This class handles the default case, using only standard WMO tables.
 * Subclasses override as needed.
 *
 * Bit of a contradiction, since getParamter() allows different center, subcenter, version (the version is for sure needed)
 * But other tables are fixed by center.
 *
 * @author caron
 * @since 1/13/12
 */
public class Grib1Customizer implements GribTables {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1Customizer.class);

  public static Grib1Customizer factory(Grib1Record proto, Grib1ParamTables tables) {
    int center = proto.getPDSsection().getCenter();
    int subcenter = proto.getPDSsection().getSubCenter();
    int version = proto.getPDSsection().getTableVersion();
    return factory(center, subcenter, version, tables);
  }

  public static Grib1Customizer factory(int center, int subcenter, int version, Grib1ParamTables tables) {
    switch (center) {
      case 7: return new NcepTables(tables);
      case 9: return new NcepRfcTables(tables);
      case 34: return new JmaTables(tables);
      case 57: return new AfwaTables(tables);
      case 58: return new FnmocTables(tables);
      case 60: return new NcarTables(tables);
      default: return new Grib1Customizer(center, tables);
    }
  }

  public static String getSubCenterNameStatic(int center, int subcenter) {
    Grib1Customizer cust = Grib1Customizer.factory(center, subcenter, 0, null);
    return cust.getSubCenterName( subcenter);
  }

  ///////////////////////////////////////
  private final int center;
  private final Grib1ParamTables tables;

  protected Grib1Customizer(int center, Grib1ParamTables tables) {
    this.center = center;
    this.tables = (tables == null) ? new Grib1ParamTables() : tables;

    synchronized (Grib1Customizer.class) {
      if (wmoTable3 == null)
        wmoTable3 = readTable3("resources/grib1/wmoTable3.xml");
    }
  }

  public int getCenter() {
    return center;
  }

  public Grib1Parameter getParameter(int center, int subcenter, int tableVersion, int param_number) {
    return tables.getParameter(center, subcenter, tableVersion, param_number);
  }

  @Override
  @Nullable
  public String getGeneratingProcessName(int genProcess) {
    return null;
  }

  @Override
  public String getGeneratingProcessTypeName(int genProcess) {
    return null;
  }

  @Nullable
  public String getSubCenterName(int subcenter) {
    return CommonCodeTable.getSubCenterName(center, subcenter);
  }

  @Override
  @Nullable
  public String getSubCenterName(int center, int subcenter) {
    return CommonCodeTable.getSubCenterName(center, subcenter);
  }

  ///////////////////////////////////////////////////
  // time

  public Grib1ParamTime getParamTime(Grib1SectionProductDefinition pds) {
    return new Grib1ParamTime(this, pds);
  }

  // code table 5
  public String getTimeTypeName(int timeRangeIndicator) {
    return Grib1ParamTime.getTimeTypeName(timeRangeIndicator);
  }

  @Override
  @Nullable
  public GribStatType getStatType(int timeRangeIndicator) {
    return Grib1WmoTimeType.getStatType(timeRangeIndicator);
  }

  /////////////////////////////////////////
  // level

  public Grib1ParamLevel getParamLevel(Grib1SectionProductDefinition pds) {
    return new Grib1ParamLevel(this, pds);
  }

  @Override
  public VertCoordType getVertUnit(int code) {
    return makeVertUnit(code);
  }

  public boolean isVerticalCoordinate(int levelType) {
    return getLevelUnits(levelType) != null;
  }

  // below are the methods a subclass may need to override for levels

  protected VertCoordType makeVertUnit(int code) {
    return getLevelType(code);
  }

  @Override
  public String getLevelNameShort(int levelType) {
    VertCoordType lt = getLevelType(levelType);
    String result = lt.getAbbrev();
    if (result == null) result = "unknownLevel"+levelType;
    return result;
  }

  public String getLevelDescription(int levelType) {
    VertCoordType lt = getLevelType(levelType);
    return lt.getDesc();
  }

  public boolean isLayer(int levelType) {
    VertCoordType lt = getLevelType(levelType);
    return lt.isLayer();
  }

  // only for 3D
  public boolean isPositiveUp(int levelType) {
    VertCoordType lt = getLevelType(levelType);
    return lt.isPositiveUp();
  }

  // only for 3D
  public String getLevelUnits(int levelType) {
    VertCoordType lt = getLevelType(levelType);
    return lt.getUnits();
  }

  // only for 3D
  public String getLevelDatum(int levelType) {
    VertCoordType lt = getLevelType(levelType);
    return lt.getDatum();
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

  private static Map<Integer, VertCoordType> wmoTable3;  // shared by all instances

  protected VertCoordType getLevelType(int code) {
    VertCoordType result = wmoTable3.get(code);
    if (result == null)
      result = new VertCoordType(code, "unknownLayer"+code, null, "unknownLayer"+code, null, false, false);
    return result;
  }

  @Nullable
  protected synchronized Map<Integer, VertCoordType> readTable3(String path) {
    try (InputStream is =  GribResourceReader.getInputStream(path)) {
      SAXBuilder builder = new SAXBuilder();
      org.jdom2.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      Map<Integer, VertCoordType> result = new HashMap<>(200);
      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        String abbrev = elem1.getChildText("abbrev");
        String units = elem1.getChildText("units");
        String datum = elem1.getChildText("datum");
        boolean isLayer = elem1.getChild("isLayer") != null;
        boolean isPositiveUp = elem1.getChild("isPositiveUp") != null;
        VertCoordType lt = new VertCoordType(code, desc, abbrev, units, datum, isPositiveUp, isLayer);
        result.put(code, lt);
      }

      return Collections.unmodifiableMap(result);  // all at once - thread safe
    } catch (IOException | JDOMException e) {
      logger.error("Cant parse NcepLevelTypes = " + path, e);
      return null;
    }
  }

  public static void main(String[] args) {
    Grib1Customizer cust = new Grib1Customizer(0, null);
    String units = cust.getLevelUnits(110);
    assert units != null;
  }
}
