package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuilderFinishIssue;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.Service;
import thredds.catalog2.Property;
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
    { fail( "Bad URI syntax: " + e.getMessage()); }

    type = ServiceType.OPENDAP;
  }

  public void testConstructorNullName()
  {
    try
    { new ServiceImpl( null, type, baseUri, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testConstructorNullType()
  {
    try
    { new ServiceImpl( "s1", null, baseUri, null ); }
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
      new ServiceImpl( "s1", type, null, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testCtorGetSet()
  {
    String name = "s1";
    ServiceBuilder sb = new ServiceImpl( name, type, baseUri, null );

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

  public void testServiceContainerNonuniqueServiceName()
  {
    ServiceBuilder sb = new ServiceImpl( "s1", type, baseUri, null );
    sb.addService( "s2", type, baseUri );
    sb.addService( "s3", type, baseUri );
    assertTrue( "Failed to discover that service name [s2] already in use globally.",
                sb.isServiceNameAlreadyInUseGlobally( "s2" ) );
    try
    { sb.addService( "s2", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage()); }
    fail( "No IllegalStateException.");
  }

  public void testServiceContainerNonuniqueServiceNameNested()
  {
    ServiceBuilder sb = new ServiceImpl( "s1", type, baseUri, null );
    sb.addService( "s2", type, baseUri );
    ServiceImpl sb3 = (ServiceImpl) sb.addService( "s3", type, baseUri );
    sb3.addService( "s3.1", type, baseUri );
    assertTrue( "Failed to discover that service name [s2] already in use globally.",
                sb3.isServiceNameAlreadyInUseGlobally( "s2" ) );
    try
    { sb3.addService( "s2", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage()); }
    fail( "No IllegalStateException.");
  }

  public void testServiceContainerNonuniqueServiceNameNestedTwoLevels()
  {
    ServiceBuilder sb = new ServiceImpl( "s1", type, baseUri, null );
    sb.addService( "s2", type, baseUri );
    ServiceBuilder sb3 = (ServiceImpl) sb.addService( "s3", type, baseUri );
    sb3.addService( "s3.1", type, baseUri );
    ServiceBuilder sb3_2 = (ServiceImpl) sb3.addService( "s3.2", type, baseUri );
    sb3_2.addService( "s3.2.1", type, baseUri );
    assertTrue( "Failed to discover that service name [s2] already in use globally.",
                sb3_2.isServiceNameAlreadyInUseGlobally( "s2" ));
    try
    { sb3_2.addService( "s2", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage()); }
    fail( "No IllegalStateException.");
  }

  public void testAddGetRemoveServices()
  {
    ServiceBuilder sb = new ServiceImpl( "s1n", type, baseUri, null );
    String s1_1n = "s1_1n";
    ServiceBuilder sb1_1 = sb.addService( s1_1n, type, baseUri );
    String s1_2n = "s1_2n";
    ServiceBuilder sb1_2 = sb.addService( s1_2n, type, baseUri );
    sb1_2.addService( "s1_2_1n", type, baseUri );
    ServiceBuilder sb1_2_2 = sb1_2.addService( "s1_2_2n", type, baseUri );
    sb1_2_2.addService( "s1_2_2_1", type, baseUri );

    List<ServiceBuilder> sbList = sb.getServiceBuilders();
    assertTrue( sbList.size() == 2 );
    assertTrue( sbList.get( 0).getName().equals( s1_1n ));
    assertTrue( sbList.get( 1).getName().equals( s1_2n));

    assertTrue( sb.getServiceBuilderByName( s1_1n ).equals( sb1_1 ));
    assertTrue( sb.getServiceBuilderByName( s1_2n ).equals( sb1_2 ));

    assertTrue( sb.isServiceNameAlreadyInUseGlobally( s1_2n ));

    // Test removal of service
    assertTrue( sb.removeService( s1_1n ) );
    assertNull( "Found removed service [" + s1_1n + "].",
                sb.getServiceBuilderByName( s1_1n ) );

    // Test that non-build getters fail.
    Service s = (Service) sb;
    try
    { s.getServices(); }
    catch ( IllegalStateException ise )
    {
      try
      { s.getServiceByName( s1_1n); }
      catch ( IllegalStateException ise2 )
      { return; }
      catch ( Exception e )
      { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
    }
    catch ( Exception e )
    { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage()); }

  }
  public void testAddGetReplaceRemoveProperties()
  {
    ServiceBuilder sb = new ServiceImpl( "s1", type, baseUri, null );
    String p1n = "p1";
    String p1v = "p1.v";
    sb.addProperty( p1n, p1v );
    String p2n = "p2";
    String p2v = "p2.v";
    sb.addProperty( p2n, p2v );
    String p3n = "p3";
    String p3v = "p3.v";
    sb.addProperty( p3n, p3v );

    // Test getPropertyNames()
    List<String> propNames = sb.getPropertyNames();
    assertTrue( propNames.size() == 3);
    assertTrue( propNames.get( 0 ).equals( p1n ));
    assertTrue( propNames.get( 1 ).equals( p2n ));
    assertTrue( propNames.get( 2 ).equals( p3n ));

    // Test getPropertyValue()
    String testValue = sb.getPropertyValue( p1n );
    assertTrue( "Property [" + p1n + "]/[" + testValue + "] not as expected ["+p1n+"]/[" + p1v + "].",
                testValue.equals( p1v ) );
    testValue = sb.getPropertyValue( p2n );
    assertTrue( "Property [" + p2n + "]/[" + testValue + "] not as expected ["+p2n+"]/[" + p2v + "].",
                testValue.equals( p2v ) );
    testValue = sb.getPropertyValue( p3n );
    assertTrue( "Property [" + p3n + "]/[" + testValue + "] not as expected ["+p3n+"]/[" + p3v + "].",
                testValue.equals( p3v ) );

    // Test replacement.
    String p1vNew = "p1.vNew";
    sb.addProperty( p1n, p1vNew );
    testValue = sb.getPropertyValue( p1n );
    assertTrue( "Property [" + p1n + "]/[" + testValue + "] not as expected ["+p1n+"]/[" + p1vNew + "].",
                testValue.equals( p1vNew ) );

    // Test removal of property.
    assertTrue( sb.removeProperty( p1n ));
    assertNull( sb.getPropertyValue( p1n ));

    // Test that non-build getters fail.
    Service s = (Service) sb;
    try
    { s.getProperties(); }
    catch ( IllegalStateException ise )
    {
      try
      { s.getPropertyByName( p1n); }
      catch ( IllegalStateException ise2 )
      { return; }
      catch ( Exception e )
      { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
    }
    catch ( Exception e )
    { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
  }

  // Set, add, build and test that non-build getters succeed and build add/getters/remove fail.
  public void testBuildGet()
  {
    ServiceBuilder sb = new ServiceImpl( "s1", type, baseUri, null );

    sb.setDescription( "description" );
    sb.setSuffix( "suffix" );

    String p1n = "p1";
    String p1v = "p1.v";
    sb.addProperty( p1n, p1v );
    String p2n = "p2";
    String p2v = "p2.v";
    sb.addProperty( p2n, p2v );
    String p3n = "p3";
    String p3v = "p3.v";
    sb.addProperty( p3n, p3v );

    String s1_1n = "s1_1n";
    ServiceBuilder sb1_1 = sb.addService( s1_1n, type, baseUri );
    String s1_2n = "s1_2n";
    ServiceBuilder sb1_2 = sb.addService( s1_2n, type, baseUri );
    sb1_2.addService( "s1_2_1n", type, baseUri );
    ServiceBuilder sb1_2_2 = sb1_2.addService( "s1_2_2n", type, baseUri );
    sb1_2_2.addService( "s1_2_2_1", type, baseUri );

    // Check if buildable
    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( ! sb.isBuildable( issues ))
    {
      StringBuilder stringBuilder = new StringBuilder( "Not isBuildable(): ");
      for ( BuilderFinishIssue bfi : issues )
        stringBuilder.append( "\n    ").append( bfi.getMessage()).append(" [").append( bfi.getBuilder().getClass().getName()).append( "]");
      fail( stringBuilder.toString());
    }

    // Build
    Service s = null;
    try
    { s = sb.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage()); }

    // Test that Service methods succeed after build.
    List<Property> propList = s.getProperties();
    assertTrue( propList.size() == 3 );
    assertTrue( propList.get( 0).getName().equals( p1n));
    assertTrue( propList.get( 1).getName().equals( p2n));
    assertTrue( propList.get( 2).getName().equals( p3n));

    assertTrue( s.getPropertyByName( p1n ).getName().equals( p1n));

    List<Service> sList = s.getServices();
    assertTrue( sList.size() == 2);
    assertTrue( sList.get( 0) == sb1_1 );
    assertTrue( sList.get( 1) == sb1_2 );

    assertTrue( s.getServiceByName( s1_1n ) == sb1_1);

    // Test that ServiceBuilder methods fail after build.
    try
    { sb.setType( ServiceType.ADDE ); }
    catch ( IllegalStateException ise1 )
    {
      try
      { sb.setBaseUri( docBaseUri ); }
      catch ( IllegalStateException ise2)
      {
        try
        { sb.setDescription( "fred" ); }
        catch ( IllegalStateException ise3 )
        {
          try
          { sb.setSuffix( "suf" ); }
          catch ( IllegalStateException ise4 )
          {
            try
            { sb.addProperty( "f", "" ); }
            catch ( IllegalStateException ise5 )
            {
              try
              { sb.addService( "a", type, baseUri ); }
              catch ( IllegalStateException ise6 )
              {
                try
                { sb.getPropertyNames(); }
                catch ( IllegalStateException ise7 )
                {
                  try
                  { sb.getPropertyValue( p1n ); }
                  catch ( IllegalStateException ise8 )
                  {
                    try
                    { sb.getServiceBuilders(); }
                    catch ( IllegalStateException ise9 )
                    {
                      try
                      { sb.getServiceBuilderByName( s1_1n ); }
                      catch ( IllegalStateException ise10 )
                      { return; }
                      catch ( Exception e )
                      { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
                    }
                    catch ( Exception e )
                    { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
                  }
                  catch ( Exception e )
                  { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
                }
                catch ( Exception e )
                { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
              }
              catch ( Exception e )
              { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
            }
            catch ( Exception e )
            { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
          }
          catch ( Exception e )
          { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
        }
        catch ( Exception e )
        { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
      }
      catch ( Exception e )
      { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
    }
    catch ( Exception e )
    { fail( "Unexpected non-IllegalStateException exception thrown: " + e.getMessage() ); }
    fail( "No exception thrown.");
  }
}
