package thredds.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import thredds.mock.web.MockTdsContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext.xml"},loader=MockTdsContextLoader.class)
public class ThreddsConfigTest {

  @Autowired
  private TdsContext tdsContext;

	private String threddsConfigPath;

	@Before
	public void setUp(){
		
		//It uses maven path for resources and default threddsConfig
		//threddsConfigPath ="C:/dev/github/thredds3/tds/src/test/content/thredds/threddsConfig.xml";
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
	   assertFalse(ThreddsConfig.hasElement("AggregationCache") );
	}
	
	
}
