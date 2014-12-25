/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.inventory.partition;

import thredds.filesystem.MFileOS7;
import thredds.inventory.CollectionAbstract;
import thredds.inventory.MFile;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

/**
 * Manage MFiles from one directory.
 * Doesnt know about parents or children.
 * Use getFileIterator() for best performance on large directories
 *
 * @author caron
 * @since 11/16/13
 */
public class DirectoryCollection extends CollectionAbstract {

  /**
   * Create standard name = topCollectionName + last directory
   * @param topCollectionName from config, name of the collection
   * @param dir directory for this
   * @return  standard collection name, to name the index file
   */
  public static String makeCollectionName(String topCollectionName, Path dir) {
    int last = dir.getNameCount()-1;
    Path lastDir = dir.getName(last);
    String lastDirName = lastDir.toString();
    return topCollectionName +"-" + lastDirName;
  }

  /**
   * Create standard name = topCollectionName + last directory
   * @param topCollectionName from config, name of the collection
   * @param dir directory for this
   * @return  standard collection name, to name the index file
   */
  public static Path makeCollectionIndexPath(String topCollectionName, Path dir) {
    String collectionName = makeCollectionName(topCollectionName, dir);
    return Paths.get(dir.toString(), collectionName + NCX_SUFFIX);
  }

  ///////////////////////////////////////////////////////////////////////////////////

  final String topCollection;
  final Path collectionDir;              //  directory for this collection
  final long olderThanMillis;
  final boolean isTop;

  public DirectoryCollection(String topCollectionName, String topDirS, boolean isTop, String olderThan, org.slf4j.Logger logger) {
    this(topCollectionName, Paths.get(topDirS), isTop, olderThan, logger);
  }

  public DirectoryCollection(String topCollectionName, Path collectionDir, boolean isTop, String olderThan, org.slf4j.Logger logger) {
    super(null, logger);
    this.topCollection = cleanName(topCollectionName);
    this.collectionDir = collectionDir;
    this.collectionName = isTop ? this.topCollection : makeCollectionName(topCollection, collectionDir);
    this.isTop = isTop;

    this.olderThanMillis = parseOlderThanString(olderThan);
    if (debug) System.out.printf("Open DirectoryCollection %s%n", collectionName);
  }

  @Override
  public String getRoot() {
    return collectionDir.toString();
  }

  @Override
  public String getIndexFilename() {
    if (isTop) return super.getIndexFilename();
    Path indexPath = DirectoryCollection.makeCollectionIndexPath(topCollection, collectionDir);
    return indexPath.toString();
  }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return makeFileListSorted();
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MyFileIterator(collectionDir);
  }

  @Override
  public void close() {
    if (debug) System.out.printf("Close DirectoryCollection %s%n", collectionName);
  }

  // returns everything in the current directory, subject to sfilter
  private class MyFileIterator implements CloseableIterator<MFile> {
    int debugNum;
    DirectoryStream<Path> dirStream;
    Iterator<Path> dirStreamIterator;
    MFile nextMFile;
    int count = 0;

    MyFileIterator(Path dir) throws IOException {
      if (debug) {
        debugNum = debugCount++;
        System.out.printf(" MyFileIterator %s (%d)", dir, debugNum);
      }
      try {
        dirStream = Files.newDirectoryStream(dir, new MyStreamFilter());
        dirStreamIterator = dirStream.iterator();
      } catch (IOException ioe) {
        logger.error("Files.newDirectoryStream failed to open directory "+dir.getFileName(), ioe);
        throw ioe;
      }
    }

    public boolean hasNext() {
      while (true) {
        // if (debug && count % 100 == 0) System.out.printf("%d ", count);
        count++;
        if (!dirStreamIterator.hasNext()) {
          nextMFile = null;
          return false;
        }

        long now = System.currentTimeMillis();
        try {
          Path nextPath = dirStreamIterator.next();
          BasicFileAttributes attr =  Files.readAttributes(nextPath, BasicFileAttributes.class);
          if (attr.isDirectory()) continue;
          FileTime last = attr.lastModifiedTime();
          //System.out.printf("%s%n", last);
          long millisSinceModified = now - last.toMillis();
          if (millisSinceModified < olderThanMillis)
            continue;
          nextMFile = new MFileOS7(nextPath, attr);
          return true;

       } catch (IOException e) {
         throw new RuntimeException(e);
       }
      }
    }

    public MFile next() {
      if (nextMFile == null) throw new NoSuchElementException();
      return nextMFile;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    // better alternative is for caller to send in callback (Visitor pattern)
    // then we could use the try-with-resource
    public void close() throws IOException {
      if (debug) System.out.printf(" closed %d (%d)%n", count, debugNum);
      dirStream.close();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private static final boolean debug = false;
  private static int debugCount = 0;
  // this idiom keeps the iterator from escaping, so that we can use try-with-resource, and ensure it closes. like++
  public void iterateOverMFileCollection(Visitor visit) throws IOException {
    if (debug) System.out.printf(" iterateOverMFileCollection %s ", collectionDir);
    int count = 0;
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(collectionDir, new MyStreamFilter())) {
      for (Path p : ds) {
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        if (!attr.isDirectory())
          visit.consume(new MFileOS7(p));
        if (debug) System.out.printf("%d ", count++);
      }
    }
    if (debug) System.out.printf("%d%n", count);
  }

  public interface Visitor {
     public void consume(MFile mfile);
  }

}
