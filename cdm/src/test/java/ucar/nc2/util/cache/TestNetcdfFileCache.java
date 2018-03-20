/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util.cache;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author caron
 * @since May 31, 2008
 */
public class TestNetcdfFileCache {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static FileCache cache;
  static FileFactory factory = new MyFileFactory();

  @BeforeClass
  static public void setUp() throws java.lang.Exception {
    cache = new FileCache(5, 100, 60 * 60);
  }

  static public class MyFileFactory implements FileFactory {
    public FileCacheable open(DatasetUrl location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
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
          DatasetUrl durl = new DatasetUrl(null, want);
          cache.acquire(factory, durl);
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
    Assert.assertEquals(count, map.values().size());

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      //System.out.println(" "+key+" == "+elem);
      Assert.assertEquals(1, elem.list.size());
    }

    // load same files again - should be added to the list, rather than creating a new elem
    int saveCount = count;
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    map = cache.getCache();
    cache.showCache(new Formatter(System.out));

    Assert.assertEquals(saveCount, map.values().size()); // problem

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      //System.out.println(" "+key+" == "+elem);
      Assert.assertEquals(2, elem.list.size());
      checkAllSame(elem.list);
    }

    cache.clearCache(true);
    map = cache.getCache();
    Assert.assertEquals(0, map.values().size());

    // load again
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    map = cache.getCache();
    Assert.assertEquals(saveCount, map.values().size());

    // close all
    List<FileCacheable> files = new ArrayList<>();
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      Assert.assertEquals(1, elem.list.size());
      for (FileCache.CacheElement.CacheFile file : elem.list) {
        files.add(file.ncfile);
      }
    }
    for (FileCacheable ncfile : files) {
      ncfile.close();
    }
    cache.clearCache(false);
    map = cache.getCache();
    Assert.assertEquals(0, map.values().size());

    // load twice
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    loadFilesIntoCache(new File(TestDir.cdmLocalTestDataDir), cache);
    map = cache.getCache();
    Assert.assertEquals(saveCount, map.values().size());

    // close 1 of 2
    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      Assert.assertEquals(2, elem.list.size());
      FileCache.CacheElement.CacheFile first = elem.list.get(0);
      first.ncfile.close();
      Assert.assertTrue(!first.isLocked.get());
      Assert.assertEquals(2, elem.list.size());
    }

    map = cache.getCache();
    Assert.assertEquals(saveCount, map.values().size());

    cache.clearCache(false);
    map = cache.getCache();
    Assert.assertEquals(saveCount, map.values().size());

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      Assert.assertEquals(1, elem.list.size());
    }

    cache.clearCache(true);
  }

  void checkAllSame(List<FileCache.CacheElement.CacheFile> list) {
    FileCache.CacheElement.CacheFile first = null;
    for (FileCache.CacheElement.CacheFile file : list) {
      Assert.assertTrue(file.isLocked.get());
      Assert.assertEquals(0, file.countAccessed);  // countAccessed not incremented until its closed, so == 0
      Assert.assertNotEquals(0, file.lastAccessed);

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

    ExecutorService qexec = null;
    ExecutorService exec = null;
    try {
      Formatter format = new Formatter(System.out);
      ConcurrentLinkedQueue<Future> q = new ConcurrentLinkedQueue<>();
      qexec = Executors.newFixedThreadPool(CONS_THREAD);
      qexec.submit(new Consumer(q, format));

      exec = Executors.newFixedThreadPool(PROD_THREAD);
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

    } finally {
      if (qexec != null) qexec.shutdownNow();
      if (exec != null) exec.shutdownNow();
    }
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
      DatasetUrl durl = new DatasetUrl(null, location);
      return cache.acquire(factory, durl);
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
