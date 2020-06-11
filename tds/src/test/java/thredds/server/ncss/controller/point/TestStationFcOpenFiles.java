package thredds.server.ncss.controller.point;

import java.lang.invoke.MethodHandles;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import thredds.mock.web.MockTdsContextLoader;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.cache.FileCache;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext-tdsConfig.xml"}, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class TestStationFcOpenFiles {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;
  private FileCache rafCache;

  @Before
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    rafCache = (FileCache) RandomAccessFile.getGlobalFileCache();
    rafCache.clearCache(true);
    NetcdfDataset.getNetcdfFileCache().clearCache(true);
  }

  @Test
  public void checkFeatureTypePoint() throws Exception {
    // This collection uses FeatureType=Station
    String dataset = "/ncss/point/testSurfaceGempakFeatureCollection/Surface_Point_Data_-_GEMKAP_fc.cdmr";
    String partialReq = "?var=TMPC&west=-71&east=-70&south=42.5&north=42.6&accept=csv";

    // Will cause TDS to look in one file for data
    String req = String.format(partialReq + "&time=%s", "2009-05-22T13:00:00");
    testReq(dataset, req);

    // Will cause TDS to look in multiple multiple files for data, including the
    // the file needed by the previous request
    req = String.format(partialReq + "&time=%s", "2009-05-22T00:00:00");
    testReq(dataset, req);
  }

  @Test
  public void checkFeatureTypeStation() throws Exception {
    // This collection uses FeatureType=Station
    String dataset = "/ncss/point/testStationFeatureCollection/Metar_Station_Data_fc.cdmr";
    //String partialReq = "?var=air_temperature&west=-71&east=-70&south=42.5&north=42.6&accept=csv";
    String partialReq = "?var=air_temperature&west=-88&east=-62&south=34&north=50&accept=csv";

    // Will cause TDS to look in one file for data
    String req = String.format(partialReq + "&time=%s", "2006-03-26T13:00:00");
    testReq(dataset, req);

    // Will cause TDS to look in multiple multiple files for data, including the
    // the file needed by the previous request
    req = String.format(partialReq + "&time=%s", "2006-03-26T00:00:00");
    testReq(dataset, req);
  }

  @Test
  public void checkFeatureTypePointWithMissing() throws Exception {
    // This collection uses FeatureType=Point
    String dataset = "/ncss/point/metarArchive2019Point/ncdecoded/2019_Archived_Metar_Point_Data_fc.cdmr";
    String partialReq = "?var=CTYM&var=WNUM&var=CTYH&var=CHC2&var=CHC3&var=CHC1&var=DWPC&var=ALTI&var=PMSL&var=VSBY&var=TMPC&var=CTYL&var=DRCT&var=SKNT&west=-88&east=-62&south=34&north=50&accept=csv";

    // Will cause TDS to look in one file for data
    String req = String.format(partialReq + "&time=%s", "2019-04-03T00:30:00");
    testReq(dataset, req);

    // Will cause TDS to look in multiple multiple files for data, including the
    // the file needed by the previous request
    req = String.format(partialReq + "&time=%s", "2019-04-03T00:00:00");
    testReq(dataset, req);
  }

  @Test
  public void checkFeatureTypeStationWithMissing() throws Exception {
    // This collection uses FeatureType=Station, but has a combination of data and
    // a request that results in a StationTimeSeriesFeatureCollection not containing
    // a given station for a given file, which exposed a bug.
    String dataset = "/ncss/point/metarArchive2019/ncdecoded/2019_Archived_Metar_Station_Data_fc.cdmr";
    String partialReq = "?var=CTYM&var=WNUM&var=CTYH&var=CHC2&var=CHC3&var=CHC1&var=DWPC&var=ALTI&var=PMSL&var=VSBY&var=TMPC&var=CTYL&var=DRCT&var=SKNT&west=-88&east=-62&south=34&north=50&accept=csv";

    // Will cause TDS to look in one file for data
    String req = String.format(partialReq + "&time=%s", "2019-04-03T00:30:00");
    testReq(dataset, req);

    // Will cause TDS to look in multiple multiple files for data, including the
    // the file needed by the previous request
    // This triggered multiple enteries of the same file in the cache, as we
    // we not releasing/closing resource in the CompositeStationFeatureIterator
    req = String.format(partialReq + "&time=%s", "2019-04-03T00:00:00");
    testReq(dataset, req);
  }

  private void checkCache(List<String> cacheEntries) {
    boolean isAnyFileLocked = false;
    for (String ce : cacheEntries) {
      if (ce.startsWith("true")) {
        isAnyFileLocked = true;
      }
    }
    Assert.assertFalse(isAnyFileLocked);
  }

  private void testReq(String dataset, String req) throws Exception {
    List<String> cacheEntries = rafCache.showCache();

    RequestBuilder rb = MockMvcRequestBuilders.get(dataset + req).servletPath(dataset);
    System.out.printf("%nURL='%s'%n", dataset + req);
    ResultActions mockMvc = this.mockMvc.perform(rb);
    MvcResult result = mockMvc.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    MockHttpServletResponse response = result.getResponse();
    // if any file in the cache is locked, the string representation of that cache entry
    // will start with true. We do not want any entry to be locked, so make sure this
    // none of these entries start with "true"
    cacheEntries = rafCache.showCache();
    int numberOfCacheEnteries = cacheEntries.size();
    checkCache(cacheEntries);

    // make another request for the same data
    // the cache should not change in size. If so, we're not releasing
    // resources properly.
    mockMvc = this.mockMvc.perform(rb);
    MvcResult result2 = mockMvc.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    MockHttpServletResponse response2 = result2.getResponse();

    cacheEntries = rafCache.showCache();
    // make sure we didn't end up with more or less files in the cache after a new request
    Assert.assertEquals(numberOfCacheEnteries, cacheEntries.size());
    // make sure none of the entries are locked
    checkCache(cacheEntries);

    // Not directly related to the resource issue, but while we're here, let's
    // make sure the first and second requests, which are identical, return the same data.
    Assert.assertArrayEquals(response.getContentAsByteArray(), response2.getContentAsByteArray());
  }
}
