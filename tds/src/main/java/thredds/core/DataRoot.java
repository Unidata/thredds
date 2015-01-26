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

import thredds.server.catalog.ConfigCatalog;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollection;

/**
* A DataRoot matches URLs to file directories
*
* @author caron
* @since 1/23/2015
*/
public class DataRoot {
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

  ////////////////

  public String getFileLocationFromRequestPath(String path) {

    if (scan != null)
      return getFileLocationFromRequestPath(path, scan.getPath(), scan.getScanLocation());

    if (featCollection != null)
      return getFileLocationFromRequestPath(path, featCollection.getPath(), featCollection.getTopDirectoryLocation());
    //   return null; // if featCollection exists, bail out and deal with it in caller LOOK why ?

    // must be a datasetRoot
    return getFileLocationFromRequestPath(path, getPath(), getDirLocation());

  }

  public static String getFileLocationFromRequestPath(String reqPath, String rootPath, String rootLocation) {
    if (reqPath == null) return null;
     if (reqPath.length() == 0) return null;

     if (reqPath.startsWith("/"))
       reqPath = reqPath.substring(1);

     if (!reqPath.startsWith(rootPath))
       return null;

     // remove the matching part, the rest is the "data directory"
     String locationReletive = reqPath.substring(rootPath.length());
     if (locationReletive.startsWith("/"))
       locationReletive = locationReletive.substring(1);

     if (!locationReletive.endsWith("/"))
       locationReletive = locationReletive + "/";

        // translate any properties
    String scanDir = ConfigCatalog.translateAlias(rootLocation); // LOOK we may have already done this

    // put it together
    return (locationReletive.length() > 1) ? scanDir + "/" + locationReletive : scanDir;
  }
}
