package ucar.nc2.grib.grib2.table;

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
import java.util.Collection;
import java.util.Formatter;
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
  private ImmutableListMultimap<String, LocalConcept> localConcepts;

  EccodesLocalConcepts(String directoryPath) throws IOException {
    String[] dirs = directoryPath.split("/");
    this.tableId = dirs[dirs.length-1];

    ImmutableListMultimap.Builder<LocalConceptPart, LocalConceptPart> partsBuilder = ImmutableListMultimap.builder();
    parseLocalConcept(partsBuilder, directoryPath + "/name.def", Type.name);
    parseLocalConcept(partsBuilder, directoryPath + "/shortName.def", Type.shortName);
    parseLocalConcept(partsBuilder, directoryPath + "/paramId.def", Type.paramId);
    parseLocalConcept(partsBuilder, directoryPath + "/units.def", Type.units);
    parseLocalConcept(partsBuilder, directoryPath + "/cfName.def", Type.cfName);
    parseLocalConcept(partsBuilder, directoryPath + "/cfVarName.def", Type.cfVarName);

    ImmutableListMultimap.Builder<String, LocalConcept> conceptsBuilder = ImmutableListMultimap.builder();
    for (Collection<LocalConceptPart> parts : partsBuilder.build().asMap().values()) {
      LocalConcept localConcept = null;
      for (LocalConceptPart part : parts) {
        if (localConcept == null) {
          localConcept = new LocalConcept(part);
        } else {
          localConcept.merge(part);
        }
      }
      conceptsBuilder.put(localConcept.getKey(), localConcept);
    }
    localConcepts = conceptsBuilder.build();
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
    #paramName
    'value' = {
      attName = attValue ;
      attName = attValue ;
      attName = attValue ;
    }
    examples:

    #Potential vorticity
    '82001004' = {
              discipline = 0 ;
              parameterCategory = 2 ;
              parameterNumber = 14 ;
              }
    #Lake depth
    'Lake depth' = {
       discipline = 192 ;
       parameterCategory = 228 ;
       parameterNumber = 7 ;
      }
    #Convective available potential energy
     'cape' = {
     discipline = 0 ;
     parameterCategory = 7 ;
     parameterNumber = 6 ;
     typeOfFirstFixedSurface = 1 ;
     typeOfSecondFixedSurface = 8 ;
    }
    #Minimum temperature at 2 metres since previous post-processing
    'K' = {
       discipline = 0 ;
       parameterCategory = 0 ;
       parameterNumber = 0 ;
       scaledValueOfFirstFixedSurface = 15 ;
       scaleFactorOfFirstFixedSurface = 1 ;
       typeOfStatisticalProcessing = 3 ;
       typeOfFirstFixedSurface = 103 ;
       is_uerra = 1 ;
      }
	*/

  private void parseLocalConcept(ImmutableListMultimap.Builder<LocalConceptPart, LocalConceptPart> localConceptParts, String path, Type conceptType) throws IOException {
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
              localConceptParts.put(current, current);
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
              current.addAttribute(name, value);
            } catch (Exception e) {
              logger.warn("Table {}/{} line {}", tableId, conceptType, line);
            }
          }
        }
        if (current != null) {
          localConceptParts.put(current, current);
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
    private final Map<String, Integer> atts = new TreeMap<>();
    int hashCode = 0;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      AttributeBag that = (AttributeBag) o;
      return atts.equals(that.atts);
    }

    @Override
    public int hashCode() {
      // must roll our own hashcode
      if (hashCode == 0) {
        int result = 17;
        for (Map.Entry<String, Integer> entry : atts.entrySet()) {
          result = 37 * result + entry.getKey().hashCode();
          result = 37 * result + entry.getValue().hashCode();
        }
        hashCode = result;
      }
      return hashCode;
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
    private final String paramName;
    private final LocalConceptPart org;

    private int discipline ;
    private int category;
    private int number;
    private String paramId;
    private String name;
    private String shortName;
    private String units;
    private String cfName;
    private String cfVarName;

    private final AttributeBag bag = new AttributeBag();

    LocalConcept(LocalConceptPart part) {
      this.paramName = part.paramName;
      this.org = part;
      extractValue(part);
      extractAtts(part);
    }

    String getKey() {
      return paramName + ":" + Grib2Tables.makeParamCode(discipline, category, number);
    }

    void merge(LocalConceptPart part) {
      assert(this.paramName.equals(part.paramName));
      assert(this.org.atts.equals(part.atts));
      extractValue(part);
    }

    void extractAtts(LocalConceptPart part) {
      for (Map.Entry<String, Integer> entry : part.atts.atts.entrySet()) {
        String name = entry.getKey();
        Integer value = entry.getValue();
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
            bag.atts.put(name, value);
            break;
        }
      }
    }

    private void extractValue(LocalConceptPart part) {
      switch (part.type) {
        case name:
          this.name = part.value;
          break;
        case shortName:
          this.shortName = part.value;
          break;
        case paramId:
          this.paramId = part.value;
          break;
        case units:
          this.units = part.value;
          break;
        case cfName:
          this.cfName = part.value;
          break;
        case cfVarName:
          this.cfVarName = part.value;
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
    private final AttributeBag atts = new AttributeBag();
    private final String paramName;
    private final String value;
    private final Type type;

    LocalConceptPart(String paramName, String value, Type type) {
      this.paramName = paramName;
      this.value = value;
      this.type = type;
    }

    void addAttribute(String name, int value) {
      atts.atts.put(name, value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      LocalConceptPart that = (LocalConceptPart) o;

      if (!atts.equals(that.atts)) {
        return false;
      }
      return paramName.equals(that.paramName);
    }

    @Override
    public int hashCode() {
      int result = atts.hashCode();
      result = 31 * result + paramName.hashCode();
      return result;
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
      lc.bag.show(f);
      attNames.addAll(lc.bag.atts.keySet());
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
      Collection<LocalConcept> match = localConcepts.get(key);
      for (LocalConcept concept : match) {
        if (concept.getShortName().equals(param.getAbbrev())) {
          concepts.add(concept);
          showLocalConcept(f, concept, attNames);
        }
      }
    }
    f.format("%n");

    int count = 0;
    f.format("%-30s   ", "");
    for (LocalConcept concept : concepts) {
      f.format("  (%2d)   ", count);
      count++;
    }
    f.format("%n");

    for (String attName : attNames) {
      f.format("%-30s ", attName);
      for (LocalConcept concept : concepts) {
        Integer value = concept.bag.atts.get(attName);
        f.format("%8s ", value == null ? "" : value);
      }
      f.format("%n");
    }
  }

  private void showLocalConcept(Formatter f, LocalConcept lc, Set<String> attNames) {
    f.format(FORMAT, lc.getCode(), lc.paramName, lc.shortName, lc.paramId, lc.units, lc.cfName, lc.cfVarName);
    lc.bag.show(f);
    attNames.addAll(lc.bag.atts.keySet());
    f.format("%n");
  }

  public static void main(String[] args) throws IOException {
    EccodesLocalConcepts ec = new EccodesLocalConcepts("resources/grib2/ecmwf/localConcepts/ecmf");
    ec.showDetails(new Formatter(System.out));
  }

}
