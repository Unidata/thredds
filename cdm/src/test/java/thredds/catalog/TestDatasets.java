package thredds.catalog;

import junit.framework.*;

public class TestDatasets extends TestCase {

  public TestDatasets( String name) {
    super(name);
  }

  public void testDataType() {
    InvCatalogImpl cat = TestCatalogAll.open("InvCatalog.0.6.xml", true);

    InvDataset ds = cat.findDatasetByID("testSubset");
    assert (ds != null) : "cant find dataset 'testSubset'";
    assert ds.getDataType() == DataType.GRID;

    InvDataset co2 = ds.findDatasetByName("CO2");
    assert (co2 != null) : "cant find dataset 'CO2'";
    assert co2.getDataType() == DataType.IMAGE;

    InvDataset no2 = ds.findDatasetByName("NO2");
    assert (no2 != null) : "cant find dataset 'NO2'";
    assert no2.getDataType() == DataType.GRID;
  }

  public void testAccessPath() {
    InvCatalogImpl cat = TestCatalogAll.open("InvCatalog.0.6.xml", true);

    InvDataset ds = cat.findDatasetByID("CO2_Flux");
    assert (ds != null) : "cant find dataset 'CO2_Flux'";

    InvAccess a = ds.getAccess(ServiceType.DODS);
    assert a != null;
    assert a.getStandardUrlName().equals("http://motherlode.ucar.edu/cgi-bin/dods/1998/CO2.nc") : a.getStandardUrlName();

    a = ds.getAccess(ServiceType.NETCDF);
    assert a != null;
    assert a.getUnresolvedUrlName().equals("1998/CO2.nc") : a.getUnresolvedUrlName();
    assert a.getStandardUrlName().equals(TestCatalogAll.makeFilepath() +"1998/CO2.nc") : a.getStandardUrlName();

  }

  public void testExpandedAccess() {
    InvCatalogImpl cat = TestCatalogAll.open("InvCatalog.0.6.xml", true);

    InvDataset ds = cat.findDatasetByID("HasCompoundService");
    assert (ds != null) : "cant find dataset 'HasCompoundService'";

    java.util.List access = ds.getAccess();
    assert access.size() == 2 : access.size();

    InvAccess a = ds.getAccess(ServiceType.DODS);
    assert a != null;
    a = ds.getAccess(ServiceType.FTP);
    assert a != null;

  }

  public void testFullName() {
    InvCatalogImpl cat = TestCatalogAll.openAbsolute("http://lead.unidata.ucar.edu:8080/thredds/idd/obsData.xml", true);

    InvDataset ds = cat.findDatasetByID("NWS/RASS/1hour");
    assert (ds != null) : "cant find dataset";

    System.out.println(" fullName= "+ds.getFullName());
    System.out.println(" name= "+ds.getName());
  }

}