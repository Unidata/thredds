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

import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

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
  public String getCollectionName();

  /**
   * Get common root directory of all MFiles in the collection - may be null
   *
   * @return root directory name, or null.
   */
  public String getRoot();

  /**
   * Use the date extractor to extract the date from the filename.
   * Only call if hasDateExtractor() == true.
   *
   * @param mfile extract from here
   * @return Date, or null if none
   */
  public CalendarDate extractDate(MFile mfile);

  /**
   * Does this CollectionManager have the ability to extract a date from the MFile ?
   * @return true if CollectionManager has a DateExtractor
   */
  public boolean hasDateExtractor();

  /**
   * The date to partition on, usually the starting date of the collection.
   * @return partition date of the collection, or null if unknown
   */
  public CalendarDate getPartitionDate();

  /**
   * Close and release any resources. Do not make further calls on this object.
   */
  public void close();

  /**
   * Choose Proto dataset as index from [0..n-1], based on configuration.
   * @param n size to choose from
   * @return index within range [0..n-1]
   */
  public int getProtoIndex(int n);

  /**
   * last time this collection was modified
   * @return msess since epoch
   */
  public long getLastModified();

  // not in cache
  public String getIndexFilename();

  //////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get the current collection of MFile.
   * if hasDateExtractor() == true, these will be sorted by Date, otherwise by path.
   *
   * @return current collection of MFile as an Iterable.
   */
  public Iterable<MFile> getFilesSorted() throws IOException;

  /**
   * Sorted filename
   * @return Sorted filename
   * @throws IOException
   */
  public List<String> getFilenames() throws IOException;

  /**
   * The latest file in the collection.
   * Only call if hasDateExtractor() == true.
   * @return latest file in the collection
   */
  public MFile getLatestFile() throws IOException;

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
  public CloseableIterator<MFile> getFileIterator() throws IOException;


  ////////////////////////////////////////////////////
  // ability to pass arbitrary information to users of the mcollection .

  public Object getAuxInfo(String key);
  public void putAuxInfo(String key, Object value);

}
