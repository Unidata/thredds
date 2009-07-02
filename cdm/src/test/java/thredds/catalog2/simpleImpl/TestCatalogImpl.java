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

import junit.framework.*;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.text.ParseException;

import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestCatalogImpl extends TestCase
{
  private URI docBaseUri;

  private ServiceType type;
  private URI baseUri;

  public TestCatalogImpl( String name )
  {
    super( name );
  }

  protected void setUp()throws Exception
  {
    try
    { baseUri = new URI( "http://server/thredds/dodsC/" );
      docBaseUri = new URI( "http://server/thredds/aCat.xml" ); }
    catch ( URISyntaxException e )
    { fail("Bad URI syntax: " + e.getMessage()); }

    type = ServiceType.OPENDAP;
  }

  public void testConstructorNullDocBaseUri()
  {
    try
    { new CatalogImpl( "cat", null, "", null, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testConstructorNormal() throws ParseException
  {
    String name = "cat";
    String verString = "v1";
    long curTime = System.currentTimeMillis();
    DateType lastModified = new DateType( false, new Date( curTime) );
    DateType expires = new DateType( lastModified ).add( new TimeDuration( "P5D"));

    CatalogBuilder cb = new CatalogImpl( name, docBaseUri, verString, expires, lastModified );

    assertFalse( cb.isBuilt() );

    assertTrue( "Name [" + cb.getName() + "] not as expected [" + name + "].",
                cb.getName().equals( name ) );
    assertTrue( "BaseUri [" + cb.getDocBaseUri() + "] not as expected [" + docBaseUri + "].",
                cb.getDocBaseUri().equals( docBaseUri ) );
    assertTrue( "Version [" + cb.getVersion() + "] not as expected [" + verString + "].",
                cb.getVersion().equals( verString ) );
    assertTrue( "Expires time [" + cb.getExpires() + "] not as expected [" + expires + "].",
                cb.getExpires().equals( expires) );
    assertTrue( "Last modified time [" + cb.getLastModified() + "] not as expected [" + lastModified + "].",
                cb.getLastModified().equals( lastModified ) );

  }

  public void testContainerCatalogNonuniqueDatasetName()
  {
    CatalogBuilder cat = new CatalogImpl( "cat1", docBaseUri, "", null, null );
    cat.addService( "s1", type, baseUri );
    cat.addService( "s2", type, baseUri );
    try
    { cat.addService( "s1", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage() ); }
    fail( "No IllgalStateException." );
  }
  public void testContainerCatalogNonuniqueDatasetNameNested()
  {
    CatalogBuilder cat = new CatalogImpl( "cat1", docBaseUri, "", null, null );
    cat.addService( "s1", type, baseUri );
    ServiceBuilder s2 = cat.addService( "s2", type, baseUri );
    s2.addService( "s2.1", type, baseUri );
    try
    { s2.addService( "s1", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage() ); }
    fail( "No IllgalStateException." );
  }

  public void testContainerCatalogNonuniqueDatasetNameNestedTwoLevels()
  {
    CatalogBuilder cat = new CatalogImpl( "cat1", docBaseUri, "", null, null );
    cat.addService( "s1", type, baseUri );
    ServiceBuilder s2 = cat.addService( "s2", type, baseUri );
    s2.addService( "s2.1", type, baseUri );
    ServiceBuilder s2_2 = s2.addService( "s2.2", type, baseUri );
    s2_2.addService( "s2.2.1", type, baseUri );
    try
    { s2_2.addService( "s1", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage() ); }
    fail( "No IllgalStateException." );
  }

}
