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

import thredds.catalog2.builder.*;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.text.ParseException;

import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestCatalogImpl
{
  private CatalogImpl catImpl;
  private String catName;
  private URI catDocBaseUri;
  private String catVersion;
  private DateType catExpires;
  private DateType catLastModified;
  private DateType catExpires2;
  private DateType catLastModified2;

  private URI docBaseUri;

  private ServiceType type;
  private URI baseUri;

  @Before
  public void setupBasicCatalog() throws URISyntaxException, ParseException
  {
    this.catName = "Catalog Name";
    this.catDocBaseUri = new URI( "http://server/thredds/cat.xml");
    this.catVersion = "1.0.2";
    this.catLastModified = new DateType( false, new Date( System.currentTimeMillis() ) );
    this.catExpires = new DateType( catLastModified ).add( new TimeDuration( "P5D" ) );
    this.catLastModified2 = this.catLastModified.add( new TimeDuration( "P1D"));
    this.catExpires2 = this.catLastModified2.add( new TimeDuration( "P5D"));
    this.catImpl = new CatalogImpl( catName, catDocBaseUri, this.catVersion, this.catExpires, this.catLastModified);

    try
    { baseUri = new URI( "http://server/thredds/dodsC/" );
      docBaseUri = new URI( "http://server/thredds/aCat.xml" ); }
    catch ( URISyntaxException e )
    { fail("Bad URI syntax: " + e.getMessage()); }

    type = ServiceType.OPENDAP;
  }

  @Test(expected=IllegalArgumentException.class)
  public void checkForExceptionOnConstructorWithNullDocBaseUri() {
    new CatalogImpl( this.catName, null, this.catVersion, this.catExpires, this.catLastModified );
  }

  @Test(expected=IllegalArgumentException.class)
  public void checkForExceptionWhenNullSetDocBaseUri() {
    this.catImpl.setDocBaseUri( null );
  }

  @Test
  public void checkConstructorWithOtherNulls()
  {
    new CatalogImpl( null, this.catDocBaseUri, this.catVersion, this.catExpires, this.catLastModified );
    new CatalogImpl( this.catName, this.catDocBaseUri, null, this.catExpires, this.catLastModified );
    new CatalogImpl( this.catName, this.catDocBaseUri, this.catVersion, null, this.catLastModified );
    new CatalogImpl( this.catName, this.catDocBaseUri, this.catVersion, this.catExpires, null );
  }

  @Test
  public void checkOtherSettersWithNull()
  {
    this.catImpl.setName( null );
    this.catImpl.setVersion( null );
    this.catImpl.setExpires( null );
    this.catImpl.setLastModified( null );
  }

  @Test
  public void checkCatalogAsExpected()
  {
    assertBasicCatalogAsExpected();
  }

  private void assertBasicCatalogAsExpected()
  {
    assertEquals( this.catName, this.catImpl.getName());
    assertEquals( this.catDocBaseUri, this.catImpl.getDocBaseUri());
    assertEquals( this.catVersion, this.catImpl.getVersion());
    assertEquals( this.catExpires, this.catImpl.getExpires());
    assertEquals( this.catLastModified, this.catImpl.getLastModified());
  }

  @Test
  public void checkChangedCatalogAsExpected() throws URISyntaxException
  {
    this.catImpl.setName( "name" );
    assertEquals( "name", this.catImpl.getName());

    URI uri = new URI( "http://server/thredds/bad/" );
    this.catImpl.setDocBaseUri( uri );
    assertEquals( uri, this.catImpl.getDocBaseUri());

    this.catImpl.setVersion( "ver" );
    assertEquals( "ver", this.catImpl.getVersion());

    this.catImpl.setExpires( this.catExpires2 );
    assertEquals( this.catExpires2, this.catImpl.getExpires());

    this.catImpl.setLastModified( this.catLastModified2 );
    assertEquals( this.catLastModified2, this.catImpl.getLastModified());
  }

  @Test
  public void checkCatalogAddGetRemoveProperties()
  {
    this.catImpl.addProperty( "color", "red" );
    this.catImpl.addProperty( "taste", "sweet" );

    assertBasicCatalogAsExpected();

    assertEquals( "red", this.catImpl.getPropertyValue( "color" ));
    assertEquals( "sweet", this.catImpl.getPropertyValue( "taste" ));

    assertTrue( this.catImpl.removeProperty( "color" ));

    assertNull( this.catImpl.getPropertyValue( "color" ));
  }

  @Test
  public void checkCatalogAddGetRemoveServices() throws URISyntaxException
  {
    ServiceBuilder odapService =  this.catImpl.addService( "odap", ServiceType.OPENDAP, new URI( "http://server/thredds/dodsC/") );
    ServiceBuilder wcsService =  this.catImpl.addService( "wcs", ServiceType.WCS, new URI( "http://server/thredds/wcs/") );

    assertBasicCatalogAsExpected();

    assertEquals( odapService, this.catImpl.getServiceBuilderByName( "odap" ));
    assertEquals( wcsService, this.catImpl.getServiceBuilderByName( "wcs" ));

    List<ServiceBuilder> services = this.catImpl.getServiceBuilders();
    assertFalse( services.isEmpty());
    assertEquals( 2, services.size());
    assertEquals( odapService, services.get( 0));
    assertEquals( wcsService, services.get( 1));

    assertTrue( this.catImpl.removeService( odapService ));
    assertNull( this.catImpl.getServiceBuilderByName( "odap" ));
  }

  @Test
  public void checkCatalogAddGetRemoveCatalogRefsAndDatasets() throws URISyntaxException
  {
    CatalogRefBuilder cat2 =  this.catImpl.addCatalogRef( "cat2", new URI( "http://server/thredds/cat2.xml") );
    CatalogRefBuilder cat3 =  this.catImpl.addCatalogRef( "cat3", new URI( "http://server/thredds/cat3.xml") );
    cat2.setId( "cat2");
    cat3.setId( "cat3");
    DatasetBuilder ds1 = this.catImpl.addDataset( "data1" );
    DatasetBuilder ds2 = this.catImpl.addDataset( "data2" );
    ds1.setId( "dataId1" );
    ds2.setId( "dataId2" );

    assertBasicCatalogAsExpected();

    assertEquals( cat2, this.catImpl.getDatasetNodeBuilderById( "cat2" ) );
    assertEquals( cat3, this.catImpl.getDatasetNodeBuilderById( "cat3" ) );
    assertEquals( ds1, this.catImpl.getDatasetNodeBuilderById( "dataId1" ) );
    assertEquals( ds2, this.catImpl.getDatasetNodeBuilderById( "dataId2" ) );

    List<DatasetNodeBuilder> dsNodes = this.catImpl.getDatasetNodeBuilders();
    assertFalse( dsNodes.isEmpty() );
    assertEquals( 4, dsNodes.size() );

    assertEquals( cat2, dsNodes.get( 0));
    assertEquals( cat3, dsNodes.get( 1));
    assertEquals( ds1, dsNodes.get( 2));
    assertEquals( ds2, dsNodes.get( 3));

    assertTrue( this.catImpl.removeDataset( cat2 ));
    assertTrue( this.catImpl.removeDataset( ds1 ));

    assertNull( this.catImpl.getDatasetNodeBuilderById( "cat2" ));
    assertNull( this.catImpl.getDatasetNodeBuilderById( "dataId1" ));
  }

  @Test
  public void checkBuildIssuesForMultipleServices()
          throws URISyntaxException
  {
    this.catImpl.addService( "odap", ServiceType.OPENDAP,  new URI( "http://server/thredds/dodsC/"));
    this.catImpl.addService( "wcs", ServiceType.WCS,  new URI( "http://server/thredds/wcs/"));
    this.catImpl.addService( "wms", ServiceType.WMS,  new URI( "http://server/thredds/wms/"));

    BuilderIssues issues = this.catImpl.getIssues();
    assertTrue( issues.toString(), issues.isValid() );
    assertTrue( issues.toString(), issues.isEmpty() );
  }

  @Test
  public void checkBuildIssuesForDuplicateServiceName()
          throws URISyntaxException
  {
    this.catImpl.addService( "odap", ServiceType.OPENDAP, new URI( "http://server/thredds/dodsC/" ) );
    this.catImpl.addService( "odap", ServiceType.OPENDAP, new URI( "http://server/thredds/dodsC/" ) );

    BuilderIssues issues = this.catImpl.getIssues();
    assertTrue( issues.toString(), issues.isValid() );
    assertFalse( issues.toString(), issues.isEmpty() );
  }

  @Test
  public void checkBuildIssuesCompoundService()
          throws URISyntaxException
  {
    ServiceImpl compoundService = (ServiceImpl) this.catImpl.addService( "all", ServiceType.COMPOUND, new URI( "") );
    compoundService.addService( "odap", ServiceType.OPENDAP,  new URI( "http://server/thredds/dodsC/"));
    compoundService.addService( "wcs", ServiceType.WCS,  new URI( "http://server/thredds/wcs/"));
    compoundService.addService( "wms", ServiceType.WMS,  new URI( "http://server/thredds/wms/"));

    BuilderIssues issues = this.catImpl.getIssues();
    assertTrue( issues.toString(), issues.isValid() );
    assertTrue( issues.toString(), issues.isEmpty() );
  }

  @Test
  public void checkBuildIssuesCompoundServiceWithDuplicate()
          throws URISyntaxException
  {
    ServiceImpl compoundService = (ServiceImpl) this.catImpl.addService( "all", ServiceType.COMPOUND, new URI( "") );
    compoundService.addService( "odap", ServiceType.OPENDAP,  new URI( "http://server/thredds/dodsC/"));
    compoundService.addService( "wcs", ServiceType.WCS,  new URI( "http://server/thredds/wcs/"));
    compoundService.addService( "wms", ServiceType.WMS,  new URI( "http://server/thredds/wms/"));
    compoundService.addService( "odap", ServiceType.WMS,  new URI( "http://server/thredds/wms/"));

    BuilderIssues issues = this.catImpl.getIssues();
    assertTrue( issues.toString(), issues.isValid() );
    assertFalse( issues.toString(), issues.isEmpty() );
  }

  @Test
  public void checkBuildIssuesForDuplicateServiceNameInDifferentContainer()
          throws URISyntaxException
  {
    ServiceImpl compoundService = (ServiceImpl) this.catImpl.addService( "all", ServiceType.COMPOUND, new URI( "" ) );
    compoundService.addService( "odap", ServiceType.OPENDAP, new URI( "http://server/thredds/dodsC/" ) );
    compoundService.addService( "wcs", ServiceType.WCS, new URI( "http://server/thredds/wcs/" ) );
    compoundService.addService( "wms", ServiceType.WMS, new URI( "http://server/thredds/wms/" ) );
    compoundService.addService( "odap", ServiceType.WMS, new URI( "http://server/thredds/wms/" ) );

    this.catImpl.addService( "odap", ServiceType.OPENDAP, new URI( "http://server/thredds/dodsC/" ) );

    BuilderIssues issues = this.catImpl.getIssues();
    assertTrue( issues.toString(), issues.isValid() );
    assertFalse( issues.toString(), issues.isEmpty() );
  }

}
