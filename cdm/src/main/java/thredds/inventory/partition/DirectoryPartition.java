package thredds.inventory.partition;

import thredds.inventory.MFile;
import ucar.nc2.time.CalendarDate;
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
 * Each Partition is associated with one directory, and one ncx index.
 * If there are subdirectories, these are children DirectoryPartitions.
 *
 * @author caron
 * @since 11/10/13
 */
public class DirectoryPartition {

  private final boolean debug = false;

  private final String topCollectionName;  // collection name
  private final String partitionName;      // partition name
  private final Path dir;                  // the directory
  private final FileTime dirLastModified;  // directory last modified
  private Path index;                      // TimePartition index file (ncx with magic = TimePartition)
  private FileTime indexLastModified;      // index last modified
  private long indexSize;                  // index size

  private List<DirectoryPartition> children;  // children

  /**
   * The directory that the partition covers
   * @return directory
   */
  public Path getDir() {
    return dir;
  }

  /**
   * The ncx file
   * @return  ncx file path
   */
  public Path getIndex() {
    return index;
  }

  public List<DirectoryPartition> getChildren() {
    if (children == null) return new ArrayList<>();
    return children;
  }

  public boolean hasChildren() {
    return (children != null) && children.size() > 0;
  }

  // LOOK
  public Iterable<MFile> getFiles() {
    return null;
  }

  // LOOK
  public MFile getLatestFile() {
    return null;
  }

  // LOOK
  public CalendarDate getStartCollection() {
    return null;
  }

  public String getPartitionName() {
    return partitionName;
  }

  // LOOK
  public void close() {
  }

  public DirectoryPartition(String topCollectionName, String dirFilename) throws IOException {
    this(topCollectionName, Paths.get(dirFilename), null);
  }

  /**
   * Create a DirectoryPartitionBuilder for the named directory
   * @param topCollectionName  from config, name of the collection
   * @param dir covers this directory
   * @param attr file attributes, may be null
   * @throws IOException
   */
  public DirectoryPartition(String topCollectionName, Path dir, BasicFileAttributes attr) throws IOException {
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

  /**
   * Find all children directories. Recurse.
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
  public List<DirectoryPartition> constructChildren(IndexReader indexReader) throws IOException {
    children = new ArrayList<>(25);  // children

    if (index != null) {
      if (!indexReader.readChildren(index, new AddChild()))
        return children; // empty

    } else {
      scanForChildren();

      for (DirectoryPartition c : children)
        c.constructChildren(indexReader);
    }

    return children;
  }

  // add a child partition from the index file
  private class AddChild implements IndexReader.AddChildCallback {
    public void addChild(String dirName, String indexFilename, long lastModified) throws IOException {
      Path indexPath = Paths.get(indexFilename);
      DirectoryPartition child = new DirectoryPartition(topCollectionName, indexPath, lastModified);
      children.add(child);
    }
  }

  // coming in from the index reader
  private DirectoryPartition(String topCollectionName, Path indexFile, long indexLastModified) throws IOException {
    this.topCollectionName = topCollectionName;
    this.index = indexFile;
    this.indexLastModified = FileTime.fromMillis(indexLastModified);

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
    if (debug) System.out.printf("%DirectoryPartition %s%n", dir);

    int count = 0;
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
      for (Path p : ds) {
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        if (attr.isDirectory()) {
          children.add(new DirectoryPartition(topCollectionName, p, attr));
        }
        if (debug && (count % 100 == 0)) System.out.printf("%d ", count);
        count++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  ////////////////////////////////////////////////////////

  public void show(Formatter out) {
    out.format("Collection %s%n", partitionName);
    toString(out, new Indent(2));
    out.format("%n%n");
  }

  private void toString(Formatter out, Indent indent) {
    out.format("%sDir '%s' (%s) index '%s' (%s)%n", indent, dir, dirLastModified, index, indexLastModified);
    indent.incr();
    for (DirectoryPartition c : children)
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
