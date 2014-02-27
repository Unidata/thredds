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
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"}, loader=MockTdsContextLoader.class)
public class OpendapServletTest {

	@Autowired
	private MockServletConfig servletConfig;
	
	private OpendapServlet opendapServlet;
	private String path =  "/gribCollection/GFS_CONUS_80km/GFS_CONUS_80km_20120229_1200.grib1/GC";
  private String uri =  "/thredds/dodsC" + path;

	@Before
	public void setUp() throws Exception {
		opendapServlet =new OpendapServlet();
		opendapServlet.init(servletConfig);
		opendapServlet.init();
	}
	
	@Test
	public void asciiDataRequestTest() throws UnsupportedEncodingException{
		
		String mockURI = uri + ".ascii";
		String mockQueryString ="Temperature_height_above_ground[0:1:0][0:1:0][41][31]";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", mockURI);		
		request.setContextPath("/thredds");
		request.setQueryString(mockQueryString);
		request.setPathInfo( path + ".ascii");
		MockHttpServletResponse response = new MockHttpServletResponse();
		opendapServlet.doGet(request, response);
    assertEquals(200, response.getStatus());

		String strResponse = response.getContentAsString();
		System.out.printf("%s%n",strResponse );
	}
	
	
	@Test
	public void dodsDataRequestTest() throws IOException{
		
    String mockURI = uri + ".dods";
    String mockQueryString ="Temperature_height_above_ground[0:1:0][0:1:0][41][31]";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", mockURI);
		request.setContextPath("/thredds");
		request.setQueryString(mockQueryString);
		request.setPathInfo(path + ".dods");
		MockHttpServletResponse response = new MockHttpServletResponse();
		opendapServlet.doGet(request, response);
    assertEquals(200, response.getStatus());

		assertEquals("application/octet-stream" , response.getContentType());

    String strResponse = response.getContentAsString();
    System.out.printf("%s%n",strResponse );
	}
	
}
