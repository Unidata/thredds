/* Copyright */
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import thredds.client.catalog.*;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 6/29/2015
 */
public class TestServiceDefaults {

  @Test
  public void testUserDefinedServices() throws IOException {
    String catalog = "/catalog/catalogs5/testServices.xml"; // serviceName ="cdmremoteOnly" from local catalog
    Catalog cat = TdsLocalCatalog.open(catalog);
    Assert.assertEquals(3, cat.getServices().size());

    check(cat, "all", 11);
    check(cat, "GridServices", 10);
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
        localServices = cat2.findService("cdmremoteOnly");
        Assert.assertNotNull(localServices);
        Assert.assertEquals(ServiceType.CdmRemote, localServices.getType());
        Assert.assertNull(cat2.findService("Resolver"));
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
