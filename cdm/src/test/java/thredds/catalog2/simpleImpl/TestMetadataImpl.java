package thredds.catalog2.simpleImpl;

import junit.framework.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;

import thredds.catalog2.builder.BuilderFinishIssue;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestMetadataImpl extends TestCase
{

  private URI uri;
  private String title;
  private String content;

//  private MetadataImpl me;

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
  }

  public void testCtorGet()
  {
    MetadataImpl md1 = new MetadataImpl( this.title, this.uri );
    assertFalse( md1.isContainedContent());
    assertTrue( md1.getTitle().equals(  this.title ));
    assertTrue( md1.getExternalReference().equals( this.uri ));

    MetadataImpl md2 = new MetadataImpl( this.content);
    assertTrue( md2.isContainedContent());
    assertTrue( md2.getContent().equals( this.content));

    // Test non-Builder gets before build, should all throw IllegalStateExceptions
    try
    { md1.getContent(); }
    catch ( IllegalStateException ise1 )
    {
      try
      { md2.getTitle(); }
      catch ( IllegalStateException ise2 )
      {
        try
        { md2.getExternalReference(); }
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
}
