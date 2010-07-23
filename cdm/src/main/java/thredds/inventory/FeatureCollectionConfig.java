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
import ucar.unidata.util.StringUtil;

import java.util.*;

/**
 * Beans for FeatureCollection configuration
 *
 * @author caron
 * @since Mar 30, 2010
 */
public class FeatureCollectionConfig {

  static public enum ProtoChoice {
    First, Random, Latest, Penultimate
  }

  static public enum FmrcDatasetType {
    TwoD, Best, Files, Runs, ConstantForecasts, ConstantOffsets
  }

  public static void setRegularizeDefault(boolean t) {
    regularizeDefault = t;
  }

  public static boolean getRegularizeDefault() {
    return regularizeDefault;
  }

  static private boolean regularizeDefault = false;
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeatureCollectionConfig.class);

  static public class Config {
    public String name, spec, olderThan, recheckAfter;
    public UpdateConfig updateConfig = new UpdateConfig();
    public ProtoConfig protoConfig = new ProtoConfig();
    public FmrcConfig fmrcConfig = new FmrcConfig();

    public Config() {
    }

    public Config(String name, String spec, String olderThan, String recheckAfter) {
      this.name = name;
      this.spec = spec.trim();
      this.olderThan = olderThan;
      this.recheckAfter = recheckAfter;
    }

    @Override
    public String toString() {
      return "Config{" +
              "name='" + name + '\'' +
              "spec='" + spec + '\'' +
              ", olderThan='" + olderThan + '\'' +
              ", recheckAfter='" + recheckAfter + '\'' +
              "\n " + updateConfig +
              "\n " + protoConfig +
              "\n " + fmrcConfig +
              '}';
    }
  }

  static public class UpdateConfig {
    public boolean startup;
    public String rescan = null;
    public boolean triggerOk;

    public UpdateConfig() { // defaults
    }

    public UpdateConfig(String startup, String rescan, String trigger) {
      if (startup != null)
        this.startup = startup.equalsIgnoreCase("true");
      if (trigger != null)
        this.triggerOk = trigger.equalsIgnoreCase("allow");
      this.rescan = rescan;
    }

    @Override
    public String toString() {
      return "UpdateConfig{" +
              "startup=" + startup +
              ", rescan='" + rescan + '\'' +
              ", triggerOk=" + triggerOk +
              '}';
    }
  }

  static public class ProtoConfig {
    public ProtoChoice choice = ProtoChoice.Penultimate;
    public String change = null;
    public Element ncml = null;
    public boolean cacheAll = true;

    public ProtoConfig() { // defaults
    }

    public ProtoConfig(String choice, String change, Element ncml) {
      if (choice != null) {
        try {
          this.choice = ProtoChoice.valueOf(choice);
        } catch (Exception e) {
          log.warn("Dont recognize ProtoChoice " + choice);
        }
      }

      this.change = change;
      this.ncml = ncml;
    }

    @Override
    public String toString() {
      return "ProtoConfig{" +
              "choice=" + choice +
              ", change='" + change + '\'' +
              ", ncml='" + ncml + '\'' +
              ", cacheAll=" + cacheAll +
              '}';
    }
  }

  static private Set<FmrcDatasetType> defaultDatasetTypes =
          Collections.unmodifiableSet(EnumSet.of(FmrcDatasetType.TwoD, FmrcDatasetType.Best, FmrcDatasetType.Files, FmrcDatasetType.Runs));

  static public class FmrcConfig {
    public boolean regularize = regularizeDefault;
    public Set<FmrcDatasetType> datasets = defaultDatasetTypes;
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

      String[] types = StringUtil.split(datasetTypes);
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

}
