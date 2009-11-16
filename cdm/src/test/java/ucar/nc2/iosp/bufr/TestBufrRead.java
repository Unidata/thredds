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
 * Class Description.
 *
 * @author caron
 * @since Apr 1, 2008
 */
public class TestBufrRead extends TestCase {

  public void testReadAll() throws IOException {
    //readandCountAllInDir(testDir, null);
    int count = 0;
    count += TestAll.readAllDir("C:/data/bufr2/mlode/", new MyFileFilter());
    System.out.println("***READ " + count + " files");
  }

  class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return !pathname.getName().endsWith(".bfx");
    }
  }

  public void testReadAllInDir() throws IOException {
    //readandCountAllInDir(testDir, null);
    int count = 0;
    count += TestAll.actOnAll("C:/data/formats/bufr3/ISIS01.bufr", new MyFileFilter(), new TestAll.Act() {
      public int doAct(String filename) throws IOException {
        return readBufr(filename);
      }
    });
    System.out.println("***READ " + count + " files");
  }

  public void testReadOneBufrMessage() throws IOException {
     readBufr("C:/data/formats/bufr3/ISIS01.bufr");
  }

  private int readBufr(String filename) throws IOException {
    boolean oneRecord = true;
    boolean getData = false;

    // Reading of Bufr files must be inside a try-catch block
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    MessageScanner scan = new MessageScanner(raf);
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      int totalObs = m.getNumberDatasets();
      System.out.println("Total number observations =" + totalObs);
      m.calcTotalBits( null); // new Formatter(System.out));
      m.showCounters( new Formatter(System.out));
      break;
    }
    return 1;
  }

  public void testCompressedSequence() throws IOException {
    NetcdfFile ncfile = NetcdfFile.open("C:/data/formats/bufr3//JUTX52.bufr");
    Variable v = ncfile.findVariable("obsRecord");
    v.read();
    ncfile.close();
  }

}
