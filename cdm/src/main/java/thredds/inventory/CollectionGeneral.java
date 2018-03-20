/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
