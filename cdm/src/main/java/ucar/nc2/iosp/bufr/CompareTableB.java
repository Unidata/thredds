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
import java.util.*;

import ucar.unidata.util.StringUtil;

/**
 * @author caron
 * @since Jul 24, 2008
 */
public class CompareTableB {

  String bmt = "file:C:/doc/bufr/britMet/BUFR_B_080731.xml";
  String robbt = "C:\\dev\\tds\\bufr\\resources\\resources\\source\\wmo\\verified\\B4M-000-013-B";
  String robbxml = "file:C:\\dev\\tds\\bufr\\resources\\resources\\source\\wmo\\xml/B4M-000-013-B.xml";

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
    for (Element featureCat : featureCatList) {
      List<Element> features = featureCat.getChildren("feature");
      count += features.size();

      for (Element feature : features) {
        int f = Integer.parseInt(feature.getChildText("F"));
        int x = Integer.parseInt(feature.getChildText("X"));
        int y = Integer.parseInt(feature.getChildText("Y"));
        String name = feature.getChild("annotation").getChildTextNormalize("documentation");
        Element bufrElem = feature.getChild("BUFR");
        int scale = Integer.parseInt( clean(bufrElem.getChildText("BUFR_scale")));
        int reference = Integer.parseInt( clean(bufrElem.getChildText("BUFR_reference")));
        int width = Integer.parseInt( clean(bufrElem.getChildText("BUFR_width")));
        int fxy = (f << 16) + (x << 8) + y;
        Feature feat = new Feature(fxy, name, scale, reference, width);
        bmTable.put(fxy, feat);
      }
    }
    return count;
  }

  String clean(String s) {
    return StringUtil.remove(s, ' ');
  }



  public void readTable() throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(robbxml);
      Element root = doc.getRootElement();
      int count = makeTable(root.getChildren("element"));
      System.out.println(" robb count= "+count);

    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
  }

  static Map<Integer, Feature> map = new TreeMap<Integer, Feature>();
  public int makeTable(List<Element> elemList) {
    int count = 0;
    for (Element elem : elemList) {
      int f = Integer.parseInt(elem.getAttributeValue("F"));
      int x = Integer.parseInt(elem.getAttributeValue("X"));
      int y = Integer.parseInt(elem.getAttributeValue("Y"));
      String name = elem.getChildTextNormalize("name");
      int scale = Integer.parseInt(elem.getChildText("scale"));
      int reference = Integer.parseInt(elem.getChildText("reference"));
      int width = Integer.parseInt(elem.getChildText("width"));
      int fxy = (f << 16) + (x << 8) + y;
      Feature feat = new Feature(fxy, name, scale, reference, width);
      map.put(fxy, feat);
      count++;
    }
    return count;
  }

  public void compare(Map<Integer, Feature> thisMap, Map<Integer, Feature> thatMap) {
    for (Integer key : thisMap.keySet()) {
      Feature f1 = thisMap.get(key);
      Feature f2 = thatMap.get(key);
      if (f2 == null)
        System.out.printf(" No key %s %n", fxy(key));
      else {
        if (!f1.name.equals(f2.name)) System.out.printf("%n key %s%n  %s%n  %s %n", fxy(key), f1.name, f2.name);
        if (f1.scale != f2.scale) System.out.printf(" key %s scale %d != %d %n", fxy(key), f1.scale, f2.scale);
        if (f1.reference != f2.reference) System.out.printf(" key %s reference %d != %d %n", fxy(key), f1.reference, f2.reference);
        if (f1.width != f2.width) System.out.printf(" key %s width %d != %d %n", fxy(key), f1.width, f2.width);
      }
    }

  }

  class Feature {
    int fxy, scale, reference, width;
    String name;
    Feature(int fxy, String name, int scale, int reference, int width) {
      this.fxy = fxy;
      this.name = name.trim();
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

  static public void main( String args[]) throws IOException {
    CompareTableB ct = new CompareTableB();
    ct.readBmt();
    ct.readTable();
    System.out.printf("Compare britMet to ours %n");
    ct.compare(bmTable, map);
    //System.out.printf("%n Compare britMet to ours %n");
    //ct.compare(map, bmTable);
  }
}
