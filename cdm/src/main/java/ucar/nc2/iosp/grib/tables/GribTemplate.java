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
import ucar.grib.GribNumbers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Read and process WMO GRIB templates.
 *
 * @author caron
 * @since Jul 31, 2010
 */

public class GribTemplate implements Comparable<GribTemplate> {
  static Map<String, String> convertMap = new HashMap<String, String>();
  static Map<String, GribCodeTable> gribCodes;

  static {
    convertMap.put("Type of generating process", "4.3");
    convertMap.put("Indicator of unit of time range", "4.4");
    convertMap.put("Indicator of unit of time for time range over which statistical processing is done", "4.4");
    convertMap.put("Indicator of unit of time for the increment between the successive fields used", "4.4");
    convertMap.put("Type of first fixed surface", "4.5");
    convertMap.put("Type of second fixed surface", "4.5");
    convertMap.put("Type of ensemble forecast", "4.6");
    convertMap.put("Derived forecast", "4.7");
    convertMap.put("Probability type", "4.9");
    convertMap.put("Statistical process used to calculate the processed field from the field at each time increment during the time range", "4.10");
    convertMap.put("Type of time increment between successive fields used in the statistical processing", "4.11");
  }

  static String convert(String table, int value) {
    if (gribCodes == null) {
      try {
        gribCodes = GribCodeTable.readGribCodes();
      } catch (IOException e) {
        return "Read GridCodes failed";
      }
    }

    GribCodeTable gct = gribCodes.get(table);
    if (gct == null) return table+" not found";
    GribCodeTable.TableEntry entry = gct.get(value);
    if (entry != null) return entry.meaning;
    return "Table "+table+" code "+ value+ " not found";
  }


  ///////////////////////////////////////


  public String name, desc;
  public int m1, m2;
  public List<Field> flds = new ArrayList<Field>();

  GribTemplate(String desc) {
    this.desc = desc;

    String[] slist = desc.split(" ");
    for (int i = 0; i < slist.length; i++) {
      if (slist[i].equalsIgnoreCase("template")) {
        name = slist[i + 1];
        String[] slist2 = name.split("\\.");
        if (slist2.length == 2) {
          m1 = Integer.parseInt(slist2[0]);
          m2 = Integer.parseInt(slist2[1]);
        } else
          System.out.println("HEY bad= %s%n" + name);
        break;
      }
    }
  }

  void add(String octet, String content) {
    flds.add(new Field(octet, content));
  }

  void add(int start, int nbytes, String content) {
    flds.add(new Field(start, nbytes, content));
  }

  @Override
  public int compareTo(GribTemplate o) {
    if (m1 == o.m1) return m2 - o.m2;
    else return m1 - o.m1;
  }

  public class Field implements Comparable<Field> {
    public String octet, content;
    public int start, nbytes;

    Field(String octet, String content) {
      this.octet = octet;
      this.content = content;

      try {
        int pos = octet.indexOf('-');
        if (pos > 0) {
          start = Integer.parseInt(octet.substring(0, pos));
          String stops = octet.substring(pos+1);
          int stop = -1;
          try {
            stop = Integer.parseInt(stops);
            nbytes = stop - start + 1;
          } catch (Exception e) {
          }
        } else {
          start = Integer.parseInt(octet);
          nbytes = 1;
        }
      } catch (Exception e) {
        start = -1;
        nbytes = 0;
      }
    }

    Field(int start, int nbytes, String content) {
      this.start = start;
      this.nbytes = nbytes;
      this.content = content;
      this.octet = start+"-"+(start+nbytes-1);
    }

    @Override
    public int compareTo(Field o) {
      return start - o.start;
    }

    int value(byte[] pds) {
      switch (nbytes) {
        case 1 : return get(pds, start);
        case 2 : return GribNumbers.int2( get(pds, start), get(pds, start+1));
        case 4 : return GribNumbers.int4( get(pds, start), get(pds, start+1), get(pds, start+2), get(pds, start+3));
        default : return -9999;
      }
    }

    int get(byte[] pds, int offset) {
      return pds[offset-1] & 0xff;
    }
  }

  public void showInfo(byte[] pds, Formatter f) {
     f.format("%n(%s) %s %n", name, desc);
     for (Field fld : flds) {
       if (fld.start < 0) continue;
       
       String info = convertMap.get(fld.content);
       if (info == null)
         f.format("%3d: %90s == %d %n", fld.start, fld.content, fld.value(pds));
       else {
         String desc = convert(info, fld.value(pds));
         if (desc == null)
           f.format("%3d: %90s == %d (%s) %n", fld.start, fld.content, fld.value(pds), convert(info, fld.value(pds)));
         else
           f.format("%3d: %90s == %d (table %s: %s) %n", fld.start, fld.content, fld.value(pds), info, desc);
       }
     }
  }


  ////////////////////////////////////////////////////////////////////////

  /*
 <ForExport_Templates_E>
    <No>444</No>
    <TemplateName_E>Product definition template 4.4 - derived forecasts based on a cluster of ensemble members over a
      circular area at a horizontal level or in a horizontal layer at a point in time
    </TemplateName_E>
    <OctetNo>61-64</OctetNo>
    <Contents_E>Scaled value of distance of the cluster from ensemble mean</Contents_E>
    <Status>Operational</Status>
  </ForExport_Templates_E>
  */

  static public List<GribTemplate> readXml(InputStream ios) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(ios);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    Map<String, GribTemplate> map = new HashMap<String, GribTemplate>();

    Element root = doc.getRootElement();

    List<Element> featList = root.getChildren("ForExport_Templates_E");
    for (Element elem : featList) {
      String desc = elem.getChildTextNormalize("TemplateName_E");
      String octet = elem.getChildTextNormalize("OctetNo");
      String content = elem.getChildTextNormalize("Contents_E");

      GribTemplate t = map.get(desc);
      if (t == null) {
        t = new GribTemplate(desc);
        map.put(desc, t);
      }
      t.add(octet, content);
    }
    ios.close();

    List<GribTemplate> tlist = new ArrayList<GribTemplate>(map.values());
    Collections.sort(tlist);
    for (GribTemplate t : tlist) {
      if (t.m1 == 3) {
        t.add( 1, 4, "GDS length");
        t.add( 5, 1, "Section");
        t.add( 6, 1, "Source of Grid Definition (see code table 3.0)");
        t.add( 7, 4, "Number of data points");
        t.add( 11, 1, "Number of octects for optional list of numbers");
        t.add( 12, 1, "Interpretation of list of numbers");
        t.add( 13, 2, "Grid Definition Template Number");

      } else if (t.m1 == 4) {
        t.add( 1, 4, "PDS length");
        t.add( 5, 1, "Section");
        t.add( 6, 2, "Number of coordinates values after Template");
        t.add( 8, 2, "Product Definition Template Number");
      }
      Collections.sort(t.flds);
    }
    return tlist;
  }

  static String resourceName = "/resources/grib/wmo/GRIB2_5_2_0_Templates_E.xml";

  static public Map<String, GribTemplate> getParameterTemplates() throws IOException {
    Class c = GribCodeTable.class;
    InputStream in = c.getResourceAsStream(resourceName);
    if (in == null) {
      System.out.printf("cant open %s%n", resourceName);
      return null;
    }

    List<GribTemplate> tlist = readXml(in);

    Map<String, GribTemplate> map = new HashMap<String, GribTemplate>(100);
    for (GribTemplate t : tlist) {
        map.put(t.m1+"."+t.m2, t);
    }
    return map;
  }

  public static List<GribTemplate> getWmoStandard() throws IOException {
    Class c = GribCodeTable.class;
    InputStream in = c.getResourceAsStream(resourceName);
    if (in == null) {
      System.out.printf("cant open %s%n", resourceName);
      return null;
    }
    try {
      return readXml(in);
    } finally {
      in.close();
    }
  }


  public static void main(String arg[]) throws IOException {
    Class c = GribCodeTable.class;
    InputStream in = c.getResourceAsStream(resourceName);
    if (in == null) {
      System.out.printf("cant open %s%n", resourceName);
      return;
    }

    List<GribTemplate> tlist = readXml(in);

    for (GribTemplate t : tlist) {
      System.out.printf("%n(%s) %s %n", t.name, t.desc);
      for (GribTemplate.Field f : t.flds) {
        System.out.printf(" (%d,%d) %10s : %s %n", f.start, f.nbytes, f.octet, f.content);
      }
    }

    for (GribTemplate t : tlist) {
      System.out.printf("%n(%s) %s %n", t.name, t.desc);
      int start = -1;
      int next = 0;
      for (GribTemplate.Field f : t.flds) {
        if (f.start < 0) continue;
        
        if (start < 0) {
          start = f.start;
          next = start + f.nbytes;
        } else {
          if (f.start != next) System.out.printf(" missing %d to %d %n", next, start);
          next = f.start + f.nbytes;
        }
      }
      System.out.printf(" range %d-%d %n", start, next);
    }

  }
}
