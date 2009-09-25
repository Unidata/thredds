/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.catalog2.simpleImpl;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderIssues;
import thredds.catalog.ServiceType;

import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test ServiceContainer.
 *
 * Most normal cases are exercised in TestServiceImpl and TestCatalogImpl.
 * Here we do a few tests on a new container.
 *
 * @author edavis
 * @since 4.0
 */
public class TestServiceContainer
{
  private GlobalServiceContainer globalServiceContainer;
  private ServiceContainer serviceContainer;
  private ServiceImpl odapService, wmsService, wcsService;

  @Before
  public void setupContainerWithThreeUniquelyNamedServices() throws Exception
  {
    this.globalServiceContainer = new GlobalServiceContainer();
    this.serviceContainer = new ServiceContainer( this.globalServiceContainer );

    this.odapService = this.serviceContainer.addService( "odap", ServiceType.OPENDAP, new URI( "http://server/thredds/dodsC/" ) );
    this.wmsService = this.serviceContainer.addService( "wms", ServiceType.WMS, new URI( "http://server/thredds/wms/" ) );
    this.wcsService = this.serviceContainer.addService( "wcs", ServiceType.WCS, new URI( "http://server/thredds/wcs/" ) );
  }

  @Test
  public void checkThatNewlyCreatedContainerIsEmpty()
  {
    GlobalServiceContainer gsc = new GlobalServiceContainer();
    ServiceContainer sc = new ServiceContainer( gsc);
    assertTrue( sc.isEmpty());

    assertNotNull( gsc );
    assertTrue( gsc.isEmpty() );
  }

  @Test(expected = IllegalStateException.class)
  public void checkExceptionOnPreBuildGetServiceByName() {
    this.serviceContainer.getServiceByName( "odap" );
  }

  @Test(expected = IllegalStateException.class)
  public void checkExceptionOnPreBuildGetServices() {
    this.serviceContainer.getServices();
  }

  @Test
  public void checkThreeAddedServicesAreContained() {
    assertThatThreeAddedServicesAreContained( 0);
  }

  private void assertThatThreeAddedServicesAreContained( int numDuplicates)
  {
    assertFalse( serviceContainer.isEmpty());
    assertEquals( 3 + numDuplicates, serviceContainer.size() );

    assertTrue( serviceContainer.containsServiceName( "odap" ));
    assertTrue( serviceContainer.containsServiceName( "wms" ));
    assertTrue( serviceContainer.containsServiceName( "wcs" ));

    assertEquals( odapService, serviceContainer.getServiceBuilderByName( "odap" ));
    assertEquals( wmsService, serviceContainer.getServiceBuilderByName( "wms" ));
    assertEquals( wcsService, serviceContainer.getServiceBuilderByName( "wcs" ));
  }

  @Test
  public void checkThreeAddedServicesAreContainedInOrder() {
    assertThatThreeAddedServicesAreContainedInOrder( 0);
  }

  private void assertThatThreeAddedServicesAreContainedInOrder( int numDuplicates)
  {
    List<ServiceBuilder> serviceBuilders = serviceContainer.getServiceBuilders();
    assertNotNull( serviceBuilders);
    assertEquals( 3 + numDuplicates, serviceBuilders.size());

    assertEquals( odapService, serviceBuilders.get( 0));
    assertEquals( wmsService, serviceBuilders.get( 1));
    assertEquals( wcsService, serviceBuilders.get( 2));
  }

  @Test
  public void checkThreeAddedServicesAreContainedGlobally() {
    assertThatThreeAddedServicesAreContainedGlobally();
  }

  private void assertThatThreeAddedServicesAreContainedGlobally()
  {
    assertEquals( odapService, serviceContainer.getServiceByGloballyUniqueName( "odap" ));
    assertEquals( wmsService, serviceContainer.getServiceByGloballyUniqueName( "wms" ));
    assertEquals( wcsService, serviceContainer.getServiceByGloballyUniqueName( "wcs" ));

    assertTrue( globalServiceContainer.isServiceNameInUseGlobally( "odap" ) );
    assertTrue( globalServiceContainer.isServiceNameInUseGlobally( "wms" ) );
    assertTrue( globalServiceContainer.isServiceNameInUseGlobally( "wcs" ) );

    assertEquals( odapService, globalServiceContainer.getServiceByGloballyUniqueName( "odap" ) );
    assertEquals( wmsService, globalServiceContainer.getServiceByGloballyUniqueName( "wms" ) );
    assertEquals( wcsService, globalServiceContainer.getServiceByGloballyUniqueName( "wcs" ) );
  }

  @Test
  public void checkThatThreeUniqueOneDup()
          throws URISyntaxException
  {
    ServiceImpl dupWmsService = this.serviceContainer.addService( "wms", ServiceType.WCS, new URI( "http://server/thredds/wcs/") );

    assertThatThreeAddedServicesAreContained( 1);
    assertThatThreeAddedServicesAreContainedInOrder( 1);
    assertThatThreeAddedServicesAreContainedGlobally();

    assertEquals( dupWmsService, this.serviceContainer.getServiceBuilders().get(3));

    assertFalse( this.globalServiceContainer.isEmpty() );
    assertEquals( 3, this.globalServiceContainer.numberOfServicesWithGloballyUniqueNames() );
    assertEquals( 1, this.globalServiceContainer.numberOfServicesWithDuplicateNames() );
  }

  @Test
  public void checkThatThreeUniqueOneDupAfterRemoveDup()
          throws URISyntaxException
  {
    ServiceImpl dupWmsService = this.serviceContainer.addService( "wms", ServiceType.WCS, new URI( "http://server/thredds/wcs/" ) );

    this.serviceContainer.removeService( dupWmsService );

    assertThatThreeAddedServicesAreContained( 0);
    assertThatThreeAddedServicesAreContainedInOrder( 0);
    assertThatThreeAddedServicesAreContainedGlobally();


    assertFalse( this.globalServiceContainer.isEmpty() );
    assertEquals( 3, this.globalServiceContainer.numberOfServicesWithGloballyUniqueNames() );
    assertEquals( 0, this.globalServiceContainer.numberOfServicesWithDuplicateNames() );
  }

  @Test
  public void checkThatThreeUniqueOneDupAfterRemoveVarious()
          throws URISyntaxException
  {
    ServiceImpl dupWmsService = this.serviceContainer.addService( "wms", ServiceType.WCS, new URI( "http://server/thredds/wcs/" ) );

    this.serviceContainer.removeService( wmsService );

    assertEquals( dupWmsService, this.serviceContainer.getServiceBuilderByName( "wms" ));
    assertEquals( dupWmsService, this.serviceContainer.getServiceByGloballyUniqueName( "wms" ));


    assertFalse( this.globalServiceContainer.isEmpty() );
    assertEquals( 3, this.globalServiceContainer.numberOfServicesWithGloballyUniqueNames() );
    assertEquals( 0, this.globalServiceContainer.numberOfServicesWithDuplicateNames() );

    this.serviceContainer.removeService( wcsService );

    assertEquals( odapService, this.serviceContainer.getServiceBuilderByName( "odap" ) );
    assertEquals( odapService, this.serviceContainer.getServiceByGloballyUniqueName( "odap" ) );

    assertFalse( this.globalServiceContainer.isEmpty() );
    assertEquals( 2, this.globalServiceContainer.numberOfServicesWithGloballyUniqueNames() );
    assertEquals( 0, this.globalServiceContainer.numberOfServicesWithDuplicateNames() );

  }

  @Test
  public void checkThatThreeUniqueServicesContainerBuilds() throws BuilderException
  {
    BuilderIssues issues = this.serviceContainer.getIssues();
    assertTrue( issues.toString(), issues.isValid());
    assertTrue( issues.toString(), issues.isEmpty());

    this.serviceContainer.build();

    assertTrue( this.serviceContainer.isBuilt() );
  }

  @Test
  public void checkThatNewContainerBuilds() throws BuilderException
  {
    GlobalServiceContainer gsc = new GlobalServiceContainer();
    ServiceContainer sc = new ServiceContainer( gsc);
    BuilderIssues issues = sc.getIssues();
    assertTrue( issues.toString(), issues.isValid() );

    sc.build();

    assertTrue( sc.isBuilt() );
  }
}