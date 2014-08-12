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

import org.slf4j.Logger;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.filesystem.MFileOS;
import thredds.inventory.*;
import thredds.inventory.filter.StreamFilter;
import thredds.inventory.partition.*;
import ucar.nc2.grib.grib1.Grib1RecordScanner;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;
import java.util.List;

/**
 * Utilities for creating GRIB ncx2 files, both collections and partitions
 *
 * @author John
 * @since 12/5/13
 */
public class GribCdmIndex implements IndexReader {
  static public enum GribCollectionType {GRIB1, GRIB2, Partition1, Partition2, none}

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
    byte[] b = new byte[Grib2CollectionWriter.MAGIC_START.getBytes().length];   // they are all the same
    raf.readFully(b);
    magic = new String(b);

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

    // open GribCollection from an existing index file. caller must close; return null on failure
  static public GribCollection openCdmIndex(String indexFilename, FeatureCollectionConfig config, boolean dataOnly, Logger logger) {
    return openCdmIndex(indexFilename, config, dataOnly, true, logger);
  }

    // open GribCollection from an existing index file. caller must close; return null on failure
  static public GribCollection openCdmIndex(String indexFilename, FeatureCollectionConfig config, boolean dataOnly, boolean useCache, Logger logger) {
    File indexFileInCache = useCache ? GribCollection.getFileInCache(indexFilename) : new File(indexFilename);
    String indexFilenameInCache = indexFileInCache.getPath();
    String name = GribCollection.makeNameFromIndexFilename(indexFilename);
    RandomAccessFile raf = null;
    GribCollection result = null;

    try {
      raf = new RandomAccessFile(indexFilenameInCache, "r");
      GribCollectionType type = getType(raf);

      switch (type) {
        case GRIB2:
          result = Grib2CollectionBuilderFromIndex.readFromIndex(name, raf, config, dataOnly, logger);
          break;
        case Partition2:
          result = Grib2PartitionBuilderFromIndex.createTimePartitionFromIndex(name, raf, config, dataOnly, logger);
          break;
        case GRIB1:
          result = Grib1CollectionBuilderFromIndex.readFromIndex(name, raf, config, dataOnly, logger);
          break;
        case Partition1:
          result = Grib1PartitionBuilderFromIndex.createTimePartitionFromIndex(name, raf, config, dataOnly, logger);
          break;
        default:
          logger.warn("GribCdmIndex.openCdmIndex failed on {} type={}", indexFilenameInCache, type);
      }

    } catch (Throwable t) {
      logger.warn("GribCdmIndex.openCdmIndex failed on "+indexFilenameInCache, t);
      if (raf != null) try {
        raf.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // clean up on failure
    if (result == null && raf != null) {
      try { raf.close(); } catch (IOException ioe) {}
    }

    return result;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // used by PartitionCollection

  /**
    * Create a GribCollection from an MCollection.
    * full metadata is read in.
    *
    * @param isGrib1 true if files are grib1, else grib2
    * @param dcm     the MCollection : files or other collections
    * @param updateType   should index file be used or remade?
    * @return GribCollection
    * @throws IOException on io error
    */
  static public GribCollection openGribCollectionFromMCollection(boolean isGrib1, MCollection dcm, CollectionUpdateType updateType,
                                                                 Formatter errlog, org.slf4j.Logger logger) throws IOException {

    FeatureCollectionConfig config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);

    if (updateType == CollectionUpdateType.never || dcm instanceof CollectionSingleIndexFile) { // would isIndexFile() be better ?
      return openCdmIndex(dcm.getIndexFilename(), config, false, logger);  // then just open the existing index file
    }

    // update if needed
    boolean changed = updateGribCollectionFromMCollection(isGrib1, dcm, updateType, errlog, logger);

    // now open the index
    return openCdmIndex(dcm.getIndexFilename(), config, false, logger);
  }


  static public boolean updateGribCollectionFromMCollection(boolean isGrib1, MCollection dcm, CollectionUpdateType updateType,
                                                                 Formatter errlog, org.slf4j.Logger logger) throws IOException {

    //FeatureCollectionConfig config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);

    if (updateType == CollectionUpdateType.never || dcm instanceof CollectionSingleIndexFile) { // would isIndexFile() be better ?
      // then just open the existing index file
      return false;
    }

    boolean changed;
    // otherwise got to check
    if (isGrib1) {
      if (dcm.isLeaf()) {
        Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm.getCollectionName(), dcm, logger);
        changed = builder.updateNeeded(updateType) && builder.createIndex(errlog);

      } else {
        Grib1PartitionBuilder builder = new Grib1PartitionBuilder(dcm.getCollectionName(), new File(dcm.getRoot()), (PartitionManager) dcm, logger);
        changed = builder.updateNeeded(updateType) && builder.createPartitionedIndex(updateType, updateType, errlog);
      }

    } else {
      if (dcm.isLeaf()) {
        Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
        changed = builder.updateNeeded(updateType) && builder.createIndex(errlog);

      } else {
        Grib2PartitionBuilder builder = new Grib2PartitionBuilder(dcm.getCollectionName(), new File(dcm.getRoot()), (PartitionManager) dcm, logger);
        changed = builder.updateNeeded(updateType) && builder.createPartitionedIndex(updateType, updateType, errlog);
      }
    }
    if (errlog != null) errlog.format("GribCollection %s was recreated %s%n", dcm.getCollectionName(), changed);

    // now open the index
    return changed;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // used by Tdm

  /**
   * update the collection if needed
   * @return true if the collection was updated
   * @throws IOException
   */
  static public boolean updateGribCollection(FeatureCollectionConfig config,
                                             CollectionUpdateType updateType,
                                             Logger logger) throws IOException {

    long start = System.currentTimeMillis();

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);
    Path rootPath = Paths.get(specp.getRootDir());
    boolean isGrib1 = config.type == FeatureCollectionType.GRIB1;

    boolean changed;

    if (config.ptype == FeatureCollectionConfig.PartitionType.none) {

      CollectionAbstract dcm = specp.wantSubdirs() ? new CollectionGeneral(config.name, rootPath, logger) : new DirectoryCollection(config.name, rootPath, logger);
      dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);

      if (specp.getFilter() != null)
        dcm.setStreamFilter(new StreamFilter(specp.getFilter()));

      if (isGrib1) {
        Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm.getCollectionName(), dcm, logger);
        changed = builder.updateNeeded(updateType) && builder.createIndex(errlog);
      } else {
        Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
        changed = builder.updateNeeded(updateType) && builder.createIndex(errlog);
      }

    } else {

      if (specp.wantSubdirs()) {
        // its a partition
        DirectoryPartition dpart = new DirectoryPartition(config, rootPath, new GribCdmIndex(logger), logger);
        dpart.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
        changed = updateDirectoryCollectionRecurse(isGrib1, dpart, config, updateType, logger);

      } else {

        // otherwise its a leaf directory
        changed = updateLeafCollection(isGrib1, config, updateType, logger, rootPath);
      }
    }

    long took = System.currentTimeMillis() - start;
    logger.info("updateGribCollection {} changed {} took {} msecs", config.name, changed, took);
    return changed;
  }

  static private boolean updateDirectoryCollectionRecurse(boolean isGrib1, DirectoryPartition dpart,
                                                          FeatureCollectionConfig config,
                                                          CollectionUpdateType forceCollection,
                                                          Logger logger) throws IOException {

    if (debug) System.out.printf("GribCdmIndex.updateDirectoryCollectionRecurse %s %s%n", dpart.getRoot(), forceCollection);

    if (forceCollection == CollectionUpdateType.never) return false;  // dont do nothin

    // if (forceCollection != CollectionUpdateType.always) {
      Path idxFile = dpart.getIndexPath();
      if (Files.exists(idxFile)) {
        if (forceCollection == CollectionUpdateType.nocheck) return false;  // use if index exists

        // otherwise read it to verify its a PC
        try (RandomAccessFile raf = new RandomAccessFile(idxFile.toString(), "r")) {
          GribCollectionType type = getType(raf);
          assert type == (isGrib1 ? GribCollectionType.Partition1 : GribCollectionType.Partition2);
        }
      }
    // }

    // index does not yet exist, or we want to test if it changed
    // must scan it
    for (MCollection part : dpart.makePartitions(forceCollection)) {
      part.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
      try {
        if (part.isLeaf()) {
          Path partPath = Paths.get(part.getRoot());
          updateLeafCollection(isGrib1, config, forceCollection, logger, partPath);

        } else {
          updateDirectoryCollectionRecurse(isGrib1, (DirectoryPartition) part, config, forceCollection, logger);
        }
      } catch (Throwable t) {
        logger.warn("Error making partition "+part.getRoot(), t);
        continue; // keep on truckin; can happen if directory is empty
      }
    }   // loop over partitions

    // do this partition; we just did children so never update them
    boolean changed;
    Formatter errlog = new Formatter();
    if (isGrib1) {
      Grib1PartitionBuilder builder = new Grib1PartitionBuilder(dpart.getCollectionName(), new File(dpart.getRoot()), dpart, logger);
      changed = builder.updateNeeded(forceCollection) && builder.createPartitionedIndex(forceCollection, CollectionUpdateType.never, errlog);

    } else {
      Grib2PartitionBuilder builder = new Grib2PartitionBuilder(dpart.getCollectionName(), new File(dpart.getRoot()), dpart, logger);
      changed = builder.updateNeeded(forceCollection) && builder.createPartitionedIndex(forceCollection, CollectionUpdateType.never, errlog);
    }

    if (debug) System.out.printf("GribCdmIndex.updateDirectoryCollectionRecurse complete (%s) on %s errlog=%s%n", changed, dpart.getRoot(), errlog);
    return changed;
  }

  /**
   * Update all the grib indices in one directory, and the collection index for that directory
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @throws IOException
   */
  static private boolean updateLeafCollection(boolean isGrib1, FeatureCollectionConfig config,
                                              CollectionUpdateType forceCollection,
                                              Logger logger, Path dirPath) throws IOException {

    if (config.ptype == FeatureCollectionConfig.PartitionType.file) {
      return updateFilePartition(isGrib1, config, forceCollection, logger, dirPath);

    } else {
      return updateLeafDirectoryCollection(isGrib1, config, forceCollection, logger, dirPath);
    }

  }

  /**
   * Update all the grib indices in one directory, and the collection index for that directory
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @throws IOException
   */
  static private boolean updateLeafDirectoryCollection(boolean isGrib1, FeatureCollectionConfig config,
                                                       CollectionUpdateType forceCollection,
                                                       Logger logger, Path dirPath) throws IOException {

    if (forceCollection == CollectionUpdateType.never) return false;  // dont do nothin

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);

    DirectoryCollection dcm = new DirectoryCollection(config.name, dirPath, logger);
    dcm.setLeaf(true);
    dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
    if (specp.getFilter() != null)
      dcm.setStreamFilter(new StreamFilter(specp.getFilter()));

    Path idxFile = dcm.getIndexPath();
    if (Files.exists(idxFile)) {
      if (forceCollection == CollectionUpdateType.nocheck) { // use if index exists
        if (debug) System.out.printf("  GribCdmIndex.updateLeafDirectoryCollection %s use existing index%n", dirPath);
        return false;
      }
    }

    boolean changed;
    if (isGrib1) {
      Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm.getCollectionName(), dcm, logger);
      changed = builder.updateNeeded(forceCollection) && builder.createIndex(errlog);
    } else {
      Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
      changed = builder.updateNeeded(forceCollection) && builder.createIndex(errlog);
    }

    if (debug) System.out.printf("  GribCdmIndex.updateLeafDirectoryCollection was updated=%s on %s%n", changed, dirPath);
    return changed;
  }

  /**
   * File Partition: each File is a collection of Grib records, and the collection of all files in the directory is a PartitionCollection.
   * Rewrite the PartitionCollection and optionally its children
   *
   * @param config              FeatureCollectionConfig
   * @param updateType          always, test, nocheck, never
   * @return true if partition was rewritten
   * @throws IOException
   */
  static private boolean updateFilePartition(final boolean isGrib1, final FeatureCollectionConfig config,
                                             final CollectionUpdateType updateType,
                                             final Logger logger, Path dirPath) throws IOException {
    long start = System.currentTimeMillis();

    final Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);

    FilePartition partition = new FilePartition(config.name, dirPath, logger);
    partition.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
    if (specp.getFilter() != null)
      partition.setStreamFilter(new StreamFilter(specp.getFilter()));

    // final AtomicBoolean anyChange = new AtomicBoolean(false); // just need a mutable boolean

    // redo the child collection here; could also do inside Grib2PartitionBuilder, not sure if advantage
    if (updateType != CollectionUpdateType.never) {
      partition.iterateOverMFileCollection(new DirectoryCollection.Visitor() {
        public void consume(MFile mfile) {
          MCollection dcm = new CollectionSingleFile(mfile, logger);
          dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);

          if (isGrib1) {
            Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm.getCollectionName(), dcm, logger);
            try {
              boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(errlog));
            } catch (IOException e) { e.printStackTrace(); }

          } else {
            Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
            try {
              boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(errlog));
            } catch (IOException e) { e.printStackTrace(); }

          }
        }
      });
    }

    // redo partition index if needed, will detect if children have changed
    boolean recreated;
    if (isGrib1) {
      Grib1PartitionBuilder builder = new Grib1PartitionBuilder(partition.getCollectionName(), new File(partition.getRoot()), partition, logger);
      recreated = builder.updateNeeded(updateType) && builder.createPartitionedIndex(updateType, CollectionUpdateType.never, errlog);
    } else {
      Grib2PartitionBuilder builder = new Grib2PartitionBuilder(partition.getCollectionName(), new File(partition.getRoot()), partition, logger);
      recreated = builder.updateNeeded(updateType) && builder.createPartitionedIndex(updateType, CollectionUpdateType.never, errlog);
    }

    long took = System.currentTimeMillis() - start;
    String collectionName = partition.getCollectionName();
    if (recreated) logger.info("RewriteFilePartition {} took {} msecs", collectionName, took);

    return recreated;
  }


  ///////////
  // used by InvDatasetFcGrib

  /**
   * Open GribCollection from config.
   * dataOnly
   */
  static public GribCollection openGribCollection(FeatureCollectionConfig config, CollectionUpdateType updateType, Logger logger) throws IOException {

    // update if needed
    updateGribCollection(config, updateType, logger);

    File idxFile = GribCollection.makeTopIndexFileFromConfig(config);
    return openCdmIndex(idxFile.getPath(), config, true, logger);
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

  ////////////////////////////////////////////////////////////////////////////////////
  // Used by IOSPs; dataOnly

  public static GribCollection makeGribCollectionFromRaf(RandomAccessFile raf,
            FeatureCollectionConfig config, CollectionUpdateType updateType, org.slf4j.Logger logger) throws IOException {

    GribCollection result;

      // check if its a plain ole GRIB1/2 data file
    boolean isGrib1 = false;
    boolean isGrib2 = Grib2RecordScanner.isValidFile(raf);
    if (!isGrib2) isGrib1 = Grib1RecordScanner.isValidFile(raf);

    if (isGrib1 || isGrib2) {

      result = openGribCollectionFromDataFile(isGrib1, raf, config, updateType, null, logger);
      // close the data file, the ncx2 raf file is managed by gribCollection
      raf.close();

    } else {  // check its an ncx2 file
      result = openGribCollectionFromIndexFile(raf, config, true, logger);
    }

    return result;
}


  /**
   * Open a grib collection from a single grib1 or grib2 file.
   * Create the gbx9 and ncx2 files if needed.
   * dataOnly
   *
   * @param isGrib1 true if grib1
   * @param dataRaf the data file already open
   * @param config  special configuration
   * @param updateType   force writing index
   * @return the resulting GribCollection
   * @throws IOException on io error
   */
  private static GribCollection openGribCollectionFromDataFile(boolean isGrib1, RandomAccessFile dataRaf, FeatureCollectionConfig config,
            CollectionUpdateType updateType, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    String filename = dataRaf.getLocation();
    File dataFile = new File(filename);

    MFile mfile = new MFileOS(dataFile);
    return openGribCollectionFromDataFile(isGrib1, mfile, updateType, config, errlog, logger);
  }

    // for InvDatasetFeatureCollection.getNetcdfDataset() and getGridDataset()

  // from a single file, read in the index, create if it doesnt exist; return null on failure
  static public GribCollection openGribCollectionFromDataFile(boolean isGrib1, MFile mfile, CollectionUpdateType updateType,
                   FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    MCollection dcm = new CollectionSingleFile(mfile, logger);
    dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
    if (isGrib1) {
      Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm.getCollectionName(), dcm, logger);
      boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(errlog));
    } else {
      Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
      boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(errlog));
    }

    // the index file should now exist, open it
    GribCollection result = openCdmIndex( dcm.getIndexFilename(), config, true, logger);
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
   * @param config  special configuration
   * @return the resulting GribCollection, or null on failure
   * @throws IOException on io error
   */
  public static GribCollection openGribCollectionFromIndexFile(RandomAccessFile indexRaf, FeatureCollectionConfig config,
                                                               boolean dataOnly, org.slf4j.Logger logger) throws IOException {

    GribCollectionType type = getType(indexRaf);

    String location = indexRaf.getLocation();
    File f = new File(location);
    int pos = f.getName().lastIndexOf(".");
    String name = (pos > 0) ? f.getName().substring(0, pos) : f.getName(); // remove ".ncx2"

    switch (type) {
      case Partition1 :
         return Grib1PartitionBuilderFromIndex.createTimePartitionFromIndex(name, indexRaf, config, dataOnly, logger);
      case GRIB1 :
        return Grib1CollectionBuilderFromIndex.readFromIndex(name, indexRaf, config, dataOnly, logger);
      case Partition2 :
         return Grib2PartitionBuilderFromIndex.createTimePartitionFromIndex(name, indexRaf, config, dataOnly, logger);
      case GRIB2 :
        return Grib2CollectionBuilderFromIndex.readFromIndex(name, indexRaf, config, dataOnly, logger);
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
    try (RandomAccessFile raf = new RandomAccessFile(indexFile.toString(), "r")) {
      GribCollectionType type = getType(raf);
      if (type == GribCollectionType.Partition1 || type == GribCollectionType.Partition2) {
        if (openIndex(raf, logger)) {
          String dirName = gribCollectionIndex.getTopDir();
          int n = gribCollectionIndex.getMfilesCount(); // partition index files stored in MFiles
          for (int i = 0; i < n; i++) {
            GribCollectionProto.MFile mfilep = gribCollectionIndex.getMfiles(i);
            callback.addChild(dirName, mfilep.getFilename(), mfilep.getLastModified());
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
    try (RandomAccessFile raf = new RandomAccessFile(indexFile.toString(), "r")) {
      GribCollectionType type = getType(raf);
      return (type == GribCollectionType.Partition1) || (type == GribCollectionType.Partition2);
    }
  }

  @Override
  public boolean readMFiles(Path indexFile, List<MFile> result) throws IOException {
    if (debug) System.out.printf("GribCdmIndex.readMFiles %s%n", indexFile);
    try (RandomAccessFile raf = new RandomAccessFile(indexFile.toString(), "r")) {
      GribCollectionType type = getType(raf);
      if (type == GribCollectionType.GRIB1 || type == GribCollectionType.GRIB2) {
        if (openIndex(raf, logger)) {
          File protoDir = new File(gribCollectionIndex.getTopDir());
          int n = gribCollectionIndex.getMfilesCount();
          for (int i = 0; i < n; i++) {
            GribCollectionProto.MFile mfilep = gribCollectionIndex.getMfiles(i);
            result.add(new GcMFile(protoDir, mfilep.getFilename(), mfilep.getLastModified(), mfilep.getIndex()));
          }
        }
        return true;
      }
    }
    return false;
  }

  private boolean openIndex(RandomAccessFile indexRaf, Logger logger) {
    try {
      indexRaf.order(RandomAccessFile.BIG_ENDIAN);
      indexRaf.seek(0);

      //// header message
      magic = new byte[Grib2CollectionWriter.MAGIC_START.getBytes().length];   // they are all the same
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

  public static void main(String[] args) throws IOException {
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    PartitionManager partition = new PartitionManagerFromIndexDirectory("NAM-Polar_90km", new FeatureCollectionConfig(), new File("B:/lead/NAM-Polar_90km/"),  logger);
    Grib1PartitionBuilder builder = new Grib1PartitionBuilder("NAM_Polar_90km", new File(partition.getRoot()), partition, logger);
    builder.createPartitionedIndex(CollectionUpdateType.nocheck, CollectionUpdateType.never, new Formatter());
  }

}
