package thredds.server.cataloggen;

import static org.junit.Assert.fail;
import static org.springframework.test.web.ModelAndViewAssert.assertAndReturnModelAttributeOfType;
import static org.springframework.test.web.ModelAndViewAssert.assertViewName;

import javax.servlet.ServletContext;

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
import thredds.server.config.TdsContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml","/WEB-INF/catalogGen-servlet.xml" }, loader=MockTdsContextLoader.class)
public class CatGenControllerTests {

	@Autowired
	CatGenController catGenController;
	
	@Autowired 
	TdsContext tdsContext;
	
	@Autowired
	ServletContext servletContext;
	
	@Before
	public void setUp(){
		//re-init catGenController before running the tests!!!
		//Spring runs CatGenController init method when creates the bean and this is before the MockContextLoader runs all the TdsContext initialization.
		//So to get the CatGenController with the right settings it has to run its init method again!!!! 
		catGenController.init();	
		
	}
	
	@Test
	public void cataloggenRootRequestTest() throws Exception{
		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/cataloggen");
		request.setServletPath("/cataloggen");
		request.setPathInfo("/");
        MockHttpServletResponse response = new MockHttpServletResponse();		
		
        ModelAndView mv = catGenController.handleRequest(request, response);
        
        assertViewName(mv,"/thredds/server/cataloggen/catGenConfig");
        CatGenConfig cgc = assertAndReturnModelAttributeOfType(mv , "catGenConfig", CatGenConfig.class );        
        String catGenResultsDirName = assertAndReturnModelAttributeOfType(mv , "catGenResultsDirName", String.class);
        
		fail("No yet implemented");
	}
	
	@Test
	public void cataloggenRequestTest() throws Exception{
		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/cataloggen/catalogs/");
		request.setServletPath("/cataloggen");	
		request.setPathInfo("/catalogs/");
        MockHttpServletResponse response = new MockHttpServletResponse();		
		
        ModelAndView mv = catGenController.handleRequest(request, response);
        
        assertViewName(mv,"/thredds/server/cataloggen/catGenConfig");
        CatGenConfig cgc = assertAndReturnModelAttributeOfType(mv , "catGenConfig", CatGenConfig.class );        
        String catGenResultsDirName = assertAndReturnModelAttributeOfType(mv , "catGenResultsDirName", String.class);
        
		fail("No yet implemented");
	}	
	
}
