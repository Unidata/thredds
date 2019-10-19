/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.mock.web;

import javax.servlet.RequestDispatcher;

import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockRequestDispatcher;
import org.springframework.mock.web.MockServletContext;

import java.io.IOException;

/**
 * 
 * Mock servlet context that overrides the getNamedDispatcher returning a MockRequestDispatcher
 * TdsContext calls getNamedDispatcher to get the default and the jsp RequestDispatchers from
 * the Tomcat's StandardContext
 *
 *
 * 
 * @author mhermida
 *
 */

/*
http://docs.spring.io/spring/docs/3.2.x/javadoc-api/org/springframework/mock/web/MockServletContext.html

A common setup is to point your JVM working directory to the root of your web application directory, in combination with filesystem-based
resource loading. This allows to load the context files as used in the web application, with relative paths getting interpreted correctly.
Such a setup will work with both FileSystemXmlApplicationContext (which will load straight from the filesystem) and XmlWebApplicationContext
with an underlying MockServletContext (as long as the MockServletContext has been configured with a FileSystemResourceLoader).
 */
public class MockTdsServletContext extends MockServletContext {
	
	public MockTdsServletContext(){
    super();
		// super("src/main/webapp");   // default resourceBasePath - HACK i think

    String current = null;
    try {
      current = new java.io.File( "." ).getCanonicalPath();
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
	}
	
	public MockTdsServletContext(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}
	
	public MockTdsServletContext(String resourceBasePath){
		super(resourceBasePath);
	}
	
	public MockTdsServletContext(String resourceBasePath, ResourceLoader resourceLoader) {
		super(resourceBasePath, resourceLoader);
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.mock.web.MockServletContext#getNamedDispatcher(java.lang.String)
	 */
	@Override
	public RequestDispatcher getNamedDispatcher(String path){
		
		return new MockRequestDispatcher(path);
	}

  public javax.servlet.descriptor.JspConfigDescriptor getJspConfigDescriptor()
  {
      throw new UnsupportedOperationException();
  }


	/**
 	 * Build a full resource location for the given path,
 	 * prepending the resource base path of this MockServletContext.
 	 * @param path the path as specified
 	 * @return the full resource path
 	 *
 	protected String getResourceLocation(String path) {
 		if (!path.startsWith("/")) {
 			path = "/" + path;
 		}
 		returnu "src/main/webapp" + path;
 	} */

	public String getVirtualServerName() {
		return null;
	}
}
