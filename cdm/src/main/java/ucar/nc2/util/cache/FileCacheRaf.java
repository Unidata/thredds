/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.util.cache;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.io.IOException;


/**
 * Keep cache of open RandomAccessFile, for performance.
 * Used by TDS to optimize serving netCDF files over HTTP.
 * <p/>
 * <pre>
 * RandomAccessFile raf = null;
 * try {
 * RandomAccessFile raf = FileCache.acquire(location, cancelTask);
 * ...
 * } finally {
 * FileCache.release( raf)
 * }
 * </pre>
 * <p/>
 * <p/>
 * <p/>
 * Library ships with cache disabled.
 * If you want to use, call init() and make sure you call exit() when exiting program.
 * All methods are thread safe.
 * Cleanup is done automatically in a background thread, using LRU.
 *
 * @author jcaron
 */
public class FileCacheRaf {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileCacheRaf.class);
  private FileCache cache;
  private FileFactory factory;

  public FileCacheRaf(int minElementsInMemory, int maxElementsInMemory, int period) {
    cache = new FileCache(minElementsInMemory, maxElementsInMemory, period);
    factory = new RafFactory();
  }

  public void clearCache(boolean force) {
    cache.clearCache(force);
  }

  public Collection getCache() {
    return cache.getCache().values();
  }


  static public void shutdown() {
    FileCache.shutdown();
  }

  public Raf acquire(String filename) throws IOException {
    return (Raf) cache.acquire(filename, null, factory);
  }

  public void release(Raf craf) throws IOException {
    cache.release(craf);
  }

  private class RafFactory implements FileFactory {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      return new Raf( location);
    }
  }

  public class Raf implements FileCacheable {
    private ucar.unidata.io.RandomAccessFile raf;

    Raf(String location) throws IOException {
      this.raf = new RandomAccessFile( location, "r");
    }

    public String getLocation() {
      return raf.getLocation();
    }

    public ucar.unidata.io.RandomAccessFile getRaf() { return raf; }

    public void close() throws IOException {
      raf.close();
    }

    public boolean sync() throws IOException {
      return false;
    }

    public void setFileCache(FileCache fileCache) {
    }
  }

}
