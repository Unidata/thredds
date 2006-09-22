package thredds.tds;

import junit.framework.*;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogFactory;
import ucar.nc2.dods.DODSNetcdfFile;

/**
 * TestSuite that runs all the sample tests for testing the TDS.
 *
 * This assumes that you are running the TDS on localhost, with the standard test catalog (catalog_unitTest.xml).
 *
 * Test data is typically put into /upc/share/testdata/tds
 *
 */
public class TestTDSAll extends TestCase {
  public static String topCatalog = "http://localhost:8080/thredds";
  public static boolean showValidationMessages = false;

  public static InvCatalogImpl open(String catalogName) {
    if (catalogName == null) catalogName = "/catalog.xml";
    String catalogPath = topCatalog + catalogName;
    System.out.println("\n open= "+catalogPath);
    StringBuffer buff = new StringBuffer();
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
    suite.addTest(new TestSuite(TestDodsServer.class));
    suite.addTest(new TestSuite(TestNcml.class));
    suite.addTest(new TestSuite(TestNetcdfSubsetService.class));
    suite.addTest(new TestSuite(TestDatasetScan.class));

    return suite;
  }
}