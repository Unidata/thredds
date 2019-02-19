/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2.table;

import com.google.common.base.MoreObjects;
import javax.annotation.Nullable;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.grib.grib2.Grib2Parameter;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Read NCEP parameter tables. inner class Table for each discipline, category
 *
 * @author caron
 * @since 1/9/12
 */
class NcepLocalParams {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcepLocalParams.class);
  private static final boolean debugOpen = false;
  private static final boolean debug = false;

  private Map<Integer, Table> tableMap = new HashMap<>(30);
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
  Table factory(String path) {
    Table params = new Table();
    if (!params.readParameterTableXml(path)) return null;
    return params;
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
  // one table for the discipline, category
  static class Table {

    private String title;
    private String source;
    private String tableName;
    private int discipline, category;
    private Map<Integer, Grib2Parameter> paramMap;

    public List<Grib2Parameter> getParameters() {
      List<Grib2Parameter> result = new ArrayList<>(paramMap.values());
      Collections.sort(result);
      return result;
    }

    @Nullable
    public Grib2Parameter getParameter(int code) {
      return paramMap.get(code);
    }

    private boolean readParameterTableXml(String path) {
      if (debugOpen) System.out.printf("readParameterTableXml table %s%n", path);
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
      if (debugOpen) System.out.printf("readParameterTableFromResource from resource %s%n", resource);
      ClassLoader cl = this.getClass().getClassLoader();
      try (InputStream is = cl.getResourceAsStream(resource)) {
        if (is == null) {
          log.info("Cant read resource " + resource);
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
      int pos = tableName.indexOf(match);
      String dc = tableName.substring(pos + match.length());
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
        if (debug) System.out.printf(" %s%n", parameter);
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

  private static final String match = "Table4.2.";

  /*
  //////////////////////////////////////////////////////////////////////////
  // LOOK - compare to Grib2TablesViewer

  private static void compareTables(NcepLocalParams test, Grib2Customizer current) {
    Formatter f = new Formatter();
    //f.format("Table 1 = %s%n", test.tableName);
    //f.format("Table 2 = %s%n", "currentNcep");

    int extra = 0;
    int udunits = 0;
    int conflict = 0;
    // f.format("Table 1 : %n");
    for (Grib2Parameter p1 : test.getParameters()) {
      Grib2Customizer.Parameter  p2 = current.getParameter(p1.getDiscipline(), p1.getCategory(), p1.getNumber());
      if (p2 == null) {
        extra++;
        if (p1.getNumber() < 192) f.format("  WMO missing %s%n", p1);

      } else {
        String p1n = Util.cleanName(StringUtil2.substitute(p1.getName(), "-", " "));
        String p2n = Util.cleanName(StringUtil2.substitute(p2.getName(), "-", " "));

        if (!p1n.equalsIgnoreCase(p2n) ||
           (p1.getNumber() >= 192 && !p1.getAbbrev().equals(p2.getAbbrev()))) {
          f.format("  p1=%10s %40s %15s  %15s%n", p1.getId(), p1.getName(), p1.getUnit(), p1.getAbbrev());
          f.format("  p2=%10s %40s %15s  %15s%n%n", p2.getId(), p2.getName(), p2.getUnit(), p2.getAbbrev());
          conflict++;
        }

        if (!p1.getUnit().equalsIgnoreCase(p2.getUnit())) {
          String cu1 = Util.cleanUnit(p1.getUnit());
          String cu2 = Util.cleanUnit(p2.getUnit());

          // eliminate common non-udunits
          boolean isUnitless1 = isUnitless(cu1);
          boolean isUnitless2 = isUnitless(cu2);

          if (isUnitless1 != isUnitless2) {
            f.format("  ud=%10s %s != %s for %s (%s)%n%n", p1.getId(), cu1, cu2, p1.getId(), p1.getName());
            udunits++;

          } else if (!isUnitless1) {

            try {
              SimpleUnit su1 = SimpleUnit.factoryWithExceptions(cu1);
              if (!su1.isCompatible(cu2)) {
                f.format("  ud=%10s %s (%s) != %s for %s (%s)%n%n", p1.getId(), cu1, su1, cu2, p1.getId(), p1.getName());
                udunits++;
              }
            } catch (Exception e) {
              f.format("  udunits cant parse=%10s %15s %15s%n", p1.getId(), cu1, cu2);
            }
          }

        }
      }

    }
    f.format("Conflicts=%d extra=%d udunits=%d%n%n", conflict, extra, udunits);

    /* extra = 0;
    f.format("Table 2 : %n");
    for (Object t : current.getParameters()) {
      Grib2Tables.Parameter p2 = (Grib2Tables.Parameter) t;
      Grib2Parameter  p1 = test.getParameter(p2.getNumber());
      if (p1 == null) {
        extra++;
        f.format(" Missing %s in table 1%n", p2);
      }
    }
    f.format("%nextra=%d%n%n", extra);
    System.out.printf("%s%n", f);
  }  */

  static boolean isUnitless(String unit) {
    if (unit == null) return true;
    String munge = unit.toLowerCase().trim();
    munge = StringUtil2.remove(munge, '(');
    return munge.length()  == 0 ||
            munge.startsWith("numeric") || munge.startsWith("non-dim") || munge.startsWith("see") ||
            munge.startsWith("proportion") || munge.startsWith("code") || munge.startsWith("0=") ||
            munge.equals("1") ;
  }
}
