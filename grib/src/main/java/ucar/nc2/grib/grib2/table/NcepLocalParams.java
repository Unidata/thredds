/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2.table;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.Grib2Parameter;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Read NCEP parameter tables.
 * There will be a Table for each discipline, category
 *
 * @author caron
 * @since 1/9/12
 */
class NcepLocalParams {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NcepLocalParams.class);
  private static final String MATCH = "Table4.2.";

  private static final boolean debugOpen = false;
  private static final boolean debug = false;

  private final Map<Integer, Table> tableMap = new HashMap<>(30);
  private final String resourcePath;

  NcepLocalParams(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  @Nullable
  public Grib2Parameter getParameter(int discipline, int category, int number) {
    int key = (discipline << 8) + category;
    Table params = tableMap.get( key);
    if (params == null) {
      params = factory( discipline, category);
      if (params == null) return null;
      tableMap.put(key, params);
    }
    return params.getParameter(number);
  }

  @Nullable
  public String getCategory(int discipline, int category) {
    int key = (discipline << 8) + category;
    Table params = tableMap.get( key);
    return (params == null) ? null : params.title;
  }

  @Nullable
  ImmutableList<GribTables.Parameter> getParameters(String path) {
    Table table = new Table();
    if (!table.readParameterTableXml(path)) return null;
    return table.getParameters();
  }

  @Nullable
  private Table factory(int discipline, int category) {
    Table params = new Table();
    if (!params.readParameterTableFromResource(getTablePath(discipline, category))) return null;
    return params;
  }

  String getTablePath(int discipline, int category) {
    return resourcePath + "Table4.2."+discipline+"."+category+".xml";
  }

  ////////////////////////////////////////////////////
  // one table for each discipline, category
  static class Table {

    private String title;
    private String source;
    private String tableName;
    private int discipline, category;
    private Map<Integer, Grib2Parameter> paramMap;

    private ImmutableList<GribTables.Parameter> getParameters() {
      return paramMap.values().stream().sorted().collect(ImmutableList.toImmutableList());
    }

    @Nullable
    public Grib2Parameter getParameter(int code) {
      return paramMap.get(code);
    }

    private boolean readParameterTableXml(String path) {
      if (debugOpen) logger.debug("readParameterTableXml table %s%n", path);
      try (InputStream is = GribResourceReader.getInputStream(path)) {
        SAXBuilder builder = new SAXBuilder();
        org.jdom2.Document doc = builder.build(is);
        Element root = doc.getRootElement();
        paramMap = parseXml(root);  // all at once - thread safe
        return true;

      } catch (IOException | JDOMException ioe) {
        ioe.printStackTrace();
        return false;
      }
    }

    private boolean readParameterTableFromResource(String resource) {
      if (debugOpen) logger.debug("readParameterTableFromResource from resource %s%n", resource);
      ClassLoader cl = this.getClass().getClassLoader();
      try (InputStream is = cl.getResourceAsStream(resource)) {
        if (is == null) {
          logger.info("Cant read resource " + resource);
          return false;
        }
        SAXBuilder builder = new SAXBuilder();
        org.jdom2.Document doc = builder.build(is);
        Element root = doc.getRootElement();
        paramMap = parseXml(root);  // all at once - thread safe
        return true;

      } catch (IOException | JDOMException ioe) {
        ioe.printStackTrace();
        return false;
      }
    }

    /*
    <parameterMap>
    <table>Table4.2.0.0</table>
    <title>Temperature</title>
    <source>http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-0.shtml</source>
    <parameter code="0">
      <shortName>TMP</shortName>
      <description>Temperature</description>
      <units>K</units>
    </parameter>
     */
    HashMap<Integer, Grib2Parameter> parseXml(Element root) {
      tableName = root.getChildText("table");
      title = root.getChildText("title");
      source = root.getChildText("source");

      // Table4.2.0.0
      int pos = tableName.indexOf(MATCH);
      String dc = tableName.substring(pos + MATCH.length());
      String[] dcs = dc.split("\\.");
      discipline = Integer.parseInt(dcs[0]);
      category = Integer.parseInt(dcs[1]);

      HashMap<Integer, Grib2Parameter> result = new HashMap<>();
      List<Element> params = root.getChildren("parameter");
      for (Element elem : params) {
        int code = Integer.parseInt(elem.getAttributeValue("code"));
        String abbrev = elem.getChildText("shortName");
        String desc = elem.getChildText("description");
        String units = elem.getChildText("units");
        if (units == null) units = "";

        String name;
        if (desc.length() > 80 && abbrev != null && !abbrev.equalsIgnoreCase("Validation")) {
          name = abbrev;
        } else {
          name = desc;
          desc = null;
        }

        //   public Grib2Parameter(int discipline, int category, int number, String name, String unit, String abbrev) {
        Grib2Parameter parameter = new Grib2Parameter(discipline, category, code, name, units, abbrev, desc);
        result.put(parameter.getNumber(), parameter);
        if (debug) logger.debug(" %s%n", parameter);
      }
      return result;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("title", title)
          .add("source", source)
          .add("tableName", tableName)
          .add("discipline", discipline)
          .add("category", category)
          .toString();
    }
  }

}
