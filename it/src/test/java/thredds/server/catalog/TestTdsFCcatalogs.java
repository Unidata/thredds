/* Copyright */
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import thredds.util.ContentType;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;

/**
 * Sanity check opening Feature Collection catalogs
 *
 * @author caron
 * @since 4/20/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestTdsFCcatalogs {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}{1}")
  public static Collection<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][]{
            {"catalogGrib", ""},
            {"grib/NDFD/CONUS_5km/catalog", ""},
            {"grib/NDFD/CONUS_5km/latest", ""},

            {"catalogFmrc", ""},
            {"testGFSfmrc/catalog", ""},
            {"testGFSfmrc/catalog", "?dataset=testGFSfmrc/GFS_CONUS_80km_nc_best.ncd"},
            {"testNAMfmrc/runs/catalog", ""},
            {"testNAMfmrc/runs/catalog", "?dataset=testNAMfmrc/runs/NAM_FMRC_RUN_2006-09-26T00:00:00Z"},

            {"catalogs5/pointCatalog5", ""},
            {"testStationScan.v5/catalog", "?dataset=testStationScan.v5/Surface_METAR_20130823_0000.nc"},       // datasetScan
            {"testStationFeatureCollection.v5/files/catalog", "?dataset=testStationFeatureCollection.v5/files/Surface_METAR_20060328_0000.nc"}, // point fc
    });
  }

  @Parameterized.Parameter(value = 0)
  public String path;

  @Parameterized.Parameter(value = 1)
  public String query;

  @Test
  public void testOpenXml() {
    String endpoint = TestOnLocalServer.withHttpPath("catalog/"+path+".xml"+query);
    byte[] response = TestOnLocalServer.getContent(endpoint, 200, ContentType.xml);
    Assert.assertNotNull(response);
    logger.debug(new String(response, CDM.utf8Charset));
  }

  @Test
  public void testOpenHtml() {
    String endpoint = TestOnLocalServer.withHttpPath("catalog/"+path+".html"+query);
    byte[] response = TestOnLocalServer.getContent(endpoint, 200, ContentType.html);
    Assert.assertNotNull(response);
    logger.debug(new String(response, CDM.utf8Charset));
  }
}
