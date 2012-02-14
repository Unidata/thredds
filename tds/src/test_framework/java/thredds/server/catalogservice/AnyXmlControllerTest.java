package thredds.server.catalogservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.ModelAndViewAssert.assertAndReturnModelAttributeOfType;
import static org.springframework.test.web.ModelAndViewAssert.assertViewName;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.ModelAndView;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;


@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml","/WEB-INF/catalogService-servlet.xml" }, loader=MockTdsContextLoader.class)
@TdsContentRootPath(path = "/share/testcatalogs/content")
public class AnyXmlControllerTest extends AbstractCatalogServiceTest{
	
	@Autowired
	private LocalCatalogServiceController anyXmlController;
	
	@Before
	public void setUp(){		
		anyXmlController.setTdsContext(tdsContext);
		anyXmlController.setHtmlWriter(htmlWriter);
	}
	
	public void showCommandTest() throws Exception{
		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/hioos/model/wav/swan/oahu/catalog.xml");
		request.setServletPath("/hioos/model/wav/swan/oahu/catalog.xml");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =anyXmlController.handleRequest(request, response);        
        
        assertViewName(mv, "threddsInvCatXmlView");
        assertAndReturnModelAttributeOfType(mv,"catalog" , thredds.catalog.InvCatalogImpl.class);
        		
	}
	
	public void subsetCommandTest() throws Exception{
		
		// SUBSET COMMAND REQUEST 
		// setting up the request with default values:
		// command =null
		// datasetId=NCOF/POLCOMS/IRISH_SEA 
		// htmlView= null
		// verbose = null
		// command null and a providing a datasetId becomes in a subset command  		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/hioos/model/wav/swan/oahu/catalog.xml");
		request.setServletPath("/hioos/model/wav/swan/oahu/catalog.xml");
		//request.setParameter("command", "subset");
		request.setParameter("dataset", "swan_oahu/SWAN_Oahu_Regional_Wave_Model_(500m)_best.ncd");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =anyXmlController.handleRequest(request, response);        
        assertViewName(mv, "threddsInvCatXmlView");
        assertAndReturnModelAttributeOfType(mv,"catalog" , thredds.catalog.InvCatalogImpl.class);		
		
	}

	public void validateCommandTest() throws Exception{
		
		// VALIDATE REQUEST --> invalid command!!!!
		// command =validate
		// datasetId= null
		// htmlView= null
		// verbose = null 		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/NCOF/POLCOMS/IRISH_SEA/catalog.xml");
		request.setServletPath("/NCOF/POLCOMS/IRISH_SEA/catalog.xml");
		request.setParameter("datasetId", "FMRC/NCEP/SREF");		
		request.setParameter("command", "validate");
		
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =anyXmlController.handleRequest(request, response);        
        assertNull( mv );
        assertEquals(400, response.getStatus() );
        assertEquals("Bad request: The \"command\" field may not be VALIDATE." , response.getErrorMessage());		
		
	}	

}
