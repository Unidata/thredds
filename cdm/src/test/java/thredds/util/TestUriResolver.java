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

//  private UriResolver me;

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
      uri = new URI( "http://motherlode.ucar.edu:8080/thredds/catalog.xml");
    }
    catch ( URISyntaxException e )
    {
      fail();
    }
    UriResolver uriResolver = UriResolver.newDefaultUriResolver();
    try
    {
      uriResolver.getInputStream( uri );
    }
    catch ( IOException e )
    {
      fail();
    }
  }
}
