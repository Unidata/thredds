/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import jdk.nashorn.internal.ir.annotations.Immutable;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import thredds.inventory.partition.PartitionManager;
import thredds.inventory.partition.PartitionManagerFromIndexList;
import ucar.coord.Coordinate;
import ucar.coord.CoordinateRuntime;
import ucar.coord.CoordinateTime2D;
import ucar.coord.CoordinateTimeAbstract;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
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

  protected abstract boolean writeIndex(String name, String indexFilepath, CoordinateRuntime masterRuntime,
                                        List<? extends Group> groups, List<MFile> files, CalendarDateRange dateRange) throws IOException;

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

    File collectionIndexFile = GribIndexCache.getExistingFileOrCache(dcm.getIndexFilename(GribCdmIndex.NCX_SUFFIX));
    if (collectionIndexFile == null) return true;

    if (ff == CollectionUpdateType.nocheck) return false;

    return needsUpdate(ff, collectionIndexFile);
  }

  private boolean needsUpdate(CollectionUpdateType ff, File collectionIndexFile) throws IOException {
    long collectionLastModified = collectionIndexFile.lastModified();
    Set<String> newFileSet = new HashSet<>();

    CollectionManager.ChangeChecker cc = GribIndex.getChangeChecker();
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) {
      while (iter != null && iter.hasNext()) {
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

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

   // throw exception if failure
  public boolean createIndex(FeatureCollectionConfig.PartitionType ptype, Formatter errlog) throws IOException {
    if (ptype == FeatureCollectionConfig.PartitionType.all)
      return createAllRuntimeCollections(errlog);
    else
      return createMultipleRuntimeCollections(errlog);
  }

   // throw exception if failure
  private boolean createMultipleRuntimeCollections(Formatter errlog) throws IOException {
    long start = System.currentTimeMillis();

    List<MFile> files = new ArrayList<>();
    List<? extends Group> groups = makeGroups(files, false, errlog);
    List<MFile> allFiles = Collections.unmodifiableList(files);
    if (allFiles.size() == 0) {
      throw new IllegalStateException("No files in this collection =" + name + " topdir=" + dcm.getRoot());
    }
    if (groups.size() == 0) {
      throw new IllegalStateException("No records in this collection =" + name + " topdir=" + dcm.getRoot());
    }

    // create the master runtimes, classify the result
    CalendarDateRange calendarDateRangeAll = null;
    //boolean allTimesAreOne = true;
    boolean allTimesAreUnique = true;
    Set<Long> allRuntimes = new HashSet<>();
    for (Group g : groups) {
      for (Long cd : g.getCoordinateRuntimes())
        allRuntimes.add(cd);
      for (Coordinate coord : g.getCoordinates()) {
        if (coord instanceof CoordinateTime2D) {
          CoordinateTime2D coord2D = (CoordinateTime2D) coord;
          //if (coord2D.getNtimes() > 1) allTimesAreOne = false;
          if (allTimesAreUnique) {
            allTimesAreUnique = coord2D.hasUniqueTimes();
          }
        }
        if (coord instanceof CoordinateTimeAbstract) {
          CalendarDateRange calendarDateRange = ((CoordinateTimeAbstract) coord).makeCalendarDateRange(null);
          if (calendarDateRangeAll == null) calendarDateRangeAll = calendarDateRange;
          else calendarDateRangeAll = calendarDateRangeAll.extend(calendarDateRange);
        }
      }
    }
    List<Long> sortedList = new ArrayList<>();
    for (Long cd : allRuntimes) sortedList.add(cd);
    Collections.sort(sortedList);
    if (sortedList.size() == 0)
      throw new IllegalArgumentException("No runtimes in this collection ="+name);

    else if (sortedList.size() == 1)
      this.type = GribCollectionImmutable.Type.SRC;
    else if (allTimesAreUnique)
      this.type =  GribCollectionImmutable.Type.MRUTC;
    else
      this.type =  GribCollectionImmutable.Type.MRC;

    /* GribCollectionMutable result = new GribCollectionMutable(name, directory, config, isGrib1);
    GribCollectionMutable.Dataset dataset2D = result.makeDataset(this.type);
    dataset2D.groups = groups; */

    CoordinateRuntime masterRuntimes = new CoordinateRuntime(sortedList, null);
    MFile indexFileForRuntime = GribCollectionMutable.makeIndexMFile(this.name, directory);
    boolean ok = writeIndex(this.name, indexFileForRuntime.getPath(), masterRuntimes, groups, allFiles, calendarDateRangeAll);

    /* if (this.type ==  GribCollectionImmutable.Type.MRC) {
      GribBestDatasetBuilder.makeDatasetBest();
    } */

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return ok;
  }

  // PartitionType = all; not currently used but leave it here in case it needs to be revived
  // creates seperate collection and index for each runtime.
  private boolean createAllRuntimeCollections(Formatter errlog) throws IOException {
    long start = System.currentTimeMillis();
    this.type =  GribCollectionImmutable.Type.SRC;
    boolean ok = true;

    List<MFile> files = new ArrayList<>();
    List<? extends Group> groups = makeGroups(files, true, errlog);
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

     // create the master runtimes, consisting of the single runtime
      List<Long> runtimes = new ArrayList<>(1);
      runtimes.add(g.getRuntime().getMillis());
      CoordinateRuntime masterRuntimes = new CoordinateRuntime(runtimes, null);

      CalendarDateRange calendarDateRangeAll = null;
      for (Coordinate coord : g.getCoordinates()) {
        if (coord instanceof CoordinateTimeAbstract) {
          CalendarDateRange calendarDateRange = ((CoordinateTimeAbstract) coord).makeCalendarDateRange(null);
          if (calendarDateRangeAll == null) calendarDateRangeAll = calendarDateRange;
          else calendarDateRangeAll = calendarDateRangeAll.extend(calendarDateRange);
        }
      }
      assert calendarDateRangeAll != null;

      // for each Group write an index file
      ok &= writeIndex(gcname, indexFileForRuntime.getPath(), masterRuntimes, runGroupList, allFiles, calendarDateRangeAll);
      logger.info("GribCollectionBuilder write {} ok={}", indexFileForRuntime.getPath(), ok);
    }

    // if theres more than one runtime, create a partition collection to collect all the runtimes together
    if (multipleRuntimes) {
      Collections.sort(partitions); // ??
      PartitionManager part = new PartitionManagerFromIndexList(dcm, partitions, logger);
      part.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG));
      ok &= GribCdmIndex.updateGribCollectionFromPCollection(isGrib1, part, CollectionUpdateType.always, errlog, logger);
    }

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return ok;
  }

  public interface Group {
    CalendarDate getRuntime();
    List<Coordinate> getCoordinates();
    Set<Long> getCoordinateRuntimes();
  }

  @Immutable
  static protected class GroupAndRuntime {
    private final int hashCode;
    private final long runtime;

    GroupAndRuntime(int hashCode, long runtime) {
      this.hashCode = hashCode;
      this.runtime = runtime;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      GroupAndRuntime that = (GroupAndRuntime) o;
      if (hashCode != that.hashCode) return false;
      return runtime == that.runtime;
    }

    @Override
    public int hashCode() {
      int result = hashCode;
      result = 31 * result + (int) (runtime ^ (runtime >>> 32));
      return result;
    }
  }
}
