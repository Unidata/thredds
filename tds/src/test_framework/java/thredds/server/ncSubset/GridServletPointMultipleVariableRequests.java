package thredds.server.ncSubset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;
import thredds.mock.web.ncss.NcssMockHttpServletRequest;
import thredds.test.util.xml.XmlUtil;

/*
 * Pressure_reduced_to_MSL_msl   -- VAR_7-0-2-2_L102
 * Pressure_surface              -- VAR_7-0-2-1_L1
 * Convective_inhibition_surface -- VAR_7-0-2-156_L1
 * 
 * Relative_humidity_height_above_ground -- VAR_7-0-2-52_L105
 * Temperature_height_above_ground       -- VAR_7-0-2-11_L105
 * 
 * Temperature_isobaric         -- VAR_7-0-2-11_L100
 * Relative_humidity_isobaric   -- VAR_7-0-2-52_L100
 * 
 */

@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@TdsContentRootPath(path = "/share/testcatalogs/content")
public class GridServletPointMultipleVariableRequests extends GridServletRequestTest{
	
	
	
	/*----------------------------------------------------------------
	  Requests on multiple vars without vertical levels;
	----------------------------------------------------------------*/
	@Test
	public void testNCSSPointXMLNoVertlLevelVarRequest() throws ServletException, IOException, JDOMException {

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-2_L102,VAR_7-0-2-1_L1,VAR_7-0-2-156_L1").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
		
		//Expected response: lat and lon = 0, and values NaN????		
		Document doc = XmlUtil.getStringResponseAsDoc(response);
		assertTrue( XmlUtil.containsXPath("/grid", doc).size() > 0 );
		assertTrue( XmlUtil.containsXPath("/grid/point", doc).size() > 0 );
		Element dateTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='date']", doc).get(0);
		assertEquals( "2012-02-29T12:00:00.000Z",  dateTag.getText());
		Element var1Tag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-2_L102' and @units='Pa']", doc).get(0);
		assertFalse( "NaN".equals(var1Tag.getText()));
		Element var2Tag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-1_L1' and @units='Pa']", doc).get(0);
		assertFalse( "NaN".equals(var2Tag.getText()));
		Element var3Tag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-156_L1' and @units='J/kg']", doc).get(0);
		assertFalse( "NaN".equals(var3Tag.getText()));		
		Element latTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lat' and @units='degrees_north']", doc).get(0);
		assertFalse( "0.0".equals(latTag.getText()) );
		Element lonTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lon' and @units='degrees_east' ]", doc).get(0);
		assertFalse( "0.0".equals(lonTag.getText()) );		
		
	}
	
	@Test
	public void testNCSSPointCSVNoVertlLevelVarRequest() throws ServletException, IOException {

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-2_L102,VAR_7-0-2-1_L1,VAR_7-0-2-156_L1").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("csv").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "text/csv", response.getContentType() );			
		//check csv content		
	}
	
	@Test
	public void testNCSSPointNetCDFNoVertlLevelVarRequest() throws ServletException, IOException {

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-2_L102,VAR_7-0-2-1_L1,VAR_7-0-2-156_L1").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("netcdf").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/x-netcdf", response.getContentType() );			
		//check binary content
		fail("Not yet implemented");
	}
	
	/*----------------------------------------------------------------
	  Requests on multiple vars with same vertical levels;
	----------------------------------------------------------------*/
	
	/*----------------------------------------------------------------
	  Vertical Dimension with one single level
	----------------------------------------------------------------*/	
	@Test
	public void testNCSSPointXMLOneVertlLevelVarRequest() throws ServletException, IOException, JDOMException {

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").									
									setVar("VAR_7-0-2-52_L105,VAR_7-0-2-11_L105").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
			
		Document doc = XmlUtil.getStringResponseAsDoc(response);
		assertTrue( XmlUtil.containsXPath("/grid", doc).size() > 0 );
		assertTrue( XmlUtil.containsXPath("/grid/point", doc).size() > 0 );
		Element dateTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='date']", doc).get(0);
		assertEquals( "2012-02-29T12:00:00.000Z",  dateTag.getText());
		Element var1Tag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-52_L105' and @units='%']", doc).get(0);
		assertFalse( "NaN".equals(var1Tag.getText()));
		Element var2Tag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-11_L105' and @units='K']", doc).get(0);
		assertFalse( "NaN".equals(var2Tag.getText()));
		Element latTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lat' and @units='degrees_north']", doc).get(0);
		assertFalse( "0.0".equals(latTag.getText()) );
		Element lonTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lon' and @units='degrees_east' ]", doc).get(0);
		assertFalse( "0.0".equals(lonTag.getText()) );		
		
	}	
	
	@Test
	public void testNCSSPointCSVOneVertlLevelVarRequest() throws ServletException, IOException {

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-52_L105,VAR_7-0-2-11_L105").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("csv").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "text/csv", response.getContentType() );		
		//check csv content
	}	

	@Test
	public void testNCSSPointNetCDFOneVertlLevelVarRequest() throws ServletException, IOException {

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-52_L105,VAR_7-0-2-11_L105").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("netcdf").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/x-netcdf", response.getContentType() );		
		//check binary content
		fail("Not yet implemented");
	}	
	
	/*----------------------------------------------------------------
	  Vertical Dimension with multiple levels
	----------------------------------------------------------------*/		
	@Test
	public void testNCSSPointXMLMultipleVertlLevelVarRequest() throws ServletException, IOException, JDOMException{

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-11_L100,VAR_7-0-2-52_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
			
		Document doc = XmlUtil.getStringResponseAsDoc(response);
		assertTrue( XmlUtil.containsXPath("/grid", doc).size() > 0 );
		assertTrue( XmlUtil.containsXPath("/grid/point", doc).size() > 0 );
		Element dateTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='date']", doc).get(0);
		assertEquals( "2012-02-29T12:00:00.000Z",  dateTag.getText());
		
		List vertCoordTags = XmlUtil.containsXPath("/grid/point/data[@name='vertCoord' and @units='hPa' ]", doc);
		assertTrue( vertCoordTags.size()== 29 );
		
		Element var1Tag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-52_L100' and @units='%']", doc).get(0);
		assertFalse( "NaN".equals(var1Tag.getText()));
		Element var2Tag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-11_L100' and @units='K']", doc).get(0);
		assertFalse( "NaN".equals(var2Tag.getText()));
		Element latTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lat' and @units='degrees_north']", doc).get(0);
		assertFalse( "0.0".equals(latTag.getText()) );
		Element lonTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lon' and @units='degrees_east' ]", doc).get(0);
		assertFalse( "0.0".equals(lonTag.getText()) );		
		
	}
	
	@Test
	public void testNCSSPointCSVMultipleVertlLevelVarRequest() throws ServletException, IOException{

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-52_L100,VAR_7-0-2-52_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("csv").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "text/csv", response.getContentType() );
		//Check csv content		
		
	}
	
	@Test
	public void testNCSSPointNetCDFMultipleVertlLevelVarRequest() throws ServletException, IOException {

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-52_L100,VAR_7-0-2-52_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("netcdf").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/x-netcdf", response.getContentType() );
		//Check binary content		
		fail("Not yet implemented");
	}
	
	/*----------------------------------------------------------------
	  Requests on multiple vars with different vertical levels;
	----------------------------------------------------------------*/
	//We should get vertical values of the first variable processed and good values for the matching level and NaN for the others?
	@Test
	public void testNCSSPointXMLDifferentVertlLevelVarRequest() throws ServletException, IOException, JDOMException {

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-11_L100,VAR_7-0-2-41_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
		
		//Expected response: lat and lon = 0, and values NaN????		
		Document doc = XmlUtil.getStringResponseAsDoc(response);
		assertTrue( XmlUtil.containsXPath("/grid", doc).size() > 0 );
		assertTrue( XmlUtil.containsXPath("/grid/point", doc).size() > 0 );
		Element dateTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='date']", doc).get(0);
		assertEquals( "2012-02-29T12:00:00.000Z",  dateTag.getText());
		
		List vertCoordTags = XmlUtil.containsXPath("/grid/point/data[@name='vertCoord' and @units='hPa' ]", doc);
		assertTrue( vertCoordTags.size()== 29 );		
		Element var1Tag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-11_L100' and @units='K']", doc).get(0);
		assertFalse( "NaN".equals(var1Tag.getText()));
		Element var2Tag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-41_L100' and @units='1/s']", doc).get(0);
		//NaN is expected for the non matching levels...
		//Just now we are getting NaN for all the values, when we get correct values we should check matching and non matching levels and the values of the vertCoord
		assertTrue( "NaN".equals(var2Tag.getText()));
		//Element latTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lat' and @units='degrees_north']", doc).get(0);
		//assertFalse( "0.0".equals(latTag.getText()) );
		//Element lonTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lon' and @units='degrees_east' ]", doc).get(0);
		//assertFalse( "0.0".equals(lonTag.getText()) );		
		
	}
	
	@Test
	public void testNCSSPointCSVDifferentVertlLevelVarRequest() throws ServletException, IOException {

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-11_L100,VAR_7-0-2-41_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("csv").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "text/csv", response.getContentType() );
		//check csv values				
	}	

	@Test
	public void testNCSSPointNetCDFDifferentVertlLevelVarRequest() throws ServletException, IOException {

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-11_L100,VAR_7-0-2-41_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("netcdf").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/x-netcdf", response.getContentType() );
		//check binary values
		fail("Not yet implemented");
	}	
	
	/*----------------------------------------------------------------
	  Bad requests
	----------------------------------------------------------------*/
	@Test
	public void testVerticalCoordIllegalParamValueRequest() throws ServletException, IOException {
		
		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTemporal("point").
									setTime("2012-02-29T12:00:00.000Z").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeEnd("2012-02-29T12:00:00.000Z").
									setVar("VAR_7-0-2-11_L100,VAR_7-0-2-41_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setVertCoord("850,100").
									setAccept("netcdf").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(400, response.getStatus()); // --> shouldn't be 200?
		//assertEquals( "text/plain", response.getContentType() ); //Is null ??		
		assertTrue(response.getContentAsString().contains("Illegal param"));
		
	}	
	
}
