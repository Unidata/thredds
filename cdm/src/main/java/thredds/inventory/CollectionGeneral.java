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

package thredds.inventory;

import org.slf4j.Logger;
import thredds.filesystem.MFileOS7;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 2/7/14
 */
public class CollectionGeneral extends CollectionAbstract {
  private Path rootPath;

  public CollectionGeneral(String collectionName, Path rootPath, Logger logger) {
    super(collectionName, logger);
    this.rootPath = rootPath;
    this.root = rootPath.toString();
  }

  @Override
  public void close() { }

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
    return new MyFileIterator(rootPath);
  }

  // returns everything in the current directory, TODO: recurse into subdirectories
  private class MyFileIterator implements CloseableIterator<MFile> {
    DirectoryStream<Path> dirStream;
    Iterator<Path> dirStreamIterator;
    MFile nextMFile;

    MyFileIterator(Path dir) throws IOException {
      dirStream = Files.newDirectoryStream(dir, new MyGribFilter());
      dirStreamIterator = dirStream.iterator();
    }

    public boolean hasNext() {
      while (true) {
        if (!dirStreamIterator.hasNext()) return false;

        try {
          Path nextPath = dirStreamIterator.next();
          BasicFileAttributes attr =  Files.readAttributes(nextPath, BasicFileAttributes.class);
          if (attr.isDirectory()) continue;  // LOOK fix this
          nextMFile = new MFileOS7(nextPath, attr);

       } catch (IOException e) {
         throw new RuntimeException(e);
       }
       if (filter == null || filter.accept(nextMFile)) return true;
      }
    }


    public MFile next() {
      return nextMFile;
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
}
