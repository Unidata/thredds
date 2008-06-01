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
package ucar.nc2;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;

/**
 * Keep cache of open NetcdfFile objects, for performance.
 * The NetcdfFile object typically contains a RandomAccessFile object that wraps a system resource like a file handle.
 * These are left open when the NetcdfFile is in the cache. The maximum number of these is bounded, though not strictly.
 * A cleanup routine reduces cache size to a minimum number. This cleanup is called periodically in a background thread,
 * and also when the maximum cache size is reached.
 * <ol>
 * <li>The NetcdfFile object must not be modified.
 * <li>The hashKey must uniquely define the NetcdfFile object.
 * <li>The location must be usable in the NetcdfFileFactory, typically NetcdfFile.open()
 * <li>If the NetcdfFile is acquired from the cache (ie already open), ncfile.sync() is called on it.
 * </ol>
 * Normal usage is through the NetcdfDataset interface:
 * <pre>
 * NetcdfDataset.initNetcdfFileCache(...);
 * NetcdfFile ncfile = null;
 * try {
 *   ncfile = NetcdfDataset.acquireFile(location, cancelTask);
 *   ...
 * } finally {
 *   if (ncfile != null) ncfile.close();
 * }
 * </pre>
 * All methods are thread safe.
 * Cleanup is done automatically in a background thread, using LRU.
 * <p/>
 * Make sure you call NetcdfFileCache.exit() when exiting the program, in order to shut down the cleanup thread.
 *
 * @author caron
 * @since May 30, 2008
 */
public class NetcdfFileCache {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFileCache.class);
  static private ScheduledExecutorService exec;

  /**
   * You must call exit() to shut down the background threads in order to get a clean process shutdown.
   */
  static public void exit() {
    if (exec != null)
      exec.shutdown();
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  private final NetcdfFileFactory defaultFactory;
  private final int maxElements, minElements;

  private final ConcurrentHashMap<Object, CacheElement> cache;
  private final AtomicInteger counter = new AtomicInteger();
  private final AtomicBoolean hasScheduled = new AtomicBoolean(false);

  private boolean disabled = false;

  // debugging and stats
  private final AtomicInteger hits = new AtomicInteger();
  private final AtomicInteger miss = new AtomicInteger();

  /**
   * Constructor, which also enables.
   *
   * @param defaultFactory      default factory to create a CacheObject
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param period              (secs) do periodic cleanups every this number of seconds.
   */
  public NetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int period, NetcdfFileFactory defaultFactory) {
    this.minElements = minElementsInMemory;
    this.maxElements = maxElementsInMemory;
    this.defaultFactory = defaultFactory;

    cache = new ConcurrentHashMap<Object, CacheElement>(2 * maxElements, 0.75f, 8);
    disabled = false;

    if (exec == null)
      exec = Executors.newSingleThreadScheduledExecutor();

    exec.scheduleAtFixedRate(new CleanupTask(), period, period, TimeUnit.SECONDS);
  }

  /**
   * Disable the cache, and force release all files.
   * You must still call NetcdfFileCache.exit() before exiting the application.
   */
  public void disable() {
    this.disabled = true;
    clearCache(true);
  }

  /**
   * Enable the cache, with the current set of parameters.
   */
  public void enable() {
    this.disabled = false;
  }

  /**
   * Acquire a NetcdfFile, and lock it so no one else can use it.
   * call NetcdfFile.close() when done.
   *
   * @param location   file location, also used as the cache name, will be passed to the NetcdfFileFactory
   * @param cancelTask user can cancel, ok to be null.
   * @return NetcdfFile corresponding to location.
   * @throws IOException on error
   */
  public NetcdfFile acquire(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquire(location, location, -1, cancelTask, null, null);
  }

  /**
   * Acquire a NetcdfFile from the cache, and lock it so no one else can use it.
   * If not already in cache, open it with NetcdfFile.open(), and put in cache.
   * <p/>
   * You should call NetcdfFile.close() when done, (rather than
   * NetcdfFileCache.release() directly) and the file is then released instead of closed.
   * <p/>
   * If cache size goes over maxElement, then immediately (actually in 100 msec) schedule a cleanup in a background thread.
   * This means that the cache should never get much larger than maxElement, unless you have them all locked.
   *
   * @param location    file location, also used as the cache name, will be passed to the NetcdfFileFactory
   * @param hashKey     unique key for thius file. If null, the location will be used
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  user can cancel, ok to be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @param factory     use this factory to open the file; if null, use NetcdfFile.open
   * @return NetcdfFile corresponding to location.
   * @throws IOException on error
   */
  public NetcdfFile acquire(String location, Object hashKey, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject, NetcdfFileFactory factory) throws IOException {
    if (null == hashKey) hashKey = location;
    NetcdfFile ncfile = acquireCacheOnly(hashKey);
    if (ncfile != null) {
      hits.incrementAndGet();
      return ncfile;
    }
    miss.incrementAndGet();

    // open the file
    if (factory == null)
      ncfile = defaultFactory.open(location, buffer_size, cancelTask, spiObject);
    else
      ncfile = factory.open(location, buffer_size, cancelTask, spiObject);

    // user may have canceled
    if ((cancelTask != null) && (cancelTask.isCancel())) {
      if (ncfile != null) ncfile.close();
      return null;
    }

    if (disabled) return ncfile;

    // see if cache element already exists
    // must synchronize to avoid race condition with other puts; gets are ok
    CacheElement elem;
    synchronized (cache) {
      elem = cache.get(hashKey);
      if (elem == null) cache.put(hashKey, new CacheElement(ncfile, hashKey)); // new element
    }

    // already exists, add to list
    if (elem != null) {
      synchronized (elem) {
        elem.addFile(ncfile); // add to existing list
      }
    }

    int count = counter.incrementAndGet();
    if (count > maxElements) {
      if (hasScheduled.compareAndSet(false, true))
        exec.schedule(new CleanupTask(), 100, TimeUnit.MILLISECONDS); // immediate cleanup in 100 msec
    }

    return ncfile;
  }

  /**
   * Try to find a file in the cache.
   *
   * @param hashKey used as the key.
   * @return file if its in the cache, null otherwise.
   */
  private NetcdfFile acquireCacheOnly(Object hashKey) {
    if (disabled) return null;
    NetcdfFile ncfile = null;

    // see if its in the cache
    CacheElement elem = cache.get(hashKey);
    if (elem != null) {
      synchronized (elem) {
        for (CacheElement.CacheFile file : elem.list) {
          if (!file.isLocked) {
            file.isLocked = true;
            ncfile = file.ncfile;
            break;
          }
        }
      }
    }

    // sync the file when you want to use it again : needed for grib growing index, netcdf-3 record growing, etc
    if (ncfile != null) {
      try {
        ncfile.sync();
        if (log.isDebugEnabled()) log.debug("NetcdfFileCache.aquire from cache " + ncfile.getLocation());
      } catch (IOException e) {
        log.error("NetcdfFileCache.synch failed on " + ncfile.getLocation() + " " + e.getMessage());
      }
    }

    return ncfile;
  }

  /**
   * Release the file. This unlocks it, updates its lastAccessed date.
   * Normally you need not call this, just close the file as usual.
   *
   * @param ncfile release this file.
   * @param cacheKey the file was stored with this hash key
   * @throws IOException if file not in cache.
   */
  public void release(NetcdfFile ncfile, Object cacheKey) throws IOException {
    if (ncfile == null) return;

    if (disabled) {
      ncfile.setFileCache( null, null); // prevent infinite loops
      ncfile.close();
      return;
    }

    // see if its in the cache
    CacheElement elem = cache.get(cacheKey);
    if (elem != null) {
      synchronized (elem) {
        for (CacheElement.CacheFile file : elem.list) {
          if (file.ncfile == ncfile) {
            if (!file.isLocked)
              log.warn("NetcdfFileCache.release " + ncfile.getLocation() + " not locked");
            file.isLocked = false;
            file.lastAccessed = System.currentTimeMillis();
            file.countAccessed++;
            if (log.isDebugEnabled()) log.debug("NetcdfFileCache.release " + ncfile.getLocation());
            return;
          }
        }
      }
    }
    throw new IOException("NetcdfFileCache.release does not have file in cache = " + ncfile.getLocation());
  }

  // debug
  Map<Object, CacheElement> getCache() {
    return cache;
  }

  /**
   * Remove all cache entries.
   *
   * @param force if true, remove them even if they are currently locked.
   */
  public void clearCache(boolean force) {
    ArrayList<CacheElement.CacheFile> deleteList;

    synchronized (cache) {
      Collection<CacheElement> all = cache.values();
      deleteList = new ArrayList<CacheElement.CacheFile>(2 * all.size());

      if (force) {
        cache.clear(); // deletes everything from the cache
        counter.set(0);
        for (CacheElement elem : all) {
          synchronized (elem) {
            for (CacheElement.CacheFile file : elem.list)
              deleteList.add(file);  // add everything to the delete list
          }
        }

      } else {

        for (CacheElement elem : all) {
          boolean keep = false; // keep if there are any locked files
          synchronized (elem) {
            for (CacheElement.CacheFile file : elem.list) {
              if (file.isLocked)
                keep = true;
              else
                deleteList.add(file); // add all unlocked files to the delete list
            }

            if (keep) {
              Iterator<CacheElement.CacheFile> iter = elem.list.iterator();
              while (iter.hasNext()) {
                CacheElement.CacheFile file = iter.next();
                if (!file.isLocked)
                  iter.remove();
              }

            } else {
              cache.remove( elem.hashKey); // remove entire cache element
            }
          }
        }
      }
    }

    // close all files in oldcache
    for (CacheElement.CacheFile file : deleteList) {
      if (file.isLocked)
        log.warn("NetcdfFileCache close locked file= " + file);
      counter.decrementAndGet();

      try {
        file.ncfile.setFileCache( null, null);
        file.ncfile.close();
        file.ncfile = null; // help the gc
      } catch (IOException e) {
        log.error("NetcdfFileCache.close failed on " + file);
      }
    }
    log.debug("*NetcdfFileCache.clearCache force= " + force + " deleted= " + deleteList.size() + " left=" + counter.get());
    System.out.println("\n*NetcdfFileCache.clearCache force= " + force + " deleted= " + deleteList.size() + " left=" + counter.get());
  }

  public void showCache(Formatter format) {
    ArrayList<CacheElement.CacheFile> allFiles = new ArrayList<CacheElement.CacheFile>(counter.get());
    for (CacheElement elem : cache.values()) {
      synchronized (elem) {
        allFiles.addAll(elem.list);
      }
    }
    Collections.sort(allFiles); // sort so oldest are on top

    for (CacheElement.CacheFile file : allFiles) {
      format.format("  %s\n", file);
    }
  }

  public void showStats(Formatter format) {
    format.format("  hits= %d miss= %d nfiles= %d elems= %d\n", hits.get(), miss.get(), counter.get(), cache.values().size());
  }

  /**
   * Cleanup the cache, bringing it down to minimum number.
   * Will close the LRU (least recently used) ones first. Will not close locked files.
   * Normally this is done in a background thread, you dont need to call.
   */
  private void cleanup() {
    if (disabled) return;

    int size = counter.get();
    if (size <= minElements) return;

    ArrayList<CacheElement.CacheFile> deleteList;

    synchronized (cache) {
      // add unlocked files to the all list
      ArrayList<CacheElement.CacheFile> allFiles = new ArrayList<CacheElement.CacheFile>(counter.get());
      for (CacheElement elem : cache.values()) {
        synchronized (elem) {
          for (CacheElement.CacheFile file : elem.list)
            if (!file.isLocked) allFiles.add(file);
        }
      }
      Collections.sort(allFiles); // sort so oldest are on top

      // take oldest ones and put on delete list

      int need2delete = size - minElements;
      int minDelete = size - maxElements;
      deleteList = new ArrayList<CacheElement.CacheFile>(need2delete);

      int count = 0;
      Iterator<CacheElement.CacheFile> iter = allFiles.iterator();
      while (iter.hasNext() && (count < need2delete)) {
        CacheElement.CacheFile file = iter.next();
        file.remove(); // remove from the containing element
        deleteList.add(file);
        count++;
      }
      if (count < minDelete)
        log.warn("NetcdfFileCache.cleanup couldnt delete enough to keep under the maximum= " + maxElements + " due to locked files; currently at = " + (size - count));

      //  remove empty cache elements
      for (CacheElement elem : cache.values()) {
        synchronized (elem) {
          if (elem.list.size() == 0)
            cache.remove(elem.hashKey);
        }
      }
    }

    // now actually close the files
    long start = System.currentTimeMillis();
    for (CacheElement.CacheFile file : deleteList) {
      counter.decrementAndGet();

      try {
        file.ncfile.setFileCache( null, null);
        file.ncfile.close();
        file.ncfile = null; // help the gc
      } catch (IOException e) {
        log.error("NetcdfFileCache.close failed on " + file.getCacheName());
      }
    }

    long took = System.currentTimeMillis() - start;
    log.debug("NetcdfFileCache.cleanup had= " + size + " deleted= " + deleteList.size() + " took=" + took + " msec");
    System.out.println("\n*NetcdfFileCache.cleanup started with= " + size + " deleted= " + deleteList.size() + " took=" + took + " msec");
  }

  class CacheElement {
    Object hashKey;
    List<CacheFile> list = new LinkedList<CacheFile>(); // may have multiple copies of the same file opened
                                                        //  guarded by CacheElement object lock
    CacheElement(NetcdfFile ncfile, Object hashKey) {
      this.hashKey = hashKey;
      list.add(new CacheFile(ncfile));
      if (log.isDebugEnabled()) log.debug("NetcdfFileCache add to cache " + ncfile.getLocation());
    }

    CacheFile addFile(NetcdfFile ncfile) {
      CacheFile file = new CacheFile(ncfile);
      synchronized (this) {
        list.add( file);
      }
      return file;
    }

    public String toString() {
      return hashKey + " count=" + list.size();
    }
    
    class CacheFile implements Comparable<CacheFile> {
      NetcdfFile ncfile;
      boolean isLocked = true;
      int countAccessed = 1;
      long lastAccessed = 0;

      CacheFile(NetcdfFile ncfile) {
        this.ncfile = ncfile;
        this.lastAccessed = System.currentTimeMillis();

        ncfile.setFileCache( NetcdfFileCache.this, hashKey);
        if (log.isDebugEnabled()) log.debug("NetcdfFileCache add to cache " + ncfile.getLocation());
      }

      String getCacheName() { return ncfile.getLocation(); }

      void remove() {
        synchronized (CacheElement.this) {
          list.remove(this);
        }
      }

      public String toString() {
        return isLocked + " " + ncfile.getLocation() + " " + countAccessed + " " + new Date(lastAccessed);
      }

      public int compareTo(CacheFile o) {
        return (int) (lastAccessed - o.lastAccessed);
      }
    }
  }

  private class CleanupTask implements Runnable {
    public void run() {
      cleanup();
    }
  }

}