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
package ucar.nc2.ncml;

import junit.framework.*;
import ucar.nc2.TestAll;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestNcML {
  public static String topDir = TestAll.cdmLocalTestDataDir + "ncml/";

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();

    //test reading XML
    suite.addTest(new TestSuite(TestNcMLequals.class)); // ncml == referenced dataset
    suite.addTest(new TestSuite(TestNcMLRead.class)); // explicit;  metadata in xml
    suite.addTest(new TestSuite(TestNcMLRead.TestRead2.class)); // readMetadata //

    suite.addTest(new TestSuite(TestNcMLReadOverride.class)); // read and override
    suite.addTest(new TestSuite(TestNcMLModifyAtts.class)); // modify atts
    suite.addTest(new TestSuite(TestNcMLModifyVars.class)); // modify vars
    suite.addTest(new TestSuite(TestNcMLRenameVar.class)); // all metadata in xml, rename vars  */

    // test aggregations
    suite.addTest(new TestSuite(TestAggUnionSimple.class));
    suite.addTest(new TestSuite(TestAggUnion.class));

    suite.addTest(new TestSuite(TestAggExistingCoordVars.class));
    suite.addTest(new TestSuite(TestAggExisting.class));

    suite.addTest(new TestSuite(TestAggSynthetic.class)); //

    suite.addTest(new TestSuite(TestAggExistingPromote.class));
    suite.addTest(new TestSuite(TestAggMisc.class));

    // requires remote (network) access
    suite.addTest(new TestSuite(TestRemoteAggregation.class));

    // LOOK wait until grids are done - fails when cached
    //suite.addTest(new TestSuite(TestAggSynGrid.class));

    return suite;
  }

}