/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.inventory;

import ucar.nc2.util.CancelTask;
import ucar.nc2.units.TimeUnit;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Manages a dynamic collection of MFile objects. Allows storing key/value pairs on MFiles.
 * An MFile must have the property that
 * <pre>  NetcdfDataset.open(MFile.getPath, ...); </pre>
 * should work.
 *
 * @author caron
 * @since Jan 19, 2010
 */
public interface CollectionManager {

  public String getCollectionName();

  /**
   * Scan the directory(ies) and create MFile objects.
   * Get the results from getFiles()
   *
   * @param cancelTask allow user to cancel
   * @throws java.io.IOException if io error
   */
  public void scan(CancelTask cancelTask) throws IOException;

  /**
   * Compute if rescan is needed.
   *
   * @return true if rescan is needed.
   */
  public boolean isRescanNeeded();

  /**
   * Rescan directories. Files may be deleted or added.
   * If the MFile already exists in the current list, leave it in the list.
   * If returns true, get the results from getFiles(), otherwise nothing has changed.
   *
   * @return true if anything actually changed.
   * @throws IOException on I/O error
   */
  public boolean rescan() throws IOException;

  /**
   * Get how often to rescan
   *
   * @return time duration of rescan period, or null if none.
   */
  public TimeUnit getRecheck();

  /**
   * Get the last time scanned
   *
   * @return msecs since 1970
   */
  public long getLastScanned();

  /**
   * Get common root of all MFiles in the collection - may be null
   *
   * @return msecs since 1970
   */
  public String getRoot();

  /**
   * Get the current collection of MFile, since last scan or rescan.
   *
   * @return current list of MFile, sorted by name
   */
  public List<MFile> getFiles();

  /**
   * Use the date extractor to extract the date from the filename
   * @param mfile extract from here
   * @return Date, or null if none
   */
  public Date extractRunDate(MFile mfile);

  /////////////////////////////////////////////////////////////////////
  // experimental

  public void putMetadata(MFile file, String key, byte[] value);
  public byte[] getMetadata(MFile file, String key);

}
