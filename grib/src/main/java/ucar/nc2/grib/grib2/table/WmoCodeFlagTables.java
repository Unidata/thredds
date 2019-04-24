package ucar.nc2.grib.grib2.table;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.wmo.Util;

/**
 * Read and manage the WMO GRIB2 Code, Flag, and Parameter tables, in their standard XML format
 */
public class WmoCodeFlagTables {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WmoCodeFlagTables.class);
  public static final Version standard = Version.GRIB2_22_0_0;

  public enum Version {
    GRIB2_22_0_0;

    public String getResourceName() {
      return "/resources/grib2/wmo/" + this.name() + "_CodeFlag_exp_en.xml";
    }

    @Nullable
    String[] getElemNames() {
      if (this == GRIB2_22_0_0) {
        return new String[]{"GRIB2_22_0_0_CodeFlag_exp_en", "Title_en", "SubTitle_en", "MeaningParameterDescription_en",
            "UnitComments_en"};
      }

      return null;
    }
  }

  public enum TableType {param, code, flag, cat}

  private static WmoCodeFlagTables instance = null;

  public static WmoCodeFlagTables getInstance() {
    if (instance == null) {
      instance = new WmoCodeFlagTables();
      try {
        instance.readGribCodes(standard);
      } catch (IOException e) {
        logger.error("Cant read WMO Grib2 tables");
        throw new RuntimeException(e);
      }
    }
    return instance;
  }

/////////////////////////////////////////

  private ImmutableList<WmoTable> wmoTables;
  private ImmutableMap<String, WmoTable> wmoTableMap;

  public ImmutableList<WmoTable> getWmoTables() {
    return wmoTables;
  }

  @Nullable
  public TableType getTableType(String tableName) {
    WmoTable wmoTable = wmoTableMap.get(tableName);
    if (wmoTable == null) {
      return null;
    }
    return wmoTable.getType();
  }

  @Nullable
  public WmoCodeTable getCodeTable(String tableName) {
    WmoTable wmoTable = wmoTableMap.get(tableName);
    if (wmoTable == null) {
      return null;
    }
    return new WmoCodeTable(wmoTable);
  }

  @Nullable
  public WmoCodeTable getCodeTable(int m1, int m2) {
    String name = String.format("%d.%d", m1, m2);
    WmoTable wmoTable = wmoTableMap.get(name);
    if (wmoTable == null) {
      return null;
    }
    return new WmoCodeTable(wmoTable);
  }

  @Nullable
  public WmoParamTable getParamTable(int discipline, int category) {
    String name = String.format("4.2.%d.%d", discipline, category);
    WmoTable wmoTable = wmoTableMap.get(name);
    if (wmoTable == null) {
      return null;
    }
    return new WmoParamTable(wmoTable);
  }

  /*
  Param Table:
  <GRIB2_22_0_0_CodeFlag_exp_en>
    <No>524</No>
    <Title_en>Code table 4.2 - Parameter number by product discipline and parameter category</Title_en>
    <SubTitle_en>Product discipline 0 - Meteorological products, parameter category 1: moisture</SubTitle_en>
    <CodeFlag>101</CodeFlag>
    <MeaningParameterDescription_en>Specific number concentration of snow</MeaningParameterDescription_en>
    <UnitComments_en>kg-1</UnitComments_en>
    <ElementDescription_en>Number of particles per unit mass of air</ElementDescription_en>
    <Status>Operational</Status>
  </GRIB2_22_0_0_CodeFlag_exp_en>

  Code Table:
  <GRIB2_22_0_0_CodeFlag_exp_en>
    <No>2</No>
    <Title_en>Code table 0.0 - Discipline of processed data in the GRIB message, number of GRIB Master table</Title_en>
    <CodeFlag>1</CodeFlag>
    <MeaningParameterDescription_en>Hydrological products</MeaningParameterDescription_en>
    <Status>Operational</Status>
  </GRIB2_22_0_0_CodeFlag_exp_en>

  FlagTable:
  <GRIB2_22_0_0_CodeFlag_exp_en>
    <No>168</No>
    <Title_en>Flag table 3.4 - Scanning mode</Title_en>
    <CodeFlag>1</CodeFlag>
    <Value>0</Value>
    <MeaningParameterDescription_en>Points of first row or column scan in the +i (+x) direction</MeaningParameterDescription_en>
    <Status>Operational</Status>
  </GRIB2_22_0_0_CodeFlag_exp_en>
  <GRIB2_22_0_0_CodeFlag_exp_en>
    <No>169</No>
    <Title_en>Flag table 3.4 - Scanning mode</Title_en>
    <CodeFlag>1</CodeFlag>
    <Value>1</Value>
    <MeaningParameterDescription_en>Points of first row or column scan in the -i (-x) direction</MeaningParameterDescription_en>
    <Status>Operational</Status>
  </GRIB2_22_0_0_CodeFlag_exp_en>
  */

  private void readGribCodes(Version version) throws IOException {
    String[] elems = version.getElemNames();
    if (elems == null) {
      throw new IllegalStateException("unknown version = " + version);
    }

    try (InputStream ios = WmoCodeFlagTables.class.getResourceAsStream(version.getResourceName())) {
      if (ios == null) {
        logger.error("cant open WmoCodeTable=" + version.getResourceName());
        throw new IOException("cant open WmoCodeTable=" + version.getResourceName());
      }

      org.jdom2.Document doc;
      try {
        SAXBuilder builder = new SAXBuilder();
        doc = builder.build(ios);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }
      Element root = doc.getRootElement();

      Map<String, WmoTable> map = new HashMap<>();

      List<Element> featList = root.getChildren(elems[0]); // main element
      for (Element elem : featList) {
        String line = elem.getChildTextNormalize("No");
        String tableName = elem.getChildTextNormalize(elems[1]); // Title_en
        Element subtableElem = elem.getChild(elems[2]); // "SubTitle_en"

        TableType type;
        if (tableName.startsWith("Code table 4.1 ")) {
          type = TableType.cat;
        } else if (tableName.startsWith("Code table 4.2 ")) {
          type = TableType.param;
        } else if (tableName.startsWith("Flag")) {
          type = TableType.flag;
        } else if (tableName.startsWith("Code")) {
          type = TableType.code;
        } else {
          logger.warn("Unknown wmo table entry = '%s'", tableName);
          continue;
        }

        if (subtableElem != null) {
          tableName = subtableElem.getTextNormalize();
        }

        TableType finalType = type;
        WmoTable wmoTable = map.computeIfAbsent(tableName, name -> new WmoTable(name, finalType));

        String code = elem.getChildTextNormalize("CodeFlag");
        String value = elem.getChildTextNormalize("Value");    // Flag table only
        String meaning = elem.getChildTextNormalize(elems[3]); // MeaningParameterDescription_en

        Element unitElem = elem.getChild(elems[4]); // "UnitComments_en"
        String unit = (unitElem == null) ? null : unitElem.getTextNormalize();

        Element statusElem = elem.getChild("Status");
        String status = (statusElem == null) ? null : statusElem.getTextNormalize();

        wmoTable.addEntry(line, code, value, meaning, unit, status);
      }
      ios.close();

      this.wmoTables = map.values().stream().sorted().collect(ImmutableList.toImmutableList());
      ImmutableMap.Builder<String, WmoTable> builder = ImmutableMap.builder();
      map.values().forEach(t -> builder.put(t.getId(), t));
      this.wmoTableMap = builder.build();
    }
  }

  public static class WmoTable implements Comparable<WmoTable> {

    private final String name;
    private final TableType type;
    private final List<WmoEntry> entries = new ArrayList<>();
    private final String id;
    private int m1 = -1, m2 = -1, discipline = -1, category = -1;

    private WmoTable(String name, TableType type) {
      this.name = name;
      this.type = type;

      // extract the table id from the name.
      if (type == TableType.cat) {
        m1 = 4;
        m2 = 1;
        String[] slist = name.split("[ :]+");
        for (int i = 0; i < slist.length; i++) {
          if (slist[i].equalsIgnoreCase("discipline")) {
            discipline = Integer.parseInt(slist[i + 1]);
          }
        }
        if (discipline < 0) {
          throw new IllegalArgumentException("Cant extract param id from table " + name);
        }
        this.id = String.format("%d.%d.%d", m1, m2, discipline);

      } else if (type == TableType.param) {
        m1 = 4;
        m2 = 2;
        String[] slist = name.split("[ :]+");
        for (int i = 0; i < slist.length; i++) {
          if (slist[i].equalsIgnoreCase("discipline")) {
            discipline = Integer.parseInt(slist[i + 1]);
          }
          if (slist[i].equalsIgnoreCase("category")) {
            category = Integer.parseInt(slist[i + 1]);
          }
        }
        if (discipline < 0 || category < 0) {
          throw new IllegalArgumentException("Cant extract param id from table " + name);
        }
        this.id = String.format("%d.%d.%d.%d", m1, m2, discipline, category);

      } else {
        String[] s = name.split(" ");
        String id = s[2];
        String[] slist2 = id.split("\\.");
        if (slist2.length == 2) {
          m1 = Integer.parseInt(slist2[0]);
          m2 = Integer.parseInt(slist2[1]);
        } else {
          logger.warn("WmoCodeTable bad= %s%n" + name);
        }
        if (m1 < 0 || m2 < 0) {
          throw new IllegalArgumentException("Cant extract id from table " + name);
        }
        this.id = String.format("%d.%d", m1, m2);
      }
    }

    private WmoEntry addEntry(String line, String code, String value, String meaning, String unit, String status) {
      WmoEntry entry = new WmoEntry(line, code, value, meaning, unit, status);
      boolean isRange = (entry.start != entry.stop);
      if (!isRange) {
        entries.add(entry);
      }
      return entry;
    }

    @Override
    public int compareTo(@Nonnull WmoTable o) {
      if (m1 != o.m1) {
        return m1 - o.m1;
      }
      if (m2 != o.m2) {
        return m2 - o.m2;
      }
      if (discipline != o.discipline) {
        return discipline - o.discipline;
      }
      return category - o.category;
    }

    public String getName() {
      return name;
    }

    public TableType getType() {
      return type;
    }

    public String getId() {
      return id;
    }

    public ImmutableList<WmoEntry> getEntries() {
      return ImmutableList.copyOf(entries);
    }

    public class WmoEntry {
      private final int start, stop, line;
      private final int number, value;
      private final String code, meaning, name, unit, status;

      WmoEntry(String line, String code, String valueS, String meaning, String unit,
          String status) {
        this.line = Integer.parseInt(line);
        this.code = code;
        this.meaning = meaning;
        this.status = status;
        this.name = meaning;

        String unitW = unit;
        int startW, stopW, numberW = 0;

        try {
          int pos = code.indexOf('-');
          if (pos > 0) {
            startW = Integer.parseInt(code.substring(0, pos));
            String stops = code.substring(pos + 1);
            stopW = Integer.parseInt(stops);
          } else {
            startW = Integer.parseInt(code);
            stopW = startW;
            numberW = startW;
          }
        } catch (Exception e) {
          startW = -1;
          stopW = 0;
        }
        this.start = startW;
        this.stop = stopW;
        this.number = numberW;

        int valueW = -1;
        if (valueS != null) {
          try {
            valueW = Integer.parseInt(valueS);
          } catch (Exception e) {
            valueW = -2;
          }
        }
        this.value = valueW;

        if (type == TableType.param) {
          // massage units
          if (unit != null) {
            unitW = Util.cleanUnit(unit);
          } else {
            unitW = ""; // no null unit allowed
          }
        }
        this.unit = unitW;
      }

      public int getLine() {
        return line;
      }

      public int getStart() {
        return start;
      }

      public int getStop() {
        return stop;
      }

      public int getNumber() {
        return number;
      }

      public int getValue() {
        return value;
      }

      public String getCode() {
        return code;
      }

      public String getMeaning() {
        return meaning;
      }

      public String getName() {
        return name;
      }

      public String getUnit() {
        return unit;
      }

      public String getStatus() {
        return status;
      }

      public String getId() {
        return WmoTable.this.getId() + "." + getNumber();
      }

      public int getCategory() {
        return WmoTable.this.category;
      }

      public int getDiscipline() {
        return WmoTable.this.discipline;
      }
    }
  }

  /*


  @Nullable
  public static String getTableValue(String tableId, int value) {
    if (wmoTables == null)
      try {
        wmoTables = getWmoStandard();
      } catch (IOException e) {
        throw new IllegalStateException("cant open WMO tables");
      }

    WmoCodeTable table = wmoTables.map.get(tableId);
    if (table == null) return null;
    TableEntry entry = table.get(value);
    if (entry == null) return null;
    return entry.meaning;
  }

  @Nullable
  private static TableEntry getTableEntry(String tableId, int value) {
    if (wmoTables == null)
      try {
        wmoTables = getWmoStandard();
      } catch (IOException e) {
        throw new IllegalStateException("cant open wmo tables");
      }

    WmoCodeTable table = wmoTables.map.get(tableId);
    if (table == null) return null;
    return table.get(value);
  }

  void wtf(Formatter f) {

    int total = 0;
    int nsame = 0;
    int nsameIgn = 0;
    int ndiff = 0;
    int unknown = 0;

    f.format("DIFFERENCES between %s and %s%n", gt1.name, gt2.name);
    for (WmoCodeTable gc1 : gt1.list) {

    WmoCodeTable gc2 = gt2.map.get(gc1.tableName);
    if (gc2 == null) {
      f.format("1 table %s not found in %s%n", gc1.getTableId(), gt2.name);
      continue;
    }

    for (TableEntry p1 : gc1.entries) {
      TableEntry p2 = gc2.get(p1.start);
      if (p2 == null) {
        f.format("2 code %s not found in %s%n", p1.getId(), gt2.name);
        continue;
      }

      if (showDiff && !p1.equals(p2)) {
        f.format("3 %s not equal%n  %s%n%n", p1, p2);
      }
    }
  }
    f.format("Total=%d same=%d sameIgn=%d dif=%d unknown=%d%n", total, nsame, nsameIgn, ndiff, unknown);
}


  /* public static void showDiffFromCurrent(List<WmoCodeTable> tlist) throws IOException {
    int total = 0;
    int nsame = 0;
    int nsameIgn = 0;
    int ndiff = 0;
    int unknown = 0;

    f.format("DIFFERENCES with current parameter table (now,org) %n");
    for (WmoCodeTable gt : tlist) {
      if (!gt.isParameter) continue;
      for (TableEntry p : gt.entries) {
        if (p.meaning.equalsIgnoreCase("Missing")) continue;
        if (p.start != p.stop) continue;

        GridParameter gp = ParameterTable.getParameter(gt.discipline, gt.category, p.start);
        String paramOrg = gp.getDescription();
        if (paramOrg.startsWith("Unknown")) {
          unknown++;
          continue;
        }

        String paramOrgM = munge(paramOrg);
        String paramM = munge(p.name);
        boolean same = paramOrgM.equals(paramM);
        if (same) nsame++;
        boolean sameIgnore = paramOrgM.equalsIgnoreCase(paramM);
        if (sameIgnore) nsameIgn++;
        else ndiff++;
        total++;
        String state = same ? "  " : (sameIgnore ? "* " : "**");
        if (!same && !sameIgnore)
          f.format("%s%d %d %d%n %s%n %s%n", state, gt.discipline, gt.category, p.start, p.name, paramOrg);
      }
    }
    f.format("Total=%d same=%d sameIgn=%d dif=%d unknown=%d%n", total, nsame, nsameIgn, ndiff, unknown);
  }

  static String munge(String org) {
    String result = StringUtil2.remove(org, "_");
    result = StringUtil2.remove(result, "-");
    return result;
  }

  public static void showTable(List<WmoCodeTable> tlist,  Formatter f) {
    for (WmoCodeTable gt : tlist) {
      f.format("%d.%d (%d,%d) %s %n", gt.m1, gt.m2, gt.discipline, gt.category, gt.tableName);
      for (TableEntry p : gt.entries) {
        f.format("  %s (%d-%d) = %s %n", p.code, p.start, p.stop, p.meaning);
      }
    }
  } */

}
