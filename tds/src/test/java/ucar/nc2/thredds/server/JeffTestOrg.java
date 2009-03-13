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

package ucar.nc2.thredds.server;

/**
 * Class Description.
 *
 * @author caron
 * @since Mar 13, 2009
 */

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class JeffTestOrg {


  public static void main(String[] args) throws Exception {
    final String ramaddaUrl =
            "http://motherlode.ucar.edu/repository/entry/show/output:data.opendap/entryid:733e04b2-1669-4b6e-8867-92aec1fbd286/dodsC/entry.das.dods?Temperature%5B0:1:0%5D%5B0:1:0%5D%5B0:1:64%5D%5B0:1:92%5D";

    String tdsVolumeUrl =
            "http://motherlode.ucar.edu:8081/thredds/dodsC/model/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20090313_1200.grib1.dods?Temperature%5B0:1:0%5D%5B0:1:18%5D%5B0:1:64%5D%5B0:1:92%5D";


    final String urlToFetch = tdsVolumeUrl;
    //        final String urlToFetch = ramaddaUrl;
    final int[] threadsRunning = {0};

    final int numReads = 10;
    for (int threadCnt = 1; threadCnt < 10; threadCnt++) {
      ArrayList<Thread> threads = new ArrayList<Thread>();
      for (int i = 0; i < threadCnt; i++) {
        final int threadId = i;
        final int nthreads = threadCnt;
        threads.add(new Thread(new Runnable() {
          public void run() {
            try {
              //System.err.println("   thread  #" + threadId + " started");
              for (int i = 0; i < numReads; i++) {
                URL url = new URL(urlToFetch);
                InputStream inputStream =
                        url.openConnection().getInputStream();
                byte[] buffer = new byte[10000];
                int totalRead = 0;
                int read = 0;
                while ((read = inputStream.read(buffer)) > 0) {
                  totalRead += read;
                }
                //                                System.err.println ("    jobend read:" + totalRead +" bytes");
              }

              //System.err.println("   thread  #" + threadId + " done");
            } catch (Exception exc) {
              exc.printStackTrace();
            } finally {
              synchronized (threadsRunning) {
                threadsRunning[0]--;
              }
            }
          }
        }));
      }

      System.err.println("Starting " + threadCnt + " threads each fetching the URL " + numReads + " times");
      long t1 = System.currentTimeMillis();
      threadsRunning[0] = 0;
      for (Thread thread : threads) {
        synchronized (threadsRunning) {
          threadsRunning[0]++;
        }

        thread.start();
      }
      while (true) {
        synchronized (threadsRunning) {
          if (threadsRunning[0] <= 0) {
            break;
          }
        }
        try {
          Thread.currentThread().sleep(1);
        } catch (Exception exc) {
        }
      }
      long t2 = System.currentTimeMillis();
      System.err.println("     total time: " + (t2 - t1) + " # threads:" + threadCnt);
    }

  }


}


