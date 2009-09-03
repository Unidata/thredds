package thredds.catalog2.simpleImpl;

import org.junit.Test;

import static org.junit.Assert.*;

import thredds.catalog.ServiceType;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class GlobalServiceContainerTest
{
  @Test
  public void addAllServicesWithUniqueNames()
          throws URISyntaxException
  {
    GlobalServiceContainer globalServiceContainer = new GlobalServiceContainer();

    assertTrue( globalServiceContainer.isEmpty());

    ServiceImpl odapService = new ServiceImpl( "odap", ServiceType.OPENDAP, new URI( "http://server/thredds/dodsC/" ), null );
    ServiceImpl wmsService = new ServiceImpl( "wms", ServiceType.WMS, new URI( "http://server/thredds/wms/" ), null );
    ServiceImpl wcsService = new ServiceImpl( "wcs", ServiceType.WCS, new URI( "http://server/thredds/wcs/" ), null );

    globalServiceContainer.addService( odapService );
    globalServiceContainer.addService( wmsService );
    globalServiceContainer.addService( wcsService );

    assertFalse( globalServiceContainer.isEmpty());
    assertEquals( 3, globalServiceContainer.numberOfServicesWithGloballyUniqueNames() );
    assertEquals( 0, globalServiceContainer.numberOfServicesWithDuplicateNames());

    assertTrue( globalServiceContainer.isServiceNameInUseGlobally( "odap" ));
    assertTrue( globalServiceContainer.isServiceNameInUseGlobally( "wms" ));
    assertTrue( globalServiceContainer.isServiceNameInUseGlobally( "wcs" ));

    assertEquals( odapService, globalServiceContainer.getServiceByGloballyUniqueName( "odap" ) );
    assertEquals( wmsService, globalServiceContainer.getServiceByGloballyUniqueName( "wms" ) );
    assertEquals( wcsService, globalServiceContainer.getServiceByGloballyUniqueName( "wcs" ) );
  }

  @Test
  public void addServiceWithDuplicateName()
          throws URISyntaxException
  {
    GlobalServiceContainer globalServiceContainer = new GlobalServiceContainer();

    ServiceImpl odapService = new ServiceImpl( "odap", ServiceType.OPENDAP, new URI( "http://server/thredds/dodsC/" ), null );
    ServiceImpl wmsService = new ServiceImpl( "wms", ServiceType.WMS, new URI( "http://server/thredds/wms/" ), null );
    // Create a service with duplicate name.
    ServiceImpl wcsService = new ServiceImpl( "wms", ServiceType.WCS, new URI( "http://server/thredds/wcs/" ), null );

    globalServiceContainer.addService( odapService );
    globalServiceContainer.addService( wmsService );
    globalServiceContainer.addService( wcsService );

    assertFalse( globalServiceContainer.isEmpty() );
    assertEquals( 2, globalServiceContainer.numberOfServicesWithGloballyUniqueNames() );
    assertEquals( 1, globalServiceContainer.numberOfServicesWithDuplicateNames() );

    // Find all but duplicate.
    assertTrue( globalServiceContainer.isServiceNameInUseGlobally( "odap" ) );
    assertTrue( globalServiceContainer.isServiceNameInUseGlobally( "wms" ) );
    assertFalse( globalServiceContainer.isServiceNameInUseGlobally( "wcs" ) );

    // Again, find all but duplicate.
    assertEquals( odapService, globalServiceContainer.getServiceByGloballyUniqueName( "odap" ) );
    assertEquals( wmsService, globalServiceContainer.getServiceByGloballyUniqueName( "wms" ) );
    assertNull( globalServiceContainer.getServiceByGloballyUniqueName( "wcs" ) );

    // Make sure after removal the duplicate is then found.
    assertTrue( globalServiceContainer.removeService( wmsService ));
    assertEquals( wcsService, globalServiceContainer.getServiceByGloballyUniqueName( "wms" ));

    assertEquals( 2, globalServiceContainer.numberOfServicesWithGloballyUniqueNames() );
    assertEquals( 0, globalServiceContainer.numberOfServicesWithDuplicateNames() );


    // Make sure after removal service is not found.
    assertTrue( globalServiceContainer.removeService( odapService ));
    assertFalse( globalServiceContainer.isServiceNameInUseGlobally( "odap" ));
    assertNull( globalServiceContainer.getServiceByGloballyUniqueName( "odap" ));

    assertEquals( 1, globalServiceContainer.numberOfServicesWithGloballyUniqueNames() );
    assertEquals( 0, globalServiceContainer.numberOfServicesWithDuplicateNames() );
  }
}
