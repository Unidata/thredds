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

package thredds.inventory;

import org.jdom.Element;
import ucar.unidata.util.StringUtil2;

import java.util.*;

/**
 * Beans for FeatureCollection configuration
 *
 * @author caron
 * @since Mar 30, 2010
 */
public class FeatureCollectionConfig {
  static public final String AUX_GDSHASH = "gdshash";
  static public final String AUX_INTERVAL_MERGE = "intvMerge";

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
    Collection, Files
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

  public String name, spec, dateFormatMark, olderThan, timePartition;
  public UpdateConfig tdmConfig = new UpdateConfig();
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
  public FeatureCollectionConfig(String name, String spec, String dateFormatMark, String olderThan, String recheckAfter,
                                 String timePartition, String useIndexOnlyS, Element innerNcml) {
    this.name = name;
    this.spec = spec;
    this.dateFormatMark = dateFormatMark;
    this.olderThan = olderThan;
    if (recheckAfter != null) this.updateConfig.recheckAfter = recheckAfter;
    this.timePartition = timePartition;
    this.useIndexOnly = useIndexOnlyS != null && useIndexOnlyS.equalsIgnoreCase("true");
    this.innerNcml = innerNcml;
  }

  @Override
  public String toString() {
    return "FeatureCollectionConfig{" +
            "name='" + name + '\'' +
            ", spec='" + spec + '\'' +
            ", dateFormatMark='" + dateFormatMark + '\'' +
            ", olderThan='" + olderThan + '\'' +
            ", timePartition=" + timePartition +
            ", updateConfig=" + updateConfig +
            ", tdmConfig=" + tdmConfig +
            ", protoConfig=" + protoConfig +
            ", fmrcConfig=" + fmrcConfig +
            ", pointConfig=" + pointConfig +
            ", hasInnerNcml=" + (innerNcml != null) +
            '}';
  }

  // <update startup="true" rescan="cron expr" trigger="allow" append="true"/>
  static public class UpdateConfig {
    public String recheckAfter;
    public String rescan;
    public boolean triggerOk;
    public boolean startup;
    public String deleteAfter = null;

    public UpdateConfig() { // defaults
    }

    public UpdateConfig(String startupS, String recheckAfter, String rescan, String triggerS, String deleteAfter) {
      this.rescan = rescan; // may be null
      if (recheckAfter != null) this.recheckAfter = recheckAfter; // in case it was set in collection element
      this.deleteAfter = deleteAfter; // may be null
      if ((startupS != null) && startupS.equalsIgnoreCase("true"))
        this.startup = true;
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
          Collections.unmodifiableSet(EnumSet.of(GribDatasetType.Collection, GribDatasetType.Files));

  static public class GribConfig  {
    public Set<GribDatasetType> datasets = defaultGribDatasetTypes;
    public Map<Integer, Integer> gdsHash;
    protected boolean explicit = false;
    public boolean intervalMerge = false;

    public GribConfig() { // defaults
    }

    public void addDatasetType(String datasetTypes) {
      // if they list datasetType explicitly, remove defaults
      if (!explicit) datasets = EnumSet.noneOf(GribDatasetType.class);
      explicit = true;

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

    public void addGdsHash(String fromS, String toS) {
      if (gdsHash == null) gdsHash = new HashMap<Integer, Integer>(5);

      try {
        int from = Integer.parseInt(fromS);
        int to = Integer.parseInt(toS);
        gdsHash.put(from,to);
      } catch (Exception e) {
        log.warn("Failed  to parse as Integer = {} {}", fromS, toS);
      }
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      f.format("GribConfig: datasetTypes=%s", datasets);
      return f.toString();
    }
  }

}
