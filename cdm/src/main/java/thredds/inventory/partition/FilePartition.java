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

import thredds.inventory.*;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A Partition consisting of single files, each is a GribCollection.
 * This FilePartition represents the collection of the GribCollections.
 * Eg how we store files on motherlode.
 *
 * @author caron
 * @since 12/9/13
 */
public class FilePartition extends DirectoryCollection implements PartitionManager {

  public FilePartition(String topCollectionName, Path topDir, boolean isTop, String olderThan, org.slf4j.Logger logger) {
    super(topCollectionName, topDir, isTop, olderThan, logger);
  }

  @Override
  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {

    List<MCollection> result = new ArrayList<>(100);
    try (CloseableIterator<MFile> iter = getFileIterator()) {
      while (iter.hasNext()) {
        MCollection part = new CollectionSingleFile(iter.next(), logger);
        if (!wasRemoved(part))
          result.add(part);
        lastModified = Math.max(lastModified, part.getLastModified());
      }
    }

    return result;
  }

  /////////////////////////////////////////////////////////////
  // partitions can be removed (!)
  private List<String> removed;

  public void removePartition(MCollection partition) {
    if (removed == null) removed = new ArrayList<>();
    removed.add(partition.getCollectionName());
  }

  private boolean wasRemoved(MCollection partition) {
    return removed != null && (removed.contains(partition.getCollectionName()));
  }

}
