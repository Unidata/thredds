// $Id:NetcdfDatasetCache.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2.dataset;

import java.io.IOException;
import java.util.*;

import ucar.nc2.NetcdfFile;

/**
 * Keep cache of open NetcdfDataset, for performance. Call NetcdfDatasetCache.acquire instead of NetcdfDataset.open.
 * <ol>
 * <li>The NetcdfDataset object must not be modified.
 * <li>The location must uniquely define the NetcdfDataset object.
 * <li>The location must be usable in NetcdfDataset.openDataset(). We actually use an equivilent package private method,
 *   NetcdfDataset.acquireDataset() which acquires the underlying NetcdfFile.
 * <li>When the NetcdfDataset is closed, the underlying file is also closed.
 * <li>If the NetcdfDataset is acquired from the cache, ncfile.synch() the underlying file is acquired again.
 * </ol>
 * <pre>
  NetcdfDataset ncd = null;
  try {
    ncd = NetcdfDatsetCache.acquire(location, enhance, cancelTask);
    ...
  } finally {
    ncd.close( ncfile)
  }
 </pre>
 *
 *
 * <p>
 * Library ships with cache disabled, call init() to use. Make sure you call exit() when exiting program.
 * All methods are thread safe.
 * Cleanup is done automatically in a background thread, using LRU.
 * Uses org.slf4j.Logger for error messages.
 *
 * @author john caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
public class NetcdfDatasetCache {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NetcdfDatasetCache.class);
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
   * If found, call sync() on it and return it.
   * @param cacheName used as the key.
   * @return file if its in the cache, null otherwise.
   */
  static public NetcdfDataset acquireCacheOnly(String cacheName, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    if (disabled) return null;
    if (cache == null) init();

    // see if its in the cache
    CacheElement wantElem = null;
    synchronized (lock) {
      for (int i = 0; i < cache.size(); i++) {
        CacheElement testElem =  (CacheElement) cache.get(i);
        if (testElem.cacheName.equals(cacheName) && !testElem.isLocked) {
          testElem.isLocked = true;
          wantElem = testElem;
          break;
        }
      }
    }

    if (wantElem == null)
      return null;
       
    NetcdfDataset ncd = wantElem.ncd;
    if (ncd != null) {
      try {
        ncd.sync();
        if (logger.isDebugEnabled()) logger.debug("NetcdfFileCache.aquire from cache {}", cacheName);
      } catch (IOException e) {
        logger.error("NetcdfFileCache.synch failed on "+cacheName+" "+e.getMessage());
      }
    }

    return ncd;
  }

  /**
   * Acquire a NetcdfDataset, and lock it so no one else can use it.
   * If not already in cache, open it with NetcdfDataset.openDataset() with enhance=true, and put in cache.
   * <p>
   * You should call NetcdfDataset.close() when done, (rather than
   *  NetcdfDatasetCache.release() directly) and the file is then released instead of closed.
   * <p>
   * If cache size goes over maxElement, then immediately (actually in 10 msec) schedule a cleanup in a background thread.
   * This means that the cache should never get much larger than maxElement, unless you have them all locked.
   *
   * @param location file location, also used as the cache name
   * @param cancelTask user can cancel the task, ok to be null.
   * @return NetcdfDataset corresponding to location.
   * @throws IOException on error
   *
   * @see NetcdfFile#open
   */
  static public NetcdfDataset acquire(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquire(location, cancelTask, null);
  }

  /** Same as acquire(), but use a factory to open the dataset.
   * @param cacheName : use this as the cache name, may or may not be the same as the location. This is also passed to the
   *  factory object.
   * @param cancelTask user can cancel the task, ok to be null.
   * @param factory use this to open; if null use NetcdfDataset.openDataset(). factory should not use NetcdfFileCache.
   * @return NetcdfFile corresponding to location.
   * @throws IOException on error
   */
  static public NetcdfDataset acquire(String cacheName, ucar.nc2.util.CancelTask cancelTask, NetcdfDatasetFactory factory) throws IOException {
    return acquire(cacheName, -1, cancelTask, null, factory);
  }

  /** Same as acquire(), but use a factory to open the dataset.
     * @param cacheName : use this as the cache name, may or may not be the same as the location. This is also passed to the
     *  factory object.
     * @param cancelTask user can cancel the task, ok to be null.
     * @param factory use this to open; if null use NetcdfDataset.openDataset(). factory should not use NetcdfFileCache.
     * @return NetcdfFile corresponding to location.
     * @throws IOException on error
     */
    static public NetcdfDataset acquire(String cacheName, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject, NetcdfDatasetFactory factory) throws IOException {
    // see if its in the cache
    NetcdfDataset ncd = acquireCacheOnly( cacheName, cancelTask);
    if (ncd != null) return ncd;

    // open the file
    if (factory != null)
      ncd = factory.openDataset(cacheName, buffer_size, cancelTask, spiObject);
    else
      ncd = NetcdfDataset.openDataset(cacheName, true, buffer_size, cancelTask, spiObject);

    // user may have canceled
    if ((cancelTask != null) && (cancelTask.isCancel())) {
      ncd.close();
      return null;
    }

    if (disabled) return ncd;

    // keep in cache
    boolean needCleanup;
    synchronized (lock) {
      cache.add( new CacheElement( ncd, cacheName, factory));
      needCleanup = (cache.size() > maxElements);
    }
    if (needCleanup)
      timer.schedule(new CleanupTask(), 10); // start up in 10 msec

    return ncd;
  }

  /**
   * Release the file. This unlocks it, updates its lastAccessed date.
   * Normally you need not call this, just close the dataset as usual.
   * @param ncd release this file.
   * @throws IOException if file not in cache.
   */
  static public void release(NetcdfFile ncd) throws IOException {
    if (ncd == null) return;

    if (disabled) {  // // should not happen
      ncd.setCached( 0); // prevent infinite loops
      ncd.close();
      return;
    }

    String cacheName = ncd.getCacheName();

    synchronized (lock) {
      for (int i = 0; i < cache.size(); i++) {
        CacheElement elem =  (CacheElement) cache.get(i);
        if (elem.ncd == ncd) {
          if (!elem.isLocked)
            logger.warn("NetcdfDatasetCache.release "+cacheName+" not locked");
        //if (elem.cacheName.equals(cacheName) && elem.isLocked) {
          elem.isLocked = false;
          elem.lastAccessed = System.currentTimeMillis();
          elem.countAccessed++;
          if (logger.isDebugEnabled()) logger.debug("NetcdfDatasetCache release "+cacheName);
          return;
        }
      }
    }
    throw new IOException("NetcdfDatasetCache.release does not have in cache = "+ cacheName);
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
      if (elem.ncd.isCached() != 2)
        logger.warn("NetcdfDatasetCache file cache flag not set "+elem.cacheName);

      try {
        elem.ncd.setCached(0);
        elem.ncd.close();
        elem.ncd = null; // help the gc
      } catch (IOException e) {
        logger.error("NetcdfDatasetCache.close failed on "+elem.cacheName);
      }
    }

    long took = System.currentTimeMillis() - start;
    if (logger.isDebugEnabled())
      logger.debug("NetcdfDatasetCache.cleanup had= "+ size+" deleted= "+count+" took="+took+" msec");
    if (count < need2delete)
      logger.warn("NetcdfDatasetCache.cleanup couldnt delete enough for minimum= "+ minElements+" actual= "+cache.size());
  }

  /**
   * Get the files in the cache. For debugging/status only, do not change!
   * @return List of NetcdfDatasetCache.CacheElement
   */
  static public List getCache() {
    return (cache == null) ? new ArrayList() : new ArrayList(cache);
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
        logger.warn("NetcdfDatasetCache close locked file= "+elem);
      if (elem.ncd.isCached() != 2)
        logger.warn("NetcdfDatasetCache file cache flag not set= "+elem);
      //System.out.println("NetcdfDatasetCache close file= "+elem);

      try {
        elem.ncd.setCached(0);
        elem.ncd.close();
        elem.ncd = null; // help the gc
      } catch (IOException e) {
        logger.error("NetcdfDatasetCache.close failed on "+elem);
      }
    }

  }

  /**
   * Public as an artifact.
   * This tracks the elements in the cache.
   * Do not modify.
   */
  static public class CacheElement implements Comparable {
    public String cacheName;
    public NetcdfDataset ncd;
    public NetcdfDatasetFactory factory;

    public boolean isLocked = true;
    public int countAccessed = 0;
    public long lastAccessed = 0;

    CacheElement(NetcdfDataset ncd, String cacheName, NetcdfDatasetFactory factory) {
      this.cacheName = cacheName;
      this.ncd = ncd;
      this.factory = factory;
      ncd.setCached( 2);
      ncd.setCacheName( cacheName);
      if (logger.isDebugEnabled()) logger.debug("NetcdfDatasetCache add to cache "+cacheName);
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