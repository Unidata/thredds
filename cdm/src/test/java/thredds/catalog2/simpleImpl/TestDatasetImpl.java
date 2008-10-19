package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.Dataset;
import thredds.catalog.ServiceType;
import thredds.catalog.DataFormatType;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestDatasetImpl extends TestCase
{
  private DatasetImpl dsImpl;
  private DatasetBuilder dsBuider;
  private Dataset ds;

  private ServiceBuilder serviceBuilder;
  private String serviceName;
  private URI serviceBaseUri;
  private ServiceType serviceType;

  private AccessImpl access;
  private String urlPath;
  private int dataSize;
  private DataFormatType formatType;


  public TestDatasetImpl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testCtorBuilderSetGet()
  {
    fail( "testCtorAndBuilderSetGet() not implemented.");
  }

  public void testChildDatasetBuilderSetGet()
  {
    fail( "testCtorAndBuilderSetGet() not implemented.");
  }

  public void testBuilderRemove()
  {
    fail( "testBuilderRemove() not implemented." );
  }

  public void testBuilderIllegalStateException()
  {
    fail( "testBuilderIllegalStateException() not implemented." );
  }

  public void testBuild()
  {
    fail( "testBuild() not implemented." );
  }

  public void testPostBuildGetters()
  {
    fail( "testPostBuildGetters() not implemented." );
  }

  public void testPostBuildIllegalStateException()
  {
    fail( "testPostBuildIllegalStateException() not implemented." );
  }
}
