/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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


import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.nio.charset.Charset;

import ucar.unidata.util.StringUtil;

/**
 * Compare wmo excel CodeTables with ours
 *
 * @author caron
 * @since Nov 30, 2009
 */


public class CompareCodeTables {

  class Feature {
    int fxy;
    String name;
    Map<Integer,String> map = new HashMap<Integer,String>(10);

    Feature(int x, int y) {
      this.fxy = (x << 8) + y;
    }

  private void addValue(String valueS, String text) {
    if (text.toLowerCase().contains("reserved")) return;
    text = StringUtil.remove(text, '"');
    try {
     int value = Integer.parseInt(valueS);
     map.put(value,text);
    } catch (Exception e) {
      System.out.printf("%s cant parse %s text = %s%n", this, valueS, text);
    }
  }

    public String toString() {
      return fxy(fxy);
    }
  }

  String fxy(int fxy) {
    int f = fxy >> 16;
    int x = (fxy & 0xff00) >> 8;
    int y = (fxy & 0xff);

    return f + "-" + x + "-" + y;
  }

  //////////////////////////////////////////////////////////////
  // Read WMO csv format

  void readWmoCsv(String filename, Map<Integer, Feature> wmoMap) throws IOException {
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Charset.forName("UTF-8")));
    int count = 0;
    int currSeqno = -1;
    Feature currSeq = null;
    while (true) {
      String line = dataIS.readLine();
      count++;

      if (line == null) break;
      if (line.startsWith("#")) continue;

      if (count == 1) {
        System.out.println("header line == " + line);
        continue;
      }

      // commas embedded in quotes - replace with blanks for now
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
        System.out.printf("%d INCOMPLETE line == %s%n", count, line);
        continue;
      }

      int fldidx = 0;
      try {
        int sno = Integer.parseInt(flds[fldidx++]);
        int seq = Integer.parseInt(flds[fldidx++]);
        String codeFigure = flds[fldidx++];
        String desc1 = flds[fldidx++];
        if  (flds.length > 4) desc1 += " & " + flds[fldidx++];
        if  (flds.length > 5) desc1 += " & " + flds[fldidx++];
 
        if (currSeqno != seq) {
          int y = seq % 1000;
          int w = seq / 1000;
          int x = w % 100;
          currSeq = new Feature(x, y);
          wmoMap.put(currSeq.fxy, currSeq);
          currSeqno = seq;
        }

        currSeq.addValue(codeFigure, desc1);

      } catch (Exception e) {
        System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
      }
    }
    int n = wmoMap.values().size();
    System.out.printf("%s lines=%d elems=%d%n", filename, count, n);
  }

  void compare1(Map<Integer, Feature> map) {
    System.out.printf("compare wmo to ours%n");
    int countValues = 0;
    for (Integer key : map.keySet()) {
      Feature f = map.get(key);
      CodeFlagTables t = CodeFlagTables.getTable(key.shortValue());
      if (t == null) {
        System.out.printf("%s missing in ours %n", f);
        continue;
      }
      Map<Integer, String> tm = t.getMap();

      System.out.printf("%s%n", f);
      for (Integer code : f.map.keySet()) {
        String name = f.map.get(code);
        String name2 = tm.get(code);
        if (name2 == null)
          System.out.printf("   %s missing in ours (%s) %n", code, name);
        else if (!equiv(name,name2))
          System.out.printf("   %s %s != %s %n", code, name, name2);
      }

      for (Integer code : tm.keySet()) {
        String name = tm.get(code);
        String name2 = f.map.get(code);
        if (name2 == null)
          System.out.printf("   %s missing in wmo%n", code);
        countValues++;
      }
    }
    System.out.printf("  enums=%s values=%d%n", map.keySet().size(), countValues);

  }

  void compare2(Map<Integer, Feature> map) {
    System.out.printf("compare ours to wmo%n");
    Map<Short, CodeFlagTables> ours = CodeFlagTables.tableMap;
    for (Short key : ours.keySet()) {
      Feature f = map.get(key.intValue());
      if (f == null) {
        CodeFlagTables t = ours.get(key);
        System.out.printf("%s missing in wmo %n", t.fxy());
        continue;
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


  static public void main( String args[]) throws IOException {
    Map<Integer, Feature> wmoMap = new TreeMap<Integer, Feature>();
    CompareCodeTables ct = new CompareCodeTables();
    ct.readWmoCsv("C:/docs/BC_CodeFlagTable.csv", wmoMap);
    ct.compare1(wmoMap);
    ct.compare2(wmoMap);
  }
}
