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

package thredds.inventory.partition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.filesystem.MFileOS7;
import thredds.inventory.CollectionManagerAbstract;
import thredds.inventory.CollectionManagerRO;
import thredds.inventory.MFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Manage Files from just one collection
 *
 * @author caron
 * @since 11/16/13
 */
public class DirectoryCollection extends CollectionManagerAbstract implements CollectionManagerRO, Iterable<MFile> {
  static private final Logger logger = LoggerFactory.getLogger(DirectoryCollection.class);
  static public final String NCX_SUFFIX = ".ncx";

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
  final Path topDir;

  public DirectoryCollection(String collectionName, Path topDir, org.slf4j.Logger logger) {
    super(collectionName, logger);

    this.topDir = topDir;
    this.topCollection = this.collectionName;  // lame
    this.collectionName = makeCollectionName(collectionName, topDir);
  }

  @Override
  public String getRoot() {
    return topDir.toString();
  }

  @Override
  public long getLastScanned() {
    return 0;
  }

  @Override
  public long getLastChanged() {
    return 0;
  }

  @Override
  public boolean isScanNeeded() {
    return false;
  }

  @Override
  public boolean scan(boolean sendEvent) throws IOException {
    return false;
  }

  @Override
  public Iterable<MFile> getFiles() {
    return this;
  }

  @Override
  public Iterator<MFile> iterator() {
    try {
      return new MFileIterator(topDir, new MyFilter());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // returns everything in the current directory
  private class MFileIterator implements Iterator<MFile> {
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
        throw new RuntimeException(e);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }


  private class MyFilter implements DirectoryStream.Filter<Path> {
    @Override
    public boolean accept(Path entry) throws IOException {
      String last = entry.getName(entry.getNameCount()-1).toString();
      return !last.endsWith(".gbx9") && !last.endsWith(".ncx");
    }
  }

  private static void dirStream() throws IOException {
    Path topDir = Paths.get("B:/ndfd/200901/20090101");

    int count = 0;
    long start = System.currentTimeMillis();
    DirectoryCollection c = new DirectoryCollection("ncdc1Year", topDir, logger);
    for (MFile mfile : c.getFiles()) {
      if (count++ < 100) System.out.printf("%s%n", mfile);
    }
    long took = (System.currentTimeMillis() - start) / 1000;
    System.out.printf("took %s secs%n", took);
  }

  public static void main(String[] args) throws IOException {
    dirStream();
  }

}
