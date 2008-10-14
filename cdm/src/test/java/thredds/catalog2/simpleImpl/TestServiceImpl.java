package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.CatalogBuilderFactory;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderFinishIssue;
import thredds.catalog2.Property;
import thredds.catalog2.Service;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;

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

  private CatalogBuilderFactory catBuildFactory;

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

    catBuildFactory = new CatalogBuilderFactoryImpl();
  }

  public void testConstructorNullName()
  {
    try
    { catBuildFactory.newServiceBuilder( null, type, baseUri ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testConstructorNullType()
  {
    try
    {
      catBuildFactory.newServiceBuilder( "s1", null, baseUri ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testConstructorNullDocBaseUri()
  {
    try
    {
      catBuildFactory.newServiceBuilder( "s1", type, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testNormal()
  {
    String name = "s1";
    ServiceBuilder sb = catBuildFactory.newServiceBuilder( name, type, baseUri );

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
    ServiceBuilder sb = catBuildFactory.newServiceBuilder( "s1", type, baseUri );
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

  public void testContainerDatasetNonuniqueDatasetNameNested()
  {
    ServiceBuilder sb = catBuildFactory.newServiceBuilder( "s1", type, baseUri );
    sb.addService( "s2", type, baseUri );
    ServiceImpl sb3 = (ServiceImpl) sb.addService( "s3", type, baseUri );
    sb3.addService( "s3.1", type, baseUri );
    try
    { sb3.addService( "s3.2", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage()); }
    fail( "No IllegalStateException.");
  }

  public void testContainerDatasetNonuniqueDatasetNameNestedTwoLevels()
  {
    ServiceBuilder sb = catBuildFactory.newServiceBuilder( "s1", type, baseUri );
    sb.addService( "s2", type, baseUri );
    ServiceBuilder sb3 = (ServiceImpl) sb.addService( "s3", type, baseUri );
    sb3.addService( "s3.1", type, baseUri );
    ServiceBuilder sb3_2 = (ServiceImpl) sb3.addService( "s3.2", type, baseUri );
    sb3_2.addService( "s3.2.1", type, baseUri );
    try
    { sb3_2.addService( "s2", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage()); }
    fail( "No IllegalStateException.");
  }

  public void testProperties()
  {
    ServiceBuilder sb = catBuildFactory.newServiceBuilder( "s1", type, baseUri );
    String nameProp1 = "p1";
    String valueProp1 = "p1.v";
    sb.addProperty( nameProp1, valueProp1 );
    sb.addProperty( "p2", "p2.v" );
    sb.addProperty( "p3", "p3.v" );
    assertTrue( "Property(" + nameProp1 + ") value [" + sb.getPropertyValue( nameProp1 )+ "] not as expected [" + valueProp1 + "].",
                sb.getPropertyValue( nameProp1 ).equals( valueProp1));
    String newValueProp1 = "p1.vNew";
    sb.addProperty( nameProp1, newValueProp1 );
    assertTrue( "Property(" + nameProp1 + ") new value [" + sb.getPropertyValue( nameProp1 ) + "] not as expected [" + newValueProp1 + "].",
                sb.getPropertyValue( nameProp1 ).equals( newValueProp1 ) );

    boolean pass = false;
    try
    { ((ServiceImpl) sb).getProperties(); }
    catch ( IllegalStateException e )
    { pass = true; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage()); }
    if ( ! pass ) fail( "No IllegalStateException.");

    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    boolean isBuildable = sb.isBuildable( issues );
    if ( ! isBuildable )
    {
      StringBuilder sb2 = new StringBuilder( "ServiceBuilder not buildable: ");
      for ( BuilderFinishIssue bfi : issues)
        sb2.append( "\n    ").append( bfi.getMessage());
      fail( sb2.toString());
    }

    Service s = null;
    try
    {
      s = sb.build();
    }
    catch ( BuilderException e )
    {
      fail();
    }
    List<Property> props = null;
    try
    { props = s.getProperties(); }
    catch ( IllegalStateException e )
    { fail( "Unexpected IllegalStateException: " + e.getMessage() ); }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage()); }

    assertTrue( "Size of property list [" + props.size() + "] not as expected [3]",
                props.size() == 3);
  }
}
