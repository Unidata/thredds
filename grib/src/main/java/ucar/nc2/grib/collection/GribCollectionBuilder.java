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
 * Superclass to build indexes for collections of Grib files.
 *
 * @author caron
 * @since 2/19/14
 */
abstract class GribCollectionBuilder {

  protected final MCollection dcm;
  protected final org.slf4j.Logger logger;
  protected final boolean isGrib1;
  protected GribCollectionImmutable.Type type;

  protected String name;            // collection name
  protected File directory;         // top directory

  protected abstract List<? extends Group> makeGroups(List<MFile> allFiles, boolean singleRuntime, Formatter errlog) throws IOException;

  // indexFile not in cache
  protected abstract boolean writeIndex(String name, String indexFilepath, CoordinateRuntime masterRuntime, List<? extends Group> groups, List<MFile> files) throws IOException;

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

    File collectionIndexFile = GribIndex.getFileInCache(dcm.getIndexFilename());
    if (!collectionIndexFile.exists()) return true;

    if (ff == CollectionUpdateType.nocheck) return false;

    return needsUpdate(ff, collectionIndexFile);
  }

  private boolean needsUpdate(CollectionUpdateType ff, File collectionIndexFile) throws IOException {
    long collectionLastModified = collectionIndexFile.lastModified();
    Set<String> newFileSet = new HashSet<>();

    CollectionManager.ChangeChecker cc = GribIndex.getChangeChecker();
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) {
      while (iter.hasNext()) {
        MFile memberOfCollection = iter.next();
        if (cc.hasChangedSince(memberOfCollection, collectionLastModified)) return true;   // checks both data and gbx9 file
        newFileSet.add(memberOfCollection.getPath());
      }
    }
    if (ff == CollectionUpdateType.testIndexOnly) return false;

    // now see if any files were deleted, by reading the index and comparing to the files there
    GribCdmIndex reader = new GribCdmIndex(logger);
    List<MFile> oldFiles = new ArrayList<>();
    reader.readMFiles(collectionIndexFile.toPath(), oldFiles);
    Set<String> oldFileSet = new HashSet<>();
    for (MFile oldFile : oldFiles) {
      if (!newFileSet.contains(oldFile.getPath()))
        return true;              // got deleted - must recreate the index
      oldFileSet.add(oldFile.getPath());
    }

    // now see if any files were added
    for (String newFilename : newFileSet) {
      if (!oldFileSet.contains(newFilename))
        return true;              // got added - must recreate the index
    }

    return false;
  }

  public boolean createIndex(FeatureCollectionConfig.PartitionType ptype, Formatter errlog) throws IOException {
    switch (ptype) {
      case none: return createSingleRuntimeCollections(errlog);
      case directory: return createMultipleRuntimeCollections(errlog);
      case file: return createMultipleRuntimeCollections(errlog);
    }
    throw new IllegalArgumentException("unknown FeatureCollectionConfig.PartitionType ="+ptype);
  }

  private boolean createMultipleRuntimeCollections(Formatter errlog) throws IOException {
    long start = System.currentTimeMillis();
    this.type =  GribCollectionImmutable.Type.MRC;  // not actually correct; but still need to implement Best, so, fix then

    List<MFile> files = new ArrayList<>();
    List<? extends Group> groups = makeGroups(files, false, errlog); // populate files
    List<MFile> allFiles = Collections.unmodifiableList(files);

    Group canon = null;
    for (Group g : groups) {
      // LOOK if multiple groups, probably need to combine all runtimes ?
      canon = g;   // fake it for now
    }
    assert canon != null;

    MFile indexFileForRuntime = GribCollectionMutable.makeIndexMFile(this.name, directory);
    boolean ok = writeIndex(this.name, indexFileForRuntime.getPath(), canon.getCoordinateRuntime(), groups, allFiles);

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return ok;
  }

   private boolean createSingleRuntimeCollections(Formatter errlog) throws IOException {
    long start = System.currentTimeMillis();
    this.type =  GribCollectionImmutable.Type.SRC;

    List<MFile> files = new ArrayList<>();
    List<? extends Group> groups = makeGroups(files, true, errlog); // populate files
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
    List<MFile> partitions = new ArrayList<>();
    for (List<Group> runGroupList : runGroups.values()) {
      Group g = runGroupList.get(0);
      // if multiple Runtimes, we will write a partition. otherwise, we need to use the standard name (without runtime) so we know the filename from the collection
      String gcname = multipleRuntimes ? GribCollectionMutable.makeName(this.name, g.getRuntime()) : this.name;
      MFile indexFileForRuntime = GribCollectionMutable.makeIndexMFile(gcname, directory); // not using disk cache LOOK why ?
      partitions.add(indexFileForRuntime);

      // for each Group write an index file
      boolean ok = writeIndex(gcname, indexFileForRuntime.getPath(), g.getCoordinateRuntime(), runGroupList, allFiles);
      logger.info("GribCollectionBuilder write {} ok={}", indexFileForRuntime.getPath(), ok);
    }

    boolean ok = true;

    // if theres more than one runtime, create a partition collection to collect all the runtimes together
    if (multipleRuntimes) {
      Collections.sort(partitions); // ??
      PartitionManager part = new PartitionManagerFromIndexList(dcm, partitions, logger);
      part.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG));
      ok = GribCdmIndex.updateGribCollectionFromPCollection(isGrib1, part, CollectionUpdateType.always, errlog, logger);
    }

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return ok;
  }


  static public interface Group {
    CalendarDate getRuntime();             // LOOK HERE1 a single runtime
    CoordinateRuntime getCoordinateRuntime();
  }

  static protected class GroupAndRuntime {
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
