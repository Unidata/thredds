package thredds.server.ncSubset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;
import thredds.mock.web.ncss.NcssMockHttpServletRequest;

@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@TdsContentRootPath(path = "/share/testcatalogs/content")
public class GridServletBadRequestTest extends GridServletRequestTest{

	@Test
	public void testPointOutOfGrid() throws ServletException, IOException{		
		
		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1").
									setPoint("true").
									setTimeStart("2012-03-02T06:00:00Z"). //2012/14/29, 2012-14-2912:00:00 are not illegal??
									setTimeEnd("2012-03-02T06:00:00Z").							
									setVar("Vertical_velocity_pressure_isobaric,Temperature_isobaric").
									setVertCoord("850").
									setLatitude("0").
									setLongitude("0").
									setAccept("xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertTrue(response.getContentAsString().contains("Requested Lat/Lon Point (+.0N .0E) is not contained in the Data") );
		assertEquals(200, response.getStatus());
		
				
		
	}
}
