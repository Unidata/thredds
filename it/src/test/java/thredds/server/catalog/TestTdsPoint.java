/* Copyright */
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.client.catalog.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.List;

/**
 * Catalog processing for point Features type collections
 *
 * @author caron
 * @since 7/1/2015
 */
// These tests all rely on collections defined in tds/src/test/content/thredds/catalogs5/pointCatalog5.xml
// In turn, those collections all have "${cdmUnitTest}" in their spec.
@Category(NeedsCdmUnitTest.class)
public class TestTdsPoint {

   ///////////////////////////////////////////////////
  // work with catalogs with service elements removed

  @Test
  public void testDefaultPointCollectionServices() throws IOException {
    String catalog = "/catalog/testStationFeatureCollection.v5/catalog.xml";  // no service name, should use PointCollection default
    Catalog cat = TdsLocalCatalog.open(catalog);
    List<Service> services = cat.getServices();
    Assert.assertEquals(1, services.size());

    Service service = cat.getServices().get(0);
    Assert.assertEquals(ServiceType.Compound, service.getType());
    Assert.assertEquals("PointCollectionServices", service.getName());

    List<Service> nested = service.getNestedServices();
    Assert.assertEquals(1, nested.size());
    Service nestedOne = nested.get(0);
    Assert.assertEquals(ServiceType.NetcdfSubset, nestedOne.getType());
  }

  @Test
  public void testDefaultPointFileServices() throws IOException {
    String catalog = "/catalog/testStationFeatureCollection.v5/files/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);
    List<Service> services = cat.getServices();
    Assert.assertEquals(2, services.size());

    Service service = cat.findService(ServiceType.CdmRemote);
    Assert.assertNotNull(ServiceType.CdmRemote.toString(), service);

    service = cat.findService(ServiceType.Resolver);
    Assert.assertNotNull(ServiceType.Resolver.toString(), service);
  }

  @Test
  public void testGlobalServices() throws IOException {
    String catalog = "/catalog/testSurfaceSynopticFeatureCollection.v5/files/catalog.xml"; // serviceName ="opendapOnly" from root catalog
    Catalog cat = TdsLocalCatalog.open(catalog);
    testCat(cat, 0, true, "opendapOnly", 1);

    Dataset top = cat.getDatasetsLocal().get(0);
    Assert.assertTrue(!top.hasAccess());
    for (Dataset ds : top.getDatasetsLocal()) {
      if (!(ds instanceof CatalogRef)) {
        Assert.assertTrue(ds.hasAccess());

      } else {
        CatalogRef catref = (CatalogRef) ds;
        Catalog cat2 = TdsLocalCatalog.openFromURI(catref.getURI());
        testCat(cat2, 0, true, "opendapOnly", 1);
        break;
      }
    }

  }

  @Test
  public void testUserDefinedServices() throws IOException {
    String catalog = "/catalog/testBuoyFeatureCollection.v5/files/catalog.xml"; // serviceName ="cdmremoteOnly" from local catalog
    Catalog cat = TdsLocalCatalog.open(catalog);
    Service localServices = cat.findService("cdmremoteOnly");
    Assert.assertNotNull(localServices);
    Assert.assertEquals(ServiceType.CdmRemote, localServices.getType());

    Dataset top = cat.getDatasetsLocal().get(0);
    Assert.assertTrue(!top.hasAccess());
    for (Dataset ds : top.getDatasetsLocal()) {
      if (!(ds instanceof CatalogRef)) {
        Assert.assertTrue(ds.hasAccess());

      } else {
        CatalogRef catref = (CatalogRef) ds;
        Catalog cat2 = TdsLocalCatalog.openFromURI(catref.getURI());
        localServices = cat2.findService("cdmremoteOnly");
        Assert.assertNotNull(localServices);
        Assert.assertEquals(ServiceType.CdmRemote, localServices.getType());
        Assert.assertNotNull(cat2.findService("Resolver"));
        break;
      }
    }
   }

  private void testCat(Catalog cat, int virtCount, boolean hasResolver, String orgName, int orgCount) throws IOException {
    if (virtCount > 0) {
      Service virtualServices = cat.findService("VirtualServices");
      Assert.assertNotNull(virtualServices);
      Assert.assertEquals(ServiceType.Compound, virtualServices.getType());
      Assert.assertNotNull(virtualServices.getNestedServices());
      Assert.assertEquals(virtCount, virtualServices.getNestedServices().size());
      for (Service sn : virtualServices.getNestedServices())
        Assert.assertNotEquals(ServiceType.HTTPServer, sn.getType());
    }

    if (hasResolver) {
      Service resolverService = cat.findService("Resolver");
      Assert.assertNotNull(resolverService);
      Assert.assertEquals(ServiceType.Resolver, resolverService.getType());
    } else {
      Assert.assertNull(cat.findService("Resolver"));
    }

    if (orgName != null) {
      Service orgServices = cat.findService(orgName);
      Assert.assertNotNull(orgServices);
      if (orgCount > 1) {
        Assert.assertEquals(ServiceType.Compound, orgServices.getType());
        Assert.assertNotNull(orgServices.getNestedServices());
        Assert.assertEquals(orgCount, orgServices.getNestedServices().size());
        boolean hasFileServer = false;
        for (Service sn : orgServices.getNestedServices())
          if (ServiceType.HTTPServer == sn.getType()) hasFileServer = true;
        Assert.assertTrue(hasFileServer);
      }
    }
  }

}
