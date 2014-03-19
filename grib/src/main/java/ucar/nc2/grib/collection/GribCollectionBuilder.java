/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import thredds.inventory.partition.PartitionManager;
import thredds.inventory.partition.PartitionManagerFromIndexList;
import ucar.coord.CoordinateRuntime;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Superclass to builds indexes for collections of Grib files.
 *
 * @author caron
 * @since 2/19/14
 */
public abstract class GribCollectionBuilder {

  protected final MCollection dcm;
  protected final org.slf4j.Logger logger;
  protected final boolean isGrib1;

  protected String name;            // collection name
  protected File directory;         // top directory

  protected abstract List<? extends Group> makeGroups(List<MFile> allFiles, Formatter errlog) throws IOException;
  // indexFile not in cache
  protected abstract boolean writeIndex(String name, File indexFile, CoordinateRuntime masterRuntime, List<? extends Group> groups, List<MFile> files) throws IOException;

  // LOOK prob name could be dcm.getCollectionName()
  public GribCollectionBuilder(boolean isGrib1, String name, MCollection dcm, org.slf4j.Logger logger) {
    this.dcm = dcm;
    this.logger = logger;
    this.isGrib1 = isGrib1;

    this.name = StringUtil2.replace(name, ' ', "_");
    this.directory = new File(dcm.getRoot());
  }

  public boolean updateNeeded(CollectionUpdateType ff) throws IOException {
    if (ff == CollectionUpdateType.never) return false;
    if (ff == CollectionUpdateType.always) return true;

    File idx = GribCollection.getFileInCache(dcm.getIndexFilename());
    if (!idx.exists()) return true;

    if (ff == CollectionUpdateType.nocheck) return false;

    return collectionWasChanged(idx.lastModified());
  }

  private boolean collectionWasChanged(long idxLastModified) throws IOException {
    CollectionManager.ChangeChecker cc = GribIndex.getChangeChecker();
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) {
      while (iter.hasNext()) {
        if (cc.hasChangedSince(iter.next(), idxLastModified)) return true;   // checks both data and gbx9 file
      }
    }
    return false;
  }

  public boolean createIndex(Formatter errlog) throws IOException {
    if (dcm == null) {
      logger.error("GribCollectionBuilder " + name + " : cannot create new index ");
      throw new IllegalStateException();
    }

    long start = System.currentTimeMillis();

    List<MFile> files = new ArrayList<>();
    List<? extends Group> groups = makeGroups(files, errlog);
    List<MFile> allFiles = Collections.unmodifiableList(files);

    // gather into collections with a single runtime
    Map<Long, List<Group>> runGroups = new HashMap<>();
    for (Group g : groups) {
      List<Group> runGroup = runGroups.get(g.getRuntime().getMillis());
      if (runGroup == null) {
        runGroup = new ArrayList<>();
        runGroups.put(g.getRuntime().getMillis(), runGroup);
      }
      runGroup.add(g);
    }

    // write each rungroup separately
    boolean multipleRuntimes = runGroups.values().size() > 1;
    List<File> partitions = new ArrayList<>();
    for (List<Group> runGroupList : runGroups.values()) {
      Group g = runGroupList.get(0);
      // if multiple groups, we will write a partition. otherwise, we need to use the standard name (without runtime) so we know the filename from the collection
      String gcname = multipleRuntimes ? GribCollection.makeName(this.name, g.getRuntime()) : this.name;
      File indexFileForRuntime = GribCollection.makeIndexFile(gcname, directory); // not in cache
      partitions.add(indexFileForRuntime);

      boolean ok = writeIndex(gcname, indexFileForRuntime, g.getCoordinateRuntime(), runGroupList, allFiles);
      logger.info("GribCollectionBuilder write {} ok={}", indexFileForRuntime.getPath(), ok);
    }

    boolean ok = true;

    // if theres more than one runtime, create a partition collection to collect all the runtimes together
    if (multipleRuntimes) {
      Collections.sort(partitions); // ??
      PartitionManager part = new PartitionManagerFromIndexList(dcm, partitions, logger);
      part.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG));
      ok = GribCdmIndex.updateGribCollectionFromMCollection(isGrib1, part, CollectionUpdateType.always, errlog, logger);
    }

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return ok;
  }

  public interface Group {
    CalendarDate getRuntime();
    CoordinateRuntime getCoordinateRuntime();
  }

  protected class GroupAndRuntime {
    int gdsHash;
    long runtime;

    GroupAndRuntime(int gdsHash, long runtime) {
      this.gdsHash = gdsHash;
      this.runtime = runtime;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      GroupAndRuntime that = (GroupAndRuntime) o;

      if (gdsHash != that.gdsHash) return false;
      if (runtime != that.runtime) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = gdsHash;
      result = 31 * result + (int) (runtime ^ (runtime >>> 32));
      return result;
    }
  }
}
