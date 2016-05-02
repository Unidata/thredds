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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author caron
 * @since May 31, 2008
 */
public class TestFileCacheConcurrent {

  FileCacheIF cache = new FileCache(50, 100, 30);
  FileFactory factory = new MyFileFactory();

  class MyFileFactory implements FileFactory {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      return NetcdfDataset.openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }

  static public void makeFileList(File dir, String suffix, List<String> result) {
    File[] files = dir.listFiles();
    if (files == null) {
      assert false;
      return;
    }
    for (File f : files) {
      if (f.isDirectory() && !f.getName().equals("exclude")) {
        makeFileList(f, suffix, result);

      } else if (f.getPath().endsWith(suffix) && f.length() > 0) {
        //System.out.println(" open "+f.getPath());
        String want = StringUtil2.replace(f.getPath(), '\\', "/");
        result.add(want);
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  int PRINT_EVERY = 1000;
  int CLIENT_THREADS = 50;
  int WAIT_MAX = 25; // msecs
  int MAX_TASKS = 1000; // bounded queue
  int NSAME = 3; // submit same file n consecutive

  @Test
  public void testConcurrentAccess() throws InterruptedException {
    System.out.printf("TestFileCacheConcurrent%n");
    // load some files into the cache
    List<String> fileList = new ArrayList<>(100);
    makeFileList(new File(TestDir.cdmLocalTestDataDir), "nc", fileList);
    int nfiles = fileList.size();
    System.out.println(" loaded " + nfiles + " files");

    Random r = new Random();
    ArrayBlockingQueue q = new ArrayBlockingQueue(MAX_TASKS);
    ThreadPoolExecutor pool = new ThreadPoolExecutor(CLIENT_THREADS, CLIENT_THREADS, 100, TimeUnit.SECONDS, q);

    int count = 0;
    while (count < 100) {
      if (q.remainingCapacity() > NSAME) {
        // pick a file at random
        String location = fileList.get(r.nextInt(nfiles));

        for (int i = 0; i < NSAME; i++) {
          count++;
          pool.submit(new CallAcquire(location, r.nextInt(WAIT_MAX)));

          if (count % PRINT_EVERY == 0) {
            Formatter f = new Formatter();
            cache.showStats(f);
            System.out.printf(" submit %d queue= %d cache= %s%n", count, q.size(), f);
          }
        }
      } else {
        Thread.sleep(100);        
      }
    }

    /* pool.shutdown(); // Disable new tasks from being submitted
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
    } */


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
        FileCacheable fc = cache.acquire(factory, location);
        NetcdfFile ncfile = (NetcdfFile) fc;
        //assert ncfile.isLocked();
        assert (null != ncfile.getIosp());
        Thread.sleep(wait);
        ncfile.close();
        int d = done.incrementAndGet();
        if (d % PRINT_EVERY == 0) System.out.printf(" done %d%n", d);

      } catch (InterruptedException e) {
        return;

      } catch (Throwable e) {
        System.out.println(" fail="+e.getMessage());
        e.printStackTrace();
        assert false;
      }

    }
  }
}
