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
import thredds.filesystem.MFileOS;
import thredds.filesystem.MFileOS7;
import thredds.inventory.*;
import ucar.nc2.util.CloseableIterator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A PartitionManager from a list of index files.
 * Used in GribCollectionBuilder with multiple runtimes.
 * Index Files are already in the cache.
 *
 * @author John
 * @since 2/5/14                   `
 */
public class PartitionManagerFromIndexList extends CollectionAbstract implements PartitionManager {
  private List<MFile> partIndexFiles;
  private final FeatureCollectionConfig config;

  public PartitionManagerFromIndexList(MCollection dcm, List<MFile> partFiles, org.slf4j.Logger logger) {
    super(dcm.getCollectionName(), logger);
    this.config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);

    this.root = dcm.getRoot();
    this.partIndexFiles = partFiles;
  }

  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {
    return new PartIterator();
  }

  private class PartIterator implements Iterator<MCollection>, Iterable<MCollection> {
    Iterator<MFile> iter = partIndexFiles.iterator();

    @Override
    public Iterator<MCollection> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public MCollection next() {
      MFile nextFile = iter.next();

      // try {
        MCollection result = new CollectionSingleIndexFile( nextFile, logger);
        result.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
        return result;

      //} catch (IOException e) {
      //  logger.error("PartitionManagerFromList failed on "+nextFile.getPath(), e);
     //   throw new RuntimeException(e);
     // }
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

  public void removePartition( MCollection partition) {
    for (MFile mfile : partIndexFiles) {
      if (mfile.getName().equalsIgnoreCase(partition.getCollectionName())) {
        List<MFile> part = new ArrayList<>(partIndexFiles);
        part.remove(mfile);
        partIndexFiles = part;
        return;
      }
    }
  }
}
