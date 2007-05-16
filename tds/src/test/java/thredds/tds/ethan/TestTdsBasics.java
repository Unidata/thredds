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

  private boolean showDebug = false;

  private String host = "motherlode.ucar.edu:8080";
  private String[] catalogList;

  private String targetTdsUrl;

  public TestTdsBasics( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    showDebug = Boolean.parseBoolean( System.getProperty( "thredds.tds.test.showDebug", "false" ) );
    host = System.getProperty( "thredds.tds.test.server", host );
    String catalogListString = System.getProperty( "thredds.tds.test.catalogs", null );
    if ( catalogListString == null )
      catalogListString = System.getProperty( "thredds.tds.test.catalog", "catalog.xml" );
    catalogList = catalogListString.split( ",");


    targetTdsUrl = "http://" + host + "/thredds/";
  }

  public void testPingCatalogs()
  {
    boolean pass = true;
    StringBuffer msg = new StringBuffer();

    for ( int i = 0; i < catalogList.length; i++ )
    {
      pass &= null != TestAll.openAndValidateCatalog( targetTdsUrl + catalogList[i], msg, showDebug);
    }
    assertTrue( "Ping failed on catalog(s): " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testCrawlCatalogs()
  {
    boolean pass = true;
    StringBuffer msg = new StringBuffer();

    for ( int i = 0; i < catalogList.length; i++ )
    {
      pass &= TestAll.openAndValidateCatalogTree( targetTdsUrl + catalogList[i], msg, true );
    }
    assertTrue( "Invalid catalog(s): " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testCrawlCatalogsOneLevelDeep()
  {
    boolean pass = true;
    StringBuffer msg = new StringBuffer();

    for ( int i = 0; i < catalogList.length; i++ )
    {
      pass &= TestAll.openAndValidateCatalogOneLevelDeep( targetTdsUrl + catalogList[i], msg, false );
    }

    assertTrue( "Invalid catalog(s): " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testCrawlCatalogsOpenOneDatasetInEachCollection()
  {
    boolean pass = true;
    StringBuffer msg = new StringBuffer();

    for ( int i = 0; i < catalogList.length; i++ )
    {
      pass &= TestAll.crawlCatalogOpenRandomDataset( targetTdsUrl + catalogList[i], msg );
    }

    assertTrue( "Failed to open dataset(s): " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

}
