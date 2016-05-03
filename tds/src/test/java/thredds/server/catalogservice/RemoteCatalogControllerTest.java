package thredds.server.catalogservice;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.ModelAndView;
import thredds.mock.web.MockTdsContextLoader;
import ucar.unidata.util.test.category.NeedsContentRoot;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.ModelAndViewAssert.assertModelAttributeAvailable;
import static org.springframework.test.web.ModelAndViewAssert.assertViewName;

@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsContentRoot.class)
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
	@Category(NeedsExternalResource.class)
	public void showCommandTest() throws Exception{
		// Testing against some reliable remote TDS
		catUriString = "http://"+ TestDir.threddsTestServer+"/thredds/catalog.xml";
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
		ModelAndView mv = remoteCatalogController.handleAll(request, response);
		// Must be null
		assertNull(mv);
		//and we should have a nice response
		assertTrue(response.getStatus() == 200 );
		

	}

	// http://thredds.ucar.edu/thredds/catalog/grib/NCEP/NAM/CONUS_80km/catalog.html?dataset=grib/NCEP/NAM/CONUS_80km/best
	@Test
	@Category(NeedsExternalResource.class)
	public void subsetCommandTest() throws Exception{
		// SUBSET REQUEST PROVIDING A datasetId
		// setting up the request with default values:
		// command =null
		// datasetId=FMRC/NCEP/SREF (in motherlode)
		// htmlView= null
		// verbose = null
		// command null and a providing a datasetId becomes in a subset command  
		catUriString = "http://"+TestDir.threddsTestServer+"/thredds/catalog/grib/NCEP/NAM/CONUS_80km/catalog.xml";
		request.setParameter(parameterNameCatalog, catUriString);
		request.setParameter(parameterNameCommand, command);
		request.setParameter(parameterNameDatasetId, "grib/NCEP/NAM/CONUS_80km/Best");
		request.setParameter(parameterNameHtmlView, htmlView);
		request.setParameter(parameterNameVerbose, verbose);
		
		MockHttpServletResponse response = new MockHttpServletResponse();

		ModelAndView mv  = remoteCatalogController.handleAll(request, response);
		// Must be null
		assertNull(mv);
		// and we should have a nice response		
		assertTrue( response.getStatus() == 200 );
	}

	@Test
	@Category(NeedsExternalResource.class)
	public void validateCommandTest() throws Exception {
		// VALIDATE REQUEST 
		// command =validate
		// datasetId= null
		// htmlView= null
		// verbose = null 
    catUriString = "http://"+TestDir.threddsTestServer+"/thredds/catalog/grib/NCEP/NAM/CONUS_80km/catalog.xml";
		request.setParameter(parameterNameCatalog, catUriString);
		request.setParameter(parameterNameCommand, cmdValidate);
		request.setParameter(parameterNameDatasetId, datasetId);
		request.setParameter(parameterNameHtmlView, htmlView);
		request.setParameter(parameterNameVerbose, verbose);		
		MockHttpServletResponse response = new MockHttpServletResponse();

		ModelAndView mv = remoteCatalogController.handleAll(request,response);
		
		assertViewName(mv, "/thredds/server/catalogservice/validationMessage");		
		assertModelAttributeAvailable(mv, "catalogUrl");
		assertModelAttributeAvailable(mv, "message");
	}
	
	@Test
	public void validateFormTest() throws Exception {

		request.setRequestURI("/thredds/remoteCatalogValidation.html");
		request.setServletPath("/remoteCatalogValidation.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		ModelAndView mv = remoteCatalogController.handleAll(request, response);
		assertViewName(mv, "/thredds/server/catalogservice/validationForm");
		
	}	

}
