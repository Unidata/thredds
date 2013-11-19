package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * A TimePartition that uses directories to do the partitioning.
 * Intended for very large collections - must be careful to not scan more than one directory at a time.
 *
 * @author caron
 * @since 11/9/13
 */
public class DirectoryPartitionCollection extends TimePartitionCollection {

  final String topCollection;
  final IndexReader indexReader;
  final Path topDir;

  public DirectoryPartitionCollection(FeatureCollectionConfig config, Path topDir, IndexReader indexReader, Formatter errlog, org.slf4j.Logger logger) {
    super(config, errlog, logger);
    this.topDir = topDir;
    this.indexReader = indexReader;
    this.type = Type.directory;

    // corrections - lame
    this.topCollection = this.collectionName;
    this.collectionName = DirectoryCollection.makeCollectionName(collectionName, topDir);
    this.rootDir = topDir.toString();
  }

  public String getTopCollectionName() {
    return topCollection;
  }

  @Override
  public Iterable<CollectionManagerRO> makePartitions() throws IOException {

    DirectoryPartition builder = new DirectoryPartition(topCollection, topDir, null);
    builder.constructChildren(indexReader);

    List<CollectionManagerRO> result = new ArrayList<>();
    for (DirectoryPartition child : builder.getChildren()) {
      // String name = collectionName+"-"+mfile.getName();
      DirectoryPartitionManager dcm = new DirectoryPartitionManager(child);
      result.add(dcm);
    }

    return result;
  }

  /* @Override
  public Iterable<DirectoryPartitionCollection> makePartitionCollections() throws IOException {

    DirectoryPartition builder = new DirectoryPartition(topCollection, topDir, null);
    builder.constructChildren(indexReader);

    List<DirectoryPartitionCollection> result = new ArrayList<>();
    for (DirectoryPartition child : builder.getChildren()) {
      // String name = collectionName+"-"+mfile.getName();
      DirectoryPartitionCollection dcm = new DirectoryPartitionCollection(child);
      result.add(dcm);
    }

    return result;
  }  */

  // adapter of DirectoryPartitionBuilder to CollectionManagerRO
  // claim to fame is that it scans files on demand
  // using DirectoryPartitionBuilder, it will read files from ncx index
  private class DirectoryPartitionManager implements CollectionManagerRO {
    final DirectoryPartition builder;

    DirectoryPartitionManager(DirectoryPartition builder) {
      this.builder = builder;
    }

    @Override
    public boolean hasDateExtractor() {
      return true;
    }

    @Override
    public CalendarDate getStartCollection() {
      return builder.getStartCollection();
    }

    @Override
    public MFile getLatestFile() {
      return builder.getLatestFile();
    }

    @Override
    public void close() {
      builder.close();
    }

    @Override
    public Object getAuxInfo(String key) {
      return (auxInfo == null) ? null : auxInfo.get(key);
    }

    @Override
    public void putAuxInfo(String key, Object value) {
      if (auxInfo == null) auxInfo = new HashMap<>(10);
      auxInfo.put(key, value);
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
    public Iterable<MFile> getFiles() {
      return builder.getFiles();
    }

    @Override
    public CalendarDate extractRunDate(MFile mfile) {
      return dateExtractor.getCalendarDate(mfile);
    }

  }

}
