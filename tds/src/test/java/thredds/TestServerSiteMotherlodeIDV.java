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
    StringBuilder curLog = new StringBuilder();
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
    StringBuilder curLog = new StringBuilder();
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
    StringBuilder curLog = new StringBuilder();
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
    StringBuilder curLog = new StringBuilder();
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

  protected static boolean checkCatalogResolverDatasets( WebConversation wc, WebResponse resp, StringBuilder curLog )
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
