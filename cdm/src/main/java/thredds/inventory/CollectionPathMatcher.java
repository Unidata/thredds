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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

/**
 * A collection defined by the collection spec (not directory sensitive)
 * May have by regexp: or glob: (experimental)
 * @author caron
 * @since 12/23/2014
 */
public class CollectionPathMatcher extends CollectionAbstract {
  protected final FeatureCollectionConfig config;
  private final boolean wantSubdirs;
  private final long olderThanMillis;
  private final Path rootPath;
  private final PathMatcher matcher;

  public CollectionPathMatcher(FeatureCollectionConfig config, CollectionSpecParser specp, Logger logger) {
    super(config.collectionName, logger);
    this.config = config;
    this.wantSubdirs = specp.wantSubdirs();
    setRoot(specp.getRootDir());
    DateExtractor extract = config.getDateExtractor();
    if (extract != null && !(extract instanceof DateExtractorNone))
      setDateExtractor(extract);

    putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
    matcher = specp.getPathMatcher();                       // LOOK still need to decide what you are matching on name, path, etc

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
    return new AllFilesIterator();
  }

  // could also use  Files.walkFileTree
  // returns everything defined by specp, checking olderThanMillis, descends into subdirs as needed
  private class AllFilesIterator implements CloseableIterator<MFile> {
    Queue<OneDirIterator> subdirs = new LinkedList<>();
    OneDirIterator current;

    AllFilesIterator() throws IOException {
      current = new OneDirIterator(rootPath, subdirs);
    }

    public boolean hasNext() {
      if (!current.hasNext()) {
        try {
          current.close();
        } catch (IOException e) {
          logger.error("Error closing dirStream", e);
        }
        current = subdirs.poll();
        return current != null && hasNext();
      }
      return true;
    }

    public MFile next() {
      if (current == null) throw new NoSuchElementException();
      return current.next();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
    public void close() throws IOException {
      if (current != null)
        current.close();
      current = null;
    }
  }

  private class OneDirIterator implements CloseableIterator<MFile> {
    Queue<OneDirIterator> subdirs;
    DirectoryStream<Path> dirStream;
    Iterator<Path> dirStreamIterator;
    MFile nextMFile;
    long now;

    OneDirIterator(Path dir, Queue<OneDirIterator> subdirs) throws IOException {
      this.subdirs = subdirs;
      dirStream = Files.newDirectoryStream(dir); // , new MyStreamFilter());  LOOK dont use the  DirectoryStream.Filter<Path>
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
          BasicFileAttributes attr = Files.readAttributes(nextPath, BasicFileAttributes.class);

          if (wantSubdirs && attr.isDirectory()) {                                // dont filter subdirectories
            subdirs.add(new OneDirIterator(nextPath, subdirs));
            continue;
          }

          if (!matcher.matches(nextPath))                                         // otherwise apply the filter specified by the specp
             continue;

          if (olderThanMillis > 0) {
            FileTime last = attr.lastModifiedTime();
            long millisSinceModified = now - last.toMillis();
            if (millisSinceModified < olderThanMillis)
              continue;
          }
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

    public void close() throws IOException {
      dirStream.close();
    }
  }
}
