/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.cache;

import net.jcip.annotations.ThreadSafe;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keep cache of open FileCacheable objects, for example NetcdfFile.
 * The FileCacheable object typically contains a RandomAccessFile object that wraps a system resource like a file handle.
 * These are left open when the FileCacheable is in the cache. The maximum number of these is bounded, though not strictly.
 * A cleanup routine reduces cache size to a minimum number. This cleanup is called periodically in a background thread,
 * and also when the maximum cache size is reached.
 * <ol>
 * <li>The FileCacheable object must not be modified.
 * <li>The hashKey must uniquely define the FileCacheable object.
 * <li>The location must be usable in a FileFactory.open().
 * <li>If the FileCacheable is acquired from the cache (ie already open), getLastModified() is used to see if it has changed,
 *     and is discarded if so.
 * <li>Make sure you call shutdown() when exiting the program, in order to shut down the cleanup thread.
 * </ol>
 * <p/>
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
 *
 * @author caron
 * @since 10/28/2014
 */
@ThreadSafe
public class FileCacheARC implements FileCacheIF {
  static protected final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileCacheARC.class);
  static protected final org.slf4j.Logger cacheLog = org.slf4j.LoggerFactory.getLogger("cacheLogger");
  static boolean debug = false;
  static boolean debugPrint = false;
  static boolean trackAll = true;

  /////////////////////////////////////////////////////////////////////////////////////////

  protected String name;
  protected final int softLimit, minElements, hardLimit, period;

  private final AtomicBoolean disabled = new AtomicBoolean(false);  // cache is disabled

  protected final ConcurrentSkipListMap<CacheElement, CacheElement> shadowCache; // this is used to decide which file to release
                                                                                 // this can be biggger than the cache, eg ARC uses 2c, where c = cache size
  protected final ConcurrentHashMap<Object, CacheElement> cache;                // collection of all unique files in the cache, keyed by hashKey, typically filename
                                                                                // cache has open file handle / object in memory
  protected final ConcurrentHashMap<Integer, CacheElement.CacheFile> files;     // collection of all files in the cache, keyed by FileCacheable hashcode (object id)
                                                                                // this is needed for release

  // debugging and global stats
  protected final AtomicInteger hits = new AtomicInteger();
  protected final AtomicInteger miss = new AtomicInteger();
  protected ConcurrentHashMap<Object, Tracker> track;

  /**
   * Constructor.
   *
   * @param name                of file cache
   * @param minElementsInMemory keep this number in the cache
   * @param softLimit           trigger a cleanup if it goes over this number.
   * @param hardLimit           if > 0, never allow more than this many elements. This causes a cleanup to be done in the calling thread.
   * @param period              if > 0, do periodic cleanups every this number of seconds.
   */
  public FileCacheARC(String name, int minElementsInMemory, int softLimit, int hardLimit, int period) {
    this.name = name;
    this.minElements = minElementsInMemory;
    this.softLimit = softLimit;
    this.hardLimit = hardLimit;
    this.period = period;

    shadowCache = new ConcurrentSkipListMap<>(new CacheElementComparator());
    cache = new ConcurrentHashMap<>(2 * softLimit, 0.75f, 8);
    files = new ConcurrentHashMap<>(4 * softLimit, 0.75f, 8);

    if (trackAll)
      track = new ConcurrentHashMap<>(5000);
  }

  /**
   * Disable the cache, and force release all files.
   * You must still call shutdown() before exiting the application.
   */
  @Override
  public void disable() {
    this.disabled.set(true);
    clearCache(true);
  }

  /**
   * Enable the cache, with the current set of parameters.
   */
  @Override
  public void enable() {
    this.disabled.set(false);
  }

  /**
   * Acquire a FileCacheable, and lock it so no one else can use it.
   * call FileCacheable.close() when done.
   *
   * @param factory    use this factory to open the file; may not be null
   * @param location   file location, also used as the cache name, will be passed to the NetcdfFileFactory
   * @return NetcdfFile corresponding to location.
   * @throws IOException on error
   */
  @Override
  public FileCacheable acquire(FileFactory factory, String location) throws IOException {
    return acquire(factory, location, location, -1, null, null);
  }

  /**
   * Acquire a FileCacheable from the cache, and lock it so no one else can use it.
   * If not already in cache, open it with FileFactory, and put in cache.
   * <p/>
   * Call FileCacheable.close() when done, (rather than FileCacheIF.release() directly) and the file is then released instead of closed.
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
  @Override
  public FileCacheable acquire(FileFactory factory, Object hashKey, String location,
                               int buffer_size, CancelTask cancelTask, Object spiObject) throws IOException {

    if (null == hashKey) hashKey = location;
    if (null == hashKey) throw new IllegalArgumentException();

    Tracker t = null;
    if (trackAll) {
      t = new Tracker(hashKey);
      Tracker prev = track.putIfAbsent(hashKey, t);
      if (prev != null) t = prev;
    }

    FileCacheable ncfile = acquireCacheOnly(hashKey);
    if (ncfile != null) {
      hits.incrementAndGet();
      if (t != null) t.hit++;
      return ncfile;
    }
    miss.incrementAndGet();
    if (t != null) t.miss++;

    // open the file
    ncfile = factory.open(location, buffer_size, cancelTask, spiObject);
    if (cacheLog.isDebugEnabled())
      cacheLog.debug("FileCacheARC " + name + " acquire " + hashKey + " " + ncfile.getLocation());
    if (debugPrint) System.out.println("  FileCacheARC " + name + " acquire " + hashKey + " " + ncfile.getLocation());

    // user may have canceled
    if ((cancelTask != null) && (cancelTask.isCancel())) {
      if (ncfile != null) ncfile.close();
      return null;
    }

    if (disabled.get()) return ncfile;

    addToCache(hashKey, ncfile);

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

    // see if its in the cache
    CacheElement wantCacheElem  = cache.get(hashKey);
    if (wantCacheElem == null) return null;  // not found in cache

    CacheElement.CacheFile want = null;
    for (CacheElement.CacheFile file : wantCacheElem.list) {
      if (file.isLocked.compareAndSet(false, true)) {
        want = file;
        break;
      }
    }
    if (want == null) return null; // no unlocked file in cache

    // check if modified, remove if so
    if (want.ncfile != null) {
      long lastModified = want.ncfile.getLastModified();
      boolean changed = lastModified != wantCacheElem.lastModified.get();

      if (changed) { // underlying file was modified
        if (cacheLog.isDebugEnabled())
          cacheLog.debug("FileCacheARC " + name + ": acquire from cache " + hashKey + " " + want.ncfile.getLocation() + " was changed; discard");

        expireFromCache(wantCacheElem);
        return null;
      }
    }

    updateInCache(wantCacheElem);
    return want.ncfile;
  }

  // get CacheElement specified by hashKey. If found, update lastUsed in shadowCache.
  private CacheElement updateInCache(CacheElement elem) {
    if (shadowCache.firstKey() == elem) return elem;

    elem.updateAccessed();
    CacheElement prev = shadowCache.put(elem, elem); // faster if we could just insert at the top of the list. maybe we need to use LinkedList ?
    if (prev != null && (elem != prev)) {
      CacheElementComparator cc = new CacheElementComparator();
      System.out.printf("elem != prev compare=%d%n", cc.compare(elem, prev));
      System.out.printf("hash elem =%d prev=%d%n", elem.hashCode(), prev.hashCode());
    }
    return elem;
  }

  private AtomicInteger cacheSize = new AtomicInteger();

  private void expireFromCache(CacheElement elem) {
    for (CacheElement.CacheFile cacheFile : elem.list) {
      cacheFile.ncfile.setFileCache(null);        // next time it closes, it will really close LOOK race condition ??
      cacheSize.getAndDecrement();
    }
    cache.remove(elem.hashKey);
    shadowCache.remove(elem);
  }

  /** LOOK copied from FileCache, probably wrong
    * Remove all instances of object from the cache
    * @param hashKey the object
    */
   @Override
   public void eject(Object hashKey) {
   }

  /*
      if (disabled.get()) return;

      // see if its in the cache
      CacheElement wantCacheElem = cache.get(hashKey);
      if (wantCacheElem == null) return;

      synchronized (wantCacheElem) { // synch in order to traverse the list
        for (CacheElement.CacheFile want : wantCacheElem.list) {
           files.remove(want.ncfile);
           want.ncfile.setFileCache(null); // unhook the caching
           try {
             want.ncfile.close();  // really close the file
             log.debug("close "+want.ncfile.getLocation());
           } catch (IOException e) {
             log.error("close failed on "+want.ncfile.getLocation(), e);
           }
           want.ncfile = null;
        }
        wantCacheElem.list.clear();
      }
     cache.remove(hashKey);
    } */


  private void addToCache(Object hashKey, FileCacheable ncfile) {
    CacheElement newCacheElem = new CacheElement(hashKey);
    CacheElement previous = cache.putIfAbsent(hashKey, newCacheElem); // add new element if doesnt exist
    CacheElement elem = (previous != null) ? previous : newCacheElem;  // use previous if it exists

    elem.addFile(ncfile);                                 // add to existing list
    shadowCache.put(newCacheElem, newCacheElem);

    int size = cacheSize.getAndIncrement();
    if (size > softLimit) {
      removeFromCache(size - softLimit);
    }
  }

  private void removeFromCache(int count) {
    int done = 0;
    while (count > done) {
      CacheElement elem = shadowCache.lastKey();
      done += elem.list.size();
      expireFromCache(elem);
    }
  }

  /**
   * Release the file. This unlocks it, updates its lastAccessed date.
   * FileCacheable.close() needs to call this instead of actually closing.
   *
   * @param ncfile release the lock on this FileCacheable object.
   * @throws IOException if file not in cache.
   */
  @Override
  public boolean release(FileCacheable ncfile) throws IOException {
    if (ncfile == null) return false;

    if (disabled.get()) {
      ncfile.setFileCache(null); // prevent infinite loops
      ncfile.close();
      return false;
    }

    // find it in the file cache
    int hashcode = System.identityHashCode(ncfile);    // using Object hashCode of the FileCacheable
    CacheElement.CacheFile file = files.get(hashcode);
    if (file != null) {
      if (!file.isLocked.get()) {
        Exception e = new Exception("Stack trace");
        cacheLog.warn("FileCacheARC " + name + " release " + ncfile.getLocation() + " not locked; hash= "+ncfile.hashCode(), e);
      }
      //file.lastAccessed = System.currentTimeMillis();
      //file.countAccessed++;
      file.isLocked.set(false);
      if (cacheLog.isDebugEnabled()) cacheLog.debug("FileCacheARC " + name + " release " + ncfile.getLocation()+"; hash= "+ncfile.hashCode());
      if (debugPrint) System.out.println("  FileCacheARC " + name + " release " + ncfile.getLocation());
      return true;
    }
    return false;
    // throw new IOException("FileCacheARC " + name + " release does not have file in cache = " + ncfile.getLocation());
  }

  // not private for testing
  class CacheElement {
    final Object hashKey;
    final List<CacheFile> list = new CopyOnWriteArrayList<>(); // may have multiple copies of the same file opened.

    final AtomicLong lastModified = new AtomicLong();
    final AtomicLong  lastAccessed = new AtomicLong();
    final AtomicInteger countAccessed = new AtomicInteger();

    CacheElement(Object hashKey) {
      this.hashKey = hashKey;
    }

    CacheFile addFile(FileCacheable ncfile) {
      CacheFile file = new CacheFile(ncfile);
      list.add(file);

      this.lastModified.set(ncfile.getLastModified());
      this.lastAccessed.set(System.currentTimeMillis());
      this.countAccessed.incrementAndGet();

      int hashcode = System.identityHashCode(ncfile);    // using Object hashCode of the FileCacheable
      if (debug) {
        if (files.get(hashcode) != null)
          cacheLog.error("files (2) already has " + hashKey + " " + name);
      }

      files.put(hashcode, file);
      if (cacheLog.isDebugEnabled()) cacheLog.debug("CacheElement add to cache " + hashKey + " " + name);
      return file;
    }

    public long getLastAccessed() {
      return lastAccessed.get();
    }

    public void updateAccessed() {
      lastAccessed.set(System.currentTimeMillis());
      this.countAccessed.incrementAndGet();
    }

    public String toString() {
      return hashKey + " count=" + list.size()+ " countAccessed=" + countAccessed + " lastAccessed=" + new Date(getLastAccessed());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CacheElement that = (CacheElement) o;
      return that.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
      return hashKey.hashCode();
    }

    class CacheFile implements Comparable<CacheFile> {
      FileCacheable ncfile; // actually final, but we null it out for gc
      final AtomicBoolean isLocked = new AtomicBoolean(true);

      private CacheFile(FileCacheable ncfile) {
        this.ncfile = ncfile;
        ncfile.setFileCache(FileCacheARC.this);

        if (cacheLog.isDebugEnabled()) cacheLog.debug("FileCacheARC " + name + " add to cache " + hashKey);
        if (debugPrint) System.out.println("  FileCacheARC " + name + " add to cache " + hashKey);
      }

      public long getLastAccessed() {
        return lastAccessed.get();
      }

      public int getCountAccessed() {
         return countAccessed.get();
       }

      void remove() {
        int hashcode = System.identityHashCode(ncfile);    // using Object hashCode of the FileCacheable
        if (null == files.remove(hashcode))
          cacheLog.warn("FileCacheARC {} could not remove {} from files", name, ncfile.getLocation());
        if (!list.remove(this))
          cacheLog.warn("FileCacheARC {} could not remove {} from list", name, ncfile.getLocation());
        close();
      }

      void close() {
        // really close the file
        ncfile.setFileCache(null);
        try {
          ncfile.close();
        } catch (IOException e) {
          log.error("close failed on "+ncfile.getLocation(), e);
        }

        if (cacheLog.isDebugEnabled()) cacheLog.debug("FileCacheARC " + name + " remove " + ncfile.getLocation());
        if (debugPrint) System.out.println("  FileCacheARC " + name + " remove " + ncfile.getLocation());
        ncfile = null;
      }

      public String toString() {
        return isLocked + " " + ncfile.getLocation();
      }

      public int compareTo(CacheFile o) {
        return Misc.compare(lastAccessed.get(), o.getLastAccessed());
      }
    }
  }

  static private class CacheElementComparator implements
            Comparator<CacheElement> {
    @Override
    public int compare(CacheElement o1, CacheElement o2) {
      return Misc.compare(o1.getLastAccessed(), o2.getLastAccessed());
    }
  }

  /*
  case I:  if (x in T1 + T2)    // cache hit
    x -> MRU in T2

  case II: if (x in B1)         // cache miss
    adapt1(p)
    replace(x,p)
    move x from B1 -> MRU in T2

  case III: if (x in B2)        // cache miss
    adapt2(p)
    replace(x,p)
    move x from B2 -> MRU in T2

  case IV: not in cache         // cache miss
    case A: if (size L1 == c)
      if (size T1 < c)
        delete LRU in B1
        replace(x,p)
      else            // B1 is empty
        delete LRU in T1
    case B:
      if (size L1 + L2 >= c)
         if (size L1 + L2 == 2c)
           delete LRU in B2
         replace(x,p)

    x -> MRU in T1



   */

  // debug
  public String getInfo(FileCacheable ncfile) throws IOException {
    if (ncfile == null) return "";

    // find it in the file cache
    int hashcode = System.identityHashCode(ncfile);
    CacheElement.CacheFile file = files.get(hashcode);
    if (file != null) {
      return "File is in cache= " + file;
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
    List<CacheElement.CacheFile> deleteList = new ArrayList<>(2 * cache.size());

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
      for (CacheElement elem : cache.values()) {
        if (elem.list.size() == 0)
          cache.remove(elem.hashKey);
      }
    }

    // close all files in deleteList
    for (CacheElement.CacheFile file : deleteList) {
      if (force && file.isLocked.get())
        cacheLog.warn("FileCacheARC " + name + " force close locked file= " + file);
      //counter.decrementAndGet();

      if (file.ncfile == null) continue;

      try {
        file.ncfile.setFileCache(null);
        file.ncfile.close();
        file.ncfile = null; // help the gc
      } catch (IOException e) {
        log.error("FileCacheARC " + name + " close failed on " + file);
      }
    }
    if (cacheLog.isDebugEnabled())
      cacheLog.debug("*FileCacheARC " + name + " clearCache force= " + force + " deleted= " + deleteList.size() + " left=" + files.size());
    //System.out.println("\n*NetcdfFileCache.clearCache force= " + force + " deleted= " + deleteList.size() + " left=" + counter.get());
  }

  /**
   * Show individual cache entries, add to formatter.
   *
   * @param format add to this
   */
  @Override
  public void showCache(Formatter format) {
    ArrayList<CacheElement.CacheFile> allFiles = new ArrayList<>(files.size());
    for (CacheElement elem : cache.values()) {
      allFiles.addAll(elem.list);
    }
    Collections.sort(allFiles); // sort so oldest are on top

    format.format("%nFileCacheARC %s (min=%d softLimit=%d hardLimit=%d scour=%d):%n", name, minElements, softLimit, hardLimit, period);
    format.format("isLocked  accesses lastAccess                   location %n");
    for (CacheElement.CacheFile file : allFiles) {
      String loc = file.ncfile != null ? file.ncfile.getLocation() : "null";
      CalendarDate cd = CalendarDate.of(file.getLastAccessed());
      format.format("%8s %9d %s %s %n", file.isLocked, file.getCountAccessed(), CalendarDateFormatter.toTimeUnits(cd), loc);
    }
    showStats(format);
  }

  @Override
  public List<String> showCache() {
    List<CacheElement.CacheFile> allFiles = new ArrayList<>(files.size());
    for (CacheElement elem : cache.values()) {
      allFiles.addAll(elem.list);
    }
    Collections.sort(allFiles); // sort so oldest are on top

    List<String> result = new ArrayList<>(allFiles.size());
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
    format.format("  hits= %d miss= %d nfiles= %d elems= %d shadow=%d%n", hits.get(), miss.get(), files.size(), cache.values().size(), shadowCache.size());
  }

  ///////////////////////////////////////////////////////////////

  public void showTracking(Formatter format) {
    if (track == null) return;
    List<Tracker> all = new ArrayList<>(track.size());
    for (Tracker val : track.values()) all.add(val);
    Collections.sort(all);       // LOOK what should we sort by ??
    int count = 0;
    int countAll = 0;
    format.format("%nTrack of all files in FileCacheARC%n");
    format.format("   seq  accum   hit   miss  file%n");
    for (Tracker t : all) {
      count++;
      countAll += t.hit + t.miss;
      format.format("%6d  %6d : %5d %5d %s%n", count, countAll, t.hit, t.miss, t.key);
    }
    format.format("%n");
  }

  @Override
  public void resetTracking() {
    track = new ConcurrentHashMap<>(5000);
  }

  private static class Tracker implements Comparable<Tracker> {
    Object key;
    int hit, miss;

    private Tracker(Object key) {
      this.key = key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Tracker tracker = (Tracker) o;
      return key.equals(tracker.key);   // maybe == ?? LOOK
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }

    @Override
    public int compareTo(Tracker o) {
      return Misc.compare(hit + miss, o.hit + o.miss);
    }
  }
}
