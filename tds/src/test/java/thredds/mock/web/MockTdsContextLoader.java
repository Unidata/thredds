package thredds.mock.web;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import thredds.server.config.TdsContext;

/**
 * 
 * Context loader that initializes the TdsContext for the tests.
 * 
 * @author mhermida
 * 
 */
public class MockTdsContextLoader extends AbstractContextLoader {

	private TdsContentRootPath tdsContentRootPath;

	public ApplicationContext loadContext(MergedContextConfiguration mcc) throws Exception {
		return loadContext( mcc.getLocations() );
	}
	
	@Override
	public ApplicationContext loadContext(String... locations) throws Exception {
		final MockServletContext servletContext = new MockTdsServletContext();			
		final MockServletConfig servletConfig = new MockServletConfig(servletContext);
		final XmlWebApplicationContext webApplicationContext = new XmlWebApplicationContext();

		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,	webApplicationContext);
		webApplicationContext.setServletConfig(servletConfig);
		webApplicationContext.setConfigLocations(locations);
		webApplicationContext.refresh();

		TdsContext tdsContext = webApplicationContext.getBean(TdsContext.class);
		checkContentRootPath(webApplicationContext, tdsContext);

		webApplicationContext.registerShutdownHook();

		return webApplicationContext;
	}

	@Override
	protected String getResourceSuffix() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * If the test is annotated with TdsContentPath, override the contentRootPath with the provided path  
	 */
	private void checkContentRootPath(XmlWebApplicationContext webApplicationContext, TdsContext tdsContext){
		if(tdsContentRootPath != null )
			tdsContext.setContentRootPathProperty(tdsContentRootPath.path());
	}
	
	/**
	 * One of these two methods will get called before {@link #loadContext(String...)}.
	 * We just use this chance to extract the configuration.
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		extractConfiguration(clazz);		
		return super.generateDefaultLocations(clazz);
	}
	
	/**
	 * One of these two methods will get called before {@link #loadContext(String...)}.
	 * We just use this chance to extract the configuration.
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		extractConfiguration(clazz);
		return super.modifyLocations(clazz, locations);
	}	
	
	private void extractConfiguration(Class<?> clazz){
		tdsContentRootPath = AnnotationUtils.findAnnotation(clazz, TdsContentRootPath.class);
	}
}