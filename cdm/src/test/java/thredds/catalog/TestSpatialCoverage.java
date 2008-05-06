package thredds.catalog;

import junit.framework.TestCase;

/**
 * @author john
 */
public class TestSpatialCoverage  extends TestCase {
  private static boolean showValidation = false;

  public TestSpatialCoverage( String name) {
    super(name);
  }

  String urlString = "MissingGCProblem.xml";

  public void testGC() throws Exception {
    InvCatalogImpl cat = TestCatalogAll.open(urlString, true);

    StringBuilder buff = new StringBuilder();
    boolean isValid = cat.check(buff, false);
    System.out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");
    if (showValidation) {
      System.out.println(" validation output=\n" + buff);
      InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false);
      catFactory.writeXML( cat, System.out);
    }

    InvDataset ds = cat.findDatasetByID("hasGC");
    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    assert null != gc;
    assert gc.getHeightStart() == 5.0 : gc.getHeightStart();
    assert gc.getHeightExtent() == 47.0 : gc.getHeightExtent();

    assert gc.getEastWestRange() == null;
    assert gc.getNorthSouthRange() == null;

  }
}
