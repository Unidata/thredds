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

import java.net.URI;
import java.net.URISyntaxException;

import thredds.catalog.ServiceType;
import thredds.catalog.DataFormatType;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuilderIssue;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderIssues;
import thredds.catalog2.Access;

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

  public void testCtorAndBuilderSetGet()
  {
    AccessImpl access = new AccessImpl( null);

    assertFalse( access.isBuilt() );

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
    AccessImpl accessImpl = new AccessImpl( null);

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
    AccessImpl accessImpl = new AccessImpl( null);

    accessImpl.setServiceBuilder( serviceBuilder );
    accessImpl.setUrlPath( urlPath );
    accessImpl.setDataSize( dataSize );
    accessImpl.setDataFormat( formatType );

    // Check if buildable
    BuilderIssues issues = accessImpl.getIssues();
    if ( ! issues.isValid() )
    {
      StringBuilder stringBuilder = new StringBuilder( "Invalid access: ").append( issues.toString() );
      fail( stringBuilder.toString() );
    }

    // Build
    Access access = null;
    try
    { access = accessImpl.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }

    assertTrue( accessImpl.isBuilt());
  }

  public void testPostBuildGetters()
  {
    AccessImpl accessImpl = new AccessImpl( null);

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
    AccessImpl accessImpl = new AccessImpl( null);

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
