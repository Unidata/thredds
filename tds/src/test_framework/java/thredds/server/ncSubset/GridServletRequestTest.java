package thredds.server.ncSubset;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.web.MockTdsContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
public abstract class GridServletRequestTest {
	
	@Autowired
	protected ServletConfig servletConfig;

	protected GridServlet gridServlet;
	
	@Before
	public void setUp() throws ServletException {

		gridServlet = new GridServlet();
		gridServlet.init(servletConfig);
		
	}	

}
