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
package ucar.grib.grib2;

import junit.framework.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.grib.grib1.*;
import ucar.grib.*;

import java.io.IOException;
import java.io.File;

/**
 * Grib Editions tests.
 */
public final class TestGrib2Edition extends TestCase {
  private String dataPath;
  private String testPath;

  protected final void setUp() {
    dataPath = TestAll.testdataDir + "grid/grib/grib2/data/";
    testPath = TestAll.testdataDir + "grid/grib/grib2/test/";
  }

  /**
   * run Grib edition tests.
   *
   * @return results of test
   */
  public static Test suite() {

    /*
       * the dynamic way
       */
    return new TestSuite(TestGrib2Edition.class);
  }

  /**
   * .
   *
   * @throws IOException
   */
  public final void testEdition() throws IOException {
    RandomAccessFile raf;
    int edition = 0;
    System.out.println("Testing GRIB edition of ndfd.wmo");
    raf = new RandomAccessFile(dataPath + "ndfd.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("ndfd.wmo edition", 2, edition);

    System.out.println("Testing GRIB edition of eta2.wmo");
    raf = new RandomAccessFile(dataPath + "eta2.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("eta2.wmo edition", 2, edition);

    System.out.println("Testing GRIB edition of AVN.5deg.wmo");
    raf = new RandomAccessFile(dataPath + "AVN.5deg.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("eta2.wmo edition", 2, edition);

    System.out.println("Testing GRIB edition of CLDGRIB2.2005040905");
    raf = new RandomAccessFile(dataPath + "CLDGRIB2.2005040905", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("eta2.wmo edition", 2, edition);
  }
  //public void testEquals() {
  //assertEquals(12, 12);
  //assertEquals(12L, 12L);
  //assertEquals(new Long(12), new Long(12));

  //assertEquals("Size", 12, 13);
  //assertEquals("Capacity", 12.0, 11.99, 0.0);
  //}

  /**
   * main.
   *
   * @param args editon tests to run
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
