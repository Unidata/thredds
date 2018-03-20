/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.cache;

import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * An interface to a FileCache
 *
 * @author caron
 * @since 10/28/2014
 */
public interface FileCacheIF {
  void enable();
  void disable();

  FileCacheable acquire(FileFactory factory, DatasetUrl location) throws IOException;
  FileCacheable acquire(FileFactory factory, Object hashKey, DatasetUrl location, int buffer_size, CancelTask cancelTask, Object spiObject) throws IOException;

  boolean release(FileCacheable ncfile) throws IOException;
  void eject(Object hashKey);
  void clearCache(boolean force);

  // debugging
  void resetTracking();
  void showTracking(Formatter format);
  void showCache(Formatter format);
  void showStats(Formatter format);
  List<String> showCache();
}
