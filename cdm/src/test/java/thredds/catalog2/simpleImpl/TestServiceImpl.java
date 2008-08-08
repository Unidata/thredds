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
    ServiceType chgType = ServiceType.ADDE;
    sb.setType( chgType );
    assertTrue( "Type [" + sb.getType() + "] not as expected [" + chgType + "].",
                sb.getType().equals( chgType));

    assertTrue( "BaseUri [" + sb.getBaseUri() + "] not as expected [" + baseUri + "].",
                sb.getBaseUri().equals( baseUri ));
    sb.setBaseUri( docBaseUri );
    assertTrue( "BaseUri [" + sb.getBaseUri() + "] not as expected [" + docBaseUri + "].",
                sb.getBaseUri().equals( docBaseUri ));

    assertTrue( "Description [" + sb.getDescription() + "] not empty string (default).",
                sb.getDescription().equals( ""));
    String descrip = "a desc";
    sb.setDescription( descrip );
    assertTrue( "Description [" + sb.getDescription() + "] not as expected [" + descrip + "].",
                sb.getDescription().equals( descrip ) );

    assertTrue( "Suffix [" + sb.getSuffix() + "] not empty string (default).",
                sb.getSuffix().equals( ""));
    String suffix = "a suffix";
    sb.setSuffix( suffix );
    assertTrue( "suffix [" + sb.getSuffix() + "] not as expected [" + suffix + "].",
                sb.getSuffix().equals( suffix ) );
  }

  public void testContainerDatasetNonuniqueDatasetName()
  {
    ServiceImpl sb = new ServiceImpl( "s1", type, baseUri, null, null );
    sb.addService( "s2", type, baseUri );
    sb.addService( "s3", type, baseUri );
    try
    { sb.addService( "s2", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage()); }
    fail( "No IllegalStateException.");
  }
}
