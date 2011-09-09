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
package ucar.grib;

import junit.framework.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * TestSuite that runs all the sample tests.
 * not used
 *
 */
public class TestAll {
  static public String testdataDir = ucar.nc2.TestAll.cdmUnitTestDir;

    /**
     * .
     * @return results of tests
     */
  public static junit.framework.Test suite ( ) {

    //  Grib 1 testing
    TestSuite suite= new TestSuite();
    suite.addTest( ucar.grib.grib1.TestGrib1Edition.suite());
    //suite.addTest( ucar.grib.grib1.TestGrib1Dump.suite());
    suite.addTest( ucar.grib.grib1.TestGrib1Indexer.suite());
    suite.addTest( ucar.grib.grib1.TestGrib1Data.suite());

    //  Grib 2 testing
    suite.addTest( ucar.grib.grib2.TestGrib2Edition.suite());
    suite.addTest( ucar.grib.grib2.TestGrib2Dump.suite());
    suite.addTest( ucar.grib.grib2.TestGrib2Indexer.suite());
    suite.addTest( ucar.grib.grib2.TestGrib2Data.suite()); // */

    // Test for record duplication in a Grib file
    suite.addTest(  ucar.grib.TestForDupsInGribIndex.suite() );
      
      // suite.addTest( new JUnit4TestAdapter( ucar.grib.grib2.Grib2TablesTest.class ) );


     //  suite.addTest( ucar.grib.TestCompareGribPDSGDSsections.suite()); // */


    return suite;
  }

    /**
     * main.
     * @param args suites of tests
     */
  public static void main (String[] args) {
	  junit.textui.TestRunner.run(suite());
  }

}
