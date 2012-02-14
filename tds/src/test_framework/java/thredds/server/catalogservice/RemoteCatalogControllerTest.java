package thredds.server.catalogservice;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.ModelAndViewAssert.assertViewName;
import static org.springframework.test.web.ModelAndViewAssert.assertModelAttributeAvailable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;

@ContextConfiguration(locations = {	"/WEB-INF/applicationContext-tdsConfig.xml", "/WEB-INF/catalogService-servlet.xml" }, loader = MockTdsContextLoader.class)
@TdsContentRootPath(path = "/share/testcatalogs/content")
public class RemoteCatalogControllerTest extends AbstractCatalogServiceTest{

	//RemoteCatalogRequest parameters:
	private static final String parameterNameCatalog = "catalog";
	private static final String parameterNameCommand = "command";
	private static final String parameterNameDatasetId = "dataset";
	private static final String parameterNameVerbose = "verbose";
	private static final String parameterNameHtmlView = "htmlView";

	//RemoteCatalogRequest commands:
	private static final String cmdShow = "show";
	private static final String cmdSubset = "subset";
	private static final String cmdValidate = "validate";

	private String catUriString = null;
	private String command = null;
	private String datasetId = null;
	private String htmlView = null;
	private String verbose = null;

	private MockHttpServletRequest request = new MockHttpServletRequest();

	@Autowired
	private RemoteCatalogServiceController remoteCatalogController;

	@Before
	public void setUp() {

		request.setMethod("GET");
		request.setContextPath("/thredds");
		request.setServletPath("/remoteCatalogService");

	}

	@Test
	public void showCommandTest() throws Exception{

		// Testing against some reliable remote TDS
		catUriString = "http://motherlode.ucar.edu:8080/thredds/catalog.xml";
		request.setRequestURI(catUriString);

		// REQUEST WITH DEFAULT VALUES
		// setting up the request with default values:
		// command =null
		// datasetId=null
		// htmlView= null
		// verbose = null
		request.setParameter(parameterNameCatalog, catUriString);
		request.setParameter(parameterNameCommand, command);
		request.setParameter(parameterNameDatasetId, datasetId);
		request.setParameter(parameterNameHtmlView, htmlView);
		request.setParameter(parameterNameVerbose, verbose);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = remoteCatalogController.handleRequest(request, response);
		// Must be null
		assertNull(mv);
		//and we should have a nice response
		assertTrue(response.getStatus() == 200 );
		

	}

	@Test
	public void subsetCommandTest() throws Exception{

		// SUBSET REQUEST PROVIDING A datasetId
		// setting up the request with default values:
		// command =null
		// datasetId=FMRC/NCEP/SREF (in motherlode)
		// htmlView= null
		// verbose = null
		// command null and a providing a datasetId becomes in a subset command  
		catUriString = "http://motherlode.ucar.edu:8080/thredds/catalog/fmrc/NCEP/GFS/Alaska_191km/catalog.xml";
		request.setParameter(parameterNameCatalog, catUriString);
		request.setParameter(parameterNameCommand, command);
		request.setParameter(parameterNameDatasetId, "fmrc/NCEP/GFS/Alaska_191km");
		request.setParameter(parameterNameHtmlView, htmlView);
		request.setParameter(parameterNameVerbose, verbose);
		
		MockHttpServletResponse response = new MockHttpServletResponse();

		ModelAndView mv  = remoteCatalogController.handleRequest(request, response);
		// Must be null
		assertNull(mv);
		// and we should have a nice response		
		assertTrue( response.getStatus() == 200 );
	}
	
	
	@Test
	public void validateCommandTest() throws Exception {

		// VALIDATE REQUEST 
		// command =validate
		// datasetId= null
		// htmlView= null
		// verbose = null 
		catUriString = "http://motherlode.ucar.edu:8080/thredds/catalog/fmrc/NCEP/GFS/Alaska_191km/catalog.xml";		
		request.setParameter(parameterNameCatalog, catUriString);
		request.setParameter(parameterNameCommand, cmdValidate);
		request.setParameter(parameterNameDatasetId, datasetId);
		request.setParameter(parameterNameHtmlView, htmlView);
		request.setParameter(parameterNameVerbose, verbose);		
		MockHttpServletResponse response = new MockHttpServletResponse();

		ModelAndView mv = remoteCatalogController.handleRequest(request,response);
		
		assertViewName(mv, "/thredds/server/catalogservice/validationMessage");		
		assertModelAttributeAvailable(mv, "catalogUrl");
		assertModelAttributeAvailable(mv, "message");
	}
	
	@Test
	public void validateFormTest() throws Exception {

		request.setRequestURI("/thredds/remoteCatalogValidation.html");
		request.setServletPath("/remoteCatalogValidation.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		ModelAndView mv = remoteCatalogController.handleRequest(request, response);
		assertViewName(mv, "/thredds/server/catalogservice/validationForm");
		
	}	

}
