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

import net.jcip.annotations.Immutable;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.server.catalog.ConfigCatalog;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;

/**
 * A DataRoot matches URLs to the objects that can serve them.
 * A PathMatcher manages a hash table of path -> DataRoot
 *
 * Possible design:
 *   catKey : which catalog defined this?   not present at the moment
 *   directory : if its a simple DataRoot
 *   fc        : prob is this drags in entire configCat
 *   dscan     : ditto
*
* @author caron
* @since 1/23/2015
*/
@Immutable
public class DataRoot {
  private final String path;          // match this path
  private final String dirLocation;   // to this directory
  private final DatasetScan scan;     // the DatasetScan that created this (may be null)
  private final FeatureCollectionRef featCollection; // the FeatureCollection that created this (may be null)

  DataRoot(FeatureCollectionRef featCollection) {
    this.path = featCollection.getPath();
    this.dirLocation = featCollection.getTopDirectoryLocation();
    this.scan = null;
    this.featCollection = featCollection;
    show();
  }

  DataRoot(DatasetScan scan) {
    this.path = scan.getPath();
    this.dirLocation = scan.getScanLocation();
    this.scan = scan;
    this.featCollection = null;
    show();
  }

  DataRoot(String path, String dirLocation) {
    this.path = path;
    this.dirLocation = dirLocation;
    this.scan = null;
    this.featCollection = null;
    show();
  }

  private void show() {
    if (DataRootManager.debug) System.out.printf(" DataRoot %s==%s%n", path, dirLocation);
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

  public FeatureCollectionRef getFeatureCollection() {
    return featCollection;
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

  public String getFileLocationFromRequestPath(String reqPath) {

    if (scan != null) {
      // LOOK should check to see if its been filtered out by scan
      return getFileLocationFromRequestPath(reqPath, scan.getPath(), scan.getScanLocation());

    } else if (featCollection != null) {
       // LOOK should check to see if its allowed in fc
      return getFileLocationFromRequestPath(reqPath, featCollection.getPath(), featCollection.getTopDirectoryLocation());

    } else {  // must be a datasetRoot
       // LOOK should check to see if it exists ??
      return getFileLocationFromRequestPath(reqPath, getPath(), getDirLocation());
    }

  }

  private String getFileLocationFromRequestPath(String reqPath, String rootPath, String rootLocation) {
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

     if (!rootLocation.endsWith("/"))
       rootLocation = rootLocation + "/";

    // put it together
    return (locationReletive.length() > 1) ? rootLocation + locationReletive : rootLocation;
  }
}
