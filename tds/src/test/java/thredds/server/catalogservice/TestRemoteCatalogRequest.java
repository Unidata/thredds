package thredds.server.catalogservice;

import junit.framework.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.*;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestRemoteCatalogRequest extends TestCase
{

//  private CatalogServiceRequest me;

  public TestRemoteCatalogRequest( String name )
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
    req.setServletPath( "/remoteCatalogService" );
    req.setParameter( "catalog", "http://motherlode.ucar.edu:8080/thredds/catalog.xml" );
    req.setParameter( "verbose", "true" );
    req.setParameter( "command", "show" );
    req.setParameter( "htmlView", "true" );
    //req.setParameter( "dataset", "my/cool/dataset" );

    BindingResult bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );

    RemoteCatalogRequest rcr = (RemoteCatalogRequest) bindingResult.getTarget();

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
    assertEquals( bindingResult.getTarget(), rcr);
  }
}