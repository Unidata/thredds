// $Id: TestServerSiteMotherlodeIDV.java 51 2006-07-12 17:13:13Z caron $
package thredds;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A description
 *
 * @author edavis
 * @since 15 July 2005 15:50:59 -0600
 */
public class TestServerSiteMotherlodeIDV extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( TestServerSiteMotherlodeIDV.class );

  private WebConversation wc;

  /** The TDS site to test. */
  private String host = "motherlode.ucar.edu:8080";

  String latestString = "dqc/latestModel-InvCat1.0";
  String latestOldString = "dqc/latestModel-InvCat0.6";
  String [] queryStrings = {
          "?nam_211", "?gfs_211", "?gfs_37-44", "?gfs_25-26",
          "?ruc_211", "?ruc2_236", "?sst_21-24", "?sst_61-64", "?ocean_21-24"
  };

  private String targetUrl;

  public TestServerSiteMotherlodeIDV( String name )
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

  public void testIdvCatalog()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp = TestServerSite.getResponseToAGetRequest( wc, targetUrl + "idv/rt-models.1.0.xml", curLog );
    assertTrue( curLog.toString(), resp != null );

    if ( ! TestServerSite.checkInvCatalog( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }

    // @todo check the resolver datasets in this catalog
//    if ( ! checkCatalogResolverDatasets( wc, resp, curLog, 0, 2 ) )
//    {
//      assertTrue( curLog.toString(), false );
//      return;
//    }

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testIdvCatalogOld()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp = TestServerSite.getResponseToAGetRequest( wc, targetUrl + "idv/rt-models.xml", curLog );
    assertTrue( curLog.toString(), resp != null );

    if ( ! TestServerSite.checkResponseCodeOk( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }

    // @todo Reading the DOM of a 0.6 catalog fails. Why?
//    if ( ! TestServerSite.checkInvCatalog( resp, curLog ) )
//    {
//      assertTrue( curLog.toString(), false );
//      return;
//    }

    // @todo check the resolver datasets in this catalog
//    if ( ! checkCatalogResolverDatasets( wc, resp, curLog, 0, 2 ) )
//    {
//      assertTrue( curLog.toString(), false );
//      return;
//    }

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testIdvLatestDatasets()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp;

    for ( int i = 0; i < queryStrings.length; i++ )
    {
      resp = TestServerSite.getResponseToAGetRequest( wc, targetUrl + latestString + queryStrings[i], curLog );
      assertTrue( curLog.toString(), resp != null );

      if ( ! TestServerSite.checkInvCatalog( resp, curLog ) )
      {
        assertTrue( curLog.toString(), false );
        return;
      }
    }

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testIdvLatestDatasetsOld()
  {
    StringBuffer curLog = new StringBuffer();
    WebResponse resp;

    for ( int i = 0; i < queryStrings.length; i++ )
    {
      resp = TestServerSite.getResponseToAGetRequest( wc, targetUrl + latestOldString + queryStrings[i], curLog );
      assertTrue( curLog.toString(), resp != null );

      if ( ! TestServerSite.checkResponseCodeOk( resp, curLog ) )
      {
        assertTrue( curLog.toString(), false );
        return;
      }

      // @todo Reading the DOM of a 0.6 catalog fails. Why?
//      if ( ! TestServerSite.checkInvCatalog( resp, curLog ) )
//      {
//        assertTrue( curLog.toString(), false );
//        return;
//      }
    }

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  protected static boolean checkCatalogResolverDatasets( WebConversation wc, WebResponse resp, StringBuffer curLog )
  {
    String respUrlString = resp.getURL().toString();

    // Check that given response OK.
    if ( ! TestServerSite.checkResponseCodeOk( resp, curLog ) )
    {
      return false;
    }

    // Check that response content type is XML.
    if ( ! resp.getContentType().equals( "text/xml" ) )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - not XML." );
      return false;
    }

    // Parse the response and get DOM.
    Document doc;
    try
    {
      doc = resp.getDOM();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - could not parse:" ).append( e.getMessage() );
      return false;
    }

    // Check that this is a valid catalog.
    if ( ! TestServerSite.checkInvCatalog( doc, respUrlString, curLog ) )
    {
      return false;
    }

    boolean success = true;

    // Find all Resolver services.
    NodeList nodeList = doc.getElementsByTagName( "service" );
    List resolverList = new ArrayList();
    Node curNode;
    for ( int i = 0; i < nodeList.getLength(); i++ )
    {
      curNode = nodeList.item( i );
      if ( curNode.getAttributes().getNamedItem( "serviceType").getNodeValue().equals( "Resolver") )
      {
        resolverList.add( curNode );
      }
    }

    // Find all datasets that have Resolver access.
    // @todo
    //doc.get
    resolverList.iterator();
    WebResponse curResp = TestServerSite.getResponseToAGetRequest( wc, "", curLog );
    TestServerSite.checkResponseCodeOk( curResp, curLog );


    return ( success );
  }

}

/*
 * $Log: TestServerSiteMotherlodeIDV.java,v $
 * Revision 1.2  2006/01/23 18:51:07  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.1  2005/10/26 23:19:33  edavis
 * Updated TDS site tests.
 *
 * Revision 1.2  2005/08/22 19:39:13  edavis
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