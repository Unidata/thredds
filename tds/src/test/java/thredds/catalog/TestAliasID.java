package thredds.catalog;

import junit.framework.*;

/** Test catalog read JUnit framework. */

public class TestAliasID extends TestCase {

  public TestAliasID( String name) {
    super(name);
  }

  public void testValid() {
    InvCatalogImpl cat = TestCatalogAll.open("TestAlias.xml", true);

    InvDataset real = cat.findDatasetByID("FluxData");

    InvDatasetImpl top = (InvDatasetImpl) cat.getDataset();
    InvDatasetImpl ds1 = top.findDatasetByName("Model data");
    InvDatasetImpl ds2 = ds1.findDatasetByName("Flux measurements");

    assert real == ds2;
  }

}
