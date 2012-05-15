package thredds.catalog;

import org.junit.Test;

/**
 * https://www.unidata.ucar.edu/jira/browse/TDS-193
 *
 * @author caron
 * @since 5/14/12
 */
public class TestInvService {

  private static String urlString = "nestedServices.xml";

  @Test
  public void testNested() {
    InvCatalogImpl cat = TestCatalogAll.open(urlString, true);

    StringBuilder buff = new StringBuilder();
    boolean isValid = cat.check(buff, false);
    System.out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");

    InvDataset ds = cat.findDatasetByID("top");
    assert ds != null;
    assert ds.getServiceDefault() != null : ds.getID();

    ds = cat.findDatasetByID("nest1");
    assert ds != null;
    assert ds.getServiceDefault() != null  : ds.getID();

    ds = cat.findDatasetByID("nest2");
    assert ds != null;
    assert ds.getServiceDefault() != null  : ds.getID();


    System.out.printf("OK%n");
 }
}
