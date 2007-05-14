package thredds.tds.ethan;

import junit.framework.TestCase;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 30, 2006 11:13:36 AM
 */
public class TestTdsBasics extends TestCase
{

  private String host = "motherlode.ucar.edu:8080";
  private String catalog = "catalog.xml";

  private String catalogUrl;

  public TestTdsBasics( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    host = System.getProperty( "thredds.tds.test.server", host );
    catalog = System.getProperty( "thredds.tds.test.catalog", catalog );

    catalogUrl = "http://" + host + "/thredds/" + catalog;
  }

  public void testPingCatalog()
  {
    TestAll.openAndValidateCatalog( catalogUrl );
  }

  public void testCrawlCatalog()
  {
    StringBuffer msg = new StringBuffer();

    boolean pass = TestAll.openAndValidateCatalogTree( catalogUrl, msg );
    assertTrue( "Invalid catalog(s) under catalog <" + catalogUrl + ">: " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testCrawlCatalogOneLevelDeep()
  {
    StringBuffer msg = new StringBuffer();

    boolean pass = TestAll.openAndValidateCatalogOneLevelDeep( catalogUrl, msg );
    assertTrue( "Invalid catalog(s) under catalog <" + catalogUrl + ">: " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testCrawlCatalogOpenOneDatasetInEachCollection()
  {

    StringBuffer msg = new StringBuffer();

    boolean pass = TestAll.crawlCatalogOpenRandomDataset( catalogUrl, msg );
    assertTrue( "Failed to open dataset(s) under catalog <" + catalogUrl + ">: " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

}
