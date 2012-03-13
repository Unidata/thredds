package thredds.server.ncSubset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import javax.servlet.ServletException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;
import thredds.mock.web.ncss.NcssMockHttpServletRequest;
import thredds.test.util.xml.XmlUtil;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;

/*
 * Variable names:
 * 
 *  Pressure_reduced_to_MSL_msl            -- VAR_7-0-2-2_L102
 *  Relative_humidity_height_above_ground  -- VAR_7-0-2-52_L105
 *  Temperature_isobaric                   -- VAR_7-0-2-11_L100  
 * 
 */
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@TdsContentRootPath(path = "/share/testcatalogs/content")
public class GridServletPointSingleVariableRequestTest extends GridServletRequestTest{
	
	
	/*----------------------------------------------------------------
	  Requests on a var without vertical levels;
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
									setVar("VAR_7-0-2-2_L102").
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
		Element varTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-2_L102' and @units='Pa']", doc).get(0);
		assertFalse( "NaN".equals(varTag.getText()));
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
									setVar("VAR_7-0-2-2_L102").
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
	public void testNCSSPointNetCDFNoVertlLevelVarRequest() throws ServletException, IOException{

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
									setVar("VAR_7-0-2-2_L102").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("netcdf").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/x-netcdf", response.getContentType() );
		//Check csv content
	}	
	
	/*----------------------------------------------------------------
	  Requests on a var with vertical dimension and just one level;
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
									//setVertCoord("0").
									setVar("VAR_7-0-2-52_L105").
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
		Element varTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-52_L105' and @units='%']", doc).get(0);
		assertFalse( "NaN".equals(varTag.getText()));
		Element latTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lat' and @units='degrees_north']", doc).get(0);
		assertFalse( "0.0".equals(latTag.getText()) );
		Element lonTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lon' and @units='degrees_east' ]", doc).get(0);
		assertFalse( "0.0".equals(lonTag.getText()) );
		Element vertCoordTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='vertCoord' and @units='m' ]", doc).get(0);
		assertFalse( "0.0".equals(vertCoordTag.getText()) );		
		
	}
	
	@Test
	public void testNCSSPointCSVOneVertlLevelVarRequest() throws ServletException, IOException{
		
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
									setVar("VAR_7-0-2-52_L105").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("csv").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();

		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());
		assertEquals( "text/csv", response.getContentType() ); //Fail!!		
	}	
	
	@Test
	public void testNCSSPointNetCDFOneVertlLevelVarRequest() throws ServletException, IOException{
		
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
									setVar("VAR_7-0-2-52_L105").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("netcdf").build();

		MockHttpServletResponse response = new MockHttpServletResponse();

		gridServlet.doGet(ncssRequest.getRequest() , response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/x-netcdf", response.getContentType() );
		
		byte[] content = response.getContentAsByteArray();		
		NetcdfFile nf = null;
		NetcdfDataset nfd = null;
		
		//Open an in memory NetcdfFile and transform it into a NetcdfDataset  
		try{
			nf = NetcdfFile.openInMemory("in_memory_file", content);
			nfd =new NetcdfDataset(nf);
			Formatter errlog = new Formatter();
			FeatureDataset fd = FeatureDatasetFactoryManager.wrap(FeatureType.ANY_POINT, nfd , null, errlog);
			assertNotNull(fd);
			
		}finally{
			nf.close();
			nfd.close();
		}		
		
	}	
	
	/*----------------------------------------------------------------
	  Requests on a var with vertical dimension and multiple levels;
	----------------------------------------------------------------*/	
	@Test
	public void testNCSSPointXMLMultipleVertlLevelVarRequest() throws ServletException, IOException, JDOMException {

		
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
									setVar("VAR_7-0-2-11_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("xml").build();

		MockHttpServletResponse response = new MockHttpServletResponse();

		gridServlet.doGet(ncssRequest.getRequest() , response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
				
		Document doc = XmlUtil.getStringResponseAsDoc(response);
		assertTrue( XmlUtil.containsXPath("/grid", doc).size() > 0 );
		assertTrue( XmlUtil.containsXPath("/grid/point", doc).size() > 0 );
		Element dateTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='date']", doc).get(0);
		assertEquals( "2012-02-29T12:00:00.000Z",  dateTag.getText());
		Element varTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-11_L100' and @units='K']", doc).get(0);
		assertFalse( "NaN".equals(varTag.getText()));
		Element latTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lat' and @units='degrees_north']", doc).get(0);
		assertFalse( "0.0".equals(latTag.getText()) );
		Element lonTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='lon' and @units='degrees_east' ]", doc).get(0);
		assertFalse( "0.0".equals(lonTag.getText()) );
		List vertCoordTags = XmlUtil.containsXPath("/grid/point/data[@name='vertCoord' and @units='hPa' ]", doc);
		assertTrue( vertCoordTags.size()== 29 );
		Element vertCoordTag = (Element)vertCoordTags.get(0);
		assertFalse( "0.0".equals(vertCoordTag.getText()) );
		
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
									setVar("VAR_7-0-2-11_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("csv").build();

		MockHttpServletResponse response = new MockHttpServletResponse();

		gridServlet.doGet(ncssRequest.getRequest() , response);
		assertEquals(200, response.getStatus());
		assertEquals( "text/csv", response.getContentType() );
		//check csv content...
		
	}
	
	@Test
	public void testNCSSPointNetCDFMultipleVertlLevelVarRequest() throws ServletException, IOException{
		
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
									setVar("VAR_7-0-2-11_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setAccept("netcdf").build();

		MockHttpServletResponse response = new MockHttpServletResponse();

		gridServlet.doGet(ncssRequest.getRequest() , response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/x-netcdf", response.getContentType() );
		//check binary content...		
	}
	
	/*----------------------------------------------------------------
	  Bad requests
	----------------------------------------------------------------*/
	/**
	 * 
	 * Test Request with vertical values out of range, we should get a missing value  
	 * according to the NCSS doc: http://www.unidata.ucar.edu/projects/THREDDS/tech/interfaceSpec/NetcdfSubsetService.html
	 * 
	 * @throws ServletException
	 * @throws IOException
	 * @throws JDOMException
	 */
	@Test
	public void testVerticalCoordOutOfRangeRequest() throws ServletException, IOException, JDOMException {
		
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
									setVar("VAR_7-0-2-11_L100").
									setLatitude("40.023").
									setLongitude("-105.268").
									setVertCoord("2000").
									setAccept("xml").build();

		MockHttpServletResponse response = new MockHttpServletResponse();

		gridServlet.doGet(ncssRequest.getRequest() , response);
		assertEquals(200, response.getStatus());
		assertEquals( "application/xml", response.getContentType() );
		
		Document doc = XmlUtil.getStringResponseAsDoc(response);
		Element varTag = (Element)XmlUtil.containsXPath("/grid/point/data[@name='VAR_7-0-2-11_L100' and @units='K']", doc).get(0);
		assertTrue( "NaN".equals(varTag.getText()));		
		
	}
		
}
