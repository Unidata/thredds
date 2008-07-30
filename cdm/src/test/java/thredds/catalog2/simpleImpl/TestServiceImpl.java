package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.Service;
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
    { docBaseUri = new URI( "http://server/path/name" ); }
    catch ( URISyntaxException e )
    { fail(); }

    type = ServiceType.OPENDAP;
  }

  public void testNullName()
  {
    try
    { new ServiceImpl( null, type, docBaseUri ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testNullType()
  {
    try
    { new ServiceImpl( "s1", null, docBaseUri ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testNullDocBaseUri()
  {
    try
    { new ServiceImpl( "s1", type, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testNormal()
  {
    String name = "s1";
    ServiceBuilder sb = new ServiceImpl( name, type, docBaseUri );

    assertTrue( "Name [" + sb.getName() + "] not as expected [" + name + "].",
                sb.getName().equals( name));
    assertTrue( "Type [" + sb.getType() + "] not as expected [" + type + "].",
                sb.getType().equals( type));
    assertTrue( "DocBaseUri [" + sb.getBaseUri() + "] not as expected [" + docBaseUri + "].",
                sb.getBaseUri().equals( docBaseUri ));
  }
}
