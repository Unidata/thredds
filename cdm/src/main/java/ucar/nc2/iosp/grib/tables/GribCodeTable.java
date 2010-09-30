/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.nc2.iosp.grib.tables;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import ucar.grib.grib2.ParameterTable;
import ucar.grid.GridParameter;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Read and process WMO grib code tables.
 *
 * @author caron
 * @since Jul 31, 2010
 */

public class GribCodeTable implements Comparable<GribCodeTable> {
  private static Map<String, GribCodeTable> tables;

  public static GribCodeTable.TableEntry getEntry(int discipline, int category, int number) {
    GribCodeTable table = tables.get( getId(discipline, category)) ;
    if (table == null) return null;
    return table.get(number);
  }

  private static String getId(int discipline, int category) {
    return discipline+"."+ category;
  }

  public String name;
  public int m1, m2;
  public boolean isParameter;
  public int discipline = -1;
  public int category = -1;

  public List<TableEntry> entries = new ArrayList<TableEntry>();

  GribCodeTable(String name) {
    this.name = name;
    String[] s = name.split(" ");
    String id = s[2];
    String[] slist2 = id.split("\\.");
    if (slist2.length == 2) {
      m1 = Integer.parseInt(slist2[0]);
      m2 = Integer.parseInt(slist2[1]);
    } else
      System.out.println("HEY bad= %s%n" + name);
  }

  GribCodeTable(String tableName, String subtableName) {
    String[] s = tableName.split(" ");
    String id = s[2];
    String[] slist2 = id.split("\\.");
    if (slist2.length == 2) {
      m1 = Integer.parseInt(slist2[0]);
      m2 = Integer.parseInt(slist2[1]);
    } else
      System.out.println("HEY bad= %s%n" + name);

    this.name = subtableName;
    String[] slist = name.split("[ :]+");
    try {
      for (int i = 0; i < slist.length; i++) {
        if (slist[i].equalsIgnoreCase("discipline"))
          discipline = Integer.parseInt(slist[i + 1]);
        if (slist[i].equalsIgnoreCase("category"))
          category = Integer.parseInt(slist[i + 1]);
      }
    } catch (Exception e) {
    }

    isParameter = (discipline >= 0) && (category >= 0);
  }

  void add(String line, String code, String meaning, String unit, String status) {
    entries.add(new TableEntry(line, code, meaning, unit, status));
  }

  TableEntry get(int value) {
    for (TableEntry p : entries) {
      if (p.start == value) return p;
    }
    return null;
  }

  @Override
  public int compareTo(GribCodeTable o) {
    if (m1 != o.m1) return m1 - o.m1;
    if (m2 != o.m2) return m2 - o.m2;
    if (discipline != o.discipline) return discipline - o.discipline;
    return category - o.category;
  }

  @Override
  public String toString() {
    return "GribCodeTable{" +
        "name='" + name + '\'' +
        ", m1=" + m1 +
        ", m2=" + m2 +
        ", isParameter=" + isParameter +
        ", discipline=" + discipline +
        ", category=" + category +
        '}';
  }

  // remove () for the following:
  private static int[] badones = new int[] {
          0, 1, 51,
          0, 6, 25,
          0, 19, 22,
          0, 191, 0,
          1, 0, 0,
          1, 0, 1,
          1, 1, 0,
          1, 1, 1,
          1, 1, 2,
          2, 0, 0,
          10, 191, 0};

  private boolean remove(TableEntry entry) {
    for (int i=0; i < badones.length; i+=3) {
      if (discipline == badones[i] && category == badones[i+1] && entry.number == badones[i+2]) return true;
    }
    return false;
  }

  public class TableEntry implements Comparable<TableEntry> {
    public int start, stop, line;
    public int number = -1;
    public String code, meaning, name, unit, status;

    TableEntry(String line, String code, String meaning, String unit, String status) {
      this.line = Integer.parseInt(line);
      this.code = code;
      this.meaning = meaning;
      this.status = status;

      try {
        int pos = code.indexOf('-');
        if (pos > 0) {
          start = Integer.parseInt(code.substring(0, pos));
          String stops = code.substring(pos + 1);
          stop = Integer.parseInt(stops);
        } else {
          start = Integer.parseInt(code);
          stop = start;
          number = start;
        }
      } catch (Exception e) {
        start = -1;
        stop = 0;
      }

      if (isParameter) {
        // some need the () comment removed - must be hand specified (!)
        if (remove(this)) {
          int pos1 = meaning.indexOf('(');
          int pos2 = meaning.indexOf(')');
          if ((pos1 > 0) && (pos2 > 0))
            meaning = meaning.substring(0, pos1).trim(); // assume () is at the end od string
        }

        // meaning = StringUtil.replace(meaning, '-', "_");
        meaning = StringUtil.replace(meaning, '/', "-");
        meaning = StringUtil.replace(meaning, '.', "p");
        meaning = StringUtil.remove(meaning, '(');
        meaning = StringUtil.remove(meaning, ')');
        this.name = N3iosp.createValidNetcdf3ObjectName(meaning);

        // massage units
        if (unit != null) {
          if (unit.equalsIgnoreCase("Proportion")) unit = "";
          if (unit.equalsIgnoreCase("Numeric")) unit = "";
        }
        this.unit = unit;
      }
   }

    @Override
    public int compareTo(TableEntry o) {
      return start - o.start;
    }

    @Override
    public String toString() {
      return "TableEntry{" +
              ", discipline=" + discipline +
              ", category=" + category +
              ", number=" + number +
              ", org='" + meaning + '\'' +
              ", name='" + name + '\'' +
              ", unit='" + unit + '\'' +
              ", status='" + status + '\'' +
              '}';
    }
  }

  //////////////////////////////////////////////////////////////////////

  /*
  <ForExport_CodeFlag_E>
    <No>645</No>
    <TableTitle_E>Code table 4.2 - Parameter number by product discipline and parameter category</TableTitle_E>
    <TableSubTitle_E>Product discipline 0 - Meteorological products, parameter category 19: physical atmospheric
      properties
    </TableSubTitle_E>
    <CodeFlag>13</CodeFlag>
    <Meaning_E>Contrail intensity</Meaning_E>
    <AsciiUnit_x002F_Description_E>(Code table 4.210)</AsciiUnit_x002F_Description_E>
    <Status>Operational</Status>
  </ForExport_CodeFlag_E>
  */

  static private List<GribCodeTable> readGribCodes(InputStream ios) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(ios);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    Element root = doc.getRootElement();

    Map<String, GribCodeTable> map = new HashMap<String, GribCodeTable>();
    tables = new HashMap<String, GribCodeTable>();

    List<Element> featList = root.getChildren("ForExport_CodeFlag_E");
    for (Element elem : featList) {
      String line = elem.getChildTextNormalize("No");
      String tableName = elem.getChildTextNormalize("TableTitle_E");
      String code = elem.getChildTextNormalize("CodeFlag");
      String meaning = elem.getChildTextNormalize("Meaning_E");

      GribCodeTable ct = map.get(tableName);
      if (ct == null) {
        ct = new GribCodeTable(tableName);
        map.put(tableName, ct);
      }

      Element unitElem = elem.getChild("AsciiUnit_x002F_Description_E");
      String unit = (unitElem == null) ? null : unitElem.getTextNormalize();

      Element statusElem = elem.getChild("Status");
      String status = (statusElem == null) ? null : statusElem.getTextNormalize();

      Element subtableElem = elem.getChild("TableSubTitle_E");
      if (subtableElem != null) {
        String subTableName = subtableElem.getTextNormalize();
        GribCodeTable cst = map.get(subTableName);
        if (cst == null) {
          cst = new GribCodeTable(tableName, subTableName);
          map.put(subTableName, cst);
          tables.put(getId(cst.discipline, cst.category), cst);
        }
        cst.add(line, code, meaning, unit, status);

      } else {
        ct.add(line, code, meaning, unit, status);
      }

    }

    ios.close();

    List<GribCodeTable> tlist = new ArrayList<GribCodeTable>(map.values());
    Collections.sort(tlist);
    for (GribCodeTable gt : tlist)
      Collections.sort(gt.entries);

    return tlist;
  }

  static public Map<String, GribCodeTable> readGribCodes() throws IOException {
    Class c = GribCodeTable.class;
    InputStream in = c.getResourceAsStream(resourceName);
    if (in == null) {
      System.out.printf("cant open %s%n", resourceName);
      return null;
    }
    
    List<GribCodeTable> tlist = readGribCodes(in);
    Map<String, GribCodeTable> map = new HashMap<String, GribCodeTable>( 2* tlist.size());
    for (GribCodeTable ct : tlist) {
      String id = ct.m1+"."+ct.m2;
      if (ct.isParameter)
        id += "."+ct.discipline+"."+ct.category;
      map.put(id, ct);
    }
    return map;
  }

  public static List<GribCodeTable> getWmoStandard() throws IOException {
    Class c = GribCodeTable.class;
    InputStream in = c.getResourceAsStream(resourceName);
    if (in == null) {
      System.out.printf("cant open %s%n", resourceName);
      return null;
    }
    try {
      return readGribCodes(in);
    } finally {
      in.close();
    }
  }

  static String resourceName = "/resources/grib/wmo/GRIB2_5_2_0_CodeFlag_E.xml";
  static boolean showDiff = true;
  public static void main(String arg[]) throws IOException {
    //String filename = "C:\\docs\\dataFormats\\grib\\GRIB2_5_2_0_xml\\wmoGribCodes.xml";
    //List<GribCodeTable> tlist = readGribCodes(new FileInputStream(filename));

    Class c = GribCodeTable.class;
    InputStream in = c.getResourceAsStream(resourceName);
    if (in == null) {
      System.out.printf("cant open %s%n", resourceName);
      return;
    }
    List<GribCodeTable> tlist = readGribCodes(in);

    for (GribCodeTable gt : tlist) {
      System.out.printf("%d.%d (%d,%d) %s %n", gt.m1, gt.m2, gt.discipline, gt.category, gt.name);
      for (TableEntry p : gt.entries) {
        System.out.printf("  %s (%d-%d) = %s %n", p.code, p.start, p.stop, p.meaning);
      }
    }

    if (showDiff) {
      int total = 0;
      int nsame = 0;
      int nsameIgn = 0;
      int ndiff = 0;
      int unknown = 0;

      System.out.printf("DIFFERENCES with current parameter table");
      for (GribCodeTable gt : tlist) {
        if (!gt.isParameter) continue;
        for (TableEntry p : gt.entries) {
          if (p.meaning.equalsIgnoreCase("Missing")) continue;
          if (p.start != p.stop) continue;

          GridParameter gp = ParameterTable.getParameter(gt.discipline, gt.category, p.start);
          String paramDesc = gp.getDescription();
          if (paramDesc.startsWith("Unknown")) unknown++;
          boolean same = paramDesc.equals(p.meaning);
          if (same) nsame++;
          boolean sameIgnore = paramDesc.equalsIgnoreCase(p.meaning);
          if (sameIgnore) nsameIgn++;
          else ndiff++;
          total++;
          String state = same ? "  " : (sameIgnore ? "* " : "**");
          System.out.printf("%s%d %d %d%n %s%n %s%n", state, gt.discipline, gt.category, p.start, p.meaning, paramDesc);
        }
      }
      System.out.printf("Total=%d same=%d sameIgn=%d dif=%d unknown=%d%n", total, nsame, nsameIgn, ndiff, unknown);
    }
  }
}
