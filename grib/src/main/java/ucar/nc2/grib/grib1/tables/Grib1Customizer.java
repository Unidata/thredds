/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib1.tables;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import thredds.featurecollection.TimeUnitConverter;
import ucar.nc2.grib.*;
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
 * But other tables are fixed by  center.
 *
 * @author caron
 * @since 1/13/12
 */
public class Grib1Customizer implements GribTables {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1Customizer.class);

  static public Grib1Customizer factory(Grib1Record proto, Grib1ParamTables tables) {
    int center = proto.getPDSsection().getCenter();
    int subcenter = proto.getPDSsection().getSubCenter();
    int version = proto.getPDSsection().getTableVersion();
    return factory(center, subcenter, version, tables);
  }

  static public Grib1Customizer factory(int center, int subcenter, int version, Grib1ParamTables tables) {
    if (center == 7) return new NcepTables(tables);
    else if (center == 9) return new NcepRfcTables(tables);
    else if (center == 34) return new JmaTables(tables);
    else if (center == 57) return new AfwaTables(tables);
    else if (center == 58) return new FnmocTables(tables);
    else if (center == 60) return new NcarTables(tables);
    else return new Grib1Customizer(center, tables);
  }

  static public String getSubCenterNameStatic(int center, int subcenter) {
    Grib1Customizer cust = Grib1Customizer.factory(center, subcenter, 0, null);
    return cust.getSubCenterName( subcenter);
  }

  ///////////////////////////////////////
  private int center;
  private Grib1ParamTables tables;

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
  public String getGeneratingProcessName(int genProcess) {
    return null;
  }


  @Override
  public String getGeneratingProcessTypeName(int genProcess) {
    return null;
  }

  public String getSubCenterName(int subcenter) {
    return CommonCodeTable.getSubCenterName(center, subcenter);
  }

  @Override
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
    if (timeRangeIndicator < 0) return null;
    return Grib1ParamTime.getTimeTypeName(timeRangeIndicator);
  }

  @Override
  public GribStatType getStatType(int timeRangeIndicator) {
    return Grib1WmoTimeType.getStatType(timeRangeIndicator);
  }

  /////////////////////////////////////////
  // level

  public Grib1ParamLevel getParamLevel(Grib1SectionProductDefinition pds) {
    return new Grib1ParamLevel(this, pds);
  }

  public VertCoord.VertUnit getVertUnit(int code) {
    return makeVertUnit(code);
  }

  public boolean isVerticalCoordinate(int levelType) {
    return getLevelUnits(levelType) != null;
  }

  // below are the methods a subclass may need to override for levels

  protected VertCoord.VertUnit makeVertUnit(int code) {
    return getLevelType(code);
  }


  @Override
  public String getLevelNameShort(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    String result = (lt == null) ? null : lt.getAbbrev();
    if (result == null) result = "unknownLevel"+levelType;
    return result;
  }

  public String getLevelDescription(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    return (lt == null) ? null : lt.getDesc();
  }

  public boolean isLayer(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    return (lt == null) ? false : lt.isLayer();
  }

  // only for 3D
  public boolean isPositiveUp(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    return (lt == null) ? false : lt.isPositiveUp();
  }

  // only for 3D
  public String getLevelUnits(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    return (lt == null) ? null : lt.getUnits();
  }

  // only for 3D
  public String getLevelDatum(int levelType) {
    GribLevelType lt = getLevelType(levelType);
    return (lt == null) ? null : lt.getDatum();
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

  static private Map<Integer, GribLevelType> wmoTable3;  // shared by all instances

  protected GribLevelType getLevelType(int code) {
    GribLevelType result = wmoTable3.get(code);
    if (result == null)
      result = new GribLevelType(code, "unknownLayer"+code, null, "unknownLayer"+code, null, false, false);
    return result;
  }

  protected synchronized Map<Integer, GribLevelType> readTable3(String path) {
    try (InputStream is =  GribResourceReader.getInputStream(path)) {
      if (is == null) {
        logger.error("Cant find Table 3 = " + path);
        return null;
      }

      SAXBuilder builder = new SAXBuilder();
      org.jdom2.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      Map<Integer, GribLevelType> result = new HashMap<Integer, GribLevelType>(200);
      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        String abbrev = elem1.getChildText("abbrev");
        String units = elem1.getChildText("units");
        String datum = elem1.getChildText("datum");
        boolean isLayer = elem1.getChild("isLayer") != null;
        boolean isPositiveUp = elem1.getChild("isPositiveUp") != null;
        GribLevelType lt = new GribLevelType(code, desc, abbrev, units, datum, isPositiveUp, isLayer);
        result.put(code, lt);
      }

      return Collections.unmodifiableMap(result);  // all at once - thread safe

    } catch (IOException ioe) {
      logger.error("Cant read NcepLevelTypes = " + path, ioe);
      return null;

    } catch (JDOMException e) {
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
