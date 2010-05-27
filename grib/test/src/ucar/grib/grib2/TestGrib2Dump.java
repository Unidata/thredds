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
import ucar.grib.*;

import java.io.IOException;
import java.io.File;

/**
 * Some grib tests.
 */
public final class TestGrib2Dump extends TestCase {
  private final boolean reset = false;
  private String dataPath;
  private String testPath;
  private Diff d;

  protected final void setUp() {
    dataPath = TestAll.testdataDir + "grid/grib/grib2/data/";
    testPath = TestAll.testdataDir + "grid/grib/grib2/test/";
    d = new Diff();
  }

  /**
   * .
   *
   * @return results of test
   */
  public static Test suite() {

    /*
       * the dynamic way
       */
    return new TestSuite(TestGrib2Dump.class);
  }

  /**
   * .
   */
  public final void testDump() {

    System.out.println("\nTesting header dump of ndfd.wmo");
    String args[] = new String[2];
    args[0] = dataPath + "ndfd.wmo";
    args[1] = "NdfdDump";
    Grib2Dump.main(args);
    d.doDiff(testPath + "NdfdDump.test", "NdfdDump");
    File f = new File("NdfdDump");
    f.delete();
    //System.out.println( "reset =" + reset );
    if (reset) {
      args[1] = testPath + "NdfdDump.test";
      System.out.println("Creating Dump " + args[1]);
      Grib2Dump.main(args);
    }

    System.out.println("\nTesting header dump of AVN.5deg.wmo");
    args[0] = dataPath + "AVN.5deg.wmo";
    args[1] = "AVN.5degDump";
    Grib2Dump.main(args);
    d = new Diff();
    d.doDiff(testPath + "AVN.5degDump.test", "AVN.5degDump");
    f = new File("AVN.5degDump");
    f.delete();
    if (reset) {
      args[1] = testPath + "AVN.5degDump.test";
      Grib2Dump.main(args);
    }

    System.out.println("\nTesting header dump of CLDGRIB2.2005040905");
    args[0] = dataPath + "CLDGRIB2.2005040905";
    args[1] = "CLDGRIB2Dump";
    Grib2Dump.main(args);
    d = new Diff();
    d.doDiff(testPath + "CLDGRIB2Dump.test", "CLDGRIB2Dump");
    f = new File("CLDGRIB2Dump");
    f.delete();
    if (reset) {
      args[1] = testPath + "CLDGRIB2Dump.test";
      Grib2Dump.main(args);
    }

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
   * @param args dump test to run
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
