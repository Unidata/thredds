/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.tools.DataFactory;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.test.util.NeedsCdmUnitTest;

import java.io.IOException;
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
  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    String server = TestWithLocalServer.server;
    return Arrays.asList(new Object[][]{
            /// GRIB feature collections
            {"thredds:"+server+"catalog/grib.v5/NDFD/CONUS_5km/catalog.xml#grib.v5/NDFD/CONUS_5km/TwoD", FeatureType.FMRC, ServiceType.CdmrFeature},
            {"thredds:"+server+"catalog/gribCollection.v5/GFS_CONUS_80km/catalog.xml#gribCollection.v5/GFS_CONUS_80km/TwoD", FeatureType.FMRC, ServiceType.CdmrFeature},
            {"thredds:"+server+"catalog/gribCollection.v5/GFS_CONUS_80km/catalog.xml#gribCollection.v5/GFS_CONUS_80km/Best", FeatureType.GRID, ServiceType.CdmrFeature},
            {"thredds:resolve:"+server+"catalog/gribCollection.v5/GFS_CONUS_80km/latest.xml", FeatureType.GRID, ServiceType.CdmrFeature},
            {"thredds:resolve:"+server+"catalog/grib/NDFD/CONUS_5km/latest.xml", FeatureType.GRID, ServiceType.CdmRemote},
            //           {"thredds:resolve:http://rdavm.ucar.edu:8080/thredds/catalog/aggregations/g/ds083.2/1/latest.xml", FeatureType.GRID, ServiceType.CdmrFeature},
            {"thredds:"+server+"catalog/gribCollection.v5/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1/catalog.xml#gribCollection.v5/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1", FeatureType.GRID, ServiceType.CdmrFeature},
            {"thredds:"+server+"catalog/rdaTest/ds094.2_dt/catalog.xml#rdaTest/ds094.2_dt/GaussLatLon_880X1760-0p00N-180p00E", FeatureType.GRID, ServiceType.CdmrFeature},

            // dataset or datasetScan
            {"thredds:"+server+"catalog/catalog.xml#testDataset", FeatureType.GRID, ServiceType.OPENDAP},
            {"thredds:"+server+"catalog/testEnhanced/catalog.xml#testEnhanced/2004050412_eta_211.nc", FeatureType.GRID, ServiceType.OPENDAP},

            // test that cdmRemote takes precedence over OpenDAP
            // {"thredds:"+server+"catalog/hioos/model/wav/swan/oahu/catalog.xml#hioos/model/wav/swan/oahu/SWAN_Oahu_Regional_Wave_Model_(500m)_fmrc.ncd", FeatureType.GRID, ServiceType.CdmRemote},

            /// point data
            {"thredds:"+server+"catalog/testStationScan/catalog.xml#testStationScan/Surface_METAR_20130824_0000.nc", FeatureType.STATION, ServiceType.CdmRemote},
            // LOOK not ready yet
            // {"thredds:"+server+"catalog/testStationFeatureCollection/catalog.xml#testStationFeatureCollection/Metar_Station_Data_fc.cdmr", FeatureType.STATION, ServiceType.CdmrFeature},
            {"thredds:resolve:"+server+"catalog/testStationFeatureCollection/files/latest.xml", FeatureType.STATION, ServiceType.CdmRemote},
            {"thredds:"+server+"catalog/testStationFeatureCollection/files/catalog.xml#testStationFeatureCollection/files/Surface_METAR_20060328_0000.nc", FeatureType.STATION, ServiceType.CdmRemote},
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
        System.out.printf("  Dataset fatalError=%s%n", result.errLog);
        assert false;
      } else {
        System.out.printf("  Dataset '%s' opened as type=%s%n", path, result.featureDataset.getFeatureType());
        Assert.assertEquals(expectService, result.accessUsed.getService().getType());
        Assert.assertEquals(expectFeature, result.featureDataset.getFeatureType());
      }
    }
  }
}

