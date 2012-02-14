package thredds.server.ncSubset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;
import thredds.mock.web.ncss.NcssMockHttpServletRequest;
import thredds.test.util.xml.XmlUtil;


@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@TdsContentRootPath(path = "/share/testcatalogs/content")
public class GridServletTimeRequestTest extends GridServletRequestTest{

	@Test
	public void testStartAndEndTimeParamsRequest() throws ServletException, IOException, JDOMException{

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTimeStart("2012-02-29T18:00:00.000Z").
									setTimeEnd("2012-02-30T12:00:00.000Z").
									setVar("Pressure_reduced_to_MSL_msl").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
		
		Document doc = XmlUtil.getStringResponseAsDoc(response);
		List dateTags = XmlUtil.containsXPath("/grid/point/data[@name='date']", doc);
		assertEquals(4, dateTags.size());
		
	}
	
	@Test
	public void testStartAndDurationTimeParamsRequest() throws ServletException, IOException, JDOMException{

		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTimeStart("2012-02-29T18:00:00.000Z").
									setTimeDuration("P1DT12H").									
									setVar("Pressure_reduced_to_MSL_msl").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
		
		Document doc = XmlUtil.getStringResponseAsDoc(response);
		List dateTags = XmlUtil.containsXPath("/grid/point/data[@name='date']", doc);
		assertEquals(7, dateTags.size());
		
		
	}	
	
	@Test
	public void testEndAndDurationTimeParamsRequest() throws ServletException, IOException, JDOMException{
		
		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTimeEnd("2012-03-02T18:00:00.000Z").
									setTimeDuration("P1DT6H").									
									setVar("Pressure_reduced_to_MSL_msl").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
		
		Document doc = XmlUtil.getStringResponseAsDoc(response);
		List dateTags = XmlUtil.containsXPath("/grid/point/data[@name='date']", doc);
		assertEquals(6, dateTags.size());		
		
	}
	/**
	 * 
	 * Variables with different time ranges.
	 * Expected is to get NaN for variable values that are out of its range and correct values 
	 * in any other case. 
	 * 
	 * @throws ServletException
	 * @throws IOException
	 * @throws JDOMException
	 */
	@Test
	public void testVariablesWithDifferentTimeRangesRequest() throws ServletException, IOException, JDOMException{
		
		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTimeStart("2012-02-29T12:00:00.000Z").
									setTimeDuration("P1DT6H").									
									setVar("Vertical_velocity_pressure_isobaric,Temperature_isobaric").
									setVertCoord("850").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
		
		Document doc = XmlUtil.getStringResponseAsDoc(response);
		assertEquals(6, XmlUtil.containsXPath("/grid/point/data[@name='date']", doc));		
		//Check values...
	}
	
	/*----------------------------------------------------------------
 		Bad request
	----------------------------------------------------------------*/
	@Test
	public void testTimeIllegalParamValueRequest() throws ServletException, IOException{
		
		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTimeStart("20xxx12-03-02T06:00:00Z"). //2012/14/29, 2012-14-2912:00:00 are not illegal??
									setTimeEnd("20xxx12-03-02T06:00:00Z").
									//setTimeDuration("P1DT6H").									
									setVar("Vertical_velocity_pressure_isobaric,Temperature_isobaric").
									setVertCoord("850").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertTrue(response.getContentAsString().contains("Illegal param") );
		assertEquals( "application/xml", response.getContentType() );		
	
		
	}
	
	/**
	 * Always get the closest time to the param ??
	 */
	@Test
	public void testOutOfRangeParamValueRequest() throws ServletException, IOException{
		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").									
									setTime("2012-01-01T00:00:00Z").																	
									setVar("Vertical_velocity_pressure_isobaric,Temperature_isobaric").
									setVertCoord("850").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
		
		
	}
	
	
}
