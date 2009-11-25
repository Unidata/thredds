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
  static private org.slf4j.Logger cacheLog = org.slf4j.LoggerFactory.getLogger("cacheLogger");
  static private ScheduledExecutorService exec;
  static boolean debug = false;
  static boolean debugPrint = false;
  static boolean debugCleanup = false;

  /**
   * You must call shutdown() to shut down the background threads in order to get a clean process shutdown.
   */
  static public void shutdown() {
    if (exec != null)
      exec.shutdown();
    exec = null;
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  private String name;
  private final int softLimit, minElements, hardLimit;

  private final ConcurrentHashMap<Object, CacheElement> cache; // unique files (by key, often = filename)
  private final ConcurrentHashMap<Object, CacheElement.CacheFile> files; // list of all files in the cache
  //private final AtomicInteger counter = new AtomicInteger(); // how many files in the cache
  private final AtomicBoolean hasScheduled = new AtomicBoolean(false); // a cleanup is scheduled
  private final AtomicBoolean disabled = new AtomicBoolean(false);  // cache is disabled

  // debugging and stats
  private final AtomicInteger cleanups = new AtomicInteger();  // how many cleanups
  private final AtomicInteger hits = new AtomicInteger();
  private final AtomicInteger miss = new AtomicInteger();

  /**
   * Constructor.
   *
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param period              (secs) do periodic cleanups every this number of seconds.
   */
  public FileCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    this("", minElementsInMemory, maxElementsInMemory, -1, period);
  }

  /**
   * Constructor.
   *
   * @param minElementsInMemory keep this number in the cache
   * @param softLimit           trigger a cleanup if it goes over this number.
   * @param hardLimit           if > 0, never allow more than this many elements. This causes a cleanup to be done in the calling thread.
   * @param period              if > 0, do periodic cleanups every this number of seconds.
   */
  public FileCache(int minElementsInMemory, int softLimit, int hardLimit, int period) {
    this("", minElementsInMemory, softLimit, hardLimit, period);
  }

  /**
   * Constructor.
   *
   * @param name                of file cache
   * @param minElementsInMemory keep this number in the cache
   * @param softLimit           trigger a cleanup if it goes over this number.
   * @param hardLimit           if > 0, never allow more than this many elements. This causes a cleanup to be done in the calling thread.
   * @param period              if > 0, do periodic cleanups every this number of seconds.
   */
  public FileCache(String name, int minElementsInMemory, int softLimit, int hardLimit, int period) {
    this.name = name;
    this.minElements = minElementsInMemory;
    this.softLimit = softLimit;
    this.hardLimit = hardLimit;

    cache = new ConcurrentHashMap<Object, CacheElement>(2 * softLimit, 0.75f, 8);
    files = new ConcurrentHashMap<Object, CacheElement.CacheFile>(4 * softLimit, 0.75f, 8);

    if (period > 0) {
      if (exec == null)
        exec = Executors.newSingleThreadScheduledExecutor();
      exec.scheduleAtFixedRate(new CleanupTask(), period, period, TimeUnit.SECONDS);
      cacheLog.debug("FileCache " + name + " cleanup every " + period + " secs");
    }
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
   * @param factory    use this factory to open the file; may not be null
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
   * @param location    file location, may also used as the cache name, will be passed to the NetcdfFileFactory
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
    if (cacheLog.isDebugEnabled()) cacheLog.debug("FileCache " + name + " acquire " + hashKey + " " + ncfile.getLocation());
    if (debugPrint) System.out.println("  FileCache " + name + " acquire " + hashKey + " " + ncfile.getLocation());

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

    // increment the number of files in the cache
    //int count = counter.incrementAndGet();

    // do we need a cleanup ??
    boolean needHard = false;
    boolean needSoft = false;
    synchronized (hasScheduled) {
      if (!hasScheduled.get()) {
        int count = files.size();
        if ((count > hardLimit) && (hardLimit > 0)) {
          needHard = true;
          hasScheduled.getAndSet(true); // tell other threads not to schedule another cleanup

        } else if ((count > softLimit)) { // && (softLimit > 0)) {
          hasScheduled.getAndSet(true); // tell other threads not to schedule another cleanup
          needSoft = true;
        }
      }
    }

    if (needHard) {
      if (debugCleanup) System.out.println("CleanupTask due to hard limit time=" + new Date().getTime()); // +" Thread="+Thread.currentThread().hashCode()
      cleanup(hardLimit);

    } else if (needSoft) {
      exec.schedule(new CleanupTask(), 100, TimeUnit.MILLISECONDS); // immediate cleanup in 100 msec
      if (debugCleanup) System.out.println("CleanupTask scheduled due to soft limit time=" + new Date());
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
    // also sets isClosed = false
    if (ncfile != null) {
     try {
        boolean changed = ncfile.sync();
       if (cacheLog.isDebugEnabled())
         cacheLog.debug("FileCache " + name + " aquire from cache " + hashKey + " " + ncfile.getLocation()+" changed = "+changed);
       if (debugPrint)
         System.out.println("  FileCache " + name + " aquire from cache " + hashKey + " " + ncfile.getLocation()+" changed = "+changed);
       } catch (IOException e) {
        log.error("FileCache " + name + " synch failed on " + ncfile.getLocation() + " " + e.getMessage());
        return null;
      }
    }

    return ncfile;
  }

  /**
   * Release the file. This unlocks it, updates its lastAccessed date.
   * Normally applications need not call this, just close the file as usual.
   *
   * @param ncfile release this file.
   * @throws IOException if file not in cache.
   */
  public void release(FileCacheable ncfile) throws IOException {
    if (ncfile == null) return;

    if (disabled.get()) {
      ncfile.setFileCache(null); // prevent infinite loops
      ncfile.close();
      return;
    }

    // find it in the file cache
    CacheElement.CacheFile file = files.get(ncfile); // using hashCode of the FileCacheable
    if (file != null) {
      if (!file.isLocked.get())
        cacheLog.warn("FileCache " + name + " release " + ncfile.getLocation() + " not locked");
      file.lastAccessed = System.currentTimeMillis();
      file.countAccessed++;
      file.isLocked.set(false);
      if (cacheLog.isDebugEnabled()) cacheLog.debug("FileCache " + name + " release " + ncfile.getLocation());
      if (debugPrint) System.out.println("  FileCache " + name + " release " + ncfile.getLocation());
      return;
    }
    throw new IOException("FileCache " + name + " release does not have file in cache = " + ncfile.getLocation());
  }

  // debug
  public String getInfo(FileCacheable ncfile) throws IOException {
    if (ncfile == null) return "";

    // find it in the file cache
    CacheElement.CacheFile file = files.get(ncfile);
    if (file != null) {
      return "File is in cache= "+file;
    }
    return "File not in cache";
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
  public synchronized void clearCache(boolean force) {
    List<CacheElement.CacheFile> deleteList = new ArrayList<CacheElement.CacheFile>(2 * cache.size());

    if (force) {
      cache.clear(); // deletes everything from the cache
      deleteList.addAll(files.values());  // add everything to the delete list
      files.clear();
      // counter.set(0);

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
        cacheLog.warn("FileCache " + name + " force close locked file= " + file);
      //counter.decrementAndGet();

      try {
        file.ncfile.setFileCache(null);
        file.ncfile.close();
        file.ncfile = null; // help the gc
      } catch (IOException e) {
        log.error("FileCache " + name + " close failed on " + file);
      }
    }
    cacheLog.debug("*FileCache " + name + " clearCache force= " + force + " deleted= " + deleteList.size() + " left=" + files.size());
    //System.out.println("\n*NetcdfFileCache.clearCache force= " + force + " deleted= " + deleteList.size() + " left=" + counter.get());
  }

  /**
   * Show individual cache entries, add to formatter.
   *
   * @param format add to this
   */
  public void showCache(Formatter format) {
    ArrayList<CacheElement.CacheFile> allFiles = new ArrayList<CacheElement.CacheFile>(files.size());
    for (CacheElement elem : cache.values()) {
      synchronized (elem) {
        allFiles.addAll(elem.list);
      }
    }
    Collections.sort(allFiles); // sort so oldest are on top

    format.format("FileCache %s (%d):%n", name, allFiles.size());
    format.format("isLocked  accesses lastAccess                   location %n");
    for (CacheElement.CacheFile file : allFiles) {
      String loc = file.ncfile != null ? file.ncfile.getLocation() : "null";
      format.format("%8s %9d %s %s %n", file.isLocked, file.countAccessed, new Date(file.lastAccessed), loc);
    }
  }

  public List<String> showCache() {
    ArrayList<CacheElement.CacheFile> allFiles = new ArrayList<CacheElement.CacheFile>(files.size());
    for (CacheElement elem : cache.values()) {
      synchronized (elem) {
        allFiles.addAll(elem.list);
      }
    }
    Collections.sort(allFiles); // sort so oldest are on top

    ArrayList<String> result = new ArrayList<String>(allFiles.size());
    for (CacheElement.CacheFile file : allFiles) {
      result.add(file.toString());
    }

    return result;
  }

  /**
   * Add stat report (hits, misses, etc) to formatter.
   *
   * @param format add to this
   */
  public void showStats(Formatter format) {
    format.format("  hits= %d miss= %d nfiles= %d elems= %d\n", hits.get(), miss.get(), files.size(), cache.values().size());
  }

  /**
   * Cleanup the cache, bringing it down to minimum number.
   * Will close the LRU (least recently used) ones first. Will not close locked files.
   * Normally this is done in a background thread, you dont need to call.
   * <p/>
   * We have to synchronize because of clearCache()
   */
  synchronized void cleanup(int maxElements) {
    if (disabled.get()) return;

    try {
      /* int size = counter.get();
      int fsize = files.size();
      if (debug && (size != fsize)) {
        log.warn("FileCache " + name + " counter " + size + " doesnt match files().size=" + fsize);
      } */

      int size = files.size();
      if (size <= minElements) return;

      cacheLog.debug(" FileCache " + name + " cleanup started at " + new Date() + " for cleanup maxElements=" + maxElements);
      if (debugCleanup)
        System.out.println(" FileCache " + name + "cleanup started at " + new Date() + " for cleanup maxElements=" + maxElements);

      cleanups.incrementAndGet();

      // add unlocked files to the all list
      ArrayList<CacheElement.CacheFile> allFiles = new ArrayList<CacheElement.CacheFile>(size + 10);
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
      if (count < minDelete) {
        cacheLog.warn("FileCache " + name + " cleanup couldnt remove enough to keep under the maximum= " + maxElements + " due to locked files; currently at = " + (size - count));
        if (debugCleanup)
          System.out.println("FileCache " + name + "cleanup couldnt remove enough to keep under the maximum= " + maxElements + " due to locked files; currently at = " + (size - count));
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

      // now actually close the files
      long start = System.currentTimeMillis();
      for (CacheElement.CacheFile file : deleteList) {
        //counter.decrementAndGet();
        files.remove(file.ncfile);
        try {
          file.ncfile.setFileCache(null);
          file.ncfile.close();
          file.ncfile = null; // help the gc
        } catch (IOException e) {
          log.error("FileCache " + name + " close failed on " + file.getCacheName());
        }
      }

      long took = System.currentTimeMillis() - start;
      cacheLog.debug(" FileCache " + name + " cleanup had= " + size + " removed= " + deleteList.size() + " took=" + took + " msec");
      if (debugCleanup)
        System.out.println(" FileCache " + name + "cleanup had= " + size + " removed= " + deleteList.size() + " took=" + took + " msec");

    } finally {
      // allow scheduling again
      hasScheduled.set(false);
    }
  }

  class CacheElement {
    @GuardedBy("this")
    List<CacheFile> list = new LinkedList<CacheFile>(); // may have multiple copies of the same file opened
    final Object hashKey;

    CacheElement(FileCacheable ncfile, Object hashKey) {
      this.hashKey = hashKey;
      CacheFile file = new CacheFile(ncfile);
      list.add(file);
      if (debug) {
        if (files.get(ncfile) != null)
          cacheLog.error("files already has " + hashKey + " " + name);
      }
      files.put(ncfile, file);
      if (cacheLog.isDebugEnabled()) cacheLog.debug("CacheElement add to cache " + hashKey + " " + name);
    }

    CacheFile addFile(FileCacheable ncfile) {
      CacheFile file = new CacheFile(ncfile);
      synchronized (this) {
        list.add(file);
      }
      if (debug) {
        if (files.get(ncfile) != null)
          cacheLog.error("files (2) already has " + hashKey + " " + name);
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

        ncfile.setFileCache(FileCache.this);

        if (cacheLog.isDebugEnabled()) cacheLog.debug("FileCache " + name + " add to cache " + hashKey);
        if (debugPrint) System.out.println("  FileCache " + name + " add to cache " + hashKey);
      }

      String getCacheName() {
        return ncfile.getLocation();
      }

      void remove() {
        synchronized (CacheElement.this) {
          if (!list.remove(this))
            cacheLog.warn("FileCache " + name + " could not remove " + ncfile.getLocation());
        }
        if (cacheLog.isDebugEnabled()) cacheLog.debug("FileCache " + name + " remove " + ncfile.getLocation());
        if (debugPrint) System.out.println("  FileCache " + name + " remove " + ncfile.getLocation());
      }

      public String toString() {
        return isLocked  + " " + countAccessed + " " + new Date(lastAccessed) + " " + ncfile.getLocation();
      }

      public int compareTo(CacheFile o) {
        return (int) (lastAccessed - o.lastAccessed);
      }
    }
  }

  private class CleanupTask implements Runnable {
    public void run() {
      cleanup(softLimit);
    }
  }
 
}