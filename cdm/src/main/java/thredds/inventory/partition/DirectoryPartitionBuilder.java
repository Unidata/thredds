package thredds.inventory.partition;

import thredds.inventory.CollectionManager;
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
 * Can be a tree.
 *
 * @author caron
 * @since 11/10/13
 */
public class DirectoryPartitionBuilder {

  static String makePartitionName(String topCollectionName, Path dir) {
    int last = dir.getNameCount()-1;
    Path lastDir = dir.getName(last);
    String lastDirName = lastDir.toString();
    return topCollectionName +"-" + lastDirName;
  }

  private final String NCX_SUFFIX = ".ncx";
  private final boolean debug = true;

  private final String topCollectionName;
  private final String collectionName;
  private final Path dir;
  private final FileTime dirLastModified;
  private Path index;
  private FileTime indexLastModified;
  private long indexSize;
  List<DirectoryPartitionBuilder> children = new ArrayList<>(20);

  public Path getDir() {
    return dir;
  }

  public Path getIndex() {
    return index;
  }

  public Iterable<MFile> getFiles() {
    return null;
  }

  public MFile getLatestFile() {
    return null;
  }

  public CalendarDate getStartCollection() {
    return null;
  }

  public String getCollectionName() {
    return collectionName;
  }

  public void close() {
  }

  public DirectoryPartitionBuilder(String topCollectionName, String dirFilename) throws IOException {
    this(topCollectionName, Paths.get(dirFilename), null);
  }

  public DirectoryPartitionBuilder(String topCollectionName, Path dir, BasicFileAttributes attr) throws IOException {
    this.topCollectionName = topCollectionName;
    this.dir = dir;
    this.collectionName = makePartitionName(topCollectionName, dir);

    if (attr == null)
      attr = Files.readAttributes(this.dir, BasicFileAttributes.class);
    if (!attr.isDirectory())
      throw new IllegalArgumentException("DirectoryPartitionBuilder needs a directory");
    dirLastModified = attr.lastModifiedTime();

    // see if we can find the index
    findIndex();
  }

  public boolean findIndex() throws IOException {
    Path indexPath = Paths.get(dir.toString(), collectionName+NCX_SUFFIX);
    if (Files.exists(indexPath)) {
      this.index = indexPath;
      BasicFileAttributes attr = Files.readAttributes(indexPath, BasicFileAttributes.class);
      this.indexLastModified = attr.lastModifiedTime();
      this.indexSize = attr.size();
      return true;
    }
    return false;
  }

  public List<DirectoryPartitionBuilder> constructChildren(IndexReader indexReader) throws IOException {
    if (index != null) {
      if (!indexReader.readChildren(index, new AddChild()))
        return children; // empty

    } else {
      scanForChildren();

      for (DirectoryPartitionBuilder c : children)
        c.constructChildren(indexReader);
    }

    return children;
  }

  // add a child partition from the index file
  private class AddChild implements IndexReader.AddChildCallback {
    public void addChild(String dirName, String filename, long lastModified) throws IOException {
      Path childPath = Paths.get(filename);
      DirectoryPartitionBuilder child = new DirectoryPartitionBuilder(topCollectionName, childPath, lastModified);
      children.add(child);
    }
  }

  // coming in from the index
  private DirectoryPartitionBuilder(String topCollectionName, Path indexFile, long indexLastModified) throws IOException {
    this.topCollectionName = topCollectionName;
    this.index = indexFile;
    this.indexLastModified = FileTime.fromMillis(indexLastModified);

    this.dir = indexFile.getParent();
    this.collectionName = makePartitionName(topCollectionName, dir);

    BasicFileAttributes attr = Files.readAttributes(this.dir, BasicFileAttributes.class);
    if (!attr.isDirectory())
      throw new IllegalArgumentException("DirectoryPartitionBuilder needs a directory");
    dirLastModified = attr.lastModifiedTime();
  }

  private void scanForChildren() {
    if (debug) System.out.printf("%nDirectoryPartitionBuilder %s%n", dir);

    int count = 0;
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
      for (Path p : ds) {
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        if (attr.isDirectory()) {
          children.add(new DirectoryPartitionBuilder(topCollectionName, p, attr));
        }
        if (debug && (count++ % 100 == 0)) System.out.printf("%d ", count);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  ////////////////////////////////////////////////////////

  public void show(Formatter out) {
    out.format("Collection %s%n", collectionName);
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
