package ucar.nc2.grib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.partition.DirectoryPartition;
import thredds.inventory.partition.DirectoryPartitionBuilder;
import thredds.inventory.partition.IndexReader;
import thredds.inventory.partition.PartitionManager;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.grib1.Grib1CollectionBuilder;
import ucar.nc2.grib.grib1.Grib1TimePartitionBuilder;
import ucar.nc2.grib.grib2.builder.*;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Utilities for GribCollection ncx files.
 *
 * @author caron
 * @since 11/9/13
 */
public class GribCdmIndex implements IndexReader {
  static private final Logger logger = LoggerFactory.getLogger(GribCdmIndex.class);

  // show DirectoryPartition Index for everything under the topDir
  static public boolean showDirectoryPartitionIndex(String collectionName, File topDir, Formatter out) throws IOException {
    DirectoryPartitionBuilder builder = new DirectoryPartitionBuilder(collectionName, topDir.getPath());
    builder.constructChildren(CollectionManager.Force.nocheck, new GribCdmIndex());
    builder.show(out);
    return true;
  }


  // make DirectoryPartition Index for everything under the topDir
  static public boolean makeDirectoryPartitionIndex(FeatureCollectionConfig config, File topDir, Formatter out) throws IOException {
    GribCdmIndex indexWriter = new GribCdmIndex();
    Path topPath = Paths.get(topDir.getPath());
    DirectoryPartition dpart = new DirectoryPartition( config, topPath, indexWriter, out, logger);

    Grib2DirectoryPartitionBuilder builder = new Grib2DirectoryPartitionBuilder(dpart.getCollectionName(), topPath, dpart, logger);

    return builder.createPartitionedIndex();

    // first find all the children
    //DirectoryPartitionBuilder dirPart = new DirectoryPartitionBuilder(collectionName, topDir.getPath());
    //dirPart.constructChildren(CollectionManager.Force.nocheck, indexWriter);

    // now construct the index
    //Grib2DirectoryPartitionBuilder builder = new Grib2DirectoryPartitionBuilder();
    //builder.createIndex(dirPart);
    //dirPart.show(out);
  }

  @Override
  public boolean readChildren(Path indexFile, AddChildCallback callback) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(indexFile.toString(), "r");
    if (openIndex(raf, logger)) {
      String dirName = gribCollectionIndex.getDirName();
      for (GribCollectionProto.Partition part : gribCollectionIndex.getPartitionsList()) {
        callback.addChild(dirName, part.getFilename(), part.getLastModified());
      }
      return true;
    }
    return false;
  }

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
          gc = Grib2CollectionBuilderFromIndex.createFromIndex(indexFile, null, raf, config, logger);
          break;
        case Grib1CollectionBuilder.MAGIC_START:
          gc = Grib1CollectionBuilder.createFromIndex(indexFile, null, raf, config, logger);
          break;
        case Grib2TimePartitionBuilder.MAGIC_START:
          gc = Grib2TimePartitionBuilderFromIndex.createFromIndex(indexFile, null, raf, logger);
          break;
        case Grib1TimePartitionBuilder.MAGIC_START:
          gc = Grib1TimePartitionBuilder.createFromIndex(indexFile, null, raf, logger);
          break;
      }
    } catch (Throwable t) {
      raf.close();
      throw t;
    }

    return gc;
  }

    // move index to be a directory partition
  static public boolean moveCdmIndex(File indexFile, Logger logger) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(indexFile.getPath(), "r");
    RandomAccessFile newRaf = new RandomAccessFile(indexFile.getPath()+".copy", "rw");

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
          break; */
      }
    } catch (Throwable t) {
      raf.close();
      throw t;

    } finally {
      if (raf != null) raf.close();
      if (newRaf != null) newRaf.close();
    }

    return false;
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // manipulate the ncx without building a gc

  private byte[] magic;
  private int version;
  private GribCollectionProto.GribCollectionIndex gribCollectionIndex;

  private String computeNewDir(String oldDir, List<ucar.nc2.grib.GribCollectionProto.MFile> oldFiles, List<String> newFiles) throws IOException {
    String oldDir2 = StringUtil2.replace(oldDir, '\\', "/");

    if (oldFiles.size() == 0) return null;

    ucar.nc2.grib.GribCollectionProto.MFile first = oldFiles.get(0);
    String firstS = first.getFilename();
    int pos = firstS.lastIndexOf("/");
    String oldParent = firstS.substring(0, pos+1);  // remove the filename
    int cut = oldParent.length();

    boolean ok = true;
    for (ucar.nc2.grib.GribCollectionProto.MFile f : oldFiles) {
      String fname = f.getFilename();
      if (!fname.startsWith(oldParent)) {
        System.out.printf("BAD %s not start with %s%n", fname, oldParent);
        ok = false;
      } else {
        newFiles.add(fname.substring(cut));
      }
    }

    if (!ok) return null;

    int pos0 = (oldDir2.endsWith("/") && firstS.startsWith("/")) ? 1 : 0;
    String oldReletivePath = firstS.substring(pos0,pos+1);  // remove first "/" if needed and the filename
    String newPath = oldDir2 + oldReletivePath;

    return newPath;
  }


  /*
  Grib 1 or 2 Collection

   MAGIC_START
   version
   sizeRecords
   VariableRecords (sizeRecords bytes)
   sizeIndex
   GribCollectionIndex (sizeIndex bytes)
   */

  protected boolean moveIndex(RandomAccessFile oldIndex, RandomAccessFile newIndex, Logger logger) {
    try {
      oldIndex.order(RandomAccessFile.BIG_ENDIAN);
      newIndex.order(RandomAccessFile.BIG_ENDIAN);
      oldIndex.seek(0);
      newIndex.seek(0);

      //// header message
      magic = new byte[Grib2CollectionBuilder.MAGIC_START.getBytes().length];   // they are all the same
      oldIndex.read(magic);
      newIndex.write(magic);

      version = oldIndex.readInt();
      newIndex.writeInt(version);

      long recordLength = oldIndex.readLong();
      if (recordLength > Integer.MAX_VALUE) {
        logger.error("Grib2Collection {}: invalid recordLength size {}", oldIndex.getLocation(), recordLength);
        return false;
      }
      newIndex.writeLong(recordLength);

      byte[] records = new byte[(int) recordLength];
      oldIndex.read(records);
      newIndex.write(records);

      int size = NcStream.readVInt(oldIndex);
      if ((size < 0) || (size > 100 * 1000 * 1000)) {
        logger.warn("Grib2Collection {}: invalid index size {}", oldIndex.getLocation(), size);
        return false;
      }

      byte[] m = new byte[size];
      oldIndex.readFully(m);
      gribCollectionIndex = GribCollectionProto.GribCollectionIndex.parseFrom(m);

      // LOOK version > 12
      // make mods to gribCollectionIndex
      GribCollectionProto.GribCollectionIndex.Builder builder = GribCollectionProto.GribCollectionIndex.newBuilder(gribCollectionIndex);
      System.out.printf("DirName= %s%n", builder.getDirName());

      List<String> newFiles = new ArrayList<String>( builder.getMfilesCount());
      String newDir = computeNewDir(builder.getDirName(), builder.getMfilesList(), newFiles);
      if (newDir == null) return false;
      builder.setDirName(newDir);

      builder.clearMfiles();
      for (int i=0; i<gribCollectionIndex.getMfilesCount(); i++ ) {
        ucar.nc2.grib.GribCollectionProto.MFile s = gribCollectionIndex.getMfiles(i);
        GribCollectionProto.MFile.Builder mb = GribCollectionProto.MFile.newBuilder(s);
        mb.setFilename(newFiles.get(i));
        builder.addMfiles(mb);
      }

      // barf it back out
      GribCollectionProto.GribCollectionIndex index = builder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(newIndex, b.length); // message size
      System.out.printf("GribCollectionIndex len=%d%n", b.length);
      newIndex.write(b);  // message  - all in one gulp  */
      newIndex.close();

      Path source = Paths.get(newIndex.getLocation());
      Path org = Paths.get(oldIndex.getLocation());
      Path dest = Paths.get(newDir, org.getFileName().toString());
      Files.move(source, dest);
      System.out.printf("moved %s to %s%n", org, dest);
      return true;

    } catch (Throwable t) {
      logger.error("Error reading index " + oldIndex.getLocation(), t);
      return false;
    }
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

  void createIndex(DirectoryPartitionBuilder dirPart) {

  }

}
