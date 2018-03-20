/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.builder.CatalogBuilder;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 1/15/2015
 */
public class TestClientCatalogInherit {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static final String urlString = "file:"+TestDir.cdmLocalTestDataDir + "thredds/catalog/TestInherit.1.0.xml";
  Catalog cat;
  
  @Before
  public void openCatalog() throws IOException {
    CatalogBuilder builder = new CatalogBuilder();
    cat = builder.buildFromLocation(urlString, null);
    if (builder.hasFatalError()) {
      System.out.printf("ERRORS %s%n", builder.getErrorMessage());
      assert false;
    }
  }

  @Test
  public void testPropertyInherit() {

    Dataset top = cat.findDatasetByID("top");
    String val = top.findProperty("GoodThing");
    assert val == null : val;

    Dataset nest1 = cat.findDatasetByID("nest1");
    val = nest1.findProperty("GoodThing");
    assert val.equals("Where have you gone?")  : val;

    Dataset nest2 = cat.findDatasetByID("nest2");
    val = nest2.findProperty("GoodThing");
    assert val == null  : val;

    Dataset nest11 = cat.findDatasetByID("nest11");
    val = nest11.findProperty("GoodThing");
    assert val.equals("Where have you gone?")  : val;

    Dataset nest12 = cat.findDatasetByID("nest12");
    Assert.assertTrue(nest12.hasProperty(new Property("GoodThing", "override")));
  }

  @Test
  public void testServiceInherit() {
    Dataset ds;
    Service s;
    String val;

    ds = cat.findDatasetByID("top");
    s = ds.getServiceDefault();
    assert (s == null) : s;

    ds = cat.findDatasetByID("nest1");
    s = ds.getServiceDefault();
    assert s != null : "nest1";
    val = s.getName();
    assert val.equals("ACD") : val;

    ds = cat.findDatasetByID("nest11");
    s = ds.getServiceDefault();
    assert s != null : "nest11";
    val = s.getName();
    assert val.equals("ACD") : val;

    ds = cat.findDatasetByID("nest12");
    s = ds.getServiceDefault();
    assert s != null : "nest12";
    val = s.getName();
    assert val.equals("local") : val;

    ds = cat.findDatasetByID("nest121");
    s = ds.getServiceDefault();
    assert s != null : "nest121";
    val = s.getName();
    assert val.equals("ACD") : val;

    ds = cat.findDatasetByID("nest2");
    s = ds.getServiceDefault();
    assert (s == null) : s;
  }

  @Test
  public void testdataTypeInherit() {
    Dataset ds;
    FeatureType s;

    ds = cat.findDatasetByID("top");
    s = ds.getFeatureType();
    assert (s == null) : s;

    ds = cat.findDatasetByID("nest1");
    s = ds.getFeatureType();
    assert (s == FeatureType.GRID) : s;

    ds = cat.findDatasetByID("nest11");
    s = ds.getFeatureType();
    assert (s == FeatureType.GRID) : s;

    ds = cat.findDatasetByID("nest12");
    s = ds.getFeatureType();
    assert (s.toString().equalsIgnoreCase("Image")) : s;

    ds = cat.findDatasetByID("nest121");
    s = ds.getFeatureType();
    assert (s == FeatureType.GRID) : s;

    ds = cat.findDatasetByID("nest2");
    s = ds.getFeatureType();
    assert (s == null) : s;
  }

  @Test
  public void testAuthorityInherit() {
    Dataset ds;
    String val;

    ds = cat.findDatasetByID("top");
    val = ds.getAuthority();
    assert val.equals("ucar") : val;

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
    assert val.equals("divine") : val;

    ds = cat.findDatasetByID("nest2");
    val = ds.getAuthority();
    assert (val == null) : val;
  }

  @Test
  public void testMetadataInherit() {
    Dataset ds;
    List list;

    ds = cat.findDatasetByID("top");
    list = ds.getMetadata( "NetCDF");
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest1");
    list = ds.getMetadata( "NetCDF");
    assert (list.size() == 1) : list.size();
    ThreddsMetadata.MetadataOther m = (ThreddsMetadata.MetadataOther) list.get(0);
    assert (m != null) : "nest1";

    ds = cat.findDatasetByID("nest11");
    list = ds.getMetadata( "NetCDF");
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest12");
    list = ds.getMetadata( "NetCDF");
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest121");
    list = ds.getMetadata( "NetCDF");
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest2");
    list = ds.getMetadata( "NetCDF");
    assert list.isEmpty();
  }

  @Test
  public void testDocInherit() {
    Dataset ds;
    List list;
    Documentation d;

    ds = cat.findDatasetByID("top");
    assert ds != null;
    list = ds.getDocumentation();
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest1");
    assert ds != null;
    list = ds.getDocumentation();
    d = (Documentation) list.get(0);
    assert (d != null) : "nest1";
    assert d.getInlineContent().equals("HEY");

    ds = cat.findDatasetByID("nest11");
    assert ds != null;
    list = ds.getDocumentation();
    assert list.isEmpty();

    ds = cat.findDatasetByID("nest2");
    assert ds != null;
    list = ds.getDocumentation();
    assert list.isEmpty();
  }
}
