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

package ucar.nc2.grib.grib2.table;

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
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcepLocalParams.class);
  private static final boolean debugOpen = false;
  private static final boolean debug = false;

  private Map<Integer, Table> tableMap = new HashMap<>(30);
  private final String resourcePath;

  NcepLocalParams(String resourcePath) {
    this.resourcePath = resourcePath;
  }

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

  public String getCategory(int discipline, int category) {
    int key = (discipline << 8) + category;
    Table params = tableMap.get( key);
    return (params == null) ? null : params.title;
  }


  Table factory(String path) {
    Table params = new Table();
    if (!params.readParameterTableXml(path)) return null;
    return params;
  }

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

    public Grib2Parameter getParameter(int code) {
      if (paramMap == null) return null;
      return paramMap.get(code);
    }

    private boolean readParameterTableXml(String path) {
      if (debugOpen) System.out.printf("readParameterTableXml table %s%n", path);
      try (InputStream is = GribResourceReader.getInputStream(path)) {
        if (is == null) {
          log.warn("Cant read file " + path);
          return false;
        }

        SAXBuilder builder = new SAXBuilder();
        org.jdom2.Document doc = builder.build(is);
        Element root = doc.getRootElement();
        paramMap = parseXml(root);  // all at once - thread safe
        return true;

      } catch (IOException ioe) {
        ioe.printStackTrace();
        return false;

      } catch (JDOMException e) {
        e.printStackTrace();
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

      } catch (IOException ioe) {
        ioe.printStackTrace();
        return false;

      } catch (JDOMException e) {
        e.printStackTrace();
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
    public HashMap<Integer, Grib2Parameter> parseXml(Element root) {
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
      final StringBuilder sb = new StringBuilder();
      sb.append("NcepTable");
      sb.append("{title='").append(title).append('\'');
      sb.append(", source='").append(source).append('\'');
      sb.append(", tableName='").append(tableName).append('\'');
      sb.append('}');
      return sb.toString();
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


  /*
  public static void main2(String[] args) {

    //Grib2Customizer current = Grib2Customizer.factory(7, -1, -1, -1);
    Grib2Customizer wmo = Grib2Customizer.factory(0, 0, 0, 0, 0);

    File dir = new File("C:\\dev\\github\\thredds\\grib\\src\\main\\resources\\resources\\grib2\\ncep");
    for (File f : dir.listFiles()) {
      if (f.getName().startsWith(match)) {
        NcepLocalParams nt = factory(f.getPath());
        System.out.printf("%s%n", nt);
        compareTables(nt, wmo);
      }
    }
  }

  public static void main3(String[] args) {
    GribTables.Parameter p = getParameter(0, 16, 195);
    System.out.printf("%s%n", p);

    Grib2Customizer tables = Grib2Customizer.factory(7, 0, 0, 0, 0);
    GribTables.Parameter p2 = tables.getParameter(0, 16, 195);
    System.out.printf("%s%n", p2);
  }

  public static void main(String[] args) {
    Map<String, Grib2Parameter> abbrevSet = new HashMap<>(5000);
     File dir = new File("C:\\dev\\github\\thredds\\grib\\src\\main\\resources\\resources\\grib2\\ncep");
     for (File f : dir.listFiles()) {
       if (f.getName().startsWith(match)) {
         NcepLocalParams nt = factory(f.getPath());
         System.out.printf("%s%n", nt);
         for (Grib2Parameter p : nt.getParameters()) {
           if (p.getCategory() < 192 && p.getNumber() < 192) continue;

           if (p.getAbbrev() != null && !p.getAbbrev().equals("Validation")) {
             Grib2Parameter dup = abbrevSet.get(p.getAbbrev());
             if (dup != null) System.out.printf("DUPLICATE %s and %s%n", dup.getId(), p.getId());
             abbrevSet.put(p.getAbbrev(), p);
           }

           if (p.getDescription().length() > 60) System.out.printf("  %d %s = '%s' %s%n", p.getDescription().length(), p.getId(), p.getDescription(), p.getAbbrev());
           else if (p.getDescription().length() > 50) System.out.printf("  50 %s = '%s' %s%n", p.getId(), p.getDescription(), p.getAbbrev());
           else if (p.getDescription().length() > 40) System.out.printf("  40 %s = '%s' %s%n", p.getId(), p.getDescription(), p.getAbbrev());
           else if (p.getDescription().length() > 30) System.out.printf("  30 %s = '%s' %s%n", p.getId(), p.getDescription(), p.getAbbrev());
         }
       }
     }
   }

   */

}
