// $Id$
package thredds;

import com.meterware.httpunit.WebConversation;
import junit.framework.TestCase;

import java.util.Properties;

/**
 * A description
 *
 * @author edavis
 * @since 15 July 2005 15:50:59 -0600
 */
public class TestServerSiteIDD extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( TestServerSiteIDD.class );

  private WebConversation wc;

  /** The TDS site to test. */
  private String host = "motherlode.ucar.edu:8088";

  private String targetUrl = "http://" + host + "/thredds/";

  public TestServerSiteIDD( String name )
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

  public void testIddTds()
  {
    assertTrue( "Need to implement some IDD TDS tests.", false );
  }

  // @todo Check for bad subsetting with ".ascii" request (dods.jar 1.1.7b fixes), e.g.,
  // http://motherlode.ucar.edu:8088/thredds/dodsC/radar2/KGLD/20050829_1556.raw.ascii?Reflectivity[0:1:1][0:1:1][0:1:1]
  // Header:
  //   HTTP/1.1 200 OK
  //   XDODS-Server: NetCDF-DODS-Server/3.0
  //   Content-Description: dods_error
  //   Content-Type: text/plain
  //   Date: Mon, 29 Aug 2005 19:31:41 GMT
  //   Server: Apache-Coyote/1.1
  // Body:
  //   Error {
  //      code = 0;
  //      message = "Connection cannot be read http://motherlode.ucar.edu:8088/thredds/dodsC/radar2/KGLD/20050829_1556.raw.dods?Reflectivity[0:1:1][0:1:1][0:1:1]";
  //   };
  public void testErrorSubsettingAsciiOpendapRequest()
  {
    assertTrue( "Need to implement this test (and move to different location?).", false );
  }
}

/*
 * $Log: TestServerSiteIDD.java,v $
 * Revision 1.3  2006/01/23 18:51:07  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.2  2005/10/26 23:19:33  edavis
 * Updated TDS site tests.
 *
 * Revision 1.1  2005/08/31 17:10:57  edavis
 * Update DqcServletRedirect for release as dqcServlet.war. It forwards
 * /dqcServlet/*, /dqcServlet/dqc/*, and /dqcServlet/dqcServlet/* requests
 * to /thredds/dqc/*. It also provides some URLs for testing various HTTP
 *  redirections (301, 302, 305) and forwarding (i.e.,
 * javax.servlet.RequestDispatcher.forward()) at /dqcServlet/redirect-test/.
 *
 *
 */