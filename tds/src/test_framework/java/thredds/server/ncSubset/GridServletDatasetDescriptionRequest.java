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
public class GridServletDatasetDescriptionRequest extends GridServletRequestTest{
	
	@Test
	public void testXmlDatasetDescriptionReuest() throws ServletException, IOException{
		
		NcssMockHttpServletRequest.NcssMockHttpServletRequestBuilder builder = NcssMockHttpServletRequest.createBuilder();  		
		NcssMockHttpServletRequest ncssRequest = builder.setRequestMethod("GET").
									setRequestURI("/thredds/ncss/grid/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1/dataset.xml").
									setContextPath("/thredds").
									setPathInfo("/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1/dataset.xml").build();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		gridServlet.doGet(ncssRequest.getRequest(), response);
		assertEquals(200, response.getStatus());		
		
	}

}
