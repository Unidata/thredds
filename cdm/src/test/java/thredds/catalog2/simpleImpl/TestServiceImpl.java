package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestServiceImpl extends TestCase
{
  private URI baseUri;
  private URI docBaseUri;
  private ServiceType type;

  public TestServiceImpl( String name )
  {
    super( name );
  }

  @Override
  protected void setUp() throws Exception
  {
    try
    { baseUri = new URI( "http://server/thredds/dodsC/" );
      docBaseUri = new URI( "http://server/thredds/aCat.xml"); }
    catch ( URISyntaxException e )
    { fail(); }

    type = ServiceType.OPENDAP;
  }

  public void testConstructorNullName()
  {
    try
    { new ServiceImpl( null, type, baseUri, null, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testConstructorNullType()
  {
    try
    { new ServiceImpl( "s1", null, baseUri, null, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testConstructorNullDocBaseUri()
  {
    try
    { new ServiceImpl( "s1", type, null, null, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testConstructorBothContainersNotNull()
  {
    try
    { new ServiceImpl( "s1", type, baseUri,
                       new CatalogImpl( "cat1", docBaseUri, "", null, null ),
                       new ServiceImpl( "s2", type, baseUri, null, null));
    }
    catch( IllegalArgumentException e)
    { return; }
    catch( Exception e)
    { fail( "Unexpected exception: " + e.getMessage() ); }
    fail( "No IllegalArgumentException." );
  }

  public void testNormal()
  {
    String name = "s1";
    ServiceBuilder sb = new ServiceImpl( name, type, baseUri, null, null );

    assertTrue( "Name [" + sb.getName() + "] not as expected [" + name + "].",
                sb.getName().equals( name));
    assertTrue( "Type [" + sb.getType() + "] not as expected [" + type + "].",
                sb.getType().equals( type));
    assertTrue( "DocBaseUri [" + sb.getBaseUri() + "] not as expected [" + baseUri + "].",
                sb.getBaseUri().equals( baseUri ));
  }
}
