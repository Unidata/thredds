package thredds;

import com.meterware.httpunit.*;
import junit.framework.TestCase;

import java.util.Properties;

/**
 * A description
 *
 * @author edavis
 * @since 15 July 2005 15:50:59 -0600
 */
public class TestServerSiteOutOfBox extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( TestServerSiteOutOfBox.class );

  private WebConversation wc;

  /** The TDS site to test. */
  private String host = "motherlode.ucar.edu:8088";

  private String targetUrl;

  public TestServerSiteOutOfBox( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    wc = new WebConversation();

    Properties env = System.getProperties();
    host = env.getProperty( "thredds.tds.site", host);

    targetUrl = "http://" + host + "/thredds/";

  }

  public void testFreshInstall()
  {
    assertTrue( "Need to implement some fresh install tests", false );
  }


  public void testServerSiteView()
  {
    assertTrue( "Need to implement some vanilla install tests", false );

    String catUrlString = targetUrl + "dodsC/testReletiveEta/catalog.xml";
    String dsIdString = "test/eta/latest.xml";
    String theTestUrl = targetUrl + "view/nj22UI.jnlp?catalog=" + catUrlString + "&dataset=" + dsIdString;

    StringBuilder curLog = new StringBuilder();
    WebResponse resp = TestServerSite.getResponseToAGetRequest( wc, theTestUrl, curLog );
    assertTrue( curLog.toString(), resp != null );

    boolean success = TestServerSite.checkResponseCodeOk( resp, curLog );

    if ( success )
    {
      String jnlpContentTypeString = "application/x-java-jnlp-file";
      if ( ! resp.getContentType().equals( jnlpContentTypeString ) )
      {
        curLog.append( "\n" ).append( theTestUrl ).append( " - content type not \"" ).append( jnlpContentTypeString ).append( "\"  " );
        success = false;
      }
    }

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

}
