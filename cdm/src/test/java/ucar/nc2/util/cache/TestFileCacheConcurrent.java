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

import junit.framework.TestCase;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author caron
 * @since May 31, 2008
 */
public class TestFileCacheConcurrent extends TestCase {

  public TestFileCacheConcurrent(String name) {
    super(name);
  }

  FileCache cache;
  FileFactory factory = new MyFileFactory();

  protected void setUp() throws java.lang.Exception {
    cache = new FileCache(50, 100, 20);
  }

  class MyFileFactory implements FileFactory {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      return NetcdfDataset.openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }


  void loadFiles(File dir, List<String> result) {
    for (File f : dir.listFiles()) {
      if (f.isDirectory())
        loadFiles(f, result);

      else if (f.getPath().endsWith(".nc") && f.length() > 0) {
        //System.out.println(" open "+f.getPath());
        String want = StringUtil.replace(f.getPath(), '\\', "/");
        result.add(want);
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  int N = 100000;
  int SKIP = 10000;
  int CLIENT_THREADS = 100;
  int WAIT_MAX = 25; // msecs

  public void testConcurrentAccess() throws InterruptedException {
    cache.debugCleanup = true;

    // load some files into the cache
    List<String> fileList = new ArrayList<String>(100);
    loadFiles(new File("D:/AStest/aggManyFiles/"), fileList);
    int nfiles = fileList.size();
    System.out.println(" loaded " + nfiles + " files");

    Random r = new Random();
    ExecutorService pool = Executors.newFixedThreadPool(CLIENT_THREADS);
    for (int i = 0; i < N; i++) {
      // pick a file at random
      int findex = r.nextInt(nfiles);
      int wait = r.nextInt(WAIT_MAX);
      String location = fileList.get(findex);
      pool.submit(new CallAcquire(location, wait));
      if (i % SKIP == 0) System.out.printf(" submit %d%n", i);
    }

    pool.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(600, TimeUnit.SECONDS)) {
        pool.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(60, TimeUnit.SECONDS))
            System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      pool.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }


  }

  AtomicInteger done = new AtomicInteger();

  // as files are acquired, check that they are locked and then release them
  class CallAcquire implements Runnable {
    String location;
    int wait;

    CallAcquire(String location, int wait) {
      this.location = location;
      this.wait = wait;
    }

    public void run() {
      try {
        //System.out.printf("acquire %s%n", location);
        FileCacheable fc = cache.acquire(factory, location, null);
        NetcdfFile ncfile = (NetcdfFile) fc;
        assert !ncfile.isUnlocked();
        assert (null != ncfile.getIosp());
        Thread.sleep( wait);
        ncfile.close();
        int d = done.incrementAndGet();
        if (d % SKIP == 0) System.out.printf(" done %d%n", d);

      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (InterruptedException e) {
        return;
      }

    }
  }
}
