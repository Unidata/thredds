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

package ucar.nc2.iosp.bufr;

import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.Formatter;

import junit.framework.TestCase;

/**
 * Saanity check on bufr messages
 *
 * @author caron
 * @since Apr 1, 2008
 */
public class TestBufrRead extends TestCase {

  class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return !pathname.getName().endsWith(".bfx");
    }
  }

  public void utestReadAllInDir() throws IOException {
    int count = 0;
    count += TestAll.actOnAll(TestAll.cdmUnitTestDir + "iosp/bufr", new MyFileFilter(), new TestAll.Act() {
      public int doAct(String filename) throws IOException {
        return readBufr(filename);
      }
    });
    System.out.println("***READ " + count + " files");
  }

  public void testReadMessages() throws IOException {
    int count = 0;
    assert 5519 == (count = readBufr(TestAll.cdmUnitTestDir + "iosp/bufr/uniqueIDD.bufr")) : count;
    assert 11533 == (count = readBufr(TestAll.cdmUnitTestDir + "iosp/bufr/uniqueBrasil.bufr")) : count;
    assert 12727 == (count = readBufr(TestAll.cdmUnitTestDir + "iosp/bufr/uniqueExamples.bufr")) : count;
    assert 9929 == (count = readBufr(TestAll.cdmUnitTestDir + "iosp/bufr/uniqueFnmoc.bufr")) : count;
  }

  private int readBufr(String filename) throws IOException {
    System.out.printf("%n***READ bufr %s%n", filename);
    int count = 0;
    int totalObs = 0;
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(filename, "r");

      MessageScanner scan = new MessageScanner(raf);
      while (scan.hasNext()) {
        try {
          
          Message m = scan.next();
          if (m == null) continue;
          int nobs = m.getNumberDatasets();
          System.out.printf(" %3d nobs = %4d %s", count++, nobs, m.getHeader());
          if (m.isTablesComplete()) {
            if (m.isBitCountOk()) {
              totalObs += nobs;
              System.out.printf("%n");
            } else
              System.out.printf(" BITS NOT OK%n");                                                           
          } else
            System.out.printf(" TABLES NOT COMPLETE%n");

        } catch (Exception e) {
          System.out.printf(" CANT READ %n");
          e.printStackTrace();
        }

      }

    } finally {
      if (raf != null)
        raf.close();
    }

    return totalObs;
  }


}
