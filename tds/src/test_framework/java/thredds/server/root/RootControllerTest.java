package thredds.server.root;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.ServletException;

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
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml","/WEB-INF/root-servlet.xml" },loader=MockTdsContextLoader.class)
public class RootControllerTest {
	
	
	@Autowired
	private TdsContext tdsContext;
	
  	
	@Autowired
	private RootController rootController;
		
	@Before
	public void setUp() throws Exception {
	
		rootController.setTdsContext(tdsContext);
				
	}
	
	@Test
	public void testHandleRequest() throws Exception{
							
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog.html");
		request.setContextPath("/thredds");
		request.setPathInfo("catalog.html");        
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv = rootController.handleRequest(request, response);
	        
		fail("Not yet implemented");
	}


}
