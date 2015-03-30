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
		
		// Initialize the File Cache 
		//initFileCache();
		
		final XmlWebApplicationContext webApplicationContext = new XmlWebApplicationContext();

		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,	webApplicationContext);
		webApplicationContext.setServletConfig(servletConfig);
		webApplicationContext.setConfigLocations(locations);
		
//		webApplicationContext.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor(){
//			@Override
//			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory){
//			
//				//servletContext and servletConfig can be autowired by the servlet tests (OpendapServlet, fileServlet...)							
//				beanFactory.registerResolvableDependency(MockServletContext.class, servletContext );
//				beanFactory.registerResolvableDependency(MockServletConfig.class, servletConfig );
//				
//				
//			}
//		});		

				
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


	//No longer needed?
//	private void initFileCache() {
//		// Mock file cache...needed for running init method in thredds.server.views.FileView
//		// CdmInit will set again the file cache through the its init method
//		int min = ThreddsConfig.getInt("HTTPFileCache.minFiles", 10);
//		int max = ThreddsConfig.getInt("HTTPFileCache.maxFiles", 20);
//		int secs = ThreddsConfig.getSeconds("HTTPFileCache.scour", 17 * 60);
//		if (max > 0) {
//			ServletUtil.setFileCache(new FileCache("HTTP File Cache", min, max,	-1, secs));
//
//		}
//	}
	
	/**
	 * If the test is annotated with TdsContentPath, override the contentRootPath with the provided path  
	 */
	private void checkContentRootPath(XmlWebApplicationContext webApplicationContext, TdsContext tdsContext){
		if(tdsContentRootPath != null )
			tdsContext.setContentRootPath(tdsContentRootPath.path());
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