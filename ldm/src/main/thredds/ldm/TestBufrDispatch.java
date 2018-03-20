/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ldm;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 8, 2008
 */
public class TestBufrDispatch {

  interface MClosure {
    void run(String filename) throws IOException;
  }

  void test(String filename, MClosure closure) throws IOException {
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

  void test(String filename, String startsWith, MClosure closure) throws IOException {
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

  void testAllInDir(File dir, String startsWith,  MClosure closure) {
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

  void testAllInDir(File dir, MClosure closure) {
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

  void scan(String filename, MessageBroker broker) throws IOException {
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

  TestBufrDispatch()  {
  }

  MessageBroker broker;
  public void setMessageBroker( MessageBroker broker) {
    this.broker = broker;
  }

  private String testDir;
   public void setTestDir( String testDir) {
    this.testDir = testDir;
  }

  void doScan() throws IOException {

    //ExecutorService executor = Executors.newFixedThreadPool(5);
    //final MessageBroker broker = new MessageBroker(executor);

    long start = System.nanoTime();

    //test("D:/bufr/ncepBug2/20081008_0800.bufr", new MClosure() {
    test(testDir, new MClosure() {
    //test("D:/bufr/nlode/snap080808/","20080805", new MClosure() {
    //test("D:/bufr/nlode/snap080808/", new MClosure() {
    //test("D:/bufr/dispatch/fslprofilers/fslprofilers-2008-7-28.bufr", new MClosure() {
        public void run(String filename) throws IOException {
          System.out.println("scan "+filename);
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

  public static void main(String args[]) throws IOException, InterruptedException {
    ApplicationContext springContext =
        new FileSystemXmlApplicationContext("file:C:/dev/tds/thredds/ldm/src/main/thredds/ldm/application-config.xml");

    TestBufrDispatch driver = (TestBufrDispatch) springContext.getBean("testDriver");
    driver.doScan();
  }

}
