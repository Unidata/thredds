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
package thredds.tds;

import junit.framework.*;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogFactory;
import ucar.nc2.dods.DODSNetcdfFile;

/**
 * TestSuite that runs all the sample tests for testing the TDS on localhost.
 *
 * The local server should run the catalog at thredds\tds\src\test\data\thredds\tds\catalog.xml. Please keep this
 * updated and checked into svn.
 *
 * Data should be kept in /upc/share/cdmUnitTest/tds
 *
 * jcaron, resurrected Sep 2010
 */
public class TestTdsLocal extends TestCase {
  public static String topCatalog = "http://localhost:8080/thredds";
  public static boolean showValidationMessages = false;

  public static InvCatalogImpl open(String catalogName) {
    if (catalogName == null) catalogName = "/catalog.xml";
    String catalogPath = topCatalog + catalogName;
    System.out.println("\n open= "+catalogPath);
    StringBuilder buff = new StringBuilder();
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false);

    try {
      InvCatalogImpl cat = catFactory.readXML(catalogPath);
      boolean isValid = cat.check( buff, false);
      if (!isValid) {
        System.out.println("Validate failed "+ catalogName+" = \n<"+ buff.toString()+">");
        assertTrue( false);
      } else if (showValidationMessages)
        System.out.println("Validate ok "+ catalogName+" = \n<"+ buff.toString()+">");
      return cat;

    } catch (Exception e) {
      e.printStackTrace();
      assertTrue( false);
    }

    return null;
  }

  public static junit.framework.Test suite ( ) {
    DODSNetcdfFile.debugServerCall = true;

    TestSuite suite= new TestSuite();
    suite.addTest(new TestSuite(TestTdsDodsServer.class));
    suite.addTest(new TestSuite(TestTdsNcml.class));
    suite.addTest(new TestSuite(TestTdsDatasetScan.class));
    //suite.addTest(new TestSuite(TestTdsNetcdfSubsetService.class));
    //suite.addTest(new TestSuite(TestTdsWxs.class));

    return suite;
  }
}