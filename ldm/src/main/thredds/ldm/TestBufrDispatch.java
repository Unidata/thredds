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
