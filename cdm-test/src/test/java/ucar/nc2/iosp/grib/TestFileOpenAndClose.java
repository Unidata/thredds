/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp.grib;

import org.junit.Test;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Keep opening files until we get an error
 *
 * @author caron
 * @since 11/4/2014
 */
public class TestFileOpenAndClose {

  //@Test
  public void testNFilesOpen() throws IOException {
    File topDir = new File(TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2");
    openAll(topDir);
  }

  //@Test
  public void testNFilesOpenAndClose() throws IOException {
    File topDir = new File(TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2");
    openAndClose(topDir);
  }


  int count = 0;
  void openAll(File dir) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) return;
    System.out.printf("%n%s%n", dir.getPath());
    for (File f :files) {
      if (f.isDirectory()) openAll(f);
      else try {
        RandomAccessFile raf = new RandomAccessFile(f.getPath(), "r");
        count++;

      } catch (IOException ioe) {
        System.out.printf("%s count = %d%n", ioe.getMessage(), count);
        throw ioe;
      }
    }
  }

  void openAndClose(File dir) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) return;
    System.out.printf("%n%s%n", dir.getPath());

    for (File f :files) {
      if (f.isDirectory()) {
        openAndClose(f);
      } else {

        try (RandomAccessFile raf = new RandomAccessFile(f.getPath(), "r")) {
          count++;
          System.out.printf("%d ", count);
          /* if (count % 10000 == 0) {
            Thread.currentThread();
            Thread.sleep(3000);
            System.out.printf("sleep%n");
          } */

        } catch (IOException ioe) {
          System.out.printf("%n%s count = %d%n", ioe.getMessage(), count);
          throw ioe;

        } /* catch (InterruptedException e) {
          e.printStackTrace();
        } */

      }

    }
  }

}
