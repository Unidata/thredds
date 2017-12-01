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
package ucar.nc2.grib.grib2.table;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.grib.GribNumbers;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Read and process WMO GRIB templates from standard XML.
 *
 * @author caron
 * @since Jul 31, 2010
 */

public class WmoTemplateTable implements Comparable<WmoTemplateTable> {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WmoTemplateTable.class);

  public static final Version standard = Version.GRIB2_20_0_0;

  public enum Version {
    // GRIB2_5_2_0, GRIB2_6_0_1, GRIB2_7_0_0, GRIB2_8_0_0, GRIB2_10_0_1, GRIB2_13_0_1;
    GRIB2_20_0_0;

    String getResourceName() {
      return "/resources/grib2/wmo/" + this.name() + "_Template_en.xml";
    }

    String[] getElemNames() {

      if (this == GRIB2_20_0_0)
        return new String[]{"GRIB2_20_0_0_Template_en", "Title_en", "Note_en", "Contents_en"};

      return null;
    }
  }

  static public class GribTemplates {
    public List<WmoTemplateTable> list;
    public Map<String, WmoTemplateTable> map; // key is "disc.cat"

    public GribTemplates(List<WmoTemplateTable> list, Map<String, WmoTemplateTable> map) {
      this.list = list;
      this.map = map;
    }
  }

  /*

  <GRIB2_10_0_1_Template_en>
    <No>817</No>
    <Title_en>Product definition template 4.14 - derived forecasts based on a cluster of ensemble members over a circular area at a horizontal level or in a horizontal layer in a continuous or non-continuous time interval</Title_en>
    <OctetNo>11</OctetNo>
    <Contents_en>Parameter number</Contents_en>
    <Note_en>(see Code table 4.2)</Note_en>
    <Status>Operational</Status>
  </GRIB2_10_0_1_Template_en>

  <GRIB2_8_0_0_Template_en>
    <No>1065</No>
    <Title_en>Product definition template 4.50 - analysis or forecast of a multi component parameter or matrix element
      at a point in time
    </Title_en>
    <OctetNo>18</OctetNo>
    <Contents_en>Indicator of unit of time range</Contents_en>
    <Note_en>(see Code table 4.4)</Note_en>
    <Status>Validation</Status>
  </GRIB2_8_0_0_Template_en>


 5.2

 <ForExport_Templates_E>
   <No>1037</No>
   <TemplateName_E>Product definition template 4.47 - individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer in a continuous or non-continuous time interval for aerosol</TemplateName_E>
   <OctetNo>11</OctetNo>
   <Contents_E>Parameter number</Contents_E>
   <Nindicator_E>(see Code table 4.2)</Nindicator_E>
   <Status>Validation</Status>
 </ForExport_Templates_E>

 6.1

 <Exp_template_E>
   <No>903</No>
   <Title_E>Product definition template 4.43 - individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer in a continuous or non-continuous time interval for atmospheric chemical constituents</Title_E>
   <OctetNo>20</OctetNo>
   <Contents_E>Indicator of unit of time range</Contents_E>
   <Note_E>(see Code table 4.4)#GRIB2_6_0_1_codeflag.doc#G2_CF44</Note_E>
   <Status>Operational</Status>
 </Exp_template_E>

 7.0
<Exp_Temp_E>
  <No>15</No>
  <Title_E>Grid definition template 3.0 - latitude/longitude (or equidistant cylindrical, or Plate Carrée)</Title_E>
  <OctetNo>56-59</OctetNo>
  <Contents_E>La2 - latitude of last grid point</Contents_E>
  <Note_E>(see Note 1)#GRIB2_7_0_0_Temp.doc#G2_Gdt30n</Note_E>
  <Status>Operational</Status>
</Exp_Temp_E>

  */

  static public GribTemplates readXml(Version version) throws IOException {
    try (InputStream ios = WmoCodeTable.class.getResourceAsStream(version.getResourceName())) {
      if (ios == null) {
        System.out.printf("cant open %s%n", version.getResourceName());
        return null;
      }

      org.jdom2.Document doc;
      try {
        SAXBuilder builder = new SAXBuilder();
        doc = builder.build(ios);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }

      Map<String, WmoTemplateTable> map = new HashMap<>();
      String[] elems = version.getElemNames();
      assert elems != null;
      assert elems.length > 3;

      Element root = doc.getRootElement();
      List<Element> featList = root.getChildren(elems[0]); // 0 = main element
      for (Element elem : featList) {
        String desc = elem.getChildTextNormalize(elems[1]); // 1 = title
        String octet = elem.getChildTextNormalize("OctetNo");
        String content = elem.getChildTextNormalize(elems[3]); // 3 = content
        String status = elem.getChildTextNormalize("Status");
        String note = elem.getChildTextNormalize(elems[2]); // 2 == note

        WmoTemplateTable t = map.get(desc);
        if (t == null) {
          t = new WmoTemplateTable(desc);
          map.put(desc, t);
        }
        t.add(octet, content, status, note);
      }
      ios.close();

      List<WmoTemplateTable> tlist = new ArrayList<>(map.values());
      Collections.sort(tlist);
      for (WmoTemplateTable t : tlist) {
        if (t.m1 == 3) {
          t.add(1, 4, "GDS length");
          t.add(5, 1, "Section");
          t.add(6, 1, "Source of Grid Definition (see code table 3.0)");
          t.add(7, 4, "Number of data points");
          t.add(11, 1, "Number of octects for optional list of numbers");
          t.add(12, 1, "Interpretation of list of numbers");
          t.add(13, 2, "Grid Definition Template Number");

        } else if (t.m1 == 4) {
          t.add(1, 4, "PDS length");
          t.add(5, 1, "Section");
          t.add(6, 2, "Number of coordinates values after Template");
          t.add(8, 2, "Product Definition Template Number");
        }
        Collections.sort(t.flds);
      }

      Map<String, WmoTemplateTable> map2 = new HashMap<>(100);
      for (WmoTemplateTable t : tlist) {
        map2.put(t.m1 + "." + t.m2, t);
      }

      return new GribTemplates(tlist, map2);
    }
  }


  // static private Map<String, WmoCodeTable> gribCodes;

  public static GribTemplates getWmoStandard() throws IOException {
    return readXml(standard);
  }

  ///////////////////////////////////////


  public String name, desc;
  public int m1, m2;
  public List<Field> flds = new ArrayList<>();

  private WmoTemplateTable(String desc) {
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
          logger.warn("WmoTemplateTable bad= {}", name);
        break;
      }
    }
  }

  void add(String octet, String content, String status, String note) {
    flds.add(new Field(octet, content, status, note));
  }

  void add(int start, int nbytes, String content) {
    flds.add(new Field(start, nbytes, content));
  }

  @Override
  public int compareTo(WmoTemplateTable o) {
    if (m1 == o.m1) return m2 - o.m2;
    else return m1 - o.m1;
  }

  public static class Field implements Comparable<Field> {
    public String octet, content, status, note;
    public int start, nbytes;

    Field(String octet, String content, String status, String note) {
      this.octet = octet;
      this.content = content;
      this.status = status;
      this.note = note;

      try {
        int pos = octet.indexOf('-');
        if (pos > 0) {
          start = Integer.parseInt(octet.substring(0, pos));
          String stops = octet.substring(pos + 1);
          try {
            int stop = Integer.parseInt(stops);
            nbytes = stop - start + 1;
          } catch (Exception e) {
            logger.debug("Error parsing wmo template line=" + content, e);
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
      this.octet = start + "-" + (start + nbytes - 1);
    }

    @Override
    public int compareTo(Field o) {
      return start - o.start;
    }

    int value(byte[] pds) {
      switch (nbytes) {
        case 1:
          return get(pds, start);
        case 2:
          return GribNumbers.int2(get(pds, start), get(pds, start + 1));
        case 4:
          return GribNumbers.int4(get(pds, start), get(pds, start + 1), get(pds, start + 2), get(pds, start + 3));
        default:
          return -9999;
      }
    }

    int get(byte[] pds, int offset) {
      return pds[offset - 1] & 0xff;
    }
  }

  static private final Map<String, String> convertMap = new HashMap<>();
  static {
    // gds
    convertMap.put("Source of Grid Definition (see code table 3.0)", "3.0");
    convertMap.put("Shape of the Earth", "3.2");
    convertMap.put("Interpretation of list of numbers", "3.11");

    // pds
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

    convertMap.put("Analysis or forecast generating process identifier (defined by originating centre)", "ProcessId"); // ??
    convertMap.put("Background generating process identifier (defined by originating centre)", "ProcessId");
    convertMap.put("Forecast generating process identifier (defined by originating centre)", "ProcessId");
  }

  public void showInfo(Grib2Customizer tables, byte[] raw, Formatter f) {
    f.format("%n(%s) %s %n", name, desc);
    for (Field fld : flds) {
      if (fld.start < 0) continue;

      String info = convertMap.get(fld.content);
      if (info == null)
        f.format("%3d: %90s == %d %n", fld.start, fld.content, fld.value(raw));
      else {
        String desc = convert(tables, info, fld.value(raw));
        if (desc == null)
          f.format("%3d: %90s == %d (%s) %n", fld.start, fld.content, fld.value(raw), convert(tables, info, fld.value(raw)));
        else
          f.format("%3d: %90s == %d (table %s: %s) %n", fld.start, fld.content, fld.value(raw), info, desc);
      }
    }
  }

  /*
    private String convert(String table, int value) {
    if (gribCodes == null) {
      try {
        gribCodes = GribCodeTableWmo.getWmoStandard().map;
      } catch (IOException e) {
        return "Read GridCodes failed";
      }
    }

    GribCodeTableWmo gct = gribCodes.get(table);
    if (gct == null) return table + " not found";
    GribCodeTableWmo.TableEntry entry = gct.get(value);
    if (entry != null) return entry.meaning;
    return "Table " + table + " code " + value + " not found";
  } */

  private String convert(Grib2Customizer tables, String table, int value) {
    String result = tables.getTableValue(table, value);
    return (result != null) ? result : "Table " + table + " code " + value + " not found";
  }

  public static void main(String arg[]) throws IOException {
    List<WmoTemplateTable> tlist = readXml(standard).list;

    for (WmoTemplateTable t : tlist) {
      System.out.printf("%n(%s) %s %n", t.name, t.desc);
      for (WmoTemplateTable.Field f : t.flds) {
        System.out.printf(" (%d,%d) %10s : %s %n", f.start, f.nbytes, f.octet, f.content);
      }
    }

    for (WmoTemplateTable t : tlist) {
      System.out.printf("%n(%s) %s %n", t.name, t.desc);
      int start = -1;
      int next = 0;
      for (WmoTemplateTable.Field f : t.flds) {
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
