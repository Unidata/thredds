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
package thredds.server.views;

import org.springframework.test.web.AbstractModelAndViewTests;
import org.springframework.web.servlet.View;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import thredds.catalog.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestInvCatalogXmlView extends AbstractModelAndViewTests
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TestInvCatalogXmlView.class );

  public void testUnknownEncoding()
  {
    StringBuilder catAsString = new StringBuilder()
            .append( "<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"\n" )
            .append( "         xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" )
            .append( "         version=\"1.0.1\">\n" )
            .append( "  <service name=\"ncDap\" serviceType=\"OPENDAP\" base=\"/thredds/dodsC/\" />\n" )
            .append( "  <dataset name=\"some data\" ID=\"SomeData\">\n" )
            .append( "     <metadata inherited=\"true\">\n" )
            .append( "       <serviceName>ncDap</serviceName>\n" )
            .append( "     </metadata>\n" )
            .append( "     <dataset name=\"data one\" id=\"data1\" urlPath=\"some/data/one.nc\" />")
            .append( "     <dataset name=\"data two\" id=\"data2\" urlPath=\"some/data/two.nc\" />")
            .append( "  </dataset>\n" )
            .append( "</catalog>" );
    String catUri = "Cat.TestInvCatalogXmlView.testNoDeclaredEncoding";

    URI catURI = null;
    try
    {
      catURI = new URI( catUri );
    }
    catch ( URISyntaxException e )
    {
      fail( "URISyntaxException: " + e.getMessage() );
      return;
    }

    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    InvCatalogImpl cat = fac.readXML( catAsString.toString(), catURI );

    Map model = Collections.singletonMap( "catalog", cat );

    View view = new InvCatalogXmlView();
    try
    {
      view.render( model, new MockHttpServletRequest(), new MockHttpServletResponse() );
    }
    catch ( Exception e )
    {
      e.printStackTrace();
    }


  }


}
