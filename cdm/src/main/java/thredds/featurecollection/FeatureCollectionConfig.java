/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package thredds.featurecollection;

import org.jdom2.Element;
import org.jdom2.Namespace;
import thredds.inventory.CollectionManager;
import ucar.unidata.util.StringUtil2;

import java.util.*;

/**
 * Beans for FeatureCollection configuration
 *
 * @author caron
 * @since Mar 30, 2010
 */
public class FeatureCollectionConfig {
  // keys for storing AuxInfo objects
  static public final String AUX_GRIB_CONFIG = "gribConfig";

  static public enum ProtoChoice {
    First, Random, Latest, Penultimate, Run
  }

  static public enum FmrcDatasetType {
    TwoD, Best, Files, Runs, ConstantForecasts, ConstantOffsets
  }

  static public enum PointDatasetType {
    cdmrFeature, Files
  }

  static public enum GribDatasetType {
    Best, Files, LatestFile
  }

  public static void setRegularizeDefault(boolean t) {
    regularizeDefault = t;
  }

  public static boolean getRegularizeDefault() {
    return regularizeDefault;
  }

  static private boolean regularizeDefault = false;
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeatureCollectionConfig.class);

  //////////////////////////////////////////////

  public FeatureCollectionType type;
  public String name, spec, dateFormatMark, olderThan, timePartition;
  public UpdateConfig tdmConfig;
  public UpdateConfig updateConfig = new UpdateConfig();
  public ProtoConfig protoConfig = new ProtoConfig();
  public FmrcConfig fmrcConfig = new FmrcConfig();
  public PointConfig pointConfig = new PointConfig();
  public GribConfig gribConfig = new GribConfig();
  public Element innerNcml = null;
  public boolean useIndexOnly = false;

  public FeatureCollectionConfig() {
  }

  // <collection spec="/data/ldm/pub/native/satellite/3.9/WEST-CONUS_4km/WEST-CONUS_4km_3.9_#yyyyMMdd_HHmm#.gini$"
  //          name="WEST-CONUS_4km" olderThan="1 min" recheckAfter="15 min" />
  public FeatureCollectionConfig(String name, FeatureCollectionType fcType, String spec, String dateFormatMark, String olderThan, String recheckAfter,
                                 String timePartition, String useIndexOnlyS, Element innerNcml) {
    this.name = name;
    this.type = fcType;
    this.spec = spec;
    this.dateFormatMark = dateFormatMark;
    this.olderThan = olderThan;
    if (recheckAfter != null) this.updateConfig.recheckAfter = recheckAfter;
    this.timePartition = timePartition;
    this.useIndexOnly = useIndexOnlyS != null && useIndexOnlyS.equalsIgnoreCase("true");
    this.innerNcml = innerNcml;
  }

  public boolean isTrigggerOk() {
    if (updateConfig.triggerOk) return true;
    return (tdmConfig != null) && tdmConfig.triggerOk;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("name ='%s' type='%s'%n", name, type);
    f.format("  spec='%s'%n", spec);
    if (dateFormatMark != null)
      f.format("  dateFormatMark ='%s'%n", dateFormatMark);
    if (olderThan != null)
      f.format("  olderThan =%s%n", olderThan);
    if (timePartition != null)
      f.format("  timePartition =%s%n", timePartition);
    else
      f.format("  NOT timePartition%n");
    if (updateConfig != null)
      f.format("  updateConfig =%s%n", updateConfig);
    if (tdmConfig != null)
      f.format("  tdmConfig =%s%n", tdmConfig);
    if (protoConfig != null)
      f.format("  protoConfig =%s%n", protoConfig);
    f.format("  hasInnerNcml =%s%n", innerNcml != null);

    switch (type) {
      case GRIB:
        f.format("  gribConfig =%s%n", gribConfig);
        break;
      case FMRC:
        f.format("  fmrcConfig =%s%n", fmrcConfig);
        break;
      case Point:
      case Station:
      case Station_Profile:
        f.format("  pointConfig =%s%n", pointConfig);
        break;
    }

    return f.toString();
  }

  // <update startup="true" rescan="cron expr" trigger="allow" append="true"/>
  static public class UpdateConfig {
    public String recheckAfter;
    public String rescan;
    public boolean triggerOk;
    public boolean startup;
    public CollectionManager.Force startupForce;
    public String deleteAfter = null;

    public UpdateConfig() { // defaults
    }

    public UpdateConfig(String startupS, String recheckAfter, String rescan, String triggerS, String deleteAfter) {
      this.rescan = rescan; // may be null
      if (recheckAfter != null) this.recheckAfter = recheckAfter; // in case it was set in collection element
      if (rescan != null) this.recheckAfter = null;               // both not allowed
      this.deleteAfter = deleteAfter; // may be null
      if (startupS != null) {
        if (startupS.equalsIgnoreCase("true") || startupS.equalsIgnoreCase("test"))
          this.startupForce = CollectionManager.Force.test;
        else if (startupS.equalsIgnoreCase("nocheck"))
          this.startupForce = CollectionManager.Force.nocheck;
        startup = (this.startupForce != null);
      }
      if (triggerS != null)
        this.triggerOk = triggerS.equalsIgnoreCase("allow");
    }

    @Override
    public String toString() {
      return "UpdateConfig{" +
              "startup=" + startup +
              ", recheckAfter='" + recheckAfter + '\'' +
              ", rescan='" + rescan + '\'' +
              ", triggerOk=" + triggerOk +
              ", deleteAfter=" + deleteAfter +
              '}';
    }
  }

  // <protoDataset choice="First | Random | Penultimate | Latest | Run" param="0" change="expr" />
  static public class ProtoConfig {
    public ProtoChoice choice = ProtoChoice.Penultimate;
    public String param = null;
    public String change = null;
    public Element outerNcml = null;
    public boolean cacheAll = true;

    public ProtoConfig() { // defaults
    }

    public ProtoConfig(String choice, String change, String param, Element ncml) {
      if (choice != null) {
        try {
          this.choice = ProtoChoice.valueOf(choice);
        } catch (Exception e) {
          log.warn("Dont recognize ProtoChoice " + choice);
        }
      }

      this.change = change;
      this.param = param;
      this.outerNcml = ncml;
    }

    @Override
    public String toString() {
      return "ProtoConfig{" +
              "choice=" + choice +
              ", change='" + change + '\'' +
              ", param='" + param + '\'' +
              ", outerNcml='" + outerNcml + '\'' +
              ", cacheAll=" + cacheAll +
              '}';
    }
  }

  static private Set<FmrcDatasetType> defaultFmrcDatasetTypes =
          Collections.unmodifiableSet(EnumSet.of(FmrcDatasetType.TwoD, FmrcDatasetType.Best, FmrcDatasetType.Files, FmrcDatasetType.Runs));

  static public class FmrcConfig {
    public boolean regularize = regularizeDefault;
    public Set<FmrcDatasetType> datasets = defaultFmrcDatasetTypes;
    private boolean explicit = false;
    private List<BestDataset> bestDatasets = null;

    public FmrcConfig() { // defaults
    }

    public FmrcConfig(String regularize) {
      this.regularize = (regularize != null) && (regularize.equalsIgnoreCase("true"));
    }

    public void addDatasetType(String datasetTypes) {
      // if they list datasetType explicitly, remove defaults
      if (!explicit) datasets = EnumSet.noneOf(FmrcDatasetType.class);
      explicit = true;

      String[] types = StringUtil2.splitString(datasetTypes);
      for (String type : types) {
        try {
          FmrcDatasetType fdt = FmrcDatasetType.valueOf(type);
          datasets.add(fdt);
        } catch (Exception e) {
          log.warn("Dont recognize FmrcDatasetType " + type);
        }
      }
    }

    public void addBestDataset(String name, double greaterEqual) {
      if (bestDatasets == null) bestDatasets = new ArrayList<BestDataset>(2);
      bestDatasets.add(new BestDataset(name, greaterEqual));
    }

    public List<BestDataset> getBestDatasets() {
      return bestDatasets;
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      f.format("FmrcConfig: regularize=%s datasetTypes=%s", regularize, datasets);
      if (bestDatasets != null)
        for (BestDataset bd : bestDatasets)
          f.format("best = (%s, %f) ", bd.name, bd.greaterThan);
      return f.toString();
    }
  }

  static public class BestDataset {
    public String name;
    public double greaterThan;

    public BestDataset(String name, double greaterThan) {
      this.name = name;
      this.greaterThan = greaterThan;
    }

  }

  static private Set<PointDatasetType> defaultPointDatasetTypes =
          Collections.unmodifiableSet(EnumSet.of(PointDatasetType.cdmrFeature, PointDatasetType.Files));

  static public class PointConfig {
    public Set<PointDatasetType> datasets = defaultPointDatasetTypes;
    protected boolean explicit = false;

    public PointConfig() { // defaults
    }

    public void addDatasetType(String datasetTypes) {
      // if they list datasetType explicitly, remove defaults
      if (!explicit) datasets = EnumSet.noneOf(PointDatasetType.class);
      explicit = true;

      String[] types = StringUtil2.splitString(datasetTypes);
      for (String type : types) {
        try {
          PointDatasetType fdt = PointDatasetType.valueOf(type);
          datasets.add(fdt);
        } catch (Exception e) {
          log.warn("Dont recognize PointDatasetType " + type);
        }
      }
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      f.format("PointConfig: datasetTypes=%s", datasets);
      return f.toString();
    }
  }

  static private Set<GribDatasetType> defaultGribDatasetTypes =
          Collections.unmodifiableSet(EnumSet.of(GribDatasetType.Best, GribDatasetType.Files));

  static public class GribConfig {
    public Set<GribDatasetType> datasets = defaultGribDatasetTypes;
    public Map<Integer, Integer> gdsHash;
    public Map<Integer, String> gdsNamer;
    public String groupNamer;
    public String lookupTablePath, paramTablePath;
    public String latestNamer, bestNamer;
    public Element paramTable;
    public Map<String,Boolean> pdsHash = new HashMap<String,Boolean>();
    //public Boolean useGenType = null;
    public Boolean filesSortIncreasing = true;
    public GribIntvFilter intvFilter;

    private TimeUnitConverterHash tuc;
    private boolean explicitDatasets = false;

    public GribConfig() { // defaults
    }

    public TimeUnitConverter getTimeUnitConverter() {
      return tuc;
    }

    public void configFromXml(Element configElem, Namespace ns) {
      String datasetTypes = configElem.getAttributeValue("datasetTypes");
      if (null != datasetTypes)
        addDatasetType(datasetTypes);

      List<Element> gdsElems = configElem.getChildren("gdsHash", ns);
      for (Element gds : gdsElems)
        addGdsHash(gds.getAttributeValue("from"), gds.getAttributeValue("to"));

      List<Element> tuElems = configElem.getChildren("timeUnitConvert", ns);
      for (Element tu : tuElems)
        addTimeUnitConvert(tu.getAttributeValue("from"), tu.getAttributeValue("to"));

      gdsElems = configElem.getChildren("gdsName", ns);
      for (Element gds : gdsElems)
        addGdsName(gds.getAttributeValue("hash"), gds.getAttributeValue("groupName"));

      if (configElem.getChild("parameterMap", ns) != null)
        paramTable = configElem.getChild("parameterMap", ns);
      if (configElem.getChild("gribParameterTable", ns) != null)
        paramTablePath = configElem.getChildText("gribParameterTable", ns);
      if (configElem.getChild("gribParameterTableLookup", ns) != null)
        lookupTablePath = configElem.getChildText("gribParameterTableLookup", ns);
      if (configElem.getChild("groupNamer", ns) != null)
        groupNamer = configElem.getChildText("groupNamer", ns);
      if (configElem.getChild("latestNamer", ns) != null)
        latestNamer = configElem.getChild("latestNamer", ns).getAttributeValue("name");
      if (configElem.getChild("bestNamer", ns) != null)
        bestNamer = configElem.getChild("bestNamer", ns).getAttributeValue("name");

      List<Element> filesSortElems = configElem.getChildren("filesSort", ns);
      if (filesSortElems != null) {
        for (Element filesSort : filesSortElems) {
          if (filesSort.getChild("lexigraphicByName", ns) != null) {
            filesSortIncreasing = Boolean.valueOf(
                    filesSort.getChild("lexigraphicByName", ns).getAttributeValue("increasing"));
          }
        }
      }

      List<Element> intvElems = configElem.getChildren("intvFilter", ns);
      for (Element intvElem : intvElems) {
        if (intvFilter == null) intvFilter = new GribIntvFilter();
        String excludeZero = intvElem.getAttributeValue("excludeZero");
        if (excludeZero != null) intvFilter.isZeroExcluded = true;
        String intvLengthS = intvElem.getAttributeValue("intvLength");
        if (intvLengthS == null) continue;
        int intvLength = Integer.parseInt(intvLengthS);
        List<Element> varElems = intvElem.getChildren("variable", ns);
        for (Element varElem : varElems) {
          intvFilter.addVariable(intvLength, varElem.getAttributeValue("id"), varElem.getAttributeValue("prob"));
        }
      }

      Element pdsHashElement = configElem.getChild("pdsHash", ns);
      readValue(pdsHashElement, "intvMerge", ns, true);
      readValue(pdsHashElement, "useGenType", ns, false);
      readValue(pdsHashElement, "useTableVersion", ns, true);
    }

    private void readValue(Element pdsHashElement, String key, Namespace ns, boolean value) {
      if (pdsHashElement != null) {
        Element e = pdsHashElement.getChild(key, ns);
        if (e != null) {
          value = true; // no value means true
          String t = e.getTextNormalize();
          if (t != null && t.equalsIgnoreCase("true")) value = true;
          if (t != null && t.equalsIgnoreCase("false")) value = false;
        }
      }
      pdsHash.put(key, value);
    }

    public void addDatasetType(String datasetTypes) {
      // if they list datasetType explicitly, remove defaults
      if (!explicitDatasets) datasets = EnumSet.noneOf(GribDatasetType.class);
      explicitDatasets = true;

      String[] types = StringUtil2.splitString(datasetTypes);
      for (String type : types) {
        try {
          GribDatasetType fdt = GribDatasetType.valueOf(type);
          datasets.add(fdt);
        } catch (Exception e) {
          log.warn("Dont recognize GribDatasetType {}", type);
        }
      }
    }

    public boolean hasDatasetType(GribDatasetType type) {
      return datasets.contains(type);
    }

    public void addGdsHash(String fromS, String toS) {
      if (fromS == null || toS == null) return;
      if (gdsHash == null) gdsHash = new HashMap<Integer, Integer>(10);

      try {
        int from = Integer.parseInt(fromS);
        int to = Integer.parseInt(toS);
        gdsHash.put(from, to);
      } catch (Exception e) {
        log.warn("Failed  to parse as Integer = {} {}", fromS, toS);
      }
    }

    public void addTimeUnitConvert(String fromS, String toS) {
      if (fromS == null || toS == null) return;
      if (tuc == null) tuc = new TimeUnitConverterHash();

      try {
        int from = Integer.parseInt(fromS);
        int to = Integer.parseInt(toS);
        tuc.map.put(from, to);
      } catch (Exception e) {
        log.warn("Failed  to parse as Integer = {} {}", fromS, toS);
      }
    }

    public void addGdsName(String hashS, String name) {
      if (hashS == null || name == null) return;
      if (gdsNamer == null) gdsNamer = new HashMap<Integer, String>(5);

      try {
        int hash = Integer.parseInt(hashS);
        gdsNamer.put(hash, name);
      } catch (Exception e) {
        log.warn("Failed  to parse as Integer = {} {}", hashS, name);
      }
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      f.format("GribConfig: datasetTypes=%s", datasets);
      return f.toString();
    }

    public Object getIospMessage() {
      if (lookupTablePath != null) return "gribParameterTableLookup="+lookupTablePath;
      if (paramTablePath != null) return "gribParameterTable="+paramTablePath;
      return null;
    }
  }

  static class GribIntvFilterParam {
    int id;
    int intvLength;
    int prob = Integer.MIN_VALUE;

    GribIntvFilterParam(int id, int intvLength, int prob) {
      this.id = id;
      this.intvLength = intvLength;
      this.prob = prob;
    }
  }

  static public class GribIntvFilter {
    List<GribIntvFilterParam> filter;
    boolean isZeroExcluded;

    public boolean isZeroExcluded() {
      return isZeroExcluded;
    }

    public boolean hasFilter() {
      return (filter != null);
    }
    /*
          <intvFilter intvLength="12">
            <variable id="0-1-8" prob="50800"/>
          </intvFilter>
          <intvFilter intvLength="3">
            <variable id="0-1-8"/>
          </intvFilter>

     */

    // true means remove
    public boolean filterOut(int id, int hasLength, int prob) {
      if (filter == null) return false;
      for (GribIntvFilterParam param : filter) {
        boolean needProb = (param.prob != Integer.MIN_VALUE); // filter uses prob
        boolean hasProb = (prob != Integer.MIN_VALUE); // record has prob
        boolean isMine = !needProb || hasProb && (param.prob == prob);
        if (param.id == id && isMine) { // first match in the filter list is used
          if (param.intvLength != hasLength)
            return true; // remove the ones whose intervals dont match
          return false;
        }
      }
      return false;
    }

    void addVariable(int intvLength, String idS, String probS) {
      if (idS == null) {
        log.warn("Error on intvFilter: must have an id attribute");
        return;
      }

      String[] s = idS.split("-");
      if (s.length != 3 && s.length != 4) {
        log.warn("Error on intvFilter: id attribute must be of format 'discipline-category-number' (GRIB2) or 'center-subcenter-version-param' (GRIB1)");
        return;
      }

      try {
        int id;
        if (s.length == 3) {
          int discipline = Integer.parseInt(s[0]);
          int category = Integer.parseInt(s[1]);
          int number = Integer.parseInt(s[2]);
          id = (discipline << 16) + (category << 8) + number;
        } else {
          int center = Integer.parseInt(s[0]);
          int subcenter = Integer.parseInt(s[1]);
          int version = Integer.parseInt(s[2]);
          int param = Integer.parseInt(s[3]);
          id = (center << 8) + (subcenter << 16) + (version << 24) + param;
        }

        int prob = (probS == null) ? Integer.MIN_VALUE : Integer.parseInt(probS);

        if (filter == null) filter = new ArrayList<GribIntvFilterParam>(10);
        filter.add(new GribIntvFilterParam(id, intvLength, prob));

      } catch (NumberFormatException e) {
        log.info("Error on intvFilter element - attribute must be an integer");
      }
    }

  }

  private static class TimeUnitConverterHash implements TimeUnitConverter {
    Map<Integer, Integer> map = new HashMap<Integer, Integer>(5);

    public int convertTimeUnit(int timeUnit) {
      if (map == null) return timeUnit;
      Integer convert = map.get(timeUnit);
      return (convert == null) ? timeUnit : convert;
    }
  }

}
