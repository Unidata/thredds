package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.filesystem.MFileOS7;
import thredds.inventory.*;
import thredds.inventory.Collection;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * A Collection that uses directories to do the partitioning.
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
      if (child.isPartition(indexReader)) {
        result.add(makeChildCollection(child)); // nested

      } else {
        result.add(new DirectoryPartitionManager(child));
      }
    }

    return result;
  }

  private DirectoryPartition makeChildCollection(DirectoryPartitionBuilder dp) {
    DirectoryPartition result = new DirectoryPartition(this.config, dp.getDir(), this.indexReader, this.logger);
    result.setPartition(true);
    return result;
  }

  @Override
  public String getRoot() {
    return topDir.toString();
  }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return null;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public interface Visitor {
     public void consume(MFile mfile);
  }


  public interface PathFilter {
     public boolean accept(Path path);
  }

  @Override
  public void close() {
    // noop
  }

    /////////////////////////////////////////////////////////////////////////

  private class MyFilter implements DirectoryStream.Filter<Path> {
    PathFilter pathFilter;

    private MyFilter(PathFilter pathFilter) {
      this.pathFilter = pathFilter;
    }

    public boolean accept(Path entry) throws IOException {
      if (pathFilter != null && !pathFilter.accept(entry)) return false;
      String last = entry.getName(entry.getNameCount()-1).toString();
      return !last.endsWith(".gbx9") && !last.endsWith(".ncx");
    }
  }

  // LOOK should this be a DirectoryCollection ??
  // adapter of DirectoryPartitionBuilder to Collection
  // claim to fame is that it scans files on demand
  // using DirectoryPartitionBuilder, it will read files from ncx index
  private class DirectoryPartitionManager extends CollectionAbstract {
    final DirectoryPartitionBuilder builder;  // LOOK adapts DirectoryPartitionBuilder as Collection - why ?

    DirectoryPartitionManager(DirectoryPartitionBuilder builder) {
      super(builder.getPartitionName(), DirectoryPartition.this.logger );
      setDateExtractor(DirectoryPartition.this.dateExtractor);
      this.builder = builder;
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
    public CloseableIterator<MFile> getFileIterator() throws IOException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() {
      builder.close();
    }

    @Override
    public boolean isPartition() {
      try {
        return builder.isPartition(DirectoryPartition.this.indexReader);
      } catch (IOException e) {
        logger.error("DirectoryPartitionManager.isPartition", e);
      }
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
    public Iterable<MFile> getFilesSorted() {
      return builder.getFiles();
    }

  }

}
