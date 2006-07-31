// $Id:NetcdfFileCache.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2;

import java.io.IOException;
import java.util.*;


/**
 * Keep cache of open NetcdfFile objects, for performance. Call NetcdfFileCache.acquire instead of NetcdfFile.open.
 * The NetcdfFile object typically contains a RandomAccessFile object that wraps a system resource like a file handle.
 * These are left open when the NetcdfFile is in the cache. The maximum number of these is bounded, though not strictly.
 * A cleanup routine reduces cache size to a minimum number. This cleanup is called periodically and when the maximum 
 * cache size is reached.
 * <ol>
 * <li>The NetcdfFile object must not be modified.
 * <li>The location must uniquely define the NetcdfFile object.
 * <li>The location must be usable in NetcdfFile.open(), or in the NetcdfFileFactory object if you pass one in.
 * <li>If the NetcdfFile is acquired from the cache (ie already open), ncfile.synch() is called on it.
 * </ol>
 * <pre>
  NetcdfFile ncfile = null;
  try {
    ncfile = NetcdfFileCache.acquire(location, cancelTask);
    ...
  } finally {
    ncfile.close();
  }
 </pre>
 *
 *
 * <p>
 * Library ships with cache disabled, call init() to use. Make sure you call exit() when exiting program.
 * All methods are thread safe.
 * Cleanup is done automatically in a background thread, using LRU.
 * Uses org.apache.commons.logging for error messages.
 *
 * @see ucar.nc2.dataset.NetcdfDataset#acquireFile
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
public class NetcdfFileCache {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFileCache.class);
  //static private org.apache.commons.logging.Log log = LogFactory.getLog( NetcdfFileCache.class);
  static private ArrayList cache; // CacheElement
  static private final Object lock = new Object(); // for synchronizing
  static private int maxElements, minElements;
  static private boolean disabled = true;
  static private Timer timer;

  /** Default 10 minimum, 20 maximum files, cleanup every 20 minutes */
  static public void init() {
    init( 10, 20, 20*60 );
  }

  /**
   * Initialize the cache. If you dont, default values will be used. Call disable if you dont want caching.
   *
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param period (secs) do periodic cleanups every this number of seconds.
   */
  static public void init( int minElementsInMemory, int maxElementsInMemory, long period) {
    minElements = minElementsInMemory;
    maxElements = maxElementsInMemory;
    cache = new ArrayList(2*maxElements-minElements);
    disabled = false;

    // in case its called more than once
    if (timer != null)
      timer.cancel();

    // cleanup task
    timer = new Timer();
    timer.schedule(new CleanupTask(), 1000 * period, 1000 * period);
  }

  /** Disable use of the cache.
   * Call init() to start again.
   * Generally call this before any possible use.
   */
  static public void disable() {
    disabled = true;
    if (timer != null) timer.cancel();
    timer = null;
    if ((cache != null) && cache.size() > 0)
      clearCache( true);
  }

  /** You must call exit() to shut down the background timer in order to get a clean process shutdown. */
  static public void exit() {
    disabled = true;
    if (timer != null) timer.cancel();
    timer = null;
    if ((cache != null) && cache.size() > 0)
      clearCache( true);
  }

  /**
   * Try to find a file in the cache.
   * @param cacheName used as the key.
   * @return file if its in the cache, null otherwise.
   */
  static public NetcdfFile acquireCacheOnly(String cacheName) {
    if (disabled) return null;
    if (cache == null) init();

    // see if its in the cache
    NetcdfFile ncfile = null;
    synchronized (lock) {
      for (int i = 0; i < cache.size(); i++) {
        CacheElement elem =  (CacheElement) cache.get(i);
        if (elem.cacheName.equals(cacheName) && !elem.isLocked) {
          elem.isLocked = true;
          ncfile = elem.ncfile;
          break;
        }
      }
    }

    // sync the file when you want to use it again : needed for grib growing index, netcdf-3 record growing, etc
    if (ncfile != null) {
      try {
        ncfile.sync();
        if (log.isDebugEnabled()) log.debug("NetcdfFileCache.aquire from cache "+cacheName);
      } catch (IOException e) {
        log.error("NetcdfFileCache.synch failed on "+cacheName+" "+e.getMessage());
      }
    }

    return ncfile;
  }

  /**
   * Acquire a NetcdfFile, and lock it so no one else can use it.
   * If not already in cache, open it with NetcdfFile.open(), and put in cache.
   * <p>
   * You should call NetcdfFile.close() when done, (rather than
   *  NetcdfFileCache.release() directly) and the file is then released instead of closed.
   * <p>
   * If cache size goes over maxElement, then immediately (actually in 10 msec) schedule a cleanup in a background thread.
   * This means that the cache should never get much larger than maxElement, unless you have them all locked.
   *
   * @param location file location, also used as the cache name
   * @param cancelTask user can cancel, ok to be null.
   * @return NetcdfFile corresponding to location.
   * @throws IOException on error
   *
   * @see NetcdfFile#open
   */
  static public NetcdfFile acquire(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquire( location, -1, cancelTask, null, null);
  }

  static public NetcdfFile acquire(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject, NetcdfFileFactory factory) throws IOException {
    // see if its in the cache LOOK problem is what if spiObject has changed ???
    NetcdfFile ncfile = acquireCacheOnly( location);
    if (ncfile != null) return ncfile;

    // open the file
    if (factory == null)
      ncfile = NetcdfFile.open(location, buffer_size, cancelTask, spiObject);
    else
      ncfile = factory.open(location, buffer_size, cancelTask, spiObject);

    // user may have canceled
    if ((cancelTask != null) && (cancelTask.isCancel())) {
      if (ncfile != null) ncfile.close();
      return null;
    }

    if (disabled) return ncfile;

    // keep in cache
    boolean needCleanup;
    synchronized (lock) {
      cache.add( new CacheElement( ncfile, location));
      needCleanup = (cache.size() > maxElements);
    }
    if (needCleanup)
      timer.schedule(new CleanupTask(), 10); // start up in 10 msec

    return ncfile;
  }

  /**
   * Release the file. This unlocks it, updates its lastAccessed date.
   * Normally you need not call this, just close the file as usual.
   * @param ncfile release this file.
   * @throws IOException if file not in cache.
   */
  static public void release(NetcdfFile ncfile) throws IOException {
    if (ncfile == null) return;

    if (disabled) { // should not happen
      ncfile.setCached( 0); // prevent infinite loops
      ncfile.close();
      return;
    }

    String cacheName = ncfile.getCacheName();

    synchronized (lock) {
      for (int i = 0; i < cache.size(); i++) {
        CacheElement elem =  (CacheElement) cache.get(i);
        //if (elem.cacheName.equals(cacheName) && elem.isLocked) {
        if (elem.ncfile == ncfile) {
          if (!elem.isLocked)
            log.warn("NetcdfFileCache.release "+cacheName+" not locked");
          elem.isLocked = false;
          elem.lastAccessed = System.currentTimeMillis();
          elem.countAccessed++;
          if (log.isDebugEnabled()) log.debug("NetcdfFileCache.release "+cacheName);
          return;
        }
      }
    }
    throw new IOException("NetcdfFileCache.release does not have in cache = "+ cacheName);
  }

  /**
   * Cleanup the cache, bringing it down to minimum number.
   * Will close the LRU (least recently used) ones first. Will not close locked files.
   * Normally this is done in a background thread, you dont need to call.
   */
  static private void cleanup() {
    int size = cache.size();
    if (size <= minElements) return;

    int count = 0;
    int need2delete = size - minElements;
    ArrayList deleteList = new ArrayList();

    synchronized (lock) {
      Collections.sort( cache); //sort so oldest are on top
      Iterator iter = cache.iterator();
      while (iter.hasNext()) {
        CacheElement elem =  (CacheElement) iter.next();
        if (!elem.isLocked) {
          iter.remove();
          deleteList.add(elem);
          count++;
        }
        if (count >= need2delete) break;
      }
    }

    long start = System.currentTimeMillis();
    for (int i = 0; i < deleteList.size(); i++) {
      CacheElement elem =  (CacheElement) deleteList.get(i);
      if (elem.ncfile.isCached() != 1)
        log.warn("NetcdfFileCache file cache flag not set "+elem.cacheName);

      try {
        elem.ncfile.setCached(0);
        elem.ncfile.close();
        elem.ncfile = null; // help the gc
      } catch (IOException e) {
        log.error("NetcdfFileCache.close failed on "+elem.cacheName);
      }
    }

    long took = System.currentTimeMillis() - start;
    log.debug("NetcdfFileCache.cleanup had= "+ size+" deleted= "+count+" took="+took+" msec");
    if (count < need2delete)
      log.warn("NetcdfFileCache.cleanup couldnt delete enough for minimum= "+ minElements+" actual= "+cache.size());
  }

  /**
   * Get the files in the cache. For debugging/status only, do not change!
   * @return List of NetcdfFileCache.CacheElement
   */
  static public List getCache() {
    return new ArrayList(cache);
  }

  /**
   * Remove all cache entries.
   * @param force if true, remove them even if they are currently locked.
   */
  static public void clearCache(boolean force) {
    if (null == cache) return;

    ArrayList oldcache;
    synchronized (lock) {
      if (force) {
        // may need to force all files closed
        oldcache = new ArrayList(cache);
        cache.clear();

      } else  {
        // usual case is to respect locks
        oldcache = new ArrayList(cache.size());
        Iterator iter = cache.iterator();
        while (iter.hasNext()) {
          CacheElement elem =  (CacheElement) iter.next();
          if (!elem.isLocked) {
            iter.remove();
            oldcache.add(elem);
          }
        }
      } // not force
    } // synch

    // close all files in oldcache
    Iterator iter = oldcache.iterator();
    while (iter.hasNext()) {
      CacheElement elem =  (CacheElement) iter.next();
      if (elem.isLocked)
        log.warn("NetcdfFileCache close locked file= "+elem);
      if (elem.ncfile.isCached() != 1)
        log.warn("NetcdfFileCache file cache flag not set= "+elem);
      //System.out.println("NetcdfFileCache close file= "+elem);

      try {
        elem.ncfile.setCached(0);
        elem.ncfile.close();
        elem.ncfile = null; // help the gc
      } catch (IOException e) {
        log.error("NetcdfFileCache.close failed on "+elem);
      }
    }

  }

  /**
   * Public as an artifact.
   * This tracks the elements in the cache.
   */
  static public class CacheElement implements Comparable {
    public String cacheName;
    public NetcdfFile ncfile;
    public boolean isLocked = true;
    public int countAccessed = 0;
    public long lastAccessed = 0;

    CacheElement(NetcdfFile ncfile, String cacheName) {
      this.cacheName = cacheName;
      this.ncfile = ncfile;
      ncfile.setCached( 1);
      ncfile.setCacheName( cacheName);
      if (log.isDebugEnabled()) log.debug("NetcdfFileCache add to cache "+cacheName);
    }

    public String toString() {
      return isLocked+" "+cacheName+" "+countAccessed+" "+new Date(lastAccessed);
    }

    public int compareTo(Object o) {
      CacheElement e = (CacheElement) o;
      return (int) (lastAccessed - e.lastAccessed);
    }
  }

  static private class CleanupTask extends TimerTask  {
    public void run() { cleanup(); }
  }

}