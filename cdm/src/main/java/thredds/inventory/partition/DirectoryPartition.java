package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import thredds.inventory.Collection;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.*;

/**
 * A Partition that uses directories to do the partitioning.
 * Intended for very large collections - must be careful to not scan more than one directory at a time.
 * May be a TimePartition or a collection of MFiles
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

  @Override
  public Iterable<Collection> makePartitions() throws IOException {

    DirectoryPartitionBuilder builder = new DirectoryPartitionBuilder(topCollection, topDir, null);
    builder.constructChildren(indexReader);

    List<Collection> result = new ArrayList<>();
    for (DirectoryPartitionBuilder child : builder.getChildren()) {
      Collection dc = DirectoryPartitionBuilder.factory(config, child.getDir(), indexReader, logger);
      result.add(dc);
    }

    return result;
  }

  Collection makeChildCollection(DirectoryPartitionBuilder dpb) throws IOException {
    Collection result;
    boolean hasIndex = dpb.findIndex();
    if (hasIndex)
      result = new DirectoryCollectionFromIndex(dpb);
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
    return new MFileIterator( getFilesSorted().iterator());
  }

  @Override
  public void close() {
    // noop
  }

  // LOOK should this be a DirectoryCollection ??
  // adapter of DirectoryPartitionBuilder to Collection
  // claim to fame is that it scans files on demand
  // using DirectoryPartitionBuilder, it will read files from ncx index
  private class DirectoryCollectionFromIndex extends CollectionAbstract {
    final DirectoryPartitionBuilder builder;  // LOOK adapts DirectoryPartitionBuilder as Collection - why ?

    DirectoryCollectionFromIndex(DirectoryPartitionBuilder builder) {
      super(builder.getPartitionName(), DirectoryPartition.this.logger );
      setDateExtractor(DirectoryPartition.this.dateExtractor);
      this.builder = builder;
    }

    @Override
    public CloseableIterator<MFile> getFileIterator() throws IOException {
      return new MFileIterator( getFilesSorted().iterator());
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isPartition() {
      return false;
    }

    @Override
    public String getCollectionName() {
      return builder.getPartitionName();
    }

    @Override
    public String getRoot() {
      return builder.getDir().toString();
    }

    @Override
    public Iterable<MFile> getFilesSorted() throws IOException {
      return builder.getFiles(indexReader);
    }

  }

}
