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
import java.util.*;

/**
 * Manage MFiles from just one directory.
 * Use getFileIterator() for nest performance on large directories
 *
 * @author caron
 * @since 11/16/13
 */
public class DirectoryCollection extends CollectionAbstract {
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

  public DirectoryCollection(String topCollectionName, Path topDir, org.slf4j.Logger logger) {
    super(topCollectionName, logger);
    this.topCollection = cleanName(topCollectionName);
    this.topDir = topDir;
    this.collectionName = makeCollectionName(collectionName, topDir);
  }

  public DirectoryCollection(String topCollectionName, String topDirS, org.slf4j.Logger logger) {
    super(topCollectionName, logger);
    this.topCollection = cleanName(topCollectionName);
    this.topDir = Paths.get(topDirS);
    this.collectionName = makeCollectionName(collectionName, topDir);
  }

  @Override
  public String getRoot() {
    return topDir.toString();
  }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    List<MFile> list = new ArrayList<>(100);
    try (CloseableIterator<MFile> iter = getFileIterator()) {
       while (iter.hasNext()) {
         list.add(iter.next());
       }
     }
    if (hasDateExtractor()) {
      Collections.sort(list, new DateSorter());  // sort by date
    } else {
      Collections.sort(list);                    // sort by name
    }
    return list;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator(topDir, new MyFilter());
  }

  @Override
  public void close() {
  }

  // returns everything in the current directory
  private class MFileIterator implements CloseableIterator<MFile> {
    DirectoryStream<Path> dirStream;
    Iterator<Path> dirStreamIterator;

    MFileIterator(Path dir, DirectoryStream.Filter<Path> filter) throws IOException {
      if (filter != null)
        dirStream = Files.newDirectoryStream(dir, filter);
      else
        dirStream = Files.newDirectoryStream(dir);

      dirStreamIterator = dirStream.iterator();
    }

    public boolean hasNext() {
      return dirStreamIterator.hasNext();
    }

    public MFile next() {
      try {
        return new MFileOS7(dirStreamIterator.next());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    // better alternative is for caller to send in callback (Visitor pattern)
    // then we could use the try-with-resource
    public void close() throws IOException {
      dirStream.close();
    }
  }

  /////////////////////////////////////////////////////////////////////////

  private class MyFilter implements DirectoryStream.Filter<Path> {
    public boolean accept(Path entry) throws IOException {
      String last = entry.getName(entry.getNameCount()-1).toString();
      return !last.endsWith(".gbx9") && !last.endsWith(".gbx8") && !last.endsWith(".ncx") && !last.endsWith(".ncx2") &&  // LOOK GRIB specific
              !last.endsWith(".xml");
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  // this idiom keeps the iterator from escaping, so that we can use try-with-resource, and ensure it closes. like++
  public void iterateOverMFileCollection(Visitor visit) throws IOException {
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(topDir, new MyFilter())) {
      for (Path p : ds) {
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        if (!attr.isDirectory())
          visit.consume(new MFileOS7(p));
      }
    }
  }

  public interface Visitor {
     public void consume(MFile mfile);
  }

}
