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
import thredds.catalog2.builder.*;
import thredds.catalog2.Dataset;
import thredds.catalog2.Access;
import thredds.catalog2.DatasetNode;
import thredds.catalog.ServiceType;
import thredds.catalog.DataFormatType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestDatasetImpl extends TestCase
{
  private CatalogImpl parentCatalog;
  private String parentCatName;
  private URI parentCatDocBaseUri;
  private String parentCatVer;

  private DatasetNodeBuilder parentDatasetBldr;
  private DatasetNode parentDataset;
  private String parentDsName;

  private CatalogRefBuilder catRefBldr;
  private String catRefTitle;
  private URI catRefUri;

  private DatasetImpl dsImpl;
  private DatasetBuilder dsBuilder;
  private Dataset ds;
  private String dsName;

  private ServiceBuilder sb1, sb2;
  private String sn1, sn2;
  private URI sbu1, sbu2;
  private ServiceType sType1, sType2;

  private AccessBuilder ab1, ab2;
  private String aup1, aup2;
  private int ads1, ads2;
  private DataFormatType aft1, aft2;


  public TestDatasetImpl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    parentCatName = "parent catalog";
    try
    {
      parentCatDocBaseUri = new URI( "http://server/thredds/aCat.xml" );
      catRefUri = new URI( "http://server/thredds/anotherCat.xml");
      sbu1 = new URI( "http://server/thredds/dodsC/");
      sbu2 = new URI( "http://server/thredds/wcs/");
    }
    catch ( URISyntaxException e )
    {
      fail( "Bad URI syntax: " + e.getMessage() );
      return;
    }
    parentCatVer = "version";
    parentCatalog = new CatalogImpl( parentCatName, parentCatDocBaseUri, parentCatVer, null, null );

    sn1 = "odap";
    sType1 = ServiceType.OPENDAP;
    sb1 = parentCatalog.addService( sn1, sType1, sbu1 );

    sn2 = "wcs";
    sType2 = ServiceType.WCS;
    sb2 = parentCatalog.addService( sn2, sType2,  sbu2);

    parentDsName = "parent dataset";
    parentDatasetBldr = parentCatalog.addDataset( parentDsName );

    dsName = "dataset name";
    dsBuilder = parentDatasetBldr.addDataset( "dsName" );

    catRefTitle = "Catalog Ref";
    catRefBldr = parentDatasetBldr.addCatalogRef( catRefTitle, catRefUri  );

    ab1 = dsBuilder.addAccessBuilder();

    aup1 = "someData.nc";
    ads1 = 5678;
    aft1 = DataFormatType.NETCDF;

    ab1.setServiceBuilder( sb1 );
    ab1.setUrlPath( aup1 );
    ab1.setDataSize( ads1 );
    ab1.setDataFormat( aft1 );

    ab2 = dsBuilder.addAccessBuilder();

    aup2 = "someData2.nc";
    ads2 = 56782;
    aft2 = DataFormatType.GRIB2;

    ab2.setServiceBuilder( sb2 );
    ab2.setUrlPath( aup2 );
    ab2.setDataSize( ads2 );
    ab2.setDataFormat( aft2 );
  }

  public void testBuilderGet()
  {
    assertFalse( parentDatasetBldr.isBuilt() );
    assertFalse( catRefBldr.isBuilt() );
    assertFalse( dsBuilder.isBuilt() );

    List<AccessBuilder> abl = dsBuilder.getAccessBuilders();
    assertTrue( abl.size() == 2 );
    assertTrue( abl.get( 0) == ab1);
    assertTrue( abl.get( 1) == ab2);

    List<AccessBuilder> ablOdap = dsBuilder.getAccessBuildersByType( ServiceType.OPENDAP );
    assertTrue( ablOdap.size() == 1 );
    assertTrue( ablOdap.get( 0) == ab1 );
    List<AccessBuilder> ablWcs = dsBuilder.getAccessBuildersByType( ServiceType.WCS );
    assertTrue( ablWcs.size() == 1 );
    assertTrue( ablWcs.get( 0 ) == ab2 );
  }

  public void testBuilderRemove()
  {
    assertTrue( dsBuilder.removeAccessBuilder( ab1 ));
    List<AccessBuilder> abl = dsBuilder.getAccessBuilders();
    assertTrue( abl.size() == 1 );
    assertTrue( abl.get( 0 ) == ab2 );

    List<AccessBuilder> ablOdap = dsBuilder.getAccessBuildersByType( ServiceType.OPENDAP );
    assertTrue( ablOdap.size() == 0 );

    List<AccessBuilder> ablWcs = dsBuilder.getAccessBuildersByType( ServiceType.WCS );
    assertTrue( ablWcs.size() == 1 );
    assertTrue( ablWcs.get( 0 ) == ab2 );

    assertFalse( dsBuilder.removeAccessBuilder( ab1 ) );
  }

  public void testBuilderIllegalStateException()
  {
    ds = (DatasetImpl) dsBuilder;
    try
    { ds.getAccesses(); }
    catch ( IllegalStateException ise )
    {
      try
      { ds.getAccessesByType( ServiceType.OPENDAP ); }
      catch ( IllegalStateException ise2 )
      { return; }
      catch ( Exception e )
      { fail( "Unexpected non-IllegalStateException: " + e.getMessage()); }
    }
    catch( Exception e )
    { fail( "Unexpected non-IllegalStateException: " + e.getMessage() ); }
    fail( "Did not throw expected IllegalStateException." );
  }

  public void testBuild()
  {
    // Check if buildable
    BuilderIssues issues = parentDatasetBldr.getIssues();
    if ( ! issues.isValid() )
    {
      StringBuilder stringBuilder = new StringBuilder( "Invalid dataset: " ).append( issues.toString());
      fail( stringBuilder.toString() );
    }

    // Build
    try
    { parentDataset = parentDatasetBldr.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }

    assertTrue( parentDatasetBldr.isBuilt() );
    assertTrue( catRefBldr.isBuilt() );
    assertTrue( dsBuilder.isBuilt() );
    ds = (Dataset) dsBuilder;

    // Test getters of resulting Dataset.
    assertTrue( ds.isAccessible() );
    List<Access> al = ds.getAccesses();
    assertTrue( al.size() == 2 );
    assertTrue( al.get( 0) == ab1 );
    assertTrue( al.get( 1) == ab2 );

    al = ds.getAccessesByType( ServiceType.OPENDAP );
    assertTrue( al.size() == 1);
    assertTrue( al.get( 0 ) == ab1 );
    al = ds.getAccessesByType( ServiceType.WCS );
    assertTrue( al.size() == 1 );
    assertTrue( al.get( 0 ) == ab2 );

    try
    { dsBuilder.addAccessBuilder(); }
    catch( IllegalStateException ise )
    { return; }
    catch( Exception e )
    { fail( "Unexpected non-IllegalStateException thrown: " + e.getMessage()); }
    fail( "Did not throw expected IllegalStateException.");
  }
}
