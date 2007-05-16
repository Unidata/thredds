package thredds.tds.ethan;

import junit.framework.TestCase;

import java.util.Properties;

import thredds.catalog.InvCatalogImpl;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 30, 2006 11:13:36 AM
 */
public class TestTdsPingMotherlode extends TestCase
{

  private String host = "motherlode.ucar.edu:8080";
  private String targetTomcatUrl;
  private String targetTdsUrl;

  public TestTdsPingMotherlode( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    Properties env = System.getProperties();
    host = env.getProperty( "thredds.tds.test.server", host );

    targetTomcatUrl = "http://" + host;
    targetTdsUrl = "http://" + host + "/thredds";
  }

  public void testMainCatalog()
  {
    String catUrl = targetTdsUrl + "/catalog.xml";

    StringBuffer msg = new StringBuffer();
    InvCatalogImpl catalog = TestAll.openAndValidateCatalog( catUrl, msg, false );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }
  }

  public void testTopCatalog()
  {
    String catUrl = targetTdsUrl + "/topcatalog.xml";

    StringBuffer msg = new StringBuffer();
    InvCatalogImpl catalog = TestAll.openAndValidateCatalog( catUrl, msg, false );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }
  }

  public void testIdvModelsCatalog()
  {
    String catUrl = targetTdsUrl + "/idv/models.xml";

    StringBuffer msg = new StringBuffer();
    InvCatalogImpl catalog = TestAll.openAndValidateCatalog( catUrl, msg, false );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }
  }

  public void testIdvLatestModelsCatalog()
  {
    TestAll.openValidateAndCheckAllLatestModelsInCatalogTree( targetTdsUrl + "/idv/latestModels.xml" );
  }

  public void testIdvRtModels10Catalog()
  {
    String catUrl = targetTdsUrl + "/idv/rt-models.1.0.xml";

    StringBuffer msg = new StringBuffer();
    InvCatalogImpl catalog = TestAll.openValidateAndCheckExpires( catUrl, msg );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }

    if ( msg.length() > 0 ) System.out.println( msg.toString() );
  }

  public void testIdvRtModels06Catalog()
  {
    String catUrl = targetTdsUrl + "/idv/rt-models.xml";

    StringBuffer msg = new StringBuffer();
    InvCatalogImpl catalog = TestAll.openValidateAndCheckExpires( catUrl, msg );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }

    if ( msg.length() > 0 ) System.out.println( msg.toString() );
  }

  public void testCatGenCdpCatalog()
  {
    String catUrl = targetTdsUrl + "/cataloggen/catalogs/uniModelsInvCat1.0en.xml";

    StringBuffer msg = new StringBuffer();
    InvCatalogImpl catalog = TestAll.openValidateAndCheckExpires( catUrl, msg );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }

    if ( msg.length() > 0 ) System.out.println( msg.toString() );
  }

  public void testCasestudiesCatalogs()
  {
    // ToDo crawl rather than ping the catalog(s) and check some datasets.

    String [] catUrls = {
            targetTdsUrl + "/casestudies/catalog.xml",
            targetTdsUrl + "/casestudies/ccs034Catalog.xml",
            targetTdsUrl + "/casestudies/ccs039Catalog.xml",
            targetTdsUrl + "/casestudies/july18_2002cat.xml",
            targetTdsUrl + "/casestudies/vgeeCatalog.xml"
    };

    boolean pass = true;
    StringBuffer msg = new StringBuffer();

    for ( int i = 0; i < catUrls.length; i++ )
    {
      pass &= null != TestAll.openAndValidateCatalog( catUrls[i], msg, false );
    }
    assertTrue( "Invalid catalog(s): " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testAllNcModelsCatalog()
  {
    String catUrl = targetTdsUrl + "/idd/allModels.TDS-nc.xml";

    StringBuffer msg = new StringBuffer();
    InvCatalogImpl catalog = TestAll.openAndValidateCatalog( catUrl, msg, false );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }
  }

  public void testDqcServletCatalog()
  {
    TestAll.openAndValidateDqcDoc( targetTomcatUrl + "/dqcServlet/latestModel.xml" );
  }
}
