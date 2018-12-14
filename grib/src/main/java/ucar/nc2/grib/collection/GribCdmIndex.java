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

package ucar.nc2.grib.collection;

import com.beust.jcommander.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionConfigBuilder;
import thredds.featurecollection.FeatureCollectionType;
import thredds.filesystem.MFileOS;
import thredds.inventory.*;
import thredds.inventory.filter.StreamFilter;
import thredds.inventory.partition.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.grib1.Grib1RecordScanner;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.stream.NcStream;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utilities for creating GRIB CDM index (ncx3) files, both collections and partitions
 *
 * @author John
 * @since 12/5/13
 */
public class GribCdmIndex implements IndexReader {
  static public enum GribCollectionType {GRIB1, GRIB2, Partition1, Partition2, none}

  static private final Logger classLogger = LoggerFactory.getLogger(GribCdmIndex.class);


  /////////////////////////////////////////////////////////////////////////////

  // object cache for ncx3 files - these are opened only as GribCollection
  static public FileCacheIF gribCollectionCache;

  static public void initDefaultCollectionCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    // gribCollectionCache = new ucar.nc2.util.cache.FileCache("DefaultGribCollectionCache", minElementsInMemory, maxElementsInMemory, -1, period);
    gribCollectionCache = new ucar.nc2.util.cache.FileCacheGuava("DefaultGribCollectionCache", maxElementsInMemory);
  }

  static public void disableGribCollectionCache() {
    if (null != gribCollectionCache) gribCollectionCache.disable();
    gribCollectionCache = null;
  }

  static public void setGribCollectionCache(FileCacheIF cache) {
    if (null != gribCollectionCache) gribCollectionCache.disable();
    gribCollectionCache = cache;
  }

  // open GribCollectionImmutable from an existing index file. return null on failure
  static public GribCollectionImmutable acquireGribCollection(FileFactory factory, Object hashKey, String location, int buffer_size, CancelTask cancelTask, Object spiObject) throws IOException {
    FileCacheable result;

    if (gribCollectionCache != null) {
      // FileFactory factory, Object hashKey, String location, int buffer_size, CancelTask cancelTask, Object spiObject
      result = GribCdmIndex.gribCollectionCache.acquire(factory, hashKey, location, buffer_size, cancelTask, spiObject);

    } else {
      // String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object iospMessage
      result = factory.open(location, buffer_size, cancelTask, spiObject);
    }

    return (GribCollectionImmutable) result;
  }

  static public void shutdown() {
    if (gribCollectionCache != null) gribCollectionCache.clearCache(true);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * This is only used for the top level GribCollection.
   *
   * @param config use this FeatureCollectionConfig
   * @return index File
   */
  static private File makeTopIndexFileFromConfig(FeatureCollectionConfig config) {
    Formatter errlog = new Formatter();
    CollectionSpecParser specp = config.getCollectionSpecParser(errlog);

    String name = StringUtil2.replace(config.collectionName, '\\', "/");
    // String cname = DirectoryCollection.makeCollectionName(name, Paths.get(specp.getRootDir()));

    return makeIndexFile(name, new File(specp.getRootDir()));
  }

  static File makeIndexFile(String collectionName, File directory) {
    String nameNoBlanks = StringUtil2.replace(collectionName, ' ', "_");
    return new File(directory, nameNoBlanks + CollectionAbstract.NCX_SUFFIX);
  }

  static private String makeNameFromIndexFilename(String idxPathname) {
    idxPathname = StringUtil2.replace(idxPathname, '\\', "/");
    int pos = idxPathname.lastIndexOf('/');
    String idxFilename = (pos < 0) ? idxPathname : idxPathname.substring(pos + 1);
    assert idxFilename.endsWith(CollectionAbstract.NCX_SUFFIX);
    return idxFilename.substring(0, idxFilename.length() - CollectionAbstract.NCX_SUFFIX.length());
  }

  ///////////////////////////////////////////

  /**
   * Find out what kind of index this is
   *
   * @param raf open RAF
   * @return GribCollectionType
   * @throws IOException on read error
   */
  static public GribCollectionType getType(RandomAccessFile raf) throws IOException {
    String magic;

    raf.seek(0);
    magic = raf.readString(Grib2CollectionWriter.MAGIC_START.getBytes(CDM.utf8Charset).length);

    switch (magic) {
      case Grib2CollectionWriter.MAGIC_START:
        return GribCollectionType.GRIB2;

      case Grib1CollectionWriter.MAGIC_START:
        return GribCollectionType.GRIB1;

      case Grib2PartitionBuilder.MAGIC_START:
        return GribCollectionType.Partition2;

      case Grib1PartitionBuilder.MAGIC_START:
        return GribCollectionType.Partition1;

    }
    return GribCollectionType.none;
  }

  // open GribCollection from an existing index file. return null on failure
  static public GribCollectionImmutable openCdmIndex(String indexFilename, FeatureCollectionConfig config, Logger logger) {
    return openCdmIndex(indexFilename, config, true, logger);
  }

  // open GribCollectionImmutable from an existing index file. return null on failure
  static public GribCollectionImmutable openCdmIndex(String indexFilename, FeatureCollectionConfig config, boolean useCache, Logger logger) {
    File indexFileInCache = useCache ? GribIndexCache.getExistingFileOrCache(indexFilename) : new File(indexFilename);
    if (indexFileInCache == null)
      return null;
    String indexFilenameInCache = indexFileInCache.getPath();
    String name = makeNameFromIndexFilename(indexFilename);
    GribCollectionImmutable result = null;

    try (RandomAccessFile raf = RandomAccessFile.acquire(indexFilenameInCache)) {
      GribCollectionType type = getType(raf);

      switch (type) {
        case GRIB2:
          result = Grib2CollectionBuilderFromIndex.readFromIndex(name, raf, config, logger);
          break;
        case Partition2:
          result = Grib2PartitionBuilderFromIndex.createTimePartitionFromIndex(name, raf, config, logger);
          break;
        case GRIB1:
          result = Grib1CollectionBuilderFromIndex.readFromIndex(name, raf, config, logger);
          break;
        case Partition1:
          result = Grib1PartitionBuilderFromIndex.createTimePartitionFromIndex(name, raf, config, logger);
          break;
        default:
          logger.warn("GribCdmIndex.openCdmIndex failed on {} type={}", indexFilenameInCache, type);
      }

    } catch (Throwable t) {
      logger.warn("GribCdmIndex.openCdmIndex failed on " + indexFilenameInCache, t);
      RandomAccessFile.eject(indexFilenameInCache);
      if (!indexFileInCache.delete())
        logger.warn("failed to delete {}", indexFileInCache.getPath());
    }

    return result;
  }

  // used by PartitionCollectionMutable.Partition
  // open GribCollectionImmutable from an existing index file. return null on failure
  static public GribCollectionMutable openMutableGCFromIndex(String indexFilename, FeatureCollectionConfig config, boolean dataOnly, boolean useCache, Logger logger) {
    File indexFileInCache = useCache ? GribIndexCache.getExistingFileOrCache(indexFilename) : new File(indexFilename);
    if (indexFileInCache == null) {
      return null;
    }
    String indexFilenameInCache = indexFileInCache.getPath();
    String name = makeNameFromIndexFilename(indexFilename);
    GribCollectionMutable result = null;

    try (RandomAccessFile raf = RandomAccessFile.acquire(indexFilenameInCache)) {
      GribCollectionType type = getType(raf);

      switch (type) {
        case GRIB2:
          result = Grib2CollectionBuilderFromIndex.openMutableGCFromIndex(name, raf, config, logger);
          break;
        case Partition2:
          result = Grib2PartitionBuilderFromIndex.openMutablePCFromIndex(name, raf, config, logger);
          break;
        case GRIB1:
          result = Grib1CollectionBuilderFromIndex.openMutableGCFromIndex(name, raf, config, logger);
          break;
        case Partition1:
          result = Grib1PartitionBuilderFromIndex.openMutablePCFromIndex(name, raf, config, logger);
          break;
        default:
          logger.warn("GribCdmIndex.openMutableGCFromIndex failed on {} type={}", indexFilenameInCache, type);
      }

      if (result != null) {
        result.lastModified = raf.getLastModified();
        result.fileSize = raf.length();
      }

    } catch (Throwable t) {
      logger.warn("GribCdmIndex.openMutableGCFromIndex failed on " + indexFilenameInCache, t);
    }

    if (result == null) {
      RandomAccessFile.eject(indexFilenameInCache);
      if (!indexFileInCache.delete())
        logger.warn("failed to delete {}", indexFileInCache.getPath());
    }

    return result;
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  // used by PartitionCollection

  // used (only) by GribCollectionBuilder
  static public boolean updateGribCollectionFromPCollection(boolean isGrib1, PartitionManager dcm, CollectionUpdateType updateType,
                                                            Formatter errlog, org.slf4j.Logger logger) throws IOException {

    if (updateType == CollectionUpdateType.never || dcm instanceof CollectionSingleIndexFile) { // LOOK would isIndexFile() be better ?
      // then just open the existing index file
      return false;
    }

    boolean changed = updatePartition(isGrib1, dcm, updateType, logger, errlog);
    if (errlog != null) errlog.format("PartitionCollection %s was recreated %s%n", dcm.getCollectionName(), changed);
    return changed;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // used by Tdm (indirectly by InvDatasetFcGrib)

  /**
   * update Grib Collection if needed
   *
   * @return true if the collection was updated
   * @throws IOException
   */
  static public boolean updateGribCollection(FeatureCollectionConfig config, CollectionUpdateType updateType, Logger logger) throws IOException {

    long start = System.currentTimeMillis();

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = config.getCollectionSpecParser(errlog);
    Path rootPath = Paths.get(specp.getRootDir());
    boolean isGrib1 = config.type == FeatureCollectionType.GRIB1;

    boolean changed;

    if (config.ptype == FeatureCollectionConfig.PartitionType.none) {

      try (CollectionAbstract dcm = new CollectionPathMatcher(config, specp, logger)) {
        changed = updateGribCollection(isGrib1, dcm, updateType, FeatureCollectionConfig.PartitionType.none, logger, errlog);
      }

    } else if (config.ptype == FeatureCollectionConfig.PartitionType.timePeriod) {

      try (TimePartition tp = new TimePartition(config, specp, logger)) {
        changed = updateTimePartition(isGrib1, tp, updateType, logger);
      }

    } else {

      // LOOK assume wantSubdirs makes it into a Partition. Isnt there something better ??
      if (specp.wantSubdirs()) {  // its a partition

        try (DirectoryPartition dpart = new DirectoryPartition(config, rootPath, true, new GribCdmIndex(logger), logger)) {
          dpart.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
          changed = updateDirectoryCollectionRecurse(isGrib1, dpart, config, updateType, logger);
        }

      } else { // otherwise its a leaf directory
        changed = updateLeafCollection(isGrib1, config, updateType, true, logger, rootPath);
      }
    }

    long took = System.currentTimeMillis() - start;
    logger.info("updateGribCollection {} changed {} took {} msecs", config.collectionName, changed, took);
    return changed;
  }

  // return true if changed, exception on failure
  static private boolean updateGribCollection(boolean isGrib1, MCollection dcm, CollectionUpdateType updateType, FeatureCollectionConfig.PartitionType ptype,
                                              Logger logger, Formatter errlog) throws IOException {

    if (debug) System.out.printf("GribCdmIndex.updateGribCollection %s %s%n", dcm.getCollectionName(), updateType);
    if (!isUpdateNeeded(dcm.getIndexFilename(), updateType, (isGrib1 ? GribCollectionType.GRIB1 : GribCollectionType.GRIB2), logger)) return false;

    boolean changed;
    if (isGrib1) {  // existing case handles correctly - make seperate index for each runtime (OR) partition == runtime
      Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm.getCollectionName(), dcm, logger);
      changed = builder.updateNeeded(updateType) && builder.createIndex(ptype, errlog);
    } else {
      Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
      changed = builder.updateNeeded(updateType) && builder.createIndex(ptype, errlog);
    }
    return changed;
  }

  // return true if changed, exception on failure
  static private boolean updatePartition(boolean isGrib1, PartitionManager dcm, CollectionUpdateType updateType,
                                         Logger logger, Formatter errlog) throws IOException {
    boolean changed;
    if (isGrib1) {
      Grib1PartitionBuilder builder = new Grib1PartitionBuilder(dcm.getCollectionName(), new File(dcm.getRoot()), dcm, logger);
      changed = builder.updateNeeded(updateType) && builder.createPartitionedIndex(updateType, errlog);

    } else {
      Grib2PartitionBuilder builder = new Grib2PartitionBuilder(dcm.getCollectionName(), new File(dcm.getRoot()), dcm, logger);
      changed = builder.updateNeeded(updateType) && builder.createPartitionedIndex(updateType, errlog);
    }
    return changed;
  }


  static private boolean updateTimePartition(boolean isGrib1, TimePartition tp, CollectionUpdateType updateType, Logger logger) throws IOException {

    if (debug) System.out.printf("GribCdmIndex.updateTimePartition %s %s%n", tp.getRoot(), updateType);
    if (!isUpdateNeeded(tp.getIndexFilename(), updateType, (isGrib1 ? GribCollectionType.Partition1 : GribCollectionType.Partition2), logger)) return false;

    long start = System.currentTimeMillis();
    Formatter errlog = new Formatter();

    for (MCollection part : tp.makePartitions(updateType)) {
      try {
        updateGribCollection(isGrib1, part, updateType, FeatureCollectionConfig.PartitionType.timePeriod, logger, errlog);

      } catch (Throwable t) {
        logger.warn("Error making partition " + part.getRoot(), t);
        tp.removePartition(part); // keep on truckin; can happen if directory is empty
      }
    }   // loop over component grib collections


    try {
      boolean changed = updatePartition(isGrib1, tp, updateType, logger, errlog);

      long took = System.currentTimeMillis() - start;
      errlog.format(" INFO updateTimePartition %s took %d msecs%n", tp.getRoot(), took);
      if (debug) System.out.printf("GribCdmIndex.updateTimePartition complete (%s) on %s errlog=%s%n", changed, tp.getRoot(), errlog);
      return changed;

    } catch (IllegalStateException t) {
      logger.warn("Error making partition {} '{}'", tp.getRoot(), t.getMessage());
      return false;

    } catch (Throwable t) {
      logger.error("Error making partition " + tp.getRoot(), t);
      return false;
    }
  }

  static private boolean isUpdateNeeded(String idxFilenameOrg, CollectionUpdateType updateType, GribCollectionType wantType, Logger logger) {
    if (updateType == CollectionUpdateType.never) return false;

    // see if index already exists
    File collectionIndexFile = GribIndexCache.getExistingFileOrCache(idxFilenameOrg);
    if (collectionIndexFile != null) {   // it exists

      boolean bad;
      try (RandomAccessFile raf = RandomAccessFile.acquire(collectionIndexFile.getPath())) {  // read it to verify its good
        GribCollectionType type = getType(raf);
        bad = (type != wantType);
        if (!bad && updateType == CollectionUpdateType.nocheck) return false;  // use if index is ok

      } catch (IOException ioe) {
        bad = true;
      }

      if (bad) { // delete the file and remove from cache if its in there
        RandomAccessFile.eject(collectionIndexFile.getPath());
        if (!collectionIndexFile.delete())
          logger.warn("failed to delete {}", collectionIndexFile.getPath());
      }
    }

    return true;
  }

  static private boolean updateDirectoryCollectionRecurse(boolean isGrib1, DirectoryPartition dpart,
                                                          FeatureCollectionConfig config,
                                                          CollectionUpdateType updateType,
                                                          Logger logger) throws IOException {

    if (debug) System.out.printf("GribCdmIndex.updateDirectoryCollectionRecurse %s %s%n", dpart.getRoot(), updateType);
    if (!isUpdateNeeded(dpart.getIndexFilename(), updateType, (isGrib1 ? GribCollectionType.Partition1 : GribCollectionType.Partition2), logger)) return false;

    long start = System.currentTimeMillis();

    // check the children partitions first
    if (updateType != CollectionUpdateType.testIndexOnly) {   // skip children on testIndexOnly
      for (MCollection part : dpart.makePartitions(updateType)) {
        part.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
        try {
          if (part instanceof DirectoryPartition) {   // LOOK if child partition fails, the parent partition doesnt know that - suckage
            updateDirectoryCollectionRecurse(isGrib1, (DirectoryPartition) part, config, updateType, logger);
          } else {
            Path partPath = Paths.get(part.getRoot());
            updateLeafCollection(isGrib1, config, updateType, false, logger, partPath); // LOOK why not using part ??
          }
        } catch (IllegalStateException t) {
          logger.warn("Error making partition {} '{}'", part.getRoot(), t.getMessage());
          dpart.removePartition(part); // keep on truckin; can happen if directory is empty

        } catch (Throwable t) {
          logger.error("Error making partition " + part.getRoot(), t);
          dpart.removePartition(part);
        }
      }   // loop over partitions
    }

    try {
      // update the partition
      Formatter errlog = new Formatter();
      boolean changed = updatePartition(isGrib1, dpart, updateType, logger, errlog);

      long took = System.currentTimeMillis() - start;
      errlog.format(" INFO updateDirectoryCollectionRecurse %s took %d msecs%n", dpart.getRoot(), took);
      if (debug) System.out.printf("GribCdmIndex.updateDirectoryCollectionRecurse complete (%s) on %s errlog=%s%n", changed, dpart.getRoot(), errlog);
      return changed;

    } catch (IllegalStateException t) {
      logger.warn("Error making partition {} '{}'", dpart.getRoot(), t.getMessage());
      return false;

    } catch (Throwable t) {
      logger.error("Error making partition " + dpart.getRoot(), t);
      return false;
    }
  }

  /**
   * Update all the gbx indices in one directory, and the ncx index for that directory
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @return true if collection was rewritten, exception on failure
   * @throws IOException
   */
  static private boolean updateLeafCollection(boolean isGrib1, FeatureCollectionConfig config,
                                              CollectionUpdateType updateType, boolean isTop,
                                              Logger logger, Path dirPath) throws IOException {

    if (config.ptype == FeatureCollectionConfig.PartitionType.file) {
      return updateFilePartition(isGrib1, config, updateType, isTop, logger, dirPath);

    } else {
      Formatter errlog = new Formatter();
      CollectionSpecParser specp = config.getCollectionSpecParser(errlog);

      try (DirectoryCollection dcm = new DirectoryCollection(config.collectionName, dirPath, isTop, config.olderThan, logger)) {
        dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
        if (specp.getFilter() != null)
          dcm.setStreamFilter(new StreamFilter(specp.getFilter(), specp.getFilterOnName()));

        boolean changed = updateGribCollection(isGrib1, dcm, updateType, FeatureCollectionConfig.PartitionType.directory, logger, errlog);
        if (debug) System.out.printf("  GribCdmIndex.updateDirectoryPartition was updated=%s on %s%n", changed, dirPath);
        return changed;
      }
    }

  }


  /**
   * File Partition: each File is a collection of Grib records, and the collection of all files in the directory is a PartitionCollection.
   * Rewrite the PartitionCollection and optionally its children
   *
   * @param config     FeatureCollectionConfig
   * @param updateType always, test, nocheck, never
   * @return true if partition was rewritten, exception on failure
   * @throws IOException
   */
  static private boolean updateFilePartition(final boolean isGrib1, final FeatureCollectionConfig config,
                                             final CollectionUpdateType updateType, boolean isTop,
                                             final Logger logger, Path dirPath) throws IOException {
    long start = System.currentTimeMillis();
    final Formatter errlog = new Formatter();
    CollectionSpecParser specp = config.getCollectionSpecParser(errlog);

    try (FilePartition partition = new FilePartition(config.collectionName, dirPath, isTop, config.olderThan, logger)) {
      partition.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
      if (specp.getFilter() != null)
        partition.setStreamFilter(new StreamFilter(specp.getFilter(), specp.getFilterOnName()));

      if (debug) System.out.printf("GribCdmIndex.updateFilePartition %s %s%n", partition.getCollectionName(), updateType);
      if (!isUpdateNeeded(partition.getIndexFilename(), updateType, (isGrib1 ? GribCollectionType.Partition1 : GribCollectionType.Partition2), logger))
        return false;

      final AtomicBoolean anyChange = new AtomicBoolean(false); // just need a mutable boolean we can declare final

      // redo the children here
      if (updateType != CollectionUpdateType.testIndexOnly) {   // skip children on testIndexOnly
        partition.iterateOverMFileCollection(new DirectoryCollection.Visitor() {
          public void consume(MFile mfile) {
            MCollection part = new CollectionSingleFile(mfile, logger);
            part.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);

            try {
              boolean changed = updateGribCollection(isGrib1, part, updateType, FeatureCollectionConfig.PartitionType.file, logger, errlog);
              if (changed) anyChange.set(true);

            } catch (IllegalStateException t) {
              logger.warn("Error making partition {} '{}'", part.getRoot(), t.getMessage());
              partition.removePartition(part); // keep on truckin; can happen if directory is empty

            } catch (Throwable t) {
              logger.error("Error making partition " + part.getRoot(), t);
              partition.removePartition(part);
            }
          }
        });
      }

      // LOOK what if theres only one file?

      try {
        // redo partition index if needed, will detect if children have changed
        boolean recreated = updatePartition(isGrib1, partition, updateType, logger, errlog);

        long took = System.currentTimeMillis() - start;
        if (recreated) logger.info("RewriteFilePartition {} took {} msecs", partition.getCollectionName(), took);
        return recreated;

      } catch (IllegalStateException t) {
        logger.warn("Error making partition {} '{}'", partition.getRoot(), t.getMessage());
        return false;

      } catch (Throwable t) {
        logger.error("Error making partition " + partition.getRoot(), t);
        return false;
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////


  // DirectoryPartitionViewer
  static public boolean makeIndex(FeatureCollectionConfig config, Formatter errlog, Path topPath) throws IOException {
    return false;
    /* GribCdmIndex indexReader = new GribCdmIndex();
     MCollection dpart = DirectoryBuilder.factory(config, topPath, indexReader, logger);
     dpart.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);

     if (dpart.isLeaf()) {
       return Grib2TimePartitionBuilder.makePartitionIndex((PartitionManager) dpart, errlog, logger);

     } else {
       return Grib2CollectionBuilder.makeIndex(dpart, errlog, logger);
     }  */
  }

  // move index to be a directory partition
  static public boolean moveCdmIndex(String indexFilename, Logger logger) throws IOException {
    return false;
    /* RandomAccessFile raf = new RandomAccessFile(indexFilename, "r");
    RandomAccessFile newRaf = new RandomAccessFile(indexFilename + ".copy", "rw");

    try {
      raf.seek(0);
      byte[] b = new byte[Grib2CollectionBuilder.MAGIC_START.getBytes().length]; // they are all the same
      raf.read(b);
      String magic = new String(b, CDM.utf8Charset);

      GribCdmIndex gci = new GribCdmIndex();

      switch (magic) {
        case Grib1CollectionBuilder.MAGIC_START:
        case Grib2CollectionBuilder.MAGIC_START:
          return gci.moveIndex(raf, newRaf, logger);

        /* case Grib1CollectionBuilder.MAGIC_START:
          gc = Grib1CollectionBuilder.createFromIndex(indexFile, null, raf, null, logger);
          break;
        case Grib2TimePartitionBuilder.MAGIC_START:
          gc = Grib2TimePartitionBuilder.createFromIndex(indexFile, null, raf, logger);
          break;
        case Grib1TimePartitionBuilder.MAGIC_START:
          gc = Grib1TimePartitionBuilder.createFromIndex(indexFile, null, raf, logger);
          break; //
      }
    } catch (Throwable t) {
      raf.close();
      throw t;

    } finally {
      if (raf != null) raf.close();
      if (newRaf != null) newRaf.close();
    }

    return false;   */
  }

  /*
   * Directory Collection: the collection of all files in the directory is a DirectoryCollection.
   * Rewrite the DirectoryCollection in one Directory
   *
   * @param config       FeatureCollectionConfig
   * @param forceCollection       always, test, nocheck, never
   * @param forceChildren         always, test, nocheck, never
   * @throws IOException
   */
  /* static public boolean rewriteDirectoryCollection(final FeatureCollectionConfig config,
                                              final CollectionUpdateType forceCollection,
                                              final CollectionUpdateType forceChildren,
                                              final Logger logger) throws IOException {
    long start = System.currentTimeMillis();
    final Formatter errlog = new Formatter();

    int pos = config.spec.lastIndexOf("/");
    Path dirPath = Paths.get(config.spec.substring(0,pos));

    DirectoryCollection dirCollection = new DirectoryCollection(config.name, dirPath, logger);
    String collectionName = DirectoryCollection.makeCollectionName(config.name, dirPath);

    if (forceChildren != CollectionUpdateType.never) {
      dirCollection.iterateOverMFileCollection(new DirectoryCollection.Visitor() {
        public void consume(MFile mfile) {
          try (GribCollection gcNested =
                       Grib2CollectionBuilder.readOrCreateIndexFromSingleFile(mfile, forceChildren, config.gribConfig, errlog, logger)) {
          } catch (IOException e) {
            logger.error("rewriteIndexesFilesAndCollection", e);
          }
        }
      });
    }

    // redo partition index
    dirCollection.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);
    try (GribCollection gcNew = makeGribCollectionFromMCollection(false, dirCollection, forceCollection, errlog, logger)) {
    }

    long took = System.currentTimeMillis() - start;
    logger.info("RewriteDirectoryPartition {} took {} msecs", collectionName, took);
    return true;
  }  */

  ///////////////////////////////////////////////////////
  // used by InvDatasetFcGrib

  /**
   * Open GribCollection from config.
   * CollectionUpdater calls InvDatasetFc.update() calls InvDatasetFcGrib.updateCollection()
   */
  static public GribCollectionImmutable openGribCollection(FeatureCollectionConfig config, CollectionUpdateType updateType, Logger logger) throws IOException {

    // update if needed
    boolean changed = updateGribCollection(config, updateType, logger);

    File idxFile = makeTopIndexFileFromConfig(config);

    // If call to updateGribCollection shows a change happened, then collection changed.
    // If updateType is never (tds is in charge of updating, not TDM or some other external application),
    // then this is being called after receiving an outside trigger. Assume collection changed.
    //
    // At this point, there isn't a good way of invalidating the gribColectionCache entries associated with the
    // particular collection being updated, so we have to clear the whole cache. Will revisit this in
    // 5.0 if performance is an issue
    if ((updateType == CollectionUpdateType.never) || changed) {
      gribCollectionCache.clearCache(true);
    }

    return openCdmIndex(idxFile.getPath(), config, true, logger);
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // Used by IOSPs

  public static GribCollectionImmutable openGribCollectionFromRaf(RandomAccessFile raf,
                                                                  FeatureCollectionConfig config, CollectionUpdateType updateType, org.slf4j.Logger logger) throws IOException {

    GribCollectionImmutable result;

    // check if its a plain ole GRIB1/2 data file
    boolean isGrib1 = false;
    boolean isGrib2 = Grib2RecordScanner.isValidFile(raf);
    if (!isGrib2) isGrib1 = Grib1RecordScanner.isValidFile(raf);

    if (isGrib1 || isGrib2) {

      result = openGribCollectionFromDataFile(isGrib1, raf, config, updateType, null, logger);
      // close the data file, the ncx2 raf file is managed by gribCollection
      raf.close();

    } else {  // check its an ncx2 file
      result = openGribCollectionFromIndexFile(raf, config, logger);
    }

    return result;
  }


  /**
   * Open a grib collection from a single grib1 or grib2 file.
   * Create the gbx9 and ncx2 files if needed.
   *
   * @param isGrib1    true if grib1
   * @param dataRaf    the data file already open
   * @param config     special configuration
   * @param updateType force writing index
   * @return the resulting GribCollection
   * @throws IOException on io error
   */
  private static GribCollectionImmutable openGribCollectionFromDataFile(boolean isGrib1, RandomAccessFile dataRaf, FeatureCollectionConfig config,
                                                                        CollectionUpdateType updateType, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    String filename = dataRaf.getLocation();
    File dataFile = new File(filename);

    MFile mfile = new MFileOS(dataFile);
    return openGribCollectionFromDataFile(isGrib1, mfile, updateType, config, errlog, logger);
  }

  // for InvDatasetFeatureCollection.getNetcdfDataset() and getGridDataset()

  // from a single file, read in the index, create if it doesnt exist; return null on failure
  static public GribCollectionImmutable openGribCollectionFromDataFile(boolean isGrib1, MFile mfile, CollectionUpdateType updateType,
                                                                       FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    MCollection dcm = new CollectionSingleFile(mfile, logger);
    dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
    if (isGrib1) {
      Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm.getCollectionName(), dcm, logger);       // LOOK ignoring partition type
      boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(FeatureCollectionConfig.PartitionType.file, errlog));
    } else {
      Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
      boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(FeatureCollectionConfig.PartitionType.file, errlog));
    }

    // the index file should now exist, open it
    GribCollectionImmutable result = openCdmIndex(dcm.getIndexFilename(), config, true, logger);
    if (result != null) return result;

    // if open fails, force recreate the index
    if (updateType == CollectionUpdateType.never) return null; // not allowed to write
    if (updateType == CollectionUpdateType.always) return null;// already tried to force write, give up
    return openGribCollectionFromDataFile(isGrib1, mfile, CollectionUpdateType.always, config, errlog, logger);
  }

  /**
   * Create a grib collection / partition collection from an existing ncx2 file.
   * PartionCollection.partition.getGribCollection().
   *
   * @param indexRaf the ncx2 file already open
   * @param config   special configuration
   * @return the resulting GribCollection, or null on failure
   * @throws IOException on io error
   */
  public static GribCollectionImmutable openGribCollectionFromIndexFile(RandomAccessFile indexRaf, FeatureCollectionConfig config,
                                                                        org.slf4j.Logger logger) throws IOException {

    GribCollectionType type = getType(indexRaf);

    String location = indexRaf.getLocation();
    File f = new File(location);
    int pos = f.getName().lastIndexOf(".");
    String name = (pos > 0) ? f.getName().substring(0, pos) : f.getName(); // remove ".ncx2"

    switch (type) {
      case Partition1:
        return Grib1PartitionBuilderFromIndex.createTimePartitionFromIndex(name, indexRaf, config, logger);
      case GRIB1:
        return Grib1CollectionBuilderFromIndex.readFromIndex(name, indexRaf, config, logger);
      case Partition2:
        return Grib2PartitionBuilderFromIndex.createTimePartitionFromIndex(name, indexRaf, config, logger);
      case GRIB2:
        return Grib2CollectionBuilderFromIndex.readFromIndex(name, indexRaf, config, logger);
    }

    return null;
  }


  /////////////////////////////////////////////////////////////////////////////////////
  // manipulate the ncx without building a gc
  private static final boolean debug = false;
  private byte[] magic;
  private int version;
  private GribCollectionProto.GribCollection gribCollectionIndex;
  private final Logger logger;

  public GribCdmIndex(Logger logger) {
    this.logger = logger;
  }

  /// IndexReader interface
  @Override
  public boolean readChildren(Path indexFile, AddChildCallback callback) throws IOException {
    if (debug) System.out.printf("GribCdmIndex.readChildren %s%n", indexFile);
    try (RandomAccessFile raf = RandomAccessFile.acquire(indexFile.toString())) {
      GribCollectionType type = getType(raf);
      if (type == GribCollectionType.Partition1 || type == GribCollectionType.Partition2) {
        if (openIndex(raf, logger)) {
          String topDir = gribCollectionIndex.getTopDir();
          int n = gribCollectionIndex.getMfilesCount(); // partition index files stored in MFiles
          for (int i = 0; i < n; i++) {
            GribCollectionProto.MFile mfilep = gribCollectionIndex.getMfiles(i);
            callback.addChild(topDir, mfilep.getFilename(), mfilep.getLastModified());
          }
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public boolean isPartition(Path indexFile) throws IOException {
    if (debug) System.out.printf("GribCdmIndex.isPartition %s%n", indexFile);
    try (RandomAccessFile raf = RandomAccessFile.acquire(indexFile.toString())) {
      GribCollectionType type = getType(raf);
      return (type == GribCollectionType.Partition1) || (type == GribCollectionType.Partition2);
    }
  }

  @Override
  public boolean readMFiles(Path indexFile, List<MFile> result) throws IOException {
    if (debug) System.out.printf("GribCdmIndex.readMFiles %s%n", indexFile);
    try (RandomAccessFile raf = RandomAccessFile.acquire(indexFile.toString())) {
      // GribCollectionType type = getType(raf);
      // if (type == GribCollectionType.GRIB1 || type == GribCollectionType.GRIB2) {
      if (openIndex(raf, logger)) {
        File protoDir = new File(gribCollectionIndex.getTopDir());
        int n = gribCollectionIndex.getMfilesCount();
        for (int i = 0; i < n; i++) {
          GribCollectionProto.MFile mfilep = gribCollectionIndex.getMfiles(i);
          result.add(new GcMFile(protoDir, mfilep.getFilename(), mfilep.getLastModified(), mfilep.getLength(), mfilep.getIndex()));
        }
      }
      return true;
      //}
    }
    //return false;
  }

  private boolean openIndex(RandomAccessFile indexRaf, Logger logger) {
    try {
      indexRaf.order(RandomAccessFile.BIG_ENDIAN);
      indexRaf.seek(0);

      //// header message
      magic = new byte[Grib2CollectionWriter.MAGIC_START.getBytes(CDM.utf8Charset).length];   // they are all the same
      indexRaf.readFully(magic);

      version = indexRaf.readInt();

      long recordLength = indexRaf.readLong();
      if (recordLength > Integer.MAX_VALUE) {
        logger.error("Grib2Collection {}: invalid recordLength size {}", indexRaf.getLocation(), recordLength);
        return false;
      }
      indexRaf.skipBytes(recordLength);

      int size = NcStream.readVInt(indexRaf);
      if ((size < 0) || (size > 100 * 1000 * 1000)) {
        logger.warn("GribCdmIndex {}: invalid index size {}", indexRaf.getLocation(), size);
        return false;
      }

      byte[] m = new byte[size];
      indexRaf.readFully(m);
      gribCollectionIndex = GribCollectionProto.GribCollection.parseFrom(m);
      return true;

    } catch (Throwable t) {
      logger.error("Error reading index " + indexRaf.getLocation(), t);
      return false;
    }
  }


  public static void main2(String[] args) throws IOException {
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    PartitionManager partition = new PartitionManagerFromIndexDirectory("NCDC-gfs4_all", new FeatureCollectionConfig(), new File("B:/ncdc/gfs4_all/"), logger);
    Grib1PartitionBuilder builder = new Grib1PartitionBuilder("NCDC-gfs4_all", new File(partition.getRoot()), partition, logger);
    builder.createPartitionedIndex(CollectionUpdateType.nocheck, new Formatter());
  }

  public static void main3(String[] args) throws IOException {
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");

    /*
       <featureCollection name="DGEX-CONUS_12km" featureType="GRIB" harvest="true" path="grib/NCEP/DGEX/CONUS_12km">
                  <collection spec="F:/data/grib/idd/dgex/  /.*grib2$"
                    dateFormatMark="#DGEX_CONUS_12km_#yyyyMMdd_HHmm"
                    timePartition="directory"
                    olderThan="5 min"/>
     */

    // String name, String path, FeatureCollectionType fcType, String spec, String dateFormatMark, String olderThan, String timePartition, String useIndexOnlyS, Element innerNcml
    //FeatureCollectionConfig config = new FeatureCollectionConfig("DGEX-test", "grib/NCEP/DGEX/CONUS_12km", FeatureCollectionType.GRIB2,
    //        "Q:/cdmUnitTest/gribCollections/dgex/**/.*grib2$", "#DGEX_CONUS_12km_#yyyyMMdd_HHmm", null, "directory", null, null);

    //  FeatureCollectionConfig config = new FeatureCollectionConfig("GFS_CONUS_80km", "grib/NCEP/GFS/CONUS_80km", FeatureCollectionType.GRIB1,
    //          "Q:/cdmUnitTest/ncss/GFS/CONUS_80km/GFS_CONUS_80km_#yyyyMMdd_HHmm#.grib1", null, null, "file", null, null);

    //FeatureCollectionConfig config = new FeatureCollectionConfig("ds083.2_Aggregation", "ds083.2/Aggregation", FeatureCollectionType.GRIB1,
    //         "Q:/cdmUnitTest/gribCollections/rdavm/ds083.2/grib1/**/.*grib1", "#fnl_#yyyyMMdd_HH_mm", null, "directory", null, null);
    /*
    <pdsHash>
       <useTableVersion>false</useTableVersion>
     </pdsHash>
     */

    FeatureCollectionConfig config = new FeatureCollectionConfig("RFC", "grib/NPVU/RFC", FeatureCollectionType.GRIB1,
            "B:/motherlode/rfc/**/.*grib1$", null, "yyyyMMdd#.grib1#", null, "directory", null);

    // config.gribConfig.pdsHash.put("useTableVersion", false);

    // boolean isGrib1, MCollection dcm, CollectionUpdateType updateType, Formatter errlog, org.slf4j.Logger logger
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.test, logger);
    System.out.printf("changed = %s%n", changed);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////

  private static class CommandLine {
    @Parameter(names = {"-fc", "--featureCollection"}, description = "Input XML file containing <featureCollection> root element", required = true)
    public File inputFile;

    @Parameter(names = {"-update", "--CollectionUpdateType"}, description = "Collection Update Type")
    public CollectionUpdateType updateType = CollectionUpdateType.always;

    @Parameter(names = {"-h", "--help"}, description = "Display this help and exit", help = true)
    public boolean help = false;

    public class CollectionUpdateTypeConverter implements IStringConverter<CollectionUpdateType> {
      @Override
      public CollectionUpdateType convert(String value) {
        return CollectionUpdateType.valueOf(value);
      }
    }

    private static class ParameterDescriptionComparator implements Comparator<ParameterDescription> {
      // Display parameters in this order in the usage information.
      private final List<String> orderedParamNames = Arrays.asList("--featureCollection", "--CollectionUpdateType", "--help");

      @Override
      public int compare(ParameterDescription p0, ParameterDescription p1) {
        int index0 = orderedParamNames.indexOf(p0.getLongestName());
        int index1 = orderedParamNames.indexOf(p1.getLongestName());
        assert index0 >= 0 : "Unexpected parameter name: " + p0.getLongestName();
        assert index1 >= 0 : "Unexpected parameter name: " + p1.getLongestName();

        return Integer.compare(index0, index1);
      }
    }

    private final JCommander jc;

    public CommandLine(String progName, String[] args) throws ParameterException {
      this.jc = new JCommander(this, args);  // Parses args and uses them to initialize *this*.
      jc.setProgramName(progName);           // Displayed in the usage information.

      // Set the ordering of parameters in the usage information.
      jc.setParameterDescriptionComparator(new ParameterDescriptionComparator());
    }

    public void printUsage() {
      jc.usage();
    }
  }

  public static void main(String[] args) throws Exception {
    String progName = GribCdmIndex.class.getName();

    try {
      CommandLine cmdLine = new CommandLine(progName, args);

      if (cmdLine.help) {
        cmdLine.printUsage();
        return;
      }

      Formatter errlog = new Formatter();
      try {
        FeatureCollectionConfigBuilder reader = new FeatureCollectionConfigBuilder(errlog);
        String location = cmdLine.inputFile.getAbsolutePath();
        FeatureCollectionConfig config = reader.readConfigFromFile(location);
        boolean changed = GribCdmIndex.updateGribCollection(config, cmdLine.updateType, classLogger);
        System.out.printf("changed = %s%n", changed);

      } catch (Exception e) {
        System.out.printf("%s = %s %n", e.getClass().getName(), e.getMessage());
        String err = errlog.toString();
        if (err.length() > 0)
          System.out.printf(" errlog=%s%n", err);
        // e.printStackTrace();
      }

    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.err.printf("Try \"%s --help\" for more information.%n", progName);
    }
  }

}
