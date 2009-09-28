/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.nc2.util.cache;

import java.io.IOException;

/**
 * Interface for files that are cacheable.
 *
 * @author caron
 * @since Jun 2, 2008
 */
public interface FileCacheable {

  /**
   * The location of the FileCacheable, used only for debug and log messages.
   * @return location
   */
  public String getLocation();

  /**
   * Close the FileCacheable, release all resources.
   * Also must honor contract with setFileCache().
   * @throws IOException
   */
  public void close() throws IOException;

  /**
   * Sync() is called when the FileCacheable is found in the cache, before returning the object to the
   *  application. FileCacheable has an opportunity to freshen itself. FileCacheable mag ignore this call.
   * @return true if FileCacheable was changed
   * @throws IOException on i/o error.
   */
  public boolean sync() throws IOException;

  /**
   * If the FileCache is set, the FileCacheable object must store it and call FileCache.release() on FileCacheable.close():
   * <pre>
  public synchronized void close() throws java.io.IOException {
    if (isClosed) return;
    if (cache != null) {
      cache.release(this);
    } else {
      reallyClose();
    }
    isClosed = true;
   </pre>
   * @param fileCache must store this, use it on close as above.
   */
  public void setFileCache( FileCache fileCache);
}
