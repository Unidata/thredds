/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.filesystem;

import thredds.inventory.CollectionConfig;
import thredds.inventory.MController;
import thredds.inventory.MFile;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

/**
 * Use Java 7 NIO for scanning the file system
 *
 * @author caron
 * @since 11/8/13
 */
@ThreadSafe
public class ControllerOS7 implements MController {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControllerOS7.class);

  ////////////////////////////////////////

  public ControllerOS7() {
  }

  @Override
  public Iterator<MFile> getInventoryAll(CollectionConfig mc, boolean recheck) {
    return null;
  }

  @Override
  public Iterator<MFile> getInventoryTop(CollectionConfig mc, boolean recheck) throws IOException {
    String path = mc.getDirectoryName();
    if (path.startsWith("file:")) {
      path = path.substring(5);
    }

    Path cd = Paths.get(path);
    if (!Files.exists(cd)) return null;
    return new MFileIterator(cd, new CollectionFilter(mc));  // removes subdirs
  }

  public Iterator<MFile> getSubdirs(CollectionConfig mc, boolean recheck) {
    return null;
  }


  public void close() {
  } // NOOP


  ////////////////////////////////////////////////////////////

  private static class CollectionFilter implements DirectoryStream.Filter<Path> {
    CollectionConfig mc; // LOOK not used yet

    private CollectionFilter(CollectionConfig mc) {
      this.mc = mc;
    }

    @Override
    public boolean accept(Path entry) throws IOException {
      return !entry.endsWith(".gbx9") && !entry.endsWith(".ncx");
    }
  }

  // returns everything in the current directory
  private static class MFileIterator implements Iterator<MFile> {
    Iterator<Path> dirStream;

    MFileIterator(Path dir, DirectoryStream.Filter<Path> filter) throws IOException {
      if (filter != null)
        dirStream = Files.newDirectoryStream(dir, filter).iterator();
      else
        dirStream = Files.newDirectoryStream(dir).iterator();
    }

    public boolean hasNext() {
      return dirStream.hasNext();
    }

    public MFile next() {
      try {
        return new MFileOS7(dirStream.next());
      } catch (IOException e) {
        e.printStackTrace();  // LOOK we should pass this exception up
        throw new RuntimeException(e);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  //////////////////////////////////////////////////////////////////
  // playing around with NIO

  public static class PrintFiles extends SimpleFileVisitor<Path> {
    private int countFiles;
    private int countDirs;
    private int countOther;
    private int countSyms;
    long start = System.currentTimeMillis();

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
      if (attr.isSymbolicLink()) {
        countSyms++;
      } else if (attr.isRegularFile()) {
        countFiles++;
      } else {
        countOther++;
      }
      if (countFiles % 10000 == 0) {
        double took = (System.currentTimeMillis() - start);
        double rate = countFiles / took;
        double drate = countDirs / took;
        System.out.printf("%s file rate=%f/msec drate=%f/msec%n", countFiles, rate, drate);
      }
      return FileVisitResult.CONTINUE;
    }

    // Print each directory visited.
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      System.out.format("Directory: %s%n", dir);
      countDirs++;
      return FileVisitResult.CONTINUE;
    }

    // If there is some error accessing
    // the file, let the user know.
    // If you don't override this method
    // and an error occurs, an IOException
    // is thrown.
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      System.err.println(exc);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("PrintFiles{");
      sb.append("countFiles=").append(countFiles);
      sb.append(", countDirs=").append(countDirs);
      sb.append(", countOther=").append(countOther);
      sb.append(", countSyms=").append(countSyms);
      sb.append('}');
      return sb.toString();
    }
  }

  /* private static void walkFileTree() throws IOException {
    Path dir = Paths.get("B:/ndfd/");

    long start = System.currentTimeMillis();
    PrintFiles pf = new PrintFiles();
    EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
    Files.walkFileTree(dir, pf);
    long took = (System.currentTimeMillis() - start) / 1000;
    System.out.printf("took %s secs%n", took);
    System.out.printf("%s%n", pf);
  }

  /////////////////////////////////////////////////////////////


  private static class MyFilter implements DirectoryStream.Filter<Path> {
    @Override
    public boolean accept(Path entry) throws IOException {
      return !entry.endsWith(".gbx9") && !entry.endsWith(".ncx");
    }
  }

  public interface Visitor {
     public void consume(MFile mfile);
  }


  public interface PathFilter {
     public boolean accept(Path path);
  }

  /* private static class MyFilter2 implements DirectoryStream.Filter<Path> {
    PathFilter pathFilter;

    private MyFilter2(PathFilter pathFilter) {
      this.pathFilter = pathFilter;
    }

    public boolean accept(Path entry) throws IOException {
      if (pathFilter != null && !pathFilter.accept(entry)) return false;
      String last = entry.getName(entry.getNameCount()-1).toString();
      return !last.endsWith(".gbx9") && !last.endsWith(".ncx");
    }
  }

  private void show(Path p, BasicFileAttributes attr) {
    System.out.printf("File: %s%n", p);
    System.out.println("    creationTime: " + attr.creationTime());
    System.out.println("  lastAccessTime: " + attr.lastAccessTime());
    System.out.println("lastModifiedTime: " + attr.lastModifiedTime());

    System.out.println("   isDirectory: " + attr.isDirectory());
    System.out.println("       isOther: " + attr.isOther());
    System.out.println(" isRegularFile: " + attr.isRegularFile());
    System.out.println("isSymbolicLink: " + attr.isSymbolicLink());
    System.out.println("size: " + attr.size());
    System.out.println("--------------------");
  }

  private int countFiles;
  private int countDirs;
  private int countOther;
  private int countSyms;
  long start = System.currentTimeMillis();

  private void visitFile(Path file, BasicFileAttributes attr) {
    if (attr.isSymbolicLink()) {
      countSyms++;
    } else if (attr.isRegularFile()) {
      countFiles++;
    } else {
      countOther++;
    }
    if (countFiles % 10000 == 0) {
      double took = (System.currentTimeMillis() - start);
      double rate = countFiles / took;
      double drate = countDirs / took;
      System.out.printf("%s file rate=%f/msec drate=%f/msec%n", countFiles, rate, drate);
    }
  }

  private void dirStream(Path dir) throws IOException {
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, new MyFilter())) {
      for (Path p : ds) {
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        visitFile(p, attr);
        if (attr.isDirectory()) {
          countDirs++;
          dirStream(p);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /* private static void dirStream() throws IOException {
    Path dir = Paths.get("B:/ndfd/");

    long start = System.currentTimeMillis();
    ControllerOS7 c = new ControllerOS7();
    c.dirStream(dir);
    long took = (System.currentTimeMillis() - start) / 1000;
    System.out.printf("took %s secs%n", took);
  }

  public static void main(String[] args) throws IOException {
    // dirStream();

    Path firstPath = Paths.get("/200901/20090101/LEHZ97_KNHC_200901011102").getParent();
    for (int i=0; i< firstPath.getNameCount(); i++)
      System.out.printf("  %s%n", firstPath.getName(i));

    Path currentBasePath = Paths.get("B:/ndfd/ncdc1Year-20090101.ncx").getParent();
    for (int i=0; i< currentBasePath.getNameCount(); i++)
       System.out.printf("  %s%n", currentBasePath.getName(i));

    //Path rel =  currentBasePath.relativize(firstPath);
    //Path rel2 =  firstPath.relativize(currentBasePath);
    Path res =  currentBasePath.resolve(firstPath);
    System.out.printf("res=%s%n", res);
  }  */

}
