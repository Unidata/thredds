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
import thredds.TestOnLocalServer;
import thredds.client.catalog.*;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.units.DateType;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * it tests for GRIB data served through TDS
 *
 * @author caron
 * @since 4/20/2015
 */
@Category(NeedsCdmUnitTest.class)

public class TestTdsGrib {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testGribCatRefs() throws IOException {
    String catalog = "/catalog/grib/NDFD/CONUS_5km/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    Set<String> ss = new HashSet<>();
    for (Service s : cat.getServices()) {
      assert !ss.contains(s.getName()) : "already has "+s;
      ss.add(s.getName());
    }

    Dataset top = cat.getDatasetsLocal().get(0);
    for (Dataset ds : top.getDatasetsLocal()) {
      if (ds instanceof CatalogRef) {
        CatalogRef catref = (CatalogRef) ds;
        String name =  catref.getName();
        assert name != null : "name is null";
        assert name.length() > 0 : "name is empty";
      }
    }
  }

  @Test
  public void testOneFilePartition() throws IOException {
    String catalog = "/catalog/gribCollection/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);
    Assert.assertEquals(1, cat.getDatasetsLocal().size());

    Dataset top = cat.getDatasetsLocal().get(0);
    Assert.assertTrue(0 < top.getDataSize());

    DateType dt =  top.getLastModifiedDate();
    Assert.assertNotNull(dt);
    Assert.assertEquals("modified", dt.getType());
    Assert.assertTrue(dt.getCalendarDate().getMillis() > 0);

    Service all = top.getServiceDefault();
    Assert.assertNotNull(all);
    Assert.assertEquals(ServiceType.Compound, all.getType());

    boolean gotHttp = false;
    for (Service s : all.getNestedServices()) {
      if (s.getType() == ServiceType.HTTPServer)
        gotHttp = true;
    }
    Assert.assertTrue(gotHttp);
  }

  @Test
  public void testTPanalyis() throws IOException {
    String catalog = "catalog/HRRR/analysis/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);
    Assert.assertEquals(1, cat.getDatasetsLocal().size());

    Dataset full = cat.findDatasetByID("HRRR/analysis/TP");
    Assert.assertNotNull(full);
    Assert.assertEquals(11, full.getAccess().size());
    Assert.assertNull(full.getAccess(ServiceType.Resolver));
    Assert.assertNull(full.getAccess(ServiceType.HTTPServer));
    Assert.assertNotNull(full.getAccess(ServiceType.CdmRemote));
    Assert.assertNotNull(full.getAccess(ServiceType.JupyterNotebook));

    Dataset latest = cat.findDatasetByID("latest.xml");
    Assert.assertNotNull(latest);
    Assert.assertEquals(1, latest.getAccess().size());
    Assert.assertNotNull(latest.getAccess(ServiceType.Resolver));
    Assert.assertNull(latest.getAccess(ServiceType.HTTPServer));
    Assert.assertNull(latest.getAccess(ServiceType.CdmRemote));
  }

  @Test
  public void testMissingCollection() throws IOException {
    String catalogPath = TestOnLocalServer.withHttpPath("catalog/Grib/Nonexist/catalog.xml");
    try (HTTPMethod method = HTTPFactory.Get(null, catalogPath)) {
      int statusCode = method.execute();
      assert statusCode == 404;       // not 500
    }
  }

  @Test
  public void testEmptyCollection() throws IOException {
    String catalogPath = TestOnLocalServer.withHttpPath("catalog/Grib/Emptiness/catalog.xml");
    try (HTTPMethod method = HTTPFactory.Get(null, catalogPath)) {
      int statusCode = method.execute();
      assert statusCode == 404;       // not 500
    }
  }

  ///////////////////////////////////////////////////
  // work with catalogs useing declared services
  /*   <service name="some" base="" serviceType="compound">
    <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/"/>
    <service name="http" serviceType="HTTPServer" base="/thredds/fileServer/"/>
    <service name="ncss" serviceType="NetcdfSubset" base="/thredds/ncss/grid/"/>
    <service name="cdmremote" serviceType="CdmRemote" base="/thredds/cdmremote/"/>
  </service> */

  @Test
  public void testDeclaredServices() throws IOException {
    String catUrl = "/catalog/grib/NDFD/CONUS_5km/catalog.xml";  // service name "some"
    Catalog cat = TdsLocalCatalog.open(catUrl);
    Assert.assertNotNull(catUrl, cat);

    String id = "grib/NDFD/CONUS_5km/TwoD";
    Dataset ds = cat.findDatasetByID(id);
    Assert.assertNotNull("cant find dataset id=" + id, id);
    List<Access> accesses = ds.getAccess();

    Assert.assertEquals(3, accesses.size());
    Assert.assertNull("should not have servicetype HTTP", ds.getAccess(ServiceType.HTTPServer));
  }

  @Test
  public void testDeclaredServicesInNestedDatasets() throws IOException {
    String catUrl = "catalog/grib/NDFD/CONUS_5km/NDFD_CONUS_5km_20131212_0000.grib2/catalog.xml";  // service name "some"
    Catalog cat = TdsLocalCatalog.open(catUrl);
    Assert.assertNotNull(catUrl, cat);

    String id = "grib/NDFD/CONUS_5km/NDFD_CONUS_5km_20131212_0000.grib2";
    Dataset ds = cat.findDatasetByID(id);
    Assert.assertNotNull("cant find dataset id=" + id, id);
    List<Access> accesses = ds.getAccess();

    Assert.assertEquals(4, accesses.size());
    Assert.assertNotNull("should have servicetype HTTP", ds.getAccess(ServiceType.HTTPServer));
  }


  ///////////////////////////////////////////////////
  // work with catalogs ussing default services

  @Test
  public void testDefaultGribServices() throws IOException {
    String catalog = "/catalog/grib.v5/NDFD/CONUS_5km/catalog.xml";  // no service name, should use GRID default
    Catalog cat = TdsLocalCatalog.open(catalog);
    testCat(cat, 11, true, null, 0);

    Dataset top = cat.getDatasetsLocal().get(0);
    Assert.assertTrue(!top.hasAccess());
    for (Dataset ds : top.getDatasetsLocal()) {
      if (!(ds instanceof CatalogRef)) {
        Assert.assertTrue(ds.hasAccess());

      } else {
        CatalogRef catref = (CatalogRef) ds;
        Catalog cat2 = TdsLocalCatalog.openFromURI(catref.getURI());
        testCat(cat2, 11, false, "GridServices", 12);
        break;
      }
    }

  }

  @Test
  public void testGlobalServices() throws IOException {
    String catalog = "/catalog/gribCollection.v5/GFS_CONUS_80km/catalog.xml"; // serviceName ="all" from root catalog
    Catalog cat = TdsLocalCatalog.open(catalog);
    testCat(cat, 11, true, null, 0);
    testCat(cat, 11, true, null, 0);

    Dataset top = cat.getDatasetsLocal().get(0);
    Assert.assertTrue(!top.hasAccess());
    for (Dataset ds : top.getDatasetsLocal()) {
      if (!(ds instanceof CatalogRef)) {
        Assert.assertTrue(ds.hasAccess());

      } else {
        CatalogRef catref = (CatalogRef) ds;
        Catalog cat2 = TdsLocalCatalog.openFromURI(catref.getURI());
        testCat(cat2, 11, false, "all", 12);
        break;
      }
    }

  }

  @Test
  public void testUserDefinedServices() throws IOException {
    String catalog = "/catalog/restrictCollection.v5/GFS_CONUS_80km/catalog.xml"; // serviceName ="cdmremoteOnly" from local catalog
    Catalog cat = TdsLocalCatalog.open(catalog);
    Service localServices = cat.findService("cdmremoteOnly");
    Assert.assertNotNull(localServices);
    Assert.assertEquals(ServiceType.CdmRemote, localServices.getType());

    Service resolverService = cat.findService("Resolver");
    Assert.assertNotNull(resolverService);
    Assert.assertEquals(ServiceType.Resolver, resolverService.getType());

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
        Assert.assertNull(cat2.findService("Resolver"));
        break;
      }
    }
   }

  private void testCat(Catalog cat, int virtCount, boolean hasResolver, String orgName, int orgCount) throws IOException {
    Service virtualServices = cat.findService("VirtualServices");
    Assert.assertNotNull(virtualServices);
    Assert.assertEquals(ServiceType.Compound, virtualServices.getType());
    Assert.assertNotNull(virtualServices.getNestedServices());
    Assert.assertEquals(virtCount, virtualServices.getNestedServices().size());
    for (Service sn : virtualServices.getNestedServices())
      Assert.assertNotEquals(ServiceType.HTTPServer, sn.getType());

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
      Assert.assertEquals(ServiceType.Compound, orgServices.getType());
      Assert.assertNotNull(orgServices.getNestedServices());
      Assert.assertEquals(orgCount, orgServices.getNestedServices().size());
      boolean hasFileServer = false;
      for (Service sn : orgServices.getNestedServices())
        if( ServiceType.HTTPServer == sn.getType()) hasFileServer = true;
      Assert.assertTrue(hasFileServer);
    }
  }

}

