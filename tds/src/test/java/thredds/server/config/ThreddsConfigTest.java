package thredds.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import thredds.servlet.ThreddsConfig;

public class ThreddsConfigTest {

	private String threddsConfigPath;
	
	@Before
	public void setUp(){
		
		//It uses maven path for resources and default threddsConfig
		//threddsConfigPath ="target/test-classes/WEB-INF/altContent/startup/threddsConfig.xml";
		threddsConfigPath="target/test-classes/content/thredds/threddsConfig.xml";
		ThreddsConfig.init(threddsConfigPath);
	}
	
	@Test
	public void testGet(){
		
		assertEquals("THREDDS Support", ThreddsConfig.get( "serverInformation.contact.name", null));
		assertEquals("true", ThreddsConfig.get( "CatalogServices.allowRemote", null));
		assertEquals("true", ThreddsConfig.get( "WMS.allow", null));
		assertEquals( 8388608, ThreddsConfig.getBytes( "NetcdfSubsetService.maxFileDownloadSize", -1L));
	}
	
	@Test 
	public void testHasElement(){
	    
	   assertFalse(ThreddsConfig.hasElement("AggregationCache") );	
	}
	
	
}
