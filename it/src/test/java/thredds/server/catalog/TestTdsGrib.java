/* Copyright */
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.TestWithLocalServer;
import thredds.client.catalog.*;
import thredds.client.catalog.tools.DataFactory;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.ma2.Array;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.units.DateType;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.test.util.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * it tests for GRIB data served through TDS
 *
 * @author caron
 * @since 4/20/2015
 */
@Category(NeedsCdmUnitTest.class)

public class TestTdsGrib {

  @Test
  public void testGribDataset2D() throws IOException {
    String catalog = "/catalog/grib/NDFD/CONUS_5km/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset ds = cat.findDatasetByID("grib/NDFD/CONUS_5km/TwoD");
    assert (ds != null) : "cant find dataset 'dataset=grib/NDFD/CONUS_5km/TwoD'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset(ds, null)) {

      assert dataResult != null;
      assert !dataResult.fatalError : dataResult.errLog;
      assert dataResult.featureDataset != null;

      FeatureDatasetCoverage gds = (FeatureDatasetCoverage) dataResult.featureDataset;
      String gridName = "Maximum_temperature_Forecast_height_above_ground_12_Hour_Maximum";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      Assert.assertEquals(1, gds.getCoverageCollections().size());
      CoverageCollection cc = gds.getCoverageCollections().get(0);
      Coverage grid = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      int[] expectShape = new int[] {4,4,1,689,1073};
      Assert.assertArrayEquals(expectShape, grid.getShape());

      CoverageCoordSys gcs = grid.getCoordSys();
      Assert.assertNotNull(gcs);

      CoverageCoordAxis reftime = gcs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(reftime);
      Assert.assertEquals(4, reftime.getNcoords());
      double[] want = new double[]{0., 12., 24., 36.};
      CompareNetcdf2 cn = new CompareNetcdf2();
      assert cn.compareData("time", reftime.getCoordsAsArray(), Array.makeFromJavaArray(want), false);

      CoverageCoordAxis time = gcs.getTimeAxis();
      Assert.assertNotNull(time);
      Assert.assertTrue(time instanceof FmrcTimeAxis2D);
      Assert.assertEquals(16, time.getNcoords());

      //double[] want = new double[]{108.000000, 132.000000, 156.000000, 180.000000};
      //assert cn.compareData("time", time.getCoordsAsArray(), Array.makeFromJavaArray(want), false);
    }
  }

  @Test
  public void testGribLatest() throws IOException {
    String catalog = "/catalog/grib/NDFD/CONUS_5km/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset ds = cat.findDatasetByID("latest.xml");
    assert (ds != null) : "cant find dataset 'dataset=latest.xml'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset(ds, null)) {
      Assert.assertFalse(dataResult.errLog.toString(), dataResult.fatalError);
      Assert.assertNotNull(dataResult.featureDataset);
      Assert.assertEquals(FeatureDatasetCoverage.class, dataResult.featureDataset.getClass());

      FeatureDatasetCoverage gds = (FeatureDatasetCoverage) dataResult.featureDataset;
      String gridName = "Maximum_temperature_Forecast_height_above_ground_12_Hour_Maximum";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      Assert.assertEquals(1, gds.getCoverageCollections().size());
      CoverageCollection cc = gds.getCoverageCollections().get(0);
      Coverage grid = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      int[] expectShape = new int[] {4,1,689,1073};
      Assert.assertArrayEquals(expectShape, grid.getShape());

      CoverageCoordSys gcs = grid.getCoordSys();
      Assert.assertNotNull(gcs);

      CoverageCoordAxis reftime = gcs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(reftime);
      Assert.assertEquals(1, reftime.getNcoords());
      double[] runtimeValues = new double[]{0.};
      CompareNetcdf2 cn = new CompareNetcdf2();
      assert cn.compareData("time", reftime.getCoordsAsArray(), Array.makeFromJavaArray(runtimeValues), false);

      CoverageCoordAxis time = gcs.getTimeAxis();
      Assert.assertNotNull(time);
      Assert.assertEquals(4, time.getNcoords());
      double[] timeValues = new double[]{102.,126.,150,174};  // midpoints
      assert cn.compareData("time", time.getCoordsAsArray(), Array.makeFromJavaArray(timeValues), false);
    }
  }

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
    Assert.assertEquals(9, full.getAccess().size());
    Assert.assertNull(full.getAccess(ServiceType.Resolver));
    Assert.assertNull(full.getAccess(ServiceType.HTTPServer));
    Assert.assertNotNull(full.getAccess(ServiceType.CdmRemote));

    Dataset latest = cat.findDatasetByID("latest.xml");
    Assert.assertNotNull(latest);
    Assert.assertEquals(1, latest.getAccess().size());
    Assert.assertNotNull(latest.getAccess(ServiceType.Resolver));
    Assert.assertNull(latest.getAccess(ServiceType.HTTPServer));
    Assert.assertNull(latest.getAccess(ServiceType.CdmRemote));
  }

  @Test
  public void testMissingCollection() throws IOException {
    String catalogPath = TestWithLocalServer.withPath("catalog/Grib/Nonexist/catalog.xml");
    try (HTTPMethod method = HTTPFactory.Get(null, catalogPath)) {
      int statusCode = method.execute();
      assert statusCode == 404;       // not 500
    }
  }

  @Test
  public void testEmptyCollection() throws IOException {
    String catalogPath = TestWithLocalServer.withPath("catalog/Grib/Emptiness/catalog.xml");
    try (HTTPMethod method = HTTPFactory.Get(null, catalogPath)) {
      int statusCode = method.execute();
      assert statusCode == 404;       // not 500
    }
  }

  ///////////////////////////////////////////////////
  // work with catalogs with service elements removed

  @Test
  public void testDefaultGribServices() throws IOException {
    String catalog = "/catalog/grib.v5/NDFD/CONUS_5km/catalog.xml";  // no service name, should use GRID default
    Catalog cat = TdsLocalCatalog.open(catalog);
    testCat(cat, 9, true, null, 0);

    Dataset top = cat.getDatasetsLocal().get(0);
    Assert.assertTrue(!top.hasAccess());
    for (Dataset ds : top.getDatasetsLocal()) {
      if (!(ds instanceof CatalogRef)) {
        Assert.assertTrue(ds.hasAccess());

      } else {
        CatalogRef catref = (CatalogRef) ds;
        Catalog cat2 = TdsLocalCatalog.openFromURI(catref.getURI());
        testCat(cat2, 9, false, "GridServices", 10);
        break;
      }
    }

  }

  @Test
  public void testGlobalServices() throws IOException {
    String catalog = "/catalog/gribCollection.v5/GFS_CONUS_80km/catalog.xml"; // serviceName ="all" from root catalog
    Catalog cat = TdsLocalCatalog.open(catalog);
    testCat(cat, 10, true, null, 0);

    Dataset top = cat.getDatasetsLocal().get(0);
    Assert.assertTrue(!top.hasAccess());
    for (Dataset ds : top.getDatasetsLocal()) {
      if (!(ds instanceof CatalogRef)) {
        Assert.assertTrue(ds.hasAccess());

      } else {
        CatalogRef catref = (CatalogRef) ds;
        Catalog cat2 = TdsLocalCatalog.openFromURI(catref.getURI());
        testCat(cat2, 10, false, "all", 11);
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

