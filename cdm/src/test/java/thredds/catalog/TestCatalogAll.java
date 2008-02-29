package thredds.catalog;

import java.util.*;
import junit.framework.*;
import ucar.nc2.TestAll;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestCatalogAll extends TestCase {
  public static String tmpDir = TestAll.temporaryDataDir;
  public static String dataDir = TestAll.cdmTestDataDir + "thredds/catalog/";
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
    StringBuffer buff = new StringBuffer();
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
    StringBuffer buff = new StringBuffer();
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

    suite.addTestSuite( thredds.catalog.parser.jdom.TestDatasetScanFilter.class );
    suite.addTestSuite( thredds.catalog.parser.jdom.TestReadMetadata.class );

    return suite;
  }
}