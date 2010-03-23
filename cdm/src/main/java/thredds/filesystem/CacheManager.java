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
import java.util.Formatter;

import ucar.unidata.util.StringUtil;
import ucar.nc2.util.IO;

/**
 * Cache filesystem info.
 * Must be thread safe.
 * Uses ehcache underneath.
 *
 * @author caron
 * @since Mar 21, 2009
 */
@ThreadSafe
public class CacheManager {
  static private org.slf4j.Logger cacheLog = org.slf4j.LoggerFactory.getLogger(thredds.filesystem.CacheManager.class);
  static private net.sf.ehcache.CacheManager cacheManager;
  static private boolean debugConfig = false;

  static public net.sf.ehcache.CacheManager getEhcache() {
    return cacheManager;
  }

  static public void makeStandardCacheManager(String configFile, String cacheDir) throws IOException {
    String config = IO.readFile(configFile);
    String configString = StringUtil.substitute(config, "${cacheDir}", cacheDir);
    cacheLog.info("thredds.filesystem.CacheManager configuraton " + configString);
    cacheManager = new net.sf.ehcache.CacheManager(new StringBufferInputStream(configString));
  }

  static public void makeTestCacheManager(String cacheDir) {
    String configString = StringUtil.substitute(config, "${cacheDir}", cacheDir);
    if (debugConfig) System.out.printf("CacheManager test=%n %s %n", configString);
    cacheManager = new net.sf.ehcache.CacheManager(new StringBufferInputStream(configString));
  }

  static public void makeReadOnlyCacheManager(String cacheDir, String cacheName) {
    String configString = StringUtil.substitute(configReadOnly, "${cacheDir}", cacheDir);
    configString = StringUtil.substitute(configString, "${cacheName}", cacheName);
    if (debugConfig) System.out.printf("CacheManager readonly =%n %s %n", configString);
    cacheManager = new net.sf.ehcache.CacheManager(new StringBufferInputStream(configString));
  }

  static public void shutdown() {
    if (cacheManager != null) {
      cacheLog.info("thredds.filesystem.CacheManager shutdown");
      cacheManager.shutdown();
    }
    cacheManager = null;
  }

  /////////////////////////////////////////

  private Cache cache;
  private AtomicLong addElements = new AtomicLong();
  private AtomicLong hits = new AtomicLong();
  private AtomicLong requests = new AtomicLong();

  public CacheManager(String cacheName) {
    cache = cacheManager.getCache(cacheName);
    cacheLog.info("thredds.filesystem.CacheManager " + cache);
    cacheLog.info("thredds.filesystem.CacheManager " + cache.getStatistics().toString());
  }

  public void add(Serializable path, Serializable value) {
    if (cache == null) return;

    cache.put(new Element(path, value));
    addElements.incrementAndGet();
  }

  static public String show(String cacheName) {
    if (cacheManager == null) return "no cacheManager set";
    Cache cache = cacheManager.getCache(cacheName);
    if (cache == null) return "no cache named "+cacheName;
    Formatter f = new Formatter();
    f.format("Cache %s%n %s%n", cache, cache.getStatistics().toString());
    return f.toString();
  }

  /**
   * Get a CacheDirectory from the path. If not in cache, read OS and put in cache.
   * 
   * @param path file path
   * @param recheck if true, check that directory hasnt changed, otherwise ok to use cached element without chcking
   * @return  CacheDirectory
   */
  public CacheDirectory get(String path, boolean recheck) {
    requests.incrementAndGet();

    Element e = cache.get(path);
    if (e != null) {
      if (cacheLog.isDebugEnabled()) cacheLog.debug("thredds.filesystem.CacheManager found in cache; path =" + path);

      CacheDirectory m = (CacheDirectory) e.getValue();
      if (m != null) { // not sure how a null m gets in here
        if (!recheck) return m;

        File f = new File(m.getPath()); // check if file exists and when last modified
        if (!f.exists()) {
          cache.put(new Element(path, null));
          return null;
        }

        boolean modified = (f.lastModified() > m.lastModified);
        if (cacheLog.isDebugEnabled()) cacheLog.debug("thredds.filesystem.CacheManager modified diff = "+
                (f.lastModified() - m.lastModified) +
                "; path=" + path);

        if (!modified) {
          hits.incrementAndGet();
          return m;
        }
        // if modified, null it out and reread it
        cache.put(new Element(path, null));
      }
    }

    File p = new File(path);
    if (!p.exists()) return null;

    if (cacheLog.isDebugEnabled()) cacheLog.debug("thredds.filesystem.CacheManager read from filesystem; path=" + path);
    CacheDirectory m = new CacheDirectory(p);
    add(path, m);
    return m;
  }

  public void close() {
    if (cacheManager != null) {
      cacheLog.info("thredds.filesystem.CacheManager shutdown");
      cacheManager.shutdown();
    }
    cacheManager = null;
  }

  ////////////////////////////////////////////////////////////////
  // test and debug

  public void showKeys() {
    List keys = cache.getKeys();
    Collections.sort(keys);
    for (Object key : keys) {
      Element elem = cache.get(key);
      System.out.printf(" %40s == %s%n", key, elem);
    }
  }

  public void stats() {
    System.out.printf(" elems added= %s%n", addElements.get());
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

  public void populateFiles(String root) {

    long startCount = addElements.get();
    long start = System.nanoTime();
    addRecursiveFiles(new File(root));
    long end = System.nanoTime();
    long total = addElements.get() - startCount;
    System.out.printf("populate %n%-20s total %d took %d msecs %n", root, total, (end - start) / 1000 / 1000);
  }

  private void addRecursiveFiles(File dir) {
    for (File f : dir.listFiles()) {
      if (f.isDirectory()) addRecursiveFiles(f);
      else add(f.getPath(), new CacheFile(f));
    }
  }

  public void populateFilesProto(String root) {

    long startCount = addElements.get();
    long start = System.nanoTime();
    addRecursiveFilesProto(new File(root));
    long end = System.nanoTime();
    long total = addElements.get() - startCount;
    System.out.printf("populate %n%-20s total %d took %d msecs %n", root, total, (end - start) / 1000 / 1000);
  }

  private void addRecursiveFilesProto(File dir) {
    for (File f : dir.listFiles()) {
      if (f.isDirectory()) addRecursiveFilesProto(f);
      else add(f.getPath(), new CacheFileProto(f));
    }
  }

  public void populateDirs(String root) {

    long startCount = addElements.get();
    long start = System.nanoTime();
    addRecursiveDirs(new File(root));
    long end = System.nanoTime();
    long total = addElements.get() - startCount;
    System.out.printf("populate %n%-20s total %d took %d msecs %n", root, total, (end - start) / 1000 / 1000);
  }

  private void addRecursiveDirs(File dir) {
    add(dir.getPath(), new CacheDirectory(dir));
    for (File f : dir.listFiles()) {
      if (f.isDirectory()) addRecursiveDirs(f);
    }
  }

  public void populateDirsProto(String root) {

    long startCount = addElements.get();
    long start = System.nanoTime();
    addRecursiveDirsProto(new File(root));
    long end = System.nanoTime();
    long total = addElements.get() - startCount;
    System.out.printf("populate %n%-20s total %d took %d msecs %n", root, total, (end - start) / 1000 / 1000);
  }

  private void addRecursiveDirsProto(File dir) {
    add(dir.getPath(), new CacheDirectoryProto(dir));
    for (File f : dir.listFiles()) {
      if (f.isDirectory()) addRecursiveDirsProto(f);
    }
  }

///////////////////////////////////////////////////////////////////////////////

  private static String configReadOnly =
          "<ehcache>\n" +
                  "    <diskStore path='${cacheDir}' />\n" +
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
                  "    <cache name='${cacheName}'\n" +
                  "            maxElementsInMemory='10000'\n" +
                  "            eternal='false'\n" +
                  "            timeToIdleSeconds='864000'\n" +
                  "            timeToLiveSeconds='0'\n" +
                  "            overflowToDisk='true'\n" +
                  "            maxElementsOnDisk='100000'\n" +
                  "            diskPersistent='true'\n" +
                  "            diskExpiryThreadIntervalSeconds='3600'\n" +
                  "            memoryStoreEvictionPolicy='LRU'\n" +
                  "            />\n" +
                  "</ehcache>";

  private static String config =
          "<ehcache>\n" +
                  "    <diskStore path='${cacheDir}' />\n" +
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
                  "    <cache name='directories'\n" +
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
                  "    <cache name='files'\n" +
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
                  "    <cache name='filesProto'\n" +
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
                  "    <cache name='dirsProto'\n" +
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

  static public void main(String args[]) throws IOException {
    //makeStandardCacheManager("C:/Documents and Settings/caron/.unidata/ehcache/");
    makeTestCacheManager("C:/data/ehcache/");

    /* CacheManager cm = new CacheManager("files");
    cm.populateFiles( "C:/data/");
    cm.stats();
    
    System.out.printf("=====================%n");
    CacheManager cmDir = new CacheManager("directories");
    cmDir.populateDirs( "C:/data/");
    cmDir.stats();

    System.out.printf("=====================%n");
    CacheManager cmProto = new CacheManager("filesProto");
    cmProto.populateFilesProto("C:/data/");
    cmProto.stats(); */

    System.out.printf("=====================%n");
    CacheManager dirProto = new CacheManager("dirsProto");
    dirProto.populateDirsProto("C:/data/");
    dirProto.stats();

    shutdown();


    Formatter f = new Formatter(System.out);
    f.format(" Proto count = %d size = %d %n", CacheFileProto.countWrite, CacheFileProto.countWriteSize);
    int avg = CacheFileProto.countWrite == 0 ? 0 : CacheFileProto.countWriteSize / CacheFileProto.countWrite;
    f.format("       avg = %d %n", avg);
    f.flush();
  }
}
