package thredds.catalog2.simpleImpl;

import junit.framework.*;

import java.net.URI;
import java.net.URISyntaxException;

import thredds.catalog.ServiceType;
import thredds.catalog.DataFormatType;
import thredds.catalog2.builder.ServiceBuilder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestAccessImpl extends TestCase
{

  private AccessImpl access;
  private ServiceBuilder serviceBuilder;
  private String serviceName;
  private URI serviceBaseUri;
  private ServiceType type;

  private String urlPath;
  private int dataSize;
  private DataFormatType formatType;


  public TestAccessImpl( String name )
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

  public void testCtorSetGet()
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

  public void testAccessBuilderIllegalStateException()
  {
    fail( "testAccessBuilderIllegalStateException() not implemented.");
  }

  public void testBuild()
  {
    fail( "  public void testBuild()() not implemented." );
  }

  public void testAccessIllegalStateException()
  {
    fail( "testAccessIllegalStateException() not implemented." );
  }
}
