/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.bufr;

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

/**
 * @author caron
 * @since Jul 24, 2008
 */
public class CompareTableB {

  String bmt = "file:C:/dev/tds/bufr/resources/source/britMet/BUFR_B_080731.xml";
  String robbt = "C:/dev/tds/bufr/resources/resources/bufr/tables/B4M-000-013-B";
  
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
  static Map<Integer, Feature> bmTable = new TreeMap<Integer, Feature>();
  public void readBmt() throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(bmt);
      Element root = doc.getRootElement();
      int count = makeBmtTable(root.getChildren("featureCatalogue"));
      System.out.println(" bmt count= "+count);

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
        int scale = 0, reference=0, width = 0;

        String s = null;
        try {
          s = bufrElem.getChildTextNormalize("BUFR_scale");
          scale = Integer.parseInt( clean(s));
        } catch (NumberFormatException e) {
          System.out.printf(" key %s name '%s' has bad scale='%s'%n", fxy(fxy), name, s);
        }

        try {
          s = bufrElem.getChildTextNormalize("BUFR_reference");
          reference = Integer.parseInt( clean(s));
        } catch (NumberFormatException e) {
          System.out.printf(" key %s name '%s' has bad reference='%s' %n", fxy(fxy), name, s);
        }

        try {
          s = bufrElem.getChildTextNormalize("BUFR_width");
          width = Integer.parseInt( clean(s));
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

  //////////////////////////////////////////////////////////////
  public void readTable(String filename, Map<Integer, Feature> map) throws IOException {
    BufferedReader dataIS = new BufferedReader(new InputStreamReader( new FileInputStream(filename), Charset.forName("UTF8")));
    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith("#")) continue;

      String[] flds = line.split("; ");
      if (flds.length < 8) {
        System.out.println("BAD line == "+line);
        continue;
      }

      int i=0;
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
    System.out.println(filename+" count= "+count);
  }

  //////////////////////////////////////////////////////////
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
    list.add(p +" ("+fname+")");
  }

  public void compare(String fname, Map<Integer, Feature> thisMap, Map<Integer, Feature> thatMap) {
    for (Integer key : thisMap.keySet()) {
      Feature f1 = thisMap.get(key);
      Feature f2 = thatMap.get(key);
      if (f2 == null)
        System.out.printf(" No key %s %n", fxy(key));
      else {
        //if (!f1.name.equals(f2.name))
        //  System.out.printf("%n key %s%n  %s%n  %s %n", fxy(key), f1.name, f2.name);
        //if (!f1.units.equalsIgnoreCase(f2.units))
        //  System.out.printf("%n key %s units %s != %s %n", fxy(key), f1.units, f2.units);
        if (f1.scale != f2.scale) {
          System.out.printf(" key %s scale %d != %d %n", fxy(key), f1.scale, f2.scale);
          addProblem(fname, key, "scale "+f1.scale+" != "+f2.scale);
        }
        if (f1.reference != f2.reference){
          System.out.printf(" key %s reference %d != %d %n", fxy(key), f1.reference, f2.reference);
          addProblem(fname, key, "refer "+f1.reference+" != "+f2.reference);
        }
        if (f1.width != f2.width) {
          System.out.printf(" key %s width %d != %d %n", fxy(key), f1.width, f2.width);
          addProblem(fname, key, "width "+f1.width+" != "+f2.width);
        }
      }
    }

  }

  public void compare2(Map<Integer, Feature> thisMap, Map<Integer, Feature> thatMap) {
    for (Integer key : thisMap.keySet()) {
      Feature f1 = thisMap.get(key);
      Feature f2 = thatMap.get(key);
      if (f2 == null)
        System.out.printf(" No key %s %n", fxy(key));
    }

  }

  class Feature {
    int fxy, scale, reference, width;
    String name, units;
    Feature(int fxy, String name, String units, int scale, int reference, int width) {
      this.fxy = fxy;
      this.name = name.trim();
      this.units = units.trim();
      this.scale = scale;
      this.reference = reference;
      this.width = width;
    }
  }

  String fxy( int fxy) {
    int f = fxy >> 16;
    int x = (fxy & 0xff00) >> 8;
    int y = (fxy & 0xff);

    return f +"-"+x+"-"+y;
  }

  public void compareDiff( ) throws IOException {
    Map<Integer, Feature> wmoMap = new TreeMap<Integer, Feature>();
    readTable(robbt, wmoMap);

    for (String fname : diffTable) {
      System.out.printf("=============================================================%n");
      Map<Integer, Feature> diffMap = new TreeMap<Integer, Feature>();
      readTable(diffTableDir+fname, diffMap);

      System.out.printf("Compare diff ("+fname+") to standard %n");
      compare(fname, diffMap, wmoMap);
    }

    Set<Integer> keys = problems.keySet();
    for (Integer key : keys) {
      System.out.printf("%n%s%n",fxy(key));
      List<String> list = problems.get(key);
      for (String p : list)
        System.out.printf(" %s%n",p);      
    }
  }

  public void compareBrit( ) throws IOException {
    readBmt();

    Map<Integer, Feature> robbMap = new TreeMap<Integer, Feature>();
    readTable(robbt, robbMap);

    System.out.printf("Compare britMet to ours %n");
    compare(bmt, bmTable, robbMap);
    System.out.printf("%n Compare britMet to ours %n");
    compare2(robbMap, bmTable);
  }

  static public void main( String args[]) throws IOException {
    CompareTableB ct = new CompareTableB();
    ct.compareBrit();
  }
}
