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
package thredds.catalog;

import java.util.*;
import junit.framework.*;
import ucar.nc2.TestAll;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestCatalogAll extends TestCase {
  public static String tmpDir = TestAll.temporaryLocalDataDir;
  public static String dataDir = TestAll.cdmLocalTestDataDir + "thredds/catalog/";
  static private boolean showValidation = false;
  static boolean debug = true, showValidationMessages = true;

  public static String makeFilepath(String catalogName) {
    return makeFilepath() + catalogName;
  }

  public static String makeFilepath() {
    return "file:"+dataDir;
  }

  public static InvCatalogImpl open(String catalogName, boolean validate) {
    catalogName = makeFilepath( catalogName);
    System.out.println("\nTestAll open= "+catalogName);
    StringBuilder buff = new StringBuilder();
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(validate);

    try {
      InvCatalogImpl cat = (InvCatalogImpl) catFactory.readXML(catalogName);
      boolean isValid = ((InvCatalogImpl)cat).check( buff, showValidation);
      if (!isValid)
        System.out.println("Validate failed "+ catalogName+" = \n<"+ buff.toString()+">");
      else if (showValidationMessages)
        System.out.println("Validate ok "+ catalogName+" = \n<"+ buff.toString()+">");
      return cat;

    } catch (Exception e) {
      e.printStackTrace();
      assertTrue( false);
    }

    return null;
  }

  public static InvCatalogImpl openAbsolute(String catalogName, boolean validate) {
    System.out.println("\nTestAll openAbsolute= "+catalogName);
    StringBuilder buff = new StringBuilder();
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(validate);

    try {
      InvCatalogImpl cat = (InvCatalogImpl) catFactory.readXML(catalogName);
      boolean isValid = ((InvCatalogImpl)cat).check( buff, showValidation);
      if (!isValid)
        System.out.println("Validate failed "+ catalogName+" = \n<"+ buff.toString()+">");
      else if (showValidationMessages)
        System.out.println("Validate ok "+ catalogName+" = \n<"+ buff.toString()+">");
      return cat;

    } catch (Exception e) {
      e.printStackTrace();
      assertTrue( false);
    }

    return null;
  }


  public static junit.framework.Test suite ( ) {

    if (false) {
        try {
          Properties p = System.getProperties();
          Enumeration en = p.keys();
          while (en.hasMoreElements()) {
            Object key = en.nextElement();
            System.out.println("  "+key + " = " + p.get(key));
          }
        } catch (SecurityException e) {
          System.out.println("not allowed to get Properties");
        }
    }

    String dd = System.getProperty("test.data.dir");
    if (dd != null) // run test in JBuilder
      dataDir = dd;
    System.out.println("TestAllDqc data directory= "+ dataDir);

    TestSuite suite= new TestSuite();
    suite.addTest(new TestSuite(TestURL.class));
    // suite.addTest(new TestSuite(TestOpen.class)); // 0.6 catalogs
    suite.addTest(new TestSuite(TestRead.class));
    suite.addTest(new TestSuite(TestAliasID.class));
    // suite.addTest(new TestSuite(TestProperty.class)); // 0.6 catalogs
    //suite.addTest(new TestSuite(TestDatasets.class)); // 0.6 catalogs
    // suite.addTest(new TestSuite(TestInherit6.class)); // 0.6 catalogs
    suite.addTest(new TestSuite(TestInherit1.class)); //
    suite.addTest(new TestSuite(TestResolve1.class)); //
    suite.addTest(new TestSuite(TestMetadata.class)); //
    suite.addTest(new TestSuite(TestSubset.class)); //

    suite.addTest(new TestSuite(TestCatalogReference.class)); //
    suite.addTest(new TestSuite(TestVariables.class)); // */

    //suite.addTest(new TestSuite(TestWrite.class)); // */
    //suite.addTest(new TestSuite(TestConvert.class)); // */
    suite.addTest(new TestSuite(TestSpatialCoverage.class)); // */
    suite.addTest(new TestSuite(TestTimeCoverage.class)); // */

    suite.addTest( new JUnit4TestAdapter( DatasetScanExpandSubdirsTest.class ));
    
    suite.addTestSuite( thredds.catalog.parser.jdom.TestDatasetScanFilter.class );
    suite.addTestSuite( thredds.catalog.parser.jdom.TestReadMetadata.class );

    return suite;
  }
}