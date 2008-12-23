package thredds.catalogservice;

import junit.framework.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.*;
import thredds.server.controller.CatalogServiceRequestDataBinder;
import thredds.server.controller.CatalogServiceRequest;
import thredds.server.controller.CatalogServiceRequestValidator;

import java.util.Map;
import java.util.List;
import java.util.Iterator;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestCatalogServiceRequest extends TestCase
{

//  private CatalogServiceRequest me;

  public TestCatalogServiceRequest( String name )
  {
    super( name );
  }

  public void testShowXml()
  {
    // explicit and implicit               
  }

  /**
   * Test ...
   */
  public void testOne()
  {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod( "GET" );
    req.setContextPath( "/thredds" );
    req.setServletPath( "/catalog" );
    req.setPathInfo( "/model/data/stuff/catalog.xml" );
    req.setParameter( "debug", "true" );
    req.setParameter( "command", "subset" );
    //req.setParameter( "htmlView", "false" );
    req.setParameter( "dataset", "true" );

    CatalogServiceRequest csr = new CatalogServiceRequest();
    CatalogServiceRequestDataBinder db = new CatalogServiceRequestDataBinder( csr, "request", true, false);
    //db.registerCustomEditor( boolean.class, "htmlView", new CatalogServiceRequestDataBinder.ViewEditor() );
    db.setAllowedFields( new String[] {"catalog", "debug", "command", "htmlView", "dataset"} );
    
    db.bind( req );

    BindingResult bindingResult = db.getBindingResult();
    ValidationUtils.invokeValidator( new CatalogServiceRequestValidator(), bindingResult.getTarget(), bindingResult  );

    if ( bindingResult.hasErrors() )
    {
      List<ObjectError> errors = bindingResult.getAllErrors();
      StringBuilder sb = new StringBuilder();
      for ( ObjectError error : errors )
      {
        sb.append( "Binding error [" ).append( error.toString() ).append( "]" );
      }
      fail( sb.toString());
    }
    assertEquals( bindingResult.getTarget(), csr);
  }
}
