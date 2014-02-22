package thredds.inventory.partition;

import thredds.inventory.*;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A Partition consisting of single files, each is a GribCollection.
 * This FilePartition represents the collection of the GribCollections.
 * Eg how we store files on motherlode.
 *
 * @author caron
 * @since 12/9/13
 */
public class FilePartition extends DirectoryCollection implements PartitionManager {

  public FilePartition(String topCollectionName, Path topDir, org.slf4j.Logger logger) {
    super(topCollectionName, topDir, logger);
    this.collectionName = DirectoryCollection.makeCollectionName(topCollection, topDir);
  }

  @Override
  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {
    if (forceCollection == null) forceCollection = CollectionUpdateType.test;

    List<MCollection> result = new ArrayList<>(100);
    try (CloseableIterator<MFile> iter = getFileIterator()) {
       while (iter.hasNext()) {
         MCollection part = new CollectionSingleFile(iter.next(), logger);
         result.add( part);
         lastModified = Math.max(lastModified, part.getLastModified());
       }
     }

    return result;
  }

}
