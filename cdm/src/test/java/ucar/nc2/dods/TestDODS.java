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
package ucar.nc2.dods;

import junit.framework.*;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestDODS {
  //public static String server = "http://dods.coas.oregonstate.edu:8080/dods/dts/";
  public static String server = "http://test.opendap.org:8080/dods/dts/";

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();

    // just read em and see if they weep
    suite.addTest(new TestSuite(TestDODSRead.class)); //

    // scalars and arrays
    suite.addTest(new TestSuite(TestDODSScalars.class));  // test.01 //
    suite.addTest(new TestSuite(TestDODSArrayPrimitiveTypes.class)); // test.02
    suite.addTest(new TestSuite(TestDODSMultiArrayPrimitiveTypes.class)); // test.03

    // structures
    suite.addTest(new TestSuite(TestDODSStructureScalars.class)); // test.04
    suite.addTest(new TestSuite(TestDODSStructureScalarsNested.class)); // test.05
    //suite.addTest(new TestSuite(TestDODSSubset.class)); // test.05, 02 with CE : DOESNT WORK
    suite.addTest(new TestSuite(TestDODSStructureArray.class)); // test.21

    // arrays of structure
    //suite.addTest(new TestSuite(TestDODSArrayOfStructure.class)); // test.50
    //suite.addTest(new TestSuite(TestDODSArrayOfStructureNested.class)); // test.53

    // grids
    suite.addTest(new TestSuite(TestDODSGrid.class));           // test.06a
    suite.addTest(new TestSuite(TestDODSGrids.class));           // test.06, 23 //
    suite.addTest(new TestSuite(TestBennoGrid.class));           // Benno grid

    // sequences
    suite.addTest(new TestSuite(TestDODSSequence.class)); // test.07, test.23
    suite.addTest(new TestSuite(TestDODSnestedSequence.class)); // nestedSeq   */
    // suite.addTest(new TestSuite(TestDODSStructureForSequence.class)); // read sequence using DODSStructure
    // 
    return suite;
  }
}