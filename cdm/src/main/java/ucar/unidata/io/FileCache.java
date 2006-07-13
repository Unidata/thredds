// $Id: FileCache.java 64 2006-07-12 22:30:50Z edavis $
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

package ucar.unidata.io;

import java.util.*;
import java.io.IOException;


/**
 * Keep cache of open RandomAccessFile, for performance.
 * <pre>
  RandomAccessFile raf = null;
  try {
    RandomAccessFile raf = FileCache.acquire(location, cancelTask);
    ...
  } finally {
    FileCache.release( raf)
  }
 </pre>
 *
 *
 * <p>
 * Library ships with cache disabled.
 * If you want to use, call init() and make sure you call exit() when exiting program.
 * All methods are thread safe.
 * Cleanup is done automatically in a background thread, using LRU.
 *
 * @author jcaron
 */
public class FileCache {
   static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileCache.class);
    //For now, use the LogUtil.LogCategory logger so the build don't break
//    static private ucar.unidata.util.LogUtil.LogCategory log = new ucar.unidata.util.LogUtil.LogCategory(FileCache.class.getName());
  //static private org.apache.commons.logging.Log log = LogFactory.getLog( FileCache.class);
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

  /** Disable use of the cache. Call init() to start again.
   *  Generally call this before any possible use.
   */
  static public void disable() {
    disabled = true;
    if (timer != null) timer.cancel();
    timer = null;
    if ((cache != null) && cache.size() > 0)
      clearCache( false);
  }

  /** You must call exit() to shut down the background timer in order to get a clean process shutdpwn. */
  static public void exit() {
    disabled = true;
    if (timer != null) timer.cancel();
    timer = null;
    if ((cache != null) && cache.size() > 0)
      clearCache( true);
  }

  /**
   * Try to find a file in the cache.
   * @param location file location is used as the key.
   * @return file if its in the cache, null otherwise.
   */
  static public RandomAccessFile acquireCacheOnly(String location) {
    if (disabled) return null;
    if (cache == null) init();

    // see if its in the cache
    RandomAccessFile raf = null;
    synchronized (lock) {
      for (int i = 0; i < cache.size(); i++) {
        CacheElement elem =  (CacheElement) cache.get(i);
        if (elem.location.equals(location) && !elem.isLocked) {
          elem.isLocked = true;
          raf = elem.raf;
          break;
        }
      }
    }

    // synch the file when you want to use it again : needed for grib growing index
    if (raf != null) {
      try {
        raf.synch();
      } catch (IOException e) {
	  log.error("FileCache.synch failed on "+location+" "+e.getMessage());
      }
    }

    return raf;
  }

  static public RandomAccessFile acquire(String location) throws IOException {
    // see if its in the cache
    RandomAccessFile raf = acquireCacheOnly( location);
    if (raf != null) return raf;

    // open the file
    raf = new RandomAccessFile(location, "r");
    if (disabled) return raf;

    // keep in cache
    boolean needCleanup;
    synchronized (lock) {
      cache.add( new CacheElement( raf));
      needCleanup = (cache.size() > maxElements);
    }
    if (needCleanup)
      timer.schedule(new CleanupTask(), 10); // start up in 10 msec

    return raf;
  }

  /**
   * Release the file. This unlocks it, updates its lastAccessed date.
   * @param raf release this file.
   * @throws IOException if file not in cache.
   */
  static public void release(RandomAccessFile raf) throws IOException {
    if (raf == null) return;

    if (disabled) {
      raf.close();
      return;
    }

    String location = raf.getLocation();

    synchronized (lock) {
      for (int i = 0; i < cache.size(); i++) {
        CacheElement elem =  (CacheElement) cache.get(i);
        if (elem.location.equals(location) && elem.isLocked) {
          elem.isLocked = false;
          elem.lastAccessed = System.currentTimeMillis();
          elem.countAccessed++;
          return;
        }
      }
    }
    throw new IOException("FileCache.release does not have in cache = "+ location);
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
      if (!elem.raf.isCached())
	  log.warn("FileCache file cache flag not set "+elem.location);

      try {
        elem.raf.setCached(false);
        elem.raf.close();
        elem.raf = null; // help the gc
      } catch (IOException e) {
	  log.error("FileCache.close failed on "+elem.location);
      }
    }

    long took = System.currentTimeMillis() - start;
    if (log.isDebugEnabled()) log.debug("FileCache.cleanup had= "+ size+" deleted= "+count+" took="+took+" msec");
    if (count < need2delete)
      log.warn("FileCache.cleanup couldnt delete enough for minimum= "+ minElements+" actual= "+cache.size());
  }

  /**
   * Get the files in the cache. For debugging/status only, do not change!
   * @return List of FileCache.CacheElement
   */
  static public List getCache() {
    return (cache == null) ? new ArrayList() : new ArrayList(cache);
  }

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
        log.warn("FileCache close locked file= "+elem);
      if (!elem.raf.isCached())
        log.warn("FileCache file cache flag not set= "+elem);
      //System.out.println("FileCache close file= "+elem);

      try {
        elem.raf.setCached(false);
        elem.raf.close();
        elem.raf = null; // help the gc
      } catch (IOException e) {
        log.error("FileCache.close failed on "+elem);
      }
    }

  }

  /**
   * This tracks the elements in the cache.
   * Do not modify.
   */
  static public class CacheElement implements Comparable {
    public String location;
    public ucar.unidata.io.RandomAccessFile raf;
    public boolean isLocked = true;
    public int countAccessed = 0;
    public long lastAccessed = 0;

    CacheElement(RandomAccessFile raf) {
      this.location = raf.getLocation();
      this.raf = raf;
      raf.setCached( true);
    }

    public String toString() {
      return location+" "+isLocked+" "+countAccessed+" "+new Date(lastAccessed);
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
