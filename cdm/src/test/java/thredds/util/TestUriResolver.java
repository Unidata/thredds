package thredds.util;

import junit.framework.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestUriResolver extends TestCase
{

//  private HttpUriResolver me;

  public TestUriResolver( String name )
  {
    super( name );
  }

  /**
   * Test ...
   */
  public void testOne()
  {
    URI uri = null;
    try
    {
      uri = new URI( "http://newmotherlode.ucar.edu:8080/thredds/catalog/nexrad/composite/gini/ntp/4km/20080731/catalog.xml");
    }
    catch ( URISyntaxException e )
    {
      fail();
    }
    HttpUriResolver httpUriResolver = HttpUriResolver.newDefaultUriResolver();
    String resp = null;
    try
    {
      resp = httpUriResolver.getResponseBodyAsString( uri );
    }
    catch ( IOException e )
    {
      fail( e.getMessage());
    }
    assertTrue( "URI resolved to null string.", resp != null );
  }
}
