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
import static org.junit.Assert.assertEquals;

import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderIssues;
import thredds.catalog2.Service;
import thredds.catalog2.Property;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestServiceImpl
{
  private String allName, odapName, wcsName, wmsName;
  private ServiceType allType, odapType, wcsType, wmsType;
  private URI allBaseUri, odapBaseUri, wcsBaseUri, wmsBaseUri;
  private ServiceImpl allService, odapService, wcsService, wmsService;

  @Before
  public void setupCompoundService() throws URISyntaxException
  {
    allName = "all";
    allType = ServiceType.COMPOUND;
    allBaseUri = new URI( "");
    allService = new ServiceImpl( allName, allType, allBaseUri, null );

    odapName = "odap";
    odapType = ServiceType.OPENDAP;
    odapBaseUri = new URI( "http://server/thredds/dodsC/" );
    odapService = (ServiceImpl) allService.addService( odapName, odapType, odapBaseUri );

    wcsName = "wcs";
    wcsType = ServiceType.WCS;
    wcsBaseUri = new URI( "http://server/thredds/wcs/" );
    wcsService = (ServiceImpl) allService.addService( wcsName, wcsType, wcsBaseUri );

    wmsName = "wms";
    wmsType = ServiceType.WMS;
    wmsBaseUri = new URI( "http://server/thredds/wms/" );
    wmsService = (ServiceImpl) allService.addService( wmsName, wmsType, wmsBaseUri );
  }

  @Test
  public void checkNewServiceAsExpectedAndBuildIssues()
  {
    ServiceImpl si = new ServiceImpl( odapName, odapType, odapBaseUri, null );

    assertEquals( odapName, si.getName());
    assertEquals( odapType, si.getType());
    assertEquals( odapBaseUri, si.getBaseUri());

    BuilderIssues issues = si.getIssues();
    assertTrue( issues.toString(), issues.isValid());
    assertTrue( issues.toString(), issues.isEmpty());
  }

  @Test
  public void checkChangedServiceAsExpectedAndBuilderIssues() throws URISyntaxException
  {
    ServiceImpl service = new ServiceImpl( odapName, odapType, odapBaseUri, null );

    service.setType( wcsType );
    assertEquals( wcsType, service.getType());

    service.setBaseUri( wcsBaseUri );
    assertEquals( wcsBaseUri, service.getBaseUri());

    String descrip = "description";
    service.setDescription( descrip );
    assertEquals( descrip, service.getDescription());

    String suffix = ".suffix";
    service.setSuffix( suffix );
    assertEquals( suffix, service.getSuffix() );

    BuilderIssues issues = service.getIssues();
    assertTrue( issues.toString(), issues.isValid() );
    assertTrue( issues.toString(), issues.isEmpty() );
  }

  @Test(expected=IllegalArgumentException.class)
  public void checkExceptionOnConstructorWithNullName() {
    new ServiceImpl( null, odapType, odapBaseUri, null );
  }

  @Test(expected=IllegalArgumentException.class)
  public void checkExceptionOnConstructorWithNullType() {
    new ServiceImpl( odapName, null, odapBaseUri, null );
  }

  @Test(expected=IllegalArgumentException.class)
  public void checkExceptionOnConstructorWithNullBaseUri() {
    new ServiceImpl( odapName, odapType, null, null );
  }

  @Test(expected=IllegalArgumentException.class)
  public void checkExceptionOnChangeToNullType() {
    odapService.setType( null );
  }

  @Test(expected=IllegalArgumentException.class)
  public void checkExceptionOnChangeToNullBaseUri() {
    odapService.setBaseUri( null );
  }

  @Test
  public void checkCompoundServiceAsExpected() {
    assertCompoundServiceAsExpected( 0);
  }

  private void assertCompoundServiceAsExpected( int numExtraServices)
  {
    assertEquals( allName, allService.getName());
    assertEquals( allType, allService.getType());
    assertEquals( allBaseUri, allService.getBaseUri());

    assertEquals( odapService, allService.getServiceBuilderByName( odapName ));
    assertEquals( wcsService, allService.getServiceBuilderByName( wcsName ));
    assertEquals( wmsService, allService.getServiceBuilderByName( wmsName ));

    assertEquals( odapService, allService.findServiceBuilderByNameGlobally( odapName ));
    assertEquals( wcsService, allService.findServiceBuilderByNameGlobally( wcsName ));
    assertEquals( wmsService, allService.findServiceBuilderByNameGlobally( wmsName ));

    List<ServiceBuilder> serviceBuilders = allService.getServiceBuilders();
    assertFalse( serviceBuilders.isEmpty());
    assertEquals( 3 + numExtraServices, serviceBuilders.size());

    assertEquals( odapService, serviceBuilders.get( 0));
    assertEquals( wcsService, serviceBuilders.get( 1));
    assertEquals( wmsService, serviceBuilders.get( 2));
  }

  @Test
  public void checkCompoundServiceWhenContainingServiceWithNonuniqueName()
  {
    ServiceBuilder dupService = this.allService.addService( this.odapName, this.wcsType, this.wcsBaseUri );

    assertCompoundServiceAsExpected( 1 );

    assertEquals( dupService, this.allService.getServiceBuilders().get(3));
  }

  @Test
  public void testAddGetRemoveServices() throws URISyntaxException
  {
    ServiceBuilder sb1 = this.allService.addService( "one", ServiceType.HTTPServer, new URI( "http://server/thredds/httpServer/") );
    ServiceBuilder sb2 = this.allService.addService( "two", ServiceType.HTTP, new URI( "http://server/thredds/two/") );

    assertCompoundServiceAsExpected( 2 );

    assertEquals( sb1, this.allService.getServiceBuilderByName( "one" ));
    assertEquals( sb2, this.allService.getServiceBuilderByName( "two" ));

    assertEquals( sb1, this.allService.findServiceBuilderByNameGlobally( "one" ));
    assertEquals( sb2, this.allService.findServiceBuilderByNameGlobally( "two" ));

    assertTrue( this.allService.removeService( sb1 ));

    assertCompoundServiceAsExpected( 1 );

    assertNull( this.allService.getServiceBuilderByName( "one" ));
    assertNull( this.allService.findServiceBuilderByNameGlobally( "one" ));

    assertEquals( sb2, this.allService.getServiceBuilderByName( "two" ) );
    assertEquals( sb2, this.allService.findServiceBuilderByNameGlobally( "two" ) );

    assertTrue( this.allService.removeService( odapService ));

    assertNull( this.allService.getServiceBuilderByName( odapName ));
    assertNull( this.allService.findServiceBuilderByNameGlobally( odapName ));

    List<ServiceBuilder> serviceBuilders = this.allService.getServiceBuilders();
    assertFalse( serviceBuilders.isEmpty());
    assertEquals( 3, serviceBuilders.size());
    assertEquals( wcsService, serviceBuilders.get( 0));
    assertEquals( wmsService, serviceBuilders.get( 1));
    assertEquals( sb2, serviceBuilders.get( 2));
  }

  @Test
  public void testAddGetReplaceRemoveProperties()
  {
    String colorPropName = "color";
    String colorPropValue = "red";
    odapService.addProperty( colorPropName, colorPropValue );
    String texturePropName = "texture";
    String texturePropValue = "rough";
    odapService.addProperty( texturePropName, texturePropValue );
    String tastePropName = "taste";
    String tastePropValue = "sweet";
    odapService.addProperty( tastePropName, tastePropValue );

    // Test getPropertyNames()
    List<String> propNames = odapService.getPropertyNames();
    assertEquals( 3, propNames.size());
    assertEquals( colorPropName, propNames.get( 0 ));
    assertEquals( texturePropName, propNames.get( 1 ));
    assertEquals( tastePropName, propNames.get( 2 ));

    // Test getPropertyValue()
    assertEquals( colorPropValue, odapService.getPropertyValue( colorPropName ));
    assertEquals( texturePropValue, odapService.getPropertyValue( texturePropName ));
    assertEquals( tastePropValue, odapService.getPropertyValue( tastePropName ));

    // Test replacement.
    String colorPropNewValue = "orange";
    odapService.addProperty( colorPropName, colorPropNewValue );
    assertEquals( colorPropNewValue, odapService.getPropertyValue( colorPropName ));

    // Test removal of property.
    assertTrue( odapService.removeProperty( colorPropName ));
    assertNull( odapService.getPropertyValue( colorPropName ));
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPreBuildGetProperties() {
    odapService.getProperties();
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPreBuildGetPropertyByNAme() {
    odapService.getPropertyByName( "name");
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPreBuildGetServices() {
    odapService.getServices();
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPreBuildGetServiceByName() {
    odapService.getServiceByName( "name");
  }

  // Set, add, build and test that non-build getters succeed and build add/getters/remove fail.
  @Test
  public void checkBuildAndGet() throws BuilderException
  {
    this.allService.addProperty( "propName1", "propValue1" );
    this.allService.addProperty( "propName2", "propValue2" );

    // Check if buildable
    BuilderIssues issues = this.allService.getIssues();
    assertTrue( issues.toString(), issues.isValid());
    assertTrue( issues.toString(), issues.isEmpty());

    // Build
    Service s = this.allService.build();

    assertNotNull( s);
    assertTrue( this.allService.isBuilt() );

    assertEquals( allName, s.getName() );
    assertEquals( allType, s.getType() );
    assertEquals( allBaseUri, s.getBaseUri() );

    assertEquals( odapService, s.getServiceByName( odapName ) );
    assertEquals( wcsService, s.getServiceByName( wcsName ) );
    assertEquals( wmsService, s.getServiceByName( wmsName ) );

    assertEquals( odapService, s.findServiceByNameGlobally( odapName ) );
    assertEquals( wcsService, s.findServiceByNameGlobally( wcsName ) );
    assertEquals( wmsService, s.findServiceByNameGlobally( wmsName ) );

    List<Service> services = s.getServices();
    assertFalse( services.isEmpty() );
    assertEquals( 3, services.size() );

    assertEquals( odapService, services.get( 0 ) );
    assertEquals( wcsService, services.get( 1 ) );
    assertEquals( wmsService, services.get( 2 ) );

    // Test that Service methods succeed after build.
    List<Property> propList = s.getProperties();
    assertEquals( 2, propList.size() );
    Property prop1 = propList.get( 0 );
    Property prop2 = propList.get( 1 );

    assertEquals( "propName1", prop1.getName() );
    assertEquals( "propName2", prop2.getName() );

    assertEquals( "propValue1", prop1.getValue() );
    assertEquals( "propValue2", prop2.getValue() );
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPostBuildsetType() throws BuilderException
  {
    this.allService.build();
    this.allService.setType( odapType);
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPostBuildSetBaseUri() throws BuilderException
  {
    this.allService.build();
    this.allService.setBaseUri( odapBaseUri);
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPostBuildSetDescription() throws BuilderException
  {
    this.allService.build();
    this.allService.setDescription( "desc");
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPostBuildSetSuffix() throws BuilderException
  {
    this.allService.build();
    this.allService.setSuffix( ".suffix");
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPostBuildAddProperty() throws BuilderException
  {
    this.allService.build();
    this.allService.addProperty( "propName1", "propVal1");
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPostBuildAddService()
          throws BuilderException, URISyntaxException
  {
    this.allService.build();
    this.allService.addService( "newService", ServiceType.FILE, new URI( "http://server/thredds/new/"));
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPostBuildGetPropertyNames()
          throws BuilderException
  {
    this.allService.build();
    this.allService.getPropertyNames();
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPostBuildGetPropertyValue()
          throws BuilderException
  {
    this.allService.build();
    this.allService.getPropertyValue( "name");
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPostBuildGetServiceBuilders()
          throws BuilderException
  {
    this.allService.build();
    this.allService.getServiceBuilders();
  }

  @Test(expected=IllegalStateException.class)
  public void checkExceptionOnPostBuildGetServiceBuilderByName()
          throws BuilderException
  {
    this.allService.build();
    this.allService.getServiceBuilderByName( "name");
  }

  @Test
  public void checkBuildIssuesOnCompoundServiceWhenContainingServiceWithNonuniqueName()
  {
    ServiceBuilder dupService = this.allService.addService( this.odapName, this.wcsType, this.wcsBaseUri );

    BuilderIssues issues = this.allService.getIssues();
    assertTrue( issues.toString(), issues.isValid() );
    assertFalse( issues.toString(), issues.isEmpty() );
  }
}
