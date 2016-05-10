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
package ucar.nc2.iosp.bufr.tables;

import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.Element;
import ucar.nc2.iosp.bufr.Descriptor;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.util.StringUtil2;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Read BUFR Code / Flag tables.
 *
 * @author caron
 * @since Jul 12, 2008
 */
public class CodeFlagTables {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CodeFlagTables.class);
  static private final String CodeFlagFilename = "wmo/BUFRCREX_25_0_0_CodeFlag_en.xml";
  static Map<Short, CodeFlagTables> tableMap;

  static public CodeFlagTables getTable(short id) {
    if (tableMap == null) init();

    if (id == 263) return useCC(id, 5); // 0-1-7
    if (id == 526) return useCC(id, 7); // 0-2-14
    if (id == 531) return useCC(id, 8); // 0-2-19
    if (id == 5699) return useCC(id, 3); // 0-22-67
    if (id == 5700) return useCC(id, 4); // 0-22-68

    return tableMap.get(id);
  }

  static private CodeFlagTables useCC(short fxy, int cc) {
    CodeFlagTables cft = tableMap.get(fxy);
    if (cft == null) {
      CommonCodeTable cct =  CommonCodeTable.getTable(cc);
      cft = new CodeFlagTables(fxy, cct.getTableName(),  cct.getMap());
      tableMap.put(fxy, cft);
    }
    return cft;
  }

  static public boolean hasTable(short id) {
    if (tableMap == null) init();
    CodeFlagTables result = tableMap.get(id);
    return result != null;
  }

  static private void init() {
    tableMap = new HashMap<>(300);
    init(tableMap);
  }

  static public Map<Short, CodeFlagTables> getTables() {
    if (tableMap == null) init();
    return tableMap;
  }

  /*
  <Exp_CodeFlagTables_E>
    <No>837</No>
    <FXY>002119</FXY>
    <ElementName_E>Instrument operations</ElementName_E>
    <CodeFigure>0</CodeFigure>
    <EntryName_E>Intermediate frequency calibration mode (IF CAL)</EntryName_E>
    <Status>Operational</Status>
  </Exp_CodeFlagTables_E>

<BUFRCREX_19_1_1_CodeFlag_en>
  <No>2905</No>
  <FXY>020042</FXY>
  <ElementName_en>Airframe icing present</ElementName_en>
  <CodeFigure>2</CodeFigure>
  <EntryName_en>Reserved</EntryName_en>
  <Status>Operational</Status>
</BUFRCREX_19_1_1_CodeFlag_en>

<BUFRCREX_22_0_1_CodeFlag_en>
<No>3183</No>
<FXY>020063</FXY>
<ElementName_en>Special phenomena</ElementName_en>
<CodeFigure>31</CodeFigure>
<EntryName_en>Slight coloration of clouds at sunrise associated with a tropical disturbance</EntryName_en>
<Status>Operational</Status>
</BUFRCREX_22_0_1_CodeFlag_en>

   */
  static private void init(Map<Short, CodeFlagTables> table) {
    String filename = BufrTables.RESOURCE_PATH + CodeFlagFilename;
    try (InputStream is = CodeFlagTables.class.getResourceAsStream(filename)) {
      SAXBuilder builder = new SAXBuilder();
      org.jdom2.Document tdoc = builder.build(is);
      org.jdom2.Element root = tdoc.getRootElement();

      List<Element> elems = root.getChildren();
      for (Element elem : elems) {
        String fxyS = elem.getChildText("FXY");
        String desc = elem.getChildText("ElementName_en");

        short fxy = Descriptor.getFxy2(fxyS);
        CodeFlagTables ct = table.get(fxy);
        if (ct == null) {
          ct = new CodeFlagTables(fxy, desc);
          table.put(fxy, ct);
          // System.out.printf(" added %s == %s %n", ct.id, desc);
        }

        String line = elem.getChildText("No");
        String codeS = elem.getChildText("CodeFigure");
        String value = elem.getChildText("EntryName_en");

        if ((codeS == null) || (value == null)) continue;
        if (value.toLowerCase().startsWith("reserved"))  continue;
        if (value.toLowerCase().startsWith("not used")) continue;

        int code;
        if (codeS.toLowerCase().contains("all")) {
          code = -1;
        } else try {
          code = Integer.parseInt(codeS);
        } catch (NumberFormatException e) {
          log.debug("NumberFormatException on line " + line + " in " + codeS);
          continue;
        }
        ct.addValue((short) code, value);
      }

    } catch (IOException|JDOMException e) {
      log.error("Can't read BUFR code table " + filename, e);
    }
  }

  static private final boolean showReadErrs = false, showNameDiff = false;

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
          continue;
        }

        // any commas that are embedded in quotes - replace with blanks for now so split works
        int pos1 = line.indexOf('"');
        if (pos1 >= 0) {
          int pos2 = line.indexOf('"', pos1 + 1);
          StringBuilder sb = new StringBuilder(line);
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
        int fldidx = 1; // start at 1 to skip sno
        try {
          int xy = Integer.parseInt(flds[fldidx++].trim());
          int no;
          try {
            no = Integer.parseInt(flds[fldidx++].trim());
          } catch (NumberFormatException e) {
            if (showReadErrs) System.out.printf("%d skip == %s%n", count, line);
            continue;
          }
          String name = StringUtil2.remove(flds[fldidx], '"');
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
          ct.addValue((short) no, name);

        } catch (NumberFormatException e) {
          if (showReadErrs) System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
        }
      }

    } catch (IOException e) {
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
    HashMap<Short, CodeFlagTables> tableMap1 = new HashMap<>(300);
    init(tableMap1);

    HashMap<Short, CodeFlagTables> tableMap2 = new HashMap<>(300);
    init2(tableMap2);

    System.out.printf("Compare 1 with 2%n");
    for (Map.Entry<Short, CodeFlagTables> ent : tableMap1.entrySet()) {
      CodeFlagTables t = ent.getValue();
      CodeFlagTables t2 = tableMap2.get(ent.getKey());
      if (t2 == null)
        System.out.printf(" NOT FOUND in 2: %s (%d)%n", t.fxy(), t.fxy);
      else {
        for (int no : t.map.keySet()) {
          String name1 = t.map.get(no);
          String name2 = t2.map.get(no);
          if (name2 == null)
            System.out.printf(" %s val %d name '%s' missing in 2%n", t.fxy(), no, name1);
          else if (showNameDiff && !name1.equals(name2))
            System.out.printf(" *** %s names different%n  %s%n  %s%n", t.fxy(), name1, name2);
        }
      }
    }

    System.out.printf("Compare 2 with 1%n");
    for (Map.Entry<Short, CodeFlagTables> ent : tableMap2.entrySet()) {
      CodeFlagTables t = ent.getValue();
      CodeFlagTables t1 = tableMap1.get(ent.getKey());
      if (t1 == null)
        System.out.printf(" NOT FOUND in 1: %s (%d)%n", t.fxy(), t.fxy);
      else {
        for (int no : t.map.keySet()) {
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
  private Map<Integer, String> map;  // needs to be integer for EnumTypedef

  private CodeFlagTables(short fxy, String name) {
    this.fxy = fxy;
    this.name = (name == null) ? fxy() : name; // StringUtil2.replace(name, ' ', "_") + "("+fxy()+")";
    map = new HashMap<>(20);
  }

  private CodeFlagTables(short fxy, String name, Map<Integer, String> map) {
    this.fxy = fxy;
    this.name = (name == null) ? fxy() : name;
    this.map = map;
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

  public short getId() { return fxy; }

  public String fxy() {
    int f = fxy >> 14;
    int x = (fxy & 0xff00) >> 8;
    int y = (fxy & 0xff);

    return f + "-" + x + "-" + y;
  }

  public String toString() { return name; }

}
