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
package ucar.nc2.iosp.bufr.tables;

import org.jdom.input.SAXBuilder;
import org.jdom.Element;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;

import ucar.unidata.util.StringUtil;

/**
 * Read Code / Flag tables.
 *
 * @author caron
 * @since Jul 12, 2008
 */
public class CodeFlagTables {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CodeFlagTables.class);
  static Map<Short, CodeFlagTables> tableMap;

  static public CodeFlagTables getTable(short id) {
    if (tableMap == null) init();
    return tableMap.get(id);
  }

  static public boolean hasTable(short id) {
    if (tableMap == null) init();
    CodeFlagTables result = tableMap.get(id);
    return result != null;
  }

  static private void init() {
    tableMap = new HashMap<Short, CodeFlagTables>(300);
    init2(tableMap);
  }

  static private void initOld(Map<Short, CodeFlagTables> table) {
    String filename = BufrTables.RESOURCE_PATH + "wmo/Code-FlagTables.xml";
    InputStream is = CodeFlagTables.class.getResourceAsStream(filename);

    try {
      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document tdoc = builder.build(is);
      org.jdom.Element root = tdoc.getRootElement();

      for (Element elem : (List<Element>) root.getChildren("table")) {

        String kind = elem.getAttributeValue("kind");
        if ((kind == null) || !kind.equals("code")) {
          continue;
        }

        List<Element> cElems = (List<Element>) elem.getChildren("code");
        if (cElems.size() == 0) {
          continue;
        }

        String name = elem.getAttributeValue("name");
        String desc = elem.getAttributeValue("desc");
        CodeFlagTables ct = new CodeFlagTables(getFxy(name), desc);
        table.put(ct.fxy, ct);
        // System.out.printf(" added %s == %s %n", ct.id, desc);

        for (Element cElem : cElems) {
          String valueS = cElem.getAttributeValue("value").trim();
          String text = cElem.getText();
          if (text.toLowerCase().startsWith("reserved"))
            continue;
          else if (text.toLowerCase().startsWith("not used"))
            continue;
          else {
            try {
              int value = Integer.parseInt(valueS);
              ct.addValue(value, text);
            } catch (NumberFormatException e) {
              log.warn("NumberFormatException on '" + valueS + "' for CodeTable " + name + " in " + filename);
            }
          }
        }
      }

    } catch (Exception e) {
      log.error("Can't read BUFR code table " + filename, e);
    }
  }

  static private short getFxy(String name) {
    try {
      String[] tok = name.split(" ");
      int f = (tok.length > 0) ? Integer.parseInt(tok[0]) : 0;
      int x = (tok.length > 1) ? Integer.parseInt(tok[1]) : 0;
      int y = (tok.length > 2) ? Integer.parseInt(tok[2]) : 0;
      return (short) ((f << 14) + (x << 8) + (y));
    } catch (NumberFormatException e) {
      log.warn("Illegal table name=" + name);
      return 0;
    }
  }

  static private boolean showReadErrs = false, showNameDiff = false;

  static private void init2(Map<Short, CodeFlagTables> table) {
    String filename = BufrTables.RESOURCE_PATH + "wmo/BC_CodeFlagTable.csv";
    BufferedReader dataIS = null;

    try {
      InputStream is = CodeFlagTables.class.getResourceAsStream(filename);
      dataIS = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF8")));
      int count = 0;
      while (true) {
        String line = dataIS.readLine();
        if (line == null) break;
        if (line.startsWith("#")) continue;
        count++;

        if (count == 1) { // skip first line - its the header
          if (showReadErrs) System.out.println("header line == " + line);
          continue;
        }

        // any commas that are embedded in quotes - replace with blanks for now so split works
        int pos1 = line.indexOf('"');
        if (pos1 >= 0) {
          int pos2 = line.indexOf('"', pos1 + 1);
          StringBuffer sb = new StringBuffer(line);
          for (int i = pos1; i < pos2; i++)
            if (sb.charAt(i) == ',') sb.setCharAt(i, ' ');
          line = sb.toString();
        }

        String[] flds = line.split(",");
        if (flds.length < 4) {
          if (showReadErrs) System.out.printf("%d BAD split == %s%n", count, line);
          continue;
        }

        // SNo,FXY,CodeFigure,enDescription1,enDescription2,enDescription3
        int fldidx = 0;
        try {
          int sno = Integer.parseInt(flds[fldidx++].trim());
          int xy = Integer.parseInt(flds[fldidx++].trim());
          int no = -1;
          try {
            no = Integer.parseInt(flds[fldidx++].trim());
          } catch (Exception e) {
            if (showReadErrs) System.out.printf("%d skip == %s%n", count, line);
            continue;
          }
          String name = StringUtil.remove(flds[fldidx++], '"');
          String nameLow = name.toLowerCase();
          if (nameLow.startsWith("reserved")) continue;
          if (nameLow.startsWith("not used")) continue;

          int x = xy / 1000;
          int y = xy % 1000;
          int fxy = (x << 8) + y;

          CodeFlagTables ct = table.get((short) fxy);
          if (ct == null) {
            ct = new CodeFlagTables((short) fxy, null);
            table.put(ct.fxy, ct);
            //System.out.printf(" added in 2: %s (%d)%n", ct.fxy(), ct.fxy);
          }
          ct.addValue(no, name);

        } catch (Exception e) {
          if (showReadErrs) System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
        }
      }

    } catch (Exception e) {
      log.error("Can't read BUFR code table " + filename, e);

    } finally {
      if (dataIS != null)
        try {
          dataIS.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
    }
  }

  public static void main(String arg[]) throws IOException {
    HashMap<Short, CodeFlagTables> tableMap1 = new HashMap<Short, CodeFlagTables>(300);
    initOld(tableMap1);

    HashMap<Short, CodeFlagTables> tableMap2 = new HashMap<Short, CodeFlagTables>(300);
    init2(tableMap2);

    System.out.printf("Compare 1 with 2%n");
    for (Short key : tableMap1.keySet()) {
      CodeFlagTables t = tableMap1.get(key);
      CodeFlagTables t2 = tableMap2.get(key);
      if (t2 == null)
        System.out.printf(" NOT FOUND in 2: %s (%d)%n", t.fxy(), t.fxy);
      else {
        if (t.fxy().equals("0-21-76"))
          System.out.println("HEY");
        for (Integer no : t.map.keySet()) {
          String name1 = t.map.get(no);
          String name2 = t2.map.get(no);
          if (name2 == null)
            System.out.printf(" %s val %d name '%s' missing%n", t.fxy(), no, name1);
          else if (showNameDiff && !name1.equals(name2))
            System.out.printf(" %s names different%n  %s%n  %s%n", t.fxy(), name1, name2);
        }
      }
    }

    System.out.printf("Compare 2 with 1%n");
    for (Short key : tableMap2.keySet()) {
      CodeFlagTables t = tableMap2.get(key);
      CodeFlagTables t1 = tableMap1.get(key);
      if (t1 == null)
        System.out.printf(" NOT FOUND in 1: %s (%d)%n", t.fxy(), t.fxy);
      else {
        for (Integer no : t.map.keySet()) {
          String name = t.map.get(no);
          String name1 = t1.map.get(no);
          if (name1 == null)
            System.out.printf(" %s val %d name '%s' missing%n", t.fxy(), no, name);
          else if (showNameDiff && !name.equals(name1))
            System.out.printf(" %s names different%n  %s%n  %s%n", t.fxy(), name, name1);
        }
      }
    }

  }

  ////////////////////////////////////////////////
  private short fxy;
  private String name;
  private Map<Integer, String> map;

  private CodeFlagTables(short fxy, String name) {
    this.fxy = fxy;
    this.name = (name == null) ? fxy() : StringUtil.replace(name, ' ', "_") + "("+fxy()+")";
    map = new HashMap<Integer, String>(20);
  }


  public String getName() {
    return name;
  }

  public Map<Integer, String> getMap() {
    return map;
  }

  private void addValue(int value, String text) {
    map.put(value, text);
  }

  String fxy() {
    int f = fxy >> 16;
    int x = (fxy & 0xff00) >> 8;
    int y = (fxy & 0xff);

    return f + "-" + x + "-" + y;
  }

  public String toString() { return name; }

}
