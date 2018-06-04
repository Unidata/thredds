/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.cdmr;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.*;
import thredds.client.catalog.tools.CatalogCrawler;
import thredds.client.catalog.tools.DataFactory;
import thredds.server.catalog.TdsLocalCatalog;
import ucar.ma2.Array;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.ft2.coverage.*;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

@Category(NeedsCdmUnitTest.class)
public class TestCdmRemoteServer2 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testCdmRemote() throws IOException {
    Catalog cat = TdsLocalCatalog.open(null);

    Dataset ds = cat.findDatasetByID("testClimatology");
    assert (ds != null) : "cant find dataset 'testClimatology'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset( ds, null)) {
      if (dataResult.fatalError) {
        logger.debug("fatalError = {}", dataResult.errLog);
        assert false;
      }
      Assert.assertNotNull(dataResult.featureDataset);
      Assert.assertEquals(ucar.nc2.ft2.coverage.FeatureDatasetCoverage.class, dataResult.featureDataset.getClass());

      FeatureDatasetCoverage gds = (FeatureDatasetCoverage) dataResult.featureDataset;
      String gridName = "sst";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      Assert.assertEquals(1, gds.getCoverageCollections().size());
      CoverageCollection cc = gds.getCoverageCollections().get(0);
      Coverage grid = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      CoverageCoordSys gcs = grid.getCoordSys();
      Assert.assertNotNull(gcs);
      assert null == gcs.getZAxis();

      CoverageCoordAxis time = gcs.getTimeAxis();
      Assert.assertNotNull("time axis", time);
      Assert.assertEquals(12, time.getNcoords());

      double[] expect = new double[]{366.0, 1096.485, 1826.97, 2557.455, 3287.94, 4018.425, 4748.91, 5479.395, 6209.88, 6940.365, 7670.85, 8401.335};
      Array data = time.getCoordsAsArray();
      for (int i = 0; i < expect.length; i++)
        Assert2.assertNearlyEquals(expect[i], data.getDouble(i));
    }
  }

  @Test
  public void testUrlReading() throws IOException {
    Catalog cat = TdsLocalCatalog.open("catalog/scanCdmUnitTests/formats/netcdf3/catalog.xml");
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.Type.all_direct, 0, null, new CatalogCrawler.Listener() {

      @Override
      public void getDataset(Dataset dd, Object context) {
        try {
          doOne(dd);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    },  null, null, null);
    long start = System.currentTimeMillis();
    try {
      crawler.crawl(cat);

    } finally {
      long took = (System.currentTimeMillis() - start);
      System.out.format("***Done " + cat + " took = " + took + " msecs%n");
    }
  }

  private void doOne(Dataset ds) throws IOException {
    Access access = ds.getAccess(ServiceType.CdmRemote);
    if (access == null) {
      logger.debug("No cdmremote access for {}", ds.getName());
      return;
    }

    DataFactory fac = new DataFactory();
    DataFactory.Result dataResult = fac.openFeatureDataset(access, null);
    logger.debug("DataFactory.Result = {}", dataResult);
  }

  @Test
  public void testCdmRemoteOnly() throws IOException {
    Catalog cat = TdsLocalCatalog.open("catalog/cdmremote.v5/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1/catalog.xml");

    Dataset ds = cat.findDatasetByID("cdmremote.v5/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1");
    assert (ds != null) : "cant find dataset";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset( ds, null)) {
      Assert.assertTrue(dataResult.errLog.toString(), !dataResult.fatalError);
      Assert.assertNotNull(dataResult.featureDataset);
      Assert.assertEquals( ucar.nc2.dt.grid.GridDataset.class, dataResult.featureDataset.getClass());

      ucar.nc2.dt.grid.GridDataset gds = ( ucar.nc2.dt.grid.GridDataset) dataResult.featureDataset;
      String gridName = "Pressure_surface";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      GeoGrid grid = gds.findGridByShortName(gridName);
      Assert.assertNotNull(gridName, grid);

      GridCoordSystem gcs = grid.getCoordinateSystem();
      Assert.assertNotNull(gcs);
      assert null == gcs.getVerticalAxis();

      CoordinateAxis time = gcs.getTimeAxis();
      Assert.assertNotNull("time axis", time);
      Assert.assertEquals(36, time.getSize());

      Array data = grid.readDataSlice(0,0,0,0);
    }
  }
}
