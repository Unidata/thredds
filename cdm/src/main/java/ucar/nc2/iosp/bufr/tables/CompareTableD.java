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
import java.util.*;

import ucar.unidata.util.StringUtil;

/**
 * @author caron
 * @since Jul 24, 2008
 */
public class CompareTableD {

  String bmt = "file:C:\\docs\\bufr\\britishMet\\WORKING\\bufr\\Code Tables 2007\\edited/BUFR_Tab_D_6.xml";
  String robbt = "C:\\dev\\tds\\bufr\\resources\\resources\\source\\wmo\\verified\\B4M-000-013-D";
  String robbxml = "file:C:\\dev\\tds\\bufr\\resources\\resources\\source\\wmo\\xml/B4M-000-013-D.xml";

  static Map<Integer, Sequence> bmTable = new TreeMap<Integer, Sequence>();
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
      List<Element> featureCollectList = featureCat.getChildren("featureCollection");
      count += featureCollectList.size();

      for (Element featureCollect : featureCollectList) {
        int f = Integer.parseInt(featureCollect.getChildText("F"));
        int x = Integer.parseInt(featureCollect.getChildText("X"));
        int y = Integer.parseInt(featureCollect.getChildText("Y"));
        String name = featureCollect.getChild("annotation").getChildTextNormalize("documentation");

        int fxy = (f << 16) + (x << 8) + y;
        Sequence seq = new Sequence(fxy, name);
        bmTable.put(fxy, seq);

        List<Element> features = featureCollect.getChildren("feature");

        for (Element feature : features) {
          f = Integer.parseInt(feature.getChildText("F"));
          x = Integer.parseInt(feature.getChildText("X"));
          y = Integer.parseInt(feature.getChildText("Y"));
          name = feature.getChild("annotation").getChildTextNormalize("documentation");
          fxy = (f << 16) + (x << 8) + y;
          Feature feat = new Feature(fxy, name);
          seq.features.add(feat); 
        }
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
      int count = makeTable(root.getChildren("sequence"));
      System.out.println(" robb count= "+count);

    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
  }

  static Map<Integer, Sequence> map = new TreeMap<Integer, Sequence>();
  public int makeTable(List<Element> seqList) {
    int count = 0;
    for (Element seqElem : seqList) {
      int f = Integer.parseInt(seqElem.getAttributeValue("F"));
      int x = Integer.parseInt(seqElem.getAttributeValue("X"));
      int y = Integer.parseInt(seqElem.getAttributeValue("Y"));
      String name = ""; // seqElem.getChildTextNormalize("name");

      int fxy = (f << 16) + (x << 8) + y;
      Sequence seq = new Sequence(fxy, name);
      map.put(fxy, seq);

      List<Element> elemList = seqElem.getChildren("element");
      for (Element elem : elemList) {
        f = Integer.parseInt(elem.getAttributeValue("F"));
        x = Integer.parseInt(elem.getAttributeValue("X"));
        y = Integer.parseInt(elem.getAttributeValue("Y"));
        name = elem.getChildTextNormalize("name");
        fxy = (f << 16) + (x << 8) + y;
        Feature feat = new Feature(fxy, name);
        seq.features.add(feat);
      }
      count++;
    }
    return count;
  }

  public void compare(Map<Integer, Sequence> thisMap, Map<Integer, Sequence> thatMap) {
    for (Integer key : thisMap.keySet()) {
      Sequence seq1 = thisMap.get(key);
      Sequence seq2 = thatMap.get(key);
      if (seq2 == null)
        System.out.printf(" No key %s %n", fxy(key));
      else {
        //if (!f1.name.equals(f2.name)) System.out.printf("%n key %s%n  %s%n  %s %n", fxy(key), f1.name, f2.name);
        if (seq1.features.size() != seq2.features.size())
          System.out.printf(" key %s size %d != %d %n", fxy(key), seq1.features.size(), seq2.features.size());
        else {
          for (int i=0; i<seq1.features.size(); i++) {
            Feature f1 = seq1.features.get(i);
            Feature f2 = seq2.features.get(i);
            if (f1.fxy != f2.fxy)
              System.out.printf("  key %s feature %s != %s %n", fxy(key), fxy(f1.fxy), fxy(f2.fxy));
          }
        }
      }
    }

  }

  class Sequence {
    int fxy;
    String name;
    List<Feature> features;
    Sequence(int fxy, String name) {
      this.fxy = fxy;
      this.name = name.trim();
      features = new ArrayList<Feature>(10);
    }
  }

  class Feature {
    int fxy;
    String name;
    Feature(int fxy, String name) {
      this.fxy = fxy;
      this.name = name.trim();
    }
  }

  String fxy( int fxy) {
    int f = fxy >> 16;
    int x = (fxy & 0xff00) >> 8;
    int y = (fxy & 0xff);

    return f +"-"+x+"-"+y;
  }

  static public void main( String args[]) throws IOException {
    CompareTableD ct = new CompareTableD();
    ct.readBmt();
    ct.readTable();
    System.out.printf("Compare britMet to ours %n");
    ct.compare(bmTable, map);
    System.out.printf("%nCompare ours to britMet %n");
    ct.compare(map, bmTable);
  }
}
