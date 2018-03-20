/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import thredds.inventory.MCollection;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * A Partition that uses directories to do the partitioning.
 * Intended for very large collections - does not scan more than one directory at a time.
 * The partitions may be DirectoryPartitions or DirectoryCollections, figured out by DirectoryBuilder.factory()
 *
 * @author caron
 * @since 11/9/13
 */
public class DirectoryPartition extends CollectionAbstract implements PartitionManager {

  private final FeatureCollectionConfig config;
  private final Path collectionDir;              // directory for this collection
  private final String topCollection;            // config collection name,
  private final boolean isTop;                   // is this the top of the tree ?
  private final IndexReader indexReader;
  private final String suffix;

  public DirectoryPartition(FeatureCollectionConfig config, Path collectionDir, boolean isTop, IndexReader indexReader, String suffix, org.slf4j.Logger logger) {
    super(null, logger);
    this.config = config;
    this.collectionDir = collectionDir;
    this.isTop = isTop;
    this.indexReader = indexReader;
    this.suffix = suffix;

    this.topCollection = cleanName(config.collectionName);
    this.collectionName = isTop ? this.topCollection : DirectoryCollection.makeCollectionName(topCollection, collectionDir);
  }

  @Override
  public String getIndexFilename(String suffix) {
    if (isTop) return super.getIndexFilename(suffix);
    Path indexPath = DirectoryCollection.makeCollectionIndexPath(topCollection, collectionDir, suffix);
    return indexPath.toString();
  }

  @Override
  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {
    if (forceCollection == null)
      forceCollection = CollectionUpdateType.test;

    DirectoryBuilder builder = new DirectoryBuilder(topCollection, collectionDir, null, suffix);
    builder.constructChildren(indexReader, forceCollection);

    List<MCollection> result = new ArrayList<>();
    for (DirectoryBuilder child : builder.getChildren()) {
      MCollection dc = null;
      try {
        dc = DirectoryBuilder.factory(config, child.getDir(), false, indexReader, suffix, logger);  // DirectoryPartitions or DirectoryCollections
        if (!wasRemoved( dc))
          result.add(dc);
        lastModified = Math.max(lastModified, dc.getLastModified());

      } catch (Throwable ioe) {
        logger.warn("DirectoryBuilder on "+child.getDir()+" failed: skipping", ioe);
        if (dc != null) dc.close();
      }
    }

    // sort collection by name
    Collections.sort(result, new Comparator<MCollection>() {
      public int compare(MCollection o1, MCollection o2) {
        return o1.getCollectionName().compareTo(o2.getCollectionName());
      }
    });

    return result;
  }

  MCollection makeChildCollection(DirectoryBuilder dpb) throws IOException {
    MCollection result;
    boolean hasIndex = dpb.findIndex();
    if (hasIndex)
      result = new DirectoryCollectionFromIndex(dpb, dateExtractor, indexReader, this.logger);
    else
      result = new DirectoryCollection(topCollection, dpb.getDir(), false, config.olderThan, this.logger);
    return result;
  }

  @Override
  public String getRoot() {
    return collectionDir.toString();
  }

  // empty mfile list

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return new ArrayList<>();
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator( getFilesSorted().iterator(), null);
  }

  @Override
  public void close() {
    // noop
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
