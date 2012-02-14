package thredds.servlet;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.web.MockTdsContextLoader;
import thredds.server.config.TdsContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"}, loader=MockTdsContextLoader.class)
public class DataRootHandlerTest {
	
	@Autowired
	private TdsContext tdsContext;
	
	@Autowired
	private DataRootHandler tdsDRH;
	
	@Autowired
	private MockServletContext servletContext; 
	
	@Before
	public void setUp() throws Exception {		
		
		tdsContext.init(servletContext);
	}	
	
	@Test
	public void testSomething(){
		
		tdsDRH.registerConfigListener( new RestrictedAccessConfigListener() );
		tdsDRH.init();
		
		
		fail("Not yet implemented");
	}

}
