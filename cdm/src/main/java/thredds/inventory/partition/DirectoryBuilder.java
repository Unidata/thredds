package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionAbstract;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.nc2.util.Indent;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * A Builder of Directory Partitions and Collections
 * Each DirectoryBuilder is associated with one directory, and one ncx2 index.
 * This may contain collections of files, or subdirectories.
 * If there are subdirectories, these are children DirectoryBuilders.
 *
 * @author caron
 * @since 11/10/13
 */
public class DirectoryBuilder {

  static public MCollection factory(FeatureCollectionConfig config, Path topDir, IndexReader indexReader, org.slf4j.Logger logger) throws IOException {
    DirectoryBuilder builder = new DirectoryBuilder(config.name, topDir.toString());

    DirectoryPartition dpart = new DirectoryPartition(config, topDir, indexReader, logger);
    if (!builder.isLeaf(indexReader))  { // its a partition
      dpart.setLeaf(false);
      return dpart;
    }

    // its a collection
    boolean hasIndex = builder.findIndex();
    if (hasIndex) {
      return dpart.makeChildCollection(builder);
    } else {
      DirectoryCollection result = new DirectoryCollection(config.name, topDir, logger); // no index file
      result.setLeaf(true);
      return result;
    }
  }

  static private enum PartitionStatus {unknown, isDirectoryPartition, isLeaf}

  //////////////////////////////////////////////////////////////////////////////////////////////

  private static final boolean debug = false;
  private final String topCollectionName;  // collection name
  private final String partitionName;      // partition name
  private final Path dir;                  // the directory
  private final FileTime dirLastModified;  // directory last modified
  private Path index;                      // TimePartition index file (ncx2 with magic = TimePartition)
  private FileTime indexLastModified;      // index last modified
  private long indexSize;                  // index size

  private boolean childrenConstructed = false;
  private List<DirectoryBuilder> children = new ArrayList<>(25);
  private PartitionStatus partitionStatus = PartitionStatus.unknown;

  public DirectoryBuilder(String topCollectionName, String dirFilename) throws IOException {
    this(topCollectionName, Paths.get(dirFilename), null);
  }

  /**
   * Create a DirectoryBuilder for the named directory
   * @param topCollectionName  from config, name of the collection
   * @param dir covers this directory
   * @param attr file attributes, may be null
   * @throws IOException
   */
  public DirectoryBuilder(String topCollectionName, Path dir, BasicFileAttributes attr) throws IOException {
    this.topCollectionName = topCollectionName;
    this.dir = dir;
    this.partitionName = DirectoryCollection.makeCollectionName(topCollectionName, dir);

    if (attr == null)
      attr = Files.readAttributes(this.dir, BasicFileAttributes.class);
    if (!attr.isDirectory())
      throw new IllegalArgumentException("DirectoryPartitionBuilder needs a directory");
    dirLastModified = attr.lastModifiedTime();

    // see if we can find the index
    findIndex();
  }

  /**
   * Find the index file, using its canonical name
   * @return true if found
   * @throws IOException
   */
  public boolean findIndex() throws IOException {
    Path indexPath = Paths.get(dir.toString(), partitionName + CollectionAbstract.NCX_SUFFIX);
    if (Files.exists(indexPath)) {
      this.index = indexPath;
      BasicFileAttributes attr = Files.readAttributes(indexPath, BasicFileAttributes.class);
      this.indexLastModified = attr.lastModifiedTime();
      this.indexSize = attr.size();
      return true;
    }
    return false;
  }

  /**
   * Read the index file to find out if a partition or collection of files
   * @param indexReader reads the index
   * @return true if partition, false if file collection
   * @throws IOException on IO error
   */
  public boolean isLeaf(IndexReader indexReader) throws IOException {
    if (partitionStatus == PartitionStatus.unknown) {
      /* if (index != null) {
        boolean isPartition = indexReader.isPartition(index);
        partitionStatus = isPartition ? PartitionStatus.isPartition : PartitionStatus.isLeaf;

      } else { // no index file  */
        // temporary - just to scan 100 files in the directory
        DirectoryCollection dc = new DirectoryCollection(partitionName, dir, null);
        partitionStatus = dc.isLeafDirectory() ? PartitionStatus.isLeaf : PartitionStatus.isDirectoryPartition;
      // }
    }

    return partitionStatus == PartitionStatus.isLeaf;
  }

  /**
   * Find all children directories. Does not recurse.
   * We separate this from the constructor so it can be done on demand
   *
   * Look for children by:
   * <ol>
   *   <li>If index exists , use the children inside there./li>
   *   <li>(or) scan the directory for children partitions</li>
   * </ol>
   *
   * @param indexReader  this reads the index, and calls AddChild.addchild() for each child
   * @return children, may be empty but not null
   * @throws IOException
   */
  public List<DirectoryBuilder> constructChildren(IndexReader indexReader, CollectionUpdateType forceCollection) throws IOException {
    if (childrenConstructed) return children;

    if (index != null && forceCollection == CollectionUpdateType.nocheck) { // use index if it exists
      childrenConstructed = true;  // otherwise we are good
      if (!indexReader.readChildren(index, new AddChild())) {
        partitionStatus =  PartitionStatus.isLeaf;
        return children;  // no children - we are at the GribCollection leaves
      }

    } else {
      scanForChildren();
    }

    //once we have found children, we know that this is a time partition
    partitionStatus = (children.size() > 0) ?  PartitionStatus.isDirectoryPartition : PartitionStatus.isLeaf;

    return children;
  }

  // add a child partition from the index file (callback from constructChildren)
  // we dont know at this point if its another partition or a gribCollection
  private class AddChild implements IndexReader.AddChildCallback {
    public void addChild(String dirName, String indexFilename, long lastModified) throws IOException {
      Path indexPath = Paths.get(indexFilename);
      DirectoryBuilder child = new DirectoryBuilder(topCollectionName, indexPath, lastModified);
      children.add(child);
    }
  }

  // coming in from the index reader
  private DirectoryBuilder(String topCollectionName, Path indexFile, long indexLastModified) throws IOException {
    this.topCollectionName = topCollectionName;
    if (Files.exists(indexFile)) {
      this.index = indexFile;
      this.indexLastModified = FileTime.fromMillis(indexLastModified);
    }

    this.dir = indexFile.getParent();
    this.partitionName = DirectoryCollection.makeCollectionName(topCollectionName, dir);

    BasicFileAttributes attr = Files.readAttributes(this.dir, BasicFileAttributes.class);
    if (!attr.isDirectory())
      throw new IllegalArgumentException("DirectoryPartition needs a directory");
    dirLastModified = attr.lastModifiedTime();
  }

  /**
   * Scan for subdirectories, make each into a DirectoryBuilder and add as a child
   */
  private void scanForChildren() {
    if (debug) System.out.printf(" DirectoryBuilder.scanForChildren %s ", dir);

    int count = 0;
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
      for (Path p : ds) {
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        if (attr.isDirectory()) {
          children.add(new DirectoryBuilder(topCollectionName, p, attr));
        }
        if (debug && (count % 100 == 0)) System.out.printf("%d ", count);
        count++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (debug) System.out.printf("done=%d%n", count);
    childrenConstructed = true;
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // read the list of files from the index

  public List<MFile> readFilesFromIndex(IndexReader indexReader) throws IOException {
    List<MFile> result = new ArrayList<>(100);
    if (index == null) return result;

    indexReader.readMFiles(index, result);
    return result;
  }

  ////////////////////////////////////////////////////////

  /**
    * The directory that the partition covers
    * @return directory
    */
   public Path getDir() {
     return dir;
   }

   /**
    * The ncx2 file
    * @return  ncx2 file path
    */
   public Path getIndex() {
     return index;
   }

   /**
    * May be null if constructChildren() was not called
    * @return children directories
    */
   public List<DirectoryBuilder> getChildren() {
     return children;
   }

   public String getPartitionName() {
     return partitionName;
   }

  public void show(Formatter out) {
    out.format("Collection %s%n", partitionName);
    toString(out, new Indent(2));
    out.format("%n%n");
  }

  private void toString(Formatter out, Indent indent) {
    out.format("%sDir '%s' (%s) index '%s' (%s)%n", indent, dir, dirLastModified, index, indexLastModified);
    indent.incr();
    for (DirectoryBuilder c : children)
      c.toString(out, indent);
    indent.decr();
  }

}
