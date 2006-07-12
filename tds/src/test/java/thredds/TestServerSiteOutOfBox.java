// $Id$
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

    StringBuffer curLog = new StringBuffer();
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

/*
 * $Log: TestServerSiteOutOfBox.java,v $
 * Revision 1.2  2006/01/23 18:51:07  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.1  2005/10/26 23:19:33  edavis
 * Updated TDS site tests.
 *
 * Revision 1.1  2005/08/22 19:39:13  edavis
 * Changes to switch /thredds/dqcServlet URLs to /thredds/dqc.
 * Expand testing for server installations: TestServerSiteFirstInstall
 * and TestServerSite. Fix problem with compound services breaking
 * the filtering of datasets.
 *
 * Revision 1.1  2005/08/04 22:54:50  edavis
 * Rename TestMotherlode to TestServerSite and centralize modifications
 * needed to test other sites (though still not configurable).
 *
 * Revision 1.1  2005/07/27 17:18:38  edavis
 * Added some basic HttpUnit testing of motherlode:8088 server.
 *
 *
 */