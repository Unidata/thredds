package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.ThreddsMetadata;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestThreddsMetadataImpl extends TestCase
{
  private ThreddsMetadataImpl tmi;
  private ThreddsMetadataBuilder tmb;
  private ThreddsMetadata tm;

  private URI docRefUri;
  private String docTitle;
  private String docContent;

  private String kp1, kp2, kp3;

  public TestThreddsMetadataImpl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    try
    { docRefUri = new URI( "http://server/thredds/doc.xml" ); }
    catch ( URISyntaxException e )
    { fail("Bad URI syntax: " + e.getMessage()); return; }
    this.docTitle = "documentation title";
    this.docContent = "<x>some content</x>";
  }

  public void testCtoGetSet()
  {
    tmi = new ThreddsMetadataImpl();
    assertFalse( tmi.isBuilt() );
  }
}
