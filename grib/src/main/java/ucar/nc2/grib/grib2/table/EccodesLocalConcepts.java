package ucar.nc2.grib.grib2.table;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.Grib2Parameter;
import ucar.unidata.util.StringUtil2;

/**
 * Parse the localConcept files needed to create tables for use by the CDM.
 */
class EccodesLocalConcepts {
  private static final Logger logger = LoggerFactory.getLogger(EccodesLocalTables.class);
  private static final String DISCIPLINE = "discipline";
  private static final String CATEGORY = "parameterCategory";
  private static final String NUMBER = "parameterNumber";
  private static final Charset ENCODING = StandardCharsets.UTF_8;

  private final String tableId;

  private ImmutableListMultimap.Builder<String, LocalConceptPart> localConceptsBuilder = ImmutableListMultimap.builder();
  private HashMap<String, LocalConcept> localConcepts = new HashMap<>();

  EccodesLocalConcepts(String directoryPath) throws IOException {
    String[] parts = directoryPath.split("/");
    this.tableId = parts[parts.length-1];

    parseLocalConcept(directoryPath + "/name.def", Type.name);
    parseLocalConcept(directoryPath + "/shortName.def", Type.shortName);
    parseLocalConcept(directoryPath + "/paramId.def", Type.paramId);
    parseLocalConcept(directoryPath + "/units.def", Type.units);
    parseLocalConcept(directoryPath + "/cfName.def", Type.cfName);
    parseLocalConcept(directoryPath + "/cfVarName.def", Type.cfVarName);

    ImmutableListMultimap<String, LocalConceptPart> localConceptPieces = localConceptsBuilder.build();

    for (LocalConceptPart conceptPart : localConceptPieces.values()) {
      localConcepts.merge(conceptPart.getKey(), new LocalConcept(conceptPart), LocalConcept::merge);
    }
  }

  ImmutableListMultimap<Integer, Grib2Parameter> getLocalConceptMultimap() {
    ImmutableListMultimap.Builder<Integer, Grib2Parameter> result = ImmutableListMultimap.builder();
    for (LocalConcept lc : localConcepts.values()) {
      if (lc.getName() != null && lc.getShortName() != null) {
        int code = Grib2Tables.makeParamId(lc.discipline, lc.category, lc.number);
        Grib2Parameter param = new Grib2Parameter(lc.discipline, lc.category, lc.number,
            lc.getName(), lc.getUnits(), lc.getShortName(), null);
        result.put(code, param);
      }
    }
    return result.build();
  }


  /*
#Mean of 10 metre wind speed
'Mean of 10 metre wind speed' = {
	 discipline = 192 ;
	 parameterCategory = 228 ;
	 parameterNumber = 5 ;
	}
#Mean total cloud cover
'Mean total cloud cover' = {
	 discipline = 192 ;
	 parameterCategory = 228 ;
	 parameterNumber = 6 ;
	}
#Lake depth
'Lake depth' = {
	 discipline = 192 ;
	 parameterCategory = 228 ;
	 parameterNumber = 7 ;
	}
	   */

  private void parseLocalConcept(String path, Type conceptType) throws IOException {
    ClassLoader cl = EccodesLocalConcepts.class.getClassLoader();
    try (InputStream is = cl.getResourceAsStream(path)) {
      if (is == null) return; // file not found is ok
      try (BufferedReader br = new BufferedReader(new InputStreamReader(is, ENCODING))) {
        boolean header = true;

        LocalConceptPart current = null;
        while (true) {
          String line = br.readLine();
          if (line == null) {
            break; // done with the file
          }
          if ((line.length() == 0)) {
            continue;
          }
          // skip header lines
          if (header && (line.startsWith("# Auto") || line.startsWith("#Provide"))) {
            continue;
          }
          header = false;

          // edzw has this extra line
          if (line.startsWith("#paramId")) {
            continue;
          }

          if (line.startsWith("#")) {
            if (current != null) {
              localConceptsBuilder.put(current.getKey(), current);
            }
            String paramName = line.substring(1);
            line = br.readLine();
            current = new LocalConceptPart(paramName, clean(line), conceptType);
            continue;
          }

          if (line.contains("=")) {
            Iterator<String> tokens = Splitter.on('=')
                .trimResults()
                .omitEmptyStrings()
                .split(line)
                .iterator();
            String name = tokens.next();
            String valueS = clean(tokens.next());
            Integer value;
            try {
              value = Integer.parseInt(valueS);
              current.add(name, value);
            } catch (Exception e) {
              logger.warn("Table {}/{} line {}", tableId, conceptType, line);
            }
          }
        }
        if (current != null) {
          localConceptsBuilder.put(current.getKey(), current);
        }
      }
    }
  }

  private String clean(String in) {
    StringBuilder sb = new StringBuilder(in);
    StringUtil2.removeAll(sb, ";={}'");
    return sb.toString().trim();
  }

  private class AttributeBag {
    final Map<String, Integer> atts;
    AttributeBag(Map<String, Integer> atts) {
      this.atts = atts;
    }

    public void show(Formatter f) {
      if (atts.isEmpty()) return;
      f.format("     ");
      for (Map.Entry<String, Integer> entry : atts.entrySet()) {
        f.format("%s(%3d) ",  entry.getKey(), entry.getValue());
      }
      f.format("%n");
    }
  }

  private class LocalConcept implements Comparable<LocalConcept> {
    private final List<AttributeBag> attBags = new ArrayList<>();
    private final String key;
    private final int discipline ;
    private final int category;
    private final int number;
    private final Type type;
    private final String value;

    private String paramName;
    private String shortName;
    private String units;
    private String cfName;
    private String cfVarName;
    private String paramId; // LOOK: WTF?

    LocalConcept(LocalConceptPart part) {
      this.key = part.getKey();
      this.discipline = part.discipline;
      this.category = part.category;
      this.number = part.number;

      this.attBags.add(new AttributeBag(part.att));
      this.type = part.type;
      this.value = part.value;
      extract(this);
    }

    LocalConcept merge(LocalConcept other) {
      extract(other);

      // assume that only the attributes in the name.def files are important.
      if (other.type == Type.name) {
        this.attBags.addAll(other.attBags);
      }
      return this;
    }

    private void extract(LocalConcept other) {
      switch (other.type) {
        case name:
          this.paramName = other.value;
          break;
        case shortName:
          this.shortName = other.value;
          break;
        case paramId:
          this.paramId = other.value;
          break;
        case units:
          this.units = other.value;
          break;
        case cfName:
          this.cfName = other.value;
          break;
        case cfVarName:
          this.cfVarName = other.value;
          break;
      }
    }

    String getCode() {
      return Grib2Tables.makeParamCode(discipline, category, number);
    }

    String getName() {
      return paramName != null ? paramName : cfName;
    }

    String getShortName() {
      return shortName != null ? shortName : cfVarName;
    }

    String getUnits() {
      return units != null ? units : "";
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("atBag size=", attBags.size())
          .add("paramName", paramName)
          .add("discipline", discipline)
          .add("category", category)
          .add("number", number)
          .add("type", type)
          .add("value", value)
          .add("shortName", shortName)
          .add("units", units)
          .add("paramId", paramId)
          .toString();
    }

    @Override
    public int compareTo(LocalConcept o) {
      int c = discipline - o.discipline;
      if (c != 0) return c;
      c = category - o.category;
      if (c != 0) return c;
      return number - o.number;
    }
  }

  private enum Type {name, shortName, paramId, units, cfName, cfVarName}
  private class LocalConceptPart {
    private final Map<String, Integer> att = new TreeMap<>();
    private final String paramName;
    private final String value;
    private final Type type;

    private int discipline = -1;
    private int category = -1;
    private int number = -1;

    LocalConceptPart(String paramName, String value, Type type) {
      this.paramName = paramName;
      this.value = value;
      this.type = type;
    }

    void add(String name, int value) {
      switch (name) {
        case DISCIPLINE:
          discipline = value;
          break;
        case CATEGORY:
          category = value;
          break;
        case NUMBER:
          number = value;
          break;
        default:
          att.put(name, value);
          break;
      }
    }

    String getKey() {
      return paramName + ":" + Grib2Tables.makeParamCode(discipline, category, number);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("paramName", paramName)
          .add("type", type)
          .add("discipline", discipline)
          .add("category", category)
          .add("number", number)
          .add("att", att)
          .toString();
    }
  }

  // Obsolete
  private void checkLocalConceptsParts() {
    ImmutableListMultimap<String, LocalConceptPart> localConceptPieces = localConceptsBuilder.build();
    ImmutableList<String> keys = localConceptPieces.asMap().keySet().stream().sorted().collect(ImmutableList.toImmutableList());
    System.out.printf("%-70s: %-10s: %-10s - %-10s - %s%n", "name", "code", "shortName", "paramId", "units");
    System.out.printf("-------------------------------------------------------------------------------------------------------------%n");
    for (String key : keys) {
      LocalConceptPart name = null;
      LocalConceptPart shortName = null;
      LocalConceptPart paramId = null;
      LocalConceptPart units = null;
      for (LocalConceptPart concept : localConceptPieces.get(key)) {
        if (concept.type == Type.name)
          name = concept;
        if (concept.type == Type.shortName)
          shortName = concept;
        if (concept.type == Type.paramId)
          paramId = concept;
        if (concept.type == Type.units)
          units = concept;
      }
      if (name == null || shortName == null || paramId == null || units == null) {
        System.out.printf("***Missing %s == %s, %s, %s, %s%n", key, name == null, shortName == null,
            paramId == null, units == null);
      } else {
        System.out.printf("%-70s: %-10s: %-10s - %s - %s%n", key, name.getKey(), shortName.value, paramId.value, units.value);
        for (LocalConceptPart concept : localConceptPieces.get(key)) {
          if (!concept.att.isEmpty()) {
            for (Map.Entry<String, Integer> entry : concept.att.entrySet()) {
              System.out.printf("    %s: %s = %s%n", concept.type, entry.getKey(), entry.getValue());
            }
          }
        }
      }
    }
  }

  private static final String FORMAT =  "%-10s: %-70s: %-10s - %-8s - %-10s - %-20s - %s%n";

  void showDetails(Formatter f) {
    ImmutableList<LocalConcept> sorted = localConcepts.values().stream().sorted().collect(ImmutableList.toImmutableList());
    Set<String> attNames = new TreeSet<>();

    f.format(FORMAT, "code", "name", "shortName", "paramId", "units", "cfName", "cfVarName");
    f.format("%s%n", StringUtil2.padRight("-", 120, "-"));
    for (LocalConcept lc : sorted) {;
      f.format(FORMAT, lc.getCode(), lc.paramName, lc.shortName, lc.paramId, lc.units, lc.cfName, lc.cfVarName);
      for (AttributeBag bag : lc.attBags) {
        bag.show(f);
        attNames.addAll(bag.atts.keySet());
      }
      f.format("%n");
    }
    f.format("%s%n", StringUtil2.padRight("-", 120, "-"));
    f.format("All attribute names in this table:%n");
    for (String attName : attNames) {
      f.format(" %s%n", attName);
    }
  }

  public void showEntryDetails(Formatter f, List<GribTables.Parameter> params) {
    List<LocalConcept> concepts = new ArrayList<>();
    Set<String> attNames = new TreeSet<>();

    for (GribTables.Parameter param : params) {
      String key = param.getName() + ":" + Grib2Tables.makeParamCode(param.getDiscipline(), param.getCategory(), param.getNumber());
      LocalConcept want = localConcepts.get(key);
      concepts.add(want);
      showLocalConcept(f, want, attNames);
    }
    f.format("%n");

    int count = 0;
    f.format("%-30s   ", "");
    for (LocalConcept concept : concepts) {
      for (AttributeBag bag : concept.attBags) {
        f.format("  (%2d)   ", count);
      }
      count++;
    }
    f.format("%n");

    for (String attName : attNames) {
      f.format("%-30s ", attName);
      for (LocalConcept concept : concepts) {
        for (AttributeBag bag : concept.attBags) {
          Integer value = bag.atts.get(attName);
          f.format("%8s ", value == null ? "" : value);
        }
      }
      f.format("%n");
    }
  }

  private void showLocalConcept(Formatter f, LocalConcept lc, Set<String> attNames) {
    f.format(FORMAT, lc.getCode(), lc.paramName, lc.shortName, lc.paramId, lc.units, lc.cfName, lc.cfVarName);
    for (AttributeBag bag : lc.attBags) {
      bag.show(f);
      attNames.addAll(bag.atts.keySet());
    }
    f.format("%n");
  }

  public static void main(String[] args) throws IOException {
    EccodesLocalConcepts ec = new EccodesLocalConcepts("resources/grib2/ecmwf/localConcepts/ecmf");
    //ec.checkLocalConceptsParts();
    ec.showDetails(new Formatter(System.out));
  }

}
