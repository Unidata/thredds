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
