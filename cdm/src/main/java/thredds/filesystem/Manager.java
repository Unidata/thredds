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
package thredds.filesystem;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.jcip.annotations.ThreadSafe;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Iterator;

/**
 * Manage filesystem info, with optional caching.
 * Must be thread safe. Uses ehcache underneath.
 * @author caron
 * @since Mar 21, 2009
 */
@ThreadSafe
public class Manager {

  private CacheManager cacheManager;
  private Cache cache;
  private AtomicLong addDir = new AtomicLong();
  private AtomicLong hits = new AtomicLong();
  private AtomicLong requests = new AtomicLong();

  // no cache
  public Manager() {
  }

  public Manager(String ehconfig) {
    cacheManager = new CacheManager(ehconfig);
    cache = cacheManager.getCache("directory");
  }

  public Manager(InputStream ehconfig) {
    cacheManager = new CacheManager(ehconfig);
    cache = cacheManager.getCache("directory");
  }

  public void add(Object key, Object value) {
    if (cache == null) return;

    cache.put(new Element(key, value));
    addDir.incrementAndGet();    
  }

  public MDirectory get(String path) {
    requests.incrementAndGet();

    if (cache != null) {
      Element e = cache.get(path);
      if (e != null) {
        System.out.printf(" InCache %s%n", path);

        MDirectory m = (MDirectory) e.getValue();
        if (m.notModified()) {
          //System.out.printf(" Hit %s%n", path);
          hits.incrementAndGet();
          return m;
        }
        // if modified, null it out and reread it
        cache.put(new Element(path, null));
      }
    }

    File p = new File(path);
    if (!p.exists()) return null;

    System.out.printf(" Read %s%n", path);
    MDirectory m = new MDirectory(p);
    add(path, m);
    return m;
  }

  public void close() {
    cacheManager.shutdown();
  }

  public void show() {
    Iterator iter = cache.getKeys().iterator();
    while (iter.hasNext())
      System.out.printf(" %s%n", iter.next());
  }

  public void populate() {
    String root = "C:/";

    long startCount = addDir.get();
    long start = System.nanoTime();
    add( root, new MDirectory(this, new File(root)));
    long end = System.nanoTime();
    long total = addDir.get() - startCount;
    System.out.printf("populate %n%-20s total %d took %d msecs %n", root, total, (end - start) / 1000 / 1000);

  }

  public void stats() {
    System.out.printf(" dirs added= %s%n", addDir.get());
    //System.out.printf(" files added= %s%n", MFile.total);
    System.out.printf(" reqs= %d%n", requests.get());
    System.out.printf(" hits= %d%n", hits.get());

    System.out.printf(" cache= %s%n", cache.toString());
    System.out.printf(" cache.size= %d%n", cache.getSize());
    System.out.printf(" cache.memorySize= %d%n", cache.getMemoryStoreSize());
    Statistics stats = cache.getStatistics();
    System.out.printf(" stats= %s%n", stats.toString());
  }

  static public void main( String args[]) throws IOException {
    Manager man = new Manager("C:/dev/tds/fileManager/src/main/ehcache.xml");

    DataInputStream in = new DataInputStream( System.in);
    while (true) {
      System.out.printf("dir: ");
      String line = in.readLine();
      if ((line == null) || (line.length() == 0)) break;
      if (line.equals("show")) {
        man.show();
        continue;
      }
      if (line.equals("populate")) {
        man.populate();
        continue;
      }

      long start = System.nanoTime();
      MDirectory dir = man.get(line);
      long end = System.nanoTime();
      System.out.printf("%n%-20s took %d usecs %n", line, (end - start) / 1000);
      System.out.printf(" man.size=%s%n", dir.getChildren().length);
    }

    man.stats();
    man.close();
  }
}
