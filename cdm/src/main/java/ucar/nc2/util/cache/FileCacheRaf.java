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
  private FileCache cache;
  private FileFactory factory;

  public FileCacheRaf(int minElementsInMemory, int maxElementsInMemory, int period) {
    cache = new FileCache("FileCacheRaf", minElementsInMemory, maxElementsInMemory, -1, period);
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
    return (Raf) cache.acquire(factory, filename, null);
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
