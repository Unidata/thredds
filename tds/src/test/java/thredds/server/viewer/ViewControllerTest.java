package thredds.server.viewer;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import thredds.mock.web.MockTdsContextLoader;
import ucar.unidata.util.test.category.NeedsContentRoot;

import java.io.IOException;

import static org.junit.Assert.assertEquals;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/WEB-INF/applicationContext-tdsConfig.xml",  loader = MockTdsContextLoader.class)
@Category(NeedsContentRoot.class)
public class ViewControllerTest{
	
	@Autowired
	private ViewerController viewerController;
	
		
	@Before
	public void setUp(){
														
				
	}
	
	@Test
	public void testLaunchViewerIDVRequest() throws IOException{
		
		ViewerRequestParamsBean params = new ViewerRequestParamsBean(); 		
		params.setUrl("http://localhost:9080/thredds/dodsC/gribCollection/GFS_CONUS_80km/files/GFS_CONUS_80km_20120227_0000.grib1");
		params.setViewer("idv");		
		BindingResult result = new BeanPropertyBindingResult(params, "params");
		MockHttpServletResponse res = new MockHttpServletResponse();
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRequestURI("/thredds/view/idv.jnlp?url=http://localhost:9080/thredds/dodsC/gribCollection/GFS_CONUS_80km/files/GFS_CONUS_80km_20120227_0000.grib1");
		viewerController.launchViewer(params, result, res, req);		
		assertEquals(200, res.getStatus() );
		assertEquals("application/x-java-jnlp-file", res.getContentType() );
	}
	
	@Test
	public void testLaunchViewerToolsUIVRequest() throws IOException{
		
		ViewerRequestParamsBean params = new ViewerRequestParamsBean(); 		
		params.setViewer("ToolsUI");
		params.setCatalog("http://localhost:9080/thredds/catalog/gribCollection/GFS_CONUS_80km/files/catalog.xml");
		params.setDataset("gribCollection/GFS_CONUS_80km/files/GFS_CONUS_80km_20120227_0000.grib1");
		BindingResult result = new BeanPropertyBindingResult(params, "params");
		MockHttpServletResponse res = new MockHttpServletResponse();
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRequestURI("/thredds/view/ToolsUI.jnlp?catalog=http://localhost:9080/thredds/catalog/gribCollection/GFS_CONUS_80km/files/catalog.xml&dataset=ncss_tests/files/GFS_CONUS_80km_20120227_0000.grib1");
		viewerController.launchViewer(params, result, res, req);		
		assertEquals(200, res.getStatus() );
		assertEquals("application/x-java-jnlp-file", res.getContentType() );
	}
	
	@Test
	public void testBadRequest() throws IOException{
	
		ViewerRequestParamsBean params = new ViewerRequestParamsBean();
		params.setViewer("BadViewer");
		params.setCatalog("wrong_catalog.xml");//
		params.setDataset("wrong_dataset.nc"); //		
		BindingResult result = new BeanPropertyBindingResult(params, "params");
		MockHttpServletResponse res = new MockHttpServletResponse();
		MockHttpServletRequest req = new MockHttpServletRequest();		
		req.setRequestURI("/thredds/view/ToolsUI.jnlp?catalog=http://localhost:9080/thredds/catalog/gribCollection/GFS_CONUS_80km/files/catalog.xml&dataset=ncss_tests/files/GFS_CONUS_80km_20120227_0000.grib1");
		viewerController.launchViewer(params, result, res, req);
		assertEquals(404, res.getStatus() );
	} 

}
