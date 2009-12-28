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
import org.jdom.JDOMException;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.charset.Charset;

import ucar.unidata.util.StringUtil;
import ucar.nc2.iosp.bufr.Descriptor;

/**
 * @author caron
 * @since Jul 24, 2008
 */
public class CompareTableB {
  static String btRoot = "C:/dev/tds/thredds/bufrTables/";
  static String bmt = "file:C:/dev/tds/bufr/resources/source/ukmet/original/BUFR_B_080731.xml";
  static String robbt = "C:/dev/tds/bufr/resources/resources/bufr/tables/B4M-000-013-B";

  String diffTableDir = "C:/dev/tds/bufr/resources/resources/bufr/tables/";
  String[] diffTable = {
          "B2M-000-002-B.diff",
          "B3M-000-003-B.diff",
          "B3M-000-004-B.diff",
          "B3M-000-005-B.diff",
          "B3M-000-006-B.diff",
          "B3M-000-007-B.diff",
          "B3M-000-008-B.diff",
          "B3M-000-009-B.diff",
          "B3M-000-010-B.diff",
          "B3M-000-011-B.diff",
          "B3M-000-012-B.diff"};

  //String robbxml = "file:C:\\dev\\tds\\bufr\\resources\\resources\\source\\wmo\\xml/B4M-000-013-B.xml";

  Pattern pattern = Pattern.compile("(.*)\\([sS]ee [nN]ote.*");

  //////////////////////////////////////////////////////////////////////
  // read British met table  XML format

  static Map<Integer, Feature> bmTable = new TreeMap<Integer, Feature>();

  public void readBmt() throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(bmt);
      Element root = doc.getRootElement();
      int count = makeBmtTable(root.getChildren("featureCatalogue"));
      System.out.println(" bmt count= " + count);

      /* Format pretty = Format.getPrettyFormat();

      // wierd - cant pretty print ??!!
      XMLOutputter fmt = new XMLOutputter(pretty);
      //Writer pw = new FileWriter("C:/docs/bufr/wmo/wordNice.txt");
      fmt.output(doc, new PrintWriter(System.out)); */

    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
  }

  public int makeBmtTable(List<Element> featureCatList) {
    int count = 0;
    System.out.printf("Parsing BritMet file %s%n", bmt);

    for (Element featureCat : featureCatList) {
      List<Element> features = featureCat.getChildren("feature");
      count += features.size();

      for (Element feature : features) {
        String name = feature.getChild("annotation").getChildTextNormalize("documentation");
        int f = Integer.parseInt(feature.getChildText("F"));
        int x = Integer.parseInt(feature.getChildText("X"));
        int y = Integer.parseInt(feature.getChildText("Y"));
        int fxy = (f << 16) + (x << 8) + y;

        Element bufrElem = feature.getChild("BUFR");
        String units = bufrElem.getChildTextNormalize("BUFR_units");
        int scale = 0, reference = 0, width = 0;

        String s = null;
        try {
          s = bufrElem.getChildTextNormalize("BUFR_scale");
          scale = Integer.parseInt(clean(s));
        } catch (NumberFormatException e) {
          System.out.printf(" key %s name '%s' has bad scale='%s'%n", fxy(fxy), name, s);
        }

        try {
          s = bufrElem.getChildTextNormalize("BUFR_reference");
          reference = Integer.parseInt(clean(s));
        } catch (NumberFormatException e) {
          System.out.printf(" key %s name '%s' has bad reference='%s' %n", fxy(fxy), name, s);
        }

        try {
          s = bufrElem.getChildTextNormalize("BUFR_width");
          width = Integer.parseInt(clean(s));
        } catch (NumberFormatException e) {
          System.out.printf(" key %s name '%s' has bad width='%s' %n", fxy(fxy), name, s);
        }

        Feature feat = new Feature(fxy, name, units, scale, reference, width);
        bmTable.put(fxy, feat);
      }
    }
    return count;
  }

  String clean(String s) {
    return StringUtil.remove(s, ' ');
  }

  public void compareBrit() throws IOException {
    readBmt();

    Map<Integer, Feature> robbMap = new TreeMap<Integer, Feature>();
    readTable(robbt, robbMap);

    System.out.printf("Compare britMet to ours %n");
    compare(bmt, bmTable, robbMap);
    System.out.printf("%n Compare britMet to ours %n");
    compare2(robbMap, bmTable);
  }

  static public void mainBrit(String args[]) throws IOException {
    CompareTableB ct = new CompareTableB();
    ct.compareBrit();
  }

  //////////////////////////////////////////////////////////////
  // Read WMO csv format
  void readWmoCsv(String filename, Map<Integer, Feature> map) throws IOException {
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Charset.forName("UTF8")));
    int avg = 0;
    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      count++;

      if (line == null) break;
      if (line.startsWith("#")) continue;

      if (count==1) {
        System.out.println("header line == " + line);
        continue;
      }

      // commas embedded in quotes - replace with blanks for now
      int pos1 = line.indexOf('"');
      if (pos1 >= 0) {
        int pos2 = line.indexOf('"', pos1+1);
        StringBuffer sb = new StringBuffer(line);
        for (int i=pos1; i<pos2; i++)
          if(sb.charAt(i)==',') sb.setCharAt(i, ' ');
        line = sb.toString();
      }

      String[] flds = line.split(",");
      if (flds.length < 7) {
        System.out.printf("%d BAD line == %s%n", count, line);
        continue;
      }

      int fldidx = 0;
      try {
        int classId = Integer.parseInt(flds[fldidx++]);
        int xy = Integer.parseInt(flds[fldidx++]);
        String name = flds[fldidx++];
        String units = flds[fldidx++];
        int scale = Integer.parseInt(clean(flds[fldidx++]));
        int reference = Integer.parseInt(clean(flds[fldidx++]));
        int width = Integer.parseInt(clean(flds[fldidx++]));

        int x = xy / 1000;
        int y = xy % 1000;
        int fxy = (x << 8) + y;  // f always 0
        Feature feat = new Feature(fxy, norm(name), units, scale, reference, width);
        map.put(fxy, feat);
        avg += name.length();

      } catch (Exception e) {
        System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
      }
    }
    int n = map.values().size();
    System.out.printf("%s lines=%d elems=%d avg name len=%d%n", filename, count, n, avg/n);
  }

  static public void main(String args[]) throws IOException {
    CompareTableB ct = new CompareTableB();
    Map<Integer, Feature> wmoMap = new TreeMap<Integer, Feature>();
    ct.readWmoCsv("C:/docs/BC_TableB.csv", wmoMap);
    
    Map<Integer, Feature> robbMap = new TreeMap<Integer, Feature>();
    ct.readTable(robbt, robbMap);
    //ct.readBmt();

    System.out.printf("Compare ours to wmo %n");
    ct.compare(robbt, robbMap, wmoMap);
    System.out.printf("%n Compare wmo to ours %n");
    ct.compare2(wmoMap, robbMap);
  }

  //////////////////////////////////////////////////////////////
  // Read robbs format
  public void readTable(String filename, Map<Integer, Feature> map) throws IOException {
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Charset.forName("UTF8")));
    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith("#")) continue;

      String[] flds = line.split("; ");
      if (flds.length < 8) {
        System.out.println("BAD line == " + line);
        continue;
      }

      int i = 0;
      int f = Integer.parseInt(flds[i++]);
      int x = Integer.parseInt(flds[i++]);
      int y = Integer.parseInt(flds[i++]);
      int scale = Integer.parseInt(flds[i++]);
      int reference = Integer.parseInt(flds[i++]);
      int width = Integer.parseInt(flds[i++]);
      String units = flds[i++];
      String name = flds[i++];

      int fxy = (f << 16) + (x << 8) + y;
      Feature feat = new Feature(fxy, norm(name), units, scale, reference, width);
      map.put(fxy, feat);
      count++;
    }
    System.out.println(filename + " count= " + count);
  }



  //////////////////////////////////////////////////////////
  // compare tables, accumulate problem messages

  /* public void readTableXML() throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(robbxml);
      Element root = doc.getRootElement();
      int count = makeTableXML(root.getChildren("element"));
      System.out.println(" robb count= "+count);

    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
  }

  public int makeTableXML(List<Element> elemList) {
    int count = 0;
    for (Element elem : elemList) {
      int f = Integer.parseInt(elem.getAttributeValue("F"));
      int x = Integer.parseInt(elem.getAttributeValue("X"));
      int y = Integer.parseInt(elem.getAttributeValue("Y"));
      String name = elem.getChildTextNormalize("name");
      String units = elem.getChildTextNormalize("units");
      int scale = Integer.parseInt(elem.getChildText("scale"));
      int reference = Integer.parseInt(elem.getChildText("reference"));
      int width = Integer.parseInt(elem.getChildText("width"));
      int fxy = (f << 16) + (x << 8) + y;
      Feature feat = new Feature(fxy, norm(name), units, scale, reference, width);
      map.put(fxy, feat);
      count++;
    }
    return count;
  }  */

  String norm(String s) {
    Matcher matcher = pattern.matcher(s);
    if (!matcher.matches()) return s;
    return matcher.group(1);
  }

  Map<Integer, List<String>> problems = new TreeMap<Integer, List<String>>();

  void addProblem(String fname, Integer key, String p) {
    List<String> list = problems.get(key);
    if (list == null) {
      list = new ArrayList<String>();
      problems.put(key, list);
    }
    list.add(p + " (" + fname + ")");
  }

  public void compare(String fname, Map<Integer, Feature> thisMap, Map<Integer, Feature> thatMap) {
    for (Integer key : thisMap.keySet()) {
      Feature f1 = thisMap.get(key);
      Feature f2 = thatMap.get(key);
      if (f2 == null)
        System.out.printf("%n No key %s in second table %n", fxy(key));
      else {
         if (!equiv(f1.name,f2.name))
          System.out.printf("%n key %s name%n  %s%n  %s%n", fxy(key), f1.name, f2.name);
        if (!equiv(f1.units,f2.units))
          System.out.printf("%n key %s units%n  %s%n  %s%n", fxy(key), f1.units, f2.units); // */
        
        if (f1.scale != f2.scale) {
          System.out.printf("%n key %s scale %d != %d %n", fxy(key), f1.scale, f2.scale);
          addProblem(fname, key, "scale " + f1.scale + " != " + f2.scale);
        }
        if (f1.reference != f2.reference) {
          System.out.printf("%n key %s reference %d != %d %n", fxy(key), f1.reference, f2.reference);
          addProblem(fname, key, "refer " + f1.reference + " != " + f2.reference);
        }
        if (f1.width != f2.width) {
          System.out.printf("%n key %s width %d != %d %n", fxy(key), f1.width, f2.width);
          addProblem(fname, key, "width " + f1.width + " != " + f2.width);
        }
      }
    }
  }

  char[] remove = new char[] {'(', ')', ' ', '"', ',', '*', '-'};
  String[] replace = new String[] {"", "", "", "", "", "", ""};
  boolean equiv(String org1, String org2) {
    String s1 = StringUtil.replace(org1, remove, replace).toLowerCase();
    String s2 = StringUtil.replace(org2, remove, replace).toLowerCase();
    return s1.equals(s2);
  }

  public void compare2(Map<Integer, Feature> thisMap, Map<Integer, Feature> thatMap) {
    for (Integer key : thisMap.keySet()) {
      Feature f1 = thisMap.get(key);
      Feature f2 = thatMap.get(key);
      if (f2 == null)
        System.out.printf(" No key %s in second table %n", fxy(key));
    }
  }

  class Feature {
    int fxy, scale, reference, width;
    String name, units;

    // 1300 entries. dominated by size of name. max 65K (fxy)
    Feature(int fxy, String name, String units, int scale, int reference, int width) {
      this.fxy = fxy; // short
      this.name = name.trim();
      this.units = units.trim(); // most are common - Code Table, Flag Table, Numeric, CCITT IA5
      this.scale = scale;  // 0-13
      this.reference = reference; // int 62M to -1G
      this.width = width;  // 0-256
    }
  }

  String fxy(int fxy) {
    int f = fxy >> 16;
    int x = (fxy & 0xff00) >> 8;
    int y = (fxy & 0xff);

    return f + "-" + x + "-" + y;
  }

  public void compareDiff() throws IOException {
    Map<Integer, Feature> wmoMap = new TreeMap<Integer, Feature>();
    readTable(robbt, wmoMap);

    for (String fname : diffTable) {
      System.out.printf("=============================================================%n");
      Map<Integer, Feature> diffMap = new TreeMap<Integer, Feature>();
      readTable(diffTableDir + fname, diffMap);

      System.out.printf("Compare diff (" + fname + ") to standard %n");
      compare(fname, diffMap, wmoMap);
    }

    Set<Integer> keys = problems.keySet();
    for (Integer key : keys) {
      System.out.printf("%n%s%n", fxy(key));
      List<String> list = problems.get(key);
      for (String p : list)
        System.out.printf(" %s%n", p);
    }
  }

  ///////////////////////////////////////////////////

  class DescTrack {
    short id;
    List<TableB.Descriptor> descList = new ArrayList<TableB.Descriptor>(10);
    List<String> whereList = new ArrayList<String>(10);

    DescTrack(short id) {
      this.id = id;
    }

    void add(TableB.Descriptor d, String where) {
      descList.add(d);
      whereList.add(where);
    }

    void showSingles() {
      if (descList.size() < 2) { // skip if only wmo
        String where0 = whereList.get(0);
        if (where0.equals("WHO")) return;
      }

      for (int i = 0; i < descList.size(); i++) {
        TableB.Descriptor bdesc = descList.get(i);
        String where = whereList.get(i);
        System.out.printf(" %s == %s%n", bdesc, where);
      }
    }

  }

  class TableName {
    String filename, name;

    TableName(String name, String filename) {
      this.name = name;
      this.filename = filename;
    }
  }

  String tableDirName = "C:\\dev\\tds\\bufr\\resources\\resources\\bufr\\tables\\";

  void addToMap(TableName t, Map<Short, DescTrack> mapAll) throws IOException {
    System.out.printf("Read (" + t.filename + ")%n");
    TableB tableB = BufrTables.readTableB(tableDirName + t.filename, "robb", false);
    Collection<TableB.Descriptor> desc = tableB.getDescriptors();

    for (TableB.Descriptor d : desc) {
      short fxy = d.getId();
      if (!Descriptor.isWmoRange(fxy)) continue;

      DescTrack f = mapAll.get(fxy);
      if (f == null) {
        f = new DescTrack(fxy);
        mapAll.put(fxy, f);
      }
      f.add(d, t.name);
    }
  }


  public void compareAll() throws IOException {
    TableName[] tables = new TableName[6];
    tables[0] = new TableName("WMO", "B4M-000-014-B");
    tables[1] = new TableName("NCEP", "NCEPtable-B.diff");
    tables[2] = new TableName("Brazil", "B4L-046-013-B.diff");
    tables[3] = new TableName("ECMWF", "B4L-098-013-B.diff");
    tables[4] = new TableName("FNMOC", "B4L-058-013-B.diff");
    tables[5] = new TableName("Eumetsat", "B3L-254-011-B.diff");
    int[] want = new int[]{0, 1};

    Map<Short, DescTrack> mapAll = new TreeMap<Short, DescTrack>();

    //for (int i : want) {
    for (int i = 0; i < tables.length; i++) {
      addToMap(tables[i], mapAll);
    }

    Set<Short> keys = mapAll.keySet();
    List<Short> sortKeys = new ArrayList<Short>(keys);
    Collections.sort(sortKeys);

    System.out.printf("pass one for differences with WMO%n");
    for (Short key : sortKeys) {
      DescTrack dtrack = mapAll.get(key);

      if (dtrack.descList.size() < 2) continue;
      String where0 = dtrack.whereList.get(0);
      if (!where0.equals("WMO")) continue; // must have WMO

      TableB.Descriptor wmo = null;
      System.out.printf("%nFxy=%s%n", Descriptor.makeString(key));
      for (int i = 0; i < dtrack.descList.size(); i++) {
        TableB.Descriptor bdesc = dtrack.descList.get(i);
        String where = dtrack.whereList.get(i);
        System.out.printf(" %s == %s%n", bdesc, where);
        if (i == 0) wmo = bdesc;
        else System.out.printf("**%s%n", compare(wmo, bdesc));
      }
    }


    System.out.printf("%n===========================%n");
    System.out.printf("%npass two for addition to WMO%n");
    for (Short key : sortKeys) {
      DescTrack dtrack = mapAll.get(key);

      String where0 = dtrack.whereList.get(0);
      if (where0.equals("WMO")) continue; // must not have WMO

      System.out.printf("%nFxy=%s%n", Descriptor.makeString(key));
      for (int i = 0; i < dtrack.descList.size(); i++) {
        TableB.Descriptor bdesc = dtrack.descList.get(i);
        String where = dtrack.whereList.get(i);
        System.out.printf(" %s == %s%n", bdesc, where);
      }
    }
  }

  String compare(TableB.Descriptor f1, TableB.Descriptor f2) {
    StringBuilder sb = new StringBuilder();
    if (!f1.getUnits().equalsIgnoreCase(f2.getUnits())) {
      sb.append(" units");
    }

    if (f1.getScale() != f2.getScale()) {
      sb.append(" scale");
    }

    if (f1.getRefVal() != f2.getRefVal()) {
      sb.append(" refVal");
    }

    if (f1.getDataWidth() != f2.getDataWidth()) {
      sb.append(" width");
    }

    return sb.toString();
  }

  static public void main2(String args[]) throws IOException {
    CompareTableB ct = new CompareTableB();
    ct.compareAll();
  }
}
