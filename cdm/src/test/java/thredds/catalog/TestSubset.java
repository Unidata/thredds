package thredds.catalog;

import junit.framework.*;
import ucar.nc2.constants.DataType;

public class TestSubset extends TestCase {

  public TestSubset( String name) {
    super(name);
  }

  public void testDataType() {
    InvCatalogImpl cat = TestCatalogAll.open("InvCatalog.0.6.xml", true);

    InvDataset ds = cat.findDatasetByID("testSubset");
    assert (ds != null) : "cant find dataset 'testSubset'";
    assert ds.getDataType() == DataType.GRID;

    cat.subset(ds);

    try {
      cat.writeXML(System.out);
    } catch (java.io.IOException ioe) {
      assert false;
    }

  }

}