/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import junit.framework.*;
import ucar.nc2.iosp.TestIndexer;
import ucar.nc2.iosp.TestRegularLayout;

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
      NetcdfFile ncfile = NetcdfFileCache.acquire(filename, null);
      if (dumpFile) System.out.println("open "+ncfile);
      return ncfile;

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
      return null;
    }
  }

  public static NetcdfFile openFile( String filename) {
    return open( TestLocal.cdmTestDataDir +filename);
  }

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();
    suite.addTest(new TestSuite(TestIndexer.class));
    suite.addTest(new TestSuite(TestRegularLayout.class));
    suite.addTest(new TestSuite(TestWrite.class));
    suite.addTest(new TestSuite(TestRead.class));
    suite.addTest(new TestSuite(TestOpenInMemory.class));
    suite.addTest(new TestSuite(TestAttributes.class)); //
    suite.addTest(new TestSuite(TestWriteRecord.class)); //
    suite.addTest(new TestSuite(TestWriteFill.class)); //
    suite.addTest(new TestSuite(TestReadRecord.class));
    suite.addTest(new TestSuite(TestDump.class)); //

    suite.addTest(new TestSuite(TestLongOffset.class)); //
    suite.addTest(new TestSuite(TestReadSection.class)); //
    suite.addTest(new TestSuite(TestStructure.class)); //
    suite.addTest(new TestSuite(TestStructureArray.class)); //

    suite.addTest(new TestSuite(TestReadStrides.class));// */

    return suite;
  }
}
