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

import java.io.File;

/**
 * @author caron
 * @since Mar 21, 2009
 */
public class TestFileSystem {
  static long touch = 0;
  static boolean readLast = false;

  int readDir(File f) {
    File[] subs = f.listFiles();
    if (subs == null) return 0;
    int count = subs.length;
    for (File sub : subs) {
      if (sub.isDirectory())
        count += readDir(sub);
      if (readLast)
        touch += sub.lastModified();
      touch += sub.getPath().hashCode();
    }
    return count;
  }

  int timeDir(File f) {
    int total = 0;
    System.out.printf("file                     count  msecs%n");
    File[] subs = f.listFiles();
    for (File sub : subs) {
      if (sub.isDirectory()) {
        long start = System.nanoTime();
        int count = readDir(sub);
        long end = System.nanoTime();
        System.out.printf("%-20s, %8d, %f%n", sub.getPath(), count, (end - start) / 1000 / 1000.0);
        total += count;
      }
    }
    return total;
  }

  static public void main(String args[]) {
    String root = "C:/";

    for (int i=0; i<args.length; i++) {
      // root=C:/dadsd
      if (args[i].startsWith("root")) {
        int pos = args[i].indexOf('=');
        if (pos > 0) {
          root = args[i].substring(pos+1);
          System.out.printf("root=%s ",root);
        }

        if (args[i].equals("usage")) {
          System.out.printf("java -classpath {jar} ucar.nc2.util.cache.TestFileSystem root={rootDir} [readLastModified] -%n");
          System.exit(0);
        }
      }

      if (args[i].equals("readLastModified")) {
        readLast = true;
        System.out.printf(" readLastModified ");
      }
    }
    System.out.printf(" %n%n");


    TestFileSystem test = new TestFileSystem();

    long start = System.nanoTime();
    int total = test.timeDir(new File(root));
    long end = System.nanoTime();

    System.out.printf("%n%-20s, %8d, %f usecs%n", root, total, (end - start) / 1000 / 1000.0);
    System.out.printf("%n %d %n", touch);
  }

}
