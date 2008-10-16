package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuilderFinishIssue;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog.ServiceType;

import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test ServiceContainer.
 *
 * Most normal cases are exercised in TestServiceImpl and TestCatalogImpl.
 * Here we do a few tests on a new container.
 * ToDo May want to add more later.
 *
 * @author edavis
 * @since 4.0
 */
public class TestServiceContainer extends TestCase
{
  private ServiceContainer sc;

  private String p1n, p1v, p2n, p2v, p3n, p3v;
  private String s1n, s2n, s3n, s1_1n, s1_2n, s1_2_1n, s1_2_2n, s1_2_2_1n;
  private URI baseUri1;
  private ServiceType type;


  private ServiceImpl si1, si2, si3, si1_1, si1_2, si1_2_1, si1_2_2, si1_2_2_1;
  private ServiceBuilder sb1, sb2, sb3, sb1_1, sb1_2, sb1_2_1, sb1_2_2, sb1_2_2_1;

  public TestServiceContainer( String name )
  {
    super( name );
  }

  @Override
  protected void setUp() throws Exception
  {
    try
    { baseUri1 = new URI( "http://server/thredds/dodsC/" ); }
    catch ( URISyntaxException e )
    { fail( "Bad URI syntax: " + e.getMessage() ); }

    type = ServiceType.OPENDAP;

    p1n = "p1";
    p1v = "p1.v";
    p2n = "p2";
    p2v = "p2.v";
    p3n = "p3";
    p3v = "p3.v";

    s1n = "s1n";
    s2n = "s2n";
    s1_1n = "s1_1n";
    s1_2n = "s1_2n";
    s1_2_1n = "s1_2_1n";
    s1_2_2n = "s1_2_2n";
    s1_2_2_1n = "s1_2_2_1n";
  }

  private void optionalSetUp()
  {
    sc = new ServiceContainer( null );

    si1 = new ServiceImpl( s1n, type, baseUri1, null );
    sb1 = si1;
    sb1.setDescription( "description" );
    sb1.setSuffix( "suffix" );

    sb1.addProperty( p1n, p1v );
    sb1.addProperty( p2n, p2v );
    sb1.addProperty( p3n, p3v );
    sc.addService( si1 );

    sb1_1 = sb1.addService( s1_1n, type, baseUri1 );
    sb1_2 = sb1.addService( s1_2n, type, baseUri1 );
    sb1_2_1 = sb1_2.addService( s1_2_1n, type, baseUri1 );
    sb1_2_2 = sb1_2.addService( s1_2_2n, type, baseUri1 );
    sb1_2_2_1 = sb1_2_2.addService( s1_2_2_1n, type, baseUri1 );

    si2 = new ServiceImpl( s2n, type, baseUri1, null );
    sc.addService( si2 );
    si3 = new ServiceImpl( s3n, type, baseUri1, null );
    sc.addService( si3 );
  }


  public void testNewContainerBasics()
  {
    sc = new ServiceContainer( null);
    assertTrue( "New service container not empty.",
                sc.isEmpty());
    int size = sc.size();
    assertTrue( "New service container not size()==0 ["+size+"].",
                size == 0 );

    assertFalse( "New service container has service [name].",
                 sc.containsServiceName( "name" ));
    ServiceBuilder sb = sc.getServiceBuilderByName( "name" );
    if ( sb != null )
      fail( "New service container holds unexpected ServiceBuilder [name].");

    assertTrue( "New service container has non-empty ServiceBuilder list.",
                sc.getServiceBuilders().isEmpty());

    assertNull( "New service container has unexpected globally unique name [name].",
                sc.getServiceByGloballyUniqueName( "name" ));

    assertFalse( "New service container succeeded in removing serivce [name].",
                 sc.removeService( "name" ));
    assertFalse( "New service container succeeded in removing service by globally unique name [name].",
                 sc.removeServiceByGloballyUniqueName( "name" ));
  }

  public void testNewContainerPreBuildStateExceptions()
  {
    sc = new ServiceContainer( null );

    try
    { sc.getServiceByName( "name" ); }
    catch ( IllegalStateException ise1 )
    {
      try { sc.getServices(); }
      catch ( IllegalStateException ise2 )
      { return; }
      catch ( Exception e1 )
      { fail( "Unexpected non-IllegalStateException exception thrown: " + e1.getMessage() ); }
    }
    catch ( Exception e1 )
    { fail( "Unexpected non-IllegalStateException exception thrown: " + e1.getMessage() ); }
  }

  public void testNewContainerBuild()
  {
    sc = new ServiceContainer( null );
    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( !sc.isBuildable( issues ) )
    {
      StringBuilder stringBuilder = new StringBuilder( "Not in buildable state: " );
      for ( BuilderFinishIssue bfi : issues )
        stringBuilder.append( "\n    " ).append( bfi.getMessage() ).append( " [" ).append( bfi.getBuilder().getClass().getName() ).append( "]" );
      fail( stringBuilder.toString() );
    }

    try
    { sc.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }

    // Test build getters.

    // Test post build state exceptions
  }
}