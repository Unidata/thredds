/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
 		return "src/main/webapp" + path;
 	} */
}
