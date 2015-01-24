/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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
package thredds.core;

import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollection;
import ucar.unidata.util.StringUtil2;

import java.util.HashMap;
import java.util.Map;

/**
* A DataRoot matches URLs to file directories
*
* @author caron
* @since 1/23/2015
*/
public class DataRoot {

  private static Map<String, String> alias = new HashMap<>(); // LOOK temp kludge

  public static void addAlias(String aliasKey, String actual) {
    alias.put(aliasKey, StringUtil2.substitute(actual, "\\", "/"));
  }

  public static String translateAlias(String scanDir) {
    for (Map.Entry<String, String> entry : alias.entrySet()) {
      if (scanDir.contains(entry.getKey()))
        return StringUtil2.substitute(scanDir, entry.getKey(), entry.getValue());
    }
    return scanDir;
  }

  private String path;          // match this path
  private String dirLocation;   // to this directory
  private DatasetScan scan;     // the DatasetScan that created this (may be null)
  private FeatureCollection featCollection; // the FeatureCollection that created this (may be null)
  private boolean cache = true;

  DataRoot(FeatureCollection featCollection) {
    setPath(featCollection.getPath());
    this.featCollection = featCollection;
    this.dirLocation = featCollection.getTopDirectoryLocation();
    show();
  }

  DataRoot(DatasetScan scan) {
    setPath( scan.getPath());
    this.scan = scan;
    this.dirLocation = scan.getScanLocation();
    show();
  }

  DataRoot(String path, String dirLocation) {
    setPath(path);
    this.dirLocation = dirLocation;
    this.scan = null;
    show();
  }

  private void setPath(String path) {
    // if (path.endsWith("/")) path = path + "/";
    this.path = path;
  }


  private void show() {
    if (DataRootHandler.debug) System.out.printf(" DataRoot %s==%s%n", path, dirLocation);
  }

  public String getPath() {
    return path;
  }

  public String getDirLocation() {
    return dirLocation;
  }

  public DatasetScan getDatasetScan() {
    return scan;
  }

  public FeatureCollection getFeatureCollection() {
    return featCollection;
  }

  public boolean isCache() {
    return cache;
  }

  // used by PathMatcher
  public String toString() {
    return path;
  }

  // debug
  public String toString2() {
    return path + "," + dirLocation;
  }

  /**
   * Instances which have same path are equal.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataRoot root = (DataRoot) o;
    return path.equals(root.path);
  }

  public int hashCode() {
    return path.hashCode();
  }
}
