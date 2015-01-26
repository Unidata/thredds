/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.catalog;

import org.jdom2.Element;

import java.util.List;

/**
 * DatasetScan Configuration object
 * Could enclose in a DatasetScan ?
 *
 * @author John
 * @since 1/12/2015
 */
public class DatasetScanConfig {
  public String name;
  public String path;
  public String scanDir;
  public String restrictAccess;

  public boolean addDatasetSize = true;
  public boolean isSortIncreasing = true;

  public Element ncmlElement;
  public List<Filter> filters;
  public List<Namer> namers;
  public AddLatest addLatest;
  public AddTimeCoverage addTimeCoverage;

  @Override
  public String toString() {
    return "DatasetScanConfig{" +
            "name='" + name + '\'' +
            ", path='" + path + '\'' +
            ", scanDir='" + scanDir + '\'' +
            ", restrictAccess='" + restrictAccess + '\'' +
            ", addDatasetSize=" + addDatasetSize +
            ", isSortIncreasing=" + isSortIncreasing +
            ", ncmlElement=" + ncmlElement +
            ", filters=" + filters +
            ", namers=" + namers +
            ", proxies=" + addLatest +
            ", addTimeCoverage=" + addTimeCoverage +
            '}';
  }

  public static class Filter {
    String regExpAttVal, wildcardAttVal;
    long lastModLimitAttVal;
    boolean atomic, collection, includer;

    public Filter(String regExpAttVal, String wildcardAttVal, long lastModLimitAttVal, boolean atomic, boolean collection, boolean includer) {
      this.regExpAttVal = regExpAttVal;
      this.wildcardAttVal = wildcardAttVal;
      this.lastModLimitAttVal = lastModLimitAttVal;
      this.atomic = atomic;
      this.collection = collection;
      this.includer = includer;
    }
  }

  public static class Namer {
    boolean onName;
    String regExp, replaceString;

    public Namer(boolean onName, String regExp, String replaceString) {
      this.onName = onName;
      this.regExp = regExp;
      this.replaceString = replaceString;
    }
  }

  public static class AddLatest {
    String latestName, latestServiceName;
    boolean latestOnTop, isResolver;
    long lastModLimit;

    public AddLatest() {
      latestName = "latest.xml";
      latestOnTop = true;
      latestServiceName = "latest";
      isResolver = true;
      lastModLimit = -1;
    }

    public AddLatest(String latestName, String latestServiceName, boolean latestOnTop, boolean isResolver, long lastModLimit) {
      this.latestName = latestName;
      this.latestServiceName = latestServiceName;
      this.latestOnTop = latestOnTop;
      this.isResolver = isResolver;
      this.lastModLimit = lastModLimit;
    }
  }

  public static class AddTimeCoverage {
    String matchName, matchPath, subst, duration;

    public AddTimeCoverage(String matchName, String matchPath, String subst, String duration) {
      this.matchName = matchName;
      this.matchPath = matchPath;
      this.subst = subst;
      this.duration = duration;
    }
  }
}
