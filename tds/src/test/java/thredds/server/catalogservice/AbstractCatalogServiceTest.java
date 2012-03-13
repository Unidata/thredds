package thredds.server.catalogservice;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.server.config.TdsContext;
import thredds.servlet.HtmlWriter;

@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractCatalogServiceTest {
	
	@Autowired
	protected TdsContext tdsContext;
	
	@Autowired
	protected HtmlWriter htmlWriter;	
	
	@Test
	public abstract void showCommandTest() throws Exception;
	
	@Test
	public abstract void subsetCommandTest() throws Exception;;
	
	@Test
	public abstract void validateCommandTest() throws Exception;;	
	

}
