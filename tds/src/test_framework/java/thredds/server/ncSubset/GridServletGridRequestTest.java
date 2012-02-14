package thredds.server.ncSubset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;


@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@TdsContentRootPath(path = "/share/testcatalogs/content")
public class GridServletGridRequestTest extends GridServletRequestTest{

	
	private String tempFileName ="test_response.nc"; 


	@Test
	public void testNCSSGridRequest() throws ServletException, IOException {

		String mockURI = "/thredds/ncss/grid/hioos/model/wav/swan/oahu/runs/SWAN_Oahu_Regional_Wave_Model_(500m)_RUN_2011-07-12T00:00:00.000Z";
		String mockQueryString = "var=salt,temp&north=21.9823&south=19.0184&east=-154.5193&west=-161.8306&time=2011-07-12T00:00:00.000Z";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", mockURI);
		request.setContextPath("/thredds");
		request.setPathInfo("/hioos/model/wav/swan/oahu/runs/SWAN_Oahu_Regional_Wave_Model_(500m)_RUN_2011-07-12T00:00:00.000Z");
		request.setQueryString(mockQueryString);
		
		request.setParameter("var", "temp,salt");
		request.setParameter("north", "21.9823");
		request.setParameter("south", "19.0184");
		request.setParameter("east", "-154.5193");
		request.setParameter("west", "-161.8306");
		request.setParameter("time", "2011-07-12T00:00:00.000Z");
		request.setParameter("time_start", "2011-07-12T00:00:00.000Z");
		request.setParameter("time_end", "2011-07-12T00:00:00.000Z");

		MockHttpServletResponse response = new MockHttpServletResponse();
		
		gridServlet.doGet(request, response);

		assertEquals(200, response.getStatus());		
		assertEquals("application/x-netcdf", response.getContentType());
		
		byte[] content = response.getContentAsByteArray();		
		NetcdfFile nf = null;
		
		//Open an in memory NetcdfFile and transform it into a NetcdfDataset  
		try{
			nf = NetcdfFile.openInMemory(tempFileName, content);
			GridDataset gds = new GridDataset( new NetcdfDataset(nf) );			
			double delta = 0.03;
			assertEquals(21.9823, gds.getBoundingBox().getLatMax()  , delta);
			assertEquals(19.0184, gds.getBoundingBox().getLatMin()  , delta);
			assertEquals(-154.5193, gds.getBoundingBox().getLonMax(), delta);
			assertEquals(-161.8306, gds.getBoundingBox().getLonMin(), delta);			
			assertEquals(2, gds.getGrids().size());
			assertNotNull(gds.findGridByName("temp"));
			assertNotNull(gds.findGridByName("salt"));
			
			
		}finally{
			nf.close();
		}		

		
	}	

}
