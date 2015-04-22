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

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.filesystem.MFileOS7;
import thredds.inventory.*;
import ucar.nc2.util.CloseableIterator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * used only for debugging
 *
 * @author caron
 * @since 3/14/14
 */
public class PartitionManagerFromIndexDirectory extends CollectionAbstract implements PartitionManager {
  private List<File> partIndexFiles;
  private final FeatureCollectionConfig config;

  public PartitionManagerFromIndexDirectory(String name, FeatureCollectionConfig config, File directory, org.slf4j.Logger logger) {
    super(name, logger);
    this.config = config;
    this.root = directory.getPath();
    this.partIndexFiles = new ArrayList<>();

    File[] files = directory.listFiles( new FilenameFilter() {
      public boolean accept(File dir, String name) { return name.endsWith(CollectionAbstract.NCX_SUFFIX); }
    });
    if (files != null) {
      Collections.addAll(partIndexFiles, files);
    }

    this.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);

  }

  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {
    return new PartIterator();
  }

  private class PartIterator implements Iterator<MCollection>, Iterable<MCollection> {
    Iterator<File> iter = partIndexFiles.iterator();
    MCollection next = null;

    @Override
    public Iterator<MCollection> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      if (!iter.hasNext()) {
        next = null;
        return false;
      }

      File nextFile = iter.next();
      try {
        MCollection result = new CollectionSingleIndexFile( new MFileOS7(nextFile.getPath()), logger);
        if (wasRemoved(result)) return hasNext();

        result.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
        next = result;
        return true;

      } catch (IOException e) {
        logger.error("PartitionManagerFromList failed on "+nextFile.getPath(), e);
        throw new RuntimeException(e);
      }

    }

    @Override
    public MCollection next() {
      return next;
    }

    @Override
    public void remove() {
    }
  }

  @Override
  public void close() { }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return null;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return null;
  }

    /////////////////////////////////////////////////////////////
  // partitions can be removed (!)
  private List<String> removed;

  public void removePartition( MCollection partition) {
    if (removed == null) removed = new ArrayList<>();
    removed.add(partition.getCollectionName());
  }

  private boolean wasRemoved(MCollection partition) {
    return removed != null && (removed.contains(partition.getCollectionName()));
  }
}
