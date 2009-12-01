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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.*;
import java.nio.charset.Charset;

import ucar.unidata.util.StringUtil;

/**
 * @author caron
 * @since Jul 24, 2008
 */
public class CompareTableD {
  final int default_size = 1200;

  //String bmt = "file:C:\\docs\\bufr\\britishMet\\WORKING\\bufr\\Code Tables 2007\\edited/BUFR_Tab_D_6.xml";
  String robbt = "C:\\dev\\tds\\bufr\\resources\\resources\\source\\wmo\\verified\\B4M-000-013-D";
  String robbxml = "file:C:/dev/tds/bufr/resources/source/wmo/xml/B4M-000-013-D.xml";

  ////////////////////////////////////////////////////////////////////
  // read brit met table

  String bmt = "file:C:/dev/tds/bufr/resources/source/ukmet/original/BUFR_D_080731.xml";
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
        Sequence seq = new Sequence(x, y, name);
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

  //////////////////////////////////////////////////////////////
   // Read WMO csv format
  static Map<Integer, Sequence> wmoMap = new TreeMap<Integer, Sequence>();

   void readWmoCsv(String filename, Map<Integer, Sequence> map) throws IOException {
     BufferedReader dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Charset.forName("UTF8")));
     int count = 0;
     int currSeqno = -1;
     Sequence currSeq = null;
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
       if (flds.length < 5) {
         System.out.printf("%d INCOMPLETE line == %s%n", count, line);
         continue;
       }

       int fldidx = 0;
       try {
         int sno = Integer.parseInt(flds[fldidx++]);
         int cat = Integer.parseInt(flds[fldidx++]);
         int seq = Integer.parseInt(flds[fldidx++]);
         String seqName = flds[fldidx++];
         String featno = flds[fldidx++];
         if (featno.trim().length() == 0) {
           System.out.printf("%d skip line == %s%n", count, line);
           continue;
         }
         String featName =  (flds.length > 5) ? flds[fldidx++] : "n/a";

         if (currSeqno != seq) {
           int y = seq % 1000;
           int w = seq / 1000;
           int x = w % 100;
           currSeq = new Sequence(x, y, seqName);
           wmoMap.put(currSeq.fxy, currSeq);
           currSeqno = seq;
         }

         int fno = Integer.parseInt(featno);
         int y = fno % 1000;
         int w = fno / 1000;
         int x = w % 100;
         int f = w / 100;

         int fxy = (f << 16) + (x << 8) + y;
         Feature feat = new Feature(fxy, featName);
         currSeq.features.add( feat);

       } catch (Exception e) {
         System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
       }
     }
     int n = map.values().size();
     System.out.printf("%s lines=%d elems=%d%n", filename, count, n);
   }


  ///////////////////////////////////////////////////
  // read robb's table

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
      Sequence seq = new Sequence(x, y, name);
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
        System.out.printf(" No key %s in second table %n", fxy(key));
      else {
        //if (!f1.name.equals(f2.name)) System.out.printf("%n key %s%n  %s%n  %s %n", fxy(key), f1.name, f2.name);
        if (seq1.features.size() != seq2.features.size()) {
          System.out.printf(" key %s size %d != %d %n  ", fxy(key), seq1.features.size(), seq2.features.size());
          for (Feature f1 : seq1.features) System.out.printf(" %s,", f1);
          System.out.printf("%n  ");
          for (Feature f2 : seq2.features) System.out.printf(" %s,", f2);
          System.out.printf("%n");
        } else {
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
    Sequence(int x, int y, String name) {
      this.fxy = (3 << 16) + (x << 8) + y;
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

    public String toString() { return fxy(fxy); }
  }

  String fxy( int fxy) {
    int f = fxy >> 16;
    int x = (fxy & 0xff00) >> 8;
    int y = (fxy & 0xff);

    return f +"-"+x+"-"+y;
  }

  static public void main( String args[]) throws IOException {
    CompareTableD ct = new CompareTableD();
    ct.readWmoCsv("C:/docs/B_TableD.csv", wmoMap);
    ct.readTable();
    System.out.printf("Compare wmoMap to ours %n");
    ct.compare(wmoMap, map);
    System.out.printf("%nCompare ours to wmoMap %n");
    ct.compare(map, wmoMap);
  }
}
