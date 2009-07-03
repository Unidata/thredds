/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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
package thredds.filesystem;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.jcip.annotations.ThreadSafe;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.Collections;

/**
 * Cache filesystem info.
 * Must be thread safe.
 * Uses ehcache underneath.
 *
 * @author caron
 * @since Mar 21, 2009
 */
@ThreadSafe
class CacheManager {
  public boolean debug = false;

  private net.sf.ehcache.CacheManager cacheManager;
  private Cache cache;
  private AtomicLong addDir = new AtomicLong();
  private AtomicLong hits = new AtomicLong();
  private AtomicLong requests = new AtomicLong();

  public CacheManager(String ehconfig) {
    cacheManager = new net.sf.ehcache.CacheManager(ehconfig);
    cache = cacheManager.getCache("directory");
  }

  public CacheManager(InputStream ehconfig) {
    cacheManager = new net.sf.ehcache.CacheManager(ehconfig);
    cache = cacheManager.getCache("directory");
  }

  public void add(String path, Object value) {
    if (cache == null) return;

    cache.put(new Element(path, value));
    addDir.incrementAndGet();
  }

  public CacheDirectory get(String path) {
    requests.incrementAndGet();

    if (cache != null) {
      Element e = cache.get(path);
      if (e != null) {
        if (debug) System.out.printf(" InCache %s%n", path);

        CacheDirectory m = (CacheDirectory) e.getValue();
        if (m.notModified()) {
          if (debug) System.out.printf(" Hit %s%n", path);
          hits.incrementAndGet();
          return m;
        }
        // if modified, null it out and reread it
        cache.put(new Element(path, null));
      }
    }

    File p = new File(path);
    if (!p.exists()) return null;

    if (debug) System.out.printf(" Read file system %s%n", path);
    CacheDirectory m = new CacheDirectory(p);
    add(path, m);
    return m;
  }

  public void close() {
    stats();
    if (cacheManager != null)
      cacheManager.shutdown();
    cacheManager = null;
  }

  ////////////////////////////////////////////////////////////////

  public void show() {
    for (Object o : cache.getKeys())
      System.out.printf(" %s%n", o);
  }

  public void populate() {
    String root = "C:/";

    long startCount = addDir.get();
    long start = System.nanoTime();
    //add( root, new CacheDirectory(this, new File(root)));
    long end = System.nanoTime();
    long total = addDir.get() - startCount;
    System.out.printf("populate %n%-20s total %d took %d msecs %n", root, total, (end - start) / 1000 / 1000);

  }

  public void stats() {
    System.out.printf(" dirs added= %s%n", addDir.get());
    System.out.printf(" reqs= %d%n", requests.get());
    System.out.printf(" hits= %d%n", hits.get());

    if (cache != null) {
      System.out.printf(" cache= %s%n", cache.toString());
      System.out.printf(" cache.size= %d%n", cache.getSize());
      System.out.printf(" cache.memorySize= %d%n", cache.getMemoryStoreSize());
      Statistics stats = cache.getStatistics();
      System.out.printf(" stats= %s%n", stats.toString());
    }
  }

  private static String ehLocation = "/data/thredds/ehcache/";
  private static String config =
          "<ehcache>\n" +
                  "    <diskStore path='" + ehLocation + "'/>\n" +
                  "    <defaultCache\n" +
                  "              maxElementsInMemory='10000'\n" +
                  "              eternal='false'\n" +
                  "              timeToIdleSeconds='120'\n" +
                  "              timeToLiveSeconds='120'\n" +
                  "              overflowToDisk='true'\n" +
                  "              maxElementsOnDisk='10000000'\n" +
                  "              diskPersistent='false'\n" +
                  "              diskExpiryThreadIntervalSeconds='120'\n" +
                  "              memoryStoreEvictionPolicy='LRU'\n" +
                  "              />\n" +
                  "    <cache name='directory'\n" +
                  "            maxElementsInMemory='1000'\n" +
                  "            eternal='true'\n" +
                  "            timeToIdleSeconds='864000'\n" +
                  "            timeToLiveSeconds='0'\n" +
                  "            overflowToDisk='true'\n" +
                  "            maxElementsOnDisk='0'\n" +
                  "            diskPersistent='true'\n" +
                  "            diskExpiryThreadIntervalSeconds='3600'\n" +
                  "            memoryStoreEvictionPolicy='LRU'\n" +
                  "            />\n" +
                  "</ehcache>";

  public static CacheManager makeStandardCacheManager() {
    CacheManager cm = new CacheManager(new StringBufferInputStream(config));
    System.out.printf("Open StandardCacheManager %s%n", cm.cacheManager);
    return cm;
  }

  static public void main( String args[]) throws IOException {
    CacheManager cm = new CacheManager(new StringBufferInputStream(config));
    net.sf.ehcache.Cache cache = cm.cacheManager.getCache("directory");

    System.out.printf("Cache %s%n", cache);
    List keys = cache.getKeys();
    Collections.sort(keys);
    for (Object key : keys) {
      Element elem = cache.get(key);
      System.out.printf(" %40s == %s%n", key, elem);
    }
    cm.close();
  }
}
