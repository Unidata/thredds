/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.grib;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;

/**
 * Keep opening files until we get an error
 *
 * @author caron
 * @since 11/4/2014
 */
public class TestFileOpenAndClose {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
