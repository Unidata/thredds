/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.tools.DataFactory;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;

/**
 * Use DataFactory on various URLs
 *
 * @author caron
 * @since 2/18/2016.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestDataFactory {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][]{
        /// GRIB feature collections
        { TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/grib.v5/NDFD/CONUS_5km/catalog.xml#grib.v5/NDFD/CONUS_5km/TwoD"), FeatureType.FMRC, ServiceType.CdmrFeature},
        { TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/gribCollection.v5/GFS_CONUS_80km/catalog.xml#gribCollection.v5/GFS_CONUS_80km/TwoD"), FeatureType.FMRC, ServiceType.CdmrFeature},
        { TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/gribCollection.v5/GFS_CONUS_80km/catalog.xml#gribCollection.v5/GFS_CONUS_80km/Best"), FeatureType.GRID, ServiceType.CdmrFeature},
        { TestOnLocalServer.withProtocolAndPath("thredds:resolve:http", "catalog/gribCollection.v5/GFS_CONUS_80km/latest.xml"), FeatureType.GRID, ServiceType.CdmrFeature},
        { TestOnLocalServer.withProtocolAndPath("thredds:resolve:http", "catalog/grib/NDFD/CONUS_5km/latest.xml"), FeatureType.GRID, ServiceType.CdmRemote},
        // {"thredds:resolve:http://rdavm.ucar.edu:8080/thredds/catalog/aggregations/g/ds083.2/1/latest.xml", FeatureType.GRID, ServiceType.CdmrFeature},
        { TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/gribCollection.v5/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1/catalog.xml#gribCollection.v5/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1"), FeatureType.GRID, ServiceType.CdmrFeature},
        { TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/rdaTest/ds094.2_dt/catalog.xml#rdaTest/ds094.2_dt/GaussLatLon_880X1760-0p00N-180p00E"), FeatureType.GRID, ServiceType.CdmrFeature},

        // dataset or datasetScan
        { TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/catalog.xml#testDataset"), FeatureType.GRID, ServiceType.OPENDAP},
        { TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/testEnhanced/catalog.xml#testEnhanced/2004050412_eta_211.nc"), FeatureType.GRID, ServiceType.OPENDAP},

        // test that cdmRemote takes precedence over OpenDAP
        // {TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/hioos/model/wav/swan/oahu/catalog.xml#hioos/model/wav/swan/oahu/SWAN_Oahu_Regional_Wave_Model_(500m)_fmrc.ncd"), FeatureType.GRID, ServiceType.CdmRemote},

        /// point data
        { TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/testStationScan/catalog.xml#testStationScan/Surface_METAR_20130824_0000.nc"), FeatureType.STATION, ServiceType.CdmRemote},
        // LOOK not ready yet
        // {TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/testStationFeatureCollection/catalog.xml#testStationFeatureCollection/Metar_Station_Data_fc.cdmr"), FeatureType.STATION, ServiceType.CdmrFeature},
        { TestOnLocalServer.withProtocolAndPath("thredds:resolve:http", "catalog/testStationFeatureCollection/files/latest.xml"), FeatureType.STATION, ServiceType.CdmRemote},
        { TestOnLocalServer.withProtocolAndPath("thredds:http", "catalog/testStationFeatureCollection/files/catalog.xml#testStationFeatureCollection/files/Surface_METAR_20060328_0000.nc"), FeatureType.STATION, ServiceType.CdmRemote},
    });
  }

  @Parameterized.Parameter(value = 0)
  public String path;

  @Parameterized.Parameter(value = 1)
  public FeatureType expectFeature;

  @Parameterized.Parameter(value = 2)
  public ServiceType expectService;

  @Test
  public void testOpenFromDataFactory() throws IOException {
    DataFactory fac = new DataFactory();
    try (DataFactory.Result result = fac.openFeatureDataset(path, null)) {
      if (result.fatalError) {
        logger.debug("Dataset fatalError = {}", result.errLog);
        assert false;
      } else {
        logger.debug("Dataset '{}' opened as type = {}", path, result.featureDataset.getFeatureType());
        Assert.assertEquals(expectService, result.accessUsed.getService().getType());
        Assert.assertEquals(expectFeature, result.featureDataset.getFeatureType());
      }
    }
  }
}
