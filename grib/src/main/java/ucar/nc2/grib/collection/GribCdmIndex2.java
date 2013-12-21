package ucar.nc2.grib.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionSpecParser;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import thredds.inventory.partition.DirectoryCollection;
import thredds.inventory.partition.DirectoryPartition;
import thredds.inventory.partition.FilePartition;
import thredds.inventory.partition.IndexReader;
import ucar.nc2.grib.*;
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
 * Utilities for creating GRIB2 ncx2 files, both collections and partitions
 *
 * @author John
 * @since 12/5/13
 */
public class GribCdmIndex2 implements IndexReader {
  static private final Logger logger = LoggerFactory.getLogger(GribCdmIndex2.class);

  static public enum GribCollectionType {GRIB1, GRIB2, Partition1, Partition2, none}

  // open GribCollection. caller must close
  static public GribCollection openCdmIndex(String indexFile, FeatureCollectionConfig.GribConfig config, Logger logger) throws IOException {
    GribCollection gc = null;
    String magic = null;
    RandomAccessFile raf = new RandomAccessFile(indexFile, "r");

    try {
      raf.seek(0);
      byte[] b = new byte[Grib2CollectionBuilder.MAGIC_START.getBytes().length];   // they are all the same
      raf.read(b);
      magic = new String(b);

      switch (magic) {
        case Grib2CollectionBuilder.MAGIC_START:
          gc = Grib2CollectionBuilderFromIndex.readFromIndex(indexFile, null, raf, config, logger);
          break;
        case Grib2PartitionBuilder.MAGIC_START:
          gc = Grib2PartitionBuilderFromIndex.createTimePartitionFromIndex(
                  indexFile, null, raf, config, logger);
          break;
      }
      return gc;

    } catch (Throwable t) {
      raf.close();
      throw t;
    }
  }

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
    byte[] b = new byte[Grib2CollectionBuilder.MAGIC_START.getBytes().length];   // they are all the same
    raf.read(b);
    magic = new String(b);

    switch (magic) {
      case Grib2CollectionBuilder.MAGIC_START:
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

  /**
   * Rewrite all the collection indices for all the directories in a directory partition recursively
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @throws IOException
   */
  static public void rewriteIndexesPartitionAll(FeatureCollectionConfig config, Path dirPath) throws IOException {
    GribCdmIndex indexReader = new GribCdmIndex();
    DirectoryPartition dpart = new DirectoryPartition(config, dirPath, indexReader, logger);
    rewriteIndexesPartitionRecurse(dpart, config);
  }

  static private void rewriteIndexesPartitionRecurse(DirectoryPartition dpart, FeatureCollectionConfig config) throws IOException {
    dpart.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);

    // do its children
    for (MCollection part : dpart.makePartitions()) {
      if (part.isPartition()) {
        rewriteIndexesPartitionRecurse((DirectoryPartition) part, config);

      } else {
        Path partPath = Paths.get(part.getRoot());
        rewriteIndexesFilesAndCollection(config, partPath);
      }
    }

    // do this partition
    try (Grib2Partition tp = Grib2PartitionBuilder.factory(dpart, CollectionManager.Force.always, CollectionManager.Force.never, null, logger)) {
    }
  }

  /**
   * Rewrite all the grib indices in a directory, and the collection index for that directory
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @throws IOException
   */
  static public void rewriteIndexesFilesAndCollection(final FeatureCollectionConfig config, Path dirPath) throws IOException {
    long start = System.currentTimeMillis();
    String what;
    final Formatter errlog = new Formatter();

    String collectionName = DirectoryCollection.makeCollectionName(config.name, dirPath);
    Path idxFile = DirectoryCollection.makeCollectionIndexPath(config.name, dirPath);
    if (Files.exists(idxFile)) {
      what = "IndexRead";
      // read collection index
      try (GribCollection gc = Grib2CollectionBuilderFromIndex.readFromIndex(collectionName, dirPath.toFile(), config.gribConfig, logger)) {
        for (MFile mfile : gc.getFiles()) {
          try (GribCollection gcNested =
                       Grib2CollectionBuilder.readOrCreateIndexFromSingleFile(mfile, CollectionManager.Force.always, config.gribConfig, errlog, logger)) {
          }
        }
      }

    } else {
      what = "DirectoryScan";

      // collection index doesnt exists, so we have to scan
      // this idiom keeps the iterator from escaping, so that we can use try-with-resource, and ensure it closes. like++
      // i wonder what this looks like in Java 8 closures ??
      DirectoryCollection collection = new DirectoryCollection(config.name, dirPath, logger);
      collection.iterateOverMFileCollection(new DirectoryCollection.Visitor() {
        public void consume(MFile mfile) {
          try (GribCollection gcNested =
                       Grib2CollectionBuilder.readOrCreateIndexFromSingleFile(mfile, CollectionManager.Force.always, config.gribConfig, errlog, logger)) {
          } catch (IOException e) {
            logger.error("rewriteIndexesFilesAndCollection", e);
          }
        }
      });
    }

    // redo collection index
    DirectoryCollection dpart = new DirectoryCollection(config.name, dirPath, logger);
    dpart.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);
    try (GribCollection gcNew = Grib2CollectionBuilder.factory(dpart, CollectionManager.Force.always, errlog, logger)) {
    }

    long took = System.currentTimeMillis() - start;
    System.out.printf("%s %s took %s msecs%n%s%n", collectionName, what, took, errlog);
  }

  /**
   * File Partition: each File is a GribCollection, and the collection of all files in the directory is a PartitionCollection.
   * Rewrite the PartitionCollection and optionally its children
   *
   * @param config                FeatureCollectionConfig
   * @param forceCollection       always, test, nocheck, never
   * @param forceChildren         always, test, nocheck, never
   * @return true if partition was rewritten
   * @throws IOException
   */
  static public boolean rewriteFilePartition(final FeatureCollectionConfig config,
                                            final CollectionManager.Force forceCollection,
                                            final CollectionManager.Force forceChildren,
                                            final Logger logger) throws IOException {
    long start = System.currentTimeMillis();

    final Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);
    Path rootPath = Paths.get(specp.getRootDir());

    FilePartition partition = new FilePartition(config.name, rootPath, logger);
    partition.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);

    // redo the child collection here; could also do inside Grib2PartitionBuilder, not sure if advantage
    String collectionName = DirectoryCollection.makeCollectionName(config.name, rootPath);
    if (forceChildren != CollectionManager.Force.never) {
      partition.iterateOverMFileCollection(new DirectoryCollection.Visitor() {
        public void consume(MFile mfile) {
          try (GribCollection gcNested =
                       Grib2CollectionBuilder.readOrCreateIndexFromSingleFile(mfile, forceChildren, config.gribConfig, errlog, logger)) {
          } catch (IOException e) {
            logger.error("rewriteIndexesFilesAndCollection", e);
          }
        }
      });
    }

    // redo partition index if needed
    boolean recreated = Grib2PartitionBuilder.recreateIfNeeded(partition, forceCollection, CollectionManager.Force.never, errlog, logger);

    long took = System.currentTimeMillis() - start;
    if (recreated) logger.info("RewriteFilePartition {} took {} msecs \n errs={}", collectionName, took, errlog);

    return recreated;
  }

  /**
   * Directory Collection: the collection of all files in the directory is a DirectoryCollection.
   * Rewrite the DirectoryCollection
   *
   * @param config       FeatureCollectionConfig
   * @param dirPath      directory path
   * @param makeChildren if true, make a GribCollection index for each file. Not needed for the DirectoryCollection.
   * @throws IOException
   */
  static public void rewriteDirectoryCollection(final FeatureCollectionConfig config, Path dirPath, boolean makeChildren) throws IOException {
    long start = System.currentTimeMillis();
    final Formatter errlog = new Formatter();

    DirectoryCollection dirCollection = new DirectoryCollection(config.name, dirPath, logger);
    String collectionName = DirectoryCollection.makeCollectionName(config.name, dirPath);

    if (makeChildren) {
      dirCollection.iterateOverMFileCollection(new DirectoryCollection.Visitor() {
        public void consume(MFile mfile) {
          try (GribCollection gcNested =
                       Grib2CollectionBuilder.readOrCreateIndexFromSingleFile(mfile, CollectionManager.Force.always, config.gribConfig, errlog, logger)) {
          } catch (IOException e) {
            logger.error("rewriteIndexesFilesAndCollection", e);
          }
        }
      });
    }

    // redo partition index
    dirCollection.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);
    try (GribCollection gcNew = Grib2PartitionBuilder.factory(dirCollection, CollectionManager.Force.always, errlog, logger)) {
    }

    long took = System.currentTimeMillis() - start;
    System.out.printf("rewriteDirectoryCollection %s took %s msecs%n%s%n", collectionName, took, errlog);
  }


  /////////////////////////////////////////////////////////////////////////////////////
  // manipulate the ncx without building a gc
  private static final boolean debug = true;
  private byte[] magic;
  private int version;
  private GribCollectionProto.GribCollectionIndex gribCollectionIndex;

  /// IndexReader interface
  @Override
  public boolean readChildren(Path indexFile, AddChildCallback callback) throws IOException {
    if (debug) System.out.printf("GribCdmIndex.readChildren %s%n", indexFile);
    try (RandomAccessFile raf = new RandomAccessFile(indexFile.toString(), "r")) {
      GribCollectionType type = getType(raf);
      if (type == GribCollectionType.Partition1 || type == GribCollectionType.Partition2) {
        if (openIndex(raf, logger)) {
          String dirName = gribCollectionIndex.getTopDir();
          for (GribCollectionProto.Partition part : gribCollectionIndex.getPartitionsList()) {
            callback.addChild(dirName, part.getFilename(), part.getLastModified());
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
      magic = new byte[Grib2CollectionBuilder.MAGIC_START.getBytes().length];   // they are all the same
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
      gribCollectionIndex = GribCollectionProto.GribCollectionIndex.parseFrom(m);
      return true;

    } catch (Throwable t) {
      logger.error("Error reading index " + indexRaf.getLocation(), t);
      return false;
    }
  }

}
