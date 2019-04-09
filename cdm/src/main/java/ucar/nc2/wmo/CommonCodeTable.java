/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.wmo;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.io.InputStream;

/**
 * Read and process WMO common code and flag tables.
 *
 * <pre>
 *   see manual on codes I.2 —Co Tab — 1
  COMMON CODE TABLE C–1:   Identification of originating/generating centre GRIB-1, BUFR-3
  COMMON CODE TABLE C–2:   Radiosonde/sounding system used
  COMMON CODE TABLE C–3:   Instrument make and type for water temperature profile measurement with fall rate equation coefficients
  COMMON CODE TABLE C–4:   Water temperature profile recorder types
  COMMON CODE TABLE C–5:   Satellite identifier
  COMMON CODE TABLE C–6:   List of units for TDCFs
  COMMON CODE TABLE C–7:   Tracking techniques/status of system used
  COMMON CODE TABLE C–8:   Satellite instruments
  COMMON CODE TABLE C–11:  Originating/generating centres GRIB-2, BUFR-4
  COMMON CODE TABLE C–12:  Sub-centres of originating centres defined by entries in Common code tables C–1 or C–11
  COMMON CODE TABLE C–13:  Data sub-categories of categories defined by entries in BUFR Table A
  COMMON CODE TABLE C–14:  Atmospheric chemical or physical constituent type

 * </pre>
 * @author caron
 * @since 3/29/11
 */
public class CommonCodeTable implements Comparable<CommonCodeTable> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CommonCodeTable.class);
  private static final Map<Integer, CommonCodeTable> tableMap = new HashMap<>();
  private static final String version = "_20181107_en";

  //////////////////////////////////////////////////////////////////////////
/*
<Common_C13_20130508_en>
<No>35</No>
<CodeFigure_DataCategories>2</CodeFigure_DataCategories>
<Name_DataCategories_en>Vertical soundings (other than satellite)</Name_DataCategories_en>
<CodeFigure_InternationalDataSubcategories>21</CodeFigure_InternationalDataSubcategories>
<Name_InternationalDataSubcategories_en>Profiles of atmospheric constituents concentrations</Name_InternationalDataSubcategories_en>
<Status>Operational</Status>
</Common_C13_20130508_en>

<Common_C01_20141105_en>
<No>6</No>
<CodeFigureForF1F2>04</CodeFigureForF1F2>
<CodeFigureForF3F3F3>004</CodeFigureForF3F3F3>
<Octet5GRIB1_Octet6BUFR3>4</Octet5GRIB1_Octet6BUFR3>
<OriginatingGeneratingCentres_en>Moscow</OriginatingGeneratingCentres_en>
<Status>Operational</Status>
</Common_C01_20141105_en>

   */
  public enum Table {                              // code                code2                  value
    C1("Centers-GRIB1,BUFR3",    1, 1, new String[]{"CodeFigureForF1F2", "CodeFigureForF3F3F3", "OriginatingGeneratingCentres_en"}),
    // C2("Radiosondes",            2, 1, new String[]{"CodeFigureForBUFR", null, "RadiosondeSoundingSystemUsed_en"}),
    C3("Water temperature profile instrument", 3, 1, new String[]{"CodeFigureForBUFR", null, "InstrumentMakeAndType_en"}),
    C4("Water temperature profile recorder",   4, 1, new String[]{"CodeFigureForBUFR", null, "Meaning_en"}),
    C5("Satellite identifier",   5, 1, new String[]{"CodeFigureForBUFR", null, "SatelliteName_en"}),
    C7("Satellite tracking",     7, 1, new String[]{"CodeFigureForBUFR", null, "TrackingTechniquesStatusOfSystemUsed_en"}),
    C8("Satellite instruments",  8, 1, new String[]{"Code", null, "InstrumentLongName_en", "InstrumentShortName_en"}),
    C11("Centers-GRIB2,BUFR4",    11,1, new String[]{"GRIB2_BUFR4", null, "OriginatingGeneratingCentre_en"}),
                                                    // code                            value                       code2                  value2
    C12("Subcenters",             12,2, new String[]{"CodeFigure_OriginatingCentres", "Name_OriginatingCentres_en", "CodeFigure_SubCentres", "Name_SubCentres_en"}),
    C13("Data sub-categories",    13,2, new String[]{"CodeFigure_DataCategories", "Name_DataCategories_en", "CodeFigure_InternationalDataSubcategories", "Name_InternationalDataSubcategories_en"}),
    C14("Atmospheric chemical or physical constituent type",14,2, new String[]{"CodeFigure", "ChemicalFormula", null, "Meaning_en"}),
    ;

    String name;
    String[] elems; // type 1: 0 = code, 1 = code2 (may be null), 2 = value
                    // type 2: 0 = code1, 1 = comment , 2 = code3 (may be null), 3 = value
    int num, type; // type 1 = 1 code, type 2 = 2 codes
    String num0;
    Table(String name, int num, int type, String[] elems) {
      this.name = name;
      this.num = num;
      this.type = type;
      this.elems = elems;

      Formatter f = new Formatter();
      f.format("%02d", num);
      num0 = f.toString();
    }

    public String getResourceName() {
      return "/resources/wmo/Common_C" + this.num0 + version +".xml";
    }

    public String getRootElemName() {
      return "Common_C" + this.num0 + version;
    }

    public int getTableNo() {
      return num;
    }

    public int getTableType() {
      return type;
    }

    String[] getElemNames() {
      return elems;
    }

    public String getName() {
      return name;
    }
  }

  /**
   * Center name, from table C-1 or C-11
   *
   * @param center_id center id
   * @param edition grib edition
   * @return center name, or "unknown"
   */
  public static String getCenterName(int center_id, int edition) {
    String result = (edition == 1) ? getTableValue(1, center_id) : getTableValue(11, center_id);
    if (result != null) return result;
    if (center_id == 0) return "WMO standard table";
    return "Unknown center=" + center_id;
  }

  /**
   * Center name, from table C-1 or C-11
   *
   * @param center_id center id
   * @param edition bufr edition
   * @return center name, or "unknown"
   */
  public static String getCenterNameBufr(int center_id, int edition) {
    String result = (edition < 4) ? getTableValue(1, center_id) : getTableValue(11, center_id);
    if (result != null) return result;
    if (center_id == 0) return "WMO standard table";
    return "Unknown center=" + center_id;
  }

  /**
   * Subcenter name, from table C-12
   *
   * @param center_id    center id
   * @param subcenter_id subcenter id
   * @return subcenter name, or null if not found
   */
  @Nullable
  static public String getSubCenterName(int center_id, int subcenter_id) {
    return getTableValue(12, center_id, subcenter_id);
  }

  /**
   * data subcategory name, from table C-13
   *
   * @param cat    data category
   * @param subcat data subcategory
   * @return subcategory name, or null if not found
   */
  @Nullable
  static public String getDataSubcategoy(int cat, int subcat) {
    return getTableValue(13, cat, subcat);
  }

  public static CommonCodeTable getTable(int tableNo) {
    CommonCodeTable cct = tableMap.get(tableNo);

     if (cct == null) {
       Table want = null;
       for (Table t : Table.values())
         if (t.num == tableNo) {
           want = t;
           break;
         }
       if (want == null)
         throw new IllegalStateException("Unknown wmo common code table number= "+tableNo);

       try {
         cct = readCommonCodes(want);
         tableMap.put(tableNo, cct);
       } catch (IOException e) {
         throw new IllegalStateException("Cant open wmo common code table "+want);
       }
     }

    return cct;
  }

  @Nullable
  public static String getTableValue(int tableNo, int code) {
    CommonCodeTable cct = getTable(tableNo);
    if (cct == null) {
      logger.error("WMO common table {} is not implemented", tableNo);
      return null;
    }
    TableEntry te =  cct.get(code);
    if (te == null) return null;
    return te.value;
  }

  @Nullable
  public static String getTableValue(int tableNo, int code, int code2) {
    CommonCodeTable cct = getTable(tableNo);
    TableEntry te =  cct.get(code, code2);
    if (te == null) return null;
    return te.value;
  }

  static private CommonCodeTable readCommonCodes(Table version) throws IOException {
    InputStream ios = null;
    try {
      Class c = CommonCodeTable.class;
      ios = c.getResourceAsStream(version.getResourceName());
      if (ios == null) {
        throw new IllegalStateException("CommonCodeTable cannot open " + version.getResourceName());
      }

      org.jdom2.Document doc;
      try {
        SAXBuilder builder = new SAXBuilder();
        doc = builder.build(ios);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }
      Element root = doc.getRootElement();
      String previousValue = null;
      CommonCodeTable ct = new CommonCodeTable(version.name(), version.getTableType());
      String[] elems = version.getElemNames();

      List<Element> featList = root.getChildren(version.getRootElemName());
      for (Element elem : featList) {
        if (version.type == 1) {
          String line = elem.getChildTextNormalize("No");
          String code = elem.getChildTextNormalize(elems[0]);
          String code2 = (elems[1] != null) ? elem.getChildTextNormalize(elems[1]) : null;
          String value = elem.getChildTextNormalize(elems[2]);
          //if ((value == null || value.length() == 0) && elems[3] != null) value = elem.getChildTextNormalize(elems[3]);  WTF ?
          Element statusElem = elem.getChild("Status");
          String status = (statusElem == null) ? null : statusElem.getTextNormalize();
          if (value != null && value.equals(")")) value = previousValue;
          ct.add(line, code, code2, value, status);
          previousValue = value;

        } else {
          String line = elem.getChildTextNormalize("No");
          String code = elem.getChildTextNormalize(elems[0]);
          String value = elem.getChildTextNormalize(elems[1]);
          String code2 = (elems[2] == null) ? null : elem.getChildTextNormalize(elems[2]);
          String value2 = elem.getChildTextNormalize(elems[3]);
          Element statusElem = elem.getChild("Status");
          String status = (statusElem == null) ? null : statusElem.getTextNormalize();
          ct.add(line, code, value, code2, value2, status);
        }
      }

      ios.close();
      return ct;

    } finally {
      if (ios != null)
        ios.close();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////

  public final String tableName;
  public final int type;
  public final List<TableEntry> entries = new ArrayList<>();
  private Map<Integer, String> map = null;

  CommonCodeTable(String name, int type) {
    this.tableName = name;
    this.type = type;
  }

  void add(String line, String code, String code2, String value, String status) {
    entries.add(new TableEntry(line, code, code2, value, status));
  }

  void add(String line, String code, String value, String code2, String value2, String status) {
    entries.add(new TableEntry(line, code, value, code2, value2, status));
  }

  // look replace with hash or array
  @Nullable
  TableEntry get(int code) {
    for (TableEntry p : entries) {
      if (p.code == code) return p;
    }
    return null;
  }

  @Nullable
  TableEntry get(int code, int code2) {
    for (TableEntry p : entries) {
      if ((p.code == code) && (p.code2 == code2))return p;
    }
    return null;
  }

  public String getTableName() {
    return tableName;
  }

  public Map<Integer, String> getMap() {
    if (map == null) {
      map = new HashMap<>(entries.size() * 2);
      for (TableEntry p : entries) {
        map.put(p.code, p.value);
      }
    }
    return map;
  }

  @Override
  public int compareTo(CommonCodeTable o) {
    return tableName.compareTo(o.tableName);
  }

  String state() {
    Set<Integer> set = new HashSet<>();
    int max = 0;
    int dups = 0;
    for (TableEntry entry : entries) {
      if (entry.comment != null) continue;
      if (entry.code > max) max = entry.code;
      if (set.contains(entry.code)) dups++;
      else set.add(entry.code);
    }
    return "density= "+entries.size() + "/" + max+"; dups= "+dups;
  }

  public class TableEntry implements Comparable<TableEntry> {
    public int line, code, code2;
    public String value, status, comment;

    int parse(String s) {
      if (s == null) return -1;
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e2) {
        return -2;
      }
    }

    TableEntry(String line, String code1, String code2, String value, String status) {
      this.line = Integer.parseInt(line);
      this.code = parse(code1);
      if (this.code < 0)
        this.code = parse(code2);
      if (this.code < 1)
        comment = value;
      else {
        this.value = filter(value);
        this.status = status;
      }
    }

    TableEntry(String line, String code1, String value1, String code2, String value2, String status) {
      this.line = Integer.parseInt(line);
      this.code = parse(code1);
      this.code2 = parse(code2);
      this.value = filter(value2);
      this.comment = value1;
      this.status = status;
    }

    @Override
    public int compareTo(TableEntry o) {
      if (type == 1)
        return Integer.compare(code, o.code);
      else {
        int diff = Integer.compare(code, o.code);
        return (diff == 0) ?  Integer.compare(code2, o.code2) : diff;
      }
    }

    @Override
    public String toString() {
      if (comment != null)
        return "TableEntry{" +
              ", line=" + line +
              ", comment=" + comment +
                '}';

      else return "TableEntry{" +
              ", line=" + line +
              ", code=" + code +
              ", value='" + value + '\'' +
              ", status='" + status + '\'' +
              '}';
    }
  }

  private static final char badDash = 173;
  private String filter(String s) {
    if (s == null) return "";
    return StringUtil2.replace(s, badDash, "-");
  }

  public static void main(String arg[]) throws IOException {
    CommonCodeTable ct = readCommonCodes(Table.C1);
    for (TableEntry entry : ct.entries)
      System.out.printf("%s%n", entry);
    System.out.printf("%n%s%n", ct.state());
  }
}