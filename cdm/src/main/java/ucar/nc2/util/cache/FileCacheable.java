/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.cache;

import java.io.IOException;

/**
 * Interface for files that can be stored in FileCache.
 * Requirements:
 *   1. hashCode() must return Object.hashCode()
 *   2. close() must call cache.release(this) if cache is not null.
 *   3. must be able to detect changes in underlying object, and indicate whether it has changed.
 *
 * @author caron
 * @since Jun 2, 2008
 */
public interface FileCacheable {
  /**
   * The location of the FileCacheable. This must be sufficient for FileFactory.factory() to create the FileCacheable object
   * @return location
   */
  String getLocation();

  /**
   * Close the FileCacheable, release all resources.
   * Must call cache.release(this) if cache is not null.
   * @throws IOException on io error
   */
  void close() throws IOException;

  /**
   * Returns the time that the underlying file(s) were last modified. If they've changed since they were stored in the
   * cache, they will be closed and reopened with {@link ucar.nc2.util.cache.FileFactory}.
   *
   * @return  a {@code long} value representing the time the file(s) were last modified or {@code 0L} if the
   *          last-modified time couldn't be determined for any reason.
   */
  long getLastModified();

  /**
   * If the FileCache is not null, FileCacheable.close() must call FileCache.release()
  <pre>
  public synchronized void close() throws java.io.IOException {
    if (cache != null) {
      if (cache.release(this)) return;
    }

    reallyClose();
  } </pre>
   *
   * @param fileCache must store this, use it on close as above.
   */
  void setFileCache( FileCacheIF fileCache);

  /**
   * Release any system resources like file handles.
   * Optional, implement only if you are able to reacquire.
   * Used when object is made inactive in cache.
   * @throws IOException
   */
  void release() throws IOException;

  /**
   * Reacquire any resources like file handles
   * Used when reactivating in cache.
   * @throws IOException
   */
  void reacquire() throws IOException;

}
