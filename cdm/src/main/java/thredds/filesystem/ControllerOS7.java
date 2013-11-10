/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package thredds.filesystem;

import thredds.inventory.MCollection;
import thredds.inventory.MController;
import thredds.inventory.MFile;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Use Java 7 NIO for scanning the file system
 *
 * @author caron
 * @since 11/8/13
 */
public class ControllerOS7 implements MController {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControllerOS7.class);

  ////////////////////////////////////////

  public ControllerOS7() {
  }

  @Override
  public Iterator<MFile> getInventoryAll(MCollection mc, boolean recheck) {
    String path = mc.getDirectoryName();
    if (path.startsWith("file:")) {
      path = path.substring(5);
    }

    File cd = new File(path);
    if (!cd.exists()) return null;
    if (!cd.isDirectory()) return null;
    return new FilteredIterator(mc, new MFileIteratorAll(cd), false);
  }

  @Override
  public Iterator<MFile> getInventoryTop(MCollection mc, boolean recheck) {
    String path = mc.getDirectoryName();
    if (path.startsWith("file:")) {
      path = path.substring(5);
    }

    File cd = new File(path);
    if (!cd.exists()) return null;
    if (!cd.isDirectory()) return null;
    return new FilteredIterator(mc, new MFileIterator(cd), false);  // removes subdirs
  }

  public Iterator<MFile> getSubdirs(MCollection mc, boolean recheck) {
    String path = mc.getDirectoryName();
    if (path.startsWith("file:")) {
      path = path.substring(5);
    }

    File cd = new File(path);
    if (!cd.exists()) return null;
    if (!cd.isDirectory()) return null;
    return new FilteredIterator(mc, new MFileIterator(cd), true);  // return only subdirs
  }


  public void close() {
  } // NOOP


  ////////////////////////////////////////////////////////////

  // handles filtering and removing/including subdirectories
  private class FilteredIterator implements Iterator<MFile> {
    private Iterator<MFile> orgIter;
    private MCollection mc;
    private boolean wantDirs;

    private MFile next;

    FilteredIterator(MCollection mc, Iterator<MFile> iter, boolean wantDirs) {
      this.orgIter = iter;
      this.mc = mc;
      this.wantDirs = wantDirs;
    }

    public boolean hasNext() {
      next = nextFilteredFile();  /// 7
      return (next != null);
    }

    public MFile next() {
      return next;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    private MFile nextFilteredFile() {
      if (orgIter == null) return null;
      if (!orgIter.hasNext()) return null;

      MFile pdata = orgIter.next();
      while ((pdata.isDirectory() != wantDirs) || !mc.accept(pdata)) {  // skip directories, and filter
        if (!orgIter.hasNext()) return null;  /// 6
        pdata = orgIter.next();
      }
      return pdata;
    }
  }

  // returns everything in the current directory
  private class MFileIterator implements Iterator<MFile> {
    List<File> files;
    int count = 0;

    MFileIterator(File dir) {
      File[] f = dir.listFiles();
      if (f == null) { // null on i/o error
        logger.warn("I/O error on " + dir.getPath());
        throw new IllegalStateException("dir.getPath() returned null on " + dir.getPath());
      } else
        files = Arrays.asList(f);
    }

    MFileIterator(List<File> files) {
      this.files = files;
    }

    public boolean hasNext() {
      return count < files.size();
    }

    public MFile next() {
      File cfile = files.get(count++);
      return new MFileOS(cfile);
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  // recursively scans everything in the directory and in subdirectories, depth first (leaves before subdirs)
  private class MFileIteratorAll implements Iterator<MFile> {
    Queue<Traversal> traverse;
    Traversal currTraversal;
    Iterator<MFile> currIter;

    MFileIteratorAll(File top) {
      traverse = new LinkedList<Traversal>();
      currTraversal = new Traversal(top);
    }

    public boolean hasNext() {
      if (currIter == null) {
        currIter = getNextIterator();
        if (currIter == null) {
          return false;
        }
      }

      if (!currIter.hasNext()) {
        currIter = getNextIterator(); /// 5
        return hasNext();
      }

      return true;
    }

    public MFile next() {
      return currIter.next();
    }

    private Iterator<MFile> getNextIterator() {

      if (!currTraversal.leavesAreDone) {
        currTraversal.leavesAreDone = true;
        return new MFileIterator(currTraversal.fileList); // look for leaves in the current directory. may be empty.

      } else {
        if ((currTraversal.subdirIterator != null) && currTraversal.subdirIterator.hasNext()) { // has subdirs
          File nextDir = currTraversal.subdirIterator.next(); /// NCDC gets null

          traverse.add(currTraversal); // keep track of current traversal
          currTraversal = new Traversal(nextDir);   /// 2
          return getNextIterator();

        } else {
          if (traverse.peek() == null) return null;
          currTraversal = traverse.remove();
          return getNextIterator();  // 3 and 4  iteration
        }
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  // traversal of one directory
  private class Traversal {
    File dir; // top directory
    List<File> fileList;  // list of files
    Iterator<File> subdirIterator;  // list of subdirs
    boolean leavesAreDone = false;   // when all the files are done, start on the subdirs

    Traversal(File dir) {
      this.dir = dir;

      fileList = new ArrayList<File>();
      if (dir == null) return;  // LOOK WHY
      if (dir.listFiles() == null) return;

      if (logger.isTraceEnabled()) logger.trace("List Directory " + dir);
      List<File> subdirList = new ArrayList<File>();
      for (File f : dir.listFiles()) {  /// 1
        if (f == null) {
          logger.warn("  NULL FILE " + f + " in directory " + dir);
          continue;
        }
        if (logger.isTraceEnabled()) logger.trace("  File " + f);

        if (f.isDirectory())
          subdirList.add(f);
        else
          fileList.add(f);
      }

      if (subdirList.size() > 0)
        this.subdirIterator = subdirList.iterator();
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

  private static void walkFileTree() throws IOException {
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

  private class MyFilter implements DirectoryStream.Filter<Path> {
    @Override
    public boolean accept(Path entry) throws IOException {
      return !entry.endsWith(".gbx9") && !entry.endsWith(".ncx");
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

  private static void dirStream() throws IOException {
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
  }

}