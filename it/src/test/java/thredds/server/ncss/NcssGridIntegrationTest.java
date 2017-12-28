package thredds.server.ncss;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import thredds.util.ContentType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertNotNull;

@Category(NeedsCdmUnitTest.class)
public class NcssGridIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /* @HttpTest(method = Method.GET, path = "ncss/grid/gribCollection/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1/GC?var=Temperature_isobaric&latitude=40&longitude=-102&vertCoord=225")
  public void checkGridAsPointXml() throws JDOMException, IOException {
    assertOk(response);
    String xml = response.getBody(String.class);
    logger.debug("xml={}", xml);
    Reader in = new StringReader(xml);
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    XPathExpression<Element> xpath = XPathFactory.instance().compile("/grid/point/data[@name='Temperature_isobaric']", Filters.element());
    List<Element> elements = xpath.evaluate(doc);
    assertEquals(1, elements.size());
  } */

  @Test
  public void checkGrid() throws Exception {
    String endpoint = TestOnLocalServer.withHttpPath("/ncss/grid/gribCollection/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1?var=Temperature_isobaric");

    byte[] content = TestOnLocalServer.getContent(endpoint, 200, ContentType.netcdf);
    // Open the binary response in memory
    try (NetcdfFile nf = NetcdfFile.openInMemory("test_data.nc", content)) {
      GridDataset gdsDataset = new GridDataset(new NetcdfDataset(nf));
      assertNotNull(gdsDataset.findGridByName("Temperature_isobaric"));
      logger.debug("{}", nf);
    }
  }

  @Test
  public void checkGridNoVars() throws Exception {
    String endpoint = TestOnLocalServer.withHttpPath("/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd");
    TestOnLocalServer.getContent(endpoint, 400, null);
  }

  @Test
  public void checkFmrcBest() throws Exception {
    String endpoint = TestOnLocalServer.withHttpPath("/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd?var=Relative_humidity_height_above_ground,Temperature_height_above_ground");

    byte[] content = TestOnLocalServer.getContent(endpoint, 200, ContentType.netcdf);

    // Open the binary response in memory
    try (NetcdfFile nf = NetcdfFile.openInMemory("test_data.nc", content)) {
      GridDataset gdsDataset = new GridDataset(new NetcdfDataset(nf));
      assertNotNull(gdsDataset.findGridByName("Relative_humidity_height_above_ground"));
      logger.debug("{}", nf);
    }
  }

  // this fails when _ChunkSizes are left on
  @Test
  public void testNcssFailure() throws Exception{
    String filename = "scanCdmUnitTests/formats/netcdf4/COMPRESS_LEV2_20140201000000-GLOBCURRENT-L4-CURekm_15m-ERAWS_EEM-v02.0-fv01.0.nc";
    String endpoint = TestOnLocalServer.withHttpPath("/ncss/grid/"+filename+
            "?var=eastward_ekman_current_velocity&north=79.8750&west=-140&east=170&south=-79.8750&horizStride=1&"+
            "time_start=2014-02-01T00%3A00%3A00Z&time_end=2014-02-01T00%3A00%3A00Z&timeStride=1&accept=netcdf4");

    byte[] content = TestOnLocalServer.getContent(endpoint, 200, ContentType.netcdf);

    // Open the binary response in memory
    try (NetcdfFile nf = NetcdfFile.openInMemory("test_data.nc", content)) {
      GridDataset gdsDataset = new GridDataset(new NetcdfDataset(nf));
      assertNotNull(gdsDataset.findGridByName("eastward_ekman_current_velocity"));
      logger.debug("{}", nf);
    }
  }
}
