package thredds.catalog2.simpleImpl;

import junit.framework.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;

import thredds.catalog.ServiceType;
import thredds.catalog.DataFormatType;
import thredds.catalog2.builder.*;
import thredds.catalog2.Access;
import thredds.catalog2.DatasetNode;

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
  private URI parentCatUri;
  private String parentCatVer;

  private DatasetNodeBuilder parentDataset;
  private String parentDsName;

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

  public TestDatasetNodeImpl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    String parentCatName = "parent catalog";
    URI parentCatDocBaseUri = null;
    try
    { parentCatDocBaseUri = new URI( "http://server/thredds/aCat.xml"); }
    catch ( URISyntaxException e )
    { fail( "Bad URI syntax: " + e.getMessage()); }
    String parentCatVer = "version";
    parentCatalog = new CatalogImpl( parentCatName, parentCatDocBaseUri, parentCatVer, null, null);

    String parentDsName = "parent dataset";
    parentDataset = parentCatalog.addDataset( parentDsName );

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
  }

  private void initBuilder()
  {
    dsNodeBldr.setId( id );
    dsNodeBldr.setIdAuthority( idAuthority );

    dsNodeBldr.addProperty( p1n, p1v );
    dsNodeBldr.addProperty( p2n, p2v );
    dsNodeBldr.addProperty( p3n, p3v );

    // ToDo dsNodeBldr.setThreddsMetadata( ??? );
    // ToDo mdBuilder = dsNodeBldr.addMetadata();

    childDsNodeBuilder1 = dsNodeBldr.addDataset( childDsNodeName1 );
    childDsNodeBuilder2 = dsNodeBldr.addDataset( childDsNodeName2 );
    childDsNodeBuilder1.setId( childDsNodeId1 );
    childDsNodeBuilder2.setId( childDsNodeId2 );
  }

  private void callBuildOnBuilder()
  {
    // Check if buildable
    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( !dsNodeImpl.isBuildable( issues ) )
    {
      StringBuilder stringBuilder = new StringBuilder( "Not isBuildable(): " );
      for ( BuilderFinishIssue bfi : issues )
        stringBuilder.append( "\n    " ).append( bfi.getMessage() ).append( " [" ).append( bfi.getBuilder().getClass().getName() ).append( "]" );
      fail( stringBuilder.toString() );
    }

    // Build
    try
    { dsNode = dsNodeImpl.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }
  }

  public void testCtorBuilderSetGet()
  {
    // Setup.
    dsNodeImpl = new DatasetNodeImpl( name, null, null );
    dsNodeBldr = dsNodeImpl;

    this.initBuilder();

    // Tests.
    String s = dsNodeBldr.getId();
    assertTrue( "getId() ["+s+"] not as expected ["+id+"].",
                s.equals(  id ));
    s = dsNodeBldr.getIdAuthority();
    assertTrue( "getIdAuthority() ["+s+"] not as expected ["+idAuthority+"].",
                s.equals( idAuthority));
    s = dsNodeBldr.getName();
    assertTrue( "getName() ["+s+"] not as expected ["+name+"].",
                s.equals(  name ));

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
    assertTrue( "Number of child datasets ["+dsNodeBuilderList.size()+"] not as expected [2].",
                dsNodeBuilderList.size() == 2);
    assertTrue( dsNodeBuilderList.get( 0 ) == childDsNodeBuilder1 );
    assertTrue( dsNodeBuilderList.get( 1 ) == childDsNodeBuilder2 );

    assertTrue( dsNodeBldr.getDatasetNodeBuilderById( childDsNodeId1 ) == childDsNodeBuilder1 );
    assertTrue( dsNodeBldr.getDatasetNodeBuilderById( childDsNodeId2 ) == childDsNodeBuilder2 );

    assertNull( dsNodeBldr.getParentCatalogBuilder() );
    assertNull( dsNodeBldr.getParentDatasetBuilder() );

    String newName = "new name";
    dsNodeBldr.setName( newName );
    s = dsNodeBldr.getName();
    assertTrue( "Renamed getName() [" + s + "] not as expected [" + newName + "].",
                s.equals( newName ) );

    String newId1 = "newID1";
    String newId2 = "newID1";
    childDsNodeBuilder1.setId( newId1 );
    childDsNodeBuilder1.getId().equals( newId1 );
    childDsNodeBuilder2.setId( newId2 );
    childDsNodeBuilder2.getId().equals( newId2 );
  }

  public void testChildDatasetNodeBuilderGetSet()
  {
    // Setup
    dsNodeBldr = parentDataset.addDataset( name );

    this.initBuilder();

    // test similar to above

    // test when id of child dataset is changed.
  }

  public void testBuilderRemove()
  {

  }

  public void testBuilderIllegalStateException()
  {
    fail( "testBuilderIllegalStateException() not implemented.");
  }

  public void testBuild()
  {
    dsNodeImpl = new DatasetNodeImpl( this.name, null, null );

    this.initBuilder();

    // do some stuff here

    this.callBuildOnBuilder();
  }

  public void testPostBuildGetters()
  {
    fail( "testPistBuildGetters() is not implemented.");
  }

  public void testPostBuildIllegalStateException()
  {
    fail( "testPostBuildIllegalStateException() is not implemented.");
  }
}