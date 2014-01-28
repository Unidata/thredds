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
 * May be a TimePartition or a collection of MFiles
 * LOOK: should this extend DirectoryCollection? Use DirectoryCollection at leaves ?
 *
 * @author caron
 * @since 11/9/13
 */
public class DirectoryPartition extends CollectionAbstract implements PartitionManager {

  private final FeatureCollectionConfig config;
  private final Path topDir;
  private final IndexReader indexReader;
  private final String topCollection;

  public DirectoryPartition(FeatureCollectionConfig config, Path topDir, IndexReader indexReader, org.slf4j.Logger logger) {
    super(null, logger);
    this.config = config;
    this.topDir = topDir;
    this.indexReader = indexReader;

    this.topCollection = config.name;
    this.collectionName = DirectoryCollection.makeCollectionName(topCollection, topDir);
    setPartition(true);
  }

  public String getTopCollectionName() {
    return topCollection;
  }

  public Path getIndexPath() {
    return DirectoryCollection.makeCollectionIndexPath(topCollection, topDir);
  }

  @Override
  public String getIndexFilename() {
    return getIndexPath().toString();
  }

  @Override
  public Iterable<MCollection> makePartitions() throws IOException {
    return makePartitions(CollectionUpdateType.test);
  }

  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {

    DirectoryBuilder builder = new DirectoryBuilder(topCollection, topDir, null);
    builder.constructChildren(indexReader);

    List<MCollection> result = new ArrayList<>();
    for (DirectoryBuilder child : builder.getChildren()) {
      MCollection dc = DirectoryBuilder.factory(config, child.getDir(), indexReader, logger);
      result.add(dc);
      lastModified = Math.max(lastModified, dc.getLastModified());
    }

    return result;
  }

  MCollection makeChildCollection(DirectoryBuilder dpb) throws IOException {
    MCollection result;
    boolean hasIndex = dpb.findIndex();
    if (hasIndex)
      result = new DirectoryCollectionFromIndex(dpb, dateExtractor, indexReader, this.logger);
    else
      result = new DirectoryCollection(topCollection, dpb.getDir(), this.logger);
    return result;
  }

  @Override
  public String getRoot() {
    return topDir.toString();
  }

  // empty mfiles

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return new ArrayList<>();
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator( getFilesSorted().iterator(), filter);
  }

  @Override
  public void close() {
    // noop
  }

}
