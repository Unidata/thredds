package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
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
 * Each DirectoryPartitionBuilder is associated with one directory, and one ncx2 index.
 * If there are subdirectories, these are children DirectoryPartitionBuilders.
 *
 * @author caron
 * @since 11/10/13
 */
public class DirectoryPartitionBuilder {

  static public MCollection factory(FeatureCollectionConfig config, Path topDir, IndexReader indexReader, org.slf4j.Logger logger) throws IOException {
    DirectoryPartitionBuilder builder = new DirectoryPartitionBuilder(config.name, topDir.toString());

    DirectoryPartition dpart = new DirectoryPartition(config, topDir, indexReader, logger);
    if (builder.isPartition(indexReader))  // its a partition
      return dpart;

    // its a collection
    boolean hasIndex = builder.findIndex();
    if (hasIndex)
      return dpart.makeChildCollection(builder); // with an index
    else
      return new DirectoryCollection(config.name, topDir, logger); // no index file
  }


  static private enum PartitionStatus {unknown, isPartition ,isGribCollection}

  private final boolean debug = true;
  private final String topCollectionName;  // collection name
  private final String partitionName;      // partition name
  private final Path dir;                  // the directory
  private final FileTime dirLastModified;  // directory last modified
  private Path index;                      // TimePartition index file (ncx2 with magic = TimePartition)
  private FileTime indexLastModified;      // index last modified
  private long indexSize;                  // index size

  private boolean childrenConstructed = false;
  private List<DirectoryPartitionBuilder> children = new ArrayList<>(25);
  private PartitionStatus partitionStatus = PartitionStatus.unknown;

  public DirectoryPartitionBuilder(String topCollectionName, String dirFilename) throws IOException {
    this(topCollectionName, Paths.get(dirFilename), null);
  }

  /**
   * Create a DirectoryPartitionBuilder for the named directory
   * @param topCollectionName  from config, name of the collection
   * @param dir covers this directory
   * @param attr file attributes, may be null
   * @throws IOException
   */
  public DirectoryPartitionBuilder(String topCollectionName, Path dir, BasicFileAttributes attr) throws IOException {
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
   * Find the TimePartition index file (ncx with magic = TimePartition)
   * @return true if found
   * @throws IOException
   */
  public boolean findIndex() throws IOException {
    Path indexPath = Paths.get(dir.toString(), partitionName + DirectoryCollection.NCX_SUFFIX);
    if (Files.exists(indexPath)) {
      this.index = indexPath;
      BasicFileAttributes attr = Files.readAttributes(indexPath, BasicFileAttributes.class);
      this.indexLastModified = attr.lastModifiedTime();
      this.indexSize = attr.size();
      return true;
    }
    return false;
  }

  public boolean isPartition(IndexReader indexReader) throws IOException {
    if (partitionStatus == PartitionStatus.unknown) {
      if (index != null) {
        boolean isPartition = indexReader.isPartition(index);
        partitionStatus = isPartition ? PartitionStatus.isPartition : PartitionStatus.isGribCollection;

      } else { // no index file
        scanForChildren();
        if (children.size() > 0) partitionStatus = PartitionStatus.isPartition;
      }
    }

    return partitionStatus == PartitionStatus.isPartition;
  }

  /**
   * Find all children directories. Does not recurse.
   * We seperate this from the constructor so it can be done on demand
   *
   * Look for children by:
   * <ol>
   *   <li>If index exists , use the children inside there./li>
   *   <li>(or) scan the directory for children partitions</li>
   * </ol>
   *
   * @param indexReader  this reads the index, and calls AddChild.addchild() fro each child
   * @return children, may be empty but not null
   * @throws IOException
   */
  public List<DirectoryPartitionBuilder> constructChildren(IndexReader indexReader) throws IOException {
    if (childrenConstructed) return children;

    childrenConstructed = true;

    if (index != null) {
      if (!indexReader.readChildren(index, new AddChild())) {
        partitionStatus =  PartitionStatus.isGribCollection;
        return children;  // no children - we are at the GribCollection leaves
      }

    } else {
      scanForChildren();
    }

    //once we have found children, we know that this is a time partition
    partitionStatus = (children.size() > 0) ?  PartitionStatus.isPartition : PartitionStatus.isGribCollection;

    return children;
  }

  // add a child partition from the index file
  // we dont know at this point if its another partition or a gribCollection
  private class AddChild implements IndexReader.AddChildCallback {
    public void addChild(String dirName, String indexFilename, long lastModified) throws IOException {
      Path indexPath = Paths.get(indexFilename);
      DirectoryPartitionBuilder child = new DirectoryPartitionBuilder(topCollectionName, indexPath, lastModified);
      children.add(child);
    }
  }

  // coming in from the index reader
  private DirectoryPartitionBuilder(String topCollectionName, Path indexFile, long indexLastModified) throws IOException {
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
   * Scan for subdirectories, make each into a DirectoryPartitionBuilder and add as a child
   */
  private void scanForChildren() {
    if (debug) System.out.printf("DirectoryPartitionBuilder.scanForChildren %s%n", dir);

    int count = 0;
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
      for (Path p : ds) {
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        if (attr.isDirectory()) {
          children.add(new DirectoryPartitionBuilder(topCollectionName, p, attr));
        }
        if (debug && (count % 100 == 0)) System.out.printf("%d ", count);
        count++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // read the list of files from the index

  public List<MFile> getFiles(IndexReader indexReader) throws IOException {
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
   public List<DirectoryPartitionBuilder> getChildren() {
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
    for (DirectoryPartitionBuilder c : children)
      c.toString(out, indent);
    indent.decr();
  }

  private class MyFilter implements DirectoryStream.Filter<Path> {
    @Override
    public boolean accept(Path entry) throws IOException {
      return !entry.endsWith(".gbx9") && !entry.endsWith(".ncx");
    }
  }
}
