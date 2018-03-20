/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.cdmr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.IO;

import java.io.InputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class NcstreamThreads {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  //http://motherlode.ucar.edu:8081/thredds/ncstream/fmrc/NCEP/GFS/Global_0p5deg/files/GFS_Global_0p5deg_20090303_0000.grib2?showForm
  static String urlStart = "http://motherlode.ucar.edu:8081/thredds/ncstream/fmrc/NCEP/GFS/Global_0p5deg/files/GFS_Global_0p5deg_";

  //   float Absolute_vorticity(time=61, pressure=26, lat=361, lon=720);
  static String[] flds = new String[] {"Absolute_vorticity", "Temperature", "Geopotential_height", "U-component_of_wind",
    "V-component_of_wind"};

  static int current_dataset = 0;
  static String nextDataset() {
    return "20090305_0000";
  }

  static AtomicInteger current_field = new AtomicInteger(0);
  static String nextField() {
    if (current_field.get() >= flds.length) {
      current_field.set(0);
      current_dataset++;
    }
    return flds[current_field.get()];
  }

  static AtomicInteger current_level = new AtomicInteger();
  static int nextLevel() {
    if (current_level.get() >= 26) {
      current_level.set(0);
      current_field.incrementAndGet();
    }
    return current_level.get();
  }

  static AtomicInteger current_time = new AtomicInteger();
  static int nextTime() {
    if (current_time.get() >= 61) {
      current_time.set(0);
      current_level.incrementAndGet();
    }
    return current_time.getAndIncrement();
  }

  static String nextUrl() {
    int t = nextTime();
    int v = nextLevel();
    return urlStart + nextDataset() + ".grib2?" + nextField() + "("+t+","+v+",:,:)";
  }

  static boolean show = true;
  static int counter = 0;

  public static void main(String[] args) throws Exception {

    //        final String urlToFetch = ramaddaUrl;
    final int[] threadsRunning = {0};

    final int numReads = 2;
    for (int threadCnt = 1; threadCnt <= 2; threadCnt++) {
    //int threadCnt = 10;

    ArrayList<Thread> threads = new ArrayList<Thread>();

    for (int i = 0; i < threadCnt; i++) {
      final int threadId = i;
      // System.err.println("   thread  #" + threadId + " created ");

      threads.add(new Thread(new Runnable() {
        final int who = counter++;

        public void run() {
          try {
            for (int i = 0; i < numReads; i++) {
              String urls = nextUrl();
              URL url = new URL(urls);

              if (show) System.out.printf("%d %d Send %s%n", who, i, urls);
              InputStream inputStream = url.openConnection().getInputStream();
              long size = IO.copy2null(inputStream, 10 * 1000);

              if (show) System.out.printf(" data size= %d%n",size);
              //System.out.printf(who + "end= %d%n", System.currentTimeMillis());
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

      //System.err.println("Starting " + threadCnt + " threads each fetching the URL " + numReads + " times");
      threadsRunning[0] = 0;
      for (Thread thread : threads) {
        synchronized (threadsRunning) {
          threadsRunning[0]++;
        }

        thread.start();
      }

      long t1 = System.currentTimeMillis();
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
      System.err.println("#threads, total time : " + threadCnt+", "+(t2 - t1));
    }
  }

}
