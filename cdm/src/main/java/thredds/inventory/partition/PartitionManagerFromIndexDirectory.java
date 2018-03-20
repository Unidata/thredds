/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.filesystem.MFileOS7;
import thredds.inventory.*;
import ucar.nc2.util.CloseableIterator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * used only for debugging
 *
 * @author caron
 * @since 3/14/14
 */
public class PartitionManagerFromIndexDirectory extends CollectionAbstract implements PartitionManager {
  private List<File> partIndexFiles;
  private final FeatureCollectionConfig config;

  public PartitionManagerFromIndexDirectory(String name, FeatureCollectionConfig config, File directory, String suffix, org.slf4j.Logger logger) {
    super(name, logger);
    this.config = config;
    this.root = directory.getPath();
    this.partIndexFiles = new ArrayList<>();

    File[] files = directory.listFiles( new FilenameFilter() {
      public boolean accept(File dir, String name) { return name.endsWith(suffix); }
    });
    if (files != null) {
      Collections.addAll(partIndexFiles, files);
    }

    this.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);

  }

  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {
    return new PartIterator();
  }

  private class PartIterator implements Iterator<MCollection>, Iterable<MCollection> {
    Iterator<File> iter = partIndexFiles.iterator();
    MCollection next = null;

    @Override
    public Iterator<MCollection> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      if (!iter.hasNext()) {
        next = null;
        return false;
      }

      File nextFile = iter.next();
      try {
        MCollection result = new CollectionSingleIndexFile( new MFileOS7(nextFile.getPath()), logger);
        if (wasRemoved(result)) return hasNext();

        result.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
        next = result;
        return true;

      } catch (IOException e) {
        logger.error("PartitionManagerFromList failed on "+nextFile.getPath(), e);
        throw new RuntimeException(e);
      }

    }

    @Override
    public MCollection next() {
      return next;
    }

    @Override
    public void remove() {
    }
  }

  @Override
  public void close() { }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return null;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return null;
  }

    /////////////////////////////////////////////////////////////
  // partitions can be removed (!)
  private List<String> removed;

  public void removePartition( MCollection partition) {
    if (removed == null) removed = new ArrayList<>();
    removed.add(partition.getCollectionName());
  }

  private boolean wasRemoved(MCollection partition) {
    return removed != null && (removed.contains(partition.getCollectionName()));
  }
}
