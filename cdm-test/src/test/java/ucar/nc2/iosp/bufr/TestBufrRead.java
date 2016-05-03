/* Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;

/**
 * Sanity check on reading bufr messages
 *
 * @author caron
 * @since Apr 1, 2008
 */
@Category(NeedsCdmUnitTest.class)
public class TestBufrRead {
  String unitDir =  TestDir.cdmUnitTestDir + "formats/bufr";
  boolean show = false;

  class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      if (pathname.getPath().indexOf("exclude") > 0) return false;
      if (pathname.getName().endsWith(".bfx")) return false;
      if (pathname.getName().endsWith(".jpg")) return false;
      return true;
    }
  }

  @Test
  public void bitCountAllInUnitTestDir() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(unitDir, new MyFileFilter(), new TestDir.Act() {
      public int doAct(String filename) throws IOException {
        return bitCount(filename);
      }
    }, true);
    System.out.println("***BitCount " + count + " records");
  }

  @Test
  public void openAllInUnitTestDir() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(unitDir, new MyFileFilter(), new TestDir.Act() {
      public int doAct(String filename) throws IOException {
        openNetcdf(filename);
        return 1;
      }
    }, true);
    System.out.println("***Opened " + count + " files");
  }

    // @Test
  public void problem() throws IOException {
    openNetcdf("Q:\\cdmUnitTest\\formats\\bufr\\US058MCUS-BUFtdp.SPOUT_00011_buoy_20091101021700.bufr");
  }


  // @Test
  public void bitCountAllInIddDir() throws IOException {
    int count = 0;
    assert 13852 == (count = bitCount(TestDir.cdmUnitTestDir + "formats/bufr/exclude/uniqueIDD.bufr")) : count; // was 12337
    assert 11249 == (count = bitCount(TestDir.cdmUnitTestDir + "formats/bufr/exclude/uniqueBrasil.bufr")) : count;  // was 11533
    assert 22710 == (count = bitCount(TestDir.cdmUnitTestDir + "formats/bufr/exclude/uniqueExamples.bufr")) : count; // was 12727
    assert 9929 == (count = bitCount(TestDir.cdmUnitTestDir + "formats/bufr/exclude/uniqueFnmoc.bufr")) : count;
  }

  public void utestCountMessages() throws IOException {
    int count = 0;
    count += bitCount(TestDir.cdmUnitTestDir + "formats/bufr/uniqueIDD.bufr");
    //count += readBufr(TestAll.cdmUnitTestDir + "formats/bufr/uniqueBrasil.bufr");
    //count += readBufr(TestAll.cdmUnitTestDir + "formats/bufr/uniqueExamples.bufr");
    //count += readBufr(TestAll.cdmUnitTestDir + "formats/bufr/uniqueFnmoc.bufr");
    System.out.printf("total read ok = %d%n",count);
  }

  private int bitCount(String filename) throws IOException {
    System.out.printf("%n***bitCount bufr %s%n", filename);
    int count = 0;
    int totalObs = 0;
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(filename, "r");

      MessageScanner scan = new MessageScanner(raf, 0, true);
      while (scan.hasNext()) {
        try {
          
          Message m = scan.next();
          if (m == null) continue;
          int nobs = m.getNumberDatasets();
          if (show) System.out.printf(" %3d nobs = %4d (%s) center = %s table=%s cat=%s ", count++, nobs, m.getHeader(),
                  m.getLookup().getCenterNo(), m.getLookup().getTableName(), m.getLookup().getCategoryNo());
          assert m.isTablesComplete() : "incomplete tables";

          if (nobs > 0) {
            if (!m.isBitCountOk()) {
              Formatter f = new Formatter();
              int n = m.calcTotalBits(f);
              FileOutputStream out = new FileOutputStream("C:/tmp/bitcount.txt");
              IO.writeContents(f.toString(), out);
              out.close();
              System.out.printf("  nbits = %d%n", n);
            }
            assert m.isBitCountOk() : "bit count wrong on " + filename;
          }

          totalObs += nobs;
          if (show) System.out.printf("%n");

        } catch (Exception e) {
          e.printStackTrace();
          assert true : e.getMessage();
        }

      }

    } finally {
      if (raf != null)
        raf.close();
    }

    return totalObs;
  }

  private void openNetcdf(String filename) throws IOException {
    System.out.printf("%n***openNetcdf bufr %s%n", filename);
    NetcdfFile ncfile = null;
    try {
      ncfile = NetcdfFile.open(filename);
      System.out.printf("%s%n", ncfile);
    } finally {
      if (ncfile != null) ncfile.close();
    }
  }



}
