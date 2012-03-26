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
import java.util.List;

import thredds.catalog2.builder.*;
import thredds.catalog2.DatasetNode;
import thredds.catalog2.Property;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestDatasetNodeImpl extends TestCase
{
  private CatalogImpl parentCatalog;
  private String parentCatName;
  private URI parentCatDocBaseUri;
  private String parentCatVer;

  private DatasetNodeBuilder parentDataset;
  private String parentDsName;

  private CatalogRefBuilder catRefBldr;
  private String catRefId;
  private String catRefTitle;
  private URI catRefUri;


  private DatasetNodeImpl dsNodeImpl;
  private DatasetNode dsNode;
  private DatasetNodeBuilder dsNodeBldr;

  private String id;
  private String idAuthority;
  private String name;

  private String p1n, p1v, p2n, p2v, p3n, p3v;

  private ThreddsMetadataBuilder thrMdBuilder;
  private MetadataBuilder mdBuilder;

  private DatasetNodeBuilder childDsNodeBuilder1;
  private DatasetNodeBuilder childDsNodeBuilder2;
  private String childDsNodeName1;
  private String childDsNodeName2;
  private String childDsNodeId1;
  private String childDsNodeId2;
  private String childDsNodeId1_new;
  private String childDsNodeId2_new;

  public TestDatasetNodeImpl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    parentCatName = "parent catalog";
    parentCatDocBaseUri = null;
    try
    { parentCatDocBaseUri = new URI( "http://server/thredds/aCat.xml");
      catRefUri = new URI( "http://server/thredds/anotherCat.xml" );
    }
    catch ( URISyntaxException e )
    { fail( "Bad URI syntax: " + e.getMessage()); }
    parentCatVer = "version";
    parentCatalog = new CatalogImpl( parentCatName, parentCatDocBaseUri, parentCatVer, null, null);

    parentDsName = "parent dataset";
    parentDataset = parentCatalog.addDataset( parentDsName );

    catRefId = "catRef1";
    catRefTitle = "Catalog Ref";

    id = "id";
    idAuthority = "idAuthority";
    name = "name";

    p1n = "p1";
    p1v = "p1.v";
    p2n = "p2";
    p2v = "p2.v";
    p3n = "p3";
    p3v = "p3.v";

    childDsNodeName1 = "child ds 1";
    childDsNodeName2 = "child ds 2";
    childDsNodeId1 = "id1";
    childDsNodeId2 = "id2";
    childDsNodeId1_new = "id1_new";
    childDsNodeId2_new = "id2_new";
  }

  private void initBuilder()
  {
    assertFalse( parentDataset.isBuilt() );
    assertFalse( dsNodeBldr.isBuilt() );

    dsNodeBldr.setId( id );
    dsNodeBldr.setIdAuthority( idAuthority );

    dsNodeBldr.addProperty( p1n, p1v );
    dsNodeBldr.addProperty( p2n, p2v );
    dsNodeBldr.addProperty( p3n, p3v );

    // ToDo dsNodeBldr.setThreddsMetadata( ??? );
    // ToDo mdBuilder = dsNodeBldr.addMetadata();

    childDsNodeBuilder1 = dsNodeBldr.addDataset( childDsNodeName1 );
    childDsNodeBuilder2 = dsNodeBldr.addDataset( childDsNodeName2 );

    catRefBldr = dsNodeBldr.addCatalogRef( catRefTitle, catRefUri );
  }

  private void checkBuilderGet()
  {
    String s = dsNodeBldr.getId();
    assertTrue( "getId() [" + s + "] not as expected [" + id + "].",
                s.equals( id ) );
    s = dsNodeBldr.getIdAuthority();
    assertTrue( "getIdAuthority() [" + s + "] not as expected [" + idAuthority + "].",
                s.equals( idAuthority ) );
    s = dsNodeBldr.getName();
    assertTrue( "getName() [" + s + "] not as expected [" + name + "].",
                s.equals( name ) );

    List<String> propertyNameList = dsNodeBldr.getPropertyNames();
    assertTrue( propertyNameList.size() == 3 );
    assertTrue( propertyNameList.get( 0 ).equals( p1n ) );
    assertTrue( propertyNameList.get( 1 ).equals( p2n ) );
    assertTrue( propertyNameList.get( 2 ).equals( p3n ) );

    assertTrue( dsNodeBldr.getPropertyValue( p1n ).equals( p1v ) );
    assertTrue( dsNodeBldr.getPropertyValue( p2n ).equals( p2v ) );
    assertTrue( dsNodeBldr.getPropertyValue( p3n ).equals( p3v ) );

    // ToDo dsNodeBldr.getThreddsMetadata();
    // ToDo dsNodeBldr.getMetadataBuilders();

    List<DatasetNodeBuilder> dsNodeBuilderList = dsNodeBldr.getDatasetNodeBuilders();
    assertTrue( "Number of child datasets [" + dsNodeBuilderList.size() + "] not as expected [2].",
                dsNodeBuilderList.size() == 3 );
    assertTrue( dsNodeBuilderList.get( 0 ) == childDsNodeBuilder1 );
    assertTrue( dsNodeBuilderList.get( 1 ) == childDsNodeBuilder2 );
    assertTrue( dsNodeBuilderList.get( 2 ) == catRefBldr );

    assertNull( dsNodeBldr.getDatasetNodeBuilderById( childDsNodeId1 ) );
    assertNull( dsNodeBldr.getDatasetNodeBuilderById( childDsNodeId2 ) );
    assertNull( dsNodeBldr.getDatasetNodeBuilderById( catRefId ) );

    String newName = "new name";
    dsNodeBldr.setName( newName );
    s = dsNodeBldr.getName();
    assertTrue( "Renamed getName() [" + s + "] not as expected [" + newName + "].",
                s.equals( newName ) );
  }

  private void checkDsNodeIdSetGetAndGlobal()
  {
    childDsNodeBuilder1.setId( childDsNodeId1 );
    childDsNodeBuilder2.setId( childDsNodeId2 );
    catRefBldr.setId( catRefId );

    assertTrue( childDsNodeBuilder1.getId().equals( childDsNodeId1 ) );
    assertTrue( childDsNodeBuilder2.getId().equals( childDsNodeId2 ) );
    assertTrue( catRefBldr.getId().equals( catRefId ) );

    assertTrue( childDsNodeBuilder1.isDatasetIdInUseGlobally( childDsNodeId1 ) );
    assertTrue( childDsNodeBuilder1.isDatasetIdInUseGlobally( childDsNodeId2 ) );
    assertTrue( childDsNodeBuilder1.isDatasetIdInUseGlobally( catRefId ) );

    assertNull( childDsNodeBuilder1.getDatasetNodeBuilderById( childDsNodeId1 ));
    assertNull( childDsNodeBuilder1.getDatasetNodeBuilderById( childDsNodeId2 ));
    assertNull( childDsNodeBuilder1.getDatasetNodeBuilderById( catRefId ));

    assertTrue( childDsNodeBuilder1.findDatasetNodeBuilderByIdGlobally( childDsNodeId1 ) == childDsNodeBuilder1 );
    assertTrue( childDsNodeBuilder1.findDatasetNodeBuilderByIdGlobally( childDsNodeId2 ) == childDsNodeBuilder2 );
    assertTrue( childDsNodeBuilder1.findDatasetNodeBuilderByIdGlobally( catRefId ) == catRefBldr );

    childDsNodeBuilder1.setId( childDsNodeId1_new );
    childDsNodeBuilder2.setId( childDsNodeId2_new );
    assertTrue( childDsNodeBuilder1.getId().equals( childDsNodeId1_new ) );
    assertTrue( childDsNodeBuilder2.getId().equals( childDsNodeId2_new ) );
    assertFalse( childDsNodeBuilder1.isDatasetIdInUseGlobally( childDsNodeId1 ) );
    assertFalse( childDsNodeBuilder1.isDatasetIdInUseGlobally( childDsNodeId2 ) );
    assertTrue( childDsNodeBuilder1.isDatasetIdInUseGlobally( childDsNodeId1_new ) );
    assertTrue( childDsNodeBuilder1.isDatasetIdInUseGlobally( childDsNodeId2_new ) );
    assertNull( childDsNodeBuilder1.getDatasetNodeBuilderById( childDsNodeId1 ) );
    assertNull( childDsNodeBuilder1.getDatasetNodeBuilderById( childDsNodeId2 ) );
    assertTrue( childDsNodeBuilder1.findDatasetNodeBuilderByIdGlobally( childDsNodeId1_new ) == childDsNodeBuilder1 );
    assertTrue( childDsNodeBuilder1.findDatasetNodeBuilderByIdGlobally( childDsNodeId2_new ) == childDsNodeBuilder2 );
  }

  private void callBuildOnBuilder()
  {
    // Check if buildable
    BuilderIssues issues = dsNodeBldr.getIssues();
    if ( ! issues.isValid() )
    {
      StringBuilder stringBuilder = new StringBuilder( "Invalid dsNode: " ).append( issues.toString());
      fail( stringBuilder.toString() );
    }

    // Build
    try
    { dsNode = dsNodeBldr.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }

    assertFalse( parentDataset.isBuilt());
    assertTrue( dsNodeBldr.isBuilt() );
    assertTrue( childDsNodeBuilder1.isBuilt());
    assertTrue( childDsNodeBuilder2.isBuilt());
    assertTrue( catRefBldr.isBuilt());
  }

  public void testCtorBuilderSetGet()
  {
    dsNodeImpl = new DatasetNodeImpl( name, null, null );
    dsNodeBldr = dsNodeImpl;

    this.initBuilder();

    // Tests.
    checkBuilderGet();
    assertNull( dsNodeBldr.getParentCatalogBuilder() );
    assertNull( dsNodeBldr.getParentDatasetBuilder() );

  }

  public void testCtorBuilderSetId()
  {
    dsNodeImpl = new DatasetNodeImpl( name, null, null );
    dsNodeBldr = dsNodeImpl;

    this.initBuilder();

    checkDsNodeIdSetGetAndGlobal();
  }

  public void testChildDatasetNodeBuilderGetSet()
  {
    dsNodeBldr = parentDataset.addDataset( name );

    this.initBuilder();

    // Tests.
    checkBuilderGet();
    assertTrue( dsNodeBldr.getParentCatalogBuilder() == parentCatalog );
    assertTrue( dsNodeBldr.getParentDatasetBuilder() == parentDataset );

  }

  public void testChildDatasetNodeBuilderSetId()
  {
    dsNodeBldr = parentDataset.addDataset( name );

    this.initBuilder();

    checkDsNodeIdSetGetAndGlobal();
  }

  public void testBuilderRemove()
  {
    // Setup
    dsNodeBldr = parentDataset.addDataset( name );

    this.initBuilder();

    assertTrue( dsNodeBldr.removeProperty( p1n ));
    assertNull( dsNodeBldr.getPropertyValue( p1n ));

    assertTrue( dsNodeBldr.removeDatasetNode( childDsNodeBuilder1 ));
    assertNull( dsNodeBldr.getDatasetNodeBuilderById( childDsNodeId1 ));
    assertFalse( dsNodeBldr.isDatasetIdInUseGlobally( childDsNodeId1 ));

    assertTrue( dsNodeBldr.removeDatasetNode( catRefBldr ));
    assertNull( dsNodeBldr.getDatasetNodeBuilderById( catRefId ));
    assertFalse( dsNodeBldr.isDatasetIdInUseGlobally( catRefId ));
  }

  public void testBuilderIllegalStateExceptionOnProperty()
  {
    dsNodeBldr = parentDataset.addDataset( name );
    this.initBuilder();

    dsNode = (DatasetNodeImpl) dsNodeBldr;

    try
    {
      // Should throw IllegalStateException
      dsNode.getProperties();
    }
    catch ( IllegalStateException ise1 )
    {
      try
      {
        // Should throw IllegalStateException
        dsNode.getPropertyByName( p1n );
      }
      catch ( IllegalStateException ise2 )
      { return; }
      catch ( Exception e )
      { fail( "Unexpected non-IllegalStateException: " + e.getMessage()); }
    }
    catch ( Exception e)
    { fail( "Unexpected non-IllegalStateException: " + e.getMessage()); }
    fail( "No IllegalStateException thrown.");
  }

  public void testBuilderIllegalStateExceptionOnDataset()
  {
    dsNodeBldr = parentDataset.addDataset( name );
    this.initBuilder();

    dsNode = (DatasetNodeImpl) dsNodeBldr;

    try
    {
      // Should throw IllegalStateException
      dsNode.getDatasets();
    }
    catch ( IllegalStateException ise1 )
    {
      try
      {
        // Should throw IllegalStateException
        dsNode.getDatasetById( childDsNodeId1 );
      }
      catch ( IllegalStateException ise2 )
      { return; }
      catch ( Exception e )
      { fail( "Unexpected non-IllegalStateException: " + e.getMessage() ); }
    }
    catch ( Exception e )
    { fail( "Unexpected non-IllegalStateException: " + e.getMessage() ); }
    fail( "No IllegalStateException thrown." );
  }

  public void testBuild()
  {
    dsNodeBldr = parentDataset.addDataset( name );

    this.initBuilder();

    this.callBuildOnBuilder();
  }

  public void testPostBuildGetters()
  {
    dsNodeBldr = parentDataset.addDataset( name );

    this.initBuilder();

    childDsNodeBuilder1.setId( childDsNodeId1 );
    childDsNodeBuilder2.setId( childDsNodeId2 );
    catRefBldr.setId( catRefId );

    this.callBuildOnBuilder();

    List<Property> pl = dsNode.getProperties();
    assertTrue( pl.size() == 3 );
    assertTrue( pl.get( 0 ).getName().equals( p1n ) );
    assertTrue( pl.get( 1 ).getName().equals( p2n ) );
    assertTrue( pl.get( 2 ).getName().equals( p3n ) );

    assertTrue( dsNode.getPropertyByName( p1n ).getName().equals( p1n ) );
    assertTrue( dsNode.getPropertyByName( p2n ).getName().equals( p2n ) );
    assertTrue( dsNode.getPropertyByName( p3n ).getName().equals( p3n ) );

    List<DatasetNode> dl = dsNode.getDatasets();
    assertTrue( dl.size() == 3 );
    assertTrue( dl.get( 0 ) == childDsNodeBuilder1 );
    assertTrue( dl.get( 1 ) == childDsNodeBuilder2 );
    assertTrue( dl.get( 2 ) == catRefBldr );

    assertTrue( dsNode.getDatasetById( childDsNodeId1 ) == childDsNodeBuilder1 );
    assertTrue( dsNode.getDatasetById( childDsNodeId2 ) == childDsNodeBuilder2 );
    assertTrue( dsNode.getDatasetById( catRefId ) == catRefBldr );
  }

  public void testPostBuildIllegalStateExceptionOnDataset()
  {
    dsNodeBldr = parentDataset.addDataset( name );

    this.initBuilder();
    this.callBuildOnBuilder();

    try
    {
      // Should throw IllegalStateException
      dsNodeBldr.getDatasetNodeBuilders();
    }
    catch ( IllegalStateException ise1 )
    {
      try
      {
        // Should throw IllegalStateException
        dsNodeBldr.getDatasetNodeBuilderById( childDsNodeId1 );
      }
      catch ( IllegalStateException ise2 )
      { return; }
      catch ( Exception e )
      { fail( "Unexpected non-IllegalStateException: " + e.getMessage() ); }
    }
    catch ( Exception e )
    { fail( "Unexpected non-IllegalStateException: " + e.getMessage() ); }
    fail( "No IllegalStateException thrown." );
  }
}