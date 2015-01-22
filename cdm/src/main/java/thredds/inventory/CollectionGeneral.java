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

package thredds.inventory;

import org.slf4j.Logger;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.filesystem.MFileOS7;
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
 * All files are read in at once.
 * timePartition=none
 *
 * @author caron
 * @since 2/7/14
 */
public class CollectionGeneral extends CollectionAbstract {
  private final long olderThanMillis;
  private final Path rootPath;

  public CollectionGeneral(FeatureCollectionConfig config, CollectionSpecParser specp, Logger logger) {
    super(config.collectionName, logger);
    this.root = specp.getRootDir();
    this.rootPath = Paths.get(this.root);
    this.olderThanMillis = parseOlderThanString( config.olderThan);
  }

  @Override
  public void close() { }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return makeFileListSorted();
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MyFileIterator(rootPath);
  }

  // returns everything defined by specp, checking olderThanMillis
  private class MyFileIterator implements CloseableIterator<MFile> {
    DirectoryStream<Path> dirStream;
    Iterator<Path> dirStreamIterator;
    MFile nextMFile;
    long now;

    MyFileIterator(Path dir) throws IOException {
      dirStream = Files.newDirectoryStream(dir, new MyStreamFilter());
      dirStreamIterator = dirStream.iterator();
      now = System.currentTimeMillis();
    }

    public boolean hasNext() {

      while (true) {
        if (!dirStreamIterator.hasNext()) {
          nextMFile = null;
          return false;
        }

        try {
          Path nextPath = dirStreamIterator.next();
          BasicFileAttributes attr =  Files.readAttributes(nextPath, BasicFileAttributes.class);
          if (attr.isDirectory()) continue;  // LOOK fix this

          FileTime last = attr.lastModifiedTime();
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
      dirStream.close();
    }
  }
}
