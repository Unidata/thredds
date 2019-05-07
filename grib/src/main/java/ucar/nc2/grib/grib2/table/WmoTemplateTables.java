/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.grib2.table;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.grib.GribNumbers;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Read and process WMO GRIB templates from standard WMO source file.
 *
 * @author caron
 * @since Jul 31, 2010
 */

public class WmoTemplateTables {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WmoTemplateTables.class);
  private static final Map<String, String> convertMap = new HashMap<>();
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
    convertMap.put(
        "Statistical process used to calculate the processed field from the field at each time increment during the time range",
        "4.10");
    convertMap.put("Type of time increment between successive fields used in the statistical processing", "4.11");

    convertMap.put("Analysis or forecast generating process identifier (defined by originating centre)", "ProcessId"); // ??
    convertMap.put("Background generating process identifier (defined by originating centre)", "ProcessId");
    convertMap.put("Forecast generating process identifier (defined by originating centre)", "ProcessId");
  }

  public static final Version standard = Version.GRIB2_22_0_0;
  public enum Version {
    GRIB2_22_0_0;

    String getResourceName() {
      return "/resources/grib2/wmo/" + this.name() + "_Template_en.xml";
    }

    @Nullable
    String[] getElemNames() {
      if (this == GRIB2_22_0_0) {
        return new String[]{"GRIB2_22_0_0_Template_en", "Title_en", "Note_en", "Contents_en"};
      }
      return null;
    }
  }

  private static WmoTemplateTables instance = null;

  public static WmoTemplateTables getInstance() {
    if (instance == null) {
      instance = new WmoTemplateTables();
      try {
        instance.readXml(standard);
      } catch (IOException e) {
        logger.error("Cant read WMO Grib2 tables");
        throw new RuntimeException(e);
      }
    }
    return instance;
  }

  /*
  <GRIB2_22_0_0_Template_en>
    <No>1451</No>
    <Title_en>Product definition template 4.55 - spatio-temporal changing tiles at a horizontal level or horizontal layer at a point in time</Title_en>
    <OctetNo>35</OctetNo>
    <Contents_en>Type of second fixed surface</Contents_en>
    <Note_en>(see Code table 4.5)</Note_en>
    <Status>Operational</Status>
  </GRIB2_22_0_0_Template_en>
  */

  private void readXml(Version version) throws IOException {
    try (InputStream ios = WmoTemplateTables.class.getResourceAsStream(version.getResourceName())) {
      if (ios == null) {
        throw new IOException("cant open TemplateTable %s " + version.getResourceName());
      }

      org.jdom2.Document doc;
      try {
        SAXBuilder builder = new SAXBuilder();
        doc = builder.build(ios);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }

      Map<String, TemplateTable> map = new HashMap<>();
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

        TemplateTable template = map.computeIfAbsent(desc, name -> new TemplateTable(name));
        template.add(octet, content, status, note);
      }
      ios.close();

      List<TemplateTable> tlist = new ArrayList<>(map.values());
      for (TemplateTable t : tlist) {
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

      this.templateTables = map.values().stream().sorted().collect(ImmutableList.toImmutableList());
      ImmutableMap.Builder<String, TemplateTable> builder = ImmutableMap.builder();
      map.values().forEach(t -> builder.put(t.getId(), t));
      this.templateMap = builder.build();
    }
  }

  private ImmutableList<TemplateTable> templateTables;
  private ImmutableMap<String, TemplateTable> templateMap;

  public ImmutableList<TemplateTable> getTemplateTables() {
    return templateTables;
  }

  @Nullable
  public TemplateTable getTemplateTable(String key) {
    return templateMap.get(key);
  }

  public class TemplateTable implements Comparable<TemplateTable> {
    private String name, desc;
    private int m1, m2;
    private final List<Field> flds = new ArrayList<>();

    private TemplateTable(String desc) {
      this.desc = desc;

      String[] slist = desc.split(" ");
      for (int i = 0; i < slist.length; i++) {
        if (slist[i].equalsIgnoreCase("template")) {
          name = slist[i + 1];
          String[] slist2 = name.split("\\.");
          if (slist2.length == 2) {
            m1 = Integer.parseInt(slist2[0]);
            m2 = Integer.parseInt(slist2[1]);
          } else {
            logger.warn("TemplateTable bad= {}", name);
          }
          break;
        }
      }
    }

    private void add(String octet, String content, String status, String note) {
      flds.add(new Field(octet, content, status, note));
    }

    private void add(int start, int nbytes, String content) {
      flds.add(new Field(start, nbytes, content));
    }

    public String getId() {
      return m1 + "." + m2;
    }

    public String getName() {
      return name;
    }

    public String getDesc() {
      return desc;
    }

    public int getM1() {
      return m1;
    }

    public int getM2() {
      return m2;
    }

    public ImmutableList<Field> getFlds() {
      return ImmutableList.copyOf(flds);
    }

    @Override
    public int compareTo(@Nonnull TemplateTable o) {
      if (m1 == o.m1) {
        return m2 - o.m2;
      } else {
        return m1 - o.m1;
      }
    }

    public void showInfo(Grib2Tables tables, byte[] raw, Formatter f) {
      f.format("%n(%s) %s %n", name, desc);
      for (Field fld : flds) {
        if (fld.start < 0) {
          continue;
        }

        String info = convertMap.get(fld.content);
        if (info == null) {
          f.format("%3d: %90s == %d %n", fld.start, fld.content, fld.value(raw));
        } else {
          String desc = convert(tables, info, fld.value(raw));
          if (desc == null) {
            f.format("%3d: %90s == %d (%s) %n", fld.start, fld.content, fld.value(raw), convert(tables, info, fld.value(raw)));
          } else {
            f.format("%3d: %90s == %d (table %s: %s) %n", fld.start, fld.content, fld.value(raw), info, desc);
          }
        }
      }
    }

    private String convert(Grib2Tables tables, String table, int value) {
      String result = tables.getCodeTableValue(table, value);
      return (result != null) ? result : "Table " + table + " code " + value + " not found";
    }
  }

  public class Field implements Comparable<Field> {
    private final String octet;
    private final String content;
    private String status;
    private String note;
    private int start, nbytes;

    private Field(String octet, String content, String status, String note) {
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

    private Field(int start, int nbytes, String content) {
      this.start = start;
      this.nbytes = nbytes;
      this.content = content;
      this.octet = start + "-" + (start + nbytes - 1);
    }

    public String getOctet() {
      return octet;
    }

    public String getContent() {
      return content;
    }

    public String getStatus() {
      return status;
    }

    public String getNote() {
      return note;
    }

    public int getStart() {
      return start;
    }

    public int getNbytes() {
      return nbytes;
    }

    @Override
    public int compareTo(@Nonnull Field o) {
      return start - o.start;
    }

    private int value(byte[] pds) {
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

    private int get(byte[] pds, int offset) {
      return pds[offset - 1] & 0xff;
    }
  }
}
