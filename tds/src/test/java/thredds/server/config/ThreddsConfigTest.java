package thredds.server.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import thredds.mock.web.MockTdsContextLoader;
import thredds.servlet.ThreddsConfig;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.test.category.NeedsContentRoot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"},loader=MockTdsContextLoader.class)
@Category(NeedsContentRoot.class)
public class ThreddsConfigTest {

  @Autowired
  private TdsContext tdsContext;

	private String threddsConfigPath;

	@Before
	public void setUp(){
		//threddsConfigPath ="/thredds/tds/src/test/content/thredds/threddsConfig.xml";
		threddsConfigPath= tdsContext.getContentRootPath() +  "/thredds/threddsConfig.xml";
		ThreddsConfig.init(threddsConfigPath);
	}
	
	@Test
	public void testGet(){
		assertEquals("THREDDS Support", ThreddsConfig.get( "serverInformation.contact.name", null));
		assertEquals("true", ThreddsConfig.get( "CatalogServices.allowRemote", null));
		assertEquals("true", ThreddsConfig.get( "WMS.allow", null));
		assertEquals( 52428800, ThreddsConfig.getBytes( "NetcdfSubsetService.maxFileDownloadSize", -1L));
	}
	
	@Test 
	public void testHasElement(){
	   assertFalse(ThreddsConfig.hasElement("CORS") );
	}

	// Tests the "cachePathPolicy" element, added in response to this message on the thredds mailing list:
	// http://www.unidata.ucar.edu/mailing_lists/archives/thredds/2016/msg00001.html
	@Test
	public void testCachePathPolicy() {
		String policyStr = ThreddsConfig.get("AggregationCache.cachePathPolicy", null);
		assertEquals("OneDirectory", policyStr);

		DiskCache2.CachePathPolicy policyObj = DiskCache2.CachePathPolicy.valueOf(policyStr);
		assertSame(DiskCache2.CachePathPolicy.OneDirectory, policyObj);
	}

	@Test
	public void testNetcdf4ClibraryUseForReading() {
		assertFalse(ThreddsConfig.getBoolean("Netcdf4Clibrary.useForReading", true));
	}
}
