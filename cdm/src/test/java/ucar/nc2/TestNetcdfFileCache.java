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

import junit.framework.TestCase;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author caron
 * @since May 31, 2008
 */
public class TestNetcdfFileCache extends TestCase {

  public TestNetcdfFileCache( String name) {
    super(name);
  }

  NetcdfFileCache cache;
  protected void setUp() throws java.lang.Exception {
    cache = new NetcdfFileCache(5, 100, 60 * 60, new NetcdfFileFactory() {
      public NetcdfFile open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
        return NetcdfDataset.openFile(location, buffer_size, cancelTask, iospMessage);
      }
    });
  }

  public void testNetcdfFileCache() throws IOException {
    loadFiles( new File(TestLocal.cdmTestDataDir), cache);
    System.out.println(" loaded "+count);

    Formatter format = new Formatter();
    cache.showCache(format);
    System.out.println(format);

    // count cache size
    Map<Object, NetcdfFileCache.CacheElement> map = cache.getCache();
    assert map.values().size() == count;

    for (Object key : map.keySet()) {
      NetcdfFileCache.CacheElement elem = map.get(key);
      //System.out.println(" "+key+" == "+elem);
      assert elem.list.size() == 1;
    }

    // load same files again - should be added to the list, rather than creating a new elem
    int saveCount = count;
    loadFiles( new File(TestLocal.cdmTestDataDir), cache);
    map = cache.getCache();
    assert map.values().size() == saveCount;

    for (Object key : map.keySet()) {
      NetcdfFileCache.CacheElement elem = map.get(key);
      //System.out.println(" "+key+" == "+elem);
      assert elem.list.size() == 2;
      checkAllSame( elem.list);
    }

    cache.clearCache(true);
    map = cache.getCache();
    assert map.values().size() == 0;

    // load again
    loadFiles( new File(TestLocal.cdmTestDataDir), cache);
    map = cache.getCache();
    assert map.values().size() == saveCount;

    // close all
    List<NetcdfFile> files = new ArrayList<NetcdfFile>();
    for (Object key : map.keySet()) {
      NetcdfFileCache.CacheElement elem = map.get(key);
      assert elem.list.size() == 1;
      for (NetcdfFileCache.CacheElement.CacheFile file : elem.list) {
        files.add( file.ncfile);
      }
    }
    for (NetcdfFile ncfile : files) {
      ncfile.close();
    }
    cache.clearCache( false);
    map = cache.getCache();
    assert map.values().size() == 0 : map.values().size();

    // load twice
    loadFiles( new File(TestLocal.cdmTestDataDir), cache);
    loadFiles( new File(TestLocal.cdmTestDataDir), cache);
    map = cache.getCache();
    assert map.values().size() == saveCount;

    // close 1 of 2
    for (Object key : map.keySet()) {
      NetcdfFileCache.CacheElement elem = map.get(key);
      assert elem.list.size() == 2;
      NetcdfFileCache.CacheElement.CacheFile first = elem.list.get(0);
      first.ncfile.close();
      assert !first.isLocked.get();
      assert elem.list.size() == 2;
    }

    map = cache.getCache();
    assert map.values().size() == saveCount;

    cache.clearCache( false);
    map = cache.getCache();
    assert map.values().size() == saveCount;

    for (Object key : map.keySet()) {
      NetcdfFileCache.CacheElement elem = map.get(key);
      assert elem.list.size() == 1;
    }
  }

  void checkAllSame(List<NetcdfFileCache.CacheElement.CacheFile> list) {
    NetcdfFileCache.CacheElement.CacheFile first = null;
    for (NetcdfFileCache.CacheElement.CacheFile file : list) {
      assert file.isLocked.get();
      assert file.countAccessed == 1;
      assert file.lastAccessed != 0;

      if (first == null)
        first = file;
      else {
        assert first.ncfile.getLocation().equals( file.ncfile.getLocation());
        assert first.lastAccessed < file.lastAccessed;
      }
    }
  }

  int count = 0;
  void loadFiles(File dir, NetcdfFileCache cache) {
    for (File f : dir.listFiles()) {
      if (f.isDirectory())
        loadFiles(f, cache);

      else if (f.getPath().endsWith(".nc") && f.length() > 0) {
        //System.out.println(" open "+f.getPath());
        try {
          String want = StringUtil.replace(f.getPath(), '\\', "/");
          cache.acquire(want, null);
          count++;
        } catch (IOException e) {
          // e.printStackTrace();
          System.out.println(" *** failed on "+f.getPath());
        }
      }
    }
  }

  int N = 10000;
  int PROD_THREAD = 10;
  int CONS_THREAD = 10;
 // int QSIZE = 10;
  int SKIP = 100;

  public void testConcurrentAccess() throws InterruptedException {
    loadFiles( new File(TestLocal.cdmTestDataDir), cache);
    Map<Object, NetcdfFileCache.CacheElement> map = cache.getCache();
    List<String> files = new ArrayList<String>();
    for (Object key : map.keySet()) {
      NetcdfFileCache.CacheElement elem = map.get(key);
      for (NetcdfFileCache.CacheElement.CacheFile file : elem.list)
        files.add( file.ncfile.getLocation());
    }

    Random r = new Random();
    int nfiles = files.size();

    Formatter format = new Formatter(System.out);
    ConcurrentLinkedQueue<Future> q = new ConcurrentLinkedQueue<Future>();
    ExecutorService qexec = Executors.newFixedThreadPool(CONS_THREAD);
    qexec.submit( new Consumer(q, format));

    ExecutorService exec = Executors.newFixedThreadPool(PROD_THREAD);
    for (int i=0; i< N; i++) {
      // pick a file at random
      int findex = r.nextInt(nfiles);
      String location = files.get(findex);
      q.add( exec.submit( new CallAcquire( location)));

      if (i % SKIP == 0) {
        format.format(" %3d qsize= %3d ",i, q.size());
        cache.showStats(format);
      }
    }

    format.format("awaitTermination 10 secs qsize= %3d\n", q.size());
    cache.showStats(format);
    exec.awaitTermination(10, TimeUnit.SECONDS);
    format.format("done qsize= %4d\n", q.size());
    cache.showStats(format);

    int total = 0;
    int total_locks = 0;
    HashSet<Object> checkUnique = new HashSet<Object>();
    map = cache.getCache();
    for (Object key : map.keySet()) {
      assert !checkUnique.contains(key);
      checkUnique.add(key);
      NetcdfFileCache.CacheElement elem = map.get(key);
      int locks = 0;
      for (NetcdfFileCache.CacheElement.CacheFile file : elem.list)
        if (file.isLocked.get()) locks++;
      //System.out.println(" key= "+key+ " size= "+elem.list.size()+" locks="+locks);
      total_locks += locks;
      total += elem.list.size();
    }
    System.out.println(" total="+total+" total_locks="+total_locks);
//    assert total_locks == map.keySet().size();

    cache.clearCache( false);
    format.format("after cleanup qsize= %4d\n", q.size());
    cache.showStats(format);
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
         consume( queue.poll());
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

  class CallAcquire implements Callable<NetcdfFile> {
    String location;
    CallAcquire(String location) { this.location = location; }
    public NetcdfFile call() throws Exception {
      return cache.acquire(location, null);
    }
  }

  class RunClose implements Runnable {
    NetcdfFile f;
    RunClose(NetcdfFile f) { this.f = f; }
    public void run() {
      try {
        f.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
