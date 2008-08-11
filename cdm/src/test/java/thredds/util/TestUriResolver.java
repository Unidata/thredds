package thredds.util;

import junit.framework.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
      uri = new URI( "http://motherlode.ucar.edu:8080/thredds/catalog.xml");
    }
    catch ( URISyntaxException e )
    {
      fail();
    }
    HttpUriResolver httpUriResolver = HttpUriResolverFactory.getDefaultHttpUriResolver( uri );
    InputStream resp = null;
    try
    {
      httpUriResolver.makeRequest();
      resp = httpUriResolver.getResponseBodyAsInputStream();
      if ( -1 == resp.read())
        fail( "");
    }
    catch ( IOException e )
    {
      fail( e.getMessage());
    }
    assertTrue( "URI resolved to null string.", resp != null );
  }

  public static void main( String[] args )
  {
    String catUriString = "http://newmotherlode.ucar.edu:8080/thredds/catalog/nexrad/level2/KFTG/20080730/catalog.xml";
    //String catUriString = "http://newmotherlode.ucar.edu:8080/thredds/catalog/nexrad/composite/gini/ntp/4km/20080731/catalog.xml";
    if ( args.length > 0 && args[0] != null && args[0].startsWith( "http://" ) )
      catUriString = args[0];
    //String catUriString = "http://newmotherlode.ucar.edu:8080/thredds/catalog.xml";
    URI catUri = null;
    try
    {
      catUri = new URI( catUriString );
    }
    catch ( URISyntaxException e )
    {
      fail( "Bad syntax in catalog URI [" + catUriString + "]: " + e.getMessage() );
    }
    try
    {
      HttpUriResolver httpUriResolver = HttpUriResolverFactory.getDefaultHttpUriResolver( catUri );
      httpUriResolver.makeRequest();

      InputStream is = httpUriResolver.getResponseBodyAsInputStream();

      InputStreamReader isr = new InputStreamReader( is, "UTF-8" );
      int cnt = 1;
      while ( isr.ready() )
      {
        char[] c = new char[1000];
        int num = isr.read( c );
        System.out.println( cnt + "[" + num + "]" + new String( c ) );
        cnt++;
      }
    }
    catch ( IOException e )
    {
      fail( "Failed to read catalog [" + catUriString + "]: " + e.getMessage() );
    }

  }
}
