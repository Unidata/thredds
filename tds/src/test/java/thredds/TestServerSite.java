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
// $Id: TestServerSite.java 55 2006-07-12 19:40:44Z edavis $
package thredds;

import com.meterware.httpunit.*;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;

import javax.servlet.http.HttpServletResponse;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.*;
import java.util.Properties;
import java.security.cert.Certificate;

/**
 * A description
 *
 * @author edavis
 * @since 15 July 2005 15:50:59 -0600
 */
public class TestServerSite extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( TestServerSite.class );


  private WebConversation wc;

  /** The TDS site to test. */
  private String host = "motherlode.ucar.edu:9080";
  /** The name of a user with tdsConfig role. */
  private String tdsConfigUser;
  private String tdsConfigWord;

  private int catGenAdminColumnCount = 6;
  private int dqcAdminColumnCount = 3;

  private String targetUrlTomcat;
  private String targetUrl;

  private int maxCrawlDepth = 2;

  public TestServerSite( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    wc = new WebConversation();

    Properties env = System.getProperties();
    host = env.getProperty( "thredds.tds.site", host);
    tdsConfigUser = env.getProperty( "thredds.tds.config.user");
    tdsConfigWord = env.getProperty( "thredds.tds.config.password");

    targetUrlTomcat = "http://" + host + "/";
    targetUrl = "http://" + host + "/thredds/";

    java.net.Authenticator.setDefault( new Authenticator()
    {
      public PasswordAuthentication getPasswordAuthentication()
      {
        return new PasswordAuthentication( tdsConfigUser, tdsConfigWord.toCharArray() );
      }
    } );

  }

  /** Test that top Tomcat page is OK. */
  public void testServerSiteTomcat()
  {
    StringBuilder curLog = new StringBuilder();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrlTomcat, curLog );
    assertTrue( curLog.toString(), resp != null );

    assertTrue( curLog.toString(), checkResponseCodeOk( resp, curLog ) );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  /**
   * Crawl /thredds/catalog.xml tree.
   */
  public void testServerSiteTopCatalog()
  {
    StringBuilder curLog = new StringBuilder();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "catalog.xml", curLog );
    assertTrue( curLog.toString(), resp != null );

    if ( ! crawlCatalogTree( wc, resp, curLog, 0, maxCrawlDepth ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  /** Crawl /thredds/catalog.html tree. */
  public void testServerSiteTop()
  {
    StringBuilder curLog = new StringBuilder();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl, curLog );
    assertTrue( curLog.toString(), resp != null );

    boolean success = checkResponseCodeOk( resp, curLog );
    success &= crawlHtmlTree( wc, resp, curLog, 0, maxCrawlDepth );
    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  /** Crawl TDS Docs pages. */
  public void testServerSiteDocsTop()
  {
    StringBuilder curLog = new StringBuilder();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "docs/", curLog );
    assertTrue( curLog.toString(), resp != null );

    boolean success = checkResponseCodeOk( resp, curLog );
    success &= crawlHtmlTree( wc, resp, curLog, 0, maxCrawlDepth );
    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteValidateTopCatalog()
  {
    StringBuilder curLog = new StringBuilder();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "catalog?cmd=validate", curLog );
    assertTrue( curLog.toString(), resp != null );

    boolean success = checkResponseCodeOk( resp, curLog );

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteSubset()
  {
    assertTrue( "Need to implement subset test", false );
  }

  public void testServerSiteConvert0_6To1_0()
  {
    assertTrue( "Need to implement 0.6 to 1.0 conversion test", false );
  }

  public void testServerSiteDebug()
  {
    if ( tdsConfigUser == null || tdsConfigWord == null )
    {
      String tmpMsg = "No \"tdsConfig\" authentication info provided - skipping this test.";
      log.warn( tmpMsg );
      assertTrue( tmpMsg, false);
      return;
    }

    StringBuilder curLog = new StringBuilder();

    // Test with tdsConfig authentication
    //wc.setAuthorization( tdsConfigUser, tdsConfigWord );

    String urlString = targetUrl + "debug";
    URL url = null;
    try
    {
      url = new URL( urlString );
    }
    catch ( MalformedURLException e )
    {
      curLog.append( "\n" ).append( urlString ).append( " - malformed URL: " ).append( e.getMessage());
      assertTrue( curLog.toString(), false );
      return;
    }
    HttpURLConnection con = null;
    try
    {
      con = (HttpURLConnection) url.openConnection();
    }
    catch ( IOException e )
    {
      curLog.append( "\n" ).append( urlString ).append( " - IOException opening connection: " ).append( e.getMessage() );
      assertTrue( curLog.toString(), false );
      return;
    }
    con.setInstanceFollowRedirects( true );
    int respCode;
    try
    {
      respCode = con.getResponseCode();
    }
    catch ( IOException e )
    {
      curLog.append( "\n" ).append( urlString ).append( " - IOException getting response code: " ).append( e.getMessage() );
      assertTrue( curLog.toString(), false );
      return;
    }
    System.out.println( urlString + " - Response code = " + respCode );
    String urlString2 = con.getHeaderField( "Location" );
    System.out.println( "location header: " + urlString2 );
    URL url2;
    try
    {
      url2 = new URL( urlString2 );
    }
    catch ( MalformedURLException e )
    {
      curLog.append( "\n" ).append( urlString2 ).append( " - malformed URL: " ).append( e.getMessage() );
      assertTrue( curLog.toString(), false );
      return;
    }
    HttpURLConnection con2 = null;
    try
    {
      con2 = (HttpURLConnection) url2.openConnection();
    }
    catch ( IOException e )
    {
      curLog.append( "\n" ).append( urlString ).append( " - IOException opening connection: " ).append( e.getMessage() );
      assertTrue( curLog.toString(), false );
      return;
    }
    System.out.println( "Check if HttpsURLConnection ..." );
    if ( con2 instanceof HttpsURLConnection )
    {
      System.out.println( "... is HttpsURLConnection" );
      HttpsURLConnection scon = (HttpsURLConnection) con2;
      Certificate[] certs = new Certificate[0];
      certs = scon.getLocalCertificates();
      for ( int i = 0; i < certs.length; i++ )
      {
        System.out.println( "Local Cert[" + i + "]: " + certs[i].toString() );
      }
      try
      {
        certs = scon.getServerCertificates();
      }
      catch ( SSLPeerUnverifiedException e )
      {
        curLog.append( "\n" ).append( urlString ).append( " - SSLPeerUnverifiedException getting certificates: " ).append( e.getMessage() );
        assertTrue( curLog.toString(), false );
        return;
      }

      for ( int i = 0; i < certs.length; i++ )
      {
        System.out.println( "Server Cert[" + i + "]: " + certs[i].toString() );
      }
    }
    else
      System.out.println( "... not HttpsURLConnection" );

    int respCode2;
    try
    {
      respCode2 = con2.getResponseCode();
    }
    catch ( IOException e )
    {
      curLog.append( "\n" ).append( urlString2 ).append( " - IOException getting response code: " ).append( e.getMessage() );
      assertTrue( curLog.toString(), false );
      return;
    }
    System.out.println( urlString + " - Response code = " + respCode2 );


    WebResponse resp = getResponseToAGetRequest( wc, targetUrlTomcat + "dqcServlet/redirect-test/302" , curLog );
    //WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "debug", curLog );
    assertTrue( curLog.toString(), resp != null );
    String respUrlString = resp.getURL().toString();

    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }
    if ( ! resp.isHTML() )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - response not HTML." );
      assertTrue( curLog.toString(), false );
      return;
    }

    boolean success = checkTitle( resp, "THREDDS Debug", curLog );

    success &= checkLinkExistence( resp, "Show Logs", curLog );
    success &= checkLinkExistence( resp, "Show Build Version", curLog );
    //success &= checkLinkExistence( resp, "Reinitialize", curLog );

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteRoot()
  {
    assertTrue( "Need to implement /thredds/root/ test", false );
  }

  public void testServerSiteContent()
  {
    if ( tdsConfigUser == null || tdsConfigWord == null )
    {
      String tmpMsg = "No \"tdsConfig\" authentication info provided - skipping this test.";
      log.warn( tmpMsg );
      assertTrue( tmpMsg, false );
      return;
    }

    // Test with tdsConfig authentication
    wc.setAuthorization( tdsConfigUser, tdsConfigWord );

    StringBuilder curLog = new StringBuilder();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "content/", curLog );
    assertTrue( curLog.toString(), resp != null );
    String respUrlString = resp.getURL().toString();

    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }
    if ( ! resp.isHTML() )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - response not HTML." );
      assertTrue( curLog.toString(), false );
      return;
    }

    boolean success = checkTitle( resp, "Directory listing for /content/", curLog );

    success &= checkLinkExistence( resp, "catalog.xml", curLog );
    success &= checkLinkExistence( resp, "cataloggen/", curLog );
    success &= checkLinkExistence( resp, "dodsC/", curLog );
    success &= checkLinkExistence( resp, "dqcServlet/", curLog );
    success &= checkLinkExistence( resp, "extraCatalogs.txt", curLog );
    success &= checkLinkExistence( resp, "logs/", curLog );
    success &= checkLinkExistence( resp, "root/", curLog );
    success &= checkLinkExistence( resp, "wcs/", curLog );
    success &= checkLinkExistence( resp, "junk/", curLog );

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteCatGen()
  {
    if ( tdsConfigUser == null || tdsConfigWord == null )
    {
      String tmpMsg = "No \"tdsConfig\" authentication info provided - skipping this test.";
      log.warn( tmpMsg );
      assertTrue( tmpMsg, false );
      return;
    }

    // Test with tdsConfig authentication
    wc.setAuthorization( tdsConfigUser, tdsConfigWord );

    StringBuilder curLog = new StringBuilder();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "cataloggen/admin/", curLog );
    assertTrue( curLog.toString(), resp != null );
    String respUrlString = resp.getURL().toString();

    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }
    if ( ! resp.isHTML() )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - response not HTML." );
      assertTrue( curLog.toString(), false );
      return;
    }

    boolean success = checkTitle( resp, "Catalog Generator Servlet Config", curLog );
    WebTable[] tables;
    try
    {
      tables = resp.getTables();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n").append( respUrlString).append( " - failed to parse: " ).append( e.getMessage());
      assertTrue( curLog.toString(),
                  false);
      return;
    }

    success &= checkTableCellText( resp, tables[0], 0, 0, "Task Name", curLog );
    success &= checkTableCellText( resp, tables[0], 0, 1, "Configuration Document", curLog );

    int columnCount = tables[0].getColumnCount();
    if ( columnCount != catGenAdminColumnCount )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " # columns <" ).append( columnCount ).append( "> not as expected <" ).append( catGenAdminColumnCount ).append( ">" );
      success = false;
      return;
    }

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteDqcServletHtml()
  {
    StringBuilder curLog = new StringBuilder();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "dqc/", curLog );
    assertTrue( curLog.toString(), resp != null );
    String respUrlString = resp.getURL().toString();

    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }
    if ( ! resp.isHTML() )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - response not HTML." );
      assertTrue( curLog.toString(), false );
      return;
    }

    boolean success = checkTitle( resp, "DQC Servlet - Available Datasets", curLog );

    WebTable[] tables;
    try
    {
      tables = resp.getTables();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - failed to parse: " ).append( e.getMessage() );
      assertTrue( curLog.toString(),
                  false );
      return;
    }

    success &= checkTableCellText( resp, tables[0], 0, 0, "Name", curLog );
    success &= checkTableCellText( resp, tables[0], 0, 1, "Description", curLog );
    success &= checkTableCellText( resp, tables[0], 0, 2, "DQC Document", curLog );

    int columnCount = tables[0].getColumnCount();
    if ( columnCount != dqcAdminColumnCount )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " # columns <" ).append( columnCount ).append( "> not as expected <" ).append( dqcAdminColumnCount ).append( ">" );
      success = false;
    }

    assertTrue( curLog.toString(), success );

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  public void testServerSiteDqcServletXml()
  {
    StringBuilder curLog = new StringBuilder();
    WebResponse resp = getResponseToAGetRequest( wc, targetUrl + "dqc/catalog.xml", curLog );
    assertTrue( curLog.toString(), resp != null );

    if ( ! checkInvCatalog( resp, curLog ) )
    {
      assertTrue( curLog.toString(), false );
      return;
    }

    if ( curLog.length() != 0 )
    {
      System.out.println( "Passed with log messages:\n" + curLog.toString() );
    }
  }

  protected static boolean checkInvCatalog( WebResponse resp, StringBuilder curLog )
  {
    String respUrlString = resp.getURL().toString();

    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      return false;
    }

    if ( ! resp.getContentType().equals( "text/xml" ) )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - not XML." );
      return false;
    }

    // Get DOM for this response.
    Document catDoc;
    try
    {
      catDoc = resp.getDOM();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - parsing error: " ).append( e.getMessage() );
      return false;
    }

    return checkInvCatalog( catDoc, respUrlString, curLog );
  }

  protected static boolean checkInvCatalog( Document catDoc, String respUrlString, StringBuilder curLog )
  {
    // Get InvCatalogImp of this response.
    InvCatalogImpl cat = null;
    org.jdom.input.DOMBuilder builder = new org.jdom.input.DOMBuilder();
    try
    {
      cat = InvCatalogFactory.getDefaultFactory( false ).readXML( builder.build( catDoc), new URI( respUrlString ) );
    }
    catch ( URISyntaxException e )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - bad URI syntax: " ).append( e.getMessage() );
      return false;
    }

    // Check validity of catalog.
    if ( ! cat.check( curLog ) )
    {
      curLog.append( "\n").append( respUrlString ).append( " - not a valid catalog." );
      return false;
    }
    return true;
  }

  protected static boolean crawlCatalogTree( WebConversation wc, WebResponse resp, StringBuilder curLog, int curCrawlDepth, int maxCrawlDepth )
  {
    if ( curCrawlDepth + 1 > maxCrawlDepth ) return true;
    curCrawlDepth++;

    String respUrlString = resp.getURL().toString();

    // Check that given response OK.
    if ( ! checkResponseCodeOk( resp, curLog ) )
    {
      return false;
    }

    // Check that response content type is XML.
    if ( ! resp.getContentType().equals( "text/xml") )
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
      curLog.append( "\n").append( respUrlString).append( " - could not parse:").append( e.getMessage() );
      return false;
    }

    // Check that this is a valid catalog.
    if ( ! checkInvCatalog( doc, respUrlString, curLog ) )
    {
      return false;
    }

    boolean success = true;
    NodeList catRefNodeList = doc.getElementsByTagName( "catalogRef");
    Node curNode;
    for ( int i = 0; i < catRefNodeList.getLength(); i++ )
    {
      curNode = catRefNodeList.item( i);
//      String curLink = curNode.getAttributes().getNamedItemNS( "http://www.w3.org/1999/xlink", "href").getNodeValue();
      String curLink = curNode.getAttributes().getNamedItem( "xlink:href").getNodeValue();
      try
      {
        curLink = new URL( resp.getURL(), curLink).toString();
      }
      catch ( MalformedURLException e )
      {
        curLog.append( "\n" ).append( curLink ).append( " - malformed URL:" ).append( e.getMessage() );
        success = false;
        continue;
      }

      // If the current link is under the current catalog URL,
      // continue crawling.
      if ( curLink.startsWith( respUrlString ))
      {
        WebResponse curResp = getResponseToAGetRequest( wc, curLink, curLog );
        if ( curResp == null )
        {
          success = false;
          continue;
        }
        success &= crawlCatalogTree( wc, curResp, curLog, curCrawlDepth, maxCrawlDepth );
      }
    }

    return( success );
  }

  protected static boolean crawlHtmlTree( WebConversation wc, WebResponse resp, StringBuilder curLog, int curCrawlDepth, int maxCrawlDepth )
  {
    if ( curCrawlDepth + 1 > maxCrawlDepth ) return true;
    curCrawlDepth++;

    String respUrlString = resp.getURL().toString();
    if ( ! resp.isHTML() && ! resp.getContentType().equals( "text/xml" ) )
    {
      curLog.append( "\n").append( respUrlString).append( " - not HTML or XML." );
      return( false );
    }

    // Get all links on this page.
    WebLink[] links;
    try
    {
      links = resp.getLinks();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n").append( respUrlString).append( " - parsing error : ").append( e.getMessage() );
      return ( false );
    }

    boolean success = true;

    for ( int i = 0; i < links.length; i++ )
    {
      String curLinkUrlString;
      WebLink curLink = links[i];
      try
      {
        curLinkUrlString =  curLink.getRequest().getURL().toString();
      }
      catch ( MalformedURLException e )
      {
        curLog.append( "\n").append( respUrlString).append( " - malformed current link URL <").append( curLink.getURLString()).append( ">: ").append( e.getMessage() );
        success = false;
        continue;
      }

      if ( curLinkUrlString.startsWith( respUrlString))
      {
        WebResponse curResp = getResponseToAGetRequest( wc, curLinkUrlString, curLog);
        if ( curResp == null )
        {
          success = false;
          continue;
        }

        success &= checkResponseCodeOk( curResp, curLog );
        if ( curResp.isHTML() )
        {
          success &= crawlHtmlTree( wc, curResp, curLog, curCrawlDepth, maxCrawlDepth );
        }
      }
    }
    return( success);
  }

  protected static boolean checkResponseCodeOk( WebResponse resp, StringBuilder log )
  {
    String respUrlString = resp.getURL().toString();
    int respCode = resp.getResponseCode();
    if ( respCode != HttpServletResponse.SC_OK )
    {
      log.append( "\n").append( respUrlString).append( " - response code <").append( respCode).append( "> not as expected <OK - 200>");
      return false;
    }
    return true;
  }

  protected static WebResponse getResponseToAGetRequest( WebConversation wc, String reqUrl, StringBuilder curLog )
  {
    WebRequest req = new GetMethodWebRequest( reqUrl );
    WebResponse resp;
    try
    {
      resp = wc.getResponse( req );
    }
    catch ( IOException e )
    {
      curLog.append( "\n").append( reqUrl).append( " - failed to get response: ").append( e.getMessage() );
      return null;
    }
    catch ( SAXException e )
    {
      curLog.append( "\n").append( reqUrl).append( " - failed to parse response: ").append( e.getMessage() );
      return null;
    }
    catch ( com.meterware.httpunit.HttpException e )
    {
      curLog.append( "\n" ).append( reqUrl ).append( " - HTTP error: " ).append( e.getMessage() );
      return null;
    }
    return resp;
  }

  protected static boolean checkTitle( WebResponse resp, String title, StringBuilder curLog )
  {
    String respUrlString = resp.getURL().toString();
    String pageTitle = null;
    try
    {
      pageTitle = resp.getTitle();
    }
    catch ( SAXException e )
    {
      curLog.append( "\n" ).append( respUrlString )
              .append( " - parse error reading page title: " )
              .append( e.getMessage() );
      return false;
    }

    if ( ! pageTitle.equals( title ) )
    {
      curLog.append( "\n" ).append( respUrlString )
              .append( " - title <" ).append( pageTitle )
              .append( "> not as expected <" ).append( title )
              .append( ">." );
      return false;
    }
    return true;
  }

  protected static boolean checkLinkExistence( WebResponse resp, String linkText, StringBuilder curLog )
  {
    String respUrlString = resp.getURL().toString();
    WebLink link = null;
    try
    {
      link = resp.getLinkWith( linkText );
    }
    catch ( SAXException e )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - parse error checking for link <\"" ).append( linkText ).append( "\">: " ).append( e.getMessage() );
      return false;
    }
    if ( link == null )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - did not find link <\"" ).append( linkText ).append( "\">: " );
      return false;
    }
    return true;
  }

  protected boolean checkTableCellText( WebResponse resp, WebTable table, int headerRow, int headerCol, String headerText, StringBuilder curLog )
  {
    String respUrlString = resp.getURL().toString();
    String headerCellAsText = table.getCellAsText( headerRow, headerCol );
    if ( ! headerCellAsText.equals( headerText ) )
    {
      curLog.append( "\n" ).append( respUrlString ).append( " - table header in column 0 <" ).append( headerCellAsText ).append( "> not as expected <" ).append( headerText ).append( ">." );
      return false;
    }
    return true;
  }

}

