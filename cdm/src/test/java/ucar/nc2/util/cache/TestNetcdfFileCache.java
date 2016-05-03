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

import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author caron
 * @since May 31, 2008
 */
public class TestNetcdfFileCache {

  static FileCache cache;
  static FileFactory factory = new MyFileFactory();

  @BeforeClass
  static public void setUp() throws java.lang.Exception {
    cache = new FileCache(5, 100, 60 * 60);
  }

  static public class MyFileFactory implements FileFactory {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      return NetcdfDataset.openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }


  int count = 0;

  void loadFilesIntoCache(File dir, FileCache cache) {
    File[] files = dir.listFiles();
    if ( files == null) return;

    for (File f : files) {
      if (f.isDirectory())
        loadFilesIntoCache(f, cache);

      else if (f.getPath().endsWith(".nc") && f.length() > 0) {
        //System.out.println(" open "+f.getPath());
        try {
          String want = StringUtil2.replace(f.getPath(), '\\', "/");
          cache.acquire(factory, want, null);
          count++;
        } catch (IOException e) {
          // e.printStackTrace();
          System.out.println(" *** failed on " + f.getPath());
        }
      }
    }
  }

  @Test
  public void testNetcdfFileCache() throws IOException {
    System.out.printf("TestNetcdfFileCache%n");

    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    System.out.println(" loaded " + count);

    cache.showCache(new Formatter(System.out));

    // count cache size
    Map<Object, FileCache.CacheElement> map = cache.getCache();
    assert map.values().size() == count;

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      //System.out.println(" "+key+" == "+elem);
      assert elem.list.size() == 1;
    }

    // load same files again - should be added to the list, rather than creating a new elem
    int saveCount = count;
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    map = cache.getCache();
    assert map.values().size() == saveCount;

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      //System.out.println(" "+key+" == "+elem);
      assert elem.list.size() == 2;
      checkAllSame(elem.list);
    }

    cache.clearCache(true);
    map = cache.getCache();
    assert map.values().size() == 0;

    // load again
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    map = cache.getCache();
    assert map.values().size() == saveCount;

    // close all
    List<FileCacheable> files = new ArrayList<>();
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assert elem.list.size() == 1;
      for (FileCache.CacheElement.CacheFile file : elem.list) {
        files.add(file.ncfile);
      }
    }
    for (FileCacheable ncfile : files) {
      ncfile.close();
    }
    cache.clearCache(false);
    map = cache.getCache();
    assert map.values().size() == 0 : map.values().size();

    // load twice
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    map = cache.getCache();
    assert map.values().size() == saveCount;

    // close 1 of 2
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assert elem.list.size() == 2;
      FileCache.CacheElement.CacheFile first = elem.list.get(0);
      first.ncfile.close();
      assert !first.isLocked.get();
      assert elem.list.size() == 2;
    }

    map = cache.getCache();
    assert map.values().size() == saveCount;

    cache.clearCache(false);
    map = cache.getCache();
    assert map.values().size() == saveCount;

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assert elem.list.size() == 1;
    }

    cache.clearCache(true);
  }

  void checkAllSame(List<FileCache.CacheElement.CacheFile> list) {
    FileCache.CacheElement.CacheFile first = null;
    for (FileCache.CacheElement.CacheFile file : list) {
      assert file.isLocked.get();
      assert file.countAccessed == 0 : file.countAccessed;  // countAccessed not incremented until its closed, so == 0
      assert file.lastAccessed != 0;

      if (first == null)
        first = file;
      else {
        assert first.ncfile.getLocation().equals(file.ncfile.getLocation());
        assert first.lastAccessed < file.lastAccessed;
      }
    }
  }


  /////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testPeriodicClear() throws IOException {
    FileCache cache = new FileCache(0, 10, 60 * 60);
    testPeriodicCleanup(cache);
    Map<Object, FileCache.CacheElement> map = cache.getCache();
    assert map.values().size() == 0 : map.values().size();

    cache = new FileCache(5, 10, 60 * 60);
    testPeriodicCleanup(cache);
    map = cache.getCache();
    assert map.values().size() == 5 : map.values().size(); 
  }

  private void testPeriodicCleanup(FileCache cache) throws IOException {
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    System.out.println(" loaded " + count);

    // close all
    Map<Object, FileCache.CacheElement> map = cache.getCache();
    List<FileCacheable> files = new ArrayList<>();
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assert elem.list.size() == 1;
      for (FileCache.CacheElement.CacheFile file : elem.list) {
        files.add(file.ncfile);
      }
    }
    System.out.println(" close " + files.size());

    for (FileCacheable ncfile : files) {
      ncfile.close();
    }

    cache.showCache(new Formatter(System.out));    
    cache.cleanup(10);
  }


  //////////////////////////////////////////////////////////////////////////////////
  int N = 10000;
  int PROD_THREAD = 10;
  int CONS_THREAD = 10;
  // int QSIZE = 10;
  int SKIP = 100;

  @Test
  public void testConcurrentAccess() throws InterruptedException {
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    Map<Object, FileCache.CacheElement> map = cache.getCache();
    List<String> files = new ArrayList<>();
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      for (FileCache.CacheElement.CacheFile file : elem.list)
        files.add(file.ncfile.getLocation());
    }

    Random r = new Random();
    int nfiles = files.size();

    Formatter format = new Formatter(System.out);
    ConcurrentLinkedQueue<Future> q = new ConcurrentLinkedQueue<>();
    ExecutorService qexec = Executors.newFixedThreadPool(CONS_THREAD);
    qexec.submit(new Consumer(q, format));

    ExecutorService exec = Executors.newFixedThreadPool(PROD_THREAD);
    for (int i = 0; i < N; i++) {
      // pick a file at random
      int findex = r.nextInt(nfiles);
      String location = files.get(findex);
      q.add(exec.submit(new CallAcquire(location)));

      if (i % SKIP == 0) {
        format.format(" %3d qsize= %3d ", i, q.size());
        cache.showStats(format);
      }
    }

    format.format("awaitTermination 10 secs qsize= %3d%n", q.size());
    cache.showStats(format);
    exec.awaitTermination(10, TimeUnit.SECONDS);
    format.format("done qsize= %4d%n", q.size());
    cache.showStats(format);

    int total = 0;
    int total_locks = 0;
    HashSet<Object> checkUnique = new HashSet<>();
    map = cache.getCache();
    for (Object key : map.keySet()) {
      assert !checkUnique.contains(key);
      checkUnique.add(key);
      int locks = 0;
      FileCache.CacheElement elem = map.get(key);
      synchronized (elem) {
        for (FileCache.CacheElement.CacheFile file : elem.list)
          if (file.isLocked.get()) locks++;
      }
      //System.out.println(" key= "+key+ " size= "+elem.list.size()+" locks="+locks);
      total_locks += locks;
      total += elem.list.size();
    }
    System.out.println(" total=" + total + " total_locks=" + total_locks);
//    assert total_locks == map.keySet().size();

    cache.clearCache(false);
    format.format("after cleanup qsize= %4d%n", q.size());
    cache.showStats(format);

    cache.clearCache(true);
  }

  class Consumer implements Runnable {
    private final ConcurrentLinkedQueue<Future> queue;
    Formatter format;

    Consumer(ConcurrentLinkedQueue<Future> q, Formatter format) {
      queue = q;
      this.format = format;
    }


    public void run() {
      try {
        while (true) {
          consume(queue.poll());
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    void consume(Future x) throws ExecutionException, InterruptedException, IOException {
      if (x == null) return;

      if (x.isDone()) {
        NetcdfFile ncfile = (NetcdfFile) x.get();
        ncfile.close();
        //format.format("  closed qsize= %3d\n", queue.size());
      } else {
        // format.format("  lost file= %3d\n", queue.size());
        queue.add(x); // put it back
      }

    }
  }

  class CallAcquire implements Callable<FileCacheable> {
    String location;

    CallAcquire(String location) {
      this.location = location;
    }

    public FileCacheable call() throws Exception {
      return cache.acquire(factory, location, null);
    }
  }

  class RunClose implements Runnable {
    NetcdfFile f;

    RunClose(NetcdfFile f) {
      this.f = f;
    }

    public void run() {
      try {
        f.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
