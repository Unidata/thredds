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

package thredds.ldm;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 8, 2008
 */
public class TestDriver {

  interface MClosure {
    void run(String filename) throws IOException;
  }

  static void test(String filename, MClosure closure) throws IOException {
    File f = new File(filename);
    if (!f.exists()) {
      System.out.println(filename + " does not exist");
      return;
    }
    if (f.isDirectory()) testAllInDir(f, closure);
    else {
      try {
        closure.run(f.getPath());
      } catch (Exception ioe) {
        System.out.println("Failed on " + f.getPath() + ": " + ioe.getMessage());
        ioe.printStackTrace();
      }
    }
  }

  static void test(String filename, String startsWith, MClosure closure) throws IOException {
    File f = new File(filename);
    if (!f.exists()) {
      System.out.println(filename + " does not exist");
      return;
    }
    if (f.isDirectory()) testAllInDir(f, startsWith, closure);
    else {
      try {
        closure.run(f.getPath());
      } catch (Exception ioe) {
        System.out.println("Failed on " + f.getPath() + ": " + ioe.getMessage());
        ioe.printStackTrace();
      }
    }
  }

  static void testAllInDir(File dir, String startsWith,  MClosure closure) {
    List<File> list = Arrays.asList(dir.listFiles());
    Collections.sort(list);

    for (File f : list) {
      if (!f.getName().startsWith(startsWith)) continue;

      if (f.isDirectory())
        testAllInDir(f, closure);
      else {
        try {
          closure.run(f.getPath());
        } catch (Exception ioe) {
          System.out.println("Failed on " + f.getPath() + ": " + ioe.getMessage());
          ioe.printStackTrace();
        }
      }
    }
  }

  static void testAllInDir(File dir, MClosure closure) {
    List<File> list = Arrays.asList(dir.listFiles());
    Collections.sort(list);

    for (File f : list) {
      if (f.getName().endsWith("bfx")) continue;
      if (f.getName().endsWith("txt")) continue;
      if (f.getName().endsWith("zip")) continue;
      if (f.getName().endsWith("csh")) continue;
      if (f.getName().endsWith("rtf")) continue;

      if (f.isDirectory())
        testAllInDir(f, closure);
      else {
        try {
          closure.run(f.getPath());
        } catch (Exception ioe) {
          System.out.println("Failed on " + f.getPath() + ": " + ioe.getMessage());
          ioe.printStackTrace();
        }
      }
    }
  }

  static void scan(String filename, MessageBroker broker) throws IOException {
    System.out.println("Read from file "+filename);
    FileInputStream fin = new FileInputStream( filename);

    long start = System.nanoTime();
    int startMess = broker.total_msgs;

    broker.process(fin);

    long stop = System.nanoTime();
    int n = broker.total_msgs - startMess;

    double secs = 1.0e-9 * (stop - start);
    double rate = n / secs;
    System.out.printf("%s done reading %d in %.1f secs, rate = %.0f msg/sec %n",filename, n, secs, rate);
  }

  public static void main(String args[]) throws IOException, InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(5);
    final MessageBroker broker = new MessageBroker(executor);

    long start = System.nanoTime();

    test("D:/bufr/out/RJTD-IUCN53-1.bufr", new MClosure() {
    // test("D:/bufr/nlode/snap080808/20080805_0100.bufr", new MClosure() {
    //test("D:/bufr/nlode/snap080808/","20080805", new MClosure() {
    //test("D:\\bufr\\nlode\\snap080808", new MClosure() {
    //test("C:/data/bufr2/mlode/mlodeSorted", new MClosure() {
        public void run(String filename) throws IOException {
          scan(filename, broker);
        }
      });

    long stop = System.nanoTime();
    int n = broker.total_msgs;
    double secs = 1.0e-9 * (stop - start);
    double rate = n / secs;
    System.out.printf(" done reading %d (bad %d) in %.1f secs, rate = %.0f msg/sec %n",n, broker.bad_msgs, secs, rate);

    broker.exit();

    stop = System.nanoTime();
    secs = 1.0e-9 * (stop - start);
    rate = n / secs;
    System.out.printf(" final exit %.1f secs, rate = %.0f msg/sec %n", secs, rate);

  }


}
