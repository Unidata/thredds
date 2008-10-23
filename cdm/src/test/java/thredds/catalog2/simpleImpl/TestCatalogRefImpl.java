package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.builder.CatalogRefBuilder;
import thredds.catalog2.builder.DatasetNodeBuilder;
import thredds.catalog2.builder.BuilderFinishIssue;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.CatalogRef;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestCatalogRefImpl extends TestCase
{
  private CatalogImpl parentCatalog;
  private String parentCatName;
  private URI parentCatDocBaseUri;
  private String parentCatVer;

  private DatasetNodeBuilder parentDataset;
  private String parentDsName;

  private CatalogRefBuilder catRefBldr;
  private CatalogRef catRef;

  private String catRefName;
  private URI catRefUri;
  private URI catRefUri2;

  public TestCatalogRefImpl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    parentCatName = "parent catalog";
    try
    {
      parentCatDocBaseUri = new URI( "http://server/thredds/aCat.xml");
      catRefUri = new URI( "http://server/thredds/cat2.xml" );
      catRefUri2 = new URI( "http://server/thredds/cat3.xml" );
    }
    catch ( URISyntaxException e )
    {
      fail( "Bad URI syntax: " + e.getMessage());
      return;
    }
    parentCatVer = "version";
    parentCatalog = new CatalogImpl( parentCatName, parentCatDocBaseUri, parentCatVer, null, null);

    parentDsName = "parent dataset";
    parentDataset = parentCatalog.addDataset( parentDsName );

    catRefName = "catRef name";
    catRefBldr = parentDataset.addCatalogRef( catRefName, catRefUri );
  }

  public void testGetSet()
  {
    assertFalse( catRefBldr.isBuilt());

    assertTrue( catRefBldr.getName().equals( catRefName ));
    assertTrue( catRefBldr.getReference().equals( catRefUri ));

    catRefBldr.setReference( catRefUri2 );
    assertTrue( catRefBldr.getReference().equals( catRefUri2 ) );
  }

  public void testBuild()
  {
    // Check if buildable
    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( !catRefBldr.isBuildable( issues ) )
    {
      StringBuilder stringBuilder = new StringBuilder( "Not isBuildable(): " );
      for ( BuilderFinishIssue bfi : issues )
        stringBuilder.append( "\n    " ).append( bfi.getMessage() ).append( " [" ).append( bfi.getBuilder().getClass().getName() ).append( "]" );
      fail( stringBuilder.toString() );
    }

    // Build
    try
    { catRef = catRefBldr.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }

    assertTrue( catRefBldr.isBuilt() );

    // Test getters of resulting CatalogRef.
    assertTrue( catRef.getName().equals( catRefName ) );
    assertTrue( catRef.getReference().equals( catRefUri ) );

    try
    { catRefBldr.setReference( catRefUri2 ); }
    catch( IllegalStateException ise )
    { return; }
    catch( Exception e )
    { fail( "Unexpected non-IllegalStateException thrown: " + e.getMessage()); }
    fail( "Did not throw expected IllegalStateException.");  
  }
}
