package thredds.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import thredds.servlet.ThreddsConfig;

import java.io.File;

public class ThreddsConfigTest {

	private String threddsConfigPath;
	
	@Before
	public void setUp(){
		
		//It uses maven path for resources and default threddsConfig
		//threddsConfigPath ="C:\dev\github\thredds3\tds\src\test\resources\content2\thredds\threddsConfig.xml"
		threddsConfigPath="src/test/resources/content2/thredds/threddsConfig.xml";
    File f = new File(threddsConfigPath);
    System.out.printf("threddsConfigPath= %s exist=%s%n", f.getAbsolutePath(), f.exists());
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
