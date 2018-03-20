/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;

/**
 * Collection of MFiles
 *
 * @author caron
 * @since 11/11/13
 */
public interface MCollection extends Closeable {

  /**
   * The name of the collection
   * @return name of the collection
   */
  String getCollectionName();

  /**
   * Get common root directory of all MFiles in the collection - may be null
   *
   * @return root directory name, or null.
   */
  String getRoot();

  /**
   * Use the date extractor to extract the date from the filename.
   * Only call if hasDateExtractor() == true.
   *
   * @param mfile extract from here
   * @return Date, or null if none
   */
  CalendarDate extractDate(MFile mfile);

  /**
   * Does this CollectionManager have the ability to extract a date from the MFile ?
   * @return true if CollectionManager has a DateExtractor
   */
  boolean hasDateExtractor();

  /**
   * The date to partition on, usually the starting date of the collection.
   * @return partition date of the collection, or null if unknown
   */
  CalendarDate getPartitionDate();

  /**
   * Close and release any resources. Do not make further calls on this object.
   */
  void close();

  /**
   * Choose Proto dataset as index from [0..n-1], based on configuration.
   * @param n size to choose from
   * @return index within range [0..n-1]
   */
  int getProtoIndex(int n);

  /**
   * last time this collection was modified
   * @return msess since epoch
   */
  long getLastModified();

  // not in cache
  String getIndexFilename(String suffix);

  //////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get the current collection of MFile.
   * if hasDateExtractor() == true, these will be sorted by Date, otherwise by path.
   *
   * @return current collection of MFile as an Iterable.
   */
  Iterable<MFile> getFilesSorted() throws IOException;

  /**
   * Sorted filename
   * @return Sorted filename
   * @throws IOException
   */
  List<String> getFilenames() throws IOException;

  /**
   * The latest file in the collection.
   * Only call if hasDateExtractor() == true.
   * @return latest file in the collection
   */
  MFile getLatestFile() throws IOException;

  /**
   * Get the current collection of MFile, no guaranteed order.
   * May be faster than getFilesSorted() for large collections, use when order is not important.
   * <pre>
     try (CloseableIterator<MFile> iter = getFileIterator()) {
      while (iter.hasNext()) {
        MFile file = iter.next();
      }
    }
   * </pre>
   * @return current collection of MFile as an CloseableIterator.
   */
  CloseableIterator<MFile> getFileIterator() throws IOException;


  ////////////////////////////////////////////////////
  // ability to pass arbitrary information to users of the mcollection .

  Object getAuxInfo(String key);
  void putAuxInfo(String key, Object value);

}
