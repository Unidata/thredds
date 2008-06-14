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

import net.jcip.annotations.ThreadSafe;
import net.jcip.annotations.GuardedBy;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;

import ucar.nc2.util.CancelTask;

/**
 * Keep cache of open FileCacheable objects, for example NetcdfFile.
 * The FileCacheable object typically contains a RandomAccessFile object that wraps a system resource like a file handle.
 * These are left open when the NetcdfFile is in the cache. The maximum number of these is bounded, though not strictly.
 * A cleanup routine reduces cache size to a minimum number. This cleanup is called periodically in a background thread,
 * and also when the maximum cache size is reached.
 * <ol>
 * <li>The FileCacheable object must not be modified.
 * <li>The hashKey must uniquely define the FileCacheable object.
 * <li>The location must be usable in the FileCacheableFactory.
 * <li>If the FileCacheable is acquired from the cache (ie already open), ncfile.sync() is called on it.
 * <li>Make sure you call NetcdfDataset.shutdown() when exiting the program, in order to shut down the cleanup thread.
 * </ol>
 * Normal usage is through the NetcdfDataset interface:
 * <pre>
 * NetcdfDataset.initNetcdfFileCache(...); // on application startup
 * ...
 * NetcdfFile ncfile = null;
 * try {
 *   ncfile = NetcdfDataset.acquireFile(location, cancelTask);
 *   ...
 * } finally {
 *   if (ncfile != null) ncfile.close();
 * }
 * ...
 * NetcdfDataset.shutdown();  // when terminating the application
 * </pre>
 * All methods are thread safe.
 * Cleanup is done automatically in a background thread, using LRU algorithm.
 *
 * @author caron
 * @since May 30, 2008
 */

@ThreadSafe
public class FileCache {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileCache.class);
  static private ScheduledExecutorService exec;
  static private boolean debugPrint = true;

  /**
    * You must call shutdown() to shut down the background threads in order to get a clean process shutdown.
    */
   static public void shutdown() {
     if (exec != null)
       exec.shutdown();
    exec = null;
   }

   /////////////////////////////////////////////////////////////////////////////////////////

  private final int maxElements, minElements;

  private final ConcurrentHashMap<Object, CacheElement> cache;
  private final ConcurrentHashMap<Object, CacheElement.CacheFile> files;
  private final AtomicInteger counter = new AtomicInteger();
  private final AtomicInteger cleanups = new AtomicInteger();
  private final AtomicBoolean hasScheduled = new AtomicBoolean(false);
  private final AtomicBoolean disabled = new AtomicBoolean(false);

  // debugging and stats
  private final AtomicInteger hits = new AtomicInteger();
  private final AtomicInteger miss = new AtomicInteger();

  /**
   * Constructor, which also enables.
   *
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param period              (secs) do periodic cleanups every this number of seconds.
   */
  public FileCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    this.minElements = minElementsInMemory;
    this.maxElements = maxElementsInMemory;

    cache = new ConcurrentHashMap<Object, CacheElement>(2 * maxElements, 0.75f, 8);
    files = new ConcurrentHashMap<Object, CacheElement.CacheFile>(4 * maxElements, 0.75f, 8);

    if (exec == null)
      exec = Executors.newSingleThreadScheduledExecutor();

    exec.scheduleAtFixedRate(new CleanupTask(), period, period, TimeUnit.SECONDS);
  }

  /**
   * Disable the cache, and force release all files.
   * You must still call shutdown() before exiting the application.
   */
  public void disable() {
    this.disabled.set(true);
    clearCache(true);
  }

  /**
   * Enable the cache, with the current set of parameters.
   */
  public void enable() {
    this.disabled.set(false);
  }

  /**
   * Acquire a FileCacheable, and lock it so no one else can use it.
   * call FileCacheable.close() when done.
   *
   * @param factory     use this factory to open the file; may not be null
   * @param location   file location, also used as the cache name, will be passed to the NetcdfFileFactory
   * @param cancelTask user can cancel, ok to be null.
   * @return NetcdfFile corresponding to location.
   * @throws IOException on error
   */
  public FileCacheable acquire(FileFactory factory, String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquire(factory, location, location, -1, cancelTask, null);
  }

  /**
   * Acquire a FileCacheable from the cache, and lock it so no one else can use it.
   * If not already in cache, open it the FileFactory, and put in cache.
   * <p/>
   * Call FileCacheable.close() when done, (rather than FileCach.release() directly) and the file is then released instead of closed.
   * <p/>
   * If cache size goes over maxElement, then immediately (actually in 100 msec) schedule a cleanup in a background thread.
   * This means that the cache should never get much larger than maxElement, unless you have them all locked.
   *
   * @param factory     use this factory to open the file if not in the cache; may not be null
   * @param hashKey     unique key for this file. If null, the location will be used
   * @param location    file location, msy also used as the cache name, will be passed to the NetcdfFileFactory
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  user can cancel, ok to be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return FileCacheable corresponding to location.
   * @throws IOException on error
   */
  public FileCacheable acquire(FileFactory factory, Object hashKey,
          String location, int buffer_size, CancelTask cancelTask, Object spiObject) throws IOException {

    if (null == hashKey) hashKey = location;
    FileCacheable ncfile = acquireCacheOnly(hashKey);
    if (ncfile != null) {
      hits.incrementAndGet();
      return ncfile;
    }
    miss.incrementAndGet();

    // open the file
    ncfile = factory.open(location, buffer_size, cancelTask, spiObject);

    // user may have canceled
    if ((cancelTask != null) && (cancelTask.isCancel())) {
      if (ncfile != null) ncfile.close();
      return null;
    }

    if (disabled.get()) return ncfile;

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
  private FileCacheable acquireCacheOnly(Object hashKey) {
    if (disabled.get()) return null;
    FileCacheable ncfile = null;

    // see if its in the cache
    CacheElement elem = cache.get(hashKey);
    if (elem != null) {
      synchronized (elem) { // synch in order to travers the list
        for (CacheElement.CacheFile file : elem.list) {
          if (file.isLocked.compareAndSet(false, true)) {
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
        if (log.isDebugEnabled()) log.debug("FileCache.aquire from cache " + hashKey);
        if (debugPrint) System.out.println("FileCache.aquire from cache " + hashKey);
      } catch (IOException e) {
        log.error("FileCache.synch failed on " + ncfile.getLocation() + " " + e.getMessage());
      }
    }

    return ncfile;
  }

  /**
   * Release the file. This unlocks it, updates its lastAccessed date.
   * Normally you need not call this, just close the file as usual.
   *
   * @param ncfile release this file.
   * @throws IOException if file not in cache.
   */
  public void release(FileCacheable ncfile) throws IOException {
    if (ncfile == null) return;

    if (disabled.get()) {
      ncfile.setFileCache( null); // prevent infinite loops
      ncfile.close();
      return;
    }

    // find it in the file cache
    CacheElement.CacheFile file = files.get(ncfile);
    if (file != null) {
      if (!file.isLocked.get())
        log.warn("FileCache.release " + ncfile.getLocation() + " not locked");
      file.lastAccessed = System.currentTimeMillis();
      file.countAccessed++;
      file.isLocked.set(false);
      if (log.isDebugEnabled()) log.debug("FileCache.release " + ncfile.getLocation());
      return;
    }
    throw new IOException("FileCache.release does not have file in cache = " + ncfile.getLocation());
  }

  /**
   * debug only, do not use
   */
  Map<Object, CacheElement> getCache() {
    return cache;
  }

  /**
   * Remove all cache entries.
   *
   * @param force if true, remove them even if they are currently locked.
   */
  public void clearCache(boolean force) {
    List<CacheElement.CacheFile> deleteList = new ArrayList<CacheElement.CacheFile>(2*cache.size());

    if (force) {
      cache.clear(); // deletes everything from the cache
      deleteList.addAll(files.values());  // add everything to the delete list
      files.clear();
      counter.set(0);

    } else {

      // add unlocked files to the delete list, remove from files hash
      Iterator<CacheElement.CacheFile> iter = files.values().iterator();
      while (iter.hasNext()) {
        CacheElement.CacheFile file = iter.next();
        if (file.isLocked.compareAndSet(false, true)) {
          file.remove(); // remove from the containing CacheElement
          deleteList.add(file);
          iter.remove();
        }
      }

      // remove empty cache elements
      synchronized (cache) {
        for (CacheElement elem : cache.values()) {
          synchronized (elem) {
            if (elem.list.size() == 0)
              cache.remove(elem.hashKey);
          }
        }
      }
  }

    // close all files in deleteList
    for (CacheElement.CacheFile file : deleteList) {
      if (force && file.isLocked.get())
        log.warn("FileCache force close locked file= " + file);
      counter.decrementAndGet();

      try {
        file.ncfile.setFileCache( null);
        file.ncfile.close();
        file.ncfile = null; // help the gc
      } catch (IOException e) {
        log.error("FileCache.close failed on " + file);
      }
    }
    log.debug("*FileCache.clearCache force= " + force + " deleted= " + deleteList.size() + " left=" + counter.get());
    //System.out.println("\n*NetcdfFileCache.clearCache force= " + force + " deleted= " + deleteList.size() + " left=" + counter.get());
  }

  /**
   * Show individual cache entries, add to formatter.
   * @param format add to this
   */
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

  /**
   * Add stat report (hits, misses, etc) to formatter.
   * @param format add to this
   */
  public void showStats(Formatter format) {
    format.format("  hits= %d miss= %d nfiles= %d elems= %d\n", hits.get(), miss.get(), counter.get(), cache.values().size());
  }

  /**
   * Cleanup the cache, bringing it down to minimum number.
   * Will close the LRU (least recently used) ones first. Will not close locked files.
   * Normally this is done in a background thread, you dont need to call.
   */
  private void cleanup() {
    if (disabled.get()) return;

    int size = counter.get();
    if (size <= minElements) return;

    cleanups.incrementAndGet();

    // add unlocked files to the all list
    ArrayList<CacheElement.CacheFile> allFiles = new ArrayList<CacheElement.CacheFile>(counter.get());
    for (CacheElement.CacheFile file : files.values()) {
      if (!file.isLocked.get()) allFiles.add(file);
    }
    Collections.sort(allFiles); // sort so oldest are on top

    // take oldest ones and put on delete list
    int need2delete = size - minElements;
    int minDelete = size - maxElements;
    ArrayList<CacheElement.CacheFile> deleteList = new ArrayList<CacheElement.CacheFile>(need2delete);

    int count = 0;
    Iterator<CacheElement.CacheFile> iter = allFiles.iterator();
    while (iter.hasNext() && (count < need2delete)) {
      CacheElement.CacheFile file = iter.next();
      if (file.isLocked.compareAndSet(false, true)) { // lock it so it isnt used anywhere else
        file.remove(); // remove from the containing element
        deleteList.add(file);
        count++;
      }
    }
    if (count < minDelete)
      log.warn("FileCache.cleanup couldnt delete enough to keep under the maximum= " + maxElements + " due to locked files; currently at = " + (size - count));

    // remove empty cache elements
    synchronized (cache) {
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
      files.remove(file.ncfile);
      try {
        file.ncfile.setFileCache( null);
        file.ncfile.close();
        file.ncfile = null; // help the gc
      } catch (IOException e) {
        log.error("FileCache.close failed on " + file.getCacheName());
      }
    }

    long took = System.currentTimeMillis() - start;
    log.debug("FileCache.cleanup had= " + size + " deleted= " + deleteList.size() + " took=" + took + " msec");
    //System.out.println("\n*NetcdfFileCache.cleanup started with= " + size + " deleted= " + deleteList.size() + " took=" + took + " msec");
  }

  class CacheElement {
    @GuardedBy("this") List<CacheFile> list = new LinkedList<CacheFile>(); // may have multiple copies of the same file opened
    final Object hashKey;

    CacheElement(FileCacheable ncfile, Object hashKey) {
      this.hashKey = hashKey;
      CacheFile file = new CacheFile(ncfile);
      list.add(file);
      files.put(ncfile, file);
      if (log.isDebugEnabled()) log.debug("CacheElement add to cache " + hashKey);
    }

    CacheFile addFile(FileCacheable ncfile) {
      CacheFile file = new CacheFile(ncfile);
      synchronized (this) {
        list.add( file);
      }
      files.put(ncfile, file);
      return file;
    }

    public String toString() {
      return hashKey + " count=" + list.size();
    }

    class CacheFile implements Comparable<CacheFile> {
      FileCacheable ncfile; // actually final, but we null it out for gc
      final AtomicBoolean isLocked = new AtomicBoolean(true);
      int countAccessed = 1;
      long lastAccessed = 0;

      private CacheFile(FileCacheable ncfile) {
        this.ncfile = ncfile;
        this.lastAccessed = System.currentTimeMillis();

        ncfile.setFileCache( FileCache.this);

        if (log.isDebugEnabled()) log.debug("FileCache add to cache " + hashKey);
        if (debugPrint) System.out.println("FileCache add to cache " + hashKey);
      }

      String getCacheName() { return ncfile.getLocation(); }

      void remove() {
        synchronized (CacheElement.this) {
          list.remove(this);
        }
      }

      public String toString() {
        return isLocked + " " + hashKey + " " + countAccessed + " " + new Date(lastAccessed);
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