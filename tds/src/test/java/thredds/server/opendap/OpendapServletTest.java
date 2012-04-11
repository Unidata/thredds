package thredds.server.opendap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.web.MockTdsContextLoader;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dods.DODSNetcdfFile;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"},loader=MockTdsContextLoader.class)
public class OpendapServletTest {

	@Autowired
	private MockServletConfig servletConfig;
	
	private OpendapServlet opendapServlet;
	
	
	@Before
	public void setUp() throws Exception {
		
		opendapServlet =new OpendapServlet();
		opendapServlet.init(servletConfig);
		opendapServlet.init();
		
	}
	
	@Test
	public void asciiDataRequestTest() throws UnsupportedEncodingException{
		
		String mockURI = "/thredds/dodsC/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1.ascii";
		String mockQueryString ="Temperature_height_above_ground[0:1:0][0:1:0][41][31]";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", mockURI);		
		request.setContextPath("/thredds");
		request.setQueryString(mockQueryString);
		request.setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1.ascii"); 
		MockHttpServletResponse response = new MockHttpServletResponse();

		opendapServlet.doGet(request, response);
		
		String strResponse = response.getContentAsString();
		
		fail("Not yet implemented");
		
	}
	
	
	@Test
	public void dodsDataRequestTest() throws IOException{
		
		String mockURI = "/thredds/dodsC/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1.ascii";
		String mockQueryString ="Temperature_height_above_ground[0:1:0][0:1:0][41][31]";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", mockURI);		
		request.setContextPath("/thredds");
		request.setQueryString(mockQueryString);
		request.setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1.ascii"); 
		MockHttpServletResponse response = new MockHttpServletResponse();

		opendapServlet.doGet(request, response);
		assertEquals("application/octet-stream" , response.getContentType());

		byte[] content = response.getContentAsByteArray();
		
		NetcdfFile nf = DODSNetcdfFile.openInMemory("test_data.dods", content );

		
		fail("Not yet implemented");
	}	
	
}
