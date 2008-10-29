package thredds.catalog2.simpleImpl;

import junit.framework.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;

import thredds.catalog2.builder.BuilderIssue;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.Metadata;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestMetadataImpl extends TestCase
{
  private MetadataImpl mdImpl1;
  private Metadata md1;
  private MetadataImpl mdImpl2;
  private Metadata md2;

  private URI uri;
  private String title;
  private String content;

  public TestMetadataImpl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    try
    { uri = new URI( "http://server/thredds/md.xml" ); }
    catch ( URISyntaxException e )
    { fail("Bad URI syntax: " + e.getMessage()); return; }
    this.title = "metadata title";
    this.content = "<x>some content</x>";

    mdImpl1 = new MetadataImpl( this.title, this.uri );
    mdImpl2 = new MetadataImpl( this.content );
  }

  public void testCtorGet()
  {
    assertFalse( mdImpl1.isBuilt() );

    assertFalse( mdImpl1.isContainedContent());
    assertTrue( mdImpl1.getTitle().equals(  this.title ));
    assertTrue( mdImpl1.getExternalReference().equals( this.uri ));

    assertTrue( mdImpl2.isContainedContent());
    assertTrue( mdImpl2.getContent().equals( this.content));
  }

  public void testBuilderIllegalState()
  {
    // Test getContent() when isContainedContent()==false;
    // Should throw IllegalStateException.
    try
    { mdImpl1.getContent(); }
    catch ( IllegalStateException ise1 )
    {
      // Test getTitle() when isContainedContent()==true;
      // Should throw IllegalStateException.
      try
      { mdImpl2.getTitle(); }
      catch ( IllegalStateException ise2 )
      {
        // Test ExternalReference() when isContainedContent()==true;
        // Should throw IllegalStateException.
        try
        { mdImpl2.getExternalReference(); }
        catch ( IllegalStateException ise3 )
        {
          return;
        }
        catch( Exception e )
        { fail( "Unexpected non-IllegalStateException."); }
      }
      catch( Exception e )
      { fail( "Unexpected non-IllegalStateException."); }
    }
    catch( Exception e )
    { fail( "Unexpected non-IllegalStateException."); }
    fail( "Did not throw expected IllegalStateException." );
  }

  public void testBuild()
  {
    // Check if buildable
    List<BuilderIssue> issues = new ArrayList<BuilderIssue>();
    if ( ! mdImpl1.isBuildable( issues ) )
    {
      StringBuilder stringBuilder = new StringBuilder( "Not isBuildable(): " );
      for ( BuilderIssue bfi : issues )
        stringBuilder.append( "\n    " ).append( bfi.getMessage() ).append( " [" ).append( bfi.getBuilder().getClass().getName() ).append( "]" );
      fail( stringBuilder.toString() );
    }
    if ( ! mdImpl2.isBuildable( issues ) )
    {
      StringBuilder stringBuilder = new StringBuilder( "Not isBuildable(): " );
      for ( BuilderIssue bfi : issues )
        stringBuilder.append( "\n    " ).append( bfi.getMessage() ).append( " [" ).append( bfi.getBuilder().getClass().getName() ).append( "]" );
      fail( stringBuilder.toString() );
    }

    // Build
    try
    { md1 = mdImpl1.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }

    try
    { md2 = mdImpl2.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }
  }

  public void testBuiltGet()
  {
    // *** Build doesn't do anything since MetadataImpl is immutable. ***
//    this.testBuild();
//
//    assertFalse( md1.isContainedContent() );
//    assertTrue( md1.getTitle().equals( this.title ) );
//    assertTrue( md1.getExternalReference().equals( this.uri ) );
//
//    assertTrue( md2.isContainedContent() );
//    assertTrue( md2.getContent().equals( this.content ) );
  }

  public void testBuiltGetIllegalState()
  {
    // *** Build doesn't do anything since MetadataImpl is immutable. ***
//    this.testBuild();
//
//    // Test getContent() when isContainedContent()==false;
//    // Should throw IllegalStateException.
//    try
//    { md1.getContent(); }
//    catch ( IllegalStateException ise1 )
//    {
//      // Test getTitle() when isContainedContent()==true;
//      // Should throw IllegalStateException.
//      try
//      { md2.getTitle(); }
//      catch ( IllegalStateException ise2 )
//      {
//        // Test getExternalReference() when isContainedContent()==true;
//        // Should throw IllegalStateException.
//        try
//        { md2.getExternalReference(); }
//        catch ( IllegalStateException ise3 )
//        {
//          return;
//        }
//        catch( Exception e )
//        { fail( "Unexpected non-IllegalStateException."); }
//      }
//      catch( Exception e )
//      { fail( "Unexpected non-IllegalStateException."); }
//    }
//    catch( Exception e )
//    { fail( "Unexpected non-IllegalStateException."); }
//    fail( "Did not throw expected IllegalStateException." );
  }
}
