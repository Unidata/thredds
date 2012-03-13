package thredds.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import thredds.servlet.ThreddsConfig;

public class ThreddsConfigTest {

	private String threddsConfigPath;
	
	@Before
	public void setUp(){
		
		//It uses maven path for resources and default threddsConfig
		threddsConfigPath ="target/test-classes/WEB-INF/altContent/startup/threddsConfig.xml";
		ThreddsConfig.init(threddsConfigPath);
	}
	
	@Test
	public void testGet(){
		
		assertEquals("Support", ThreddsConfig.get( "serverInformation.contact.name", null));
		assertEquals("false", ThreddsConfig.get( "CatalogServices.allowRemote", null));
		assertEquals(null, ThreddsConfig.get( "WMS.allow", null));
	}
	
	@Test 
	public void testHasElement(){
	    
	   assertFalse(ThreddsConfig.hasElement("AggregationCache") );	
	}
	
}
