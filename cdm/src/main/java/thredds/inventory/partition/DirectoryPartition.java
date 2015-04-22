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

  public DirectoryPartition(FeatureCollectionConfig config, Path collectionDir, boolean isTop, IndexReader indexReader, org.slf4j.Logger logger) {
    super(null, logger);
    this.config = config;
    this.collectionDir = collectionDir;
    this.isTop = isTop;
    this.indexReader = indexReader;

    this.topCollection = cleanName(config.collectionName);
    this.collectionName = isTop ? this.topCollection : DirectoryCollection.makeCollectionName(topCollection, collectionDir);
  }

  @Override
  public String getIndexFilename() {
    if (isTop) return super.getIndexFilename();
    Path indexPath = DirectoryCollection.makeCollectionIndexPath(topCollection, collectionDir);
    return indexPath.toString();
  }

  @Override
  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {
    if (forceCollection == null)
      forceCollection = CollectionUpdateType.test;

    DirectoryBuilder builder = new DirectoryBuilder(topCollection, collectionDir, null);
    builder.constructChildren(indexReader, forceCollection);

    List<MCollection> result = new ArrayList<>();
    for (DirectoryBuilder child : builder.getChildren()) {
      try {
        MCollection dc = DirectoryBuilder.factory(config, child.getDir(), false, indexReader, logger);  // DirectoryPartitions or DirectoryCollections
        if (!wasRemoved( dc))
          result.add(dc);
        lastModified = Math.max(lastModified, dc.getLastModified());
      } catch (IOException ioe) {
        logger.warn("DirectoryBuilder on "+child.getDir()+" failed: skipping", ioe);
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
