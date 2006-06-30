package thredds.catalog;

import junit.framework.*;

/** Test catalog read JUnit framework. */

public class TestProperty extends TestCase {
  private static boolean showValidation = false;

  public TestProperty( String name) {
    super(name);
  }

  public void testProperty() {
    InvCatalogImpl cat = TestCatalogAll.open("InvCatalog.0.6.xml", true);

    InvDataset ds1 = cat.findDatasetByID("hasProp");
    assert (ds1 != null) : "cant find dataset 'hasProp'";
    String val = ds1.findProperty("GoodThing");
    assert val.equals("Where have you gone?");

    InvDataset ds = cat.findDatasetByID("hasNoProp");
    assert ds != null : "cant find dataset 'hasNoProp'";
    val = ds.findProperty("GoodThing");
    assert val.equals("Where have you gone?");

    InvDataset ds2 = cat.findDatasetByID("hasProp2");
    assert ds2 != null : "cant find dataset 'hasProp2'";
    val = ds2.findProperty("GoodThing");
    assert val.equals("overrides the earlier one");

  }


}
