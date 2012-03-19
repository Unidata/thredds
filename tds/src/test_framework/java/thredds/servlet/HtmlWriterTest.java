package thredds.servlet;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"}, loader=MockTdsContextLoader.class)
@TdsContentRootPath(path = "/share/testcatalogs/content")
public class HtmlWriterTest {

	
	@Autowired
	HtmlWriter htmlWriter;
	
	
	@Test
	public void test() {
		
		fail("Not yet implemented");
	}

}
