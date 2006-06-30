package thredds.tds;

import junit.framework.*;

import thredds.catalog.*;

import java.io.IOException;
import java.util.List;

public class TestDatasetScan extends TestCase {

  public TestDatasetScan( String name) {
    super(name);
  }


  public void testSort() throws IOException {
    InvCatalog cat = TestTDSAll.open("/catalog/gribCollection/catalog.xml");
    InvDataset top = cat.getDataset();
    List dss = top.getDatasets();
    assert (dss.size() > 2);

    InvDataset ds1 = (InvDataset) dss.get(1);
    InvDataset ds2 = (InvDataset) dss.get(2);
    assert ds1.getName().compareTo( ds2.getName()) < 0 ;
  }

  public void testLatest() throws IOException {
     InvCatalogImpl cat = TestTDSAll.open("/catalog/gribCollection/latest.xml");
     List dss = cat.getDatasets();
     assert (dss.size() == 1);

     InvDatasetImpl ds = (InvDatasetImpl) dss.get(0);
     assert ds.hasAccess();
     assert ds.getDatasets().size() == 0;

     assert ds.getID() != null;
     assert ds.getDataSize() > 0.0;
   }

  public void testHarvest() throws IOException {
    InvCatalogImpl cat = TestTDSAll.open("/catalog/ncmodels/catalog.xml");  // http://localhost:8080/thredds/catalog/ncmodels/catalog.html?dataset=ncmodels
    InvDataset dscan = cat.findDatasetByID("ncmodels");
    assert dscan != null;
    assert dscan.isHarvest();

    List dss = dscan.getDatasets();
    assert (dss.size() > 0);
    InvDataset nested = (InvDataset) dss.get(0);
    assert !nested.isHarvest();

    cat = TestTDSAll.open("/catalog/ncmodels/canonical/catalog.xml");
    InvDataset ds = cat.findDatasetByID("ncmodels/canonical");
    assert ds != null;
    assert !ds.isHarvest();
  }

}
