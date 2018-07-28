/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.*;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test services are properly added and overridden
 *
 * @author caron
 * @since 6/29/2015
 */
public class TestServiceDefaults {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testStandardServices() throws IOException {
    String catalog = "/catalog/catalogs5/testServices.xml"; // serviceName ="cdmremoteOnly" from local catalog
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset ds = cat.findDatasetByID("testSingleGridDataset");
    Assert.assertNotNull(ds);
    FeatureType ft = ds.getFeatureType();
    Assert.assertNotNull(ft);
    Assert.assertEquals(FeatureType.GRID, ft);

    Service s = ds.getServiceDefault();
    Assert.assertNotNull(s);
    Assert.assertTrue(s.getType() == ServiceType.Compound);
    Assert.assertEquals(12, s.getNestedServices().size());
  }

  // Relies on:
  // <datasetScan name="Test Scan Grid Dataset" location="${cdmUnitTest}/ncss/CONUS_80km_nc/"
  //     path="datasetScan/ncss/CONUS_80km_nc/" dataType="Grid"/>
  // In tds/src/test/content/thredds/catalogs5/testServices.xml
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testStandardServicesDatasetScan() throws IOException {
    String catalog = "/catalog/datasetScan/ncss/CONUS_80km_nc/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset ds = cat.findDatasetByID("datasetScan/ncss/CONUS_80km_nc/GFS_CONUS_80km_20120418_1200.nc");
    Assert.assertNotNull(ds);
    FeatureType ft = ds.getFeatureType();
    Assert.assertNotNull(ft);
    Assert.assertEquals(FeatureType.GRID, ft);

    Service s = ds.getServiceDefault();
    Assert.assertNotNull(s);

    Assert.assertTrue(s.getType() == ServiceType.Compound);
    Assert.assertEquals(12, s.getNestedServices().size());
  }

  @Test
  public void testUserDefinedServices() throws IOException {
    String catalog = "/catalog/catalogs5/testServices.xml"; // serviceName ="cdmremoteOnly" from local catalog
    Catalog cat = TdsLocalCatalog.open(catalog);
    Assert.assertEquals(3, cat.getServices().size());

    check(cat, "all", 12);
    check(cat, "GridServices", 12);
    check(cat, "opendapOnly", 1);

    Service localServices = cat.findService("opendapOnly");
    Assert.assertNotNull(localServices);
    Assert.assertEquals(ServiceType.OPENDAP, localServices.getType());

    for (Dataset ds : cat.getDatasetsLocal()) {
      if (!(ds instanceof CatalogRef)) {
        Assert.assertTrue(ds.hasAccess());

      } else {
        CatalogRef catref = (CatalogRef) ds;
        Catalog cat2 = TdsLocalCatalog.openFromURI(catref.getURI());
        localServices = cat2.findService(ServiceType.CdmRemote);
        Assert.assertNotNull(localServices);
        Assert.assertEquals(ServiceType.CdmRemote, localServices.getType());
        Assert.assertNull(cat2.findService(ServiceType.Resolver));
        break;
      }
    }
  }

  private void check(Catalog cat, String serviceName, int count) {
    Service allServices = cat.findService(serviceName);
    Assert.assertNotNull(allServices);
    if (count > 1) {
      Assert.assertEquals(ServiceType.Compound, allServices.getType());
      Assert.assertNotNull(allServices.getNestedServices());
      Assert.assertEquals(count, allServices.getNestedServices().size());
    }
  }




}
