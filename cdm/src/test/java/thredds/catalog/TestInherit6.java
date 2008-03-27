package thredds.catalog;

import junit.framework.*;
import java.util.*;

import ucar.nc2.constants.FeatureType;

/** Test catalog read JUnit framework. */

public class TestInherit6 extends TestCase {
  private static boolean showValidation = false;

  public TestInherit6( String name) {
    super(name);
  }

  public void testPropertyInherit() {
    InvCatalogImpl cat = TestCatalogAll.open("TestInherit.0.6.xml", true);
    String val;

    InvDataset top = cat.findDatasetByID("top");
    val = top.findProperty("GoodThing");
    assert val == null : val;

    InvDataset nest1 = cat.findDatasetByID("nest1");
    val = nest1.findProperty("GoodThing");
    assert val.equals("Where have you gone?")  : val;

    InvDataset nest2 = cat.findDatasetByID("nest2");
    val = nest2.findProperty("GoodThing");
    assert val == null  : val;

    InvDataset nest11 = cat.findDatasetByID("nest11");
    val = nest11.findProperty("GoodThing");
    assert val.equals("Where have you gone?")  : val;

    InvDataset nest12 = cat.findDatasetByID("nest12");
    val = nest12.findProperty("GoodThing");
    assert val.equals("override")  : val;
  }


  public void testServiceInherit() {
    InvCatalogImpl cat = TestCatalogAll.open("TestInherit.0.6.xml", true);
    InvDataset ds = null;
    InvService s = null;
    String val = null;

    ds = cat.findDatasetByID("top");
    s = ds.getServiceDefault();
    assert (s == null) : s;

    ds = cat.findDatasetByID("nest1");
    s = ds.getServiceDefault();
    assert s != null : s;
    val = s.getName();
    assert val.equals("ACD") : val;

    ds = cat.findDatasetByID("nest11");
    s = ds.getServiceDefault();
    assert s != null : s;
    val = s.getName();
    assert val.equals("ACD") : val;

    ds = cat.findDatasetByID("nest12");
    s = ds.getServiceDefault();
    assert s != null : s;
    val = s.getName();
    assert val.equals("local") : val;

    ds = cat.findDatasetByID("nest121");
    s = ds.getServiceDefault();
    assert s != null : s;
    val = s.getName();
    assert val.equals("local") : val;

    ds = cat.findDatasetByID("nest2");
    s = ds.getServiceDefault();
    assert (s == null) : s;
  }

  public void testdataTypeInherit() {
    InvCatalogImpl cat = TestCatalogAll.open("TestInherit.0.6.xml", true);
    InvDataset ds = null;
    FeatureType s = null;
    String val = null;

    ds = cat.findDatasetByID("top");
    s = ds.getDataType();
    assert (s == null) : s;

    ds = cat.findDatasetByID("nest1");
    s = ds.getDataType();
    assert (s == FeatureType.GRID) : s;

    ds = cat.findDatasetByID("nest11");
    s = ds.getDataType();
    assert (s == FeatureType.GRID) : s;

    ds = cat.findDatasetByID("nest12");
    s = ds.getDataType();
    assert (s == FeatureType.IMAGE) : s;

    ds = cat.findDatasetByID("nest121");
    s = ds.getDataType();
    assert (s == FeatureType.IMAGE) : s;

    ds = cat.findDatasetByID("nest2");
    s = ds.getDataType();
    assert (s == null) : s;
  }

  public void testAuthorityInherit() {
    InvCatalogImpl cat = TestCatalogAll.open("TestInherit.0.6.xml", true);
    InvDataset ds = null;
    FeatureType s = null;
    String val = null;

    ds = cat.findDatasetByID("top");
    val = ds.getAuthority();
    assert (val == null) : val;

    ds = cat.findDatasetByID("nest1");
    val = ds.getAuthority();
    assert val.equals("divine") : val;

    ds = cat.findDatasetByID("nest11");
    val = ds.getAuthority();
    assert val.equals("divine") : val;

    ds = cat.findDatasetByID("nest12");
    val = ds.getAuthority();
    assert val.equals("human") : val;

    ds = cat.findDatasetByID("nest121");
    val = ds.getAuthority();
    assert val.equals("human") : val;

    ds = cat.findDatasetByID("nest2");
    val = ds.getAuthority();
    assert (val == null) : val;
  }

  public void testMetadataInherit() {
    InvCatalogImpl cat = TestCatalogAll.open("TestInherit.0.6.xml", true);
    InvDataset ds = null;
    List list = null;
    InvMetadata m = null;

    ds = cat.findDatasetByID("top");
    list = ds.getMetadata( MetadataType.NETCDF);
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest1");
    list = ds.getMetadata();
    m = (InvMetadata) list.get(0);
    assert (m != null) : m;
    assert m.getMetadataType().equals("NetCDF");
    list = ds.getMetadata( MetadataType.NETCDF);
    m = (InvMetadata) list.get(0);
    assert (m != null) : m;

    ds = cat.findDatasetByID("nest11");
    list = ds.getMetadata( MetadataType.NETCDF);
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest12");
    list = ds.getMetadata( MetadataType.NETCDF);
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest121");
    list = ds.getMetadata( MetadataType.NETCDF);
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest2");
    list = ds.getMetadata( MetadataType.NETCDF);
    assert list.isEmpty();
  }

  public void testDocInherit() {
    InvCatalogImpl cat = TestCatalogAll.open("TestInherit.0.6.xml", true);

    InvDataset ds = null;
    List list = null;
    InvDocumentation d = null;

    ds = cat.findDatasetByID("top");
    list = ds.getDocumentation();
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest1");
    list = ds.getDocumentation();
    d = (InvDocumentation) list.get(0);
    assert (d != null) : d;
    assert d.getInlineContent().equals("HEY");

    ds = cat.findDatasetByID("nest11");
    list = ds.getDocumentation();
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest2");
    list = ds.getDocumentation();
    assert list.isEmpty();
  }


}
