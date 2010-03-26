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
package ucar.nc2;

import junit.framework.*;
import ucar.nc2.iosp.TestIndexer;
import ucar.nc2.iosp.TestRegularLayout;
import ucar.nc2.dataset.TestScaleOffsetMissingForStructure;
import ucar.nc2.util.cache.TestNetcdfFileCache;

/**
 * ucar.nc2 core testing, using only local files (part of release)
 * created Jun 25, 2007
 *
 * @author caron
 */

public class TestLocalNC2 {
  public static boolean dumpFile = false;

  public static NetcdfFile open( String filename) {
    try {
      System.out.println("**** Open "+filename);
      NetcdfFile ncfile = NetcdfFile.open(filename, null);
      if (dumpFile) System.out.println("open "+ncfile);
      return ncfile;

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert false;
      return null;
    }
  }

  public static NetcdfFile openFile( String filename) {
    return open( TestLocal.cdmTestDataDir +filename);
  }

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();
    suite.addTest(new TestSuite(TestIndexer.class));
    suite.addTest(new TestSuite(TestRegularLayout.class)); // */
    suite.addTest(new TestSuite(TestNetcdfFileCache.class));
    
    suite.addTest(new TestSuite(TestWrite.class));
    suite.addTest(new TestSuite(TestRead.class)); //
    suite.addTest(new TestSuite(TestOpenInMemory.class));
    suite.addTest(new TestSuite(TestAttributes.class)); // 
    suite.addTest(new TestSuite(TestWriteRecord.class)); //
    suite.addTest(new TestSuite(TestWriteFill.class)); //
    suite.addTest(new TestSuite(TestWriteMiscProblems.class)); //
    suite.addTest(new TestSuite(TestReadRecord.class));
    suite.addTest(new TestSuite(TestDump.class)); // */
    suite.addTest(new TestSuite(TestRedefine.class)); // */

    suite.addTest(new TestSuite(TestLongOffset.class)); //
    suite.addTest(new TestSuite(TestReadSection.class)); //
    suite.addTest(new TestSuite(TestStructure.class)); //
    suite.addTest(new TestSuite(TestStructureArray.class)); //

    suite.addTest( new TestSuite(TestReadStrides.class));
    suite.addTest( new TestSuite(TestScaleOffsetMissingForStructure.class));
    suite.addTest( new TestSuite(TestSlice.class));

    suite.addTest( new TestSuite(TestUnsigned.class));

    return suite;
  }
}
