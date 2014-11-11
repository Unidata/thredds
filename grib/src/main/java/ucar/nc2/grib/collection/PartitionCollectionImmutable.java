/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MCollection;
import ucar.coord.CoordinateRuntime;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An Immutable PartitionCollection
 *
 * @author caron
 * @since 11/10/2014
 */
public abstract class PartitionCollectionImmutable extends GribCollectionImmutable {
  static private final Logger logger = LoggerFactory.getLogger(PartitionCollectionImmutable.class);
  static public int countPC;   // debug

    // object cache for index files - these are opened only as GribCollection
  private static FileCacheIF partitionCache;

  static public void initPartitionCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    partitionCache = new ucar.nc2.util.cache.FileCache("TimePartitionCache", minElementsInMemory, maxElementsInMemory, -1, period);
  }

  static public void initPartitionCache(int minElementsInMemory, int softLimit, int hardLimit, int period) {
    partitionCache = new ucar.nc2.util.cache.FileCache("TimePartitionCache", minElementsInMemory, softLimit, hardLimit, period);
  }

  static public FileCacheIF getPartitionCache() {
    return partitionCache;
  }

  static public void disablePartitionCache() {
    if (null != partitionCache) partitionCache.disable();
    partitionCache = null;
  }

  static private final ucar.nc2.util.cache.FileFactory collectionFactory = new FileFactory() {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      RandomAccessFile raf = null;
      try {
        raf = RandomAccessFile.acquire(location);
        Partition p = (Partition) iospMessage;
        return GribCdmIndex.openGribCollectionFromIndexFile(raf, p.getConfig(), true, p.getLogger()); // dataOnly
      } catch (Throwable t) {
        if (raf != null)
          raf.close();
        RandomAccessFile.eject(location);
        throw t;
      }
    }
  };


  ///////////////////////////////////////////////
  private final List<Partition> partitions;

  private final boolean isPartitionOfPartitions;

  private final int[] run2part;   // masterRuntime.length; which partition to use for masterRuntime i

  PartitionCollectionImmutable( PartitionCollection pc) {
    super(pc);

    List<PartitionCollection.Partition> pcParts = pc.partitions;
    List<Partition> work = new ArrayList<>(pcParts.size());
    for (PartitionCollection.Partition pcPart : pcParts) {
      work.add( new Partition(pcPart));
    }

    this.partitions = Collections.unmodifiableList(work);
    this.isPartitionOfPartitions = pc.isPartitionOfPartitions;
    this.run2part = pc.run2part;
  }

    // return open GC
  public GribCollectionImmutable getLatestGribCollection(List<String> paths) throws IOException {
    Partition last = partitions.get(partitions.size()-1);
    paths.add(last.getName());

    GribCollectionImmutable gc = last.getGribCollection();
    if (gc instanceof PartitionCollectionImmutable) {
      try {
        PartitionCollectionImmutable pc = (PartitionCollectionImmutable) gc;
        return pc.getLatestGribCollection(paths);
      } finally {
        gc.close();  // make sure its closed even on exception
      }
    } else {
      return gc;
    }

  }

  public Iterable<Partition> getPartitions() {
    return partitions;
  }

  public Partition getPartition(int idx) {
    return partitions.get(idx);
  }

  public Partition getPartitionByName(String name) {
    for (Partition p : partitions)
      if (p.name.equalsIgnoreCase(name)) return p;
    return null;
  }

  public int getPartitionSize() {
     return partitions.size();
   }

   public List<Partition> getPartitionsSorted() {
    List<Partition> c = new ArrayList<>(partitions);
    Collections.sort(c);
    if (!this.config.gribConfig.filesSortIncreasing) {
      Collections.reverse(c);
    }
    return c;
  }

    // wrapper around a GribCollection
  @Immutable
  public class Partition implements Comparable<Partition> {
      private final String name, directory;
      private final String filename;
      private final long lastModified;

      // constructor from ncx
      public Partition(PartitionCollection.Partition pcPart) {
        this.name = pcPart.name;
        this.filename = pcPart.filename; // grib collection ncx
        this.lastModified = pcPart.lastModified;
        this.directory = pcPart.directory;
      }

      public boolean isPartitionOfPartitions() {
        return isPartitionOfPartitions;
      }

      public String getName() {
        return name;
      }

      public String getFilename() {
        return filename;
      }

      public String getDirectory() {
        return directory;
      }

      public long getLastModified() {
        return lastModified;
      }

      public boolean isGrib1() {
        return isGrib1;         // in GribCollection
      }

      public FeatureCollectionConfig getConfig() {
        return config;   // in GribCollection
      }

      public org.slf4j.Logger getLogger() {
        return logger;          // in TimePartition
      }

      public String getIndexFilenameInCache() {
        File file = new File(directory, filename);
        File existingFile = GribCollection.getExistingFileOrCache(file.getPath());
        if (existingFile == null) {
          // try reletive to index file
          File parent = getIndexParentFile();
          if (parent == null) return null;
          existingFile = new File(parent, filename);
          //System.out.printf("try reletive file = %s%n", existingFile);
          if (!existingFile.exists()) return null;
        }
        return existingFile.getPath();
      }

      // acquire or construct GribCollection - caller must call gc.close() when done
      public GribCollectionImmutable getGribCollection() throws IOException {
        GribCollectionImmutable result;
        String path = getIndexFilenameInCache();
        if (path == null) {
          if (GribIosp.debugIndexOnly) {  // we are running in debug mode where we only have the indices, not the data files
            // tricky: substitute the current root
            File orgParentDir = new File(directory);
            File currentFile = new File(PartitionCollectionImmutable.this.indexFilename);
            File currentParent = currentFile.getParentFile();
            File currentParentWithDir = new File(currentParent, orgParentDir.getName());
            File nestedIndex = isPartitionOfPartitions ? new File(currentParentWithDir, filename) : new File(currentParent, filename); // JMJ
            path = nestedIndex.getPath();
          } else {
            throw new FileNotFoundException("No index filename for partition= " + this.toString());
          }
        }

        if (partitionCache != null) {                     // FileFactory factory, Object hashKey, String location, int buffer_size, CancelTask cancelTask, Object spiObject
          result = (GribCollectionImmutable) partitionCache.acquire(collectionFactory, path, path, -1, null, this);
        } else {
          result = (GribCollectionImmutable) collectionFactory.open(path, -1, null, this);
        }
        return result;
      }

      @Override
      public int compareTo(Partition o) {
        return name.compareTo(o.name);
      }

      @Override
      public String toString() {
        return "Partition{" +
                ", name='" + name + '\'' +
                ", directory='" + directory + '\'' +
                ", filename='" + filename + '\'' +
                ", lastModified='" + CalendarDate.of(lastModified) + '\'' +
                '}';
      }
    }

}
