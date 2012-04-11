package thredds.server.catalogservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.ModelAndView;

import thredds.mock.web.MockTdsContextLoader;


@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml","/WEB-INF/catalogService-servlet.xml" }, loader=MockTdsContextLoader.class)
public class AnyHtmlControllerTest extends AbstractCatalogServiceTest{
	

	
	
	@Autowired
	private LocalCatalogServiceController anyHtmlController;
	
	
	
	@Before
	public void setUp(){		
		anyHtmlController.setTdsContext(tdsContext);
		anyHtmlController.setHtmlWriter(htmlWriter);
	}
	
	public void showCommandTest() throws Exception{
		
		// SHOW COMMAND REQUEST WITH DEFAULT VALUES
		// setting up the request with default values:
		// command =null
		// datasetId=null
		// htmlView= null
		// verbose = null		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/NCOF/POLCOMS/IRISH_SEA/catalog.html");
		request.setServletPath("/NCOF/POLCOMS/IRISH_SEA/catalog.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =anyHtmlController.handleRequest(request, response);        
        assertNull( mv );        
        assertEquals(200, response.getStatus() ); 
	}	


	public void subsetCommandTest() throws Exception{
		
		// SUBSET COMMAND REQUEST 
		// setting up the request with default values:
		// command =null
		// datasetId=NCOF/POLCOMS/IRISH_SEA 
		// htmlView= null
		// verbose = null
		// command null and a providing a datasetId becomes in a subset command  		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/NCOF/POLCOMS/IRISH_SEA/catalog.html");
		request.setServletPath("/NCOF/POLCOMS/IRISH_SEA/catalog.html");
		//request.setParameter("command", "subset");
		request.setParameter("dataset", "NCOF/POLCOMS/IRISH_SEA");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =anyHtmlController.handleRequest(request, response);        
        assertNull( mv );        
        assertEquals(200, response.getStatus() ); 
	}	
	
	public void validateCommandTest() throws Exception{
		
		// VALIDATE REQUEST --> invalid command!!!!
		// command =validate
		// datasetId= null
		// htmlView= null
		// verbose = null 		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/NCOF/POLCOMS/IRISH_SEA/catalog.html");
		request.setServletPath("/NCOF/POLCOMS/IRISH_SEA/catalog.html");
		request.setParameter("datasetId", "FMRC/NCEP/SREF");		
		request.setParameter("command", "validate");
		
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =anyHtmlController.handleRequest(request, response);        
        assertNull( mv );
        assertEquals(400, response.getStatus() );
        assertEquals("Bad request: The \"command\" field may not be VALIDATE." , response.getErrorMessage());
		

	}	
}
