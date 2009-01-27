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
