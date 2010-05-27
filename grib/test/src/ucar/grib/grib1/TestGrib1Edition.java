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
package ucar.grib.grib1;

import junit.framework.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.grib.*;

import java.io.IOException;

/**
 * Some grib tests.
 */
public final class TestGrib1Edition extends TestCase {
  private String dataPath;
  private String testPath;

  protected final void setUp() {
    dataPath = TestAll.testdataDir + "grid/grib/grib1/data/";
    testPath = TestAll.testdataDir + "grid/grib/grib1/test/";
  }

  /**
   * gives status of the test.
   *
   * @return status
   */
  public static Test suite() {
    return new TestSuite(TestGrib1Edition.class);
  }

  /**
   * test the Grib edition of the files.
   *
   * @throws IOException
   */
  public final void testEdition() throws IOException {
    RandomAccessFile raf;
    int edition = 0;
    System.out.println("\nTesting GRIB edition of RUC.wmo");
    raf = new RandomAccessFile(dataPath + "RUC.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("RUC.wmo edition", 1, edition);
    System.out.println("\nTesting GRIB edition of AVN.wmo");
    raf = new RandomAccessFile(dataPath + "AVN.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("AVN.wmo edition", 1, edition);
    System.out.println("\nTesting GRIB edition of MRF.wmo");
    raf = new RandomAccessFile(dataPath + "MRF.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("MRF.wmo edition", 1, edition);
    System.out.println("\nTesting GRIB edition of OCEAN.wmo");
    raf = new RandomAccessFile(dataPath + "OCEAN.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("OCEAN.wmo edition", 1, edition);
    System.out.println("\nTesting GRIB edition of RUC2.wmo");
    raf = new RandomAccessFile(dataPath + "RUC2.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("RUC2.wmo edition", 1, edition);
    System.out.println("\nTesting GRIB edition of WAVE.wmo");
    raf = new RandomAccessFile(dataPath + "WAVE.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("WAVE.wmo edition", 1, edition);
    System.out.println("\nTesting GRIB edition of ecmf.wmo");
    raf = new RandomAccessFile(dataPath + "ecmf.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("ecmf.wmo edition", 1, edition);
    System.out.println("\nTesting GRIB edition of ensemble.wmo");
    raf = new RandomAccessFile(dataPath + "ensemble.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("ensemble.wmo edition", 1, edition);
    System.out.println("\nTesting GRIB edition of extended.wmo");
    raf = new RandomAccessFile(dataPath + "extended.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("extended.wmo edition", 1, edition);
    System.out.println("\nTesting GRIB edition of thin.wmo");
    raf = new RandomAccessFile(dataPath + "thin.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("thin.wmo edition", 1, edition);
    System.out.println("\nTesting GRIB edition of ukm.wmo");
    raf = new RandomAccessFile(dataPath + "ukm.wmo", "r");
    edition = GribChecker.getEdition(raf);
    assertEquals("ukm.wmo edition", 1, edition);
  }

  /**
   * main.
   *
   * @param args
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
