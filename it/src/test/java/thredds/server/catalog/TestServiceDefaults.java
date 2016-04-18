/* Copyright */
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import thredds.client.catalog.*;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;

/**
 * Test services are properly added and overridden
 *
 * @author caron
 * @since 6/29/2015
 */
public class TestServiceDefaults {

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
    Assert.assertEquals(11, s.getNestedServices().size());
  }

  @Test
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
    Assert.assertEquals(11, s.getNestedServices().size());
  }

  @Test
  public void testUserDefinedServices() throws IOException {
    String catalog = "/catalog/catalogs5/testServices.xml"; // serviceName ="cdmremoteOnly" from local catalog
    Catalog cat = TdsLocalCatalog.open(catalog);
    Assert.assertEquals(3, cat.getServices().size());

    check(cat, "all", 11);
    check(cat, "GridServices", 11);
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
