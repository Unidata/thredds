package thredds.catalog2.simpleImpl;

import junit.framework.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;

import thredds.catalog.ServiceType;
import thredds.catalog.DataFormatType;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuilderFinishIssue;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.Service;
import thredds.catalog2.Access;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestDatasetNodeImpl extends TestCase
{

  private AccessImpl access;
  private ServiceBuilder serviceBuilder;
  private String serviceName;
  private URI serviceBaseUri;
  private ServiceType type;

  private String urlPath;
  private int dataSize;
  private DataFormatType formatType;


  public TestDatasetNodeImpl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    serviceName = "serviceName";

    try
    { serviceBaseUri = new URI( "http://server/thredds/dodsC/" ); }
    catch ( URISyntaxException e )
    { fail( "Bad URI syntax: " + e.getMessage() ); }

    type = ServiceType.OPENDAP;
    serviceBuilder = new ServiceImpl( serviceName, type, serviceBaseUri, null );

    urlPath = "someData.nc";
    dataSize = 5678;
    formatType = DataFormatType.NETCDF;
  }

  public void testCtorAndBuilderSetGet()
  {
    AccessImpl access = new AccessImpl();

    access.setServiceBuilder( serviceBuilder );
    access.setUrlPath( urlPath );
    access.setDataSize( dataSize );
    access.setDataFormat( formatType );

    assertTrue( "getServiceBuilder() not as expected.",
                access.getServiceBuilder() == serviceBuilder);
    String respUrlPath = access.getUrlPath();
    assertTrue( "geturlPath() ["+respUrlPath+"] not as expect ["+urlPath+"]",
                respUrlPath.equals(  urlPath ));
    long respDataSize = access.getDataSize();
    assertTrue( "getDataSize() ["+respDataSize+"] not as expected ["+dataSize+"].",
                respDataSize == dataSize );
    DataFormatType respDataFormat = access.getDataFormat();
    assertTrue( "getDataFormat ["+respDataFormat+"] not as expected ["+formatType+"].",
                respDataFormat == formatType );
  }

  public void testBuilderIllegalStateException()
  {
    AccessImpl accessImpl = new AccessImpl();

    accessImpl.setServiceBuilder( serviceBuilder );
    accessImpl.setUrlPath( urlPath );
    accessImpl.setDataSize( dataSize );
    accessImpl.setDataFormat( formatType );

    try
    { accessImpl.getService(); }
    catch ( IllegalStateException ise )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception thrown: " + e.getMessage() ); }

    fail( "Did not throw expected IllegalStateException.");
  }

  public void testBuild()
  {
    AccessImpl accessImpl = new AccessImpl();

    accessImpl.setServiceBuilder( serviceBuilder );
    accessImpl.setUrlPath( urlPath );
    accessImpl.setDataSize( dataSize );
    accessImpl.setDataFormat( formatType );

    // Check if buildable
    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( ! accessImpl.isBuildable( issues ) )
    {
      StringBuilder stringBuilder = new StringBuilder( "Not isBuildable(): " );
      for ( BuilderFinishIssue bfi : issues )
        stringBuilder.append( "\n    " ).append( bfi.getMessage() ).append( " [" ).append( bfi.getBuilder().getClass().getName() ).append( "]" );
      fail( stringBuilder.toString() );
    }

    // Build
    Access access = null;
    try
    { access = accessImpl.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }
  }

  public void testPostBuildGetters()
  {
    AccessImpl accessImpl = new AccessImpl();

    accessImpl.setServiceBuilder( serviceBuilder );
    accessImpl.setUrlPath( urlPath );
    accessImpl.setDataSize( dataSize );
    accessImpl.setDataFormat( formatType );

    Access access;
    try
    { access = accessImpl.build(); }
    catch( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); return; }

    assertTrue( access.getService().getName().equals( serviceName ) );
    assertTrue( access.getUrlPath().equals( urlPath ) );
    assertTrue( access.getDataSize() == dataSize );
    assertTrue( access.getDataFormat().equals( formatType ) );
  }

  public void testPostBuildIllegalStateException()
  {
    AccessImpl accessImpl = new AccessImpl();

    accessImpl.setServiceBuilder( serviceBuilder );
    accessImpl.setUrlPath( urlPath );
    accessImpl.setDataSize( dataSize );
    accessImpl.setDataFormat( formatType );

    Access access;
    try
    { access = accessImpl.build(); }
    catch( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); return; }

    try
    { accessImpl.getServiceBuilder(); }
    catch ( IllegalStateException ise )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception thrown: " + e.getMessage() ); }
    fail( "Did not throw expected IllegalStateException." );
  }
}