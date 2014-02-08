package ucar.nc2.grib.collection;

import org.slf4j.Logger;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.filesystem.MFileOS;
import thredds.inventory.*;
import thredds.inventory.filter.StreamFilter;
import thredds.inventory.partition.*;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.Grib1Index;
import ucar.nc2.grib.grib2.Grib2Index;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utilities for creating GRIB ncx2 files, both collections and partitions
 * GRIB2 only at the moment
 *
 * @author John
 * @since 12/5/13
 */
public class GribCdmIndex2 implements IndexReader {
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
    raf.read(b);
    magic = new String(b);

    switch (magic) {
      case Grib2CollectionWriter.MAGIC_START:
        return GribCollectionType.GRIB2;

      //case Grib1CollectionBuilder.MAGIC_START:
      //  return GribCollectionType.GRIB1;

      case Grib2PartitionBuilder.MAGIC_START:
        return GribCollectionType.Partition2;

      //case Grib1TimePartitionBuilder.MAGIC_START:
      //  return GribCollectionType.Partition1;

    }
    return GribCollectionType.none;
  }

    // open GribCollection from an existing index file. caller must close
  static public GribCollection openCdmIndex(String indexFile, FeatureCollectionConfig.GribConfig config, Logger logger) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(indexFile, "r");

    File f = new File(indexFile);
    int pos = f.getName().lastIndexOf(".");
    String name = (pos > 0) ? f.getName().substring(0, pos) : f.getName(); // remove ".ncx2"

    try {
      GribCollectionType type = getType(raf);

      switch (type) {
        case GRIB2:
          return Grib2CollectionBuilderFromIndex.readFromIndex(name, f.getParentFile(), raf, config, logger);
        case Partition2:
          return Grib2PartitionBuilderFromIndex.createTimePartitionFromIndex(name, f.getParentFile(), raf, config, logger);
      }

      return null;

    } catch (Throwable t) {
      raf.close();
      throw t;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /* static public MCollection makeCollection(FeatureCollectionConfig config, Formatter errlog, Logger logger) throws IOException {

    CollectionSpecParser sp = new CollectionSpecParser(config.spec, errlog);
    String rootDir = sp.getRootDir();

    MCollection result = null;
    if (config.ptype == FeatureCollectionConfig.PartitionType.file) {
      result = new FilePartition(config.name, Paths.get(rootDir), logger);

    } else if (config.ptype == FeatureCollectionConfig.PartitionType.directory) {
      result =  new DirectoryPartition(config, Paths.get(rootDir), new GribCdmIndex2(logger), logger);

    } else {
      result = new MFileCollectionManager(config, errlog, logger);
    }

    return result;
  }

  /*
   * Create a GribCollection from a collection of grib files, or a TimePartition from a collection of GribCollection index files
   *
   * @param isGrib1 true if files are grib1, else grib2
   * @param config  configuration
   * @param force   should index file be used or remade?
   * @return GribCollection
   * @throws IOException on io error
   */


  /* static public GribCollection makeGribCollection(boolean isGrib1, FeatureCollectionConfig config, CollectionUpdateType force,
                                org.slf4j.Logger logger) throws IOException {

    Formatter errlog = new Formatter();
    MCollection dcm = makeCollection(config, errlog, logger);

    if (force == CollectionUpdateType.never || dcm instanceof CollectionSingleIndexFile) { // LOOK isIndexFile() ?
      // then just open the existing index file
      return yyopenCdmIndex(dcm.getIndexFilename(), config.gribConfig, logger);
    }

    // otherwise got to check
    if (dcm.isPartition()) {
      return Grib2PartitionBuilder.factory( (PartitionManager) dcm, force, force, errlog, logger);
    } else {
      return Grib2CollectionWriter.factory(dcm, force, errlog, logger);
    }
  } */

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // used by PartitionCollection

  /**
    * Create a GribCollection from an MCollection
    *
    * @param isGrib1 true if files are grib1, else grib2
    * @param dcm     the MCollection : files or other collections
    * @param updateType   should index file be used or remade?
    * @return GribCollection
    * @throws IOException on io error
    */
   static public GribCollection openGribCollectionFromMCollection(boolean isGrib1, MCollection dcm, CollectionUpdateType updateType,
                                                                  Formatter errlog, org.slf4j.Logger logger) throws IOException {

     FeatureCollectionConfig.GribConfig gribConfig = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);

     if (updateType == CollectionUpdateType.never || dcm instanceof CollectionSingleIndexFile) { // would isIndexFile() be better ?
       // then just open the existing index file
       return openCdmIndex(dcm.getIndexFilename(), gribConfig, logger);
     }

     // otherwise got to check
     if (dcm.isPartition()) {
       return Grib2PartitionBuilder.factory( (PartitionManager) dcm, updateType, updateType, errlog, logger);  // LOOK ?

     } else {
       Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
       boolean changed = builder.updateNeeded(updateType) && builder.createIndex(errlog);
       return openCdmIndex(builder.getIndexFile().getPath(), gribConfig, logger);
     }
   }


  ///////////
  // used by InvDatasetFcGrib

  static public GribCollection openGribCollection(boolean isGrib1, FeatureCollectionConfig config, CollectionUpdateType updateType, Logger logger) throws IOException {

    // update if needed
    updateGribCollection(config, updateType, logger);

    // now open the index
    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);
    File directory = new File(specp.getRootDir());

    File idxFile = GribCollection.getIndexFile(config.name, directory);
    return openCdmIndex(idxFile.getPath(), config.gribConfig, logger);
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

    boolean changed;

    if (config.ptype == FeatureCollectionConfig.PartitionType.none) {

      CollectionAbstract dcm = specp.wantSubdirs() ? new CollectionGeneral(config.name, rootPath, logger) : new DirectoryCollection(config.name, rootPath, logger);
      dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);

      if (specp.getFilter() != null)
        dcm.setStreamFilter(new StreamFilter(specp.getFilter()));

      Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
      changed = builder.updateNeeded(updateType) && builder.createIndex(errlog);

    } else {

      if (specp.wantSubdirs()) {
        // its a partition
        DirectoryPartition dpart = new DirectoryPartition(config, rootPath, new GribCdmIndex2(logger), logger);
        dpart.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);
        changed = updateDirectoryCollectionRecurse(dpart, config, updateType, logger);

      } else {

        // otherwise its a leaf directory
        changed = updateLeafCollection(config, updateType, logger, rootPath);
      }
    }

    long took = System.currentTimeMillis() - start;
    logger.info("updateGribCollection {} took {} msecs", config.name, took);
    return changed;
  }

  static private boolean updateDirectoryCollectionRecurse(DirectoryPartition dpart,
                                                          FeatureCollectionConfig config,
                                                          CollectionUpdateType forceCollection,
                                                          Logger logger) throws IOException {

    if (debug) System.out.printf("GribCdmIndex2.updateDirectoryCollectionRecurse %s%n", dpart.getRoot());

    if (forceCollection == CollectionUpdateType.never) return false;  // dont do nothin

    Path idxFile = dpart.getIndexPath();
    if (Files.exists(idxFile)) {
      if (forceCollection == CollectionUpdateType.nocheck) return false;  // use if index exists

      // otherwise read it to verify its a  PC
      try (RandomAccessFile raf = new RandomAccessFile(idxFile.toString(), "r")) {
        GribCollectionType type = getType(raf);
        assert type == GribCollectionType.Partition2;
      }
    }

    // index does not yet exist, or we want to test if it changed
    // must scan it
    for (MCollection part : dpart.makePartitions(forceCollection)) {
      if (part.isPartition()) {
        updateDirectoryCollectionRecurse((DirectoryPartition) part, config, forceCollection, logger);

      } else {
        Path partPath = Paths.get(part.getRoot());
        updateLeafCollection(config, forceCollection, logger, partPath);
      }
    }   // loop over partitions

    // do this partition; we just did children so never update them
    boolean result = Grib2PartitionBuilder.recreateIfNeeded(dpart, forceCollection, CollectionUpdateType.never, null, logger);

    if (debug) System.out.printf("GribCdmIndex2.updateDirectoryCollectionRecurse complete (%s) on %s%n", result, dpart.getRoot());
    return result;
  }

  /**
   * Update all the grib indices in one directory, and the collection index for that directory
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @throws IOException
   */
  static private boolean updateLeafCollection(final FeatureCollectionConfig config,
                                              CollectionUpdateType forceCollection,
                                              Logger logger, Path dirPath) throws IOException {

    if (config.ptype == FeatureCollectionConfig.PartitionType.file) {
      return updateFilePartition(config, forceCollection, logger, dirPath);

  } else {
      return updateLeafDirectoryCollection(config, forceCollection, logger, dirPath);
    }

  }

  /**
   * Update all the grib indices in one directory, and the collection index for that directory
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @throws IOException
   */
  static private boolean updateLeafDirectoryCollection(final FeatureCollectionConfig config,
                                                       CollectionUpdateType forceCollection,
                                                       Logger logger, Path dirPath) throws IOException {
    if (debug) System.out.printf(" GribCdmIndex2.updateLeafDirectoryCollection %s%n", dirPath);

    if (forceCollection == CollectionUpdateType.never) return false;  // dont do nothin

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);

    DirectoryCollection dcm = new DirectoryCollection(config.name, dirPath, logger);
    dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);
    if (specp.getFilter() != null)
      dcm.setStreamFilter(new StreamFilter(specp.getFilter()));

    Path idxFile = dcm.getIndexPath();
    if (Files.exists(idxFile)) {
      if (forceCollection == CollectionUpdateType.nocheck) return false;  // use if index exists
    }

    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    boolean changed = builder.updateNeeded(forceCollection) && builder.createIndex(errlog);

    if (debug) System.out.printf(" GribCdmIndex2.updateLeafDirectoryCollection complete (%s) on %s%n", changed, dirPath);
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
  static private boolean updateFilePartition(final FeatureCollectionConfig config,
                                             final CollectionUpdateType updateType,
                                             final Logger logger, Path dirPath) throws IOException {
    long start = System.currentTimeMillis();

    final Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);

    FilePartition partition = new FilePartition(config.name, dirPath, logger);
    partition.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);
    if (specp.getFilter() != null)
      partition.setStreamFilter(new StreamFilter(specp.getFilter()));

    // final AtomicBoolean anyChange = new AtomicBoolean(false); // just need a mutable boolean

    // redo the child collection here; could also do inside Grib2PartitionBuilder, not sure if advantage
    if (updateType != CollectionUpdateType.never) {
      partition.iterateOverMFileCollection(new DirectoryCollection.Visitor() {
        public void consume(MFile mfile) {
          MCollection dcm = new CollectionSingleFile(mfile, logger);
          Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
          try {
            boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(errlog));
            //  anyChange.set(true);

          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    }

    // redo partition index if needed, will  detect if childrenhave changed
    boolean recreated = Grib2PartitionBuilder.recreateIfNeeded(partition, updateType, CollectionUpdateType.never, errlog, logger);

    long took = System.currentTimeMillis() - start;
    String collectionName = partition.getCollectionName();
    if (recreated) logger.info("RewriteFilePartition {} took {} msecs", collectionName, took);

    return recreated;
  }

  ////////////////////////////////////////////////////////////////////////////////////

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
  // Used by IOSPs

  public static GribCollection makeGribCollectionFromRaf(boolean isGrib1, RandomAccessFile raf,
            FeatureCollectionConfig.GribConfig gribConfig, CollectionUpdateType updateType, org.slf4j.Logger logger) throws IOException {

    GribCollection result;

      // check if its a plain ole GRIB2 data file
    boolean isGribFile = (raf != null) && Grib2RecordScanner.isValidFile(raf);
    if (isGribFile) {

      result = openGribCollectionFromDataFile(false, raf, gribConfig, updateType, null, logger);
      // close the data file, the ncx2 raf file is managed by gribCollection
      raf.close();

    } else {  // check its an ncx2 file
      result = openGribCollectionFromIndexFile(raf, gribConfig, logger);
    }

    return result;
}


  /**
   * Open a grib collection from a single grib1 or grib2 file.
   * Create the gbx9 and ncx2 files if needed.
   *
   * @param isGrib1 true if grib1
   * @param dataRaf the data file already open
   * @param config  special configuration
   * @param updateType   force writing index
   * @return the resulting GribCollection
   * @throws IOException on io error
   */
  private static GribCollection openGribCollectionFromDataFile(boolean isGrib1, RandomAccessFile dataRaf, FeatureCollectionConfig.GribConfig config,
            CollectionUpdateType updateType, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    String filename = dataRaf.getLocation();
    File dataFile = new File(filename);

    // LOOK not needed: Grib2CollectionBuilder.readOrCreateIndexFromSingleFile does all this ??
    GribIndex gribIndex = isGrib1 ? new Grib1Index() : new Grib2Index();
    boolean readOk;
    try {
      // see if gbx9 file exists or is out of date; date is checked against the data file
      readOk = gribIndex.readIndex(filename, dataFile.lastModified(), updateType);
    } catch (IOException ioe) {
      readOk = false;
    }

    // make or remake the index
    if (!readOk) {
      gribIndex.makeIndex(filename, dataRaf);
      logger.debug("  Index written: {}", filename + GribIndex.GBX9_IDX);
    } else if (logger.isDebugEnabled()) {
      logger.debug("  Index read: {}", filename + GribIndex.GBX9_IDX);
    }

    MFile mfile = new MFileOS(dataFile);

    //if (isGrib1)
    //  return Grib1CollectionBuilder.readOrCreateIndexFromSingleFile(mfile, force, config, logger);
    //else
    //return Grib2CollectionWriter.readOrCreateIndexFromSingleFile(mfile, force, config, errlog, logger);

    MCollection dcm = new CollectionSingleFile(mfile, logger);
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(errlog));
    return openCdmIndex(builder.getIndexFile().getPath(), config, logger);
  }

  /**
   * Create a grib collection / partition collection from an existing ncx2 file.
   * PartionCollection.partition.getGribCollection().
   *
   * @param indexRaf the ncx2 file already open
   * @param config  special configuration
   * @return the resulting GribCollection
   * @throws IOException on io error
   */
  public static GribCollection openGribCollectionFromIndexFile(RandomAccessFile indexRaf, FeatureCollectionConfig.GribConfig config,
                                                               org.slf4j.Logger logger) throws IOException {

    GribCollectionType type = getType(indexRaf);

    String location = indexRaf.getLocation();
    File f = new File(location);
    int pos = f.getName().lastIndexOf(".");
    String name = (pos > 0) ? f.getName().substring(0, pos) : f.getName(); // remove ".ncx2"

    if (type == GribCollectionType.Partition2) {
      return Grib2PartitionBuilderFromIndex.createTimePartitionFromIndex(name, f.getParentFile(), indexRaf, config, logger);
    } else if (type == GribCollectionType.GRIB2) {
      return Grib2CollectionBuilderFromIndex.readFromIndex(name, f.getParentFile(), indexRaf, config, logger);
    }

    return null;
  }

 /* static public boolean update(boolean isGrib1, CollectionManager dcm, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    //if (isGrib1) return Grib1CollectionBuilder.update(dcm, logger);
    return Grib2CollectionBuilder.update(dcm, errlog, logger);
  }  */


  /////////////////////////////////////////////////////////////////////////////////////
  // manipulate the ncx without building a gc
  private static final boolean debug = true;
  private byte[] magic;
  private int version;
  private GribCollectionProto.GribCollection gribCollectionIndex;
  private final Logger logger;

  public GribCdmIndex2(Logger logger) {
    this.logger = logger;
  }

  /// IndexReader interface
  @Override
  public boolean readChildren(Path indexFile, AddChildCallback callback) throws IOException {
    if (debug) System.out.printf("GribCdmIndex2.readChildren %s%n", indexFile);
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
    if (debug) System.out.printf("GribCdmIndex2.isPartition %s%n", indexFile);
    try (RandomAccessFile raf = new RandomAccessFile(indexFile.toString(), "r")) {
      GribCollectionType type = getType(raf);
      return (type == GribCollectionType.Partition1) || (type == GribCollectionType.Partition2);
    }
  }

  @Override
  public boolean readMFiles(Path indexFile, List<MFile> result) throws IOException {
    if (debug) System.out.printf("GribCdmIndex2.readMFiles %s%n", indexFile);
    try (RandomAccessFile raf = new RandomAccessFile(indexFile.toString(), "r")) {
      GribCollectionType type = getType(raf);
      if (type == GribCollectionType.GRIB1 || type == GribCollectionType.GRIB2) {
        if (openIndex(raf, logger)) {
          File protoDir = new File(gribCollectionIndex.getTopDir());
          int n = gribCollectionIndex.getMfilesCount();
          for (int i = 0; i < n; i++) {
            GribCollectionProto.MFile mfilep = gribCollectionIndex.getMfiles(i);
            result.add(new GribCollectionBuilder.GcMFile(protoDir, mfilep.getFilename(), mfilep.getLastModified()));
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
      indexRaf.read(magic);

      version = indexRaf.readInt();

      long recordLength = indexRaf.readLong();
      if (recordLength > Integer.MAX_VALUE) {
        logger.error("Grib2Collection {}: invalid recordLength size {}", indexRaf.getLocation(), recordLength);
        return false;
      }
      indexRaf.skipBytes(recordLength);

      int size = NcStream.readVInt(indexRaf);
      if ((size < 0) || (size > 100 * 1000 * 1000)) {
        logger.warn("Grib2Collection {}: invalid index size {}", indexRaf.getLocation(), size);
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

}
